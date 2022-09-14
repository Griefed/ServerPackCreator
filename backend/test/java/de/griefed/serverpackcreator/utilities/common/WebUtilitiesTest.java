package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.ApplicationProperties;
import java.io.File;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WebUtilitiesTest {

  WebUtilities webUtilities;

  WebUtilitiesTest() {
    webUtilities = new WebUtilities(new ApplicationProperties(new FileUtilities(),new SystemUtilities(), new ListUtilities()));
  }

  @Test
  void downloadFileTest() {
    try {
      webUtilities.downloadFile(
          "Fabric-Server-Launcher.jar",
          new URL("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar"));
    } catch (Exception ignored) {
    }
    Assertions.assertTrue(new File("Fabric-Server-Launcher.jar").exists());
    FileUtils.deleteQuietly(new File("Fabric-Server-Launcher.jar"));
    try {
      webUtilities.downloadFile(
          "some_foooooolder/foooobar/Fabric-Server-Launcher.jar",
          new URL("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar"));
    } catch (Exception ignored) {
    }
    Assertions.assertTrue(
        new File("some_foooooolder/foooobar/Fabric-Server-Launcher.jar").exists());
    FileUtils.deleteQuietly(new File("some_foooooolder"));
  }
}
