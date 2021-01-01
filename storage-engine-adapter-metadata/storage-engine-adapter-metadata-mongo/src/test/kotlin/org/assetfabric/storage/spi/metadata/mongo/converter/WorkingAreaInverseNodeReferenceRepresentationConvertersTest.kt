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
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.support.DefaultInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultWorkingAreaInverseNodeReferenceRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("the working area inverse node representation converters")
class WorkingAreaInverseNodeReferenceRepresentationConvertersTest {

    @Test
    @DisplayName("should be able to convert an inverse node reference to and from a document")
    fun testConvert() {
        val readConverter = WorkingAreaInverseNodeReferenceRepresentationReadConverter()
        val writeConverter = WorkingAreaInverseNodeReferenceRepresentationWriteConverter()

        val reference = DefaultInverseNodeReferenceRepresentation(Path("/to/node"), Path("/from/node"), State.NORMAL)
        val waReference = DefaultWorkingAreaInverseNodeReferenceRepresentation("session123", reference)
        val retReference = readConverter.convert(writeConverter.convert(waReference))

        assertEquals(waReference.nodePath(), retReference.nodePath(), "node path mismatch")
        assertEquals(waReference.referringNodePath(), retReference.referringNodePath(), "referring node path mismatch")
        assertEquals(waReference.state(), retReference.state(), "state mismatch")
        assertEquals(waReference.sessionId(), retReference.sessionId(), "sessionID mismatch")
    }

}