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

/**
 * Branch coverage for [ModpackManifestParser] complementing the per-launcher happy-path tests in
 * ConfigurationHandlerTest and the characterization suite. Added here: all four modloader branches
 * of the Modrinth manifest parser, the manifest-precedence in `checkManifests` when several
 * manifests coexist, and the malformed-manifest error path. All JSON is crafted minimally so no
 * network icon-download is triggered.
 */
internal class ModpackManifestParserTest {
    private val api = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))
    private val parser = ModpackManifestParser(api.apiProperties, api.utilities)

    /**
     * Each Modrinth dependency key maps to the correct normalized modloader and version.
     */
    @Test
    fun modrinthManifestMapsEachModloader(@TempDir tempDir: File) {
        val cases = mapOf(
            "fabric-loader" to "Fabric",
            "quilt-loader" to "Quilt",
            "forge" to "Forge",
            "neoforge" to "NeoForge"
        )
        for ((dependencyKey, expectedLoader) in cases) {
            val manifest = File(tempDir, "$dependencyKey.json")
            manifest.writeText("""{"dependencies":{"minecraft":"1.20.1","$dependencyKey":"9.9.9"}}""")
            val packConfig = PackConfig()

            parser.updateConfigModelFromModrinthManifest(packConfig, manifest)

            Assertions.assertEquals(expectedLoader, packConfig.modloader, "Dependency $dependencyKey")
            Assertions.assertEquals("9.9.9", packConfig.modloaderVersion, "Dependency $dependencyKey")
            Assertions.assertEquals("1.20.1", packConfig.minecraftVersion, "Dependency $dependencyKey")
        }
    }

    /**
     * When both a CurseForge minecraftinstance.json and a manifest.json are present, the
     * minecraftinstance.json takes precedence — it is the only one that records the CurseForge
     * source and project IDs.
     */
    @Test
    fun checkManifestsPrefersMinecraftInstanceOverCurseManifest(@TempDir tempDir: File) {
        File(tempDir, "minecraftinstance.json").writeText(
            """
            {
              "baseModLoader": {"name":"forge-40.2.0","forgeVersion":"40.2.0","minecraftVersion":"1.18.2"},
              "name": "InstancePack",
              "projectID": "111",
              "fileID": "222"
            }
            """.trimIndent()
        )
        File(tempDir, "manifest.json").writeText(
            """{"minecraft":{"version":"1.18.2","modLoaders":[{"id":"forge-40.2.0"}]},"name":"ManifestPack"}"""
        )
        val packConfig = PackConfig()

        parser.checkManifests(tempDir.absolutePath, packConfig)

        Assertions.assertEquals(ModpackSource.CURSEFORGE, packConfig.source, "minecraftinstance.json must win")
        Assertions.assertEquals("InstancePack", packConfig.name)
        Assertions.assertEquals("111", packConfig.projectID)
    }

    /**
     * A malformed manifest is caught and reported as a modpack error rather than propagating.
     */
    @Test
    fun checkManifestsReportsMalformedManifest(@TempDir tempDir: File) {
        File(tempDir, "minecraftinstance.json").writeText("{ this is not valid json")
        val configCheck = ConfigCheck()

        parser.checkManifests(tempDir.absolutePath, PackConfig(), configCheck)

        Assertions.assertTrue(configCheck.modpackErrors.isNotEmpty(), "Malformed manifest must be reported")
    }
}
