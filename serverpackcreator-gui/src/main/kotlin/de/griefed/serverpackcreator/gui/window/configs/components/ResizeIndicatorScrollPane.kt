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
package de.griefed.serverpackcreator.gui.window.configs.components

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.utilities.getScaledInstance
import java.awt.Component
import java.awt.Graphics
import javax.swing.JScrollPane

/**
 * Regular [JScrollPane] but with a graphical indicator that the component is resizable.
 *
 * @author Griefed
 */
open class ResizeIndicatorScrollPane(
    private val guiProps: GuiProps,
    view: Component? = null,
    verticalScrollbarVisibility: Int = VERTICAL_SCROLLBAR_ALWAYS,
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_NEVER
) : JScrollPane(view, verticalScrollbarVisibility, horizontalScrollbarVisibility) {
    constructor(guiProps: GuiProps, verticalScrollbarVisibility: Int, horizontalScrollbarVisibility: Int) : this(
        guiProps,
        null,
        verticalScrollbarVisibility,
        horizontalScrollbarVisibility
    )

    override fun paint(g: Graphics?) {
        super.paint(g)
        paintComponents(g)

        val x = insets.left
        val y = height - guiProps.resizeIndicator.iconHeight
        val width = width - insets.right - x
        val scaled = guiProps.resizeIndicator.getScaledInstance(width, guiProps.resizeIndicator.iconHeight)
        scaled.paintIcon(this, g, x, y)
    }
}