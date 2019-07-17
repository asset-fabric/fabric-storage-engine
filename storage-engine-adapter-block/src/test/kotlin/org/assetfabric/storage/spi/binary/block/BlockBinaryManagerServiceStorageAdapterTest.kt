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

package org.assetfabric.storage.spi.binary.block

import org.apache.commons.io.IOUtils
import org.assetfabric.storage.InputStreamWithLength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream


@ExtendWith(SpringExtension::class)
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = [
    "assetfabric.storage.binary.adapter.type=block",
    "assetfabric.storage.binary.adapter.block.folder=target/assetfabric"
])
@DisplayName("a block binary repository storage adapter")
class BlockBinaryManagerServiceStorageAdapterTest {

    @Configuration
    @Import(BlockBinaryRepositoryStorageAdapter::class)
    internal class Config

    @Autowired
    private lateinit var storageAdapter: BlockBinaryRepositoryStorageAdapter

    @Test
    @DisplayName("should be able to store and retrieve temporary file")
    fun testStoreTemporaryFile() {
        val swl = InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        val info = storageAdapter.createTempFile(swl)
        assertNotNull(info, "File info not returned")
        var fileMatched = false
        storageAdapter.getTempFolder().walkTopDown().forEach {  file ->
            if (file.name == info.path) {
                fileMatched = true
                return
            }
        }
        assertTrue(fileMatched, "File not found")

        val stream = storageAdapter.inputStreamForTemporaryLocation(info.path)
        assertNotNull(stream, "stream not returned")
    }

    @Test
    @DisplayName("should be able to delete a temp file at a given path")
    fun testDeleteTempFile() {
        val swl = InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        val info = storageAdapter.createTempFile(swl)
        assertNotNull(info, "File info not returned")

        storageAdapter.deleteTempFile(info.path)
        var fileMatched = false
        storageAdapter.getTempFolder().walkTopDown().forEach {  file ->
            if (file.name == info.path) {
                fileMatched = true
                return
            }
        }
        assertFalse(fileMatched, "File found")
    }

    @Test
    @DisplayName("should be able to list the permanent files with a given prefix")
    fun testListFilesWithHash() {
        val prefix = "a/b/c/d"
        val permFolder = storageAdapter.getPermanentFolder()
        val hashFolder = File(permFolder, prefix)
        hashFolder.mkdirs()
        val file1 = File(hashFolder, "file1")
        val s1 = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        IOUtils.copyLarge(s1, FileOutputStream(file1))

        val file2 = File(hashFolder, "file2")
        val s2 = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        IOUtils.copyLarge(s2, FileOutputStream(file2))

        val fileInfos = storageAdapter.permanentFilesWithHashPrefix(prefix)
        assertEquals(2, fileInfos.size)
    }

    @Test
    @DisplayName("should use be able to get the input stream for a file at a permanent location")
    fun testGetPermanentFile() {
        val prefix = "a/b/c/d"
        val permFolder = storageAdapter.getPermanentFolder()
        val hashFolder = File(permFolder, prefix)
        hashFolder.mkdirs()
        val file1 = File(hashFolder, "file1")
        val s1 = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        IOUtils.copyLarge(s1, FileOutputStream(file1))

        val inStream = storageAdapter.inputStreamForPermanentLocation("$prefix/file1")
        assertNotNull(inStream, "No input stream returned")
    }

    @Test
    @DisplayName("should be able to move a temporary file to a permanent location")
    fun testMoveTempFileToPermanentStorage() {
        val swl = InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        val info = storageAdapter.createTempFile(swl)
        val tempPath = info.path
        val permPath = "x/y/z/permafile"
        storageAdapter.moveTempFileToPermanentLocation(tempPath, permPath)
        val permFile = File(storageAdapter.getPermanentFolder(), permPath)
        assertTrue(permFile.exists(), "Permanent file not found")
    }


}