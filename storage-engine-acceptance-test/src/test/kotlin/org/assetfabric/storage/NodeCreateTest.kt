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
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream
import java.util.Date

@SpringBootTest
@DisplayName("the fabric storage system")
class NodeCreateTest: AbstractNodeTest() {

    @Configuration
    @ComponentScan("org.assetfabric.storage")
    internal class Config

    @Autowired
    private lateinit var metadataManager: DefaultMetadataManagerService

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @BeforeEach
    private fun init() {
        metadataManager.reset()
    }

    @Test
    @DisplayName("should be able to create a node that doesn't exist in committed storage or in the session's working area")
    fun createNode() {
        val session = getSession().block()!!
        val propertyMap = mutableMapOf(
                "stringProp" to "string",
                "stringListProp" to TypedList(ListType.STRING, listOf("a", "b")),
                "intProp" to 3,
                "intListProp" to TypedList(ListType.INTEGER, listOf(1, 2, 3)),
                "doubleProp" to 3.0,
                "doubleListProp" to TypedList(ListType.DOUBLE, listOf(1.0, 4.56)),
                "booleanProp" to true,
                "booleanListProp" to TypedList(ListType.BOOLEAN, listOf(true, false)),
                "longProp" to 4L,
                "longListProp" to TypedList(ListType.LONG, listOf(2, 3, 4)),
                "dateProp" to Date(),
                "dateListProp" to TypedList(ListType.DATE, listOf(Date(), Date())),
                "nodeRefProp" to NodeReference("/"),
                "nodeRefListProp" to TypedList(ListType.NODE, listOf(NodeReference("/"))),
                "binProp" to InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3),
                "paramNodeRefProp" to ParameterizedNodeReference("/",
                        mapOf(
                                "string" to "string",
                                "int" to 3,
                                "bool" to false,
                                "dateProp" to Date(),
                                "dateListProp" to TypedList(ListType.DATE, listOf(Date(), Date()))
                        // TODO: add all valid types
                        )
                )
         )
        val newNode = session.rootNode().flatMap { node ->
            node.createChild("child1", NodeType.UNSTRUCTURED, propertyMap)
        }.block()!!
        assertEquals("child1", newNode.name())
        assertEquals(Path("/child1"), newNode.path())
        assertEquals(NodeType.UNSTRUCTURED, newNode.nodeType())
        val paramRefPair = newNode.parameterizedNodeReferenceProperty("paramNodeRefProp").block()!!
        assertEquals(Path("/"), paramRefPair.first.path())
    }

    @Test
    @DisplayName("should be able to create a node that doesn't exist in committed storage but which does exist in a different session's working area")
    fun createNodeThatExistsInAnotherSession() {
        val session = getSession().block()!!
        session.rootNode().flatMap { node ->
            node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf())
        }.block()!!

        val session2 = getSession().block()!!
        session2.rootNode().flatMap { node ->
            node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf())
        }.block()!!
    }

    @Test
    @DisplayName("should not be able to create a node that already exists in committed storage")
    fun createExistingCommittedNode() {
        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()

        val session = getSession().block()!!
        assertThrows(NodeCreationException::class.java) {
            session.rootNode().flatMap { node ->
                node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf())
            }.block()!!
        }
    }

    @Test
    @DisplayName("should not be able to create a node that already exists in the same session")
    fun createExistingNode() {
        val session = getSession().block()!!
        session.rootNode().flatMap { node ->
            node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf())
        }.block()!!

        assertThrows(NodeCreationException::class.java) {
            session.rootNode().flatMap { node ->
                node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf())
            }.block()!!
        }
    }

    @Test
    @DisplayName("should be able to create a node as a child of a node that exists in committed storage")
    fun createNonexistentChildNode() {
        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()

        val session = getSession().block()!!
        session.rootNode().flatMap { node ->
            node.createChild("/child1/child2", NodeType.UNSTRUCTURED, mutableMapOf())
        }.block()!!
    }

    @Test
    @DisplayName("should not be able to create a node with an invalid node reference")
    fun createNodeWithInvalidNodeReference() {
        val session = getSession().block()!!

        val props = mutableMapOf<String, Any>("badNodeRef" to NodeReference("/no/such/node"))
        assertThrows(NodeCreationException::class.java) {
            session.rootNode().flatMap { node ->
                node.createChild("child1", NodeType.UNSTRUCTURED, props)
            }.block()!!
        }
    }

    @Test
    @DisplayName("should not be able to create a node with an invalid binary reference")
    fun createNodeWithInvalidBinaryReference() {
        val session = getSession().block()!!

        val props = mutableMapOf<String, Any>("badBinRef" to BinaryReference("no/such/bin/ref"))
        assertThrows(NodeCreationException::class.java) {
            session.rootNode().flatMap { node ->
                node.createChild("child1", NodeType.UNSTRUCTURED, props)
            }.block()!!
        }
    }



}