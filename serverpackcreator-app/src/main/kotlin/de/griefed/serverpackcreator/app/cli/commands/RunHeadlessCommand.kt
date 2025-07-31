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

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.xml.sax.SAXException
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File
import java.io.IOException
import java.util.*
import javax.xml.parsers.ParserConfigurationException

@Suppress("DuplicatedCode")
@CommandLine.Command(
    name = "run", mixinStandardHelpOptions = true,
    description = [
        "Run a config check and server pack generation using the default server pack config.",
        "The default config file is expected at '</path/to/ServerPackCreator-home-directory/serverpackcreator.conf'.",
        "Windows Users: You can use / instead of \\ in your paths, too."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class RunHeadlessCommand(private val apiWrapper: ApiWrapper = ApiWrapper.api()) : Command {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    override fun run() {
        runHeadless()
    }

    @CommandLine.Command(
        mixinStandardHelpOptions = true, subcommands = [CommandLine.HelpCommand::class],
        description = [
            "Run a config check and server pack generation using a specified server pack config.",
            "You will be asked to enter the path to the desired config after starting this command."
        ]
    )
    @Suppress("unused")
    fun withSpecificConfig(
        @CommandLine.Option(
            names = ["-c", "--config"],
            description = [
                "The path to the config file.",
                "Windows Users: You can use / instead of \\ in your paths, too."
            ],
            required = false
        ) configFile: String?,
        @CommandLine.Option(
            names = ["-d", "--destination"],
            description = [
                "A destination in which the server pack will be created in.",
                "All folders, including parent folders, will be created in the process.",
                "Windows Users: You can use / instead of \\ in your paths, too."
            ],
            required = false
        ) destination: String?
    ) {
        val config = if (configFile == null) {
            requestConfigFile()
        } else {
            File(configFile)
        }
        runHeadless(config, Optional.ofNullable(destination?.let { File(it) }))
    }

    @CommandLine.Command(
        mixinStandardHelpOptions = true, subcommands = [CommandLine.HelpCommand::class],
        description = [
            "Run server pack generations for all configs in the config-directory.",
            "The config-directory is inside ServerPackCreators home-directory."
        ]
    )
    fun withAllInConfigDir() {
        val configs = apiWrapper.apiProperties.configsDirectory.listFiles()
        for (config in configs) {
            runHeadless(config)
        }
    }

    private fun requestConfigFile(): File {
        val scanner = Scanner(System.`in`)
        println("Enter the full path to the new ServerPackCreator home-directory.")

        var path: String
        do {
            print("Path: ")
            path = scanner.nextLine()
            if (!File(path).isFile) {
                println("File '$path' does not exist.")
            }
        } while (!File(path).isFile)
        try {
            scanner.close()
        } catch (_: Exception) {}
        return File(path)
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun runHeadless(
        config: File = apiWrapper.apiProperties.defaultConfig,
        destination: Optional<File> = Optional.empty()
    ) {
        if (!config.isFile) {
            log.warn("${config.absolutePath} not found...")
        } else {
            val packConfig = PackConfig()
            packConfig.customDestination = destination
            val check = apiWrapper.configurationHandler.checkConfiguration(config, packConfig)
            if (!check.allChecksPassed) {
                println("Encountered the following errors/problems with the config:")
                for (error in check.encounteredErrors) {
                    println(error)
                }
            } else {
                val generation = apiWrapper.serverPackHandler.run(packConfig)
                if (!generation.success) {
                    println("Error generating Server Pack:")
                    for (error in generation.errors) {
                        println(error)
                    }
                } else {
                    println("Successfully generated Server Pack: ${generation.serverPack.absolutePath}")
                }
            }
        }
    }
}