package de.griefed.serverpackcreator.gui.window.settings

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ExclusionFilter
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.configs.components.ComponentResizer
import de.griefed.serverpackcreator.gui.window.settings.components.*
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser

class GlobalSettings(guiProps: GuiProps, apiProperties: ApiProperties, componentResizer: ComponentResizer, mainFrame: MainFrame) : Editor("Global", guiProps) {

    val homeIcon = StatusIcon(guiProps, "Home directory setting")
    val homeLabel = ElementLabel("Home Directory")
    val homeSetting = ScrollTextFileField(guiProps, apiProperties.homeDirectory.absolutePath)
    val homeRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { homeSetting.file = apiProperties.homeDirectory }
    val homeReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { homeSetting.file = apiProperties.defaultHomeDirectory() }
    val homeChoose = BalloonTipButton(null,guiProps.folderIcon,"Select directory",guiProps) {
        val homeChooser = HomeDirChooser(apiProperties,"Home Directory Chooser")
        if (homeChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            homeSetting.file = homeChooser.selectedFile.absoluteFile
        }
    }

    val javaIcon = StatusIcon(guiProps, "Java used for server software installation")
    val javaLabel = ElementLabel("Java")
    val javaSetting = ScrollTextFileField(guiProps, apiProperties.javaPath)
    val javaRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { javaSetting.file = File(apiProperties.javaPath).absoluteFile }
    val javaReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { javaSetting.file = File(apiProperties.acquireJavaPath()).absoluteFile }
    val javaChoose = BalloonTipButton(null,guiProps.folderIcon,"Select executable",guiProps) {
        val javaChooser = JavaChooser(apiProperties,"Java Executable Chooser")
        if (javaChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            javaSetting.file = javaChooser.selectedFile.absoluteFile
        }
    }

    val serverPacksIcon = StatusIcon(guiProps, "Directory in which server packs are generated and stored in")
    val serverPacksLabel = ElementLabel("Server Packs Directory")
    val serverPacksSetting = ScrollTextFileField(guiProps, apiProperties.serverPacksDirectory.absolutePath)
    val serverPacksRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { serverPacksSetting.file = apiProperties.serverPacksDirectory }
    val serverPacksReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { serverPacksSetting.file = apiProperties.defaultServerPacksDirectory() }
    val serverPacksChoose = BalloonTipButton(null,guiProps.folderIcon,"Select directory",guiProps) {
        val serverPackDirChooser = ServerPackDirChooser(apiProperties,"Server Pack Directory Chooser")
        if (serverPackDirChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            serverPacksSetting.file = serverPackDirChooser.selectedFile.absoluteFile
        }
    }

    val zipIcon = StatusIcon(guiProps, "Exclusions from ZIP-archives")
    val zipLabel = ElementLabel("ZIP-Exclusions")
    val zipSetting = ScrollTextArea(apiProperties.zipArchiveExclusions.joinToString(", "),"ZIP-Exclusions",guiProps)
    val zipRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { zipSetting.text = apiProperties.zipArchiveExclusions.joinToString(", ") }
    val zipReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { zipSetting.text = apiProperties.fallbackZipExclusions.joinToString(",") }

    val inclusionsIcon = StatusIcon(guiProps, "Recommended inclusions in server pack")
    val inclusionsLabel = ElementLabel("Recommended Inclusions")
    val inclusionsSetting = ScrollTextArea(apiProperties.directoriesToInclude.joinToString(", "),"Recommended Inclusions",guiProps)
    val inclusionsRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { inclusionsSetting.text = apiProperties.directoriesToInclude.joinToString(", ") }
    val inclusionsReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { inclusionsSetting.text = apiProperties.fallbackDirectoriesInclusion.joinToString(",") }

    val aikarsIcon = StatusIcon(guiProps, "Global Aikars flags used when pressing the \"Use Aikars Flags\"-button in a server pack config tab")
    val aikarsLabel = ElementLabel("Global Aikars Flags")
    val aikarsSetting = ScrollTextArea(apiProperties.aikarsFlags,"Global Aikars Flags",guiProps)
    val aikarsRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { aikarsSetting.text = apiProperties.aikarsFlags }
    val aikarsReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { aikarsSetting.text = apiProperties.fallbackAikarsFlags }

