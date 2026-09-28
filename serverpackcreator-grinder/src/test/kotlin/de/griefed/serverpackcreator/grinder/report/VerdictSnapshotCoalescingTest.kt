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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Pins the snapshot cache's **coalescing window** — the bound on how often a busy grind can force the
 * whole-store derivation to be redone.
 *
 * Why a window at all. `VerdictSnapshotCache` rebuilt whenever `VerdictStore.version` moved, and `record()`
 * moves it on **every verdict**. With several workers recording continuously there is a new version between
 * almost any two requests, so during an active grind the cache was very nearly a no-op: each request paid a
 * full copy, sort and filter-column gather. That is the cost the cache exists to remove, and it came back
 * at exactly the moment the daemon is busiest.
 *
 * The trade is explicit and bounded: the report can lag by at most one window. It is the same bargain
 * `SPC_GRINDER_STORE_FLUSH_SECONDS` already makes for writes, and for the same reason — whole-store work
 * belongs on a clock, not on an event that fires thousands of times an hour.
 *
 * The clock is injected so none of this needs sleeping.
 *
 * @author Griefed
 */
internal class VerdictSnapshotCoalescingTest {

    /** A hand-wound nanosecond clock, so a window can be crossed without a test sleeping through it. */
    private class TestClock(private var nanos: Long = 0L) : () -> Long {
        override fun invoke(): Long = nanos

        /** Move time forward by [duration], as the scheduler would between two requests. */
        fun advance(duration: Duration) {
            nanos += duration.toNanos()
        }
    }

    /**
     * The defect: a verdict landing mid-window must NOT force a rebuild. This is the case that made the
     * cache useless during a grind, because it is the case that happens constantly.
     */
    @Test
    fun aVerdictRecordedInsideTheWindowDoesNotForceARebuild() {
        val clock = TestClock()
        val store = InMemoryVerdictStore()
        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CLEAR))
        val cache = VerdictSnapshotCache(store, maxAge = Duration.ofSeconds(5), clock = clock)
        val before = cache.current()

        store.record(grindVerdict(slug = "create", loader = "Forge", verdict = Verdict.CLEAR))
        clock.advance(Duration.ofSeconds(1))

        Assertions.assertSame(
            before,
            cache.current(),
            "a verdict inside the window rebuilt the whole derivation - the cache is a no-op during a grind"
        )
    }

    /**
     * And the other half of the bargain: once the window has passed, the next request must see the new data.
     * A cache that coalesced forever would be a stale report, which is a different bug.
     */
    @Test
    fun theWindowExpiringLetsTheNextRequestSeeTheNewData() {
        val clock = TestClock()
        val store = InMemoryVerdictStore()
        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CLEAR))
        val cache = VerdictSnapshotCache(store, maxAge = Duration.ofSeconds(5), clock = clock)
        val before = cache.current()

        store.record(grindVerdict(slug = "create", loader = "Forge", verdict = Verdict.CLEAR))
        clock.advance(Duration.ofSeconds(6))

        val after = cache.current()
        Assertions.assertNotSame(before, after, "the window expired, so the derivation must have been redone")
        Assertions.assertEquals(2, after.verdicts.size, "the rebuilt snapshot must hold the newly recorded verdict")
    }

    /**
     * An unchanged store is reused regardless of the clock — the window bounds staleness, it does not
     * schedule pointless work on an idle daemon.
     */
    @Test
    fun anUnchangedStoreIsNeverRebuiltHoweverLongPasses() {
        val clock = TestClock()
        val store = InMemoryVerdictStore()
        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CLEAR))
        val cache = VerdictSnapshotCache(store, maxAge = Duration.ofSeconds(5), clock = clock)
        val before = cache.current()

        clock.advance(Duration.ofHours(1))

        Assertions.assertSame(before, cache.current(), "nothing was recorded, so nothing should have been rebuilt")
    }

    /**
     * The window must be **on by default**, which is the only part of this that changes what a deployed
     * daemon does — everything above is reachable today by passing a `maxAge`, and nothing passes one.
     *
     * Deliberately uses the real clock and no injection: two calls microseconds apart are inside any sane
     * default, so this asserts the default is *some* usable window rather than restating its value, which
     * would make the guard fail on a retune that is not a regression.
     */
    @Test
    fun theDefaultWindowCoalescesRatherThanRebuildingPerVerdict() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CLEAR))
        val cache = VerdictSnapshotCache(store)
        val before = cache.current()

        store.record(grindVerdict(slug = "create", loader = "Forge", verdict = Verdict.CLEAR))

        Assertions.assertSame(
            before,
            cache.current(),
            "a default-constructed cache rebuilt on the very next verdict - during a grind that is every " +
                "request paying the full copy, sort and filter-column gather the cache exists to avoid"
        )
    }

    /**
     * Zero restores the pre-window behaviour exactly, which is what `SPC_GRINDER_REPORT_CACHE_SECONDS=0`
     * gives an operator who wants a strictly live report and has a store small enough to afford one.
     */
    @Test
    fun aZeroWindowRebuildsOnEveryChangeAsBefore() {
        val clock = TestClock()
        val store = InMemoryVerdictStore()
        store.record(grindVerdict(slug = "jei", loader = "Forge", verdict = Verdict.CLEAR))
        val cache = VerdictSnapshotCache(store, maxAge = Duration.ZERO, clock = clock)
        val before = cache.current()

        store.record(grindVerdict(slug = "create", loader = "Forge", verdict = Verdict.CLEAR))

        Assertions.assertNotSame(
            before,
            cache.current(),
            "a zero window must mean no coalescing at all - the report is strictly live"
        )
    }
}
