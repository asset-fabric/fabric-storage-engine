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
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("a revisioned node write converter")
class RevisionedNodeRepresentationWriteConverterTest {

    private val converter = RevisionedNodeRepresentationWriteConverter()

    @Test
    @DisplayName("should be able to convert a revisioned node representation to a document")
    fun testConvertNodeToDocument() {
        val node = DefaultRevisionedNodeRepresentation(Path("/test/node"), RevisionNumber(1), NodeType.UNSTRUCTURED, hashMapOf(), State.NORMAL)
        val doc = converter.convert(node)
        Assertions.assertEquals(node.name(), doc.getString("name"))
        Assertions.assertEquals(node.path().toString(), doc.getString("path"))
        Assertions.assertEquals(node.revision.toString(), doc.getString("revision"))

    }

}