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
 * @author Griefed
 */
data class GrinderVerdict(
    /** The project's short name, as the platform spells it — the Name column. */
    val slug: String,
    /** The project page, so a row can be opened rather than searched for. */
    val projectUrl: String,
    /** Modrinth, CurseForge, or whatever a future daemon crawls. */
    val platform: String,
    /**
     * The modloader this verdict was reached under — the one the grinder picked for this Minecraft line,
     * not the project's only one.
     */
    val loader: String,
    /**
     * The Minecraft version-line this verdict is about (`1.12`, `1.20`, `26.2`), or `null` from a daemon
     * that predates the line axis. **This is the row's identity**: a project is ground once per line, so
     * one project holds several rows and the same loader routinely holds more than one of them.
     */
    val minecraftLine: String? = null,
    /**
     * The verdict's own name (`CONFIRMED`, `CLEAR`, `ERROR`, `INCONCLUSIVE`, `LOCKED`, `UNVERIFIABLE`),
     * kept as a string rather than an enum so a class added by a newer daemon still renders — only
     * `CONFIRMED` is ever compared against by name.
     */
    val verdict: String,
    /** The name-pattern the grinder proposes for the clientside-mod list. */
    val suggestedEntry: String?,
    /**
     * The published file name of the artifact the grinder sampled, verbatim — shown for context, never
     * used for exclusion.
     */
    val fileName: String?,
    /** Why the grinder decided what it did — the column that makes a verdict auditable. */
    val detail: String,
    /** When it was verified, as the ISO-8601 string the feed carries. */
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
     * `FallbackPropertiesRenderer`. [fileName] is deliberately not a fallback: it names **one build** of
     * one loader, so offering it here would exclude that single artifact and nothing else — a narrower
     * mistake than the stem it replaced, which at least matched a loader's whole line.
     */
    val exclusionEntry: String? get() = suggestedEntry?.trim()?.ifEmpty { null }

    /** The verdict names this plugin treats specially, as the daemon spells them. */
    companion object {
        /** The verdict name that earns a row a place in the Confirmed pane. */
        const val CONFIRMED = "CONFIRMED"
    }
}
