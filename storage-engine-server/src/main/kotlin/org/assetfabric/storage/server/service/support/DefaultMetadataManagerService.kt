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
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.command.support.JournalCommitCommand
import org.assetfabric.storage.server.command.support.JournalEntryCreateCommand
import org.assetfabric.storage.server.command.support.MetadataStoreInitializationCommand
import org.assetfabric.storage.server.service.MetadataManagerService
import org.assetfabric.storage.spi.NodeRepresentation
import org.assetfabric.storage.spi.WorkingAreaNodeRepresentation
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.support.DefaultNodeContentRepresentation
import org.assetfabric.storage.spi.support.DefaultWorkingAreaNodeRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.annotation.PostConstruct

@Service
class DefaultMetadataManagerService: MetadataManagerService {

    private val log = LogManager.getLogger(DefaultMetadataManagerService::class.java)

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var catalogPartitionAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var workingAreaPartitionAdapter: WorkingAreaPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

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

    override fun createNodeRepresentationInWorkingArea(session: Session, parentPath: String, name: String, nodeType: NodeType, properties: Map<String, Any>): Mono<out WorkingAreaNodeRepresentation> {
        val actualParentPath = when(parentPath) {
            "/" -> ""
            else -> parentPath
        }
        val path = "$actualParentPath/$name"

        val existingNodeMono = dataPartitionAdapter.nodeRepresentation(session.revision(), path)
        val representationMono: Mono<DefaultWorkingAreaNodeRepresentation> = existingNodeMono.map { existingNode ->
            DefaultWorkingAreaNodeRepresentation(session.getSessionID(), name, Path(path), session.revision(), nodeType, existingNode, DefaultNodeContentRepresentation(properties) )
        }.defaultIfEmpty(
            DefaultWorkingAreaNodeRepresentation(session.getSessionID(), name, Path(path), session.revision(), nodeType, null, DefaultNodeContentRepresentation(properties) )
        )

        return representationMono.flatMap { repr ->
            log.debug("Filing working area representation $repr")
            workingAreaPartitionAdapter.createNodeRepresentation(repr)
        }
    }

    override fun nodeRepresentation(session: Session, path: String): Mono<NodeRepresentation> {
        val workingRepMono: Mono<WorkingAreaNodeRepresentation> = workingAreaPartitionAdapter.nodeRepresentation(session.getSessionID(), path)
        return workingRepMono
                .map { it.effectiveNodeRepresentation() }
                .switchIfEmpty(
                        dataPartitionAdapter.nodeRepresentation(session.revision(), path)
                                .map { it.getNodeRepresentation() }
                )
    }

    override fun childNodeRepresentations(session: Session, path: String): Flux<NodeRepresentation> {
        val workingAreaChildRepresentations = workingAreaPartitionAdapter.nodeChildRepresentations(session.getSessionID(), path).map {
            it.effectiveNodeRepresentation()
        }

        val committedRepresentations = dataPartitionAdapter.nodeChildRepresentations(session.revision(), path)
                .map { it.getNodeRepresentation() }

        return workingAreaChildRepresentations.mergeOrderedWith(committedRepresentations, Comparator<NodeRepresentation> { n1, n2 ->
            n1.path.compareTo(n2.path)
        }).distinct()
    }

    override fun commitSession(session: Session): Mono<Void> {
        log.debug("Committing session $session")
        val journalCreateCommand = context.getBean(JournalEntryCreateCommand::class.java, session)
        val journalCommitCommand = context.getBean(JournalCommitCommand::class.java, session)
        return journalCreateCommand.execute().then(journalCommitCommand.execute()).then()
    }

}