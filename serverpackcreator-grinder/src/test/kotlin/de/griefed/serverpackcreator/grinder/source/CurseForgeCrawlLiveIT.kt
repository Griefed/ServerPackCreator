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
package de.griefed.serverpackcreator.grinder.source

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.clientside.JdkHttpFetcher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Live-API checks for the **CurseForge** crawl, gated behind `GRINDER_CF_IT=1` *and* a present
 * `CURSEFORGE_API_KEY`, so a normal run skips them.
 *
 * These exist because the partitioned crawl was designed from CurseForge's published documentation alone, and
 * the first real run showed the docs were not enough: `pagination.totalCount` turns out to **saturate at the
 * paging cap**, which made every split condition unreachable, and `/games/{id}/versions` returns 7 335 strings
 * of which only 135 are Minecraft versions. Both were silent — the crawl would simply have covered the top
 * 10 000 of each version forever. Every assertion below therefore pins a *measured* platform behaviour that the
 * design depends on, so the next such change fails a test instead of quietly shrinking coverage.
 *
 * Deliberately cheap (a few dozen small search calls, no downloads, no containers) because an API key carries a
 * request quota. Run with:
 * `CURSEFORGE_API_KEY=… GRINDER_CF_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*CurseForgeCrawlLiveIT"`
 */
@EnabledIfEnvironmentVariable(named = "GRINDER_CF_IT", matches = "1")
@EnabledIfEnvironmentVariable(named = "CURSEFORGE_API_KEY", matches = ".+")
internal class CurseForgeCrawlLiveIT {

    @TempDir
    lateinit var tempDir: Path

    private val apiKey = System.getenv("CURSEFORGE_API_KEY") ?: ""
    private val fetcher = JdkHttpFetcher()
    private val mapper = ObjectMapper()
    private val headers = mapOf("x-api-key" to apiKey, "Accept" to "application/json")
    private val apiBase = "https://api.curseforge.com/v1"
    private val cap = CurseForgeCandidateSource.MAX_INDEX

    /** One live search, returning (slugs, totalCount). */
    private fun search(query: String): Pair<List<String>, Int> {
        val body = fetcher.get(
            "$apiBase/mods/search?gameId=432&classId=6&sortField=" +
                "${CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS}&$query",
            headers
        )
        val tree = mapper.readTree(body)
        return tree.path("data").map { it.path("slug").asText() } to tree.path("pagination").path("totalCount").asInt(-1)
    }

    /** Download counts of one live search page, in the order the API returned them. */
    private fun downloads(query: String): List<Long> {
        val body = fetcher.get(
            "$apiBase/mods/search?gameId=432&classId=6&sortField=" +
                "${CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS}&$query",
            headers
        )
        return mapper.readTree(body).path("data").map { it.path("downloadCount").asLong(0) }
    }

    /**
     * **The bug this suite was born from.** `totalCount` is the slice's true size only while it fits under the
     * paging cap; at or above it the API reports *exactly* the cap and nothing larger. So "saturated" is the only
     * signal available for deciding a slice needs splitting, and any rule phrased as `> cap` (or `> 2 × cap`)
     * can never fire — which silently reduced the whole partitioned crawl to the top 10 000 of each version.
     */
    @Test
    fun totalCountSaturatesAtThePagingCapInsteadOfReportingTheRealSize() {
        val (_, hugeSlice) = search("sortOrder=desc&pageSize=1&index=0")
        val (_, versionSlice) = search("sortOrder=desc&pageSize=1&index=0&gameVersion=1.12.2")
        val (_, tinySlice) = search("sortOrder=desc&pageSize=1&index=0&gameVersion=1.7.10&modLoaderType=${CurseForgePartitions.LITELOADER}")

        Assertions.assertEquals(cap, hugeSlice, "the whole catalog must report exactly the cap, not its real size")
        Assertions.assertEquals(cap, versionSlice, "a popular version saturates the same way")
        Assertions.assertTrue(tinySlice in 0 until cap, "a small slice reports its true size, was $tinySlice")
        println("[live] totalCount — whole catalog=$hugeSlice, 1.12.2=$versionSlice, 1.7.10+LiteLoader=$tinySlice")
    }

