package de.griefed.serverpackcreator;

import static de.griefed.serverpackcreator.Dependencies.APPLICATION_PLUGINS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPluginsTest {

  @Test
  void test() {
    Assertions.assertNotNull(APPLICATION_PLUGINS);
    Assertions.assertTrue(APPLICATION_PLUGINS.pluginsPostGenExtension().size() > 0);
    Assertions.assertTrue(APPLICATION_PLUGINS.pluginsTabExtension().size() > 0);
    Assertions.assertTrue(APPLICATION_PLUGINS.pluginsPreGenExtension().size() > 0);
    Assertions.assertTrue(APPLICATION_PLUGINS.pluginsPreZipExtension().size() > 0);

    // TODO have example plugin which creates some file or something, then assert that file exists
  }
}
