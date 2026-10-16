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
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.FAILURE_PREFIX
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.SETUP_MARKER
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * **Executes** every shipped POSIX template's `loadVariables` against a `variables.txt` that has been
 * through a text-mangling tool, and asserts the settings still arrive intact.
 *
 * ServerPackCreator writes that file LF-only, without a byte-order mark, and cannot keep it that way: it
 * travels. An FTP client in ASCII or "auto" mode rewrites `.txt` on upload, a hosting panel's web editor
 * saves what its browser gave it, and an editor on Windows adds what it likes. The start script ships
 * with the pack, so it is the last point of control.
 *
 * Both damage types fail quietly and neither looks like what it is:
 * - a carriage return is attached to every value, so `"$JAVA" -version` fails as
 *   `java\r: command not found` — a message a terminal mangles by rewinding its own cursor over it — and
 *   `[[ "$WAIT_FOR_USER_INPUT" == "true" ]]` is simply false however the operator set it;
 * - a byte-order mark sticks to the **first** setting's name, so `JAVA` becomes a name nothing reads and
 *   the operator's Java choice is ignored while every later setting still works.
 *
 * Each case is covered for each shell because they fail differently and a fix for one is not a fix for
 * the other. Measured against the shipped functions before any of this landed: bash broke on CRLF, on
 * lone CR and on a byte-order mark; fish survived CRLF already — `string trim` counts a carriage return
 * as whitespace — and broke on lone CR, which its `read` hands over as one single line, and on the mark,
 * which `string trim` does not treat as whitespace.
 *
 * @author Griefed
 */
internal class VariablesFileLoadingTest {

    private val runner = TemplateInterpreterRunner()

    /** What every run below must end up with, whatever was done to the file in transit. */
    private val expected = mapOf(
        "JAVA" to "java",
        "WAIT_FOR_USER_INPUT" to "true",
        "JAVA_ARGS" to "-Xmx4G -Xms4G"
    )

    /**
     * The fixture, as a shipped `variables.txt` looks: comments, a quoted value, a blank line.
     *
     * `JAVA` is deliberately the **first** setting, because a byte-order mark only ever damages that one —
     * a fixture whose first line were a comment would be read correctly by a loader that handles nothing
     * at all.
     */
    private fun variablesFile(damage: Damage): ByteArray {
        val body = listOf(
            "JAVA=java",
            "",
            "# Whether to wait for user input before closing.",
            "WAIT_FOR_USER_INPUT=true",
            "JAVA_ARGS=\"-Xmx4G -Xms4G\"",
            ""
        ).joinToString(damage.separator)
        return (if (damage.byteOrderMark) BYTE_ORDER_MARK + body else body).toByteArray()
    }

    /**
     * bash reads every damaged form, executed locally.
     *
     * The harness extracts the shipped `loadVariables` rather than restating it, so a change to how a
     * template loads its settings is caught here instead of being sidestepped.
     */
    @Test
    fun theBashTemplatesLoadVariablesHoweverTheFileArrived() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed - variables-loading check skipped")
        val workDir = File.createTempFile("spc-variables-", "-pack").apply { delete(); mkdirs() }

