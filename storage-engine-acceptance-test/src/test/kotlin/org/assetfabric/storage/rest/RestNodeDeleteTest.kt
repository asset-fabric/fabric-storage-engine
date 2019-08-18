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
import org.assetfabric.storage.rest.property.types.BooleanProperty
import org.assetfabric.storage.server.Application
import org.assetfabric.storage.server.controller.Constants
import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the node controller")
class RestNodeDeleteTest: RestAbstractTest() {

    @Autowired
    private lateinit var manager: DefaultMetadataManagerService

    @BeforeEach
    private fun init() {
        RestAssured.port = port.toInt()
    }

    @Test
    @DisplayName("should be able to delete a deletable node")
    fun testDeleteNode() {
        manager.reset()

        val nodeName = "node1"
        val nodePath = "/$nodeName"
        val token = getLoginToken()
        val contentRepresentation = NodeContentRepresentation()
        contentRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
        contentRepresentation.setProperty("booleanProp", BooleanProperty(true))
        val (_, createResponse) = createNode(token, nodePath, contentRepresentation, hashMapOf())
        assertEquals(200, createResponse.statusCode, "Wrong HTTP status code")

        val deleteRes = RestAssured.given()
                .header("Cookie", "${Constants.API_TOKEN}=$token")
                .queryParam("path", "/node1")
                .delete("/v1/node").andReturn()
        assertEquals(200, deleteRes.statusCode, "Wrong HTTP status code")

        val response = RestAssured.given()
                .header("Cookie", "${Constants.API_TOKEN}=$token")
                .queryParam("path", "/node1")
                .get("/v1/node").andReturn()
        assertEquals(404, response.statusCode, "Wrong HTTP status code")
    }

}