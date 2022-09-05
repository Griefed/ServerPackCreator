package de.griefed.serverpackcreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigurationModelTest {

  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};

  ConfigurationModelTest() {
  }

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

    modloader = "fAbRiC";
    configurationModel.setModLoader(modloader);
    Assertions.assertEquals("Fabric", configurationModel.getModLoader());
    Assertions.assertNotEquals(modloader, configurationModel.getModLoader());

    modloader = "qUiLt";
    configurationModel.setModLoader(modloader);
    Assertions.assertEquals("Quilt", configurationModel.getModLoader());
    Assertions.assertNotEquals(modloader, configurationModel.getModLoader());

    modloader = "lEgAcYfAbRiC";
    configurationModel.setModLoader(modloader);
    Assertions.assertEquals("LegacyFabric", configurationModel.getModLoader());
    Assertions.assertNotEquals(modloader, configurationModel.getModLoader());
  }

  @Test
  void scriptSettingsTest() throws FileNotFoundException {
    ConfigurationModel configurationModel = new ConfigurationModel(ServerPackCreator.getInstance(args)
        .getUtilities(), new File("backend/test/resources/testresources/spcconfs/scriptSettings.conf"));

    Assertions.assertEquals(configurationModel.getServerIconPath(),"C:/Minecraft/ServerPackCreator/server_files/server-icon.png");
    Assertions.assertEquals(configurationModel.getServerPackSuffix(),"-4.0.0");
    Assertions.assertEquals(configurationModel.getServerPropertiesPath(),"C:/Minecraft/ServerPackCreator/server_files/scp3.properties");
    Assertions.assertEquals(configurationModel.getJavaArgs(),"-Xms8G -Xmx8G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true");
    Assertions.assertEquals(configurationModel.getJavaPath(),"C:/Program Files/JetBrains/IntelliJ IDEA 2021.1.1/jbr/bin/java.exe");
    Assertions.assertEquals(configurationModel.getModpackDir(),"C:/Minecraft/Game/Instances/Survive Create Prosper 3");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(),"14.23.5.2860");
    Assertions.assertEquals(configurationModel.getMinecraftVersion(),"1.12.2");
    Assertions.assertEquals(configurationModel.getModLoader(),"Forge");
    Assertions.assertFalse(configurationModel.getIncludeServerInstallation());
    Assertions.assertTrue(configurationModel.getIncludeServerProperties());
    Assertions.assertTrue(configurationModel.getIncludeServerIcon());
    Assertions.assertTrue(configurationModel.getIncludeZipCreation());

    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_FLYNN_LIVES_SPC"));
    Assertions.assertEquals(configurationModel.getScriptSettings().get("SPC_FLYNN_LIVES_SPC"),"Now that's a big door");
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_SOME_VALUE_SPC"));
    Assertions.assertEquals(configurationModel.getScriptSettings().get("SPC_SOME_VALUE_SPC"),"something");
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_PRAISE_THE_LAMB_SPC"));
    Assertions.assertEquals(configurationModel.getScriptSettings().get("SPC_PRAISE_THE_LAMB_SPC"),"Kannema jajaja kannema");
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_ANOTHER_VALUE_SPC"));
    Assertions.assertEquals(configurationModel.getScriptSettings().get("SPC_ANOTHER_VALUE_SPC"),"another");
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_HELLO_SPC"));
    Assertions.assertEquals(configurationModel.getScriptSettings().get("SPC_HELLO_SPC"),"Is it me you are looking foooooor");

    Assertions.assertNotNull(configurationModel.getAddonConfigs("tetris"));
    Assertions.assertNotNull(configurationModel.getAddonConfigs("example"));
    Assertions.assertEquals(3, configurationModel.getAddonsConfigs().get("tetris").size());
    Assertions.assertEquals(3, configurationModel.getAddonsConfigs().get("example").size());

    List<String> list = new ArrayList<>(Arrays.asList("foo", "bar", "fasel", "blubba"));
    configurationModel.getAddonConfigs("tetris").get().forEach(config -> {
      Assertions.assertTrue((Boolean) config.get("bool"));
      Assertions.assertTrue(config.get("loader").toString().matches("(forge|fabric|quilt)"));
      Assertions.assertEquals(config.get("id"),"tetris");
      Assertions.assertEquals(((ArrayList<String>) config.get("list")).size(),4);
      Assertions.assertEquals(config.get("list"), list);
    });
    configurationModel.getAddonConfigs("example").get().forEach(config -> {
      Assertions.assertTrue((Boolean) config.get("bool"));
      Assertions.assertTrue(config.get("loader").toString().matches("(forge|fabric|quilt)"));
      Assertions.assertEquals(config.get("id"),"example");
      Assertions.assertEquals(((ArrayList<String>) config.get("list")).size(),4);
      Assertions.assertEquals(config.get("list"), list);
    });

    File afterFile = new File("after.conf");
    configurationModel.save(afterFile);

    ConfigurationModel after = new ConfigurationModel(ServerPackCreator.getInstance(args).getUtilities(), afterFile);
    Assertions.assertEquals(after.getServerIconPath(),configurationModel.getServerIconPath());
    Assertions.assertEquals(after.getServerPackSuffix(),configurationModel.getServerPackSuffix());
    Assertions.assertEquals(after.getServerPropertiesPath(),configurationModel.getServerPropertiesPath());
    Assertions.assertEquals(after.getJavaArgs(),configurationModel.getJavaArgs());
    Assertions.assertEquals(after.getJavaPath(),configurationModel.getJavaPath());
    Assertions.assertEquals(after.getModpackDir(),configurationModel.getModpackDir());
    Assertions.assertEquals(after.getModLoaderVersion(),configurationModel.getModLoaderVersion());
    Assertions.assertEquals(after.getMinecraftVersion(),configurationModel.getMinecraftVersion());
    Assertions.assertEquals(after.getModLoader(),configurationModel.getModLoader());
    Assertions.assertFalse(after.getIncludeServerInstallation());
    Assertions.assertTrue(after.getIncludeServerProperties());
    Assertions.assertTrue(after.getIncludeServerIcon());
    Assertions.assertTrue(after.getIncludeZipCreation());

    Assertions.assertTrue(after.getScriptSettings().containsKey("SPC_FLYNN_LIVES_SPC"));
    Assertions.assertEquals(after.getScriptSettings().get("SPC_FLYNN_LIVES_SPC"),"Now that's a big door");
    Assertions.assertTrue(after.getScriptSettings().containsKey("SPC_SOME_VALUE_SPC"));
    Assertions.assertEquals(after.getScriptSettings().get("SPC_SOME_VALUE_SPC"),"something");
    Assertions.assertTrue(after.getScriptSettings().containsKey("SPC_PRAISE_THE_LAMB_SPC"));
    Assertions.assertEquals(after.getScriptSettings().get("SPC_PRAISE_THE_LAMB_SPC"),"Kannema jajaja kannema");
    Assertions.assertTrue(after.getScriptSettings().containsKey("SPC_ANOTHER_VALUE_SPC"));
    Assertions.assertEquals(after.getScriptSettings().get("SPC_ANOTHER_VALUE_SPC"),"another");
    Assertions.assertTrue(after.getScriptSettings().containsKey("SPC_HELLO_SPC"));
    Assertions.assertEquals(after.getScriptSettings().get("SPC_HELLO_SPC"),"Is it me you are looking foooooor");

    Assertions.assertNotNull(after.getAddonConfigs("tetris"));
    Assertions.assertNotNull(after.getAddonConfigs("example"));
    Assertions.assertEquals(configurationModel.getAddonsConfigs().get("tetris").size(), after.getAddonsConfigs().get("tetris").size());
    Assertions.assertEquals(configurationModel.getAddonsConfigs().get("example").size(), after.getAddonsConfigs().get("example").size());

    after.getAddonConfigs("tetris").get().forEach(config -> {
      Assertions.assertTrue((Boolean) config.get("bool"));
      Assertions.assertTrue(config.get("loader").toString().matches("(forge|fabric|quilt)"));
      Assertions.assertEquals(config.get("id"),"tetris");
      Assertions.assertEquals(((ArrayList<String>) config.get("list")).size(),4);
      Assertions.assertEquals(config.get("list"), list);
    });
    after.getAddonConfigs("example").get().forEach(config -> {
      Assertions.assertTrue((Boolean) config.get("bool"));
      Assertions.assertTrue(config.get("loader").toString().matches("(forge|fabric|quilt)"));
      Assertions.assertEquals(config.get("id"),"example");
      Assertions.assertEquals(((ArrayList<String>) config.get("list")).size(),4);
      Assertions.assertEquals(config.get("list"), list);
    });
  }
}
