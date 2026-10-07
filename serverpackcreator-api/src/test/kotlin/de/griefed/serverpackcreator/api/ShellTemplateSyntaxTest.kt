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

import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.POWERSHELL_ENVIRONMENT
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.POWERSHELL_IMAGE
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.POWERSHELL_PLATFORM
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.DONE_MARKER
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.FAILURE_PREFIX
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.SETUP_MARKER
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Asks the real interpreters whether the shipped fish and PowerShell templates are even parseable.
 *
 * A syntax error in a shipped template produces a server pack that dies before it loads anything, and
 * this repository has already paid for that class of bug twice — the Forge launcher era and the Java-24
 * Security Manager flag. Yet the guards were effectively absent: the fish check skipped itself whenever
 * `fish` was missing, which is every developer machine without it *and* the CI runner, and PowerShell had
 * no source-level check at all — only `ScriptTemplateMatrixIT`, which is gated behind
 * `GRINDER_TEMPLATE_IT=1` and a built image that no workflow provides.
 *
 * Containers rather than an installed interpreter, deliberately. PowerShell is not in Ubuntu's default
 * repositories, so installing it on the runner is a multi-step setup against a third-party apt source,
 * while an image is one line and pins *which* interpreter answered. The check degrades honestly: local
 * interpreter first, container second, and a skip — never a pass — when neither is reachable.
 *
 * @author Griefed
 */
internal class ShellTemplateSyntaxTest {

    private val runner = TemplateInterpreterRunner()

    /**
     * `fish -n` parses without executing. Alpine carries fish in its own repositories, so no third-party
     * source is needed; the marker below separates "could not install it" from "the template is broken".
     */
    @Test
    fun fishTemplatesAreSyntacticallyValid() {
        val names = listOf("default_template.fish", "default_java_template.fish")
        val staged = runner.stageTemplates(names)
        val checked = runner.runLocally("fish", names.associateWith { listOf("fish", "-n", File(staged, it).absolutePath) })
            ?: runner.runInContainer(
                image = "alpine:latest",
                staged = staged,
                script = "apk add --no-cache fish >/dev/null 2>&1 || exit 90; echo $SETUP_MARKER; " +
                        "for f in /templates/*.fish; do fish -n \"${'$'}f\" || echo \"$FAILURE_PREFIX ${'$'}f\"; done; " +
                        "echo $DONE_MARKER"
            )
        assertParsed("fish", checked)
    }

    /**
     * PowerShell's own parser is the only honest check — `pwsh -Command` would *run* the template, and
     * `Parser::ParseFile` is exactly what the grinder's matrix used before this took the job over, so
     * both generations of the guard agree on what "parses" means.
     */
    @Test
    fun powerShellTemplatesParse() {
        val names = listOf("default_template.ps1", "default_java_template.ps1")
        val staged = runner.stageTemplates(names)
        val checked = runner.runLocally("pwsh", names.associateWith {
            listOf("pwsh", "-NoProfile", "-Command", parseCommandFor(File(staged, it).absolutePath))
        }) ?: runner.runInContainer(
            image = POWERSHELL_IMAGE,
            staged = staged,
            platform = POWERSHELL_PLATFORM,
            environment = POWERSHELL_ENVIRONMENT,
            command = listOf(
                "pwsh", "-NoProfile", "-Command",
                "Write-Output '$SETUP_MARKER'; " + parseCommandFor("/templates/*.ps1") + "\nWrite-Output '$DONE_MARKER'"
            )
        )
        assertParsed("PowerShell", checked)
    }

    /**
     * The zero-match guard in [parseCommandFor] has to be able to fire, or it is decoration.
     *
     * Points the same command at an empty directory — which is what a container whose templates never
     * arrived holds — and demands the `FAIL` line. Without it `foreach` iterates nothing, the check reports
     * no failures, and a run that parsed zero templates is indistinguishable from a clean one.
     */
    @Test
    fun aPowerShellGlobThatMatchesNothingIsAFailure() {
        val empty = runner.stageTemplates(emptyList())
        val checked = runner.runLocally(
            "pwsh",
            mapOf("empty" to listOf("pwsh", "-NoProfile", "-Command", parseCommandFor(File(empty, "*.ps1").absolutePath)))
        ) ?: runner.runInContainer(
            image = POWERSHELL_IMAGE,
            staged = empty,
            platform = POWERSHELL_PLATFORM,
            environment = POWERSHELL_ENVIRONMENT,
            command = listOf(
                "pwsh", "-NoProfile", "-Command",
                "Write-Output '$SETUP_MARKER'; " + parseCommandFor("/templates/*.ps1") + "\nWrite-Output '$DONE_MARKER'"
            )
        )

        Assumptions.assumeTrue(checked != null, "no PowerShell interpreter and no Docker daemon — the guard was NOT checked")
        Assumptions.assumeTrue(
            checked!!.setUp && checked.completed,
            "the PowerShell check did not run to its end: ${checked.output.take(400)}"
        )
        Assertions.assertTrue(
            checked.failures.isNotEmpty(),
            "a glob matching no template must be reported as a failure, got:\n${checked.output}"
        )
    }

    /**
     * The PowerShell one-liner that parses [glob] and reports a `FAIL` line per rejected template.
     *
     * Shared by the local and container paths so the two cannot drift into checking different things.
     *
     * A glob that matches nothing is itself a `FAIL`, because `foreach` over an empty match runs zero
     * times and the check would otherwise report success for having parsed nothing. That is the shape a
     * transport failure takes here: templates that never reached the container read as a clean run.
     */
    private fun parseCommandFor(glob: String): String =
        """
        ${'$'}templates = @(Get-ChildItem -Path '$glob' -ErrorAction SilentlyContinue)
        if (${'$'}templates.Count -eq 0) { Write-Output "$FAILURE_PREFIX nothing matched '$glob', so nothing was parsed" }
        foreach (${'$'}f in ${'$'}templates) {
          ${'$'}errs = ${'$'}null
          [System.Management.Automation.Language.Parser]::ParseFile(${'$'}f.FullName, [ref]${'$'}null, [ref]${'$'}errs) | Out-Null
          if (${'$'}errs.Count -gt 0) { Write-Output "$FAILURE_PREFIX ${'$'}(${'$'}f.Name)"; ${'$'}errs | ForEach-Object { Write-Output ${'$'}_.Message } }
        }
        """.trimIndent()

    /**
     * Fail on a real syntax error, skip when nothing could run it, and never pass silently.
     *
     * The verdict is the `FAIL` lines the script printed, not its exit status — see
     * [TemplateInterpreterRunner.DONE_MARKER] for why an exit code cannot be trusted here.
     */
    private fun assertParsed(interpreter: String, outcome: TemplateInterpreterRunner.Outcome?) {
        Assumptions.assumeTrue(
            outcome != null,
            "no $interpreter interpreter and no Docker daemon — the template syntax was NOT checked"
        )
        Assumptions.assumeTrue(
            outcome!!.setUp,
            "the $interpreter container could not be prepared (no daemon, no network or no package): ${outcome.output.take(400)}"
        )
        Assumptions.assumeTrue(
            outcome.completed,
            "the $interpreter check did not run to its end, so it checked nothing: ${outcome.output.take(400)}"
        )
        Assertions.assertTrue(
            outcome.failures.isEmpty(),
            "${outcome.ranWith} rejected a shipped $interpreter template:\n${outcome.output}"
        )
    }
}
