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
import org.assetfabric.storage.TypedList
import org.assetfabric.storage.spi.NodeContentRepresentation
import org.bson.Document
import org.springframework.core.convert.converter.Converter

class NodeContentRepresentationWriteConverter: Converter<NodeContentRepresentation, Document> {

    override fun convert(representation: NodeContentRepresentation): Document? {
        val doc = Document()
        doc["properties"] = documentFromMap(representation.properties)
        doc["state"] = representation.state.toString()
        doc["nodeType"] = representation.nodeType.toString()
        return doc
    }

    private fun documentFromMap(map: Map<String, Any>): Document {
        val doc = Document()
        map.forEach { key, value ->
            doc.set(key, itemForValue(value))
        }
        return doc
    }

    private fun itemForValue(value: Any): Any {
        return when(value) {
            is BinaryReference -> {
                val doc = Document()
                doc.set("type", "BinaryReference")
                doc.set("path", value.path)
                doc
            }
            is TypedList -> {
                val doc = Document()
                doc.set("type", "TypedList")
                doc.set("listType", value.listType.toString())
                doc.set("values", value.values)
                doc
            }
            else -> value
        }
    }

}