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

class NodeType(val type: String) {

    companion object NodeType {
        val UNSTRUCTURED = NodeType("af:unstructured:1")
    }

    private val separator = ":"

    private val namespace: String

    private val name: String

    private val version: Int

    init {
        val parts = type.split(separator)
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid node type: $type")
        }
        namespace = parts[0]
        name = parts[1]
        version = parts[2].toInt()
    }

    fun namespace(): String = this.namespace

    fun name(): String = this.name

    fun version(): Int = this.version

    override fun toString(): String = type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is org.assetfabric.storage.NodeType) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }


}