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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.DONE_MARKER
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.POWERSHELL_ENVIRONMENT
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.POWERSHELL_IMAGE
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.POWERSHELL_PLATFORM
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.SETUP_MARKER
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

/**
 * Runs the shipped Batch wrapper's own PowerShell invocation against a path containing an apostrophe.
 *
 * `start.bat` exists so a Windows user never has to touch their execution policy, which makes it the
 * entry point for every Windows server pack — and it built its command line by pasting the script's
 * path into a *single-quoted* PowerShell string. An apostrophe in that path closes the string early,
 * so every pack under `C:\Users\O'Brien\…` failed to start with a parse error naming neither the path
 * nor the quote. Apostrophes in Windows user names are ordinary; the pack author never sees it.
 *
 * The check derives the invocation from the template rather than restating it: the `SET` lines are
 * expanded the way cmd.exe expands them, textually, and the result is handed to a real PowerShell. A
 * rename of `PSSCRIPTPATH`, or a move back to `-Command`, is therefore caught rather than sidestepped.
 *
 * Two deliberate differences from Windows, neither of which touches what is being tested: the binary
 * is `pwsh` rather than `PowerShell`, and the apostrophe path is a POSIX one. The quoting rule under
 * test belongs to PowerShell's parser, which is the same on both.
 *
 * @author Griefed
 */
internal class BatchWrapperQuotingTest {

    private val runner = TemplateInterpreterRunner()

    /**
     * The wrapper must start a script whose path contains an apostrophe.
     *
     * The verdict is the marker the probe prints, not an exit code — this image exits 133 under Rosetta
     * whatever happens, which is the whole reason [TemplateInterpreterRunner.DONE_MARKER] exists.
     */
    @Test
    fun theWrapperStartsAScriptWhosePathContainsAnApostrophe() {
        val outcome = probeUnder(PROBE_DIRECTORY)
        Assertions.assertTrue(
            outcome.lineSequence().any { it.trim() == PROBE_MARKER },
            "the Batch wrapper did not start a script under '$PROBE_DIRECTORY' - a path with an apostrophe " +
                    "in it must not be pasted into a quoted PowerShell string:\n$outcome"
        )
    }

    /** Drive the template's own invocation against a probe script living in [directory], and return its output. */
    private fun probeUnder(directory: String): String {
        val staged = runner.stageTemplates(emptyList())
        runner.stageScript(staged, "probe.ps1", "Write-Output '$PROBE_MARKER'\n")
        runner.stageScript(staged, "invocation.sh", "${invocationFor(directory)}\n")


        val outcome = runner.runInContainer(
            image = POWERSHELL_IMAGE,
            staged = staged,
            platform = POWERSHELL_PLATFORM,
            environment = POWERSHELL_ENVIRONMENT,
            script = "echo $SETUP_MARKER; " +
                    "mkdir -p \"$directory\" && cp /templates/probe.ps1 \"$directory/start.ps1\"; " +
                    // Echoed before it runs: a failure then shows the command line that produced it,
                    // rather than leaving the reader to reconstruct it from the template.
                    "echo 'ran:'; cat /templates/invocation.sh; " +
                    "sh /templates/invocation.sh; " +
                    "echo $DONE_MARKER"
        )

        Assumptions.assumeTrue(outcome != null, "no Docker daemon — the Batch wrapper was NOT checked")
        Assumptions.assumeTrue(
            outcome!!.setUp && outcome.completed,
            "the PowerShell container never ran to its end, so it checked nothing: ${outcome.output.take(400)}"
        )
        return outcome.output
    }

    /**
     * The control: the same derivation, against a path with nothing special in it, must start the script.
     *
     * Without this, a red apostrophe test says only "something in this harness does not work". With it,
     * the two together say the apostrophe is the difference - which is the actual claim.
     */
    @Test
    fun theWrapperStartsAScriptUnderAnOrdinaryPath() {
        val outcome = probeUnder(ORDINARY_DIRECTORY)
        Assertions.assertTrue(
            outcome.lineSequence().any { it.trim() == PROBE_MARKER },
            "the wrapper could not start a script under an ordinary path - the harness is wrong, not the " +
                    "template:\n$outcome"
        )
    }

