/* Copyright (C) 2023  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.api.utilities.common

@Suppress("UNCHECKED_CAST", "unused")
fun interface Affirm<T> {
    fun test(test: T): Boolean
    fun and(other: Affirm<in T>): Affirm<T> = Affirm { test: T -> test(test) && other.test(test) }
    fun negate(): Affirm<T> = Affirm { test: T -> !test(test) }
    fun or(other: Affirm<in T>): Affirm<T> = Affirm { test: T -> test(test) || other.test(test) }

    companion object {
        fun <T> isEqual(targetRef: Any): Affirm<T> = Affirm { `object`: T -> targetRef == `object` }
        fun <T> not(target: Affirm<in T>): Affirm<T> = target.negate() as Affirm<T>
    }
}