package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.Dependencies;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SystemUtilitiesTest {

  SystemUtilities systemUtilities;

  SystemUtilitiesTest() {
    systemUtilities = Dependencies.getInstance().UTILITIES().SystemUtils();
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
