package de.griefed.serverpackcreator.gui.components

import java.awt.BorderLayout
import java.awt.Component
import java.awt.LayoutManager
import javax.swing.JPanel
import javax.swing.JTabbedPane

abstract class TabPanel(layout: LayoutManager = BorderLayout()) {
    val panel = JPanel(layout, true)
    val tabs = JTabbedPane()

    init {
        panel.add(tabs)
    }

    val activeTab: Component?
        get() {
            return tabs.selectedComponent
        }
    val allTabs: List<Component>
        get() {
            val paneTabs = mutableListOf<Component>()
            for (tab in 0 until tabs.tabCount) {
                paneTabs.add(tabs.getComponentAt(tab))
            }
            return paneTabs.toList()
        }
}