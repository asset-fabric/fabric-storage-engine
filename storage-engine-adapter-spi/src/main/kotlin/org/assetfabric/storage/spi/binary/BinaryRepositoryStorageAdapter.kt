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

package org.assetfabric.storage.spi.binary

import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.spi.FileInfo
import java.io.InputStream

/**
 * Handles low-level binary storage operations.
 */
interface BinaryRepositoryStorageAdapter {

    /**
     * Creates a temporary file containing the data taken from an input stream
     * @param stream an input stream
     * @return the information about the temp file that was created
     */
    fun createTempFile(streamWithLength: InputStreamWithLength): FileInfo

    /**
     * Opens an input stream for the temporary file at the given location
     * @param location the location of the temporary file for which an input stream should be returned
     */
    fun inputStreamForTemporaryLocation(location: String): InputStream

    /**
     * Deletes the temp file at a given path
     * @param path the relative path of a temp file to delete
     */
    fun deleteTempFile(path: String)

    /**
     * Returns information about files that begin with the specified hash prefix
     * @param hashPrefix the prefix to use in order to find files
     */
    fun permanentFilesWithHashPrefix(hashPrefix: String): List<FileInfo>

    /**
     * Opens an input stream for the permanent file at the given location
     * @param location the location of the permanent file for which an input stream should be returned
     */
    fun inputStreamForPermanentLocation(location: String): InputStream

    /**
     * Moves a temporary file at a given relative path to a permanent file at the given
     * relative path.
     * @param tempPath the relative path of a temporary file
     * @param targetLocation the relative path of the permanent file to be created.
     */
    fun moveTempFileToPermanentLocation(tempPath: String, targetLocation: String)

}