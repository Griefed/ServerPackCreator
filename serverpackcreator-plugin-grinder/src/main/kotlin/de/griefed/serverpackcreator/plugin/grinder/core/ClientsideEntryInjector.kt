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
 * Merges the entries a user ticked into the clientside-mod exclusion list of a server pack.
 *
 * Deliberately a pure function over two lists: it is the one place in this plugin where a mistake either
 * silently removes mods from somebody's server pack or silently fails to, and everything around it —
 * the HTTP fetch, the table, the pf4j extension point — is scenery by comparison.
 *
 * @author Griefed
 */
object ClientsideEntryInjector {

    /**
     * [existing] with every entry of [selected] it does not already contain, appended in order.
     *
     * Comparison is case-insensitive on trimmed values because ServerPackCreator's own matching is, so
     * an entry differing only in case is a duplicate rather than a second rule. The existing order is
     * kept: the exclusion list is applied in sequence and re-sorting a user's own list is not a change
     * this plugin has any business making. Blanks are dropped from both sides — an empty entry matches
     * every mod name under the `startsWith` and `contains` filters, and would empty a mods directory.
     */
    fun inject(existing: Collection<String>, selected: Collection<String>): List<String> {
        val merged = LinkedHashMap<String, String>()
        for (entry in existing.asSequence() + selected.asSequence()) {
            val trimmed = entry.trim()
            if (trimmed.isNotEmpty()) {
                merged.putIfAbsent(trimmed.lowercase(), trimmed)
            }
        }
        return merged.values.toList()
    }
}
