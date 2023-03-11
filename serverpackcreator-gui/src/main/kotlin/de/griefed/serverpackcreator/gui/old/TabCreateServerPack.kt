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
package de.griefed.serverpackcreator.gui.old

import Gui
import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.*
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.api.utilities.ReticulatingSplines
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.old.themes.DarkTheme
import de.griefed.serverpackcreator.gui.old.themes.LightTheme
import de.griefed.serverpackcreator.gui.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mdlaf.components.textpane.MaterialTextPaneUI
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.text.*

/**
 * This class creates the tab which displays the labels, text-fields, buttons and functions in order
 * to create a new server pack. Available are:<br></br> JLabels and JTextFields for modpackDir,
 * clientMods, copyDirs, javaPath, minecraftVersion, modLoader, modLoaderVersion<br></br> Checkboxes for
 * Include- serverInstallation, serverIcon, serverProperties, startScripts, ZIP-archive<br></br> Buttons
 * opening the file explorer and choosing: modpackDir, clientMods, copyDirs, javaPath,
 * loadConfigFromFile<br></br> A button for displaying an information windows which provides detailed
 * explanation of the important parts of the GUI.<br></br> The button start the generation of a new
 * server pack.<br></br> The label under the button to start the generation of a new server pack, which
 * displays the latest log entry of the serverpackcreator.log *during* the creation of a new
 * server pack.
 * <br></br>
 * If a configuration file is found during startup of ServerPackCreator, it is automatically loaded
 * into the GUI.
 *
 * @author Griefed
 */
