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

package org.assetfabric.storage.server.runtime.command

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.Path
import org.assetfabric.storage.Session
import org.assetfabric.storage.rest.NodeContentRepresentation
import org.assetfabric.storage.rest.NodeRepresentation
import org.assetfabric.storage.server.command.ReturningCommand
import org.assetfabric.storage.server.command.support.DataBufferCollector
import org.assetfabric.storage.server.runtime.service.NodePropertyRepresentationMappingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

abstract class AbstractRestNodeCommand(val session: Session, val nodePath: Path, val requestParts: Flux<Part>): ReturningCommand<NodeRepresentation> {

    private val log = LogManager.getLogger(AbstractRestNodeCommand::class.java)

    protected val objectMapper = ObjectMapper()

    @Autowired
    protected lateinit var nodeMapper: NodePropertyRepresentationMappingService

    /**
     * Used to collect a flux of upload parts into a single set of upload components.
     */
    class PartComponents {
        var representation: NodeContentRepresentation? = null
        val streamMap: MutableMap<String, InputStreamWithLength> = hashMapOf()
    }

    /**
     * Collects the flux of uploaded parts into a single PartComponents object.
     */
    protected fun extractRequestParts(requestParts: Flux<Part>): Mono<PartComponents> {
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

    protected fun createNodeElements(partComponents: PartComponents): Pair<NodeContentRepresentation, MutableMap<String, Any>> {
        log.debug("Processing extracted request parts")

        val representation: NodeContentRepresentation = partComponents.representation!!
        val props: MutableMap<String, Any> = nodeMapper.getInternalPropertyRepresentation(representation.getProperties(), partComponents.streamMap)

        // TODO shouldn't the representation contain the properties?
        return Pair(representation, props)
    }

}