package de.griefed.serverpackcreator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPluginsTest {

  ApplicationPlugins applicationPlugins;

  ApplicationPluginsTest() {
    applicationPlugins = Dependencies.getInstance().APPLICATION_PLUGINS();
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
