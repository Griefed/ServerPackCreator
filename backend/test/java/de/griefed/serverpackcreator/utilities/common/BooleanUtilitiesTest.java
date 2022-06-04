package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BooleanUtilitiesTest {

  private final LocalizationManager LOCALIZATIONMANAGER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final BooleanUtilities BOOLEANUTILITIES;

  BooleanUtilitiesTest() {
    this.LOCALIZATIONMANAGER = new LocalizationManager();
    this.APPLICATIONPROPERTIES = new ApplicationProperties();
    this.BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
  }

  @Test
  void convertToBooleanTestTrue() {
    Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("True"));
    Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("true"));
    Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("1"));
    Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("Yes"));
    Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("yes"));
    Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("Y"));
    Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("y"));
  }

  @Test
  void convertToBooleanTestFalse() {
    Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("False"));
    Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("false"));
    Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("0"));
    Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("No"));
    Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("no"));
    Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("N"));
    Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("n"));
  }
}
