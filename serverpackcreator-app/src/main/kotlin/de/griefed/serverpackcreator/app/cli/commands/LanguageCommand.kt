package de.griefed.serverpackcreator.app.cli.commands

import Translations
import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiWrapper
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.util.*

@CommandLine.Command(
    name = "lang",
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
    }
}