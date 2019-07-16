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

package org.assetfabric.storage.server.controller.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.server.controller.BinaryController
import org.assetfabric.storage.server.service.BinaryManagerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class DefaultBinaryController: BinaryController {

    private val log = LogManager.getLogger(DefaultBinaryController::class.java)

    @Autowired
    private lateinit var sessionExecutor: SessionExecutor

    @Autowired
    private lateinit var binaryManager: BinaryManagerService

    override fun getBinary(token: String, @RequestParam("path") path: String): Mono<ResponseEntity<InputStreamResource>> {
        val realPath = when(path.startsWith("/")) {
            true -> path
            false -> "/$path"
        }
        log.debug("Returning binary for path $realPath")
        return sessionExecutor.executeWithSession(token) {
            binaryManager.getInputStreamForFile(realPath).map { stream ->
                val res = InputStreamResource(stream)
                ResponseEntity.ok(res)
            }
        }.switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
    }

}