package de.griefed.serverpackcreator.gui.window.components

import de.griefed.serverpackcreator.gui.window.configs.ConfigEditorPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.JTabbedPane

abstract class TabPanel {
    val panel = JPanel()
    val tabs = JTabbedPane()

    init {
        panel.layout = BorderLayout()
        panel.add(tabs)
    }

    val activeTab: Component?
        get() {
            return tabs.selectedComponent
        }
    val allTabs: List<Component>
        get() {
            val paneTabs = mutableListOf<ConfigEditorPanel>()
            for (tab in 0..tabs.tabCount) {
                paneTabs.add(tabs.getTabComponentAt(tab) as ConfigEditorPanel)
            }
            return paneTabs.toList()
        }
}