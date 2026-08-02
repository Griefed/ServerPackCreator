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
package de.griefed.serverpackcreator.grinder.loader

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that a failed install's console survives the next attempt on the same tuple.
 *
 * `DockerLoaderInstaller` writes the live install console beside the generated pack rather than into the cache
 * directory, because `LoaderCache` wipes the cache on failure — which would delete the log exactly when it is the
 * only evidence of why. But the *pack's* tuple directory is wiped wholesale at the start of the next attempt
 * (`ApiVanillaPackGenerator.generate`), so the failing run's console disappeared precisely when a retry made you
 * want to compare the two. One generation is kept instead.
 */
internal class InstallLogRetentionTest {

    /** A previous console is carried across the wipe, so the retry can be compared against what it replaced. */
    @Test
    fun aPreviousInstallConsoleSurvivesTheNextAttempt(@TempDir workDirectory: File) {
        val tupleDir = File(workDirectory, "26.2-Forge-65.1.0").apply { mkdirs() }
        File(tupleDir, INSTALL_LOG).writeText("Exception: installer 404\n")

        val carried = InstallLogRetention.preserve(tupleDir)
        tupleDir.deleteRecursively()
        tupleDir.mkdirs()
        InstallLogRetention.writePrevious(tupleDir, carried)

        val previous = File(tupleDir, "$INSTALL_LOG.previous")
        Assertions.assertTrue(previous.isFile, "the failing attempt's console must outlive the retry that replaced it")
        Assertions.assertEquals("Exception: installer 404\n", previous.readText())
    }

    /** A first attempt has nothing to carry, and must not leave an empty file that reads like a lost log. */
    @Test
    fun aFirstAttemptLeavesNoPreviousLog(@TempDir workDirectory: File) {
        val tupleDir = File(workDirectory, "26.2-Forge-65.1.0").apply { mkdirs() }

        val carried = InstallLogRetention.preserve(tupleDir)
        InstallLogRetention.writePrevious(tupleDir, carried)

        Assertions.assertNull(carried, "there was no console to preserve")
        Assertions.assertFalse(
            File(tupleDir, "$INSTALL_LOG.previous").exists(),
            "writing an empty .previous would suggest a console was captured and lost"
        )
    }

    /** Exactly one generation is kept — the tuple directory must not grow a chain of stale consoles. */
    @Test
    fun onlyOneGenerationIsKept(@TempDir workDirectory: File) {
        val tupleDir = File(workDirectory, "26.2-Forge-65.1.0").apply { mkdirs() }
        File(tupleDir, "$INSTALL_LOG.previous").writeText("two attempts ago\n")
        File(tupleDir, INSTALL_LOG).writeText("the attempt that just failed\n")

        val carried = InstallLogRetention.preserve(tupleDir)
        tupleDir.deleteRecursively()
        tupleDir.mkdirs()
        InstallLogRetention.writePrevious(tupleDir, carried)

        Assertions.assertEquals(
            "the attempt that just failed\n",
            File(tupleDir, "$INSTALL_LOG.previous").readText(),
            "the newest failure is the useful one; older consoles are not accumulated"
        )
        Assertions.assertEquals(
            listOf("$INSTALL_LOG.previous"),
            tupleDir.list()?.sorted(),
            "nothing else should survive the wipe"
        )
    }
}
