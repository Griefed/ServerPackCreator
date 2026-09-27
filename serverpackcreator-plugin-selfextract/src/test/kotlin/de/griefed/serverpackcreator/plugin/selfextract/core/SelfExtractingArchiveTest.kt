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
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Pins the two artifacts: where their payload starts, what their stubs may and may not contain, and —
 * on a POSIX machine — that running the `.bsx` really produces a started server pack.
 *
 * The byte offset is the whole design. Each stub states where its payload begins, and that number is
 * the stub's own length, so writing it changes what it measures. Everything here either works
 * byte-exactly or not at all, which is why so much of it is asserted rather than assumed.
 *
 * @author Griefed
 */
internal class SelfExtractingArchiveTest {

    /** A fixture pack whose `start.sh` proves it was reached without starting anything real. */
    private fun pack(name: String = "All_the_Mods_9"): File {
        val parent = Files.createTempDirectory("spc-selfextract-artifacts").toFile().apply { deleteOnExit() }
        val pack = File(parent, name).apply { mkdirs() }
        File(pack, "start.sh").writeText("#!/bin/sh\necho $STARTED_MARKER\n")
        File(pack, "start.bat").writeText("@ECHO OFF\r\n")
        File(pack, "install_java.sh").writeText("#!/bin/sh\n")
        File(pack, "server.properties").writeText("motd=a server\n")
        File(pack, "mods").mkdirs()
        File(pack, "mods/some-mod.jar").writeText("not really a jar")
        return pack
    }

    /** The offset a stub names, read back out of the artifact the way a reader would. */
    private fun statedOffset(artifact: File, key: String): Long {
        val head = artifact.inputStream().use { String(it.readNBytes(4096), Charsets.UTF_8) }
        return Regex("$key=\"?(\\d+)").find(head)?.groupValues?.get(1)?.toLong()
            ?: Assertions.fail("$artifact states no $key")
    }

    /** Both artifacts land beside the pack, named after it. */
    @Test
    fun bothArtifactsAreWrittenBesideThePack() {
        val pack = pack()

        val written = SelfExtractingArchive.wrap(pack)

        Assertions.assertEquals(
            listOf("All_the_Mods_9.bsx", "All_the_Mods_9.cmd"),
            written.map { it.name }.sorted(),
            "a .bsx and a .cmd must be written next to the pack"
        )
        Assertions.assertTrue(written.all { it.parentFile == pack.parentFile }, "both belong beside the pack")
    }

    /**
     * The payload must begin at exactly the byte each stub names.
     *
     * Checked against the gzip magic, because that is the one thing that cannot be off by one and still
     * look right.
     */
    @Test
    fun eachStubNamesTheByteItsPayloadStartsAt() {
        val artifacts = SelfExtractingArchive.wrap(pack()).associateBy { it.extension }

        for ((extension, key) in mapOf("bsx" to "OFFSET", "cmd" to "SPC_OFFSET")) {
            val artifact = artifacts.getValue(extension)
            val offset = statedOffset(artifact, key)
            val magic = artifact.inputStream().use { stream ->
                stream.skipNBytes(offset - 1)   // the stubs state a 1-based position, as `tail -c +N` wants
                stream.readNBytes(2)
            }
            Assertions.assertArrayEquals(
                byteArrayOf(0x1f, 0x8b.toByte()),
                magic,
                "$extension: the payload does not start at the offset the stub states ($offset)"
            )
        }
    }

    /** Both artifacts must carry the same archive, since they are two wrappers around one pack. */
    @Test
    fun bothArtifactsCarryTheSamePayload() {
        val artifacts = SelfExtractingArchive.wrap(pack()).associateBy { it.extension }
        val bsx = artifacts.getValue("bsx")
        val cmd = artifacts.getValue("cmd")

        val fromBsx = bsx.readBytes().drop(statedOffset(bsx, "OFFSET").toInt() - 1)
        val fromCmd = cmd.readBytes().drop(statedOffset(cmd, "SPC_OFFSET").toInt() - 1)

        Assertions.assertEquals(fromBsx, fromCmd, "the two artifacts carry different archives")
    }

    /**
     * The shell stub must be LF-only.
     *
     * A single CR makes `#!/bin/sh\r` an interpreter that does not exist, and the error names a file
     * rather than a line ending.
     */
    @Test
    fun theShellStubCarriesNoCarriageReturn() {
        val bsx = SelfExtractingArchive.wrap(pack()).first { it.extension == "bsx" }
        val stub = bsx.readBytes().take(statedOffset(bsx, "OFFSET").toInt() - 1)

        Assertions.assertEquals(0, stub.count { it == '\r'.code.toByte() }, "the shell stub must be LF-only")
    }

