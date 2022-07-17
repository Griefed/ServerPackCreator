package de.griefed.serverpackcreator;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPluginsTest {

  @Test
  void test() throws IOException {
    FileUtils.copyDirectory(
        new File("backend/test/resources/testresources/addons"), new File("plugins"));
    ApplicationPlugins applicationPlugins = new ApplicationPlugins();
    Assertions.assertNotNull(applicationPlugins);
    Assertions.assertTrue(applicationPlugins.pluginsPostGenExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsTabExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsPreGenExtension().size() > 0);
    Assertions.assertTrue(applicationPlugins.pluginsPreZipExtension().size() > 0);


    //TODO have example plugin which creates some file or something, then assert that file exists
  }
}
