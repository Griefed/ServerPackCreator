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

import de.griefed.serverpackcreator.api.ApiProperties
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Characterization tests for [WebUtilities.hasteBinPreChecks], written before it stops reading whole files
 * into memory to measure them.
 *
 * The check has **two** independent limits — 10 MB of *bytes* and 400,000 *characters* — and they are not
 * the same measurement. UTF-8 spends up to four bytes on one character, so a file can be comfortably over
 * the byte figure while being well under the character one. That is the case any "just use the file size"
 * shortcut gets wrong, and it is pinned here explicitly, because the whole point of the change is to stop
 * materialising the file while keeping the answer identical.
 */
internal class HasteBinPreChecksTest {

    private fun webUtilities() = WebUtilities(mockk<ApiProperties>(relaxed = true))

    /** A file of [count] repetitions of [char], written without holding the whole thing in memory. */
    private fun fileOf(dir: File, name: String, char: Char, count: Int): File {
        val file = File(dir, name)
        file.bufferedWriter().use { writer ->
            val chunk = CharArray(8192) { char }
            var written = 0
            while (written < count) {
                val next = minOf(chunk.size, count - written)
                writer.write(chunk, 0, next)
                written += next
            }
        }
        return file
    }

    /** An ordinary log-sized file passes. */
    @Test
    fun anOrdinaryFileIsAccepted(@TempDir tempDir: File) {
        val file = fileOf(tempDir, "small.log", 'a', 1_000)

        Assertions.assertTrue(webUtilities().hasteBinPreChecks(file))
    }

    /**
     * Pins the character limit: 400,000 is the boundary, so exactly 400,000 characters is already too many.
     */
    @Test
    fun tooManyCharactersIsRejected(@TempDir tempDir: File) {
        val file = fileOf(tempDir, "long.log", 'a', 400_000)

        Assertions.assertFalse(webUtilities().hasteBinPreChecks(file))
        Assertions.assertTrue(
            webUtilities().hasteBinPreChecks(fileOf(tempDir, "just-under.log", 'a', 399_999)),
            "399,999 characters is under the limit and must still be accepted"
        )
    }

    /**
     * **The discriminating case.** 200,000 three-byte characters is 600,000 bytes — far past the 400,000
     * *character* figure if you mistake bytes for characters, but only half of it in characters. It must be
     * accepted. A shortcut that answers from `File.length()` alone fails exactly here.
     */
    @Test
    fun aMultiByteFileIsJudgedByCharactersNotBytes(@TempDir tempDir: File) {
        val file = fileOf(tempDir, "euro.log", '€', 200_000)

        Assertions.assertEquals(
            600_000L, file.length(),
            "Precondition: the fixture must be over the character limit in bytes but under it in characters"
        )
        Assertions.assertTrue(
            webUtilities().hasteBinPreChecks(file),
            "200,000 characters is under the limit; 600,000 bytes is not the measurement being applied"
        )
    }

    /** A file past the 10 MB byte limit is rejected regardless of how few characters it holds. */
    @Test
    fun aFileOverTenMegabytesIsRejected(@TempDir tempDir: File) {
        val file = fileOf(tempDir, "huge.log", '€', 3_400_000)   // 10,200,000 bytes

        Assertions.assertTrue(file.length() > 10_000_000L, "Precondition: fixture must exceed 10 MB")
        Assertions.assertFalse(webUtilities().hasteBinPreChecks(file))
    }

    /** A directory is not something HasteBin can take; the pre-check must say so rather than throw. */
    @Test
    fun aDirectoryIsRejected(@TempDir tempDir: File) {
        val directory = File(tempDir, "adirectory").apply { mkdirs() }
        File(directory, "child.txt").writeText("content")

        Assertions.assertFalse(webUtilities().hasteBinPreChecks(directory))
    }
}
