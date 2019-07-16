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

package org.assetfabric.storage.spi

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber

interface WorkingAreaNodeRepresentation {

    var sessionId: String

    var name: String

    var path: Path

    var nodeType: NodeType

    var revision: RevisionNumber

    var permanentRepresentation: NodeContentRepresentation?

    var workingAreaRepresentation: NodeContentRepresentation

    /**
     * Returns a representation of this node that factors in both the
     * node's existing properties as well as the current
     * working changes on the node in the working area.
     */
    fun effectiveNodeRepresentation(): NodeRepresentation

}