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

public class ApplicationAddonsTest {

  private static final Logger LOG = LogManager.getLogger(ApplicationAddonsTest.class);

  static {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("tests/plugins"));
    } catch (IOException e) {
      LOG.error("Error copying file.", e);
    }
  }

  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};

  @Test
  void test() throws IOException, ParserConfigurationException, SAXException {
    Assertions.assertNotNull(ServerPackCreator.getInstance(args).getApplicationAddons());
    ServerPackCreator.getInstance(args).getApplicationAddons().loadPlugins();
    ServerPackCreator.getInstance(args).getApplicationAddons().startPlugins();
    Assertions.assertTrue(
        ServerPackCreator.getInstance(args).getApplicationAddons().postGenExtensions().size() > 0);
    Assertions.assertTrue(
        ServerPackCreator.getInstance(args).getApplicationAddons().tabExtensions().size() > 0);
    Assertions.assertTrue(
        ServerPackCreator.getInstance(args).getApplicationAddons().preGenExtensions().size() > 0);
    Assertions.assertTrue(
        ServerPackCreator.getInstance(args).getApplicationAddons().preZipExtensions().size() > 0);
    Assertions.assertTrue(
        ServerPackCreator.getInstance(args).getApplicationAddons().configCheckExtensions().size()
            > 0);
    Assertions.assertTrue(
        ServerPackCreator.getInstance(args).getApplicationAddons().configPanelExtensions().size()
            > 0);
  }
}
