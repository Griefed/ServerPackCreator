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
package de.griefed.serverpackcreator.app.gui.window.control

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.configs.TabbedConfigsTab
import de.griefed.serverpackcreator.app.gui.window.control.components.GenerationButton
import de.griefed.serverpackcreator.app.gui.window.control.components.LarsonScanner
import de.griefed.serverpackcreator.app.gui.window.control.components.ServerPacksButton
import kotlinx.coroutines.*
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Desktop
import java.io.File
import java.io.IOException
import javax.swing.JOptionPane
import javax.swing.JPanel

/**
 * Control panel giving the user the ability to start a server pack generation from the currently selected server pack
 * config tab, as well as the option to open the server pack directory which houses all generated packs. The latest
 * log messages of the INFO type are displayed and a [LarsonScanner] indicating whether a generation is taking place or
 * not.
 *
 * @author Griefed
 */
class ControlPanel(
    private val guiProps: GuiProps,
    private val tabbedConfigsTab: TabbedConfigsTab,
    private val larsonScanner: LarsonScanner,
    private val apiWrapper: ApiWrapper,
    private val mainFrame: MainFrame
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val statusPanel = StatusPanel()

    private val runGeneration = GenerationButton(guiProps) { generate() }
    private val serverPacks = ServerPacksButton(guiProps) {
        FileUtilities.openFolder(apiWrapper.apiProperties.serverPacksDirectory)
    }
    val panel = JPanel()

    init {
        panel.layout = MigLayout(
            "",
            "0[200!]0[grow]0",
            "0[75!,bottom]10[75!,top]0"
        )
        panel.add(runGeneration, "cell 0 0 1 1,grow,height 50!,width 150!,align center")
        panel.add(serverPacks, "cell 0 1 1 1,grow,height 50!,width 150!,align center")
        panel.add(statusPanel.panel, "cell 1 0 1 2,grow,push, h 160!")
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun generate() {
        GlobalScope.launch(guiProps.generationDispatcher, CoroutineStart.ATOMIC) {
            launchGeneration()
            readyForGeneration()
        }
    }

    /**
     * Upon button-press, check the entered configuration and if successful, generate a server pack.
     *
     * @author Griefed
     */
    private fun launchGeneration() {
        runGeneration.isEnabled = false
        larsonScanner.loadConfig(guiProps.busyConfig)
        var decision = 0
        if (tabbedConfigsTab.selectedEditor == null) {
            JOptionPane.showMessageDialog(
                panel,
                Translations.createserverpack_log_error_configuration_none_message.toString(),
                Translations.createserverpack_log_error_configuration_none_title.toString(),
                JOptionPane.INFORMATION_MESSAGE, guiProps.largeInfoIcon
            )
            statusPanel.updateStatus(Translations.createserverpack_log_error_configuration_none_message.toString())
            return
        }
        if (tabbedConfigsTab.selectedEditor!!.getInclusions().any { inclusion -> inclusion.source == "lazy_mode" }) {
            val message = Translations.configuration_log_warn_checkconfig_copydirs_lazymode0.toString() + "\n" +
                    Translations.configuration_log_warn_checkconfig_copydirs_lazymode1.toString() + "\n" +
                    Translations.configuration_log_warn_checkconfig_copydirs_lazymode2.toString() + "\n" +
                    Translations.configuration_log_warn_checkconfig_copydirs_lazymode3.toString()
            decision = JOptionPane.showConfirmDialog(
                panel,
                message,
                Translations.createserverpack_gui_createserverpack_lazymode.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                guiProps.largeWarningIcon
            )
        }
        log.debug("Case $decision")
        when (decision) {
            0 -> runGenerationTasks()
        }
    }

    /**
     * Set the GUI ready for the next generation.
     *
     * @author Griefed
     */
    private fun readyForGeneration() {
        runGeneration.isEnabled = true
        larsonScanner.loadConfig(guiProps.idleConfig)
    }

    /**
     * Generate a server pack from the current configuration in the GUI.
     *
     * @author Griefed
     */
    private fun runGenerationTasks() {
        val activeTab = tabbedConfigsTab.selectedEditor!!
        val packConfig: PackConfig = activeTab.getCurrentConfiguration()
        val check = ConfigCheck()

        log.info("Checking entered configuration.")
        statusPanel.updateStatus(Translations.createserverpack_log_info_buttoncreateserverpack_start.toString())

        if (apiWrapper.configurationHandler.checkConfiguration(packConfig, check, true).allChecksPassed) {
            if (activeTab.getModpackDirectory().endsWith(".zip",ignoreCase = true)) {
                JOptionPane.showMessageDialog(
                    panel.parent,
                    Translations.createserverpack_gui_config_zip_info_message.toString(),
                    Translations.createserverpack_gui_config_zip_info_title.toString(),
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
            log.info("Config check passed.")
            statusPanel.updateStatus(Translations.createserverpack_log_info_buttoncreateserverpack_checked.toString())
            generateServerPack(packConfig)
        } else {
            generationFailed(check.encounteredErrors)
        }
    }

    /**
     * @author Griefed
     */
    private fun generateServerPack(packConfig: PackConfig) {
        log.info("Starting ServerPackCreator run.")
        statusPanel.updateStatus(Translations.createserverpack_log_info_buttoncreateserverpack_generating(File(packConfig.modpackDir).name))
        try {
            val generation = apiWrapper.serverPackHandler.run(packConfig)
            statusPanel.updateStatus(Translations.createserverpack_log_info_buttoncreateserverpack_ready.toString())
            if (guiProps.generationFocusEnabled) {
                mainFrame.toFront()
            }
            if (generation.success) {
                if (JOptionPane.showConfirmDialog(
                        panel.parent,
                        Translations.createserverpack_gui_createserverpack_openfolder_browse.toString(),
                        Translations.createserverpack_gui_createserverpack_openfolder_title.toString(),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        guiProps.infoIcon
                    ) == 0
                ) {
                    try {
                        Desktop.getDesktop().open(generation.serverPack)

                    } catch (ex: IOException) {
                        log.error("Error opening file explorer for server pack.", ex)
                    }
                }
            } else {
                generationFailed(generation.errors)
            }
        } catch (ex: Exception) {
            log.error("An error occurred when generating the server pack.", ex)
        }
    }

    /**
     * @author Griefed
     */
    private fun generationFailed(encounteredErrors: List<String>) {
        statusPanel.updateStatus(Translations.createserverpack_gui_buttongenerateserverpack_fail.toString())
        if (encounteredErrors.isNotEmpty()) {
            val errors = StringBuilder()
            for (i in encounteredErrors.indices) {
                errors.append(i + 1).append(": ").append(encounteredErrors[i]).append("    ").append("\n")
            }
            JOptionPane.showMessageDialog(
                panel.parent,
                errors,
                Translations.createserverpack_gui_createserverpack_errors_encountered(encounteredErrors.size),
                JOptionPane.ERROR_MESSAGE,
                guiProps.errorIcon
            )
        }
    }

    /**
     * Update the labels in the status panel.
     *
     * @param text The text to update the status with.
     * @author Griefed
     */
    fun updateStatus(text: String) {
        statusPanel.updateStatus(text)
    }
}
