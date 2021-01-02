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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Interface for low-level working area node index operations.
 */
interface WorkingAreaNodeIndexPartitionAdapter {

    /**
     * Creates the given node references in the node index.
     */
    fun createInverseNodeReferences(refs: Flux<WorkingAreaInverseNodeReferenceRepresentation>): Flux<WorkingAreaInverseNodeReferenceRepresentation>

    /**
     * Returns the current references to the given node within the given session.
     * @param sessionId the ID of the session for which references should be retrieved
     * @param nodePath the path of the node for which references should be returned
     */
    fun nodeReferences(sessionId: String, nodePath: Path): Flux<WorkingAreaInverseNodeReferenceRepresentation>

    /**
     * Returns the current references to nodes at or below the given node path within the given session.
     * @param sessionId the ID of the session for which references should be retrieved
     * @param nodePath the path of the node for which references, and references to its children, should be retrieved
     */
    fun nodeReferencesAtOrBelow(sessionId: String, nodePath: Path): Flux<WorkingAreaInverseNodeReferenceRepresentation>

    /**
     * Deletes the current node references from the node index for the given session.
     * @param sessionId the ID of the session for which the current references should be returned
     */
    fun deleteNodeReferences(sessionId: String): Mono<Void>

    /**
     * Removes all entries from the node index. USE WITH CAUTION!
     */
    fun reset(): Mono<Void>

}