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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux

@SpringBootTest
@DisplayName("the fabric storage system")
class NodeDeleteTest: AbstractNodeTest() {

    @Configuration
    @ComponentScan("org.assetfabric.storage")
    internal class Config

    @Test
    @DisplayName("should not be able to delete the root node")
    fun deleteRootNode() {
        val session = getSession().block()!!
        val rootNode = session.rootNode().block()!!
        assertThrows(NodeDeletionException::class.java) {
            rootNode.delete()
        }
    }

    @Test
    @DisplayName("should be able to delete a working area node that has no children and no incoming references")
    fun deleteNode() {
        val session = getSession().block()!!
        session.rootNode().flatMap { node ->
            node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf())
        }.block()!!

        val node = session.node("/child1").block()!!
        node.delete().block()

        val retNode = session.node("/child1").blockOptional()
        assertFalse(retNode.isPresent, "found deleted node")
    }

    @Test
    @DisplayName("should be able to delete a node whose children are deletable")
    fun deleteNodeWithDeletableChildren() {
        val session = getSession().block()!!
        session.rootNode().flatMapMany { node ->
            Flux.concat(
                node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf()),
                node.createChild("child1/child2", NodeType.UNSTRUCTURED, mutableMapOf())
            )
        }.then().block()

        val node = session.node("/child1").block()!!
        val childNode = session.node("/child1/child2").block()!!
        assertNotNull(node)
        assertNotNull(childNode)

        node.delete().block()

        val retNode = session.node("/child1").blockOptional()
        assertFalse(retNode.isPresent, "found deleted node")
    }

    @Test
    @DisplayName("should delete a node's children when the node is deleted")
    fun deleteChildrenWithNode() {
        val session = getSession().block()!!
        session.rootNode().flatMapMany { node ->
            Flux.concat(
                    node.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf()),
                    node.createChild("child1/child2", NodeType.UNSTRUCTURED, mutableMapOf())
            )
        }.then().block()

        val node = session.node("/child1").block()!!
        val childNode = session.node("/child1/child2").block()!!
        assertNotNull(node)
        assertNotNull(childNode)

        node.delete().block()

        val retNode2 = session.node("/child1/child2").blockOptional()
        assertFalse(retNode2.isPresent, "found deleted child")
    }

    @Test
    @DisplayName("should not be able to delete a node that has incoming references")
    fun deleteReferencedNode() {
        val session = getSession().block()!!

        val root = session.rootNode().block()!!
        val node1 = root.createChild("node1", NodeType.UNSTRUCTURED, mutableMapOf()).block()!!
        root.createChild("node2", NodeType.UNSTRUCTURED, mutableMapOf("nodeRef" to NodeReference("/node1"))).block()!!

        assertThrows(NodeDeletionException::class.java) {
            node1.delete().block()
        }
    }

    @Test
    @DisplayName("should not be able to delete a node that has non-deletable children")
    fun deleteNodeWithNonDeleteableChildren() {
        val session = getSession().block()!!

        val root = session.rootNode().block()!!
        val node1 = root.createChild("node1", NodeType.UNSTRUCTURED, mutableMapOf()).block()!!

        node1.createChild("child1", NodeType.UNSTRUCTURED, mutableMapOf()).block()
        // /node1/child1 is not deletable becuase /node1/child2 has a reference to it
        node1.createChild("child2", NodeType.UNSTRUCTURED, mutableMapOf("nodeRef" to NodeReference("/node1/child1"))).block()!!

        assertThrows(NodeDeletionException::class.java) {
            node1.delete().block()
        }
    }

}