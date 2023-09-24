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

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.testFileWrite
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.settings.components.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.event.ChangeListener

/**
 * @author Griefed
 */
class WebserviceSettings(
    guiProps: GuiProps,
    private val apiProperties: ApiProperties,
    mainFrame: MainFrame,
    documentChangeListener: DocumentChangeListener,
    changeListener: ChangeListener
) : Editor(Gui.settings_webservice.toString(), guiProps) {

    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING)
    private val cronParser = CronParser(cronDefinition)

    val artemisDataDirectoryIcon = StatusIcon(guiProps, Gui.settings_webservice_artemisdata_tooltip.toString())
    val artemisDataDirectoryLabel = ElementLabel(Gui.settings_webservice_artemisdata_label.toString())
    val artemisDataDirectorySetting = ScrollTextFileField(guiProps,apiProperties.artemisDataDirectory.absoluteFile, documentChangeListener)
    val artemisDataDirectoryRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { artemisDataDirectorySetting.file = apiProperties.artemisDataDirectory.absoluteFile }
    val artemisDataDirectoryReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { artemisDataDirectorySetting.file = apiProperties.defaultArtemisDataDirectory().absoluteFile }
    val artemisDataDirectoryChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_select_directory.toString(),guiProps) {
        val artemisChooser = ArtemisDataDirChooser(apiProperties,Gui.settings_webservice_artemisdata_chooser.toString())
        if (artemisChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (artemisChooser.selectedFile.absoluteFile.testFileWrite()) {
                artemisDataDirectorySetting.file = artemisChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Gui.settings_directory_error(artemisChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    val artemisQueueMaxDiskUsageIcon = StatusIcon(guiProps,Gui.settings_webservice_artemisusage_tooltip.toString())
    val artemisQueueMaxDiskUsageLabel = ElementLabel(Gui.settings_webservice_artemisusage_label.toString())
    val artemisQueueMaxDiskUsageSetting = ActionSlider(10,90,apiProperties.artemisQueueMaxDiskUsage, changeListener)
    val artemisQueueMaxDiskUsageRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { artemisQueueMaxDiskUsageSetting.value = apiProperties.artemisQueueMaxDiskUsage }
    val artemisQueueMaxDiskUsageReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(), guiProps) { artemisQueueMaxDiskUsageSetting.value = apiProperties.fallbackArtemisQueueMaxDiskUsage }

    val databaseFileIcon = StatusIcon(guiProps,Gui.settings_webservice_database_tooltip.toString())
    val databaseFileLabel = ElementLabel(Gui.settings_webservice_database_label.toString())
    val databaseFileSetting = ScrollTextFileField(guiProps,apiProperties.serverPackCreatorDatabase.absoluteFile, documentChangeListener)
    val databaseFileRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { databaseFileSetting.file = apiProperties.serverPackCreatorDatabase.absoluteFile }
    val databaseFileReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { databaseFileSetting.file = apiProperties.defaultWebserviceDatabase().absoluteFile }
    val databaseFileChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_select_directory.toString(),guiProps) {
        val webserviceChooser = WebserviceDBDirChooser(apiProperties,Gui.settings_webservice_database_chooser.toString())
        if (webserviceChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (webserviceChooser.selectedFile.absoluteFile.testFileWrite()) {
                databaseFileSetting.file = File(webserviceChooser.selectedFile.absoluteFile,"serverpackcreator.db").absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Gui.settings_directory_error(webserviceChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    val cleanupScheduleIcon = StatusIcon(guiProps,Gui.settings_webservice_schedule_cleanup_tooltip.toString())
    val cleanupScheduleLabel = ElementLabel(Gui.settings_webservice_schedule_cleanup_label.toString())
    val cleanupScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceCleanupSchedule,Gui.settings_webservice_schedule_cleanup_label.toString(),documentChangeListener)
    val cleanupRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { cleanupScheduleSetting.text = apiProperties.webserviceCleanupSchedule }
    val cleanupReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { cleanupScheduleSetting.text = apiProperties.fallbackCleanupSchedule }

    val logDirectoryIcon = StatusIcon(guiProps,Gui.settings_webservice_tomcat_logs_tooltip.toString())
    val logDirectoryLabel = ElementLabel(Gui.settings_webservice_tomcat_logs_label.toString())
    val logDirectorySetting = ScrollTextFileField(guiProps,apiProperties.tomcatLogsDirectory.absoluteFile, documentChangeListener)
    val logDirectoryRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { logDirectorySetting.file = apiProperties.tomcatLogsDirectory.absoluteFile }
    val logDirectoryReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { logDirectorySetting.file = apiProperties.defaultTomcatLogsDirectory().absoluteFile }
    val logDirectoryChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_select_directory.toString(),guiProps) {
        val logDirectoryChooser = TomcatLogDirChooser(apiProperties,Gui.settings_webservice_tomcat_logs_chooser.toString())
        if (logDirectoryChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (logDirectoryChooser.selectedFile.absoluteFile.testFileWrite()) {
                logDirectorySetting.file = logDirectoryChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Gui.settings_directory_error(logDirectoryChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    val baseDirIcon = StatusIcon(guiProps,Gui.settings_webservice_tomcat_dir_tooltip.toString())
    val baseDirLabel = ElementLabel(Gui.settings_webservice_tomcat_dir_label.toString())
    val baseDirSetting = ScrollTextFileField(guiProps,apiProperties.tomcatBaseDirectory.absoluteFile, documentChangeListener)
    val baseDirRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { baseDirSetting.file = apiProperties.tomcatBaseDirectory.absoluteFile }
    val baseDirReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { baseDirSetting.file = apiProperties.defaultTomcatBaseDirectory().absoluteFile }
    val baseDirChoose = BalloonTipButton(null,guiProps.folderIcon,Gui.settings_select_directory.toString(),guiProps) {
        val baseDirChooser = TomcatBaseDirChooser(apiProperties,Gui.settings_webservice_tomcat_dir_chooser.toString())
        if (baseDirChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            baseDirSetting.file = baseDirChooser.selectedFile.absoluteFile
        }
    }

    val versionScheduleIcon = StatusIcon(guiProps,Gui.settings_webservice_schedule_versions_tooltip.toString())
    val versionScheduleLabel = ElementLabel(Gui.settings_webservice_schedule_versions_label.toString())
    val versionScheduleSetting = ScrollTextField(
        guiProps,
        apiProperties.webserviceVersionSchedule,
        Gui.settings_webservice_schedule_versions_label.toString(),
        documentChangeListener
    )
    val versionRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { versionScheduleSetting.text = apiProperties.webserviceVersionSchedule }
    val versionReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { versionScheduleSetting.text = apiProperties.fallbackVersionSchedule }

    val databaseCleanupScheduleIcon = StatusIcon(guiProps,Gui.settings_webservice_schedule_database_tooltip.toString())
    val databaseCleanupScheduleLabel = ElementLabel(Gui.settings_webservice_schedule_database_label.toString())
    val databaseCleanupScheduleSetting = ScrollTextField(
        guiProps,
        apiProperties.webserviceDatabaseCleanupSchedule,
        Gui.settings_webservice_schedule_database_label.toString(),
        documentChangeListener
    )
    val databaseCleanupRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) { databaseCleanupScheduleSetting.text = apiProperties.webserviceDatabaseCleanupSchedule }
    val databaseCleanupReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) { databaseCleanupScheduleSetting.text = apiProperties.fallbackDatabaseCleanupSchedule }

    init {
        loadSettings()
        artemisQueueMaxDiskUsageSetting.paintTicks = true
        artemisQueueMaxDiskUsageSetting.paintLabels = true
        artemisQueueMaxDiskUsageSetting.majorTickSpacing = 10
        artemisQueueMaxDiskUsageSetting.minorTickSpacing = 5

        var y = 0
        panel.add(artemisQueueMaxDiskUsageIcon, "cell 0 $y")
        panel.add(artemisQueueMaxDiskUsageLabel, "cell 1 $y")
        panel.add(artemisQueueMaxDiskUsageSetting, "cell 2 $y, grow")
        panel.add(artemisQueueMaxDiskUsageRevert, "cell 3 $y")
        panel.add(artemisQueueMaxDiskUsageReset, "cell 4 $y")

        y++
        panel.add(artemisDataDirectoryIcon, "cell 0 $y")
        panel.add(artemisDataDirectoryLabel, "cell 1 $y")
        panel.add(artemisDataDirectorySetting, "cell 2 $y, grow")
        panel.add(artemisDataDirectoryRevert, "cell 3 $y")
        panel.add(artemisDataDirectoryReset, "cell 4 $y")
        panel.add(artemisDataDirectoryChoose, "cell 5 $y")

        y++
        panel.add(databaseFileIcon, "cell 0 $y")
        panel.add(databaseFileLabel, "cell 1 $y")
        panel.add(databaseFileSetting, "cell 2 $y, grow")
        panel.add(databaseFileRevert, "cell 3 $y")
        panel.add(databaseFileReset, "cell 4 $y")
        panel.add(databaseFileChoose, "cell 5 $y")

        y++
        panel.add(logDirectoryIcon, "cell 0 $y")
        panel.add(logDirectoryLabel, "cell 1 $y")
        panel.add(logDirectorySetting, "cell 2 $y, grow")
        panel.add(logDirectoryRevert, "cell 3 $y")
        panel.add(logDirectoryReset, "cell 4 $y")
        panel.add(logDirectoryChoose, "cell 5 $y")

        y++
        panel.add(baseDirIcon, "cell 0 $y")
        panel.add(baseDirLabel, "cell 1 $y")
        panel.add(baseDirSetting, "cell 2 $y, grow")
        panel.add(baseDirRevert, "cell 3 $y")
        panel.add(baseDirReset, "cell 4 $y")
        panel.add(baseDirChoose, "cell 5 $y")

        y++
        panel.add(cleanupScheduleIcon, "cell 0 $y")
        panel.add(cleanupScheduleLabel, "cell 1 $y")
        panel.add(cleanupScheduleSetting, "cell 2 $y, grow")
        panel.add(cleanupRevert, "cell 3 $y")
        panel.add(cleanupReset, "cell 4 $y")

        y++
        panel.add(versionScheduleIcon, "cell 0 $y")
        panel.add(versionScheduleLabel, "cell 1 $y")
        panel.add(versionScheduleSetting, "cell 2 $y, grow")
        panel.add(versionRevert, "cell 3 $y")
        panel.add(versionReset, "cell 4 $y")

        y++
        panel.add(databaseCleanupScheduleIcon, "cell 0 $y")
        panel.add(databaseCleanupScheduleLabel, "cell 1 $y")
        panel.add(databaseCleanupScheduleSetting, "cell 2 $y, grow")
        panel.add(databaseCleanupRevert, "cell 3 $y")
        panel.add(databaseCleanupReset, "cell 4 $y")
    }

    /**
     * @author Griefed
     */
    override fun loadSettings() {
        artemisDataDirectorySetting.file = apiProperties.artemisDataDirectory.absoluteFile
        artemisQueueMaxDiskUsageSetting.value = apiProperties.artemisQueueMaxDiskUsage
        databaseFileSetting.file = apiProperties.serverPackCreatorDatabase.absoluteFile
        cleanupScheduleSetting.text = apiProperties.webserviceCleanupSchedule
        logDirectorySetting.file = apiProperties.tomcatLogsDirectory.absoluteFile
        baseDirSetting.file = apiProperties.tomcatBaseDirectory.absoluteFile
        versionScheduleSetting.text = apiProperties.webserviceVersionSchedule
        databaseCleanupScheduleSetting.text = apiProperties.webserviceDatabaseCleanupSchedule
    }

    /**
     * @author Griefed
     */
    override fun saveSettings() {
        apiProperties.artemisDataDirectory = artemisDataDirectorySetting.file.absoluteFile
        apiProperties.artemisQueueMaxDiskUsage = artemisQueueMaxDiskUsageSetting.value
        apiProperties.serverPackCreatorDatabase = databaseFileSetting.file.absoluteFile
        apiProperties.webserviceCleanupSchedule = cleanupScheduleSetting.text
        apiProperties.tomcatLogsDirectory = logDirectorySetting.file.absoluteFile
        apiProperties.tomcatBaseDirectory = baseDirSetting.file.absoluteFile
        apiProperties.webserviceVersionSchedule = versionScheduleSetting.text
        apiProperties.webserviceDatabaseCleanupSchedule = databaseCleanupScheduleSetting.text
    }

    /**
     * @author Griefed
     */
    override fun validateSettings(): List<String> {
        val errors = mutableListOf<String>()
        if (!artemisDataDirectorySetting.file.absoluteFile.isDirectory || !artemisDataDirectorySetting.file.absoluteFile.canWrite()) {
            artemisDataDirectoryIcon.error(Gui.settings_webservice_artemisdata_error.toString())
            errors.add(Gui.settings_webservice_artemisdata_error.toString())
        } else {
            artemisDataDirectoryIcon.info()
        }

        if (artemisQueueMaxDiskUsageSetting.value < 10 || artemisQueueMaxDiskUsageSetting.value > 90 ) {
            artemisQueueMaxDiskUsageIcon.error(Gui.settings_webservice_artemisusage_error.toString())
            errors.add(Gui.settings_webservice_artemisusage_error.toString())
        } else {
            artemisQueueMaxDiskUsageIcon.info()
        }

        if (!databaseFileSetting.file.absoluteFile.parentFile.isDirectory || !databaseFileSetting.file.absoluteFile.parentFile.canWrite()) {
            databaseFileIcon.error(Gui.settings_webservice_database_error.toString())
            errors.add(Gui.settings_webservice_database_error.toString())
        } else {
            databaseFileIcon.info()
        }

        try {
            cronParser.parse(cleanupScheduleSetting.text).validate()
            cleanupScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            cleanupScheduleIcon.error(Gui.settings_webservice_schedule_cleanup_error.toString())
            errors.add(Gui.settings_webservice_schedule_cleanup_error.toString())
        }

        if (!logDirectorySetting.file.absoluteFile.isDirectory || !logDirectorySetting.file.absoluteFile.canWrite()) {
            logDirectoryIcon.error(Gui.settings_webservice_tomcat_logs_error.toString())
            errors.add(Gui.settings_webservice_tomcat_logs_error.toString())
        } else {
            logDirectoryIcon.info()
        }

        if (!baseDirSetting.file.absoluteFile.isDirectory || !baseDirSetting.file.absoluteFile.canWrite()) {
            baseDirIcon.error(Gui.settings_webservice_tomcat_dir_error.toString())
            errors.add(Gui.settings_webservice_tomcat_dir_error.toString())
        } else {
            baseDirIcon.info()
        }

        try {
            cronParser.parse(versionScheduleSetting.text).validate()
            versionScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            versionScheduleIcon.error(Gui.settings_webservice_schedule_versions_error.toString())
            errors.add(Gui.settings_webservice_schedule_versions_error.toString())
        }

        try {
            cronParser.parse(databaseCleanupScheduleSetting.text).validate()
            databaseCleanupScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            databaseCleanupScheduleIcon.error(Gui.settings_webservice_schedule_database_error.toString())
            errors.add(Gui.settings_webservice_schedule_database_error.toString())
        }

        if (errors.isNotEmpty()) {
            title.setAndShowErrorIcon(Gui.settings_webservice_errors.toString())
        } else {
            title.hideErrorIcon()
        }

        return errors.toList()
    }

    /**
     * @author Griefed
     */
    override fun hasUnsavedChanges(): Boolean {
        val changes = artemisDataDirectorySetting.file != apiProperties.artemisDataDirectory.absoluteFile ||
                artemisQueueMaxDiskUsageSetting.value != apiProperties.artemisQueueMaxDiskUsage ||
                databaseFileSetting.file != apiProperties.serverPackCreatorDatabase.absoluteFile ||
                cleanupScheduleSetting.text != apiProperties.webserviceCleanupSchedule ||
                logDirectorySetting.file != apiProperties.tomcatLogsDirectory.absoluteFile ||
                baseDirSetting.file != apiProperties.tomcatBaseDirectory.absoluteFile ||
                versionScheduleSetting.text != apiProperties.webserviceVersionSchedule ||
                databaseCleanupScheduleSetting.text != apiProperties.webserviceDatabaseCleanupSchedule
        if (changes) {
            title.showWarningIcon()
        } else {
            title.hideWarningIcon()
        }
        return changes
    }
}