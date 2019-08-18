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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        Type(value = StringProperty::class, name = "string"),
        Type(value = IntegerProperty::class, name = "int"),
        Type(value = LongProperty::class, name = "long"),
        Type(value = DoubleProperty::class, name = "double"),
        Type(value = BooleanProperty::class, name = "boolean"),
        Type(value = DateProperty::class, name = "date"),
        Type(value = NodeReferenceProperty::class, name = "node"),
        Type(value = BinaryProperty::class, name = "binary"),
        Type(value = BinaryInputProperty::class, name = "binaryinput"),
        Type(value = ParameterizedNodeReferenceProperty::class, name = "paramnode"),

        Type(value = StringListProperty::class, name = "string[]"),
        Type(value = IntegerListProperty::class, name = "int[]"),
        Type(value = LongListProperty::class, name = "long[]"),
        Type(value = DoubleListProperty::class, name = "double[]"),
        Type(value = BooleanListProperty::class, name = "boolean[]"),
        Type(value = DateListProperty::class, name = "date[]"),
        Type(value = NodeReferenceListProperty::class, name = "node[]"),
        Type(value = BinaryListProperty::class, name = "binary[]"),
        Type(value = ParameterizedNodeReferenceListProperty::class, name = "paramnode[]")
        )
abstract class NodeProperty {

    @JsonIgnore
    abstract fun getType(): NodePropertyType

}