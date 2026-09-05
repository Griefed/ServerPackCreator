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
 * Derives the version-less file-name stem(s) that the clientside-only mod-list matches against. The
 * list excludes a mod by `fileName.startsWith(stem)`, so the goal is the longest leading part of a
 * project's file-names that is stable across versions (e.g. `jei-1.20.1-15.2.jar` -> `jei-`).
 *
 * The result is a *suggestion* a maintainer confirms — file-naming is not standardized, so ambiguous
 * cases are expected and the surrounding report always lists the raw file-names for cross-checking.
 *
 * @author Griefed
 */
object FilenameStemDeriver {
    /** Delimiters that separate a mod-name from its version/loader/Minecraft tokens. */
    private val delimiters = charArrayOf('-', '_', '+', ' ')

    /**
     * Trailing version-ish remainder of a single file-name: an optional `mc`/`v` marker, then a digit
     * and everything after it. The preceding delimiter is intentionally *kept* (e.g. `rubidium-0.7`
     * -> `rubidium-`) to mirror the existing list-entries, which retain the trailing separator to
     * avoid over-matching neighbouring mods.
     */
    private val trailingVersion = Regex("(mc|v)?\\d.*$", RegexOption.IGNORE_CASE)

    /** The `.jar` (or `.jar.disabled`) ending, removed before deriving a stem. */
    private val jarEnding = Regex("\\.jar(\\.disabled)?$", RegexOption.IGNORE_CASE)

    /**
     * Derive one stem from all [fileNames] belonging to a single loader-group.
     *
     * With several file-names the longest common prefix naturally diverges at the version; that
     * prefix is then trimmed back to its last delimiter so no partial version-token (e.g. a stray
     * `mc1`) leaks into the stem. With a single file-name the trailing version is stripped instead.
     *
     * Called once per loader-group, so a project shipping for several loaders commonly yields several
     * *different* stems — `sodium-fabric-` for Fabric against `embeddium-` for Forge/NeoForge — and each is
     * an independent list-entry. That divergence is why `ClientsideVerifier.loaderDisprovingTheCrash`
     * compares **entries** rather than loaders: where the stems differ, one loader's published entry strips
     * nothing the other proved bootable, so there is no contradiction to reconcile.
     * Returns `null` when [fileNames] is empty.
     */
    fun deriveStem(fileNames: Collection<String>): String? {
        val names = fileNames.map { it.replace(jarEnding, "") }.filter { it.isNotBlank() }
        if (names.isEmpty()) {
            return null
        }
        if (names.size == 1) {
            return stripTrailingVersion(names.first())
        }
        val commonPrefix = longestCommonPrefix(names)
        val trimmed = trimToDelimiter(commonPrefix)
        // If the names share no delimited prefix (e.g. "totaldarkness1.2"), fall back to stripping
        // the version off the shortest name, which is the safest broad matcher.
        return trimmed.ifBlank { stripTrailingVersion(names.minByOrNull { it.length } ?: names.first()) }
    }

    /** Strip the trailing version-remainder from a single name, keeping at least the leading token. */
    private fun stripTrailingVersion(name: String): String {
        val stripped = name.replace(trailingVersion, "")
        return stripped.ifBlank { name }
    }

    /** Longest character-prefix shared by every name in [names]. */
    private fun longestCommonPrefix(names: List<String>): String {
        val shortest = names.minByOrNull { it.length } ?: return ""
        for (index in shortest.indices) {
            val char = shortest[index]
            if (names.any { it[index] != char }) {
                return shortest.substring(0, index)
            }
        }
        return shortest
    }

    /**
     * Cut [prefix] back to and including its last delimiter, dropping the partial token that follows
     * it. `"sodium-fabric-mc1."` -> `"sodium-fabric-"`. Returns "" when there is no delimiter.
     */
    private fun trimToDelimiter(prefix: String): String {
        val lastDelimiter = prefix.lastIndexOfAny(delimiters)
        return if (lastDelimiter < 0) "" else prefix.substring(0, lastDelimiter + 1)
    }
}
