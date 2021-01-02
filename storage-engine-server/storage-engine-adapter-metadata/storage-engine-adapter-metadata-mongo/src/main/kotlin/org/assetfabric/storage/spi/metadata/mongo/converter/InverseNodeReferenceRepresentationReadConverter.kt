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
import org.assetfabric.storage.spi.metadata.InverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultInverseNodeReferenceRepresentation
import org.bson.Document
import org.springframework.core.convert.converter.Converter

class InverseNodeReferenceRepresentationReadConverter: Converter<Document, InverseNodeReferenceRepresentation> {

    override fun convert(doc: Document): InverseNodeReferenceRepresentation {
        val nodePath = doc.getString("nodePath")
        val referringNodePath = doc.getString("referringNodePath")
        val state = doc.getString("state")
        return DefaultInverseNodeReferenceRepresentation(Path(nodePath), Path(referringNodePath), State.valueOf(state))
    }

}