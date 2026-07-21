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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Parses canned CurseForge JSON through [CurseForgePlatform], pinning the slug→modId→files flow, the
 * separation of loader-tags from Minecraft versions in `gameVersions`, and the detection of
 * distribution-locked files (a `null` download-URL).
 */
internal class CurseForgePlatformTest {

    private val searchJson = """
        {"data": [{"id": 238222, "links": {"websiteUrl": "https://www.curseforge.com/minecraft/mc-mods/jei"}}]}
    """.trimIndent()

    private val filesJson = """
        {"data": [
          {
            "id": 5000,
            "fileName": "jei-1.20.1-15.2.jar",
            "gameVersions": ["1.20.1", "Forge"],
            "downloadUrl": "https://edge.forgecdn.net/files/5000/jei.jar",
            "dependencies": [
              {"modId": 238086, "relationType": 3},
              {"modId": 999999, "relationType": 2}
            ]
          },
          {
            "id": 5001,
            "fileName": "jei-locked-1.19.2.jar",
            "gameVersions": ["1.19.2", "Fabric"],
            "downloadUrl": null,
            "dependencies": []
          }
        ]}
    """.trimIndent()

    /** A fetcher answering the search- and files-endpoints with the canned JSON above. */
    private val fetcher = HttpFetcher { url, headers ->
        Assertions.assertEquals("test-key", headers["x-api-key"])
        when {
            url.contains("/mods/search") -> searchJson
            url.contains("/mods/238222/files") -> filesJson
            else -> throw IllegalStateException("unexpected url $url")
        }
    }

    private val platform = CurseForgePlatform("test-key", fetcher)

    @Test
    fun handlesCurseForgeLinks() {
        Assertions.assertTrue(platform.handles("https://www.curseforge.com/minecraft/mc-mods/jei"))
        Assertions.assertFalse(platform.handles("https://modrinth.com/mod/jei"))
    }

    @Test
    fun resolvesFilesWithUnknownSidenessAndSeparatesLoadersFromVersions() {
        val project = platform.resolve("https://www.curseforge.com/minecraft/mc-mods/jei/files/all")

        Assertions.assertEquals("CurseForge", project.platform)
        Assertions.assertEquals(Sideness.UNKNOWN, project.clientSide)
        Assertions.assertEquals(Sideness.UNKNOWN, project.serverSide)
        Assertions.assertEquals(setOf("Fabric", "Forge"), project.loaders)

        val forgeFile = project.files.first { "Forge" in it.loaders }
        Assertions.assertEquals(setOf("1.20.1"), forgeFile.minecraftVersions)
        Assertions.assertEquals(listOf("238086"), forgeFile.requiredDependencies)
        Assertions.assertTrue(forgeFile.pageUrl!!.endsWith("/files/5000"))
    }

    @Test
    fun flagsDistributionLockedFile() {
        val project = platform.resolve("https://www.curseforge.com/minecraft/mc-mods/jei")
        val locked = project.files.first { it.fileName.contains("locked") }
        Assertions.assertTrue(locked.locked)
        Assertions.assertNull(locked.downloadUrl)
    }
}
