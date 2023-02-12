/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.gui.window.components

import java.awt.Graphics
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JTextArea
import javax.swing.border.Border
import javax.swing.event.DocumentListener

/**
 * Based on [IconTextField]
 *
 * [it_qna](https://itqna.net/questions/10833/decorating-jtextfield-icon)
 */
class IconTextArea(s: String?) : JTextArea(s) {
    private var mBorder: Border? = null
    private var mIcon: Icon? = null

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        if (mIcon != null) {
            val iconInsets = mBorder!!.getBorderInsets(this)
            mIcon!!.paintIcon(this, graphics, iconInsets.left, height - mIcon!!.iconHeight)
        }
    }

    override fun setBorder(border: Border?) {
        mBorder = border
        if (mIcon == null) {
            super.setBorder(border)
        } else {
            val margin = BorderFactory.createEmptyBorder(0, mIcon!!.iconWidth + ICON_SPACING, 0, 0)
            val compound: Border = BorderFactory.createCompoundBorder(border, margin)
            super.setBorder(compound)
        }
    }

    fun setIcon(icon: Icon?) {
        mIcon = icon
        resetBorder()
    }

    private fun resetBorder() {
        border = mBorder!!
    }

    fun addDocumentListener(listener: DocumentListener?) {
        document.addDocumentListener(listener)
    }

    companion object {
        private const val ICON_SPACING = 4
    }
}