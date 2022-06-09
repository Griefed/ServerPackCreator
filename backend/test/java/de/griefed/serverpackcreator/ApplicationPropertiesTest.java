package de.griefed.serverpackcreator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPropertiesTest {

  private final ApplicationProperties APPLICATIONPROPERTIES;

  ApplicationPropertiesTest() {
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.APPLICATIONPROPERTIES = new ApplicationProperties();
  }

  @Test
  void finalsTest() {
    Assertions.assertNotNull(APPLICATIONPROPERTIES.SERVERPACKCREATOR_PROPERTIES());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.SERVERPACKCREATOR_PROPERTIES(),
        new File("serverpackcreator.properties"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.START_SCRIPT_WINDOWS());
    Assertions.assertEquals(APPLICATIONPROPERTIES.START_SCRIPT_WINDOWS(), new File("start.bat"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.START_SCRIPT_LINUX());
    Assertions.assertEquals(APPLICATIONPROPERTIES.START_SCRIPT_LINUX(), new File("start.sh"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.USER_JVM_ARGS());
    Assertions.assertEquals(APPLICATIONPROPERTIES.USER_JVM_ARGS(), new File("user_jvm_args.txt"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.FALLBACK_CLIENTSIDE_MODS());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.FALLBACK_CLIENTSIDE_MODS(),
        new ArrayList<>(
            Arrays.asList(
                ("3dSkinLayers-,"
                        + "3dskinlayers-,"
                        + "Absolutely-Not-A-Zoom-Mod-,"
                        + "AdvancementPlaques-,"
                        + "AmbientEnvironment-,"
                        + "AmbientSounds_,"
                        + "antighost-,"
                        + "armorchroma-,"
                        + "armorpointspp-,"
                        + "ArmorSoundTweak-,"
                        + "authme-,"
                        + "autoreconnect-,"
                        + "auto-reconnect-,"
                        + "axolotl-item-fix-,"
                        + "backtools-,"
                        + "BetterAdvancements-,"
                        + "BetterAnimationsCollection-,"
                        + "betterbiomeblend-,"
                        + "BetterDarkMode-,"
                        + "BetterF3-,"
                        + "BetterFoliage-,"
                        + "BetterPingDisplay-,"
                        + "BetterPlacement-,"
                        + "BetterTaskbar-,"
                        + "bhmenu-,"
                        + "BH-Menu-,"
                        + "blur-,"
                        + "Blur-,"
                        + "borderless-mining-,"
                        + "catalogue-,"
                        + "charmonium-,"
                        + "Charmonium-,"
                        + "chat_heads-,"
                        + "cherishedworlds-,"
                        + "classicbar-,"
                        + "clickadv-,"
                        + "ClientTweaks_,"
                        + "configured-,"
                        + "Controlling-,"
                        + "CraftPresence-,"
                        + "CTM-,"
                        + "cullleaves-,"
                        + "customdiscordrpc-,"
                        + "CustomMainMenu-,"
                        + "dashloader-,"
                        + "DefaultOptions_,"
                        + "defaultoptions-,"
                        + "DefaultSettings-,"
                        + "DeleteWorldsToTrash-,"
                        + "desiredservers-,"
                        + "Ding-,"
                        + "drippyloadingscreen_,"
                        + "drippyloadingscreen-,"
                        + "DripSounds-,"
                        + "Durability101-,"
                        + "DurabilityNotifier-,"
                        + "dynamic-fps-,"
                        + "dynamic-music-,"
                        + "DynamicSurroundings-,"
                        + "DynamicSurroundingsHuds-,"
                        + "dynmus-,"
                        + "effective-,"
                        + "eggtab-,"
                        + "EiraMoticons_,"
                        + "eiramoticons-,"
                        + "EnchantmentDescriptions-,"
                        + "entity-texture-features-,"
                        + "EquipmentCompare-,"
                        + "extremesoundmuffler-,"
                        + "extremeSoundMuffler-,"
                        + "fabricemotes-,"
                        + "Fallingleaves-,"
                        + "fallingleaves-,"
                        + "fancymenu_,"
                        + "findme-,"
                        + "flickerfix-,"
                        + "FPS-Monitor-,"
                        + "FpsReducer-,"
                        + "FullscreenWindowed-,"
                        + "InventoryEssentials_,"
                        + "InventorySpam-,"
                        + "InventoryTweaks-,"
                        + "invtweaks-,"
                        + "ItemBorders-,"
                        + "itemzoom,"
                        + "itlt-,"
                        + "jeed-,"
                        + "jehc-,"
                        + "jeiintegration_,"
                        + "just-enough-harvestcraft-,"
                        + "justenoughbeacons-,"
                        + "JustEnoughCalculation-,"
                        + "JustEnoughProfessions-,"
                        + "JustEnoughProfessions-,"
                        + "JustEnoughResources-,"
                        + "keymap-,"
                        + "keywizard-,"
                        + "konkrete_,"
                        + "lazydfu-,"
                        + "LegendaryTooltips-,"
                        + "light-overlay-,"
                        + "LightOverlay-,"
                        + "LLOverlayReloaded-,"
                        + "loadmyresources_,"
                        + "lootbeams-,"
                        + "mcbindtype-,"
                        + "medievalmusic-,"
                        + "modcredits-,"
                        + "modmenu-,"
                        + "modnametooltip_,"
                        + "modnametooltip-,"
                        + "moreoverlays-,"
                        + "MouseTweaks-,"
                        + "movement-vision-,"
                        + "multihotbar-,"
                        + "musicdr-,"
                        + "music-duration-reducer-,"
                        + "MyServerIsCompatible-,"
                        + "Neat ,"
                        + "ngrok-lan-expose-mod-,"
                        + "NotifMod-,"
                        + "OldJavaWarning-,"
                        + "OptiFine,"
                        + "OptiForge,"
                        + "ornaments-,"
                        + "overloadedarmorbar-,"
                        + "PackMenu-,"
                        + "PickUpNotifier-,"
                        + "Ping-,"
                        + "preciseblockplacing-,"
                        + "presencefootsteps-,"
                        + "PresenceFootsteps-,"
                        + "ReAuth-,"
                        + "rebrand-,"
                        + "ResourceLoader-,"
                        + "shutupexperimentalsettings-,"
                        + "SimpleDiscordRichPresence-,"
                        + "smoothboot-,"
                        + "sounddeviceoptions-,"
                        + "SpawnerFix-,"
                        + "spoticraft-,"
                        + "tconplanner-,"
                        + "timestamps-,"
                        + "Tips-,"
                        + "TipTheScales-,"
                        + "Toast Control-,"
                        + "Toast-Control-,"
                        + "ToastControl-,"
                        + "torchoptimizer-,"
                        + "torohealth-,"
                        + "toughnessbar-,"
                        + "TravelersTitles-,"
                        + "WindowedFullscreen-,"
                        + "WorldNameRandomizer-,"
                        + "yisthereautojump-")
                    .split(","))));
    Assertions.assertNotNull(APPLICATIONPROPERTIES.getListFallbackMods());
    APPLICATIONPROPERTIES.setProperty(
        "de.griefed.serverpackcreator.configuration.fallbackmodslist", "bla");
    APPLICATIONPROPERTIES.updateFallback();
    Assertions.assertNotEquals(
        APPLICATIONPROPERTIES.getProperty(
            "de.griefed.serverpackcreator.configuration.fallbackmodslist"),
        "bla");

    Assertions.assertNotNull(APPLICATIONPROPERTIES.DEFAULT_CONFIG());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.DEFAULT_CONFIG(), new File("serverpackcreator.conf"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.OLD_CONFIG());
    Assertions.assertEquals(APPLICATIONPROPERTIES.OLD_CONFIG(), new File("creator.conf"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES(), new File("server.properties"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON(), new File("server-icon.png"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST(), new File("minecraft-manifest.json"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST(), new File("forge-manifest.json"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST(), new File("fabric-manifest.xml"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST(),
        new File("fabric-installer-manifest.xml"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST(), new File("quilt-manifest.xml"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST(),
        new File("quilt-installer-manifest.xml"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.SERVERPACKCREATOR_DATABASE());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.SERVERPACKCREATOR_DATABASE(), new File("serverpackcreator.db"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
        new File("./work/minecraft-manifest.json"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
        new File("./work/forge-manifest.json"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
        new File("./work/fabric-manifest.xml"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./work/fabric-installer-manifest.xml"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
        new File("./work/quilt-manifest.xml"));

    Assertions.assertNotNull(APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./work/quilt-installer-manifest.xml"));
  }

  @Test
  void getDirectoryServerPacksTest() {
    Assertions.assertNotNull(APPLICATIONPROPERTIES.getDirectoryServerPacks());
  }

  @Test
  void getListOfDirectoriesToExcludeTest() {
    Assertions.assertNotNull(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude());
    APPLICATIONPROPERTIES.addToListOfDirectoriesToExclude("test");
    Assertions.assertTrue(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude().contains("test"));
  }

  @Test
  void reloadTest() {
    Assertions.assertNotNull(APPLICATIONPROPERTIES.reload());
    Assertions.assertInstanceOf(ApplicationProperties.class, APPLICATIONPROPERTIES.reload());
  }

  @Test
  void booleanTests() {
    Assertions.assertFalse(APPLICATIONPROPERTIES.getSaveLoadedConfiguration());
    Assertions.assertFalse(APPLICATIONPROPERTIES.checkForAvailablePreReleases());
  }

  @Test
  void queueMaxUsageTest() {
    Assertions.assertEquals(90, APPLICATIONPROPERTIES.getQueueMaxDiskUsage());
  }

  @Test
  void getServerPackCreatorVersionTest() {
    Assertions.assertEquals("dev", APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION());
  }
}
