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
 * Keeps `serverpackcreator-clientside`'s README in step with the commands that implement it.
 *
 * The four clientside verbs are documented in a *different module* than the one they live in — the engine is
 * `-clientside`, the commands are here — so nothing links a renamed flag to the guide a user follows. This
 * reads both and compares them: every option name picocli accepts must appear in the README, and the README
 * must not promise flags the commands do not have.
 *
 * Reflection would not help: the point is that the *documentation* still matches, which only a text
 * comparison can establish.
 */
internal class ClientsideReadmeFlagsTest {

    private val readme = File("../serverpackcreator-clientside/README.md")

    private val commandSources = listOf(
        "ScanCommand", "ClientsideReportCommand", "VerifyClientsideCommand", "ClientsideApplyCommand"
    ).map { File("src/main/kotlin/de/griefed/serverpackcreator/app/cli/commands/$it.kt") }

    /** `names = ["-x", "--yyy"]` as declared on a picocli `@Option`. */
    private val optionNames = Regex("""names\s*=\s*\[([^]]*)]""")

    /**
     * Long-form options the commands really accept: the explicit `@Option` names, plus `--help`/`--version`
     * for any command enabling picocli's `mixinStandardHelpOptions` — those are accepted without being
     * declared, so treating them as absent would wrongly call the README's `--help` mention a phantom.
     */
    private fun declaredFlags(): Set<String> = commandSources
        .flatMap { source ->
            Assertions.assertTrue(source.isFile, "command source not found at ${source.absolutePath}")
            val text = source.readText()
            val explicit = optionNames.findAll(text)
                .flatMap { match -> Regex("""["']([^"']+)["']""").findAll(match.groupValues[1]) }
                .map { it.groupValues[1] }
                .toList()
            val standard = if (text.contains("mixinStandardHelpOptions = true")) {
                listOf("--help", "--version")
            } else {
                emptyList()
            }
            explicit + standard
        }
        .filter { it.startsWith("--") }          // long forms are the documented contract
        .toSet()

    @Test
    fun everyDocumentedVerbFlagExists() {
        Assertions.assertTrue(readme.isFile, "clientside README not found at ${readme.absolutePath}")
        val declared = declaredFlags()

        val documented = Regex("""`(--[a-z-]+)`""").findAll(readme.readText())
            .map { it.groupValues[1] }
            .toSet()
            // Not verb options: prerequisites and other tools mentioned in prose.
            .filterNot { it == "--yes" }
            .toSet()

        val phantom = documented - declared
        Assertions.assertTrue(phantom.isEmpty(), "README documents flags the commands do not accept: $phantom")
    }

    @Test
    fun everyCommandFlagIsDocumented() {
        val readmeText = readme.readText()
        val undocumented = declaredFlags()
            .filterNot { it == "--version" }   // picocli freebie, not part of the documented workflow
            .filterNot { readmeText.contains(it) }

        Assertions.assertTrue(
            undocumented.isEmpty(),
            "the clientside README does not mention these command flags: $undocumented"
        )
    }
}
