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

package org.assetfabric.storage.spi.search

import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.State

/**
 * An entry to be added to an Asset Fabric search index.
 * @param path the path of the node to index
 * @param revision the revision of the node to index
 * @param state the state of the node to index
 * @param currentProperties the properties currently set on the node
 * @param priorProperties the properties that were replaced or removed from the node since the prior revision
 */
class SearchEntry(
        val path: Path,
        val nodeType: NodeType,
        val revision: RevisionNumber,
        val state: State,
        val currentProperties: Map<String, Any>,
        val priorProperties: Map<String, Any>?)