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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

/**
 * Creates a Hazelcast cluster configuration using multicast networking to connect nodes
 * in the cluster.
 */
@Configuration
@ConditionalOnProperty("assetfabric.storage.service.cluster.strategy", havingValue = "multicast")
class HazelcastMulticastClusterConfigurationFactory: ClusterConfigurationFactory {

    override fun getObject(): Config? {
        val cfg = Config()
        val network = cfg.networkConfig
        network.interfaces.isEnabled = true
        network.interfaces.interfaces = listOf("192.168.1.*")
        val join = network.join
        join.multicastConfig.isEnabled = true
        return cfg
    }

}