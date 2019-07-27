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

package org.assetfabric.storage

import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.support.DefaultNodeContentRepresentation
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.assetfabric.storage.spi.support.DefaultWorkingAreaNodeRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DisplayName("the fabric storage system")
class NodeUpdateTest: AbstractNodeTest() {

    @Configuration
    @ComponentScan("org.assetfabric.storage")
    internal class Config

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @Autowired
    private lateinit var workingAreaPartitionAdapter: WorkingAreaPartitionAdapter

    @Autowired
    private lateinit var metadataManager: DefaultMetadataManagerService

    @BeforeEach
    fun init() {
        metadataManager.reset()
    }

    @Test
    @DisplayName("should be able to update all properties of an existing committed node")
    fun updateNode() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()

        val node = session.node("/child1").block()!!
        val newProps = mutableMapOf<String, Any>(
                "stringProp" to "string"
        )
        node.setProperties(newProps).block()

        val updatedNode = session.node("/child1").block()!!
        assertEquals("string", updatedNode.stringProperty("stringProp"))
    }

    @Test
    @DisplayName("should be able to update a node that exists in the working area")
    fun updateWorkingAreaNode() {
        val session = getSession().block()!!

        val workingProps = mutableMapOf<String, Any>(
                "stringProp" to "oldString"
        )
        val contentRepr = DefaultNodeContentRepresentation(NodeType.UNSTRUCTURED, workingProps, State.NORMAL)
        val workingRepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), Path("/child1"), NodeType.UNSTRUCTURED, null, contentRepr)
        workingAreaPartitionAdapter.createNodeRepresentation(workingRepr).block()


        val node = session.node("/child1").block()!!
        val newProps = mutableMapOf<String, Any>(
                "stringProp" to "string"
        )
        node.setProperties(newProps).block()

        val updatedNode = session.node("/child1").block()!!
        assertEquals("string", updatedNode.stringProperty("stringProp"))
    }

    @Test
    @DisplayName("should be not be able to update a node with an invalid node reference property")
    fun updateNodeWithInvalidNodeReference() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()

        val node = session.node("/child1").block()!!
        val newProps = mutableMapOf(
                "stringProp" to "string",
                "nodeRef" to NodeReference("/not/a/path")
        )
        assertThrows(NodeModificationException::class.java) {
            node.setProperties(newProps).block()
        }

    }

    @Test
    @DisplayName("should not be able to update a node with an invalid binary property")
    fun updateNodeWithInvalidBinary() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()

        val node = session.node("/child1").block()!!
        val newProps = mutableMapOf(
                "stringProp" to "string",
                "binRef" to BinaryReference("not/a/binary")
        )
        assertThrows(NodeModificationException::class.java) {
            node.setProperties(newProps).block()
        }
    }



}