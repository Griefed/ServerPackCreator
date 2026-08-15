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
 * Parses canned Modrinth JSON through [ModrinthPlatform] so the mapping of API-shapes onto
 * [ProjectFiles] is pinned without touching the network.
 */
internal class ModrinthPlatformTest {

    private val projectJson = """
        {
          "slug": "jei",
          "client_side": "required",
          "server_side": "unsupported",
          "loaders": ["fabric", "forge"],
          "game_versions": ["1.20.1"]
        }
    """.trimIndent()

    private val versionsJson = """
        [
          {
            "loaders": ["fabric"],
            "game_versions": ["1.20.1"],
            "dependencies": [
              {"project_id": "P7dR8mSH", "dependency_type": "required"},
              {"project_id": "ignored", "dependency_type": "optional"}
            ],
            "files": [{"filename": "jei-fabric-1.20.1-15.2.jar", "url": "https://cdn.modrinth.com/jei-fabric.jar"}]
          },
          {
            "loaders": ["forge"],
            "game_versions": ["1.20.1"],
            "dependencies": [],
            "files": [{"filename": "jei-forge-1.20.1-15.2.jar", "url": "https://cdn.modrinth.com/jei-forge.jar"}]
          }
        ]
    """.trimIndent()

    /** A fetcher answering the project- and versions-endpoints with the canned JSON above. */
    private val fetcher = HttpFetcher { url, _ ->
        when {
            url.endsWith("/version") -> versionsJson
            url.contains("/project/") -> projectJson
            else -> throw IllegalStateException("unexpected url $url")
        }
    }

    private val platform = ModrinthPlatform(fetcher)

    @Test
    fun handlesModrinthLinks() {
        Assertions.assertTrue(platform.handles("https://modrinth.com/mod/jei"))
        Assertions.assertFalse(platform.handles("https://www.curseforge.com/minecraft/mc-mods/jei"))
    }

    @Test
    fun resolvesDeclaredSidenessAndFiles() {
        val project = platform.resolve("https://modrinth.com/mod/jei/versions")

        Assertions.assertEquals("Modrinth", project.platform)
        Assertions.assertEquals("jei", project.slug)
        Assertions.assertEquals(DeclaredSupport.REQUIRED, project.clientSide)
        Assertions.assertEquals(DeclaredSupport.UNSUPPORTED, project.serverSide)
        Assertions.assertEquals(setOf("Fabric", "Forge"), project.loaders)
        Assertions.assertEquals(2, project.files.size)
    }

    @Test
    fun keepsOnlyRequiredDependenciesAndMarksFilesDownloadable() {
        val fabricFile = platform.resolve("https://modrinth.com/mod/jei").files
            .first { "Fabric" in it.loaders }
        Assertions.assertEquals(listOf("P7dR8mSH"), fabricFile.requiredDependencies)
        Assertions.assertFalse(fabricFile.locked)
        Assertions.assertEquals("https://cdn.modrinth.com/jei-fabric.jar", fabricFile.downloadUrl)
    }
}