    /**
     * The template's PowerShell line with cmd.exe's variable expansion applied, for a pack living in
     * [directory].
     *
     * cmd expands `%NAME%` textually, in order, so the `SET` lines are walked in order and each value
     * is expanded against what is already known. `%~dp0` is the directory the batch file sits in and
     * always ends in a separator.
     */
    private fun invocationFor(directory: String): String {
        val template = javaClass.getResourceAsStream("/de/griefed/resources/server_files/default_template.bat")
            ?.bufferedReader()?.use { it.readText() }
            ?: Assertions.fail("default_template.bat is not on the classpath")

        // `%~dp0` is an argument reference, not a variable: it has no trailing `%`, so it cannot go
        // through the `%NAME%` expansion below and is substituted directly.
        val batchFileDirectory = "$directory/"
        val variables = linkedMapOf<String, String>()
        val setting = Regex("^\\s*SET\\s+([A-Za-z_][A-Za-z0-9_]*)=(.*)$", RegexOption.IGNORE_CASE)
        var invocation: String? = null
        for (line in template.lines()) {
            val declaration = setting.find(line)
            if (declaration != null) {
                variables[declaration.groupValues[1]] =
                    expand(declaration.groupValues[2].trim().replace("%~dp0", batchFileDirectory), variables)
            } else if (line.trimStart().startsWith("PowerShell", ignoreCase = true)) {
                invocation = expand(line.trim().replace("%~dp0", batchFileDirectory), variables)
            }
        }
        val line = invocation ?: Assertions.fail("default_template.bat no longer invokes PowerShell")
        // `pwsh` is the binary everywhere but Windows, and `%1` is the optional argument cmd passes
        // through - there is none here, and cmd substitutes an empty string for an absent one.
        val windowsLine = line.replaceFirst(Regex("^PowerShell", RegexOption.IGNORE_CASE), "pwsh")
            .replace("%1", "")
            .trim()
            .removeSuffix(";")
        // Re-quoted for sh one argument at a time rather than handed over as a line. cmd.exe and sh do
        // NOT tokenise this the same way - sh would split the argument at the very apostrophe under
        // test and hand PowerShell something cmd would never produce, which is a harness artefact
        // masquerading as the bug. Tokenising as cmd does and re-quoting for sh gives PowerShell the
        // exact argv it gets on Windows.
        return tokeniseAsCmd(windowsLine).joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
    }

    /**
     * Split [line] into arguments the way cmd.exe hands them to a program: whitespace separates,
     * double quotes group and are removed, and nothing else is interpreted.
     */
    private fun tokeniseAsCmd(line: String): List<String> {
        val arguments = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var started = false
        for (character in line) {
            when {
                character == '"' -> {
                    quoted = !quoted
                    started = true
                }
                character.isWhitespace() && !quoted -> {
                    if (started) {
                        arguments += current.toString()
                        current.setLength(0)
                        started = false
                    }
                }
                else -> {
                    current.append(character)
                    started = true
                }
            }
        }
        if (started) {
            arguments += current.toString()
        }
        return arguments
    }

    /** Replace every `%NAME%` and `%~dp0` in [text] with what [variables] holds, the way cmd.exe does. */
    private fun expand(text: String, variables: Map<String, String>): String {
        var expanded = text
        for ((name, value) in variables) {
            expanded = expanded.replace("%$name%", value)
        }
        return expanded
    }

    private companion object {
        /** A directory whose name carries the character that broke the wrapper. */
        const val PROBE_DIRECTORY = "/probe/O'Brien"

        /** A directory with nothing special about it, for the control. */
        const val ORDINARY_DIRECTORY = "/probe/Smith"

        /** Printed by the probe script; its presence in the output is the whole assertion. */
        const val PROBE_MARKER = "PROBE-RAN"
    }
}
