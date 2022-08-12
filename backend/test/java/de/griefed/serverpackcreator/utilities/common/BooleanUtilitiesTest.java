package de.griefed.serverpackcreator.utilities.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BooleanUtilitiesTest {

  BooleanUtilities booleanUtilities;

  BooleanUtilitiesTest() {
    booleanUtilities = new BooleanUtilities();
  }

  @Test
  void convertToBooleanTestTrue() {
    Assertions.assertTrue(booleanUtilities.convert("True"));
    Assertions.assertTrue(booleanUtilities.convert("true"));
    Assertions.assertTrue(booleanUtilities.convert("1"));
    Assertions.assertTrue(booleanUtilities.convert("Yes"));
    Assertions.assertTrue(booleanUtilities.convert("yes"));
    Assertions.assertTrue(booleanUtilities.convert("Y"));
    Assertions.assertTrue(booleanUtilities.convert("y"));
  }

  @Test
  void convertToBooleanTestFalse() {
    Assertions.assertFalse(booleanUtilities.convert("False"));
    Assertions.assertFalse(booleanUtilities.convert("false"));
    Assertions.assertFalse(booleanUtilities.convert("0"));
    Assertions.assertFalse(booleanUtilities.convert("No"));
    Assertions.assertFalse(booleanUtilities.convert("no"));
    Assertions.assertFalse(booleanUtilities.convert("N"));
    Assertions.assertFalse(booleanUtilities.convert("n"));
  }
}
