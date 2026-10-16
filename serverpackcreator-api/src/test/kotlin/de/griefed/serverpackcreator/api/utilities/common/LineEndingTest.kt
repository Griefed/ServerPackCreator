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
package de.griefed.serverpackcreator.api.utilities.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins [withLineEndings], the one place ServerPackCreator decides how a generated file's lines end.
 *
 * It exists because the obvious spelling of the same intent — deleting `\r` — is not a conversion. It
 * is right for CRLF input and wrong for every other kind, silently, and the file it produces still
 * parses.
 */
internal class LineEndingTest {

    /** CRLF in, LF out: the ordinary case, and the only one deleting `\r` ever handled. */
    @Test
    fun windowsTextIsConvertedToUnixText() {
        Assertions.assertEquals("one\ntwo\n", "one\r\ntwo\r\n".withLineEndings(LineEnding.LF))
    }

    /**
     * Lone carriage returns become line feeds rather than disappearing.
     *
     * Deleting them joins the lines instead of separating them, which is the defect this function was
     * written to make unrepresentable.
     */
    @Test
    fun loneCarriageReturnsBecomeLineFeedsRatherThanVanishing() {
        Assertions.assertEquals("one\ntwo\n", "one\rtwo\r".withLineEndings(LineEnding.LF))
    }

    /** A file mixing conventions still comes out using exactly one of them. */
    @Test
    fun mixedConventionsAreNormalisedToOne() {
        Assertions.assertEquals(
            "one\ntwo\nthree\n",
            "one\r\ntwo\rthree\n".withLineEndings(LineEnding.LF)
        )
    }

    /** LF in, CRLF out, for the one reader that wants it. */
    @Test
    fun unixTextIsConvertedToWindowsText() {
        Assertions.assertEquals("one\r\ntwo\r\n", "one\ntwo\n".withLineEndings(LineEnding.CRLF))
    }

    /**
     * Converting text that is already CRLF leaves it alone.
     *
     * Expanding without normalising first turns every `\r\n` into `\r\r\n`, which is the mirror of the
     * deletion bug: plausible-looking output that no longer says what it did.
     */
    @Test
    fun alreadyWindowsTextIsNotDoubled() {
        Assertions.assertEquals("one\r\ntwo\r\n", "one\r\ntwo\r\n".withLineEndings(LineEnding.CRLF))
    }

    /** Converting twice changes nothing the first conversion did not. */
    @Test
    fun theConversionIsIdempotent() {
        val once = "one\r\ntwo\rthree\n".withLineEndings(LineEnding.CRLF)

        Assertions.assertEquals(once, once.withLineEndings(LineEnding.CRLF))
    }

    /** Text with no line break at all is handed back unchanged, under either convention. */
    @Test
    fun textWithoutLineBreaksIsUnchanged() {
        Assertions.assertEquals("JAVA=java", "JAVA=java".withLineEndings(LineEnding.LF))
        Assertions.assertEquals("JAVA=java", "JAVA=java".withLineEndings(LineEnding.CRLF))
    }
}
