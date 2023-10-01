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
package de.griefed.serverpackcreator.gui.window.settings

import Gui
import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ExclusionFilter
import de.griefed.serverpackcreator.api.utilities.common.testFileWrite
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.configs.components.ComponentResizer
import de.griefed.serverpackcreator.gui.window.settings.components.*
import java.awt.event.ActionListener
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JFileChooser
import javax.swing.JOptionPane

/**
 * @author Griefed
 */
class GlobalSettings(
    private val guiProps: GuiProps,
    private val apiProperties: ApiProperties,
    componentResizer: ComponentResizer,
    mainFrame: MainFrame,
    changeListener: DocumentChangeListener,
    actionListener: ActionListener
) : Editor(Gui.settings_global.toString(), guiProps) {

    val homeIcon = StatusIcon(guiProps, Gui.settings_global_home_tooltip.toString())
    val homeLabel = ElementLabel(Gui.settings_global_home_label.toString())
    val homeSetting = ScrollTextFileField(guiProps, apiProperties.homeDirectory.absoluteFile, changeListener)
    val homeRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { homeSetting.file = apiProperties.homeDirectory }
    val homeReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { homeSetting.file = apiProperties.defaultHomeDirectory() }
    val homeChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_select_directory.toString(),guiProps) {
        val homeChooser = HomeDirChooser(apiProperties,Gui.settings_global_home_chooser.toString())
        if (homeChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (homeChooser.selectedFile.absoluteFile.testFileWrite()) {
                homeSetting.file = homeChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Gui.settings_directory_error(homeChooser.selectedFile.absolutePath)
                )
            }
        }
    }

    val javaIcon = StatusIcon(guiProps, Gui.settings_global_java_tooltip.toString())
    val javaLabel = ElementLabel(Gui.settings_global_java_label.toString())
    val javaSetting = ScrollTextFileField(guiProps, File(apiProperties.javaPath).absoluteFile, changeListener)
    val javaRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { javaSetting.file = File(apiProperties.javaPath).absoluteFile }
    val javaReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { javaSetting.file = File(apiProperties.acquireJavaPath()).absoluteFile }
    val javaChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_global_java_executable.toString(),guiProps) {
        val javaChooser = JavaChooser(apiProperties,Gui.settings_global_java_chooser.toString())
        if (javaChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            javaSetting.file = javaChooser.selectedFile.absoluteFile
        }
    }

    val serverPacksIcon = StatusIcon(guiProps, Gui.settings_global_serverpacks_tooltip.toString())
    val serverPacksLabel = ElementLabel(Gui.settings_global_serverpacks_label.toString())
    val serverPacksSetting = ScrollTextFileField(guiProps, apiProperties.serverPacksDirectory.absoluteFile, changeListener)
    val serverPacksRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { serverPacksSetting.file = apiProperties.serverPacksDirectory }
    val serverPacksReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { serverPacksSetting.file = apiProperties.defaultServerPacksDirectory() }
    val serverPacksChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_select_directory.toString(),guiProps) {
        val serverPackDirChooser = ServerPackDirChooser(apiProperties,Gui.settings_global_serverpacks_chooser.toString())
        if (serverPackDirChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (serverPackDirChooser.selectedFile.absoluteFile.testFileWrite()) {
                serverPacksSetting.file = serverPackDirChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Gui.settings_directory_error(serverPackDirChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    val zipIcon = StatusIcon(guiProps, Gui.settings_global_zip_tooltip.toString())
    val zipLabel = ElementLabel(Gui.settings_global_zip_label.toString())
    val zipSetting = ScrollTextArea(apiProperties.zipArchiveExclusions.joinToString(", "),Gui.settings_global_zip_label.toString(), changeListener, guiProps)
    val zipRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { zipSetting.text = apiProperties.zipArchiveExclusions.joinToString(", ") }
    val zipReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { zipSetting.text = apiProperties.fallbackZipExclusions.joinToString(",") }

    val inclusionsIcon = StatusIcon(guiProps, Gui.settings_global_inclusions_tooltip.toString())
    val inclusionsLabel = ElementLabel(Gui.settings_global_inclusions_label.toString())
    val inclusionsSetting = ScrollTextArea(apiProperties.directoriesToInclude.joinToString(", "),Gui.settings_global_inclusions_label.toString(), changeListener, guiProps)
    val inclusionsRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { inclusionsSetting.text = apiProperties.directoriesToInclude.joinToString(", ") }
    val inclusionsReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { inclusionsSetting.text = apiProperties.fallbackDirectoriesInclusion.joinToString(",") }

    val aikarsIcon = StatusIcon(guiProps, Gui.settings_global_aikars_tooltip.toString())
    val aikarsLabel = ElementLabel(Gui.settings_global_aikars_label.toString())
    val aikarsSetting = ScrollTextArea(apiProperties.aikarsFlags,Gui.settings_global_aikars_label.toString(), changeListener, guiProps)
    val aikarsRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { aikarsSetting.text = apiProperties.aikarsFlags }
    val aikarsReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { aikarsSetting.text = apiProperties.fallbackAikarsFlags }

    val scriptIcon = StatusIcon(guiProps, Gui.settings_global_scripts_tooltip.toString())
    val scriptLabel = ElementLabel(Gui.settings_global_scripts_label.toString())
    val scriptSetting = ScrollTextArea(apiProperties.scriptTemplates.joinToString(", "),Gui.settings_global_scripts_label.toString(), changeListener, guiProps)
    val scriptRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { scriptSetting.text = apiProperties.scriptTemplates.joinToString(", ") }
    val scriptReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { scriptSetting.text = apiProperties.defaultScriptTemplates().joinToString(", ") }
    val scriptChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_select_directory.toString(),guiProps) {
        val scriptChooser = ScriptTemplatesChooser(apiProperties,Gui.settings_global_scripts_chooser.toString())
        if (scriptChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            scriptSetting.text = scriptChooser.selectedFiles.joinToString(", ") { it.absolutePath }
        }
    }

    val fallbackURLIcon = StatusIcon(guiProps, Gui.settings_global_fallbackurl_tooltip.toString())
    val fallbackURLLabel = ElementLabel(Gui.settings_global_fallbackurl_label.toString())
    val fallbackURLSetting = ScrollTextField(guiProps,apiProperties.updateUrl.toString(),Gui.settings_global_fallbackurl_label.toString(),changeListener)
    val fallbackURLRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { fallbackURLSetting.text = apiProperties.updateUrl.toString() }
    val fallbackURLReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { fallbackURLSetting.text = apiProperties.fallbackUpdateURL }

    val exclusionIcon = StatusIcon(guiProps, Gui.settings_global_exclusions_tooltip.toString())
    val exclusionLabel = ElementLabel(Gui.settings_global_exclusions_label.toString())
    val exclusionSetting = ActionComboBox<ExclusionFilter>(DefaultComboBoxModel(ExclusionFilter.values()), actionListener)
    val exclusionRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { exclusionSetting.selectedItem = apiProperties.exclusionFilter }
    val exclusionReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { exclusionSetting.selectedItem = apiProperties.fallbackExclusionFilter }

    val languageIcon = StatusIcon(guiProps, Gui.settings_global_language_tooltip.toString())
    val languageLabel = ElementLabel(Gui.settings_global_language_label.toString())
    val languageSetting = ActionComboBox<Locale>(DefaultComboBoxModel(Gui.locales.toTypedArray()), actionListener)
    val languageRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { languageSetting.selectedItem = apiProperties.i18n4kConfig.locale }
    val languageReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { languageSetting.selectedItem = apiProperties.i18n4kConfig.defaultLocale }

    val overwriteIcon = StatusIcon(guiProps, Gui.settings_global_overwrite_tooltip.toString())
    val overwriteLabel = ElementLabel(Gui.settings_global_overwrite_label.toString())
    val overwriteSetting = ActionCheckBox(actionListener)
    val overwriteRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { overwriteSetting.isSelected = apiProperties.isServerPacksOverwriteEnabled }
    val overwriteReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { overwriteSetting.isSelected = apiProperties.fallbackOverwriteEnabled }

    val javaVariableIcon = StatusIcon(guiProps, Gui.settings_global_scriptjava_tooltip.toString())
    val javaVariableLabel = ElementLabel(Gui.settings_global_scriptjava_label.toString())
    val javaVariableSetting = ActionCheckBox(actionListener)
    val javaVariableRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { javaVariableSetting.isSelected = apiProperties.isJavaScriptAutoupdateEnabled }
    val javaVariableReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { javaVariableSetting.isSelected = apiProperties.fallbackJavaScriptAutoupdateEnabled }

    val prereleaseIcon = StatusIcon(guiProps, Gui.settings_global_prerelease_tooltip.toString())
    val prereleaseLabel = ElementLabel(Gui.settings_global_prerelease_label.toString())
    val prereleaseSetting = ActionCheckBox(actionListener)
    val prereleaseRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { prereleaseSetting.isSelected = apiProperties.isCheckingForPreReleasesEnabled }
    val prereleaseReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { prereleaseSetting.isSelected = apiProperties.fallbackCheckingForPreReleasesEnabled }

    val zipExclusionsIcon = StatusIcon(guiProps, Gui.settings_global_zipexclusions_tooltip.toString())
    val zipExclusionsLabel = ElementLabel(Gui.settings_global_zipexclusions_label.toString())
    val zipExclusionsSetting = ActionCheckBox(actionListener)
    val zipExclusionsRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { zipExclusionsSetting.isSelected = apiProperties.isZipFileExclusionEnabled }
    val zipExclusionsReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { zipExclusionsSetting.isSelected = apiProperties.fallbackZipFileExclusionEnabled }

    val cleanupIcon = StatusIcon(guiProps, Gui.settings_global_cleanup_tooltip.toString())
    val cleanupLabel = ElementLabel(Gui.settings_global_cleanup_label.toString())
    val cleanupSetting = ActionCheckBox(actionListener)
    val cleanupRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { cleanupSetting.isSelected = apiProperties.isServerPackCleanupEnabled }
    val cleanupReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { cleanupSetting.isSelected = apiProperties.fallbackServerPackCleanupEnabled }

    val snapshotsIcon = StatusIcon(guiProps, Gui.settings_global_snapshots_tooltip.toString())
    val snapshotsLabel = ElementLabel(Gui.settings_global_snapshots_label.toString())
    val snapshotsSetting = ActionCheckBox(actionListener)
    val snapshotsRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { snapshotsSetting.isSelected = apiProperties.isMinecraftPreReleasesAvailabilityEnabled }
    val snapshotsReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { snapshotsSetting.isSelected = apiProperties.fallbackMinecraftPreReleasesAvailabilityEnabled }

    val autodetectionIcon = StatusIcon(guiProps, Gui.settings_global_autodetection_tooltip.toString())
    val autodetectionLabel = ElementLabel(Gui.settings_global_autodetection_label.toString())
    val autodetectionSetting = ActionCheckBox(actionListener)
    val autodetectionRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.settings_revert.toString(), guiProps) { autodetectionSetting.isSelected = apiProperties.isAutoExcludingModsEnabled }
    val autodetectionReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { autodetectionSetting.isSelected = apiProperties.fallbackAutoExcludingModsEnabled }

    init {
        loadSettings()
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

    /**
     * @author Griefed
     */
    override fun loadSettings() {
        homeSetting.file = apiProperties.homeDirectory.absoluteFile
        javaSetting.file = File(apiProperties.javaPath).absoluteFile
        serverPacksSetting.file = apiProperties.serverPacksDirectory.absoluteFile
        zipSetting.text = apiProperties.zipArchiveExclusions.joinToString(", ")
        inclusionsSetting.text = apiProperties.directoriesToInclude.joinToString(", ")
        aikarsSetting.text = apiProperties.aikarsFlags
        scriptSetting.text = apiProperties.scriptTemplates.joinToString(", ")
        fallbackURLSetting.text = apiProperties.updateUrl.toString()
        exclusionSetting.selectedItem = apiProperties.exclusionFilter
        languageSetting.selectedItem = apiProperties.i18n4kConfig.locale
        overwriteSetting.isSelected = apiProperties.isServerPacksOverwriteEnabled
        javaVariableSetting.isSelected = apiProperties.isJavaScriptAutoupdateEnabled
        prereleaseSetting.isSelected = apiProperties.isCheckingForPreReleasesEnabled
        zipExclusionsSetting.isSelected = apiProperties.isZipFileExclusionEnabled
        cleanupSetting.isSelected = apiProperties.isServerPackCleanupEnabled
        snapshotsSetting.isSelected = apiProperties.isMinecraftPreReleasesAvailabilityEnabled
        autodetectionSetting.isSelected = apiProperties.isAutoExcludingModsEnabled
    }

    /**
     * @author Griefed
     */
    override fun saveSettings() {
        apiProperties.homeDirectory = homeSetting.file.absoluteFile
        apiProperties.javaPath = javaSetting.file.absolutePath
        apiProperties.serverPacksDirectory = serverPacksSetting.file.absoluteFile
        apiProperties.zipArchiveExclusions = TreeSet(zipSetting.text.replace(", ",",").split(","))
        apiProperties.directoriesToInclude = TreeSet(inclusionsSetting.text.replace(", ",",").split(","))
        apiProperties.aikarsFlags = aikarsSetting.text
        apiProperties.scriptTemplates = TreeSet(scriptSetting.text.replace(", ",",").split(",").map { File(it).absoluteFile })
        apiProperties.updateUrl = URL(fallbackURLSetting.text)
        apiProperties.exclusionFilter = exclusionSetting.selectedItem as ExclusionFilter
        apiProperties.language = languageSetting.selectedItem as Locale
        apiProperties.isServerPacksOverwriteEnabled = overwriteSetting.isSelected
        apiProperties.isJavaScriptAutoupdateEnabled = javaVariableSetting.isSelected
        apiProperties.isCheckingForPreReleasesEnabled = prereleaseSetting.isSelected
        apiProperties.isZipFileExclusionEnabled = zipExclusionsSetting.isSelected
        apiProperties.isServerPackCleanupEnabled = cleanupSetting.isSelected
        apiProperties.isMinecraftPreReleasesAvailabilityEnabled = snapshotsSetting.isSelected
        apiProperties.isAutoExcludingModsEnabled = autodetectionSetting.isSelected
    }

    /**
     * @author Griefed
     */
    override fun validateSettings(): List<String> {
        val errors = mutableListOf<String>()
        if (!homeSetting.file.absoluteFile.isDirectory || !homeSetting.file.absoluteFile.canWrite()) {
            homeIcon.error(Gui.settings_check_home.toString())
            errors.add(Gui.settings_check_home.toString())
        } else {
            homeIcon.info()
        }

        if (!javaSetting.file.absoluteFile.isFile || !javaSetting.file.absoluteFile.canRead() || !javaSetting.file.absoluteFile.canExecute()) {
            javaIcon.error(Gui.settings_check_java.toString())
            errors.add(Gui.settings_check_java.toString())
        } else {
            javaIcon.info()
        }

        if (!serverPacksSetting.file.absoluteFile.isDirectory || !serverPacksSetting.file.absoluteFile.canWrite()) {
            serverPacksIcon.error(Gui.settings_check_serverpacks.toString())
            errors.add(Gui.settings_check_serverpacks.toString())
        } else {
            serverPacksIcon.info()
        }

        if (zipSetting.text.matches(guiProps.whitespace)) {
            zipIcon.error(Gui.settings_check_whitespace.toString())
            errors.add(Gui.settings_check_whitespace.toString())
        } else {
            zipIcon.info()
        }

        if (inclusionsSetting.text.matches(guiProps.whitespace)) {
            inclusionsIcon.error(Gui.settings_check_whitespace.toString())
            errors.add(Gui.settings_check_whitespace.toString())
        } else {
            inclusionsIcon.info()
        }

        if (scriptSetting.text.matches(guiProps.whitespace)) {
            scriptIcon.error(Gui.settings_check_whitespace.toString())
            errors.add(Gui.settings_check_whitespace.toString())
        } else {
            scriptIcon.info()
        }

        try {
            URL(fallbackURLSetting.text)
            fallbackURLIcon.info()
        } catch (ex: MalformedURLException) {
            fallbackURLIcon.error(Gui.settings_check_fallbackurl.toString())
            errors.add(Gui.settings_check_fallbackurl.toString())
        }
        return errors.toList()
    }

    /**
     * @author Griefed
     */
    override fun hasUnsavedChanges(): Boolean {
        val changes = homeSetting.file.absolutePath != apiProperties.homeDirectory.absolutePath ||
        javaSetting.file.absolutePath != File(apiProperties.javaPath).absolutePath ||
        serverPacksSetting.file.absolutePath != apiProperties.serverPacksDirectory.absolutePath ||
        zipSetting.text != apiProperties.zipArchiveExclusions.joinToString(", ") ||
        inclusionsSetting.text != apiProperties.directoriesToInclude.joinToString(", ") ||
        aikarsSetting.text != apiProperties.aikarsFlags ||
        scriptSetting.text != apiProperties.scriptTemplates.joinToString(", ") ||
        fallbackURLSetting.text != apiProperties.updateUrl.toString() ||
        exclusionSetting.selectedItem.toString() != apiProperties.exclusionFilter.toString() ||
        languageSetting.selectedItem.toString().lowercase() != apiProperties.i18n4kConfig.locale.toString().lowercase() ||
        overwriteSetting.isSelected != apiProperties.isServerPacksOverwriteEnabled ||
        javaVariableSetting.isSelected != apiProperties.isJavaScriptAutoupdateEnabled ||
        prereleaseSetting.isSelected != apiProperties.isCheckingForPreReleasesEnabled ||
        zipExclusionsSetting.isSelected != apiProperties.isZipFileExclusionEnabled ||
        cleanupSetting.isSelected != apiProperties.isServerPackCleanupEnabled ||
        snapshotsSetting.isSelected != apiProperties.isMinecraftPreReleasesAvailabilityEnabled ||
        autodetectionSetting.isSelected != apiProperties.isAutoExcludingModsEnabled
        if (changes) {
            title.showWarningIcon()
        } else {
            title.hideWarningIcon()
        }
        return changes
    }
}