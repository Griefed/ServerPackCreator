package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import de.griefed.serverpackcreator.app.Mode
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.xml.sax.SAXException
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File
import java.io.IOException
import java.util.*
import javax.xml.parsers.ParserConfigurationException

@CommandLine.Command(
    name = "runWCustomConf",
    description = [
        "Run a config check and server pack generation using a specified server pack config.",
        "You will be asked to enter the path to the desired config after starting this command."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class RunHeadlessCustomConfigCommand(private val apiWrapper: ApiWrapper = ApiWrapper.api()): Command {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    override fun run() {
        val config = requestConfigFile()
        runHeadless(config)
    }

    private fun requestConfigFile(): File {
        val scanner = Scanner(System.`in`)
        println("Enter the full path to the new ServerPackCreator home-directory.")
        if (SystemUtilities.IS_WINDOWS) {
            println("Don't forget to escape any \\ in your paths, so 'C:\\Some\\Path' becomes 'C:\\\\Some\\\\Path'.")
        }

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

    /**
     * Run ServerPackCreator in [Mode.CLI]. Requires a `serverpackcreator.conf`-file to be present.
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
    private fun runHeadless(config: File) {
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