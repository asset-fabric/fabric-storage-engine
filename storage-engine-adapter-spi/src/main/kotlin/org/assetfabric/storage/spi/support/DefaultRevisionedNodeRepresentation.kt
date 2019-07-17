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
import org.assetfabric.storage.spi.NodeContentRepresentation
import org.assetfabric.storage.spi.NodeRepresentation
import org.assetfabric.storage.spi.NodeState
import org.assetfabric.storage.spi.RevisionedNodeRepresentation

class DefaultRevisionedNodeRepresentation(override var name: String, override var path: Path, override var revision: RevisionNumber, override var nodeType: NodeType, override var properties: MutableMap<String, Any>, override var state: NodeState): RevisionedNodeRepresentation {

    constructor(revision: RevisionNumber, nodeRepr: NodeRepresentation): this(nodeRepr.name, nodeRepr.path, revision, NodeType.UNSTRUCTURED, nodeRepr.properties, NodeState.NORMAL)

    override fun contentRepresentation(): NodeContentRepresentation {
        return DefaultNodeContentRepresentation(properties)
    }

    override fun getNodeRepresentation(): NodeRepresentation {
        return DefaultNodeRepresentation(name, path, nodeType, properties, state)
    }
}