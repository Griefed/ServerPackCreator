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
package de.griefed.serverpackcreator.app.gui.window.configs.components.inclusions

import de.griefed.serverpackcreator.api.config.InclusionSpecification
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * Custom renderer for [InclusionSpecification] to display the source of the
 * spec in a [JList]. If no source is specified, but the spec contains a filter, then it is considered a global filter
 * and marked as such in the [JList].
 *
 * @author Griefed
 */
class InclusionSpecificationRenderer : JLabel(), ListCellRenderer<InclusionSpecification> {

    init {
        isOpaque = true
    }

    override fun getListCellRendererComponent(
        list: JList<out InclusionSpecification>,
        value: InclusionSpecification,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {

        text = if (value.isGlobalFilter()) {
            if (value.hasInclusionFilter()) {
                "(I) ${value.inclusionFilter}"
            } else {
                "(E) ${value.exclusionFilter}"
            }
        } else {
            val prefix = StringBuilder()
            if (value.hasDestination()) {
                prefix.append("D")
            } else {
                prefix.append("-")
            }
            if (value.hasInclusionFilter()) {
                prefix.append("I")
            } else {
                prefix.append("-")
            }
            if (value.hasExclusionFilter()) {
                prefix.append("E")
            } else {
                prefix.append("-")
            }

            prefix.append(" ${value.source}")
            prefix.toString()
        }

        if (isSelected) {
            background = list.selectionBackground
            foreground = list.selectionForeground
        } else {
            background = list.background
            foreground = list.foreground
        }
        return this
    }
}