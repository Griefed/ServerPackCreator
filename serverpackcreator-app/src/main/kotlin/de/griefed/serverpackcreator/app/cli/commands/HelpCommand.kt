package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.utilities.common.readText
import de.griefed.serverpackcreator.app.Mode
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.IOException

@CommandLine.Command(
    name = "printHelp",
    description = ["Print a list of arguments to start ServerPackCreator with, as well as some general help."],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class HelpCommand : Command {

    override fun run() {
        printHelp()
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
                HelpCommand::class.java.getResourceAsStream("/de/griefed/resources/cli_help.txt")!!.readText()
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

}