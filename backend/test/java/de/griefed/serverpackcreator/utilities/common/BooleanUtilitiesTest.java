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
    Assertions.assertTrue(booleanUtilities.convertToBoolean("True"));
    Assertions.assertTrue(booleanUtilities.convertToBoolean("true"));
    Assertions.assertTrue(booleanUtilities.convertToBoolean("1"));
    Assertions.assertTrue(booleanUtilities.convertToBoolean("Yes"));
    Assertions.assertTrue(booleanUtilities.convertToBoolean("yes"));
    Assertions.assertTrue(booleanUtilities.convertToBoolean("Y"));
    Assertions.assertTrue(booleanUtilities.convertToBoolean("y"));
  }

  @Test
  void convertToBooleanTestFalse() {
    Assertions.assertFalse(booleanUtilities.convertToBoolean("False"));
    Assertions.assertFalse(booleanUtilities.convertToBoolean("false"));
    Assertions.assertFalse(booleanUtilities.convertToBoolean("0"));
    Assertions.assertFalse(booleanUtilities.convertToBoolean("No"));
    Assertions.assertFalse(booleanUtilities.convertToBoolean("no"));
    Assertions.assertFalse(booleanUtilities.convertToBoolean("N"));
    Assertions.assertFalse(booleanUtilities.convertToBoolean("n"));
  }
}
