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

package org.assetfabric.storage.rest.property.types

import org.assetfabric.storage.rest.property.AbstractSimpleListNodeProperty
import org.assetfabric.storage.rest.property.NodePropertyType

class DoubleListProperty(): AbstractSimpleListNodeProperty<String>() {

    constructor(vararg vals: Double): this() {
        this.setValues(vals.map { it.toString() })
    }

    override fun getType(): NodePropertyType = NodePropertyType.DOUBLE
}