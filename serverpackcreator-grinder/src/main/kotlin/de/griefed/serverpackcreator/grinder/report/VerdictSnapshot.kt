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

import de.griefed.serverpackcreator.grinder.GrindVerdict
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * The whole store plus the two derivations every request needs from it, computed at most once.
 *
 * Both are properties of the *store*, not of the query: the filter choices are gathered across every
 * verdict regardless of what is being filtered, and the default order is the one the report shows when
 * nobody asked for another. Recomputing them per request is what made the page cost grow with the store
 * while the page itself stayed 250 rows — measured at 251 ms of selection against 3 ms of rendering at
 * the deployed row count.
 *
 * Both are `lazy`, so a filtered or explicitly-sorted request pays only for what it reads, and `lazy`'s
 * synchronization means concurrent request threads compute each at most once between them.
 *
 * @author Griefed
 */
internal class VerdictSnapshot(
    /** Every verdict in the store, as one list all readers of this snapshot share. */
    val verdicts: List<GrindVerdict>,
    /**
     * Names this snapshot to a client paging `/verdicts.json`: two pages carrying the same value were sliced
     * from the same rows. [VerdictSnapshotCache] gives every snapshot it builds a value no other one had; a
     * snapshot built outside it serves no paged feed and keeps the default.
     */
    val version: Long = 0L
) {

    /** The options each CHOICE column offers, gathered across every verdict. */
    val choices: Map<VerdictField, List<String>> by lazy {
        VerdictField.entries.filter { it.filter == FilterKind.CHOICE }.associateWith { field ->
            verdicts.map { field.text(it) }.filter { it.isNotBlank() }.distinct().sorted()
        }
    }

    /** Every verdict in the order the report shows when no sort was asked for. */
    val defaultOrder: List<GrindVerdict> by lazy { VerdictSelection.inDefaultOrder(verdicts) }
}

/**
 * Hands out the current [VerdictSnapshot], rebuilding it only once the store says it has changed, and keeps
 * the recently served ones answerable by version so a paged read can finish on the snapshot it started from.
 *
 * @author Griefed
 */
