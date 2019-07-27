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

package org.assetfabric.storage.server.service.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.server.service.BinaryManagerService
import org.assetfabric.storage.spi.FileInfo
import org.assetfabric.storage.spi.binary.BinaryRepositoryStorageAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Optional
import java.util.UUID

@Repository
class DefaultBinaryManagerService: BinaryManagerService {

    private final val log = LogManager.getLogger(DefaultBinaryManagerService::class.java)

    @Autowired
    private lateinit var storageAdapter: BinaryRepositoryStorageAdapter

    @Autowired
    private lateinit var deduplicationStrategy: BinaryDeduplicationStrategy

    private val hexFormat = "%02x"

    private fun pathForHash(sha1hash: String) =  sha1hash.chunked(2).joinToString(File.separator)

    override fun createFile(streamWithLength: InputStreamWithLength): String {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        val dis = DigestInputStream(streamWithLength.inputStream, messageDigest)
        val swl = InputStreamWithLength(dis, streamWithLength.length)
        try {
            val tempLocationInfo: FileInfo = storageAdapter.createTempFile(swl)

            val digestBytes = messageDigest.digest()
            val sb = StringBuffer()
            for (b in digestBytes) sb.append(String.format(hexFormat, b)) // convert byte to hex char
            val sha1hash = sb.toString()

            val sha1RelativePath = pathForHash(sha1hash)
            val targetPath = "$sha1RelativePath/$sha1hash-${UUID.randomUUID()}"

            val permanentFilesWithHash = storageAdapter.permanentFilesWithHashPrefix(sha1RelativePath)
            if (permanentFilesWithHash.isEmpty()) {
                log.debug("No permanent file found for hash $sha1RelativePath")
                storageAdapter.moveTempFileToPermanentLocation(tempLocationInfo.path, targetPath)
                return targetPath
            } else {
                val duplicateFilePathOptional: Optional<FileInfo> = deduplicationStrategy.findDuplicateBinary(tempLocationInfo, permanentFilesWithHash)
                if (duplicateFilePathOptional.isPresent) {
                    log.debug("Found duplicate permanent file for hash $sha1hash")
                    storageAdapter.deleteTempFile(tempLocationInfo.path)
                    return duplicateFilePathOptional.get().path
                } else {
                    log.debug("No permanent file matching de-dupe strategy for hash $sha1hash")
                    storageAdapter.moveTempFileToPermanentLocation(tempLocationInfo.path, targetPath)
                    return targetPath
                }
            }

        } finally {
            streamWithLength.inputStream.close()
            dis.close()
        }
    }

    override fun getInputStreamForFile(path: String): Mono<InputStream> {
        return Mono.defer {
            Mono.just(storageAdapter.inputStreamForPermanentLocation(path))
        }
    }

    override fun fileExists(hashPath: String): Boolean {
        return storageAdapter.permanentLocationExists(hashPath)
    }

    override fun delete(hashPath: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}