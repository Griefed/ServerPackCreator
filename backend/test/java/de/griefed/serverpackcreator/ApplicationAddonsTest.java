package de.griefed.serverpackcreator;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class ApplicationAddonsTest {

  private static final Logger LOG = LogManager.getLogger(ApplicationAddonsTest.class);

  static {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("plugins"));
    } catch (IOException e) {
      LOG.error("Error copying file.", e);
    }
  }
  ApplicationAddons applicationAddons;

  ApplicationAddonsTest() throws IOException, ParserConfigurationException, SAXException {
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
    Assertions.assertTrue(applicationAddons.configPanelExtensions().size() > 0);

    // TODO have example plugin which creates some file or something, then assert that file exists
  }
}
