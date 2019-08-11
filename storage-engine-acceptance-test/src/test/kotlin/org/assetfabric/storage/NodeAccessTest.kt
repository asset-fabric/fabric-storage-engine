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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.ByteArrayInputStream
import java.util.Date

@SpringBootTest
@DisplayName("the fabric storage system")
class NodeAccessTest: AbstractNodeTest() {

    @Configuration
    @ComponentScan("org.assetfabric.storage")
    internal class Config

    @Test
    @DisplayName("should be able to access all properties of a node")
    fun readAllNodeProperties() {
        val session = getSession().block()!!
        val propertyMap = mutableMapOf(
                "stringProp" to "string",
                "stringListProp" to TypedList(ListType.STRING, listOf("a", "b")),
                "intProp" to 3,
                "intListProp" to TypedList(ListType.INTEGER, listOf(1, 2, 3)),
                "booleanProp" to true,
                "booleanListProp" to TypedList(ListType.BOOLEAN, listOf(true, false)),
                "longProp" to 4L,
                "longListProp" to TypedList(ListType.LONG, listOf(2, 3, 4)),
                "dateProp" to Date(),
                "dateListProp" to TypedList(ListType.DATE, listOf(Date(), Date())),
                "nodeRefProp" to NodeReference("/"),
                "nodeRefListProp" to TypedList(ListType.NODE, listOf(NodeReference("/"))),
                "binProp" to InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        )
        val newNode = session.rootNode().flatMap { node ->
            node.createChild("child1", NodeType.UNSTRUCTURED, propertyMap)
        }.block()!!
        val newNodeProps = newNode.properties()

        assertEquals(propertyMap["stringProp"], newNodeProps["stringProp"])
        assertEquals(propertyMap["stringListProp"], newNodeProps["stringListProp"])
        assertEquals(propertyMap["intProp"], newNodeProps["intProp"])
        assertEquals(propertyMap["intListProp"], newNodeProps["intListProp"])
        assertEquals(propertyMap["booleanProp"], newNodeProps["booleanProp"])
        assertEquals(propertyMap["booleanListProp"], newNodeProps["booleanListProp"])
        assertEquals(propertyMap["longProp"], newNodeProps["longProp"])
        assertEquals(propertyMap["longListProp"], newNodeProps["longListProp"])
        assertEquals(propertyMap["dateProp"], newNodeProps["dateProp"])
        assertEquals(propertyMap["dateListProp"], newNodeProps["dateListProp"])
        assertEquals(propertyMap["nodeRefProp"], newNodeProps["nodeRefProp"])
        assertEquals(propertyMap["nodeRefListProp"], newNodeProps["nodeRefListProp"])
        assertTrue(newNodeProps["binProp"] is BinaryReference, "Binary property is not a reference")
    }

    @Test
    @DisplayName("should be able to retrieve the working nodes that are pointing at a given node")
    fun retrieveWorkingInverseNodes() {
        val session = getSession().block()!!

        val root = session.rootNode().block()!!
        root.createChild("node1", NodeType.UNSTRUCTURED, mutableMapOf()).block()
        root.createChild("node2", NodeType.UNSTRUCTURED, mutableMapOf("nodeRef" to NodeReference("/node1"))).block()

        val node1 = root.child("node1").block()!!
        val referringNodes = node1.referringNodes().collectList().block()!!
        assertEquals(1, referringNodes.size, "Referring node count mismatch")
    }

    @Test
    @DisplayName("should be able to retrieve the committed nodes that are pointing at a given node")
    fun retrieveCommittedInverseNodes() {
        val session = getSession().block()!!

        val root = session.rootNode().block()!!
        root.createChild("node1", NodeType.UNSTRUCTURED, mutableMapOf()).block()
        root.createChild("node2", NodeType.UNSTRUCTURED, mutableMapOf("nodeRef" to NodeReference("/node1"))).block()
        session.commit().block()

        val node1 = root.child("node1").block()!!
        val referringNodes = node1.referringNodes().collectList().block()!!
        assertEquals(1, referringNodes.size, "Referring node count mismatch")
    }

}