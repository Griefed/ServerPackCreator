package de.griefed.serverpackcreator.utilities.common;

import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SystemUtilitiesTest {

  SystemUtilities systemUtilities;

  SystemUtilitiesTest() {
    systemUtilities = new SystemUtilities();
  }

  @Test
  void acquireJavaPathFromSystemTest() {
    Assertions.assertNotNull(systemUtilities.acquireJavaPathFromSystem());
    Assertions.assertTrue(new File(systemUtilities.acquireJavaPathFromSystem()).exists());
    Assertions.assertTrue(new File(systemUtilities.acquireJavaPathFromSystem()).isFile());
    Assertions.assertTrue(
        systemUtilities.acquireJavaPathFromSystem().replace("\\", "/").contains("bin/java"));
  }
}
