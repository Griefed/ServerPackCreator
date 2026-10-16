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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the line endings every run-file leaves [ServerPackProvisioner.createServerRunFiles] with.
 *
 * The endings are what the file's *reader* needs, and the readers disagree: bash `source`s
 * `variables.txt`, so a carriage return ends up inside every value — `JAVA=java\r` makes
 * `"$JAVA" -version` fail as `java\r: command not found`, with a message a terminal mangles by
 * rewinding its own cursor. cmd.exe resolves a `goto`/`call` label by scanning the file and misses
 * labels that cross a 512-byte boundary when nothing resets its line counter, which bare LF does not
 * — so a batch file is the one artifact here that wants CRLF.
 *
 * The templates these are generated from are read off disk and may be edited by the operator, so the
 * endings arriving at generation are not ours to assume. Conversion therefore has to be a
 * normalisation — every convention in, one convention out — and not the removal of a single
 * character, which turns a classic-Mac template into one very long line.
 */
internal class RunFileLineEndingsTest {

    /** The placeholder substitution is not what is under test, so one key is enough to exercise it. */
    private val scriptSettings = hashMapOf("SPC_MINECRAFT_VERSION_SPC" to "1.21.1")

    /**
     * A provisioner whose templates are the files given, and nothing else.
     *
     * [ApiProperties] is mocked rather than reconfigured because the live one is backed by the shared
     * test home: pointing the real template maps at a fixture would outlive this class and change what
     * every later test generates. `createServerRunFiles` reads exactly these three properties.
     */
    private fun provisionerFor(
        startTemplates: HashMap<String, String>,
        javaTemplates: HashMap<String, String> = hashMapOf(),
        variablesTemplate: File
    ): ServerPackProvisioner {
        val apiProperties = mockk<ApiProperties>()
        every { apiProperties.startScriptTemplates } returns startTemplates
        every { apiProperties.javaScriptTemplates } returns javaTemplates
        every { apiProperties.defaultVariablesTemplate } returns variablesTemplate
        return ServerPackProvisioner(apiProperties, mockk<VersionMeta>(), mockk<Utilities>())
    }

    /** Write [content] into a file named [name] under [directory], byte for byte, and return it. */
    private fun template(directory: File, name: String, content: String): File =
        File(directory, name).apply { writeBytes(content.toByteArray()) }

    /**
     * A template using lone carriage returns must keep its lines.
     *
     * This is the case deleting `\r` cannot serve: with nothing left to separate them, every line of
     * the template becomes part of the first one, and the result is a syntactically valid file that
     * does something else entirely. Nothing reports it.
     */
    @Test
    fun aTemplateUsingLoneCarriageReturnsKeepsItsLines(@TempDir tempDir: File) {
        val templates = File(tempDir, "templates").also { it.mkdirs() }
        val destination = File(tempDir, "pack").also { it.mkdirs() }
        val start = template(templates, "classicmac.sh", "#!/usr/bin/env bash\recho one\recho two\r")

        provisionerFor(
            startTemplates = hashMapOf("sh" to start.absolutePath),
            variablesTemplate = template(templates, "variables.txt", "MINECRAFT_VERSION=SPC_MINECRAFT_VERSION_SPC\n")
        ).createServerRunFiles(scriptSettings, destination.absolutePath, true)

        Assertions.assertEquals(
            "#!/usr/bin/env bash\necho one\necho two\n",
            File(destination, "start.sh").readText(),
            "A carriage-return-separated template must be converted line for line, never collapsed into one"
        )
    }

    /**
     * Every file a POSIX shell reads is written with LF, whatever the template used.
     *
     * `variables.txt` is the one that is `source`d rather than executed, so it is listed explicitly
     * alongside the scripts: a stray carriage return there is attached to a *value* and survives into
     * whatever the script does with it.
     */
    @Test
    fun theFilesAPosixShellReadsCarryNoCarriageReturn(@TempDir tempDir: File) {
        val templates = File(tempDir, "templates").also { it.mkdirs() }
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        provisionerFor(
            startTemplates = hashMapOf(
                "sh" to template(templates, "start.sh", "#!/usr/bin/env bash\r\necho SPC_MINECRAFT_VERSION_SPC\r\n").absolutePath,
                "fish" to template(templates, "start.fish", "#!/usr/bin/env fish\r\necho one\r\n").absolutePath,
                "ps1" to template(templates, "start.ps1", "Write-Host 'one'\r\n").absolutePath
            ),
            javaTemplates = hashMapOf(
                "sh" to template(templates, "java.sh", "#!/usr/bin/env bash\r\necho install\r\n").absolutePath
            ),
            variablesTemplate = template(
                templates, "variables.txt", "MINECRAFT_VERSION=SPC_MINECRAFT_VERSION_SPC\r\nJAVA=java\r\n"
            )
        ).createServerRunFiles(scriptSettings, destination.absolutePath, true)

        for (name in listOf("start.sh", "start.fish", "start.ps1", "install_java.sh", "variables.txt", "HOW-TO-RUN.md")) {
            val written = File(destination, name).readText()
            Assertions.assertFalse(
                written.contains('\r'),
                "$name is read by a POSIX shell and must carry no carriage return; got ${written.take(80)}"
            )
        }
        Assertions.assertEquals(
            "MINECRAFT_VERSION=1.21.1\nJAVA=java\n",
            File(destination, "variables.txt").readText(),
            "variables.txt must be the template with its placeholders filled in and its endings normalised"
        )
    }

    /**
     * The batch script is written with CRLF, because cmd.exe is the one reader that wants it.
     *
     * The template on disk is LF here deliberately: the repository stores it that way, so the ending a
     * generated `start.bat` carries has to be decided at generation rather than inherited.
     */
    @Test
    fun theBatchScriptIsWrittenWithWindowsLineEndings(@TempDir tempDir: File) {
        val templates = File(tempDir, "templates").also { it.mkdirs() }
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        provisionerFor(
            startTemplates = hashMapOf(
                "bat" to template(templates, "start.bat", "@ECHO OFF\nECHO SPC_MINECRAFT_VERSION_SPC\n").absolutePath
            ),
            variablesTemplate = template(templates, "variables.txt", "JAVA=java\n")
        ).createServerRunFiles(scriptSettings, destination.absolutePath, true)

        Assertions.assertEquals(
            "@ECHO OFF\r\nECHO 1.21.1\r\n",
            File(destination, "start.bat").readText(),
            "cmd.exe loses its place in a batch file whose lines are not CRLF-terminated"
        )
    }

    /**
     * A batch template that already uses CRLF is not doubled up.
     *
     * The conversion runs over whatever the operator's template holds, so it has to normalise first and
     * expand afterwards — expanding alone turns every existing `\r\n` into `\r\r\n`.
     */
    @Test
    fun aBatchTemplateThatIsAlreadyCrlfIsNotDoubled(@TempDir tempDir: File) {
        val templates = File(tempDir, "templates").also { it.mkdirs() }
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        provisionerFor(
            startTemplates = hashMapOf(
                "bat" to template(templates, "start.bat", "@ECHO OFF\r\nECHO one\r\n").absolutePath
            ),
            variablesTemplate = template(templates, "variables.txt", "JAVA=java\n")
        ).createServerRunFiles(scriptSettings, destination.absolutePath, true)

        Assertions.assertEquals(
            "@ECHO OFF\r\nECHO one\r\n",
            File(destination, "start.bat").readText(),
            "An already-CRLF template must come out unchanged, not with a doubled carriage return"
        )
    }
}
