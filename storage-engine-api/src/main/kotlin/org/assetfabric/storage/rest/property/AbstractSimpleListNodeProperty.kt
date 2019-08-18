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

package org.assetfabric.storage.rest.property

import com.fasterxml.jackson.annotation.JsonIgnore

abstract class AbstractSimpleListNodeProperty<T>(): SimpleNodeProperty() {

    @JsonIgnore
    protected lateinit var vals: List<T>

    constructor(v: List<T>): this() {
        this.setValues(v)
    }

    fun getValues(): List<T> = vals

    fun setValues(list: List<T>) {
        vals = list
    }

    override fun toString(): String {
        return "AbstractListNodeProperty(type=${getType()}, values='$vals')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractSimpleListNodeProperty<*>) return false

        if (getType() != other.getType()) return false
        if (vals != other.vals) return false

        return true
    }

    override fun hashCode(): Int {
        var result = getType().hashCode()
        result = 31 * result + vals.hashCode()
        return result
    }

}