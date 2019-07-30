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

package org.assetfabric.storage.spi.search

import org.assetfabric.storage.Path
import org.assetfabric.storage.Session
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Adapter interface for implementations that support Asset Fabric searches.
 */
interface SearchAdapter {

    /**
     * Adds a search entry for a committed node.
     * @param entry the entry to add to the search index
     */
    fun addSearchEntry(entry: SearchEntry): Mono<Void>

    /**
     * Adds search entries for several committed nodes.
     */
    fun addSearchEntries(entries: Flux<SearchEntry>): Mono<Void>

    /**
     * Adds a search entry for a working area node.
     * @param path the path of the node
     * @param sessionId the id of the session that owns the working area
     * @param properties the properties of the node
     */
    fun addWorkingAreaSearchEntry(path: Path, sessionId: String, properties: Map<String, Any>): Mono<Void>

    /**
     * Removes working area search entries.
     * @param sessionId the ID of the session for which the working area entries should be removed
     */
    fun removeWorkingAreaSearchEntries(sessionId: String): Mono<Void>

    /**
     * Returns the paths of the nodes that match a given query at a given revision.
     * @param session the session for which the query is being executed
     * @param query the search query to run
     * @param start the index of the first node in all matching results to return
     * @param count the number of matching nodes to return, starting with the given start index
     */
    fun search(session: Session, query: Query, start: Int, count: Int): Flux<Path>

}