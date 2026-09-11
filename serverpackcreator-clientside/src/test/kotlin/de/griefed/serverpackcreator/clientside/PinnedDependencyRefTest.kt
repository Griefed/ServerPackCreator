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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the two ways a dependency ref could go missing or be fabricated at the platform boundary.
 *
 * **Modrinth: a version-pinned dependency was silently dropped.** An entry may carry a `version_id` and a
 * **null** `project_id` — an author pinning one exact build. `filesOf` read only `project_id`, so such an
 * entry vanished from `requiredDependencies` *and* `relatedDependencies`, with no log: the dependency was
 * never staged, `askLinkedProjects` could not recover it either (it reads the same list), and the loader
 * then refused the pack with the **candidate** wearing the verdict. One GET of `/version/{id}` answers
 * which project it is.
 *
 * **CurseForge: a null `modId` became the literal string `"null"`.** `asText()` on a JSON-null returns
 * `"null"`, which is the documented hazard `textOrNull` exists for and the one place it was still live. A
 * ref of `"null"` is resolved, missed, and reported as an unmet dependency named `null` — noise that reads
 * like a real finding.
 *
 * Both are asserted through the **real** platform classes over canned JSON, because both defects are in
 * how a response is read: a test building `ModFile`s cannot see a producer dropping or inventing one.
 *
 * @author Griefed
 */
internal class PinnedDependencyRefTest {

    /** Every URL the platform fetched, so the cost of resolving a pin is asserted rather than assumed. */
    private val requested = mutableListOf<String>()

    private val modrinthVersions = """
        [
          {"version_number":"1.0.0","version_type":"release","loaders":["fabric"],"game_versions":["1.20.1"],
           "files":[{"filename":"some-mod-1.0.0.jar","primary":true,"url":"https://cdn/some-mod.jar"}],
           "dependencies":[
             {"project_id":null,"version_id":"AAAAAAAA","dependency_type":"required"},
             {"project_id":null,"version_id":null,"dependency_type":"required"}
           ]},
          {"version_number":"0.9.0","version_type":"release","loaders":["fabric"],"game_versions":["1.20.1"],
           "files":[{"filename":"some-mod-0.9.0.jar","primary":true,"url":"https://cdn/some-mod-0.9.jar"}],
           "dependencies":[{"project_id":null,"version_id":"AAAAAAAA","dependency_type":"required"}]}
        ]
    """.trimIndent()

    private val modrinth = ModrinthPlatform(
        HttpFetcher { url, _ ->
            requested.add(url)
            when {
                url.endsWith("/version") -> modrinthVersions
                url.contains("/version/AAAAAAAA") -> """{"id":"AAAAAAAA","project_id":"P7dR8mSH"}"""
                else -> """{"slug":"some-mod","client_side":"required","server_side":"required"}"""
            }
        },
        ObjectMapper()
    )

    /** **The dropped dependency.** A pinned build still names a project, and it has to be staged. */
    @Test
    fun aVersionPinnedModrinthDependencyResolvesToItsProject() {
        val files = modrinth.resolve("https://modrinth.com/mod/some-mod").files

        Assertions.assertEquals(
            listOf("P7dR8mSH"), files.first().requiredDependencies,
            "an author pinning one exact build still declared a required dependency"
        )
        Assertions.assertEquals(
            listOf("P7dR8mSH"), files.first().relatedDependencies,
            "and the linked list must carry it too, or askLinkedProjects cannot recover it either"
        )
    }

    /**
     * **One lookup, however many versions pin it.** A project publishes hundreds of versions and `resolve`
     * reads every one of them, so an un-memoised lookup would be a request per version — the cost shape
     * this module already paid for once with CurseForge's paging.
     */
    @Test
    fun thePinIsLookedUpOnce() {
        modrinth.resolve("https://modrinth.com/mod/some-mod")

        Assertions.assertEquals(
            1, requested.count { it.contains("/version/AAAAAAAA") },
            "two versions pin the same build; it is one fact: $requested"
        )
    }

    /** A dependency naming neither a project nor a version is nothing at all, and stays dropped. */
    @Test
    fun aDependencyNamingNothingIsStillDropped() {
        val files = modrinth.resolve("https://modrinth.com/mod/some-mod").files

        Assertions.assertEquals(
            1, files.first().requiredDependencies.size,
            "the second entry names no project and no version: there is nothing to resolve"
        )
    }

    /** **The fabricated ref.** A JSON-null `modId` is absent, not a dependency called `null`. */
    @Test
    fun aNullCurseForgeModIdIsNotADependency() {
        val curseForge = CurseForgePlatform(
            "test-key",
            HttpFetcher { url, _ ->
                when {
                    url.contains("/files") -> """
                        {"data":[{"id":1,"fileName":"some-mod-1.0.0.jar","gameVersions":["1.20.1","Forge"],
                                  "releaseType":1,"downloadUrl":"https://edge/some-mod.jar",
                                  "dependencies":[{"modId":null,"relationType":3},
                                                  {"modId":306612,"relationType":3}]}],
                         "pagination":{"index":0,"resultCount":1,"totalCount":1}}
                    """.trimIndent()

                    else -> """{"data":[{"id":900,"slug":"some-mod","links":{"websiteUrl":"https://cf/some-mod"}}]}"""
                }
            },
            ObjectMapper()
        )

        val file = curseForge.resolve("https://www.curseforge.com/minecraft/mc-mods/some-mod").files.single()

        Assertions.assertEquals(
            listOf("306612"), file.requiredDependencies,
            "`asText()` on a JSON-null yields the literal \"null\", which is then reported as a missing mod"
        )
        Assertions.assertEquals(listOf("306612"), file.relatedDependencies)
    }
}
