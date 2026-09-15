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

import com.electronwill.nightconfig.core.CommentedConfig

/**
 * The two lists a user ticks in, kept apart because they carry different risk.
 *
 * @param key The key this pane's selection is stored under in the plugin configuration. It is part of
 *            the on-disk format, so renaming one orphans every user's saved selection.
 *
 * @author Griefed
 */
enum class SelectionPane(val key: String) {

    /** Verdicts the grinder proved by crashing a server with the mod in place. */
    CONFIRMED("selectedConfirmed"),

    /** Everything else — unproven, and possibly a server mod. Ticked at the user's own risk. */
    OTHER("selectedOther")
}

/**
 * Reads and writes the plugin's settings inside the `CommentedConfig` ServerPackCreator owns.
 *
 * That object is the whole mechanism the feature rests on: `ApiPlugins` hands **the same instance** to
 * the GUI tab and to the pre-generation extension, so a tick made in the tab is visible to a generation
 * started moments later without a save, while `ExtensionTab.saveConfiguration()` is what carries it
 * across a restart. This class holds no state of its own — it is a typed view over that config, cheap
 * to construct wherever one is in hand.
 *
 * Every read tolerates a damaged value. The config is a file a user may hand-edit, and this object is
 * constructed on the generation path, where throwing over a typo would abort somebody's server pack.
 *
 * @param config The plugin configuration provided by ServerPackCreator.
 *
 * @author Griefed
 */
class SelectionStore(private val config: CommentedConfig) {

    /** The grinder address exactly as the user typed it, so the Settings field can show it back unchanged. */
    var grinderUrl: String
        get() = config.textOrDefault(URL_KEY, "")
        set(value) = config.set(URL_KEY, value)

    /**
     * The address to actually request, run through the one URL rule, or `null` when none is usable.
     * Callers branch on this rather than on [grinderUrl] being non-empty.
     */
    val resolvedUrl: String? get() = GrinderUrl.normalise(grinderUrl)

    /**
     * How often the Dashboard polls, in seconds, clamped to something a daemon can survive: zero would
     * busy-loop it, and a non-positive delay is rejected by `javax.swing.Timer` outright.
     */
    var refreshIntervalSeconds: Int
        get() = (config.get(INTERVAL_KEY) as? Number)?.toInt()?.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
            ?: DEFAULT_INTERVAL
        set(value) = config.set(INTERVAL_KEY, value.coerceIn(MIN_INTERVAL, MAX_INTERVAL))

    /** The entries ticked in [pane]. */
    fun selected(pane: SelectionPane): Set<String> = config.stringList(pane.key)

    /**
     * Replace [pane]'s selection with [entries], dropping blanks.
     *
     * Blanks are refused here rather than at the point of use because an empty exclusion entry matches
     * every mod name under SPC's `startsWith`/`contains` filters — one stored by accident would empty a
     * server pack's mods directory.
     */
    fun setSelected(pane: SelectionPane, entries: Collection<String>) {
        config.set<Any>(pane.key, entries.map { it.trim() }.filter { it.isNotEmpty() }.distinct())
    }

    /**
     * Everything ticked anywhere — what a server pack generation actually excludes. The split into two
     * panes is a distinction the user interface draws about risk, not one generation observes.
     */
    fun allSelected(): Set<String> = SelectionPane.entries.flatMapTo(LinkedHashSet()) { selected(it) }

    /** The value at [key] when it really is a string, else [fallback]. */
    private fun CommentedConfig.textOrDefault(key: String, fallback: String): String =
        get<Any?>(key) as? String ?: fallback

    /**
     * The value at [key] read as a list of non-blank strings. A key holding something else — a
     * hand-edited scalar, a list of numbers — yields an empty set rather than a class-cast failure.
     */
    private fun CommentedConfig.stringList(key: String): Set<String> {
        val stored = get<Any?>(key) as? Collection<*> ?: return emptySet()
        return stored.filterIsInstance<String>().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private companion object {
        /** Configuration keys. Part of the on-disk format — see `config.toml`. */
        const val URL_KEY = "grinderUrl"
        const val INTERVAL_KEY = "refreshIntervalSeconds"

        const val DEFAULT_INTERVAL = 5
        const val MIN_INTERVAL = 1
        const val MAX_INTERVAL = 3_600
    }
}
