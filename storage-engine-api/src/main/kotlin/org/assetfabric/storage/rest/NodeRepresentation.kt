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
 * A RESTful representation of a Node in the repository.
 */
class NodeRepresentation: NodeContentRepresentation() {

    private lateinit var name: String

    private lateinit var path: String

    fun getName(): String = name

    fun setName(n: String) {
        name = n
    }

    fun getPath(): String = path

    fun setPath(p: String) {
        path = p
    }

    override fun toString(): String {
        return "NodeRepresentation(name=$name, path=$path, properties=$_properties)"
    }

}