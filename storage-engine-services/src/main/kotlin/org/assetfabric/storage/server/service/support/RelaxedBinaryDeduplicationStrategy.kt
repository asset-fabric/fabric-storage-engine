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

import org.assetfabric.storage.spi.binary.FileInfo
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * A relaxed form of duplication detection. Considers an existing file with the same hash and size
 * as the given temporary file as being a duplicate.  Although this is likely a perfectly safe assumption
 * to make, it does not account for the extremely remote possibility that two binaries of the same size
 * and hash actually contain different content.
 */
@Component
@ConditionalOnProperty("assetfabric.storage.binary.deduplication.strategy", havingValue = "relaxed")
class RelaxedBinaryDeduplicationStrategy: BinaryDeduplicationStrategy {

    override fun findDuplicateBinary(temporaryFile: FileInfo, permanentFilesWithHash: List<FileInfo>): Optional<FileInfo> {
        val filesWithSameSize = permanentFilesWithHash.filter { it.size == temporaryFile.size }
        return when (filesWithSameSize.size) {
            0 -> Optional.empty()
            1 -> Optional.of(filesWithSameSize.first())
            else -> throw IllegalArgumentException("Found multiple files size ${temporaryFile.size}")
        }
    }
}