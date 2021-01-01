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
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.JournalEntryNodeRepresentation
import org.assetfabric.storage.spi.metadata.JournalPartitionAdapter
import org.assetfabric.storage.spi.metadata.RevisionedNodeRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultJournalEntryNodeRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultRevisionedNodeRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader
import reactor.core.publisher.Flux

open class JournalPartitionAdapterTest {

    private val log = LogManager.getLogger(JournalPartitionAdapterTest::class.java)

    @Autowired
    private lateinit var partitionAdapter: JournalPartitionAdapter

    private fun createRepresentation(path: Path, revision: RevisionNumber): RevisionedNodeRepresentation {
        return DefaultRevisionedNodeRepresentation(path, revision, NodeType.UNSTRUCTURED, hashMapOf(), State.NORMAL)
    }

    @Test
    @DisplayName("should be able to create a journal entry from a stream of nodes")
    fun testCreateJournalEntry() {
        partitionAdapter.reset().block()
        val count = 5
        val nodesFlux = Flux.range(0, count).map<JournalEntryNodeRepresentation> {
            val repr = createRepresentation(Path("/testa/node$it"), RevisionNumber(3))
            DefaultJournalEntryNodeRepresentation("sessionID", repr.path(), repr.revision(), repr.nodeType(), null, repr)
        }

        partitionAdapter.createJournalEntrySet(nodesFlux).block()
        val listOptional = partitionAdapter.getJournalEntrySet(RevisionNumber(3)).collectList().blockOptional()
        assertTrue(listOptional.isPresent)
        assertEquals(count, listOptional.get().size, "Node count mismatch")
    }

    @Test
    @DisplayName("should not allow the creation of a set of journal entries that contain duplicate paths")
    fun testCreateDuplicatePaths() {
        partitionAdapter.reset().block()
        val count = 5
        val nodesFlux = Flux.range(0, count).map<JournalEntryNodeRepresentation> {
            val repr = createRepresentation(Path("/testa/node"), RevisionNumber(3))
            DefaultJournalEntryNodeRepresentation("sessionID", repr.path(), repr.revision(), repr.nodeType(), null, repr)
        }

        assertThrows(Exception::class.java) {
            partitionAdapter.createJournalEntrySet(nodesFlux).block()
        }
    }

    @Test
    @DisplayName("should be able to get the next journal entry revision number")
    fun testGetNextJournalEntryRevision() {
        partitionAdapter.reset().block()
        val count = 5
        val nodesFlux = Flux.range(0, count).map<JournalEntryNodeRepresentation> {
            val repr = createRepresentation(Path("/testa/node$it"), RevisionNumber(3))
            DefaultJournalEntryNodeRepresentation("sessionID", repr.path(), repr.revision(), repr.nodeType(), null, repr)
        }
        partitionAdapter.createJournalEntrySet(nodesFlux).block()

        val revision = partitionAdapter.getNextJournalRevision().block()!!
        assertEquals(RevisionNumber(3), revision)
    }

    @Test
    @DisplayName("should be able to return the next set of journal entries")
    fun testGetNextJournalEntries() {

        log.debug("Resetting journal")
        partitionAdapter.reset().block()
        log.debug("Journal reset")

        val count = 5
        val nodesFlux = Flux.range(0, count).map<JournalEntryNodeRepresentation> {
            val repr = createRepresentation(Path("/testa/node$it"), RevisionNumber(3))
            DefaultJournalEntryNodeRepresentation("sessionID", repr.path(), repr.revision(), repr.nodeType(), null, repr)
        }
        partitionAdapter.createJournalEntrySet(nodesFlux).block()

        val nodesFlux2 = Flux.range(0, count).map<JournalEntryNodeRepresentation> {
            val repr = createRepresentation(Path("/testa/node$it"), RevisionNumber(4))
            DefaultJournalEntryNodeRepresentation("sessionID", repr.path(), repr.revision(), repr.nodeType(), null, repr)
        }
        partitionAdapter.createJournalEntrySet(nodesFlux2).block()

        val nextNodes = partitionAdapter.getJournalEntrySet(RevisionNumber(3)).collectList().block()
        assertEquals(5, nextNodes!!.size)

    }

}