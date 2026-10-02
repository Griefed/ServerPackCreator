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

import java.awt.Component
import javax.swing.JOptionPane

/**
 * The plugin's dialogs, in one place so none of them can be raised with a raw string.
 *
 * `JOptionPane` wraps a `String` message in a `JLabel`, which installs an HTML view for anything starting
 * with `<html>` — and Swing's HTML subset loads remote images. Every message this plugin shows names a
 * server pack, whose directory name the user chose and an imported modpack may have influenced, or carries
 * an exception's text. Measured: the same hostile string is markup as a `String` and literal as a
 * [PlainTextRendering] label.
 *
 * Routing every dialog through here is what makes that checkable — a `JOptionPane` call anywhere else
 * would reintroduce the surface one line at a time.
 *
 * @author Griefed
 */
object Dialogs {

    /**
     * Warn the user about something they need to act on, showing [message] exactly as given.
     *
     * @param parent  The component the dialog is centred on.
     * @param message The text to show, treated as literal text however it looks.
     */
    fun warn(parent: Component?, message: String) {
        JOptionPane.showMessageDialog(
            parent,
            PlainTextRendering.label(message),
            TITLE,
            JOptionPane.WARNING_MESSAGE
        )
    }

    /** The title every dialog from this plugin carries, so a user can tell who is asking. */
    const val TITLE = "Server Test"
}
