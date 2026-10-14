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
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that replacing a file's contents either happens completely or not at all.
 *
 * The failure this exists for is not hypothetical: a version manifest written by truncate-then-write was
 * left truncated on a grinder host, and because nothing re-seeds a manifest that already exists, every
 * subsequent start died parsing it. A write that cannot leave a half-file behind removes that state
 * entirely.
 */
internal class AtomicTextWriteTest {

    /** Pins the ordinary case: the destination ends up holding exactly what was written. */
    @Test
    fun theDestinationHoldsTheWholeText(@TempDir tempDir: File) {
        val destination = File(tempDir, "manifest.json")
        destination.writeText("""{"versions":["one"]}""")

        destination.writeTextAtomically("""{"versions":["one","two"]}""")

        Assertions.assertEquals("""{"versions":["one","two"]}""", destination.readText())
    }

    /** Pins that nothing is left beside the destination once the write has finished. */
    @Test
    fun noTemporaryFileSurvivesTheWrite(@TempDir tempDir: File) {
        val destination = File(tempDir, "manifest.json")

        destination.writeTextAtomically("""{"versions":[]}""")

        Assertions.assertArrayEquals(
            arrayOf("manifest.json"),
            tempDir.list()?.sortedArray(),
            "A completed write must leave the destination and nothing else"
        )
    }

    /**
     * Pins the whole point: a write that fails must leave the previous contents readable.
     *
     * The staging file is injectable purely so this can be executed — the real failures (a killed JVM, a
     * full disk) cannot be provoked from a test, and pointing the staging at a directory that does not
     * exist reaches the same code path with the same question: *what does the destination hold when the
     * write did not finish?* Truncate-then-write answers "nothing"; this answers "what it held before".
     */
    @Test
    fun aFailedWriteLeavesThePreviousContentsIntact(@TempDir tempDir: File) {
        val destination = File(tempDir, "manifest.json")
        val original = """{"versions":["one"]}"""
        destination.writeText(original)

        Assertions.assertThrows(Exception::class.java) {
            destination.writeTextAtomically(
                """{"versions":["one","two"]}""",
                File(tempDir, "absent-directory/manifest.json.tmp")
            )
        }

        Assertions.assertEquals(
            original,
            destination.readText(),
            "A write that could not be completed must not have destroyed what was there"
        )
    }
}
