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
package de.griefed.serverpackcreator.api.config

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins what `suggestInclusions` does with a modpack directory it cannot read.
 *
 * `listFiles()` returns null for a path that is not a readable directory. That was handled by an
 * `assert` — disabled at runtime outside tests, so decorative — and a `!!` inside a `catch` for the
 * `NullPointerException` it produced, logging a message about "copy dirs" that described a different
 * problem. The caller received an empty list, which in `isZip` is merged into the pack's inclusions,
 * so an unreadable modpack produced a server pack with no directories and said nothing.
 */
internal class SuggestInclusionsTest {

    private val configurationHandler =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).configurationHandler

    @Test
    fun aReadableModpackYieldsItsDirectories(@TempDir tempDir: File) {
        File(tempDir, "mods").mkdirs()
        File(tempDir, "config").mkdirs()
        File(tempDir, "a-file.txt").writeText("not a directory")

        val suggested = configurationHandler.suggestInclusions(tempDir.absolutePath).map { it.source }

        Assertions.assertTrue(suggested.contains("mods"), "suggested $suggested")
        Assertions.assertTrue(suggested.contains("config"), "suggested $suggested")
        Assertions.assertFalse(suggested.contains("a-file.txt"), "a plain file was suggested: $suggested")
    }

    @Test
    fun aPathThatIsNotADirectoryYieldsNothingAndDoesNotThrow(@TempDir tempDir: File) {
        val notADirectory = File(tempDir, "modpack.zip").apply { writeText("an archive, not a directory") }

        val suggested = Assertions.assertDoesNotThrow<List<InclusionSpecification>> {
            configurationHandler.suggestInclusions(notADirectory.absolutePath)
        }

        Assertions.assertTrue(suggested.isEmpty())
    }

    @Test
    fun aPathThatDoesNotExistYieldsNothingAndDoesNotThrow(@TempDir tempDir: File) {
        val absent = File(tempDir, "never-created")

        val suggested = Assertions.assertDoesNotThrow<List<InclusionSpecification>> {
            configurationHandler.suggestInclusions(absent.absolutePath)
        }

        Assertions.assertTrue(suggested.isEmpty())
    }
}
