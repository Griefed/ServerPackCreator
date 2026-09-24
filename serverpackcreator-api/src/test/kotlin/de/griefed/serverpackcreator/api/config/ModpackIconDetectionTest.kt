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
 * Pins that a modpack's own `server-icon.png` and `server.properties` are found.
 *
 * `isZip` looks for them under the *pack name* returned by `checkManifests` — a display string such as
 * "Vanilla Forge 1.16.5", not a path. `File("Vanilla Forge 1.16.5", "server-icon.png")` resolves
 * against the JVM's working directory, so for any modpack carrying a manifest the lookup can only
 * fail. The no-manifest fallback sets the pack name to the extracted directory and does work, which
 * is why this is invisible: the packs that come from CurseForge, GDLauncher and MultiMC are exactly
 * the ones that lose their icon.
 */
internal class ModpackIconDetectionTest {

    @TempDir
    lateinit var tempDir: File

    private val configurationHandler =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).configurationHandler

    /**
     * A minimal but valid modpack archive: `mods/` and `config/` at the root, an icon, a properties
     * file, and optionally a CurseForge `manifest.json` naming the pack.
     */
    private fun modpackZip(name: String, withManifest: Boolean): File {
        val zip = File(tempDir, "$name.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            out.putNextEntry(ZipEntry("mods/somemod.jar")); out.write(ByteArray(4)); out.closeEntry()
            out.putNextEntry(ZipEntry("config/someconfig.toml")); out.write("a = 1".toByteArray()); out.closeEntry()
            out.putNextEntry(ZipEntry("server-icon.png")); out.write(ByteArray(8) { 1 }); out.closeEntry()
            out.putNextEntry(ZipEntry("server.properties")); out.write("motd=hi".toByteArray()); out.closeEntry()
            if (withManifest) {
                out.putNextEntry(ZipEntry("manifest.json"))
                out.write(
                    """
                    {
                      "minecraft": { "version": "1.16.5", "modLoaders": [ { "id": "forge-36.0.1", "primary": true } ] },
                      "manifestType": "minecraftModpack",
                      "manifestVersion": 1,
                      "name": "A Manifest Named Pack",
                      "version": "1.0"
                    }
                    """.trimIndent().toByteArray()
                )
                out.closeEntry()
            }
        }
        return zip
    }

    /** Runs a full check over [zip] and hands back the config it filled in. */
    private fun checkedConfig(zip: File): PackConfig {
        val packConfig = PackConfig().apply { modpackDir = zip.absolutePath }
        configurationHandler.checkConfiguration(packConfig)
        return packConfig
    }

    @Test
    fun aModpackCarryingAManifestStillHasItsServerIconFound() {
        val packConfig = checkedConfig(modpackZip("withManifest", withManifest = true))

        Assertions.assertTrue(
            packConfig.serverIconPath.endsWith("server-icon.png"),
            "the icon was not found; serverIconPath was '${packConfig.serverIconPath}'"
        )
        Assertions.assertTrue(File(packConfig.serverIconPath).exists())
    }

    @Test
    fun aModpackCarryingAManifestStillHasItsServerPropertiesFound() {
        val packConfig = checkedConfig(modpackZip("withManifestProps", withManifest = true))

        Assertions.assertTrue(
            packConfig.serverPropertiesPath.endsWith("server.properties"),
            "server.properties was not found; path was '${packConfig.serverPropertiesPath}'"
        )
        Assertions.assertTrue(File(packConfig.serverPropertiesPath).exists())
    }

    @Test
    fun aModpackWithoutAManifestKeepsFindingItsServerIcon() {
        // The branch that already worked. Kept so the two cases discriminate: if this ever fails too,
        // the fix broke the path that was fine rather than repairing the one that was not.
        val packConfig = checkedConfig(modpackZip("withoutManifest", withManifest = false))

        Assertions.assertTrue(packConfig.serverIconPath.endsWith("server-icon.png"))
        Assertions.assertTrue(File(packConfig.serverIconPath).exists())
    }
}
