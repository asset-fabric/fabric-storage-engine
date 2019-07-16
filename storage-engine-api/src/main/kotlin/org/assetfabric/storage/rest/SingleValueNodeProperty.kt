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

class SingleValueNodeProperty(): NodeProperty() {

    private lateinit var value: String

    constructor (t: NodePropertyType, v: String): this() {
        this.setType(t)
        this.setValue(v)
    }

    fun getValue(): String = value

    fun setValue(v: String) {
        value = v
    }

    override fun toString(): String {
        return "SingleValueNodeProperty(type=$propertyType, value='$value')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SingleValueNodeProperty) return false

        if (propertyType != other.propertyType) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = propertyType.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

}