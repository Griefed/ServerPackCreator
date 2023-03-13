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
package de.griefed.serverpackcreator.gui.window

import Gui
import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.splash.SplashScreen
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditorPanel
import de.griefed.serverpackcreator.gui.window.menu.MainMenu
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import javax.swing.text.*
import kotlin.system.exitProcess

/**
 * TODO docs
 */
class MainFrame(
    private val guiProps: GuiProps,
    configurationHandler: ConfigurationHandler,
    serverPackHandler: ServerPackHandler,
    private val apiProperties: ApiProperties,
    versionMeta: VersionMeta,
    utilities: Utilities,
    updateChecker: UpdateChecker,
    splashScreen: SplashScreen,
    apiPlugins: ApiPlugins,
    private val migrationManager: MigrationManager
) {
    private val log = cachedLoggerOf(this.javaClass)
    val frame: JFrame = JFrame(Gui.createserverpack_gui_createandshowgui.toString())
    private val larsonScanner = LarsonScanner()
    private val mainPanel = MainPanel(
        guiProps,
        configurationHandler,
        serverPackHandler,
        apiProperties,
        versionMeta,
        utilities,
        updateChecker,
        apiPlugins,
        migrationManager,
        larsonScanner
    )
    private val updateDialogs = UpdateDialogs(guiProps, utilities.webUtilities, apiProperties, updateChecker, frame)
    private val mainMenu = MainMenu(
        guiProps,
        apiProperties,
        utilities,
        updateDialogs,
        larsonScanner,
        mainPanel.configsTab,
        this
    )

    init {
        frame.iconImage = guiProps.appIcon
        frame.jMenuBar = mainMenu.menuBar
        frame.contentPane.add(mainPanel.scroll)
        frame.pack()
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.isLocationByPlatform = true
        frame.setSize(1200, 800)
        frame.isVisible = true
        frame.isAutoRequestFocus = true
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                closeAndExit()
            }
        })
        splashScreen.close()
    }

    fun closeAndExit() {
        if (mainPanel.configsTab.tabs.tabCount == 0) {
            exitProcess(0)
        }
        val configs = mutableListOf<String>()
        for (tab in mainPanel.configsTab.allTabs) {
            val config = tab as ConfigEditorPanel
            val modpackName = File(config.getModpackDirectory()).name
            if (config.title.title != Gui.createserverpack_gui_title_new.toString() && config.hasUnsavedChanges()) {
                mainPanel.configsTab.tabs.selectedComponent = tab
                if (DialogUtilities.createShowGet(
                        Gui.createserverpack_gui_close_unsaved_message(modpackName),
                        Gui.createserverpack_gui_close_unsaved_title(modpackName),
                        frame,
                        JOptionPane.WARNING_MESSAGE,
                        JOptionPane.YES_NO_OPTION,
                        guiProps.warningIcon
                    ) == 0
                ) {
                    configs.add(config.saveCurrentConfiguration().absolutePath)
                }
            }
        }
        apiProperties.storeCustomProperty("lastloaded", configs.joinToString(","))
        apiProperties.saveToDisk(apiProperties.serverPackCreatorPropertiesFile)
        exitProcess(0)
    }

    /**
     * Display the available migration messages.
     *
     * @author Griefed
     */
    fun displayMigrationMessages() {
        val styledDocument: StyledDocument = DefaultStyledDocument()
        val simpleAttributeSet = SimpleAttributeSet()
        StyleConstants.setBold(simpleAttributeSet, true)
        StyleConstants.setFontSize(simpleAttributeSet, 14)
        StyleConstants.setAlignment(simpleAttributeSet, StyleConstants.ALIGN_LEFT)
        styledDocument.setParagraphAttributes(
            0, styledDocument.length, simpleAttributeSet, false
        )
        val jTextPane = JTextPane(styledDocument)
        jTextPane.setCharacterAttributes(simpleAttributeSet, true)

        val messages = StringBuilder()
        if (apiProperties.apiVersion != "dev") {
            if (migrationManager.migrationMessages.isEmpty()) {
                messages.append("No migrations occurred. :)")
            } else {
                for (message in migrationManager.migrationMessages) {
                    messages.append(message.get()).append("\n")
                }
            }
        } else {
            messages.append("No migrations will occur in a dev-version of SPC.")
        }

        try {
            styledDocument.insertString(0, messages.toString(), simpleAttributeSet)
        } catch (ex: BadLocationException) {
            log.error("Error inserting text into aboutDocument.", ex)
        }

        val scrollPane = JScrollPane(
            jTextPane,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        scrollPane.preferredSize = Dimension(800, 400)
        jTextPane.isEditable = false

        DialogUtilities.createDialog(
            scrollPane,
            Gui.migration_message_title.toString(),
            frame,
            JOptionPane.INFORMATION_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            guiProps.infoIcon,
            true
        )
    }
}