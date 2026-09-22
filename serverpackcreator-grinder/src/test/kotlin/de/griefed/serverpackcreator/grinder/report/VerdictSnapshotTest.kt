package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.GrindVerdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Pins the derivation the report caches: that it is reused while the store is unchanged, rebuilt once it
 * is, and — the guard that matters most — that selecting through a snapshot answers exactly what selecting
 * through the plain list answers.
 *
 * That last one is load-bearing because `/`, `/export.csv` and `/verdicts.json` share `select`, so the two
 * paths disagreeing would silently make the table and the export describe different data.
 */
internal class VerdictSnapshotTest {

    private fun rows(count: Int): List<GrindVerdict> = (1..count).map { index ->
        grindVerdict(
            slug = "mod-${(index * 7) % count}",
            loader = listOf("Forge", "NeoForge", "Fabric", "Quilt")[index % 4],
            verdict = Verdict.entries[index % Verdict.entries.size],
            platform = if (index % 2 == 0) "Modrinth" else "CurseForge",
            minecraftLine = listOf("1.20", "1.21", "1.19", "1.12")[index % 4]
        )
    }

    private fun query(raw: String?) = VerdictQuery.parse(QueryParams.parse(raw), VerdictQuery.DEFAULT_PAGE_SIZE)

    /**
     * The snapshot path must still narrow, order and page as documented.
     *
     * Asserted against expectations derived in the test, **not** by comparing the two `select` overloads:
     * the list overload delegates to the snapshot one, so cross-comparing them runs the same code twice and
     * passes however broken it is. Verified — forcing `selectsEverything` to `true` left a cross-comparison
     * green while filtering was completely disabled.
     */
    @Test
    fun theSnapshotPathStillFiltersOrdersAndPages() {
        val verdicts = rows(400)
        val snapshot = VerdictSnapshot(verdicts)

        val fabric = VerdictSelection.select(snapshot, query("f.loader=Fabric"))
        Assertions.assertEquals(
            verdicts.count { it.loader == "Fabric" },
            fabric.matched,
            "a loader filter must narrow to that loader"
        )
        Assertions.assertTrue(fabric.rows.isNotEmpty(), "the fixture must produce Fabric rows to assert on")
        Assertions.assertTrue(fabric.rows.all { it.loader == "Fabric" }, "a filtered page must hold only matches")

        val searched = VerdictSelection.select(snapshot, query("q=mod-11"))
        Assertions.assertTrue(searched.rows.isNotEmpty(), "the fixture must produce a search hit to assert on")
        Assertions.assertTrue(
            searched.rows.all { row -> VerdictField.entries.any { it.text(row).contains("mod-11", true) } },
            "a searched page must hold only rows containing the term"
        )
        Assertions.assertTrue(searched.matched < verdicts.size, "the search must actually narrow")

        val bySlug = VerdictSelection.select(snapshot, query("sort=name")).rows.map { it.slug }
        Assertions.assertEquals(bySlug.sorted(), bySlug, "sort=name must order by slug ascending")

        val byRank = VerdictSelection.select(snapshot, query(null)).rows.map { VerdictField.VERDICT.sortKey(it) }
        Assertions.assertEquals(byRank.sorted(), byRank, "the default order leads with the verdict rank")

        val first = VerdictSelection.select(snapshot, query(null))
        val second = VerdictSelection.select(snapshot, query("page=2"))
        Assertions.assertEquals(250, first.rows.size, "the default page size must bound the page")
        Assertions.assertEquals(150, second.rows.size, "400 rows over 250 leaves 150 on page two")
        Assertions.assertTrue(
            first.rows.map { it.identityKey() }.intersect(second.rows.map { it.identityKey() }.toSet()).isEmpty(),
            "pages must not overlap"
        )
        Assertions.assertEquals(400, first.total, "total counts the whole store")
    }

    /**
     * The choices a snapshot offers are gathered across every verdict, not across the filtered ones — a
     * filter bar that only offered what the current filter already left would be a dead end.
     */
    @Test
    fun choicesAreGatheredAcrossEveryVerdictNotTheFilteredOnes() {
        val snapshot = VerdictSnapshot(rows(40))

        val page = VerdictSelection.select(snapshot, query("f.loader=Fabric"))

        Assertions.assertTrue(
            page.choices[VerdictField.LOADER].orEmpty().containsAll(listOf("Fabric", "Forge", "NeoForge", "Quilt")),
            "filtering to one loader must still offer the others"
        )
    }

    /**
     * The cache hands back the same snapshot while nothing has been recorded.
     */
    @Test
    fun theSnapshotIsReusedWhileTheStoreIsUnchanged() {
        val store = InMemoryVerdictStore()
        rows(5).forEach { store.record(it) }
        val cache = VerdictSnapshotCache(store)

        val first = cache.current()

        Assertions.assertSame(first, cache.current(), "nothing changed, so nothing should have been rebuilt")
    }

    /**
     * And rebuilds once something has. Recording *replaces* by identity here, so the row count is unchanged
     * — which is exactly why the cache keys on a version counter rather than on a size.
     *
     * Pinned with the coalescing window **off** (`Duration.ZERO`), because that is the behaviour this test
     * has always been about: that a changed store invalidates the derivation. Every assertion below is
     * unchanged. What a *default* cache does with a window on is
     * `VerdictSnapshotCoalescingTest.theDefaultWindowCoalescesRatherThanRebuildingPerVerdict`, and the two
     * together are what stop the window being confused with a cache that never notices a change.
     */
    @Test
    fun theSnapshotIsRebuiltWhenAVerdictIsRecorded() {
        val store = InMemoryVerdictStore()
        val original = grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CLEAR)
        store.record(original)
        val cache = VerdictSnapshotCache(store, maxAge = Duration.ZERO)
        val before = cache.current()

        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CONFIRMED))

        val after = cache.current()
        Assertions.assertNotSame(before, after, "a re-ground verdict must not keep serving the old derivation")
        Assertions.assertEquals(1, after.verdicts.size, "the replacement did not change the row count")
        Assertions.assertEquals(
            Verdict.CONFIRMED,
            after.verdicts.single().verdict,
            "the rebuilt snapshot must hold the replacement"
        )
    }

    /**
     * The counter has to move on a replacement, since that is the case a size comparison cannot see.
     */
    @Test
    fun theStoreVersionMovesEvenWhenARecordReplaces() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CLEAR))
        val afterFirst = store.version

        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CONFIRMED))

        Assertions.assertNotEquals(afterFirst, store.version, "a replacement changes what the report must show")
        Assertions.assertEquals(1, store.all().size, "…while leaving the row count identical")
    }
}
