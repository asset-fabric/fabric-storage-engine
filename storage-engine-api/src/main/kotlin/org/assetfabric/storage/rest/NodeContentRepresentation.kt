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

package org.assetfabric.storage.rest

/**
 * A RESTful representation of the contents of a node, excluding other details like the node's name, path, etc.
 */
open class NodeContentRepresentation {

    protected lateinit var _nodeType: String

    protected val _properties: MutableMap<String, NodeProperty> = mutableMapOf()

    fun setNodeType(t: String) {
        _nodeType = t
    }

    fun getNodeType(): String = _nodeType

    fun getProperties(): Map<String, NodeProperty> = _properties

    fun setProperties(m: Map<String, NodeProperty>) {
        _properties.clear()
        _properties.putAll(m)
    }

    fun setProperty(name: String, property: NodeProperty) {
        _properties.put(name, property)
    }

    fun removeProperty(name: String) {
        _properties.remove(name)
    }

}