internal class VerdictSnapshotCache(
    private val store: VerdictStore,
    /**
     * How long a snapshot may go on being served after the store has moved on — the bound on how often a
     * busy grind can force the whole-store derivation to be redone.
     *
     * [Duration.ZERO] disables coalescing entirely: every change rebuilds, and the report is strictly live.
     */
    maxAge: Duration = DEFAULT_MAX_AGE,
    /** Nanosecond source for the window, injected so tests need no sleeping. */
    private val clock: () -> Long = System::nanoTime,
    /** The most snapshots [pinned] can answer from; the least recently used goes first. Floored at one. */
    retainedCount: Int = DEFAULT_RETAINED_COUNT,
    /** How long a retained snapshot survives without being served. */
    retainedIdle: Duration = DEFAULT_RETAINED_IDLE
) {

    /** The window in nanos, floored at zero so a negative configuration cannot mean "cache for ever". */
    private val maxAgeNanos: Long = maxAge.toNanos().coerceAtLeast(0L)

    private val held = AtomicReference<Held?>(null)

    /**
     * The [VerdictSnapshot.version] the next build takes. A counter of its own rather than
     * [VerdictStore.version]: the store's counter is read before the rows, so a record landing in between can
     * give two different row sets the same store version. Seeded from the wall clock so a restarted daemon
     * never reissues a value a client paging across the restart still holds — builds happen far less often
     * than once a millisecond.
     */
    private val nextVersion = AtomicLong(System.currentTimeMillis())

    private val retainedIdleNanos: Long = retainedIdle.toNanos().coerceAtLeast(0L)

    /** A retained snapshot and when it was last served, by [VerdictSnapshot.version]. */
    private class Retained(val snapshot: VerdictSnapshot, var lastServed: Long)

    /** Access-ordered, so eviction past the count bound takes the snapshot served longest ago. Guarded by itself. */
    private val retained = object : LinkedHashMap<Long, Retained>(16, 0.75f, true) {
        private val bound = retainedCount.coerceAtLeast(1)
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Retained>): Boolean = size > bound
    }

    /** One cached derivation, with the store version and the instant it was built from. */
    private class Held(val version: Long, val builtAt: Long, val snapshot: VerdictSnapshot)

    /**
     * The snapshot to serve, reusing the held one while [VerdictStore.version] is unchanged **or** while the
     * held one is still inside [maxAgeNanos].
     *
     * The version is read *before* the rows, so a record landing between the two is cached under the older
     * version and rebuilt on the next call — wasteful once, never stale beyond the window. Two threads
     * racing to build simply both build; the result is the same either way, which is cheaper than holding a
     * lock across the derivation.
     *
     * An unchanged store is reused **regardless of age**: the window bounds staleness, it does not schedule
     * pointless work on an idle daemon. Whatever is served is retained for [pinned].
     */
    fun current(): VerdictSnapshot = retain(currentUnretained())

    /**
     * The retained snapshot [version] names, or `null` once it has been evicted — by count, or by going unserved
     * for longer than the idle time. Serving it refreshes that idle time, so a read in progress keeps the
     * snapshot it is paging.
     */
    fun pinned(version: Long): VerdictSnapshot? = synchronized(retained) {
        val now = clock()
        val entry = retained[version] ?: return null
        if (now - entry.lastServed > retainedIdleNanos) {
            retained.remove(version)
            return null
        }
        entry.lastServed = now
        entry.snapshot
    }

    /**
     * Record [snapshot] as served now, adding it if it is not retained. Every snapshot [current] hands out goes
     * through here, the held one included: on an idle daemon that one may have outlived its idle time, and page 2
     * must be able to get the version page 1 just reported.
     */
    private fun retain(snapshot: VerdictSnapshot): VerdictSnapshot = synchronized(retained) {
        val now = clock()
        retained.values.removeIf { now - it.lastServed > retainedIdleNanos && it.snapshot !== snapshot }
        retained.getOrPut(snapshot.version) { Retained(snapshot, now) }.lastServed = now
        snapshot
    }

    private fun currentUnretained(): VerdictSnapshot {
        val version = store.version
        held.get()?.let { holder ->
            if (holder.version == version) {
                return holder.snapshot
            }
            if (maxAgeNanos > 0L && clock() - holder.builtAt < maxAgeNanos) {
                return holder.snapshot
            }
        }
        val rebuilt = VerdictSnapshot(store.all(), nextVersion.getAndIncrement())
        held.set(Held(version, clock(), rebuilt))
        return rebuilt
    }

    companion object {
        /**
         * How long a derivation is reused after the store has moved on, unless an operator says otherwise
         * via `SPC_GRINDER_REPORT_CACHE_SECONDS`.
         *
         * Five seconds because the thing being bounded fires thousands of times an hour: `record()` moves
         * the version on **every** verdict, so without a window several grind workers guarantee a new
         * version between almost any two requests and the cache degrades to nothing precisely when the
         * daemon is busiest. The cost it bounds grows with the store — selection was measured at 251 ms for
         * 38,258 verdicts against 3 ms to render the 250 rows actually sent.
         *
         * The trade is that the report may lag by up to one window. That is the same bargain
         * `SPC_GRINDER_STORE_FLUSH_SECONDS` already makes for writes, and five seconds is far below the time
         * a reader takes to notice a row is missing.
         */
        val DEFAULT_MAX_AGE: Duration = Duration.ofSeconds(5)

        /**
         * How many snapshots a pinned read can be answered from.
         *
         * A snapshot shares its row objects with the store, so what retaining one costs is its own row list plus,
         * once something sorted by default, its `defaultOrder`: a reference per row each, about 2.6 MB at 320,000
         * rows with compressed references — about 21 MB for all eight — plus whichever rows the store has since
         * replaced, which only a retained snapshot still references (an estimate from the JVM's object layout,
         * not a measurement). Pinned pages never build; a build happens only when an unpinned request — another
         * reader's first page, the table, `/as-properties` — finds the store moved, and at most once per coalescing
         * window. A read's snapshot is evicted by count only once eight other snapshots have been served since its
         * last page.
         */
        const val DEFAULT_RETAINED_COUNT = 8

        /**
         * How long a retained snapshot survives unserved. A paging client asks for its next page as soon as the
         * last one arrives and the plugin gives each request at most 15 seconds, so a read in progress touches
         * its snapshot far more often than this.
         */
        val DEFAULT_RETAINED_IDLE: Duration = Duration.ofSeconds(60)
    }
}
