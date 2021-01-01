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

import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.spi.binary.FileInfo
import org.assetfabric.storage.spi.binary.BinaryRepositoryStorageAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * A conservative approach to duplicate detection. If there is an existing file with the same hash and size
 * as the given temporary file, it will then perform a byte-by-byte comparison of the temporary file and the
 * existing file in order to decide if the two are exactly the same. This obviously carries a significant performance
 * impact but guarantees that two files with the same hash and size, but different content, will not be wrongly
 * treated as duplicates.
 */
@Component
@ConditionalOnProperty("assetfabric.storage.binary.deduplication.strategy", havingValue = "conservative")
class ConservativeBinaryDeduplicationStrategy: BinaryDeduplicationStrategy {

    private val log = LogManager.getLogger(ConservativeBinaryDeduplicationStrategy::class.java)

    @Autowired
    private lateinit var storageAdapter: BinaryRepositoryStorageAdapter

    override fun findDuplicateBinary(temporaryFile: FileInfo, permanentFilesWithHash: List<FileInfo>): Optional<FileInfo> {
        log.info("Seeking duplicate binary for file ${temporaryFile.path}")

        val filesWithSameSize = permanentFilesWithHash.filter { it.size == temporaryFile.size }

        return when(filesWithSameSize.size) {
            0 -> Optional.empty()
            else -> {
                val matchingBinaryFiles = filesWithSameSize.filter { compareBinaryData(temporaryFile, it) }

                when(matchingBinaryFiles.size) {
                    0 -> Optional.empty()
                    1 -> Optional.of(matchingBinaryFiles.first())
                    else -> throw IllegalArgumentException("Found multiple matching permanent files with the same hash, size and data")
                }
            }
        }
    }

    private fun compareBinaryData(tempLocationInfo: FileInfo, targetInfo: FileInfo): Boolean {
        log.debug("Comparing ${tempLocationInfo.path} to permanent file ${targetInfo.path}")

        val tempInputStream = storageAdapter.inputStreamForTemporaryLocation(tempLocationInfo.path)
        val permanentInputStream = storageAdapter.inputStreamForPermanentLocation(targetInfo.path)
        try {
            return IOUtils.contentEquals(tempInputStream, permanentInputStream)
        } finally {
            log.debug("Closing input streams")
            tempInputStream.close()
            permanentInputStream.close()
        }
    }

}