package de.griefed.serverpackcreator.gui.window.configs

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.components.ElementLabel
import de.griefed.serverpackcreator.gui.window.components.ScrollTextArea
import de.griefed.serverpackcreator.gui.window.components.interactivetable.InteractiveTable
import net.miginfocom.swing.MigLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class AdvancedSettingsPanel(
    exclusionsInfo: JLabel,
    argumentsInfo: JLabel,
    scriptInfo: JLabel,
    exclusions: ScrollTextArea,
    exclusionsRevert: JButton,
    exclusionsBrowser: JButton,
    exclusionsReset: JButton,
    startArgs: ScrollTextArea,
    aikarsFlags: JButton,
    scriptKVPairs: InteractiveTable,
    scriptKVPairsRevert: JButton,
    scriptKVPairsBrowser: JButton,
    scriptKVPairsReset: JButton
) : JPanel(
    MigLayout(
        "left,wrap",
        "[left,::64]5[left]5[left,grow]5[left,::64]5[left,::64]", "30"
    )
) {

    init {
        // Mod Exclusions
        add(exclusionsInfo, "cell 0 0 1 3")
        add(ElementLabel("Mod-Exclusions"), "cell 1 0 1 3")
        add(exclusions, "cell 2 0 1 3,grow,w 10:500:,h 150!")
        add(exclusionsRevert, "cell 3 0 2 1, h 30!, aligny center, alignx center,growx")
        add(exclusionsBrowser, "cell 3 1 2 1, h 30!, aligny center, alignx center,growx")
        add(exclusionsReset, "cell 3 2 2 1, h 30!, aligny top, alignx center,growx")

        // Java Arguments
        add(argumentsInfo, "cell 0 3 1 3")
        add(ElementLabel("Run Arguments"), "cell 1 3 1 3")
        add(startArgs, "cell 2 3 1 3,grow,w 10:500:,h 100!")
        add(aikarsFlags, "cell 3 3 2 3,growy")

        // Script Key-Value Pairs
        add(scriptInfo, "cell 0 6 1 3")
        add(ElementLabel("Script Key-Value Pairs"), "cell 1 6 1 3")
        add(scriptKVPairs.scrollPanel, "cell 2 6 1 3,grow,w 10:500:,h 200!")
        add(scriptKVPairsRevert, "cell 3 6 2 1, h 30!, aligny center, alignx center,growx")
        add(scriptKVPairsBrowser, "cell 3 7 2 1, h 30!, aligny center, alignx center,growx")
        add(scriptKVPairsReset, "cell 3 8 2 1, h 30!, aligny top, alignx center,growx")
        isVisible = false
    }

}