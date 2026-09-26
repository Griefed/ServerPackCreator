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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Asks the real interpreters whether the shipped fish and PowerShell templates are even parseable, using
 * a container when the interpreter is not installed locally.
 *
 * A syntax error in a shipped template produces a server pack that dies before it loads anything, and
 * this repository has already paid for that class of bug twice — the Forge launcher era and the Java-24
 * Security Manager flag. Yet the guards were effectively absent: the fish check skipped itself whenever
 * `fish` was missing, which is every developer machine without it *and* the CI runner, and PowerShell had
 * no source-level check at all — only `ScriptTemplateMatrixIT`, gated behind `GRINDER_TEMPLATE_IT=1` and
 * a built image, so it never runs either. Measured 2026-09-26: neither interpreter is present on this
 * machine, and no workflow in `.forgejo/` or `.github/` sets that gate.
 *
 * Containers rather than an installed interpreter, deliberately. PowerShell is not in Ubuntu's default
 * repositories, so installing it on the runner is a multi-step setup against a third-party apt source,
 * while an image is one line and pins *which* interpreter answered. The check degrades honestly: local
 * interpreter first, container second, and a skip — never a pass — when neither is reachable.
 *
 * @author Griefed
 */
internal class ShellTemplateSyntaxTest {

    /**
     * `fish -n` parses without executing. Alpine carries fish in its own repositories, so no third-party
     * source is needed; the marker below separates "could not install it" from "the template is broken".
     */
    @Test
    fun fishTemplatesAreSyntacticallyValid() {
        val staged = stageTemplates("default_template.fish", "default_java_template.fish")
        val checked = checkLocally("fish", staged) { file -> listOf("fish", "-n", file.absolutePath) }
            ?: checkInContainer(
                image = "alpine:latest",
                staged = staged,
                script = "apk add --no-cache fish >/dev/null 2>&1 || exit 90; echo $SETUP_MARKER; " +
                        "status=0; for f in /templates/*.fish; do fish -n \"${'$'}f\" || status=1; done; exit ${'$'}status"
            )
        assertParsed("fish", checked)
    }

    /**
     * PowerShell's own parser is the only honest check — `pwsh -Command` would *run* the template, and
     * `Parser::ParseFile` is exactly what `ScriptTemplateMatrixIT.powerShellTemplatesParse` uses, so both
     * guards agree on what "parses" means.
     */
    @Test
    fun powerShellTemplatesParse() {
        val staged = stageTemplates("default_template.ps1", "default_java_template.ps1")
        val checked = checkLocally("pwsh", staged) { file -> listOf("pwsh", "-NoProfile", "-Command", parseCommandFor(file.absolutePath)) }
            ?: checkInContainer(
                image = "mcr.microsoft.com/powershell:latest",
                staged = staged,
                // No linux/arm64 is published — the tags are amd64, arm/v7 and windows/amd64 — so an
                // Apple-Silicon daemon picks arm/v7 and qemu dies with "uncaught target signal 11", then
                // hangs. Naming the platform costs an emulated pull and is the difference between a
                // 30-second check and a wedged container.
                platform = "linux/amd64",
                command = listOf("pwsh", "-NoProfile", "-Command", "echo $SETUP_MARKER; " + parseCommandFor("/templates/*.ps1"))
            )
        assertParsed("PowerShell", checked)
    }

    /**
     * The PowerShell one-liner that parses [glob] and exits non-zero with the parser's own messages.
     *
     * Shared by the local and container paths so the two cannot drift into checking different things.
     */
    private fun parseCommandFor(glob: String): String =
        """
        ${'$'}failed = 0
        foreach (${'$'}f in (Get-ChildItem -Path '$glob')) {
          ${'$'}errs = ${'$'}null
          [System.Management.Automation.Language.Parser]::ParseFile(${'$'}f.FullName, [ref]${'$'}null, [ref]${'$'}errs) | Out-Null
          if (${'$'}errs.Count -gt 0) { ${'$'}failed = 1; Write-Output "FAIL ${'$'}(${'$'}f.Name)"; ${'$'}errs | ForEach-Object { Write-Output ${'$'}_.Message } }
        }
        exit ${'$'}failed
        """.trimIndent()

    /**
     * Copy the named classpath templates into one throwaway directory and return it.
     *
     * One directory rather than one temp file each, because the container paths mount it whole and the
     * per-interpreter glob then names every template without the test repeating the list.
     */
    private fun stageTemplates(vararg names: String): File {
        val directory = Files.createTempDirectory("spc-template-syntax").toFile().canonicalFile
        directory.deleteOnExit()
        for (name in names) {
            val body = javaClass.getResourceAsStream("/de/griefed/resources/server_files/$name")
                ?.bufferedReader()?.use { it.readText() }
                ?: Assertions.fail("template resource '$name' is not on the classpath")
            File(directory, name).apply { writeText(body); deleteOnExit() }
        }
        return directory
    }

