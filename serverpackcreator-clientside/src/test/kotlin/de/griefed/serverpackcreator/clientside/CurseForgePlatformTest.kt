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
        Assertions.assertEquals(DeclaredSupport.UNKNOWN, project.clientSide)
        Assertions.assertEquals(DeclaredSupport.UNKNOWN, project.serverSide)
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

    // --- paging the files endpoint ------------------------------------------------------------------

    /**
     * Serves [totalCount] synthetic Forge files, [PAGE_SIZE] at a time, echoing CurseForge's own `pagination`
     * block — and records every `index` it was asked for, which is the only way to tell "paged" from
     * "happened to get everything in one call".
     */
    private class PagingFilesFetcher(private val totalCount: Int, private val claimedTotal: Int = totalCount) : HttpFetcher {
        val requestedIndices = mutableListOf<Int>()

        override fun get(url: String, headers: Map<String, String>): String {
            if (url.contains("/mods/search")) {
                return """{"data": [{"id": 228756, "links": {"websiteUrl": "https://www.curseforge.com/x/y/z"}}]}"""
            }
            if (!url.contains("/files")) {
                // The single mod-node lookup a dependency starts with; only /files calls are paged.
                return """{"data": {"id": 228756, "links": {"websiteUrl": "https://www.curseforge.com/x/y/z"}}}"""
            }
            val index = Regex("index=(\\d+)").find(url)?.groupValues?.get(1)?.toInt() ?: 0
            requestedIndices.add(index)
            val files = (index until minOf(index + PAGE_SIZE, totalCount)).joinToString(",") { fileId ->
                """{"id": $fileId, "fileName": "themod-$fileId.jar", "gameVersions": ["1.20.1", "Forge"],
                    "downloadUrl": "https://cdn/$fileId.jar", "dependencies": []}"""
            }
            return """{"data": [$files],
                "pagination": {"index": $index, "pageSize": $PAGE_SIZE, "resultCount": ${maxOf(0, minOf(index + PAGE_SIZE, totalCount) - index)}, "totalCount": $claimedTotal}}"""
        }
    }

    /**
     * A project's older builds have to be reachable, not just its newest 50 files. The case that forced this:
     * a mod that migrated Forge → NeoForge keeps publishing NeoForge builds, so its last *Forge* build sinks
     * toward the far end of the window and the ones before it fall out of it entirely — leaving the crash
     * re-check with nothing of that loader to boot, precisely for the projects that produce the false HIGH.
     */
    @Test
    fun resolvePagesThroughEveryPublishedFile() {
        val fetcher = PagingFilesFetcher(totalCount = 130)

        val project = CurseForgePlatform("test-key", fetcher).resolve("https://www.curseforge.com/minecraft/mc-mods/iron-chests")

        Assertions.assertEquals(130, project.files.size, "every published file must be resolved, not the newest page")
        Assertions.assertEquals(listOf(0, 50, 100), fetcher.requestedIndices, "pages are walked in order, once each")
    }

    /** `totalCount` says when to stop, so a project inside one page still costs exactly one call. */
    @Test
    fun aProjectThatFitsInOnePageCostsOneCall() {
        val fetcher = PagingFilesFetcher(totalCount = 12)

        CurseForgePlatform("test-key", fetcher).resolve("https://www.curseforge.com/minecraft/mc-mods/small")

        Assertions.assertEquals(listOf(0), fetcher.requestedIndices)
    }

    /**
     * A `totalCount` that never arrives must not spin forever — CurseForge answering oddly, or a response
     * shape changing, has to cost a bounded number of calls against the key's quota. The cap is stated in the
     * platform's own constant rather than duplicated here.
     */
    @Test
    fun aTotalCountThatIsNeverReachedStopsAtTheCap() {
        val fetcher = PagingFilesFetcher(totalCount = 10_000, claimedTotal = Int.MAX_VALUE)

        val project = CurseForgePlatform("test-key", fetcher).resolve("https://www.curseforge.com/minecraft/mc-mods/endless")

        Assertions.assertEquals(CurseForgePlatform.MAX_FILE_PAGES, fetcher.requestedIndices.size)
        Assertions.assertEquals(CurseForgePlatform.MAX_FILE_PAGES * PAGE_SIZE, project.files.size)
    }

    /**
     * A dependency is deliberately **not** paged: it only needs *a* usable file for the loader and Minecraft
     * being booted, and paging every dependency of every candidate would multiply the API calls a catalog
     * sweep spends for evidence nobody reads.
     */
    @Test
    fun aDependencyIsResolvedFromASinglePage() {
        val fetcher = PagingFilesFetcher(totalCount = 130)

        val dependency = CurseForgePlatform("test-key", fetcher).resolveDependency("228756")

        // resolveDependency swallows its failures, so assert it actually resolved — otherwise "one page" would
        // also be satisfied by it having thrown on the first call.
        Assertions.assertEquals(PAGE_SIZE, dependency?.files?.size)
        Assertions.assertEquals(listOf(0), fetcher.requestedIndices)
    }

    private companion object {
        /** CurseForge's maximum `pageSize` for the files endpoint, and therefore the platform's page size. */
        const val PAGE_SIZE = 50
    }
}
