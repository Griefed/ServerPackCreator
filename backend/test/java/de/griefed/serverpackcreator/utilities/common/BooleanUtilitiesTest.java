package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.Dependencies;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BooleanUtilitiesTest {

  BooleanUtilitiesTest() {}

  @Test
  void convertToBooleanTestTrue() {
    Assertions.assertTrue(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("True"));
    Assertions.assertTrue(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("true"));
    Assertions.assertTrue(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("1"));
    Assertions.assertTrue(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("Yes"));
    Assertions.assertTrue(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("yes"));
    Assertions.assertTrue(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("Y"));
    Assertions.assertTrue(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("y"));
  }

  @Test
  void convertToBooleanTestFalse() {
    Assertions.assertFalse(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("False"));
    Assertions.assertFalse(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("false"));
    Assertions.assertFalse(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("0"));
    Assertions.assertFalse(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("No"));
    Assertions.assertFalse(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("no"));
    Assertions.assertFalse(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("N"));
    Assertions.assertFalse(
        Dependencies.getInstance().UTILITIES().BooleanUtils().convertToBoolean("n"));
  }
}
