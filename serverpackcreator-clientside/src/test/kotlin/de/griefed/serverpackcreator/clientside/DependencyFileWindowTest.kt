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
 * Pins that a dependency is looked for **at the Minecraft version being booted**, not in whatever the
 * newest page happens to hold.
 *
 * `resolveDependency` reads a single page of 50 files, deliberately — a dependency needs *a* usable file,
 * not a history, and paging every dependency of every candidate would multiply the API key's quota. But it
 * asked for the newest 50 **unfiltered**, and CurseForge returns those newest-first across every loader and
 * every Minecraft version. For a library that publishes constantly the window never reaches back far
 * enough: Fabric API has well over a thousand files on CurseForge, so its newest 50 are all current
 * Minecraft, and a boot on 1.20.4 finds nothing.
 *
 * Measured on the live daemon, 2026-09-04: `architectury-api` scored **ERROR** on Quilt / Minecraft 1.20.4
 * with *"Required dependency unavailable … 306612"*. Fabric API has published for 1.20.4 since December
 * 2023 — the file exists, it was simply outside the window we asked for.
 *
 * **This is the harmful half of that report.** The unreadable `306612` was cosmetic; this refused a boot
 * that should have run, and a staging refusal publishes ERROR over whatever the store held.
 *
 * The fix asks the API to filter (`gameVersion`), which
 * [the documented parameter set](https://docs.curseforge.com/rest-api/) supports on
 * `/v1/mods/{modId}/files`. **`modLoaderType` is deliberately not used**, even though it exists: filtering
 * to Quilt would hide Fabric API's Fabric-tagged files, which is precisely the cross-loader fallback
 * `LoaderCompatibility.alsoRuns` exists for, and Fabric API is its canonical case. Narrowing by
 * version is what shrinks the set; picking the loader stays in the selector, where the fallback lives.
 */
internal class DependencyFileWindowTest {

    private val projectJson = """
        {"data": {"id": 306612, "slug": "fabric-api",
                  "links": {"websiteUrl": "https://www.curseforge.com/minecraft/mc-mods/fabric-api"}}}
    """.trimIndent()

    /** One file, tagged for [minecraftVersion] on Fabric. */
    private fun filesJson(minecraftVersion: String) = """
        {"data": [{
          "id": 7001,
          "fileName": "fabric-api-$minecraftVersion.jar",
          "gameVersions": ["$minecraftVersion", "Fabric"],
          "downloadUrl": "https://edge.forgecdn.net/files/7001/fabric-api.jar",
          "dependencies": []
        }]}
    """.trimIndent()

    /**
     * CurseForge as it really behaves for a heavily-published library: the unfiltered newest page holds
     * only current-Minecraft builds, and the older one is reachable only by asking for it.
     */
    private val curseForge = CurseForgePlatform(
        "test-key",
        HttpFetcher { url, _ ->
            when {
                url.contains("/files") ->
                    if (url.contains("gameVersion=1.20.4")) filesJson("1.20.4") else filesJson("1.21.11")
                url.contains("/mods/306612") -> projectJson
                else -> throw IllegalStateException("unexpected url $url")
            }
        }
    )

    /** The `architectury-api` report, verbatim: the 1.20.4 file exists and must be found. */
    @Test
    fun aDependencyIsResolvedAtTheMinecraftVersionBeingBooted() {
        val resolved = curseForge.resolveDependency("306612", "1.20.4")

        Assertions.assertEquals(
            listOf("fabric-api-1.20.4.jar"), resolved?.files?.map { it.fileName },
            "the newest 50 files hold no 1.20.4 build; the API has to be asked for one"
        )
    }

    /** And the picked file really is usable for the boot, which is what the refusal turned on. */
    @Test
    fun theResolvedFileSatisfiesTheQuiltBootThatWasRefused() {
        val resolved = requireNotNull(curseForge.resolveDependency("306612", "1.20.4"))

        Assertions.assertNotNull(
            BootCandidateSelector.pickDependencyFile(resolved.files, "Quilt", "1.20.4"),
            "Quilt runs Fabric mods, so a Fabric-tagged Fabric API file satisfies a Quilt boot"
        )
    }

    /**
     * **The loader is not filtered server-side, on purpose.** Asking CurseForge for Quilt files would
     * return nothing for Fabric API and re-create the same refusal one layer down — the cross-loader
     * fallback has to see the Fabric builds to fall back to them.
     */
    @Test
    fun theLoaderIsNeverPushedIntoTheQuery() {
        var requested: String? = null
        val recording = CurseForgePlatform(
            "test-key",
            HttpFetcher { url, _ ->
                if (url.contains("/files")) requested = url
                if (url.contains("/files")) filesJson("1.20.4") else projectJson
            }
        )

        recording.resolveDependency("306612", "1.20.4")

        Assertions.assertTrue(requested?.contains("gameVersion=1.20.4") == true, "must narrow: $requested")
        Assertions.assertFalse(
            requested?.contains("modLoaderType") == true,
            "must not narrow by loader, or the Quilt-to-Fabric fallback loses its files: $requested"
        )
    }

    /** With no version to narrow by, behaviour is unchanged — the newest page, as before. */
    @Test
    fun withoutAMinecraftVersionTheQueryIsUnnarrowed() {
        val resolved = curseForge.resolveDependency("306612", null)

        Assertions.assertEquals(listOf("fabric-api-1.21.11.jar"), resolved?.files?.map { it.fileName })
    }
}
