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

import org.assetfabric.storage.spi.NodeRepresentation
import org.bson.Document
import org.springframework.core.convert.converter.Converter

class NodeRepresentationWriteConverter: Converter<NodeRepresentation, Document> {

    private val coreWriter = NodeContentRepresentationWriteConverter()

    override fun convert(representation: NodeRepresentation): Document? {
        val doc = coreWriter.convert(representation.contentRepresentation())!!
        doc["name"] = representation.name
        doc["path"] = representation.path.toString()
        doc["parentPath"] = when(representation.path.isRoot()) {
            true -> ""
            false -> representation.path.parentPath().toString()
        }
        return doc
    }


}