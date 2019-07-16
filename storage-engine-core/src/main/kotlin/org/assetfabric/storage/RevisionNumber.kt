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

import java.math.BigInteger

/**
 * Revision numbers are used to track the verion numbers for [Session]s, [Node]s, and the repository.
 * They are represented as hex strings.
 */
class RevisionNumber(private val number: BigInteger): Comparable<RevisionNumber> {

    constructor(hexString: String): this(BigInteger(hexString, 16))

    constructor(i: Int): this(BigInteger(i.toString(), 10))

    operator fun plus(i: Int): RevisionNumber = RevisionNumber(number.add(BigInteger(i.toString())))

    operator fun minus(i: Int): RevisionNumber = RevisionNumber(number.minus(BigInteger(i.toString())))

    override fun toString(): String = number.toString(16)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is RevisionNumber -> number == other.number
            is Int -> number.intValueExact() == other
            is Long -> number.longValueExact() == other
            is BigInteger -> number == other
            else -> false
        }
    }

    override fun hashCode(): Int {
        return number.hashCode()
    }

    override fun compareTo(other: RevisionNumber): Int = number.compareTo(other.number)

}