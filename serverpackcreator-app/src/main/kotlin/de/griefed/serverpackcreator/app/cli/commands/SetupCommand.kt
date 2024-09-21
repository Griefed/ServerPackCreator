package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen

@CommandLine.Command(
    name = "setup",
    description = [
        "Force-run the ServerPackCreator API setup, generating and providing all required working- and template-files and folders."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class SetupCommand(private val apiWrapper: ApiWrapper = ApiWrapper.api()) : Command {
    override fun run() {
        forceApiSetup()
    }

    private fun forceApiSetup() {
        apiWrapper.setup(force = true)
    }
}