package de.griefed.serverpackcreator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ApplicationPropertiesTest {

    private final ApplicationProperties APPLICATIONPROPERTIES;

    ApplicationPropertiesTest() {
        this.APPLICATIONPROPERTIES = new ApplicationProperties();
    }

    @Test
    void finalsTest() {
        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_PROPERTIES);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_PROPERTIES,new File("serverpackcreator.properties"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_WINDOWS);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_WINDOWS,new File("start.bat"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_LINUX);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_LINUX,new File("start.sh"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS,new File("user_jvm_args.txt"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.LIST_FALLBACK_MODS_DEFAULT);
        Assertions.assertEquals(APPLICATIONPROPERTIES.LIST_FALLBACK_MODS_DEFAULT,new ArrayList<>(
            Arrays.asList((
                "3dSkinLayers-," +
                "AdvancementPlaques-," +
                "AmbientSounds_," +
                "armorchroma-," +
                "backtools-," +
                "BetterAdvancements-," +
                "BetterAnimationsCollection-," +
                "BetterDarkMode-," +
                "BetterF3-," +
                "BetterF3-," +
                "BetterFoliage-," +
                "BetterPingDisplay-," +
                "BetterPlacement-," +
                "Blur-," +
                "catalogue-," +
                "cherishedworlds-," +
                "classicbar-," +
                "clickadv-," +
                "ClientTweaks_," +
                "configured-," +
                "Controlling-," +
                "CraftPresence-," +
                "CTM-," +
                "customdiscordrpc-," +
                "CustomMainMenu-," +
                "DefaultOptions_," +
                "defaultoptions-," +
                "desiredservers-," +
                "Ding-," +
                "drippyloadingscreen_," +
                "drippyloadingscreen-," +
                "Durability101-," +
                "dynamic-music-," +
                "DynamicSurroundings-," +
                "DynamicSurroundingsHuds-," +
                "dynmus-," +
                "EiraMoticons_," +
                "eiramoticons-," +
                "EnchantmentDescriptions-," +
                "EquipmentCompare-," +
                "extremesoundmuffler-," +
                "extremeSoundMuffler-," +
                "Fallingleaves-," +
                "fallingleaves-," +
                "fancymenu_," +
                "findme-," +
                "flickerfix-," +
                "FpsReducer-," +
                "FullscreenWindowed-," +
                "InventoryEssentials_," +
                "InventorySpam-," +
                "InventoryTweaks-," +
                "invtweaks-," +
                "ItemBorders-," +
                "itemzoom," +
                "itlt-," +
                "jeed-," +
                "jehc-," +
                "jeiintegration_," +
                "JEITweaker-," +
                "just-enough-harvestcraft-," +
                "justenoughbeacons-," +
                "JustEnoughCalculation-," +
                "JustEnoughProfessions-," +
                "JustEnoughProfessions-," +
                "JustEnoughResources-," +
                "keywizard-," +
                "konkrete_," +
                "lazydfu-," +
                "LegendaryTooltips-," +
                "light-overlay-," +
                "LightOverlay-," +
                "LLOverlayReloaded-," +
                "loadmyresources_," +
                "lootbeams-," +
                "mcbindtype-," +
                "medievalmusic-," +
                "modnametooltip_," +
                "modnametooltip-," +
                "moreoverlays-," +
                "MouseTweaks-," +
                "multihotbar-," +
                "MyServerIsCompatible-," +
                "Neat ," +
                "NotifMod-," +
                "OldJavaWarning-," +
                "ornaments-," +
                "overloadedarmorbar-," +
                "PackMenu-," +
                "PickUpNotifier-," +
                "Ping-," +
                "preciseblockplacing-," +
                "presencefootsteps-," +
                "PresenceFootsteps-," +
                "ReAuth-," +
                "ResourceLoader-," +
                "shutupexperimentalsettings-," +
                "SimpleDiscordRichPresence-," +
                "smoothboot-," +
                "sounddeviceoptions-," +
                "SpawnerFix-," +
                "spoticraft-," +
                "tconplanner-," +
                "timestamps-," +
                "Tips-," +
                "TipTheScales-," +
                "Toast Control-," +
                "Toast-Control-," +
                "ToastControl-," +
                "torchoptimizer-," +
                "torohealth-," +
                "toughnessbar-," +
                "TravelersTitles-," +
                "WindowedFullscreen-," +
                "WorldNameRandomizer-," +
                "yisthereautojump-"
            ).split(","))));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_CONFIG);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_CONFIG,new File("serverpackcreator.conf"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_CONFIG_OLD);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_CONFIG_OLD,new File("creator.conf"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES,new File("server.properties"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_SERVER_ICON);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_SERVER_ICON,new File("server-icon.png"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT,new File("minecraft-manifest.json"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE,new File("forge-manifest.json"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC,new File("fabric-manifest.xml"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER,new File("fabric-installer-manifest.xml"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_DATABASE);
        Assertions.assertEquals(APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_DATABASE,new File ("serverpackcreator.db"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_MINECRAFT);
        Assertions.assertEquals(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_MINECRAFT,new File("./work/minecraft-manifest.json"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FORGE);
        Assertions.assertEquals(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FORGE,new File("./work/forge-manifest.json"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC);
        Assertions.assertEquals(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC,new File("./work/fabric-manifest.xml"));

        Assertions.assertNotNull(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER);
        Assertions.assertEquals(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER,new File("./work/fabric-installer-manifest.xml"));
    }

    @Test
    void getDirectoryServerPacksTest() {
        Assertions.assertNotNull(APPLICATIONPROPERTIES.getDirectoryServerPacks());
    }

    @Test
    void getListFallbackModsTest() {
        Assertions.assertNotNull(APPLICATIONPROPERTIES.getListFallbackMods());
    }

    @Test
    void getListOfDirectoriesToExcludeTest() {
        Assertions.assertNotNull(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude());
        APPLICATIONPROPERTIES.addToListOfDirectoriesToExclude("test");
        Assertions.assertTrue(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude().contains("test"));
    }

    @Test
    void getScheduleDatabaseCleanupTest() {
        Assertions.assertEquals(APPLICATIONPROPERTIES.getScheduleDatabaseCleanup(),"0 0 24 * *");
    }
}
