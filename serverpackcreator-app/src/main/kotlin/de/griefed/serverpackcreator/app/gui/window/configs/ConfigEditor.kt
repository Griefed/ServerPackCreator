/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.gui.window.configs

import Translations
import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.ListUtilities
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.*
import de.griefed.serverpackcreator.app.gui.window.configs.components.*
import de.griefed.serverpackcreator.app.gui.window.configs.components.advanced.AdvancedSettingsPanel
import de.griefed.serverpackcreator.app.gui.window.configs.components.advanced.ScriptKVPairs
import de.griefed.serverpackcreator.app.gui.window.configs.components.inclusions.InclusionsEditor
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Dimension
import java.awt.event.ActionListener
import java.io.File
import java.io.IOException
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
    private val tabbedConfigsTab: TabbedConfigsTab,
    private val apiWrapper: ApiWrapper,
    private val noVersions: DefaultComboBoxModel<String>,
    componentResizer: ComponentResizer
) : JScrollPane(), ServerPackConfigTab {

    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val panel = JPanel(
        MigLayout(
            "left,wrap",
            "[left,::64]5[left]5[left,grow]5[left,::64]5[left,::64]",
            "30"
        )
    )
    private val validationChangeListener = object : DocumentChangeListener { override fun update(e: DocumentEvent) { validateInputFields() }}
    private val validationActionListener = ActionListener { validateInputFields() }
    private val updateMinecraftActionListener = ActionListener { updateMinecraftValues() }
    private val legacyFabricModel = DefaultComboBoxModel(apiWrapper.versionMeta.legacyFabric.loaderVersions().toTypedArray())
    private val fabricModel = DefaultComboBoxModel(apiWrapper.versionMeta.fabric.loaderVersions().reversed().toTypedArray())
    private val quiltModel = DefaultComboBoxModel(apiWrapper.versionMeta.quilt.loaderVersions().reversed().toTypedArray())

    private val modpackIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelmodpackdir_tip.toString())
    private val modpackLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelmodpackdir.toString())
    private val modpackSetting = ScrollTextFileField(guiProps,apiWrapper.apiProperties.homeDirectory, FileFieldDropType.FOLDER_OR_ZIP, validationChangeListener)
    private val modpackChooser = BalloonTipButton(null, guiProps.folderIcon, Translations.createserverpack_gui_browser.toString(), guiProps) { selectModpackDirectory() }
    private val modpackCheck = BalloonTipButton(null, guiProps.inspectIcon,Translations.createserverpack_gui_buttonmodpackdir_scan_tip.toString(), guiProps) { updateGuiFromSelectedModpack() }

    private val propertiesIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelpropertiespath_tip.toString())
    private val propertiesLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelpropertiespath.toString())
    private val propertiesSetting = ScrollTextFileField(guiProps,apiWrapper.apiProperties.defaultServerProperties, FileFieldDropType.PROPERTIES, validationChangeListener)
    private val propertiesQuickSelectLabel = ElementLabel(Translations.createserverpack_gui_quickselect.toString())
    private val propertiesQuickSelect = QuickSelect(tabbedConfigsTab.propertiesQuickSelections()) { setProperties() }
    private val propertiesChooser = BalloonTipButton(null, guiProps.folderIcon, Translations.createserverpack_gui_browser.toString(), guiProps) { selectServerProperties() }
    private val propertiesOpen = BalloonTipButton(null, guiProps.openIcon, Translations.createserverpack_gui_createserverpack_button_open_properties.toString(), guiProps) { openServerProperties() }

    private val iconIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labeliconpath_tip.toString())
    private val iconLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labeliconpath.toString())
    private val iconSetting = ScrollTextFileField(guiProps,apiWrapper.apiProperties.defaultServerIcon, FileFieldDropType.IMAGE, validationChangeListener)
    private val iconQuickSelectLabel = ElementLabel(Translations.createserverpack_gui_quickselect.toString())
    private val iconQuickSelect = QuickSelect(tabbedConfigsTab.iconQuickSelections()) { setIcon() }
    private val iconChooser = BalloonTipButton(null, guiProps.folderIcon, Translations.createserverpack_gui_browser.toString(), guiProps) { selectServerIcon() }
    private val iconPreview = IconPreview(guiProps)

    private val advSetExclusionsSetting = ScrollTextArea(apiWrapper.apiProperties.clientSideMods().joinToString(","),Translations.createserverpack_gui_createserverpack_labelclientmods.toString(),validationChangeListener, guiProps)
    private val advSetWhitelistSetting = ScrollTextArea(apiWrapper.apiProperties.whitelistedMods().joinToString(","),Translations.createserverpack_gui_createserverpack_labelwhitelistmods.toString(),validationChangeListener, guiProps)

    private val inclusionsSourceSetting = ScrollTextField(guiProps, "", "source")
    private val inclusionsDestinationSetting = ScrollTextField(guiProps, "", "destination")
    private val inclusionsInclusionFilterSetting = ScrollTextField(guiProps, "", "inclusion")
    private val inclusionsExclusionFilterSetting = ScrollTextField(guiProps, "", "exclusion")
    private val inclusionsIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelcopydirs_tip.toString())
    private val inclusionsLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelcopydirs.toString())
    private val inclusionsSetting = InclusionsEditor(
        guiProps.defaultFileChooserDimension,
        guiProps,
        this,
        apiWrapper,
        inclusionsSourceSetting,
        inclusionsDestinationSetting,
        inclusionsInclusionFilterSetting,
        inclusionsExclusionFilterSetting,
        advSetExclusionsSetting,
        advSetWhitelistSetting
    )

    private val suffixIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelsuffix_tip.toString())
    private val suffixLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelsuffix.toString())
    private val suffixSetting = ScrollTextField(guiProps, "", "suffix", validationChangeListener)

    private val mcVersionIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelminecraft_tip.toString())
    private val mcVersionLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelminecraft.toString())
    private val mcVersionSetting = ActionComboBox(DefaultComboBoxModel(apiWrapper.versionMeta.minecraft.settingsDependantVersions()),updateMinecraftActionListener)

    private val javaVersionIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_minecraft_java_tooltip.toString())
    private val javaVersionLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_minecraft_java.toString(), 16)
    private val javaVersionInfo = ElementLabel("8", 16)

    private val modloaderIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelmodloader_tip.toString())
    private val modloaderLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelmodloader.toString())
    private val modloaderSetting = ActionComboBox(DefaultComboBoxModel(apiWrapper.apiProperties.supportedModloaders),updateMinecraftActionListener)

    private val includeIconIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_checkboxicon_tip.toString())
    private val includeIconSetting = ActionCheckBox(Translations.createserverpack_gui_createserverpack_checkboxicon.toString(),validationActionListener)

    private val zipIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_checkboxzip_tip.toString())
    private val zipSetting = ActionCheckBox(Translations.createserverpack_gui_createserverpack_checkboxzip.toString(),validationActionListener)

    private val modloaderVersionIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_labelmodloaderversion_tip.toString())
    private val modloaderVersionLabel = ElementLabel(Translations.createserverpack_gui_createserverpack_labelmodloaderversion.toString())
    private val modloaderVersionSetting = ActionComboBox<String> { validateInputFields() }

    private val includePropertiesIcon = StatusIcon(guiProps,Translations.createserverpack_gui_createserverpack_checkboxproperties_tip.toString())
    private val includePropertiesSetting = ActionCheckBox(Translations.createserverpack_gui_createserverpack_checkboxproperties.toString(),validationActionListener)

    private val advSetJavaArgsSetting = ScrollTextArea("-Xmx4G -Xms4G",Translations.createserverpack_gui_createserverpack_javaargs.toString(),validationChangeListener, guiProps)
    private val advSetScriptKVPairsSetting = ScriptKVPairs(guiProps, this)
    private val advSetPanel = AdvancedSettingsPanel(this, advSetExclusionsSetting, advSetWhitelistSetting, advSetJavaArgsSetting, advSetScriptKVPairsSetting, guiProps, apiWrapper.apiProperties)
    private val advSetCollapsible = CollapsiblePanel(Translations.createserverpack_gui_advanced.toString(), advSetPanel)

    private val pluginPanels = apiWrapper.apiPlugins.getConfigPanels(this).toMutableList()
    private val pluginSettings = PluginsSettingsPanel(pluginPanels)
    private val pluginPanel = CollapsiblePanel(Translations.createserverpack_gui_plugins.toString(), pluginSettings)

    private val modloaderVersionGuide = ThemedBalloonTip(modloaderVersionSetting,ElementLabel(Translations.firstrun_modloader_version.toString()),true, guiProps) {
        JOptionPane.showMessageDialog(
            panel,
            Translations.firstrun_finish_message.toString(),
            Translations.firstrun_finish_title.toString(),
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    private val modloaderGuide = ThemedBalloonTip(modloaderSetting,ElementLabel(Translations.firstrun_modloader.toString()),true, guiProps) {
        modloaderVersionGuide.isVisible = true
        modloaderVersionSetting.requestFocusInWindow()
    }
    private val mcVersionRequiredJavaGuide = ThemedBalloonTip(javaVersionLabel,ElementLabel(Translations.firstrun_java.toString()),true, guiProps) {
        modloaderGuide.isVisible = true
        modloaderSetting.requestFocusInWindow()
    }
    private val mcVersionGuide = ThemedBalloonTip(mcVersionSetting,ElementLabel(Translations.firstrun_minecraftversion.toString()),true, guiProps) {
        mcVersionRequiredJavaGuide.isVisible = true
    }
    private val inclusionsGuide = ThemedBalloonTip(inclusionsSourceSetting,ElementLabel(Translations.firstrun_inclusions.toString()),true, guiProps) {
        mcVersionGuide.isVisible = true
        mcVersionSetting.requestFocusInWindow()
    }
    private val modpackGuide = ThemedBalloonTip(modpackSetting,ElementLabel(Translations.firstrun_modpack.toString()),true, guiProps) {
        inclusionsGuide.isVisible = true
        inclusionsSourceSetting.highlight()
    }

    var iconQuickSelectModel: ComboBoxModel<String>
        set(value) {
            iconQuickSelect.model = value
        }
        get() {
            return iconQuickSelect.model
        }
    var propertiesQuickSelectModel: ComboBoxModel<String>
        set(value) {
            propertiesQuickSelect.model = value
        }
        get() {
            return propertiesQuickSelect.model
        }
    val title = ConfigEditorTitle(guiProps, tabbedConfigsTab, this)
    var lastConfig: PackConfig? = null
        private set
    var configFile: File? = null
        private set

    init {
        viewport.view = panel
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBar.unitIncrement = 10
        mcVersionSetting.selectedIndex = 0
        modloaderSetting.selectedIndex = 0
        updateMinecraftValues()

        // "cell column row width height"
        var column = 0
        // Modpack directory
        panel.add(modpackIcon, "cell 0 0,grow")
        panel.add(modpackLabel, "cell 1 0,grow")
        panel.add(modpackSetting, "cell 2 0,grow")
        panel.add(modpackChooser, "cell 3 0, h 30!,w 30!")
        panel.add(modpackCheck, "cell 4 0")

        // Server Properties
        column++ //1
        panel.add(propertiesIcon, "cell 0 $column,grow")
        panel.add(propertiesLabel, "cell 1 $column,grow")
        panel.add(propertiesSetting, "cell 2 $column, split 3,grow, w 50:50:")
        panel.add(propertiesQuickSelectLabel, "cell 2 $column")
        panel.add(propertiesQuickSelect, "cell 2 $column,w 200!")
        panel.add(propertiesChooser, "cell 3 $column")
        panel.add(propertiesOpen, "cell 4 $column")

        // Server Icon
        column++ //2
        panel.add(iconIcon, "cell 0 $column,grow")
        panel.add(iconLabel, "cell 1 $column,grow")
        panel.add(iconSetting, "cell 2 $column, split 2,grow, w 50:50:")
        panel.add(iconQuickSelectLabel, "cell 2 $column")
        panel.add(iconQuickSelect, "cell 2 $column,w 200!")
        panel.add(iconChooser, "cell 3 $column")
        panel.add(iconPreview, "cell 4 $column")

        // Server Files
        column++ //3
        panel.add(inclusionsIcon, "cell 0 $column 1 3")
        panel.add(inclusionsLabel, "cell 1 $column 1 3,grow")
        panel.add(inclusionsSetting, "cell 2 $column 3 3, grow, w 10:500:, h 150:225:300")

        // Server Pack Suffix
        column += 3 //6
        panel.add(suffixIcon, "cell 0 $column,grow")
        panel.add(suffixLabel, "cell 1 $column,grow")
        panel.add(suffixSetting, "cell 2 $column 3 1,grow")

        // Minecraft Version
        column++ //7
        panel.add(mcVersionIcon, "cell 0 $column,grow")
        panel.add(mcVersionLabel, "cell 1 $column,grow")
        panel.add(mcVersionSetting, "cell 2 $column,w 200!")

        // Java Version Of Minecraft Version
        panel.add(javaVersionIcon, "cell 2 $column, w 40!, gapleft 40")
        panel.add(javaVersionLabel, "cell 2 $column")
        panel.add(javaVersionInfo, "cell 2 $column, w 40!")

        // Modloader
        column++ //8
        panel.add(modloaderIcon, "cell 0 $column,grow")
        panel.add(modloaderLabel, "cell 1 $column,grow")
        panel.add(modloaderSetting, "cell 2 $column,w 200!")

        // Include Server Icon
        panel.add(includeIconIcon, "cell 2 $column, w 40!, gapleft 40,grow")
        panel.add(includeIconSetting, "cell 2 $column, w 200!")

        // Create ZIP Archive
        panel.add(zipIcon, "cell 2 $column, w 40!,grow")
        panel.add(zipSetting, "cell 2 $column, w 200!")

        // Modloader Version
        column++ //9
        panel.add(modloaderVersionIcon, "cell 0 $column,grow")
        panel.add(modloaderVersionLabel, "cell 1 $column,grow")
        panel.add(modloaderVersionSetting, "cell 2 $column,w 200!")

        // Include Server Properties
        panel.add(includePropertiesIcon, "cell 2 $column, w 40!, gapleft 40,grow")
        panel.add(includePropertiesSetting, "cell 2 $column, w 200!")

        // Advanced Settings
        column++ //10
        panel.add(advSetCollapsible, "cell 0 $column 5,grow")

        // Plugins
        column++ //11
        if (pluginPanels.isNotEmpty()) {
            panel.add(pluginPanel, "cell 0 $column 5,grow")
        }
        validateInputFields()
        lastConfig = getCurrentConfiguration()
        componentResizer.registerComponent(advSetExclusionsSetting, "cell 2 0 1 3,grow,w 10:500:,h %s!")
        componentResizer.registerComponent(advSetWhitelistSetting, "cell 2 3 1 3,grow,w 10:500:,h %s!")
        componentResizer.registerComponent(advSetJavaArgsSetting, "cell 2 6 1 3,grow,w 10:500:,h %s!")
        componentResizer.registerComponent(advSetScriptKVPairsSetting.scrollPanel, "cell 2 9 1 3,grow,w 10:500:,h %s!")
    }

    private fun selectModpackDirectory() {
        val chooser = if (guiProps.getPreference("lastmodpackchooserdir").isPresent) {
            ModpackChooser(File(guiProps.getPreference("lastmodpackchooserdir").get()), guiProps.defaultFileChooserDimension)
        } else if (File(getModpackDirectory()).isDirectory) {
            ModpackChooser(File(getModpackDirectory()), guiProps.defaultFileChooserDimension)
        } else if (File(getModpackDirectory()).isFile) {
            ModpackChooser(File(File(getModpackDirectory()).parent), guiProps.defaultFileChooserDimension)
        } else {
            ModpackChooser(guiProps.defaultFileChooserDimension)
        }

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            val filesInFolder = chooser.selectedFile.listFiles() ?: arrayOf()
            val fileNames = filesInFolder.map { file -> file.name }
            if (fileNames.contains("instance") && fileNames.contains("instance.json")) {
                if (JOptionPane.showConfirmDialog(
                        panel.parent,
                        Translations.createserverpack_gui_modpack_select_gdlauncher_message.toString(),
                        Translations.createserverpack_gui_modpack_select_gdlauncher_title.toString(),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        guiProps.infoIcon
                    ) == 0
                ) {
                    setModpackDirectory(File(chooser.selectedFile,"instance").path)
                } else {
                    setModpackDirectory(chooser.selectedFile.path)
                }
            } else {
                setModpackDirectory(chooser.selectedFile.path)
            }
            updateGuiFromSelectedModpack()
            log.debug("Selected modpack directory: " + chooser.selectedFile.path)
        }
    }

    private fun selectServerProperties() {
        val serverPropertiesChooser = if (guiProps.getPreference("lastserverpropertieschooserdir").isPresent) {
            ServerPropertiesChooser(File(guiProps.getPreference("lastserverpropertieschooserdir").get()), guiProps.defaultFileChooserDimension)
        } else if (File(getServerPropertiesPath()).isFile) {
            ServerPropertiesChooser(File(getServerPropertiesPath()), guiProps.defaultFileChooserDimension)
        } else {
            ServerPropertiesChooser(guiProps.defaultFileChooserDimension)
        }
        if (serverPropertiesChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            setServerPropertiesPath(serverPropertiesChooser.selectedFile.absolutePath)
        }
    }

    private fun selectServerIcon() {
        val serverIconChooser = if (guiProps.getPreference("lastservericonchooserdir").isPresent) {
            ServerIconChooser(File(guiProps.getPreference("lastservericonchooserdir").get()), guiProps.defaultFileChooserDimension)
        } else if (File(getServerIconPath()).isFile) {
            ServerIconChooser(File(getServerIconPath()), guiProps.defaultFileChooserDimension)
        } else {
            ServerIconChooser(guiProps.defaultFileChooserDimension)
        }
        if (serverIconChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            setServerIconPath(serverIconChooser.selectedFile.absolutePath)
        }
    }

    override fun setClientSideMods(entries: MutableList<String>) {
        advSetExclusionsSetting.text = StringUtilities.buildString(entries)
        validateInputFields()
    }

    override fun setWhitelist(entries: MutableList<String>) {
        advSetWhitelistSetting.text = StringUtilities.buildString(entries)
        validateInputFields()
    }

    override fun setInclusions(entries: MutableList<InclusionSpecification>) {
        inclusionsSetting.setServerFiles(entries)

    }

    override fun setIconInclusionTicked(ticked: Boolean) {
        includeIconSetting.isSelected = ticked
    }

    override fun setJavaArguments(javaArguments: String) {
        advSetJavaArgsSetting.text = javaArguments
    }

    override fun setMinecraftVersion(version: String) {
        for (i in 0 until mcVersionSetting.model.size) {
            if (mcVersionSetting.model.getElementAt(i) == version) {
                mcVersionSetting.selectedIndex = i
                break
            }
        }
    }

    override fun setModloader(modloader: String) {
        when (modloader) {
            "Fabric" -> modloaderSetting.selectedIndex = 0
            "Forge" -> modloaderSetting.selectedIndex = 1
            "NeoForge" -> modloaderSetting.selectedIndex = 4
            "Quilt" -> modloaderSetting.selectedIndex = 2
            "LegacyFabric" -> modloaderSetting.selectedIndex = 3
        }
        setModloaderVersionsModel()
    }

    override fun setModloaderVersion(version: String) {
        for (i in 0 until modloaderVersionSetting.model.size) {
            if (modloaderVersionSetting.model.getElementAt(i) == version) {
                modloaderVersionSetting.selectedIndex = i
                break
            }
        }
    }

    override fun setModpackDirectory(directory: String) {
        if (File(directory).parentFile.isDirectory) {
            guiProps.storePreference("lastmodpackchooserdir",File(directory).parent)
        }
        modpackSetting.text = directory
    }

    override fun setPropertiesInclusionTicked(ticked: Boolean) {
        includePropertiesSetting.isSelected = ticked
    }

    override fun setScriptVariables(variables: HashMap<String, String>) {
        advSetScriptKVPairsSetting.loadData(variables)
    }

    override fun setServerIconPath(path: String) {
        if (File(path).parentFile.isDirectory) {
            guiProps.storePreference("lastservericonchooserdir",File(path).parent)
        }
        iconSetting.text = path
    }

    override fun setServerPackSuffix(suffix: String) {
        suffixSetting.text = StringUtilities.pathSecureText(suffix)
    }

    override fun setServerPropertiesPath(path: String) {
        if (File(path).parentFile.isDirectory) {
            guiProps.storePreference("lastserverpropertieschooserdir",File(path).parent)
        }
        propertiesSetting.text = path
    }

    override fun setZipArchiveCreationTicked(ticked: Boolean) {
        zipSetting.isSelected = ticked
    }

    override fun getClientSideMods(): String {
        return advSetExclusionsSetting.text.replace(", ", ",")
    }

    override fun getWhitelist(): String {
        return advSetWhitelistSetting.text.replace(", ", ",")
    }

    override fun getClientSideModsList(): MutableList<String> {
        return ListUtilities.cleanList(
            getClientSideMods().split(",")
                .dropLastWhile { it.isEmpty() }
                .toMutableList()
        )
    }

    override fun getWhitelistList(): MutableList<String> {
        return ListUtilities.cleanList(
            getWhitelist().split(",")
                .dropLastWhile { it.isEmpty() }
                .toMutableList()
        )
    }

    override fun getInclusions(): MutableList<InclusionSpecification> {
        return inclusionsSetting.getServerFiles()
    }

    override fun getCurrentConfiguration(): PackConfig {
        return PackConfig(
            getClientSideModsList(),
            getWhitelistList(),
            getInclusions(),
            getModpackDirectory(),
            getMinecraftVersion(),
            getModloader(),
            getModloaderVersion(),
            getJavaArguments(),
            getServerPackSuffix(),
            getServerIconPath(),
            getServerPropertiesPath(),
            isServerIconInclusionTicked(),
            isServerPropertiesInclusionTicked(),
            isZipArchiveCreationTicked(),
            getScriptSettings(),
            getExtensionsConfigs()
        )
    }

    override fun saveCurrentConfiguration(): File {
        val modpackName = StringUtilities.pathSecureText( title.title + ".conf")
        val config = if (configFile != null) {
            configFile!!
        } else {
            File(apiWrapper.apiProperties.configsDirectory, modpackName)
        }
        lastConfig = getCurrentConfiguration().save(config)
        configFile = config
        title.hideWarningIcon()
        saveSuggestions()
        validateInputFields()
        return configFile!!
    }

    /**
     * @author Griefed
     */
    private fun saveSuggestions() {
        val suffixSuggestions = suffixSetting.suggestionProvider!!.allSuggestions()
        suffixSuggestions.add(suffixSetting.text)
        guiProps.storeGuiProperty(
            "autocomplete.${suffixSetting.identifier!!}",
            suffixSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        val clientModsSuggestions = advSetExclusionsSetting.suggestionProvider!!.allSuggestions()
        clientModsSuggestions.addAll(advSetExclusionsSetting.text.split(",").map { entry -> entry.trim { it <= ' ' } })
        guiProps.storeGuiProperty(
            "autocomplete.${advSetExclusionsSetting.identifier}",
            clientModsSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        val javaArgsSuggestions = advSetJavaArgsSetting.suggestionProvider!!.allSuggestions()
        javaArgsSuggestions.addAll(advSetJavaArgsSetting.text.split(" ").map { entry -> entry.trim { it <= ' ' } })
        guiProps.storeGuiProperty(
            "autocomplete.${advSetJavaArgsSetting.identifier}",
            javaArgsSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        inclusionsSetting.saveSuggestions()
    }

    override fun getJavaArguments(): String {
        return advSetJavaArgsSetting.text
    }

    override fun getMinecraftVersion(): String {
        return mcVersionSetting.selectedItem!!.toString()
    }

    override fun getModloader(): String {
        return modloaderSetting.selectedItem!!.toString()
    }

    override fun getModloaderVersion(): String {
        return modloaderVersionSetting.selectedItem!!.toString()
    }

    override fun getModpackDirectory(): String {
        return modpackSetting.text
    }

    override fun getScriptSettings(): HashMap<String, String> {
        return advSetScriptKVPairsSetting.getData()
    }

    override fun getServerIconPath(): String {
        return iconSetting.text
    }

    /**
     * @author Griefed
     */
    private fun setIcon() {
        if (iconQuickSelect.selectedIndex == 0) {
            return
        }
        val icon = iconQuickSelect.selectedItem
        if (icon != null && icon.toString() != Translations.createserverpack_gui_quickselect_choose.toString()) {
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
        if (properties != null && properties.toString() != Translations.createserverpack_gui_quickselect_choose.toString()) {
            val serverProps = File(apiWrapper.apiProperties.propertiesDirectory, properties.toString())
            setServerPropertiesPath(serverProps.absolutePath)
            propertiesQuickSelect.selectedIndex = 0
        }
    }

    override fun getServerPackSuffix(): String {
        return StringUtilities.pathSecureText(suffixSetting.text)
    }

    override fun getServerPropertiesPath(): String {
        return propertiesSetting.text
    }

    override fun isMinecraftServerAvailable(): Boolean {
        return apiWrapper.versionMeta.minecraft.isServerAvailable(mcVersionSetting.selectedItem!!.toString())
    }

    override fun isServerIconInclusionTicked(): Boolean {
        return includeIconSetting.isSelected
    }

    override fun isServerPropertiesInclusionTicked(): Boolean {
        return includePropertiesSetting.isSelected
    }

    override fun isZipArchiveCreationTicked(): Boolean {
        return zipSetting.isSelected
    }

    override fun clearScriptVariables() {
        advSetScriptKVPairsSetting.clearData()
    }

    override fun setAikarsFlagsAsJavaArguments() {
        if (getJavaArguments().isNotEmpty()) {
            when (JOptionPane.showConfirmDialog(
                this,
                Translations.createserverpack_gui_createserverpack_javaargs_confirm_message.toString(),
                Translations.createserverpack_gui_createserverpack_javaargs_confirm_title.toString(),
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

    override fun validateInputFields() {
        tabbedConfigsTab.checkAll()
    }

    override fun acquireRequiredJavaVersion(): String {
        val server = apiWrapper.versionMeta.minecraft.getServer(getMinecraftVersion())
        return if (server.isPresent && server.get().javaVersion().isPresent) {
            server.get().javaVersion().get().toString()
        } else {
            "?"
        }
    }

    /**
     * @author Griefed
     */
    fun compareSettings() {
        if (lastConfig == null) {
            title.showWarningIcon()
            return
        }

        val currentConfig = getCurrentConfiguration()

        when {
            currentConfig.clientMods != lastConfig!!.clientMods
                    || currentConfig.modsWhitelist != lastConfig!!.modsWhitelist
                    || currentConfig.inclusions != lastConfig!!.inclusions
                    || currentConfig.javaArgs != lastConfig!!.javaArgs
                    || currentConfig.minecraftVersion != lastConfig!!.minecraftVersion
                    || currentConfig.modloader != lastConfig!!.modloader
                    || currentConfig.modloaderVersion != lastConfig!!.modloaderVersion
                    || currentConfig.modpackDir != lastConfig!!.modpackDir
                    || currentConfig.scriptSettings != lastConfig!!.scriptSettings
                    || currentConfig.serverIconPath != lastConfig!!.serverIconPath
                    || currentConfig.serverPropertiesPath != lastConfig!!.serverPropertiesPath
                    || currentConfig.serverPackSuffix != lastConfig!!.serverPackSuffix
                    || currentConfig.isServerIconInclusionDesired != lastConfig!!.isServerIconInclusionDesired
                    || currentConfig.isServerPropertiesInclusionDesired != lastConfig!!.isServerPropertiesInclusionDesired
                    || currentConfig.isZipCreationDesired != lastConfig!!.isZipCreationDesired -> {
                title.showWarningIcon()
            }

            else -> {
                title.hideWarningIcon()
            }
        }
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into
     * ServerPackCreator.
     *
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun loadConfiguration(packConfig: PackConfig, confFile: File) {
        GlobalScope.launch(guiProps.configDispatcher, CoroutineStart.ATOMIC) {
            try {
                setModpackDirectory(packConfig.modpackDir)
                if (packConfig.clientMods.isEmpty()) {
                    setClientSideMods(apiWrapper.apiProperties.clientSideMods().toMutableList())
                    log.debug("Set clientMods to fallback list.")
                } else {
                    setClientSideMods(packConfig.clientMods)
                }
                if (packConfig.modsWhitelist.isEmpty()) {
                    setWhitelist(apiWrapper.apiProperties.whitelistedMods().toMutableList())
                    log.debug("Set whitelist to fallback list.")
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
                inclusionsSetting.updateIndex()
                setScriptVariables(packConfig.scriptSettings)
                setServerIconPath(packConfig.serverIconPath)
                setServerPropertiesPath(packConfig.serverPropertiesPath)
                if (packConfig.minecraftVersion.isEmpty()) {
                    packConfig.minecraftVersion = apiWrapper.versionMeta.minecraft.latestRelease().version
                }
                setMinecraftVersion(packConfig.minecraftVersion)
                setModloader(packConfig.modloader)
                setModloaderVersion(packConfig.modloaderVersion)
                setIconInclusionTicked(packConfig.isServerIconInclusionDesired)
                setPropertiesInclusionTicked(packConfig.isServerPropertiesInclusionDesired)
                setZipArchiveCreationTicked(packConfig.isZipCreationDesired)
                setJavaArguments(packConfig.javaArgs)
                setServerPackSuffix(packConfig.serverPackSuffix)
                for (panel in pluginPanels) {
                    panel.setServerPackExtensionConfig(packConfig.getPluginConfigs(panel.pluginID))
                }
                lastConfig = packConfig
                configFile = confFile
                title.hideWarningIcon()
            } catch (ex: Exception) {
                log.error("Couldn't load configuration file.", ex)
                JOptionPane.showMessageDialog(
                    this@ConfigEditor,
                    Translations.createserverpack_gui_config_load_error_message.toString() + " " + ex.cause + "   ",
                    Translations.createserverpack_gui_config_load_error.toString(),
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
    private fun setModloaderVersionsModel(minecraftVersion: String = mcVersionSetting.selectedItem!!.toString()) {
        when (modloaderSetting.selectedIndex) {
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
        tooltip: String = Translations.createserverpack_gui_createserverpack_labelmodloaderversion_tip.toString()
    ) {
        modloaderVersionIcon.icon = icon
        modloaderVersionIcon.toolTipText = tooltip
        modloaderVersionSetting.model = model
    }

    /**
     * @author Griefed
     */
    private fun updateFabricModel(minecraftVersion: String = mcVersionSetting.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
            setModloaderVersions(fabricModel)
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Translations.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * @author Griefed
     */
    private fun updateForgeModel(minecraftVersion: String = mcVersionSetting.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta.forge.isMinecraftVersionSupported(minecraftVersion)) {
            setModloaderVersions(
                DefaultComboBoxModel(
                    apiWrapper.versionMeta.forge.supportedForgeVersions(minecraftVersion).get().toTypedArray()
                )
            )
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Translations.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * @author Griefed
     */
    private fun updateNeoForgeModel(minecraftVersion: String = mcVersionSetting.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta.neoForge.isMinecraftVersionSupported(minecraftVersion)) {
            setModloaderVersions(
                DefaultComboBoxModel(
                    apiWrapper.versionMeta.neoForge.supportedNeoForgeVersions(minecraftVersion).get().toTypedArray()
                )
            )
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Translations.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * @author Griefed
     */
    private fun updateQuiltModel(minecraftVersion: String = mcVersionSetting.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
            setModloaderVersions(quiltModel)
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Translations.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * @author Griefed
     */
    private fun updateLegacyFabricModel(minecraftVersion: String = mcVersionSetting.selectedItem!!.toString()) {
        if (apiWrapper.versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) {
            setModloaderVersions(legacyFabricModel)
        } else {
            setModloaderVersions(
                noVersions,
                guiProps.errorIcon,
                Translations.configuration_log_error_minecraft_modloader(getMinecraftVersion(), getModloader())
            )
        }
    }

    /**
     * Validate the input field for modpack directory.
     *
     * @author Griefed
     */
    fun validateModpackDir(): List<String> {
        val check = ConfigCheck()
        if (apiWrapper.configurationHandler.checkModpackDir(getModpackDirectory(), check, false).modpackChecksPassed) {
            modpackIcon.info()
        } else {
            modpackIcon.error("<html>${check.modpackErrors.joinToString("<br>")}</html>")
        }
        for (error in check.modpackErrors) {
            log.error(error)
        }
        return check.modpackErrors
    }

    /**
     * Validate the input field for server pack suffix.
     *
     * @author Griefed
     */
    fun validateSuffix(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (StringUtilities.checkForIllegalCharacters(suffixSetting.text)) {
            suffixIcon.info()
        } else {
            errors.add(Translations.configuration_log_error_serverpack_suffix.toString())
            suffixIcon.error("<html>${errors.joinToString("<br>")}</html>")
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
        return advSetPanel.validateExclusions()
    }

    /**
     * Validate the input field for client mods.
     *
     * @author Griefed
     */
    fun validateWhitelist(): List<String> {
        return advSetPanel.validateWhitelist()
    }

    /**
     * Validate the input field for copy directories.
     *
     * @author Griefed
     */
    fun validateInclusions(): List<String> {
        val check = ConfigCheck()
        apiWrapper.configurationHandler.checkInclusions(
            getInclusions(),
            getModpackDirectory(),
            check,
            false
        )
        if (check.encounteredErrors.isNotEmpty()) {
            inclusionsIcon.error("<html>${check.encounteredErrors.joinToString("<br>")}</html>")
        } else {
            inclusionsIcon.info()
        }
        for (error in check.encounteredErrors) {
            log.error(error)
        }
        return check.encounteredErrors
    }

    /**
     * Validate the input field for server icon.
     *
     * @author Griefed
     */
    fun validateServerIcon(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        if (getServerIconPath().isNotEmpty()) {
            if (apiWrapper.configurationHandler.checkIconAndProperties(getServerIconPath())) {
                iconIcon.info()
                includeIconIcon.info()
                setIconPreview(File(getServerIconPath()), errors)
            } else {
                iconIcon.error(Translations.configuration_log_error_servericon_error.toString())
                includeIconIcon.error(Translations.configuration_log_warn_icon.toString())
                iconPreview.updateIcon(guiProps.iconError, true)
            }
        } else {
            setIconPreview(apiWrapper.apiProperties.defaultServerIcon, errors)
            iconIcon.info()
            includeIconIcon.info()
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
        if (apiWrapper.configurationHandler.checkIconAndProperties(getServerPropertiesPath())) {
            propertiesIcon.info()
            includePropertiesIcon.info()
        } else {
            propertiesIcon.error(Translations.configuration_log_warn_properties.toString())
            includePropertiesIcon.error(Translations.configuration_log_warn_properties.toString())
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
            errors.add(Translations.configuration_log_error_servericon_error.toString())
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
        GlobalScope.launch(Dispatchers.Swing, CoroutineStart.UNDISPATCHED) {
            val modpack = File(getModpackDirectory()).absoluteFile
            if (modpack.isDirectory) {
                try {
                    val updateMessage = StringBuilder()
                    val packConfig = apiWrapper.configurationHandler.generateConfigFromModpack(modpack)

                    if (packConfig.minecraftVersion.isNotBlank()) {
                        setMinecraftVersion(packConfig.minecraftVersion)
                        updateMessage.append(Translations.createserverpack_gui_modpack_scan_minecraft(packConfig.minecraftVersion))
                            .append("\n")
                    }
                    if (packConfig.modloader.isNotBlank()) {
                        setModloader(packConfig.modloader)
                        updateMessage.append(Translations.createserverpack_gui_modpack_scan_modloader(packConfig.modloader))
                            .append("\n")
                    }
                    if (packConfig.modloaderVersion.isNotBlank()) {
                        setModloaderVersion(packConfig.modloaderVersion)
                        updateMessage.append(Translations.createserverpack_gui_modpack_scan_modloader_version(packConfig.modloaderVersion))
                            .append("\n")
                    }
                    if (packConfig.serverIconPath.isNotBlank()) {
                        setServerIconPath(packConfig.serverIconPath)
                        updateMessage.append(Translations.createserverpack_gui_modpack_scan_icon(packConfig.serverIconPath))
                            .append("\n")
                    }
                    if (packConfig.inclusions.isNotEmpty()) {
                        setInclusions(ArrayList(packConfig.inclusions))
                        delay(100)
                        updateMessage.append(
                            Translations.createserverpack_gui_modpack_scan_directories(
                                packConfig.inclusions.joinToString(", ") { inclusion -> inclusion.source }
                            )
                        ).append("\n")
                    }
                    JOptionPane.showMessageDialog(
                        this@ConfigEditor,
                        Translations.createserverpack_gui_modpack_scan_message(updateMessage.toString()),
                        Translations.createserverpack_gui_modpack_scan.toString(),
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
     * Setter for the Minecraft version depending on which one is selected in the GUI.
     *
     * @author Griefed
     */
    private fun updateMinecraftValues() {
        setModloaderVersionsModel(mcVersionSetting.selectedItem!!.toString())
        updateRequiredJavaVersion()
        checkMinecraftServer()
        validateInputFields()
    }

    /**
     * @author Griefed
     */
    private fun updateRequiredJavaVersion() {
        javaVersionInfo.text = acquireRequiredJavaVersion()
        if (javaVersionInfo.text != "?"
            && apiWrapper.apiProperties.javaPath(javaVersionInfo.text).isPresent
            && !advSetScriptKVPairsSetting.getData()["SPC_JAVA_SPC"].equals(
                apiWrapper.apiProperties.javaPath(javaVersionInfo.text).get()
            )
            && apiWrapper.apiProperties.isJavaScriptAutoupdateEnabled
        ) {
            val path = apiWrapper.apiProperties.javaPath(javaVersionInfo.text).get()
            val data: HashMap<String, String> = advSetScriptKVPairsSetting.getData()
            data.replace(
                "SPC_JAVA_SPC", path
            )
            setScriptVariables(data)
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
        val mcVersion = mcVersionSetting.selectedItem?.toString()
        val server = apiWrapper.versionMeta.minecraft.getServer(mcVersion!!)
        if (!server.isPresent) {
            JOptionPane.showMessageDialog(
                this,
                Translations.createserverpack_gui_createserverpack_minecraft_server_unavailable(mcVersion) + "   ",
                Translations.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
        } else if (server.isPresent && !server.get().url().isPresent) {
            JOptionPane.showMessageDialog(
                this,
                Translations.createserverpack_gui_createserverpack_minecraft_server_url_unavailable(mcVersion) + "   ",
                Translations.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
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
    @OptIn(DelicateCoroutinesApi::class)
    fun checkServer(): Boolean {
        var okay = true
        if (modloaderVersionSetting.selectedItem == Translations.createserverpack_gui_createserverpack_forge_none.toString()) {
            return true
        }
        val mcVersion = mcVersionSetting.selectedItem!!.toString()
        val modloader = modloaderSetting.selectedItem!!.toString()
        val modloaderVersion = modloaderVersionSetting.selectedItem!!.toString()
        if (!apiWrapper.serverPackHandler.serverDownloadable(mcVersion, modloader, modloaderVersion)) {
            val message = Translations.createserverpack_gui_createserverpack_checkboxserver_unavailable_message(
                modloader,
                mcVersion,
                modloader,
                modloaderVersion,
                modloader
            ) + "    "
            val title = Translations.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                mcVersion,
                modloader,
                modloaderVersion
            )
            GlobalScope.launch(Dispatchers.Swing) {
                JOptionPane.showMessageDialog(
                    tabbedConfigsTab.panel,
                    message,
                    title,
                    JOptionPane.WARNING_MESSAGE,
                    guiProps.largeWarningIcon
                )
            }
            okay = false
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
    @Suppress("unused")
    private fun checkJava(): Boolean {
        return if (!apiWrapper.apiProperties.javaAvailable()) {
            when (JOptionPane.showConfirmDialog(
                this,
                Translations.createserverpack_gui_createserverpack_checkboxserver_confirm_message.toString(),
                Translations.createserverpack_gui_createserverpack_checkboxserver_confirm_title.toString(),
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
                        Translations.createserverpack_gui_createserverpack_checkboxserver_message_message.toString(),
                        Translations.createserverpack_gui_createserverpack_checkboxserver_message_title.toString(),
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
        javaChooser.isFileHidingEnabled = false
        javaChooser.dialogTitle = Translations.createserverpack_gui_buttonjavapath_tile.toString()
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
            FileUtilities.openFile(getServerPropertiesPath())
        } else {
            FileUtilities.openFile(apiWrapper.apiProperties.defaultServerProperties)
        }
    }

    /**
     * `true` if this tab has unsaved changes.
     *
     * @author Griefed
     */
    fun hasUnsavedChanges(): Boolean {
        return title.hasUnsavedChanges
    }

    /**
     * `true` if this is a new tab with default values and is unchanged.
     *
     * @author Griefed
     */
    fun isNewTab(): Boolean {
        return title.title == Translations.createserverpack_gui_title_new.toString()
    }

    /**
     * Run the user through a step-by-step guide to show them the ropes of configuration a server pack config from
     * which to generate a server pack.
     *
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun stepByStepGuide() {
        GlobalScope.launch(Dispatchers.Swing) {
            Thread.sleep(500)
            modpackGuide.isVisible = false
            inclusionsGuide.isVisible = false
            mcVersionGuide.isVisible = false
            mcVersionRequiredJavaGuide.isVisible = false
            modloaderGuide.isVisible = false
            modloaderVersionGuide.isVisible = false
            modpackSetting.highlight()
            modpackGuide.isVisible = true
        }
    }
}
