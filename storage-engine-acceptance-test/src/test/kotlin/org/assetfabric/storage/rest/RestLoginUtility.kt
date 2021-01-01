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
import org.assetfabric.storage.server.runtime.controller.Constants.API_TOKEN
import org.junit.jupiter.api.Assertions

class RestLoginUtility {

    fun getTokenForUser(sessionUrl: String, name: String, pass: String): String {
        val creds = Credentials(name, pass)
        val response = RestAssured.given().contentType(ContentType.JSON).body(creds).`when`().post(sessionUrl).andReturn()
        Assertions.assertEquals(200, response.statusCode, "Wrong HTTP status code")
        val cookie = response.header("Set-Cookie")
        val regex = Regex("$API_TOKEN=([\\w-]+)")
        val result = regex.find(cookie)
        Assertions.assertNotNull(result)
        val tokenRes = result!!.groups[1]
        Assertions.assertNotNull(tokenRes)
        return tokenRes!!.value
    }

}