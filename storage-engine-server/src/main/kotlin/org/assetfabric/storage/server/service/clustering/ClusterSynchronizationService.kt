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

package org.assetfabric.storage.server.service.clustering

import reactor.core.publisher.Mono

/**
 * Cluster-wide coordination operations.
 */
interface ClusterSynchronizationService {

    /**
     * Adds information about a session to the cluster.
     * @param key the key of the session to be added
     * @param info information about the session
     */
    fun addSessionInfo(key: String, info: ClusterSessionInfo)

    /**
     * Updates the information about the given session.
     * @paramk key the key of the session to be updated
     * @param info the new session info to place into the cluster
     */
    fun updateSessionInfo(key: String, info: ClusterSessionInfo)

    /**
     * Retrieves information about a session from the cluster.
     * @param key the key of the session for which information should be retrieved
     * @return information about the session if it exists, or an empty optional if it doesn't
     */
    fun getSessionInfo(key: String): Mono<ClusterSessionInfo>

    /**
     * Removes the session information associated with the given key.
     * @param key the session key to remove
     */
    fun removeSessionInfo(key: String)

    /**
     * Attempts to acquire a global lock and execute a process.
     * @param lockName the name of the global lock to acquire
     * @param process the process to run if the lock is acquired
     */
    fun executeWithGlobalLock(lockName: String, process: () -> Unit)

}