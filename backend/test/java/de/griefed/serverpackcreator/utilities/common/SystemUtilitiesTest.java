package de.griefed.serverpackcreator.utilities.common;

import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SystemUtilitiesTest {

  private final SystemUtilities SYSTEMUTILITIES;

  SystemUtilitiesTest() {
    this.SYSTEMUTILITIES = new SystemUtilities();
  }

  @Test
  void acquireJavaPathFromSystemTest() {
    Assertions.assertNotNull(SYSTEMUTILITIES.acquireJavaPathFromSystem());
    Assertions.assertTrue(new File(SYSTEMUTILITIES.acquireJavaPathFromSystem()).exists());
    Assertions.assertTrue(new File(SYSTEMUTILITIES.acquireJavaPathFromSystem()).isFile());
    Assertions.assertTrue(
        SYSTEMUTILITIES.acquireJavaPathFromSystem().replace("\\", "/").contains("bin/java"));
  }
}
