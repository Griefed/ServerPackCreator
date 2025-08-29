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
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import de.griefed.serverpackcreator.api.utilities.common.testFileWrite
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.*
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.settings.components.Editor
import de.griefed.serverpackcreator.app.gui.window.settings.components.TomcatBaseDirChooser
import de.griefed.serverpackcreator.app.gui.window.settings.components.TomcatLogDirChooser
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.JOptionPane

/**
 * @author Griefed
 */
class WebserviceSettings(
    guiProps: GuiProps,
    private val apiProperties: ApiProperties,
    mainFrame: MainFrame,
    documentChangeListener: DocumentChangeListener
) : Editor(Translations.settings_webservice.toString(), guiProps) {

    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING)
    private val cronParser = CronParser(cronDefinition)

    private val databaseHostIcon = StatusIcon(guiProps,Translations.settings_webservice_database_host_tooltip.toString())
    private val databaseHostLabel = ElementLabel(Translations.settings_webservice_database_host_label.toString())
    private val databaseHostSetting = ScrollTextField(guiProps, getHost(apiProperties.databaseUrl), Translations.settings_webservice_database_host_label.toString(), documentChangeListener)
    private val databaseHostRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { databaseHostSetting.text = getHost(apiProperties.databaseUrl) }
    private val databaseHostReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { databaseHostSetting.text = getHost(apiProperties.defaultWebserviceDatabase()) }

    private val databasePortIcon = StatusIcon(guiProps,Translations.settings_webservice_database_port_tooltip.toString())
    private val databasePortLabel = ElementLabel(Translations.settings_webservice_database_port_label.toString())
    private val databasePortSetting = ScrollTextField(guiProps, getPort(apiProperties.databaseUrl), Translations.settings_webservice_database_port_label.toString(), documentChangeListener)
    private val databasePortRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { databasePortSetting.text = getPort(apiProperties.databaseUrl) }
    private val databasePortReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { databasePortSetting.text = getPort(apiProperties.defaultWebserviceDatabase()) }

    private val databaseDatabaseIcon = StatusIcon(guiProps,Translations.settings_webservice_database_database_tooltip.toString())
    private val databaseDatabaseLabel = ElementLabel(Translations.settings_webservice_database_database_label.toString())
    private val databaseDatabaseSetting = ScrollTextField(guiProps, getDatabase(apiProperties.databaseUrl), Translations.settings_webservice_database_database_label.toString(), documentChangeListener)
    private val databaseDatabaseRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) {
        databaseDatabaseSetting.text = getDatabase(apiProperties.databaseUrl)
    }
    private val databaseDatabaseReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) {
        databaseDatabaseSetting.text = getDatabase(apiProperties.defaultWebserviceDatabase())
    }

    private val databaseUsernameIcon = StatusIcon(guiProps,Translations.settings_webservice_database_username_tooltip.toString())
    private val databaseUsernameLabel = ElementLabel(Translations.settings_webservice_database_username_label.toString())
    private val databaseUsernameSetting = ScrollTextField(guiProps, getUsername(apiProperties.databaseUrl), Translations.settings_webservice_database_username_label.toString(), documentChangeListener)
    private val databaseUsernameRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { databaseUsernameSetting.text = getUsername(apiProperties.databaseUrl) }
    private val databaseUsernameReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { databaseUsernameSetting.text = "" }

    private val databasePasswordIcon = StatusIcon(guiProps,Translations.settings_webservice_database_password_tooltip.toString())
    private val databasePasswordLabel = ElementLabel(Translations.settings_webservice_database_password_label.toString())
    private val databasePasswordSetting = ScrollTextField(guiProps, getPassword(apiProperties.databaseUrl), Translations.settings_webservice_database_password_label.toString(), documentChangeListener)
    private val databasePasswordRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { databasePasswordSetting.text = getPassword(apiProperties.databaseUrl) }
    private val databasePasswordReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { databasePasswordSetting.text = "" }

    private val cleanupScheduleIcon = StatusIcon(guiProps,Translations.settings_webservice_schedule_cleanup_tooltip.toString())
    private val cleanupScheduleLabel = ElementLabel(Translations.settings_webservice_schedule_cleanup_label.toString())
    private val cleanupScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceCleanupSchedule,Translations.settings_webservice_schedule_cleanup_label.toString(),documentChangeListener)
    private val cleanupRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { cleanupScheduleSetting.text = apiProperties.webserviceCleanupSchedule }
    private val cleanupReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { cleanupScheduleSetting.text = apiProperties.fallbackCleanupSchedule }

    private val logDirectoryIcon = StatusIcon(guiProps,Translations.settings_webservice_tomcat_logs_tooltip.toString())
    private val logDirectoryLabel = ElementLabel(Translations.settings_webservice_tomcat_logs_label.toString())
    private val logDirectorySetting = ScrollTextFileField(guiProps,apiProperties.tomcatLogsDirectory.absoluteFile, FileFieldDropType.FOLDER, documentChangeListener)
    private val logDirectoryRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { logDirectorySetting.file = apiProperties.tomcatLogsDirectory.absoluteFile }
    private val logDirectoryReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { logDirectorySetting.file = apiProperties.defaultTomcatLogsDirectory().absoluteFile }
    private val logDirectoryChoose = BalloonTipButton(null, guiProps.folderIcon,Translations.settings_select_directory.toString(), guiProps) {
        val logDirectoryChooser = TomcatLogDirChooser(apiProperties,Translations.settings_webservice_tomcat_logs_chooser.toString())
        if (logDirectoryChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (logDirectoryChooser.selectedFile.absoluteFile.testFileWrite()) {
                logDirectorySetting.file = logDirectoryChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Translations.settings_directory_error(logDirectoryChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    private val baseDirIcon = StatusIcon(guiProps,Translations.settings_webservice_tomcat_dir_tooltip.toString())
    private val baseDirLabel = ElementLabel(Translations.settings_webservice_tomcat_dir_label.toString())
    private val baseDirSetting = ScrollTextFileField(guiProps,apiProperties.tomcatBaseDirectory.absoluteFile, FileFieldDropType.FOLDER, documentChangeListener)
    private val baseDirRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { baseDirSetting.file = apiProperties.tomcatBaseDirectory.absoluteFile }
    private val baseDirReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { baseDirSetting.file = apiProperties.defaultTomcatBaseDirectory().absoluteFile }
    private val baseDirChoose = BalloonTipButton(null, guiProps.folderIcon,Translations.settings_select_directory.toString(), guiProps) {
        val baseDirChooser = TomcatBaseDirChooser(apiProperties,Translations.settings_webservice_tomcat_dir_chooser.toString())
        if (baseDirChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (baseDirChooser.selectedFile.absoluteFile.testFileWrite()) {
                baseDirSetting.file = baseDirChooser.selectedFile.absoluteFile
            } else {
                JOptionPane.showMessageDialog(
                    mainFrame.frame,
                    Translations.settings_directory_error(baseDirChooser.selectedFile.absoluteFile)
                )
            }
        }
    }

    private val versionScheduleIcon = StatusIcon(guiProps,Translations.settings_webservice_schedule_versions_tooltip.toString())
    private val versionScheduleLabel = ElementLabel(Translations.settings_webservice_schedule_versions_label.toString())
    private val versionScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceVersionSchedule,Translations.settings_webservice_schedule_versions_label.toString(),documentChangeListener)
    private val versionRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { versionScheduleSetting.text = apiProperties.webserviceVersionSchedule }
    private val versionReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { versionScheduleSetting.text = apiProperties.fallbackVersionSchedule }

    private val databaseCleanupScheduleIcon = StatusIcon(guiProps,Translations.settings_webservice_schedule_database_tooltip.toString())
    private val databaseCleanupScheduleLabel = ElementLabel(Translations.settings_webservice_schedule_database_label.toString())
    private val databaseCleanupScheduleSetting = ScrollTextField(guiProps,apiProperties.webserviceDatabaseCleanupSchedule,Translations.settings_webservice_schedule_database_label.toString(),documentChangeListener)
    private val databaseCleanupRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) { databaseCleanupScheduleSetting.text = apiProperties.webserviceDatabaseCleanupSchedule }
    private val databaseCleanupReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) { databaseCleanupScheduleSetting.text = apiProperties.fallbackDatabaseCleanupSchedule }

    init {
        loadSettings()
        var y = 0

        y++
        panel.add(databaseHostIcon, "cell 0 $y")
        panel.add(databaseHostLabel, "cell 1 $y")
        panel.add(databaseHostSetting, "cell 2 $y, grow")
        panel.add(databaseHostRevert, "cell 3 $y")
        panel.add(databaseHostReset, "cell 4 $y")

        y++
        panel.add(databasePortIcon, "cell 0 $y")
        panel.add(databasePortLabel, "cell 1 $y")
        panel.add(databasePortSetting, "cell 2 $y, grow")
        panel.add(databasePortRevert, "cell 3 $y")
        panel.add(databasePortReset, "cell 4 $y")

        y++
        panel.add(databaseDatabaseIcon, "cell 0 $y")
        panel.add(databaseDatabaseLabel, "cell 1 $y")
        panel.add(databaseDatabaseSetting, "cell 2 $y, grow")
        panel.add(databaseDatabaseRevert, "cell 3 $y")
        panel.add(databaseDatabaseReset, "cell 4 $y")

        y++
        panel.add(databaseUsernameIcon, "cell 0 $y")
        panel.add(databaseUsernameLabel, "cell 1 $y")
        panel.add(databaseUsernameSetting, "cell 2 $y, grow")
        panel.add(databaseUsernameRevert, "cell 3 $y")
        panel.add(databaseUsernameReset, "cell 4 $y")

        y++
        panel.add(databasePasswordIcon, "cell 0 $y")
        panel.add(databasePasswordLabel, "cell 1 $y")
        panel.add(databasePasswordSetting, "cell 2 $y, grow")
        panel.add(databasePasswordRevert, "cell 3 $y")
        panel.add(databasePasswordReset, "cell 4 $y")

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

    private fun getHost(url: String): String {
        val uri = URI(url.replace("mongodb","http"))
        return uri.host
    }

    private fun getPort(url: String): String {
        val uri = URI(url.replace("mongodb","http"))
        return uri.port.toString()
    }

    private fun getDatabase(url: String): String {
        val uri = URI(url.replace("mongodb","http"))
        return uri.path.substring(1)
    }

    private fun getUsername(url: String): String {
        //mongodb\://user:password@localhost\:27017/serverpackcreatordb
        var user = url.replace("mongodb://","")
        user = user.substring(0,user.indexOf(":"))
        return user
    }

    private fun getPassword(url: String): String {
        ////mongodb\://user:password@localhost\:27017/serverpackcreatordb
        var password = url.replace("mongodb://","")
        password = password.substring(password.indexOf(":") + 1,password.indexOf("@"))
        return password
    }

    override fun loadSettings() {
        databaseHostSetting.text = getHost(apiProperties.databaseUrl)
        databasePortSetting.text = getPort(apiProperties.databaseUrl)
        databaseDatabaseSetting.text = getDatabase(apiProperties.databaseUrl)
        databaseUsernameSetting.text = getUsername(apiProperties.databaseUrl)
        databasePasswordSetting.text = getPassword(apiProperties.databaseUrl)
        cleanupScheduleSetting.text = apiProperties.webserviceCleanupSchedule
        logDirectorySetting.file = apiProperties.tomcatLogsDirectory.absoluteFile
        baseDirSetting.file = apiProperties.tomcatBaseDirectory.absoluteFile
        versionScheduleSetting.text = apiProperties.webserviceVersionSchedule
        databaseCleanupScheduleSetting.text = apiProperties.webserviceDatabaseCleanupSchedule
    }

    override fun saveSettings() {
        apiProperties.databaseUrl = StringUtilities.createMongoUri(
            databaseUsernameSetting.text,
            databasePasswordSetting.text,
            databaseHostSetting.text,
            databasePortSetting.text.toInt(),
            databaseDatabaseSetting.text)
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
            cleanupScheduleIcon.error(Translations.settings_webservice_schedule_cleanup_error.toString())
            errors.add(Translations.settings_webservice_schedule_cleanup_error.toString())
        }

        if (!logDirectorySetting.file.absoluteFile.isDirectory || !logDirectorySetting.file.absoluteFile.canWrite()) {
            logDirectoryIcon.error(Translations.settings_webservice_tomcat_logs_error.toString())
            errors.add(Translations.settings_webservice_tomcat_logs_error.toString())
        } else {
            logDirectoryIcon.info()
        }

        if (!baseDirSetting.file.absoluteFile.isDirectory || !baseDirSetting.file.absoluteFile.canWrite()) {
            baseDirIcon.error(Translations.settings_webservice_tomcat_dir_error.toString())
            errors.add(Translations.settings_webservice_tomcat_dir_error.toString())
        } else {
            baseDirIcon.info()
        }

        try {
            cronParser.parse(versionScheduleSetting.text).validate()
            versionScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            versionScheduleIcon.error(Translations.settings_webservice_schedule_versions_error.toString())
            errors.add(Translations.settings_webservice_schedule_versions_error.toString())
        }

        try {
            cronParser.parse(databaseCleanupScheduleSetting.text).validate()
            databaseCleanupScheduleIcon.info()
        } catch (ex: IllegalArgumentException) {
            databaseCleanupScheduleIcon.error(Translations.settings_webservice_schedule_database_error.toString())
            errors.add(Translations.settings_webservice_schedule_database_error.toString())
        }

        if (errors.isNotEmpty()) {
            title.setAndShowErrorIcon(Translations.settings_webservice_errors.toString())
        } else {
            title.hideErrorIcon()
        }

        return errors.toList()
    }

    override fun hasUnsavedChanges(): Boolean {
        val changes =
            StringUtilities.createMongoUri(
                databaseUsernameSetting.text,
                databasePasswordSetting.text,
                databaseHostSetting.text,
                databasePortSetting.text.toInt(),
                databaseDatabaseSetting.text) != apiProperties.databaseUrl ||
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