package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.BooleanUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BooleanUtilitiesTest internal constructor() {

    @Test
    fun convertToBooleanTestTrue() {
        Assertions.assertTrue(BooleanUtilities.convert("True"))
        Assertions.assertTrue(BooleanUtilities.convert("true"))
        Assertions.assertTrue(BooleanUtilities.convert("1"))
        Assertions.assertTrue(BooleanUtilities.convert("Yes"))
        Assertions.assertTrue(BooleanUtilities.convert("yes"))
        Assertions.assertTrue(BooleanUtilities.convert("Y"))
        Assertions.assertTrue(BooleanUtilities.convert("y"))
    }

    @Test
    fun convertToBooleanTestFalse() {
        Assertions.assertFalse(BooleanUtilities.convert("False"))
        Assertions.assertFalse(BooleanUtilities.convert("false"))
        Assertions.assertFalse(BooleanUtilities.convert("0"))
        Assertions.assertFalse(BooleanUtilities.convert("No"))
        Assertions.assertFalse(BooleanUtilities.convert("no"))
        Assertions.assertFalse(BooleanUtilities.convert("N"))
        Assertions.assertFalse(BooleanUtilities.convert("n"))
    }
}