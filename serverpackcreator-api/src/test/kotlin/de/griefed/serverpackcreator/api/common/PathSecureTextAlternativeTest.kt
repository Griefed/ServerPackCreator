/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins that trimming a trailing dot or space trims only the trailing one.
 *
 * Windows rejects a file name ending in `.` or ` `, which is what the loop is for. It removed the
 * offending character with `replace`, which removes **every** occurrence in the string — so a version
 * number lost its separators. Published API with no call site left inside this repo, so an embedder is
 * the only one who can hit it.
 */
internal class PathSecureTextAlternativeTest {

    @Test
    fun aTrailingDotIsTrimmedWithoutTouchingTheOthers() {
        Assertions.assertEquals("My Pack v1.2", StringUtilities.pathSecureTextAlternative("My Pack v1.2."))
    }

    @Test
    fun aTrailingSpaceIsTrimmedWithoutTouchingTheOthers() {
        Assertions.assertEquals("My Pack v1", StringUtilities.pathSecureTextAlternative("My Pack v1 "))
    }

    @Test
    fun severalTrailingOffendersAreAllTrimmed() {
        Assertions.assertEquals("All the Mods 9", StringUtilities.pathSecureTextAlternative("All the Mods 9. . "))
    }

    @Test
    fun theIllegalCharactersAreStillRemovedAndSeparatorsStillAreNot() {
        // Its own KDoc is explicit that, unlike pathSecureText, this does NOT strip / or \.
        Assertions.assertEquals("ab/c\\d", StringUtilities.pathSecureTextAlternative("a<b>/c\\d"))
    }
}
