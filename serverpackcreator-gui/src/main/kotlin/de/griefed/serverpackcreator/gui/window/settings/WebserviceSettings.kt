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
package de.griefed.serverpackcreator.gui.window.settings

import Gui
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.testFileWrite
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.gui.window.settings.components.Editor
import de.griefed.serverpackcreator.gui.window.settings.components.TomcatBaseDirChooser
import de.griefed.serverpackcreator.gui.window.settings.components.TomcatLogDirChooser
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
    changeListener: ChangeListener,
    controlPanel: ControlPanel
) : Editor(Gui.settings_webservice.toString(), guiProps) {

    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING)
    private val cronParser = CronParser(cronDefinition)

    private val databaseURLIcon = StatusIcon(guiProps,Gui.settings_webservice_database_tooltip.toString())
    private val databaseURLLabel = ElementLabel(Gui.settings_webservice_database_label.toString())
    private val databaseURLSetting = ScrollTextField(guiProps, apiProperties.jdbcDatabaseUrl, Gui.settings_webservice_database_label.toString()) //TODO add editor for password and username
    private val databaseURLRevert = BalloonTipButton(null, guiProps.revertIcon,Gui.settings_revert.toString(), guiProps) { databaseURLSetting.text = apiProperties.jdbcDatabaseUrl }
    private val databaseURLReset = BalloonTipButton(null, guiProps.resetIcon,Gui.settings_reset.toString(), guiProps) { databaseURLSetting.text = apiProperties.defaultWebserviceDatabase() }

    private val cleanupScheduleIcon = StatusIcon(guiProps,Gui.settings_webservice_schedule_cleanup_tooltip.toString())
    private val cleanupScheduleLabel = ElementLabel(Gui.settings_webservice_schedule_cleanup_label.toString())
    private val cleanupScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceCleanupSchedule,Gui.settings_webservice_schedule_cleanup_label.toString(),documentChangeListener)
    private val cleanupRevert = BalloonTipButton(null, guiProps.revertIcon,Gui.settings_revert.toString(), guiProps) { cleanupScheduleSetting.text = apiProperties.webserviceCleanupSchedule }
    private val cleanupReset = BalloonTipButton(null, guiProps.resetIcon,Gui.settings_reset.toString(), guiProps) { cleanupScheduleSetting.text = apiProperties.fallbackCleanupSchedule }

    private val logDirectoryIcon = StatusIcon(guiProps,Gui.settings_webservice_tomcat_logs_tooltip.toString())
    private val logDirectoryLabel = ElementLabel(Gui.settings_webservice_tomcat_logs_label.toString())
    private val logDirectorySetting = ScrollTextFileField(guiProps,apiProperties.tomcatLogsDirectory.absoluteFile, documentChangeListener)
    private val logDirectoryRevert = BalloonTipButton(null, guiProps.revertIcon,Gui.settings_revert.toString(), guiProps) { logDirectorySetting.file = apiProperties.tomcatLogsDirectory.absoluteFile }
    private val logDirectoryReset = BalloonTipButton(null, guiProps.resetIcon,Gui.settings_reset.toString(), guiProps) { logDirectorySetting.file = apiProperties.defaultTomcatLogsDirectory().absoluteFile }
    private val logDirectoryChoose = BalloonTipButton(null, guiProps.folderIcon,Gui.settings_select_directory.toString(), guiProps) {
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

    private val baseDirIcon = StatusIcon(guiProps,Gui.settings_webservice_tomcat_dir_tooltip.toString())
    private val baseDirLabel = ElementLabel(Gui.settings_webservice_tomcat_dir_label.toString())
    private val baseDirSetting = ScrollTextFileField(guiProps,apiProperties.tomcatBaseDirectory.absoluteFile, documentChangeListener)
    private val baseDirRevert = BalloonTipButton(null, guiProps.revertIcon,Gui.settings_revert.toString(), guiProps) { baseDirSetting.file = apiProperties.tomcatBaseDirectory.absoluteFile }
    private val baseDirReset = BalloonTipButton(null, guiProps.resetIcon,Gui.settings_reset.toString(), guiProps) { baseDirSetting.file = apiProperties.defaultTomcatBaseDirectory().absoluteFile }
    private val baseDirChoose = BalloonTipButton(null, guiProps.folderIcon,Gui.settings_select_directory.toString(), guiProps) {
        val baseDirChooser = TomcatBaseDirChooser(apiProperties,Gui.settings_webservice_tomcat_dir_chooser.toString())
        if (baseDirChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (baseDirChooser.selectedFile.absoluteFile.testFileWrite()) {
                baseDirSetting.file = baseDirChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Gui.settings_directory_error(baseDirChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    private val versionScheduleIcon = StatusIcon(guiProps,Gui.settings_webservice_schedule_versions_tooltip.toString())
    private val versionScheduleLabel = ElementLabel(Gui.settings_webservice_schedule_versions_label.toString())
    private val versionScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceVersionSchedule,Gui.settings_webservice_schedule_versions_label.toString(),documentChangeListener)
    private val versionRevert = BalloonTipButton(null, guiProps.revertIcon,Gui.settings_revert.toString(), guiProps) { versionScheduleSetting.text = apiProperties.webserviceVersionSchedule }
    private val versionReset = BalloonTipButton(null, guiProps.resetIcon,Gui.settings_reset.toString(), guiProps) { versionScheduleSetting.text = apiProperties.fallbackVersionSchedule }

    private val databaseCleanupScheduleIcon = StatusIcon(guiProps,Gui.settings_webservice_schedule_database_tooltip.toString())
    private val databaseCleanupScheduleLabel = ElementLabel(Gui.settings_webservice_schedule_database_label.toString())
    private val databaseCleanupScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceDatabaseCleanupSchedule,Gui.settings_webservice_schedule_database_label.toString(),documentChangeListener)
    private val databaseCleanupRevert = BalloonTipButton(null, guiProps.revertIcon,Gui.settings_revert.toString(), guiProps) { databaseCleanupScheduleSetting.text = apiProperties.webserviceDatabaseCleanupSchedule }
    private val databaseCleanupReset = BalloonTipButton(null, guiProps.resetIcon,Gui.settings_reset.toString(), guiProps) { databaseCleanupScheduleSetting.text = apiProperties.fallbackDatabaseCleanupSchedule }

    init {
        loadSettings()
        var y = 0

        y++
        panel.add(databaseURLIcon, "cell 0 $y")
        panel.add(databaseURLLabel, "cell 1 $y")
        panel.add(databaseURLSetting, "cell 2 $y, grow")
        panel.add(databaseURLRevert, "cell 3 $y")
        panel.add(databaseURLReset, "cell 4 $y")

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
        databaseURLSetting.text = apiProperties.jdbcDatabaseUrl
        cleanupScheduleSetting.text = apiProperties.webserviceCleanupSchedule
        logDirectorySetting.file = apiProperties.tomcatLogsDirectory.absoluteFile
        baseDirSetting.file = apiProperties.tomcatBaseDirectory.absoluteFile
        versionScheduleSetting.text = apiProperties.webserviceVersionSchedule
        databaseCleanupScheduleSetting.text = apiProperties.webserviceDatabaseCleanupSchedule
    }

    override fun saveSettings() {
        apiProperties.jdbcDatabaseUrl = databaseURLSetting.text
        apiProperties.webserviceCleanupSchedule = cleanupScheduleSetting.text
        apiProperties.tomcatLogsDirectory = logDirectorySetting.file.absoluteFile
        apiProperties.tomcatBaseDirectory = baseDirSetting.file.absoluteFile
        apiProperties.webserviceVersionSchedule = versionScheduleSetting.text
        apiProperties.webserviceDatabaseCleanupSchedule = databaseCleanupScheduleSetting.text
    }

    override fun validateSettings(): List<String> {
        val errors = mutableListOf<String>()

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

    override fun hasUnsavedChanges(): Boolean {
        val changes =
                databaseURLSetting.text != apiProperties.jdbcDatabaseUrl ||
                cleanupScheduleSetting.text != apiProperties.webserviceCleanupSchedule ||
                logDirectorySetting.file.absolutePath != apiProperties.tomcatLogsDirectory.absolutePath ||
                baseDirSetting.file.absolutePath != apiProperties.tomcatBaseDirectory.absolutePath ||
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