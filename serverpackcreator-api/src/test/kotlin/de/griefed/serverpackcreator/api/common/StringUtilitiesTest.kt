package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StringUtilitiesTest internal constructor() {
    @Test
    fun buildStringTest() {
        val args: List<String> = listOf("config", "mods", "scripts", "seeds", "defaultconfigs")
        val result: String = StringUtilities.buildString(args.toString())
        Assertions.assertEquals(args.toString(), "[%s]".format(result))
    }

    /**
     * A path free of forbidden characters is considered valid.
     */
    @Test
    fun checkForInvalidPathCharactersAcceptsCleanPath() {
        Assertions.assertTrue(StringUtilities.checkForInvalidPathCharacters("some/clean/path"))
        Assertions.assertTrue(StringUtilities.checkForInvalidPathCharacters(""))
    }

    /**
     * The presence of even a single forbidden character makes a path invalid. (Regression test for
     * the former OR-of-negations logic, which only flagged a path that contained *every* forbidden
     * character at once.)
     */
    @Test
    fun checkForInvalidPathCharactersRejectsAnySingleForbiddenCharacter() {
        for (forbidden in listOf("<", ">", ":", "\"", "|", "?", "*", "#", "%", "&", "{", "}", "$", "!", "@", "`", "´", "=")) {
            Assertions.assertFalse(
                StringUtilities.checkForInvalidPathCharacters("path${forbidden}name"),
                "A path containing '$forbidden' must be rejected"
            )
        }
    }
}