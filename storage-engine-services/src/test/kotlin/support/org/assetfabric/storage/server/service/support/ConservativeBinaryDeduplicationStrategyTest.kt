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

import com.nhaarman.mockito_kotlin.eq
import org.assetfabric.storage.spi.FileInfo
import org.assetfabric.storage.spi.binary.BinaryRepositoryStorageAdapter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader
import java.io.ByteArrayInputStream

@ExtendWith(SpringExtension::class)
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = ["assetfabric.storage.binary.deduplication.strategy=conservative"])
@DisplayName("a conservative binary de-duplication strategy")
class ConservativeBinaryDeduplicationStrategyTest {

    @Configuration
    @Import(ConservativeBinaryDeduplicationStrategy::class)
    internal class Config {

        @Bean
        fun mockAdapter(): BinaryRepositoryStorageAdapter = mock(BinaryRepositoryStorageAdapter::class.java)

    }

    @Autowired
    private lateinit var adapter: BinaryRepositoryStorageAdapter

    @Autowired
    private lateinit var strategy: BinaryDeduplicationStrategy

    @BeforeEach
    private fun init() {
        reset(adapter)
    }

    @Test
    @DisplayName("should not treat a binary with different sizes from a temp file as a duplicate")
    fun shouldNotTreatDifferentSizesAsDuplicate() {

        doAnswer { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }.`when`(adapter).inputStreamForTemporaryLocation(eq("temp/file"))
        doAnswer { ByteArrayInputStream(byteArrayOf(1, 2)) }.`when`(adapter).inputStreamForPermanentLocation(eq("perm/file/hash"))

        val tempFileInfo = FileInfo("temp/file", 3)
        val permFileInfo = FileInfo("perm/file/hash", 2)
        val retOpt = strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo))
        assertFalse(retOpt.isPresent, "Duplicate file returned")
    }

    @Test
    @DisplayName("should not treat binaries with different sizes as duplicates")
    fun shouldTreatDifferentSizeAsUnique() {
        val tempFileInfo = FileInfo("temp/file", 5)
        val permFileInfo = FileInfo("perm/file/hash", 2)
        val retOpt = strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo))
        assertFalse(retOpt.isPresent, "Duplicate file returned")
    }

    @Test
    @DisplayName("should treat binaries with same size and content as duplicates")
    fun shouldTreatSameSizeAndContentAsDuplicate() {
        doAnswer { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }.`when`(adapter).inputStreamForTemporaryLocation(eq("temp/file"))
        doAnswer { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }.`when`(adapter).inputStreamForPermanentLocation(eq("perm/file/hash"))

        val tempFileInfo = FileInfo("temp/file", 5)
        val permFileInfo = FileInfo("perm/file/hash", 5)
        val retOpt = strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo))
        assertTrue(retOpt.isPresent, "Duplicate not returned")
    }


    @Test
    @DisplayName("should not treat binaries with same size but different content as duplicates")
    fun shouldNotTreatSameSizeAndDifferentContentAsDuplicate() {
        doAnswer { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }.`when`(adapter).inputStreamForTemporaryLocation(eq("temp/file"))
        doAnswer { ByteArrayInputStream(byteArrayOf(2, 3, 4)) }.`when`(adapter).inputStreamForPermanentLocation(eq("perm/file/hash"))

        val tempFileInfo = FileInfo("temp/file", 5)
        val permFileInfo = FileInfo("perm/file/hash", 5)
        val retOpt = strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo))
        assertFalse(retOpt.isPresent, "Duplicate was returned")
    }

    @Test
    @DisplayName("should not rely on file size reports alone when verifying binary content")
    fun shouldNotRelyOnReportedFileSizeWhenCheckingContent() {
        doAnswer { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }.`when`(adapter).inputStreamForTemporaryLocation(eq("temp/file"))
        doAnswer { ByteArrayInputStream(byteArrayOf(2, 3)) }.`when`(adapter).inputStreamForPermanentLocation(eq("perm/file/hash"))

        val tempFileInfo = FileInfo("temp/file", 3)
        val permFileInfo = FileInfo("perm/file/hash", 3)
        val retOpt = strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo))
        assertFalse(retOpt.isPresent, "Duplicate was returned")
    }

    @Test
    @DisplayName("should throw an exception when encountering multiple files with the same hash, size, and data")
    fun shouldThrowExceptionOnMultipleMatches() {

        doAnswer { ByteArrayInputStream(byteArrayOf(2, 3, 4)) }.`when`(adapter).inputStreamForTemporaryLocation(eq("temp/file"))
        doAnswer { ByteArrayInputStream(byteArrayOf(2, 3, 4)) }.`when`(adapter).inputStreamForPermanentLocation(eq("perm/file/hash"))
        doAnswer { ByteArrayInputStream(byteArrayOf(2, 3, 4)) }.`when`(adapter).inputStreamForPermanentLocation(eq("perm/file2/hash"))

        val tempFileInfo = FileInfo("temp/file", 3)
        val permFileInfo1 = FileInfo("perm/file/hash", 3)
        val permFileInfo2 = FileInfo("perm/file2/hash", 3)
        assertThrows(IllegalArgumentException::class.java) { strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo1, permFileInfo2)) }
    }
}