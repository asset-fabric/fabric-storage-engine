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
 * Represents the path of a node in the repository.
 */
class Path(val path: String): Comparable<Path> {

    init {
        if (path.trim() == "") {
            throw IllegalArgumentException("Invalid path; is empty")
        }
    }

    companion object PathConst {
        private val pathSeparator = "/"
    }

    fun isRoot() = path == "/"

    fun nodeName(): String = path.split(pathSeparator).last()

    fun parentPath(): Path {
        return when(isRoot()) {
            true -> throw RuntimeException("Cannot return parent path of root path")
            false -> {
                val parentPath = path.split(pathSeparator).dropLast(1).joinToString(pathSeparator)
                when(parentPath) {
                    "" -> Path("/")
                    else -> Path(parentPath)
                }
            }
        }
    }

    fun childPath(childName: String): Path {
        return when(isRoot()) {
            true -> Path("$pathSeparator$childName")
            else -> Path("$path$pathSeparator$childName")
        }
    }

    override fun toString(): String = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Path) return false

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun compareTo(other: Path): Int = path.compareTo(other.path)
}