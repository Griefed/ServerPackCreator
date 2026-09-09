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
 * Pins that a file carries **every project its page links**, not only the ones marked required.
 *
 * `requiredDependencies` is what gets staged and must stay exactly as strict as it is. This adds a second,
 * wider list that stages nothing by itself: it is the pool of projects worth *asking what they are* when a
 * required mod id resolves to nothing — the last step of the algorithm Griefed described.
 *
 * **Why optional links have to be in it.** `Modrinth/do-a-barrel-roll` declares
 * `yet_another_config_lib_v3` under `depends` in its jar, and Modrinth lists YACL for it as **optional**.
 * The required list therefore never mentions YACL, the id resolves to nothing by spelling, and the boot
 * goes ahead without a library the loader then demands. The link was on the page the whole time.
 *
 * **Incompatible links are not in it, and that is not an oversight.** Downloading a project an author
 * declared incompatible in order to read its id would be reading the right file for the wrong reason, and
 * a match would then stage the one jar the author says must not be there.
 *
 * @author Griefed
 */
internal class LinkedDependencyTest {

    // --- Modrinth ------------------------------------------------------------------------------------

    private val modrinthVersions = """
        [{
          "version_number": "3.8.4",
          "loaders": ["fabric"],
          "game_versions": ["26.2"],
          "files": [{"filename": "do-a-barrel-roll-3.8.4.jar", "url": "https://cdn/dabr.jar", "primary": true}],
          "dependencies": [
            {"project_id": "IwCkru1D", "dependency_type": "required"},
            {"project_id": "P7dR8mSH", "dependency_type": "required"},
            {"project_id": "K01OU20C", "dependency_type": "embedded"},
            {"project_id": "1eAoo2KR", "dependency_type": "optional"},
            {"project_id": "BADBADBA", "dependency_type": "incompatible"}
          ]
        }]
    """.trimIndent()

    private val modrinthProject = """{"slug": "do-a-barrel-roll", "client_side": "required", "server_side": "optional"}"""

    /** The live shape of the reported case, dependency types and all. */
    @Test
    fun modrinthKeepsOptionalLinksBesideTheRequiredOnes() {
        val platform = ModrinthPlatform(
            HttpFetcher { url, _ ->
                if (url.contains("/version")) modrinthVersions else modrinthProject
            }
        )

        val file = platform.resolve("https://modrinth.com/mod/do-a-barrel-roll").files.single()

        Assertions.assertEquals(
            listOf("IwCkru1D", "P7dR8mSH"), file.requiredDependencies,
            "what gets staged must not widen"
        )
        Assertions.assertTrue(
            file.relatedDependencies.contains("1eAoo2KR"),
            "YACL is linked as optional and is exactly the project this pool exists for: ${file.relatedDependencies}"
        )
        Assertions.assertTrue(
            file.relatedDependencies.containsAll(listOf("IwCkru1D", "P7dR8mSH")),
            "required links belong to the pool too: ${file.relatedDependencies}"
        )
        Assertions.assertFalse(
            file.relatedDependencies.contains("BADBADBA"),
            "an incompatible link must never become something we might stage: ${file.relatedDependencies}"
        )
    }

    // --- CurseForge ----------------------------------------------------------------------------------

    private val curseForgeSearch = """{"data": [{"id": 238222, "links": {"websiteUrl": "https://www.curseforge.com/minecraft/mc-mods/jei"}}]}"""

    private val curseForgeFiles = """
        {"data": [{
          "id": 5000,
          "fileName": "jei-1.21.1.jar",
          "gameVersions": ["1.21.1", "Forge"],
          "downloadUrl": "https://edge.forgecdn.net/files/5000/jei.jar",
          "displayName": "JEI 19.0.0",
          "dependencies": [
            {"modId": 306612, "relationType": 3},
            {"modId": 667299, "relationType": 2},
            {"modId": 111111, "relationType": 1},
            {"modId": 999999, "relationType": 5}
          ]
        }]}
    """.trimIndent()

    /** CurseForge spells the same distinction with numbers: 3 required, 2 optional, 5 incompatible. */
    @Test
    fun curseForgeKeepsOptionalLinksBesideTheRequiredOnes() {
        val platform = CurseForgePlatform(
            "test-key",
            HttpFetcher { url, _ ->
                if (url.contains("/files")) curseForgeFiles else curseForgeSearch
            }
        )

        val file = platform.resolve("https://www.curseforge.com/minecraft/mc-mods/jei").files.single()

        Assertions.assertEquals(listOf("306612"), file.requiredDependencies, "what gets staged must not widen")
        Assertions.assertTrue(
            file.relatedDependencies.containsAll(listOf("306612", "667299")),
            "required and optional both: ${file.relatedDependencies}"
        )
        Assertions.assertFalse(
            file.relatedDependencies.contains("999999"),
            "relationType 5 is incompatible: ${file.relatedDependencies}"
        )
    }

    /** A file linking nothing carries an empty pool rather than a null anyone has to guard. */
    @Test
    fun afileThatLinksNothingHasAnEmptyPool() {
        Assertions.assertEquals(
            emptyList<String>(),
            ModFile("x.jar", setOf("Fabric"), setOf("26.2"), "https://cdn/x.jar", null, emptyList()).relatedDependencies
        )
    }
}
