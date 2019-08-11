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

import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MongoSetupExtension: BeforeAllCallback {

    companion object MongoSetupExtension {

        private val log = LogManager.getLogger(MongoSetupExtension::class.java)

        private var started = false
        private lateinit var mongodExecutable: MongodExecutable

        private val ip = "localhost"
        private val port = 27017

        fun startUp() {
            if (!started) {
                log.debug("$started: Starting Mongo")
                started = true
                val mongodConfig = MongodConfigBuilder().version(Version.Main.PRODUCTION)
                        .net(Net(ip, port, Network.localhostIsIPv6()))
                        .build()

                val starter = MongodStarter.getDefaultInstance()
                mongodExecutable = starter.prepare(mongodConfig)
                mongodExecutable.start()
            } else {
                log.debug("Mongo already running")
            }
        }

        fun shutDown() {
            log.debug("Stopping Mongo")
            mongodExecutable.stop()
            started = false
        }

    }

    override fun beforeAll(context: ExtensionContext?) {
        startUp()
    }

}