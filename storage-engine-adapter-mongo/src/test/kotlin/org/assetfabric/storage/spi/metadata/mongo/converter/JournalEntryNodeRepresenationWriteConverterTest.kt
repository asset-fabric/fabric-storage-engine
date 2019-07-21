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

import org.assetfabric.storage.BinaryReference
import org.assetfabric.storage.ListType
import org.assetfabric.storage.NodeReference
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.TypedList
import org.assetfabric.storage.spi.support.DefaultJournalEntryNodeRepresentation
import org.assetfabric.storage.spi.support.DefaultNodeContentRepresentation
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("a journal entry write converter")
class JournalEntryNodeRepresenationWriteConverterTest {

    @Test
    @DisplayName("should be able to write a journal entry")
    fun testWriteJournalEntry() {
        val writer = JournalEntryNodeRepresentationWriteConverter()
        val priorNodeContent = DefaultNodeContentRepresentation()

        priorNodeContent.properties["stringProp"] = "testing"
        priorNodeContent.properties["intListProp"] = TypedList(ListType.INTEGER, listOf(1, 2, 3, 4))
        priorNodeContent.properties["nodeRef"] = NodeReference("/node2")
        priorNodeContent.properties["binRef"] = BinaryReference("some/binary/path")

        val newNodeContent = DefaultNodeContentRepresentation()
        newNodeContent.properties["booleanProp"] = true
        newNodeContent.properties["stringProp"] = "testing again"

        val journalEntry = DefaultJournalEntryNodeRepresentation("testSession", Path("/node1"), RevisionNumber(1), NodeType.UNSTRUCTURED, priorNodeContent, newNodeContent)

        val doc = writer.convert(journalEntry)
        assertEquals(doc.getString("sessionId"), "testSession")
        assertEquals(doc.getString("path"), "/node1")
        assertEquals(doc.get("revision"), "1")

        val priorContent = doc["priorContent"] as Document
        val priorProps = priorContent["properties"] as Document
        assertEquals(priorNodeContent.properties["stringProp"], priorProps["stringProp"])
        assertEquals((priorNodeContent.properties["intListProp"] as TypedList).values, (priorProps["intListProp"] as Document)["values"])
        assertEquals((priorNodeContent.properties["nodeRef"] as NodeReference).path, (priorProps["nodeRef"] as Document)["path"])
        assertEquals((priorNodeContent.properties["binRef"] as BinaryReference).path, (priorProps["binRef"] as Document)["path"])

        val currentContent = doc["currentContent"] as Document
        val currentProps = currentContent["properties"] as Document
        assertEquals(newNodeContent.properties["booleanProp"], currentProps["booleanProp"])
        assertEquals(newNodeContent.properties["stringProp"], currentProps["stringProp"])

    }

}