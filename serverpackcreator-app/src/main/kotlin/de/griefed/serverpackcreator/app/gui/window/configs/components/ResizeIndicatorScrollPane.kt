/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.gui.window.configs.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JScrollPane

/**
 * Regular [JScrollPane] but with a graphical indicator that the component is resizable.
 *
 * @author Griefed
 * @author Rob Camick
 */
open class ResizeIndicatorScrollPane(
    private val guiProps: GuiProps,
    private val view: Component,
    verticalScrollbarVisibility: Int = VERTICAL_SCROLLBAR_ALWAYS,
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_NEVER
) : JScrollPane(view, verticalScrollbarVisibility, horizontalScrollbarVisibility) {

    var handleBarPosition: Rectangle? = null
        private set

    /**
     * @author Rob Camick
     * @author Griefed
     */
    override fun paint(g: Graphics?) {
        super.paint(g)
        paintComponents(g)

        val center = width / 2
        val rectStartX = center - (guiProps.handleBar.iconWidth / 2)
        val rectStartY = height - guiProps.handleBar.iconHeight
        handleBarPosition = Rectangle(
            rectStartX,
            rectStartY,
            guiProps.handleBar.iconWidth,
            guiProps.handleBar.iconHeight
        )
        guiProps.handleBar.paintIcon(this, g, handleBarPosition!!.x, handleBarPosition!!.y)
    }

    /**
     * @author Griefed
     */
    @Suppress("unused")
    fun highlight() {
        view.requestFocusInWindow()
    }
}