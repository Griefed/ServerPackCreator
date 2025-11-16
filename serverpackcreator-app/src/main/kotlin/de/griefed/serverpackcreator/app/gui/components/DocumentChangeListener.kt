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
package de.griefed.serverpackcreator.app.gui.components

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * DocumentChangeListener to fire when the text of a textarea of text-field is changed.
 *
 * @author Griefed
 */
interface DocumentChangeListener : DocumentListener {

    /**
     * Convenience method called by [insertUpdate], [removeUpdate] and [changedUpdate] upon change of the document.
     *
     * @author Griefed
     */
    fun update(e: DocumentEvent)

    override fun insertUpdate(e: DocumentEvent) {
        update(e)
    }

    override fun removeUpdate(e: DocumentEvent) {
        update(e)
    }

    override fun changedUpdate(e: DocumentEvent) {
        update(e)
    }
}