    /**
     * The cap is on `index + pageSize`, not on `index` alone: a page ending exactly at it is served, one mod
     * further is refused. [CurseForgeCandidateSource] clamps its page size for this reason.
     */
    @Test
    fun thePagingCapIsEnforcedOnIndexPlusPageSize() {
        Assertions.assertDoesNotThrow { search("sortOrder=desc&index=${cap - 50}&pageSize=50") }
        Assertions.assertThrows(Exception::class.java) { search("sortOrder=desc&index=${cap - 49}&pageSize=50") }
        Assertions.assertThrows(Exception::class.java) { search("sortOrder=desc&index=$cap&pageSize=1") }
    }

    /**
     * Descending *trends* by downloads — the ranking the whole grinder leans on — but is **not strictly
     * monotonic**: a live 10-mod page had 385 316 073 before 386 940 279, so the sort key is not exactly the
     * `downloadCount` the response reports. Ascending is not ordered at all, yet it does reach the catalog's
     * tail, which is what makes crawling a saturated slice from both ends worth ~10 000 extra mods. Both facts
     * are why `warnIfMisordered` checks the descending *trend* only — flagging adjacent inversions would warn on
     * ordinary pages.
     */
    @Test
    fun descendingTrendsByDownloadsWhileAscendingReachesTheTail() {
        val descending = downloads("sortOrder=desc&pageSize=10&index=0")
        val ascending = downloads("sortOrder=asc&pageSize=10&index=0")

        Assertions.assertTrue(
            descending.first() > descending.last(),
            "descending must at least trend downwards: $descending"
        )
        Assertions.assertTrue(
            ascending.max() < descending.min(),
            "ascending must reach the tail (max ${ascending.max()} should be far below desc's min ${descending.min()})"
        )
        val inversions = descending.zipWithNext().count { (left, right) -> right > left }
        println("[live] desc head=${descending.take(3)} ($inversions adjacent inversions in 10) asc tail=${ascending.take(3)} (asc sorted: ${ascending == ascending.sorted()})")
    }

    /**
     * The version axis must be restricted to Minecraft version *types*: `/games/432/versions` also carries
     * modloader version families and types like `Server Side` or `Shader Loader`, which are not Minecraft
     * versions and would become thousands of pointless partitions.
     */
    @Test
    fun onlyMinecraftVersionTypesBelongToTheVersionAxis() {
        val typesBody = fetcher.get("$apiBase/games/432/version-types", headers)
        val minecraftTypeIds = mapper.readTree(typesBody).path("data")
            .filter { it.path("name").asText("").startsWith(CurseForgeCandidateSource.MINECRAFT_VERSION_TYPE_PREFIX) }
            .map { it.path("id").asInt() }
            .toSet()
        val groups = mapper.readTree(fetcher.get("$apiBase/games/432/versions", headers)).path("data")
        val allVersions = groups.flatMap { group -> group.path("versions").map { it.asText() } }
        val minecraftVersions = groups
            .filter { it.path("type").asInt(-1) in minecraftTypeIds }
            .flatMap { group -> group.path("versions").map { it.asText() } }

        Assertions.assertTrue(minecraftTypeIds.isNotEmpty(), "no version type is named 'Minecraft …' any more")
        Assertions.assertTrue(
            minecraftVersions.size < allVersions.size / 2,
            "filtering by Minecraft types must remove the bulk: ${minecraftVersions.size} of ${allVersions.size}"
        )
        Assertions.assertTrue(minecraftVersions.any { it.startsWith("1.20") }, "1.20.x must survive the filter")
        Assertions.assertTrue(
            CurseForgePartitions.orderVersions(minecraftVersions).first().let { it.startsWith("1.2") || it.startsWith("2") },
            "newest-first ordering should put a recent version first, was ${CurseForgePartitions.orderVersions(minecraftVersions).first()}"
        )
        println("[live] version axis — ${minecraftVersions.size} Minecraft versions out of ${allVersions.size} total version strings")
    }

