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

package org.assetfabric.storage

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * A connection to the metadata and binary stores; similar to a JDBC Connection object.
 */
interface Session {

    /**
     * Returns the ID of this session.
     */
    fun getSessionID(): String

    /**
     * Returns the ID of the user that owns this session.
     */
    fun getUserID(): String

    /**
     * Returns the root node.
     */
    fun rootNode(): Mono<Node>

    /**
     * Returns the revision number of the session
     */
    fun revision(): RevisionNumber

    /**
     * Returns the node at the given path, if it's available.
     * @param path the path of the node to retrieve
     * @return an optional containing the given node, or empty if the node doesn't exist
     */
    fun node(path: String): Mono<Node>

    /**
     * Returns the nodes that match the given search term.
     */
    fun search(term: String): Flux<Node>

    /**
     * Commits this session, saving working changes to permanent storage.
     */
    fun commit(): Mono<Void>

    /**
     * Closes this session, discarding any unsaved changes.
     */
    fun close()

}