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
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URL

/**
 * Branch coverage for [ModpackManifestParser] complementing the per-launcher happy-path tests in
 * ConfigurationHandlerTest and the characterization suite. Added here: all four modloader branches
 * of the Modrinth manifest parser, the manifest-precedence in `checkManifests` when several
 * manifests coexist, the malformed-manifest error path, and — via a MockK-stubbed [WebUtilities]
 * download — the icon download success/failure branches of `getAndSetIcon` (driven offline through
 * the minecraftinstance.json parser). All other JSON is crafted minimally so no network
 * icon-download is triggered.
 */
internal class ModpackManifestParserTest {
    private val api = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))
    private val parser = ModpackManifestParser(api.apiProperties, api.utilities)

    /**
     * Builds a parser whose icon download outcome is fixed to [downloadSucceeds], keeping the real
     * JSON/XML utilities so manifest parsing and nested-text lookups still run for real while the
     * network download is stubbed.
     */
    private fun parserWithStubbedDownload(downloadSucceeds: Boolean): ModpackManifestParser {
        val webUtilities = mockk<WebUtilities>()
        every { webUtilities.downloadFile(any<File>(), any<URL>()) } returns downloadSucceeds
        val utilities = Utilities(webUtilities, api.utilities.jsonUtilities, api.utilities.xmlUtilities)
        return ModpackManifestParser(api.apiProperties, utilities)
    }

    /**
     * minecraftinstance.json with a thumbnail URL whose download succeeds sets the server-icon path
     * to the downloaded file under the icons directory.
     */
    @Test
    fun getAndSetIconSetsIconPathWhenDownloadSucceeds(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraftinstance.json").apply {
            writeText(
                """
                {
                  "baseModLoader": {"name":"forge-40.2.0","forgeVersion":"40.2.0","minecraftVersion":"1.18.2"},
                  "installedModpack": {"thumbnailUrl":"https://example.com/icon.png"},
                  "name": "IconPack",
                  "projectID": "111",
                  "fileID": "222"
                }
                """.trimIndent()
            )
        }
        val packConfig = PackConfig()

        parserWithStubbedDownload(downloadSucceeds = true)
            .updateConfigModelFromMinecraftInstance(packConfig, manifest)

        val expectedIcon = File(api.apiProperties.iconsDirectory.absolutePath, "IconPack.png")
        Assertions.assertEquals(expectedIcon.absolutePath, packConfig.serverIconPath)
    }

    /**
     * minecraftinstance.json with a thumbnail URL whose download fails leaves the server-icon path
     * unset.
     */
    @Test
    fun getAndSetIconLeavesIconPathUnsetWhenDownloadFails(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraftinstance.json").apply {
            writeText(
                """
                {
                  "baseModLoader": {"name":"forge-40.2.0","forgeVersion":"40.2.0","minecraftVersion":"1.18.2"},
                  "installedModpack": {"thumbnailUrl":"https://example.com/icon.png"},
                  "name": "IconPack",
                  "projectID": "111",
                  "fileID": "222"
                }
                """.trimIndent()
            )
        }
        val packConfig = PackConfig()

        parserWithStubbedDownload(downloadSucceeds = false)
            .updateConfigModelFromMinecraftInstance(packConfig, manifest)

        Assertions.assertEquals("", packConfig.serverIconPath, "Icon path must stay unset on failed download")
    }

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
