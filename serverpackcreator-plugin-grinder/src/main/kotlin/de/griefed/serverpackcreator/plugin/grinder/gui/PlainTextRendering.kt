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
package de.griefed.serverpackcreator.plugin.grinder.gui

import javax.swing.JLabel
import javax.swing.table.DefaultTableCellRenderer

/**
 * Swing components that show text exactly as given, never as markup.
 *
 * `JLabel` — and `DefaultTableCellRenderer`, which is one — installs an HTML view whenever the string
 * handed to it starts with `<html>`. Everything this plugin displays comes from somewhere else: a
 * verdict's `slug` and `detail` are mod metadata the grinder scraped off a platform, the `/status` worker
 * and rule-error strings are the daemon's, and that daemon's report server carries **no authentication**.
 * Swing's HTML subset loads remote images, so `<html><img src="http://…">` in a mod name would make a
 * user's ServerPackCreator window issue a request they never made.
 *
 * The grinder's own web report was hardened against this class of input — its `CLAUDE.md` records that it
 * no longer puts mod-supplied text inside a `<script>` block at all — so a Swing surface interpreting the
 * same strings would reintroduce a defect this project already paid to remove.
 *
 * **Use these for anything originating outside this plugin.** A literal written here is safe either way,
 * and the warning banner deliberately keeps its own markup.
 *
 * @author Griefed
 */
object PlainTextRendering {

    /**
     * The client property Swing sets to the parsed document when it decides a string is HTML, and the
     * one that switches the behaviour off. `"html.disable"` is checked before parsing; setting it to
     * `true` makes the component treat every string as literal text.
     */
    private const val HTML_DISABLE = "html.disable"

    /** A label showing [text] literally, whatever it looks like. */
    fun label(text: String): JLabel = JLabel().apply {
        putClientProperty(HTML_DISABLE, true)
        this.text = text
    }

    /**
     * A table cell renderer that shows every value literally. Returned per table rather than shared,
     * because a renderer carries the state of the cell it last rendered.
     */
    fun tableCellRenderer(): DefaultTableCellRenderer = object : DefaultTableCellRenderer() {
        init {
            putClientProperty(HTML_DISABLE, true)
        }
    }
}
