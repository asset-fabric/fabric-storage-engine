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

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.times
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.server.service.BinaryManagerService
import org.assetfabric.storage.spi.binary.FileInfo
import org.assetfabric.storage.spi.binary.BinaryRepositoryStorageAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.contains
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.util.StreamUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Optional

@ExtendWith(SpringExtension::class)
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@DisplayName("a default binary managerService")
class DefaultBinaryManagerServiceTest {

    @Configuration
    @Import(DefaultBinaryManagerService::class)
    internal class Config {

        @Bean
        fun mockStrategy(): BinaryDeduplicationStrategy = mock(BinaryDeduplicationStrategy::class.java)

        @Bean
        fun mockAdapter(): BinaryRepositoryStorageAdapter = mock(BinaryRepositoryStorageAdapter::class.java)

    }

    @Autowired
    private lateinit var adapter: BinaryRepositoryStorageAdapter

    @Autowired
    private lateinit var strategy: BinaryDeduplicationStrategy

    @Autowired
    private lateinit var managerService: BinaryManagerService

    @BeforeEach
    private fun init() {
        reset(adapter)
        reset(strategy)
    }

    private fun getHash(bytes: ByteArray): String {
        val stream = ByteArrayInputStream(bytes)
        val messageDigest = MessageDigest.getInstance("SHA-1")
        val dis = DigestInputStream(stream, messageDigest)
        dis.bufferedReader().use{ it.readText() }
        val sb = StringBuffer()
        val digestBytes = messageDigest.digest()
        for (b in digestBytes) sb.append(String.format("%02x", b)) // convert byte to hex char
        return sb.toString()
    }

    @Test
    @DisplayName("should write a binary with a unique hash to storage")
    fun testWriteUniqueFile() {
        val bytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5)
        val stream = ByteArrayInputStream(bytes)
        val swl = InputStreamWithLength(stream, 5)

        `when`(strategy.findDuplicateBinary(any(), any())).thenReturn(Optional.empty())
        `when`(adapter.createTempFile(any())).thenAnswer {
            (it.getArgument(0) as InputStreamWithLength).inputStream.use { stream -> StreamUtils.drain(stream) }
            FileInfo("permanent/location", 5)
        }

        val permanentPath = managerService.createFile(swl)

        verify(adapter).createTempFile(any())
        verify(adapter).moveTempFileToPermanentLocation(anyString(), anyString())
        assertNotNull(permanentPath, "Null file path returned")
    }

    @Test
    @DisplayName("should deduplicate a binary when there is a matching permanent file")
    fun testDeduplicateOnMatchingHash() {
        val bytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5)
        val stream = ByteArrayInputStream(bytes)
        val swl = InputStreamWithLength(stream, 5)
        val hash = getHash(bytes)
        val path = hash.chunked(2).joinToString(File.separator)
        val existingPath = "permanent/path"
        `when`(strategy.findDuplicateBinary(any(), any())).thenReturn(Optional.of(FileInfo(existingPath, 5)))
        `when`(adapter.createTempFile(any())).thenAnswer {
            (it.getArgument(0) as InputStreamWithLength).inputStream.use { stream -> StreamUtils.drain(stream) }
            FileInfo("temp/file", 5)
        }
        `when`(adapter.inputStreamForTemporaryLocation(eq("temp/file"))).thenReturn(ByteArrayInputStream(bytes))
        `when`(adapter.inputStreamForPermanentLocation(contains(path))).thenReturn(ByteArrayInputStream(bytes))
        `when`(adapter.permanentFilesWithHashPrefix(path)).thenReturn(mutableListOf(FileInfo("$path/$hash", 5)))
        val retPath = managerService.createFile(swl)
        verify(adapter, times(0)).moveTempFileToPermanentLocation(any(), any())
        assertEquals(existingPath, retPath, "Existing path not returned")
    }

    @Test
    @DisplayName("should deduplicate a binary when its hash exists but the file size is different")
    fun testDeduplicateOnMatchingHashDifferentSize() {
        val bytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5)
        val stream = ByteArrayInputStream(bytes)
        val swl = InputStreamWithLength(stream, 5)
        val hash = getHash(bytes)
        val path = hash.chunked(2).joinToString(File.separator)
        `when`(adapter.createTempFile(any())).thenReturn(FileInfo("temp/path", 5))
        `when`(adapter.permanentFilesWithHashPrefix(path)).thenReturn(mutableListOf(FileInfo("$path/$hash", bytes.size + 1L)))
        managerService.createFile(swl)
        verify(adapter, times(1)).moveTempFileToPermanentLocation(any(), any())
    }

    @Test
    @DisplayName("should deduplicate a binary when its hash exists and the file size matches but the contents are different")
    fun testDeduplicateOnMatchingHashSameSizeDifferentContent() {
        val bytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5)
        val stream = ByteArrayInputStream(bytes)
        val swl = InputStreamWithLength(stream, 5)
        val hash = getHash(bytes)
        val path = hash.chunked(2).joinToString(File.separator)
        `when`(adapter.createTempFile(any())).thenAnswer {
            (it.getArgument(0) as InputStreamWithLength).inputStream.use { stream -> StreamUtils.drain(stream) }
            FileInfo("temp/file", 5)
        }
        `when`(adapter.inputStreamForTemporaryLocation(eq("temp/file"))).thenReturn(ByteArrayInputStream(bytes))
        `when`(adapter.inputStreamForPermanentLocation(contains(path))).thenReturn(ByteArrayInputStream(byteArrayOf(2, 3, 4, 5, 6)))
        `when`(adapter.permanentFilesWithHashPrefix(path)).thenReturn(mutableListOf(FileInfo("$path/$hash", 5)))
        managerService.createFile(swl)
        verify(adapter, times(1)).moveTempFileToPermanentLocation(any(), any())
    }

}