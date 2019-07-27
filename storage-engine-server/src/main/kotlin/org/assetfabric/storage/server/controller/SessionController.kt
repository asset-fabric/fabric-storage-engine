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

package org.assetfabric.storage.server.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.ResponseHeader
import org.assetfabric.storage.Credentials
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@Api(tags = ["Session Controller"], value = "Session Controller", description = "Provides operations for obtaining and releasing storage server sessions. ")
@RequestMapping("/v1/session")
interface SessionController {

    /**
     * Returns a valid session token if the given credentials are authentic.
     */
    @ApiOperation("Creates a session for a valid user",  responseHeaders = [ResponseHeader(name = "Set-Cookie", description = "a session token in the form $API_TOKEN=<token>; must be used in all subsequent requests")])
    @ApiResponses(value = [
        ApiResponse(code = 401, message = "Invalid credentials")
    ])
    @PostMapping
    fun getSession(@RequestBody credentials: Credentials): Mono<ResponseEntity<Void>>

    /**
     * Commits a session, saving its changes to permanent storage.
     */
    @ApiOperation("Commits a session")
    @PostMapping("/commit")
    fun commitSession(@CookieValue(API_TOKEN) token: String): Mono<ResponseEntity<Void>>

    /**
     * Closes an active session.
     */
    @ApiOperation("Closes a session")
    @DeleteMapping
    fun closeSession(@ApiParam("the token of the session to close") @CookieValue(API_TOKEN) token: String): Mono<ResponseEntity<Void>>

}