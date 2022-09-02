package de.griefed.serverpackcreator;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ApplicationAddonsTest {

  private static final Logger LOG = LogManager.getLogger(ApplicationAddonsTest.class);
  ApplicationAddons applicationAddons;

  ApplicationAddonsTest() {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("plugins"));
    } catch (IOException e) {
      LOG.error("Error copying file.", e);
    }
    applicationAddons = ServerPackCreator.getInstance().getApplicationAddons();
  }

  @Test
  @Disabled
  void test() {
    Assertions.assertNotNull(applicationAddons);
    Assertions.assertTrue(applicationAddons.postGenExtensions().size() > 0);
    Assertions.assertTrue(applicationAddons.tabExtensions().size() > 0);
    Assertions.assertTrue(applicationAddons.preGenExtensions().size() > 0);
    Assertions.assertTrue(applicationAddons.preZipExtensions().size() > 0);
    Assertions.assertTrue(applicationAddons.configCheckExtensions().size() > 0);
    Assertions.assertTrue(applicationAddons.configPaneExtensions().size() > 0);

    // TODO have example plugin which creates some file or something, then assert that file exists
  }
}
