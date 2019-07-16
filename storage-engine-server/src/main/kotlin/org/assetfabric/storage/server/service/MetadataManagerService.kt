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

package org.assetfabric.storage.server.service

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.spi.NodeRepresentation
import org.assetfabric.storage.spi.WorkingAreaNodeRepresentation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Governs the high-level logic surrounding the manipulation of node metadata.  The metadata
 * manager sits atop a variety of back-end adapters that provide access to the various
 * sections of the metadata store, including the catalog, working area, journal, current data partition,
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
     * Returns either a session's working copy of a node representation, or the
     * representation of that node in the current data partition if the session has
     * no working copy of it, if either exist.
     * @param session the session being used to retrieve the node representation
     * @param path the path of the node representation to retrieve
     * @return either the working copy or committed copy of the node, or empty if neither exist
     */
    fun nodeRepresentation(session: Session, path: String): Mono<NodeRepresentation>

    /**
     * Returns the children of the node at the given path, either from the active data partition
     * or from the working area.
     * @param session the session being used to retrieve the child nodes
     * @param path the path of the node whose children should be returned
     */
    fun childNodeRepresentations(session: Session, path: String): Flux<NodeRepresentation>

    /**
     * Creates a new, uncommitted node representation in the metadata store's working set for the given session.
     * @param session the session being used to create the node
     * @param parentPath the path of the parent node of the new node
     * @param name the name of the new node
     * @param properties the properties of the new node
     * @return a representation of the new, uncommitted node
     */
    fun createNodeRepresentationInWorkingArea(session: Session, parentPath: String, name: String, nodeType: NodeType, properties: Map<String, Any>): Mono<out WorkingAreaNodeRepresentation>

    /**
     * Commits the given session, saving its working changes to permanent storage.
     * @param session the session to commit
     */
    fun commitSession(session: Session): Mono<Void>

}