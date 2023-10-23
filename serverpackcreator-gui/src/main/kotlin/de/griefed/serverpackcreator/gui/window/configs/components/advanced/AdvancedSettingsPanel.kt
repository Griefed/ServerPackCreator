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
package de.griefed.serverpackcreator.gui.window.configs.components.advanced

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.gui.components.ElementLabel
import de.griefed.serverpackcreator.gui.components.ScrollTextArea
import de.griefed.serverpackcreator.gui.components.StatusIcon
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditor
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
    private val log = cachedLoggerOf(this.javaClass)
    private val clientModsIcon = StatusIcon(guiProps,Gui.createserverpack_gui_createserverpack_labelclientmods_tip.toString())
    private val clientModsLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labelclientmods.toString())
    private val clientModsRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.createserverpack_gui_buttonclientmods_revert_tip.toString(), guiProps) { revertExclusions() }
    private val clientModsChooser = BalloonTipButton(null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(), guiProps) { selectClientMods() }
    private val clientModsReset = BalloonTipButton(null, guiProps.resetIcon, Gui.createserverpack_gui_buttonclientmods_reset_tip.toString(), guiProps) { resetClientMods() }

    private val javaArgsIcon = StatusIcon(guiProps,Gui.createserverpack_gui_createserverpack_javaargs_tip.toString())
    private val javaArgsLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_javaargs.toString())
    private val javaArgsAikarsFlagsButton = AikarsFlagsButton(configEditor, guiProps)

    private val advScriptSettingsIcon = StatusIcon(guiProps,Gui.createserverpack_gui_createserverpack_scriptsettings_label_tooltip.toString())
    private val advScriptSettingsLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_scriptsettings_label.toString())
    private val advScriptSettingsRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.createserverpack_gui_revert.toString(), guiProps) { revertScriptKVPairs() }
    private val advScriptSettingsReset = BalloonTipButton(null, guiProps.resetIcon, Gui.createserverpack_gui_reset.toString(), guiProps) { resetScriptKVPairs() }

    init {
        // Mod Exclusions
        add(clientModsIcon, "cell 0 0 1 3")
        add(clientModsLabel, "cell 1 0 1 3")
        add(exclusions, "cell 2 0 1 3,grow,w 10:500:,h 150!")
        add(clientModsRevert, "cell 3 0 2 1, h 30!, aligny center, alignx center,growx")
        add(clientModsChooser, "cell 3 1 2 1, h 30!, aligny center, alignx center,growx")
        add(clientModsReset, "cell 3 2 2 1, h 30!, aligny top, alignx center,growx")

        // Java Arguments
        add(javaArgsIcon, "cell 0 3 1 3")
        add(javaArgsLabel, "cell 1 3 1 3")
        add(javaArgs, "cell 2 3 1 3,grow,w 10:500:,h 100!")
        add(javaArgsAikarsFlagsButton, "cell 3 3 2 3,growy")

        // Script Key-Value Pairs
        add(advScriptSettingsIcon, "cell 0 6 1 3")
        add(advScriptSettingsLabel, "cell 1 6 1 3")
        add(scriptKVPairs.scrollPanel, "cell 2 6 1 3,grow,w 10:500:,h 200!")
        add(advScriptSettingsRevert, "cell 3 6 2 1, h 30!, aligny center, alignx center,growx")
        add(advScriptSettingsReset, "cell 3 7 2 1, h 30!, aligny top, alignx center,growx")
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
     * @author Griefed
     */
    private fun resetClientMods() {
        val current = configEditor.getClientSideModsList()
        val default = apiProperties.clientSideMods()
        if (!default.any { mod -> !default.contains(mod) }) {
            when (JOptionPane.showConfirmDialog(
                this,
                Gui.createserverpack_gui_buttonclientmods_reset_merge_message.toString(),
                Gui.createserverpack_gui_buttonclientmods_reset_merge_title.toString(),
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

                1 -> configEditor.setClientSideMods(default)
            }
        } else {
            configEditor.setClientSideMods(default)
        }
    }

    /**
     * @author Griefed
     */
    private fun revertScriptKVPairs() {
        if (configEditor.lastConfig != null) {
            configEditor.setScriptVariables(configEditor.lastConfig!!.scriptSettings)
        }
    }

    /**
     * @author Griefed
     */
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
            errors.add(Gui.configuration_log_error_formatting.toString())
            clientModsIcon.error("<html>${errors.joinToString("<br>")}</html>")
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }
}