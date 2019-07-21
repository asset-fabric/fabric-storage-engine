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

package org.assetfabric.storage.spi.support

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.JournalEntryNodeRepresentation
import org.assetfabric.storage.spi.NodeContentRepresentation

class DefaultJournalEntryNodeRepresentation(
        val sessionId: String,
        val path: Path,
        val revision: RevisionNumber,
        val nodeType: NodeType,
        val previousContent: NodeContentRepresentation?,
        val content: NodeContentRepresentation
        ): JournalEntryNodeRepresentation {

    private val addedProperties: Map<String, Any>

    private val changedProperties: Map<String, Pair<Any, Any>>

    private val removedProperties: Map<String, Any>

    init {
        val currentProperties = content.properties()
        val priorProperties = when(previousContent) {
            null -> hashMapOf()
            else -> previousContent.properties()
        }

        addedProperties = currentProperties.filter {
            !priorProperties.containsKey(it.key)
        }

        changedProperties = currentProperties.filter {
            priorProperties.containsKey(it.key) && priorProperties[it.key] != it.value
        }.map {
            it.key to Pair(priorProperties[it.key]!!, it.value)
        }.toMap()

        removedProperties = priorProperties.filter {
            !currentProperties.containsKey(it.key)
        }
    }

    override fun sessionId(): String = sessionId

    override fun name(): String = path.nodeName()

    override fun path(): Path = path

    override fun nodeType(): NodeType = nodeType

    override fun revision(): RevisionNumber = revision

    override fun priorContent(): NodeContentRepresentation? = previousContent

    override fun content(): NodeContentRepresentation = content

    override fun addedProperties(): Map<String, Any> = addedProperties

    override fun changedProperties(): Map<String, Pair<Any, Any>> = changedProperties

    override fun removedProperties(): Map<String, Any> = removedProperties
}