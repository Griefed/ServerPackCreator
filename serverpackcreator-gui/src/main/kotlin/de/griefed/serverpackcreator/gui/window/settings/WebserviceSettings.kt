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
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.settings.components.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.event.ChangeListener

class WebserviceSettings(
    guiProps: GuiProps,
    private val apiProperties: ApiProperties,
    mainFrame: MainFrame,
    documentChangeListener: DocumentChangeListener,
    changeListener: ChangeListener
) : Editor("Webservice", guiProps) {

    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING)
    private val cronParser = CronParser(cronDefinition)

    val artemisDataDirectoryIcon = StatusIcon(guiProps, "Data directory for Artemis Queue System")
    val artemisDataDirectoryLabel = ElementLabel("Artemis Data Directory")
    val artemisDataDirectorySetting = ScrollTextFileField(guiProps,apiProperties.artemisDataDirectory.absoluteFile, documentChangeListener)
    val artemisDataDirectoryRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { artemisDataDirectorySetting.file = apiProperties.artemisDataDirectory.absoluteFile }
    val artemisDataDirectoryReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { artemisDataDirectorySetting.file = apiProperties.defaultArtemisDataDirectory().absoluteFile }
    val artemisDataDirectoryChoose = BalloonTipButton(null,guiProps.folderIcon,"Select directory",guiProps) {
        val artemisChooser = ArtemisDataDirChooser(apiProperties,"Artemis Data Directory Chooser")
        if (artemisChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            artemisDataDirectorySetting.file = artemisChooser.selectedFile.absoluteFile
        }
    }

    val artemisQueueMaxDiskUsageIcon = StatusIcon(guiProps,"Max disk usage by Artemis, in absolute percentages.")
    val artemisQueueMaxDiskUsageLabel = ElementLabel("Artemis Max Disk Usage")
    val artemisQueueMaxDiskUsageSetting = ActionSlider(10,90,apiProperties.artemisQueueMaxDiskUsage, changeListener)
    val artemisQueueMaxDiskUsageRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { artemisQueueMaxDiskUsageSetting.value = apiProperties.artemisQueueMaxDiskUsage }
    val artemisQueueMaxDiskUsageReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value", guiProps) { artemisQueueMaxDiskUsageSetting.value = apiProperties.fallbackArtemisQueueMaxDiskUsage }

    val databaseFileIcon = StatusIcon(guiProps,"Webservice database file")
    val databaseFileLabel = ElementLabel("Database File")
    val databaseFileSetting = ScrollTextFileField(guiProps,apiProperties.serverPackCreatorDatabase.absoluteFile, documentChangeListener)
    val databaseFileRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { databaseFileSetting.file = apiProperties.serverPackCreatorDatabase.absoluteFile }
    val databaseFileReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { databaseFileSetting.file = apiProperties.defaultWebserviceDatabase().absoluteFile }
    val databaseFileChoose = BalloonTipButton(null,guiProps.folderIcon,"Select directory",guiProps) {
        val webserviceChooser = WebserviceDBDirChooser(apiProperties,"Webservice Database Directory Chooser")
        if (webserviceChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            databaseFileSetting.file = File(webserviceChooser.selectedFile.absoluteFile,"serverpackcreator.db").absoluteFile
        }
    }

    val cleanupScheduleIcon = StatusIcon(guiProps,"Schedule by which the webservice will perform cleanup operations")
    val cleanupScheduleLabel = ElementLabel("Cleanup Schedule")
    val cleanupScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceCleanupSchedule, "Cleanup Schedule", apiProperties, documentChangeListener)
    val cleanupRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { cleanupScheduleSetting.text = apiProperties.webserviceCleanupSchedule }
    val cleanupReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { cleanupScheduleSetting.text = apiProperties.fallbackCleanupSchedule }

    val logDirectoryIcon = StatusIcon(guiProps,"Directory which contains the Tomcat Access logs")
    val logDirectoryLabel = ElementLabel("Tomcat Access Log Directory")
    val logDirectorySetting = ScrollTextFileField(guiProps,apiProperties.tomcatLogsDirectory.absoluteFile, documentChangeListener)
    val logDirectoryRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { logDirectorySetting.file = apiProperties.tomcatLogsDirectory.absoluteFile }
    val logDirectoryReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { logDirectorySetting.file = apiProperties.defaultTomcatLogsDirectory().absoluteFile }
    val logDirectoryChoose = BalloonTipButton(null,guiProps.folderIcon,"Select directory",guiProps) {
        val logDirectoryChooser = TomcatLogDirChooser(apiProperties,"Tomcat Log Directory Chooser")
        if (logDirectoryChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            logDirectorySetting.file = logDirectoryChooser.selectedFile.absoluteFile
        }
    }

    val baseDirIcon = StatusIcon(guiProps,"Base directory for Tomcat")
    val baseDirLabel = ElementLabel("Tomcat Base Directory")
    val baseDirSetting = ScrollTextFileField(guiProps,apiProperties.tomcatBaseDirectory.absoluteFile, documentChangeListener)
    val baseDirRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { baseDirSetting.file = apiProperties.tomcatBaseDirectory.absoluteFile }
    val baseDirReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { baseDirSetting.file = apiProperties.defaultTomcatBaseDirectory().absoluteFile }
    val baseDirChoose = BalloonTipButton(null,guiProps.folderIcon,"Select directory",guiProps) {
        val baseDirChooser = TomcatBaseDirChooser(apiProperties,"Tomcat Base Directory Chooser")
        if (baseDirChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            baseDirSetting.file = baseDirChooser.selectedFile.absoluteFile
        }
    }

    val versionScheduleIcon = StatusIcon(guiProps,"Schedule by which the webservice will perform version meta updates")
    val versionScheduleLabel = ElementLabel("Version Schedule")
    val versionScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceVersionSchedule, "Version Schedule", apiProperties, documentChangeListener)
    val versionRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { versionScheduleSetting.text = apiProperties.webserviceVersionSchedule }
    val versionReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { versionScheduleSetting.text = apiProperties.fallbackVersionSchedule }

    val databaseCleanupScheduleIcon = StatusIcon(guiProps,"Schedule by which the webservice will perform database cleanup operations")
    val databaseCleanupScheduleLabel = ElementLabel("Database Cleanup Schedule")
    val databaseCleanupScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceDatabaseCleanupSchedule, "Database Schedule", apiProperties, documentChangeListener)
    val databaseCleanupRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes.",guiProps) { databaseCleanupScheduleSetting.text = apiProperties.webserviceDatabaseCleanupSchedule }
    val databaseCleanupReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) { databaseCleanupScheduleSetting.text = apiProperties.fallbackDatabaseCleanupSchedule }

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

    override fun validateSettings(): List<String> {
        val errors = mutableListOf<String>()
        if (!artemisDataDirectorySetting.file.absoluteFile.isDirectory || !artemisDataDirectorySetting.file.absoluteFile.canWrite()) {
            artemisDataDirectoryIcon.error("Artemis Data directory either does not exist or is not writable.")
            errors.add("Artemis Data directory either does not exist or is not writable.")
        } else {
            artemisDataDirectoryIcon.info()
        }

        if (artemisQueueMaxDiskUsageSetting.value < 10 || artemisQueueMaxDiskUsageSetting.value > 90 ) {
            artemisQueueMaxDiskUsageIcon.error("Artemis max disk usage must be a value from 10 to 90.")
            errors.add("Artemis max disk usage must be a value from 10 to 90.")
        } else {
            artemisQueueMaxDiskUsageIcon.info()
        }

        if (!databaseFileSetting.file.absoluteFile.parentFile.isDirectory || !databaseFileSetting.file.absoluteFile.parentFile.canWrite()) {
            databaseFileIcon.error("Database directory does not exist or is not writable.")
            errors.add("Database directory does not exist or is not writable.")
        } else {
            databaseFileIcon.info()
        }

        try {
            cronParser.parse(cleanupScheduleSetting.text).validate()
            cleanupScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            cleanupScheduleIcon.error("Invalid Cleanup Schedule QUARTZ CRON expression.")
            errors.add("Invalid Cleanup Schedule QUARTZ CRON expression.")
        }

        if (!logDirectorySetting.file.absoluteFile.isDirectory || !logDirectorySetting.file.absoluteFile.canWrite()) {
            logDirectoryIcon.error("Tomcat Log directory does not exist or is not writable.")
            errors.add("Tomcat Log directory does not exist or is not writable.")
        } else {
            logDirectoryIcon.info()
        }

        if (!baseDirSetting.file.absoluteFile.isDirectory || !baseDirSetting.file.absoluteFile.canWrite()) {
            baseDirIcon.error("Tomcat base-directory does not exist or is not writable.")
            errors.add("Tomcat base-directory does not exist or is not writable.")
        } else {
            baseDirIcon.info()
        }

        try {
            cronParser.parse(versionScheduleSetting.text).validate()
            versionScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            versionScheduleIcon.error("Invalid Version Update Schedule QUARTZ CRON expression.")
            errors.add("Invalid Version Update Schedule QUARTZ CRON expression.")
        }

        try {
            cronParser.parse(databaseCleanupScheduleSetting.text).validate()
            databaseCleanupScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            databaseCleanupScheduleIcon.error("Invalid Database Cleanup Schedule QUARTZ CRON expression.")
            errors.add("Invalid Database Cleanup Schedule QUARTZ CRON expression.")
        }

        if (errors.isNotEmpty()) {
            title.setAndShowErrorIcon("Your Webservice settings contain errors!")
        } else {
            title.hideErrorIcon()
        }

        return errors.toList()
    }

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