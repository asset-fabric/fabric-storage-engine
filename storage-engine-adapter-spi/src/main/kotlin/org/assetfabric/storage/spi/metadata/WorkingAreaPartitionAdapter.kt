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
import org.assetfabric.storage.spi.WorkingAreaNodeRepresentation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Interface for low-level working area operations.
 */
interface WorkingAreaPartitionAdapter {

    /**
     * Creates a node representation in the working area for a session.
     * @param representation the node representation to save
     */
    fun createNodeRepresentation(representation: WorkingAreaNodeRepresentation): Mono<WorkingAreaNodeRepresentation>

    /**
     * Returns the working node revision for the given session and path.
     * @param sessionId the session ID for which the representation should be retrieved
     * @param path the path of the node representation to retrieve
     * @return the working copy of the node representation, or empty if no representation exists
     */
    fun nodeRepresentation(sessionId: String, path: Path): Mono<WorkingAreaNodeRepresentation>

    /**
     * Returns the working nodes of the immediate child nodes for the given session and path.
     * @param sessionId the session ID for which the representations should be retrieved
     * @param parentPath the path of the node whose children should be returned
     */
    fun nodeChildRepresentations(sessionId: String, parentPath: Path): Flux<WorkingAreaNodeRepresentation>

    /**
     * Returns the working nodes of the descendant child nodes, at any level, for the given session and path.
     * @param sessionId the session ID for which the representations should be retrieved
     * @param parentPath the path of the node whose children should be returned
     */
    fun nodeDescendantRepresentations(sessionId: String, parentPath: Path): Flux<WorkingAreaNodeRepresentation>

    /**
     * Returns a stream of working area node representations for the given session.
     * @param sessionId the ID of the session for which the working area nodes should be retrieved
     */
    fun getWorkingAreaRepresentations(sessionId: String): Flux<WorkingAreaNodeRepresentation>

    /**
     * Updates the given working area representation with the supplied representation.
     * @param representation the working area representation to update
     */
    fun updateWorkingAreaRepresentation(representation: WorkingAreaNodeRepresentation): Mono<WorkingAreaNodeRepresentation>

    /**
     * Deletes the working area nodes for a session.
     * @param sessionId the session ID of the nodes to remove
     */
    fun deleteWorkingAreaRepresentations(sessionId: String): Mono<Void>

    /**
     * Resets the working area to its default initial state.
     */
    fun reset(): Mono<Void>

}