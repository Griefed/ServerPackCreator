package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File
import java.util.*
import java.util.prefs.Preferences

@CommandLine.Command(
    name = "homeDir",
    description = [
        "Change the home-directory for ServerPackCreator.",
        "Changing the home-directory requires a restart of ServerPackCreator afterwards."
                  ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class HomeDirCommand : Command {
    override fun run() {
        changeHomeDirectory()
    }

    private fun changeHomeDirectory() {
        val scanner = Scanner(System.`in`)
        println("Enter the full path to the new ServerPackCreator home-directory.")
        if (SystemUtilities.IS_WINDOWS) {
            println("Don't forget to escape any \\ in your paths, so 'C:\\Some\\Path' becomes 'C:\\\\Some\\\\Path'.")
        }

        var path: String
        do {
            print("Path: ")
            path = scanner.nextLine()
            if (!File(path).isDirectory) {
                println("Directory '$path' does not exist.")
            }
        } while (!File(path).isDirectory)

        Preferences.userRoot().node("ServerPackCreator").put(
            "de.griefed.serverpackcreator.home",
            path
        )

        println("You MUST restart ServerPackCreator for this change to take full effect.")
    }
}