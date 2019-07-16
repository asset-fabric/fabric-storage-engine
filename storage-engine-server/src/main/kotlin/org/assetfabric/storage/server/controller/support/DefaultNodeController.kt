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
import org.apache.logging.log4j.Logger
import org.assetfabric.storage.Node
import org.assetfabric.storage.NodeNotFoundException
import org.assetfabric.storage.Path
import org.assetfabric.storage.rest.NodeRepresentation
import org.assetfabric.storage.server.command.support.RestNodeCreateCommand
import org.assetfabric.storage.server.controller.NodeController
import org.assetfabric.storage.server.service.NodePropertyRepresentationMappingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class DefaultNodeController: NodeController {

    private val log: Logger = LogManager.getLogger(DefaultNodeController::class.java)

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var sessionExecutor: SessionExecutor

    @Autowired
    private lateinit var nodeMapper: NodePropertyRepresentationMappingService

    override fun createNode(token: String, @RequestParam("path") path: String, @RequestBody request: Flux<Part>): Mono<ResponseEntity<NodeRepresentation>> {
        return sessionExecutor.executeWithSession(token) { session ->

            val nodePath = Path(when (path.startsWith("/")) {
                true -> path
                false -> "/$path"
            })
            log.debug("Creating node at path $nodePath")

            val createCommand = context.getBean(RestNodeCreateCommand::class.java, session, nodePath, request)
            createCommand.execute().map { repr ->
                ResponseEntity.ok(repr)
            }.onErrorResume { error ->
                log.error("Error creating node", error)
                when(error) {
                    is NodeNotFoundException -> Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build<NodeRepresentation>())
                    is RuntimeException -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build<NodeRepresentation>())
                    else -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<NodeRepresentation>())
                }
            }
        }
    }

    override fun retrieveNode(token: String, @RequestParam("path") path: String): Mono<ResponseEntity<NodeRepresentation>> {
        val nodePath = when (path.startsWith("/")) {
            true -> path
            false -> "/$path"
        }

        val nodeMono: Mono<Node> = sessionExecutor.executeWithSession(token) { session -> session.node(nodePath) }
        return nodeMono.map { node ->
            val retNode = NodeRepresentation()
            retNode.setNodeType(node.nodeType().toString())
            retNode.setName(node.name())
            retNode.setPath(node.path().toString())
            retNode.setProperties(nodeMapper.getExternalPropertyRepresentation(node.properties()))
            ResponseEntity.status(HttpStatus.OK).body(retNode)
        }.switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
    }

    override fun retrieveNodeChildren(token: String, path: String): Mono<ResponseEntity<Flux<NodeRepresentation>>> {
        val nodePath = when (path.startsWith("/")) {
            true -> path
            false -> "/$path"
        }

        val nodeMono: Mono<Node> = sessionExecutor.executeWithSession(token) { session -> session.node(nodePath) }
        return nodeMono.flatMap { node ->
            val representationFlux: Flux<NodeRepresentation> = node.children().map { child ->
                val retNode = NodeRepresentation()
                retNode.setNodeType(child.nodeType().toString())
                retNode.setName(child.name())
                retNode.setPath(child.path().toString())
                retNode.setProperties(nodeMapper.getExternalPropertyRepresentation(child.properties()))
                retNode
            }
            Mono.just(ResponseEntity.status(HttpStatus.OK).body(representationFlux))
        }.switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
    }
}