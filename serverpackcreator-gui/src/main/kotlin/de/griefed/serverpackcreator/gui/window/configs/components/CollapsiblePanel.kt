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

import java.awt.BorderLayout
import java.awt.Component
import java.awt.FontMetrics
import java.awt.Rectangle
import java.awt.event.*
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.border.Border
import javax.swing.border.TitledBorder


/**
 * [CodeRanch Expand-Collapse-Panels](https://coderanch.com/t/341737/java/Expand-Collapse-Panels)
 * @author rgd
 */
class CollapsiblePanel(
    private var title: String = "Collapsible Panel",
    panel: JPanel,
    private var border: TitledBorder = BorderFactory.createTitledBorder(title)
) : JPanel() {

    private var titleListener: MouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val border: Border = getBorder()
            if (border is TitledBorder) {
                val fm: FontMetrics = getFontMetrics(font)
                val titleWidth = fm.stringWidth(border.title) + 20
                val bounds = Rectangle(0, 0, titleWidth, fm.height)
                if (bounds.contains(e.point)) {
                    toggleVisibility()
                }
            }
        }
    }

    private var contentComponentListener: ComponentListener = object : ComponentAdapter() {
        override fun componentShown(e: ComponentEvent) {
            updateBorderTitle()
        }

        override fun componentHidden(e: ComponentEvent) {
            updateBorderTitle()
        }
    }

    override fun add(comp: Component): Component {
        comp.addComponentListener(contentComponentListener)
        val r = super.add(comp)
        updateBorderTitle()
        return r
    }

    override fun add(name: String, comp: Component): Component {
        comp.addComponentListener(contentComponentListener)
        val r = super.add(name, comp)
        updateBorderTitle()
        return r
    }

    override fun add(comp: Component, index: Int): Component {
        comp.addComponentListener(contentComponentListener)
        val r = super.add(comp, index)
        updateBorderTitle()
        return r
    }

    override fun add(comp: Component, constraints: Any) {
        comp.addComponentListener(contentComponentListener)
        super.add(comp, constraints)
        updateBorderTitle()
    }

    override fun add(comp: Component, constraints: Any, index: Int) {
        comp.addComponentListener(contentComponentListener)
        super.add(comp, constraints, index)
        updateBorderTitle()
    }

    override fun remove(index: Int) {
        val comp = getComponent(index)
        comp.removeComponentListener(contentComponentListener)
        super.remove(index)
    }

    override fun remove(comp: Component) {
        comp.removeComponentListener(contentComponentListener)
        super.remove(comp)
    }

    override fun removeAll() {
        for (c in components) {
            c.removeComponentListener(contentComponentListener)
        }
        super.removeAll()
    }

    fun toggleVisibility(visible: Boolean = hasInvisibleComponent()) {
        for (c in components) {
            c.isVisible = visible
        }
        updateBorderTitle()
    }

    fun updateBorderTitle() {
        var arrow = ""
        if (componentCount > 0) {
            arrow = if (hasInvisibleComponent()) "▽" else "△"
        }
        border.title = "$title $arrow"
        repaint()
    }

    private fun hasInvisibleComponent(): Boolean {
        for (c in components) {
            if (!c.isVisible) {
                return true
            }
        }
        return false
    }

    init {
        setBorder(border)
        layout = BorderLayout()
        addMouseListener(titleListener)
        add(panel)
    }
}