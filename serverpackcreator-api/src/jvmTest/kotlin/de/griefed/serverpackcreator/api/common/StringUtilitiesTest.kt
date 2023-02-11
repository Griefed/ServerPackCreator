package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class StringUtilitiesTest internal constructor() {
    private var stringUtilities: StringUtilities =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).stringUtilities

    @Test
    fun buildStringTest() {
        val args: List<String> = listOf("config", "mods", "scripts", "seeds", "defaultconfigs")
        val result: String = stringUtilities.buildString(args.toString())
        Assertions.assertEquals(args.toString(), "[%s]".format(result))
    }
}