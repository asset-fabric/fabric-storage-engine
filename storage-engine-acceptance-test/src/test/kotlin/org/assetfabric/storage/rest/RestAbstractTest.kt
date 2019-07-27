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
import io.restassured.response.Response
import io.restassured.response.ResponseBodyExtractionOptions
import io.restassured.specification.RequestSpecification
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.server.controller.Constants
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.InputStream

@ExtendWith(SpringExtension::class)
@DirtiesContext
abstract class RestAbstractTest {

    private val log = LogManager.getLogger(RestAbstractTest::class.java)

    private val sessionUrl = "/v1/session"

    private val nodeUrl = "/v1/node"

    @Value("\${local.server.port}")
    private lateinit var port: Integer

    @Value("\${test.user}")
    private lateinit var user: String

    @Value("\${test.password}")
    private lateinit var password: String

    private val loginUtility = RestLoginUtility()

    protected inline fun <reified T> ResponseBodyExtractionOptions.to(): T {
        return this.`as`(T::class.java)
    }

    @BeforeEach
    private fun init() {
        RestAssured.port = port.toInt()
    }

    protected fun getLoginToken(): String {
        val token = loginUtility.getTokenForUser(sessionUrl, user, password)
        Assertions.assertNotNull(token, "null session token")
        log.info("Sending node create request with token $token")
        return token
    }

    protected fun createNode(token: String, nodePath: String, nodeContent: NodeContentRepresentation, files: Map<String, InputStream>): Pair<NodeContentRepresentation, Response> {
        val spec = buildNodeRequest(token, nodeContent, files)
        val response = spec.log().all().`when`()
                .post("$nodeUrl?path=$nodePath")
                .andReturn()
        return Pair(nodeContent, response)
    }

    protected fun updateNode(token: String, nodePath: String, nodeContent: NodeContentRepresentation, files: Map<String, InputStream>): Pair<NodeContentRepresentation, Response> {
        val spec = buildNodeRequest(token, nodeContent, files)
        val response = spec.log().all().`when`()
                .put("$nodeUrl?path=$nodePath")
                .andReturn()
        return Pair(nodeContent, response)
    }

    private fun buildNodeRequest(token: String, nodeContent: NodeContentRepresentation, files: Map<String, InputStream>): RequestSpecification {
        var spec = RestAssured.given()
                .header("Cookie", "${Constants.API_TOKEN}=$token")
        files.forEach {
            val entry = it
            spec = spec.multiPart(entry.key, entry.key, entry.value)
        }
        spec = spec.multiPart("nodeContent", nodeContent)

        return spec
    }

}