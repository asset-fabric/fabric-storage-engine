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
import org.assetfabric.storage.NodeModificationException
import org.assetfabric.storage.NodeNotFoundException
import org.assetfabric.storage.Path
import org.assetfabric.storage.Session
import org.assetfabric.storage.State
import org.assetfabric.storage.rest.NodeRepresentation
import org.assetfabric.storage.server.runtime.command.AbstractRestNodeCommand
import org.assetfabric.storage.server.runtime.service.MetadataManagerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty

/**
 * A command that accepts a flux of HTTP Part objects, converts them into a representation suitable for
 * the updating of a node in the repository, then translates that updated node into a format suitable
 * for response to a calling HTTP client.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class RestNodeUpdateCommand(session: Session, nodePath: Path, requestParts: Flux<Part>): AbstractRestNodeCommand(session, nodePath, requestParts) {

    private val log = LogManager.getLogger(RestNodeUpdateCommand::class.java)

    @Autowired
    private lateinit var metadataManagerService: MetadataManagerService

    override fun execute(): Mono<NodeRepresentation> {
        log.debug("Executing REST node update command")

        // figure out the parent nodeRepresentation from the path
        val nodeName = nodePath.nodeName()

        val nodeMono = session.node(nodePath.toString())
        return nodeMono.flatMap { existingNode ->

            val requestPartsMono = extractRequestParts(requestParts)

            requestPartsMono.flatMap { partComponents ->

                val (representation, props) = createNodeElements(partComponents)

                if (representation.getNodeType() != existingNode.nodeType().toString()) {
                    throw NodeModificationException("Cannot change node type ${existingNode.nodeType()} to new type ${representation.getNodeType()} for node $nodePath")
                }

                log.debug("Updating node")

                metadataManagerService.updateNode(session, nodePath, props, State.NORMAL).map {
                    val nodeRepresentation = it.effectiveNodeRepresentation()

                    val retNode = NodeRepresentation()
                    retNode.setName(nodeName)
                    retNode.setPath(nodePath.path)
                    retNode.setNodeType(nodeRepresentation.nodeType().toString())
                    retNode.setProperties(nodeMapper.getExternalPropertyRepresentation(nodeRepresentation.properties()))
                    retNode
                }

            }
        }.switchIfEmpty {
            throw NodeNotFoundException("Node $nodePath not found")
        }
    }
}