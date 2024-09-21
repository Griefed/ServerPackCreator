package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.app.Mode
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.xml.sax.SAXException
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

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
    private fun runHeadless() {
        if (!apiWrapper.apiProperties.defaultConfig.exists()) {
            log.warn("No serverpackcreator.conf found...")
            log.info("If you want to run ServerPackCreator in CLI-mode, a serverpackcreator.conf is required.")
            log.info(
                "Either copy an existing config, or run ServerPackCreator with the '-cgen'-argument to generate one via commandline."
            )
        } else {
            val packConfig = PackConfig()
            val check = apiWrapper.configurationHandler.checkConfiguration(apiWrapper.apiProperties.defaultConfig,packConfig)
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