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
import io.restassured.http.ContentType
import org.assetfabric.storage.Credentials
import org.assetfabric.storage.server.Application
import org.assetfabric.storage.server.controller.Constants.API_TOKEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("the session controller")
class RestLoginLogoutTest: RestAbstractTest() {

    private val sessionUrl = "/v1/session"

    @BeforeEach
    private fun init() {
        RestAssured.port = port.toInt()
    }

    @Test
    @DisplayName("should return a session token for a valid login")
    fun testLogin() {
        val creds = Credentials()
        creds.username = "michacod"
        creds.password = "password"
        val response = RestAssured.given().contentType(ContentType.JSON).body(creds).`when`().post(sessionUrl).andReturn()
        assertEquals(200, response.statusCode, "Wrong HTTP status code")
        val cookie = response.header("Set-Cookie")
        assertTrue(cookie.contains(API_TOKEN), "Missing token")
    }

    @Test
    @DisplayName("should return an HTTP 401 for an invalid login")
    fun testLoginRejected() {
        val creds = Credentials()
        creds.username = "asdfasdf"
        creds.password = "password"
        val response = RestAssured.given().contentType(ContentType.JSON).body(creds).`when`().post(sessionUrl).andReturn()
        assertEquals(401, response.statusCode, "Wrong HTTP status code")
    }

    @Test
    @DisplayName("should unset a session token for a valid logout")
    fun testLogout() {
        val creds = Credentials("test", "test")
        val response = RestAssured.given().contentType(ContentType.JSON).body(creds).`when`().post(sessionUrl).andReturn()
        assertEquals(200, response.statusCode, "Wrong HTTP status code")
        val cookie = response.header("Set-Cookie")
        val regex = Regex("$API_TOKEN=([\\w-]+)")
        val result = regex.find(cookie)
        assertNotNull(result)
        val tokenRes = result!!.groups[1]
        assertNotNull(tokenRes)
        val token = tokenRes!!.value

        // ok. now log out
        val logoutRes = RestAssured.given().header("Cookie", "$API_TOKEN=$token").`when`().delete(sessionUrl).andReturn()
        assertEquals(200, logoutRes.statusCode, "Wrong HTTP status code")
        val unsetCookie = logoutRes.header("Set-Cookie")
        assertTrue(unsetCookie.contains(API_TOKEN), "Missing token")
        assertTrue(unsetCookie.contains("expires"))
    }

}