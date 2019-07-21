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
import org.assetfabric.storage.RevisionNumber
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("a journal entry read converter")
class JournalEntryNodeRepresentationReadConverterTest {

    @Test
    @DisplayName("should be able to convert a journal entry document")
    fun testConvert() {
        val converter = JournalEntryNodeRepresentationReadConverter()

        val currentRep = Document()
        currentRep["name"] = "node name"
        currentRep["path"] = "/node/path"
        currentRep["nodeType"] = NodeType.UNSTRUCTURED.toString()
        currentRep["revision"] = "3fe"
        currentRep["state"] = "NORMAL"

        val props = Document()
        currentRep["properties"] = props


        val priorRep = Document()
        priorRep["name"] = "node name"
        priorRep["path"] = "/node/path"
        priorRep["nodeType"] = NodeType.UNSTRUCTURED.toString()
        priorRep["revision"] = "3fe"
        priorRep["state"] = "NORMAL"

        val priorProps = Document()
        priorRep["properties"] = priorProps

        val journalDoc = Document()
        journalDoc["sessionId"] = "sessionID"
        journalDoc["path"] = "/node/path"
        journalDoc["nodeType"] = NodeType.UNSTRUCTURED.toString()
        journalDoc["revision"] = "3fe"
        journalDoc["priorContent"] = priorRep
        journalDoc["currentContent"] = currentRep

        val repr = converter.convert(journalDoc)
        assertEquals("sessionID", repr.sessionId())
        assertEquals("path", repr.name())
        assertEquals("/node/path", repr.path().toString())
        assertEquals(RevisionNumber("3fe"), repr.revision())
        assertNotNull(repr.priorContent())
        assertNotNull(repr.content())

    }

}