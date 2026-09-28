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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.config.PackConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Optional

/**
 * Pins what `success` means, which until now was not what its name says.
 *
 * `errors` was populated by exactly one thing — the Nekodetector scan of the finished pack — so
 * `success` meant "no malware found". Every real failure (a copy that threw, a script that could not
 * be written, a modloader server that did not install, a ZIP that was not created) is logged and
 * nothing more, so it reported success; and a pack that built perfectly but contains an infected mod
 * reported failure. Six call sites across the web queue, both CLI verbs, the GUI and the grinder
 * branch on it.
 */
internal class ServerPackGenerationOutcomeTest {

    private fun generation(
        errors: List<String> = emptyList(),
        scanFindings: List<String> = emptyList(),
        serverPack: File
    ) = ServerPackGeneration(serverPack, errors, Optional.empty(), PackConfig(), emptyList(), scanFindings)

    @Test
    fun aPackThatBuiltCorrectlyIsSuccessfulEvenWhenTheScanFoundSomething(@TempDir dir: File) {
        val outcome = generation(scanFindings = listOf("Stage 1 infections:", "evil.jar"), serverPack = dir)

        Assertions.assertTrue(
            outcome.success,
            "a malware finding is a fact about the pack's contents, not a failure to build it"
        )
        Assertions.assertEquals(listOf("Stage 1 infections:", "evil.jar"), outcome.scanFindings)
    }

    @Test
    fun aGenerationThatRecordedAFailureIsNotSuccessful(@TempDir dir: File) {
        val outcome = generation(errors = listOf("Could not copy the modpack."), serverPack = dir)

        Assertions.assertFalse(outcome.success)
    }

    @Test
    fun theTwoListsAreIndependent(@TempDir dir: File) {
        val outcome = generation(
            errors = listOf("Could not copy the modpack."),
            scanFindings = listOf("evil.jar"),
            serverPack = dir
        )

        Assertions.assertEquals(listOf("Could not copy the modpack."), outcome.errors)
        Assertions.assertEquals(listOf("evil.jar"), outcome.scanFindings)
    }
}
