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
import org.assetfabric.storage.NodeReference
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.ParameterizedNodeReference
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.State
import org.assetfabric.storage.server.runtime.service.MetadataManagerService
import org.assetfabric.storage.spi.metadata.NodeRepresentation
import org.assetfabric.storage.spi.metadata.RevisionedNodeRepresentation
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

    override fun name(): String = nodeRepresentation.name()

    override fun path(): Path = nodeRepresentation.path()

    override fun nodeType(): NodeType = nodeRepresentation.nodeType()

    override fun state(): State = nodeRepresentation.state()

    override fun revision(): RevisionNumber? {
        return when(nodeRepresentation) {
            is RevisionedNodeRepresentation -> (nodeRepresentation as RevisionedNodeRepresentation).revision()
            else -> null
        }
    }

    override fun properties(): Map<String, Any> = nodeRepresentation.properties()

    override fun setProperties(properties: MutableMap<String, Any>): Mono<Void> {
        nodeRepresentation.setProperties(properties)
        return metadataManagerService.updateNode(session, path(), properties, state()).then()
    }

    override fun createChild(name: String, nodeType: NodeType, properties: MutableMap<String, Any>): Mono<Node> {
        return metadataManagerService.createNode(session, path(), name, nodeType, properties).map {
            context.getBean(DefaultNode::class.java, session, it.effectiveNodeRepresentation())
        }
    }

    override fun child(name: String): Mono<Node> {
        val childPath = path().childPath(name)
        return metadataManagerService.nodeRepresentation(session, childPath).map {
            context.getBean(DefaultNode::class.java, session, it)
        }
    }

    override fun children(): Flux<Node> {
        val childrenFlux = metadataManagerService.childNodeRepresentations(session, path())
        return childrenFlux.map { context.getBean(DefaultNode::class.java, session, it) }
    }

    override fun descendants(): Flux<Node> {
        val descFlux = metadataManagerService.descendantNodeRepresentations(session, path())
        return descFlux.map { context.getBean(DefaultNode::class.java, session, it) }
    }

    override fun stringProperty(name: String): String? {
        return nodeRepresentation.properties()[name] as String?
    }

    override fun nodeProperty(name: String): Mono<Node> {
        val nodeProp = nodeRepresentation.properties()[name] as NodeReference?
        return when(nodeProp) {
            null -> Mono.empty()
            else -> session.node(nodeProp.path)
        }
    }

    override fun parameterizedNodeReferenceProperty(name: String): Mono<Pair<Node, Map<String, Any>>> {
        val nodeProp = nodeRepresentation.properties()[name] as ParameterizedNodeReference?
        return when(nodeProp) {
            null -> Mono.empty()
            else -> session.node(nodeProp.path).map {
                Pair(it, nodeProp.properties)
            }
        }
    }

    override fun referringNodes(): Flux<Node> {
        return metadataManagerService.referringNodes(session, path()).map {
            context.getBean(DefaultNode::class.java, session, it)
        }
    }

    override fun delete(): Mono<Void> {
        return metadataManagerService.deleteNode(session, path())
    }

    override fun toString(): String {
        return "DefaultNode(nodeRepresentation=$nodeRepresentation)"
    }

}