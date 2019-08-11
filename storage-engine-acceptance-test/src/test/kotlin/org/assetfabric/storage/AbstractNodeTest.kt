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

import org.assetfabric.storage.server.service.SessionService
import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono

@ExtendWith(SpringExtension::class, MongoSetupExtension::class)
abstract class AbstractNodeTest {

    @Value("\${test.user}")
    protected lateinit var user: String

    @Value("\${test.password}")
    protected lateinit var password: String

    @Autowired
    protected lateinit var sessionService: SessionService

    @Autowired
    private lateinit var metadataManagerService: DefaultMetadataManagerService

    protected fun getSession(): Mono<Session> = sessionService.getSession(Credentials(user, password))

    @BeforeEach
    private fun reset() {
        metadataManagerService.reset()
    }


}