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
import org.assetfabric.storage.server.Application
import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.ByteArrayInputStream

@ExtendWith(SpringExtension::class)
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
        contentRepresentation.setProperty("booleanProp", NodePropertyType.BOOLEAN, "true")
        contentRepresentation.setProperty("intProp", NodePropertyType.INTEGER, "3")
        contentRepresentation.setProperty("stringProp", NodePropertyType.STRING, "hello world")
        contentRepresentation.setProperty("dateProp", NodePropertyType.DATE, "2012-01-03T00:00:00Z")
        contentRepresentation.setProperty("longProp", NodePropertyType.LONG, "45")
        contentRepresentation.setProperty("booleanListProp", NodePropertyType.BOOLEAN, listOf("true", "false"))
        contentRepresentation.setProperty("intListProp", NodePropertyType.INTEGER, listOf("1", "2"))
        contentRepresentation.setProperty("stringListProp", NodePropertyType.STRING, listOf("A", "B"))
        contentRepresentation.setProperty("longListProp", NodePropertyType.LONG, listOf("1", "2"))
        contentRepresentation.setProperty("dateListProp", NodePropertyType.DATE, listOf("2012-01-03T00:00:00Z", "2012-01-03T00:00:00Z"))
        contentRepresentation.setProperty("nodeRef", NodePropertyType.NODE, "/")
        contentRepresentation.setProperty("nodeRefList", NodePropertyType.NODE, listOf("/", "/"))

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
        val nodeProp = SingleValueNodeProperty()
        nodeProp.setValue("testfile")
        nodeProp.setType(NodePropertyType.BINARY_INPUT)
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