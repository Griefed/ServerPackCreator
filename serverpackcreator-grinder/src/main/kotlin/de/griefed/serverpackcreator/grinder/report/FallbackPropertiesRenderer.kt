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

import de.griefed.serverpackcreator.clientside.Confidence
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
 * Only [Confidence.HIGH] is ever published, because only a crash is decisive — a mod that boots cleanly
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
            .filter { it.confidence == Confidence.HIGH }
            .mapNotNull { it.suggestedEntry?.trim()?.ifEmpty { null } }
        val merged = normalise(clientsideMods + proven)
        val added = merged.size - normalise(clientsideMods).size

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
            appendLine("# ${merged.size} clientside entries (${added} contributed by the grinder), ${normalise(whitelist).size} whitelisted.")
            appendLine()
            appendList(FALLBACK_MODS_LIST_KEY, merged)
            appendLine()
            appendList(MODS_WHITELIST_KEY, normalise(whitelist))
        }
    }

    /** Merge, drop blanks, de-duplicate and sort case-insensitively so the rendering is input-order independent. */
    private fun normalise(entries: Collection<String>): List<String> =
        entries.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() }

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
            val separator = if (index == entries.lastIndex) "" else ","
            appendLine("$CONTINUATION_INDENT${escape(entry)}$separator\\".removeSuffix(if (index == entries.lastIndex) "\\" else ""))
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
