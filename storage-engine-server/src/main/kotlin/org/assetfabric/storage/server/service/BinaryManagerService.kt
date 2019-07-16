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

package org.assetfabric.storage.server.service

import org.assetfabric.storage.InputStreamWithLength
import reactor.core.publisher.Mono
import java.io.InputStream

/**
 * A binary repository is responsible for storing a given piece of binary data
 * at a unique location and allowing it to be retrieved later from that location.
 *
 * The repository must also guarantee that multiple calls to store the exact same binary
 * data must return the exact same stored location, i.e., it must de-duplicate binary
 * files so that only one copy of a given binary is actually stored.
 */
interface BinaryManagerService {

    /**
     * Creates a unique, de-duplicated file from the given input stream and returns its relative path within
     * the file storage system.
     * @param stream the stream to process
     * @return the relative path of this file within the data store
     */
    fun createFile(streamWithLength: InputStreamWithLength): String

    /**
     * Returns an input stream for the file at the given path, or None if no such file exists
     * @param path a hashed file path originally returned from the createFile() method
     */
    fun getInputStreamForFile(path: String): Mono<InputStream>

    /**
     * Returns true if there is a file at the given path.
     */
    fun fileExists(hashPath: String): Boolean

    /**
     * Deletes the file at the given path.
     */
    fun delete(hashPath: String): Boolean


}