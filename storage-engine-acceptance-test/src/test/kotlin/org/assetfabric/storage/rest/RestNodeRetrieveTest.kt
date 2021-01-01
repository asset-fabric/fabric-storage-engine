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
import io.restassured.module.webtestclient.RestAssuredWebTestClient
import org.apache.http.HttpStatus
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.State
import org.assetfabric.storage.rest.property.AbstractSimpleScalarNodeProperty
import org.assetfabric.storage.server.Application
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.support.DefaultRevisionedNodeRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the node controller")
class RestNodeRetrieveTest: RestAbstractTest() {

    private val log = LogManager.getLogger(RestNodeRetrieveTest::class.java)

    private val nodeUrl = "/v1/node"

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var catalogAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @Autowired
    private lateinit var metadataManager: DefaultMetadataManagerService

    @BeforeEach
    private fun init() {
        RestAssuredWebTestClient.webTestClient(webTestClient)
        RestAssured.port = port.toInt()
        metadataManager.reset()
    }

    @Test
    @DisplayName("should be able to retrieve an existing committed node with all of its properties")
    fun retrieveNode() {
        val properties = mutableMapOf<String, Any>("intProp" to 1, "stringProp" to "string")
        val node = DefaultRevisionedNodeRepresentation(Path("/node1"), RevisionNumber(1), NodeType.UNSTRUCTURED, properties, State.NORMAL)

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(node)).block()
        catalogAdapter.setRepositoryRevision(RevisionNumber(1)).block()

        val token = getLoginToken()
        val response = RestAssured.given()
                .header("Cookie", "$API_TOKEN=$token")
                .queryParam("path", "/node1")
                .get(nodeUrl).andReturn()
        assertEquals(HttpStatus.SC_OK, response.statusCode, "HTTP status mismatch")
        val retNode = response.body.to<NodeRepresentation>()
        assertEquals("node1", retNode.getName())
        assertEquals("/node1", retNode.getPath())
        val retProps = retNode.getProperties()
        assertEquals("1", (retProps["intProp"] as AbstractSimpleScalarNodeProperty).getValue())
        assertEquals("string", (retProps["stringProp"] as AbstractSimpleScalarNodeProperty).getValue())
    }

    @Test
    @DisplayName("should return a 404 when retrieving a node that doesn't exist")
    fun retrieveMissingNode() {
        val token = getLoginToken()
        val response = RestAssured.given()
                .header("Cookie", "$API_TOKEN=$token")
                .queryParam("path", "/notanode")
                .get(nodeUrl).andReturn()
        assertEquals(HttpStatus.SC_NOT_FOUND, response.statusCode, "HTTP status mismatch")
    }

    @Test
    @DisplayName("should be able to return the children of a parent node")
    fun retrieveChildNodes() {
        val parent = DefaultRevisionedNodeRepresentation(Path("/node1"), RevisionNumber(1), NodeType.UNSTRUCTURED, hashMapOf(), State.NORMAL)
        val child1 = DefaultRevisionedNodeRepresentation(Path("/node1/node2"), RevisionNumber(1), NodeType.UNSTRUCTURED, hashMapOf(), State.NORMAL)
        val child2 = DefaultRevisionedNodeRepresentation(Path("/node1/node3"), RevisionNumber(1), NodeType.UNSTRUCTURED, hashMapOf(), State.NORMAL)

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(parent, child1, child2)).block()
        catalogAdapter.setRepositoryRevision(RevisionNumber(1)).block()

        val token = getLoginToken()
        val retResult = webTestClient
                .get()
                .uri("$nodeUrl/children?path=/node1")
                .header("Cookie", "$API_TOKEN=$token")
                .exchange()
                .expectStatus().isOk
                .expectBodyList(NodeRepresentation::class.java)
                .returnResult()
        val nodeList: List<NodeRepresentation> = retResult.responseBody!!
        assertEquals(2, nodeList.size)
    }

}