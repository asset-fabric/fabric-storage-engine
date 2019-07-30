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

package org.assetfabric.storage.server.controller.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.Credentials
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.assetfabric.storage.server.controller.SessionController
import org.assetfabric.storage.server.service.SessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class DefaultSessionController: SessionController {

    private val log = LogManager.getLogger(DefaultSessionController::class.java)

    @Autowired
    private lateinit var sessionService: SessionService

    @Autowired
    private lateinit var sessionExecutor: SessionExecutor

    override fun getSession(@RequestBody credentials: Credentials): Mono<ResponseEntity<Void>> {
        val sessionMono = sessionService.getSession(credentials)
        return sessionMono.map { session ->
            log.debug("Created session ${session.getSessionID()}")
            ResponseEntity.ok().header("Set-Cookie", "$API_TOKEN=${session.getSessionID()}").build<Void>()
        }.switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))

    }

    override fun commitSession(token: String): Mono<ResponseEntity<Void>> {
        return sessionExecutor.monoUsingSession(token) { session ->
            session.commit()
        }.then(Mono.just(ResponseEntity.ok().build()))
    }

    override fun closeSession(@CookieValue(API_TOKEN) token: String): Mono<ResponseEntity<Void>> {
        val sessionMono = sessionService.getSession(token)
        return sessionMono.map { session: Session ->
            session.close()
            log.debug("Closed session for token $token")
            ResponseEntity.ok().header("Set-Cookie", "$API_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT").build<Void>()
        }.switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
    }

}