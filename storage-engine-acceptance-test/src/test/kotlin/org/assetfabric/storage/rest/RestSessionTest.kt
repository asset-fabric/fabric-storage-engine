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
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the session controller")
class RestSessionTest: RestAbstractTest() {

    @Autowired
    private lateinit var metadataManager: DefaultMetadataManagerService

    @Test
    @DisplayName("should commit working area changes when the session is committed")
    fun testJournalEntryCreation() {
        metadataManager.reset()

        val token = getLoginToken()

        for (num in 0..40) {
            val nodeName = "node$num"
            val nodePath = "/$nodeName"

            val contentRepresentation = NodeContentRepresentation()
            contentRepresentation.setNodeType(NodeType.UNSTRUCTURED.toString())
            contentRepresentation.setProperty("booleanProp", NodePropertyType.BOOLEAN, "true")
            contentRepresentation.setProperty("nodeRef", NodePropertyType.NODE, "/")
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