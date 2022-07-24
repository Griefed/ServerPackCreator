package de.griefed.serverpackcreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPropertiesTest {

  ApplicationProperties applicationProperties;

  ApplicationPropertiesTest() {
    applicationProperties = Dependencies.getInstance().APPLICATIONPROPERTIES();
  }

  @Test
  void finalsTest() {
    Assertions.assertNotNull(applicationProperties.SERVERPACKCREATOR_PROPERTIES());
    Assertions.assertEquals(
        applicationProperties.SERVERPACKCREATOR_PROPERTIES(),
        new File("serverpackcreator.properties"));

    Assertions.assertNotNull(applicationProperties.FALLBACK_CLIENTSIDE_MODS());
    Assertions.assertEquals(
        applicationProperties.FALLBACK_CLIENTSIDE_MODS(),
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
                        + "ItemStitchingFix-,"
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
    Assertions.assertNotNull(applicationProperties.getListFallbackMods());
    applicationProperties.setProperty(
        "de.griefed.serverpackcreator.configuration.fallbackmodslist", "bla");
    applicationProperties.updateFallback();
    Assertions.assertNotEquals(
        applicationProperties.getProperty(
            "de.griefed.serverpackcreator.configuration.fallbackmodslist"),
        "bla");

    Assertions.assertNotNull(applicationProperties.DEFAULT_CONFIG());
    Assertions.assertEquals(
        applicationProperties.DEFAULT_CONFIG(), new File("serverpackcreator.conf"));

    Assertions.assertNotNull(applicationProperties.OLD_CONFIG());
    Assertions.assertEquals(applicationProperties.OLD_CONFIG(), new File("creator.conf"));

    Assertions.assertNotNull(applicationProperties.DEFAULT_SERVER_PROPERTIES());
    Assertions.assertEquals(
        applicationProperties.DEFAULT_SERVER_PROPERTIES(), new File("server.properties"));

    Assertions.assertNotNull(applicationProperties.DEFAULT_SERVER_ICON());
    Assertions.assertEquals(
        applicationProperties.DEFAULT_SERVER_ICON(), new File("server-icon.png"));

    Assertions.assertNotNull(applicationProperties.MINECRAFT_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.MINECRAFT_VERSION_MANIFEST(), new File("minecraft-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FORGE_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.FORGE_VERSION_MANIFEST(), new File("forge-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FABRIC_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.FABRIC_VERSION_MANIFEST(), new File("fabric-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST(),
        new File("fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.QUILT_VERSION_MANIFEST(), new File("quilt-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST(),
        new File("quilt-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.SERVERPACKCREATOR_DATABASE());
    Assertions.assertEquals(
        applicationProperties.SERVERPACKCREATOR_DATABASE(), new File("serverpackcreator.db"));

    Assertions.assertNotNull(applicationProperties.MINECRAFT_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.MINECRAFT_VERSION_MANIFEST_LOCATION(),
        new File("./work/minecraft-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FORGE_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FORGE_VERSION_MANIFEST_LOCATION(),
        new File("./work/forge-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FABRIC_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FABRIC_VERSION_MANIFEST_LOCATION(),
        new File("./work/fabric-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./work/fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.QUILT_VERSION_MANIFEST_LOCATION(),
        new File("./work/quilt-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./work/quilt-installer-manifest.xml"));
  }

  @Test
  void getDirectoryServerPacksTest() {
    Assertions.assertNotNull(applicationProperties.getDirectoryServerPacks());
  }

  @Test
  void getListOfDirectoriesToExcludeTest() {
    Assertions.assertNotNull(applicationProperties.getDirectoriesToExclude());
    applicationProperties.addDirectoryToExclude("test");
    Assertions.assertTrue(applicationProperties.getDirectoriesToExclude().contains("test"));
  }

  @Test
  void booleanTests() {
    Assertions.assertFalse(applicationProperties.getSaveLoadedConfiguration());
    Assertions.assertFalse(applicationProperties.checkForAvailablePreReleases());
  }

  @Test
  void queueMaxUsageTest() {
    Assertions.assertEquals(90, applicationProperties.getQueueMaxDiskUsage());
  }

  @Test
  void getServerPackCreatorVersionTest() {
    Assertions.assertEquals("dev", applicationProperties.SERVERPACKCREATOR_VERSION());
  }
}
