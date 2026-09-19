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

import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import de.griefed.serverpackcreator.app.HomeDirectoryPreference
import de.griefed.serverpackcreator.app.cli.ConsolePrompt
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen

@CommandLine.Command(
    name = "homeDir", mixinStandardHelpOptions = true,
    description = [
        "Change the home-directory for ServerPackCreator.",
        "Changing the home-directory requires a restart of ServerPackCreator afterwards."
                  ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
/** Prints where SPC's home directory resolved to, which is the first thing to check when files turn up somewhere unexpected. */
class HomeDirCommand(private val prompt: ConsolePrompt = ConsolePrompt()) : Command {
    /** Print the resolved home directory. */
    override fun run() {
        changeHomeDirectory()
    }

    private fun changeHomeDirectory() {
        val question = buildString {
            append("Enter the full path to the new ServerPackCreator home-directory.")
            if (SystemUtilities.IS_WINDOWS) {
                append(System.lineSeparator())
                append("Don't forget to escape any \\ in your paths, so 'C:\\Some\\Path' becomes 'C:\\\\Some\\\\Path'.")
            }
        }

        HomeDirectoryPreference.store(prompt.readExistingDirectory(question).path)

        println("You MUST restart ServerPackCreator for this change to take full effect.")
    }
}