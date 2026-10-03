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
package de.griefed.serverpackcreator.app.cli.commands

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that `-verifyclientside` keeps the evidence its boots produce.
 *
 * Asserted against the source rather than through a run, because the alternative needs an `ApiWrapper`,
 * a real generation and a booting server — and the property worth protecting is structural anyway: for
 * months `BootArtifacts.collect` had no caller outside the grinder, so the only thing the CI job could
 * upload was a single `boot.log` that every re-check had already overwritten. Nothing was red. Deleting
 * the sink again would be equally silent, which is the case for pinning the shape.
 *
 * @author Griefed
 */
internal class VerifyClientsideArtifactSinkTest {

    /** `-verifyclientside`'s source, resolved from the module directory the test runs in. */
    private val source =
        File("src/main/kotlin/de/griefed/serverpackcreator/app/cli/commands/VerifyClientsideCommand.kt")

    /**
     * The verb that boots must hand its `BootVerifier` somewhere to put what the boots produce. Without
     * it the engine still runs, still crashes correctly and still classifies — and throws the console,
     * the server's own logs and the crash report away before anything can read them.
     */
    @Test
    fun theBootingVerbSuppliesABootArtifactSink() {
        Assertions.assertTrue(source.isFile, "VerifyClientsideCommand.kt not found at ${source.absolutePath}")

        Assertions.assertTrue(
            source.readText().contains("bootArtifactSink"),
            "-verifyclientside constructs a BootVerifier with no bootArtifactSink, so BootArtifacts.collect " +
                "is never called and every attempt's evidence is wiped by the next staging"
        )
    }

    /**
     * And it must write somewhere staging will not wipe. `<work>/boot/<attemptName>` is deleted and
     * re-created before every attempt, so evidence kept inside the tree it is collected from is evidence
     * kept until the next boot.
     */
    @Test
    fun theEvidenceIsKeptOutsideTheDirectoryStagingWipes() {
        val text = source.readText()

        Assertions.assertTrue(
            text.contains("BootArtifactWriter"),
            "the sink must go through BootArtifactWriter, which files each attempt separately"
        )
        Assertions.assertFalse(
            Regex("""BootArtifactWriter\(\s*File\(workDirectory,\s*"boot"\)""").containsMatchIn(text),
            "writing into <work>/boot means writing into the directory the next stageBootPack deletes"
        )
    }
}
