package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.Dependencies;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SystemUtilitiesTest {

  SystemUtilitiesTest() {}

  @Test
  void acquireJavaPathFromSystemTest() {
    Assertions.assertNotNull(
        Dependencies.getInstance().UTILITIES().SystemUtils().acquireJavaPathFromSystem());
    Assertions.assertTrue(
        new File(Dependencies.getInstance().UTILITIES().SystemUtils().acquireJavaPathFromSystem())
            .exists());
    Assertions.assertTrue(
        new File(Dependencies.getInstance().UTILITIES().SystemUtils().acquireJavaPathFromSystem())
            .isFile());
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .SystemUtils()
            .acquireJavaPathFromSystem()
            .replace("\\", "/")
            .contains("bin/java"));
  }
}
