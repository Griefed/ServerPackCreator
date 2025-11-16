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

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen

@CommandLine.Command(
    name = "update", mixinStandardHelpOptions = true,
    description = [
        "Check for available updates.",
        "If you installed ServerPackCreator using the official installers, and if an update is available, the update will be installed automatically."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class UpdateCommand(private val updateChecker: UpdateChecker = UpdateChecker(ApiWrapper.api().apiProperties)) : Command {

    override fun run() {
        checkAndRunUpdate()
    }

    private fun checkAndRunUpdate() {
        updateChecker.updateCheck(true)
    }
}