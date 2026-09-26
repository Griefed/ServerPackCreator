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
 * Executes the PowerShell template's `RunInstallerJavaCommand` and pins which Java it hands a modloader
 * installer.
 *
 * A parse check cannot see this. The Quilt installer needs Java 17+ even when the server itself runs on
 * an older Java — Minecraft 1.16.1 wants Java 8 by Mojang's own declaration — so the templates route
 * installers through a `JAVA_INSTALLER` override that falls back to `JAVA` when unset. **The fallback is
 * the branch every existing pack takes**, because nothing writes `JAVA_INSTALLER` into a hand-made pack,
 * so a quoting slip there breaks installs for everyone while a syntax check stays green.
 *
 * Promoted out of the grinder's `ScriptTemplateMatrixIT` on 2026-09-26. It was gated behind
 * `GRINDER_TEMPLATE_IT=1` and the built `spc-grinder-templates` image, and no workflow sets that gate —
 * yet the probe needs nothing but `pwsh` and the template, both of which a stock image supplies. Its
 * siblings stay behind that gate because they download a Minecraft server per cell.
 *
 * The template is never *run*: it shells out to Windows `CMD /C`, which does not exist on Linux. Instead
 * the one function is lifted out of the parsed AST, defined on its own, and `CMD` is stubbed to record
 * what it was handed — which is exactly the assertion worth making.
 *
 * @author Griefed
 */
internal class PowerShellInstallerJavaTest {

    private val runner = TemplateInterpreterRunner()

    /**
     * An unset `JAVA_INSTALLER` must fall back to the server's Java, and a set one must win.
     *
     * Both directions come from one probe run, so a function that ignored the override — or one that
     * ignored the fallback and handed the installer an empty path — fails on the other line.
     */
    @Test
    fun theInstallerJavaOverrideWinsAndFallsBackToTheServersJava() {
        val staged = runner.stageTemplates(listOf("default_template.ps1"))
        val probeScript = runner.stageScript(staged, PROBE_NAME, probe())

        val outcome = runner.runLocally("pwsh", mapOf(PROBE_NAME to listOf("pwsh", "-NoProfile", "-File", probeScript.absolutePath)))
            ?: runner.runInContainer(
            image = POWERSHELL_IMAGE,
            staged = staged,
            platform = POWERSHELL_PLATFORM,
            environment = POWERSHELL_ENVIRONMENT,
            command = listOf("pwsh", "-NoProfile", "-File", "/templates/$PROBE_NAME")
        )

        Assumptions.assumeTrue(
            outcome != null,
            "no pwsh and no Docker daemon — the installer-Java selection was NOT checked"
        )
        Assumptions.assumeTrue(
            outcome!!.setUp && outcome.completed,
            "the pwsh probe never ran to its end, so it checked nothing: ${outcome.output.take(600)}"
        )

        Assertions.assertTrue(
            outcome.output.lineSequence().any { it.startsWith("FALLBACK:") && it.contains(SERVER_JAVA) },
            "an unset JAVA_INSTALLER must fall back to the server's Java ($SERVER_JAVA), got:\n${outcome.output}"
        )
        Assertions.assertTrue(
            outcome.output.lineSequence().any { it.startsWith("OVERRIDE:") && it.contains(INSTALLER_JAVA) },
            "a set JAVA_INSTALLER must be used for the installer ($INSTALLER_JAVA), got:\n${outcome.output}"
        )
    }

    /**
     * The probe: lift `RunInstallerJavaCommand` out of the parsed template, define it, and record what it
     * hands `CMD` under each setting.
     *
     * The AST is walked with a `Where-Object` pipeline rather than `Ast.FindAll`, which takes a
     * ScriptBlock as a .NET delegate — under Rosetta that call site is mistranslated and the process dies
     * with `System.NullReferenceException` at `CallSite.Target` before printing anything. The pipeline
     * reaches the same node and survives.
     */
    private fun probe(): String {
        val d = '$'
        return """
            ${d}ErrorActionPreference = 'Stop'
            Write-Output '$SETUP_MARKER'
            ${d}errors = ${d}null
            ${d}ast = [System.Management.Automation.Language.Parser]::ParseFile('/templates/default_template.ps1', [ref]${d}null, [ref]${d}errors)
            if (${d}errors) { throw 'the template does not parse' }
            ${d}fn = @(${d}ast.EndBlock.Statements | Where-Object {
              ${d}_ -is [System.Management.Automation.Language.FunctionDefinitionAst] -and ${d}_.Name -like '*RunInstallerJavaCommand'
            })
            if (${d}fn.Count -ne 1) { throw "expected one RunInstallerJavaCommand, found ${d}(${d}fn.Count)" }

            # Define the template's own function, and stub CMD so nothing Windows-only actually runs.
            Invoke-Expression ${d}fn[0].Extent.Text
            function global:CMD { param([string]${d}Slash, [string]${d}Line) ${d}global:Recorded = ${d}Line }

            ${d}global:Java = '$SERVER_JAVA'
            ${d}global:JavaInstaller = ${d}null
            RunInstallerJavaCommand '-jar quilt-installer.jar'
            Write-Output "FALLBACK:${d}Recorded"

            ${d}global:JavaInstaller = '$INSTALLER_JAVA'
            RunInstallerJavaCommand '-jar quilt-installer.jar'
            Write-Output "OVERRIDE:${d}Recorded"
            Write-Output '$DONE_MARKER'
        """.trimIndent() + "\n"
    }

    private companion object {
        /** Written beside the staged template so one read-only mount serves both. */
        const val PROBE_NAME = "installer-java-probe.ps1"

        /** Stands in for the server's own Java; distinct from [INSTALLER_JAVA] so the two cannot be confused. */
        const val SERVER_JAVA = "/server/java8"

        /** Stands in for a JAVA_INSTALLER override, deliberately sharing no substring with [SERVER_JAVA]. */
        const val INSTALLER_JAVA = "/installer/java21"
    }
}
