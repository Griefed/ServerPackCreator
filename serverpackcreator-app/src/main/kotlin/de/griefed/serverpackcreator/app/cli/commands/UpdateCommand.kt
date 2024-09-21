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
package de.griefed.serverpackcreator.app.cli.commands

import Translations
import com.install4j.api.Util
import com.install4j.api.context.UserCanceledException
import com.install4j.api.launcher.ApplicationLauncher
import com.install4j.api.launcher.Variables
import com.install4j.api.update.ApplicationDisplayMode
import com.install4j.api.update.UpdateDescriptor
import com.install4j.api.update.UpdateDescriptorEntry
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutionException

@CommandLine.Command(
    name = "update", mixinStandardHelpOptions = true,
    description = [
        "Check for available updates.",
        "If you installed ServerPackCreator using the official installers, and if an update is available, the update will be installed automatically."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class UpdateCommand(private val updateChecker: UpdateChecker = UpdateChecker(ApiWrapper.api().apiProperties)) : Command {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private var i4JUpdatable = false
    private var i4JDownload = false
    private var i4JExecute = false

    init {
        checkForUpdateWithApi()
    }

    override fun run() {
        checkAndRunUpdate()
    }

    private fun checkAndRunUpdate() {
        val updateAvailable = updateChecker.updateCheck(true)
        if (updateAvailable) {
            return
        }
        if (i4JUpdatable && !i4JDownload && !i4JExecute) {
            checkForUpdateI4J()
            if (i4JUpdatable) {
                if (i4JDownload) {
                    downloadAndUpdate()
                } else if (i4JExecute) {
                    executeUpdate()
                }
            }
        }
    }

    private fun executeUpdate() {
        // The arguments that are passed to the installer switch the default GUI mode to an unattended
        // mode with a progress bar. "-q" activates unattended mode, and "-splash Updating hello world ..."
        // shows a progress bar with the specified title.
        Thread {
            com.install4j.api.update.UpdateChecker.executeScheduledUpdate(
                mutableListOf(
                    "-q",
                    "Updating ServerPackCreator ...",
                    "-alerts"
                ), true, null
            )
        }.start()
    }

    private fun downloadAndUpdate() {
        // Here the background update downloader is launched
        // Note the third argument which makes the call to the background update downloader blocking.
        // The callback receives progress information from the update downloader and changes the text on the button
        try {
            ApplicationLauncher.launchApplication("524", null, true, object : ApplicationLauncher.Callback {
                override fun exited(exitValue: Int) {
                }

                override fun prepareShutdown() {
                }

                override fun createProgressListener(): ApplicationLauncher.ProgressListener {
                    return object : ApplicationLauncher.ProgressListenerAdapter() {
                        var downloading: Boolean = false
                        override fun actionStarted(id: String) {
                            downloading = id == "downloadFile"
                        }

                        override fun percentCompleted(value: Int) {
                            if (downloading) {
                                printProgress(value)
                            }
                        }

                        override fun indeterminateProgress(indeterminateProgress: Boolean) {
                            printProgress(-1)
                        }
                    }
                }
            })
            // At this point, the update downloader has returned, and we can check if the "Schedule update installation"
            // action has registered an update installer for execution
            // We now switch to the EDT in done() for terminating the application

            if (com.install4j.api.update.UpdateChecker.isUpdateScheduled()) {
                println(Translations.update_dialog_update_message.toString())
                // We execute the update immediately, but you could ask the user whether the update should be
                // installed now. The scheduling of update installers is persistent, so this will also work
                // after a restart of the launcher.
                executeUpdate()
            } else {
                log.error(Translations.update_dialog_update_failed_message.toString())
            }
        } catch (e: InterruptedException) {
            log.error("Update interrupted.", e)
        } catch (e: ExecutionException) {
            log.error(Translations.update_dialog_update_failed_cause(e.cause!!.message.toString()))
            log.error("Update could not be executed.", e)
        }

    }

    private fun printProgress(value: Int) {
        if (value == -1) {
            println("Downloading...")
        } else {
            println("$value%...")
        }
    }

    private fun checkForUpdateI4J() {
        // Here, the "Standalone update downloader" application is launched in a new process.
        // The ID of the installer application is shown in the install4j IDE on the Installer->Screens & Actions step
        // when the "Show IDs" toggle button is selected.
        // Use the "Integration wizard" button on the "Launcher integration" tab in the configuration
        // panel of the installer application, to get such a code snippet.
        try {
            ApplicationLauncher.launchApplication("462", null, false, null)
            // This call returns immediately, because the "blocking" argument is set to false
        } catch (e: IOException) {
            log.error("Error launching updater.", e)
        }
    }

    private fun checkForUpdateWithApi() {
        try {
            if (isI4JUpdatable()) {
                // The compiler variable sys.updatesUrl holds the URL where the updates.xml file is hosted.
                // That URL is defined on the "Installer->Auto Update Options" step.
                // The same compiler variable is used by the "Check for update" actions that are contained in the update
                // downloaders.
                val updateUrl: String = Variables.getCompilerVariable("sys.updatesUrl")
                val updateDescriptor: UpdateDescriptor =
                    com.install4j.api.update.UpdateChecker.getUpdateDescriptor(updateUrl, ApplicationDisplayMode.GUI)

                try {
                    // If getPossibleUpdateEntry returns a non-null value, the version number in the updates.xml file
                    // is greater than the version number of the local installation.
                    val updateDescriptorEntry: UpdateDescriptorEntry? = updateDescriptor.possibleUpdateEntry

                    // only installers and single bundle archives on macOS are supported for background updates
                    if (updateDescriptorEntry != null && (!updateDescriptorEntry.isArchive || updateDescriptorEntry.isSingleBundle)) {
                        if (!updateDescriptorEntry.isDownloaded) {
                            // An update is available for download
                            i4JDownload = true
                        } else if (com.install4j.api.update.UpdateChecker.isUpdateScheduled()) {
                            // The update has been downloaded, but the installation did not succeed yet.
                            i4JExecute = true
                        }
                    }
                } catch (e: InterruptedException) {
                    log.error("Update interrupted.", e)
                } catch (e: ExecutionException) {
                    val cause = e.cause
                    // UserCanceledException means that the user has canceled the proxy dialog
                    if (cause !is UserCanceledException) {
                        log.error("Update could not be executed: ${e.message}.")
                    }
                }

            } else {
                i4JUpdatable = false
            }
        } catch (ncdfe: NoClassDefFoundError) {
            i4JUpdatable = false
            log.debug("Not an install4j installation.")
        }
    }

    fun isI4JUpdatable(): Boolean {
        try {
            val installationDirectory = Paths.get(Variables.getInstallerVariable("sys.installationDir").toString())
            i4JUpdatable = true
            return !Files.getFileStore(installationDirectory).isReadOnly && (Util.isWindows() || Util.isMacOS() || (Util.isLinux() && !Util.isArchive()))
        } catch (ex: IOException) {
            log.error("Error checking for install4j updatability.", ex)
        }
        return false
    }
}