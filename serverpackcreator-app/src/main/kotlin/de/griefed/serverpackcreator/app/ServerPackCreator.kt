/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.app

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme
import com.formdev.flatlaf.util.SystemFileChooser
import com.formdev.flatlaf.util.SystemFileChooser.DIRECTORIES_ONLY
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.JarInformation
import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import de.griefed.serverpackcreator.app.cli.InteractiveCommandLine
import de.griefed.serverpackcreator.app.gui.MainWindow
import de.griefed.serverpackcreator.app.gui.splash.SplashScreen
import de.griefed.serverpackcreator.app.updater.MigrationManager
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import de.griefed.serverpackcreator.app.web.WebService
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.io.monitor.FileEntry
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.GraphicsEnvironment
import java.io.File
import java.util.*
import kotlin.system.exitProcess
import java.util.concurrent.Executors
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import kotlin.jvm.optionals.getOrNull

/**
 * Entry point for the app. Creates a new instance of [ServerPackCreator] and executes [ServerPackCreator.run] with the
 * mode determined by [CommandlineParser].
 * @author Griefed
 */
fun main(args: Array<String>) {
    val app = ServerPackCreator(args)
    val exitCode = app.run(app.commandlineParser.mode)

    // LANDMINE - never exit unconditionally here, and never exitProcess(0).
    //
    // GUI and WEB return from run() the moment they have handed off: the GUI to the Swing event
    // dispatch thread, the webservice to the embedded server. Both keep the JVM alive on their own
    // non-daemon threads, and an exitProcess(0) on this line would kill the window or the server the
    // instant it finished starting.
    //
    // So only a failure exits explicitly. A successful run falls off the end of main and lets the JVM
    // end when nothing is left running, exactly as every mode did before exit codes existed.
    if (exitCode != EXIT_SUCCESS) {
        exitProcess(exitCode)
    }
}

/** A run that did what it was asked. Also what every long-lived mode reports, since it never fails here. */
const val EXIT_SUCCESS = 0

/** A one-shot run that did not produce what it was asked for: bad arguments, a failed check, a failed generation. */
const val EXIT_FAILURE = 1

/**
 * Create and manage instances required to run ServerPackCreator and provide access to various aspects, such as the
 * API-instance used to run a given instance of SPC.
 * @author Griefed
 */
