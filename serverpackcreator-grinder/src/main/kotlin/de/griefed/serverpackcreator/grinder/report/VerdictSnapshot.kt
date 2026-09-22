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
    val verdicts: List<GrindVerdict>
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
 * Hands out the current [VerdictSnapshot], rebuilding it only once the store says it has changed.
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
    private val clock: () -> Long = System::nanoTime
) {

    /** The window in nanos, floored at zero so a negative configuration cannot mean "cache for ever". */
    private val maxAgeNanos: Long = maxAge.toNanos().coerceAtLeast(0L)

    private val held = AtomicReference<Held?>(null)

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
     * pointless work on an idle daemon.
     */
    fun current(): VerdictSnapshot {
        val version = store.version
        held.get()?.let { holder ->
            if (holder.version == version) {
                return holder.snapshot
            }
            if (maxAgeNanos > 0L && clock() - holder.builtAt < maxAgeNanos) {
                return holder.snapshot
            }
        }
        val rebuilt = VerdictSnapshot(store.all())
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
    }
}
