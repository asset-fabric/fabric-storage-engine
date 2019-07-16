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

package org.assetfabric.storage.spi.metadata.mongo

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.FactoryBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Component
@ConditionalOnProperty("assetfabric.storage.metadata.adapter.mongo", havingValue = "embedded")
class EmbeddedMongoClientFactory : FactoryBean<MongoClient> {

    private val log = LogManager.getLogger(EmbeddedMongoClientFactory::class.java)

    private val host = "localhost"

    private val port = 27017

    private var mongodExecutable: MongodExecutable? = null

    @PostConstruct
    private fun start() {
        if (mongodExecutable == null) {
            val mongodConfig = MongodConfigBuilder().version(Version.Main.PRODUCTION)
                    .net(Net(host, port, Network.localhostIsIPv6()))
                    .build()

            val starter = MongodStarter.getDefaultInstance()
            mongodExecutable = starter.prepare(mongodConfig)
            mongodExecutable!!.start()
        }
    }

    @PreDestroy
    private fun stop() {
        if (mongodExecutable != null) {
            log.info("Stopping embedded Mongo")
            mongodExecutable!!.stop()
            mongodExecutable = null
        }
    }

    override fun getObject(): MongoClient? {
        return MongoClients.create("mongodb://$host:$port")
    }

    override fun getObjectType(): Class<*>? {
        return MongoClient::class.java
    }

}
