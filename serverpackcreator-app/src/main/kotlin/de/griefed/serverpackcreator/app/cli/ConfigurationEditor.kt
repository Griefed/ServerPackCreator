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
package de.griefed.serverpackcreator.app.cli

import Translations
import com.electronwill.nightconfig.core.file.NoFormatFoundException
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.BooleanUtilities
import de.griefed.serverpackcreator.api.utilities.common.ListUtilities
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.moveTo

/**
 * Create and edit a serverpackcreator.conf file via CLI. Allows loading of a custom file for
 * editing purposes and saving any config file under a different name.
 *
 * @author Griefed
 */
class ConfigurationEditor(
    private val configurationHandler: ConfigurationHandler,
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val current = dateTimeFormatter.format(LocalDateTime.now())
    private val logFile: File = File(apiProperties.logsDirectory, "configurationCreator-$current.log")

    /**
     * Present the user with a menu, asking them what they would like to do.
     *
     * @author Griefed
     */
    fun continuedRunOptions() {
        checkLogFile()
        val scanner = Scanner(System.`in`)
        var selection: Int
        do {
            printMenu()
            selection = getDecision(scanner, 0, 2)
            when (selection) {
                1 -> createConfigurationFile(scanner)
                2 -> loadAndEdit(scanner)
                0 -> cliLog(Translations.cli_exiting.toString())
            }
        } while (selection != 0)
        scanner.close()
    }

    /**
     * Print the text-menu so the user may decide what they would like to do next.
     *
     * @author Griefed
     */
    private fun printMenu() {
        cliLog()
        cliLog(Translations.cli_print_menu_00.toString())
        cliLog(Translations.cli_print_menu_01.toString())
        cliLog(Translations.cli_print_menu_02.toString())
        cliLog(Translations.cli_print_menu_03.toString())
        cliLog(Translations.cli_print_menu_04.toString())
        cliLog(Translations.cli_print_menu_05.toString(), false)
    }

    /**
     * Allow the user to load and edit an existing configuration.
     *
     * @param scanner Used for reading the users input.
     * @author Griefed
     */
    private fun loadAndEdit(scanner: Scanner) {
        var packConfig = PackConfig()
        var fileName: String
        var configLoaded = false
        do {
            try {
                cliLog(Translations.cli_config_load_edit_input.toString())
                fileName = getNextLine(scanner)
                packConfig = PackConfig(File(fileName))
                configLoaded = true
            } catch (e: FileNotFoundException) {
                cliLog(Translations.cli_config_load_edit_error(e.message.toString()))
            } catch (e: NoFormatFoundException) {
                cliLog(Translations.cli_config_load_edit_error(e.message.toString()))
            }
        } while (!configLoaded)
        cliLog(Translations.cli_config_load_edit_success.toString())
        cliLog()
        var selection: Int
        do {
            configurationHandler.printConfigurationModel(packConfig)
            printEditMenu()
            selection = getDecision(scanner, 0, 18)
            when (selection) {
                1 -> packConfig.modpackDir = getModpackDirectory(scanner)
                2 -> getClientSideMods(scanner, packConfig.clientMods)
                3 -> getDirsAndFilesToCopy(scanner, packConfig.modpackDir, packConfig.inclusions)
                4 -> packConfig.serverIconPath = getServerIcon(scanner)
                5 -> packConfig.serverPropertiesPath = getServerProperties(scanner)
                6 -> packConfig.minecraftVersion = getMinecraftVersion(scanner)
                7 -> packConfig.modloader = getModloader(scanner)
                8 -> packConfig.modloaderVersion = getModloaderVersion(scanner, packConfig.minecraftVersion, packConfig.modloader)
                9 -> packConfig.isServerIconInclusionDesired = includeServerIcon()
                10 -> packConfig.isServerPropertiesInclusionDesired = includeServerProperties()
                11 -> packConfig.isZipCreationDesired = includeZipCreation()
                12 -> packConfig.javaArgs = getJavaArgs(scanner)
                13 -> packConfig.serverPackSuffix = getServerPackSuffix(scanner)
                14 -> saveConfiguration(scanner, packConfig)
                15 -> configurationHandler.printConfigurationModel(packConfig)
                16 -> checkConfig(packConfig)
                0 -> cliLog(Translations.cli_exiting.toString())
            }
        } while (selection != 0)
    }

    /**
     * Check the configuration model for errors. If any errors are encountered, they are written to
     * the console as well as the config-editor log.
     *
     * @param packConfig The ConfigurationModel to check.
     * @author Griefed
     */
    private fun checkConfig(packConfig: PackConfig) {
        val check = ConfigCheck()
        configurationHandler.checkConfiguration(packConfig, check, false)
        if (check.encounteredErrors.isNotEmpty()) {
            cliLog(Translations.cli_config_check_error.toString())
            for (i in check.encounteredErrors.indices) {
                cliLog("  ($i): ${check.encounteredErrors[i]}")
            }
        } else {
            cliLog(Translations.cli_config_check_success.toString())
        }
    }

    /**
     * Print the text-menu so the user may decide what they would like to do next.
     *
     * @author Griefed
     */
    private fun printEditMenu() {
        cliLog()
        cliLog(Translations.cli_print_menu_edit_00.toString()) //What would you like to edit?
        cliLog(Translations.cli_print_menu_edit_01.toString()) //(1)  : Path to the modpack directory
        cliLog(Translations.cli_print_menu_edit_02.toString()) //(2)  : List of clientside-only mods
        cliLog(Translations.cli_print_menu_edit_03.toString()) //(3)  : List of files and/or folders to include in the server pack
        cliLog(Translations.cli_print_menu_edit_04.toString()) //(4)  : Path to a custom server-icon.png
        cliLog(Translations.cli_print_menu_edit_05.toString()) //(5)  : Path to a custom server.properties
        cliLog(Translations.cli_print_menu_edit_06.toString()) //(6)  : Minecraft version
        cliLog(Translations.cli_print_menu_edit_07.toString()) //(7)  : Modloader
        cliLog(Translations.cli_print_menu_edit_08.toString()) //(8)  : Modloader version
        cliLog(Translations.cli_print_menu_edit_09.toString()) //(9)  : Whether to include a server-icon.png in the server pack - Only relevant if you set (4) to a valid path
        cliLog(Translations.cli_print_menu_edit_10.toString()) //(10) : Whether to include a server.properties in the server pack - Only relevant if you set (5) to a valid path
        cliLog(Translations.cli_print_menu_edit_11.toString()) //(11) : Whether to create a ZIP-archive of the generated server pack
        cliLog(Translations.cli_print_menu_edit_12.toString()) //(12) : JVM flags / Java Args to run the server of the generated server pack with - These will be used by the start scripts
        cliLog(Translations.cli_print_menu_edit_13.toString()) //(13) : Server pack suffix
        cliLog(Translations.cli_print_menu_edit_14.toString()) //(14) : Save config to file
        cliLog(Translations.cli_print_menu_edit_15.toString()) //(15) : Print current config
        cliLog(Translations.cli_print_menu_edit_16.toString()) //(16) : Check configuration
        cliLog(Translations.cli_print_menu_edit_17.toString()) //(0)  : Exit
        cliLog(Translations.cli_print_menu_edit_18.toString()) //---------------------------------------------------------------------------------------------------------------------------
        cliLog(Translations.cli_print_menu_edit_19.toString(), false) // Enter the number of your selection:
    }

    /**
     * Walk the user through the generation of a new ServerPackCreator configuration file by asking
     * them for input, step-by-step, regarding their modpack. At the end of this method a fully
     * configured serverpackcreator.conf file is saved and any previously existing configuration file
     * replaced by the new one.
     *
     * After every input, said input is displayed to the user, and
     * they're asked whether they are satisfied with said input. The user can then decide whether they
     * would like to restart the entry of the field they just configured, or agree and move to the
     * next one.
     *
     * At the end of this method, the user will have a newly configured and created
     * configuration file for ServerPackCreator.
     *
     * Most user-input is checked after entry to ensure the configuration is already in
     * working-condition after completion of this method.
     *
     * @param scanner Used for reading the users input.
     * @author whitebear60
     * @author Griefed
     */
    private fun createConfigurationFile(scanner: Scanner) {
        val packConfig = PackConfig()
        do {
            // -----------------------------------------------------------------MODPACK DIRECTORY---------
            packConfig.modpackDir = getModpackDirectory(scanner)

            // --------------------------------------------------------------CLIENTSIDE-ONLY MODS---------
            getClientSideMods(scanner, packConfig.clientMods)

            // ---------------------------------------DIRECTORIES OR FILES TO COPY TO SERVER PACK---------
            getDirsAndFilesToCopy(
                scanner, packConfig.modpackDir,
                packConfig.inclusions
            )

            // ------------------------------------------------PATH TO THE CUSTOM SERVER-ICON.PNG---------
            packConfig.serverIconPath = getServerIcon(scanner)

            // -----------------------------------------------PATH TO THE CUSTOM SERVER.PROPERTIES--------
            packConfig.serverPropertiesPath = getServerProperties(scanner)

            // ----------------------------------------------------MINECRAFT VERSION MODPACK USES---------
            packConfig.minecraftVersion = getMinecraftVersion(scanner)

            // ------------------------------------------------------------MODLOADER MODPACK USES---------
            packConfig.modloader = getModloader(scanner)

            // -------------------------------------------------VERSION OF MODLOADER MODPACK USES---------
            packConfig.modloaderVersion = getModloaderVersion(
                scanner, packConfig.minecraftVersion,
                packConfig.modloader
            )


            // ---------------------------------WHETHER TO INCLUDE SERVER-ICON.PNG IN SERVER PACK---------
            if (File(packConfig.serverIconPath).isFile) {
                packConfig.isServerIconInclusionDesired = includeServerIcon()
            } else {
                cliLog(Translations.cli_create_no_icon.toString())
            }

            // -------------------------------WHETHER TO INCLUDE SERVER.PROPERTIES IN SERVER PACK---------
            if (File(packConfig.serverPropertiesPath).isFile) {
                packConfig.isServerPropertiesInclusionDesired = includeServerProperties()
            } else {
                cliLog(Translations.cli_create_no_properties.toString())
            }

            // -------------------------WHETHER TO INCLUDE CREATION OF ZIP-ARCHIVE OF SERVER PACK---------
            packConfig.isZipCreationDesired = includeZipCreation()

            // ----------------------------------------------JAVA ARGS TO EXECUTE THE SERVER WITH---------
            packConfig.javaArgs = getJavaArgs(scanner)

            // ---------------------------------------------------SUFFIX TO APPEND TO SERVER PACK---------
            packConfig.serverPackSuffix = getServerPackSuffix(scanner)

            // ---------------------------------------------------PRINT CONFIG TO CONSOLE AND LOG---------
            configurationHandler.printConfigurationModel(packConfig)
            cliLog(Translations.cli_create_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())

        // ----------------------------------------------------------------CHECK CONFIGURATION----------
        cliLog(Translations.cli_create_check.toString())
        cliLog(Translations.cli_answer.toString(), false)
        if (BooleanUtilities.readBoolean()) {
            checkConfig(packConfig)
        }

        // ----------------------------------------------------------------WRITE CONFIG TO FILE---------
        saveConfiguration(scanner, packConfig)
    }

    /**
     * Acquire the path to the modpack directory from the user.
     *
     * @param scanner Used for reading the users input.
     * @return The path to the modpack directory.
     * @author Griefed
     */
    private fun getModpackDirectory(scanner: Scanner): String {
        var modpackDir: String
        cliLog(Translations.cli_modpackdir_intro.toString())
        cliLog(Translations.cli_modpackdir_example.toString())
        do {
            do {
                cliLog(Translations.cli_modpackdir_input.toString(), false)
                modpackDir = getNextLine(scanner)
            } while (!configurationHandler.checkModpackDir(modpackDir).modpackChecksPassed)
            cliLog(Translations.cli_answer_input(modpackDir))
            cliLog(Translations.cli_modpackdir_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())
        cliLog(Translations.cli_answer_input(modpackDir))
        cliLog()
        return modpackDir
    }

    /**
     * Acquire a list of clientside-only modslist from the user.
     *
     * @param clientMods List of clientside-only mods to either overwrite or edit.
     * @author Griefed
     */
    private fun getClientSideMods(
        scanner: Scanner,
        clientMods: MutableList<String>
    ) {
        var selection = 2
        do {
            if (clientMods.isNotEmpty()) {
                cliLog(Translations.cli_clientsides_entries.toString())
                cliLog(Translations.cli_list_over.toString())
                selection = getDecision(scanner, 1, 2)
            }
            when (selection) {
                1 -> editList(scanner, clientMods)
                2 -> {
                    clientMods.clear()
                    clientMods.addAll(newClientModsList())
                }
            }
            cliLog(Translations.cli_list_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())
        cliLog(Translations.cli_list_yours.toString())
        ListUtilities.printListToConsoleChunked(clientMods, 5, "    ", false)
        cliLog()
    }

    /**
     * Get a decision from the user, between and including the min and max values specified.
     *
     * @param scanner Used for reading the users input.
     * @param min     The minimum value allowed to pick.
     * @param max     The maximum value allowed to pick.
     * @return The users decision.
     * @author Griefed
     */
    private fun getDecision(
        scanner: Scanner,
        min: Int,
        max: Int
    ): Int {
        var selection: Int
        do {
            selection = try {
                getNextLine(scanner).toInt()
            } catch (ex: Exception) {
                cliLog(Translations.cli_decision(min, max))
                min - 1
            }
        } while (selection in (max + 1) until min)
        return selection
    }

    /**
     * Edit entries in a list.
     *
     * @param scanner Used for reading the users input.
     * @param list    The list in which to edit its entries.
     * @author Griefed
     */
    private fun editList(
        scanner: Scanner,
        list: MutableList<String>
    ) {
        cliLog(Translations.cli_list_entries.toString())
        for (i in list.indices) {
            cliLog("($i) : ${list[i]}")
        }
        val max = list.size - 1
        do {
            cliLog(Translations.cli_list_which.toString())
            cliLog(Translations.cli_decision(0, max))
            val selection = getDecision(scanner, 0, max)
            cliLog(Translations.cli_list_selection(selection, max, list[selection]))
            cliLog(Translations.cli_list_delete.toString())
            when (getDecision(scanner, 1, 2)) {
                1 -> {
                    cliLog(Translations.cli_list_input.toString(), false)
                    list[selection] = getNextLine(scanner)
                }

                2 -> cliLog(Translations.cli_list_deleted(list.removeAt(selection)))
            }
            for (i in list.indices) {
                cliLog("($i) : ${list[i]}")
            }
            cliLog(Translations.cli_list_separator.toString())
            cliLog(Translations.cli_list_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())
        ListUtilities.printListToConsoleChunked(list, 5, "    ", false)
        cliLog(Translations.cli_list_success.toString())
    }

    /**
     * Create a new list of clientside-only mods.
     *
     * @return A list of clientside-only mods, as per user input.
     * @author Griefed
     */
    private fun newClientModsList(): List<String> {
        cliLog(Translations.cli_clientsides_new_intro.toString())
        cliLog(Translations.cli_clientsides_new_info.toString())
        var clientMods = newCustomList()
        if (clientMods.isEmpty()) {
            clientMods = apiProperties.clientSideMods()
            cliLog(Translations.cli_clientsides_new_fallback.toString())
            for (mod in clientMods) {
                cliLog("    $mod")
            }
        }
        return clientMods
    }

    /**
     * Create a new list filled with user inputs.
     *
     * @return A list of clientside-only mods, as per user input.
     * @author Griefed
     */
    private fun newCustomList(): List<String> {
        val custom: List<String> = ListUtilities.readStringList()
        cliLog(Translations.cli_answer_input_newline.toString())
        for (i in custom.indices) {
            cliLog("  ${i + 1}. ${custom[i]}")
        }
        return custom
    }

    /**
     * Create a new inclusions-list filled with user inputs.
     *
     * @return A list of clientside-only mods, as per user input.
     * @author Griefed
     */
    private fun newCustomInclusionsList(): List<InclusionSpecification> {
        val inclusions = mutableListOf<InclusionSpecification>()
        cliLog(Translations.cli_copyfiles_entries_custom_intro.toString())
        cliLog(Translations.cli_copyfiles_entries_custom_intro2.toString())
        cliLog(Translations.cli_copyfiles_entries_custom_intro3.toString())
        val custom: List<String> = ListUtilities.readStringList()
        var split: List<String>
        cliLog(Translations.cli_answer_input_newline.toString())
        for (i in custom.indices) {
            split = custom[i].split(",")
            inclusions.add(InclusionSpecification(split[0],split[1],split[2],split[3]))
        }
        logInclusionSpecs(inclusions)
        return inclusions
    }

    /**
     * Acquire a list of files and directories to include in the server pack from the user.
     *
     * @param modpackDir The path to the modpack directory.
     * @author Griefed
     */
    private fun getDirsAndFilesToCopy(
        scanner: Scanner,
        modpackDir: String,
        inclusions: MutableList<InclusionSpecification>
    ) {
        cliLog(Translations.cli_copyfiles_intro.toString())
        listModpackFilesAndFolders(modpackDir)
        var selection = 2
        do {
            if (inclusions.isNotEmpty()) {
                cliLog(Translations.cli_copyfiles_entries.toString())
                cliLog()
                logInclusionSpecs(inclusions)
                cliLog()
                cliLog(Translations.cli_copyfiles_over.toString())
                selection = getDecision(scanner, 1, 3)
            }
            when (selection) {
                1 -> editInclusions(scanner, inclusions)
                2 -> {
                    inclusions.clear()
                    inclusions.addAll(newCustomInclusionsList())
                }

                3 -> listModpackFilesAndFolders(modpackDir)
            }
            cliLog(Translations.cli_list_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())
        cliLog(Translations.cli_list_yours.toString())
        logInclusionSpecs(inclusions)
        cliLog()
    }

    private fun logInclusionSpecs(inclusions: List<InclusionSpecification>) {
        for (i in inclusions.indices) {
            cliLog("${i + 1}. ${Translations.cli_copyfiles_entries_prefix_source}: ${inclusions[i].source}")
            cliLog("${i + 1}. ${Translations.cli_copyfiles_entries_prefix_destination}: ${inclusions[i].destination}")
            cliLog("${i + 1}. ${Translations.cli_copyfiles_entries_prefix_inclusion}: ${inclusions[i].inclusionFilter}")
            cliLog("${i + 1}. ${Translations.cli_copyfiles_entries_prefix_exclusion}: ${inclusions[i].exclusionFilter}")
            cliLog("")
        }
    }

    private fun editInclusions(scanner: Scanner, inclusions: MutableList<InclusionSpecification>) {
        cliLog(Translations.cli_list_entries.toString())
        logInclusionSpecs(inclusions)
        val max = inclusions.size - 1
        do {
            cliLog(Translations.cli_list_which.toString())
            cliLog(Translations.cli_decision(0, max))
            val selection = getDecision(scanner, 0, max)
            cliLog(Translations.cli_list_selection(selection, max, inclusions[selection]))
            cliLog(Translations.cli_list_delete.toString())
            when (getDecision(scanner, 1, 2)) {
                1 -> {
                    cliLog(Translations.cli_list_input.toString(), false)
                    val inclusion = getNextLine(scanner).split(",")
                    inclusions[selection] =
                        InclusionSpecification(inclusion[0], inclusion[1], inclusion[2], inclusion[3])
                }

                2 -> cliLog(Translations.cli_list_deleted(inclusions.removeAt(selection)))
            }
            for (i in inclusions.indices) {
                cliLog("($i) : ${inclusions[i]}")
            }
            cliLog(Translations.cli_list_separator.toString())
            cliLog(Translations.cli_list_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())
        logInclusionSpecs(inclusions)
        cliLog(Translations.cli_list_success.toString())
    }

    /**
     * List all files and folders in the provided modpack-directory.
     *
     * @param modpackDir Path to the modpack-directory of which to list the containing files and
     * folders.
     * @author Griefed
     */
    private fun listModpackFilesAndFolders(modpackDir: String) {
        try {
            val dirList: List<File> = File(modpackDir).listFiles()?.toList() ?: listOf(File(""))
            if (dirList.isNotEmpty()) {
                cliLog(Translations.cli_copyfiles_modpack_list.toString())
                for (i in dirList.indices) {
                    cliLog("  ${i + 1}. ${dirList[i]}")
                }
            } else {
                cliLog(Translations.cli_copyfiles_modpack_empty(modpackDir))
            }
        } catch (ignored: Exception) {
        }
    }

    /**
     * Acquire the path to the server-icon to include in the server pack from the user.
     *
     * @param scanner Used for reading the users input.
     * @return The path to the server-icon to include in the server pack, as per the users input.
     * @author Griefed
     */
    private fun getServerIcon(scanner: Scanner): String {
        var serverIconPath: String
        cliLog(Translations.cli_icon_intro.toString())
        do {
            do {
                cliLog(Translations.cli_icon_input.toString(), false)
                serverIconPath = getNextLine(scanner)
            } while (!configurationHandler.checkIconAndProperties(serverIconPath))
            cliLog(Translations.cli_answer_input(serverIconPath))
            cliLog(Translations.cli_setting_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())
        cliLog(Translations.cli_answer_input(serverIconPath))
        cliLog()
        return serverIconPath
    }

    /**
     * Acquire the path to the server-properties to include in the server pack from the user.
     *
     * @param scanner Used for reading the users input.
     * @return The path to the server-properties to include in the server pack, as per the users
     * input.
     * @author Griefed
     */
    private fun getServerProperties(scanner: Scanner): String {
        var serverPropertiesPath: String
        cliLog(Translations.cli_properties_intro.toString())
        do {
            do {
                cliLog(Translations.cli_properties_input.toString(), false)
                serverPropertiesPath = getNextLine(scanner)
            } while (!configurationHandler.checkIconAndProperties(serverPropertiesPath))
            cliLog(Translations.cli_answer_input(serverPropertiesPath))
            cliLog(Translations.cli_setting_satisfied.toString())
            cliLog(Translations.cli_answer.toString(), false)
        } while (!BooleanUtilities.readBoolean())
        cliLog(Translations.cli_answer_input(serverPropertiesPath))
        cliLog()
        return serverPropertiesPath
    }

    /**
     * Get the users modpacks Minecraft version.
     *
     * @param scanner Used for reading the users input.
     * @return The Minecraft version the users modpack uses, as per the users input.
     * @author Griefed
     */
    private fun getMinecraftVersion(scanner: Scanner): String {
        var minecraftVersion: String
        cliLog(Translations.cli_minecraft_intro.toString())
        do {
            cliLog(Translations.cli_minecraft_input.toString(), false)
            minecraftVersion = getNextLine(scanner)
        } while (!versionMeta.minecraft.isMinecraftVersionAvailable(minecraftVersion))
        cliLog(Translations.cli_answer_input(minecraftVersion))
        cliLog()
        return minecraftVersion
    }

    /**
     * Get the users modpacks modloader.
     *
     * @param scanner Used for reading the users input.
     * @return The modloader the users modpack uses, as per the users input.
     * @author Griefed
     */
    private fun getModloader(scanner: Scanner): String {
        var modLoader: String
        cliLog(Translations.cli_modloader_intro.toString())
        do {
            cliLog(Translations.cli_modloader_modloader.toString(), false)
            modLoader = getNextLine(scanner)
        } while (!configurationHandler.checkModloader(modLoader).modloaderChecksPassed)
        modLoader = configurationHandler.getModLoaderCase(modLoader)
        cliLog(Translations.cli_answer_input(modLoader))
        cliLog()
        return modLoader
    }

    /**
     * Get the users modpack modloader version.
     *
     * @param scanner   Used for reading the users input.
     * @param modLoader The modloader the users modpack uses.
     * @return The modloader version the users modpack uses, as per the users input.
     * @author Griefed
     */
    private fun getModloaderVersion(
        scanner: Scanner,
        minecraftVersion: String,
        modLoader: String
    ): String {
        var modLoaderVersion: String
        cliLog(Translations.cli_modloader_version_intro(modLoader))
        do {
            cliLog(Translations.cli_modloader_version_version.toString(), false)
            modLoaderVersion = getNextLine(scanner)
        } while (!configurationHandler.checkModloaderVersion(
                modLoader, modLoaderVersion, minecraftVersion
            ).modloaderChecksPassed
        )
        cliLog("You entered: $modLoaderVersion")
        cliLog()
        return modLoaderVersion
    }

    /**
     * Get the users decision on whether they want to include the server-icon.
     *
     * @return `true` if the user wants the server-icon to be included.
     * @author Griefed
     */
    private fun includeServerIcon(): Boolean {
        cliLog(Translations.cli_icon_include_intro.toString())
        cliLog(Translations.cli_icon_include_input.toString(), false)
        val includeServerIcon: Boolean = BooleanUtilities.readBoolean()
        cliLog(Translations.cli_answer_input(includeServerIcon))
        cliLog()
        return includeServerIcon
    }

    /**
     * Get the users decision on whether they want to include the server-properties.
     *
     * @return `true` if the user wants the server-properties to be included.
     * @author Griefed
     */
    private fun includeServerProperties(): Boolean {
        cliLog(Translations.cli_properties_include_intro.toString())
        cliLog(Translations.cli_properties_include_input.toString(), false)
        val includeServerProperties: Boolean = BooleanUtilities.readBoolean()
        cliLog(Translations.cli_answer_input(includeServerProperties))
        cliLog()
        return includeServerProperties
    }

    /**
     * Get the users decision on whether they want to include the ZIP-archive creation of the server
     * pack.
     *
     * @return `true` if the user wants a ZIP-archive of the server pack to be created.
     * @author Griefed
     */
    private fun includeZipCreation(): Boolean {
        cliLog(Translations.cli_zip_include_intro.toString())
        cliLog(Translations.cli_zip_include_input.toString(), false)
        val includeZipCreation: Boolean = BooleanUtilities.readBoolean()
        cliLog(Translations.cli_answer_input(includeZipCreation))
        cliLog()
        return includeZipCreation
    }

    /**
     * Get the Java args to be used when starting the server pack.
     *
     * @param scanner Used for reading the users input.
     * @return The Java args to be used when starting the server pack, as per the users input.
     * @author Griefed
     */
    private fun getJavaArgs(scanner: Scanner): String {
        var javaArgs: String
        cliLog(Translations.cli_java_args_intro.toString())
        cliLog(Translations.cli_java_args_input.toString(), false)
        javaArgs = getNextLine(scanner)
        if (javaArgs.isEmpty()) {
            javaArgs = ""
        }
        cliLog(Translations.cli_java_args(javaArgs))
        cliLog()
        return javaArgs
    }

    /**
     * Get the server pack suffix to append to the server pack.
     *
     * @param scanner Used for reading the users input.
     * @return The server pack suffix to append to the server pack, as per the users input.
     * @author Griefed
     */
    private fun getServerPackSuffix(scanner: Scanner): String {
        cliLog(Translations.cli_suffix_intro.toString())
        cliLog(Translations.cli_suffix_input.toString(), false)
        return getNextLine(scanner)
    }

    /**
     * Let the user save the created configuration to a file.
     *
     * @param scanner            Used for reading the users input.
     * @param packConfig Configuration to save.
     * @author Griefed
     */
    private fun saveConfiguration(
        scanner: Scanner,
        packConfig: PackConfig
    ) {
        cliLog(Translations.cli_config_save_intro.toString())
        if (BooleanUtilities.readBoolean()) {
            cliLog(Translations.cli_config_save_name.toString())
            val customFileName = File(StringUtilities.pathSecureText(getNextLine(scanner))).absoluteFile
            packConfig.save(customFileName)
            cliLog(Translations.cli_config_save_saved(customFileName.absolutePath))
            cliLog(Translations.cli_config_save_info.toString())
            cliLog(Translations.cli_config_save_load(customFileName))
        } else {
            packConfig.save(apiProperties.defaultConfig)
            cliLog(Translations.cli_config_save_saved_default.toString())
        }
    }

    /**
     * Acquire user input and print that input to our config-editor log.
     *
     * @param scanner Used for reading the users input.
     * @return The text the user entered.
     */
    private fun getNextLine(scanner: Scanner): String {
        val text = scanner.nextLine()
        printToFile(text, true)
        return text
    }

    /**
     * Write/print an empty line to a file as well as console. The filename is
     * `logs/configurationCreator-CURRENT_DATE_CURRENT_TIME.log`.
     *
     * @author Griefed
     */
    private fun cliLog(text: String = "") {
        cliLog(text, true)
    }

    /**
     * Write/print text to a file as well as console. The filename is
     * `logs/configurationCreator-CURRENT_DATE_CURRENT_TIME.log`.
     *
     * @param text The text to write/print
     * @author Griefed
     */
    private fun cliLog(
        text: String,
        newLine: Boolean
    ) {
        if (newLine) {
            println(text)
        } else {
            print(text)
        }
        printToFile(text, newLine)
    }

    /**
     * Append text to our config-editor log.
     *
     * @param text    The text to append to the log.
     * @param newLine Whether to include a newline after the text.
     */
    private fun printToFile(
        text: String,
        newLine: Boolean
    ) {
        if (logFile.exists()) {
            try {
                if (newLine) {
                    logFile.appendText(text + "\n")
                } else {
                    logFile.appendText(text)
                }
            } catch (ex: IOException) {
                log.error("Could not write to logfile ${logFile.name}", ex)
            }
        } else {
            log.error("Logfile ${logFile.name} does not exist.")
        }
    }

    private fun checkLogFile() {
        val files: List<File> = apiProperties.logsDirectory.listFiles()?.toList() ?: listOf(File(""))
        for (file in files) {
            if (file.name.contains("configurationCreator")) {
                try {
                    file.toPath()
                        .moveTo(File(apiProperties.logsDirectory, "archive${File.separator}${file.name}").toPath())
                } catch (ex: IOException) {
                    log.error("Could not move ${file.name} to archive.", ex)
                }
            }
        }
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (ex: IOException) {
                log.error("Could not create logfile ${logFile.name}", ex)
            }
        }
    }
}