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

import de.griefed.serverpackcreator.clientside.BootDecision
import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.GrindVerdict

/**
 * The two lists an SPC instance refreshes from its update-URL, as the grinder currently knows them.
 *
 * @param clientsideMods The fallback clientside-only mod list, as this SPC instance holds it.
 * @param whitelist      The mod-whitelist, passed through untouched so one URL can replace the other.
 * @author Griefed
 */
data class FallbackLists(
    val clientsideMods: Collection<String>,
    val whitelist: Collection<String>
)

/**
 * Renders the `/as-properties` document: a `serverpackcreator.properties` fragment an SPC instance can
 * poll through `de.griefed.serverpackcreator.configuration.fallback.updateurl`, carrying the fallback
 * clientside-mod list **plus** the grinder's crash-proven findings.
 *
 * The point is to take the fallback list off a maintainer's hand-editing loop: the grinder boots mods
 * continuously, and a mod that crashes a server is exactly the evidence the list exists to encode.
 *
 * Only [Verdict.CONFIRMED] is ever published, because only a rule match is decisive — a mod that boots cleanly
 * has proven nothing, and a false entry silently strips a mod out of everybody's server pack. That
 * asymmetry is why the gate is a floor rather than a threshold to tune.
 *
 * The output is written for `java.util.Properties.load(InputStream)`, which is what consumes it and
 * which decodes **ISO-8859-1** — hence the `\uXXXX` escaping rather than emitting UTF-8. The
 * continuation-line layout mirrors the repository's own file so the two are diff-able.
 *
 * @author Griefed
 */
object FallbackPropertiesRenderer {

    /** Property-key of the fallback clientside-mod list, matching `GenerationConfig.FALLBACK_MODS_LIST_KEY`. */
    const val FALLBACK_MODS_LIST_KEY = "de.griefed.serverpackcreator.configuration.fallbackmodslist"

    /** Property-key of the mod-whitelist, matching `GenerationConfig.MODS_WHITELIST_KEY`. */
    const val MODS_WHITELIST_KEY = "de.griefed.serverpackcreator.configuration.modswhitelist"

    /** Indent of a continuation line, matching the repository's `serverpackcreator.properties`. */
    private const val CONTINUATION_INDENT = "    "

    /**
     * Render the document from the lists an SPC instance currently holds plus everything the grinder has
     * proven. Entries are merged, de-duplicated and sorted case-insensitively, so the same inputs always
     * produce the same bytes — a poll that sees a difference has actually seen a change.
     */
    fun render(
        clientsideMods: Collection<String>,
        whitelist: Collection<String>,
        verdicts: Collection<GrindVerdict>
    ): String {
        val proven = verdicts
            // One condition, where there used to be two. `Confidence.HIGH` was also reachable from the bare
            // exit-code rung -- "exited non-zero, nothing recognised why" -- so a separate decisive-rung check
            // had to be bolted alongside it; measured on the live daemon, 27 of 43 published HIGHs rested on no
            // decisive evidence. `verdictOf` now only ever reaches CONFIRMED from a decisive rung, so CONFIRMED
            // *means* decisive and asking twice would only invite the two to drift apart.
            .filter { it.verdict == Verdict.CONFIRMED }
            .mapNotNull { it.suggestedEntry?.trim()?.ifEmpty { null } }
        val shipped = normalise(clientsideMods)
        val merged = normalise(clientsideMods + proven)
        val whitelisted = normalise(whitelist)
        val added = merged.size - shipped.size
        val dropped = unrepresentable(clientsideMods + proven) + unrepresentable(whitelist)

        return buildString {
            appendLine("# ServerPackCreator fallback lists, served by the grinder.")
            appendLine("#")
            appendLine("# Point an instance at this document with:")
            appendLine("#   de.griefed.serverpackcreator.configuration.fallback.updateurl=<this URL>")
            appendLine("#")
            appendLine("# The clientside-mod list below is the shipped list plus every mod the grinder has")
            appendLine("# *proven* clientside by crashing a real server with it. Mods that merely booted")
            appendLine("# cleanly are never published: a clean boot proves nothing, and a wrong entry strips")
            appendLine("# a mod out of every server pack that uses this list.")
            appendLine("#")
            appendLine("# ${merged.size} clientside entries ($added contributed by the grinder), ${whitelisted.size} whitelisted.")
            if (dropped > 0) {
                appendLine("# $dropped entry/entries omitted: a comma cannot be carried by a comma-separated list.")
            }
            appendLine()
            appendList(FALLBACK_MODS_LIST_KEY, merged)
            appendLine()
            appendList(MODS_WHITELIST_KEY, whitelisted)
        }
    }

