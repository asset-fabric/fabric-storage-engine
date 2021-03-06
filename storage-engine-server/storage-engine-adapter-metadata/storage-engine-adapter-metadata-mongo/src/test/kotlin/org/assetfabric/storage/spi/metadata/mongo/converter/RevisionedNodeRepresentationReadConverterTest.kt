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
import org.bson.Document
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("a revisioned node read converter")
class RevisionedNodeRepresentationReadConverterTest {

    private val converter = RevisionedNodeRepresentationReadConverter()

    @Test
    @DisplayName("should be able to convert a document to a revisioned node")
    fun testConvertDocument() {

        val d = Document()
        d["path"] = "/node/path"
        d["nodeType"] = NodeType.UNSTRUCTURED.toString()
        d["revision"] = "3fe"
        d["state"] = "NORMAL"

        val props = Document()
        d["properties"] = props

        val repr = converter.convert(d)
        Assertions.assertEquals(d.getString("path"), repr.path().toString())
        Assertions.assertEquals(d.getString("revision"), repr.revision().toString())

    }

}