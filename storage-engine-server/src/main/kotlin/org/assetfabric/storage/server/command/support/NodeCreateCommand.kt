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

package org.assetfabric.storage.server.command.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.BinaryReference
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.Node
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.command.ReturningCommand
import org.assetfabric.storage.server.model.DefaultNode
import org.assetfabric.storage.server.service.BinaryManagerService
import org.assetfabric.storage.server.service.MetadataManagerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class NodeCreateCommand(val session: Session, val parentPath: String, val name: String, val nodeType: NodeType, val properties: MutableMap<String, Any>): ReturningCommand<Node> {

    private val log = LogManager.getLogger(NodeCreateCommand::class.java)

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var metadataManagerService: MetadataManagerService

    override fun execute(): Mono<Node> {
        val actualParentPath = when(parentPath) {
            "/" -> "/"
            else -> "$parentPath/"
        }
        val nodePath = "$actualParentPath$name"
        val existingNodeMono = metadataManagerService.nodeRepresentation(session, nodePath)

        return existingNodeMono.handle { _, sink: SynchronousSink<Node> ->
            sink.error(RuntimeException("Cannot create existing node $nodePath"))
        }.switchIfEmpty(Mono.defer {
            log.debug("Creating working area node")
            val reprMono = metadataManagerService.createNode(session, parentPath, name, nodeType, properties)
            reprMono.map { repr ->
                log.debug("Returning mapped node")
                context.getBean(DefaultNode::class.java, session, repr.effectiveNodeRepresentation())
            }
        })
    }

}