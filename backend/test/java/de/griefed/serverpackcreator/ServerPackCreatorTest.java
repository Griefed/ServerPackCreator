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

  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};

  ServerPackCreatorTest() {
  }

  @Test
  void filesSetupTest() throws IOException, ParserConfigurationException, SAXException {
    ServerPackCreator.getInstance(args).run(Mode.SETUP);
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"server_files").isDirectory());
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"work").isDirectory());
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"work/temp").isDirectory());
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"server-packs").isDirectory());
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"server_files/server.properties").exists());
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"server_files/server-icon.png").exists());
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"server_files/default_template.ps1").exists());
    Assertions.assertTrue(new File(ServerPackCreator.getInstance(args).getApplicationProperties().homeDirectory(),"server_files/default_template.sh").exists());
  }
}
