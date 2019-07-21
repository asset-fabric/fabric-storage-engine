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
import org.assetfabric.storage.rest.NodeRepresentation
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@Api(tags = ["Node Controller"], value = "Node Controller", description = "Provides operations for node creation, retrieval, update and deletion.")
@RequestMapping("/v1/node")
interface NodeController {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createNode(@CookieValue(API_TOKEN) token: String, @RequestParam("path") path: String, @RequestBody request: Flux<Part>): Mono<ResponseEntity<NodeRepresentation>>

    @ApiOperation("Retrieves a node")
    @GetMapping
    fun retrieveNode(@ApiParam("a valid session token") @CookieValue(API_TOKEN) token: String, @RequestParam("path") path: String): Mono<ResponseEntity<NodeRepresentation>>

    @ApiOperation("Retrieves the children of a node")
    @GetMapping("/children", produces = ["application/stream+json"])
    fun retrieveNodeChildren(@ApiParam("a valid session token") @CookieValue(API_TOKEN) token: String, @RequestParam("path") path: String): Mono<ResponseEntity<Flux<NodeRepresentation>>>

    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateNode(@CookieValue(API_TOKEN) token: String, @RequestParam("path") path: String, @RequestBody request: Flux<Part>): Mono<ResponseEntity<NodeRepresentation>>

    @DeleteMapping
    fun deleteNode(@CookieValue(API_TOKEN) token: String, @RequestParam("path") path: String): Mono<ResponseEntity<Void>>

}