    /**
     * Run [interpreter] over each staged template, or return null when it is not on the PATH.
     *
     * Null means "not checked here", which is what makes the container the fallback rather than the
     * only path — a developer with fish installed pays no container start.
     */
    private fun checkLocally(interpreter: String, staged: File, commandFor: (File) -> List<String>): Outcome? {
        if (onPath(interpreter) == null) {
            return null
        }
        val transcript = StringBuilder()
        var worst = 0
        var timedOut = false
        for (template in staged.listFiles().orEmpty().sortedBy { it.name }) {
            val run = execute(commandFor(template))
            transcript.append(run.output)
            timedOut = timedOut || run.timedOut
            if (run.exitCode != 0) {
                worst = run.exitCode
            }
        }
        return Outcome("local $interpreter", worst, transcript.toString(), setUp = !timedOut)
    }

    /**
     * Run the check inside [image] with [staged] mounted read-only, or return null when Docker cannot.
     *
     * `script` is run through `sh -c` for images that need a package installed first; `command` is used
     * verbatim where the image already carries the interpreter.
     */
    private fun checkInContainer(
        image: String,
        staged: File,
        script: String? = null,
        command: List<String>? = null,
        platform: String? = null
    ): Outcome? {
        if (onPath("docker") == null) {
            return null
        }
        val invocation = mutableListOf("docker", "run", "--rm", "-v", "${staged.absolutePath}:/templates:ro")
        if (platform != null) {
            invocation += listOf("--platform", platform)
        }
        if (script != null) {
            invocation += listOf("--entrypoint", "sh", image, "-c", script)
        } else {
            invocation += image
            invocation += command.orEmpty()
        }
        val run = execute(invocation)
        // No marker means the image never reached the check — no daemon, no network for the pull, no
        // package — and a timeout means it never finished one. Both are environment answers, not verdicts
        // on the template, so neither may read as a syntax error.
        val reached = run.output.contains(SETUP_MARKER) && !run.timedOut
        return Outcome("container $image", run.exitCode, run.output, setUp = reached)
    }

    /** Fail on a real syntax error, skip when nothing could run it, and never pass silently. */
    private fun assertParsed(interpreter: String, outcome: Outcome?) {
        Assumptions.assumeTrue(
            outcome != null,
            "no $interpreter interpreter and no Docker daemon — the template syntax was NOT checked"
        )
        Assumptions.assumeTrue(
            outcome!!.setUp,
            "the $interpreter container could not be prepared (no daemon, no network or no package): ${outcome.output.take(400)}"
        )
        Assertions.assertEquals(
            0,
            outcome.exitCode,
            "${outcome.ranWith} rejected a shipped $interpreter template:\n${outcome.output}"
        )
    }

    /**
     * Run [command] under a bound generous enough for an image pull but not for a wedged container.
     *
     * A timeout is reported rather than folded into the exit code, because an interpreter that never
     * answered says nothing about the template and must not be read as a rejection.
     */
    private fun execute(command: List<String>): Run {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return Run(-1, "$output\n[gave up after ${CHECK_TIMEOUT_SECONDS}s]", timedOut = true)
        }
        return Run(process.exitValue(), output)
    }

    /** One invocation's result: how it ended, what it said, and whether it ended at all. */
    private data class Run(
        /** The process exit status, or -1 when it had to be killed. */
        val exitCode: Int,
        /** Combined stdout and stderr. */
        val output: String,
        /** True when the bound was hit, which makes the result an environment answer. */
        val timedOut: Boolean = false
    )

    /** First executable named [executable] on the PATH, or null. */
    private fun onPath(executable: String): File? =
        System.getenv("PATH")?.split(File.pathSeparator)
            ?.map { File(it, executable) }
            ?.firstOrNull { it.canExecute() }

    /**
     * What a check produced: who ran it, how it ended, and whether the environment was ready at all.
     *
     * `setUp` exists so "the container never got its interpreter" cannot be mistaken for "the template
     * is broken" — the two have identical non-zero exits otherwise.
     */
    private data class Outcome(
        /** Which interpreter answered, named in the failure message so a red run is reproducible. */
        val ranWith: String,
        /** The check's exit status; zero means every staged template parsed. */
        val exitCode: Int,
        /** Combined stdout and stderr, quoted verbatim on failure. */
        val output: String,
        /** Whether the interpreter was actually reached; false turns a failure into a skip. */
        val setUp: Boolean = true
    )

    private companion object {
        /** Printed by the container once its interpreter is usable; its absence means nothing was checked. */
        const val SETUP_MARKER = "SPC-SYNTAX-CHECK-READY"

        /** Bound per invocation: an emulated PowerShell pull took ~30 s measured, a wedged one never ends. */
        const val CHECK_TIMEOUT_SECONDS = 180L
    }
}
