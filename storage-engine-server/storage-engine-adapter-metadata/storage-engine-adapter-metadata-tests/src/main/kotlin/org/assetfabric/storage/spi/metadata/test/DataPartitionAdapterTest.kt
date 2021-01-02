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

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.NodeReference
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.RevisionedNodeRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultRevisionedNodeRepresentation
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Flux

open class DataPartitionAdapterTest {

    private val log = LogManager.getLogger(DataPartitionAdapterTest::class.java)

    @Autowired
    private lateinit var adapter: DataPartitionAdapter

    private fun createRepresentation(path: Path, revision: RevisionNumber): RevisionedNodeRepresentation {
        val map = mutableMapOf(
                "stringProp" to "hi",
                "stringListProp" to listOf("a", "b", "c"),
                "nodeRefProp" to NodeReference("/some/node")
        )
        return DefaultRevisionedNodeRepresentation(path, revision, NodeType.UNSTRUCTURED, map, State.NORMAL)
    }

    @BeforeEach
    private fun init() {
        adapter.reset().block()
    }

    @Test
    @DisplayName("should be able to write a set of node representations")
    fun testWriteNodes() {
        val revision = RevisionNumber(2)
        val count = 10
        val nodes = Flux.range(0, count).map { index ->
            createRepresentation(Path("/test/node$index"), revision)
        }

        adapter.writeNodeRepresentations(nodes).block()
        val paths = Flux.range(0, count).map { index ->
            Path("/test/node$index")
        }
        paths.subscribe { path ->
            Assertions.assertTrue(adapter.nodeRepresentation(revision, path).blockOptional().isPresent, "Node $path not found")
        }
    }

    @Test
    @DisplayName("should be able to return the children of a node")
    fun testGetNodeChildren() {
        val revision = RevisionNumber(2)
        val count = 10
        val nodes = Flux.range(0, count).map { index ->
            createRepresentation(Path("/node$index"), revision)
        }
        adapter.writeNodeRepresentations(nodes).block()
        log.debug("Wrote child representations")
        val retList = adapter.nodeChildRepresentations(revision, Path("/")).collectList().block()
        assertNotNull(retList, "Null children returned")
        assertEquals(count, retList!!.size)
    }

    @Test
    @DisplayName("should only return the most recent versions of a node's children")
    fun testGetNewestNodeChildren() {
        adapter.reset().block()
        val revision = RevisionNumber(2)
        val count = 3
        val nodes = Flux.range(0, count).map { index ->
            createRepresentation(Path("/node"), revision.plus(index))
        }
        adapter.writeNodeRepresentations(nodes).block()
        log.debug("Wrote child representations")
        val retList = adapter.nodeChildRepresentations(revision.plus(1), Path("/")).collectList().block()
        assertNotNull(retList, "Null children returned")
        assertEquals(1, retList!!.size)
        assertEquals(RevisionNumber(3), retList.get(0).revision())
    }

}