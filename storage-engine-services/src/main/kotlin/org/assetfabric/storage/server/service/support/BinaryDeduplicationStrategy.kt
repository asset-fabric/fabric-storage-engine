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
import java.util.Optional

/**
 * Governs the logic used by the [BinaryManagerService] in deciding when an incoming binary is a duplicate
 * of an existing binary.
 */
interface BinaryDeduplicationStrategy {

    /**
     * Returns information about a duplicate binary found in permanent storage.
     * @param temporaryFile information about the temporary binary being evaluated
     * @param permanentFilesWithHash the permanent binaries that have the same hash as the temporary binary
     * @return an optional containing information about the duplicate binary, or empty if no duplicate binary found
     */
    fun findDuplicateBinary(temporaryFile: FileInfo, permanentFilesWithHash: List<FileInfo>): Optional<FileInfo>

}