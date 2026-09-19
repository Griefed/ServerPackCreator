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
internal class VerdictSnapshotCache(private val store: VerdictStore) {

    private val held = AtomicReference<Pair<Long, VerdictSnapshot>?>(null)

    /**
     * The snapshot for the store as it stands, reusing the held one while [VerdictStore.version] is unchanged.
     *
     * The version is read *before* the rows, so a record landing between the two is cached under the older
     * version and rebuilt on the next call — wasteful once, never stale. Two threads racing to build simply
     * both build; the result is the same either way, which is cheaper than holding a lock across the
     * derivation.
     */
    fun current(): VerdictSnapshot {
        val version = store.version
        held.get()?.let { (heldVersion, snapshot) ->
            if (heldVersion == version) {
                return snapshot
            }
        }
        val rebuilt = VerdictSnapshot(store.all())
        held.set(version to rebuilt)
        return rebuilt
    }
}
