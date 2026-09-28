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

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.cli.ConsolePrompt
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen

@CommandLine.Command(
    name = "lang", mixinStandardHelpOptions = true,
    description = [
        "Change the language to use for ServerPackCreator.",
        "Mostly affects the GUI of ServerPackCreator, but the CLI-mode.",
        "A restart of ServerPackCreator is recommended after changing the language."
                  ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
/** Changes the configured locale, which every message SPC prints is read from. */
class LanguageCommand(
    private val apiWrapper: ApiWrapper = ApiWrapper.api(),
    private val prompt: ConsolePrompt = ConsolePrompt()
) : Command {
    /** Prompt for a locale and store it. */
    override fun run() {
        chooseAndSwitchLanguage()
    }

    /**
     * Offer every shipped locale under the name it is displayed by, and store whichever is chosen.
     *
     * The map is keyed by the locale's own string form, so the value offered and the value matched
     * are one and the same -- which is what stops the command listing locales it will not accept.
     */
    private fun chooseAndSwitchLanguage() {
        val available = Translations.locales.associateBy { locale -> locale.toString() }
        val chosen = prompt.readChoice("Choose one of the available languages above.", "Language: ", available)
        apiWrapper.apiProperties.changeLocale(chosen)
    }
}
