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

package org.assetfabric.storage.spi.binary.test

import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.spi.binary.BinaryRepositoryStorageAdapter
import org.assetfabric.storage.spi.binary.FileInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream

abstract class BinaryManagerServiceStorageAdapterTest {

    @Autowired
    protected lateinit var storageAdapter: BinaryRepositoryStorageAdapter

    /* Prepares the adapter to create a temp file */
    abstract fun prepareCreateFileResponse()

    /**
     *  Prepares the adapter to return an input stream for a temporary file.
     *  @return the location of a temp file
     */
    abstract fun prepareTemporaryInputStreamResponse(): String

    /**
     * Prepares a temp file via the adapter.
     * @return the location of a temp file
     */
    abstract fun prepareTempFile(): String

    /* Prepares the adapter to return a file list */
    abstract fun prepareFileListing(paths: List<String>)

    /* Prepares the adapter to return the input stream for a permanent file */
    abstract fun preparePermanentInputStreamResponse(path: String)

    /**
     * Prepares the adapter to move a file from temporary storage to permanent storage
     * @return the location of a temp file to move
    */
    abstract fun prepareFileMove(): String

    /* Verifies the adapter moved a file from temporary storage to permanent storage */
    abstract fun verifyFileMove(tempPath: String, permPath: String)

    @Test
    @DisplayName("should be able to store a temporary file")
    fun testStoreTemporaryFile() {
        prepareCreateFileResponse()
        val swl = InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        val info: FileInfo = storageAdapter.createTempFile(swl)
        assertNotNull(info, "Null file info")
    }

    @Test
    @DisplayName("should use be able to get the input stream for a file at a temp location")
    fun testGetTempFile() {
        val tempPath = prepareTemporaryInputStreamResponse()
        val stream = storageAdapter.inputStreamForTemporaryLocation(tempPath)
        assertNotNull(stream, "stream not returned")
    }

    @Test
    @DisplayName("should be able to delete a temp file at a given path")
    fun testDeleteTempFile() {
        val tempFilePath = prepareTempFile()
        storageAdapter.deleteTempFile(tempFilePath)
        assertFalse(storageAdapter.temporaryFileExists(tempFilePath), "Temp file exists after delete")
    }

    @Test
    @DisplayName("should be able to list the permanent files with a given prefix")
    fun testListFilesWithHash() {
        prepareFileListing(listOf("ab/cd/ef/gh/1", "ab/cd/ef/gh/2"))
        val retInfoList = storageAdapter.permanentFilesWithHashPrefix("ab/cd/ef/gh")
        assertNotNull(retInfoList, "Null permanent file list for prefix")
        assertEquals(2, retInfoList.size, "File listing size mismatch")
    }

    @Test
    @DisplayName("should use be able to get the input stream for a file at a permanent location")
    fun testGetPermanentFile() {
        preparePermanentInputStreamResponse("ab/cd/1")
        val stream = storageAdapter.inputStreamForPermanentLocation("ab/cd/1")
        assertNotNull(stream, "stream not returned")
    }

    @Test
    @DisplayName("should be able to move a temporary file to a permanent location")
    fun testMoveTempFileToPermanentStorage() {
        val tempPath = prepareFileMove()
        val permPath = "ab/cd/ed/fg/1"
        storageAdapter.moveTempFileToPermanentLocation(tempPath, permPath)
        verifyFileMove(tempPath, permPath)
    }

}