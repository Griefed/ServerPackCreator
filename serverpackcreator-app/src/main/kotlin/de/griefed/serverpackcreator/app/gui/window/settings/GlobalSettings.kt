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
package de.griefed.serverpackcreator.app.gui.window.settings

import Translations
import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.utilities.common.testFileWrite
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.*
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.configs.components.ComponentResizer
import de.griefed.serverpackcreator.app.gui.window.settings.components.*
import java.awt.event.ActionListener
import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.event.TableModelListener

/**
 * @author Griefed
 */
class GlobalSettings(
    private val guiProps: GuiProps,
    private val apiProperties: ApiProperties,
    componentResizer: ComponentResizer,
    private val mainFrame: MainFrame,
    changeListener: DocumentChangeListener,
    actionListener: ActionListener,
    tableModelListener: TableModelListener
) : Editor(Translations.settings_global.toString(), guiProps) {

    private val homeIcon = StatusIcon(guiProps, Translations.settings_global_home_tooltip.toString())
    private val homeLabel = ElementLabel(Translations.settings_global_home_label.toString())
    private val homeSetting = ScrollTextFileField(guiProps, apiProperties.homeDirectory.absoluteFile, FileFieldDropType.FOLDER, changeListener)
    private val homeRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { homeSetting.file = apiProperties.homeDirectory }
    private val homeReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { homeSetting.file = apiProperties.home }
    private val homeChoose = BalloonTipButton(null, guiProps.folderIcon,Translations.settings_select_directory.toString(), guiProps) {
        val homeChooser = HomeDirChooser(apiProperties,Translations.settings_global_home_chooser.toString())
        if (homeChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (homeChooser.selectedFile.absoluteFile.testFileWrite()) {
                homeSetting.file = homeChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Translations.settings_directory_error(homeChooser.selectedFile.absolutePath)
                )
            }
        }
    }

    /*
    private val javaIcon = StatusIcon(guiProps, Translations.settings_global_java_tooltip.toString())
    private val javaLabel = ElementLabel(Translations.settings_global_java_label.toString())
    private val javaSetting = ScrollTextFileField(guiProps, File(apiProperties.javaPath).absoluteFile, changeListener)
    private val javaRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { javaSetting.file = File(apiProperties.javaPath).absoluteFile }
    private val javaReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { javaSetting.file = File(apiProperties.acquireJavaPath()).absoluteFile }
    private val javaChoose = BalloonTipButton(null, guiProps.folderIcon,Translations.settings_global_java_executable.toString(), guiProps) {
        val javaChooser = JavaChooser(apiProperties,Translations.settings_global_java_chooser.toString())
        if (javaChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            javaSetting.file = javaChooser.selectedFile.absoluteFile
        }
    }
    */

    private val serverPacksIcon = StatusIcon(guiProps, Translations.settings_global_serverpacks_tooltip.toString())
    private val serverPacksLabel = ElementLabel(Translations.settings_global_serverpacks_label.toString())
    private val serverPacksSetting = ScrollTextFileField(guiProps, apiProperties.serverPacksDirectory.absoluteFile, FileFieldDropType.FOLDER, changeListener)
    private val serverPacksRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { serverPacksSetting.file = apiProperties.serverPacksDirectory }
    private val serverPacksReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { serverPacksSetting.file = apiProperties.defaultServerPacksDirectory() }
    private val serverPacksChoose = BalloonTipButton(null, guiProps.folderIcon,Translations.settings_select_directory.toString(), guiProps) {
        val serverPackDirChooser = ServerPackDirChooser(apiProperties,Translations.settings_global_serverpacks_chooser.toString())
        if (serverPackDirChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (serverPackDirChooser.selectedFile.absoluteFile.testFileWrite()) {
                serverPacksSetting.file = serverPackDirChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Translations.settings_directory_error(serverPackDirChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    private val zipIcon = StatusIcon(guiProps, Translations.settings_global_zip_tooltip.toString())
    private val zipLabel = ElementLabel(Translations.settings_global_zip_label.toString())
    private val zipSetting = ScrollTextArea(apiProperties.zipArchiveExclusions.joinToString(", "),Translations.settings_global_zip_label.toString(), changeListener, guiProps)
    private val zipRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { zipSetting.text = apiProperties.zipArchiveExclusions.joinToString(", ") }
    private val zipReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { zipSetting.text = apiProperties.fallbackZipExclusions.joinToString(",") }

    private val inclusionsIcon = StatusIcon(guiProps, Translations.settings_global_inclusions_tooltip.toString())
    private val inclusionsLabel = ElementLabel(Translations.settings_global_inclusions_label.toString())
    private val inclusionsSetting = ScrollTextArea(apiProperties.directoriesToInclude.joinToString(", "),Translations.settings_global_inclusions_label.toString(), changeListener, guiProps)
    private val inclusionsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { inclusionsSetting.text = apiProperties.directoriesToInclude.joinToString(", ") }
    private val inclusionsReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { inclusionsSetting.text = apiProperties.fallbackDirectoriesInclusion.joinToString(",") }

    private val aikarsIcon = StatusIcon(guiProps, Translations.settings_global_aikars_tooltip.toString())
    private val aikarsLabel = ElementLabel(Translations.settings_global_aikars_label.toString())
    private val aikarsSetting = ScrollTextArea(apiProperties.aikarsFlags,Translations.settings_global_aikars_label.toString(), changeListener, guiProps)
    private val aikarsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { aikarsSetting.text = apiProperties.aikarsFlags }
    private val aikarsReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { aikarsSetting.text = apiProperties.fallbackAikarsFlags }

    private val scriptIcon = StatusIcon(guiProps, Translations.settings_global_scripts_tooltip.toString())
    private val scriptLabel = ElementLabel(Translations.settings_global_scripts_label.toString())
    private val scriptSetting = ScriptTemplates(guiProps, tableModelListener)
    private val scriptReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { scriptSetting.loadData(apiProperties.defaultStartScriptTemplates()) }

    private val javaScriptIcon = StatusIcon(guiProps, Translations.settings_global_javascripts_tooltip.toString())
    private val javaScriptLabel = ElementLabel(Translations.settings_global_javascripts_label.toString())
    private val javaScriptSetting = JavaScriptTemplates(guiProps, tableModelListener)
    private val javaScriptReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { javaScriptSetting.loadData(apiProperties.defaultJavaScriptTemplates()) }

    private val fallbackURLIcon = StatusIcon(guiProps, Translations.settings_global_fallbackurl_tooltip.toString())
    private val fallbackURLLabel = ElementLabel(Translations.settings_global_fallbackurl_label.toString())
    private val fallbackURLSetting = ScrollTextField(guiProps,apiProperties.updateUrl.toString(),Translations.settings_global_fallbackurl_label.toString(),changeListener)
    private val fallbackURLRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { fallbackURLSetting.text = apiProperties.updateUrl.toString() }
    private val fallbackURLReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { fallbackURLSetting.text = apiProperties.fallbackUpdateURL }

    private val exclusionIcon = StatusIcon(guiProps, Translations.settings_global_exclusions_tooltip.toString())
    private val exclusionLabel = ElementLabel(Translations.settings_global_exclusions_label.toString())
    private val exclusionSetting = ActionComboBox<ExclusionFilter>(DefaultComboBoxModel(ExclusionFilter.entries.toTypedArray()), actionListener)
    private val exclusionRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { exclusionSetting.selectedItem = apiProperties.exclusionFilter }
    private val exclusionReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { exclusionSetting.selectedItem = apiProperties.fallbackExclusionFilter }

    private val languageIcon = StatusIcon(guiProps, Translations.settings_global_language_tooltip.toString())
    private val languageLabel = ElementLabel(Translations.settings_global_language_label.toString())
    private val languageSetting = ActionComboBox<Locale>(DefaultComboBoxModel(Translations.locales.toTypedArray()), actionListener)
    private val languageRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { languageSetting.selectedItem = apiProperties.i18n4kConfig.locale }
    private val languageReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { languageSetting.selectedItem = apiProperties.i18n4kConfig.defaultLocale }

    private val logLevelIcon = StatusIcon(guiProps, Translations.settings_global_loglevel_tooltip.toString())
    private val logLevelLabel = ElementLabel(Translations.settings_global_loglevel_label.toString())
    private val logLevelSetting = ActionComboBox<String>(DefaultComboBoxModel(arrayOf("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL")), actionListener)
    private val logLevelRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { logLevelSetting.selectedItem = apiProperties.logLevel }
    private val logLevelReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { logLevelSetting.selectedItem = if (apiProperties.devBuild || apiProperties.preRelease) { "DEBUG" } else { "INFO" } }

    private val overwriteIcon = StatusIcon(guiProps, Translations.settings_global_overwrite_tooltip.toString())
    private val overwriteLabel = ElementLabel(Translations.settings_global_overwrite_label.toString())
    private val overwriteSetting = ActionCheckBox(actionListener)
    private val overwriteRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { overwriteSetting.isSelected = apiProperties.isServerPacksOverwriteEnabled }
    private val overwriteReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { overwriteSetting.isSelected = apiProperties.fallbackOverwriteEnabled }

    private val updateServerPackIcon = StatusIcon(guiProps, Translations.settings_global_updateserverpack_tooltip.toString())
    private val updateServerPackLabel = ElementLabel(Translations.settings_global_updateserverpack_label.toString())
    private val updateServerPackSetting = ActionCheckBox(actionListener)
    private val updateServerPackRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { updateServerPackSetting.isSelected = apiProperties.isUpdatingServerPacksEnabled }
    private val updateServerPackReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { updateServerPackSetting.isSelected = apiProperties.fallbackUpdateServerPack }

    private val javaVariableIcon = StatusIcon(guiProps, Translations.settings_global_scriptjava_tooltip.toString())
    private val javaVariableLabel = ElementLabel(Translations.settings_global_scriptjava_label.toString())
    private val javaVariableSetting = ActionCheckBox(actionListener)
    private val javaVariableRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { javaVariableSetting.isSelected = apiProperties.isJavaScriptAutoupdateEnabled }
    private val javaVariableReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { javaVariableSetting.isSelected = apiProperties.fallbackJavaScriptAutoupdateEnabled }

    private val prereleaseIcon = StatusIcon(guiProps, Translations.settings_global_prerelease_tooltip.toString())
    private val prereleaseLabel = ElementLabel(Translations.settings_global_prerelease_label.toString())
    private val prereleaseSetting = ActionCheckBox(actionListener)
    private val prereleaseRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { prereleaseSetting.isSelected = apiProperties.isCheckingForPreReleasesEnabled }
    private val prereleaseReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { prereleaseSetting.isSelected = apiProperties.fallbackCheckingForPreReleasesEnabled }

    private val zipExclusionsIcon = StatusIcon(guiProps, Translations.settings_global_zipexclusions_tooltip.toString())
    private val zipExclusionsLabel = ElementLabel(Translations.settings_global_zipexclusions_label.toString())
    private val zipExclusionsSetting = ActionCheckBox(actionListener)
    private val zipExclusionsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { zipExclusionsSetting.isSelected = apiProperties.isZipFileExclusionEnabled }
    private val zipExclusionsReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { zipExclusionsSetting.isSelected = apiProperties.fallbackZipFileExclusionEnabled }

    private val cleanupIcon = StatusIcon(guiProps, Translations.settings_global_cleanup_tooltip.toString())
    private val cleanupLabel = ElementLabel(Translations.settings_global_cleanup_label.toString())
    private val cleanupSetting = ActionCheckBox(actionListener)
    private val cleanupRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { cleanupSetting.isSelected = apiProperties.isServerPackCleanupEnabled }
    private val cleanupReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { cleanupSetting.isSelected = apiProperties.fallbackServerPackCleanupEnabled }

    private val snapshotsIcon = StatusIcon(guiProps, Translations.settings_global_snapshots_tooltip.toString())
    private val snapshotsLabel = ElementLabel(Translations.settings_global_snapshots_label.toString())
    private val snapshotsSetting = ActionCheckBox(actionListener)
    private val snapshotsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { snapshotsSetting.isSelected = apiProperties.isMinecraftPreReleasesAvailabilityEnabled }
    private val snapshotsReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { snapshotsSetting.isSelected = apiProperties.fallbackMinecraftPreReleasesAvailabilityEnabled }

    private val autodetectionIcon = StatusIcon(guiProps, Translations.settings_global_autodetection_tooltip.toString())
    private val autodetectionLabel = ElementLabel(Translations.settings_global_autodetection_label.toString())
    private val autodetectionSetting = ActionCheckBox(actionListener)
    private val autodetectionRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { autodetectionSetting.isSelected = apiProperties.isAutoExcludingModsEnabled }
    private val autodetectionReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { autodetectionSetting.isSelected = apiProperties.fallbackAutoExcludingModsEnabled }

    private val preInstallFilesIcon = StatusIcon(guiProps, Translations.settings_global_install_files_pre_tooltip.toString())
    private val preInstallFilesLabel = ElementLabel(Translations.settings_global_install_files_pre_label.toString())
    private val preInstallFilesSetting = ScrollTextArea(apiProperties.preInstallCleanupFiles.joinToString(", "),Translations.settings_global_install_files_pre_label.toString(), changeListener, guiProps)
    private val preInstallFilesRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { preInstallFilesSetting.text = apiProperties.preInstallCleanupFiles.joinToString(", ") }
    private val preInstallFilesReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { preInstallFilesSetting.text = apiProperties.fallbackPreInstallCleanupFiles.joinToString(",") }

    private val postInstallFilesIcon = StatusIcon(guiProps, Translations.settings_global_install_files_post_tooltip.toString())
    private val postInstallFilesLabel = ElementLabel(Translations.settings_global_install_files_post_label.toString())
    private val postInstallFilesSetting = ScrollTextArea(apiProperties.postInstallCleanupFiles.joinToString(", "),Translations.settings_global_install_files_post_label.toString(), changeListener, guiProps)
    private val postInstallFilesRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { postInstallFilesSetting.text = apiProperties.postInstallCleanupFiles.joinToString(", ") }
    private val postInstallFilesReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { postInstallFilesSetting.text = apiProperties.fallbackPostInstallCleanupFiles.joinToString(",") }

    private val javaPathsIcon = StatusIcon(guiProps, Translations.settings_global_javapaths_tooltip.toString())
    private val javaPathsLabel = ElementLabel(Translations.settings_global_javapaths_label.toString())
    private val javaPathsSetting = JavaPaths(guiProps, tableModelListener)
    private val javaPathsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) { javaPathsSetting.loadData(apiProperties.javaPaths) }

    private val ensureUpdateOverwriteSetting = ActionListener { changeUpdateSettingState() }

    init {
        overwriteSetting.addActionListener(ensureUpdateOverwriteSetting)
        loadSettings()
        val zipY: Int
        val inclusionsY: Int
        val aikarsY: Int
        val scriptY: Int
        val javaScriptY: Int
        val preInstallY: Int
        val postInstallY: Int
        val javaPathsY: Int
        var y = 0

        panel.add(homeIcon, "cell 0 0")
        panel.add(homeLabel, "cell 1 0")
        panel.add(homeSetting, "cell 2 0, grow")
        panel.add(homeRevert, "cell 3 0")
        panel.add(homeReset, "cell 4 0")
        panel.add(homeChoose, "cell 5 0")

        /*
        y++
        panel.add(javaIcon, "cell 0 $y")
        panel.add(javaLabel, "cell 1 $y")
        panel.add(javaSetting, "cell 2 $y, grow")
        panel.add(javaRevert, "cell 3 $y")
        panel.add(javaReset, "cell 4 $y")
        panel.add(javaChoose, "cell 5 $y")
        */

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
        preInstallY = y
        panel.add(preInstallFilesIcon, "cell 0 $y")
        panel.add(preInstallFilesLabel, "cell 1 $y")
        panel.add(preInstallFilesSetting, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(preInstallFilesRevert, "cell 3 $y")
        panel.add(preInstallFilesReset, "cell 4 $y")

        y++
        postInstallY = y
        panel.add(postInstallFilesIcon, "cell 0 $y")
        panel.add(postInstallFilesLabel, "cell 1 $y")
        panel.add(postInstallFilesSetting, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(postInstallFilesRevert, "cell 3 $y")
        panel.add(postInstallFilesReset, "cell 4 $y")

        y++
        scriptY = y
        panel.add(scriptIcon, "cell 0 $y")
        panel.add(scriptLabel, "cell 1 $y")
        panel.add(scriptSetting.scrollPanel, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(scriptReset, "cell 3 $y")

        y++
        javaScriptY = y
        panel.add(javaScriptIcon, "cell 0 $y")
        panel.add(javaScriptLabel, "cell 1 $y")
        panel.add(javaScriptSetting.scrollPanel, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(javaScriptReset, "cell 3 $y")

        y++
        javaPathsY = y
        panel.add(javaPathsIcon, "cell 0 $y")
        panel.add(javaPathsLabel, "cell 1 $y")
        panel.add(javaPathsSetting.scrollPanel, "cell 2 $y, grow, w 10:500:,h 150!")
        panel.add(javaPathsRevert, "cell 3 $y")

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
        panel.add(logLevelIcon, "cell 0 $y")
        panel.add(logLevelLabel, "cell 1 $y")
        panel.add(logLevelSetting, "cell 2 $y, grow")
        panel.add(logLevelRevert, "cell 3 $y")
        panel.add(logLevelReset, "cell 4 $y")

        y++
        panel.add(overwriteIcon, "cell 0 $y")
        panel.add(overwriteLabel, "cell 1 $y")
        panel.add(overwriteSetting, "cell 2 $y, grow")
        panel.add(overwriteRevert, "cell 3 $y")
        panel.add(overwriteReset, "cell 4 $y")

        y++
        panel.add(updateServerPackIcon, "cell 0 $y")
        panel.add(updateServerPackLabel, "cell 1 $y")
        panel.add(updateServerPackSetting, "cell 2 $y, grow")
        panel.add(updateServerPackRevert, "cell 3 $y")
        panel.add(updateServerPackReset, "cell 4 $y")

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
        componentResizer.registerComponent(scriptSetting.scrollPanel,"cell 2 $scriptY, grow, w 10:500:,h %s!")
        componentResizer.registerComponent(javaScriptSetting.scrollPanel,"cell 2 $javaScriptY, grow, w 10:500:,h %s!")
        componentResizer.registerComponent(preInstallFilesSetting,"cell 2 $preInstallY, grow, w 10:500:,h %s!")
        componentResizer.registerComponent(postInstallFilesSetting,"cell 2 $postInstallY, grow, w 10:500:,h %s!")
        componentResizer.registerComponent(javaPathsSetting.scrollPanel,"cell 2 $javaPathsY, grow, w 10:500:,h %s!")
    }

    override fun loadSettings() {
        homeSetting.file = apiProperties.homeDirectory.absoluteFile
        //javaSetting.file = File(apiProperties.javaPath).absoluteFile
        serverPacksSetting.file = apiProperties.serverPacksDirectory.absoluteFile
        zipSetting.text = apiProperties.zipArchiveExclusions.joinToString(", ")
        inclusionsSetting.text = apiProperties.directoriesToInclude.joinToString(", ")
        aikarsSetting.text = apiProperties.aikarsFlags
        fallbackURLSetting.text = apiProperties.updateUrl.toString()
        exclusionSetting.selectedItem = apiProperties.exclusionFilter
        languageSetting.selectedItem = apiProperties.i18n4kConfig.locale
        logLevelSetting.selectedItem = apiProperties.logLevel
        overwriteSetting.isSelected = apiProperties.isServerPacksOverwriteEnabled
        updateServerPackSetting.isSelected = apiProperties.isUpdatingServerPacksEnabled
        javaVariableSetting.isSelected = apiProperties.isJavaScriptAutoupdateEnabled
        prereleaseSetting.isSelected = apiProperties.isCheckingForPreReleasesEnabled
        zipExclusionsSetting.isSelected = apiProperties.isZipFileExclusionEnabled
        cleanupSetting.isSelected = apiProperties.isServerPackCleanupEnabled
        snapshotsSetting.isSelected = apiProperties.isMinecraftPreReleasesAvailabilityEnabled
        autodetectionSetting.isSelected = apiProperties.isAutoExcludingModsEnabled
        preInstallFilesSetting.text = apiProperties.preInstallCleanupFiles.joinToString(", ")
        postInstallFilesSetting.text = apiProperties.postInstallCleanupFiles.joinToString(", ")
        scriptSetting.loadData(apiProperties.startScriptTemplates)
        javaScriptSetting.loadData(apiProperties.javaScriptTemplates)
        javaPathsSetting.loadData(apiProperties.javaPaths)

        changeUpdateSettingState()
    }

    private fun checkRestartNoticeRequired(): Boolean {
        var showRestartNotice = false
        if (homeSetting.file != apiProperties.homeDirectory.absoluteFile) {
            showRestartNotice = true
        }
        if (snapshotsSetting.isSelected != apiProperties.isMinecraftPreReleasesAvailabilityEnabled) {
            showRestartNotice = true
        }
        if (languageSetting.selectedItem != apiProperties.i18n4kConfig.locale) {
            showRestartNotice = true
        }
        return showRestartNotice
    }

    override fun saveSettings() {
        val showRestartNotice = checkRestartNoticeRequired()

        apiProperties.homeDirectory = homeSetting.file.absoluteFile
        //apiProperties.javaPath = javaSetting.file.absolutePath
        apiProperties.serverPacksDirectory = serverPacksSetting.file.absoluteFile
        apiProperties.zipArchiveExclusions = TreeSet(zipSetting.text.replace(", ",",").split(","))
        apiProperties.directoriesToInclude = TreeSet(inclusionsSetting.text.replace(", ",",").split(","))
        apiProperties.aikarsFlags = aikarsSetting.text
        apiProperties.updateUrl = URI(fallbackURLSetting.text).toURL()
        apiProperties.exclusionFilter = exclusionSetting.selectedItem as ExclusionFilter
        apiProperties.language = languageSetting.selectedItem as Locale
        apiProperties.logLevel = logLevelSetting.selectedItem.toString()
        apiProperties.isServerPacksOverwriteEnabled = overwriteSetting.isSelected
        apiProperties.isUpdatingServerPacksEnabled = updateServerPackSetting.isSelected
        apiProperties.isJavaScriptAutoupdateEnabled = javaVariableSetting.isSelected
        apiProperties.isCheckingForPreReleasesEnabled = prereleaseSetting.isSelected
        apiProperties.isZipFileExclusionEnabled = zipExclusionsSetting.isSelected
        apiProperties.isServerPackCleanupEnabled = cleanupSetting.isSelected
        apiProperties.isMinecraftPreReleasesAvailabilityEnabled = snapshotsSetting.isSelected
        apiProperties.isAutoExcludingModsEnabled = autodetectionSetting.isSelected
        apiProperties.preInstallCleanupFiles = TreeSet(preInstallFilesSetting.text.replace(", ",",").split(","))
        apiProperties.postInstallCleanupFiles = TreeSet(postInstallFilesSetting.text.replace(", ",",").split(","))

        val scriptTemplates = scriptSetting.getData()
        scriptTemplates.remove("placeholder")
        apiProperties.startScriptTemplates = scriptTemplates

        val javaScriptTemplates = javaScriptSetting.getData()
        javaScriptTemplates.remove("placeholder")
        apiProperties.javaScriptTemplates = javaScriptTemplates

        val javaPaths = javaPathsSetting.getData()
        javaPaths.remove("placeholder")
        apiProperties.javaPaths = javaPaths

        changeUpdateSettingState()

        if (showRestartNotice) {
            mainFrame.showRestartNotice()
        }
    }

    override fun validateSettings(): List<String> {
        val errors = mutableListOf<String>()

        if (!homeSetting.file.absoluteFile.isDirectory || !homeSetting.file.absoluteFile.canWrite()) {
            homeIcon.error(Translations.settings_check_home.toString())
            errors.add(Translations.settings_check_home.toString())
        } else {
            homeIcon.info()
        }

        /*
        if (!javaSetting.file.absoluteFile.isFile || !javaSetting.file.absoluteFile.canRead() || !javaSetting.file.absoluteFile.canExecute()) {
          javaIcon.error(Translations.settings_check_java.toString())
          errors.add(Translations.settings_check_java.toString())
        } else {
          javaIcon.info()
        }
        */

        if (!serverPacksSetting.file.absoluteFile.isDirectory || !serverPacksSetting.file.absoluteFile.canWrite()) {
            serverPacksIcon.error(Translations.settings_check_serverpacks.toString())
            errors.add(Translations.settings_check_serverpacks.toString())
        } else {
            serverPacksIcon.info()
        }

        if (zipSetting.text.matches(guiProps.whitespace)) {
            zipIcon.error(Translations.settings_check_whitespace.toString())
            errors.add(Translations.settings_check_whitespace.toString())
        } else {
            zipIcon.info()
        }

        if (inclusionsSetting.text.matches(guiProps.whitespace)) {
            inclusionsIcon.error(Translations.settings_check_whitespace.toString())
            errors.add(Translations.settings_check_whitespace.toString())
        } else {
            inclusionsIcon.info()
        }

        if (scriptSetting.getData().entries.any { (_, value) -> !File(value).isFile }) {
            scriptIcon.error(Translations.settings_global_scripts_missing.toString())
            errors.add(Translations.settings_global_scripts_missing.toString())
        } else {
            scriptIcon.info()
        }

        if (javaScriptSetting.getData().entries.any { (_, value) -> !File(value).isFile }) {
            javaScriptIcon.error(Translations.settings_global_javascripts_missing.toString())
            errors.add(Translations.settings_global_javascripts_missing.toString())
        } else {
            scriptIcon.info()
        }

        if (preInstallFilesSetting.text.matches(guiProps.whitespace)) {
            preInstallFilesIcon.error(Translations.settings_check_whitespace.toString())
            errors.add(Translations.settings_check_whitespace.toString())
        } else {
            preInstallFilesIcon.info()
        }

        if (postInstallFilesSetting.text.matches(guiProps.whitespace)) {
            postInstallFilesIcon.error(Translations.settings_check_whitespace.toString())
            errors.add(Translations.settings_check_whitespace.toString())
        } else {
            postInstallFilesIcon.info()
        }

        try {
            URI(fallbackURLSetting.text).toURL()
            fallbackURLIcon.info()
        } catch (ex: MalformedURLException) {
            fallbackURLIcon.error(Translations.settings_check_fallbackurl.toString())
            errors.add(Translations.settings_check_fallbackurl.toString())
        }
        return errors.toList()
    }

    override fun hasUnsavedChanges(): Boolean {
        val javaPaths = javaPathsSetting.getData()
        javaPaths.remove("placeholder")
        val scriptTemplates = scriptSetting.getData()
        scriptTemplates.remove("placeholder")
        val javaScriptTemplates = javaScriptSetting.getData()
        javaScriptTemplates.remove("placeholder")
        val changes = homeSetting.file.absolutePath != apiProperties.homeDirectory.absolutePath ||
        /*javaSetting.file.absolutePath != File(apiProperties.javaPath).absolutePath ||*/
        serverPacksSetting.file.absolutePath != apiProperties.serverPacksDirectory.absolutePath ||
        zipSetting.text != apiProperties.zipArchiveExclusions.joinToString(", ") ||
        inclusionsSetting.text != apiProperties.directoriesToInclude.joinToString(", ") ||
        aikarsSetting.text != apiProperties.aikarsFlags ||
        fallbackURLSetting.text != apiProperties.updateUrl.toString() ||
        exclusionSetting.selectedItem.toString() != apiProperties.exclusionFilter.toString() ||
        languageSetting.selectedItem.toString().lowercase() != apiProperties.i18n4kConfig.locale.toString().lowercase() ||
        logLevelSetting.selectedItem.toString().uppercase() != apiProperties.logLevel.uppercase() ||
        overwriteSetting.isSelected != apiProperties.isServerPacksOverwriteEnabled ||
        updateServerPackSetting.isSelected != apiProperties.isUpdatingServerPacksEnabled ||
        javaVariableSetting.isSelected != apiProperties.isJavaScriptAutoupdateEnabled ||
        prereleaseSetting.isSelected != apiProperties.isCheckingForPreReleasesEnabled ||
        zipExclusionsSetting.isSelected != apiProperties.isZipFileExclusionEnabled ||
        cleanupSetting.isSelected != apiProperties.isServerPackCleanupEnabled ||
        snapshotsSetting.isSelected != apiProperties.isMinecraftPreReleasesAvailabilityEnabled ||
        autodetectionSetting.isSelected != apiProperties.isAutoExcludingModsEnabled ||
        preInstallFilesSetting.text != apiProperties.preInstallCleanupFiles.joinToString(", ") ||
        postInstallFilesSetting.text != apiProperties.postInstallCleanupFiles.joinToString(", ") ||
        scriptTemplates != apiProperties.startScriptTemplates ||
        javaScriptTemplates != apiProperties.javaScriptTemplates ||
        javaPaths != apiProperties.javaPaths
        if (changes) {
            title.showWarningIcon()
        } else {
            title.hideWarningIcon()
        }
        return changes
    }

    private fun changeUpdateSettingState() {
        if (overwriteSetting.isSelected) {
            updateServerPackSetting.isEnabled = false
            updateServerPackSetting.isSelected = false
        } else {
            updateServerPackSetting.isEnabled = true
        }
    }
}