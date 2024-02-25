package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.BooleanUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class BooleanUtilitiesTest internal constructor() {
    private var booleanUtilities: BooleanUtilities =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).booleanUtilities

    @Test
    fun convertToBooleanTestTrue() {
        Assertions.assertTrue(booleanUtilities.convert("True"))
        Assertions.assertTrue(booleanUtilities.convert("true"))
        Assertions.assertTrue(booleanUtilities.convert("1"))
        Assertions.assertTrue(booleanUtilities.convert("Yes"))
        Assertions.assertTrue(booleanUtilities.convert("yes"))
        Assertions.assertTrue(booleanUtilities.convert("Y"))
        Assertions.assertTrue(booleanUtilities.convert("y"))
    }

    @Test
    fun convertToBooleanTestFalse() {
        Assertions.assertFalse(booleanUtilities.convert("False"))
        Assertions.assertFalse(booleanUtilities.convert("false"))
        Assertions.assertFalse(booleanUtilities.convert("0"))
        Assertions.assertFalse(booleanUtilities.convert("No"))
        Assertions.assertFalse(booleanUtilities.convert("no"))
        Assertions.assertFalse(booleanUtilities.convert("N"))
        Assertions.assertFalse(booleanUtilities.convert("n"))
    }
}