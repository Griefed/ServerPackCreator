package de.griefed.serverpackcreator;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerPackCreatorTest {

  ServerPackCreator serverPackCreator;

  ServerPackCreatorTest() throws IOException {
    serverPackCreator = new ServerPackCreator(new String[] {"--setup"});
    serverPackCreator.run(ServerPackCreator.CommandlineParser.Mode.SETUP);
  }

  @Test
  void filesSetupTest() throws IOException {
    FileUtils.deleteQuietly(new File("./server_files"));
    FileUtils.deleteQuietly(new File("./work"));
    FileUtils.deleteQuietly(new File("./work/temp"));
    FileUtils.deleteQuietly(new File("./server-packs"));
    FileUtils.deleteQuietly(new File("./server_files/server.properties"));
    FileUtils.deleteQuietly(new File("./server_files/server-icon.png"));
    //FileUtils.deleteQuietly(new File("./serverpackcreator.conf"));
    serverPackCreator.run(ServerPackCreator.CommandlineParser.Mode.SETUP);
    Assertions.assertTrue(new File("./server_files").isDirectory());
    Assertions.assertTrue(new File("./work").isDirectory());
    Assertions.assertTrue(new File("./work/temp").isDirectory());
    Assertions.assertTrue(new File("./server-packs").isDirectory());
    Assertions.assertTrue(new File("./server_files/server.properties").exists());
    Assertions.assertTrue(new File("./server_files/server-icon.png").exists());
    Assertions.assertTrue(new File("./server_files/default_template.ps1").exists());
    Assertions.assertTrue(new File("./server_files/default_template.sh").exists());
    //Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
  }
}
