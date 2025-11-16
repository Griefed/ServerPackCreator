/* Copyright (C) 2025 Griefed
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

import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Utility-class revolving around Booleans.
 *
 * @author Griefed
 */
@Suppress("unused", "KDocUnresolvedReference")
class BooleanUtilities {
    companion object {
        private val log by lazy { cachedLoggerOf(BooleanUtilities::class.java) }

        private val yYeEsS = "[Yy][Ee][Ss]".toRegex()
        private val yY = "[Yy]".toRegex()
        private val one = "1".toRegex()
        private val nNoO = "[Nn][Oo]]".toRegex()
        private val nN = "[Nn]".toRegex()
        private val zero = "0".toRegex()

        /**
         * Prompts the user to enter values which will then be converted to booleans, either `TRUE `
         * or `FALSE`. This prevents any non-boolean values from being written to the new
         * configuration file.
         *
         * @param scanner Used for reading the users input.
         * @return True or False, depending on user input.
         * @author Griefed
         */
        fun readBoolean(): Boolean {
            printBoolMenu()
            return convert(readln())
        }

        /**
         * Print a small help text to tell the user which values are accepted as `true` and which
         * values are accepted as `false`.
         *
         * @author Griefed
         */
        private fun printBoolMenu() = println("True: 1, Yes, Y, true -|- False: 0, No, N, false")

        /**
         * Converts various strings to booleans, by using regex, to allow for more variations in input.
         *
         * Converted to `TRUE` are:
         * *  `[Tt]rue`
         * * `1`
         * * `[Yy]es`
         * * `[Yy]`
         * * Language Key `cli.input.true`
         * * Language Key `cli.input.yes`
         * * Language Key `cli.input.yes.short`
         *
         * Converted to `FALSE` are:
         * * `[Ff]alse`
         * * `0`
         * * `[Nn]o`
         * * `[Nn]`
         * * Language Key `cli.input.false`
         * * Language Key `cli.input.no`
         * * Language Key `cli.input.no.short`
         *
         * @param stringBoolean The string which should be converted to boolean if it matches certain
         * patterns.
         * @return Returns the corresponding boolean if match with pattern was found. If no match is
         * found, assume and return false.
         * @author Griefed
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun convert(stringBoolean: String) =
            if (stringBoolean.matches(one)
                || stringBoolean.matches(yYeEsS)
                || stringBoolean.matches(yY)
                || stringBoolean.equals("true", ignoreCase = true)
            ) {
                true
            } else if (stringBoolean.matches(zero)
                || stringBoolean.matches(nNoO)
                || stringBoolean.matches(nN)
                || stringBoolean.equals("false", ignoreCase = true)
            ) {
                false
            } else {
                log.warn { "Warning. Couldn't parse boolean. Assuming false." }
                false
            }
    }
}