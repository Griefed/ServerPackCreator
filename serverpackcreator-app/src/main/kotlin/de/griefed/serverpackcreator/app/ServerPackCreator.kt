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
package de.griefed.serverpackcreator.app

import Translations
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme
import de.comahe.i18n4k.Locale
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
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.xml.sax.SAXException
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.xml.parsers.ParserConfigurationException

/**
 * Entry point for the app. Creates a new instance of [ServerPackCreator] and executes [ServerPackCreator.run] with the
 * mode determined by [CommandlineParser].
 * @author Griefed
 */
fun main(args: Array<String>) {
    val app = ServerPackCreator(args)
    app.run(app.commandlineParser.mode)
}

/**
 * Create and manage instances required to run ServerPackCreator and provide access to various aspects, such as the
 * API-instance used to run a given instance of SPC.
 * @author Griefed
 */
class ServerPackCreator(private val args: Array<String>) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val appInfo = JarInformation(ServerPackCreator::class.java)
    val commandlineParser: CommandlineParser = CommandlineParser(args, appInfo)

    init {
        val prefs = Optional.ofNullable(
            Preferences.userRoot().node("ServerPackCreator").get("de.griefed.serverpackcreator.home", null)
        )
        if (commandlineParser.mode == Mode.GUI && prefs.isEmpty && commandlineParser.homeDir.isEmpty) {

            FlatJetBrainsMonoFont.install()
            FlatLaf.setPreferredFontFamily(FlatJetBrainsMonoFont.FAMILY)
            FlatMaterialDarkerIJTheme.setup()

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
                val chooser = JFileChooser()
                chooser.currentDirectory = File(System.getProperty("user.home"))
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                chooser.isMultiSelectionEnabled = false
                chooser.dialogTitle = "Pick a home-directory for ServerPackCreator"
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    Preferences.userRoot().node("ServerPackCreator").put(
                        "de.griefed.serverpackcreator.home",
                        chooser.selectedFile.absolutePath
                    )
                }
            }
        }
    }

    val apiWrapper = ApiWrapper.api(commandlineParser.propertiesFile, false)

    init {
        if (commandlineParser.language != null) {
            apiWrapper.apiProperties.changeLocale(commandlineParser.language!!)
        }
        apiWrapper.apiProperties.isExe()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @get:Synchronized
    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(apiWrapper.apiProperties)
    }

    @get:Synchronized
    val interactiveCommandLine: InteractiveCommandLine by lazy {
        InteractiveCommandLine(apiWrapper, updateChecker)
    }

    fun run(mode: Mode = Mode.GUI) {
        log.info("Running with args: ${args.joinToString(" ")}")
        log.info("Running in mode: $mode")
        log.info("App information:")
        log.info("App Folder:      ${appInfo.jarFolder}")
        log.info("Appy File:        ${appInfo.jarFile}")
        log.info("App Path:        ${appInfo.jarPath}")
        log.info("App Name:        ${appInfo.jarFileName}")
        log.info("Java version:    ${apiWrapper.apiProperties.getJavaVersion()}")
        log.info("OS architecture: ${apiWrapper.apiProperties.getOSArch()}")
        log.info("OS name:         ${apiWrapper.apiProperties.getOSName()}")
        log.info("OS version:      ${apiWrapper.apiProperties.getOSVersion()}")

        when (mode) {
            Mode.HELP -> {
                interactiveCommandLine.helpCommand.run()
            }

            Mode.UPDATE -> {
                interactiveCommandLine.updateCommand.run()
            }

            Mode.WEB -> {
                apiWrapper.stageOne()
                migrationManager.migrate()
                apiWrapper.stageTwo()
                apiWrapper.stageThree()
                stageFour()
                WebService(apiWrapper).start(args)
            }

            Mode.CGEN -> {
                apiWrapper.stageOne()
                migrationManager.migrate()
                apiWrapper.stageTwo()
                interactiveCommandLine.configGenCommand.run()
            }

            Mode.CLI -> {
                apiWrapper.stageOne()
                migrationManager.migrate()
                apiWrapper.stageTwo()
                apiWrapper.stageThree()
                interactiveCommandLine.run(args)
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
            }

            Mode.SETUP -> {
                interactiveCommandLine.setupCommand.run()
                log.info("Setup completed.")
                log.debug("Exiting...")
            }

            Mode.EXIT -> log.debug("Exiting...")
            else -> log.debug("Exiting...")
        }
    }

    /**
     * Print the text-menu so the user may decide what they would like to do next.
     *
     * @author Griefed
     */
    private fun printMenu() {
        println()
        println("What would you like to do next?")
        println("(1) : Print help")
        println("(2) : Check for updates")
        println("(3) : Change locale")
        println("(4) : Generate a new configuration")
        println("(5) : Run ServerPackCreator in CLI-mode")
        println("(6) : Run ServerPackCreator as a webservice")
        println("(7) : Run ServerPackCreator with a GUI")
        println("(0) : Exit")
        println("-------------------------------------------")
        print("Enter the number of your selection: ")
    }

    /**
     * Offer the user to continue using ServerPackCreator when running in [Mode.HELP], [Mode.UPDATE] or [Mode.CGEN].
     *
     * @throws IOException                  When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to
     * be instantiated, but an error occurred during the parsing of a manifest.
     * @throws ParserConfigurationException When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to
     * be instantiated, but an error occurred during the parsing of a manifest.
     * @throws SAXException                 When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to
     * be instantiated, but an error occurred during the parsing of a manifest.
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun continuedRunOptions() {
        printMenu()
        val scanner = Scanner(System.`in`)
        var selection: Int
        do {
            try {
                selection = scanner.nextInt()
                if (selection == 7 && GraphicsEnvironment.isHeadless()) {
                    println("You environment does not support a GUI.")
                    selection = 100
                }
                when (selection) {
                    1 -> {
                        interactiveCommandLine.helpCommand.run()
                        printMenu()
                        selection = 100
                    }

                    2 -> {
                        interactiveCommandLine.updateCommand.run()
                        printMenu()
                        selection = 100
                    }

                    3 -> {
                        interactiveCommandLine.languageCommand.run()
                        printMenu()
                        selection = 100
                    }

                    4 -> {
                        interactiveCommandLine.configGenCommand.run()
                        printMenu()
                        selection = 100
                    }

                    else -> if (selection > 7) {
                        println("Not a valid number. Please pick a number from 0 to 7.")
                        printMenu()
                    }
                }
            } catch (ex: InputMismatchException) {
                println("Not a valid number. Please pick a number from 0 to 7.")
                selection = 100
            } catch (ex: ParserConfigurationException) {
                println("Not a valid number. Please pick a number from 0 to 7.")
                selection = 100
            } catch (ex: SAXException) {
                println("Not a valid number. Please pick a number from 0 to 7.")
                selection = 100
            }
        } while (selection > 7)
        scanner.close()
        when (selection) {
            5 -> run(Mode.CLI)
            6 -> run(Mode.WEB)
            7 -> run(Mode.GUI)
            0 -> println("Exiting...")
            else -> println("Exiting...")
        }
    }

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
            val fileAlterationObserver = FileAlterationObserver(
                apiWrapper.apiProperties.homeDirectory
            )
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
                        } else if (check(file, apiWrapper.apiProperties.defaultBatchScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultBatchScriptTemplate)
                            log.info("Restored default_template.bat.")
                        } else if (check(file, apiWrapper.apiProperties.defaultPowerShellScriptTemplate)) {
                            apiWrapper.checkServerFilesFile(apiWrapper.apiProperties.defaultPowerShellScriptTemplate)
                            log.info("Restored default_template.ps1.")
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

    /**
     * Allow the user to change the locale used in localization.
     *
     * @author Griefed
     */
    private fun changeLocale() {
        println("What locale would you like to use?")
        println("(Locale format is en_gb, de_de, uk_ua etc.)")
        println("Note: Changing the locale only affects the GUI. CLI always uses en_US.")
        val scanner = Scanner(System.`in`)
        val regex = "^[a-zA-Z]+_[a-zA-Z]+$".toRegex()
        var userLocale: String

        // For a list of locales, see https://stackoverflow.com/a/3191729/12537638 or
        // https://stackoverflow.com/a/28357857/12537638
        do {
            userLocale = scanner.next()
            if (!userLocale.matches(regex)) {
                println(
                    "Incorrect format. ServerPackCreator currently only supports locales in the format of en_us (Language, Country)."
                )
            } else {
                try {
                    apiWrapper.apiProperties.i18n4kConfig.locale = Locale(userLocale)
                } catch (e: RuntimeException) {
                    println(
                        "Incorrect format. ServerPackCreator currently only supports locales in the format of en_GB (Language, Country)."
                    )
                    userLocale = "en_GB"
                }
            }
        } while (!userLocale.matches(regex))
        scanner.close()
        println("Using language: ${Translations.localeName}")
    }
}