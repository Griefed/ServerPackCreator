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
 * Checks [TarGzWriter]'s output with the `tar` the machine actually has, not with a reader of our own.
 *
 * This module writes the TAR format itself, which is the kind of decision that has to be justified by
 * evidence rather than by reading the spec back to yourself: a pf4j plugin jar carries no dependencies,
 * nothing in this repository builds a fat plugin jar, and the host's runtime classpath has no TAR
 * writer — so a library could not be delivered to where it would run. What makes that safe is this
 * class. Every assertion here is the verdict of a real extractor, so a header byte in the wrong place
 * fails rather than round-tripping through our own mistake.
 *
 * `tar` is present on Linux, on macOS, and on Windows 10 1803 and newer, so it is not really a gate —
 * but it is treated as one, and an absent `tar` **skips**. A check that cannot run must never pass.
 *
 * @author Griefed
 */
internal class TarGzWriterTest {

    /** A path long enough to need a GNU long-name header, which mod configs reach in real packs. */
    private val longName =
        "config/a-mod-with-a-very-long-configuration-directory-name/and-another-nested-level-here/" +
                "some-really-long-configuration-file-name-v1.20.1.json5"

    /** What a server pack looks like for the purposes of this class: scripts, a mod, a long path. */
    private fun pack(): File {
        val pack = Files.createTempDirectory("spc-selfextract-pack").toFile()
        pack.deleteOnExit()
        File(pack, "start.sh").writeText("#!/bin/sh\necho started\n")
        File(pack, "install_java.sh").writeText("#!/bin/sh\necho java\n")
        File(pack, "server.properties").writeText("motd=a server\n")
        File(pack, "mods").mkdirs()
        File(pack, "mods/some-mod.jar").writeText("not really a jar")
        File(pack, longName).apply { parentFile.mkdirs(); writeText("{}") }
        return pack
    }

    /** Run [command], returning its combined output, or null when the binary is not there. */
    private fun run(vararg command: String, workingDirectory: File? = null): String? {
        val process = runCatching {
            ProcessBuilder(*command).apply { workingDirectory?.let { directory(it) } }
                .redirectErrorStream(true)
                .start()
        }.getOrNull() ?: return null
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        return output
    }

    /** Write the fixture and hand the archive back, skipping the test when there is no `tar`. */
    private fun archiveOf(pack: File): File {
        Assumptions.assumeTrue(run("tar", "--version") != null, "no tar on this machine — the archive was NOT checked")
        val archive = File.createTempFile("spc-selfextract-", ".tar.gz").apply { deleteOnExit() }
        archive.outputStream().use { TarGzWriter.write(pack, it) }
        return archive
    }

    /** Every file must come back out, with its content, under the name it went in as. */
    @Test
    fun aRealTarReadsBackEveryFile() {
        val pack = pack()
        val archive = archiveOf(pack)
        val destination = Files.createTempDirectory("spc-selfextract-out").toFile().apply { deleteOnExit() }

        val extraction = run("tar", "-xzf", archive.absolutePath, "-C", destination.absolutePath)
        Assertions.assertEquals("", extraction?.trim(), "tar complained while extracting")
        Assertions.assertEquals("#!/bin/sh\necho started\n", File(destination, "start.sh").readText())
        Assertions.assertEquals("not really a jar", File(destination, "mods/some-mod.jar").readText())
        Assertions.assertEquals("{}", File(destination, longName).readText(), "the long path did not survive")
    }

    /**
     * The start scripts must arrive executable **for everyone**, and nothing else may be.
     *
     * This is the reason the writer sets modes rather than copying them: the host sets 0544 on generated
     * scripts (`File.setExecutable(true)` is owner-only), and a pack built on Windows has no bits at all.
     */
    @Test
    fun theStartScriptsArriveExecutableAndNothingElseDoes() {
        val listing = run("tar", "-tvzf", archiveOf(pack()).absolutePath) ?: return
        val modes = listing.lines().filter { it.isNotBlank() }.associate { line ->
            line.substringAfterLast(' ') to line.take(10)
        }

        Assertions.assertEquals("-rwxr-xr-x", modes["start.sh"], "start.sh must be 0755, listing:\n$listing")
        Assertions.assertEquals("-rwxr-xr-x", modes["install_java.sh"], "install_java.sh must be 0755")
        Assertions.assertEquals("-rw-r--r--", modes["server.properties"], "an ordinary file must be 0644")
        Assertions.assertEquals("-rw-r--r--", modes["mods/some-mod.jar"], "an ordinary file must be 0644")
    }

    /**
     * Ownership must be nobody's.
     *
     * The builder's own user id would otherwise ride along into an artifact that gets published, and a
     * root extraction would chown the files to whatever that id means on the other machine.
     */
    @Test
    fun theArchiveCarriesNoOwnership() {
        val listing = run("tar", "-tvzf", archiveOf(pack()).absolutePath) ?: return

        // Asserted by what must NOT be there rather than by a column layout: GNU tar prints `0/0` and
        // bsdtar prints `0  0`, so matching the format pins the local tar, not the archive. The name
        // of whoever built it is the thing that must never appear, whichever tar reads it back.
        Assertions.assertFalse(
            listing.contains(System.getProperty("user.name")),
            "the builder's user name is in the archive, listing:\n$listing"
        )
        Assertions.assertTrue(
            listing.lines().filter { it.isNotBlank() }.all { Regex("\\s0[/ ]\\s*0\\s").containsMatchIn(it) },
            "every entry must be owned by uid 0 and gid 0, listing:\n$listing"
        )
    }

    /** The archive must be gzip, because both stubs hand it to `tar -xzf`. */
    @Test
    fun theArchiveIsGzip() {
        val archive = archiveOf(pack())
        val magic = archive.inputStream().use { byteArrayOf(it.read().toByte(), it.read().toByte()) }

        Assertions.assertArrayEquals(
            byteArrayOf(0x1f, 0x8b.toByte()),
            magic,
            "the payload must start with the gzip magic, or neither stub can unpack it"
        )
    }
}
