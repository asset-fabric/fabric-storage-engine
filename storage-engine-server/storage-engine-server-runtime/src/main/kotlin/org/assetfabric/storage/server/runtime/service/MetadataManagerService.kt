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

package org.assetfabric.storage.server.runtime.service

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.Query
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.NodeRepresentation
import org.assetfabric.storage.spi.metadata.WorkingAreaNodeRepresentation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Governs the high-level logic surrounding the manipulation of node metadata.  The metadata
 * manager sits atop a variety of back-end adapters that provide access to the various
 * sections of the metadata store, including the catalog, working area, journal, committed data partition,
 * and archived data partitions.
 *
 * The metadata manager service is not concerned with working with [Node]s.  It is only concerned
 * with providing access to [NodeRepresentation] objects.  It is not concerned with any type
 * of in-memory caching, either.
 */
interface MetadataManagerService {

    /**
     * Initializes the metadata store if it is not already initialized.
     */
    fun initializeMetadataStore(): Mono<Void>

    /**
     * Returns the current revision of the repository.
     */
    fun repositoryRevision(): Mono<RevisionNumber>

    /**
     * Creates a new, uncommitted node representation in the metadata store's working set for the given session.
     * @param session the session being used to create the node
     * @param parentPath the path of the parent node of the new node
     * @param name the name of the new node
     * @param properties the properties of the new node
     * @return a representation of the new, uncommitted node
     */
    fun createNode(session: Session, parentPath: Path, name: String, nodeType: NodeType, properties: MutableMap<String, Any>): Mono<out WorkingAreaNodeRepresentation>

    /**
     * Returns either a session's working copy of a node representation, or the
     * representation of that node in the current data partition if the session has
     * no working copy of it, if either exist.
     * @param session the session being used to retrieve the node representation
     * @param path the path of the node representation to retrieve
     * @return either the working copy or committed copy of the node, or empty if neither exist
     */
    fun nodeRepresentation(session: Session, path: Path): Mono<NodeRepresentation>

    /**
     * TODO: maybe kill this method in favor of search(query)
     * Returns the node representations visible to the given session that match
     * the given search term.
     */
    fun search(session: Session, searchTerm: String): Flux<NodeRepresentation>

    /**
     * Returns the node representations visible to the given session that match
     * this given query.
     */
    fun search(session: Session, query: Query): Flux<NodeRepresentation>

    /**
     * Returns the children of the node at the given path, either from the active data partition
     * or from the working area.
     * @param session the session being used to retrieve the child nodes
     * @param path the path of the node whose children should be returned
     */
    fun childNodeRepresentations(session: Session, path: Path): Flux<NodeRepresentation>

    /**
     * Returns the descendants of the node at the given path, either from the active data partition
     * or from the working area.
     * @param session the session being used to retrieve the child nodes
     * @param path the path of the node whose descendants should be returned
     */
    fun descendantNodeRepresentations(session: Session, path: Path): Flux<NodeRepresentation>

    /**
     * Returns the node representations in the working set and committed set that contain [NodeReference]s to the given node.
     */
    fun referringNodes(session: Session, path: Path): Flux<NodeRepresentation>

    /**
     * Updates the node at the given path with the specified properties.
     * @param session the session being used to update the node
     * @param path the path of the node
     * @param properties the new properties of the node
     * @param state the new state of the node
     * @return the updated working representation of the node
     *
     */
    fun updateNode(session: Session, path: Path, properties: MutableMap<String, Any>, state: State): Mono<out WorkingAreaNodeRepresentation>

    /**
     * Deletes the node at the given path using the given session.
     */
    fun deleteNode(session: Session, path: Path): Mono<Void>

    /**
     * Removes any working changes stored for the given session.
     */
    fun destroySessionChanges(session: Session): Mono<Void>

    /**
     * Commits the given session, saving its working changes to permanent storage.
     * @param session the session to commit
     */
    fun commitSession(session: Session): Mono<Void>

}