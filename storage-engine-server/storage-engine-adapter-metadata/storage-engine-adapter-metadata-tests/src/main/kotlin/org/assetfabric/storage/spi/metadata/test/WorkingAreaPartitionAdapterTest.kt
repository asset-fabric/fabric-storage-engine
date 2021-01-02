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

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.metadata.support.DefaultNodeContentRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultWorkingAreaNodeRepresentation
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono

open class WorkingAreaPartitionAdapterTest {

    @Autowired
    private lateinit var partitionAdapter: WorkingAreaPartitionAdapter

    @Test
    @DisplayName("should be able to store a working area node representation")
    fun testCreateNodeRepresentation() {
        val sessionId = "testSession"
        val path = Path("/test")

        val core = DefaultNodeContentRepresentation(hashMapOf("int" to 4))
        val repr = DefaultWorkingAreaNodeRepresentation(sessionId, path, NodeType.UNSTRUCTURED, null, core)

        partitionAdapter.createNodeRepresentation(repr).subscribe {
            val opt = partitionAdapter.nodeRepresentation(sessionId, path).blockOptional()
            Assertions.assertTrue(opt.isPresent, "no working representation found")
            val retRepr = opt.get()
            assertEquals(repr.path, retRepr.path(), "path mismatch")
            assertEquals(repr.workingAreaRepresentation, retRepr.workingAreaRepresentation(), "working are representation mismatch")
        }
    }

    @Test
    @DisplayName("should be able to update a working area node representation")
    fun testUpdateNodeRepresentation() {
        val sessionId = "testSession"
        val path = Path("/test")

        val core = DefaultNodeContentRepresentation(hashMapOf("int" to 4))
        val repr = DefaultWorkingAreaNodeRepresentation(sessionId, path, NodeType.UNSTRUCTURED, null, core)

        partitionAdapter.createNodeRepresentation(repr).block()

        core.setState(State.DELETED)
        partitionAdapter.updateWorkingAreaRepresentation(repr).block()

        val newRepr = partitionAdapter.nodeRepresentation(sessionId, path).block()!!
        assertEquals(State.DELETED, newRepr.workingAreaRepresentation().state(), "state mismatch")
    }

    @Test
    @DisplayName("should be able to retrieve all working area representations for a given session")
    fun testGetSessionWorkingNodes() {
        val sessionId = "testSession"

        val path1 = "/test"
        val core1 = DefaultNodeContentRepresentation(hashMapOf("int" to 4))
        val repr1 = DefaultWorkingAreaNodeRepresentation(sessionId, Path(path1), NodeType.UNSTRUCTURED, null, core1)
        val create1 = partitionAdapter.createNodeRepresentation(repr1)

        val path2 = "/test2"
        val core2 = DefaultNodeContentRepresentation(hashMapOf("int" to 5))
        val repr2 = DefaultWorkingAreaNodeRepresentation(sessionId, Path(path2), NodeType.UNSTRUCTURED, null, core2)
        val create2 = partitionAdapter.createNodeRepresentation(repr2)

        create1.then(create2).subscribe {
            val nodesFlux = partitionAdapter.getWorkingAreaRepresentations(sessionId)
            nodesFlux.count().subscribe {
                count -> assertEquals(2, count, "node count mismatch")
            }
        }
    }

    @Test
    @DisplayName("should be able to remove all working area representations for a given session")
    fun testDeleteSessionWorkingNodes() {

        val sessionId = "testSession"

        fun getNodeCount(): Mono<Long> {
            val nodesFlux = partitionAdapter.getWorkingAreaRepresentations(sessionId)
            return nodesFlux.count()
        }

        // clear the existing nodes
        partitionAdapter.deleteWorkingAreaRepresentations(sessionId).subscribe {
            getNodeCount().subscribe { count -> assertEquals(0, count, "node count mismatch") }
        }

        // create a test node
        val path1 = "/test"
        val core1 = DefaultNodeContentRepresentation(hashMapOf("int" to 4))
        val repr1 = DefaultWorkingAreaNodeRepresentation(sessionId, Path(path1), NodeType.UNSTRUCTURED, null, core1)
        partitionAdapter.createNodeRepresentation(repr1).subscribe {
            getNodeCount().subscribe { count -> assertEquals(1, count, "node count mismatch") }
        }

        // delete all nodes and check there are none left
        partitionAdapter.deleteWorkingAreaRepresentations(sessionId).subscribe {
            getNodeCount().subscribe { count -> assertEquals(0, count, "node count mismatch") }
        }

    }

    @Test
    @DisplayName("should be able to return the working area children of a given node")
    fun testGetWorkingAreaChildren() {
        val sessionId = "testSession"
        val path = "/test"

        val core = DefaultNodeContentRepresentation(hashMapOf("int" to 4))
        val repr = DefaultWorkingAreaNodeRepresentation(sessionId, Path(path), NodeType.UNSTRUCTURED, null, core)

        partitionAdapter.createNodeRepresentation(repr).block()

        val childNodes = partitionAdapter.nodeChildRepresentations(sessionId, Path("/")).collectList().block()
        assertFalse(childNodes!!.isEmpty(), "Node children not returned")
    }

}