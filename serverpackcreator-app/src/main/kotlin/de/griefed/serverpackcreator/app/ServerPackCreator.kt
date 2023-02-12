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
package de.griefed.serverpackcreator.app

import Api
import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.JarInformation
import de.griefed.serverpackcreator.api.utilities.common.readText
import de.griefed.serverpackcreator.cli.ConfigurationEditor
import de.griefed.serverpackcreator.gui.splash.SplashScreen
import de.griefed.serverpackcreator.gui.window.MainWindow
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import de.griefed.serverpackcreator.web.WebService
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
import javax.xml.parsers.ParserConfigurationException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val app = ServerPackCreator(args)
    app.run(app.commandlineParser.mode)
}

class ServerPackCreator(private val args: Array<String>) {
    private val log = cachedLoggerOf(this.javaClass)
    val commandlineParser: CommandlineParser = CommandlineParser(args)
    private val api = ApiWrapper.api(commandlineParser.propertiesFile, commandlineParser.language, false)
    private val appInfo = JarInformation(ServerPackCreator::class.java, api.jarUtilities)

    @Suppress("MemberVisibilityCanBePrivate")
    val updateChecker = UpdateChecker(api.apiProperties)

    @get:Synchronized
    var configurationEditor: ConfigurationEditor? = null
        get() {
            if (field == null) {
                field = ConfigurationEditor(
                    api.configurationHandler!!,
                    api.apiProperties,
                    api.utilities!!,
                    api.versionMeta!!
                )
            }
            return field!!
        }

    fun run(mode: Mode = Mode.GUI) {
        log.info("App information:")
        log.info("App Folder:      ${appInfo.jarFolder}")
        log.info("App Path:        ${appInfo.jarFile.absolutePath}")
        log.info("App Name:        ${appInfo.jarFileName}")
        log.info("Java version:    ${api.apiProperties.getJavaVersion()}")
        log.info("OS architecture: ${api.apiProperties.getOSArch()}")
        log.info("OS name:         ${api.apiProperties.getOSName()}")
        log.info("OS version:      ${api.apiProperties.getOSVersion()}")

        when (mode) {
            Mode.HELP -> {
                printHelp()
                continuedRunOptions()
            }

            Mode.UPDATE -> {
                updateChecker.updateCheck(true)
                continuedRunOptions()
            }

            Mode.WEB -> {
                api.stageOne()
                migrationManager!!.migrate()
                api.stageTwo()
                api.stageThree()
                stageFour()
                WebService(api).start(args)
            }

            Mode.CGEN -> {
                api.stageOne()
                migrationManager!!.migrate()
                api.stageTwo()
                createDefaultConfig()
                runConfigurationEditor()
                continuedRunOptions()
            }

            Mode.CLI -> {
                api.stageOne()
                migrationManager!!.migrate()
                api.stageTwo()
                api.stageThree()
                runHeadless()
            }

            Mode.GUI -> {
                splashScreen!!
                api.stageOne()
                migrationManager!!.migrate()
                splashScreen!!.update(20)
                api.stageTwo()
                splashScreen!!.update(40)
                api.stageThree()
                splashScreen!!.update(60)
                createDefaultConfig()
                Executors.newSingleThreadExecutor().execute { stageFour() }
                splashScreen!!.update(80)
                Thread(mainWindow!!).start()
            }

            Mode.SETUP -> {
                api.setup(force = true)
                log.info("Setup completed.")
                log.debug("Exiting...")
            }

            Mode.EXIT -> log.debug("Exiting...")
            else -> log.debug("Exiting...")
        }
    }

    /**
     * Check whether a `serverpackcreator.conf`-file exists. If it doesn't exist, and we are not
     * running in [Mode.CLI] or [Mode.CGEN], create an unconfigured default one which can
     * then be loaded into the GUI.
     *
     * @return `true` if a `serverpackcreator.conf`-file was created.
     * @author Griefed
     */
    private fun createDefaultConfig(): Boolean {
        return if (!api.apiProperties.defaultConfig.exists()) {
            api.utilities!!.jarUtilities.copyFileFromJar(
                "de/griefed/resources/${api.apiProperties.defaultConfig.name}",
                api.apiProperties.defaultConfig, this.javaClass
            )
        } else false
    }

