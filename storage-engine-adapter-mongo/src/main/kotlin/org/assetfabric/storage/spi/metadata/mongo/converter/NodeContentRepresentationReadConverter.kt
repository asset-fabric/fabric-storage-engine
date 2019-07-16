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
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.TypedList
import org.assetfabric.storage.spi.NodeContentRepresentation
import org.assetfabric.storage.spi.NodeState
import org.assetfabric.storage.spi.support.DefaultNodeContentRepresentation
import org.bson.Document
import org.springframework.core.convert.converter.Converter

class NodeContentRepresentationReadConverter: Converter<Document, NodeContentRepresentation> {

    override fun convert(doc: Document): NodeContentRepresentation? {
        val props = mapFromDocument(doc.get("properties", Document::class.java))
        val state = NodeState.valueOf(doc.getString("state"))
        val nodeType = NodeType(doc.getString("nodeType"))
        return DefaultNodeContentRepresentation(nodeType, props, state)
    }

    /**
     * Produces an immutable map from a [Document].
     * @param doc the document from which the map should be produced
     */
    private fun mapFromDocument(doc: Document?): Map<String, Any> {
        return when (doc) {
            null -> HashMap()
            else -> {
                doc.map {
                    val entryVal: Any = it.value
                    val value = when (entryVal) {
                        is Document -> objectFromDocument(entryVal)
                        else -> it.value
                    }
                    it.key to value
                }.toMap()
            }
        }
    }

    /**
     * Produces one of the specialized data types, i.e. a [BinaryReference] or
     * [NodeReference], from a [Document] object.
     * @param doc the document from which the data should be produced
     */
    private fun objectFromDocument(doc: Document): Any {
        val type = doc.getString("type")
        return when (type) {
            "BinaryReference" -> BinaryReference(doc.getString("path"))
            "NodeReference" -> {
                val sr: String? = doc["snapshotRevision"] as String?
                val revision: RevisionNumber? = when (sr) {
                    null -> null
                    else -> RevisionNumber(sr)
                }
                NodeReference(doc.getString("path"), revision)
            }
            "TypedList" -> {
                val listType = doc.getString("listType")
                val values = doc.get("values")
                TypedList(ListType.valueOf(listType), values as List<Any>)
            }
            else -> throw RuntimeException("Unknown property $doc")
        }
    }

}