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

package org.assetfabric.storage.server.runtime.command.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.Node
import org.assetfabric.storage.NodeNotFoundException
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.Session
import org.assetfabric.storage.rest.NodeRepresentation
import org.assetfabric.storage.server.runtime.command.AbstractRestNodeCommand
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.core.publisher.switchIfEmpty

/**
 * A command that accepts a flux of HTTP Part objects, converts them into a representation suitable for
 * the creation of a node in the respository, then translates that new node into a format suitable
 * for response to a calling HTTP client.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class RestNodeCreateCommand(session: Session, nodePath: Path, requestParts: Flux<Part>): AbstractRestNodeCommand(session, nodePath, requestParts) {

    private val log = LogManager.getLogger(RestNodeCreateCommand::class.java)

    override fun execute(): Mono<NodeRepresentation> {
        log.debug("Executing REST node create command")

        // figure out the parent nodeRepresentation from the path
        val nodeName = nodePath.nodeName()
        val parentNodePath = nodePath.parentPath()

        val nodeMono = session.node(nodePath.toString())
        val parentNodeMono = nodeMono.handle {  _, sink: SynchronousSink<Node> ->
            sink.error(RuntimeException("Cannot create existing node $nodePath"))
        }.switchIfEmpty(Mono.defer {
            session.node(parentNodePath.toString())
        })

        return parentNodeMono.flatMap { parentNode ->

            val requestPartsMono = extractRequestParts(requestParts)

            requestPartsMono.flatMap { partComponents ->

                val (representation, props) = createNodeElements(partComponents)

                log.debug("Creating new node")
                parentNode.createChild(nodeName, NodeType(representation.getNodeType()), props).map { newNode ->
                    log.debug("Mapping new node into external representation")
                    val retNode = NodeRepresentation()
                    retNode.setName(nodeName)
                    retNode.setPath(nodePath.toString())
                    retNode.setNodeType(representation.getNodeType())
                    retNode.setProperties(nodeMapper.getExternalPropertyRepresentation(newNode.properties()))
                    retNode
                }

            }
        }.switchIfEmpty {
            throw NodeNotFoundException("Parent node $parentNodePath not found")
        }
    }

}