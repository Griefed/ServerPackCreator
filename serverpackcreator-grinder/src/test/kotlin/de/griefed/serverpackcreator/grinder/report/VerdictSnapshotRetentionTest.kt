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

import de.griefed.serverpackcreator.clientside.verdict.Verdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Pins how long [VerdictSnapshotCache] keeps a snapshot a paged `/verdicts.json` read is pinned to.
 *
 * A paging client sends page 1's `version` with every later page, and the daemon answers from the snapshot that
 * version names, so a read is exact however many grinds record while it pages. Retention is bounded twice: by
 * count, because each retained snapshot holds a reference per row, and by idle time, because a snapshot nobody
 * pages any more serves no one. Every page touches the one it reads, so a read in progress keeps its own alive.
 */
internal class VerdictSnapshotRetentionTest {

    /** A hand-wound nanosecond clock, so retention can be crossed without a test sleeping through it. */
    private class TestClock(private var nanos: Long = 0L) : () -> Long {
        override fun invoke(): Long = nanos

        fun advance(duration: Duration) {
            nanos += duration.toNanos()
        }
    }

    private fun storeWith(vararg slugs: String) = InMemoryVerdictStore().apply {
        slugs.forEach { record(grindVerdict(slug = it, loader = "Forge", verdict = Verdict.CLEAR)) }
    }

    private fun cache(
        store: VerdictStore,
        clock: TestClock,
        count: Int = 4,
        idle: Duration = Duration.ofSeconds(60)
    ) = VerdictSnapshotCache(store, maxAge = Duration.ZERO, clock = clock, retainedCount = count, retainedIdle = idle)

    /** The snapshot a read started from is still served by its version after the store has moved on. */
    @Test
    fun aRetainedSnapshotIsServedByItsVersionAfterTheStoreMovedOn() {
        val store = storeWith("jei")
        val clock = TestClock()
        val cache = cache(store, clock)
        val started = cache.current()

        store.record(grindVerdict(slug = "create", loader = "Forge", verdict = Verdict.CLEAR))
        val rebuilt = cache.current()
        Assertions.assertNotEquals(started.version, rebuilt.version, "the fixture did not rebuild")

        Assertions.assertSame(
            started, cache.pinned(started.version),
            "a read pinned to the snapshot it started from must keep getting that snapshot"
        )
    }

    /** Past the count bound the least recently used snapshot goes, so retention cannot grow with the grind. */
    @Test
    fun theLeastRecentlyUsedSnapshotIsEvictedPastTheCount() {
        val store = storeWith("jei")
        val clock = TestClock()
        val cache = cache(store, clock, count = 2)
        val oldest = cache.current()
        store.record(grindVerdict(slug = "a", loader = "Forge", verdict = Verdict.CLEAR))
        val middle = cache.current()
        store.record(grindVerdict(slug = "b", loader = "Forge", verdict = Verdict.CLEAR))
        val newest = cache.current()

        Assertions.assertNull(cache.pinned(oldest.version), "three snapshots retained against a bound of two")
        Assertions.assertSame(middle, cache.pinned(middle.version))
        Assertions.assertSame(newest, cache.pinned(newest.version))
    }

    /** Touching a snapshot is what keeps it: the one a read is paging survives builds that push older ones out. */
    @Test
    fun aSnapshotBeingPagedSurvivesNewerBuilds() {
        val store = storeWith("jei")
        val clock = TestClock()
        val cache = cache(store, clock, count = 2)
        val paged = cache.current()
        store.record(grindVerdict(slug = "a", loader = "Forge", verdict = Verdict.CLEAR))
        cache.current()

        Assertions.assertSame(paged, cache.pinned(paged.version), "the page in between touched it")
        store.record(grindVerdict(slug = "b", loader = "Forge", verdict = Verdict.CLEAR))
        cache.current()

        Assertions.assertSame(paged, cache.pinned(paged.version), "the snapshot being paged was evicted first")
    }

    /** An idle snapshot goes once its idle time passes; one still being paged does not. */
    @Test
    fun anIdleSnapshotExpiresWhileAPagedOneDoesNot() {
        val store = storeWith("jei")
        val clock = TestClock()
        val cache = cache(store, clock, idle = Duration.ofSeconds(60))
        val snapshot = cache.current()

        clock.advance(Duration.ofSeconds(50))
        Assertions.assertSame(snapshot, cache.pinned(snapshot.version), "inside its idle time")
        clock.advance(Duration.ofSeconds(50))
        Assertions.assertSame(snapshot, cache.pinned(snapshot.version), "the previous page refreshed its idle time")

        clock.advance(Duration.ofSeconds(61))
        Assertions.assertNull(cache.pinned(snapshot.version), "a snapshot nobody pages any more must not be held")
    }

    /**
     * Page 1 reads the current snapshot, and on an idle daemon that is a held one built long ago. Serving it must
     * retain it again, or page 2 finds the version page 1 just reported already gone.
     */
    @Test
    fun servingTheHeldSnapshotRetainsItAgain() {
        val store = storeWith("jei")
        val clock = TestClock()
        val cache = cache(store, clock, idle = Duration.ofSeconds(60))
        val built = cache.current()

        clock.advance(Duration.ofHours(1))
        val served = cache.current()
        Assertions.assertSame(built, served, "an unchanged store reuses the held snapshot")

        Assertions.assertSame(served, cache.pinned(served.version), "page 1 reported a version page 2 cannot get")
    }

    /** A version nothing ever carried is simply absent, never an exception. */
    @Test
    fun anUnknownVersionIsAbsent() {
        val cache = cache(storeWith("jei"), TestClock())
        cache.current()

        Assertions.assertNull(cache.pinned(-1L))
    }
}
