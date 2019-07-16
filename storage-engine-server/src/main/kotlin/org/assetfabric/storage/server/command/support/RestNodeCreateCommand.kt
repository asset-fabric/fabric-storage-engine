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

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.Node
import org.assetfabric.storage.NodeNotFoundException
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.Session
import org.assetfabric.storage.rest.NodeContentRepresentation
import org.assetfabric.storage.rest.NodeProperty
import org.assetfabric.storage.rest.NodePropertyType
import org.assetfabric.storage.rest.NodeRepresentation
import org.assetfabric.storage.rest.SingleValueNodeProperty
import org.assetfabric.storage.server.command.ReturningCommand
import org.assetfabric.storage.server.service.NodePropertyRepresentationMappingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.core.publisher.switchIfEmpty
import java.util.stream.Collectors

/**
 * A command that accepts a flux of HTTP Part objects, converts them into a representation suitable for
 * the creation of a node in the respository, then translates that new node into a format suitable
 * for response to a calling HTTP client.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class RestNodeCreateCommand(val session: Session, val nodePath: Path, val requestParts: Flux<Part>): ReturningCommand<NodeRepresentation> {

    private val log = LogManager.getLogger(RestNodeCreateCommand::class.java)

    private val objectMapper = ObjectMapper()

    @Autowired
    private lateinit var nodeMapper: NodePropertyRepresentationMappingService

    /**
     * Used to collect a flux of upload parts into a single set of upload components.
     */
    class PartComponents {
        var representation: NodeContentRepresentation? = null
        val streamMap: MutableMap<String, InputStreamWithLength> = hashMapOf()
    }

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

                log.debug("Processing extracted request parts")

                val representation: NodeContentRepresentation = partComponents.representation!!

                // split the incoming properties into the ones that are binary inputs and the ones that aren't
                val propsMap: Map<Boolean, List<Map.Entry<String, NodeProperty>>> = representation.getProperties().entries.stream().collect(Collectors.groupingBy { it.value.getType() == NodePropertyType.BINARY_INPUT })

                // for the non-binary properties, map them to their internal representation
                val nonBinaryProperties: Map<String, NodeProperty> = propsMap[false].orEmpty().associateBy({it.key}, {it.value})
                val props: MutableMap<String, Any> = nodeMapper.getInternalPropertyRepresentation(nonBinaryProperties)

                val binaryProperties = propsMap[true].orEmpty()
                binaryProperties.forEach { (_, nodeProperty) ->
                    if (nodeProperty is SingleValueNodeProperty) {
                        val streamName = nodeProperty.getValue()
                        val stream: InputStreamWithLength = partComponents.streamMap[streamName]!!
                        props[streamName] = stream
                    } else {
                        throw RuntimeException("Binary properties must be single-valued")
                    }
                }

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

    /**
     * Collects the flux of uploaded parts into a single PartComponents object.
     */
    private fun extractRequestParts(requestParts: Flux<Part>): Mono<PartComponents> {
        log.debug("Extracting request parts")

        return requestParts.reduce(PartComponents()) { components: PartComponents, part: Part ->
            if (part is FilePart) {
                log.info("Got file part {}", part.name())
                val buffers: Flux<DataBuffer> = part.content()
                val streamMono: Mono<InputStreamWithLength> = buffers.collect(DataBufferCollector())
                streamMono.subscribe {
                    stream -> components.streamMap[part.name()] = stream
                }
            } else {
                log.info("Got part {}", part)
                val dataFlux = part.content()
                val objectMono: Mono<InputStreamWithLength> = dataFlux.collect(DataBufferCollector())

                objectMono.subscribe { stream ->
                    val nodeContent = objectMapper.readValue(stream.inputStream, NodeContentRepresentation::class.java)
                    components.representation = nodeContent
                    log.debug("Representation extraction complete: $nodeContent")
                }
            }
            components
        }
    }

}