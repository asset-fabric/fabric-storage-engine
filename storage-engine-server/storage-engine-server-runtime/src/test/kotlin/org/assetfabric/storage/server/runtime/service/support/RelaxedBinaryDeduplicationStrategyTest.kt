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

package org.assetfabric.storage.server.runtime.service.support

import org.assetfabric.storage.spi.binary.FileInfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
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

@ExtendWith(SpringExtension::class)
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = ["assetfabric.storage.binary.deduplication.strategy=relaxed"])
@DisplayName("a relaxed binary de-duplication strategy")
class RelaxedBinaryDeduplicationStrategyTest {

    @Configuration
    @Import(RelaxedBinaryDeduplicationStrategy::class)
    internal class Config

    @Autowired
    private lateinit var strategy: RelaxedBinaryDeduplicationStrategy

    @Test
    @DisplayName("should treat binaries with the same hash and size as duplicates")
    fun shouldTreatSameSizeAsDuplicate() {
        val tempFileInfo = FileInfo("temp/file", 5)
        val permFileInfo = FileInfo("perm/file/hash", 5)
        val retOpt = strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo))
        assertTrue(retOpt.isPresent, "Duplicate file not returned")
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
    @DisplayName("should throw an exception when finding mutliple files with the same hash and size")
    fun shouldThrowExceptionOnMultipleMatches() {
        val tempFileInfo = FileInfo("temp/file", 5)
        val permFileInfo1 = FileInfo("perm/file/hash", 5)
        val permFileInfo2 = FileInfo("perm/file2/hash", 5)
        assertThrows(IllegalArgumentException::class.java) {
            strategy.findDuplicateBinary(tempFileInfo, listOf(permFileInfo1, permFileInfo2))
        }

    }

}