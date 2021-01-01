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

import org.assetfabric.storage.Path
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.WorkingAreaNodeIndexPartitionAdapter
import org.assetfabric.storage.spi.metadata.support.DefaultWorkingAreaInverseNodeReferenceRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader
import reactor.core.publisher.Flux

open class WorkingAreaNodeIndexPartitionAdapterTest {

    @Autowired
    private lateinit var partitionAdapter: WorkingAreaNodeIndexPartitionAdapter

    @BeforeEach
    private fun reset() {
        partitionAdapter.reset().block()
    }

    @Test
    @DisplayName("should be able to store a working area node reference")
    fun testCreateNodeRepresentation() {
        val sessionId = "testSession"
        val path = "/test"
        val refPath = "/test2"

        val repr = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, Path(path), Path(refPath), State.NORMAL)
        val result = partitionAdapter.createInverseNodeReferences(Flux.just(repr))
        val reprs = result.collectList().block()!!
        assertEquals(1, reprs.size)
    }

    @Test
    @DisplayName("should be able to retrieve all working area node references for a given node in a session")
    fun testGetSessionWorkingNodes() {
        val sessionId = "testSession"
        val node1Path = Path("/test1")
        val repr = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, node1Path, Path("/test2"), State.NORMAL)
        val repr2 = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, node1Path, Path("/test3"), State.NORMAL)
        val result = partitionAdapter.createInverseNodeReferences(Flux.just(repr, repr2))
        result.collectList().block()

        val reprs = partitionAdapter.nodeReferences(sessionId, node1Path).collectList().block()!!
        assertEquals(2, reprs.size)
    }

    @Test
    @DisplayName("should be able to retrieve all working area node references for a given node in a session and its children")
    fun testGetNodeAndChildReferences() {
        val sessionId = "testSession"
        val node1Path = Path("/test1")
        val repr = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, node1Path, Path("/test2/test2"), State.NORMAL)
        val repr2 = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, node1Path, Path("/test2/test3"), State.NORMAL)
        val repr3 = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, node1Path, Path("/test2/test2/test4"), State.DELETED)
        val result = partitionAdapter.createInverseNodeReferences(Flux.just(repr, repr2, repr3))
        result.collectList().block()

        val reprs = partitionAdapter.nodeReferencesAtOrBelow(sessionId, Path("/")).collectList().block()!!
        assertEquals(3, reprs.size)
    }

    @Test
    @DisplayName("should be able to remove all working area node references for a given node in a session")
    fun testDeleteSessionWorkingNodes() {
        val sessionId = "testSession"
        val node1Path = Path("/test1")
        val repr = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, node1Path, Path("/test2"), State.NORMAL)
        val repr2 = DefaultWorkingAreaInverseNodeReferenceRepresentation(sessionId, node1Path, Path("/test3"), State.NORMAL)
        val result = partitionAdapter.createInverseNodeReferences(Flux.just(repr, repr2))
        result.collectList().block()

        var reprs = partitionAdapter.nodeReferences(sessionId, node1Path).collectList().block()!!
        assertEquals(2, reprs.size)

        partitionAdapter.deleteNodeReferences(sessionId).block()
        reprs = partitionAdapter.nodeReferences(sessionId, node1Path).collectList().block()!!
        assertEquals(0, reprs.size)
    }



}