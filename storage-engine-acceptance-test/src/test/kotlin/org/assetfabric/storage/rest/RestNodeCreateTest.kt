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

package org.assetfabric.storage.rest

import io.restassured.RestAssured
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.rest.property.types.BinaryInputProperty
import org.assetfabric.storage.rest.property.types.BooleanListProperty
import org.assetfabric.storage.rest.property.types.BooleanProperty
import org.assetfabric.storage.rest.property.types.DateListProperty
import org.assetfabric.storage.rest.property.types.DateProperty
import org.assetfabric.storage.rest.property.types.DoubleListProperty
import org.assetfabric.storage.rest.property.types.DoubleProperty
import org.assetfabric.storage.rest.property.types.IntegerListProperty
import org.assetfabric.storage.rest.property.types.IntegerProperty
import org.assetfabric.storage.rest.property.types.LongListProperty
import org.assetfabric.storage.rest.property.types.LongProperty
import org.assetfabric.storage.rest.property.types.NodeReferenceListProperty
import org.assetfabric.storage.rest.property.types.NodeReferenceProperty
import org.assetfabric.storage.rest.property.types.StringListProperty
import org.assetfabric.storage.rest.property.types.StringProperty
import org.assetfabric.storage.server.runtime.Application
import org.assetfabric.storage.server.runtime.service.support.DefaultMetadataManagerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayInputStream

@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the node controller")
class RestNodeCreateTest: RestAbstractTest() {

    private val log = LogManager.getLogger(RestNodeCreateTest::class.java)

    @Autowired
    private lateinit var metadataManager: DefaultMetadataManagerService

    @BeforeEach
    private fun init() {
        RestAssured.port = port.toInt()
        metadataManager.reset()
    }

    @Test
    @DisplayName("should be able to create a new node with all non-binary properties")
    fun createNewNode() {
        val nodeName = "node1"
        val nodePath = "/$nodeName"

        val token = getLoginToken()

        val contentRepresentation = NodeContentRepresentation()
        contentRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
        contentRepresentation.setProperty("booleanProp", BooleanProperty(true))
        contentRepresentation.setProperty("intProp", IntegerProperty(3))
        contentRepresentation.setProperty("stringProp", StringProperty("hello world"))
        contentRepresentation.setProperty("dateProp", DateProperty("2012-01-03T00:00:00Z"))
        contentRepresentation.setProperty("longProp", LongProperty(45))
        contentRepresentation.setProperty("booleanListProp", BooleanListProperty(true, false))
        contentRepresentation.setProperty("intListProp", IntegerListProperty(1, 2))
        contentRepresentation.setProperty("stringListProp", StringListProperty("A", "B"))
        contentRepresentation.setProperty("longListProp", LongListProperty(45, 92))
        contentRepresentation.setProperty("dateListProp", DateListProperty("2012-01-03T00:00:00Z", "2012-01-03T00:00:00Z"))
        contentRepresentation.setProperty("nodeRef", NodeReferenceProperty("/"))
        contentRepresentation.setProperty("nodeRefList", NodeReferenceListProperty("/", "/a"))
        contentRepresentation.setProperty("doubleProp", DoubleProperty(3.0))
        contentRepresentation.setProperty("doubleListProp", DoubleListProperty(3.2, 2.6))

        val (node, response) = createNode(token, nodePath, contentRepresentation, hashMapOf())

        assertEquals(200, response.statusCode, "Wrong HTTP status code")
        val retNodeRepresentation: NodeRepresentation = response.body.to()
        assertNotNull(retNodeRepresentation, "No node returned")
        assertEquals(nodeName, retNodeRepresentation.getName(), "name mismatch")
        assertEquals(nodePath, retNodeRepresentation.getPath(), "path mismatch")

        val testProp = node.getProperties()
        val compProp = node.getProperties()
        testProp.forEach { (name, propertyValue) ->
            val compValue = compProp[name]
            assertNotNull(compValue, "Missing property $name")
            assertEquals(propertyValue, compValue, "Mismatch for property $name")
        }
    }

    @Test
    @DisplayName("should return a 409 when asked to create a node that already exists")
    fun createExistingNode() {
        val nodeName = "node2"
        val nodePath = "/$nodeName"

        val token = getLoginToken()

        // create the node
        val repr = NodeContentRepresentation()
        repr.setNodeType(NodeType.UNSTRUCTURED.toString())
        val (_, res1) = createNode(token, nodePath, repr, hashMapOf())
        assertEquals(200, res1.statusCode, "Wrong HTTP status code")

        log.info("Creating product again")

        // try to create it again
        val repr2 = NodeContentRepresentation()
        repr2.setNodeType(NodeType.UNSTRUCTURED.toString())
        val (_, response) = createNode(token, nodePath, repr2, hashMapOf())

        assertEquals(409, response.statusCode, "Wrong HTTP status code")
    }

    @Test
    @DisplayName("should return a 403 Forbidden when asked to create a node whose parent doesn't exist")
    fun createNodeForNonexistentParent() {
        val nodeName = "node3"
        val nodePath = "/noparent/$nodeName"

        val token = getLoginToken()
        val repr = NodeContentRepresentation()
        repr.setNodeType(NodeType.UNSTRUCTURED.toString())

        val (_, response) = createNode(token, nodePath, repr, hashMapOf())

        assertEquals(403, response.statusCode, "Wrong HTTP status code")
    }

    @Test
    @DisplayName("should be able to create a node with a binary property")
    fun createNodeWithBinaryProperty() {
        val nodeName = "node4"
        val nodePath = "/$nodeName"

        val nodeRepresentation = NodeContentRepresentation()
        nodeRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
        val nodeProp = BinaryInputProperty("testfile")
        nodeRepresentation.setProperties(hashMapOf(
                "binary" to nodeProp
        ))

        val token = getLoginToken()

        val bytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5)
        val bais = ByteArrayInputStream(bytes)
        val fileMap = hashMapOf("testfile" to bais)

        val (_, response) = createNode(token, nodePath, nodeRepresentation, fileMap)
        assertEquals(200, response.statusCode, "Wrong HTTP status code")
        val retNodeRepresentation: NodeRepresentation = response.body.to()
        assertNotNull(retNodeRepresentation, "No node returned")
        assertEquals(nodeName, retNodeRepresentation.getName(), "name mismatch")
        assertEquals(nodePath, retNodeRepresentation.getPath(), "path mismatch")
    }

}