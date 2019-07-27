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

import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.JournalEntryNodeRepresentation
import org.assetfabric.storage.spi.RevisionedNodeRepresentation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface DataPartitionAdapter {

    /**
     * Writes the given node representations into the data partition.
     */
    fun writeNodeRepresentations(representations: Flux<RevisionedNodeRepresentation>): Mono<Void>

    /**
     * Writes the given journal entries into the data partition.
     */
    fun writeJournalEntries(entries: Flux<JournalEntryNodeRepresentation>): Mono<Void>

    /**
     * Returns the node representation for the given path, visible at the given revision.
     */
    fun nodeRepresentation(revision: RevisionNumber, path: Path): Mono<RevisionedNodeRepresentation>

    /**
     * Returns the representations of the nodes that are children of the node at the given path.
     */
    fun nodeChildRepresentations(revision: RevisionNumber, path: Path): Flux<RevisionedNodeRepresentation>

    /**
     * Resets the data partition to its default initial state.
     */
    fun reset(): Mono<Void>

}