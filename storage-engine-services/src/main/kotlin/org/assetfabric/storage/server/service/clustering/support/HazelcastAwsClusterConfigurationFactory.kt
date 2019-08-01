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
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

/**
 * Creates a Hazelcast cluster configuration using AWS service discovery to connect
 * nodes in the cluster.
 *
 * The 4 key settings here are AWS access key and secret key, in order to provide
 * Hazelcast the ability to find AWS resources, and the cluster tag name/value.  Servers
 * that are tagged with the given tag and value are the ones identified as being candidates
 * to join the same cluster.
 */
@Configuration
@ConditionalOnProperty("assetfabric.storage.cluster.strategy", havingValue = "aws")
class HazelcastAwsClusterConfigurationFactory: ClusterConfigurationFactory {

    /**
     * The AWS secret key to use.
     */
    @Value("\${assetfabric.storage.cluster.secretKey}")
    private lateinit var secretKey: String

    /**
     * The AWS access key to use.
     */
    @Value("\${assetfabric.storage.cluster.accessKey}")
    private lateinit var accessKey: String

    /**
     * The service tag name to use in order to find nodes that should be in the cluster.
     */
    @Value("\${assetfabric.storage.cluster.tagKey}")
    private lateinit var tagKey: String

    /**
     * The service value to use in order to find nodes that should be in the cluster.
     */
    @Value("\${assetfabric.storage.cluster.tagValue}")
    private lateinit var tagValue: String

    override fun getObject(): Config? {
        val cfg = Config()

        val network = cfg.networkConfig
        val join = network.join
        join.multicastConfig.isEnabled = false
        join.awsConfig.isEnabled = true
        join.awsConfig.setProperty("access-key", accessKey)
        join.awsConfig.setProperty("secret-key", secretKey)
        join.awsConfig.setProperty("tag-key", tagKey)
        join.awsConfig.setProperty("tag-value", tagValue)

        return cfg
    }

}