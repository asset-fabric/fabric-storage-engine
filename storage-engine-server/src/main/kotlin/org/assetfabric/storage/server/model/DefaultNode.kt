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

package org.assetfabric.storage.server.model

import org.assetfabric.storage.Node
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.command.support.NodeCreateCommand
import org.assetfabric.storage.server.service.MetadataManagerService
import org.assetfabric.storage.spi.NodeRepresentation
import org.assetfabric.storage.spi.RevisionedNodeRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DefaultNode(val session: Session, val nodeRepresentation: NodeRepresentation): Node {

    @Autowired
    private lateinit var metadataManagerService: MetadataManagerService

    @Autowired
    private lateinit var context: ApplicationContext

    override fun name(): String = nodeRepresentation.name

    override fun path(): Path = nodeRepresentation.path

    override fun nodeType(): NodeType = nodeRepresentation.nodeType

    override fun revision(): RevisionNumber? {
        return when(nodeRepresentation) {
            is RevisionedNodeRepresentation -> (nodeRepresentation as RevisionedNodeRepresentation).revision
            else -> null
        }
    }

    override fun properties(): Map<String, Any> = nodeRepresentation.properties

    override fun createChild(name: String, nodeType: NodeType, properties: MutableMap<String, Any>): Mono<Node> {
        val command = context.getBean(NodeCreateCommand::class.java, session, path().toString(), name, nodeType, properties)
        return command.execute()
    }

    override fun children(): Flux<Node> {
        val childrenFlux = metadataManagerService.childNodeRepresentations(session, path().toString())
        return childrenFlux.map { context.getBean(DefaultNode::class.java, session, it) }
    }

    override fun toString(): String {
        return "DefaultNode(nodeRepresentation=$nodeRepresentation)"
    }

}