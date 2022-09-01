package de.griefed.serverpackcreator;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class ApplicationPluginsTest {

  private static final Logger LOG = LogManager.getLogger(ApplicationPluginsTest.class);

  static {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("plugins"));
    } catch (IOException e) {
      LOG.error("Error copying file.", e);
    }
  }

  ApplicationPlugins applicationPlugins;

  ApplicationPluginsTest() {
    applicationPlugins = ServerPackCreator.getInstance().getApplicationPlugins();
  }

  @Test
  void test() {
    Assertions.assertNotNull(applicationPlugins);
    Assertions.assertTrue(applicationPlugins.pluginsPostGenExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsTabExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsPreGenExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsPreZipExtension().size() > 0);
  }
}
