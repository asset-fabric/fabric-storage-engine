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

import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.support.DefaultCommittedInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultInverseNodeReferenceRepresentation

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("the committed inverse node representation converters")
class CommittedInverseNodeReferenceRepresentationConverterTests {

    @Test
    @DisplayName("should be able to convert an inverse node reference to and from a document")
    fun testConvert() {
        val readConverter = CommittedInverseNodeReferenceRepresentationReadConverter()
        val writeConverter = CommittedInverseNodeReferenceRepresentationWriteConverter()

        val reference = DefaultInverseNodeReferenceRepresentation(Path("/to/node"), Path("/from/node"), State.NORMAL)
        val cReference = DefaultCommittedInverseNodeReferenceRepresentation(RevisionNumber(2), reference)
        val retReference = readConverter.convert(writeConverter.convert(cReference))

        Assertions.assertEquals(cReference.nodePath(), retReference.nodePath(), "node path mismatch")
        Assertions.assertEquals(cReference.referringNodePath(), retReference.referringNodePath(), "referring node path mismatch")
        Assertions.assertEquals(cReference.state(), retReference.state(), "state mismatch")
        Assertions.assertEquals(cReference.revision(), retReference.revision(), "revision mismatch")
    }

}