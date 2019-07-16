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

import io.restassured.RestAssured
import io.restassured.response.Response
import org.assetfabric.storage.rest.NodeContentRepresentation
import org.assetfabric.storage.rest.NodePropertyType
import org.assetfabric.storage.server.Application
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.InputStream

@ExtendWith(SpringExtension::class)
@DirtiesContext
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the session controller")
class SessionTest: AbstractTest() {

    private val sessionUrl = "/v1/session"

    private val nodeUrl = "/v1/node"

    private fun createNode(token: String, nodePath: String, nodeContent: NodeContentRepresentation, files: Map<String, InputStream>): Pair<NodeContentRepresentation, Response> {
        var spec = RestAssured.given()
                .header("Cookie", "$API_TOKEN=$token")
        files.forEach {
            val entry = it
            spec = spec.multiPart(entry.key, entry.key, entry.value)
        }
        spec = spec.multiPart("nodeContent", nodeContent)

        val response = spec.log().all().`when`()
                .post("$nodeUrl?path=$nodePath")
                .andReturn()
        return Pair(nodeContent, response)
    }

    @Test
    @DisplayName("should commit working area changes when the session is committed")
    fun testJournalEntryCreation() {
        val token = getLoginToken()

        for (num in 0..40) {
            val nodeName = "node$num"
            val nodePath = "/$nodeName"

            val contentRepresentation = NodeContentRepresentation()
            contentRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
            contentRepresentation.setProperty("booleanProp", NodePropertyType.BOOLEAN, "true")
            val (_, response) = createNode(token, nodePath, contentRepresentation, hashMapOf())
            assertEquals(200, response.statusCode, "Wrong HTTP status code")
        }

        val spec = RestAssured.given()
                .header("Cookie", "$API_TOKEN=$token")
                .`when`().post("/v1/session/commit")
        val commitResponse = spec.andReturn()
        assertEquals(HttpStatus.OK.value(), commitResponse.statusCode)

    }

}