    /** A Minecraft version string from that axis really is accepted as a `gameVersion` filter. */
    @Test
    fun theVersionAxisStringsAreAcceptedBySearch() {
        val (slugs, total) = search("sortOrder=desc&pageSize=5&index=0&gameVersion=1.20.1")

        Assertions.assertTrue(slugs.isNotEmpty(), "gameVersion=1.20.1 returned nothing — the axis vocabulary is wrong")
        Assertions.assertTrue(total > 0)
    }

    /**
     * The modloader axis works and maps as PrismLauncher documents. Proven by *size*: one version's loader
     * slices differ wildly, which cannot happen if the filter were ignored. The `sodium` check is the mapping
     * itself — a Fabric mod must not appear under Forge.
     */
    @Test
    fun theModLoaderFilterIsHonouredAndMapsAsDocumented() {
        val sizes = CurseForgePartitions.LOADERS.associateWith { loader ->
            search("sortOrder=desc&pageSize=1&index=0&gameVersion=1.16.5&modLoaderType=$loader").second
        }
        Assertions.assertTrue(
            sizes.values.distinct().size > 1,
            "every loader slice of 1.16.5 reported the same size ($sizes) — the filter looks ignored"
        )

        val underFabric = search("sortOrder=desc&pageSize=1&gameVersion=1.20.1&modLoaderType=${CurseForgePartitions.FABRIC}&slug=sodium").first
        val underForge = search("sortOrder=desc&pageSize=1&gameVersion=1.20.1&modLoaderType=${CurseForgePartitions.FORGE}&slug=sodium").first
        Assertions.assertEquals(listOf("sodium"), underFabric, "sodium must be in the Fabric slice (${CurseForgePartitions.FABRIC})")
        Assertions.assertTrue(
            underForge.isEmpty(),
            "sodium appeared under modLoaderType=${CurseForgePartitions.FORGE}; either the enum mapping changed or " +
                "sodium gained a Forge build — check before trusting the loader axis"
        )
        println("[live] 1.16.5 loader slice sizes = $sizes")
    }

    /**
     * The category axis is the mods class's own categories, and **a parent category does not reliably include
     * its children** — so the crawl visits every category, parents and children alike. If this ever stops
     * finding a miss, the category stage could be halved to parents only.
     */
    @Test
    fun aParentCategoryDoesNotReliablyIncludeItsChildren() {
        val categories = mapper.readTree(fetcher.get("$apiBase/categories?gameId=432&classId=6", headers)).path("data")
        val children = categories
            .filter { it.path("parentCategoryId").asInt(0).let { parent -> parent != 0 && parent != 6 } }
            .take(6)
        Assertions.assertTrue(children.isNotEmpty(), "no child categories found — the hierarchy changed")

        val missedUnderParent = children.count { child ->
            val childId = child.path("id").asInt()
            val parentId = child.path("parentCategoryId").asInt()
            val slug = search("sortOrder=desc&pageSize=1&index=0&categoryId=$childId").first.firstOrNull()
                ?: return@count false
            search("sortOrder=desc&pageSize=1&categoryId=$parentId&slug=$slug").first.isEmpty()
        }

        Assertions.assertTrue(
            missedUnderParent > 0,
            "every sampled child-category mod was visible under its parent — crawling only parent categories " +
                "would then be safe and would halve the category stage; verify before relying on it"
        )
        println("[live] category axis — ${categories.size()} categories, ${children.count()} children sampled, $missedUnderParent invisible under their parent")
    }