        for (template in BASH_TEMPLATES) {
            for (damage in DAMAGE) {
                File(workDir, "variables.txt").writeBytes(variablesFile(damage))
                val harness = File(workDir, "harness.sh")
                harness.writeText(
                    """
                    ${extractShellFunction(template, "loadVariables")}
                    loadVariables "./variables.txt"
                    ${expected.keys.joinToString("\n") { "echo \"$it=[${'$'}{$it}]\"" }}
                    """.trimIndent()
                )

                val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
                    .directory(workDir)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exit = process.waitFor()

                Assertions.assertEquals(
                    0, exit,
                    "$template: loadVariables failed on a ${damage.label} variables.txt:\n$output"
                )
                for ((key, value) in expected) {
                    Assertions.assertTrue(
                        output.contains("$key=[$value]"),
                        "$template read $key wrongly from a ${damage.label} variables.txt - expected [$value]:\n$output"
                    )
                }
            }
        }
        workDir.deleteRecursively()
    }

    /**
     * fish reads every damaged form, through the real interpreter — installed here, or a container.
     */
    @Test
    fun theFishTemplatesLoadVariablesHoweverTheFileArrived() {
        val staged = runner.stageTemplates(FISH_TEMPLATES)
        for (damage in DAMAGE) {
            File(staged, "variables.${damage.label}.txt").writeBytes(variablesFile(damage))
        }
        // The templates refuse to be sourced (each guards its own execution), so the function under test
        // is staged on its own — extracted here rather than in fish, for the same reason bash's is.
        for (template in FISH_TEMPLATES) {
            runner.stageScript(staged, "$template.loadVariables", extractFishFunction(template, "loadVariables"))
        }
        runner.stageScript(staged, "probe.fish", fishProbe())

        val outcome = runner.runLocally("fish", mapOf("probe" to listOf("fish", File(staged, "probe.fish").absolutePath)))
            ?: runner.runInContainer(
                image = ALPINE_IMAGE,
                staged = staged,
                script = "apk add --no-cache fish >/dev/null 2>&1 || exit 90; echo $SETUP_MARKER; " +
                        "fish /templates/probe.fish"
            )
            ?: Assumptions.abort("neither fish nor docker is available - fish variables-loading check skipped")

        Assumptions.assumeTrue(outcome.setUp, "fish could not be reached (${outcome.ranWith}):\n${outcome.output}")
        Assertions.assertTrue(
            outcome.failures.isEmpty(),
            "fish read a value wrongly (${outcome.ranWith}):\n${outcome.output}"
        )
        Assertions.assertTrue(
            outcome.output.contains(DONE_MARKER),
            "the fish probe did not run to completion (${outcome.ranWith}):\n${outcome.output}"
        )
        staged.deleteRecursively()
    }

    /**
     * A fish probe that loads every fixture through each template's own `loadVariables` and reports per
     * value, printing [DONE_MARKER] only once every case has been read.
     *
     * The verdict is the markers it prints, never an exit code: an interpreter the probe had to install
     * itself is not trustworthy enough to be read by exit status alone, which is the rule
     * [TemplateInterpreterRunner] exists to carry.
     *
     * `functions --erase` between templates is load-bearing rather than tidy. fish keeps a function
     * defined when a later `source` fails, so the first template's working copy graded the second one's
     * and the guard passed against a deliberately corrupted installer template — found by mutating it.
     */
    private fun fishProbe(): String {
        val checks = expected.entries.joinToString("\n") { (key, value) ->
            """
            |        if test "$$key" != "$value"
            |            echo "$FAILURE_PREFIX ${'$'}template/${'$'}label: $key is '$$key', expected '$value'"
            |        end
            """.trimMargin()
        }
        val clears = expected.keys.joinToString("\n") { "        set -e $it" }
        return """
            |for template in ${FISH_TEMPLATES.joinToString(" ")}
            |    functions --erase loadVariables
            |    if not source /templates/${'$'}template.loadVariables
            |        echo "$FAILURE_PREFIX ${'$'}template: loadVariables could not be sourced"
            |        continue
            |    end
            |    if not functions --query loadVariables
            |        echo "$FAILURE_PREFIX ${'$'}template: loadVariables is not defined after sourcing it"
            |        continue
            |    end
            |    for label in ${DAMAGE.joinToString(" ") { it.label }}
            |$clears
            |        loadVariables "/templates/variables.${'$'}label.txt"
            |$checks
            |    end
            |end
            |echo "$DONE_MARKER"
        """.trimMargin()
    }

    /** Locate an executable on `PATH`, or `null` when it is not installed. */
    private fun which(executable: String): File? =
        System.getenv("PATH")?.split(File.pathSeparator)
            ?.map { File(it, executable) }
            ?.firstOrNull { it.canExecute() }

    /** Cut the named bash function out of a shipped template, so the guard drives the real thing. */
    private fun extractShellFunction(template: String, name: String): String {
        val lines = templateLines(template)
        val start = lines.indexOfFirst { it.startsWith("$name()") }
        Assertions.assertTrue(start >= 0, "template $template has no function `$name` - update this test")
        val end = lines.drop(start + 1).indexOfFirst { it == "}" }
        Assertions.assertTrue(end >= 0, "function `$name` in $template is not closed by a brace in column 0")
        return lines.subList(start, start + end + 2).joinToString("\n")
    }

    /** Cut the named fish function out of a shipped template — `function <name>` to its closing `end`. */
    private fun extractFishFunction(template: String, name: String): String {
        val lines = templateLines(template)
        val start = lines.indexOfFirst { it.startsWith("function $name") }
        Assertions.assertTrue(start >= 0, "template $template has no function `$name` - update this test")
        val end = lines.drop(start + 1).indexOfFirst { it == "end" }
        Assertions.assertTrue(end >= 0, "function `$name` in $template is not closed by an `end` in column 0")
        return lines.subList(start, start + end + 2).joinToString("\n")
    }

    /** The shipped template, by name, as lines — read from the classpath rather than from the source tree. */
    private fun templateLines(template: String): List<String> =
        (javaClass.getResourceAsStream("/de/griefed/resources/server_files/$template")
            ?.bufferedReader()?.use { it.readText() }
            ?: Assertions.fail("template resource '$template' is not on the classpath")).lines()

    /**
     * One way a `variables.txt` can reach a server after something rewrote it.
     *
     * [label] names the fixture file and appears in every failure message, so a red run says which
     * damage the loader could not cope with rather than only that one of them beat it.
     */
    private data class Damage(val label: String, val separator: String, val byteOrderMark: Boolean)

    private companion object {
        /** The character three invisible bytes at the start of a Windows-written file decode to. */
        const val BYTE_ORDER_MARK = "﻿"

        /** Every combination of line-ending convention and leading byte-order mark. */
        val DAMAGE = listOf(
            Damage("lf", "\n", byteOrderMark = false),
            Damage("crlf", "\r\n", byteOrderMark = false),
            Damage("cr", "\r", byteOrderMark = false),
            Damage("bom_lf", "\n", byteOrderMark = true),
            Damage("bom_crlf", "\r\n", byteOrderMark = true),
            Damage("bom_cr", "\r", byteOrderMark = true)
        )

        /** Every shipped bash template that reads `variables.txt` on its own. */
        val BASH_TEMPLATES = listOf("default_template.sh", "default_java_template.sh")

        /**
         * The fish half of the same list.
         *
         * The installer scripts carry their own copy of `loadVariables` and read `variables.txt`
         * themselves, because they are runnable standalone as well as sourced by the start script — so a
         * fix applied only to the start-script templates would leave half the readers unfixed.
         */
        val FISH_TEMPLATES = listOf("default_template.fish", "default_java_template.fish")

        /** Carries fish in its own repositories, so the fallback needs no third-party source. */
        const val ALPINE_IMAGE = "alpine:3.20"
    }
}
