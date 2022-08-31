package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.ServerPackCreator.Mode;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class ServerPackCreatorTest {


  ServerPackCreatorTest() {
  }

  @Test
  void filesSetupTest() throws IOException, ParserConfigurationException, SAXException {
    FileUtils.deleteQuietly(new File("./server_files"));
    FileUtils.deleteQuietly(new File("./work"));
    FileUtils.deleteQuietly(new File("./work/temp"));
    FileUtils.deleteQuietly(new File("./server-packs"));
    FileUtils.deleteQuietly(new File("./server_files/server.properties"));
    FileUtils.deleteQuietly(new File("./server_files/server-icon.png"));
    ServerPackCreator.getInstance().run(Mode.SETUP);
    Assertions.assertTrue(new File("./server_files").isDirectory());
    Assertions.assertTrue(new File("./work").isDirectory());
    Assertions.assertTrue(new File("./work/temp").isDirectory());
    Assertions.assertTrue(new File("./server-packs").isDirectory());
    Assertions.assertTrue(new File("./server_files/server.properties").exists());
    Assertions.assertTrue(new File("./server_files/server-icon.png").exists());
    Assertions.assertTrue(new File("./server_files/default_template.ps1").exists());
    Assertions.assertTrue(new File("./server_files/default_template.sh").exists());
  }
}