class ServerPackCreator(private val args: Array<String>) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val appInfo = JarInformation(ServerPackCreator::class.java)
    /** The parsed arguments, which decide everything below — including which home directory the API is built against. */
    val commandlineParser: CommandlineParser = CommandlineParser(args, appInfo)

    init {
        val prefs = Optional.ofNullable(HomeDirectoryPreference.stored())
        if (commandlineParser.mode == Mode.GUI && prefs.isEmpty && commandlineParser.homeDir.isEmpty) {

            FlatJetBrainsMonoFont.install()
            FlatLaf.setPreferredFontFamily(FlatJetBrainsMonoFont.FAMILY)
            FlatMTMaterialDarkerIJTheme.setup()

            val decision = JOptionPane.showConfirmDialog(
                null,
                """
                     You haven't set the home-directory yet.
                     Running ServerPackCreator for the first time, or updating from an older version?
                     Please select the home-directory for ServerPackCreator. 
                     """.trimIndent(),
                "Pick a home-directory",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            )
            if (decision == 0) {
                val chooser = SystemFileChooser()
                chooser.currentDirectory = File(System.getProperty("user.home"))
                chooser.fileSelectionMode = DIRECTORIES_ONLY
                chooser.isMultiSelectionEnabled = false
                chooser.dialogTitle = "Pick a home-directory for ServerPackCreator"
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    HomeDirectoryPreference.store(chooser.selectedFile.absolutePath)
                }
            }
        }
    }

    /**
     * The API this process shares. Built eagerly and *before* the first log statement on purpose: `ApiProperties`
     * is log4j's own `ConfigurationFactory`, so logging first would construct one against an unresolved home.
     */
    val apiWrapper = ApiWrapper.api(commandlineParser.propertiesFile, false)

    init {
        if (commandlineParser.language != null) {
            apiWrapper.apiProperties.changeLocale(commandlineParser.language!!)
        }
        apiWrapper.apiProperties.isExe()
    }

    /** The release-feed check. Lazy, so a run that never asks about updates makes no network call. */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:Synchronized
    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(apiWrapper.apiProperties)
    }

    /** The picocli shell. Lazy, because only the interactive mode ever builds it. */
    @get:Synchronized
    val interactiveCommandLine: InteractiveCommandLine by lazy {
        InteractiveCommandLine(apiWrapper, updateChecker)
    }

    /**
     * Start the application the arguments selected — GUI, web, CLI, one of the headless verbs, or the
     * updater — and report [EXIT_SUCCESS] or [EXIT_FAILURE].
     *
     * A long-lived mode (GUI, WEB, CLI) always reports success: it has not failed, it has started, and
     * [main] deliberately does not exit on success so that it can keep running.
     */
    fun run(mode: Mode = Mode.GUI): Int {
        log.info("Running with args: ${args.joinToString(" ")}")
        log.info("Running in mode:   $mode")
        log.info("App information:")
        log.info("App Folder:        ${appInfo.jarFolder}")
        log.info("Appy File:         ${appInfo.jarFile}")
        log.info("App Path:          ${appInfo.jarPath}")
        log.info("App Name:          ${appInfo.jarFileName}")
        log.info("Java version:      ${apiWrapper.apiProperties.getJavaVersion()}")
        log.info("OS architecture:   ${apiWrapper.apiProperties.getOSArch()}")
        log.info("OS name:           ${apiWrapper.apiProperties.getOSName()}")
        log.info("OS version:        ${apiWrapper.apiProperties.getOSVersion()}")

        return when (mode) {
            Mode.WEB, Mode.CONFIG, Mode.WITHALLINCONFIGDIR, Mode.FEELINGLUCKY, Mode.CLI,
            Mode.SCAN, Mode.CLIENTSIDE_REPORT, Mode.VERIFY_CLIENTSIDE -> {

                apiWrapper.stageOne()
                migrationManager.migrate()
                apiWrapper.stageTwo()
                apiWrapper.stageThree()

                when (mode) {
                    Mode.WEB -> {
                        stageFour()
                        WebService(apiWrapper).start(args)
                        EXIT_SUCCESS
                    }

                    Mode.CONFIG -> {
                        // No path followed -config at all, so there is nothing to name in an error and
                        // nothing to run. runHeadless reports a path that merely does not exist.
                        if (commandlineParser.serverPackConfig.isEmpty) {
                            log.error(
                                "${Mode.CONFIG.argument()} requires the path to a server pack config, " +
                                        "e.g. ${Mode.CONFIG.argument()} \"/path/to/serverpackcreator.conf\"."
                            )
                            EXIT_FAILURE
                        } else {
                            exitCodeOf(
                                interactiveCommandLine.runHeadlessCommand.runHeadless(
                                    commandlineParser.serverPackConfig.get(),
                                    commandlineParser.serverPackDestination
                                )
                            )
                        }
                    }

                    Mode.WITHALLINCONFIGDIR -> {
                        exitCodeOf(interactiveCommandLine.runHeadlessCommand.withAllInConfigDir())
                    }

                    Mode.FEELINGLUCKY -> {
                        // Same shape as CONFIG above: feelingLucky reports a modpack-directory that does
                        // not exist, but it cannot report one it was never given.
                        if (commandlineParser.modpackDirectory.isEmpty) {
                            log.error(
                                "${Mode.FEELINGLUCKY.argument()} requires the path to a modpack-directory, " +
                                        "e.g. ${Mode.FEELINGLUCKY.argument()} \"/path/to/modpack\"."
                            )
                            EXIT_FAILURE
                        } else {
                            exitCodeOf(
                                interactiveCommandLine.cliCommands.feelingLucky(
                                    commandlineParser.modpackDirectory.get().absolutePath,
                                    commandlineParser.serverPackDestination.getOrNull()?.absolutePath,
                                )
                            )
                        }
                    }

                    Mode.CLI -> {
                        interactiveCommandLine.cli(args)
                        EXIT_SUCCESS
                    }

                    Mode.SCAN -> {
                        if (commandlineParser.scanDirectory.isEmpty) {
                            log.error(
                                "${Mode.SCAN.argument()} requires an existing directory of mods, " +
                                        "e.g. ${Mode.SCAN.argument()} \"/path/to/mods\" --loader Forge --minecraft 1.20.1."
                            )
                            EXIT_FAILURE
                        } else {
                            interactiveCommandLine.scanCommand.scan(
                                commandlineParser.scanDirectory.get(),
                                commandlineParser.scanLoader ?: "",
                                commandlineParser.scanMinecraftVersion ?: ""
                            )
                            EXIT_SUCCESS
                        }
                    }

                    Mode.CLIENTSIDE_REPORT -> {
                        if (commandlineParser.clientsideLink.isEmpty) {
                            log.error("${Mode.CLIENTSIDE_REPORT.argument()} requires a CurseForge or Modrinth project-link.")
                            EXIT_FAILURE
                        } else {
                            interactiveCommandLine.clientsideReportCommand.report(
                                commandlineParser.clientsideLink.get(),
                                commandlineParser.clientsideReportOutput?.let { File(it) }
                            )
                            EXIT_SUCCESS
                        }
                    }

                    Mode.VERIFY_CLIENTSIDE -> {
                        if (commandlineParser.clientsideVerifyLink.isEmpty) {
                            log.error("${Mode.VERIFY_CLIENTSIDE.argument()} requires a CurseForge or Modrinth project-link.")
                            EXIT_FAILURE
                        } else {
                            interactiveCommandLine.verifyClientsideCommand.verify(
                                commandlineParser.clientsideVerifyLink.get(),
                                commandlineParser.clientsideVerifyOutput?.let { File(it) }
                            )
                            EXIT_SUCCESS
                        }
                    }

                }

            }

            Mode.HELP -> {
                interactiveCommandLine.helpCommand.run()
                EXIT_SUCCESS
            }

            Mode.UPDATE -> {
                interactiveCommandLine.updateCommand.run()
                EXIT_SUCCESS
            }

            Mode.CGEN -> {
                apiWrapper.stageOne()
                migrationManager.migrate()
                apiWrapper.stageTwo()
                exitCodeOf(
                    interactiveCommandLine.configGenCommand.generateConfFromModpack(commandlineParser.modpackDirectory)
                )
            }

            Mode.CLIENTSIDE_APPLY -> {
                // Pure source-editing of the fallback-list files; no API staging or network needed.
                if (commandlineParser.clientsideApplyReport.isEmpty) {
                    log.error("${Mode.CLIENTSIDE_APPLY.argument()} requires the path to a clientside-report JSON.")
                    EXIT_FAILURE
                } else {
                    interactiveCommandLine.clientsideApplyCommand.apply(
                        File(commandlineParser.clientsideApplyReport.get()),
                        commandlineParser.clientsideApplyGenerationConfig?.let { File(it) },
                        commandlineParser.clientsideApplyProperties?.let { File(it) }
                    )
                    EXIT_SUCCESS
                }
            }

            Mode.GUI -> {
                splashScreen!!
                apiWrapper.stageOne()
                migrationManager.migrate()
                splashScreen!!.update(20)
                apiWrapper.stageTwo()
                splashScreen!!.update(40)
                apiWrapper.stageThree()
                splashScreen!!.update(60)
                stageFour()
                splashScreen!!.update(80)
                MainWindow(
                    apiWrapper,
                    updateChecker,
                    splashScreen!!,
                    migrationManager
                )
                EXIT_SUCCESS
            }

            Mode.SETUP -> {
                interactiveCommandLine.setupCommand.run()
                log.info("Setup completed.")
                log.debug("Exiting...")
                EXIT_SUCCESS
            }

            Mode.EXIT -> {
                log.debug("Exiting...")
                EXIT_SUCCESS
            }

            else -> {
                log.debug("Exiting...")
                EXIT_SUCCESS
            }
        }
    }

    /** Turn a verb's "did it work" into the code the process exits with. */
    private fun exitCodeOf(succeeded: Boolean) = if (succeeded) EXIT_SUCCESS else EXIT_FAILURE

    /** The splash window while the GUI starts, held so it can be closed once the main frame is up. `null` in every non-GUI mode. */
    @get:Synchronized
    var splashScreen: SplashScreen? = null
        get() {
            if (GraphicsEnvironment.isHeadless()) {
                throw RuntimeException("Graphical environment not supported!")
            }
            if (field == null) {
                field = SplashScreen(
                    apiWrapper.apiProperties.apiVersion
                )
            }
            return field!!
        }

    /** Runs the release-to-release migrations for this installation. Lazy, so a mode that touches no settings does not. */
    @get:Synchronized
    val migrationManager: MigrationManager by lazy {
        MigrationManager(
            apiWrapper.apiProperties,
            apiWrapper.tomlParser
        )
    }

    /**
     * Initialize our FileWatcher to ensure that vital files get restored, should they be deleted
     * whilst ServerPackCreator is running.
     *
     *
     * Files which will be restored are:
     *
     *  * serverpackcreator.properties
     *  * Default server.properties
     *  * Default server-icon.png
     *  * Default PowerShell script template
     *  * Default Shell script template
     *
     *
     * @author Griefed
     */
    private fun stageFour() {
        Executors.newSingleThreadExecutor().execute {
            log.debug("Setting up FileWatcher...")

            val fileAlterationObserver = FileAlterationObserver.builder()
                .setRootEntry(FileEntry(apiWrapper.apiProperties.homeDirectory))
                .get()

            val fileAlterationListener: FileAlterationListener = object : FileAlterationListener {
                override fun onStart(observer: FileAlterationObserver) {
                }

                override fun onDirectoryCreate(directory: File) {
                }

                override fun onDirectoryChange(directory: File) {
                }

                override fun onDirectoryDelete(directory: File) {
                }

                override fun onFileCreate(file: File) {
                }

                override fun onFileChange(file: File) {
                }

                override fun onFileDelete(file: File) {
                    if (!file.toString()
                            .contains(apiWrapper.apiProperties.serverPacksDirectory.toString())
                        && !file.toString()
                            .contains(apiWrapper.apiProperties.modpacksDirectory.toString())
                    ) {
                        if (check(file, apiWrapper.apiProperties.serverPackCreatorPropertiesFile)) {
                            createFile(apiWrapper.apiProperties.serverPackCreatorPropertiesFile)
                            apiWrapper.apiProperties.loadProperties(false)
                            log.info("Restored serverpackcreator.properties and loaded defaults.")
                        } else if (check(file, apiWrapper.apiProperties.defaultServerProperties)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultServerProperties)
                            log.info("Restored default server.properties.")
                        } else if (check(file, apiWrapper.apiProperties.defaultServerIcon)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultServerIcon)
                            log.info("Restored default server-icon.png.")
                        } else if (check(file, apiWrapper.apiProperties.defaultShellScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultShellScriptTemplate)
                            log.info("Restored default_template.sh.")
                        } else if (check(file, apiWrapper.apiProperties.defaultFishScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultFishScriptTemplate)
                            log.info("Restored default_template.fish.")
                        } else if (check(file, apiWrapper.apiProperties.defaultBatchScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultBatchScriptTemplate)
                            log.info("Restored default_template.bat.")
                        } else if (check(file, apiWrapper.apiProperties.defaultPowerShellScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultPowerShellScriptTemplate)
                            log.info("Restored default_template.ps1.")
                        } else if (check(file, apiWrapper.apiProperties.defaultJavaShellScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultJavaShellScriptTemplate)
                            log.info("Restored default_Java_template.sh.")
                        } else if (check(file, apiWrapper.apiProperties.defaultJavaFishScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultJavaFishScriptTemplate)
                            log.info("Restored default_Java_template.fish.")
                        } else if (check(file, apiWrapper.apiProperties.defaultJavaPowerShellScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultJavaPowerShellScriptTemplate)
                            log.info("Restored default_Java_template.ps1.")
                        } else if (check(file, apiWrapper.apiProperties.defaultVariablesTemplate)) {
                            // Generation reads this template, so a deleted one would otherwise fall back to the copy in
                            // the jar silently — restoring it keeps what the operator edits and what generation uses the
                            // same file.
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultVariablesTemplate)
                            log.info("Restored variables.txt.")
                        }
                    }
                }

                override fun onStop(observer: FileAlterationObserver) {
                }

                private fun check(
                    watched: File,
                    toCreate: File
                ): Boolean {
                    return watched.name == toCreate.name
                }

                private fun createFile(toCreate: File) {
                    JarUtilities.copyFileFromJar(
                        toCreate.name, ServerPackCreator::class.java,
                        toCreate.parent
                    )
                }
            }
            fileAlterationObserver.addListener(fileAlterationListener)
            val fileAlterationMonitor = FileAlterationMonitor(1000)
            fileAlterationMonitor.addObserver(fileAlterationObserver)
            try {
                fileAlterationMonitor.start()
            } catch (ex: Exception) {
                log.error("Error starting the FileWatcher Monitor.", ex)
            }
            log.debug("File-watcher started...")
        }
    }
}