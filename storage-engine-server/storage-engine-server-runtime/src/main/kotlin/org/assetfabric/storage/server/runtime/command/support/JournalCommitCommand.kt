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

package org.assetfabric.storage.server.runtime.command.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.NodeReference
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.State
import org.assetfabric.storage.server.command.ReturningCommand
import org.assetfabric.storage.server.runtime.service.SessionService
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.CommittedInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.metadata.CommittedNodeIndexPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.JournalEntryNodeRepresentation
import org.assetfabric.storage.spi.metadata.JournalPartitionAdapter
import org.assetfabric.storage.spi.metadata.support.DefaultCommittedInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.search.SearchAdapter
import org.assetfabric.storage.spi.search.SearchEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Writes the next journal entries to the committed data partition, removes the journal entries,
 * and updates the repository revision.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class JournalCommitCommand(val session: Session): ReturningCommand<RevisionNumber> {

    private val log = LogManager.getLogger(JournalCommitCommand::class.java)

    @Autowired
    private lateinit var journalPartitionAdapter: JournalPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @Autowired
    private lateinit var nodeIndexPartitionAdapter: CommittedNodeIndexPartitionAdapter

    @Autowired
    private lateinit var catalogPartitionAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var searchAdapter: SearchAdapter

    @Autowired
    private lateinit var sessionService: SessionService

    override fun execute(): Mono<RevisionNumber> {
        val nextJournalRevisionMono = journalPartitionAdapter.getNextJournalRevision()
        return nextJournalRevisionMono.flatMap { revision ->

            log.info("Committing journal entries at revision $revision")

            // TODO: is there a more efficient way to play the journal representations to all 3 subscribers below?
            val nextEntries: Flux<JournalEntryNodeRepresentation> = journalPartitionAdapter.getJournalEntrySet(revision).cache(Duration.ofMinutes(3))

            // we need to write the committed nodes, write the node index entries, and add the nodes to the search index,
            // so create 3 copies of the flux
            val indexEntryFlux = Flux.from(nextEntries)
            val writeJournalFlux = Flux.from(nextEntries)
            val searchIndexFlux = Flux.from(nextEntries).map { createSearchEntry(it) }

            val writeEntries = dataPartitionAdapter.writeJournalEntries(writeJournalFlux)
            val writeIndexes = nodeIndexPartitionAdapter.createInverseNodeReferences(createNodeIndexEntries(revision, indexEntryFlux)).then()
            val addToSearchIndex = Mono.defer { searchAdapter.addSearchEntries(searchIndexFlux) }
            val deleteEntries = Mono.defer { journalPartitionAdapter.removeJournalEntrySet(revision) }
            val updateRevision = Mono.defer { catalogPartitionAdapter.setRepositoryRevision(revision) }
            val updateSession = Mono.defer { sessionService.updateSession(session.getSessionID(), revision) }
            writeEntries.then(writeIndexes).then(deleteEntries).then(addToSearchIndex).then(updateRevision).then(updateSession).thenReturn(revision)
        }
    }

    private fun createNodeIndexEntries(revisionNumber: RevisionNumber, journalEntryFlux: Flux<JournalEntryNodeRepresentation>): Flux<CommittedInverseNodeReferenceRepresentation> {
        return journalEntryFlux.flatMap { journalEntry ->
            val addedNodeRefs: Flux<CommittedInverseNodeReferenceRepresentation> = Flux.fromIterable(journalEntry.addedProperties().entries)
                    .filter { it.value is NodeReference }
                    .map { it.value as NodeReference}
                    .map { nodeRef ->
                        log.debug("Adding node index to ${nodeRef.path} from ${journalEntry.path()} at revision $revisionNumber")
                        DefaultCommittedInverseNodeReferenceRepresentation(revisionNumber, Path(nodeRef.path), journalEntry.path(), State.NORMAL)
                    }

            addedNodeRefs
        }
    }

    private fun createSearchEntry(journalEntry: JournalEntryNodeRepresentation): SearchEntry {
        log.debug("Creating search entry for ${journalEntry.path()}")
        val path = journalEntry.path()
        val revision = journalEntry.revision()
        val type = journalEntry.nodeType()
        val currentProps = mutableMapOf<String, Any>()
        val priorProps = mutableMapOf<String, Any>()
        currentProps.putAll(journalEntry.addedProperties())
        priorProps.putAll(journalEntry.removedProperties())
        journalEntry.changedProperties().forEach { entry ->
            val (oldVal, newVal) = entry.value
            currentProps[entry.key] = newVal
            currentProps[entry.key] = oldVal
        }
        return SearchEntry(path, type, revision, State.NORMAL, currentProps, priorProps)
    }

}