/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.gui.utilities

import java.awt.Component
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JDialog
import javax.swing.JOptionPane

/**
 * Common dialog utilities to make the creation of and working with dialogs a little easier.
 *
 * @author Griefed
 */
class DialogUtilities {
    companion object {

        /**
         * Create a new [JOptionPane] using the provided [message], [messageType],[icon],[options] and [initialValue].
         * @author Griefed
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun createOptionPane(
            message: Any,
            messageType: Int = JOptionPane.PLAIN_MESSAGE, optionType: Int = JOptionPane.DEFAULT_OPTION,
            icon: Icon? = null,
            options: Array<*>? = null,
            initialValue: Any? = null
        ): JOptionPane {
            return JOptionPane(
                message,
                messageType, optionType,
                icon,
                options,
                initialValue
            )
        }

        /**
         * Create a new [JDialog] using the provided [message],[title],[parent],[messageType],[optionType],[icon],
         * [resizable],[display],[options],[initialValue].
         *
         * * [resizable] does exactly what the name implies. If `true`, the created dialog will be resizable.
         * * [display] does exactly what the name implies. If `true`, the dialog will be visible as soon as it was created.
         * @author Griefed
         */
        fun createDialog(
            message: Any,
            title: String,
            parent: Component = JDialog(),
            messageType: Int = JOptionPane.PLAIN_MESSAGE, optionType: Int = JOptionPane.DEFAULT_OPTION,
            icon: Icon? = null,
            resizable: Boolean = false, display: Boolean = true,
            options: Array<*>? = null, initialValue: Any? = null,
            width: Int? = null, height: Int? = null
        ): JDialog {
            val optionPane = createOptionPane(
                message,
                messageType, optionType,
                icon,
                options,
                initialValue
            )

            val dialog = optionPane.createDialog(
                parent,
                title
            )

            if (width != null && height != null) {
                optionPane.minimumSize = Dimension(width, height)
                optionPane.size = Dimension(width, height)
                dialog.minimumSize = Dimension(width, height)
                dialog.size = Dimension(width, height)
            }

            dialog.isResizable = resizable
            dialog.isVisible = display
            return dialog
        }

        /**
         * Based on [JOptionPane.showOptionDialog], you can allow the created dialog to be resizable by setting
         * [resizable] to `true`.
         */
        fun createShowGet(
            message: Any,
            title: String,
            parent: Component = JDialog(),
            messageType: Int = JOptionPane.PLAIN_MESSAGE,
            optionType: Int = JOptionPane.DEFAULT_OPTION,
            icon: Icon? = null,
            resizable: Boolean = false,
            options: Array<*>? = null,
            initialValue: Any? = null
        ): Int {
            val pane = createOptionPane(
                message,
                messageType, optionType,
                icon,
                options,
                initialValue
            )
            pane.selectInitialValue()
            val dialog = pane.createDialog(parent, title)
            dialog.isResizable = resizable
            dialog.isVisible = true
            dialog.dispose()
            val selected: Any = pane.value ?: return JOptionPane.CLOSED_OPTION
            if (options == null) {
                return selected as? Int ?: JOptionPane.CLOSED_OPTION
            } else {
                for (i in options.indices) {
                    if (options[i] == selected) {
                        return i
                    }
                }
            }
            return JOptionPane.CLOSED_OPTION
        }
    }
}