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

package org.assetfabric.storage.server.runtime.controller.support

import org.assetfabric.storage.Session
import org.assetfabric.storage.server.runtime.service.SessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux

@Component
class SessionExecutor {

    @Autowired
    private lateinit var sessionService: SessionService

    fun <T> monoUsingSession(token: String, function: (Session) -> Mono<T>): Mono<T> {
        val sessionMono = sessionService.getSession(token)
        return sessionMono.flatMap { function.invoke(it) }
    }

    fun <T> fluxUsingSession(token: String, function: (Session) -> Flux<T>): Flux<T> {
        val sessionMono = sessionService.getSession(token)
        return sessionMono.toFlux().flatMap { function.invoke(it) }
    }

}