    /**
     * Whether [verdict]'s crash is **decisive evidence of client-only-ness**, and may therefore be published.
     *
     * `HIGH` alone was never enough. `CRASHED` is reachable from the client-only-class marker, which no
     * broken harness can fabricate, *and* from the bare exit-code rung, which means only "the process exited
     * non-zero and nothing recognised why" — and afterwards the two were indistinguishable. Sampled against
     * the deployed grinder on 2026-08-31, **four of five** published boot logs were the latter: two mixin
     * failures, a Quilt solver give-up and a Forge jar staged for a NeoForge boot. One of those mods was
     * already in the list this function renders.
     *
     * A verdict recorded before the rung was tracked reads `null` and does **not** publish. That empties the
     * grinder's contribution until a sweep re-grinds, which is the intended trade: an empty contribution is
     * better than a wrong one, and grandfathering the old rows in would keep exactly the entries this gate
     * exists to remove.
     */
    private fun decisive(verdict: GrindVerdict): Boolean =
        BootDecision.entries.firstOrNull { it.name == verdict.decidedBy }?.decisive == true

    /**
     * Merge, drop blanks and unrepresentable entries, de-duplicate, and sort case-insensitively so the
     * rendering is input-order independent.
     *
     * An entry containing a comma is dropped rather than emitted: the consumer splits the value on commas, so
     * such an entry would arrive as *two* bogus `startsWith` matchers against real mod filenames. Filenames may
     * legally contain commas and stems are derived straight from them, so this is reachable — and silent at
     * both ends, which is why [render] states the count in the document.
     */
    private fun normalise(entries: Collection<String>): List<String> =
        entries.map { it.trim() }
            .filter { it.isNotEmpty() && !it.contains(',') }
            .distinct()
            .sortedBy { it.lowercase() }

    /**
     * How many *distinct* entries this format cannot carry, for the document to admit rather than silently
     * swallow. De-duplicated to match [normalise]: one mod appearing on three loaders is one dropped entry,
     * and a count that says three would send a reader looking for two entries that do not exist.
     */
    private fun unrepresentable(entries: Collection<String>): Int =
        entries.map { it.trim() }.filter { it.isNotEmpty() && it.contains(',') }.distinct().size

    /**
     * Append one `key=` line with its comma-separated [entries] spread over continuation lines. An empty
     * list still emits the bare key: a client that finds it absent keeps whatever it had, and silently
     * doing nothing is worse than telling it the list is empty.
     */
    private fun StringBuilder.appendList(key: String, entries: List<String>) {
        if (entries.isEmpty()) {
            appendLine("$key=")
            return
        }
        appendLine("$key=\\")
        entries.forEachIndexed { index, entry ->
            // Every line but the last carries a comma and a continuation backslash; the last carries neither.
            val lineEnding = if (index == entries.lastIndex) "" else ",\\"
            appendLine("$CONTINUATION_INDENT${escape(entry)}$lineEnding")
        }
    }

    /**
     * Escape one entry for `Properties.load`: a literal backslash must not consume the next character, and
     * anything outside ISO-8859-1's ASCII range has to travel as `\uXXXX` because the loader decodes bytes,
     * not UTF-8. Internal spaces need no escaping — only leading whitespace on a continuation line is
     * stripped, and the indent absorbs that.
     */
    private fun escape(entry: String): String = buildString {
        for (character in entry) {
            when {
                character == '\\' -> append("\\\\")
                character.code in 32..126 -> append(character)
                else -> append("\\u%04x".format(character.code))
            }
        }
    }
}