    val scriptIcon = StatusIcon(guiProps, "Script-Templates setting")
    val scriptLabel = ElementLabel("Script Templates")
    val scriptSetting = ScrollTextArea(apiProperties.scriptTemplates.joinToString(", "),"Script Templates",guiProps)
    val scriptRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { scriptSetting.text = apiProperties.scriptTemplates.joinToString(", ") }
    val scriptReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { scriptSetting.text = apiProperties.defaultScriptTemplates().joinToString(", ") }
    val scriptChoose = BalloonTipButton(null,guiProps.folderIcon,"Select directory",guiProps) {
        val scriptChooser = ScriptTemplatesChooser(apiProperties,"Script Template Chooser")
        if (scriptChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            scriptSetting.text = scriptChooser.selectedFiles.joinToString(", ") { it.absolutePath }
        }
    }

    val fallbackURLIcon = StatusIcon(guiProps, "URL to .properties-file containing default clientside-mods-list.")
    val fallbackURLLabel = ElementLabel("Properties URL")
    val fallbackURLSetting = ScrollTextField(guiProps, apiProperties.updateUrl.toString())
    val fallbackURLRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { fallbackURLSetting.text = apiProperties.updateUrl.toString() }
    val fallbackURLReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { fallbackURLSetting.text = apiProperties.fallbackUpdateURL }

    val exclusionIcon = StatusIcon(guiProps, "Exclusion-filter to use for clientside-mods exclusions")
    val exclusionLabel = ElementLabel("Exclusion Method")
    val exclusionSetting = JComboBox(DefaultComboBoxModel(ExclusionFilter.values()))
    val exclusionRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { exclusionSetting.selectedItem = apiProperties.exclusionFilter }
    val exclusionReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { exclusionSetting.selectedItem = apiProperties.fallbackExclusionFilter }

    val languageIcon = StatusIcon(guiProps, "Change the language to use ServerPackCreator in")
    val languageLabel = ElementLabel("Language")
    val languageSetting = JComboBox(DefaultComboBoxModel(Gui.locales.toTypedArray()))
    val languageRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { languageSetting.selectedItem = apiProperties.i18n4kConfig.locale }
    val languageReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { languageSetting.selectedItem = apiProperties.i18n4kConfig.defaultLocale }

    val overwriteIcon = StatusIcon(guiProps, "Overwrite server pack if it already exists")
    val overwriteLabel = ElementLabel("Overwrite Server Pack")
    val overwriteSetting = JCheckBox()
    val overwriteRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { overwriteSetting.isSelected = apiProperties.isServerPacksOverwriteEnabled }
    val overwriteReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { overwriteSetting.isSelected = apiProperties.fallbackOverwriteEnabled }

    val javaVariableIcon = StatusIcon(guiProps, "Automatically update Java variable in scripts")
    val javaVariableLabel = ElementLabel("Update Java Variable")
    val javaVariableSetting = JCheckBox()
    val javaVariableRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { javaVariableSetting.isSelected = apiProperties.isJavaScriptAutoupdateEnabled }
    val javaVariableReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { javaVariableSetting.isSelected = apiProperties.fallbackJavaScriptAutoupdateEnabled }

    val prereleaseIcon = StatusIcon(guiProps, "Include pre-releases in update checks")
    val prereleaseLabel = ElementLabel("Check Pre-Releases")
    val prereleaseSetting = JCheckBox()
    val prereleaseRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { prereleaseSetting.isSelected = apiProperties.isCheckingForPreReleasesEnabled }
    val prereleaseReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { prereleaseSetting.isSelected = apiProperties.fallbackCheckingForPreReleasesEnabled }

    val zipExclusionsIcon = StatusIcon(guiProps, "Whether to exclude files and directories from ZIP-archives")
    val zipExclusionsLabel = ElementLabel("Run ZIP-Exclusions")
    val zipExclusionsSetting = JCheckBox()
    val zipExclusionsRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { zipExclusionsSetting.isSelected = apiProperties.isZipFileExclusionEnabled }
    val zipExclusionsReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { zipExclusionsSetting.isSelected = apiProperties.fallbackZipFileExclusionEnabled }

