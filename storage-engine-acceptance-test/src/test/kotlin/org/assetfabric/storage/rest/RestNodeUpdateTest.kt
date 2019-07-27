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
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.server.Application
import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the node controller")
class RestNodeUpdateTest: RestAbstractTest() {

    @Autowired
    private lateinit var metadataManager: DefaultMetadataManagerService

    @BeforeEach
    private fun init() {
        metadataManager.reset()
        RestAssured.port = port.toInt()
    }

    @Test
    @DisplayName("should be able to update an existing node")
    fun testUpdateNode() {
        val nodeName = "node1"
        val nodePath = "/$nodeName"
        val token = getLoginToken()
        val contentRepresentation = NodeContentRepresentation()
        contentRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
        contentRepresentation.setProperty("booleanProp", NodePropertyType.BOOLEAN, "true")
        val (_, createResponse) = createNode(token, nodePath, contentRepresentation, hashMapOf())
        assertEquals(200, createResponse.statusCode, "Wrong HTTP status code")

        contentRepresentation.removeProperty("booleanProp")
        contentRepresentation.setProperty("stringProp", NodePropertyType.STRING,"hi")
        val (updated, updateResponse) = updateNode(token, nodePath, contentRepresentation, hashMapOf())
        assertEquals(200, updateResponse.statusCode, "Wrong HTTP status code")
        assertEquals(SingleValueNodeProperty(NodePropertyType.STRING, "hi"), updated.getProperties()["stringProp"], "String property mismatch")
        assertNull(updated.getProperties()["booleanProp"], "Found boolean property")
    }

    @Test
    @DisplayName("should return a 404 when updating a nonexistent node")
    fun testUpdateMissingNode() {
        val nodePath = "/nodex"
        val token = getLoginToken()
        val contentRepresentation = NodeContentRepresentation()
        contentRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
        val (_, createResponse) = updateNode(token, nodePath, contentRepresentation, hashMapOf())
        assertEquals(404, createResponse.statusCode, "Wrong HTTP status code")
    }

    @Test
    @DisplayName("should return a 409 when updating a existing node with a different node type")
    fun testChangeNodeType() {
        val nodeName = "node1"
        val nodePath = "/$nodeName"
        val token = getLoginToken()
        val contentRepresentation = NodeContentRepresentation()
        contentRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
        contentRepresentation.setProperty("booleanProp", NodePropertyType.BOOLEAN, "true")
        val (_, createResponse) = createNode(token, nodePath, contentRepresentation, hashMapOf())
        assertEquals(200, createResponse.statusCode, "Wrong HTTP status code")

        contentRepresentation.setNodeType("af:othertype:1")
        val (_, updateResponse) = updateNode(token, nodePath, contentRepresentation, hashMapOf())
        assertEquals(409, updateResponse.statusCode, "Wrong HTTP status code")
    }



}