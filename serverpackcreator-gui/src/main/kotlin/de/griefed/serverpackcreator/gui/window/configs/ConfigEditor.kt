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
package de.griefed.serverpackcreator.gui.window.configs

import Gui
import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.InclusionSpecification
import de.griefed.serverpackcreator.api.PackConfig
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.configs.components.*
import de.griefed.serverpackcreator.gui.window.configs.components.advanced.*
import de.griefed.serverpackcreator.gui.window.configs.components.inclusions.InclusionsEditor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Dimension
import java.awt.event.ActionListener
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent

/**
 * Panel to edit a server pack configuration. This panel contains any and all elements required to fully configure
 * a server pack to the users liking.
 *
 * @author Griefed
 */
class ConfigEditor(
    private val guiProps: GuiProps,
    tabbedConfigsTab: TabbedConfigsTab,
    private val apiWrapper: ApiWrapper,
    private val noVersions: DefaultComboBoxModel<String>,
    componentResizer: ComponentResizer
) : JScrollPane(), ServerPackConfigTab {
    private val log = cachedLoggerOf(this.javaClass)
    private val modpackInfo = ModpackInfo(guiProps)
    private val propertiesInfo = PropertiesInfo(guiProps)
    private val iconInfo = IconInfo(guiProps)
    private val inclusionsInfo = InclusionsInfo(guiProps)
    private val suffixInfo = SuffixInfo(guiProps)
    private val modloaderVersionInfo = ModloaderVersionInfo(guiProps)
    private val includeIconInfo = IncludeIconInfo(guiProps)
    private val includePropertiesInfo = IncludePropertiesInfo(guiProps)
    private val prepareServerInfo = PrepareServerInfo(guiProps)
    private val exclusionsInfo = ExclusionsInfo(guiProps)
    private val validate = ActionListener { validateInputFields() }
    private val chooserDimension: Dimension = Dimension(750, 450)
    private val modpackInspect = BalloonTipButton(
        null,
        guiProps.inspectIcon,
        Gui.createserverpack_gui_buttonmodpackdir_scan_tip.toString(),
        guiProps
    ) { updateGuiFromSelectedModpack() }
    private val includeIcon = ActionCheckBox(
        Gui.createserverpack_gui_createserverpack_checkboxicon.toString(),
        validate
    )
    private val includeProperties = ActionCheckBox(
        Gui.createserverpack_gui_createserverpack_checkboxproperties.toString(),
        validate
    )
    private val includeZip = ActionCheckBox(
        Gui.createserverpack_gui_createserverpack_checkboxzip.toString(),
        validate
    )
    private val includeServer = ActionCheckBox(
        Gui.createserverpack_gui_createserverpack_checkboxserver.toString(),
        validate
    )
    private val updateMinecraft = ActionListener { updateMinecraftValues() }
    private val minecraftVersions = ActionComboBox(
        DefaultComboBoxModel(apiWrapper.versionMeta!!.minecraft.settingsDependantVersionsArrayDescending()),
        updateMinecraft
    )
    private val modloaders = ActionComboBox(
        DefaultComboBoxModel(apiWrapper.apiProperties.supportedModloaders),
        updateMinecraft
    )
    private val iconPreview = IconPreview(guiProps)
    private val javaVersion = ElementLabel("8", 16)
    private val legacyFabricVersions = DefaultComboBoxModel(
        apiWrapper.versionMeta!!.legacyFabric.loaderVersionsArrayDescending()
    )
    private val fabricVersions = DefaultComboBoxModel(apiWrapper.versionMeta!!.fabric.loaderVersionsArrayDescending())
    private val quiltVersions = DefaultComboBoxModel(apiWrapper.versionMeta!!.quilt.loaderVersionsArrayDescending())
    private val modloaderVersions = ActionComboBox { validateInputFields() }
    private val aikarsFlags = AikarsFlags(this, guiProps)
    private val scriptKVPairs = ScriptKVPairs(guiProps, this)
    private val pluginPanels = apiWrapper.apiPlugins!!.getConfigPanels(this).toMutableList()
    var lastSavedConfig: PackConfig? = null
    private val changeListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            validateInputFields()
        }
    }
    private val modpackDirectory = ScrollTextFileField(guiProps,File(""), changeListener)
    private val javaArgs = ScrollTextArea(
        "-Xmx4G -Xms4G",
        Gui.createserverpack_gui_createserverpack_javaargs.toString(),
        changeListener,
        guiProps
    )
    private val serverPackSuffix = ScrollTextField(guiProps,"", "suffix", apiWrapper.apiProperties, changeListener)
    private val propertiesFile = ScrollTextFileField(guiProps,apiWrapper.apiProperties.defaultServerProperties, changeListener)
    private val iconFile = ScrollTextFileField(guiProps,apiWrapper.apiProperties.defaultServerIcon, changeListener)
    private val source = ScrollTextField(guiProps,"", "source", apiWrapper.apiProperties)
    private val destination = ScrollTextField(guiProps,"", "destination", apiWrapper.apiProperties)
    private val inclusionFilter = ScrollTextField(guiProps,"", "inclusion", apiWrapper.apiProperties)
    private val exclusionFilter = ScrollTextField(guiProps,"", "exclusion", apiWrapper.apiProperties)
    private val inclusionsEditor = InclusionsEditor(
        chooserDimension,
        guiProps,
        this,
        apiWrapper,
        source,
        destination,
        inclusionFilter,
        exclusionFilter
    )
    private val exclusions = ScrollTextArea(
        apiWrapper.apiProperties.clientSideMods().joinToString(","),
        Gui.createserverpack_gui_createserverpack_labelclientmods.toString(),
        changeListener,
        guiProps
    )
    private val timer = ConfigCheckTimer(250, this, guiProps)
    private val panel = JPanel(
        MigLayout(
            "left,wrap",
            "[left,::64]5[left]5[left,grow]5[left,::64]5[left,::64]",
            "30"
        )
    )
    val propertiesQuickSelect = QuickSelect(
        tabbedConfigsTab.propertiesQuickSelections()
    ) { setProperties() }
    val iconQuickSelect = QuickSelect(
        tabbedConfigsTab.iconQuickSelections()
    ) { setIcon() }
    val editorTitle = ConfigEditorTitle(guiProps, tabbedConfigsTab, this)
    var configFile: File? = null
        private set

    init {
        viewport.view = panel
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBar.unitIncrement = 10
        minecraftVersions.selectedIndex = 0
        modloaders.selectedIndex = 0
        updateMinecraftValues()

        val modpackLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labelmodpackdir.toString())
        val modpackShowBrowser = BalloonTipButton(
            null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(),
            guiProps
        ) { selectModpackDirectory() }
        val propertiesLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labelpropertiespath.toString())
        val quickSelectLabel = ElementLabel(Gui.createserverpack_gui_quickselect.toString())
        val propertiesShowBrowser = BalloonTipButton(
            null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(),
            guiProps
        ) { selectServerProperties() }
        val openProperties = BalloonTipButton(
            null, guiProps.openIcon, Gui.createserverpack_gui_createserverpack_button_open_properties.toString(),
            guiProps
        ) { openServerProperties() }
        val iconLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labeliconpath.toString())
        val iconQuickSelectLabel = ElementLabel(Gui.createserverpack_gui_quickselect.toString())
        val iconShowBrowser = BalloonTipButton(
            null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(),
            guiProps
        ) { selectServerIcon() }
        val filesLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labelcopydirs.toString())
        val suffixLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labelsuffix.toString())
        val mcVersionInfo = MinecraftVersionInfo(guiProps)
        val mcVersionLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labelminecraft.toString())
        val javaVersionInfo = JavaVersionInfo(guiProps)
        val javaVersionLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_minecraft_java.toString(), 16)
        val modloaderInfo = ModloaderInfo(guiProps)
        val modloaderLabel = ElementLabel(Gui.createserverpack_gui_createserverpack_labelmodloader.toString())
        val zipInfo = IncludeZipInfo(guiProps)
        val modloaderVersionLabel = ElementLabel(
            Gui.createserverpack_gui_createserverpack_labelmodloaderversion.toString()
        )
        val clientModsRevert = BalloonTipButton(
            null, guiProps.revertIcon, Gui.createserverpack_gui_buttonclientmods_revert_tip.toString(),
            guiProps
        ) { revertExclusions() }
        val clientModsShowBrowser = BalloonTipButton(
            null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(),
            guiProps
        ) { selectClientMods() }
        val clientModsReset = BalloonTipButton(
            null, guiProps.resetIcon, Gui.createserverpack_gui_buttonclientmods_reset_tip.toString(),
            guiProps
        ) { resetClientMods() }
        val kvRevert = BalloonTipButton(
            null, guiProps.revertIcon, Gui.createserverpack_gui_revert.toString(),
            guiProps
        ) { revertScriptKVPairs() }
        val kvReset = BalloonTipButton(
            null, guiProps.resetIcon, Gui.createserverpack_gui_reset.toString(),
            guiProps
        ) { resetScriptKVPairs() }
        val advancedSettingsPanel = AdvancedSettingsPanel(
            exclusionsInfo,
            JavaArgsInfo(guiProps),
            ScriptSettingsInfo(guiProps),
            exclusions,
            clientModsRevert,
            clientModsShowBrowser,
            clientModsReset,
            javaArgs,
            aikarsFlags,
            scriptKVPairs,
            kvRevert,
            kvReset
        )
        val advancedSettings = CollapsiblePanel(Gui.createserverpack_gui_advanced.toString(), advancedSettingsPanel)
        val pluginSettings = PluginsSettingsPanel(pluginPanels)
        val pluginPanel = CollapsiblePanel(Gui.createserverpack_gui_plugins.toString(), pluginSettings)

        // "cell column row width height"
        // Modpack directory
        panel.add(modpackInfo, "cell 0 0,grow")
        panel.add(modpackLabel, "cell 1 0,grow")
        panel.add(modpackDirectory, "cell 2 0,grow")
        panel.add(modpackShowBrowser, "cell 3 0, h 30!,w 30!")
        panel.add(modpackInspect, "cell 4 0")

        // Server Properties
        panel.add(propertiesInfo, "cell 0 1,grow")
        panel.add(propertiesLabel)
        panel.add(propertiesFile, "cell 2 1, split 3,grow, w 50:50:")
        panel.add(quickSelectLabel, "cell 2 1")
        panel.add(propertiesQuickSelect, "cell 2 1,w 200!")
        panel.add(propertiesShowBrowser, "cell 3 1")
        panel.add(openProperties, "cell 4 1")

        // Server Icon
        panel.add(iconInfo, "cell 0 2,grow")
        panel.add(iconLabel, "cell 1 2,grow")
        panel.add(iconFile, "cell 2 2, split 2,grow, w 50:50:")
        panel.add(iconQuickSelectLabel, "cell 2 2")
        panel.add(iconQuickSelect, "cell 2 2,w 200!")
        panel.add(iconShowBrowser, "cell 3 2")
        panel.add(iconPreview, "cell 4 2")

        // Server Files
        panel.add(inclusionsInfo, "cell 0 3 1 3")
        panel.add(filesLabel, "cell 1 3 1 3,grow")
        panel.add(inclusionsEditor, "cell 2 3 3 3, grow, w 10:500:, h 150:225:300")

        // Server Pack Suffix
        panel.add(suffixInfo, "cell 0 6,grow")
        panel.add(suffixLabel, "cell 1 6,grow")
        panel.add(serverPackSuffix, "cell 2 6 3 1,grow")

        // Minecraft Version
        panel.add(mcVersionInfo, "cell 0 7,grow")
        panel.add(mcVersionLabel, "cell 1 7,grow")
        panel.add(minecraftVersions, "cell 2 7,w 200!")
        // Java Version Of Minecraft Version
        panel.add(javaVersionInfo, "cell 2 7, w 40!, gapleft 40")
        panel.add(javaVersionLabel, "cell 2 7")
        panel.add(javaVersion, "cell 2 7, w 40!")

        // Modloader
        panel.add(modloaderInfo, "cell 0 8,grow")
        panel.add(modloaderLabel, "cell 1 8,grow")
        panel.add(modloaders, "cell 2 8,w 200!")
        // Include Server Icon
        panel.add(includeIconInfo, "cell 2 8, w 40!, gapleft 40,grow")
        panel.add(includeIcon, "cell 2 8, w 200!")
        // Create ZIP Archive
        panel.add(zipInfo, "cell 2 8, w 40!,grow")
        panel.add(includeZip, "cell 2 8, w 200!")

        // Modloader Version
        panel.add(modloaderVersionInfo, "cell 0 9,grow")
        panel.add(modloaderVersionLabel, "cell 1 9,grow")
        panel.add(modloaderVersions, "cell 2 9,w 200!")
        // Include Server Properties
        panel.add(includePropertiesInfo, "cell 2 9, w 40!, gapleft 40,grow")
        panel.add(includeProperties, "cell 2 9, w 200!")
        // Install Local Server
        panel.add(prepareServerInfo, "cell 2 9, w 40!,grow")
        panel.add(includeServer, "cell 2 9, w 200!")

        // Advanced Settings
        panel.add(advancedSettings, "cell 0 10 5,grow")

        // Plugins
        if (pluginPanels.isNotEmpty()) {
            panel.add(pluginPanel, "cell 0 11 5,grow")
        }
        validateInputFields()
        lastSavedConfig = getCurrentConfiguration()
        componentResizer.registerComponent(exclusions, "cell 2 0 1 3,grow,w 10:500:,h %s!")
        componentResizer.registerComponent(javaArgs, "cell 2 3 1 3,grow,w 10:500:,h %s!")
        componentResizer.registerComponent(scriptKVPairs.scrollPanel, "cell 2 6 1 3,grow,w 10:500:,h %s!")
    }

    /**
     * @author Griefed
     */
    private fun selectModpackDirectory() {
        val chooser = if (File(getModpackDirectory()).isDirectory) {
            ModpackChooser(File(getModpackDirectory()), chooserDimension)
        } else if (File(getModpackDirectory()).isFile) {
            ModpackChooser(File(File(getModpackDirectory()).parent), chooserDimension)
        } else {
            ModpackChooser(chooserDimension)
        }

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            setModpackDirectory(chooser.selectedFile.path)
            updateGuiFromSelectedModpack()
            log.debug(
                "Selected modpack directory: " + chooser.selectedFile.path
            )
        }
    }

    /**
     * @author Griefed
     */
    private fun selectServerProperties() {
        val serverPropertiesChooser = if (File(getServerPropertiesPath()).isFile) {
            ServerPropertiesChooser(File(getServerPropertiesPath()), chooserDimension)
        } else {
            ServerPropertiesChooser(chooserDimension)
        }
        if (serverPropertiesChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            setServerPropertiesPath(serverPropertiesChooser.selectedFile.absolutePath)
        }
    }

    /**
     * @author Griefed
     */
    private fun selectServerIcon() {
        val serverIconChooser = if (File(getServerIconPath()).isFile) {
            ServerIconChooser(File(getServerIconPath()), chooserDimension)
        } else {
            ServerIconChooser(chooserDimension)
        }
        if (serverIconChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            setServerIconPath(serverIconChooser.selectedFile.absolutePath)
        }
    }

    /**
     * @author Griefed
     */
    private fun selectClientMods() {
        val clientModsChooser = if (File(getModpackDirectory(), "mods").isDirectory) {
            ClientModsChooser(File(getModpackDirectory(), "mods"), chooserDimension)
        } else {
            ClientModsChooser(chooserDimension)
        }

        if (clientModsChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            val clientMods: Array<File> = clientModsChooser.selectedFiles
            val clientModsFilenames: TreeSet<String> = TreeSet()
            clientModsFilenames.addAll(getClientSideModsList())
            for (mod in clientMods) {
                clientModsFilenames.add(mod.name)
            }
            setClientSideMods(clientModsFilenames.toMutableList())
            log.debug("Selected mods: $clientModsFilenames")
        }
    }

    /**
     * @author Griefed
     */
    override fun setClientSideMods(entries: MutableList<String>) {
        exclusions.text = apiWrapper.utilities!!.stringUtilities.buildString(entries)
        validateInputFields()
    }

    /**
     * @author Griefed
     */
    override fun setInclusions(entries: MutableList<InclusionSpecification>) {
        inclusionsEditor.setServerFiles(entries)
    }

    /**
     * @author Griefed
     */
    override fun setIconInclusionTicked(ticked: Boolean) {
        includeIcon.isSelected = ticked
    }

    /**
     * @author Griefed
     */
    override fun setJavaArguments(javaArguments: String) {
        javaArgs.text = javaArguments
    }

    /**
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
     * @author Griefed
     */
    override fun setModloader(modloader: String) {
        when (modloader) {
            "Fabric" -> modloaders.selectedIndex = 0
            "Forge" -> modloaders.selectedIndex = 1
            "NeoForge" -> modloaders.selectedIndex = 4
            "Quilt" -> modloaders.selectedIndex = 2
            "LegacyFabric" -> modloaders.selectedIndex = 3
        }
        setModloaderVersionsModel()
    }

    /**
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
     * @author Griefed
     */
    override fun setModpackDirectory(directory: String) {
        modpackDirectory.text = directory
    }

    /**
     * @author Griefed
     */
    override fun setPropertiesInclusionTicked(ticked: Boolean) {
        includeProperties.isSelected = ticked
    }

    /**
     * @author Griefed
     */
    override fun setScriptVariables(variables: HashMap<String, String>) {
        scriptKVPairs.loadData(variables)
    }

    /**
     * @author Griefed
     */
    override fun setServerIconPath(path: String) {
        iconFile.text = path
    }

    /**
     * @author Griefed
     */
    override fun setServerInstallationTicked(ticked: Boolean) {
        includeServer.isSelected = ticked
    }

    /**
     * @author Griefed
     */
    override fun setServerPackSuffix(suffix: String) {
        serverPackSuffix.text = apiWrapper.utilities!!.stringUtilities.pathSecureText(suffix)
    }

    /**
     * @author Griefed
     */
    override fun setServerPropertiesPath(path: String) {
        propertiesFile.text = path
    }

    /**
     * @author Griefed
     */
    override fun setZipArchiveCreationTicked(ticked: Boolean) {
        includeZip.isSelected = ticked
    }

    /**
     * @author Griefed
     */
    override fun getClientSideMods(): String {
        return exclusions.text.replace(", ", ",")
    }

    /**
     * @author Griefed
     */
    override fun getClientSideModsList(): MutableList<String> {
        return apiWrapper.utilities!!.listUtilities.cleanList(
            getClientSideMods().split(",")
                .dropLastWhile { it.isEmpty() }
                .toMutableList()
        )
    }

    /**
     * @author Griefed
     */
    override fun getInclusions(): MutableList<InclusionSpecification> {
        return inclusionsEditor.getServerFiles()
    }

    /**
     * @author Griefed
     */
    override fun getCurrentConfiguration(): PackConfig {
        return PackConfig(
            getClientSideModsList(),
            getInclusions(),
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
            getExtensionsConfigs()
        )
    }

    /**
     * @author Griefed
     */
    override fun saveCurrentConfiguration(): File {
        val modpackName =
            apiWrapper.utilities!!.stringUtilities.pathSecureText(File(getModpackDirectory()).name + ".conf")
        val config = if (configFile != null) {
            configFile!!
        } else {
            File(apiWrapper.apiProperties.configsDirectory, modpackName)
        }
        lastSavedConfig = getCurrentConfiguration().save(config)
        configFile = config
        editorTitle.hideWarningIcon()
        saveSuggestions()
        return configFile!!
    }

    private fun saveSuggestions() {
        val suffixSuggestions = serverPackSuffix.suggestionProvider!!.allSuggestions()
        suffixSuggestions.add(serverPackSuffix.text)
        guiProps.storeGuiProperty(
            "autocomplete.${serverPackSuffix.identifier!!}",
            suffixSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        val clientModsSuggestions = exclusions.suggestionProvider!!.allSuggestions()
        clientModsSuggestions.addAll(exclusions.text.split(",").map { entry -> entry.trim { it <= ' ' } })
        guiProps.storeGuiProperty(
            "autocomplete.${exclusions.identifier}",
            clientModsSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        val javaArgsSuggestions = javaArgs.suggestionProvider!!.allSuggestions()
        javaArgsSuggestions.addAll(javaArgs.text.split(" ").map { entry -> entry.trim { it <= ' ' } })
        guiProps.storeGuiProperty(
            "autocomplete.${javaArgs.identifier}",
            javaArgsSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        val sourceSuggestions = source.suggestionProvider!!.allSuggestions()
        val destinationSuggestions = destination.suggestionProvider!!.allSuggestions()
        val inclusionSuggestions = inclusionFilter.suggestionProvider!!.allSuggestions()
        val exclusionSuggestions = exclusionFilter.suggestionProvider!!.allSuggestions()
        for (spec in inclusionsEditor.getServerFiles()) {
            sourceSuggestions.add(spec.source)
            spec.destination?.let { destinationSuggestions.add(it) }
            spec.inclusionFilter?.let { inclusionSuggestions.add(it) }
            spec.exclusionFilter?.let { exclusionSuggestions.add(it) }
        }
        sourceSuggestions.removeIf { entry -> entry.isBlank() }
        destinationSuggestions.removeIf { entry -> entry.isBlank() }
        inclusionSuggestions.removeIf { entry -> entry.isBlank() }
        exclusionSuggestions.removeIf { entry -> entry.isBlank() }
        guiProps.storeGuiProperty(
            "autocomplete.${source.identifier}",
            sourceSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        guiProps.storeGuiProperty(
            "autocomplete.${destination.identifier}",
            destinationSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        guiProps.storeGuiProperty(
            "autocomplete.${inclusionFilter.identifier}",
            inclusionSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        guiProps.storeGuiProperty(
            "autocomplete.${exclusionFilter.identifier}",
            exclusionSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })
    }

    /**
     * @author Griefed
     */
    override fun getJavaArguments(): String {
        return javaArgs.text
    }

    /**
     * @author Griefed
     */
    override fun getMinecraftVersion(): String {
        return minecraftVersions.selectedItem!!.toString()
    }

    /**
     * @author Griefed
     */
    override fun getModloader(): String {
        return modloaders.selectedItem!!.toString()
    }

    /**
     * @author Griefed
     */
    override fun getModloaderVersion(): String {
        return modloaderVersions.selectedItem!!.toString()
    }

    /**
     * @author Griefed
     */
    override fun getModpackDirectory(): String {
        return modpackDirectory.text
    }

    /**
     * @author Griefed
     */
    override fun getScriptSettings(): HashMap<String, String> {
        return scriptKVPairs.getData()
    }

    /**
     * @author Griefed
     */
    override fun getServerIconPath(): String {
        return iconFile.text
    }

    /**
     * @author Griefed
     */
    private fun resetClientMods() {
        val current = getClientSideModsList()
        val default = apiWrapper.apiProperties.clientSideMods()
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
                    setClientSideMods(set.toMutableList())
                }

                1 -> setClientSideMods(default)
            }
        } else {
            setClientSideMods(default)
        }
    }

    /**
     * @author Griefed
     */
    private fun setIcon() {
        if (iconQuickSelect.selectedIndex == 0) {
            return
        }
        val icon = iconQuickSelect.selectedItem
        @Suppress("KotlinConstantConditions")
        if (icon != null && icon.toString() != Gui.createserverpack_gui_quickselect_choose.toString()) {
            setServerIconPath(File(apiWrapper.apiProperties.iconsDirectory, icon.toString()).absolutePath)
            iconQuickSelect.selectedIndex = 0
        }
    }

    /**
     * @author Griefed
     */
    private fun setProperties() {
        if (propertiesQuickSelect.selectedIndex == 0) {
            return
        }
        val properties = propertiesQuickSelect.selectedItem
        @Suppress("KotlinConstantConditions")
        if (properties != null && properties.toString() != Gui.createserverpack_gui_quickselect_choose.toString()) {
            val serverProps = File(apiWrapper.apiProperties.propertiesDirectory, properties.toString())
            setServerPropertiesPath(serverProps.absolutePath)
            propertiesQuickSelect.selectedIndex = 0
        }
    }

    /**
     * @author Griefed
     */
    override fun getServerPackSuffix(): String {
        return apiWrapper.utilities!!.stringUtilities.pathSecureText(serverPackSuffix.text)
    }

    /**
     * @author Griefed
     */
    override fun getServerPropertiesPath(): String {
        return propertiesFile.text
    }

    /**
     * @author Griefed
     */
    override fun isMinecraftServerAvailable(): Boolean {
        return apiWrapper.versionMeta!!.minecraft.isServerAvailable(minecraftVersions.selectedItem!!.toString())
    }

    /**
     * @author Griefed
     */
    override fun isServerInstallationTicked(): Boolean {
        return includeServer.isSelected
    }

    /**
     * @author Griefed
     */
    override fun isServerIconInclusionTicked(): Boolean {
        return includeIcon.isSelected
    }

    /**
     * @author Griefed
     */
    override fun isServerPropertiesInclusionTicked(): Boolean {
        return includeProperties.isSelected
    }

    /**
     * @author Griefed
     */
    override fun isZipArchiveCreationTicked(): Boolean {
        return includeZip.isSelected
    }

    /**
     * @author Griefed
     */
    override fun clearScriptVariables() {
        scriptKVPairs.clearData()
    }

    /**
     * @author Griefed
     */
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
                0 -> setJavaArguments(apiWrapper.apiProperties.aikarsFlags)
                1 -> {}
                else -> {}
            }
        } else {
            setJavaArguments(apiWrapper.apiProperties.aikarsFlags)
        }
    }

    /**
     * @author Griefed
     */
    override fun validateInputFields() {
        timer.restart()
    }

    /**
     * @author Griefed
     */
    override fun acquireRequiredJavaVersion(): String {
        return if (apiWrapper.versionMeta!!.minecraft.getServer(getMinecraftVersion()).isPresent
            && apiWrapper.versionMeta!!.minecraft.getServer(getMinecraftVersion()).get().javaVersion().isPresent
        ) {
            apiWrapper.versionMeta!!.minecraft.getServer(getMinecraftVersion()).get().javaVersion().get().toString()
        } else {
            "?"
        }
    }

    /**
     * @author Griefed
     */
    fun compareSettings() {
        if (lastSavedConfig == null) {
            editorTitle.showWarningIcon()
            return
        }

        val currentConfig = getCurrentConfiguration()

        when {
            currentConfig.clientMods != lastSavedConfig!!.clientMods
                    || currentConfig.inclusions != lastSavedConfig!!.inclusions
                    || currentConfig.javaArgs != lastSavedConfig!!.javaArgs
                    || currentConfig.minecraftVersion != lastSavedConfig!!.minecraftVersion
                    || currentConfig.modloader != lastSavedConfig!!.modloader
                    || currentConfig.modloaderVersion != lastSavedConfig!!.modloaderVersion
                    || currentConfig.modpackDir != lastSavedConfig!!.modpackDir
                    || currentConfig.scriptSettings != lastSavedConfig!!.scriptSettings
                    || currentConfig.serverIconPath != lastSavedConfig!!.serverIconPath
                    || currentConfig.serverPropertiesPath != lastSavedConfig!!.serverPropertiesPath
                    || currentConfig.serverPackSuffix != lastSavedConfig!!.serverPackSuffix
                    || currentConfig.isServerIconInclusionDesired != lastSavedConfig!!.isServerIconInclusionDesired
                    || currentConfig.isServerPropertiesInclusionDesired != lastSavedConfig!!.isServerPropertiesInclusionDesired
                    || currentConfig.isServerInstallationDesired != lastSavedConfig!!.isServerInstallationDesired
                    || currentConfig.isZipCreationDesired != lastSavedConfig!!.isZipCreationDesired -> {
                editorTitle.showWarningIcon()
            }

            else -> {
                editorTitle.hideWarningIcon()
            }
        }
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into
     * ServerPackCreator.
     *
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun loadConfiguration(packConfig: PackConfig, confFile: File) {
        GlobalScope.launch(guiProps.configDispatcher) {
            try {
                setModpackDirectory(packConfig.modpackDir)
                if (packConfig.clientMods.isEmpty()) {
                    setClientSideMods(apiWrapper.apiProperties.clientSideMods())
                    log.debug("Set clientMods with fallback list.")
                } else {
                    setClientSideMods(packConfig.clientMods)
                }
                if (packConfig.inclusions.isEmpty()) {
                    val inclusions = mutableListOf<InclusionSpecification>()
                    inclusions.add(InclusionSpecification(("mods")))
                    inclusions.add(InclusionSpecification(("config")))
                    setInclusions(inclusions)
                } else {
                    setInclusions(packConfig.inclusions)
                }
                inclusionsEditor.updateIndex()
                setScriptVariables(packConfig.scriptSettings)
                setServerIconPath(packConfig.serverIconPath)
                setServerPropertiesPath(packConfig.serverPropertiesPath)
                if (packConfig.minecraftVersion.isEmpty()) {
                    packConfig.minecraftVersion = apiWrapper.versionMeta!!.minecraft.latestRelease().version
                }
                setMinecraftVersion(packConfig.minecraftVersion)
                setModloader(packConfig.modloader)
                setModloaderVersion(packConfig.modloaderVersion)
                setServerInstallationTicked(packConfig.isServerInstallationDesired)
                setIconInclusionTicked(packConfig.isServerIconInclusionDesired)
                setPropertiesInclusionTicked(packConfig.isServerPropertiesInclusionDesired)
                setZipArchiveCreationTicked(packConfig.isZipCreationDesired)
                setJavaArguments(packConfig.javaArgs)
                setServerPackSuffix(packConfig.serverPackSuffix)
                for (panel in pluginPanels) {
                    panel.setServerPackExtensionConfig(packConfig.getPluginConfigs(panel.pluginID))
                }
                lastSavedConfig = packConfig
                configFile = confFile
                editorTitle.hideWarningIcon()
            } catch (ex: Exception) {
                log.error("Couldn't load configuration file.", ex)
                JOptionPane.showMessageDialog(
                    this@ConfigEditor,
                    Gui.createserverpack_gui_config_load_error_message.toString() + " " + ex.cause + "   ",
                    Gui.createserverpack_gui_config_load_error.toString(),
                    JOptionPane.ERROR_MESSAGE,
                    guiProps.errorIcon
                )
            }
        }
    }

    /**
     * Set the modloader version combobox model depending on the currently selected modloader, with
     * the specified Minecraft version.
     *
     * @param minecraftVersion The Minecraft version to work with in the GUI update.
     *
     * @author Griefed
     */
    private fun setModloaderVersionsModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        when (modloaders.selectedIndex) {
            0 -> updateFabricModel(minecraftVersion)
            1 -> updateForgeModel(minecraftVersion)
            2 -> updateQuiltModel(minecraftVersion)
            3 -> updateLegacyFabricModel(minecraftVersion)
            4 -> updateNeoForgeModel(minecraftVersion)
            else -> {
                log.error("Invalid modloader selected.")
            }
        }
    }

    /**
     * @author Griefed
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
     * @author Griefed
     */
    private fun updateFabricModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta!!.fabric.isMinecraftSupported(minecraftVersion)) {
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
     * @author Griefed
     */
    private fun updateForgeModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta!!.forge.supportedForgeVersionsDescendingArray(minecraftVersion).isPresent) {
            setModloaderVersions(
                DefaultComboBoxModel(
                    apiWrapper.versionMeta!!.forge.supportedForgeVersionsDescendingArray(minecraftVersion).get()
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
     * @author Griefed
     */
    private fun updateNeoForgeModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta!!.neoForge.supportedNeoForgeVersionsDescendingArray(minecraftVersion).isPresent) {
            setModloaderVersions(
                DefaultComboBoxModel(
                    apiWrapper.versionMeta!!.neoForge.supportedNeoForgeVersionsDescendingArray(minecraftVersion).get()
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
     * @author Griefed
     */
    private fun updateQuiltModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta!!.fabric.isMinecraftSupported(minecraftVersion)) {
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
     * @author Griefed
     */
    private fun updateLegacyFabricModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta!!.legacyFabric.isMinecraftSupported(minecraftVersion)) {
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
    fun validateModpackDir(): List<String> {
        val errors: MutableList<String> = ArrayList(20)
        if (apiWrapper.configurationHandler!!.checkModpackDir(getModpackDirectory(), errors, false)) {
            modpackInfo.info()
        } else {
            modpackInfo.error("<html>${errors.joinToString("<br>")}</html>")
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
    fun validateSuffix(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (apiWrapper.utilities!!.stringUtilities.checkForIllegalCharacters(serverPackSuffix.text)) {
            suffixInfo.info()
        } else {
            errors.add(Gui.configuration_log_error_serverpack_suffix.toString())
            suffixInfo.error("<html>${errors.joinToString("<br>")}</html>")
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
    fun validateExclusions(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (!getClientSideMods().matches(guiProps.whitespace)) {
            exclusionsInfo.info()
        } else {
            errors.add(Gui.configuration_log_error_formatting.toString())
            exclusionsInfo.error("<html>${errors.joinToString("<br>")}</html>")
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
    fun validateInclusions(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        apiWrapper.configurationHandler!!.checkInclusions(
            getInclusions(),
            getModpackDirectory(),
            errors,
            false
        )
        if (errors.isNotEmpty()) {
            inclusionsInfo.error("<html>${errors.joinToString("<br>")}</html>")
        } else {
            inclusionsInfo.info()
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
    fun validateServerIcon(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (getServerIconPath().isNotEmpty()) {
            if (apiWrapper.configurationHandler!!.checkIconAndProperties(getServerIconPath())) {
                iconInfo.info()
                includeIconInfo.info()
                setIconPreview(File(getServerIconPath()), errors)
            } else {
                iconInfo.error(Gui.configuration_log_error_servericon_error.toString())
                includeIconInfo.error(Gui.configuration_log_warn_icon.toString())
                iconPreview.updateIcon(guiProps.iconError, true)
            }
        } else {
            setIconPreview(apiWrapper.apiProperties.defaultServerIcon, errors)
            iconInfo.info()
            includeIconInfo.info()
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
    fun validateServerProperties(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (apiWrapper.configurationHandler!!.checkIconAndProperties(getServerPropertiesPath())) {
            propertiesInfo.info()
            includePropertiesInfo.info()
        } else {
            propertiesInfo.error(Gui.configuration_log_warn_properties.toString())
            includePropertiesInfo.error(Gui.configuration_log_warn_properties.toString())
        }
        for (error in errors) {
            log.error(error)
        }
        return errors
    }

    /**
     * Updates the icon preview using the given [icon]-file.
     *
     * @author Griefed
     */
    private fun setIconPreview(icon: File, errors: MutableList<String>) {
        try {
            iconPreview.updateIcon(icon)
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
     *
     * @author Griefed
     */
    private fun getExtensionsConfigs(): HashMap<String, ArrayList<CommentedConfig>> {
        val configs: HashMap<String, ArrayList<CommentedConfig>> = HashMap(10)
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
        GlobalScope.launch(guiProps.configDispatcher) {
            val modpack = File(getModpackDirectory()).absoluteFile
            if (modpack.isDirectory) {
                try {
                    val updateMessage = StringBuilder()
                    val packConfig = PackConfig()
                    apiWrapper.configurationHandler!!.checkManifests(modpack.absolutePath, packConfig)
                    val inclusions = getInclusions()
                    val files = modpack.listFiles()
                    if (files != null && files.isNotEmpty()) {
                        for (file in files) {
                            if (apiWrapper.apiProperties.directoriesToInclude.contains(file.name) &&
                                !inclusions.any { inclusion -> inclusion.source == file.name }
                            ) {
                                inclusions.add(InclusionSpecification(file.name))
                            }
                        }
                    }
                    if (packConfig.minecraftVersion.isNotBlank()) {
                        setMinecraftVersion(packConfig.minecraftVersion)
                        updateMessage.append(Gui.createserverpack_gui_modpack_scan_minecraft(packConfig.minecraftVersion))
                            .append("\n")
                    }
                    if (packConfig.modloader.isNotBlank()) {
                        setModloader(packConfig.modloader)
                        updateMessage.append(Gui.createserverpack_gui_modpack_scan_modloader(packConfig.modloader))
                            .append("\n")
                    }
                    if (packConfig.modloaderVersion.isNotBlank()) {
                        setModloaderVersion(packConfig.modloaderVersion)
                        updateMessage.append(Gui.createserverpack_gui_modpack_scan_modloader_version(packConfig.modloaderVersion))
                            .append("\n")
                    }
                    if (packConfig.serverIconPath.isNotBlank()) {
                        setServerIconPath(packConfig.serverIconPath)
                        updateMessage.append(Gui.createserverpack_gui_modpack_scan_icon(packConfig.serverIconPath))
                            .append("\n")
                    }
                    if (inclusions.isNotEmpty()) {
                        setInclusions(ArrayList(inclusions))

                        updateMessage.append(
                            Gui.createserverpack_gui_modpack_scan_directories(
                                inclusions.joinToString(", ") { inclusion -> inclusion.source }
                            )
                        ).append("\n")
                    }
                    JOptionPane.showMessageDialog(
                        this@ConfigEditor,
                        Gui.createserverpack_gui_modpack_scan_message(updateMessage.toString()),
                        Gui.createserverpack_gui_modpack_scan.toString(),
                        JOptionPane.INFORMATION_MESSAGE,
                        guiProps.infoIcon
                    )
                } catch (ex: IOException) {
                    log.error("Couldn't update GUI from modpack manifests.", ex)
                }
            }
        }
    }

    /**
     * @author Griefed
     */
    private fun revertScriptKVPairs() {
        if (lastSavedConfig != null) {
            setScriptVariables(lastSavedConfig!!.scriptSettings)
        }
    }

    /**
     * @author Griefed
     */
    private fun resetScriptKVPairs() {
        setScriptVariables(guiProps.defaultScriptKVSetting)
    }

    /**
     * Reverts the list of clientside-only mods to the value of the last loaded configuration, if one
     * is available.
     *
     * @author Griefed
     */
    private fun revertExclusions() {
        if (lastSavedConfig != null) {
            setClientSideMods(lastSavedConfig!!.clientMods)
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
     * @author Griefed
     */
    private fun updateRequiredJavaVersion() {
        javaVersion.text = acquireRequiredJavaVersion()
        if (javaVersion.text != "?"
            && apiWrapper.apiProperties.javaPath(javaVersion.text).isPresent
            && !scriptKVPairs.getData()["SPC_JAVA_SPC"].equals(
                apiWrapper.apiProperties.javaPath(javaVersion.text).get()
            )
            && apiWrapper.apiProperties.isJavaScriptAutoupdateEnabled
        ) {
            val path = apiWrapper.apiProperties.javaPath(javaVersion.text).get()
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
        val server = apiWrapper.versionMeta!!.minecraft.getServer(mcVersion!!)
        if (!server.isPresent) {
            prepareServerInfo.warning(Gui.configuration_log_warn_server.toString())
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
        } else if (server.isPresent && !server.get().url().isPresent) {
            prepareServerInfo.warning(Gui.configuration_log_warn_server.toString())
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_url_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
        } else {
            prepareServerInfo.info()
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
     *
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
            if (!apiWrapper.serverPackHandler!!.serverDownloadable(mcVersion, modloader, modloaderVersion)) {
                val message = Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_message(
                    modloader,
                    mcVersion,
                    modloader,
                    modloaderVersion,
                    modloader
                ) + "    "
                val title = Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                    mcVersion,
                    modloader,
                    modloaderVersion
                )
                JOptionPane.showMessageDialog(
                    this.panel.parent.parent,
                    message,
                    title,
                    JOptionPane.WARNING_MESSAGE,
                    guiProps.largeWarningIcon
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
     *
     * @author Griefed
     */
    private fun checkJava(): Boolean {
        return if (!apiWrapper.apiProperties.javaAvailable()) {
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
            javaChooser.currentDirectory = apiWrapper.apiProperties.homeDirectory
        }
        javaChooser.dialogTitle = Gui.createserverpack_gui_buttonjavapath_tile.toString()
        javaChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        javaChooser.isAcceptAllFileFilterUsed = true
        javaChooser.isMultiSelectionEnabled = false
        javaChooser.preferredSize = Dimension(750, 450)
        if (javaChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            apiWrapper.apiProperties.javaPath = javaChooser.selectedFile.path
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
            apiWrapper.utilities!!.fileUtilities.openFile(getServerPropertiesPath())
        } else {
            apiWrapper.utilities!!.fileUtilities.openFile(apiWrapper.apiProperties.defaultServerProperties)
        }
    }

    /**
     * `true` if this tab has unsaved changes.
     *
     * @author Griefed
     */
    fun hasUnsavedChanges(): Boolean {
        return editorTitle.hasUnsavedChanges
    }

    /**
     * `true` if this is a new tab with default values and is unchanged.
     *
     * @author Griefed
     */
    fun isNewTab(): Boolean {
        @Suppress("KotlinConstantConditions")
        return editorTitle.title == Gui.createserverpack_gui_title_new.toString()
    }
}