    /**
     * Prints the help-text to the console. The help text contains information about:
     *
     *  * running ServerPackCreator in different modes:
     *
     *  * [Mode.CGEN]
     *  * [Mode.UPDATE]
     *  * [Mode.CLI]
     *  * [Mode.WEB]
     *  * [Mode.GUI]
     *  * [Mode.SETUP]
     *
     *  * available languages
     *  * where to report issues
     *  * where to get support
     *  * where to find the wiki
     *  * how to support me
     *
     *
     * @author Griefed
     */
    private fun printHelp() {
        try {
            println(
                this.javaClass.getResourceAsStream("/de/griefed/resources/cli_help.txt")!!.readText()
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
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
                        printHelp()
                        printMenu()
                        selection = 100
                    }

                    2 -> {
                        updateChecker.updateCheck(true)
                        printMenu()
                        selection = 100
                    }

                    3 -> {
                        changeLocale()
                        printMenu()
                        selection = 100
                    }

                    4 -> {
                        runConfigurationEditor()
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

    /**
     * Run in [Mode.CGEN] and allow the user to load, edit and create a
     * `serverpackcreator.conf`-file using the CLI.
     *
     * @throws IOException                  When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws ParserConfigurationException When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws SAXException                 When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun runConfigurationEditor() {
        configurationEditor!!.continuedRunOptions()
    }

    /**
     * Run ServerPackCreator in [Mode.CLI]. Requires a `serverpackcreator.conf`-file to be
     * present.
     *
     * @throws IOException                  When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws ParserConfigurationException When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws SAXException                 When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun runHeadless() {
        if (!api.apiProperties.defaultConfig.exists()) {
            log.warn("No serverpackcreator.conf found...")
            log.info("If you want to run ServerPackCreator in CLI-mode, a serverpackcreator.conf is required.")
            log.info(
                "Either copy an existing config, or run ServerPackCreator with the '-cgen'-argument to generate one via commandline."
            )
            exitProcess(1)
        } else {
            val packConfig = PackConfig()
            if (api.configurationHandler!!.checkConfiguration(
                    api.apiProperties.defaultConfig, packConfig
                )
            ) {
                exitProcess(1)
            }
            if (!api.serverPackHandler!!.run(packConfig)) {
                exitProcess(1)
            }
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
                    api.apiProperties.apiVersion
                )
            }
            return field!!
        }

    @get:Synchronized
    var mainWindow: MainWindow? = null
        get() {
            if (GraphicsEnvironment.isHeadless()) {
                throw RuntimeException("Graphical environment not supported!")
            }
            if (field == null) {
                field = MainWindow(
                    api.configurationHandler!!,
                    api.serverPackHandler!!,
                    api.apiProperties,
                    api.versionMeta!!,
                    api.utilities!!,
                    updateChecker,
                    splashScreen!!,
                    api.apiPlugins!!,
                    migrationManager!!
                )
            }
            return field
        }

    @get:Synchronized
    var migrationManager: MigrationManager? = null
        get() {
            if (field == null) {
                field = MigrationManager(
                    api.apiProperties,
                    api.tomlParser
                )
            }
            return field
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
        log.debug("Setting up FileWatcher...")
        val fileAlterationObserver = FileAlterationObserver(
            api.apiProperties.homeDirectory
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
                        .contains(api.apiProperties.serverPacksDirectory.toString())
                    && !file.toString()
                        .contains(api.apiProperties.modpacksDirectory.toString())
                ) {
                    if (check(file, api.apiProperties.serverPackCreatorPropertiesFile)) {
                        createFile(api.apiProperties.serverPackCreatorPropertiesFile)
                        api.apiProperties.loadProperties()
                        log.info("Restored serverpackcreator.properties and loaded defaults.")
                    } else if (check(file, api.apiProperties.defaultServerProperties)) {
                        api.checkServerFilesFile(api.apiProperties.defaultServerProperties)
                        log.info("Restored default server.properties.")
                    } else if (check(file, api.apiProperties.defaultServerIcon)) {
                        api.checkServerFilesFile(api.apiProperties.defaultServerIcon)
                        log.info("Restored default server-icon.png.")
                    } else if (check(file, api.apiProperties.defaultShellScriptTemplate)) {
                        api.checkServerFilesFile(api.apiProperties.defaultShellScriptTemplate)
                        log.info("Restored default_template.sh.")
                    } else if (check(file, api.apiProperties.defaultPowerShellScriptTemplate)) {
                        api.checkServerFilesFile(api.apiProperties.defaultPowerShellScriptTemplate)
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
                api.utilities!!.jarUtilities.copyFileFromJar(
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

    /**
     * Allow the user to change the locale used in localization.
     *
     * @author Griefed
     */
    private fun changeLocale() {
        println("What locale would you like to use?")
        println("(Locale format is en_us, de_de, uk_ua etc.)")
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
                    api.apiProperties.i18n4kConfig.locale = Locale(userLocale)
                } catch (e: RuntimeException) {
                    println(
                        "Incorrect format. ServerPackCreator currently only supports locales in the format of en_us (Language, Country)."
                    )
                    userLocale = "en_us"
                }
            }
        } while (!userLocale.matches(regex))
        scanner.close()
        println("Using language: ${Api.localeName}")
    }
}