    val cleanupIcon = StatusIcon(guiProps, "Perform cleanup operations after server pack was generated")
    val cleanupLabel = ElementLabel("Server Pack Cleanups")
    val cleanupSetting = JCheckBox()
    val cleanupRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { cleanupSetting.isSelected = apiProperties.isServerPackCleanupEnabled }
    val cleanupReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { cleanupSetting.isSelected = apiProperties.fallbackServerPackCleanupEnabled }

    val snapshotsIcon = StatusIcon(guiProps, "Allow selection of Minecraft snapshots")
    val snapshotsLabel = ElementLabel("Minecraft Snapshots")
    val snapshotsSetting = JCheckBox()
    val snapshotsRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { snapshotsSetting.isSelected = apiProperties.isMinecraftPreReleasesAvailabilityEnabled }
    val snapshotsReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { snapshotsSetting.isSelected = apiProperties.fallbackMinecraftPreReleasesAvailabilityEnabled }

    val autodetectionIcon = StatusIcon(guiProps, "Let ServerPackCreator automatically detect and exclude clientside-mods")
    val autodetectionLabel = ElementLabel("Clientside Detection")
    val autodetectionSetting = JCheckBox()
    val autodetectionRevert = BalloonTipButton(null, guiProps.revertIcon, "Revert changes.", guiProps) { autodetectionSetting.isSelected = apiProperties.isAutoExcludingModsEnabled }
    val autodetectionReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { autodetectionSetting.isSelected = apiProperties.fallbackAutoExcludingModsEnabled }

