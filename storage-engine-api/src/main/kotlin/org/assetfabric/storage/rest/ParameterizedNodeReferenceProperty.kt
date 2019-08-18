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

class ParameterizedNodeReferenceProperty(): AbstractScalarNodeProperty() {

    private lateinit var properties: Map<String, NodeProperty>

    constructor(path: String, properties: Map<String, NodeProperty>): this() {
        this.setValue(path)
        this.setProperties(properties)
    }

    override fun getType(): NodePropertyType = NodePropertyType.PARAMETERIZED_NODE

    fun getProperties(): Map<String, NodeProperty> = properties

    fun setProperties(props: Map<String, NodeProperty>) {
        properties = props
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParameterizedNodeReferenceProperty) return false
        if (!super.equals(other)) return false

        if (properties != other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }


}