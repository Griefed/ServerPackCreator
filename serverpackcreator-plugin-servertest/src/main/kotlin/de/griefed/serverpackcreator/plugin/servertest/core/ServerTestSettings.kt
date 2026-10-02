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
package de.griefed.serverpackcreator.plugin.servertest.core

import com.electronwill.nightconfig.core.CommentedConfig

/**
 * Reads the plugin's settings out of the `CommentedConfig` ServerPackCreator owns.
 *
 * Holds no state of its own — a typed view over that config, cheap to construct wherever one is in hand, in
 * the same shape the grinder plugin's `SelectionStore` uses.
 *
 * Every read tolerates a damaged value. The config is a file a user may hand-edit, and this object is built
 * while the tab is, where throwing over a typo would take the GUI's whole tab assembly with it. A missing or
 * unusable value falls back to the shipped default rather than being reported.
 *
 * @param config The plugin configuration provided by ServerPackCreator.
 * @author Griefed
 */
class ServerTestSettings(private val config: CommentedConfig) {

    /** First port a launched server may be given. Clamping into a usable range is [PortAllocator]'s job. */
    val portRangeStart: Int get() = intOrDefault(PORT_RANGE_START_KEY, PortAllocator.DEFAULT_RANGE_START)

    /** Last port a launched server may be given, inclusive. */
    val portRangeEnd: Int get() = intOrDefault(PORT_RANGE_END_KEY, PortAllocator.DEFAULT_RANGE_END)

    /**
     * How many console lines a running server keeps on screen.
     *
     * Coerced to at least one line: a scrollback of zero would make the console pane useless in a way that
     * reads as the server producing no output at all.
     */
    val consoleScrollback: Int
        get() = intOrDefault(CONSOLE_SCROLLBACK_KEY, DEFAULT_CONSOLE_SCROLLBACK).coerceAtLeast(1)

    /** The integer at [key], or [fallback] when it is absent or is not a number. */
    private fun intOrDefault(key: String, fallback: Int): Int = (config.get(key) as? Number)?.toInt() ?: fallback

    companion object {
        /** Config key for [portRangeStart]. **Stable across releases** — a rename orphans a user's setting. */
        const val PORT_RANGE_START_KEY = "portRangeStart"

        /** Config key for [portRangeEnd]. */
        const val PORT_RANGE_END_KEY = "portRangeEnd"

        /** Config key for [consoleScrollback]. */
        const val CONSOLE_SCROLLBACK_KEY = "consoleScrollback"

        /** Lines kept per running server when nothing is configured; matches SPC's own log panes. */
        const val DEFAULT_CONSOLE_SCROLLBACK = 2000
    }
}
