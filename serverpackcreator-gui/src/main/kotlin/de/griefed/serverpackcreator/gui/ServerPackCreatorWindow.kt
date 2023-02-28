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
package de.griefed.serverpackcreator.gui

import Gui
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.themes.DarkTheme
import de.griefed.serverpackcreator.gui.themes.LightTheme
import de.griefed.serverpackcreator.gui.utilities.BackgroundPanel
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.gui.utilities.ImageUtilities
import de.griefed.serverpackcreator.gui.utilities.getScaledInstance
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import mdlaf.MaterialLookAndFeel
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.text.*

/**
 * Creates and shows the GUI needed for running ServerPackCreator in....well...GUI mode.
 *
 * @author Griefed
 */
class ServerPackCreatorWindow(
    configurationHandler: ConfigurationHandler,
    serverPackHandler: ServerPackHandler,
    private val apiProperties: ApiProperties,
    versionMeta: VersionMeta,
    utilities: Utilities,
    updateChecker: UpdateChecker,
    private val serverPackCreatorSplash: ServerPackCreatorSplash,
    apiPlugins: ApiPlugins,
    private val migrationManager: MigrationManager
) : JFrame() {
    private val log = cachedLoggerOf(this.javaClass)
    private val resourcePrefix = "/de/griefed/resources/gui"
    private val serverPackCreatorBanner =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/banner.png")
    private val serverPackCreatorIcon =
        ImageUtilities.imageFromResourceStream(this.javaClass, "$resourcePrefix/app.png")
    private val issueIcon: ImageIcon =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/issue.png")
            .getScaledInstance(48, 48)
    private val infoIcon: ImageIcon =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/info.png").getScaledInstance(48, 48)
    private val windowDimension: Dimension = Dimension(1200, 800)
    private val migrationDimension: Dimension = Dimension(800, 400)
    private val lightTheme: LightTheme = LightTheme()
    private val darkTheme: DarkTheme = DarkTheme()
    private val materialLookAndFeelLight: MaterialLookAndFeel = MaterialLookAndFeel(lightTheme)
    private val materialLookAndFeelDark: MaterialLookAndFeel = MaterialLookAndFeel(darkTheme)
    private val backgroundPanel: BackgroundPanel
    private val tabCreateServerPack: TabCreateServerPack
    private val tabbedPane: JTabbedPane
    private val menuBar: MainMenuBar

    init {
        serverPackCreatorSplash.update(90)
        val bufferedImage = ImageUtilities.imageFromResourceStream(
            this.javaClass,
            "$resourcePrefix/tile${Random().nextInt(4)}.jpg"
        )
        title = "${Gui.createserverpack_gui_createandshowgui} - ${apiProperties.apiVersion}"
        tabCreateServerPack = TabCreateServerPack(
            configurationHandler,
            serverPackHandler,
            versionMeta,
            apiProperties,
            this,
            utilities,
            darkTheme,
            lightTheme,
            apiPlugins
        )
        val spcLogTab = TabServerPackCreatorLog(
            Gui.createserverpack_gui_tabbedpane_serverpackcreatorlog_tooltip.toString(),
            apiProperties.logsDirectory
        )
        val pluginsLogTab = TabPluginsLog(
            Gui.createserverpack_gui_tabbedpane_pluginshandlerlog_tip.toString(),
            apiProperties.logsDirectory
        )
        backgroundPanel = BackgroundPanel(bufferedImage, BackgroundPanel.TILED, 0.0f, 0.0f)
        tabbedPane = JTabbedPane(JTabbedPane.TOP)
        tabbedPane.addTab(
            Gui.createserverpack_gui_tabbedpane_createserverpack_title.toString(),
            null,
            tabCreateServerPack,
            Gui.createserverpack_gui_tabbedpane_createserverpack_tip.toString()
        )
        tabbedPane.addTab(
            Gui.createserverpack_gui_tabbedpane_serverpackcreatorlog_title.toString(),
            null,
            spcLogTab,
            Gui.createserverpack_gui_tabbedpane_serverpackcreatorlog_tip.toString()
        )
        tabbedPane.addTab(
            Gui.createserverpack_gui_tabbedpane_pluginshandlerlog_title.toString(),
            null,
            pluginsLogTab
        )
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1)
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2)
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3)
        apiPlugins.addTabExtensionTabs(tabbedPane)
        tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
        menuBar = MainMenuBar(
            lightTheme,
            darkTheme,
            this,
            materialLookAndFeelLight,
            materialLookAndFeelDark,
            tabCreateServerPack,
            tabbedPane,
            apiProperties,
            updateChecker,
            utilities,
            migrationManager
        )
        jMenuBar = menuBar.createMenuBar()
    }

    /**
     * Shows the GUI from the EDT by using SwingUtilities, and it's invokeLater method by calling
     * [.createAndShowGUI]. Sets the font to bold, which may be overridden by the LookAndFeel
     * which gets automatically determined and depends on the OS ServerPackCreator is run on.
     *
     * @author Griefed
     */
    fun createAndShowMainGui() {
        SwingUtilities.invokeLater {
            try {
                if (apiProperties.isDarkTheme()) {
                    UIManager.setLookAndFeel(materialLookAndFeelDark)
                    MaterialLookAndFeel.changeTheme(darkTheme)
                } else {
                    UIManager.setLookAndFeel(materialLookAndFeelLight)
                    MaterialLookAndFeel.changeTheme(lightTheme)
                }
                UIManager.put("Table.showVerticalLines", true)
                UIManager.put("Table.showHorizontalLines", true)
                UIManager.put("Table.intercellSpacing", Dimension(1, 1))
            } catch (ex: UnsupportedLookAndFeelException) {
                log.error("Error: There was an error setting the look and feel.", ex)
            }
            serverPackCreatorSplash.update(95)
            serverPackCreatorSplash.close()
            val serverPackCreatorBanner = JLabel(serverPackCreatorBanner)
            serverPackCreatorBanner.isOpaque = false
            defaultCloseOperation = EXIT_ON_CLOSE
            contentPane = backgroundPanel
            iconImage = serverPackCreatorIcon
            add(serverPackCreatorBanner, BorderLayout.PAGE_START)
            add(tabbedPane, BorderLayout.CENTER)
            size = windowDimension
            preferredSize = windowDimension
            setLocationRelativeTo(null)
            isResizable = true
            pack()
            /*
            * I know this looks stupid. Why initialize the tree if it isn't even visible yet?
            * Because otherwise, when switching from light to dark-theme, the inset for tabs of the tabbed pane suddenly
            * changes, which looks ugly. Calling this does the same, but before the GUI is visible. Dirty hack? Maybe.
            * Does it work? Yeah.
            */
            SwingUtilities.updateComponentTreeUI(this)

            /*
            * This call needs to stay here, otherwise we have a transparent background in the tab-bar of
            * our tabbed pane, which looks kinda cool, but also stupid and hard to read.
            */
            tabbedPane.isOpaque = true
            isVisible = true
            tabCreateServerPack.validateInputFields()
            tabCreateServerPack.updatePanelTheme()
            menuBar.displayUpdateDialog()
            displayMigrationMessages()
        }
    }

    /**
     * Display the available migration messages.
     *
     * @author Griefed
     */
    fun displayMigrationMessages() {
        if (migrationManager.migrationMessages.isNotEmpty() || apiProperties.apiVersion == "dev") {
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
                for (message in migrationManager.migrationMessages) {
                    messages.append(message.get()).append("\n")
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
            scrollPane.preferredSize = migrationDimension
            jTextPane.isEditable = false

            DialogUtilities.createDialog(
                scrollPane,
                Gui.migration_message_title.toString(),
                this,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                infoIcon,
                true
            )
        }
    }

    /**
     * If no Java is available, a message is displayed, warning the user that Javapath needs to be
     * defined for the modloader-server installation to work. If "Yes" is clicked, a filechooser will
     * open where the user can select their Java-executable/binary. If "No" is selected, the user is
     * warned about the consequences of not setting the Javapath.
     *
     * @return `true` if Java is available or was configured by the user.
     * @author Griefed
     */
    fun checkJava(): Boolean {
        return if (!apiProperties.javaAvailable()) {
            when (JOptionPane.showConfirmDialog(
                this,
                Gui.createserverpack_gui_createserverpack_checkboxserver_confirm_message.toString(),
                Gui.createserverpack_gui_createserverpack_checkboxserver_confirm_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                issueIcon
            )) {
                0 -> {
                    chooseJava()
                    true
                }

                1 -> {
                    JOptionPane.showMessageDialog(
                        this,
                        Gui.createserverpack_gui_createserverpack_checkboxserver_message_message.toString(),
                        Gui.createserverpack_gui_createserverpack_checkboxserver_message_title.toString(),
                        JOptionPane.ERROR_MESSAGE,
                        issueIcon
                    )
                    false
                }

                else -> false
            }
        } else {
            true
        }
    }

    /**
     * Opens a filechooser to select the Java-executable/binary.
     *
     * @author Griefed
     */
    private fun chooseJava() {
        val javaChooser = JFileChooser()
        if (File("%s/bin/".format(System.getProperty("java.home"))).isDirectory) {
            javaChooser.currentDirectory = File("%s/bin/".format(System.getProperty("java.home")))
        } else {
            javaChooser.currentDirectory = apiProperties.homeDirectory
        }
        javaChooser.dialogTitle = Gui.createserverpack_gui_buttonjavapath_tile.toString()
        javaChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        javaChooser.isAcceptAllFileFilterUsed = true
        javaChooser.isMultiSelectionEnabled = false
        javaChooser.preferredSize = Dimension(750, 450)
        if (javaChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            apiProperties.javaPath = javaChooser.selectedFile.path
            log.debug("Set path to Java executable to: ${javaChooser.selectedFile.path}")
        }
    }
}