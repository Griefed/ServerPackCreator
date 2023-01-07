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
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.InvalidFileTypeException
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.themes.DarkTheme
import de.griefed.serverpackcreator.gui.themes.LightTheme
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.gui.utilities.ImageUtilities
import de.griefed.serverpackcreator.gui.utilities.getScaledInstance
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import de.griefed.versionchecker.Update
import mdlaf.MaterialLookAndFeel
import mdlaf.components.textpane.MaterialTextPaneUI
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.WindowEvent
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.text.*

/**
 * This class creates our menubar which will be displayed at the top of the ServerPackCreator frame.
 * It contains various menus and menuitems to execute, change, open and edit various different
 * aspects of ServerPackCreator.
 *
 * @author Griefed
 */
@Suppress("unused")
class MainMenuBar(
    private val lightTheme: LightTheme,
    private val darkTheme: DarkTheme,
    private val serverPackCreatorWindow: ServerPackCreatorWindow,
    private val materialLookAndFeelLight: MaterialLookAndFeel,
    private val materialLookAndFeelDark: MaterialLookAndFeel,
    private val tabCreateServerPack: TabCreateServerPack,
    private val jTabbedPane: JTabbedPane,
    private val apiProperties: ApiProperties,
    private val updateChecker: UpdateChecker,
    private val utilities: Utilities,
    private val migrationManager: MigrationManager
) : Component() {
    private val log = cachedLoggerOf(this.javaClass)
    private val resourcePrefix = "/de/griefed/resources/gui"
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private val windowEvent: WindowEvent = WindowEvent(serverPackCreatorWindow, WindowEvent.WINDOW_CLOSING)
    private val chooserDimension: Dimension = Dimension(750, 450)
    private val aboutDimension: Dimension = Dimension(925, 520)
    private val helpIcon: ImageIcon =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/help.png")
    private val hasteBinIcon: ImageIcon =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/hastebin.png")
    private val infoIcon: ImageIcon =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/info.png").getScaledInstance(48, 48)
    private val jMenuBar: JMenuBar = JMenuBar()
    private val hasteBinOptions = arrayOfNulls<String>(3)
    private val aboutDocument: StyledDocument = DefaultStyledDocument()
    private val configDocument: StyledDocument = DefaultStyledDocument()
    private val spcLogDocument: StyledDocument = DefaultStyledDocument()
    private val configAttributeSet: SimpleAttributeSet = SimpleAttributeSet()
    private val spcLogAttributeSet: SimpleAttributeSet = SimpleAttributeSet()
    private val aboutWindowTextPane: JTextPane = JTextPane(aboutDocument)
    private val configWindowTextPane: JTextPane = JTextPane(configDocument)
    private val spcLogWindowTextPane: JTextPane = JTextPane(spcLogDocument)
    private val fileTooLargeWindowTextPane: JTextPane = JTextPane()
    private val materialTextPaneUI: MaterialTextPaneUI = MaterialTextPaneUI()
    private var configChooser: JFileChooser? = null
    private var lastLoadedConfigurationFile: File? = null

    init {
        val aboutWindowText: String = Gui.createserverpack_gui_about_text.toString()
        aboutWindowTextPane.isEditable = false
        aboutWindowTextPane.isOpaque = false
        aboutWindowTextPane.minimumSize = aboutDimension
        aboutWindowTextPane.preferredSize = aboutDimension
        aboutWindowTextPane.maximumSize = aboutDimension
        val aboutAttributeSet = SimpleAttributeSet()
        StyleConstants.setBold(aboutAttributeSet, true)
        StyleConstants.setFontSize(aboutAttributeSet, 14)
        aboutWindowTextPane.setCharacterAttributes(aboutAttributeSet, true)
        StyleConstants.setAlignment(aboutAttributeSet, StyleConstants.ALIGN_CENTER)
        aboutDocument.setParagraphAttributes(0, aboutDocument.length, aboutAttributeSet, false)
        try {
            aboutDocument.insertString(0, aboutWindowText, aboutAttributeSet)
        } catch (ex: BadLocationException) {
            log.error("Error inserting text into aboutDocument.", ex)
        }
        hasteBinOptions[0] = Gui.createserverpack_gui_about_hastebin_dialog_yes.toString()
        hasteBinOptions[1] = Gui.createserverpack_gui_about_hastebin_dialog_clipboard.toString()
        hasteBinOptions[2] = Gui.createserverpack_gui_about_hastebin_dialog_no.toString()
        configWindowTextPane.isOpaque = false
        configWindowTextPane.isEditable = false
        StyleConstants.setBold(configAttributeSet, true)
        StyleConstants.setFontSize(configAttributeSet, 14)
        configWindowTextPane.setCharacterAttributes(configAttributeSet, true)
        StyleConstants.setAlignment(configAttributeSet, StyleConstants.ALIGN_LEFT)
        configDocument.setParagraphAttributes(
            0, configDocument.length, configAttributeSet, false
        )
        spcLogWindowTextPane.isOpaque = false
        spcLogWindowTextPane.isEditable = false
        StyleConstants.setBold(spcLogAttributeSet, true)
        StyleConstants.setFontSize(spcLogAttributeSet, 14)
        spcLogWindowTextPane.setCharacterAttributes(spcLogAttributeSet, true)
        StyleConstants.setAlignment(spcLogAttributeSet, StyleConstants.ALIGN_LEFT)
        spcLogDocument.setParagraphAttributes(
            0, spcLogDocument.length, spcLogAttributeSet, false
        )
    }

    /**
     * Create the menubar, add all menus, add all menuitems and add actionlisteners for our
     * menuitems.
     *
     * @return JMenuBar. Returns the menubar containing all elements we need to control various
     * aspects of our app.
     * @author Griefed
     */
    fun createMenuBar(): JMenuBar {

        // create menus
        val fileMenu = JMenu(Gui.menubar_gui_menu_file.toString())
        val editMenu = JMenu(Gui.menubar_gui_menu_edit.toString())
        val viewMenu = JMenu(Gui.menubar_gui_menu_view.toString())
        val aboutMenu = JMenu(Gui.menubar_gui_menu_about.toString())

        // create menu items
        val fileNewConfigurationMenuItem = JMenuItem(Gui.menubar_gui_menuitem_newconfig.toString())
        val fileLoadConfigMenuItem = JMenuItem(Gui.menubar_gui_menuitem_loadconfig.toString())
        val fileSaveConfigMenuItem = JMenuItem(Gui.menubar_gui_menuitem_saveconfig.toString())
        val fileSaveAsConfigMenuItem = JMenuItem(Gui.menubar_gui_menuitem_saveas.toString())
        val fileUploadConfigurationToHasteNin = JMenuItem(Gui.menubar_gui_menuitem_uploadconfig.toString())
        val fileUploadSPCLogToHasteNin = JMenuItem(Gui.menubar_gui_menuitem_uploadlog.toString())
        val fileUpdateFallbackModsList = JMenuItem(Gui.menubar_gui_menuitem_updatefallback.toString())
        val fileExitConfigMenuItem = JMenuItem(Gui.menubar_gui_menuitem_exit.toString())
        val editSwitchTheme = JMenuItem(Gui.menubar_gui_menuitem_theme.toString())
        val editOpenInEditorServerProperties = JMenuItem(Gui.menubar_gui_menuitem_serverproperties.toString())
        val editOpenInEditorServerIcon = JMenuItem(Gui.menubar_gui_menuitem_servericon.toString())
        val viewOpenPluginsDirectoryMenuItem = JMenuItem(Gui.menubar_gui_menuitem_pluginsdir.toString())
        val viewExamplePluginRepositoryMenuItem = JMenuItem(Gui.menubar_gui_menuitem_examplepluginrepo.toString())
        val viewOpenSPCDirectoryMenuItem = JMenuItem(Gui.menubar_gui_menuitem_spcdir.toString())
        val viewOpenServerPacksDirectoryMenuItem = JMenuItem(Gui.menubar_gui_menuitem_serverpacksdir.toString())
        val viewOpenServerFilesDirectoryMenuItem = JMenuItem(Gui.menubar_gui_menuitem_serverfilesdir.toString())
        val viewMigrationMessagesItem = JMenuItem(Gui.menubar_gui_migration.toString())
        val viewOpenSPCLog = JMenuItem(Gui.menubar_gui_menuitem_spclog.toString())
        val viewOpenModloaderInstallerLog = JMenuItem(Gui.menubar_gui_menuitem_modloaderlog.toString())
        val viewOpenPluginsLog = JMenuItem(Gui.menubar_gui_menuitem_pluginlog.toString())

        //JMenuItem about_OpenAboutWindowMenuItem =
        JMenuItem(Gui.menubar_gui_menuitem_about.toString())
        val aboutOpenGitHubPageMenuItem = JMenuItem(Gui.menubar_gui_menuitem_repository.toString())
        val aboutOpenGitHubIssuesPageMenuItem = JMenuItem(Gui.menubar_gui_menuitem_issues.toString())
        val aboutOpenReleasesPageMenuItem = JMenuItem(Gui.menubar_gui_menuitem_releases.toString())
        val aboutOpenDiscordLinkMenuItem = JMenuItem(Gui.menubar_gui_menuitem_discord.toString())
        val aboutOpenDonationsPageMenuItem = JMenuItem(Gui.menubar_gui_menuitem_donate.toString())
        val aboutOpenWikiHelpMenuItem = JMenuItem(Gui.menubar_gui_menuitem_wiki_help.toString())
        val aboutOpenWikiHowToMenuItem = JMenuItem(Gui.menubar_gui_menuitem_wiki_howto.toString())
        val aboutCheckForUpdates = JMenuItem(Gui.menubar_gui_menuitem_updates.toString())

        // create action listeners for items
        fileNewConfigurationMenuItem.addActionListener { newConfiguration() }
        fileLoadConfigMenuItem.addActionListener { loadConfigFromFileMenuItem() }
        fileSaveConfigMenuItem.addActionListener { saveConfigToFileMenuItem() }
        fileSaveAsConfigMenuItem.addActionListener { saveAsConfigToFileMenuItem() }
        fileUploadConfigurationToHasteNin.addActionListener { uploadConfigToHasteBinMenuItem() }
        fileUploadSPCLogToHasteNin.addActionListener { uploadSPCLogToHasteBinMenuItem() }
        fileUpdateFallbackModsList.addActionListener { updateFallbackModslist() }
        fileExitConfigMenuItem.addActionListener { exitMenuItem() }
        editSwitchTheme.addActionListener { switchThemeMenuItem() }
        editOpenInEditorServerProperties.addActionListener { openInEditorServerProperties() }
        editOpenInEditorServerIcon.addActionListener { openServerIcon() }
        viewOpenSPCDirectoryMenuItem.addActionListener { openSPCDirectoryMenuItem() }
        viewOpenServerPacksDirectoryMenuItem.addActionListener { openServerPacksDirectoryMenuItem() }
        viewOpenServerFilesDirectoryMenuItem.addActionListener { openServerFilesDirectoryMenuItem() }
        viewOpenPluginsDirectoryMenuItem.addActionListener { openPluginsDirectoryMenuItem() }
        viewExamplePluginRepositoryMenuItem.addActionListener { viewExampleAddonMenuItem() }
        viewMigrationMessagesItem.addActionListener { viewMigrationMessagesItem() }
        viewOpenSPCLog.addActionListener { openSPClog() }
        viewOpenModloaderInstallerLog.addActionListener { openModloaderInstallerLog() }
        viewOpenPluginsLog.addActionListener { openPluginsLog() }
        aboutOpenGitHubPageMenuItem.addActionListener { openGitHubMenuItem() }
        aboutOpenGitHubIssuesPageMenuItem.addActionListener { openIssuesMenuItem() }
        aboutOpenReleasesPageMenuItem.addActionListener { openReleaseMenuItem() }
        aboutOpenDiscordLinkMenuItem.addActionListener { openDiscordLinkMenuItem() }
        aboutOpenDonationsPageMenuItem.addActionListener { openDonateMenuItem() }
        aboutOpenWikiHelpMenuItem.addActionListener { openWikiHelpMenuItem() }
        aboutOpenWikiHowToMenuItem.addActionListener { openWikiHowToMenuItem() }
        aboutCheckForUpdates.addActionListener { checkForUpdates() }

        // add items to menus
        fileMenu.add(fileNewConfigurationMenuItem)
        fileMenu.add(fileLoadConfigMenuItem)
        fileMenu.add(JSeparator())
        fileMenu.add(fileSaveConfigMenuItem)
        fileMenu.add(fileSaveAsConfigMenuItem)
        fileMenu.add(JSeparator())
        fileMenu.add(fileUploadConfigurationToHasteNin)
        fileMenu.add(fileUploadSPCLogToHasteNin)
        fileMenu.add(JSeparator())
        fileMenu.add(fileUpdateFallbackModsList)
        fileMenu.add(JSeparator())
        fileMenu.add(fileExitConfigMenuItem)
        editMenu.add(editOpenInEditorServerProperties)
        editMenu.add(editOpenInEditorServerIcon)
        editMenu.add(JSeparator())
        editMenu.add(editSwitchTheme)
        viewMenu.add(viewOpenSPCDirectoryMenuItem)
        viewMenu.add(viewOpenServerPacksDirectoryMenuItem)
        viewMenu.add(viewOpenServerFilesDirectoryMenuItem)
        viewMenu.add(viewOpenPluginsDirectoryMenuItem)
        viewMenu.add(JSeparator())
        viewMenu.add(viewExamplePluginRepositoryMenuItem)
        viewMenu.add(JSeparator())
        viewMenu.add(viewMigrationMessagesItem)
        viewMenu.add(JSeparator())
        viewMenu.add(viewOpenSPCLog)
        viewMenu.add(viewOpenModloaderInstallerLog)
        viewMenu.add(viewOpenPluginsLog)
        if (migrationManager.migrationMessages.isEmpty() && apiProperties.apiVersion != "dev") {
            viewMigrationMessagesItem.isEnabled = false
        }
        aboutMenu.add(aboutCheckForUpdates)
        aboutMenu.add(JSeparator())
        aboutMenu.add(aboutOpenWikiHelpMenuItem)
        aboutMenu.add(aboutOpenWikiHowToMenuItem)
        aboutMenu.add(JSeparator())
        aboutMenu.add(aboutOpenGitHubPageMenuItem)
        aboutMenu.add(aboutOpenGitHubIssuesPageMenuItem)
        aboutMenu.add(aboutOpenReleasesPageMenuItem)
        aboutMenu.add(JSeparator())
        aboutMenu.add(aboutOpenDiscordLinkMenuItem)
        aboutMenu.add(JSeparator())
        aboutMenu.add(aboutOpenDonationsPageMenuItem)

        // add menus
        jMenuBar.add(fileMenu)
        jMenuBar.add(editMenu)
        jMenuBar.add(viewMenu)
        jMenuBar.add(aboutMenu)
        return jMenuBar
    }

    /**
     * Upon button-press, load default values for textfields so the user can start with a new
     * configuration. Just as if ServerPackCreator was started without a serverpackcreator.conf being
     * present.
     *
     * @author Griefed
     */
    private fun newConfiguration() {
        log.debug("Clearing GUI...")
        tabCreateServerPack.clearInterface()
        lastLoadedConfigurationFile = null
    }

    /**
     * Upon button-press, open a file-selector to load a serverpackcreator.conf-file into
     * ServerPackCreator.
     *
     * @author Griefed
     */
    private fun loadConfigFromFileMenuItem() {
        log.debug("Clicked load configuration from file.")
        configChooser = JFileChooser()
        configChooser!!.currentDirectory = apiProperties.homeDirectory
        configChooser!!.dialogTitle = Gui.createserverpack_gui_buttonloadconfig_title.toString()
        configChooser!!.fileSelectionMode = JFileChooser.FILES_ONLY
        configChooser!!.fileFilter = FileNameExtensionFilter(
            Gui.createserverpack_gui_buttonloadconfig_filter.toString(), "conf"
        )
        configChooser!!.isAcceptAllFileFilterUsed = false
        configChooser!!.isMultiSelectionEnabled = false
        configChooser!!.preferredSize = chooserDimension
        if (configChooser!!.showOpenDialog(serverPackCreatorWindow) == JFileChooser.APPROVE_OPTION) {
            try {
                /* This log is meant to be read by the user, therefore we allow translation. */
                log.info("Loading from configuration file: ${configChooser!!.selectedFile.path}")
                val specifiedConfigFile: File = try {
                    File(utilities.fileUtilities.resolveLink(configChooser!!.selectedFile))
                } catch (ex: InvalidFileTypeException) {
                    log.error("Could not resolve link/symlink. Using entry from user input for checks.", ex)
                    File(configChooser!!.selectedFile.path)
                }
                tabCreateServerPack.loadConfig(specifiedConfigFile)
                lastLoadedConfigurationFile = specifiedConfigFile
            } catch (ex: IOException) {
                log.error("Error loading configuration from selected file.", ex)
            }
            log.debug("Configuration successfully loaded.")
        }
    }

    /**
     * Upon button-press, save the current configuration in the GUI to the serverpackcreator.conf-file
     * in ServerPackCreators base directory. if
     * `de.griefed.serverpackcreator.configuration.saveloadedconfig` is set to `true` and
     * the field `lastLoadedConfigurationFile` is not null, the last loaded configuration-file
     * is also saved to.
     *
     * @author Griefed
     */
    private fun saveConfigToFileMenuItem() {
        log.debug("Clicked Save.")
        log.debug("Saving serverpackcreator.conf")
        tabCreateServerPack.saveConfig(apiProperties.defaultConfig)
        if (lastLoadedConfigurationFile != null && apiProperties.isSavingOfLastLoadedConfEnabled) {
            log.debug("Saving ${lastLoadedConfigurationFile!!.name}")
            tabCreateServerPack.saveConfig(lastLoadedConfigurationFile!!)
        }
    }

    /**
     * Upon button-press, open a Filechooser dialog which allows the user to specify a file in which
     * the current configuration in the GUI will be saved to.
     *
     * @author Griefed
     */
    private fun saveAsConfigToFileMenuItem() {
        log.debug("Clicked Save As...")
        configChooser = JFileChooser()
        configChooser!!.currentDirectory = apiProperties.homeDirectory
        configChooser!!.dialogTitle = "Store current configuration"
        configChooser!!.fileSelectionMode = JFileChooser.FILES_ONLY
        configChooser!!.fileFilter = FileNameExtensionFilter(
            Gui.createserverpack_gui_buttonloadconfig_filter.toString(), "conf"
        )
        configChooser!!.isAcceptAllFileFilterUsed = false
        configChooser!!.isMultiSelectionEnabled = false
        configChooser!!.preferredSize = chooserDimension
        if (configChooser!!.showOpenDialog(serverPackCreatorWindow) == JFileChooser.APPROVE_OPTION) {
            if (configChooser!!.selectedFile.path.endsWith(".conf")) {
                tabCreateServerPack.saveConfig(
                    File(configChooser!!.selectedFile.path)
                )
                log.debug("Saved configuration to: ${configChooser!!.selectedFile.path}")
            } else {
                tabCreateServerPack.saveConfig(
                    File("${configChooser!!.selectedFile.path}.conf")
                )
                log.debug("Saved configuration to: ${configChooser!!.selectedFile.path}.conf")
            }
        }
    }

    /**
     * Upon button-press, uploads the serverpackcreator.conf-file to HasteBin and display a dialog
     * asking the user whether they want to open the URL in their default browser or copy the link to
     * their clipboard.
     *
     * @author Griefed
     */
    private fun uploadConfigToHasteBinMenuItem() {
        log.debug("Clicked Upload Configuration to HasteBin.")
        if (utilities.webUtilities.hasteBinPreChecks(
                File(apiProperties.homeDirectory, "serverpackcreator.conf")
            )
        ) {
            val urltoHasteBin: String = utilities.webUtilities.createHasteBinFromFile(
                File(apiProperties.homeDirectory, "serverpackcreator.conf")
            )
            val textContent = "URL: %s".format(urltoHasteBin)
            try {
                configDocument.insertString(0, textContent, configAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            displayUploadUrl(urltoHasteBin, configWindowTextPane)
        } else {
            fileTooLargeDialog()
        }
    }

    /**
     * Upon button-press, uploads the serverpackcreator.log-file to HasteBin and display a dialog
     * asking the user whether they want to open the URL in their default browser or copy the link to
     * their clipboard. If the filesize exceeds 10 MB, a warning is displayed, telling the user about
     * filesize limitations of HasteBin.
     *
     * @author Griefed
     */
    private fun uploadSPCLogToHasteBinMenuItem() {
        log.debug("Clicked Upload ServerPackCreator Log to HasteBin.")
        if (utilities.webUtilities.hasteBinPreChecks(
                File(apiProperties.logsDirectory, "serverpackcreator.log")
            )
        ) {
            val urltoHasteBin: String = utilities.webUtilities.createHasteBinFromFile(
                File(apiProperties.logsDirectory, "serverpackcreator.log")
            )
            val textContent = "URL: %s".format(urltoHasteBin)
            try {
                spcLogDocument.insertString(0, textContent, spcLogAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            displayUploadUrl(urltoHasteBin, spcLogWindowTextPane)
        } else {
            fileTooLargeDialog()
        }
    }

    /**
     * Update the fallback clientside-only mods-list from the repositories.
     *
     * @author Grefed
     */
    private fun updateFallbackModslist() {
        log.debug("Running update check for fallback modslist...")
        if (apiProperties.updateFallback()) {
            JOptionPane.showMessageDialog(
                serverPackCreatorWindow,
                Gui.menubar_gui_menuitem_updatefallback_updated.toString(),
                Gui.menubar_gui_menuitem_updatefallback_title.toString(),
                JOptionPane.INFORMATION_MESSAGE,
                infoIcon
            )
        } else {
            JOptionPane.showMessageDialog(
                serverPackCreatorWindow,
                Gui.menubar_gui_menuitem_updatefallback_nochange.toString(),
                Gui.menubar_gui_menuitem_updatefallback_title.toString(),
                JOptionPane.INFORMATION_MESSAGE,
                infoIcon
            )
        }
    }

    /**
     * Upon button-press, close ServerPackCreator gracefully.
     *
     * @author Griefed
     */
    private fun exitMenuItem() {
        log.debug("Clicked Exit.")
        serverPackCreatorWindow.dispatchEvent(windowEvent)
    }

    /**
     * Upon button-press, change the current theme to either light or dark-mode, depending on which
     * theme is currently active.
     *
     * @author Griefed
     */
    private fun switchThemeMenuItem() {
        log.debug("Clicked Toggle light/dark-mode.")
        if (!apiProperties.isDarkTheme()) {
            try {
                UIManager.setLookAndFeel(materialLookAndFeelDark)
                MaterialLookAndFeel.changeTheme(darkTheme)
                apiProperties.setTheme(true)
                apiProperties.saveToDisk(apiProperties.serverPackCreatorPropertiesFile)
            } catch (ex: UnsupportedLookAndFeelException) {
                log.error("Couldn't change theme.", ex)
            }
        } else {
            try {
                UIManager.setLookAndFeel(materialLookAndFeelLight)
                MaterialLookAndFeel.changeTheme(lightTheme)
                apiProperties.setTheme(false)
                apiProperties.saveToDisk(apiProperties.serverPackCreatorPropertiesFile)
            } catch (ex: UnsupportedLookAndFeelException) {
                log.error("Couldn't change theme.", ex)
            }
        }
        SwingUtilities.updateComponentTreeUI(serverPackCreatorWindow)
        jTabbedPane.isOpaque = true
        tabCreateServerPack.validateInputFields()
        tabCreateServerPack.updatePanelTheme()
    }

    /**
     * Upon button-press, open the server.properties-file, in the server-files directory, in the users
     * default text-editor.
     *
     * @author Griefed
     */
    private fun openInEditorServerProperties() {
        log.debug("Clicked Open server.properties in Editor.")
        if (File(tabCreateServerPack.getServerPropertiesPath()).isFile) {
            utilities.fileUtilities.openFile(tabCreateServerPack.getServerPropertiesPath())
        } else {
            utilities.fileUtilities
                .openFile(
                    File(
                        apiProperties.serverFilesDirectory, "server.properties"
                    )
                )
        }
    }

    /**
     * Upon button-press, open the server-icon.png-file, in the server-files directory, in the users
     * default picture-viewer.
     *
     * @author Griefed
     */
    private fun openServerIcon() {
        log.debug("Clicked Open server-icon.png in Editor.")
        if (File(tabCreateServerPack.getServerIconPath()).isFile) {
            utilities.fileUtilities.openFile(tabCreateServerPack.getServerIconPath())
        } else {
            utilities.fileUtilities
                .openFile(
                    File(
                        apiProperties.serverFilesDirectory, "server-icon.png"
                    )
                )
        }
    }

    /**
     * Upon button-press, open the base directory of ServerPackCreator in the users file-explorer.
     *
     * @author Griefed
     */
    private fun openSPCDirectoryMenuItem() {
        log.debug("Clicked open installation directory.")
        utilities.fileUtilities.openFolder(apiProperties.homeDirectory)
    }

    /**
     * Upon button-press, open the folder containing generated server packs in the users
     * file-explorer.
     *
     * @author Griefed
     */
    private fun openServerPacksDirectoryMenuItem() {
        log.debug("Clicked open server packs directory.")
        utilities.fileUtilities.openFolder(apiProperties.serverPacksDirectory)
    }

    /**
     * Upon button-press, open the folder containing the server-icon.png and server.properties files
     * in the users file-explorer.
     *
     * @author Griefed
     */
    private fun openServerFilesDirectoryMenuItem() {
        log.debug("Clicked open server files directory.")
        utilities.fileUtilities.openFolder(apiProperties.serverFilesDirectory)
    }

    /**
     * Upon button-press, open the folder containing installed plugins for ServerPackCreator in the
     * users file-explorer.
     *
     * @author Griefed
     */
    private fun openPluginsDirectoryMenuItem() {
        log.debug("Clicked open plugins directory.")
        utilities.fileUtilities.openFolder(apiProperties.pluginsDirectory)
    }

    /**
     * Upon button-press, open the example plugins repository-page on GitHub in the users default
     * browser.
     *
     * @author Griefed
     */
    private fun viewExampleAddonMenuItem() {
        log.debug("Clicked view example addon")
        utilities.webUtilities
            .openLinkInBrowser(
                URI.create("https://github.com/Griefed/ServerPackCreatorExampleAddon")
            )
    }

    /**
     * View the most recent migration messages, if any are available.
     *
     * @author Griefed
     */
    private fun viewMigrationMessagesItem() {
        serverPackCreatorWindow.displayMigrationMessages()
    }

    /**
     * Open the serverpackcreator.log in the users default editor.
     *
     * @author Griefed
     */
    private fun openSPClog() {
        log.debug("Clicked open ServerPackCreator-log.")
        utilities.fileUtilities
            .openFile(File(apiProperties.logsDirectory, "serverpackcreator.log"))
    }

    /**
     * Open the modloader_installer.log in the users default editor.
     *
     * @author Griefed
     */
    private fun openModloaderInstallerLog() {
        log.debug("Clicked open Modloader-Installer-log.")
        utilities.fileUtilities
            .openFile(File(apiProperties.logsDirectory, "modloader_installer.log"))
    }

    /**
     * Open the plugins.log in the users default editor.
     *
     * @author Griefed
     */
    private fun openPluginsLog() {
        log.debug("Clicked open plugins-log.")
        utilities.fileUtilities.openFile(File(apiProperties.logsDirectory, "plugins.log"))
    }

    /**
     * Upon button-press, open the ServerPackCreator repository GitHub page in the users
     * default-browser.
     *
     * @author Griefed
     */
    private fun openGitHubMenuItem() {
        log.debug("Clicked open GitHub repository link.")
        utilities.webUtilities
            .openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator"))
    }

    /**
     * Upon button-press, open ServerPackCreators issue-page on GitHub in the users default browser.
     *
     * @author Griefed
     */
    private fun openIssuesMenuItem() {
        log.debug("Clicked Open Issues page on GitHub.")
        utilities.webUtilities
            .openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator/issues"))
    }

    /**
     * Upon button-press, open the GitHub releases page in the users default-browser.
     *
     * @author Griefed
     */
    private fun openReleaseMenuItem() {
        log.debug("Clicked open releases link")
        utilities.webUtilities
            .openLinkInBrowser(
                URI.create("https://github.com/Griefed/ServerPackCreator/releases")
            )
    }

    /**
     * Upon button-press, open the Discord invite-link to Griefed's Discord server in the users
     * default browser.
     *
     * @author Griefed
     */
    private fun openDiscordLinkMenuItem() {
        log.debug("Clicked Join Discord.")
        utilities.webUtilities.openLinkInBrowser(URI.create("https://discord.griefed.de"))
    }

    /**
     * Upon button-press, open the GitHub Sponsors page in the users default-browser.
     *
     * @author Griefed
     */
    private fun openDonateMenuItem() {
        log.debug("Clicked open donations link.")
        utilities.webUtilities.openLinkInBrowser(URI.create("https://github.com/sponsors/Griefed"))
    }

    /**
     * Open the Help-section of the wiki in a browser.
     *
     * @author Griefed
     */
    private fun openWikiHelpMenuItem() {
        log.debug("Clicked Help.")
        utilities.webUtilities
            .openLinkInBrowser(
                URI.create(
                    "https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help"
                )
            )
    }

    /**
     * Open the HowTo-section of the wiki in a browser.
     *
     * @author Griefed
     */
    private fun openWikiHowToMenuItem() {
        log.debug("Clicked Getting started.")
        utilities.webUtilities
            .openLinkInBrowser(
                URI.create(
                    "https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-HowTo"
                )
            )
    }

    /**
     * Check for update availability and display information about the available update, if any.
     *
     * @author Griefed
     */
    private fun checkForUpdates() {
        log.debug("Clicked Check for Updates")
        if (!displayUpdateDialog()) {
            DialogUtilities.createDialog(
                Gui.menubar_gui_menuitem_updates_none.toString() + "   ",
                Gui.menubar_gui_menuitem_updates_none_title.toString() + "   ",
                serverPackCreatorWindow,
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                infoIcon,
                resizable = true
            )
        }
    }

    /**
     * Display the given URL in a text pane.
     *
     * @param urltoHasteBin   The URL, as a String, to display.
     * @param displayTextPane The text pane to display the URL in.
     * @author Griefed
     */
    private fun displayUploadUrl(
        urltoHasteBin: String,
        displayTextPane: JTextPane
    ) {
        materialTextPaneUI.installUI(displayTextPane)
        when (DialogUtilities.createShowGet(
            displayTextPane,
            Gui.createserverpack_gui_about_hastebin_dialog.toString(),
            serverPackCreatorWindow,
            JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
            hasteBinIcon,
            resizable = true,
            hasteBinOptions,
            hasteBinOptions[0]
        )) {
            0 -> utilities.webUtilities.openLinkInBrowser(URI.create(urltoHasteBin))
            1 -> clipboard.setContents(StringSelection(urltoHasteBin), null)
            else -> {}
        }
    }

    /**
     * Opens a dialog informing the user that a file exceeds 10 MB in size.
     *
     * @author Griefed
     */
    private fun fileTooLargeDialog() {
        materialTextPaneUI.installUI(fileTooLargeWindowTextPane)
        JOptionPane.showConfirmDialog(
            serverPackCreatorWindow,
            Gui.menubar_gui_filetoolarge.toString(),
            Gui.menubar_gui_filetoolargetitle.toString(),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            hasteBinIcon
        )
    }

    /**
     * If an initialize is available for ServerPackCreator, display a dialog asking the user whether
     *
     * @return `true` if an update was found and the dialog displayed.
     * @author Griefed
     */
    fun displayUpdateDialog(): Boolean {
        val update: Optional<Update> = updateChecker.checkForUpdate(
            apiProperties.apiVersion,
            apiProperties.isCheckingForPreReleasesEnabled
        )
        return if (update.isPresent) {
            val textContent: String = Gui.update_dialog_new(update.get().url())
            val styledDocument: StyledDocument = DefaultStyledDocument()
            val simpleAttributeSet = SimpleAttributeSet()
            val materialTextPaneUI = MaterialTextPaneUI()
            val jTextPane = JTextPane(styledDocument)
            StyleConstants.setBold(simpleAttributeSet, true)
            StyleConstants.setFontSize(simpleAttributeSet, 14)
            jTextPane.setCharacterAttributes(simpleAttributeSet, true)
            StyleConstants.setAlignment(simpleAttributeSet, StyleConstants.ALIGN_LEFT)
            styledDocument.setParagraphAttributes(
                0, styledDocument.length, simpleAttributeSet, false
            )
            jTextPane.isOpaque = false
            jTextPane.isEditable = false
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val options = arrayOfNulls<String>(3)
            options[0] = Gui.update_dialog_yes.toString()
            options[1] = Gui.update_dialog_no.toString()
            options[2] = Gui.update_dialog_clipboard.toString()
            try {
                styledDocument.insertString(0, textContent, simpleAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            materialTextPaneUI.installUI(jTextPane)
            when (DialogUtilities.createShowGet(
                jTextPane,
                Gui.update_dialog_available.toString(),
                serverPackCreatorWindow,
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                infoIcon,
                resizable = true,
                options,
                options[0]
            )) {
                0 -> try {
                    utilities.webUtilities.openLinkInBrowser(update.get().url().toURI())
                } catch (ex: RuntimeException) {
                    log.error("Error opening browser.", ex)
                } catch (ex: URISyntaxException) {
                    log.error("Error opening browser.", ex)
                }

                1 -> clipboard.setContents(StringSelection(update.get().url().toString()), null)
                else -> {}
            }
            true
        } else {
            false
        }
    }

    /**
     * Upon button-press, open an About-window containing information about ServerPackCreator.
     *
     * @author Griefed
     */
    private fun openAboutSPCMenuItem() {
        log.debug("Clicked open about window.")
        val aboutWindowScrollPane = JScrollPane(
            aboutWindowTextPane,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        )
        aboutWindowScrollPane.minimumSize = aboutDimension
        aboutWindowScrollPane.preferredSize = aboutDimension
        aboutWindowScrollPane.maximumSize = aboutDimension
        DialogUtilities.createDialog(
            aboutWindowScrollPane,
            Gui.createserverpack_gui_createserverpack_about_title.toString(),
            serverPackCreatorWindow,
            JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
            helpIcon
        )
    }
}