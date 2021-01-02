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

package org.assetfabric.storage.spi.metadata

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber

interface JournalEntryNodeRepresentation {

    /**
     * The ID of the session that created this journal entry.
     */
    fun sessionId(): String

    /**
     * The name of the node contained in this journal entry.
     */
    fun name(): String

    /**
     * The path of the node contained in this journal entry.
     */
    fun path(): Path

    /**
     * The type of the node contained in this journal entry.
     */
    fun nodeType(): NodeType

    /**
     * The revision being targeted by this journal entry.
     */
    fun revision(): RevisionNumber

    /**
     * The prior content of the given node, if applicable
     */
    fun priorContent(): NodeContentRepresentation?

    /**
     * The contents of the node contained in this journal entry.
     */
    fun content(): NodeContentRepresentation

    /**
     * The properties added to the node in this journal entry.
     */
    fun addedProperties(): Map<String, Any>

    /**
     * The properties modified in the node in this journal entry.
     */
    fun changedProperties(): Map<String, Pair<Any, Any>>

    /**
     * The properties that were removed from the node in this journal entry.
     */
    fun removedProperties(): Map<String, Any>

}