package de.griefed.serverpackcreator.utilities.common;

import java.io.File;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JarUtilitiesTest {

  JarUtilities jarUtilities;

  JarUtilitiesTest() {
    jarUtilities = new JarUtilities();
  }

  @Test
  void copyFileFromJarTest() {
    jarUtilities.copyFileFromJar("banner.txt", JarUtilitiesTest.class,
                                 new File("tests").getAbsolutePath());
    Assertions.assertTrue(new File("tests/banner.txt").isFile());
  }

  @Test
  void getApplicationHomeForClassTest() {
    Assertions.assertNotNull(jarUtilities.getApplicationHomeForClass(JarUtilitiesTest.class));
  }

  @Test
  void systemInformationTest() {
    HashMap<String, String> system =
        jarUtilities.jarInformation(
            jarUtilities.getApplicationHomeForClass(JarUtilitiesTest.class));
    Assertions.assertNotNull(system);
    Assertions.assertNotNull(system.get("jarPath"));
    Assertions.assertTrue(system.get("jarPath").length() > 0);
    Assertions.assertNotNull(system.get("jarName"));
    Assertions.assertTrue(system.get("jarName").length() > 0);
    Assertions.assertNotNull(system.get("javaVersion"));
    Assertions.assertTrue(system.get("javaVersion").length() > 0);
    Assertions.assertNotNull(system.get("osArch"));
    Assertions.assertTrue(system.get("osArch").length() > 0);
    Assertions.assertNotNull(system.get("osName"));
    Assertions.assertTrue(system.get("osName").length() > 0);
    Assertions.assertNotNull(system.get("osVersion"));
    Assertions.assertTrue(system.get("osVersion").length() > 0);
  }

  @Test
  void copyFolderFromJarTest() {
    try {
      jarUtilities.copyFolderFromJar(
          JarUtilitiesTest.class,
          "/de/griefed/resources/manifests",
          "tests/manifestTest",
          "",
          "xml|json");
    } catch (Exception ignored) {
    }
    Assertions.assertTrue(new File("tests/manifestTest").isDirectory());
    Assertions.assertTrue(new File("tests/manifestTest/fabric-installer-manifest.xml").isFile());
    Assertions.assertTrue(new File("tests/manifestTest/mcserver").isDirectory());
    Assertions.assertTrue(new File("tests/manifestTest/mcserver/1.8.2.json").isFile());
  }
}
