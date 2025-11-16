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
package de.griefed.serverpackcreator.app.gui.window.configs.components.advanced

import Translations
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.app.gui.components.ElementLabel
import de.griefed.serverpackcreator.app.gui.components.ScrollTextArea
import de.griefed.serverpackcreator.app.gui.components.StatusIcon
import de.griefed.serverpackcreator.app.gui.window.configs.ConfigEditor
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JPanel

/**
 * Advanced settings panel allowing a user to configure various aspects of a server pack config, such as
 * * clientside-mods to exclude
 * * JVM args/flags to use when starting the server
 * * Key-Value pairs to provide during script generation, in case a user makes use of custom script templates
 *
 * @author Griefed
 */
class AdvancedSettingsPanel(
    private val configEditor: ConfigEditor,
    exclusions: ScrollTextArea,
    whitelisted: ScrollTextArea,
    javaArgs: ScrollTextArea,
    scriptKVPairs: ScriptKVPairs,
    private val guiProps: GuiProps,
    private val apiProperties: ApiProperties
) : JPanel(
    MigLayout(
        "left,wrap",
        "[left,::64]5[left]5[left,grow]5[left,::64]5[left,::64]", "30"
    )
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val clientModsIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelclientmods_tip.toString())
    private val clientModsLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelclientmods.toString())
    private val clientModsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.createserverpack_gui_buttonclientmods_revert_tip.toString(), guiProps) { revertExclusions() }
    private val clientModsChooser = BalloonTipButton(null, guiProps.folderIcon, Translations.createserverpack_gui_browser.toString(), guiProps) { selectClientMods() }
    private val clientModsReset = BalloonTipButton(null, guiProps.resetIcon, Translations.createserverpack_gui_buttonclientmods_reset_tip.toString(), guiProps) { resetClientMods() }

    private val whitelistModsIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelwhitelistmods_tip.toString())
    private val whitelistModsLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelwhitelistmods.toString())
    private val whitelistModsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.createserverpack_gui_buttonwhitelistmods_revert_tip.toString(), guiProps) { revertWhitelist() }
    private val whitelistModsChooser = BalloonTipButton(null, guiProps.folderIcon, Translations.createserverpack_gui_browser.toString(), guiProps) { selectWhitelist() }
    private val whitelistModsReset = BalloonTipButton(null, guiProps.resetIcon, Translations.createserverpack_gui_buttonwhitelistmods_reset_tip.toString(), guiProps) { resetWhitelist() }

    private val javaArgsIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_javaargs_tip.toString())
    private val javaArgsLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_javaargs.toString())
    private val javaArgsAikarsFlagsButton = AikarsFlagsButton(configEditor, guiProps)

    private val advScriptSettingsIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_scriptsettings_label_tooltip.toString())
    private val advScriptSettingsLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_scriptsettings_label.toString())
    private val advScriptSettingsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.createserverpack_gui_revert.toString(), guiProps) { revertScriptKVPairs() }
    private val advScriptSettingsReset = BalloonTipButton(null, guiProps.resetIcon, Translations.createserverpack_gui_reset.toString(), guiProps) { resetScriptKVPairs() }

    init {
        var column = 0
        // Mod Exclusions
        add(clientModsIcon, "cell 0 0 1 3")
        add(clientModsLabel, "cell 1 0 1 3")
        add(exclusions, "cell 2 0 1 3,grow,w 10:500:,h 150!")
        add(clientModsRevert, "cell 3 0 2 1, h 30!, aligny center, alignx center,growx")
        add(clientModsChooser, "cell 3 1 2 1, h 30!, aligny center, alignx center,growx")
        add(clientModsReset, "cell 3 2 2 1, h 30!, aligny top, alignx center,growx")

        // Mod Whitelist
        column += 3
        add(whitelistModsIcon, "cell 0 $column 1 3")
        add(whitelistModsLabel, "cell 1 $column 1 3")
        add(whitelisted, "cell 2 $column 1 3,grow,w 10:500:,h 150!")
        add(whitelistModsRevert, "cell 3 $column 2 1, h 30!, aligny center, alignx center,growx")
        add(whitelistModsChooser, "cell 3 ${column + 1} 2 1, h 30!, aligny center, alignx center,growx")
        add(whitelistModsReset, "cell 3 ${column + 2} 2 1, h 30!, aligny top, alignx center,growx")

        // Java Arguments
        column += 3
        add(javaArgsIcon, "cell 0 $column 1 3")
        add(javaArgsLabel, "cell 1 $column 1 3")
        add(javaArgs, "cell 2 $column 1 3,grow,w 10:500:,h 100!")
        add(javaArgsAikarsFlagsButton, "cell 3 $column 2 3,growy")

        // Script Key-Value Pairs
        column += 3
        add(advScriptSettingsIcon, "cell 0 $column 1 3")
        add(advScriptSettingsLabel, "cell 1 $column 1 3")
        add(scriptKVPairs.scrollPanel, "cell 2 $column 1 3,grow,w 10:500:,h 200!")
        add(advScriptSettingsRevert, "cell 3 $column 2 1, h 30!, aligny center, alignx center,growx")
        add(advScriptSettingsReset, "cell 3 ${column + 1} 2 1, h 30!, aligny top, alignx center,growx")
        isVisible = false
    }

    /**
     * Reverts the list of clientside-only mods to the value of the last loaded configuration, if one
     * is available.
     *
     * @author Griefed
     */
    private fun revertExclusions() {
        if (configEditor.lastConfig != null) {
            configEditor.setClientSideMods(configEditor.lastConfig!!.clientMods)
            configEditor.validateInputFields()
        }
    }

    /**
     * Reverts the list of whitelisted mods to the value of the last loaded configuration, if one
     * is available.
     *
     * @author Griefed
     */
    private fun revertWhitelist() {
        if (configEditor.lastConfig != null) {
            configEditor.setWhitelist(configEditor.lastConfig!!.modsWhitelist)
            configEditor.validateInputFields()
        }
    }

    /**
     * Let the user choose the mods to exclude by browsing the filesystem and selecting the mod-files.
     * @author Griefed
     */
    private fun selectClientMods() {
        val clientModsChooser = if (File(configEditor.getModpackDirectory(), "mods").isDirectory) {
            ClientModsChooser(File(configEditor.getModpackDirectory(), "mods"), guiProps.defaultFileChooserDimension)
        } else {
            ClientModsChooser(guiProps.defaultFileChooserDimension)
        }

        if (clientModsChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            val clientMods: Array<File> = clientModsChooser.selectedFiles
            val clientModsFilenames: TreeSet<String> = TreeSet()
            clientModsFilenames.addAll(configEditor.getClientSideModsList())
            for (mod in clientMods) {
                clientModsFilenames.add(mod.name)
            }
            configEditor.setClientSideMods(clientModsFilenames.toMutableList())
            log.debug("Selected mods: $clientModsFilenames")
        }
    }

    /**
     * Let the user choose the mods to whitelist by browsing the filesystem and selecting the mod-files.
     * @author Griefed
     */
    private fun selectWhitelist() {
        val whitelistChooser = if (File(configEditor.getModpackDirectory(), "mods").isDirectory) {
            WhitelistChooser(File(configEditor.getModpackDirectory(), "mods"), guiProps.defaultFileChooserDimension)
        } else {
            WhitelistChooser(guiProps.defaultFileChooserDimension)
        }

        if (whitelistChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            val whitelist: Array<File> = whitelistChooser.selectedFiles
            val whitelistFilenames: TreeSet<String> = TreeSet()
            whitelistFilenames.addAll(configEditor.getWhitelistList())
            for (mod in whitelist) {
                whitelistFilenames.add(mod.name)
            }
            configEditor.setWhitelist(whitelistFilenames.toMutableList())
            log.debug("Selected mods: $whitelistFilenames")
        }
    }

    /**
     * Reset the clientside-mods list to the default value from the ServerPackCreator configuration.
     * @author Griefed
     */
    private fun resetClientMods() {
        val current = configEditor.getClientSideModsList()
        val default = apiProperties.clientSideMods()
        if (!default.any { mod -> !default.contains(mod) }) {
            when (JOptionPane.showConfirmDialog(
                this,
                Translations.createserverpack_gui_buttonclientmods_reset_merge_message.toString(),
                Translations.createserverpack_gui_buttonclientmods_reset_merge_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )) {
                0 -> {
                    val set = TreeSet<String>()
                    set.addAll(current)
                    set.addAll(default)
                    configEditor.setClientSideMods(set.toMutableList())
                }

                1 -> configEditor.setClientSideMods(default.toMutableList())
            }
        } else {
            configEditor.setClientSideMods(default.toMutableList())
        }
    }

    /**
     * Reset the whitelist to the default value from the ServerPackCreator configuration.
     * @author Griefed
     */
    private fun resetWhitelist() {
        val current = configEditor.getWhitelistList()
        val default = apiProperties.whitelistedMods()
        if (!default.any { mod -> !default.contains(mod) }) {
            when (JOptionPane.showConfirmDialog(
                this,
                Translations.createserverpack_gui_buttonwhitelistmods_reset_merge_message.toString(),
                Translations.createserverpack_gui_buttonwhitelistmods_reset_merge_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )) {
                0 -> {
                    val set = TreeSet<String>()
                    set.addAll(current)
                    set.addAll(default)
                    configEditor.setWhitelist(set.toMutableList())
                }

                1 -> configEditor.setWhitelist(default.toMutableList())
            }
        } else {
            configEditor.setWhitelist(default.toMutableList())
        }
    }

    private fun revertScriptKVPairs() {
        if (configEditor.lastConfig != null) {
            configEditor.setScriptVariables(configEditor.lastConfig!!.scriptSettings)
        }
    }

    private fun resetScriptKVPairs() {
        configEditor.setScriptVariables(guiProps.defaultScriptKVSetting)
    }

    /**
     * Validate the input field for client mods.
     *
     * @author Griefed
     */
    fun validateExclusions(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (!configEditor.getClientSideMods().matches(guiProps.whitespace)) {
            clientModsIcon.info()
        } else {
            errors.add(Translations.configuration_log_error_formatting.toString())
            clientModsIcon.error("<html>${errors.joinToString("<br>")}</html>")
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * Validate the input field for whitelisted mods.
     *
     * @author Griefed
     */
    fun validateWhitelist(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (!configEditor.getWhitelist().matches(guiProps.whitespace)) {
            whitelistModsIcon.info()
        } else {
            errors.add(Translations.configuration_log_error_formatting.toString())
            whitelistModsIcon.error("<html>${errors.joinToString("<br>")}</html>")
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }
}