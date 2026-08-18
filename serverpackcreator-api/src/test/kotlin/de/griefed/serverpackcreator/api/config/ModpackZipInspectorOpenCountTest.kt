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
package de.griefed.serverpackcreator.api.config

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Guards how many times [ModpackZipInspector] reads a modpack archive's central directory.
 *
 * That read is the expensive part of inspecting an archive and it scales with the entry count —
 * measured at ~80 ms for 10,000 entries, and a real modpack export can hold far more. It is also
 * invisible from the outside: every method returns the same answer whether it opened the archive once
 * or four times, which is exactly how the duplication survived. Hence the counting opener.
 */
internal class ModpackZipInspectorOpenCountTest {

    /** How many times the inspector under test opened an archive. */
    private val opens = AtomicInteger(0)

    /** An inspector whose archive-opens are counted. */
    private fun countingInspector() = ModpackZipInspector { file ->
        opens.incrementAndGet()
        ZipFile(file)
    }

    /**
     * A modpack-shaped archive: `mods/` and `config/` in the root, so it passes validation.
     *
     * Built with `addFolder` from a real directory tree rather than `addFile` with a path-in-zip,
     * because only the former writes explicit **directory** entries — and the files/directories
     * partition is what these tests are about. (Worth knowing: `checkZipArchive` does not need them,
     * since `getDirectoriesInModpackZipBaseDirectory` derives `mods/` from a *file* entry's name.)
     */
    private fun modpackZip(tempDir: File): String {
        val pack = File(tempDir, "pack").apply { mkdirs() }
        File(pack, "mods").mkdirs()
        File(pack, "config").mkdirs()
        File(pack, "mods/somemod.jar").writeText("x")
        File(pack, "config/somemod.toml").writeText("x")
        File(pack, "config/other.toml").writeText("x")
        val zip = File(tempDir, "modpack.zip")
        ZipFile(zip).use { archive ->
            archive.addFolder(File(pack, "mods"), ZipParameters().apply { isIncludeRootFolder = true })
            archive.addFolder(File(pack, "config"), ZipParameters().apply { isIncludeRootFolder = true })
        }
        return zip.absolutePath
    }

    /**
     * Pins that validating a modpack archive reads it **once**.
     *
     * It used to read twice: once for `isNotValidZipFile()`, then again via
     * `getDirectoriesInModpackZipBaseDirectory` — two full central-directory parses where one
     * already has every header the check needs.
     */
    @Test
    fun validatingAnArchiveReadsItOnce(@TempDir tempDir: File) {
        val check = countingInspector().checkZipArchive(modpackZip(tempDir))
        Assertions.assertTrue(
            check.modpackErrors.isEmpty(),
            "precondition: a mods+config archive is valid, errors were ${check.modpackErrors}"
        )
        Assertions.assertEquals(1, opens.get(), "Validation must read the central directory once")
    }

    /**
     * Pins that listing everything in an archive reads it **once**.
     *
     * `getAllFilesAndDirectoriesInModpackZip` delegated to one method for directories and another for
     * files, each opening the archive, when a single pass over the headers partitions both.
     */
    @Test
    fun listingEverythingReadsTheArchiveOnce(@TempDir tempDir: File) {
        val inspector = countingInspector()
        val zip = File(modpackZip(tempDir))
        val everything = inspector.getAllFilesAndDirectoriesInModpackZip(zip)
        Assertions.assertTrue(everything.contains("mods/somemod.jar"), "files must be listed: $everything")
        Assertions.assertTrue(everything.any { it == "mods/" || it == "config/" }, "directories must be listed: $everything")
        Assertions.assertEquals(1, opens.get(), "Listing everything must read the central directory once")
    }

    /**
     * Pins that the single pass still separates files from directories the way the two dedicated
     * methods do — the partition must not quietly reclassify entries.
     */
    @Test
    fun theSinglePassAgreesWithTheDedicatedMethods(@TempDir tempDir: File) {
        val inspector = countingInspector()
        val zip = File(modpackZip(tempDir))
        val expected = (inspector.getDirectoriesInModpackZip(zip) + inspector.getFilesInModpackZip(zip)).sorted()
        Assertions.assertEquals(expected, inspector.getAllFilesAndDirectoriesInModpackZip(zip).sorted())
    }

    /**
     * Pins that an invalid archive is still rejected, and still costs only one read — the
     * error path must not be the thing that regains a second parse.
     */
    @Test
    fun anInvalidArchiveIsRejectedInOneRead(@TempDir tempDir: File) {
        val notAZip = File(tempDir, "broken.zip").apply { writeText("this is not a ZIP archive") }
        val check = countingInspector().checkZipArchive(notAZip.absolutePath)
        Assertions.assertTrue(check.modpackErrors.isNotEmpty(), "A non-archive must be reported")
        Assertions.assertEquals(1, opens.get(), "Rejecting an invalid archive must read it once")
    }
}
