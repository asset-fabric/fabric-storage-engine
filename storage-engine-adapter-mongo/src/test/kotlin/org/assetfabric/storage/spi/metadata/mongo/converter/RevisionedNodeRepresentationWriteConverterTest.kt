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

package org.assetfabric.storage.spi.metadata.mongo.converter

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.NodeState
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

@DisplayName("a revisioned node write converter")
class RevisionedNodeRepresentationWriteConverterTest {

    private val converter = RevisionedNodeRepresentationWriteConverter()

    @Test
    @DisplayName("should be able to convert a revisioned node representation to a document")
    fun testConvertNodeToDocument() {
        val node = DefaultRevisionedNodeRepresentation("node", Path("/test/node"), RevisionNumber(1), NodeType.UNSTRUCTURED, hashMapOf(), NodeState.NORMAL)
        val doc = converter.convert(node)
        when (doc) {
            null -> fail("Null document")
            else -> {
                assertEquals(node.name, doc.getString("name"))
                assertEquals(node.path.toString(), doc.getString("path"))
                assertEquals(node.revision.toString(), doc.getString("revision"))
            }
        }

    }

}