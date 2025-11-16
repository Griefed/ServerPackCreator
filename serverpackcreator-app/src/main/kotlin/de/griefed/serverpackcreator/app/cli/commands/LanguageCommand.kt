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

import Translations
import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiWrapper
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.util.*

@CommandLine.Command(
    name = "lang", mixinStandardHelpOptions = true,
    description = [
        "Change the language to use for ServerPackCreator.",
        "Mostly affects the GUI of ServerPackCreator, but the CLI-mode.",
        "A restart of ServerPackCreator is recommended after changing the language."
                  ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class LanguageCommand(private val apiWrapper: ApiWrapper = ApiWrapper.api()) : Command {
    override fun run() {
        printAvailableLanguages()
        chooseAndSwitchLanguage()
    }

    private fun printAvailableLanguages() {
        for (locale in Translations.locales) {
            println(locale)
        }
    }

    private fun chooseAndSwitchLanguage() {
        val scanner = Scanner(System.`in`)
        println("Choose one of the available languages above.")

        var userLocale: String
        do {
            print("Language: ")
            userLocale = scanner.next()
            if (!Translations.locales.map { entry -> entry.language }.contains(userLocale)) {
                println("Unsupported locale $userLocale.")
            }
        } while (!Translations.locales.map { entry -> entry.language }.contains(userLocale))
        val lang = scanner.nextLine()
        apiWrapper.apiProperties.changeLocale(Locale(lang))
        try {
            scanner.close()
        } catch (_: Exception) {}
    }
}