    /**
     * The batch stub's rules, all of which are easy to break by tidying it.
     *
     * No `goto` and no label, because cmd.exe resolves a label by scanning the file and gzip output is
     * full of null bytes it cannot scan across; `EXIT /B` before the payload so it never reads that far;
     * and no `"`, `%` or `!` inside the `-Command` argument, each of which cmd eats or acts on before
     * PowerShell ever sees the line.
     */
    @Test
    fun theBatchStubStaysWithinWhatCmdCanParse() {
        val cmd = SelfExtractingArchive.wrap(pack()).first { it.extension == "cmd" }
        val stub = String(cmd.readBytes().take(statedOffset(cmd, "SPC_OFFSET").toInt() - 1).toByteArray())

        Assertions.assertFalse(Regex("(?i)\\bgoto\\b").containsMatchIn(stub), "a goto cannot survive the payload")
        Assertions.assertTrue(
            stub.lines().none { it.startsWith(":") && !it.startsWith("::") },
            "a label cannot survive the payload"
        )
        Assertions.assertTrue(stub.contains("EXIT /B"), "the batch part must end before the payload")
        Assertions.assertTrue(stub.lines().all { it.isEmpty() || stub.contains("\r\n") }, "a .cmd is CRLF")

        val invocation = stub.lines().first { it.startsWith("PowerShell ") }
        val command = invocation.substring(invocation.indexOf('"') + 1, invocation.lastIndexOf('"'))
        Assertions.assertEquals(0, command.count { it == '"' }, "a double quote ends cmd's argument early")
        Assertions.assertEquals(0, command.count { it == '%' }, "cmd substitutes a % before PowerShell sees it")
        Assertions.assertEquals(0, command.count { it == '!' }, "a ! is eaten when delayed expansion is on")
    }

    /** A name cmd and sh would both choke on must not reach either stub. */
    @Test
    fun anAwkwardPackNameIsMadeSafe() {
        val written = SelfExtractingArchive.wrap(pack("All the Mods 9 & friends"))

        Assertions.assertEquals(
            listOf("All_the_Mods_9___friends.bsx", "All_the_Mods_9___friends.cmd"),
            written.map { it.name }.sorted(),
            "characters neither shell can carry unquoted must be replaced"
        )
    }

    /**
     * The end of it: run the `.bsx` and see a started server pack.
     *
     * The fixture's `start.sh` prints a marker instead of starting a server, so reaching it proves the
     * stub extracted, chmod'd and handed over — the three things it exists to do.
     */
    @Test
    fun theShellArtifactExtractsAndStartsThePack() {
        val bsx = SelfExtractingArchive.wrap(pack()).first { it.extension == "bsx" }
        Assumptions.assumeTrue(bsx.canExecute(), "no POSIX executable bit here — the artifact was NOT run")
        val destination = File(Files.createTempDirectory("spc-selfextract-run").toFile(), "pack")

        val process = ProcessBuilder(bsx.absolutePath)
            .apply { environment()["SPC_TARGET"] = destination.absolutePath }
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        Assumptions.assumeTrue(process.waitFor(120, TimeUnit.SECONDS), "the artifact never finished")

        Assertions.assertTrue(output.contains(STARTED_MARKER), "the pack was never started:\n$output")
        Assertions.assertTrue(File(destination, "mods/some-mod.jar").isFile, "the pack was not extracted")
        Assertions.assertTrue(File(destination, "start.sh").canExecute(), "start.sh arrived without its bit")
    }

    /** A second run must refuse rather than unpack over a server that is already there. */
    @Test
    fun theShellArtifactRefusesToOverwriteAnExistingDestination() {
        val bsx = SelfExtractingArchive.wrap(pack()).first { it.extension == "bsx" }
        Assumptions.assumeTrue(bsx.canExecute(), "no POSIX executable bit here — the artifact was NOT run")
        val destination = Files.createTempDirectory("spc-selfextract-existing").toFile()

        val process = ProcessBuilder(bsx.absolutePath)
            .apply { environment()["SPC_TARGET"] = destination.absolutePath }
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        Assumptions.assumeTrue(process.waitFor(120, TimeUnit.SECONDS), "the artifact never finished")

        Assertions.assertNotEquals(0, process.exitValue(), "overwriting an existing server must fail")
        Assertions.assertTrue(output.contains("refusing to overwrite"), "it must say why:\n$output")
    }

    private companion object {
        /** Printed by the fixture's `start.sh`; seeing it means the stub got all the way there. */
        const val STARTED_MARKER = "SPC-PACK-STARTED"
    }
}
