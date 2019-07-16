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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigInteger

@DisplayName("a revision number")
class RevisionNumberTest {

    @Test
    @DisplayName("should be created from an integer")
    fun createFromInt() {
        val rev = RevisionNumber(1)
        assertEquals(rev, 1)
        assertNotEquals(rev, 2)
    }

    @Test
    @DisplayName("should be created from a hex value")
    fun createFromHex() {
        val rev = RevisionNumber("a")
        assertEquals(rev, 10)
        assertNotEquals(rev, 11)
    }

    @Test
    @DisplayName("should be created from a BigInteger")
    fun createFromBigInt() {
        val bi = BigInteger("100")
        val rev = RevisionNumber(bi)
        assertEquals(rev, bi)
    }

    @Test
    @DisplayName("should support addition")
    fun add() {
        val rev = RevisionNumber(5)
        val rev1 = rev + 1
        assertEquals(rev1, 6)
        assertEquals(rev, 5)
    }

    @Test
    @DisplayName("should support subtraction")
    fun subtract() {
        val rev = RevisionNumber(5)
        val rev1 = rev - 1
        assertEquals(rev1, 4)
        assertEquals(rev, 5)
    }

    @Test
    @DisplayName("should be comparable to a long")
    fun compareToLong() {
        val rev = RevisionNumber(5)
        assertEquals(rev, 5L)
        assertNotEquals(rev, 6L)
    }

    @Test
    @DisplayName("should not be equal to null")
    fun compareToNull() {
        val rev = RevisionNumber(5)
        assertNotEquals(rev, null)
    }

    @Test
    @DisplayName("should be represented as a hex string")
    fun shouldHash() {
        val rev = RevisionNumber(17)
        assertEquals("11", rev.toString())
    }

    @Test
    @DisplayName("should not be equal to random object types")
    fun shouldNotEqualStuff() {
        val rev = RevisionNumber(17)
        assertNotEquals(rev, "Hi there")
    }

    @Test
    @DisplayName("should use the hash code of its value")
    fun shouldUseHash() {
        val bi = BigInteger("100")
        val rev = RevisionNumber(bi)
        assertEquals(bi.hashCode(), rev.hashCode())
    }

    @Test
    @DisplayName("should be comparable to other RevisionNumbers")
    fun shouldBeComparable() {
        val rev1 = RevisionNumber(17)
        val rev2 = RevisionNumber(5)

        assertEquals(rev1, rev1)
        assertNotEquals(rev1, rev2)

        assertEquals(1, rev1.compareTo(rev2))
        assertEquals(-1, rev2.compareTo(rev1))
        assertEquals(0, rev1.compareTo(rev1))
    }


}