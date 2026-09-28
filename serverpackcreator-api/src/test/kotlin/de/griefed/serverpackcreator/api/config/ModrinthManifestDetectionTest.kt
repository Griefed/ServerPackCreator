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
 * Pins that a modpack carrying `modrinth.index.json` is actually read.
 *
 * `updateConfigModelFromModrinthManifest` is complete, exported through `ConfigurationHandler`, and
 * called by nothing: `modrinth.index.json` is absent from `manifestCandidates`, so `checkManifests`
 * never dispatches to it. The user-visible result is not "we do not support Modrinth" but "Invalid
 * modloader specified", because an undetected loader leaves `PackConfig.modloader` empty and the
 * modloader validator then rejects it.
 */
internal class ModrinthManifestDetectionTest {

    @TempDir
    lateinit var tempDir: File

    private val configurationHandler =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).configurationHandler

    /** A modpack archive carrying a Modrinth index alongside the directories SPC requires. */
    private fun modrinthModpackZip(name: String): File {
        val zip = File(tempDir, "$name.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            out.putNextEntry(ZipEntry("mods/somemod.jar")); out.write(ByteArray(4)); out.closeEntry()
            out.putNextEntry(ZipEntry("config/someconfig.toml")); out.write("a = 1".toByteArray()); out.closeEntry()
            out.putNextEntry(ZipEntry("modrinth.index.json"))
            out.write(
                """
                {
                  "formatVersion": 1,
                  "game": "minecraft",
                  "versionId": "1.0.0",
                  "name": "A Modrinth Pack",
                  "dependencies": { "minecraft": "1.20.1", "fabric-loader": "0.15.7" }
                }
                """.trimIndent().toByteArray()
            )
            out.closeEntry()
        }
        return zip
    }

    @Test
    fun aModrinthIndexSuppliesTheMinecraftVersionAndModloader() {
        val packConfig = PackConfig().apply { modpackDir = modrinthModpackZip("modrinthPack").absolutePath }

        configurationHandler.checkConfiguration(packConfig)

        Assertions.assertEquals("1.20.1", packConfig.minecraftVersion)
        Assertions.assertEquals("Fabric", packConfig.modloader)
        Assertions.assertEquals("0.15.7", packConfig.modloaderVersion)
    }
}
