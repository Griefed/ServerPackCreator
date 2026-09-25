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
package de.griefed.serverpackcreator.plugin.servertest.gui

import javax.swing.JLabel
import javax.swing.table.DefaultTableCellRenderer

/**
 * Swing components that show text exactly as given, never as markup.
 *
 * `JLabel` — and `DefaultTableCellRenderer`, which is one — installs an HTML view whenever the string handed
 * to it starts with `<html>`, and Swing's HTML subset **loads remote images**. So a directory a user named
 * `<html><img src="http://…">` would make ServerPackCreator issue a request nobody asked for, and a pack's
 * `manifest.json` is a hand-editable file whose version strings land in the same table.
 *
 * The grinder plugin carries the same object for the same reason, and this is a deliberate second copy rather
 * than a shared one: both plugins depend on `serverpackcreator-api` alone, and promoting eight lines into the
 * published API to avoid duplicating them would be a permanent compatibility obligation bought for very
 * little. If a third plugin needs it, that trade changes.
 *
 * **Use these for anything originating outside this plugin.** A literal written here is safe either way. The
 * console itself needs neither: `JTextArea` renders plain text and never parses markup.
 *
 * @author Griefed
 */
object PlainTextRendering {

    /**
     * The client property Swing consults before deciding a string is HTML. Set to `true`, the component
     * treats every string as literal text.
     */
    private const val HTML_DISABLE = "html.disable"

    /** A label showing [text] literally, whatever it looks like. */
    fun label(text: String): JLabel = JLabel().apply {
        putClientProperty(HTML_DISABLE, true)
        this.text = text
    }

    /**
     * A table cell renderer that shows every value literally. Returned per table rather than shared, because
     * a renderer carries the state of the cell it last rendered.
     */
    fun tableCellRenderer(): DefaultTableCellRenderer = object : DefaultTableCellRenderer() {
        init {
            putClientProperty(HTML_DISABLE, true)
        }
    }
}
