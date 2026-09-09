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
 * full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.app.gui.components

import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.Component
import java.awt.LayoutManager
import javax.swing.JPanel
import javax.swing.JTabbedPane

/**
 * Base tab-panel from which various panels in ServerPackCreator extend to make use of tabs.
 *
 * @author Griefed
 */
abstract class TabPanel(layout: LayoutManager = BorderLayout(), tabsConstraints: String? = null) {

    /** The panel this group lives in, which the parent window adds. */
    val panel = JPanel(layout, true)
    /** The tabbed pane itself. Exposed so subclasses can add tabs; prefer [allTabs] for reading. */
    val tabs = JTabbedPane()

    init {
        if (layout is MigLayout && tabsConstraints != null) {
            panel.add(tabs,tabsConstraints)
        } else {
            panel.add(tabs)
        }
    }

    /** The component of the tab in front, or `null` when there are none. */
    val activeTab: Component?
        get() {
            return tabs.selectedComponent
        }
    /** Every tab's component, in tab order — a snapshot, so iterating it while closing tabs is safe. */
    val allTabs: List<Component>
        get() {
            val paneTabs = mutableListOf<Component>()
            for (tab in 0 until tabs.tabCount) {
                paneTabs.add(tabs.getComponentAt(tab))
            }
            return paneTabs.toList()
        }
}