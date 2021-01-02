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
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.command.ReturningCommand
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.JournalEntryNodeRepresentation
import org.assetfabric.storage.spi.metadata.JournalPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaNodeRepresentation
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.metadata.support.DefaultJournalEntryNodeRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * A command to create a journal entry from a set of working changes for a session.
 * Returns true if a journal entry was created, false otherwise.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class JournalEntryCreateCommand(val session: Session): ReturningCommand<Boolean> {

    private val log = LogManager.getLogger(JournalEntryCreateCommand::class.java)

    @Autowired
    private lateinit var catalogPartitionAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var workingAreaPartitionAdapter: WorkingAreaPartitionAdapter

    @Autowired
    private lateinit var journalPartitionAdapter: JournalPartitionAdapter

    override fun execute(): Mono<Boolean> {

        log.debug("Executing journal entry creation")

        val currentRevisionMono = catalogPartitionAdapter.currentRepositoryRevision()

        return currentRevisionMono.flatMap { revisionNumber ->
            val nextRevision = revisionNumber.plus(1)

            log.debug("Creating new journal entries at revision $revisionNumber")

            val nodeRepresentations: Flux<WorkingAreaNodeRepresentation> = workingAreaPartitionAdapter.getWorkingAreaRepresentations(session.getSessionID())

            val journalRepresentations: Flux<JournalEntryNodeRepresentation> = nodeRepresentations.map { nodeRepresentation ->
                DefaultJournalEntryNodeRepresentation(nodeRepresentation.sessionId(), nodeRepresentation.path(), nextRevision, nodeRepresentation.nodeType(), nodeRepresentation.permanentRepresentation(), nodeRepresentation.workingAreaRepresentation())
            }

            journalPartitionAdapter.createJournalEntrySet(journalRepresentations).doAfterSuccessOrError { _, _ ->
                log.debug("Journal entry creation complete")
            }
        }

    }

}