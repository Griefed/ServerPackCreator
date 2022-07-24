package de.griefed.serverpackcreator;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPluginsTest {

  private static final Logger LOG = LogManager.getLogger(ApplicationPluginsTest.class);
  ApplicationPlugins applicationPlugins;

  ApplicationPluginsTest() {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("plugins"));
    } catch (IOException e) {
      LOG.error("Error copying file.", e);
    }
    applicationPlugins = new ApplicationPlugins();
  }

  @Test
  void test() {
    Assertions.assertNotNull(applicationPlugins);
    Assertions.assertTrue(applicationPlugins.pluginsPostGenExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsTabExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsPreGenExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsPreZipExtension().size() > 0);

    // TODO have example plugin which creates some file or something, then assert that file exists
  }
}