    /**
     * Records how many mods carry no category at all. The crawl keeps the modloader axis *because* this cannot
     * be guaranteed (CurseForge's own docs disagree on whether a category is mandatory); the assertion is a
     * loose regression guard, and the printed numbers are the actual finding.
     */
    @Test
    fun nearlyEveryModCarriesACategory() {
        var sampled = 0
        var withoutCategory = 0
        for (index in listOf(0, cap - 50)) {
            val body = fetcher.get(
                "$apiBase/mods/search?gameId=432&classId=6&sortField=" +
                    "${CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS}&sortOrder=desc&pageSize=50&index=$index",
                headers
            )
            mapper.readTree(body).path("data").forEach { mod ->
                sampled++
                if (!mod.path("categories").elements().hasNext()) {
                    withoutCategory++
                }
            }
        }

        Assertions.assertTrue(sampled > 0, "sampled nothing")
        Assertions.assertTrue(
            withoutCategory <= sampled / 10,
            "$withoutCategory of $sampled sampled mods carry no category — the category axis would miss them, " +
                "so the modloader axis is carrying the coverage"
        )
        println("[live] categories per mod — $withoutCategory of $sampled sampled mods have none")
    }

    /**
     * End to end through the production source: a slice resuming at the paging cap must **split the version by
     * modloader** rather than call the catalog finished. This is the live proof of the saturation fix — before
     * it, this returned `endOfCatalog` and the crawl wrapped.
     */
    @Test
    fun aVersionPagedOutToTheCapSplitsIntoLoaderSlicesAgainstTheLiveApi() {
        val source = CurseForgeCandidateSource(apiKey)
        val pagedOut = CurseForgePartition("1.12.2", categoryId = null, modLoaderType = null, ascending = false)

        val page = source.page(offset = cap, limit = 5, partition = pagedOut.key)

        Assertions.assertFalse(page.endOfCatalog, "a capped version is not the end of the catalog")
        Assertions.assertEquals(
            CurseForgePartition("1.12.2", categoryId = null, modLoaderType = CurseForgePartitions.FORGE, ascending = false).key,
            page.nextPartition,
            "the crawl must continue in the version's first modloader slice"
        )
        Assertions.assertTrue(page.candidates.isNotEmpty(), "the loader slice should have produced candidates")
        println("[live] 1.12.2 paged out at $cap → next partition ${page.nextPartition}, ${page.candidates.size} candidate(s)")
    }

    /**
     * The crawl itself: consecutive batches walk forward through CurseForge and the position survives a
     * restart — the CurseForge twin of [CatalogCrawlLiveIT]'s Modrinth checks.
     */
    @Test
    fun consecutiveBatchesWalkForwardAndSurviveARestart() {
        val cursorFile = File(tempDir.toFile(), "cursors.json")
        val crawler = CatalogCrawler(listOf(CurseForgeCandidateSource(apiKey)), JsonCursorStore(cursorFile), batchSize = 5)

        val first = crawler.nextBatch()
        val second = crawler.nextBatch()

        Assertions.assertEquals(5, first.candidates.size, "the live catalog must fill a 5-project slice")
        Assertions.assertTrue(
            (first.candidates.map { it.slug }.toSet() intersect second.candidates.map { it.slug }.toSet()).isEmpty(),
            "the second batch must be different projects, not the same head again"
        )
        first.candidates.forEach {
            Assertions.assertTrue(it.projectUrl.contains("curseforge.com"), "unexpected project link ${it.projectUrl}")
        }

        val stored = JsonCursorStore(cursorFile).cursor("CurseForge")
        Assertions.assertEquals(10, stored.offset)
        Assertions.assertEquals(CurseForgePartitions.FIRST.key, stored.partition, "still in the opening partition")

        // A brand-new crawler over the same store — as after a service restart.
        val resumed = CatalogCrawler(listOf(CurseForgeCandidateSource(apiKey)), JsonCursorStore(cursorFile), batchSize = 5).nextBatch()
        Assertions.assertTrue(
            (resumed.candidates.map { it.slug }.toSet() intersect first.candidates.map { it.slug }.toSet()).isEmpty(),
            "a restart re-served projects it had already crawled"
        )
        println("[live] crawl — batch1=${first.candidates.take(2).map { it.slug }} batch2=${second.candidates.take(2).map { it.slug }} resumed=${resumed.candidates.take(2).map { it.slug }}")
    }
}
