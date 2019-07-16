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
import org.assetfabric.storage.spi.NodeContentRepresentation
import org.assetfabric.storage.spi.NodeState

class DefaultNodeContentRepresentation(override var nodeType: NodeType, override var properties: Map<String, Any>, override var state: NodeState): NodeContentRepresentation {

    constructor(): this(NodeType.UNSTRUCTURED, hashMapOf(), NodeState.NORMAL)

    constructor(properties: Map<String, Any>): this(NodeType.UNSTRUCTURED, properties, NodeState.NORMAL)

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