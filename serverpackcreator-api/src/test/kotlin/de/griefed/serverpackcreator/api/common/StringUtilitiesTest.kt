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
}