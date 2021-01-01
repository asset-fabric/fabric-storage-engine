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

package org.assetfabric.storage.spi.metadata.test

import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader

open class CatalogPartitionAdapterTest {

    @Autowired
    private lateinit var adapter: CatalogPartitionAdapter

    @Test
    @DisplayName("should be able to write the current repository revision")
    fun testWriteRepositoryRevision() {
        val rev = RevisionNumber(2)
        adapter.setRepositoryRevision(rev).block()
        val retRevisionOpt = adapter.currentRepositoryRevision().blockOptional()
        assertTrue(retRevisionOpt.isPresent, "Revision number not returned")
        assertEquals(rev, retRevisionOpt.get())
    }

}