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

import org.assetfabric.storage.Path
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.metadata.InverseNodeReferenceRepresentation

class DefaultInverseNodeReferenceRepresentation(val nodePath: Path, val referringNodePath: Path, val state: State): InverseNodeReferenceRepresentation {

    override fun nodePath(): Path = nodePath

    override fun referringNodePath(): Path = referringNodePath

    override fun state(): State = state

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultInverseNodeReferenceRepresentation) return false

        if (nodePath != other.nodePath) return false
        if (referringNodePath != other.referringNodePath) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodePath.hashCode()
        result = 31 * result + referringNodePath.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }


}