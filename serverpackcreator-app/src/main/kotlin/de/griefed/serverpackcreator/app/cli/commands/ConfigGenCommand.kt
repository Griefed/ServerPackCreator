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
package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File
import java.util.*

@CommandLine.Command(
    name = "cgen", mixinStandardHelpOptions = true,
    description = [
        "Generate a basic server pack config from a modpack-directory.",
        "If you wish to change the config afterwards, open it in your favourite text-editor and change it to your liking."
                  ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
/** Builds a configuration by inspecting a modpack directory, so a user need not write one by hand. */
class ConfigGenCommand(
    private val apiWrapper: ApiWrapper = ApiWrapper.api()
) : Command {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Ask for a modpack directory, then derive and store a configuration from it. */
    override fun run() {
        val modpackDirectory = requestModpackDir()
        generateConfFromModpack(Optional.of(modpackDirectory))
    }

    /** Prompt until the user gives a directory that exists. */
    fun requestModpackDir(): File {
        val scanner = Scanner(System.`in`)
        println("Enter the full path to the modpack-directory.")

        var path: String
        do {
            print("Path: ")
            path = scanner.nextLine()
            if (!File(path).isDirectory) {
                println("File '$path' does not exist.")
            }
        } while (!File(path).isDirectory)
        try {
            scanner.close()
        } catch (_: Exception) {
            // The scanner wraps System.in; a failure while closing it is harmless and must not
            // abort the command.
        }
        return File(path)
    }

    /** Derive a configuration from the given modpack and write it out; does nothing when none was given. */
    fun generateConfFromModpack(modpackDirectory: Optional<File>) {
        if (modpackDirectory.isPresent && modpackDirectory.get().isDirectory) {
            val packConfig = apiWrapper.configurationHandler.generateConfigFromModpack(modpackDirectory.get())
            val configFile = File(apiWrapper.apiProperties.configsDirectory, packConfig.name ?: modpackDirectory.get().name)
            packConfig.save(configFile, apiWrapper.apiProperties)
            apiWrapper.configurationHandler.printConfigurationModel(packConfig)
            log.info("Config for ${modpackDirectory.get().absolutePath} available at ${configFile.absolutePath}")
        } else {
            log.error("Modpack-directory $modpackDirectory doesn't exist. Config not generated.")
        }
    }
}