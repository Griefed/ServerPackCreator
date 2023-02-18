package de.griefed.serverpackcreator.gui.window.configs

import Gui
import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.*
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.components.*
import de.griefed.serverpackcreator.gui.window.components.interactivetable.InteractiveTable
import kotlinx.coroutines.*
import net.java.balloontip.BalloonTip
import net.java.balloontip.styles.EdgedBalloonStyle
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.Timer
import javax.swing.event.DocumentEvent

class ConfigEditorPanel(
    private val guiProps: GuiProps, private val configsTab: ConfigsTab,
    private val configurationHandler: ConfigurationHandler,
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities,
    private val serverPackHandler: ServerPackHandler,
    apiPlugins: ApiPlugins,
    private val showBrowser: ActionListener
) : JPanel(
    MigLayout(
        "left,wrap",
        "[left,::64]5[left]5[left,grow]5[left,::64]5[left,::64]", "30"
    )
), ServerPackConfigTab {
    private val log = cachedLoggerOf(this.javaClass)
    private val whitespace = "^.*,\\s*\\\\*$".toRegex()
    private val modpackInfo = JLabel(guiProps.infoIcon)
    private val propertiesInfo = JLabel(guiProps.infoIcon)
    private val iconInfo = JLabel(guiProps.infoIcon)
    private val filesInfo = JLabel(guiProps.infoIcon)
    private val suffixInfo = JLabel(guiProps.infoIcon)
    private val minecraftInfo = JLabel(guiProps.infoIcon)
    private val javaVersionInfo = JLabel(guiProps.infoIcon)
    private val modloaderInfo = JLabel(guiProps.infoIcon)
    private val modloaderVersionInfo = JLabel(guiProps.infoIcon)
    private val includeIconInfo = JLabel(guiProps.infoIcon)
    private val includeZIPInfo = JLabel(guiProps.infoIcon)
    private val includePropertiesInfo = JLabel(guiProps.infoIcon)
    private val prepareServerInfo = JLabel(guiProps.infoIcon)
    private val iconPreview = JLabel(guiProps.serverIcon)
    private val exclusionsInfo = JLabel(guiProps.infoIcon)
    private val argumentsInfo = JLabel(guiProps.infoIcon)
    private val scriptInfo = JLabel(guiProps.infoIcon)
    private val modpackBrowser = JButton(guiProps.folderIcon)
    private val modpackInspect = JButton(guiProps.inspectIcon)
    private val propertiesBrowser = JButton(guiProps.folderIcon)
    private val propertiesOpen = JButton(guiProps.openIcon)
    private val iconBrowser = JButton(guiProps.folderIcon)
    private val serverPackFilesRevert = JButton(guiProps.revertIcon)
    private val serverPackFilesBrowser = JButton(guiProps.folderIcon)
    private val serverPackFilesReset = JButton(guiProps.resetIcon)
    private val javaVersion = ElementLabel("8", 16)
    private val serverPackSuffix = JTextField("")
    private val includeIcon = JCheckBox(Gui.createserverpack_gui_createserverpack_checkboxicon.toString())
    private val includeZip = JCheckBox(Gui.createserverpack_gui_createserverpack_checkboxzip.toString())
    private val includeProperties = JCheckBox(Gui.createserverpack_gui_createserverpack_checkboxproperties.toString())
    private val prepareServer = JCheckBox(Gui.createserverpack_gui_createserverpack_checkboxserver.toString())
    private val noVersions =
        DefaultComboBoxModel(arrayOf(Gui.createserverpack_gui_createserverpack_forge_none.toString()))
    private val legacyFabricVersions = DefaultComboBoxModel(versionMeta.legacyFabric.loaderVersionsArrayDescending())
    private val fabricVersions = DefaultComboBoxModel(versionMeta.fabric.loaderVersionsArrayDescending())
    private val quiltVersions = DefaultComboBoxModel(versionMeta.quilt.loaderVersionsArrayDescending())
    private val propertiesQuickSelect = JComboBox<File>()
    private val iconQuickSelect = JComboBox<File>()
    private val minecraftVersions = JComboBox<String>()
    private val modloaders = JComboBox<String>()
    private val modloaderVersions = JComboBox<String>()
    private val exclusionsRevert = JButton(guiProps.revertIcon)
    private val exclusionsBrowser = JButton(guiProps.folderIcon)
    private val exclusionsReset = JButton(guiProps.resetIcon)
    private val scriptKVPairsRevert = JButton(guiProps.revertIcon)
    private val scriptKVPairsReset = JButton(guiProps.resetIcon)
    private val aikarsFlags = JButton()
    private val modpackDirectory = FileTextField("")
    private val propertiesFile = FileTextField(apiProperties.defaultServerProperties)
    private val iconFile = FileTextField(apiProperties.defaultServerIcon)
    private val serverPackFiles = ScrollTextArea("config,mods")
    private val exclusions = ScrollTextArea(apiProperties.clientSideMods())
    private val startArgs = ScrollTextArea("-Xmx4G -Xms4G")
    private val scriptKVPairs = InteractiveTable()
    private val advancedSettingsPanel = AdvancedSettingsPanel(
        exclusionsInfo,
        argumentsInfo,
        scriptInfo,
        exclusions,
        exclusionsRevert,
        exclusionsBrowser,
        exclusionsReset,
        startArgs,
        aikarsFlags,
        scriptKVPairs,
        scriptKVPairsRevert,
        scriptKVPairsReset
    )
    val pluginPanels: MutableList<ExtensionConfigPanel> = apiPlugins.getConfigPanels(this).toMutableList()
    private val pluginsSettingsPanel = PluginsSettingsPanel(pluginPanels)
    private val collapsibleAdvanced = CollapsiblePanel(
        Gui.createserverpack_gui_advanced.toString(),
        advancedSettingsPanel
    )
    private val collapsiblePlugins = CollapsiblePanel(
        Gui.createserverpack_gui_plugins.toString(),
        pluginsSettingsPanel
    )
    var lastLoadedConfiguration: PackConfig? = null
    val title = ConfigEditorTitle(guiProps, configsTab, this)
    private val timer : Timer = Timer(100,null)
    private val iconPreviewBig = JLabel()
    private val balloonTip = BalloonTip(
        iconPreview,
        iconPreviewBig,
        guiProps.balloonStyle,
        false
    )

    @OptIn(DelicateCoroutinesApi::class)
    private val modpackChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            GlobalScope.launch(Dispatchers.Default) {
                title.titleLabel.text = modpackDirectory.file.name
                validateInputFields()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val suffixChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            GlobalScope.launch(Dispatchers.Default) {
                validateInputFields()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val exclusionChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            GlobalScope.launch(Dispatchers.Default) {
                validateInputFields()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val serverPackFilesChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            GlobalScope.launch(Dispatchers.Default) {
                validateInputFields()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val iconChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            GlobalScope.launch(Dispatchers.Default) {
                validateInputFields()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val propertyChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            GlobalScope.launch(Dispatchers.Default) {
                validateInputFields()
            }
        }
    }

    init {
        timer.stop()
        timer.addActionListener {
            timer.stop()
            val errors = mutableListOf<String>()
            runBlocking(Dispatchers.IO) {
                launch {
                    errors.addAll(validateModpackDir())
                }
                launch {
                    errors.addAll(validateSuffix())
                }
                launch {
                    errors.addAll(validateExclusions())
                }
                launch {
                    errors.addAll(validateServerPackFiles())
                }
                launch {
                    errors.addAll(validateServerIcon())
                }
                launch {
                    errors.addAll(validateServerProperties())
                }
                launch {
                    if (!checkServer()) {
                        errors.add(
                            Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                                minecraftVersions.selectedItem!!.toString(),
                                modloaders.selectedItem!!.toString(),
                                modloaderVersions.selectedItem!!.toString()
                            )
                        )
                    }
                }
                launch {
                    if (modloaderVersions.selectedItem!! == Gui.createserverpack_gui_createserverpack_forge_none.toString()) {
                        errors.add(Gui.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader()))
                    }
                }
            }
            if (errors.isEmpty()) {
                title.clearErrorIcon()
            } else {
                title.setErrorIcon("<html>${errors.joinToString("<br>")}</html>")
            }
        }

        initListeners()
        initTooltips()
        initAikarsButton()
        initComboboxes()

        // Modpack directory
        add(modpackInfo, "cell 0 0,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labelmodpackdir.toString()), "cell 1 0,grow")
        add(modpackDirectory, "cell 2 0,grow")
        add(modpackBrowser, "cell 3 0, h 30!,w 30!")
        add(modpackInspect, "cell 4 0")

        // Server Properties
        add(propertiesInfo, "cell 0 1,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labelpropertiespath.toString()), "cell 1 1,grow")
        add(propertiesFile, "cell 2 1, split 3,grow")
        add(ElementLabel(Gui.createserverpack_gui_quickselect.toString()), "cell 2 1")
        add(propertiesQuickSelect, "cell 2 1,w 200!")
        add(propertiesBrowser, "cell 3 1")
        add(propertiesOpen, "cell 4 1")

        // Server Icon
        add(iconInfo, "cell 0 2,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labeliconpath.toString()), "cell 1 2,grow")
        add(iconFile, "cell 2 2, split 2,grow")
        add(ElementLabel(Gui.createserverpack_gui_quickselect.toString()), "cell 2 2")
        add(iconQuickSelect, "cell 2 2,w 200!")
        add(iconBrowser, "cell 3 2")
        add(iconPreview, "cell 4 2")

        // Server Files
        add(filesInfo, "cell 0 3 1 3,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labelcopydirs.toString()), "cell 1 3 1 3,grow")
        add(serverPackFiles, "cell 2 3 1 3,grow,w 10:500:,h 100!")
        add(serverPackFilesRevert, "cell 3 3 2 1, h 30!, aligny center, alignx center,growx")
        add(serverPackFilesBrowser, "cell 3 4 2 1, h 30!, aligny center, alignx center,growx")
        add(serverPackFilesReset, "cell 3 5 2 1, h 30!, aligny top, alignx center,growx")

        // Server Pack Suffix
        add(suffixInfo, "cell 0 6,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labelsuffix.toString()), "cell 1 6,grow")
        add(serverPackSuffix, "cell 2 6,grow")

        // Minecraft Version
        add(minecraftInfo, "cell 0 7,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labelminecraft.toString()), "cell 1 7,grow")
        add(minecraftVersions, "cell 2 7,w 200!")
        // Java Version Of Minecraft Version
        add(javaVersionInfo, "cell 2 7, w 40!, gapleft 40")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_minecraft_java.toString(), 16), "cell 2 7")
        add(javaVersion, "cell 2 7, w 40!")

        // Modloader
        add(modloaderInfo, "cell 0 8,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labelmodloader.toString()), "cell 1 8,grow")
        add(modloaders, "cell 2 8,w 200!")
        // Include Server Icon
        add(includeIconInfo, "cell 2 8, w 40!, gapleft 40,grow")
        add(includeIcon, "cell 2 8, w 200!")
        //Create ZIP Archive
        add(includeZIPInfo, "cell 2 8, w 40!,grow")
        add(includeZip, "cell 2 8, w 200!")

        //Modloader Version
        add(modloaderVersionInfo, "cell 0 9,grow")
        add(ElementLabel(Gui.createserverpack_gui_createserverpack_labelmodloaderversion.toString()), "cell 1 9,grow")
        add(modloaderVersions, "cell 2 9,w 200!")
        //Include Server Properties
        add(includePropertiesInfo, "cell 2 9, w 40!, gapleft 40,grow")
        add(includeProperties, "cell 2 9, w 200!")
        //Install Local Server
        add(prepareServerInfo, "cell 2 9, w 40!,grow")
        add(prepareServer, "cell 2 9, w 200!")

        // Advanced Settings
        add(collapsibleAdvanced, "cell 0 10 5,grow")

        // Plugins
        if (pluginPanels.isNotEmpty()) {
            add(collapsiblePlugins, "cell 0 11 5,grow")
        }
        validateInputFields()
    }

    /**
     * TODO docs
     */
    private fun initAikarsButton() {
        val parts = Gui.createserverpack_gui_createserverpack_javaargs_aikar.toString().split(" ")
        val flags = mutableListOf<TextIcon>()
        for (part in parts) {
            flags.add(TextIcon(aikarsFlags, part))
        }
        aikarsFlags.icon = CompoundIcon(
            flags.toTypedArray(),
            5,
            CompoundIcon.Axis.Y_AXIS
        )
    }

    /**
     * TODO docs
     */
    private fun initComboboxes() {
        if (apiProperties.isMinecraftPreReleasesAvailabilityEnabled) {
            minecraftVersions.setModel(DefaultComboBoxModel(versionMeta.minecraft.allVersionsArrayDescending()))
        } else {
            minecraftVersions.setModel(DefaultComboBoxModel(versionMeta.minecraft.releaseVersionsArrayDescending()))
        }
        if (minecraftVersions.selectedItem == null) {
            minecraftVersions.selectedIndex = 0
        }
        modloaders.model = DefaultComboBoxModel(apiProperties.supportedModloaders)
        if (modloaders.selectedItem == null) {
            modloaders.selectedIndex = 0
        }
        updateMinecraftValues()
    }

    /**
     * TODO docs
     */
    private fun initTooltips() {
        modpackInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelmodpackdir_tip.toString()
        propertiesInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelpropertiespath_tip.toString()
        iconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labeliconpath_tip.toString()
        filesInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelcopydirs_tip.toString()
        suffixInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelsuffix_tip.toString()
        minecraftInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelminecraft_tip.toString()
        javaVersionInfo.toolTipText = Gui.createserverpack_gui_createserverpack_minecraft_java_tooltip.toString()
        modloaderInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelmodloader_tip.toString()
        modloaderVersionInfo.toolTipText =
            Gui.createserverpack_gui_createserverpack_labelmodloaderversion_tip.toString()
        includeIconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxicon_tip.toString()
        includeZIPInfo.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxzip_tip.toString()
        includePropertiesInfo.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxproperties_tip.toString()
        prepareServerInfo.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxserver_tip.toString()
        //iconPreview.toolTipText = Gui.createserverpack_gui_createserverpack_servericon_preview.toString()

        balloonTip.isVisible = false
        iconPreview.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mousePressed(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseReleased(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseEntered(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseExited(e: MouseEvent?) {
                balloonTip.isVisible = false
            }

        })

        exclusionsInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelclientmods_tip.toString()
        argumentsInfo.toolTipText = Gui.createserverpack_gui_createserverpack_javaargs_tip.toString()
        scriptInfo.toolTipText = Gui.createserverpack_gui_createserverpack_scriptsettings_label_tooltip.toString()
        modpackBrowser.toolTipText = Gui.createserverpack_gui_browser.toString()
        modpackInspect.toolTipText = Gui.createserverpack_gui_buttonmodpackdir_scan_tip.toString()
        propertiesBrowser.toolTipText = Gui.createserverpack_gui_browser.toString()
        propertiesOpen.toolTipText = Gui.createserverpack_gui_createserverpack_button_open_properties.toString()
        iconBrowser.toolTipText = Gui.createserverpack_gui_browser.toString()
        serverPackFilesRevert.toolTipText = Gui.createserverpack_gui_buttoncopydirs_revert_tip.toString()
        serverPackFilesBrowser.toolTipText = Gui.createserverpack_gui_browser.toString()
        serverPackFilesReset.toolTipText = Gui.createserverpack_gui_buttoncopydirs_reset_tip.toString()
        exclusionsRevert.toolTipText = Gui.createserverpack_gui_buttonclientmods_revert_tip.toString()
        exclusionsBrowser.toolTipText = Gui.createserverpack_gui_browser.toString()
        exclusionsReset.toolTipText = Gui.createserverpack_gui_buttonclientmods_reset_tip.toString()
        scriptKVPairsRevert.toolTipText = Gui.createserverpack_gui_revert.toString()
        scriptKVPairsReset.toolTipText = Gui.createserverpack_gui_reset.toString()
        aikarsFlags.toolTipText = Gui.createserverpack_gui_createserverpack_button_properties.toString()
    }

    /**
     * TODO docs
     */
    private fun initListeners() {
        modpackBrowser.addActionListener(showBrowser)
        propertiesBrowser.addActionListener(showBrowser)
        iconBrowser.addActionListener(showBrowser)
        serverPackFilesBrowser.addActionListener(showBrowser)
        exclusionsBrowser.addActionListener(showBrowser)

        modpackInspect.addActionListener { updateGuiFromSelectedModpack() }
        propertiesOpen.addActionListener { openServerProperties() }

        exclusionsRevert.addActionListener { revertExclusions() }
        serverPackFilesRevert.addActionListener { revertServerPackFiles() }

        exclusionsReset.addActionListener { setClientSideMods(apiProperties.clientSideMods()) }
        serverPackFilesReset.addActionListener { setCopyDirectories(apiProperties.directoriesToInclude.toMutableList()) }

        modpackDirectory.document.addDocumentListener(modpackChanges)
        serverPackSuffix.document.addDocumentListener(suffixChanges)
        exclusions.addDocumentListener(exclusionChanges)
        serverPackFiles.addDocumentListener(serverPackFilesChanges)
        iconFile.document.addDocumentListener(iconChanges)
        propertiesFile.document.addDocumentListener(propertyChanges)
        minecraftVersions.addActionListener { updateMinecraftValues() }
        modloaders.addActionListener { updateMinecraftValues() }
        modloaderVersions.addActionListener { updateMinecraftValues() }

        prepareServer.addActionListener { checkServer() }
        includeIcon.addActionListener { validateServerIcon() }
        includeProperties.addActionListener { validateServerProperties() }

        aikarsFlags.addActionListener { setAikarsFlagsAsJavaArguments() }
    }

    override fun setClientSideMods(entries: MutableList<String>) {
        exclusions.text = utilities.stringUtilities.buildString(entries)
        validateInputFields()
    }

    override fun setCopyDirectories(entries: MutableList<String>) {
        serverPackFiles.text = utilities.stringUtilities.buildString(entries.toString())
        validateInputFields()
    }

    override fun setIconInclusionTicked(ticked: Boolean) {
        includeIcon.isSelected = ticked
    }

    override fun setJavaArguments(javaArguments: String) {
        startArgs.text = javaArguments
    }

    override fun setMinecraftVersion(version: String) {
        for (i in 0 until minecraftVersions.model.size) {
            if (minecraftVersions.model.getElementAt(i) == version) {
                minecraftVersions.selectedIndex = i
                break
            }
        }
    }

    override fun setModloader(modloader: String) {
        when (modloader) {
            "Fabric" -> modloaders.setSelectedIndex(0)
            "Forge" -> modloaders.setSelectedIndex(1)
            "Quilt" -> modloaders.setSelectedIndex(2)
            "LegacyFabric" -> modloaders.setSelectedIndex(3)
        }
        setModloaderVersionsModel()
    }

    override fun setModloaderVersion(version: String) {
        for (i in 0 until modloaderVersions.model.size) {
            if (modloaderVersions.model.getElementAt(i) == version) {
                modloaderVersions.selectedIndex = i
                break
            }
        }
    }

    override fun setModpackDirectory(directory: String) {
        modpackDirectory.text = directory
    }

    override fun setPropertiesInclusionTicked(ticked: Boolean) {
        includeProperties.isSelected = ticked
    }

    override fun setScriptVariables(variables: HashMap<String, String>) {
        scriptKVPairs.loadData(variables)
    }

    override fun setServerIconPath(path: String) {
        iconFile.text = path
    }

    override fun setServerInstallationTicked(ticked: Boolean) {
        prepareServer.isSelected = ticked
    }

    override fun setServerPackSuffix(suffix: String) {
        serverPackSuffix.text = utilities.stringUtilities.pathSecureText(suffix)
    }

    override fun setServerPropertiesPath(path: String) {
        propertiesFile.text = path
    }

    override fun setZipArchiveCreationTicked(ticked: Boolean) {
        includeZip.isSelected = ticked
    }

    override fun getClientSideMods(): String {
        return exclusions.text.replace(", ", ",")
    }

    override fun getClientSideModsList(): MutableList<String> {
        return utilities.listUtilities.cleanList(
            getClientSideMods().split(",")
                .dropLastWhile { it.isEmpty() }
                .toMutableList()
        )
    }

    override fun getCopyDirectories(): String {
        return serverPackFiles.text.replace(", ", ",")
    }

    override fun getCopyDirectoriesList(): MutableList<String> {
        return utilities.listUtilities.cleanList(
            getCopyDirectories().split(",")
                .dropLastWhile { it.isEmpty() }
                .toMutableList()
        )
    }

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

    // TODO rename to getStartArguments ?
    override fun getJavaArguments(): String {
        return startArgs.text
    }

    override fun getMinecraftVersion(): String {
        return minecraftVersions.selectedItem!!.toString()
    }

    override fun getModloader(): String {
        return modloaders.selectedItem!!.toString()
    }

    override fun getModloaderVersion(): String {
        return modloaderVersions.selectedItem!!.toString()
    }

    override fun getModpackDirectory(): String {
        return modpackDirectory.text
    }

    override fun getScriptSettings(): HashMap<String, String> {
        return scriptKVPairs.getData()
    }

    override fun getServerIconPath(): String {
        return iconFile.text
    }

    override fun getServerPackSuffix(): String {
        return utilities.stringUtilities.pathSecureText(serverPackSuffix.text)
    }

    override fun getServerPropertiesPath(): String {
        return propertiesFile.text
    }

    override fun isMinecraftServerAvailable(): Boolean {
        return versionMeta.minecraft.isServerAvailable(minecraftVersions.selectedItem!!.toString())
    }

    override fun isServerInstallationTicked(): Boolean {
        return prepareServer.isSelected
    }

    override fun isServerIconInclusionTicked(): Boolean {
        return includeIcon.isSelected
    }

    override fun isServerPropertiesInclusionTicked(): Boolean {
        return includeProperties.isSelected
    }

    override fun isZipArchiveCreationTicked(): Boolean {
        return includeZip.isSelected
    }

    override fun clearScriptVariables() {
        scriptKVPairs.clearData()
    }

    override fun setAikarsFlagsAsJavaArguments() {
        if (getJavaArguments().isNotEmpty()) {
            when (JOptionPane.showConfirmDialog(
                this,
                Gui.createserverpack_gui_createserverpack_javaargs_confirm_message.toString(),
                Gui.createserverpack_gui_createserverpack_javaargs_confirm_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )) {
                0 -> setJavaArguments(apiProperties.aikarsFlags)
                1 -> {}
                else -> {}
            }
        } else {
            setJavaArguments(apiProperties.aikarsFlags)
        }
    }

    override fun validateInputFields() {
        timer.restart()
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
     * Set the modloader version combobox model depending on the currently selected modloader, with
     * the specified Minecraft version.
     *
     * @param minecraftVersion The Minecraft version to work with in the GUI update.
     * @author Griefed
     */
    private fun setModloaderVersionsModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        when (modloaders.selectedIndex) {
            0 -> updateFabricModel(minecraftVersion)
            1 -> updateForgeModel(minecraftVersion)
            2 -> updateQuiltModel(minecraftVersion)
            3 -> updateLegacyFabricModel(minecraftVersion)
            else -> {
                log.error("Invalid modloader selected.")
            }
        }
    }

    /**
     * TODO docs
     */
    private fun setModloaderVersions(
        model: DefaultComboBoxModel<String>,
        icon: Icon = guiProps.infoIcon,
        tooltip: String = Gui.createserverpack_gui_createserverpack_labelmodloaderversion_tip.toString()
    ) {
        modloaderVersionInfo.icon = icon
        modloaderVersionInfo.toolTipText = tooltip
        modloaderVersions.model = model
    }

    /**
     * TODO docs
     */
    private fun updateFabricModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
            setModloaderVersions(fabricVersions)
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Gui.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * TODO docs
     */
    private fun updateForgeModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.forge.supportedForgeVersionsDescendingArray(minecraftVersion).isPresent) {
            setModloaderVersions(
                DefaultComboBoxModel(
                    versionMeta.forge.supportedForgeVersionsDescendingArray(
                        minecraftVersion
                    ).get()
                )
            )
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Gui.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * TODO docs
     */
    private fun updateQuiltModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
            setModloaderVersions(quiltVersions)
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Gui.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * TODO docs
     */
    private fun updateLegacyFabricModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) {
            setModloaderVersions(legacyFabricVersions)
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Gui.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * Validate the input field for modpack directory.
     *
     * @author Griefed
     */
    private fun validateModpackDir(): List<String> {
        val errors: MutableList<String> = ArrayList(20)
        if (configurationHandler.checkModpackDir(getModpackDirectory(), errors, false)) {
            modpackInfo.icon = guiProps.infoIcon
            modpackInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelmodpackdir_tip.toString()
        } else {
            modpackInfo.icon = guiProps.errorIcon
            modpackInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * Validate the input field for server pack suffix.
     *
     * @author Griefed
     */
    private fun validateSuffix(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (utilities.stringUtilities.checkForIllegalCharacters(serverPackSuffix.text)) {
            suffixInfo.icon = guiProps.infoIcon
            suffixInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelsuffix_tip.toString()
        } else {
            errors.add(Gui.configuration_log_error_serverpack_suffix.toString())
            suffixInfo.icon = guiProps.errorIcon
            suffixInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * Validate the input field for client mods.
     *
     * @author Griefed
     */
    private fun validateExclusions(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (!getClientSideMods().matches(whitespace)) {
            exclusionsInfo.icon = guiProps.infoIcon
            exclusionsInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelclientmods_tip.toString()
        } else {
            errors.add(Gui.configuration_log_error_formatting.toString())
            exclusionsInfo.icon = guiProps.errorIcon
            exclusionsInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * Validate the input field for copy directories.
     *
     * @author Griefed
     */
    private fun validateServerPackFiles(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        configurationHandler.checkCopyDirs(getCopyDirectoriesList(), getModpackDirectory(), errors, false)
        if (getCopyDirectories().matches(whitespace)) {
            errors.add(Gui.configuration_log_error_formatting.toString())
        }
        if (errors.isNotEmpty()) {
            filesInfo.icon = guiProps.errorIcon
            filesInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
        } else {
            filesInfo.icon = guiProps.infoIcon
            filesInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelcopydirs_tip.toString()
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * Validate the input field for server icon.
     *
     * @author Griefed
     */
    private fun validateServerIcon(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (getServerIconPath().isNotEmpty()) {
            if (configurationHandler.checkIconAndProperties(getServerIconPath())) {
                iconInfo.icon = guiProps.infoIcon
                includeIconInfo.icon = guiProps.infoIcon
                includeIconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxicon_tip.toString()
                iconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_textfield_iconpath.toString()
                setIconPreview(File(getServerIconPath()), errors)
            } else {
                iconInfo.icon = guiProps.errorIcon
                includeIconInfo.icon = guiProps.warningIcon
                includeIconInfo.toolTipText = Gui.configuration_log_warn_icon.toString()
                iconInfo.toolTipText = Gui.configuration_log_error_servericon_error.toString()
                iconPreview.icon = guiProps.iconError
                iconPreviewBig.icon = guiProps.iconError.getScaledInstance(64,64,Image.SCALE_SMOOTH)
            }
        } else {
            setIconPreview(apiProperties.defaultServerIcon, errors)
            iconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_servericon_preview.toString()
            includeIconInfo.icon = guiProps.infoIcon
            includeIconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxicon_tip.toString()
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * Validate the inputfield for server properties.
     *
     * @author Griefed
     */
    private fun validateServerProperties(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (configurationHandler.checkIconAndProperties(getServerPropertiesPath())) {
            propertiesInfo.icon = guiProps.infoIcon
            includePropertiesInfo.icon = guiProps.infoIcon

            propertiesInfo.toolTipText =
                Gui.createserverpack_gui_createserverpack_textfield_propertiespath.toString()
            includePropertiesInfo.toolTipText =
                Gui.createserverpack_gui_createserverpack_checkboxproperties_tip.toString()
        } else {
            propertiesInfo.icon = guiProps.errorIcon
            includePropertiesInfo.icon = guiProps.warningIcon

            propertiesInfo.toolTipText = Gui.configuration_log_warn_properties.toString()
            includePropertiesInfo.toolTipText = Gui.configuration_log_warn_properties.toString()
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * TODO docs
     */
    private fun setIconPreview(icon: File, errors: MutableList<String>) {
        try {
            val newIcon = ImageIcon(ImageIO.read(icon))
            iconPreview.icon = newIcon.getScaledInstance(32, 32, Image.SCALE_SMOOTH)
            iconPreviewBig.icon = newIcon.getScaledInstance(128,128,Image.SCALE_SMOOTH)
        } catch (ex: IOException) {
            log.error("Error generating server icon preview.", ex)
            errors.add(Gui.configuration_log_error_servericon_error.toString())
        }
    }

    /**
     * Get the configurations of the available ExtensionConfigPanel as a hashmap, so we can store them
     * in our serverpackcreator.conf.
     *
     * @return Map containing lists of CommentedConfigs mapped to the corresponding pluginID.
     */
    private fun getConfigPanelConfigs(): HashMap<String, ArrayList<CommentedConfig>> {
        val configs: HashMap<String, ArrayList<CommentedConfig>> =
            HashMap<String, ArrayList<CommentedConfig>>(10)
        if (pluginPanels.isNotEmpty()) {
            for (panel in pluginPanels) {
                configs[panel.pluginID] = panel.serverPackExtensionConfig()
            }
        }
        return configs
    }

    /**
     * Scan the modpack directory for various manifests and, if any are found, parse them and try to
     * load the Minecraft version, modloader and modloader version.
     *
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun updateGuiFromSelectedModpack() {
        GlobalScope.launch(Dispatchers.Default) {
            modpackInspect.isEnabled = false
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
                            this@ConfigEditorPanel,
                            Gui.createserverpack_gui_modpack_scan_message(
                                getMinecraftVersion(),
                                getModloader(),
                                getModloaderVersion(),
                                utilities.stringUtilities.buildString(dirsToInclude.toList())
                            ) + "   ",
                            Gui.createserverpack_gui_modpack_scan.toString(),
                            JOptionPane.INFORMATION_MESSAGE,
                            guiProps.infoIcon
                        )
                    }
                } catch (ex: IOException) {
                    log.error("Couldn't update GUI from modpack manifests.", ex)
                }
            }
            modpackInspect.isEnabled = true
        }
    }

    /**
     * Reverts the list of clientside-only mods to the value of the last loaded configuration, if one
     * is available.
     *
     * @author Griefed
     */
    private fun revertExclusions() {
        if (lastLoadedConfiguration != null) {
            setClientSideMods(lastLoadedConfiguration!!.clientMods)
            validateInputFields()
        }
    }

    /**
     * Reverts the list of copy directories to the value of the last loaded configuration, if one is
     * available.
     *
     * @author Griefed
     */
    private fun revertServerPackFiles() {
        if (lastLoadedConfiguration != null) {
            setCopyDirectories(lastLoadedConfiguration!!.copyDirs)
            validateInputFields()
        }
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
        validateInputFields()
    }

    /**
     * TODO docs
     */
    private fun updateRequiredJavaVersion() {
        javaVersion.text = acquireRequiredJavaVersion()
        if (javaVersion.text != "?" && apiProperties.javaPath(javaVersion.text).isPresent && !scriptKVPairs.getData()["SPC_JAVA_SPC"].equals(
                apiProperties.javaPath(javaVersion.text).get()
            ) && apiProperties.isJavaScriptAutoupdateEnabled
        ) {
            val path = apiProperties.javaPath(javaVersion.text).get()
            val data: HashMap<String, String> = scriptKVPairs.getData()
            data.replace(
                "SPC_JAVA_SPC", path
            )
            scriptKVPairs.loadData(data)
            log.info("Automatically adjusted script variable SPC_JAVA_SPC to $path")
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
            prepareServerInfo.icon = guiProps.warningIcon
            prepareServerInfo.toolTipText = Gui.configuration_log_warn_server.toString()
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
        } else if (server.isPresent && !server.get().url().isPresent) {
            prepareServerInfo.icon = guiProps.warningIcon
            prepareServerInfo.toolTipText = Gui.configuration_log_warn_server.toString()
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_url_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
        } else {
            prepareServerInfo.icon = guiProps.infoIcon
            prepareServerInfo.toolTipText = Gui.createserverpack_gui_createserverpack_checkboxserver_tip.toString()
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
    fun checkServer(): Boolean {
        var okay = true
        if (isServerInstallationTicked()) {
            val mcVersion = minecraftVersions.selectedItem!!.toString()
            val modloader = modloaders.selectedItem!!.toString()
            val modloaderVersion = modloaderVersions.selectedItem!!.toString()
            if (!checkJava()) {
                setServerInstallationTicked(false)
                okay = false
            }
            if (!serverPackHandler.serverDownloadable(mcVersion, modloader, modloaderVersion)) {
                JOptionPane.showMessageDialog(
                    this, Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_message(
                        modloader, mcVersion, modloader, modloaderVersion, modloader
                    ) + "    ", Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                        mcVersion, modloader, modloaderVersion
                    ), JOptionPane.WARNING_MESSAGE, guiProps.warningIcon
                )
                setServerInstallationTicked(false)
                okay = false
            }
        }
        return okay
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
    private fun checkJava(): Boolean {
        return if (!apiProperties.javaAvailable()) {
            when (JOptionPane.showConfirmDialog(
                this,
                Gui.createserverpack_gui_createserverpack_checkboxserver_confirm_message.toString(),
                Gui.createserverpack_gui_createserverpack_checkboxserver_confirm_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
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
                        guiProps.errorIcon
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
}
