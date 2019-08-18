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

package org.assetfabric.storage

import org.assetfabric.storage.server.service.support.DefaultMetadataManagerService
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.support.DefaultNodeContentRepresentation
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.assetfabric.storage.spi.support.DefaultWorkingAreaNodeRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux

@SpringBootTest
@DisplayName("the fabric storage system")
class NodeRetrieveTest: AbstractNodeTest() {

    @Configuration
    @ComponentScan("org.assetfabric.storage")
    internal class Config

    @Autowired
    private lateinit var workingAreaAdapter: WorkingAreaPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    @Autowired
    private lateinit var manager: DefaultMetadataManagerService

    @BeforeEach
    private fun init() {
        manager.reset()
    }

    @Test
    @DisplayName("should be able to retrieve an existing node from the working area")
    fun retrieveWorkingAreaNodeInNormalState() {
        val session = getSession().block()!!

        val content = DefaultNodeContentRepresentation()
        val wrepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), Path("/some/node"), NodeType.UNSTRUCTURED, null, content)

        workingAreaAdapter.createNodeRepresentation(wrepr).block()

        val retNode = session.node("/some/node").blockOptional()
        assertTrue(retNode.isPresent, "node not found")
    }

    @Test
    @DisplayName("should not be able to retrieve a deleted node from the working area")
    fun retrieveWorkingAreaNodeInDeletedState() {
        val session = getSession().block()!!

        val content = DefaultNodeContentRepresentation(NodeType.UNSTRUCTURED, mutableMapOf(), State.DELETED)
        val wrepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), Path("/some/node"), NodeType.UNSTRUCTURED, null, content)

        workingAreaAdapter.createNodeRepresentation(wrepr).block()

        val retNode = session.node("/some/node").blockOptional()
        assertFalse(retNode.isPresent, "node found")
    }

    @Test
    @DisplayName("should be able to retrieve the children of a node from the working area and the committed store")
    fun retrieveCommittedAndWorkingChildren() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()

        val content = DefaultNodeContentRepresentation(NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL)
        val wrepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), Path("/child2"), NodeType.UNSTRUCTURED, null, content)
        workingAreaAdapter.createNodeRepresentation(wrepr).block()

        val rootNode = session.rootNode().block()!!
        val rootChildren = rootNode.children().collectList().block()!!
        assertEquals(2, rootChildren.size, "root node child count mismatch")
    }

    @Test
    @DisplayName("should be able to retrieve the descendants of a node from the working area and the committed store")
    fun retrieveCommittedAndWorkingDescendants() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(
                DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL),
                DefaultRevisionedNodeRepresentation(Path("/child1/sub1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL)
        )).block()

        val content = DefaultNodeContentRepresentation(NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL)
        val wrepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), Path("/child2"), NodeType.UNSTRUCTURED, null, content)
        workingAreaAdapter.createNodeRepresentation(wrepr).block()

        val rootNode = session.rootNode().block()!!
        val rootChildren = rootNode.descendants().collectList().block()!!
        assertEquals(3, rootChildren.size, "root node child count mismatch")
    }

    @Test
    @DisplayName("should not be able to retrieve the children of a node that are deleted in the committed store")
    fun retrieveDeletedCommittedAndNormalWorkingChildren() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.DELETED))).block()

        val rootNode = session.rootNode().block()!!
        val rootChildren = rootNode.children().collectList().block()!!
        assertEquals(0, rootChildren.size, "root node child count mismatch")
    }

    @Test
    @DisplayName("should not be able to retrieve the committed children of a node that are deleted in the working area")
    fun retrieveCommittedAndDeletedWorkingChildren() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()

        val content = DefaultNodeContentRepresentation(NodeType.UNSTRUCTURED, mutableMapOf(), State.DELETED)
        val wrepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), Path("/child1"), NodeType.UNSTRUCTURED, null, content)
        workingAreaAdapter.createNodeRepresentation(wrepr).block()

        val rootNode = session.rootNode().block()!!
        val rootChildren = rootNode.children().collectList().block()!!
        assertEquals(0, rootChildren.size, "root node child count mismatch")
    }

    @Test
    @DisplayName("should be able to retrieve children of a node that are deleted in committed storage but have been created in the working area")
    fun retrieveCommittedChildrenThatAreNormalInTheWorkingArea() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.DELETED))).block()

        val content = DefaultNodeContentRepresentation(NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL)
        val wrepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), Path("/child1"), NodeType.UNSTRUCTURED, null, content)
        workingAreaAdapter.createNodeRepresentation(wrepr).block()

        val rootNode = session.rootNode().block()!!
        val rootChildren = rootNode.children().collectList().block()!!
        assertEquals(1, rootChildren.size, "root node child count mismatch")
    }

    @Test
    @DisplayName("should be able to retrieve an existing committed node")
    fun retrieveCommittedNode() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.NORMAL))).block()
        val node = session.node("/child1").blockOptional()
        assertTrue(node.isPresent, "node not found")
    }

    @Test
    @DisplayName("should not be able to retrieve a committed, deleted node")
    fun retrieveDeletedCommittedNode() {
        val session = getSession().block()!!

        dataPartitionAdapter.writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/child1"), RevisionNumber(0), NodeType.UNSTRUCTURED, mutableMapOf(), State.DELETED))).block()
        val node = session.node("/child1").blockOptional()
        assertFalse(node.isPresent, "node found")
    }



}