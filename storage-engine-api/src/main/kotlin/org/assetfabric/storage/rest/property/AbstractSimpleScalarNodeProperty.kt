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

abstract class AbstractSimpleScalarNodeProperty(): SimpleNodeProperty() {

    @JsonIgnore
    protected lateinit var _value: String

    constructor (v: String): this() {
        this.setValue(v)
    }

    fun getValue(): String = _value

    fun setValue(v: String) {
        _value = v
    }

    override fun toString(): String {
        return "AbstractScalarNodeProperty(type=${getType()}, value='$_value')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractSimpleScalarNodeProperty) return false

        if (getType() != other.getType()) return false
        if (_value != other._value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = getType().hashCode()
        result = 31 * result + _value.hashCode()
        return result
    }

}