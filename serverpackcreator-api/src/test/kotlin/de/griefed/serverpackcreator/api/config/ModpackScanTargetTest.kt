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
import java.io.File
import java.nio.file.Path

/**
 * Pins *what* the malware scan is pointed at, which the scanner itself can never tell you.
 *
 * A scan reports its findings, never its target, so scanning the wrong path looks exactly like
 * scanning a clean modpack. For a ZIP source `modpackDir` names the archive until [ConfigurationHandler.isZip]
 * has extracted it, and Nekodetector walks directories — so a scan taken too early walks nothing and
 * still logs "Performing Nekodetector scan". Every upload the webservice accepts is a ZIP.
 */
internal class ModpackScanTargetTest {

    private val configurationHandler =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).configurationHandler

    /** Runs a check over [modpackDir], recording every path the scan is handed. */
    private fun pathsScannedFor(modpackDir: String): List<Path> {
        val scanned = mutableListOf<Path>()
        val packConfig = PackConfig().apply { this.modpackDir = modpackDir }
        configurationHandler.checkConfiguration(packConfig, ConfigCheck(), false) { path ->
            scanned.add(path)
            emptyList()
        }
        return scanned
    }

    @Test
    fun aZipModpackIsScannedAfterItHasBeenExtracted() {
        val scanned = pathsScannedFor("src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip")

        Assertions.assertEquals(1, scanned.size, "a ZIP upload was never handed to the malware scan")
        Assertions.assertTrue(
            scanned.single().toFile().isDirectory,
            "the scan was pointed at ${scanned.single()}, which is not a directory — Nekodetector walks trees"
        )
    }

    @Test
    fun aDirectoryModpackIsStillScanned() {
        val scanned = pathsScannedFor("src/test/resources/forge_tests")

        Assertions.assertEquals(1, scanned.size)
        Assertions.assertTrue(scanned.single().toFile().isDirectory)
    }
}
