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

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pins that a modpack which cannot be extracted is refused rather than half-processed.
 *
 * `FileUtilities.unzipArchive` caught every `IOException` and only logged it, and `isZip` then carried
 * straight on — reading manifests, looking for an icon and returning success — against a directory
 * that was empty or half-written. zip4j's own zip-slip rejection is an `IOException` too, so a hostile
 * archive was swallowed by the same catch.
 */
internal class ModpackExtractionFailureTest {

    @TempDir
    lateinit var tempDir: File

    private val apiProperties =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).apiProperties
    private val configurationHandler =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).configurationHandler

    /** A minimal but valid modpack archive, named so the extraction target is predictable. */
    private fun modpackZip(name: String): File {
        val zip = File(tempDir, "$name.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            out.putNextEntry(ZipEntry("mods/somemod.jar")); out.write(ByteArray(4)); out.closeEntry()
            out.putNextEntry(ZipEntry("config/someconfig.toml")); out.write("a = 1".toByteArray()); out.closeEntry()
        }
        return zip
    }

    @Test
    fun anArchiveThatCannotBeExtractedFailsTheModpackChecks() {
        val name = "extractionIsBlocked"
        val zip = modpackZip(name)
        // Extraction must land in <modpacksDirectory>/<name>. A regular file already sitting there
        // makes creating that directory impossible, so the unzip fails for a reason no fixture has to
        // guess at -- no reliance on zip4j internals or on a hand-corrupted archive.
        apiProperties.modpacksDirectory.mkdirs()
        File(apiProperties.modpacksDirectory, name).writeText("not a directory")

        val packConfig = PackConfig().apply { modpackDir = zip.absolutePath }
        val check = configurationHandler.checkConfiguration(packConfig)

        Assertions.assertFalse(
            check.modpackChecksPassed,
            "a modpack that could not be extracted passed its own checks"
        )
    }
}
