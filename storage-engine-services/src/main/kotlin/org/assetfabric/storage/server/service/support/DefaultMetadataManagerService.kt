/*
 * Copyright (C) 2019 Asset Fabric contributors (https://github.com/orgs/asset-fabric/teams/asset-fabric-contributors)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.assetfabric.storage.server.service.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.BinaryReference
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.NodeCreationException
import org.assetfabric.storage.NodeDeletionException
import org.assetfabric.storage.NodeException
import org.assetfabric.storage.NodeModificationException
import org.assetfabric.storage.NodeNotFoundException
import org.assetfabric.storage.NodeReference
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.Query
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.State
import org.assetfabric.storage.server.command.support.JournalCommitCommand
import org.assetfabric.storage.server.command.support.JournalEntryCreateCommand
import org.assetfabric.storage.server.command.support.MetadataStoreInitializationCommand
import org.assetfabric.storage.server.model.DefaultSession
import org.assetfabric.storage.server.service.BinaryManagerService
import org.assetfabric.storage.server.service.MetadataManagerService
import org.assetfabric.storage.spi.InverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.NodeRepresentation
import org.assetfabric.storage.spi.WorkingAreaInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.WorkingAreaNodeRepresentation
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.CommittedNodeIndexPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaNodeIndexPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.search.SearchAdapter
import org.assetfabric.storage.spi.search.support.AllTextQuery
import org.assetfabric.storage.spi.support.DefaultNodeContentRepresentation
import org.assetfabric.storage.spi.support.DefaultWorkingAreaInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.support.DefaultWorkingAreaNodeRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.annotation.PostConstruct

@Service
class DefaultMetadataManagerService : MetadataManagerService {

    private val log = LogManager.getLogger(DefaultMetadataManagerService::class.java)

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var catalogPartitionAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var committedNodeIndexPartitionAdapter: CommittedNodeIndexPartitionAdapter

    @Autowired
    private lateinit var workingAreaPartitionAdapter: WorkingAreaPartitionAdapter

    @Autowired
    private lateinit var workingAreaNodeIndexPartitionAdapter: WorkingAreaNodeIndexPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @Autowired
    private lateinit var binaryManager: BinaryManagerService

    @Autowired
    private lateinit var searchAdapter: SearchAdapter

    @PostConstruct
    private fun init() {
        initializeMetadataStore().block()
    }

    override fun initializeMetadataStore(): Mono<Void> {
        val command = context.getBean(MetadataStoreInitializationCommand::class.java)
        return command.execute()
    }

    override fun repositoryRevision(): Mono<RevisionNumber> {
        return catalogPartitionAdapter.currentRepositoryRevision()
    }

    override fun createNode(session: Session, parentPath: Path, name: String, nodeType: NodeType, properties: MutableMap<String, Any>): Mono<out WorkingAreaNodeRepresentation> {
        val path = parentPath.childPath(name)

        val existingNodeMono = nodeRepresentation(session, path)
        return existingNodeMono.flatMap<WorkingAreaNodeRepresentation> {
            // if this flatMap section is executing, it's because the node has been found
            Mono.error(NodeCreationException("Cannot create existing node $path"))
        }.switchIfEmpty(Mono.defer {

            // check for the presence of all referenced nodes
            val nodeRetrieveMonos = checkNodeReferences(session, properties).onErrorMap { error ->
                NodeCreationException(error)
            }

            nodeRetrieveMonos.then(Mono.defer {

                // check for presence of all referenced binaries
                checkBinaryReferences(properties)

                // map any inputstream properties to binary references
                val mappedProperties = mapInputStreamsToBinaryReferences(properties)

                val repr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), path, nodeType, null, DefaultNodeContentRepresentation(nodeType, mappedProperties, State.NORMAL))

                val createOp = workingAreaPartitionAdapter.createNodeRepresentation(repr)

                val nodeIndexRefs: List<WorkingAreaInverseNodeReferenceRepresentation> = properties.filter { it.value is NodeReference }.map {
                    DefaultWorkingAreaInverseNodeReferenceRepresentation(session.getSessionID(), Path((it.value as NodeReference).path), path, State.NORMAL)
                }
                val nodeIndexFlux = Flux.fromIterable(nodeIndexRefs)
                val nodeIndexResult = workingAreaNodeIndexPartitionAdapter.createInverseNodeReferences(nodeIndexFlux)

                nodeIndexResult.then(createOp)
            }.onErrorMap { error ->
                NodeCreationException(error)
            })

        })

    }

    override fun updateNode(session: Session, path: Path, properties: MutableMap<String, Any>, state: State): Mono<out WorkingAreaNodeRepresentation> {
        val existingWorkingAreaNodeMono = workingAreaPartitionAdapter.nodeRepresentation(session.getSessionID(), path)
        val updatedWorkingAreaNodeMono = existingWorkingAreaNodeMono.map { war ->
            val existingContent = war.workingAreaRepresentation()
            val newContent = DefaultNodeContentRepresentation(existingContent.nodeType(), properties, state)
            DefaultWorkingAreaNodeRepresentation(war.sessionId(), war.path(), war.nodeType(), war.permanentRepresentation(), newContent)
        }.switchIfEmpty(Mono.defer {
            dataPartitionAdapter.nodeRepresentation(session.revision(), path).map { existingNode ->
                DefaultWorkingAreaNodeRepresentation(session.getSessionID(), path, existingNode.nodeType(), existingNode, DefaultNodeContentRepresentation(existingNode.nodeType(), properties, state))
            }.switchIfEmpty(Mono.error(NodeNotFoundException("Node $path not found")))
        })

        val nodeRetrieveMonos = checkNodeReferences(session, properties).onErrorMap { error ->
            NodeModificationException(error)
        }

        return nodeRetrieveMonos.then(Mono.defer {
            // check for presence of all referenced binaries
            checkBinaryReferences(properties)

            // map any inputstream properties to binary references
            val mappedProperties = mapInputStreamsToBinaryReferences(properties)

            updatedWorkingAreaNodeMono.flatMap { repr ->
                repr.workingAreaRepresentation.setProperties(mappedProperties)
                workingAreaPartitionAdapter.updateWorkingAreaRepresentation(repr)
            }
        }.onErrorMap { error ->
            NodeModificationException(error)
        })

    }

    private fun checkNodeReferences(session: Session, properties: MutableMap<String, Any>): Flux<NodeRepresentation> {
        val nodeRefs = properties.filter {
            it.value is NodeReference
        }.map {
            it.value as NodeReference
        }.toList()

        val refFlux = Flux.fromIterable(nodeRefs)

        return refFlux.flatMap { ref ->
            log.debug("Checking referred node ${ref.path}")
            val reprMono: Mono<NodeRepresentation> = nodeRepresentation(session, Path(ref.path)).switchIfEmpty(
                    Mono.error(NodeNotFoundException("Cannot find referenced node ${ref.path}")))
            reprMono
        }
    }

    private fun checkBinaryReferences(properties: MutableMap<String, Any>) {
        properties.filter {
            it.value is BinaryReference
        }.map {
            it.value as BinaryReference
        }.forEach {
            if (!binaryManager.fileExists(it.path)) {
                throw NodeException("Cannot find referenced binary ${it.path}")
            }
        }
    }

    private fun mapInputStreamsToBinaryReferences(properties: MutableMap<String, Any>): MutableMap<String, Any> {
        return properties.mapValues {
            when (it.value) {
                is InputStreamWithLength -> BinaryReference(binaryManager.createFile(it.value as InputStreamWithLength))
                else -> it.value
            }
        }.toMutableMap()
    }

    override fun nodeRepresentation(session: Session, path: Path): Mono<NodeRepresentation> {
        val workingRepMono: Mono<WorkingAreaNodeRepresentation> = workingAreaPartitionAdapter.nodeRepresentation(session.getSessionID(), path)
        return workingRepMono
                .map { it.effectiveNodeRepresentation() }
                .switchIfEmpty(dataPartitionAdapter.nodeRepresentation(session.revision(), path))
                .filter { repr -> repr.state() == State.NORMAL }
    }

    override fun childNodeRepresentations(session: Session, path: Path): Flux<NodeRepresentation> {
        val workingAreaChildRepresentations = workingAreaPartitionAdapter.nodeChildRepresentations(session.getSessionID(), path).map {
            it.effectiveNodeRepresentation()
        }

        val committedRepresentations = dataPartitionAdapter.nodeChildRepresentations(session.revision(), path)

        val workingListMono = workingAreaChildRepresentations.collectList()
        val committedMono = committedRepresentations.collectList()

        /*
         * Collect the committed and working nodes.  Replace committed node representations with their working counterparts.
         * Then sort the result by path and return only those nodes that are not deleted.
         * NOTE: frankly I'd rather do this by processing the original fluxes without collecting them, but I can't
         * yet figure out a reliable way to do that....
         */
        val overridesList: Mono<List<NodeRepresentation>> = Mono.zip(workingListMono, committedMono) { workingList, permList ->
            val workingMap = workingList.map { it.path() to it }.toMap()
            val permMap = permList.map { it.path() to (it as NodeRepresentation) }.toMap().toMutableMap()
            permMap.putAll(workingMap)
            permMap.toSortedMap().map { it.value }
        }

        val listFlux: Flux<NodeRepresentation> = overridesList.flatMapMany {
            Flux.fromIterable(it)
        }.filter { it.state() == State.NORMAL }

        return listFlux
    }

    private fun inverseReferences(session: Session, path: Path): Flux<InverseNodeReferenceRepresentation> {
        val workingAreaRefListMono = workingAreaNodeIndexPartitionAdapter.nodeReferences(session.getSessionID(), path).collectList()
        val committedRefListMono = committedNodeIndexPartitionAdapter.nodeReferences(path, session.revision()).collectList()

        val overridesList: Mono<List<InverseNodeReferenceRepresentation>> = Mono.zip(workingAreaRefListMono, committedRefListMono) { working, committed ->
            val workingMap = working.map { Pair(it.referringNodePath(), it.nodePath()) to it as InverseNodeReferenceRepresentation }.toMap()
            val committedMap = committed.map { Pair(it.referringNodePath(), it.nodePath()) to it as InverseNodeReferenceRepresentation }.toMap().toMutableMap()
            committedMap.putAll(workingMap)
            committedMap.map { it.value }
        }

        return overridesList.flatMapMany {
            Flux.fromIterable(it)
        }.filter { it.state() == State.NORMAL }
    }

    override fun referringNodes(session: Session, path: Path): Flux<NodeRepresentation> {
        val listFlux = inverseReferences(session, path)

        return listFlux.flatMapSequential { inverseNodeReference ->
            nodeRepresentation(session, inverseNodeReference.referringNodePath())
        }
    }

    override fun search(session: Session, searchTerm: String): Flux<NodeRepresentation> {
        return searchAdapter.search(session, AllTextQuery(searchTerm), 0, 100).flatMapSequential { path ->
            nodeRepresentation(session, path)
        }
    }

    override fun search(session: Session, query: Query): Flux<NodeRepresentation> {
        return searchAdapter.search(session, query, 0, 100).flatMapSequential { path ->
            log.debug("Mapping search result path $path to node representation")
            nodeRepresentation(session, path)
        }
    }

    override fun commitSession(session: Session): Mono<Void> {
        log.debug("Committing session $session")
        val journalCreateCommand = context.getBean(JournalEntryCreateCommand::class.java, session)
        val journalCommitCommand = context.getBean(JournalCommitCommand::class.java, session)

        val workingAreaRemoveMono = Mono.defer { workingAreaPartitionAdapter.deleteWorkingAreaRepresentations(session.getSessionID()) }
        val workingAreaIndexMono = Mono.defer { workingAreaNodeIndexPartitionAdapter.deleteNodeReferences(session.getSessionID()) }

        return journalCreateCommand.execute().then(journalCommitCommand.execute()).flatMap { revisionNumber ->
            (session as DefaultSession).setRevision(revisionNumber)
            workingAreaRemoveMono.then(workingAreaIndexMono)
        }
    }

    override fun destroySessionChanges(session: Session): Mono<Void> {
        log.debug("Removing working changes for session ${session.getSessionID()}")
        val workingAreaRemoveMono = Mono.defer { workingAreaPartitionAdapter.deleteWorkingAreaRepresentations(session.getSessionID()) }
        val workingAreaIndexMono = Mono.defer { workingAreaNodeIndexPartitionAdapter.deleteNodeReferences(session.getSessionID()) }
        return workingAreaRemoveMono.then(workingAreaIndexMono)
    }

    override fun deleteNode(session: Session, path: Path): Mono<Void> {
        if (path.isRoot()) {
            throw NodeDeletionException("Cannot delete root node")
        }

        val refs = inverseReferences(session, path)

        val total = refs.count()
        return total.flatMap { count ->
            when (count) {
                0L -> {
                    val nodeToUpdate = nodeRepresentation(session, path)
                    nodeToUpdate.flatMap { repr ->
                        log.debug("Marking node $path as deleted")
                        updateNode(session, path, repr.properties(), State.DELETED).then()
                    }.then(Mono.defer {
                        // after the node is deleted, delete each child
                        val nodeChildren = childNodeRepresentations(session, path)
                        nodeChildren.flatMap { nodeRepr ->
                            deleteNode(session, nodeRepr.path())
                        }.then()
                    })
                }
                else -> Mono.error(NodeDeletionException("Cannot delete node with incoming references"))
            }
        }
    }

    /**
     * FOR TESTING ONLY
     */
    fun reset() {
        val catReset = catalogPartitionAdapter.reset()
        val workReset = workingAreaPartitionAdapter.reset()
        val workIndexReset = workingAreaNodeIndexPartitionAdapter.reset()
        val dataReset = dataPartitionAdapter.reset()
        val committedIndexReset = committedNodeIndexPartitionAdapter.reset()
        catReset.then(workReset).then(workIndexReset).then(dataReset).then(committedIndexReset).block()
    }

}