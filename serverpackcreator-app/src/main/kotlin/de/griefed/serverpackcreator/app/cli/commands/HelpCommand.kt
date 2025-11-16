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

import de.griefed.serverpackcreator.api.utilities.common.readText
import de.griefed.serverpackcreator.app.Mode
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.IOException

@CommandLine.Command(
    name = "printHelp", mixinStandardHelpOptions = true,
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