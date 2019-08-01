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

package org.assetfabric.storage.server.service.clustering.support

import com.hazelcast.config.Config
import com.hazelcast.core.EntryEvent
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap
import com.hazelcast.map.listener.EntryAddedListener
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.service.clustering.ClusterSessionInfo
import org.assetfabric.storage.server.service.clustering.ClusterSynchronizationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID
import javax.annotation.PostConstruct

@Service
class HazelcastClusterSynchronizationService: ClusterSynchronizationService {

    private val log = LogManager.getLogger(HazelcastClusterSynchronizationService::class.java)

    private val SESSION_MAP_NAME = "sessionMap"

    @Autowired
    private lateinit var config: Config

    private lateinit var hazelcastInstance: HazelcastInstance

    @PostConstruct
    private fun init() {
        config.instanceName = "Storage engine ${UUID.randomUUID()}"
        config.groupConfig.name = "StorageEngine"
        hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config)

        val sMap: IMap<String, Session> = hazelcastInstance.getMap(SESSION_MAP_NAME)
        sMap.addEntryListener(object: EntryAddedListener<String, ClusterSessionInfo> {
            override fun entryAdded(entry: EntryEvent<String, ClusterSessionInfo>?) {
                println("Added session ${entry!!.value} with key ${entry.key}")
            }
        }, true)

    }

    override fun addSessionInfo(key: String, info: ClusterSessionInfo) {
        val sessionMap = hazelcastInstance.getMap<String, ClusterSessionInfo>(SESSION_MAP_NAME)
        sessionMap[key] = info
        log.debug("Added session $key at revision ${info.getRevision()}")
    }

    override fun updateSessionInfo(key: String, info: ClusterSessionInfo) {
        val sessionMap = hazelcastInstance.getMap<String, ClusterSessionInfo>(SESSION_MAP_NAME)
        sessionMap[key] = info
        log.debug("Updated session $key to revision ${info.getRevision()}")
    }

    override fun getSessionInfo(key: String): Mono<ClusterSessionInfo> {
        val session = hazelcastInstance.getMap<String, ClusterSessionInfo>(SESSION_MAP_NAME).get(key)
        return Mono.justOrEmpty(session)
    }

    override fun removeSessionInfo(key: String) {
        hazelcastInstance.getMap<String, ClusterSessionInfo>(SESSION_MAP_NAME).remove(key)
        log.debug("Removed session $key")
    }

    override fun executeWithGlobalLock(lockName: String, process: () -> Unit) {
        val lock = hazelcastInstance.getLock(lockName)
        when (lock.tryLock()) {
            true -> {
                try {
                    process()
                } finally {
                    lock.unlock()
                }
            }
            else -> log.debug("Ignoring execution; lock $lockName is already in use.")
        }
    }
}