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
import org.assetfabric.storage.spi.WorkingAreaNodeRepresentation

class DefaultWorkingAreaNodeRepresentation(
        val sessionId: String,
        val path: Path,
        val revision: RevisionNumber,
        val nodeType: NodeType,
        val permanentRepresentation: NodeContentRepresentation?,
        val workingAreaRepresentation: NodeContentRepresentation): WorkingAreaNodeRepresentation {

    override fun sessionId(): String = sessionId

    override fun name(): String = path.nodeName()

    override fun path(): Path = path

    override fun nodeType(): NodeType = nodeType

    override fun revision(): RevisionNumber = revision

    override fun permanentRepresentation(): NodeContentRepresentation? = permanentRepresentation

    override fun workingAreaRepresentation(): NodeContentRepresentation = workingAreaRepresentation

    override fun effectiveNodeRepresentation(): NodeRepresentation {
        val props = mutableMapOf<String, Any>()
        if (permanentRepresentation != null) {
            permanentRepresentation.properties().forEach {
                props[it.key] = it.value
            }
        }
        workingAreaRepresentation.properties().forEach {
            props[it.key] = it.value
        }

        return DefaultNodeRepresentation(path, nodeType, workingAreaRepresentation.state(), props)
    }

}