class TabCreateServerPack(
    private val configurationHandler: ConfigurationHandler,
    private val serverPackHandler: ServerPackHandler,
    private val versionMeta: VersionMeta,
    private val apiProperties: ApiProperties,
    private val serverPackCreatorWindow: ServerPackCreatorWindow,
    private val utilities: Utilities,
    private val darkTheme: DarkTheme,
    private val lightTheme: LightTheme,
    apiPlugins: ApiPlugins
) : JPanel(), ServerPackConfigTab {
    private val log = cachedLoggerOf(this.javaClass)
    private val reticulatingSplines: ReticulatingSplines = ReticulatingSplines()
    private val serverPackGeneratedDocument: StyledDocument = DefaultStyledDocument()
    private val serverPackGeneratedAttributeSet: SimpleAttributeSet = SimpleAttributeSet()
    private val serverPackGeneratedTextPane: JTextPane = JTextPane(serverPackGeneratedDocument)
    private val lazyModeDocument: StyledDocument = DefaultStyledDocument()
    private val lazyModeTextPane: JTextPane = JTextPane(lazyModeDocument)
    private val resourcePrefix = "/de/griefed/resources/gui"
    private val errorIconSize = 18
    private val errorIconBase = ImageUtilities.imageFromResourceStream(this.javaClass, "$resourcePrefix/error.png")
    private val errorIconModpackDirectory: ImageIcon = ImageIcon(
        errorIconBase.getScaledInstance(errorIconSize, errorIconSize, Image.SCALE_SMOOTH)
    )
    private val errorIconServerPackSuffix: ImageIcon = ImageIcon(
        errorIconBase.getScaledInstance(errorIconSize, errorIconSize, Image.SCALE_SMOOTH)
    )
    private val errorIconClientsideMods: ImageIcon = ImageIcon(
        errorIconBase.getScaledInstance(errorIconSize, errorIconSize, Image.SCALE_SMOOTH)
    )
    private val errorIconCopyDirectory: ImageIcon = ImageIcon(
        errorIconBase.getScaledInstance(errorIconSize, errorIconSize, Image.SCALE_SMOOTH)
    )
    private val errorIconServerIcon: ImageIcon = ImageIcon(
        errorIconBase.getScaledInstance(errorIconSize, errorIconSize, Image.SCALE_SMOOTH)
    )
    private val errorIconServerIconPreview: ImageIcon = ImageIcon(
        errorIconBase.getScaledInstance(48, 48, Image.SCALE_SMOOTH)
    )
    private val errorIconServerProperties: ImageIcon = ImageIcon(
        errorIconBase.getScaledInstance(errorIconSize, errorIconSize, Image.SCALE_SMOOTH)
    )
    private val issueIcon: ImageIcon =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/issue.png")
            .getScaledInstance(48, 48)
    private val infoIcon: ImageIcon =
        ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/info.png").getScaledInstance(48, 48)
    private val chooserDimension: Dimension = Dimension(750, 450)
    private val buttonGenerateServerPack: JButton = JButton()
    private val serverPackPanel: JPanel = JPanel()
    private val larsonScanner: LarsonScanner = LarsonScanner()
    private val coffee = Color(192, 255, 238)
    private val swampGreen = Color(50, 83, 88)
    private val darkGrey = Color(49, 47, 47)
    private val idleConfig: LarsonScanner.ScannerConfig = LarsonScanner.ScannerConfig(
        2,
        shortArrayOf(25, 50, 75, 100, 150, 200, 255),
        50.toShort(),
        50.toShort(),
        7.toByte(),
        floatArrayOf(0.4f, 1.0f),
        50.0f,
        5.0,
        false,
        false,
        true,
        false,
        false,
        arrayOf(swampGreen, swampGreen, swampGreen, swampGreen, swampGreen, swampGreen, swampGreen),
        darkGrey,
        darkGrey
    )
    private val generatingConfig: LarsonScanner.ScannerConfig = LarsonScanner.ScannerConfig(
        2,
        shortArrayOf(25, 50, 75, 100, 150, 200, 255),
        100.toShort(),
        25.toShort(),
        7.toByte(),
        floatArrayOf(0.4f, 1.0f),
        25.0f,
        5.0,
        false,
        false,
        true,
        true,
        false,
        arrayOf(coffee, coffee, coffee, coffee, coffee, coffee, coffee),
        darkGrey,
        darkGrey
    )
    private val materialTextPaneUI: MaterialTextPaneUI = MaterialTextPaneUI()
    private val minecraftVersions: JComboBox<String> = JComboBox<String>()
    private val modloaders: JComboBox<String> = JComboBox<String>()
    private val modloaderVersions: JComboBox<String> = JComboBox<String>()
    private val noVersions: DefaultComboBoxModel<String>
    private val legacyFabricVersions: DefaultComboBoxModel<String>
    private val fabricVersions: DefaultComboBoxModel<String>
    private val quiltVersions: DefaultComboBoxModel<String>
    private val clientsideMods: IconTextArea = IconTextArea("")
    private val javaArgs: IconTextArea = IconTextArea("")
    private val copyDirectories: IconTextArea = IconTextArea("")
    private val modpackDirectory: IconTextField = IconTextField("")
    private val serverPackSuffix: IconTextField = IconTextField("")
    private val serverIconPath: IconTextField = IconTextField("")
    private val serverPropertiesPath: IconTextField = IconTextField("")
    private val configPanels: MutableList<ExtensionConfigPanel> = ArrayList<ExtensionConfigPanel>(10)
    private val scriptSettings: ScriptSettings
    private val requiredJavaVersion: JLabel = JLabel("")
    private val statusLine0: JLabel
    private val statusLine1: JLabel
    private val statusLine2: JLabel
    private val statusLine3: JLabel
    private val statusLine4: JLabel
    private val statusLine5: JLabel
    private val serverIconPreview: JLabel = JLabel()
    private val server: JCheckBox
    private val icon: JCheckBox
    private val properties: JCheckBox
    private val zip: JCheckBox
    private var lastLoadedConfiguration: PackConfig? = null
    private val whitespace = "^.*,\\s*\\\\*$".toRegex()

    init {
        serverPackGeneratedTextPane.isOpaque = false
        serverPackGeneratedTextPane.isEditable = false
        StyleConstants.setBold(serverPackGeneratedAttributeSet, true)
        StyleConstants.setFontSize(serverPackGeneratedAttributeSet, 14)
        serverPackGeneratedTextPane.setCharacterAttributes(serverPackGeneratedAttributeSet, true)
        StyleConstants.setAlignment(serverPackGeneratedAttributeSet, StyleConstants.ALIGN_LEFT)
        try {
            serverPackGeneratedDocument.insertString(
                0,
                Gui.createserverpack_gui_createserverpack_openfolder_browse.toString() + "    ",
                serverPackGeneratedAttributeSet
            )
        } catch (ex: BadLocationException) {
            log.error("Error inserting text into aboutDocument.", ex)
        }
        lazyModeTextPane.isOpaque = false
        lazyModeTextPane.isEditable = false
        val lazyModeAttributeSet = SimpleAttributeSet()
        StyleConstants.setBold(lazyModeAttributeSet, true)
        StyleConstants.setFontSize(lazyModeAttributeSet, 14)
        lazyModeTextPane.setCharacterAttributes(lazyModeAttributeSet, true)
        StyleConstants.setAlignment(lazyModeAttributeSet, StyleConstants.ALIGN_LEFT)
        try {
            lazyModeDocument.insertString(
                0,
                (((Gui.configuration_log_warn_checkconfig_copydirs_lazymode0.toString() + "\n\n" + Gui.configuration_log_warn_checkconfig_copydirs_lazymode1.toString()) + "\n" + Gui.configuration_log_warn_checkconfig_copydirs_lazymode2.toString()) + "\n" + Gui.configuration_log_warn_checkconfig_copydirs_lazymode3.toString()) + "\n\n" + Gui.configuration_log_warn_checkconfig_copydirs_lazymode0.toString(),
                lazyModeAttributeSet
            )
        } catch (ex: BadLocationException) {
            log.error("Error inserting text into aboutDocument.", ex)
        }
        val none = arrayOf(
            Gui.createserverpack_gui_createserverpack_forge_none.toString()
        )
        noVersions = DefaultComboBoxModel<String>(none)
        legacyFabricVersions = DefaultComboBoxModel<String>(versionMeta.legacyFabric.loaderVersionsArrayDescending())
        fabricVersions = DefaultComboBoxModel<String>(versionMeta.fabric.loaderVersionsArrayDescending())
        quiltVersions = DefaultComboBoxModel<String>(versionMeta.quilt.loaderVersionsArrayDescending())
        serverPackPanel.layout = GridBagLayout()
        val smallSquareButtonDimension = Dimension(24, 24)
        val bigSquareButtonDimension = Dimension(48, 48)
        val folderIconBase = ImageUtilities.imageFromResourceStream(this.javaClass, "$resourcePrefix/folder.png")
        val folderIconSmall = ImageIcon(folderIconBase.getScaledInstance(24, 24, Image.SCALE_SMOOTH))
        val folderIconBig = ImageIcon(folderIconBase.getScaledInstance(48, 48, Image.SCALE_SMOOTH))
        val revertIcon = ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/revert.png")
        val resetIcon = ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/reset.png")
        val inspectIcon = ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/inspect.png")
        val openIcon = ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/open.png")
        val notoSansDisplayRegularBold15 = Font("Noto Sans Display Regular", Font.BOLD, 15)

        // ----------------------------------------------------------------LABELS AND TEXTFIELDS--------
        val gridBagConstraints = GridBagConstraints()
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridwidth = 3
        gridBagConstraints.weightx = 1.0
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 0

        // Label and textfield modpackDir
        val labelModpackDir = JLabel(Gui.createserverpack_gui_createserverpack_labelmodpackdir.toString())
        labelModpackDir.toolTipText = Gui.createserverpack_gui_createserverpack_labelmodpackdir_tip.toString()
        val twentyTenZeroZero = Insets(20, 10, 0, 0)
        gridBagConstraints.insets = twentyTenZeroZero
        serverPackPanel.add(labelModpackDir, gridBagConstraints)
        modpackDirectory.toolTipText = Gui.createserverpack_gui_createserverpack_labelmodpackdir_tip.toString()
        modpackDirectory.addDocumentListener(object : SimpleDocumentListener {
            override fun update(e: DocumentEvent) {
                validateModpackDir()
            }
        })
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 1
        val zeroTenZeroZero = Insets(0, 10, 0, 0)
        gridBagConstraints.insets = zeroTenZeroZero
        serverPackPanel.add(modpackDirectory, gridBagConstraints)
        val buttonModpackDirectory = JButton()
        buttonModpackDirectory.toolTipText = Gui.createserverpack_gui_buttonmodpackdir.toString()
        buttonModpackDirectory.isContentAreaFilled = false
        buttonModpackDirectory.multiClickThreshhold = 1000
        buttonModpackDirectory.icon = folderIconBig
        buttonModpackDirectory.minimumSize = bigSquareButtonDimension
        buttonModpackDirectory.preferredSize = bigSquareButtonDimension
        buttonModpackDirectory.maximumSize = bigSquareButtonDimension
        buttonModpackDirectory.addActionListener { selectModpackDirectory() }
        addMouseListenerContentAreaFilledToButton(buttonModpackDirectory)
        gridBagConstraints.insets = Insets(20, 10, 0, 0)
        gridBagConstraints.gridheight = 2
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 0
        serverPackPanel.add(buttonModpackDirectory, gridBagConstraints)
        val buttonScanModpackDirectory = JButton()
        buttonScanModpackDirectory.toolTipText = Gui.createserverpack_gui_buttonmodpackdir_scan_tip.toString()
        buttonScanModpackDirectory.isContentAreaFilled = false
        buttonScanModpackDirectory.multiClickThreshhold = 1000
        buttonScanModpackDirectory.icon = inspectIcon
        buttonScanModpackDirectory.minimumSize = bigSquareButtonDimension
        buttonScanModpackDirectory.preferredSize = bigSquareButtonDimension
        buttonScanModpackDirectory.maximumSize = bigSquareButtonDimension
        buttonScanModpackDirectory.addActionListener { updateGuiFromSelectedModpack() }
        addMouseListenerContentAreaFilledToButton(buttonScanModpackDirectory)
        gridBagConstraints.insets = Insets(20, 64, 0, 0)
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 0
        serverPackPanel.add(buttonScanModpackDirectory, gridBagConstraints)

        // Label and textfield server pack suffix
        val labelServerPackSuffix = JLabel(Gui.createserverpack_gui_createserverpack_labelsuffix.toString())
        labelServerPackSuffix.toolTipText = Gui.createserverpack_gui_createserverpack_labelsuffix_tip.toString()
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridheight = 1
        gridBagConstraints.gridwidth = 2
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 0
        gridBagConstraints.insets = Insets(20, 120, 0, 0)
        serverPackPanel.add(labelServerPackSuffix, gridBagConstraints)
        serverPackSuffix.toolTipText = Gui.createserverpack_gui_createserverpack_labelsuffix_tip.toString()
        errorIconServerPackSuffix.description =
            Gui.createserverpack_gui_createserverpack_textsuffix_error.toString()
        serverPackSuffix.addDocumentListener(object : SimpleDocumentListener {
            override fun update(e: DocumentEvent) {
                validateSuffix()
            }
        })
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 1
        gridBagConstraints.insets = Insets(0, 120, 0, 0)
        serverPackPanel.add(serverPackSuffix, gridBagConstraints)
        gridBagConstraints.gridwidth = 5

        // Label and textfield clientMods
        val labelClientMods = JLabel(Gui.createserverpack_gui_createserverpack_labelclientmods.toString())
        labelClientMods.toolTipText = Gui.createserverpack_gui_createserverpack_labelclientmods_tip.toString()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 2
        gridBagConstraints.insets = twentyTenZeroZero
        serverPackPanel.add(labelClientMods, gridBagConstraints)
        clientsideMods.toolTipText = Gui.createserverpack_gui_createserverpack_labelclientmods_tip.toString()
        val notoSansDisplayRegularPlain15 = Font("Noto Sans Display Regular", Font.PLAIN, 15)
        clientsideMods.font = notoSansDisplayRegularPlain15
        errorIconClientsideMods.description =
            Gui.createserverpack_gui_createserverpack_textclientmods_error.toString()
        clientsideMods.addDocumentListener(object : SimpleDocumentListener {
            override fun update(e: DocumentEvent) {
                validateClientMods()
            }
        })
        val clientsidemodsJpanel = JPanel()
        clientsidemodsJpanel.layout = GridBagLayout()
        val textareaClientsidemodsJpanelConstraints = GridBagConstraints()
        textareaClientsidemodsJpanelConstraints.anchor = GridBagConstraints.CENTER
        textareaClientsidemodsJpanelConstraints.fill = GridBagConstraints.BOTH
        textareaClientsidemodsJpanelConstraints.gridx = 0
        textareaClientsidemodsJpanelConstraints.gridy = 0
        textareaClientsidemodsJpanelConstraints.weighty = 1.0
        textareaClientsidemodsJpanelConstraints.weightx = 1.0
        val scrollPanelClientsideMods = JScrollPane(
            clientsideMods, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        clientsidemodsJpanel.add(
            scrollPanelClientsideMods, textareaClientsidemodsJpanelConstraints
        )
        val client = Dimension(100, 150)
        clientsidemodsJpanel.size = client
        clientsidemodsJpanel.preferredSize = client
        clientsidemodsJpanel.maximumSize = client
        clientsidemodsJpanel.minimumSize = client
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 3
        gridBagConstraints.insets = zeroTenZeroZero
        gridBagConstraints.fill = GridBagConstraints.BOTH
        serverPackPanel.add(clientsidemodsJpanel, gridBagConstraints)
        gridBagConstraints.gridwidth = 1
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.insets = Insets(0, 10, 0, 10)
        gridBagConstraints.weightx = 0.0
        gridBagConstraints.weighty = 0.0
        val buttonClientsidemods = JButton()
        buttonClientsidemods.toolTipText = Gui.createserverpack_gui_buttonclientmods.toString()
        buttonClientsidemods.isContentAreaFilled = false
        buttonClientsidemods.multiClickThreshhold = 1000
        buttonClientsidemods.icon = folderIconSmall
        buttonClientsidemods.minimumSize = smallSquareButtonDimension
        buttonClientsidemods.preferredSize = smallSquareButtonDimension
        buttonClientsidemods.maximumSize = smallSquareButtonDimension
        buttonClientsidemods.addActionListener { selectClientMods() }
        addMouseListenerContentAreaFilledToButton(buttonClientsidemods)
        gridBagConstraints.gridx = 5
        gridBagConstraints.gridy = 3
        serverPackPanel.add(buttonClientsidemods, gridBagConstraints)
        val buttonClientsidemodsRevert = JButton()
        buttonClientsidemodsRevert.toolTipText = Gui.createserverpack_gui_buttonclientmods_revert_tip.toString()
        buttonClientsidemodsRevert.isContentAreaFilled = false
        buttonClientsidemodsRevert.multiClickThreshhold = 1000
        buttonClientsidemodsRevert.icon = revertIcon
        buttonClientsidemodsRevert.minimumSize = smallSquareButtonDimension
        buttonClientsidemodsRevert.preferredSize = smallSquareButtonDimension
        buttonClientsidemodsRevert.maximumSize = smallSquareButtonDimension
        buttonClientsidemodsRevert.addActionListener { revertClientsidemods() }
        addMouseListenerContentAreaFilledToButton(buttonClientsidemodsRevert)
        gridBagConstraints.insets = Insets(0, 10, 90, 10)
        serverPackPanel.add(buttonClientsidemodsRevert, gridBagConstraints)
        val buttonClientsidemodsReset = JButton()
        buttonClientsidemodsReset.toolTipText = Gui.createserverpack_gui_buttonclientmods_reset_tip.toString()
        buttonClientsidemodsReset.isContentAreaFilled = false
        buttonClientsidemodsReset.multiClickThreshhold = 1000
        buttonClientsidemodsReset.icon = resetIcon
        buttonClientsidemodsReset.minimumSize = smallSquareButtonDimension
        buttonClientsidemodsReset.preferredSize = smallSquareButtonDimension
        buttonClientsidemodsReset.maximumSize = smallSquareButtonDimension
        buttonClientsidemodsReset.addActionListener { setClientSideMods(apiProperties.clientSideMods()) }
        addMouseListenerContentAreaFilledToButton(buttonClientsidemodsReset)
        gridBagConstraints.insets = Insets(90, 10, 0, 10)
        serverPackPanel.add(buttonClientsidemodsReset, gridBagConstraints)

        // Label and textfield copyDirs
        val labelCopyDirs = JLabel(Gui.createserverpack_gui_createserverpack_labelcopydirs.toString())
        labelCopyDirs.toolTipText = Gui.createserverpack_gui_createserverpack_labelcopydirs_tip.toString()
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 4
        gridBagConstraints.insets = twentyTenZeroZero
        gridBagConstraints.gridwidth = 5
        serverPackPanel.add(labelCopyDirs, gridBagConstraints)
        copyDirectories.toolTipText = Gui.createserverpack_gui_createserverpack_labelcopydirs_tip.toString()
        copyDirectories.font = notoSansDisplayRegularPlain15
        errorIconCopyDirectory.description =
            Gui.createserverpack_gui_createserverpack_textclientmods_error.toString()
        copyDirectories.addDocumentListener(object : SimpleDocumentListener {
            override fun update(e: DocumentEvent) {
                validateCopyDirs()
            }
        })
        val copydirectoriesJpanel = JPanel()
        copydirectoriesJpanel.layout = GridBagLayout()
        val textareaCopydirectoriesJpanelConstraints = GridBagConstraints()
        textareaCopydirectoriesJpanelConstraints.anchor = GridBagConstraints.CENTER
        textareaCopydirectoriesJpanelConstraints.fill = GridBagConstraints.BOTH
        textareaCopydirectoriesJpanelConstraints.gridx = 0
        textareaCopydirectoriesJpanelConstraints.gridy = 0
        textareaCopydirectoriesJpanelConstraints.weighty = 1.0
        textareaCopydirectoriesJpanelConstraints.weightx = 1.0
        val scrollPanelCopydirectories = JScrollPane(
            copyDirectories, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        copydirectoriesJpanel.add(
            scrollPanelCopydirectories, textareaCopydirectoriesJpanelConstraints
        )
        val copy = Dimension(100, 100)
        copydirectoriesJpanel.size = copy
        copydirectoriesJpanel.preferredSize = copy
        copydirectoriesJpanel.maximumSize = copy
        copydirectoriesJpanel.minimumSize = copy
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 5
        gridBagConstraints.insets = zeroTenZeroZero
        gridBagConstraints.fill = GridBagConstraints.BOTH
        serverPackPanel.add(copydirectoriesJpanel, gridBagConstraints)
        gridBagConstraints.gridwidth = 1
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.insets = Insets(0, 10, 0, 10)
        gridBagConstraints.weightx = 0.0
        gridBagConstraints.weighty = 0.0
        val buttonCopydirectories = JButton()
        buttonCopydirectories.toolTipText = Gui.createserverpack_gui_buttoncopydirs.toString()
        buttonCopydirectories.isContentAreaFilled = false
        buttonCopydirectories.icon = folderIconSmall
        buttonCopydirectories.multiClickThreshhold = 1000
        buttonCopydirectories.minimumSize = smallSquareButtonDimension
        buttonCopydirectories.preferredSize = smallSquareButtonDimension
        buttonCopydirectories.maximumSize = smallSquareButtonDimension
        buttonCopydirectories.addActionListener { selectCopyDirs() }
        addMouseListenerContentAreaFilledToButton(buttonCopydirectories)
        gridBagConstraints.gridx = 5
        gridBagConstraints.gridy = 5
        serverPackPanel.add(buttonCopydirectories, gridBagConstraints)
        val buttonCopydirsRevert = JButton()
        buttonCopydirsRevert.toolTipText = Gui.createserverpack_gui_buttoncopydirs_revert_tip.toString()
        buttonCopydirsRevert.isContentAreaFilled = false
        buttonCopydirsRevert.multiClickThreshhold = 1000
        buttonCopydirsRevert.icon = revertIcon
        buttonCopydirsRevert.minimumSize = smallSquareButtonDimension
        buttonCopydirsRevert.preferredSize = smallSquareButtonDimension
        buttonCopydirsRevert.maximumSize = smallSquareButtonDimension
        buttonCopydirsRevert.addActionListener { revertCopydirs() }
        addMouseListenerContentAreaFilledToButton(buttonCopydirsRevert)
        gridBagConstraints.insets = Insets(0, 10, 80, 10)
        serverPackPanel.add(buttonCopydirsRevert, gridBagConstraints)
        val buttonCopydirsReset = JButton()
        buttonCopydirsReset.toolTipText = Gui.createserverpack_gui_buttoncopydirs_reset_tip.toString()
        buttonCopydirsReset.isContentAreaFilled = false
        buttonCopydirsReset.multiClickThreshhold = 1000
        buttonCopydirsReset.icon = resetIcon
        buttonCopydirsReset.minimumSize = smallSquareButtonDimension
        buttonCopydirsReset.preferredSize = smallSquareButtonDimension
        buttonCopydirsReset.maximumSize = smallSquareButtonDimension
        buttonCopydirsReset.addActionListener { setCopyDirectories(apiProperties.directoriesToInclude.toMutableList()) }
        addMouseListenerContentAreaFilledToButton(buttonCopydirsReset)
        gridBagConstraints.insets = Insets(80, 10, 0, 10)
        serverPackPanel.add(buttonCopydirsReset, gridBagConstraints)

        // Labels and textfields server-icon.png and server.properties paths
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridwidth = 5
        val labelServerIconPath = JLabel(Gui.createserverpack_gui_createserverpack_labeliconpath.toString())
        labelServerIconPath.toolTipText = Gui.createserverpack_gui_createserverpack_labeliconpath_tip.toString()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 6
        gridBagConstraints.insets = Insets(20, 10, 0, 54)
        serverPackPanel.add(labelServerIconPath, gridBagConstraints)
        serverIconPath.text = ""
        serverIconPath.toolTipText = Gui.createserverpack_gui_createserverpack_textfield_iconpath.toString()
        errorIconServerIcon.description =
            Gui.createserverpack_gui_createserverpack_textfield_iconpath_error.toString()
        serverIconPath.addDocumentListener(object : SimpleDocumentListener {
            override fun update(e: DocumentEvent) {
                validateServerIcon()
            }
        })
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 7
        gridBagConstraints.insets = Insets(0, 10, 0, 80)
        serverPackPanel.add(serverIconPath, gridBagConstraints)
        serverIconPreview.minimumSize = Dimension(48, 48)
        serverIconPreview.preferredSize = Dimension(48, 48)
        serverIconPreview.toolTipText = Gui.createserverpack_gui_createserverpack_servericon_preview.toString()
        gridBagConstraints.gridx = 5
        gridBagConstraints.gridy = 6
        gridBagConstraints.gridheight = 2
        gridBagConstraints.gridwidth = 1
        gridBagConstraints.insets = Insets(20, -75, 0, -65)
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.anchor = GridBagConstraints.WEST
        serverPackPanel.add(serverIconPreview, gridBagConstraints)
        val buttonServericon = JButton()
        buttonServericon.toolTipText = Gui.createserverpack_gui_createserverpack_button_icon.toString()
        buttonServericon.isContentAreaFilled = false
        buttonServericon.icon = folderIconBig
        buttonServericon.multiClickThreshhold = 1000
        buttonServericon.minimumSize = bigSquareButtonDimension
        buttonServericon.preferredSize = bigSquareButtonDimension
        buttonServericon.maximumSize = bigSquareButtonDimension
        buttonServericon.addActionListener { selectServerIcon() }
        addMouseListenerContentAreaFilledToButton(buttonServericon)
        gridBagConstraints.insets = Insets(20, -15, 0, 0)
        gridBagConstraints.gridx = 5
        gridBagConstraints.gridy = 6
        serverPackPanel.add(buttonServericon, gridBagConstraints)
        val labelServerPropertiesPath =
            JLabel(Gui.createserverpack_gui_createserverpack_labelpropertiespath.toString())
        labelServerPropertiesPath.toolTipText =
            Gui.createserverpack_gui_createserverpack_labelpropertiespath_tip.toString()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 8
        gridBagConstraints.gridwidth = 5
        gridBagConstraints.gridheight = 1
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.insets = twentyTenZeroZero
        serverPackPanel.add(labelServerPropertiesPath, gridBagConstraints)
        serverPropertiesPath.text = ""
        serverPropertiesPath.toolTipText =
            Gui.createserverpack_gui_createserverpack_textfield_propertiespath.toString()
        errorIconServerProperties.description =
            Gui.createserverpack_gui_createserverpack_textfield_propertiespath_error.toString()
        serverPropertiesPath.addDocumentListener(object : SimpleDocumentListener {
            override fun update(e: DocumentEvent) {
                validateServerProperties()
            }
        })
        gridBagConstraints.gridy = 9
        gridBagConstraints.insets = Insets(0, 10, 10, 80)
        serverPackPanel.add(serverPropertiesPath, gridBagConstraints)
        val buttonOpenServerproperties = JButton()
        buttonOpenServerproperties.toolTipText =
            Gui.createserverpack_gui_createserverpack_button_open_properties.toString()
        buttonOpenServerproperties.isContentAreaFilled = false
        buttonOpenServerproperties.icon = openIcon
        buttonOpenServerproperties.multiClickThreshhold = 1000
        buttonOpenServerproperties.minimumSize = bigSquareButtonDimension
        buttonOpenServerproperties.preferredSize = bigSquareButtonDimension
        buttonOpenServerproperties.maximumSize = bigSquareButtonDimension
        buttonOpenServerproperties.addActionListener { openServerProperties() }
        addMouseListenerContentAreaFilledToButton(buttonOpenServerproperties)
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.gridwidth = 1
        gridBagConstraints.gridheight = 2
        gridBagConstraints.gridx = 5
        gridBagConstraints.gridy = 8
        gridBagConstraints.insets = Insets(20, -75, 10, -65)
        serverPackPanel.add(buttonOpenServerproperties, gridBagConstraints)
        val buttonServerproperties = JButton()
        buttonServerproperties.toolTipText = Gui.createserverpack_gui_createserverpack_button_properties.toString()
        buttonServerproperties.isContentAreaFilled = false
        buttonServerproperties.icon = folderIconBig
        buttonServerproperties.multiClickThreshhold = 1000
        buttonServerproperties.minimumSize = bigSquareButtonDimension
        buttonServerproperties.preferredSize = bigSquareButtonDimension
        buttonServerproperties.maximumSize = bigSquareButtonDimension
        buttonServerproperties.addActionListener { selectServerProperties() }
        addMouseListenerContentAreaFilledToButton(buttonServerproperties)
        gridBagConstraints.gridx = 5
        gridBagConstraints.gridy = 8
        gridBagConstraints.insets = Insets(20, -15, 10, 0)
        serverPackPanel.add(buttonServerproperties, gridBagConstraints)
        val combo = Dimension(270, 30)

        // Label and combobox minecraftVersion
        val labelMinecraftVersion = JLabel(Gui.createserverpack_gui_createserverpack_labelminecraft.toString())
        labelMinecraftVersion.toolTipText = Gui.createserverpack_gui_createserverpack_labelminecraft_tip.toString()
        labelMinecraftVersion.preferredSize = combo
        labelMinecraftVersion.maximumSize = combo
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 10
        gridBagConstraints.gridwidth = 5
        gridBagConstraints.gridheight = 1
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.insets = Insets(0, 10, 0, 5)
        serverPackPanel.add(labelMinecraftVersion, gridBagConstraints)
        if (apiProperties.isMinecraftPreReleasesAvailabilityEnabled) {
            minecraftVersions.setModel(DefaultComboBoxModel(versionMeta.minecraft.allVersionsArrayDescending()))
        } else {
            minecraftVersions.setModel(DefaultComboBoxModel(versionMeta.minecraft.releaseVersionsArrayDescending()))
        }
        if (minecraftVersions.selectedItem == null) {
            minecraftVersions.selectedIndex = 0
        }
        minecraftVersions.addActionListener { updateMinecraftValues() }
        minecraftVersions.preferredSize = combo
        minecraftVersions.maximumSize = combo
        gridBagConstraints.gridy = 11
        serverPackPanel.add(minecraftVersions, gridBagConstraints)

        // Label and combobox buttons Modloader
        gridBagConstraints.insets = Insets(0, 300, 0, 5)
        val labelModloader = JLabel(Gui.createserverpack_gui_createserverpack_labelmodloader.toString())
        labelModloader.toolTipText = Gui.createserverpack_gui_createserverpack_labelmodloader_tip.toString()
        labelModloader.preferredSize = combo
        labelModloader.maximumSize = combo
        gridBagConstraints.gridy = 10
        serverPackPanel.add(labelModloader, gridBagConstraints)
        modloaders.model = DefaultComboBoxModel(apiProperties.supportedModloaders)
        if (modloaders.selectedItem == null) {
            modloaders.selectedIndex = 0
        }
        modloaders.addActionListener { setModloader(modloaders.selectedItem!!.toString()) }
        modloaders.preferredSize = combo
        modloaders.maximumSize = combo
        gridBagConstraints.gridy = 11
        serverPackPanel.add(modloaders, gridBagConstraints)

        // Label and combobox modloaderVersion
        gridBagConstraints.insets = Insets(0, 590, 0, 5)
        val labelModloaderVersion =
            JLabel(Gui.createserverpack_gui_createserverpack_labelmodloaderversion.toString())
        labelModloaderVersion.toolTipText =
            Gui.createserverpack_gui_createserverpack_labelmodloaderversion_tip.toString()
        labelModloaderVersion.preferredSize = combo
        labelModloaderVersion.maximumSize = combo
        gridBagConstraints.gridy = 10
        serverPackPanel.add(labelModloaderVersion, gridBagConstraints)
        modloaderVersions.model = fabricVersions
        modloaderVersions.selectedIndex = 0
        modloaderVersions.isVisible = true
        modloaderVersions.preferredSize = combo
        modloaderVersions.maximumSize = combo
        gridBagConstraints.gridy = 11
        serverPackPanel.add(modloaderVersions, gridBagConstraints)
        gridBagConstraints.insets = Insets(0, 880, 0, 5)
        val java = Dimension(160, 30)
        val labelRequiredJavaVersion = JLabel(Gui.createserverpack_gui_createserverpack_minecraft_java.toString())
        labelRequiredJavaVersion.toolTipText =
            Gui.createserverpack_gui_createserverpack_minecraft_java_tooltip.toString()
        labelRequiredJavaVersion.preferredSize = java
        labelRequiredJavaVersion.maximumSize = java
        gridBagConstraints.gridy = 10
        serverPackPanel.add(labelRequiredJavaVersion, gridBagConstraints)
        requiredJavaVersion.preferredSize = java
        requiredJavaVersion.maximumSize = java
        val requiredJavaFont = Font("Noto Sans Display Regular", Font.BOLD, 20)
        requiredJavaVersion.font = requiredJavaFont
        gridBagConstraints.gridy = 11
        serverPackPanel.add(requiredJavaVersion, gridBagConstraints)

        // ----------------------------------------------------------------CHECKBOXES-------------------
        // Checkbox installServer
        server = JCheckBox(
            Gui.createserverpack_gui_createserverpack_checkboxserver.toString(), true
        )
        server.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxserver_tip.toString()
        server.addActionListener { checkServer() }
        val check = Dimension(270, 40)
        server.size = check
        server.minimumSize = check
        server.preferredSize = check
        server.maximumSize = check
        gridBagConstraints.gridwidth = 5
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.fill = GridBagConstraints.NONE
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 14
        gridBagConstraints.insets = Insets(10, 5, 5, 5)
        serverPackPanel.add(server, gridBagConstraints)

        // Checkbox copyIcon
        icon = JCheckBox(Gui.createserverpack_gui_createserverpack_checkboxicon.toString(), true)
        icon.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxicon_tip.toString()
        icon.addActionListener {
            validateServerIcon()
        }
        icon.size = check
        icon.minimumSize = check
        icon.preferredSize = check
        icon.maximumSize = check
        gridBagConstraints.insets = Insets(5, 275, 5, 5)
        serverPackPanel.add(icon, gridBagConstraints)

        // Checkbox copyProperties
        properties = JCheckBox(
            Gui.createserverpack_gui_createserverpack_checkboxproperties.toString(), true
        )
        properties.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxproperties_tip.toString()
        properties.size = check
        properties.minimumSize = check
        properties.preferredSize = check
        properties.maximumSize = check
        gridBagConstraints.insets = Insets(5, 545, 5, 5)
        serverPackPanel.add(properties, gridBagConstraints)

        // Checkbox createZIP
        zip = JCheckBox(Gui.createserverpack_gui_createserverpack_checkboxzip.toString(), true)
        zip.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxzip_tip.toString()
        zip.size = check
        zip.minimumSize = check
        zip.preferredSize = check
        zip.maximumSize = check
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 14
        gridBagConstraints.insets = Insets(5, 815, 5, 5)
        serverPackPanel.add(zip, gridBagConstraints)
        val labelJavaArgs = JLabel(Gui.createserverpack_gui_createserverpack_javaargs.toString())
        labelJavaArgs.toolTipText = Gui.createserverpack_gui_createserverpack_javaargs_tip.toString()
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 15
        gridBagConstraints.gridwidth = 5
        gridBagConstraints.insets = Insets(0, 10, 0, 0)
        serverPackPanel.add(labelJavaArgs, gridBagConstraints)
        javaArgs.toolTipText = Gui.createserverpack_gui_createserverpack_javaargs_tip.toString()
        javaArgs.font = notoSansDisplayRegularPlain15
        val javaargsJpanel = JPanel()
        javaargsJpanel.layout = GridBagLayout()
        javaargsJpanel.setSize(100, 100)
        javaargsJpanel.preferredSize = Dimension(100, 100)
        javaargsJpanel.maximumSize = Dimension(100, 100)
        javaargsJpanel.minimumSize = Dimension(100, 100)
        val scrollPaneJavaArgs = JScrollPane(
            javaArgs, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        val textareaJavaargsJpanelConstraints = GridBagConstraints()
        textareaJavaargsJpanelConstraints.anchor = GridBagConstraints.CENTER
        textareaJavaargsJpanelConstraints.fill = GridBagConstraints.BOTH
        textareaJavaargsJpanelConstraints.gridx = 0
        textareaJavaargsJpanelConstraints.gridy = 0
        textareaJavaargsJpanelConstraints.weighty = 1.0
        textareaJavaargsJpanelConstraints.weightx = 1.0
        javaargsJpanel.add(scrollPaneJavaArgs, textareaJavaargsJpanelConstraints)
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridwidth = 5
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 16
        gridBagConstraints.insets = Insets(0, 10, 0, 30)
        serverPackPanel.add(javaargsJpanel, gridBagConstraints)
        val buttonAikarsFlags = JButton()
        buttonAikarsFlags.toolTipText = Gui.createserverpack_gui_createserverpack_button_properties.toString()
        buttonAikarsFlags.isContentAreaFilled = false
        buttonAikarsFlags.icon = RotatedIcon(
            TextIcon(
                buttonAikarsFlags,
                Gui.createserverpack_gui_createserverpack_javaargs_aikar.toString(),
                TextIcon.Layout.HORIZONTAL
            ), 90.0, RotatedIcon.Rotate.UP
        )
        buttonAikarsFlags.multiClickThreshhold = 1000
        buttonAikarsFlags.addActionListener { this.setAikarsFlagsAsJavaArguments() }
        buttonAikarsFlags.preferredSize = Dimension(44, 123)
        buttonAikarsFlags.maximumSize = Dimension(44, 123)
        addMouseListenerContentAreaFilledToButton(buttonAikarsFlags)
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridx = 5
        gridBagConstraints.gridy = 15
        gridBagConstraints.gridheight = 2
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.insets = Insets(0, -23, 5, 12)
        serverPackPanel.add(buttonAikarsFlags, gridBagConstraints)

        // --------------------------------------------------------SCRIPT VARIABLES---------------------
        val scriptSettingsLabel = JLabel(Gui.createserverpack_gui_createserverpack_scriptsettings_label.toString())
        scriptSettingsLabel.toolTipText =
            Gui.createserverpack_gui_createserverpack_scriptsettings_label_tooltip.toString()
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 17
        gridBagConstraints.gridwidth = 5
        gridBagConstraints.gridheight = 1
        gridBagConstraints.weightx = 1.0
        gridBagConstraints.insets = Insets(10, 10, 10, 0)
        serverPackPanel.add(scriptSettingsLabel, gridBagConstraints)
        scriptSettings = ScriptSettings()
        val tableScrollPane = JScrollPane(scriptSettings)
        tableScrollPane.preferredSize = Dimension(700, 300)
        tableScrollPane.maximumSize = Dimension(700, 300)
        gridBagConstraints.gridy = 18
        gridBagConstraints.insets = Insets(0, 10, 20, 0)
        serverPackPanel.add(tableScrollPane, gridBagConstraints)

        // --------------------------------------------------------CONFIGPANE EXTENSIONS----------------
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.fill = GridBagConstraints.BOTH
        gridBagConstraints.insets = Insets(10, 10, 10, 10)
        gridBagConstraints.gridwidth = 6
        var yPos = gridBagConstraints.gridy
        configPanels.addAll(apiPlugins.getConfigPanels(this))
        for (panel in configPanels) {
            yPos++
            gridBagConstraints.gridy = yPos
            serverPackPanel.add(panel, gridBagConstraints)
        }

        // ---------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------
        gridBagConstraints.insets = Insets(5, 10, 5, 10)
        val generateIcon =
            ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/start_generation.png")
        val dimension = Dimension(200, 70)
        buttonGenerateServerPack.icon = CompoundIcon(
            arrayOf(
                generateIcon.getScaledInstance(32, 32, Image.SCALE_SMOOTH), TextIcon(
                    buttonGenerateServerPack, Gui.createserverpack_gui_buttongenerateserverpack.toString()
                )
            ), 12, CompoundIcon.Axis.X_AXIS
        )
        buttonGenerateServerPack.addActionListener { generateServerpack() }
        buttonGenerateServerPack.multiClickThreshhold = 1000
        buttonGenerateServerPack.toolTipText = Gui.createserverpack_gui_buttongenerateserverpack_tip.toString()
        buttonGenerateServerPack.size = dimension
        buttonGenerateServerPack.minimumSize = dimension
        buttonGenerateServerPack.preferredSize = dimension
        buttonGenerateServerPack.maximumSize = dimension
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy += 1
        gridBagConstraints.gridwidth = 1
        gridBagConstraints.gridheight = 1
        gridBagConstraints.weightx = 1.0
        gridBagConstraints.weighty = 1.0
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.fill = GridBagConstraints.NONE
        serverPackPanel.add(buttonGenerateServerPack, gridBagConstraints)
        val buttonServerPacks = JButton()
        buttonServerPacks.icon = CompoundIcon(
            arrayOf(
                folderIconSmall, TextIcon(buttonServerPacks, Gui.createserverpack_gui_buttonserverpacks.toString())
            ), 8, CompoundIcon.Axis.X_AXIS
        )
        buttonServerPacks.addActionListener { utilities.fileUtilities.openFolder(apiProperties.serverPacksDirectory) }
        buttonServerPacks.multiClickThreshhold = 1000
        buttonServerPacks.toolTipText = Gui.createserverpack_gui_buttonserverpacks_tip.toString()
        buttonServerPacks.size = dimension
        buttonServerPacks.minimumSize = dimension
        buttonServerPacks.preferredSize = dimension
        buttonServerPacks.maximumSize = dimension
        gridBagConstraints.insets = Insets(85, 10, 5, 10)
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        serverPackPanel.add(buttonServerPacks, gridBagConstraints)
        val statusPanel = JPanel()
        statusPanel.layout = BoxLayout(statusPanel, BoxLayout.Y_AXIS)
        statusPanel.alignmentX = 0.0f
        statusLine0 = JLabel("...${reticulatingSplines.reticulate()}   ")
        statusLine1 = JLabel("...${reticulatingSplines.reticulate()}   ")
        statusLine2 = JLabel("...${reticulatingSplines.reticulate()}   ")
        statusLine3 = JLabel("...${reticulatingSplines.reticulate()}   ")
        statusLine4 = JLabel("...${reticulatingSplines.reticulate()}   ")
        statusLine5 = JLabel("${Gui.createserverpack_gui_buttongenerateserverpack_ready}   ")

        // Make sure all labels start on the left and extend to the right
        statusLine0.horizontalAlignment = JLabel.LEFT
        statusLine1.horizontalAlignment = JLabel.LEFT
        statusLine2.horizontalAlignment = JLabel.LEFT
        statusLine3.horizontalAlignment = JLabel.LEFT
        statusLine4.horizontalAlignment = JLabel.LEFT
        statusLine5.horizontalAlignment = JLabel.LEFT

        // Set the preferred size of the labels, so they do not resize when long texts are set later on
        val labelDimension = Dimension(700, 30)
        statusLine0.preferredSize = labelDimension
        statusLine1.preferredSize = labelDimension
        statusLine2.preferredSize = labelDimension
        statusLine3.preferredSize = labelDimension
        statusLine4.preferredSize = labelDimension
        statusLine5.preferredSize = labelDimension
        statusLine0.font = notoSansDisplayRegularBold15
        statusLine1.font = notoSansDisplayRegularBold15
        statusLine2.font = notoSansDisplayRegularBold15
        statusLine3.font = notoSansDisplayRegularBold15
        statusLine4.font = notoSansDisplayRegularBold15
        statusLine5.font = notoSansDisplayRegularBold15
        updatePanelTheme()
        statusPanel.add(statusLine0)
        statusPanel.add(statusLine1)
        statusPanel.add(statusLine2)
        statusPanel.add(statusLine3)
        statusPanel.add(statusLine4)
        statusPanel.add(statusLine5)
        statusPanel.preferredSize = Dimension(700, 140)
        gridBagConstraints.insets = Insets(15, 220, 5, 10)
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridwidth = 6
        gridBagConstraints.gridheight = 1
        gridBagConstraints.weightx = 1.0
        gridBagConstraints.weighty = 1.0
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        serverPackPanel.add(statusPanel, gridBagConstraints)
        gridBagConstraints.insets = Insets(0, 0, 0, 0)
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy += 1
        gridBagConstraints.gridwidth = 6
        gridBagConstraints.weightx = 1.0
        gridBagConstraints.weighty = 1.0
        gridBagConstraints.anchor = GridBagConstraints.SOUTH
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        larsonScanner.preferredSize = Dimension(700, 40)
        serverPackPanel.add(larsonScanner, gridBagConstraints)
        larsonScanner.loadConfig(idleConfig)
        larsonScanner.play()

        // --------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
        gridBagConstraints.fill = GridBagConstraints.NONE
        val tabCreateServerPackTabScrollPanel = JScrollPane(serverPackPanel)
        tabCreateServerPackTabScrollPanel.verticalScrollBar.unitIncrement = 16
        layout = BorderLayout()
        add(tabCreateServerPackTabScrollPanel, BorderLayout.CENTER)
        loadConfig(apiProperties.defaultConfig)
        Tailer.create(
            File(apiProperties.logsDirectory, "serverpackcreator.log"), object : TailerListenerAdapter() {
                override fun handle(line: String) {
                    if (!line.contains("DEBUG")) {
                        updateStatus(line.substring(line.indexOf(") - ") + 4))
                    }
                }
            }, 100, false
        )
    }

    /**
     * Open the server.properties-file in the local editor. If no file is specified by the user, the
     * default server.properties in `server_files` will be opened.
     *
     * @author Griefed
     */
    private fun openServerProperties() {
        if (File(getServerPropertiesPath()).isFile) {
            utilities.fileUtilities.openFile(getServerPropertiesPath())
        } else {
            utilities.fileUtilities.openFile(apiProperties.defaultServerProperties)
        }
    }

    /**
     * Reverts the list of copy directories to the value of the last loaded configuration, if one is
     * available.
     *
     * @author Griefed
     */
    private fun revertCopydirs() {
        if (lastLoadedConfiguration != null) {
            setCopyDirectories(lastLoadedConfiguration!!.copyDirs)
        }
    }

    /**
     * Reverts the list of clientside-only mods to the value of the last loaded configuration, if one
     * is available.
     *
     * @author Griefed
     */
    private fun revertClientsidemods() {
        if (lastLoadedConfiguration != null) {
            setClientSideMods(lastLoadedConfiguration!!.clientMods)
        }
    }

    /**
     * Update the status labels with the current themes font-color and alpha.
     *
     * @author Griefed
     */
    fun updatePanelTheme() {
        statusLine0.foreground = Color(
            getThemeTextColor().red, getThemeTextColor().green, getThemeTextColor().blue, 20
        )
        statusLine1.foreground = Color(
            getThemeTextColor().red, getThemeTextColor().green, getThemeTextColor().blue, 50
        )
        statusLine2.foreground = Color(
            getThemeTextColor().red, getThemeTextColor().green, getThemeTextColor().blue, 100
        )
        statusLine3.foreground = Color(
            getThemeTextColor().red, getThemeTextColor().green, getThemeTextColor().blue, 150
        )
        statusLine4.foreground = Color(
            getThemeTextColor().red, getThemeTextColor().green, getThemeTextColor().blue, 200
        )
        idleConfig.scannerBackgroundColour = background
        idleConfig.eyeBackgroundColour = background
        generatingConfig.scannerBackgroundColour = background
        generatingConfig.eyeBackgroundColour = background
        larsonScanner.background = background
        larsonScanner.eyeBackground = background
        scriptSettings.setShowGrid(true)
        scriptSettings.showHorizontalLines = true
        scriptSettings.showVerticalLines = true
        if (apiProperties.isDarkTheme()) {
            scriptSettings.gridColor = coffee
        } else {
            scriptSettings.gridColor = swampGreen
        }
    }

    /**
     * Update the labels in the status panel.
     *
     * @param text The text to update the status with.
     * @author Griefed
     */
    private fun updateStatus(text: String) {
        statusLine0.text = statusLine1.text + "   "
        statusLine1.text = statusLine2.text + "   "
        statusLine2.text = statusLine3.text + "   "
        statusLine3.text = statusLine4.text + "   "
        statusLine4.text = statusLine5.text + "   "
        statusLine5.text = "$text       "
    }

    /**
     * Validate all text-based input fields.
     *
     * @author Griefed
     */
    override fun validateInputFields() {
        validateModpackDir()
        validateSuffix()
        validateClientMods()
        validateCopyDirs()
        validateServerIcon()
        validateServerProperties()
    }

    /**
     * Validate the input field for modpack directory.
     *
     * @author Griefed
     */
    private fun validateModpackDir(): List<String> {
        val errors: MutableList<String> = ArrayList(20)
        runBlocking(Dispatchers.Default) {
            if (configurationHandler.checkModpackDir(getModpackDirectory(), errors, false)) {
                modpackDirectory.setIcon(null)
                modpackDirectory.toolTipText =
                    Gui.createserverpack_gui_createserverpack_labelmodpackdir_tip.toString()
                modpackDirectory.foreground = getThemeTextColor()
            } else {
                modpackDirectory.foreground = getThemeErrorColor()
                modpackDirectory.setIcon(errorIconModpackDirectory)
                errorIconModpackDirectory.description = errors.joinToString(",")
                modpackDirectory.toolTipText = errors.joinToString(",")
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for server pack suffix.
     *
     * @author Griefed
     */
    private fun validateSuffix(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        runBlocking(Dispatchers.Default) {
            if (utilities.stringUtilities.checkForIllegalCharacters(serverPackSuffix.text)) {
                serverPackSuffix.setIcon(null)
                serverPackSuffix.toolTipText = Gui.createserverpack_gui_createserverpack_labelsuffix_tip.toString()
                serverPackSuffix.foreground = getThemeTextColor()
            } else {
                serverPackSuffix.foreground = getThemeErrorColor()
                serverPackSuffix.setIcon(errorIconServerPackSuffix)
                serverPackSuffix.toolTipText = errorIconServerPackSuffix.description
                errors.add(Gui.configuration_log_error_serverpack_suffix.toString())
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for client mods.
     *
     * @author Griefed
     */
    private fun validateClientMods(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        runBlocking(Dispatchers.Default) {

            if (!getClientSideMods().matches(whitespace)) {
                clientsideMods.setIcon(null)
                clientsideMods.toolTipText =
                    Gui.createserverpack_gui_createserverpack_labelclientmods_tip.toString()
                clientsideMods.foreground = getThemeTextColor()
            } else {
                clientsideMods.foreground = getThemeErrorColor()
                clientsideMods.setIcon(errorIconClientsideMods)
                clientsideMods.toolTipText = errorIconClientsideMods.description
                errors.add(Gui.configuration_log_error_formatting.toString())
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for copy directories.
     *
     * @author Griefed
     */
    private fun validateCopyDirs(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        runBlocking(Dispatchers.Default) {
            configurationHandler.checkCopyDirs(
                getCopyDirectoriesList(), getModpackDirectory(), errors, false
            )
            if (getCopyDirectories().matches(whitespace)) {
                errors.add(Gui.configuration_log_error_formatting.toString())
            }
            if (errors.isNotEmpty()) {
                copyDirectories.foreground = getThemeErrorColor()
                copyDirectories.setIcon(errorIconCopyDirectory)
                errorIconCopyDirectory.description = "<html>${errors.joinToString(", ").replace(", ", "<br>")}</html>"
                copyDirectories.toolTipText = "<html>${errors.joinToString(", ").replace(", ", "<br>")}</html>"
            } else {
                copyDirectories.setIcon(null)
                copyDirectories.toolTipText =
                    Gui.createserverpack_gui_createserverpack_labelcopydirs_tip.toString()
                copyDirectories.foreground = getThemeTextColor()
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for server icon.
     *
     * @author Griefed
     */
    private fun validateServerIcon(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        runBlocking(Dispatchers.Default) {
            if (getServerIconPath().isNotEmpty()) {
                if (configurationHandler.checkIconAndProperties(getServerIconPath())) {
                    serverIconPath.setIcon(null)
                    serverIconPath.toolTipText =
                        Gui.createserverpack_gui_createserverpack_textfield_iconpath.toString()
                    serverIconPath.foreground = getThemeTextColor()
                    serverIconPreview.toolTipText =
                        Gui.createserverpack_gui_createserverpack_servericon_preview.toString()
                    try {
                        serverIconPreview.icon = ImageIcon(
                            withContext(Dispatchers.IO) {
                                ImageIO.read(
                                    File(getServerIconPath())
                                )
                            }.getScaledInstance(
                                48, 48, Image.SCALE_SMOOTH
                            )
                        )
                    } catch (ex: IOException) {
                        log.error("Error generating server icon preview.", ex)
                        errors.add(Gui.configuration_log_error_servericon_error.toString())
                    }
                } else {
                    serverIconPath.foreground = getThemeErrorColor()
                    serverIconPath.setIcon(errorIconServerIcon)
                    serverIconPath.toolTipText = errorIconServerIcon.description
                    serverIconPreview.icon = errorIconServerIconPreview
                    serverIconPreview.toolTipText =
                        Gui.createserverpack_gui_createserverpack_servericon_preview_none.toString()
                    errors.add(Gui.createserverpack_gui_createserverpack_servericon_preview_none.toString())
                }
            } else {
                try {
                    serverIconPreview.toolTipText =
                        Gui.createserverpack_gui_createserverpack_servericon_preview.toString()
                    serverIconPreview.icon = ImageIcon(
                        withContext(Dispatchers.IO) {
                            ImageIO.read(
                                apiProperties.defaultServerIcon
                            )
                        }.getScaledInstance(
                            48, 48, Image.SCALE_SMOOTH
                        )
                    )
                } catch (ex: IOException) {
                    log.error("Error generating server icon preview.", ex)
                    errors.add(Gui.configuration_log_error_servericon_error.toString())
                }
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the inputfield for server properties.
     *
     * @author Griefed
     */
    private fun validateServerProperties(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        runBlocking(Dispatchers.Default) {
            if (configurationHandler.checkIconAndProperties(getServerPropertiesPath())) {
                serverPropertiesPath.setIcon(null)
                serverPropertiesPath.toolTipText =
                    Gui.createserverpack_gui_createserverpack_textfield_propertiespath.toString()
                serverPropertiesPath.foreground = getThemeTextColor()
            } else {
                serverPropertiesPath.foreground = getThemeErrorColor()
                serverPropertiesPath.setIcon(errorIconServerProperties)
                serverPropertiesPath.toolTipText = errorIconServerProperties.description
                errors.add(Gui.configuration_log_error_serverproperties.toString())
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Sets the text of the Java args textarea to the popular Aikar flags.
     *
     * @author Griefed
     */
    override fun setAikarsFlagsAsJavaArguments() {
        if (getJavaArguments().isNotEmpty()) {
            when (JOptionPane.showConfirmDialog(
                serverPackPanel,
                Gui.createserverpack_gui_createserverpack_javaargs_confirm_message.toString(),
                Gui.createserverpack_gui_createserverpack_javaargs_confirm_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                issueIcon
            )) {
                0 -> setJavaArguments(apiProperties.aikarsFlags)
                1 -> {}
                else -> {}
            }
        } else {
            setJavaArguments(apiProperties.aikarsFlags)
        }
    }

    /**
     * If the checkbox is ticked and no Java is available, a message is displayed, warning the user
     * that Javapath needs to be defined for the modloader-server installation to work. If "Yes" is
     * clicked, a filechooser will open where the user can select their Java-executable/binary. If
     * "No" is selected, the user is warned about the consequences of not setting the
     * Javapath.<br></br><br></br>
     *
     *
     * If the installer for the current combination of the aforementioned versions can not be reached,
     * or is otherwise unavailable, the user is informed about SPC not being able to install it, thus
     * clearing the selection of the checkbox.
     *
     * Whenever the state of the server-installation checkbox changes, the global Java setting is
     * checked for validity, as well as the current combination of Minecraft version, modloader and
     * modloader version server-installer is available.<br></br><br></br>
     *
     *
     * If the checkbox is ticked and no Java is available, a message is displayed, warning the user
     * that Javapath needs to be defined for the modloader-server installation to work. If "Yes" is
     * clicked, a filechooser will open where the user can select their Java-executable/binary. If
     * "No" is selected, the user is warned about the consequences of not setting the
     * Javapath.<br></br><br></br>
     *
     *
     * If the installer for the current combination of the aforementioned versions can not be reached,
     * or is otherwise unavailable, the user is informed about SPC not being able to install it, thus
     * clearing the selection of the checkbox.
     *
     * @return `true` if, and only if, no problem was encountered.
     * @author Griefed
     */
    private fun checkServer(): Boolean {
        var okay = true
        if (isServerInstallationTicked()) {
            val mcVersion = minecraftVersions.selectedItem!!.toString()
            val modloader = modloaders.selectedItem!!.toString()
            val modloaderVersion = modloaderVersions.selectedItem!!.toString()
            if (!serverPackCreatorWindow.checkJava()) {
                this.setServerInstallationTicked(false)
                okay = false
            }
            if (!serverPackHandler.serverDownloadable(mcVersion, modloader, modloaderVersion)) {
                JOptionPane.showMessageDialog(
                    this, Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_message(
                        modloader, mcVersion, modloader, modloaderVersion, modloader
                    ) + "    ", Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                        mcVersion, modloader, modloaderVersion
                    ), JOptionPane.WARNING_MESSAGE, issueIcon
                )
                this.setServerInstallationTicked(false)
                okay = false
            }
        }
        return okay
    }

    /**
     * Adds a mouse listener to the passed button which sets the content area of said button
     * filled|not-filled when the mouse enters|leaves the button.
     *
     * @param buttonToAddMouseListenerTo JButton. The button to add the mouse listener to.
     * @author Griefed
     */
    private fun addMouseListenerContentAreaFilledToButton(buttonToAddMouseListenerTo: JButton) {
        buttonToAddMouseListenerTo.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(event: MouseEvent) {
                buttonToAddMouseListenerTo.isContentAreaFilled = true
            }

            override fun mouseExited(event: MouseEvent) {
                buttonToAddMouseListenerTo.isContentAreaFilled = false
            }
        })
    }

    /**
     * Setter for the Minecraft version depending on which one is selected in the GUI.
     *
     * @author Griefed
     */
    private fun updateMinecraftValues() {
        setModloaderVersionsModel(minecraftVersions.selectedItem!!.toString())
        updateRequiredJavaVersion()
        checkMinecraftServer()
    }

    private fun updateRequiredJavaVersion() {
        requiredJavaVersion.text = acquireRequiredJavaVersion()
        if (requiredJavaVersion.text != "?" && apiProperties.javaPath(requiredJavaVersion.text).isPresent && !scriptSettings.getData()["SPC_JAVA_SPC"].equals(
                apiProperties.javaPath(requiredJavaVersion.text).get()
            ) && apiProperties.isJavaScriptAutoupdateEnabled
        ) {
            val path = apiProperties.javaPath(requiredJavaVersion.text).get()
            val data: HashMap<String, String> = scriptSettings.getData()
            data.replace(
                "SPC_JAVA_SPC", path
            )
            scriptSettings.loadData(data)
            log.info("Automatically adjusted script variable SPC_JAVA_SPC to $path")
        }
    }

    override fun acquireRequiredJavaVersion(): String {
        return if (versionMeta.minecraft.getServer(getMinecraftVersion()).isPresent && versionMeta.minecraft.getServer(
                getMinecraftVersion()
            ).get().javaVersion().isPresent
        ) {
            versionMeta.minecraft.getServer(getMinecraftVersion()).get().javaVersion().get().toString()
        } else {
            "?"
        }
    }

    /**
     * Check whether the selected Minecraft version has a server available. If no server is available,
     * or no URL to download the server for the selected version is available, a warning is
     * displayed.
     *
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun checkMinecraftServer() {
        val mcVersion = minecraftVersions.selectedItem?.toString()
        val server = versionMeta.minecraft.getServer(mcVersion!!)
        if (!server.isPresent) {
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                issueIcon
            )
        } else if (server.isPresent && !server.get().url().isPresent) {
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_url_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                issueIcon
            )
        }
    }

    /**
     * Upon button-press, open a file-selector for the modpack-directory.
     *
     * @author Griefed
     */
    private fun selectModpackDirectory() {
        val modpackDirChooser = if (getModpackDirectory().isNotEmpty() && File(getModpackDirectory()).isDirectory) {
            JFileChooser(File(getModpackDirectory()))
        } else {
            JFileChooser(File(System.getProperty("user.home")))
        }
        if (File(getModpackDirectory()).isDirectory) {
            modpackDirChooser.currentDirectory = File(getModpackDirectory())
        } else if (File(getModpackDirectory()).isFile) {
            modpackDirChooser.currentDirectory = File(File(getModpackDirectory()).parent)
        } else {
            modpackDirChooser.currentDirectory = apiProperties.homeDirectory
        }
        modpackDirChooser.dialogTitle = Gui.createserverpack_gui_buttonmodpackdir_title.toString()
        modpackDirChooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
        modpackDirChooser.fileFilter = FileNameExtensionFilter(
            Gui.createserverpack_gui_createserverpack_chooser_modpack_filter.toString(), "zip"
        )
        modpackDirChooser.isAcceptAllFileFilterUsed = false
        modpackDirChooser.isMultiSelectionEnabled = false
        modpackDirChooser.preferredSize = chooserDimension
        if (modpackDirChooser.showOpenDialog(serverPackCreatorWindow) == JFileChooser.APPROVE_OPTION) {
            setModpackDirectory(modpackDirChooser.selectedFile.path)
            updateGuiFromSelectedModpack()
            log.debug(
                "Selected modpack directory: " + modpackDirChooser.selectedFile.path
            )
        }
    }

    /**
     * Scan the modpack directory for various manifests and, if any are found, parse them and try to
     * load the Minecraft version, modloader and modloader version.
     *
     * @author Griefed
     */
    private fun updateGuiFromSelectedModpack() {
        SwingUtilities.invokeLater {
            if (File(getModpackDirectory()).isDirectory) {
                try {
                    val packConfig = PackConfig()
                    var updated = false
                    if (File(getModpackDirectory(), "manifest.json").isFile) {
                        configurationHandler.updateConfigModelFromCurseManifest(
                            packConfig, File(
                                getModpackDirectory(), "manifest.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "minecraftinstance.json").isFile) {
                        configurationHandler.updateConfigModelFromMinecraftInstance(
                            packConfig, File(
                                getModpackDirectory(), "minecraftinstance.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "modrinth.index.json").isFile) {
                        configurationHandler.updateConfigModelFromModrinthManifest(
                            packConfig, File(
                                getModpackDirectory(), "modrinth.index.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "instance.json").isFile) {
                        configurationHandler.updateConfigModelFromATLauncherInstance(
                            packConfig, File(
                                getModpackDirectory(), "instance.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "config.json").isFile) {
                        configurationHandler.updateConfigModelFromConfigJson(
                            packConfig, File(
                                getModpackDirectory(), "config.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "mmc-pack.json").isFile) {
                        configurationHandler.updateConfigModelFromMMCPack(
                            packConfig, File(
                                getModpackDirectory(), "mmc-pack.json"
                            )
                        )
                        updated = true
                    }
                    val dirsToInclude = TreeSet(getCopyDirectoriesList())
                    val files = File(getModpackDirectory()).listFiles()
                    if (files != null && files.isNotEmpty()) {
                        for (file in files) {
                            if (apiProperties.directoriesToInclude.contains(file.name)) {
                                dirsToInclude.add(file.name)
                            }
                        }
                    }
                    if (updated) {
                        setMinecraftVersion(packConfig.minecraftVersion)
                        setModloader(packConfig.modloader)
                        setModloaderVersion(packConfig.modloaderVersion)
                        setCopyDirectories(ArrayList(dirsToInclude))
                        JOptionPane.showMessageDialog(
                            this,
                            Gui.createserverpack_gui_modpack_scan_message(
                                getMinecraftVersion(),
                                getModloader(),
                                getModloaderVersion(),
                                utilities.stringUtilities.buildString(dirsToInclude.toList())
                            ) + "   ",
                            Gui.createserverpack_gui_modpack_scan.toString(),
                            JOptionPane.INFORMATION_MESSAGE,
                            infoIcon
                        )
                    }
                } catch (ex: IOException) {
                    log.error("Couldn't update GUI from modpack manifests.", ex)
                }
            }
        }
    }

    /**
     * Upon button-press, open a file-selector for the server-icon.png.
     *
     * @author Griefed
     */
    private fun selectServerIcon() {
        val serverIconChooser = JFileChooser()
        serverIconChooser.currentDirectory = apiProperties.homeDirectory
        serverIconChooser.dialogTitle = Gui.createserverpack_gui_createserverpack_chooser_icon_title.toString()
        serverIconChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        serverIconChooser.fileFilter = FileNameExtensionFilter(
            Gui.createserverpack_gui_createserverpack_chooser_icon_filter.toString(), "png", "jpg", "jpeg", "bmp"
        )
        serverIconChooser.isAcceptAllFileFilterUsed = false
        serverIconChooser.isMultiSelectionEnabled = false
        serverIconChooser.preferredSize = chooserDimension
        if (serverIconChooser.showOpenDialog(serverPackCreatorWindow) == JFileChooser.APPROVE_OPTION) {
            setServerIconPath(serverIconChooser.selectedFile.absolutePath)
        }
    }

    /**
     * Upon button-press, open a file-selector for the server.properties.
     *
     * @author Griefed
     */
    private fun selectServerProperties() {
        val serverPropertiesChooser = JFileChooser()
        serverPropertiesChooser.currentDirectory = apiProperties.homeDirectory
        serverPropertiesChooser.dialogTitle =
            Gui.createserverpack_gui_createserverpack_chooser_properties_title.toString()
        serverPropertiesChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        serverPropertiesChooser.fileFilter = FileNameExtensionFilter(
            Gui.createserverpack_gui_createserverpack_chooser_properties_filter.toString(), "properties"
        )
        serverPropertiesChooser.isAcceptAllFileFilterUsed = false
        serverPropertiesChooser.isMultiSelectionEnabled = false
        serverPropertiesChooser.preferredSize = chooserDimension
        if (serverPropertiesChooser.showOpenDialog(serverPackCreatorWindow) == JFileChooser.APPROVE_OPTION) {
            setServerPropertiesPath(serverPropertiesChooser.selectedFile.absolutePath)
        }
    }

    /**
     * Upon button-press, open a file-selector for clientside-only mods. If the modpack-directory is
     * specified, the file-selector will open in the mods-directory in the modpack-directory.
     *
     * @author Griefed
     */
    private fun selectClientMods() {
        val clientModsChooser = JFileChooser()
        if (getModpackDirectory().isNotEmpty() && File(getModpackDirectory()).isDirectory && File(
                getModpackDirectory(),
                "mods"
            ).isDirectory
        ) {
            clientModsChooser.currentDirectory = File(getModpackDirectory(), "mods")
        } else {
            clientModsChooser.currentDirectory = apiProperties.homeDirectory
        }
        clientModsChooser.dialogTitle = Gui.createserverpack_gui_buttonclientmods_title.toString()
        clientModsChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        clientModsChooser.fileFilter = FileNameExtensionFilter(
            Gui.createserverpack_gui_buttonclientmods_filter.toString(), " jar "
        )
        clientModsChooser.isAcceptAllFileFilterUsed = false
        clientModsChooser.isMultiSelectionEnabled = true
        clientModsChooser.preferredSize = chooserDimension
        if (clientModsChooser.showOpenDialog(serverPackCreatorWindow) == JFileChooser.APPROVE_OPTION) {
            val clientMods: Array<File> = clientModsChooser.selectedFiles
            val clientModsFilenames: MutableList<String> = ArrayList(100)
            for (mod in clientMods) {
                clientModsFilenames.add(mod.name)
            }
            this.setClientSideMods(clientModsFilenames)
            log.debug("Selected mods: $clientModsFilenames")
        }
    }

    /**
     * Upon button-press, open a file-selector for directories which are to be copied to the server
     * pack. If the modpack-directory is specified, the file-selector will be opened in the
     * modpack-directory.
     *
     * @author Griefed
     */
    private fun selectCopyDirs() {
        val copyDirsChooser = JFileChooser()
        if (getModpackDirectory().isNotEmpty() && File(getModpackDirectory()).isDirectory) {
            copyDirsChooser.currentDirectory = File(getModpackDirectory())
        } else {
            copyDirsChooser.currentDirectory = apiProperties.homeDirectory
        }
        copyDirsChooser.dialogTitle = Gui.createserverpack_gui_buttoncopydirs_title.toString()
        copyDirsChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        copyDirsChooser.isAcceptAllFileFilterUsed = false
        copyDirsChooser.isMultiSelectionEnabled = true
        copyDirsChooser.preferredSize = chooserDimension
        if (copyDirsChooser.showOpenDialog(serverPackCreatorWindow) == JFileChooser.APPROVE_OPTION) {
            val directoriesToCopy: Array<File> = copyDirsChooser.selectedFiles
            val copyDirsNames: MutableList<String> = ArrayList(100)
            for (directory in directoriesToCopy) {
                copyDirsNames.add(directory.name)
            }
            setCopyDirectories(copyDirsNames)
            log.debug("Selected directories: $copyDirsNames")
        }
    }

    /**
     * Upon button-press, check the entered configuration and if successfull, generate a server pack.
     *
     * @author Griefed
     */
    private fun generateServerpack() {
        buttonGenerateServerPack.isEnabled = false
        larsonScanner.loadConfig(generatingConfig)
        var decision = 0
        materialTextPaneUI.installUI(lazyModeTextPane)
        if (getCopyDirectories() == "lazy_mode") {
            decision = JOptionPane.showConfirmDialog(
                serverPackCreatorWindow,
                lazyModeTextPane,
                Gui.createserverpack_gui_createserverpack_lazymode.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        log.debug("Case $decision")
        when (decision) {
            0 -> generate()
            else -> ready()
        }
    }

    /**
     * Generate a server pack from the current configuration in the GUI.
     *
     * @author Griefed
     */
    private fun generate() {
        log.info("Checking entered configuration.")
        updateStatus(Gui.createserverpack_log_info_buttoncreateserverpack_start.toString())
        val executorService: ExecutorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val packConfig: PackConfig = this.getCurrentConfiguration()
            if (!checkServer()) {
                packConfig.isServerInstallationDesired = false
            }
            val encounteredErrors: MutableList<String> = ArrayList(100)
            if (!configurationHandler.checkConfiguration(
                    packConfig, encounteredErrors, true
                )
            ) {
                log.info("Configuration checked successfully.")
                updateStatus(
                    Gui.createserverpack_log_info_buttoncreateserverpack_checked.toString()
                )
                packConfig.save(apiProperties.defaultConfig)
                log.info("Starting ServerPackCreator run.")
                updateStatus(Gui.createserverpack_log_info_buttoncreateserverpack_generating.toString())
                try {
                    serverPackHandler.run(packConfig)
                    loadConfig(apiProperties.defaultConfig)
                    updateStatus(
                        Gui.createserverpack_log_info_buttoncreateserverpack_ready.toString()
                    )
                    serverPackGeneratedDocument.setParagraphAttributes(
                        0, serverPackGeneratedDocument.length, serverPackGeneratedAttributeSet, false
                    )
                    materialTextPaneUI.installUI(serverPackGeneratedTextPane)
                    ready()
                    if (JOptionPane.showConfirmDialog(
                            serverPackCreatorWindow,
                            serverPackGeneratedTextPane,
                            Gui.createserverpack_gui_createserverpack_openfolder_title.toString(),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            infoIcon
                        ) == 0
                    ) {
                        try {
                            Desktop.getDesktop().open(File(serverPackHandler.getServerPackDestination(packConfig)))
                        } catch (ex: IOException) {
                            log.error("Error opening file explorer for server pack.", ex)
                        }
                    }
                } catch (ex: Exception) {
                    log.error("An error occurred when generating the server pack.", ex)
                }
            } else {
                updateStatus(Gui.createserverpack_gui_buttongenerateserverpack_fail.toString())
                if (encounteredErrors.isNotEmpty()) {
                    val errors = StringBuilder()
                    for (i in encounteredErrors.indices) {
                        errors.append(i + 1).append(": ").append(encounteredErrors[i]).append("    ").append("\n")
                    }
                    ready()
                    JOptionPane.showMessageDialog(
                        serverPackCreatorWindow,
                        errors,
                        Gui.createserverpack_gui_createserverpack_errors_encountered(encounteredErrors.size),
                        JOptionPane.ERROR_MESSAGE,
                        issueIcon
                    )
                }
            }
            encounteredErrors.clear()
            ready()
            executorService.shutdownNow()
        }
    }

    /**
     * Set the GUI ready for the next generation.
     *
     * @author Griefed
     */
    private fun ready() {
        buttonGenerateServerPack.isEnabled = true
        larsonScanner.loadConfig(idleConfig)
    }

    /**
     * Save the current configuration to a specified file.
     *
     * @param configFile The file to store the configuration under.
     * @author Griefed
     */
    fun saveConfig(configFile: File) {
        lastLoadedConfiguration = this.getCurrentConfiguration()
        lastLoadedConfiguration!!.save(configFile)
    }

    /**
     * Acquire the current settings in the GUI as a [PackConfig].
     *
     * @return The current settings in the GUI.
     * @author Griefed
     */
    override fun getCurrentConfiguration(): PackConfig {
        return PackConfig(
            getClientSideModsList(),
            getCopyDirectoriesList(),
            getModpackDirectory(),
            getMinecraftVersion(),
            getModloader(),
            getModloaderVersion(),
            getJavaArguments(),
            getServerPackSuffix(),
            getServerIconPath(),
            getServerPropertiesPath(),
            isServerInstallationTicked(),
            isServerIconInclusionTicked(),
            isServerPropertiesInclusionTicked(),
            isZipArchiveCreationTicked(),
            getScriptSettings(),
            getConfigPanelConfigs()
        )
    }

    override fun saveCurrentConfiguration(): File {
        return File("")
    }

    /**
     * Get the list of clientside-only mods to exclude from the server pack.
     *
     * @return The list of clientside-only mods to exclude from the server pack.
     * @author Griefed
     */
    override fun getClientSideModsList(): MutableList<String> {
        return utilities.listUtilities.cleanList(this.getClientSideMods().split(",").dropLastWhile { it.isEmpty() }
            .toMutableList())
    }

    /**
     * Get the list of files and directories to include in the server pack.
     *
     * @return The list of files and directories to include in the server pack.
     * @author Griefed
     */
    override fun getCopyDirectoriesList(): MutableList<String> {
        return utilities.listUtilities.cleanList(getCopyDirectories().split(",").dropLastWhile { it.isEmpty() }
            .toMutableList())
    }

    /**
     * Get the modpack directory.
     *
     * @return The modpack directory.
     * @author Griefed
     */
    override fun getModpackDirectory(): String {
        return modpackDirectory.text
    }

    /**
     * Get the Minecraft version currently selected in the GUI.
     *
     * @return The Minecraft version currently selected in the GUI.
     * @author Griefed
     */
    override fun getMinecraftVersion(): String {
        return minecraftVersions.selectedItem!!.toString()
    }

    /**
     * Set the Minecraft version selected in the GUI. If the specified version is not available in the
     * current combobox model, nothing happens.
     *
     * @param version The Minecraft version to select.
     * @author Griefed
     */
    override fun setMinecraftVersion(version: String) {
        for (i in 0 until minecraftVersions.model.size) {
            if (minecraftVersions.model.getElementAt(i) == version) {
                minecraftVersions.selectedIndex = i
                break
            }
        }
    }

    /**
     * Get the modloader selected in the GUI.
     *
     * @return The modloader selected in the GUI.
     * @author Griefed
     */
    override fun getModloader(): String {
        return modloaders.selectedItem!!.toString()
    }

    /**
     * Set the modloader selected in the GUI. If the specified modloader is not viable, nothing
     * happens.
     *
     * @param modloader The modloader to select.
     * @author Griefed
     */
    override fun setModloader(modloader: String) {
        when (modloader) {
            "Fabric" -> modloaders.setSelectedIndex(0)
            "Forge" -> modloaders.setSelectedIndex(1)
            "Quilt" -> modloaders.setSelectedIndex(2)
            "LegacyFabric" -> modloaders.setSelectedIndex(3)
        }
        setModloaderVersionsModel()
    }

    /**
     * Get the modloader version selected in the GUI.
     *
     * @return The modloader version selected in the GUI.
     * @author Griefed
     */
    override fun getModloaderVersion(): String {
        return modloaderVersions.selectedItem!!.toString()
    }

    /**
     * Getter for the currently set JVM flags / Java args.
     *
     * @return Returns the currently set JVM flags / Java args.
     * @author Griefed
     */
    override fun getJavaArguments(): String {
        return javaArgs.text
    }

    /**
     * Setter for the JVM flags / Java args.
     *
     * @param javaArguments The javaargs to set.
     * @author Griefed
     */
    override fun setJavaArguments(javaArguments: String) {
        javaArgs.text = javaArguments
    }

    /**
     * Get the server pack suffix text.
     *
     * @return The server pack suffix text.
     * @author Griefed
     */
    override fun getServerPackSuffix(): String {
        return utilities.stringUtilities.pathSecureText(serverPackSuffix.text)
    }

    /**
     * Getter for the text in the custom server-icon textfield.
     *
     * @return Returns the text in the server-icon textfield.
     * @author Griefed
     */
    override fun getServerIconPath(): String {
        return serverIconPath.text
    }

    /**
     * Getter for the text in the custom server-icon textfield.
     *
     * @param path The path to the server icon file.
     * @author Griefed
     */
    override fun setServerIconPath(path: String) {
        serverIconPath.text = path
    }

    /**
     * Getter for the text in the custom server.properties textfield
     *
     * @return Returns the text in the server.properties textfield.
     * @author Griefed
     */
    override fun getServerPropertiesPath(): String {
        return serverPropertiesPath.text
    }

    override fun isMinecraftServerAvailable(): Boolean {
        return versionMeta.minecraft.isServerAvailable(minecraftVersions.selectedItem!!.toString())
    }

    /**
     * Getter for the text in the custom server.properties textfield
     *
     * @param path The path to the server properties file.
     * @author Griefed
     */
    override fun setServerPropertiesPath(path: String) {
        serverPropertiesPath.text = path
    }

    /**
     * Is the modloader server installation desired?
     *
     * @return `true` if it is.
     * @author Griefed
     */
    override fun isServerInstallationTicked(): Boolean {
        return server.isSelected
    }

    /**
     * Is the inclusion of a server-icon.png desired?
     *
     * @return `true` if it is.
     * @author Griefed
     */
    override fun isServerIconInclusionTicked(): Boolean {
        return icon.isSelected
    }

    /**
     * Is the inclusion of a server.properties-file desired?
     *
     * @return `true` if it is.
     * @author Griefed
     */
    override fun isServerPropertiesInclusionTicked(): Boolean {
        return properties.isSelected
    }

    /**
     * Is the creation of a server pack ZIP-archive desired?
     *
     * @return `true` if it is.
     * @author Griefed
     */
    override fun isZipArchiveCreationTicked(): Boolean {
        return zip.isSelected
    }

    /**
     * Get a hashmap of the data available in the script variables table.
     *
     * @return A map containig all placeholders with their respective values.
     * @author Griefed
     */
    override fun getScriptSettings(): HashMap<String, String> {
        return scriptSettings.getData()
    }

    /**
     * Get the configurations of the available ExtensionConfigPanel as a hashmap, so we can store them
     * in our serverpackcreator.conf.
     *
     * @return Map containing lists of CommentedConfigs mapped to the corresponding pluginID.
     */
    private fun getConfigPanelConfigs(): HashMap<String, ArrayList<CommentedConfig>> {
        val configs: HashMap<String, ArrayList<CommentedConfig>> = HashMap<String, ArrayList<CommentedConfig>>(10)
        if (configPanels.isNotEmpty()) {
            for (panel in configPanels) {
                configs[panel.pluginID] = panel.serverPackExtensionConfig()
            }
        }
        return configs
    }

    /**
     * Get the list of clientside-only mods to exclude from the server pack.
     *
     * @return The list of clientside-only mods to exclude from the server pack.
     * @author Griefed
     */
    override fun getClientSideMods(): String {
        return clientsideMods.text.replace(", ", ",")
    }

    /**
     * Set the list of clientside-only mods to exclude from the server pack.
     *
     * @param entries the list of clientside-only mods.
     * @author Griefed
     */
    override fun setClientSideMods(entries: MutableList<String>) {
        clientsideMods.text = utilities.stringUtilities.buildString(entries)
    }

    /**
     * Get the list of files and directories to include in the server pack.
     *
     * @return The list of files and directories to include in the server pack.
     * @author Griefed
     */
    override fun getCopyDirectories(): String {
        return copyDirectories.text.replace(", ", ",")
    }

    /**
     * Set the list of files and directories to include in the server pack.
     *
     * @param entries The list of files and directories to include in the server pack.
     * @author Griefed
     */
    override fun setCopyDirectories(entries: MutableList<String>) {
        copyDirectories.text = utilities.stringUtilities.buildString(entries.toString())
    }

    /**
     * Set the server pack suffix text.
     *
     * @param suffix The suffix to append to the server pack folder and ZIP-archive.
     * @author Griefed
     */
    override fun setServerPackSuffix(suffix: String) {
        serverPackSuffix.text = utilities.stringUtilities.pathSecureText(suffix)
    }

    /**
     * Set the modloader version selected in the GUI. If the specified version is not available in the
     * currently set modloader version combobox model, nothing happens.
     *
     * @param version The modloader version to select.
     * @author Griefed
     */
    override fun setModloaderVersion(version: String) {
        for (i in 0 until modloaderVersions.model.size) {
            if (modloaderVersions.model.getElementAt(i) == version) {
                modloaderVersions.selectedIndex = i
                break
            }
        }
    }

    /**
     * Set the modpack directory.
     *
     * @param directory The directory which holds the modpack.
     * @author Griefed
     */
    override fun setModpackDirectory(directory: String) {
        modpackDirectory.text = directory
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into
     * ServerPackCreator.
     *
     * @param configFile The configuration file to parse and load into the GUI.
     * @author Griefed
     */
    fun loadConfig(configFile: File) {
        try {
            val packConfig = PackConfig(utilities, configFile)
            lastLoadedConfiguration = packConfig
            setModpackDirectory(packConfig.modpackDir)
            if (packConfig.clientMods.isEmpty()) {
                this.setClientSideMods(apiProperties.clientSideMods())
                log.debug("Set clientMods with fallback list.")
            } else {
                this.setClientSideMods(packConfig.clientMods)
            }
            if (packConfig.copyDirs.isEmpty()) {
                setCopyDirectories(mutableListOf("mods", "config"))
            } else {
                setCopyDirectories(packConfig.copyDirs)
            }
            setScriptVariables(packConfig.scriptSettings)
            setServerIconPath(packConfig.serverIconPath)
            setServerPropertiesPath(packConfig.serverPropertiesPath)
            if (packConfig.minecraftVersion.isEmpty()) {
                packConfig.minecraftVersion = versionMeta.minecraft.latestRelease().version
            }
            setMinecraftVersion(packConfig.minecraftVersion)
            setModloader(packConfig.modloader)
            setModloaderVersion(packConfig.modloaderVersion)
            this.setServerInstallationTicked(packConfig.isServerInstallationDesired)
            this.setIconInclusionTicked(packConfig.isServerIconInclusionDesired)
            this.setPropertiesInclusionTicked(packConfig.isServerPropertiesInclusionDesired)
            this.setZipArchiveCreationTicked(packConfig.isZipCreationDesired)
            setJavaArguments(packConfig.javaArgs)
            this.setServerPackSuffix(packConfig.serverPackSuffix)
            for (panel in configPanels) {
                panel.setServerPackExtensionConfig(packConfig.getPluginConfigs(panel.pluginID))
            }
        } catch (ex: Exception) {
            log.error("Couldn't load configuration file.", ex)
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_config_load_error_message.toString() + " " + ex.cause + "   ",
                Gui.createserverpack_gui_config_load_error.toString(),
                JOptionPane.ERROR_MESSAGE,
                issueIcon
            )
        }
    }

    /**
     * Load default values for textfields so the user can start with a new configuration. Just as if
     * ServerPackCreator was started without a serverpackcreator.conf being present.
     *
     * @author Griefed
     */
    fun clearInterface() {
        setModpackDirectory("")
        this.setServerPackSuffix("")
        this.setClientSideMods(apiProperties.clientSideMods())
        setCopyDirectories(apiProperties.directoriesToInclude.toMutableList())
        setServerIconPath("")
        setServerPropertiesPath("")
        minecraftVersions.selectedIndex = 0
        setModloader("Forge")
        this.setServerInstallationTicked(true)
        this.setIconInclusionTicked(true)
        this.setPropertiesInclusionTicked(true)
        this.setZipArchiveCreationTicked(true)
        setJavaArguments("")
        clearScriptVariables()
        for (panel in configPanels) {
            panel.clear()
        }
    }

    /**
     * Change the selection of the server installation checkbox.
     *
     * @param ticked Whether the checkbox should be ticked.
     * @author Griefed
     */
    override fun setServerInstallationTicked(ticked: Boolean) {
        server.isSelected = ticked
    }

    /**
     * Change the selection of the server icon checkbox.
     *
     * @param ticked Whether the checkbox should be ticked.
     * @author Griefed
     */
    override fun setIconInclusionTicked(ticked: Boolean) {
        icon.isSelected = ticked
    }

    /**
     * Change the selection of the server properties checkbox.
     *
     * @param ticked Whether the checkbox should be ticked.
     * @author Griefed
     */
    override fun setPropertiesInclusionTicked(ticked: Boolean) {
        properties.isSelected = ticked
    }

    /**
     * Change the selection of the server ZIP-archive checkbox.
     *
     * @param ticked Whether the checkbox should be ticked.
     * @author Griefed
     */
    override fun setZipArchiveCreationTicked(ticked: Boolean) {
        zip.isSelected = ticked
    }

    /**
     * Clear any available data in the script variables table.
     */
    override fun clearScriptVariables() {
        scriptSettings.clearData()
    }

    /**
     * Set the modloader version combobox model depending on the currently selected modloader and
     * Minecraft version.
     *
     * @author Griefed
     */
    private fun setModloaderVersionsModel() {
        setModloaderVersionsModel(
            minecraftVersions.selectedItem!!.toString()
        )
    }

    /**
     * Set the modloader version combobox model depending on the currently selected modloader, with
     * the specified Minecraft version.
     *
     * @param minecraftVersion The Minecraft version to work with in the GUI update.
     * @author Griefed
     */
    private fun setModloaderVersionsModel(minecraftVersion: String) = when (modloaders.selectedIndex) {
        0 -> {
            if (versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
                modloaderVersions.setModel(fabricVersions)
            } else {
                modloaderVersions.setModel(noVersions)
            }
        }

        1 -> {
            if (versionMeta.forge.supportedForgeVersionsDescendingArray(minecraftVersion).isPresent) {
                modloaderVersions.setModel(
                    DefaultComboBoxModel(
                        versionMeta.forge.supportedForgeVersionsDescendingArray(minecraftVersion).get()
                    )
                )
            } else {
                modloaderVersions.setModel(noVersions)
            }
        }

        2 -> {
            if (versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
                modloaderVersions.setModel(quiltVersions)
            } else {
                modloaderVersions.setModel(noVersions)
            }
        }

        3 -> if (versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) {
            modloaderVersions.setModel(legacyFabricVersions)
        } else {
            modloaderVersions.setModel(noVersions)
        }

        else -> {}
    }


    /**
     * Get the current themes error-text colour.
     *
     * @return The current themes error-text colour.
     * @author Griefed
     */
    private fun getThemeErrorColor(): Color {
        return if (apiProperties.isDarkTheme()) {
            darkTheme.textErrorColour
        } else {
            lightTheme.textErrorColour
        }
    }

    /**
     * The current themes default text colour.
     *
     * @return The current themes default text colour.
     * @author Griefed
     */
    private fun getThemeTextColor(): Color {
        return if (apiProperties.isDarkTheme()) {
            darkTheme.textColor
        } else {
            lightTheme.textColor
        }
    }

    /**
     * Load the hashmap into the script variables table. Before the data is loaded, all currently
     * available data in the table is cleared. Use with caution.
     *
     * @param variables The new map of placeholder-value pairs to set the table to.
     * @author Griefed
     */
    override fun setScriptVariables(variables: HashMap<String, String>) {
        scriptSettings.loadData(variables)
    }
}