/*
 * MIT License
 *
 * Copyright (c) 2023 Griefed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package de.griefed.example.kotlin.gui.tab

import Example
import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import java.util.*
import javax.swing.*

/**
 * Provide a couple of elements to globally configure the plugin and to start playing Tetris.
 *
 * Construct a new panel to add to the ServerPackCreator GUI as an additional tab.
 *
 * @param versionMeta   Instance of {@link VersionMeta} so you can work with available Minecraft,
 *                      Forge, Fabric, LegacyFabric and Quilt versions.
 * @param apiProperties Instance of {@link ApiProperties} The current configuration of
 *                      ServerPackCreator, like the default list of clientside-only mods, the
 *                      server pack directory etc.
 * @param utilities     Instance of {@link Utilities} commonly used across ServerPackCreator.
 * @param addonConfig   Addon specific configuration conveniently provided by ServerPackCreator.
 *                      This is the global configuration of the addon which provides the
 *                      ConfigPanelExtension to ServerPackCreator.
 * @param configFile    The config-file corresponding to the ID of the addon, wrapped in an
 *                      Optional.
 */
class TetrisTab(
    versionMeta: VersionMeta?,
    apiProperties: ApiProperties?,
    utilities: Utilities?,
    addonConfig: Optional<CommentedConfig>,
    configFile: Optional<File>
) : ExtensionTab(versionMeta!!, apiProperties!!, utilities!!, addonConfig, configFile) {
    private var ami: JTextField = JTextField()
    private var here: JTextField = JTextField()

    init {
        layout = GridBagLayout()
        val gridBagConstraints = GridBagConstraints()
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.anchor = GridBagConstraints.CENTER
        gridBagConstraints.gridwidth = 1
        gridBagConstraints.gridheight = 1
        gridBagConstraints.weightx = 1.0
        gridBagConstraints.weighty = 1.0
        val miniGame = JPanel()
        miniGame.border = BorderFactory.createTitledBorder(Example.example_tetris_tab_title.toString())
        miniGame.layout = GridBagLayout()
        val miniGameConstraints = GridBagConstraints()
        miniGameConstraints.fill = GridBagConstraints.HORIZONTAL
        miniGameConstraints.anchor = GridBagConstraints.CENTER
        val press = JLabel(Example.example_tetris_tab_label.toString())
        val play = JButton()
        play.icon = ImageIcon(Objects.requireNonNull(TetrisTab::class.java.getResource("/play.png")))
        play.toolTipText = Example.example_tetris_tab_tooltip.toString()
        play.addActionListener { Tetris.main(null) }
        miniGameConstraints.gridx = 0
        miniGameConstraints.gridy = 0
        miniGame.add(press, miniGameConstraints)
        miniGameConstraints.gridx = 0
        miniGameConstraints.gridy = 1
        miniGame.add(play, miniGameConstraints)
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 0
        gridBagConstraints.gridwidth = 2
        miniGame.preferredSize = Dimension(300, 135)
        add(miniGame, gridBagConstraints)
        gridBagConstraints.gridy += 1
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.fill = GridBagConstraints.BOTH
        gridBagConstraints.insets = Insets(10, 10, 10, 10)
        if (addonConfig.isPresent && configFile.isPresent) {
            val config = JPanel()
            config.border = BorderFactory.createTitledBorder(Example.example_addon_config_border.toString())
            config.layout = GridBagLayout()
            val configConstraints = GridBagConstraints()
            configConstraints.fill = GridBagConstraints.HORIZONTAL
            configConstraints.anchor = GridBagConstraints.NORTHWEST
            configConstraints.gridwidth = 1
            configConstraints.gridheight = 1
            configConstraints.weighty = 1.0
            val who = JLabel(Example.example_addon_config_who.toString())
            configConstraints.gridx = 0
            configConstraints.gridy = 0
            configConstraints.weightx = 0.1
            config.add(who, configConstraints)
            ami.text = addonConfig.get().get("whoami")
            configConstraints.gridx = 1
            configConstraints.weightx = 1.0
            config.add(ami, configConstraints)
            val why = JLabel(Example.example_addon_config_why.toString())
            configConstraints.gridx = 0
            configConstraints.gridy += 1
            configConstraints.weightx = 0.1
            config.add(why, configConstraints)
            here.text = addonConfig.get().get("whyamihere")
            configConstraints.gridx = 1
            configConstraints.weightx = 1.0
            config.add(here, configConstraints)
            val set = JButton(Example.example_addon_config_button.toString())
            configConstraints.gridx = 0
            configConstraints.gridy += 1
            configConstraints.gridwidth = 2
            set.addActionListener {
                addonConfig.get().set<Any>("whoami", ami.text)
                addonConfig.get().set<Any>("whyamihere", here.text)
                if (JOptionPane.showConfirmDialog(
                        null,
                        Example.example_addon_config_action_message.toString(),
                        Example.example_addon_config_action_title.toString(),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                    ) == 0
                ) {
                    saveConfiguration()
                } else {
                    JOptionPane.showMessageDialog(
                        null,
                        Example.example_addon_config_action_abort_message.toString(),
                        Example.example_addon_config_action_abort_title.toString(),
                        JOptionPane.WARNING_MESSAGE
                    )
                }
            }
            config.add(set, configConstraints)
            add(config, gridBagConstraints)
        }
    }
}