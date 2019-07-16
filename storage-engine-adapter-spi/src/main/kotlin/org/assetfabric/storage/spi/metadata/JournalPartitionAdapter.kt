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

package org.assetfabric.storage.spi.metadata

import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.RevisionedNodeRepresentation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * An adapter that is responsible for managing the storage and manipulation of entries
 * in the metadata journal.
 */
interface JournalPartitionAdapter {

    /**
     * Creates a new journal entry set containing the given nodes.
     * @param nodeRepresentations the node representations to include in the journal entry
     */
    fun createJournalEntrySet(nodeRepresentations: Flux<RevisionedNodeRepresentation>): Mono<Boolean>

    /**
     * Returns the revision number of the next set of journal entries.
     */
    fun getNextJournalRevision(): Mono<RevisionNumber>

    /**
     * Gets the set of node representations for the given revision number.
     */
    fun getJournalEntrySet(revision: RevisionNumber): Flux<RevisionedNodeRepresentation>

    /**
     * Removes the journal entries corresponding to the given revision number.
     */
    fun removeJournalEntrySet(revision: RevisionNumber): Mono<Void>

    /**
     * Resets the journal partition to its default initial state.
     */
    fun reset(): Mono<Void>

}