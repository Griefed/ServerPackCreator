package de.griefed.serverpackcreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigurationModelTest {

  ConfigurationModelTest() {}

  @Test
  void getsetCopyDirsTest() {
    List<String> testList =
        new ArrayList<>(
            Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs", "server_pack"));
    List<String> getList =
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs"));
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setCopyDirs(testList);
    Assertions.assertNotNull(configurationModel.getCopyDirs());
    Assertions.assertFalse(configurationModel.getCopyDirs().contains("server_pack"));
    Assertions.assertEquals(getList, configurationModel.getCopyDirs());
  }

  @Test
  void getsetModLoaderTest() {
    String modloader = "FoRgE";
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setModLoader(modloader);
    Assertions.assertEquals("Forge", configurationModel.getModLoader());
    Assertions.assertNotEquals(modloader, configurationModel.getModLoader());
  }
}
