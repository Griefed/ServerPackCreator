package de.griefed.serverpackcreator.app.cli.commands

import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ApiWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Pins the contract `lang` has with the user about locale strings: whatever it lists is what it
 * accepts, and what it accepts is what it stores.
 *
 * Both halves were broken. `printAvailableLanguages` printed the locale (`en_GB`) while the loop
 * compared against `locale.language` (`en`), so the command refused every value it offered. And
 * having validated `scanner.next()`, it passed `scanner.nextLine()` -- the remainder of that same
 * line, which is the empty string -- to `changeLocale`, so even an accepted answer stored the wrong
 * locale.
 */
internal class LanguageCommandTest {

    /** What the command was asked for and what it stored, from one run. */
    private data class Interaction(val shown: String, val stored: Locale?)

    /**
     * Run `lang` answering [answers], and report what it printed and which locale it stored.
     *
     * Input exhaustion is swallowed: the command loops until an answer is accepted, so a run that
     * rejects everything can only end by running out of input, and that is the very state the first
     * assertion is about.
     */
    private fun runLangAnswering(vararg answers: String): Interaction {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        val apiWrapper = mockk<ApiWrapper>()
        every { apiWrapper.apiProperties } returns apiProperties

        val captured = ByteArrayOutputStream()
        val originalIn = System.`in`
        val originalOut = System.out
        try {
            System.setIn(ByteArrayInputStream(answers.joinToString("\n", postfix = "\n").toByteArray()))
            System.setOut(PrintStream(captured))
            try {
                LanguageCommand(apiWrapper).run()
            } catch (_: NoSuchElementException) {
                // Every answer was refused and the fed input ran out, which is what the assertions below report on.
            }
        } finally {
            System.setIn(originalIn)
            System.setOut(originalOut)
        }

        val slot = slot<Locale>()
        val stored = try {
            verify { apiProperties.changeLocale(capture(slot)) }
            slot.captured
        } catch (_: AssertionError) {
            null
        }
        return Interaction(captured.toString(), stored)
    }

    /**
     * The locale the command lists must be one it accepts, and the one it then stores.
     */
    @Test
    fun langAcceptsAndStoresTheLocaleItDisplayed() {
        val interaction = runLangAnswering("en_GB")

        Assertions.assertTrue(interaction.shown.contains("en_GB"), "must offer en_GB")
        Assertions.assertFalse(
            interaction.shown.contains("Unsupported locale en_GB."),
            "lang must accept the locale it just displayed"
        )
        Assertions.assertEquals(
            "en_GB",
            interaction.stored?.toString(),
            "lang must store the locale that was chosen, not the remainder of the typed line"
        )
    }

    /**
     * A locale that was never offered is still refused, by name.
     */
    @Test
    fun langRefusesALocaleItNeverOffered() {
        val interaction = runLangAnswering("kl_KL", "pt_BR")

        Assertions.assertTrue(interaction.shown.contains("kl_KL"), "must name the answer it refused")
        Assertions.assertEquals("pt_BR", interaction.stored?.toString(), "must go on to accept a listed locale")
    }
}
