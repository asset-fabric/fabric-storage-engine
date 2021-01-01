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
import org.assetfabric.storage.Path
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.NodeContentRepresentation
import org.assetfabric.storage.spi.metadata.NodeRepresentation

class DefaultNodeRepresentation(val path: Path, val repr: NodeContentRepresentation): NodeRepresentation, NodeContentRepresentation by repr {

    constructor(path: Path, nodeType: NodeType, state: State, properties: MutableMap<String, Any>): this(path, DefaultNodeContentRepresentation(nodeType, properties, state))

    override fun name(): String = path.nodeName()

    override fun path(): Path = path

    override fun toString(): String {
        return "NodeRepresentation(name='${name()}', path='$path', properties=${properties()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultNodeRepresentation) return false

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }


}