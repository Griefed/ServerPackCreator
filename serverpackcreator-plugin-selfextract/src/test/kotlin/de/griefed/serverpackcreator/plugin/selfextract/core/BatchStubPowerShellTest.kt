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
package de.griefed.serverpackcreator.plugin.selfextract.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Asks a real PowerShell whether the batch stub's one-liner is even a program.
 *
 * Everything else about the Windows artifact can be checked structurally, and is. This cannot: the
 * `-Command` argument is one long line of PowerShell assembled in Kotlin, and a missing brace or a
 * stray quote in it produces a `.cmd` that looks perfect and fails on a user's machine with an error
 * about something else. It is also the half of this plugin that nothing here can *run* — cmd.exe's own
 * behaviour needs Windows — so the parser is the most that can be asked, and asking it is cheap.
 *
 * The parser, deliberately, and not `-Command` itself: `ParseInput` reads the script without executing
 * a line of it, so nothing here can touch the machine it runs on.
 *
 * Skips without Docker rather than passing. The image is pinned to `linux/amd64` because no arm64 tag
 * is published and an Apple-Silicon daemon otherwise picks arm/v7, where qemu dies and then hangs.
 *
 * @author Griefed
 */
internal class BatchStubPowerShellTest {

    /** The PowerShell out of a freshly written batch stub, exactly as cmd.exe would hand it over. */
    private fun oneLiner(): String {
        val stub = Stubs.batch("All_the_Mods_9").replace(Stubs.OFFSET_PLACEHOLDER, "1631")
        val invocation = stub.lines().first { it.startsWith("PowerShell ") }
        return invocation.substring(invocation.indexOf('"') + 1, invocation.lastIndexOf('"'))
    }

    /** The one-liner must parse, and the check must prove it read something before saying so. */
    @Test
    fun theBatchStubsPowerShellParses() {
        Assumptions.assumeTrue(onPath("docker") != null, "no docker binary — the batch stub was NOT checked")
        val source = oneLiner()
        Assertions.assertTrue(source.length > 200, "the one-liner was not extracted; the check would be vacuous")

        val encoded = Base64.getEncoder().encodeToString(source.toByteArray(Charsets.UTF_8))
        // Asserting the length inside the container too: a base64 that arrives empty parses perfectly,
        // which is exactly how a check like this reports success for having read nothing.
        val probe = "\$source = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('$encoded')); " +
                "Write-Output ('CHARS:' + \$source.Length); " +
                "\$errs = \$null; " +
                "[System.Management.Automation.Language.Parser]::ParseInput(\$source, [ref]\$null, [ref]\$errs) | Out-Null; " +
                "if (\$errs.Count -gt 0) { \$errs | ForEach-Object { Write-Output ('FAIL ' + \$_.Message) } } else { Write-Output 'PARSED' }"

        val output = run(
            "docker", "run", "--rm", "--platform", "linux/amd64", "-e", "HOME=/tmp",
            "mcr.microsoft.com/powershell:latest", "pwsh", "-NoProfile", "-Command", probe
        )
        Assumptions.assumeTrue(output != null, "the PowerShell container did not answer — nothing was checked")
        Assumptions.assumeTrue(
            output!!.contains("CHARS:${source.length}"),
            "the container did not receive the one-liner intact, so nothing was checked:\n$output"
        )

        Assertions.assertTrue(
            output.lineSequence().any { it.trim() == "PARSED" },
            "the batch stub's PowerShell does not parse:\n$output"
        )
    }

    /** First executable named [executable] on the PATH, or null. */
    private fun onPath(executable: String): File? =
        System.getenv("PATH")?.split(File.pathSeparator)
            ?.map { File(it, executable) }
            ?.firstOrNull { it.canExecute() }

    /** Run [command] and return its combined output, or null when it could not run or did not finish. */
    private fun run(vararg command: String): String? {
        val process = runCatching { ProcessBuilder(*command).redirectErrorStream(true).start() }
            .getOrNull() ?: return null
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(300, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        return output
    }
}