    init {
        overwriteSetting.isSelected = apiProperties.isServerPacksOverwriteEnabled
        javaVariableSetting.isSelected = apiProperties.isJavaScriptAutoupdateEnabled
        prereleaseSetting.isSelected = apiProperties.isCheckingForPreReleasesEnabled
        zipExclusionsSetting.isSelected = apiProperties.isZipFileExclusionEnabled
        cleanupSetting.isSelected = apiProperties.isServerPackCleanupEnabled
        snapshotsSetting.isSelected = apiProperties.isMinecraftPreReleasesAvailabilityEnabled
        autodetectionSetting.isSelected = apiProperties.isAutoExcludingModsEnabled

        val zipY: Int
        val inclusionsY: Int
        val aikarsY: Int
        val scriptY: Int
        var y = 0
        panel.add(homeIcon, "cell 0 $y")
        panel.add(homeLabel, "cell 1 $y")
        panel.add(homeSetting, "cell 2 $y, grow")
        panel.add(homeRevert, "cell 3 $y")
        panel.add(homeReset, "cell 4 $y")
        panel.add(homeChoose, "cell 5 $y")

        y++
        panel.add(javaIcon, "cell 0 $y")
        panel.add(javaLabel, "cell 1 $y")
        panel.add(javaSetting, "cell 2 $y, grow")
        panel.add(javaRevert, "cell 3 $y")
        panel.add(javaReset, "cell 4 $y")
        panel.add(javaChoose, "cell 5 $y")

        y++
        panel.add(serverPacksIcon, "cell 0 $y")
        panel.add(serverPacksLabel, "cell 1 $y")
        panel.add(serverPacksSetting, "cell 2 $y, grow")
        panel.add(serverPacksRevert, "cell 3 $y")
        panel.add(serverPacksReset, "cell 4 $y")
        panel.add(serverPacksChoose, "cell 5 $y")

        y++
        zipY = y
        panel.add(zipIcon, "cell 0 $y")
        panel.add(zipLabel, "cell 1 $y")
        panel.add(zipSetting, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(zipRevert, "cell 3 $y")
        panel.add(zipReset, "cell 4 $y")

        y++
        inclusionsY = y
        panel.add(inclusionsIcon, "cell 0 $y")
        panel.add(inclusionsLabel, "cell 1 $y")
        panel.add(inclusionsSetting, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(inclusionsRevert, "cell 3 $y")
        panel.add(inclusionsReset, "cell 4 $y")

        y++
        aikarsY = y
        panel.add(aikarsIcon, "cell 0 $y")
        panel.add(aikarsLabel, "cell 1 $y")
        panel.add(aikarsSetting, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(aikarsRevert, "cell 3 $y")
        panel.add(aikarsReset, "cell 4 $y")

        y++
        scriptY = y
        panel.add(scriptIcon, "cell 0 $y")
        panel.add(scriptLabel, "cell 1 $y")
        panel.add(scriptSetting, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(scriptRevert, "cell 3 $y")
        panel.add(scriptReset, "cell 4 $y")
        panel.add(scriptChoose, "cell 5 $y")

        y++
        panel.add(fallbackURLIcon, "cell 0 $y")
        panel.add(fallbackURLLabel, "cell 1 $y")
        panel.add(fallbackURLSetting, "cell 2 $y, grow")
        panel.add(fallbackURLRevert, "cell 3 $y")
        panel.add(fallbackURLReset, "cell 4 $y")

        y++
        panel.add(exclusionIcon, "cell 0 $y")
        panel.add(exclusionLabel, "cell 1 $y")
        panel.add(exclusionSetting, "cell 2 $y, grow")
        panel.add(exclusionRevert, "cell 3 $y")
        panel.add(exclusionReset, "cell 4 $y")

        y++
        panel.add(languageIcon, "cell 0 $y")
        panel.add(languageLabel, "cell 1 $y")
        panel.add(languageSetting, "cell 2 $y, grow")
        panel.add(languageRevert, "cell 3 $y")
        panel.add(languageReset, "cell 4 $y")

        y++
        panel.add(overwriteIcon, "cell 0 $y")
        panel.add(overwriteLabel, "cell 1 $y")
        panel.add(overwriteSetting, "cell 2 $y, grow")
        panel.add(overwriteRevert, "cell 3 $y")
        panel.add(overwriteReset, "cell 4 $y")

        y++
        panel.add(javaVariableIcon, "cell 0 $y")
        panel.add(javaVariableLabel, "cell 1 $y")
        panel.add(javaVariableSetting, "cell 2 $y, grow")
        panel.add(javaVariableRevert, "cell 3 $y")
        panel.add(javaVariableReset, "cell 4 $y")

        y++
        panel.add(prereleaseIcon, "cell 0 $y")
        panel.add(prereleaseLabel, "cell 1 $y")
        panel.add(prereleaseSetting, "cell 2 $y, grow")
        panel.add(prereleaseRevert, "cell 3 $y")
        panel.add(prereleaseReset, "cell 4 $y")

        y++
        panel.add(zipExclusionsIcon, "cell 0 $y")
        panel.add(zipExclusionsLabel, "cell 1 $y")
        panel.add(zipExclusionsSetting, "cell 2 $y, grow")
        panel.add(zipExclusionsRevert, "cell 3 $y")
        panel.add(zipExclusionsReset, "cell 4 $y")

        y++
        panel.add(cleanupIcon, "cell 0 $y")
        panel.add(cleanupLabel, "cell 1 $y")
        panel.add(cleanupSetting, "cell 2 $y, grow")
        panel.add(cleanupRevert, "cell 3 $y")
        panel.add(cleanupReset, "cell 4 $y")

        y++
        panel.add(snapshotsIcon, "cell 0 $y")
        panel.add(snapshotsLabel, "cell 1 $y")
        panel.add(snapshotsSetting, "cell 2 $y, grow")
        panel.add(snapshotsRevert, "cell 3 $y")
        panel.add(snapshotsReset, "cell 4 $y")

        y++
        panel.add(autodetectionIcon, "cell 0 $y")
        panel.add(autodetectionLabel, "cell 1 $y")
        panel.add(autodetectionSetting, "cell 2 $y, grow")
        panel.add(autodetectionRevert, "cell 3 $y")
        panel.add(autodetectionReset, "cell 4 $y")

        componentResizer.registerComponent(zipSetting,"cell 2 $zipY, grow, w 10:500:,h %s!")
        componentResizer.registerComponent(inclusionsSetting,"cell 2 $inclusionsY, grow, w 10:500:,h %s!")
        componentResizer.registerComponent(aikarsSetting,"cell 2 $aikarsY, grow, w 10:500:,h %s!")
        componentResizer.registerComponent(scriptSetting,"cell 2 $scriptY, grow, w 10:500:,h %s!")
    }

    override fun getSettings(): HashMap<String, Any> {
        TODO("Not yet implemented")
    }

    override fun loadSettings(settings: HashMap<String, Any>) {
        TODO("Not yet implemented")
    }

    override fun restoreSettings() {
        TODO("Not yet implemented")
    }

    override fun loadDefaults() {
        TODO("Not yet implemented")
    }

    override fun importSettings() {
        TODO("Not yet implemented")
    }
}