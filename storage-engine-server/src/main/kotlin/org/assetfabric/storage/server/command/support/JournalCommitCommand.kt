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

package org.assetfabric.storage.server.command.support

import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.command.ReturningCommand
import org.assetfabric.storage.server.service.SessionService
import org.assetfabric.storage.spi.RevisionedNodeRepresentation
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.JournalPartitionAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Writes the next journal entries to the active data partition, removes the journal entries,
 * and updates the repository revision.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class JournalCommitCommand(val session: Session): ReturningCommand<RevisionNumber> {

    @Autowired
    private lateinit var journalPartitionAdapter: JournalPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @Autowired
    private lateinit var catalogPartitionAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var sessionService: SessionService

    override fun execute(): Mono<RevisionNumber> {
        val nextJournalRevisionMono = journalPartitionAdapter.getNextJournalRevision()
        return nextJournalRevisionMono.flatMap { revision ->
            val nextEntries: Flux<RevisionedNodeRepresentation> = journalPartitionAdapter.getJournalEntrySet(revision)
            val writeEntries = dataPartitionAdapter.writeNodeRepresentations(nextEntries)
            val deleteEntries = Mono.defer { journalPartitionAdapter.removeJournalEntrySet(revision) }
            val updateRevision = Mono.defer { catalogPartitionAdapter.setRepositoryRevision(revision) }
            val updateSession = Mono.defer { sessionService.updateSession(session.getSessionID(), revision) }
            writeEntries.then(deleteEntries).then(updateRevision).then(updateSession).thenReturn(revision)
        }
    }

}