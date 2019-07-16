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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("a node type")
class NodeTypeTest {

    @Test
    @DisplayName("should correctly parse a valid node type string")
    fun testValidNodeType() {
        val nt = NodeType("af:unstructured:1")
        assertEquals("af", nt.namespace(), "namespace mismatch")
        assertEquals("unstructured", nt.name(), "name mismatch")
        assertEquals(1, nt.version(), "version mismatch")
    }

    @Test
    @DisplayName("should not parse an invalid node type string")
    fun testInvalidNodeType() {
        assertThrows(IllegalArgumentException::class.java) {
            NodeType("whatever:4")
        }
    }

}