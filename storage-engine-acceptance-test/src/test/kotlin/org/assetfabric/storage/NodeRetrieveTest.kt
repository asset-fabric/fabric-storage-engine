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
import io.restassured.module.webtestclient.RestAssuredWebTestClient
import io.restassured.response.ResponseBodyExtractionOptions
import org.apache.http.HttpStatus
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.rest.NodeRepresentation
import org.assetfabric.storage.rest.SingleValueNodeProperty
import org.assetfabric.storage.server.Application
import org.assetfabric.storage.server.command.support.MetadataStoreResetCommand
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

@ExtendWith(SpringExtension::class)
@DirtiesContext
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the node controller")
class NodeRetrieveTest {

    private val log = LogManager.getLogger(NodeRetrieveTest::class.java)

    private val sessionUrl = "/v1/session"

    private val nodeUrl = "/v1/node"

    @Value("\${test.user}")
    private lateinit var user: String

    @Value("\${test.password}")
    private lateinit var password: String

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var catalogAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @Autowired
    private lateinit var context: ApplicationContext

    private val loginUtility = LoginUtility()

    @Value("\${local.server.port}")
    private lateinit var port: Integer

    private inline fun <reified T> ResponseBodyExtractionOptions.to(): T {
        return this.`as`(T::class.java)
    }

    private fun getLoginToken(): String {
        val token = loginUtility.getTokenForUser(sessionUrl, user, password)
        Assertions.assertNotNull(token, "null session token")
        log.info("Sending node create request with token $token")
        return token
    }

    @BeforeEach
    private fun init() {
        RestAssuredWebTestClient.webTestClient(webTestClient)

        RestAssured.port = port.toInt()

        val resetCommand = context.getBean(MetadataStoreResetCommand::class.java)
        log.debug("Resetting repository")
        resetCommand.execute().block()
        log.debug("Repository reset complete")
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
        assertEquals("1", (retProps["intProp"] as SingleValueNodeProperty).getValue())
        assertEquals("string", (retProps["stringProp"] as SingleValueNodeProperty).getValue())
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