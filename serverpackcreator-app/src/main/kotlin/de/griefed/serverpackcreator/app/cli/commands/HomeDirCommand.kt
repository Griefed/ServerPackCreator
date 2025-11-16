/* Copyright (C) 2025 Griefed
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

import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File
import java.util.*
import java.util.prefs.Preferences

@Suppress("DuplicatedCode")
@CommandLine.Command(
    name = "homeDir", mixinStandardHelpOptions = true,
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
        try {
            scanner.close()
        } catch (_: Exception) {}
    }
}