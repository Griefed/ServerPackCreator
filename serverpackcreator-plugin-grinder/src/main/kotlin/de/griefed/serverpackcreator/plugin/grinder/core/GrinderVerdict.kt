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
package de.griefed.serverpackcreator.plugin.grinder.core

/**
 * One row of the grinder's verdict feed, reduced to what this plugin displays and acts on.
 *
 * It is deliberately the plugin's **own** type rather than the daemon's `GrindVerdict`: that class lives
 * in `serverpackcreator-grinder`, which is neither published nor a dependency of a plugin, and its
 * neighbours in the same document (`declared`, `jarScan`, `stagedDependencies`) are evidence the tab has
 * no use for. Fields absent from a response stay absent here, so "the grinder said nothing" and "the
 * grinder said the empty string" remain distinguishable.
 *
 * @param slug The project's short name, as the platform spells it — the Name column.
 * @param projectUrl The project page, so a row can be opened rather than searched for.
 * @param platform Modrinth, CurseForge, or whatever a future daemon crawls.
 * @param loader The modloader this verdict was reached under; one project can hold several.
 * @param verdict The verdict's own name (`CONFIRMED`, `CLEAR`, `ERROR`, `INCONCLUSIVE`), kept as a
 *                string rather than an enum so a class added by a newer daemon still renders.
 * @param suggestedEntry The name-pattern the grinder proposes for the clientside-mod list.
 * @param filenamePattern The stricter filename regex, shown for context but not used for exclusion.
 * @param detail Why the grinder decided what it did — the column that makes a verdict auditable.
 * @param scannedAt When it was verified, as the ISO-8601 string the feed carries.
 *
 * @author Griefed
 */
data class GrinderVerdict(
    val slug: String,
    val projectUrl: String,
    val platform: String,
    val loader: String,
    val verdict: String,
    val suggestedEntry: String?,
    val filenamePattern: String?,
    val detail: String,
    val scannedAt: String,
    /**
     * What the mod says about itself, folded from the platform's declaration and the jar's own descriptor:
     * `CLIENT`, `SERVER`, `CONTRADICTORY`, or `null` when neither source said anything.
     *
     * Shown beside the verdict rather than instead of it — a `CONTRADICTORY` row is one where the two
     * sources disagree, which is the case a maintainer most often wants to look at by hand.
     */
    val declared: String? = null,
    /**
     * How the jar itself scanned — `CLIENT`, `SERVER_OR_BOTH`, `DEFERRED` (distribution-locked, never read)
     * or `ERROR`. Named for the feed's own `jarScan` field so the mapping is one hop; the column is
     * labelled "JAR sideness", which is what it means to a reader.
     */
    val jarScan: String? = null
) {
    /**
     * Whether this is the one verdict class that rests on decisive evidence — the server crashed with
     * the mod in place. Everything else belongs in the at-your-own-risk pane.
     */
    val isConfirmed: Boolean get() = verdict.equals(CONFIRMED, ignoreCase = true)

    /**
     * The string that would be added to the clientside-mod exclusion list, or `null` when this row
     * offers none and therefore cannot be ticked.
     *
     * Only [suggestedEntry] qualifies, matching what the daemon itself publishes through
     * `FallbackPropertiesRenderer`. [filenamePattern] is deliberately not a fallback: it is a regex over
     * a *filename*, which the exclusion list only applies under its regex filter, so offering it here
     * would silently do nothing under SPC's default matching mode.
     */
    val exclusionEntry: String? get() = suggestedEntry?.trim()?.ifEmpty { null }

    companion object {
        /** The verdict name that earns a row a place in the Confirmed pane. */
        const val CONFIRMED = "CONFIRMED"
    }
}
