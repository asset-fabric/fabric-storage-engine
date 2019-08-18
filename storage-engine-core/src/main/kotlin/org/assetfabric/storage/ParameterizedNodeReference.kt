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

package org.assetfabric.storage

/**
 * A [NodeReference] that contains properties which provide additional information about the node relationship.
 * Most property types are supported, but NOT node reference properties or binary properties.
 */
class ParameterizedNodeReference(path: String, val properties: Map<String, Any>): NodeReference(path) {

    init {
        // don't allow complex properties
        val complexMap = properties.filterValues {
            it is BinaryReference
                    || it is ParameterizedNodeReference
                    || it is NodeReference
                    || (it is TypedList &&
                    (it.listType == ListType.PARAMETERIZED_NODE || it.listType == ListType.NODE))
        }
        if (complexMap.isNotEmpty()) {
            throw RuntimeException("Parameterized node references cannot contain complex properties")
        }

    }

}