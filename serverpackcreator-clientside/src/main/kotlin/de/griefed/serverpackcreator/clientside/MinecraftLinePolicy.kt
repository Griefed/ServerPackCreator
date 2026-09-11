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

/**
 * Which of a project's Minecraft version-lines are worth grinding: the newest few, whatever they turn out
 * to be, plus a fixed set of older lines an operator names.
 *
 * **Why a line is the unit at all.** Sideness is a property of a *build*, and builds differ far more across
 * Minecraft eras than across loaders of one era — a mod rewritten for 1.20 shares little with its 1.12.2
 * ancestor, while its Forge and NeoForge builds of the same era are usually the same source compiled twice.
 * Grinding once per loader therefore spent most of its boots re-asking one era's question and never asked
 * the older eras at all: measured 2026-09-11 on the 200 most-downloaded Modrinth mods, 3.06 boots per
 * project covering a mean of 1.6 distinct lines, and `CurseForge/aether`'s 1.12.2 build — a wholly separate
 * codebase — was never booted under any loader.
 *
 * **Why two halves rather than one list.** A bare count is self-maintaining but blind: the newest four lines
 * of a prolific project (JEI publishes for sixteen) never reach 1.12.2, which is exactly the era a fallback
 * list still gets asked about. A bare list is explicit but goes stale in silence — a new Minecraft release
 * is simply never ground until somebody edits an environment variable, and nothing reports it. The newest-N
 * half keeps the head of the catalogue current without configuration; the anchors make the tail deliberate.
 *
 * Measured cost of the shipped defaults on the same 200 projects: 3.83 boots per project against today's
 * 3.06, i.e. **1.25x**. A bare "every line" would be 7.38, or 2.41x, which would break the sizing rule that
 * `SPC_GRINDER_REVERIFY_TTL_DAYS` must outlast a full sweep.
 *
 * @param newestCount How many of the project's own newest lines are always ground.
 * @param anchors     Lines always ground **when the project publishes for them**, as version-lines.
 * @author Griefed
 */
data class MinecraftLinePolicy(
    val newestCount: Int = DEFAULT_NEWEST_COUNT,
    val anchors: Set<String> = DEFAULT_ANCHORS
) {

    /**
     * [anchors] read as version-lines, so an operator may write either `1.12.2` or `1.12` and mean the same
     * thing — the value they see on a platform page is the full version, and refusing it would fail silently
     * by simply matching nothing.
     */
    private val anchorLines: Set<String> = anchors.map { BootCandidateSelector.minecraftLine(it) }.toSet()

    /**
     * [newestCount] with a floor of one.
     *
     * **Not a tunable, and not merely defensive.** A candidate that yields no verdict at all is
     * indistinguishable from one the engine failed on: nothing is recorded, so the freshness check keeps
     * answering "never seen" and the project is re-selected every sweep forever. A configuration that
     * selects nothing — `newestCount = 0` with anchors no project publishes for — would do that to the
     * whole catalogue, which is the same shape as the knob that parsed fine and was unusable
     * (`SPC_GRINDER_WORKERS=0`). So the project's own newest line is always in.
     */
    private val keptNewest: Int = newestCount.coerceAtLeast(1)

    /**
     * The lines to grind out of [lines], newest first.
     *
     * Ordering is [BootCandidateSelector.minecraftComparator]'s, not a string compare — `1.20` is newer than
     * `1.9`, and `26.2` is newer than both.
     */
    fun select(lines: Collection<String>): List<String> {
        val newestFirst = lines.distinct()
            .sortedWith { left, right -> BootCandidateSelector.minecraftComparator.compare(right, left) }
        val kept = newestFirst.take(keptNewest).toMutableSet()
        kept.addAll(newestFirst.filter { it in anchorLines })
        return newestFirst.filter { it in kept }
    }

    companion object {
        /**
         * Two: the current release line and the one before it, which is where packs actually live while a
         * Minecraft version is being adopted.
         */
        const val DEFAULT_NEWEST_COUNT = 2

        /**
         * The older eras still worth a boot, as version-lines.
         *
         * `1.21` and `1.20` because the great majority of modpacks in circulation are built on them, and
         * `1.12` because it is the one pre-Fabric era with a living pack ecosystem. Deliberately short: each
         * entry costs a boot on every project that publishes for it, and the measurement above is what the
         * number is chosen against rather than a sense of which versions matter.
         */
        val DEFAULT_ANCHORS = setOf("1.21", "1.20", "1.12")
    }
}
