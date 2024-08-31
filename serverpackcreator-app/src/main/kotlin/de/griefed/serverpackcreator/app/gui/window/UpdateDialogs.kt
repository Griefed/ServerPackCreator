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
package de.griefed.serverpackcreator.app.gui.window

import Translations
import com.install4j.api.Util
import com.install4j.api.context.UserCanceledException
import com.install4j.api.launcher.ApplicationLauncher
import com.install4j.api.launcher.Variables
import com.install4j.api.update.ApplicationDisplayMode
import com.install4j.api.update.UpdateDescriptor
import com.install4j.api.update.UpdateDescriptorEntry
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import de.griefed.serverpackcreator.app.updater.versionchecker.Update
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ExecutionException
import javax.swing.*
import javax.swing.text.*

/**
 * Utility-class for checking and displaying dialogs when an update is available or not.
 *
 * @author Griefed
 */
@Suppress("MemberVisibilityCanBePrivate")
class UpdateDialogs(
    private val guiProps: GuiProps,
    private val webUtilities: WebUtilities,
    private val apiProperties: ApiProperties,
    private val updateChecker: UpdateChecker,
    private val mainFrame: JFrame
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    var update: Optional<Update> = updateChecker.checkForUpdate(
        apiProperties.apiVersion,
        apiProperties.isCheckingForPreReleasesEnabled
    )
        private set
    var i4JUpdatable = false
        private set

    init {
        checkForUpdateWithApi()
    }

    /**
     * If an update for ServerPackCreator is available, display a dialog letting the user choose whether they want to
     * visit the releases page or do nothing.
     *
     * @return `true` if an update was found and the dialog displayed.
     * @author Griefed
     */
    private fun displayUpdateDialog(): Boolean {
        return if (update.isPresent) {
            val textContent: String = Translations.update_dialog_new(update.get().url())
            val styledDocument: StyledDocument = DefaultStyledDocument()
            val simpleAttributeSet = SimpleAttributeSet()
            val jTextPane = JTextPane(styledDocument)
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val options = arrayOfNulls<String>(3)
            StyleConstants.setBold(simpleAttributeSet, true)
            StyleConstants.setFontSize(simpleAttributeSet, 14)
            jTextPane.setCharacterAttributes(simpleAttributeSet, true)
            StyleConstants.setAlignment(simpleAttributeSet, StyleConstants.ALIGN_LEFT)
            styledDocument.setParagraphAttributes(
                0, styledDocument.length, simpleAttributeSet, false
            )
            jTextPane.isOpaque = false
            jTextPane.isEditable = false
            if (i4JUpdatable) {
                options[0] = Translations.update_dialog_update.toString()
            } else {
                options[0] = Translations.update_dialog_yes.toString()
            }
            options[1] = Translations.update_dialog_no.toString()
            options[2] = Translations.update_dialog_clipboard.toString()
            try {
                styledDocument.insertString(0, textContent, simpleAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            when (DialogUtilities.createShowGet(
                jTextPane,
                Translations.update_dialog_available.toString(),
                mainFrame,
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                guiProps.infoIcon,
                resizable = true,
                options,
                options[0]
            )) {
                0 -> try {
                    if (i4JUpdatable) {
                        downloadAndUpdate()
                    } else {
                        webUtilities.openLinkInBrowser(update.get().url().toURI())
                    }
                } catch (ex: RuntimeException) {
                    log.error("Error opening browser.", ex)
                } catch (ex: URISyntaxException) {
                    log.error("Error opening browser.", ex)
                }

                1 -> clipboard.setContents(StringSelection(update.get().url().toString()), null)
                else -> {}
            }
            true
        } else {
            false
        }
    }

    /**
     * @author Griefed
     */
    fun checkForUpdate(): Boolean {
        update = updateChecker.checkForUpdate(apiProperties.apiVersion, apiProperties.isCheckingForPreReleasesEnabled)
        checkForUpdateWithApi()
        if (!displayUpdateDialog()) {
            DialogUtilities.createDialog(
                Translations.menubar_gui_menuitem_updates_none.toString() + "   ",
                Translations.menubar_gui_menuitem_updates_none_title.toString() + "   ",
                mainFrame,
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                guiProps.infoIcon,
                resizable = true
            )
        }
        return update.isPresent
    }

    private fun isUpdatable(): Boolean {
        try {
            val installationDirectory = Paths.get(Variables.getInstallerVariable("sys.installationDir").toString())
            return !Files.getFileStore(installationDirectory).isReadOnly && (Util.isWindows() || Util.isMacOS() || (Util.isLinux() && !Util.isArchive()))
        } catch (ex: IOException) {
            log.error("Error checking for install4j updatability.", ex)
        }
        return false
    }

    private fun checkForUpdateWithApi() {
        try {
            if (isUpdatable()) {
                // Here we check for updates in the background with the API.
                object : SwingWorker<UpdateDescriptorEntry, Any?>() {
                    @Throws(Exception::class)
                    override fun doInBackground(): UpdateDescriptorEntry {
                        // The compiler variable sys.updatesUrl holds the URL where the updates.xml file is hosted.
                        // That URL is defined on the "Installer->Auto Update Options" step.
                        // The same compiler variable is used by the "Check for update" actions that are contained in the update
                        // downloaders.
                        val updateUrl: String = Variables.getCompilerVariable("sys.updatesUrl")
                        val updateDescriptor: UpdateDescriptor =
                            com.install4j.api.update.UpdateChecker.getUpdateDescriptor(updateUrl, ApplicationDisplayMode.GUI)
                        // If getPossibleUpdateEntry returns a non-null value, the version number in the updates.xml file
                        // is greater than the version number of the local installation.
                        return updateDescriptor.possibleUpdateEntry
                    }

                    override fun done() {
                        try {
                            val updateDescriptorEntry: UpdateDescriptorEntry? = get()
                            // only installers and single bundle archives on macOS are supported for background updates
                            if (updateDescriptorEntry != null && (!updateDescriptorEntry.isArchive || updateDescriptorEntry.isSingleBundle)) {
                                // An update is available for download
                                i4JUpdatable = true
                            }
                        } catch (e: InterruptedException) {
                            log.error("Update interrupted.", e)
                        } catch (e: ExecutionException) {
                            val cause = e.cause
                            // UserCanceledException means that the user has canceled the proxy dialog
                            if (cause !is UserCanceledException) {
                                log.error("Update could not be executed.", e)
                            }
                        }
                    }
                }.execute()
            } else {
                i4JUpdatable = false
            }
        } catch (ncdfe: NoClassDefFoundError) {
            i4JUpdatable = false
            log.debug("Not an install4j installation.")
        }
    }

    private fun downloadAndUpdate() {
        // Here the background update downloader is launched in the background
        // See checkForUpdate(), where the interactive updater is launched for comments on launching an update downloader.
        object : SwingWorker<Any?, Any?>() {
            @Throws(java.lang.Exception::class)
            override fun doInBackground(): Any? {
                // Note the third argument which makes the call to the background update downloader blocking.
                // The callback receives progress information from the update downloader and changes the text on the button
                ApplicationLauncher.launchApplication("442", null, true, null)
                // At this point, the update downloader has returned, and we can check if the "Schedule update installation"
                // action has registered an update installer for execution
                // We now switch to the EDT in done() for terminating the application
                return null
            }

            override fun done() {
                try {
                    get() // rethrow exceptions that occurred in doInBackground() wrapped in an ExecutionException
                    if (com.install4j.api.update.UpdateChecker.isUpdateScheduled()) {
                        JOptionPane.showMessageDialog(
                            mainFrame,
                            Translations.update_dialog_update_message.toString(),
                            Translations.update_dialog_update_title.toString(),
                            JOptionPane.INFORMATION_MESSAGE
                        )
                        // We execute the update immediately, but you could ask the user whether the update should be
                        // installed now. The scheduling of update installers is persistent, so this will also work
                        // after a restart of the launcher.
                        executeUpdate()
                    } else {
                        JOptionPane.showMessageDialog(
                            mainFrame,
                            Translations.update_dialog_update_failed_message.toString(),
                            Translations.update_dialog_update_title.toString(),
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                } catch (e: InterruptedException) {
                    log.error("Update interrupted.", e)
                } catch (e: ExecutionException) {
                    log.error("Update could not be executed.", e)
                    JOptionPane.showMessageDialog(
                        mainFrame,
                        Translations.update_dialog_update_failed_cause(e.cause!!.message.toString()),
                        Translations.update_dialog_update_title.toString(),
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.execute()
    }

    private fun executeUpdate() {
        // The arguments that are passed to the installer switch the default GUI mode to an unattended
        // mode with a progress bar. "-q" activates unattended mode, and "-splash Updating hello world ..."
        // shows a progress bar with the specified title.
        Thread {
            com.install4j.api.update.UpdateChecker.executeScheduledUpdate(
                mutableListOf(
                    "-q",
                    "-splash",
                    "Updating ServerPackCreator ...",
                    "-alerts"
                ), true, null
            )
        }.start()
    }
}