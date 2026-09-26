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
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Runs a shell interpreter against the **shipped** templates — locally when it is installed, in a
 * container when it is not — so a check cannot quietly stop running just because a machine lacks fish
 * or PowerShell.
 *
 * One definition rather than one per test class: both [ShellTemplateSyntaxTest] and
 * [PowerShellInstallerJavaTest] need the same three things — stage the real templates, reach an
 * interpreter, and tell "the environment could not answer" apart from "the template is wrong". That
 * last distinction is the whole point, and it is why nothing here reports a verdict: it reports what
 * happened and lets the caller decide.
 *
 * @author Griefed
 */
internal class TemplateInterpreterRunner {

    /**
     * Copy the named classpath templates into one throwaway directory and return it.
     *
     * One directory rather than a temp file each, because the container mounts it whole and a probe can
     * then name its siblings without the test repeating the list.
     */
    fun stageTemplates(names: List<String>): File {
        val directory = Files.createTempDirectory("spc-template-probe").toFile().canonicalFile
        directory.deleteOnExit()
        for (name in names) {
            val body = javaClass.getResourceAsStream("/de/griefed/resources/server_files/$name")
                ?.bufferedReader()?.use { it.readText() }
                ?: Assertions.fail("template resource '$name' is not on the classpath")
            File(directory, name).apply { writeText(body); deleteOnExit() }
        }
        return directory
    }

    /** Write [body] into [staged] as [name] so a container sees it beside the templates it inspects. */
    fun stageScript(staged: File, name: String, body: String): File =
        File(staged, name).apply { writeText(body); deleteOnExit() }

    /**
     * Run [interpreter] over each labelled command in [commands], or null when it is not on the PATH.
     *
     * Null means "not run here", which is what makes the container a fallback rather than the only path —
     * a developer with the interpreter installed pays no container start. One command or one per template
     * is the caller's choice; the labels are what a synthesised failure line names, so they should be the
     * things a reader would look for.
     */
    fun runLocally(interpreter: String, commands: Map<String, List<String>>): Outcome? {
        if (onPath(interpreter) == null) {
            return null
        }
        val transcript = StringBuilder()
        var timedOut = false
        for ((label, command) in commands) {
            val run = execute(command)
            transcript.append(run.output)
            timedOut = timedOut || run.timedOut
            // Synthesise the same FAIL line a container script prints, so success has one definition
            // whichever path answered. Locally the exit code is trustworthy, which is what makes this safe.
            if (run.exitCode != 0) {
                transcript.append("\n$FAILURE_PREFIX $label (exit ${run.exitCode})\n")
            }
        }
        return Outcome("local $interpreter", transcript.toString(), setUp = !timedOut, completed = !timedOut)
    }

    /**
     * Run a check inside [image] with [staged] mounted read-only at `/templates`, or null without Docker.
     *
     * `script` goes through `sh -c` for an image that must install its interpreter first; `command` is
     * used verbatim where the image already carries one.
     */
    fun runInContainer(
        image: String,
        staged: File,
        script: String? = null,
        command: List<String>? = null,
        platform: String? = null,
        environment: Map<String, String> = emptyMap()
    ): Outcome? {
        if (onPath("docker") == null) {
            return null
        }
        val invocation = mutableListOf("docker", "run", "--rm", "-v", "${staged.absolutePath}:/templates:ro")
        if (platform != null) {
            invocation += listOf("--platform", platform)
        }
        for ((key, value) in environment) {
            invocation += listOf("-e", "$key=$value")
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
        // on the template, so neither may read as a rejection.
        val reached = run.output.contains(SETUP_MARKER) && !run.timedOut
        return Outcome("container $image", run.output, setUp = reached, completed = run.output.contains(DONE_MARKER))
    }

    /**
     * Run [command] under a bound generous enough for an image pull but not for a wedged container.
     *
     * A timeout is reported rather than folded into the exit code, because an interpreter that never
     * answered says nothing about the template and must not read as a rejection.
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

    /** First executable named [executable] on the PATH, or null. */
    fun onPath(executable: String): File? =
        System.getenv("PATH")?.split(File.pathSeparator)
            ?.map { File(it, executable) }
            ?.firstOrNull { it.canExecute() }

    /** One invocation's result: how it ended, what it said, and whether it ended at all. */
    private data class Run(
        /** The process exit status, or -1 when it had to be killed. */
        val exitCode: Int,
        /** Combined stdout and stderr. */
        val output: String,
        /** True when the bound was hit, which makes the result an environment answer. */
        val timedOut: Boolean = false
    )

    /**
     * What a check produced: who ran it, how it ended, and whether the environment was ready at all.
     *
     * `setUp` exists so "the container never got its interpreter" cannot be mistaken for "the template
     * is broken" — the two have identical non-zero exits otherwise.
     */
    data class Outcome(
        /** Which interpreter answered, named in the failure message so a red run is reproducible. */
        val ranWith: String,
        /** Combined stdout and stderr, quoted verbatim on failure. */
        val output: String,
        /** Whether the interpreter was actually reached; false turns a failure into a skip. */
        val setUp: Boolean = true,
        /** Whether the script ran to its end; false turns a failure into a skip for the same reason. */
        val completed: Boolean = true
    ) {
        /** Every `FAIL …` line the script printed — empty when the templates were all accepted. */
        val failures: List<String>
            get() = output.lines().filter { it.trimStart().startsWith(FAILURE_PREFIX) }
    }

    companion object {
        /** Printed by a container once its interpreter is usable; its absence means nothing was checked. */
        const val SETUP_MARKER = "SPC-SYNTAX-CHECK-READY"

        /**
         * Printed as a script's last act, and the reason no check here reads an exit code.
         *
         * Rosetta mistranslates .NET's dynamic call sites: measured on an aarch64 host, the PowerShell
         * probe produced every correct line and *then* died at teardown with
         * `assertion failed [block != nullptr] … (BuilderBase.h:561 block_for_offset)` and exit 133, while
         * the parse check exits 0 on one run and crashes on the next. A trivial `Write-Output` exits 0
         * every time, so it is the heavier codegen that breaks, not the image. Proving completion
         * positively survives that; trusting the exit status does not.
         */
        const val DONE_MARKER = "SPC-PROBE-COMPLETE"

        /** How a script reports a rejected template, so one rule decides success on every path. */
        const val FAILURE_PREFIX = "FAIL"

        /** Bound per invocation: an emulated PowerShell pull took ~30 s measured, a wedged one never ends. */
        const val CHECK_TIMEOUT_SECONDS = 180L

        /**
         * PowerShell's image platform, named because none is published for linux/arm64.
         *
         * The tags are amd64, arm/v7 and windows/amd64, so an Apple-Silicon daemon otherwise picks arm/v7
         * and qemu dies with `uncaught target signal 11`, then hangs — a 5-minute stall measured, against
         * ~30 s with this set.
         */
        const val POWERSHELL_PLATFORM = "linux/amd64"

        /** The stock PowerShell image; carries `pwsh` and nothing this repository has to build. */
        const val POWERSHELL_IMAGE = "mcr.microsoft.com/powershell:latest"

        /** pwsh writes `$HOME/.cache` on start-up, so it needs a writable home wherever it runs. */
        val POWERSHELL_ENVIRONMENT = mapOf("HOME" to "/tmp")
    }
}
