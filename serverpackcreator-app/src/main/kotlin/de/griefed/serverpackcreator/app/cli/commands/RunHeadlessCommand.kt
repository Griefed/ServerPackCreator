package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
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
    name = "run",
    description = [
        "Run a config check and server pack generation using the default server pack config.",
        "The default config file is expected at '</path/to/ServerPackCreator-home-directory/serverpackcreator.conf'."
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
    fun withSpecificConfig() {
        val config = requestConfigFile()
        runHeadless(config)
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
        return File(path)
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun runHeadless(config: File = apiWrapper.apiProperties.defaultConfig) {
        if (!config.isFile) {
            log.warn("${config.absolutePath} not found...")
        } else {
            val packConfig = PackConfig()
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