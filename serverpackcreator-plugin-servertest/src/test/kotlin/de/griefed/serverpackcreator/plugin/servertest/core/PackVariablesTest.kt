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
package de.griefed.serverpackcreator.plugin.servertest.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the two `variables.txt` settings the console has to warn about.
 *
 * Both change what the user sees rather than how the pack launches, and both look like a hang when nobody
 * says so first — `WAIT_FOR_USER_INPUT` especially, because bash prints its `read -p` prompt only when
 * standard input is a terminal, so over this plugin's pipe the script produces nothing at all at that point.
 */
internal class PackVariablesTest {

    /** A realistic slice of a generated `variables.txt`, comments and quoted values included. */
    private fun packWithVariables(directory: File, restart: String, waitForInput: String): File =
        File(directory, PackVariables.FILE_NAME).apply {
            writeText(
                """
                # RESTART true/false allows you to enable/disable automatically restarting the server
                MINECRAFT_VERSION=1.21
                WAIT_FOR_USER_INPUT=$waitForInput
                JAVA="java"
                RESTART=$restart
                SKIP_JAVA_CHECK=false
                """.trimIndent() + "\n"
            )
        }

    /** Both switched on is the case that needs both warnings. */
    @Test
    fun readsBothSettingsWhenTheyAreOn(@TempDir packDir: File) {
        packWithVariables(packDir, restart = "true", waitForInput = "true")

        val variables = PackVariables.read(packDir)

        Assertions.assertTrue(variables.restartsAutomatically)
        Assertions.assertTrue(variables.waitsForUserInput)
    }

    /** The generated default is both off, and must not produce warnings nobody needs. */
    @Test
    fun readsBothSettingsWhenTheyAreOff(@TempDir packDir: File) {
        packWithVariables(packDir, restart = "false", waitForInput = "false")

        Assertions.assertEquals(PackVariables(restartsAutomatically = false, waitsForUserInput = false), PackVariables.read(packDir))
    }

    /**
     * A commented-out line must not count. The shipped `variables.txt` carries 77 lines of documentation
     * above its values, and the word RESTART appears in that prose.
     */
    @Test
    fun ignoresTheDocumentationAboveTheValues(@TempDir packDir: File) {
        File(packDir, PackVariables.FILE_NAME).writeText(
            "# RESTART=true would restart the server. Do not confuse this line for a setting.\nRESTART=false\n"
        )

        Assertions.assertFalse(PackVariables.read(packDir).restartsAutomatically)
    }

    /**
     * A key that merely *ends with* the one being looked for is not that key.
     *
     * The anchor is what makes this work, and its absence is invisible in the obvious fixtures: reads take
     * the last match, so a stray match earlier in the file is masked. Put the impostor *after* the real
     * setting and an unanchored pattern answers with the impostor's value instead. Pinned because the
     * variables file carries custom keys appended by ServerPackCreator's own `CUSTOM_<X>_CUSTOM` mechanism,
     * so a name ending in a real key is reachable rather than hypothetical.
     */
    @Test
    fun doesNotMatchAKeyThatMerelyEndsWithTheOneBeingRead(@TempDir packDir: File) {
        File(packDir, PackVariables.FILE_NAME).writeText("RESTART=false\nAUTO_RESTART=true\n")

        Assertions.assertFalse(
            PackVariables.read(packDir).restartsAutomatically,
            "AUTO_RESTART is a different setting from RESTART."
        )
    }

    /** Quoted and differently-cased values still read, because the file is hand-editable. */
    @Test
    fun toleratesQuotingAndCasing(@TempDir packDir: File) {
        File(packDir, PackVariables.FILE_NAME).writeText("RESTART=\"TRUE\"\nWAIT_FOR_USER_INPUT= True \n")

        val variables = PackVariables.read(packDir)

        Assertions.assertTrue(variables.restartsAutomatically)
        Assertions.assertTrue(variables.waitsForUserInput)
    }

    /**
     * A pack with no readable variables reports neither. Its start script refuses to run for that very
     * reason, and a warning about a file that is not there would only obscure the real message.
     */
    @Test
    fun aPackWithoutVariablesReportsNeitherSetting(@TempDir packDir: File) {
        Assertions.assertEquals(
            PackVariables(restartsAutomatically = false, waitsForUserInput = false),
            PackVariables.read(packDir)
        )
    }
}
