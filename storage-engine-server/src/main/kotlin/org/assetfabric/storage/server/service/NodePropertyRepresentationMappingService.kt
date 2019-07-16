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

package org.assetfabric.storage.server.service

import org.assetfabric.storage.rest.NodeProperty

/**
 * Converts between the externally-visible REST representation and the internally-used,
 * map-based representation of a node's properties.
 */
interface NodePropertyRepresentationMappingService {

    /**
     * Parses the given RESTful node representation into properties that
     * can be understood by the internal metadata system.
     * @param map the RESTful node representation
     * @return a map of node properties
     */
    fun getInternalPropertyRepresentation(map: Map<String, NodeProperty>): MutableMap<String, Any>

    /**
     * Converts the internal node property format into the external RESTful property format.
     * @param map the internal properties representation
     * @return the external property representation
     */
    fun getExternalPropertyRepresentation(map: Map<String, Any>): MutableMap<String, NodeProperty>



}