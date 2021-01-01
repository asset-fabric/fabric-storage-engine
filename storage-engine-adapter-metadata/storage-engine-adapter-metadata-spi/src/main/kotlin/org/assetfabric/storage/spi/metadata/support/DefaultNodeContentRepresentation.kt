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

package org.assetfabric.storage.spi.metadata.support

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.NodeContentRepresentation

class DefaultNodeContentRepresentation(private val nodeType: NodeType, private val properties: MutableMap<String, Any>, state: State): NodeContentRepresentation {

    var currentState: State

    var currentProperties: MutableMap<String, Any>

    constructor(): this(NodeType.UNSTRUCTURED, hashMapOf(), State.NORMAL)

    init {
        currentState = state
        currentProperties = properties
    }

    constructor(properties: MutableMap<String, Any>): this(NodeType.UNSTRUCTURED, properties, State.NORMAL)

    override fun state(): State = currentState

    override fun setState(s: State) {
        currentState = s
    }

    override fun nodeType(): NodeType = nodeType

    override fun properties(): MutableMap<String, Any> = currentProperties

    override fun setProperties(props: MutableMap<String, Any>) {
        currentProperties = props
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultNodeContentRepresentation) return false

        if (properties != other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        return properties.hashCode()
    }


}