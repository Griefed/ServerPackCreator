package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.ApplicationProperties.ExclusionFilter;
import de.griefed.serverpackcreator.utilities.common.FileUtilities;
import de.griefed.serverpackcreator.utilities.common.ListUtilities;
import de.griefed.serverpackcreator.utilities.common.SystemUtilities;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPropertiesTest {

  private final String fallback = "3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedCompas-,AdvancementPlaques-,Ambience,AmbientEnvironment-,AmbientSounds_,Armor Status HUD-,ArmorSoundTweak-,AromaBackup-,BH-Menu-,Backups-,Batty's Coordinates PLUS Mod,BetterAdvancements-,BetterAnimationsCollection-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,Blur-,BorderlessWindow-,CTM-,ChunkAnimator-,ClientTweaks_,Controller Support-,Controlling-,CraftPresence-,CustomCursorMod-,CustomMainMenu-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,DetailArmorBar-,Ding-,DistantHorizons-,DripSounds-,Durability101-,DurabilityNotifier-,DynamicSurroundings-,DynamicSurroundingsHuds-,EffectsLeft-,EiraMoticons_,EnchantmentDescriptions-,EnhancedVisuals_,EquipmentCompare-,FPS-Monitor-,FabricCustomCursorMod-,Fallingleaves-,FancySpawnEggs,FancyVideo-API-,FirstPersonMod,FogTweaker-,ForgeCustomCursorMod-,FpsReducer-,FpsReducer2-,FullscreenWindowed-,GameMenuModOption-,HealthOverlay-,HorseStatsMod-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,InventorySpam-,InventoryTweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,JBRA-Client-,JustEnoughCalculation-,JustEnoughEffects-,JustEnoughProfessions-,JustEnoughResources-,LLOverlayReloaded-,LOTRDRP-,LegendaryTooltips,LegendaryTooltips-,LightOverlay-,MoBends,MouseTweaks-,MyServerIsCompatible-,Neat ,Neat-,NekosEnchantedBooks-,NoAutoJump-,NoFog-,Notes-,NotifMod-,OldJavaWarning-,OptiFine,OptiForge,OptiForge-,PackMenu-,PackModeMenu-,PickUpNotifier-,Ping-,PresenceFootsteps-,RPG-HUD-,ReAuth-,ResourceLoader-,ResourcePackOrganizer,ShoulderSurfing-,ShulkerTooltip-,SimpleBackups-,SimpleDiscordRichPresence-,SimpleWorldTimer-,SoundFilters-,SpawnerFix-,TRansliterationLib-,TipTheScales-,Tips-,Toast Control-,Toast-Control-,ToastControl-,TravelersTitles-,VoidFog-,WindowedFullscreen-,WorldNameRandomizer-,[1.12.2]DamageIndicatorsMod-,[1.12.2]bspkrscore-,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,authme-,auto-reconnect-,autobackup-,autoreconnect-,axolotl-item-fix-,backtools-,bannerunlimited-,beenfo-1.19-,better-recipe-book-,betterbiomeblend-,bhmenu-,blur-,borderless-mining-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,combat_music-,configured-,controllable-,cullleaves-,cullparticles-,custom-crosshair-mod-,customdiscordrpc-,darkness-,dashloader-,defaultoptions-,desiredservers-,discordrpc-,drippyloadingscreen-,drippyloadingscreen_,dynamic-fps-,dynamic-music-,dynamiclights-,dynmus-,effective-,eggtab-,eguilib-,eiramoticons-,enchantment-lore-,entity-texture-features-,entityculling-,exhaustedstamina-,extremesoundmuffler-,fabricemotes-,fancymenu_,fancymenu_video_extension,findme-,flickerfix-,fm_audio_extension_,forgemod_VoxelMap-,freelook-,ftb-backups-,ftbbackups2-,galacticraft-rpc-,gamestagesviewer-,grid-,hiddenrecipebook_,infinitemusic-,inventoryprofiles,invtweaks-,itemzoom,itlt-,jeed-,jehc-,jeiintegration_,just-enough-harvestcraft-,justenoughbeacons-,justenoughdrags-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,light-overlay-,lightfallclient-,loadmyresources_,lock_minecart_view-,lootbeams-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,mousewheelie-,movement-vision-,multihotbar-,music-duration-reducer-,musicdr-,neiRecipeHandlers-,ngrok-lan-expose-mod-,nopotionshift_,notenoughanimations-,oculus-,openbackup-,ornaments-,overloadedarmorbar-,panorama-,paperdoll-,phosphor-,preciseblockplacing-,realm-of-lost-souls-,rebrand-,replanter-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simple-rpc-,simpleautorun-,simplebackup-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,soundreloader-,spoticraft-,tconplanner-,textile_backup-,timestamps-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,wisla-,xlifeheartcolors-,yisthereautojump-";
  private final String fallbackRegex = "^3dskinlayers-.*$,^Absolutely-Not-A-Zoom-Mod-.*$,^AdvancedChat-.*$,^AdvancedCompas-.*$,^AdvancementPlaques-.*$,^Ambience.*$,^AmbientEnvironment-.*$,^AmbientSounds_.*$,^Armor Status HUD-.*$,^ArmorSoundTweak-.*$,^AromaBackup-.*$,^BH-Menu-.*$,^Backups-.*$,^Batty's Coordinates PLUS Mod.*$,^BetterAdvancements-.*$,^BetterAnimationsCollection-.*$,^BetterDarkMode-.*$,^BetterF3-.*$,^BetterFoliage-.*$,^BetterPingDisplay-.*$,^BetterPlacement-.*$,^BetterTaskbar-.*$,^BetterThirdPerson.*$,^BetterTitleScreen-.*$,^BorderlessWindow-.*$,^CTM-.*$,^ChunkAnimator-.*$,^ClientTweaks_.*$,^Controller Support-.*$,^Controlling-.*$,^CraftPresence-.*$,^CustomCursorMod-.*$,^CustomMainMenu-.*$,^DefaultOptions_.*$,^DefaultSettings-.*$,^DeleteWorldsToTrash-.*$,^DetailArmorBar-.*$,^Ding-.*$,^DistantHorizons-.*$,^DripSounds-.*$,^Durability101-.*$,^DurabilityNotifier-.*$,^DynamicSurroundings-.*$,^DynamicSurroundingsHuds-.*$,^EffectsLeft-.*$,^EiraMoticons_.*$,^EnchantmentDescriptions-.*$,^EnhancedVisuals_.*$,^EquipmentCompare-.*$,^FPS-Monitor-.*$,^FabricCustomCursorMod-.*$,^Fallingleaves-.*$,^FancySpawnEggs.*$,^FancyVideo-API-.*$,^FirstPersonMod.*$,^FogTweaker-.*$,^ForgeCustomCursorMod-.*$,^FpsReducer-.*$,^FpsReducer2-.*$,^FullscreenWindowed-.*$,^GameMenuModOption-.*$,^HealthOverlay-.*$,^HorseStatsMod-.*$,^InventoryEssentials_.*$,^InventoryHud_[1.17.1].forge-.*$,^InventorySpam-.*$,^InventoryTweaks-.*$,^ItemBorders-.*$,^ItemPhysicLite_.*$,^ItemStitchingFix-.*$,^JBRA-Client-.*$,^JustEnoughCalculation-.*$,^JustEnoughEffects-.*$,^JustEnoughProfessions-.*$,^JustEnoughResources-.*$,^LLOverlayReloaded-.*$,^LOTRDRP-.*$,^LegendaryTooltips-.*$,^LegendaryTooltips.*$,^LightOverlay-.*$,^MoBends.*$,^MouseTweaks-.*$,^MyServerIsCompatible-.*$,^Neat .*$,^Neat-.*$,^NekosEnchantedBooks-.*$,^NoAutoJump-.*$,^NoFog-.*$,^Notes-.*$,^NotifMod-.*$,^OldJavaWarning-.*$,^OptiFine.*$,^OptiForge-.*$,^OptiForge.*$,^PackMenu-.*$,^PackModeMenu-.*$,^PickUpNotifier-.*$,^Ping-.*$,^PresenceFootsteps-.*$,^RPG-HUD-.*$,^ReAuth-.*$,^ResourceLoader-.*$,^ResourcePackOrganizer.*$,^ShoulderSurfing-.*$,^ShulkerTooltip-.*$,^SimpleBackups-.*$,^SimpleDiscordRichPresence-.*$,^SimpleWorldTimer-.*$,^SoundFilters-.*$,^SpawnerFix-.*$,^TRansliterationLib-.*$,^TipTheScales-.*$,^Tips-.*$,^Toast Control-.*$,^Toast-Control-.*$,^ToastControl-.*$,^TravelersTitles-.*$,^VoidFog-.*$,^WindowedFullscreen-.*$,^WorldNameRandomizer-.*$,^[1.12.2]DamageIndicatorsMod-.*$,^[1.12.2]bspkrscore-.*$,^antighost-.*$,^anviltooltipmod-.*$,^appleskin-.*$,^armorchroma-.*$,^armorpointspp-.*$,^authme-.*$,^auto-reconnect-.*$,^autobackup-.*$,^autoreconnect-.*$,^axolotl-item-fix-.*$,^backtools-.*$,^bannerunlimited-.*$,^beenfo-1.19-.*$,^better-recipe-book-.*$,^betterbiomeblend-.*$,^bhmenu-.*$,^blur-.*$,^borderless-mining-.*$,^catalogue-.*$,^charmonium-.*$,^chat_heads-.*$,^cherishedworlds-.*$,^cirback-1.0-.*$,^classicbar-.*$,^clickadv-.*$,^clienttweaks-.*$,^combat_music-.*$,^configured-.*$,^controllable-.*$,^cullleaves-.*$,^cullparticles-.*$,^custom-crosshair-mod-.*$,^customdiscordrpc-.*$,^darkness-.*$,^dashloader-.*$,^defaultoptions-.*$,^desiredservers-.*$,^discordrpc-.*$,^drippyloadingscreen-.*$,^drippyloadingscreen_.*$,^dynamic-fps-.*$,^dynamic-music-.*$,^dynamiclights-.*$,^dynmus-.*$,^effective-.*$,^eggtab-.*$,^eguilib-.*$,^eiramoticons-.*$,^enchantment-lore-.*$,^entity-texture-features-.*$,^entityculling-.*$,^exhaustedstamina-.*$,^extremesoundmuffler-.*$,^fabricemotes-.*$,^fancymenu_.*$,^fancymenu_video_extension.*$,^findme-.*$,^flickerfix-.*$,^fm_audio_extension_.*$,^forgemod_VoxelMap-.*$,^freelook-.*$,^ftb-backups-.*$,^ftbbackups2-.*$,^galacticraft-rpc-.*$,^gamestagesviewer-.*$,^grid-.*$,^hiddenrecipebook_.*$,^infinitemusic-.*$,^inventoryprofiles.*$,^invtweaks-.*$,^itemzoom.*$,^itlt-.*$,^jeed-.*$,^jehc-.*$,^jeiintegration_.*$,^just-enough-harvestcraft-.*$,^justenoughbeacons-.*$,^justenoughdrags-.*$,^justzoom_.*$,^keymap-.*$,^keywizard-.*$,^konkrete_.*$,^konkrete_forge_.*$,^lazydfu-.*$,^light-overlay-.*$,^lightfallclient-.*$,^loadmyresources_.*$,^lock_minecart_view-.*$,^lootbeams-.*$,^lwl-.*$,^magnesium_extras-.*$,^maptooltip-.*$,^massunbind.*$,^mcbindtype-.*$,^mcwifipnp-.*$,^medievalmusic-.*$,^mightyarchitect-.*$,^mindful-eating-.*$,^minetogether-.*$,^mobplusplus-.*$,^modcredits-.*$,^modernworldcreation_.*$,^modmenu-.*$,^modnametooltip-.*$,^modnametooltip_.*$,^moreoverlays-.*$,^mousewheelie-.*$,^movement-vision-.*$,^multihotbar-.*$,^music-duration-reducer-.*$,^musicdr-.*$,^neiRecipeHandlers-.*$,^ngrok-lan-expose-mod-.*$,^nopotionshift_.*$,^notenoughanimations-.*$,^oculus-.*$,^openbackup-.*$,^ornaments-.*$,^overloadedarmorbar-.*$,^panorama-.*$,^paperdoll-.*$,^phosphor-.*$,^preciseblockplacing-.*$,^realm-of-lost-souls-.*$,^rebrand-.*$,^replanter-.*$,^rubidium-.*$,^rubidium_extras-.*$,^screenshot-to-clipboard-.*$,^shutupexperimentalsettings-.*$,^shutupmodelloader-.*$,^signtools-.*$,^simple-rpc-.*$,^simpleautorun-.*$,^simplebackup-.*$,^smartcursor-.*$,^smoothboot-.*$,^smoothfocus-.*$,^sounddeviceoptions-.*$,^soundreloader-.*$,^spoticraft-.*$,^tconplanner-.*$,^textile_backup-.*$,^timestamps-.*$,^tooltipscroller-.*$,^torchoptimizer-.*$,^torohealth-.*$,^totaldarkness.*$,^toughnessbar-.*$,^wisla-.*$,^xlifeheartcolors-.*$,^yisthereautojump-.*$";
  private final List<String> listFallback = new ArrayList<>(Arrays.asList(fallback.split(",")));
  private final List<String> listFallbackRegex = new ArrayList<>(
      Arrays.asList(fallbackRegex.split(",")));
  private final FileUtilities fileUtilities = new FileUtilities();
  private final SystemUtilities systemUtilities = new SystemUtilities();
  private final ListUtilities listUtilities = new ListUtilities();

  ApplicationPropertiesTest() {

  }

  @Test
  void test() {
    ApplicationProperties applicationProperties;

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/contains.properties"),
        fileUtilities, systemUtilities,listUtilities);

    Assertions.assertEquals(ExclusionFilter.CONTAIN, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/either.properties"),
        fileUtilities, systemUtilities,listUtilities);

    Assertions.assertEquals(ExclusionFilter.EITHER, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/end.properties"),
        fileUtilities, systemUtilities,listUtilities);

    Assertions.assertEquals(ExclusionFilter.END, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/regex.properties"),
        fileUtilities, systemUtilities,listUtilities);

    Assertions.assertEquals(ExclusionFilter.REGEX, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/start.properties"),
        fileUtilities, systemUtilities,listUtilities);

    Assertions.assertEquals(ExclusionFilter.START, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/serverpackcreator.properties"), fileUtilities,
        systemUtilities,listUtilities);

    Assertions.assertNotNull(applicationProperties.java());
    Assertions.assertTrue(new File(applicationProperties.java()).exists());

    Assertions.assertNotNull(applicationProperties.FALLBACK_CLIENTSIDE_MODS());
    Assertions.assertEquals(applicationProperties.FALLBACK_CLIENTSIDE_MODS(), listFallback);
    Assertions.assertEquals(applicationProperties.getListFallbackMods(), listFallback);

    Assertions.assertNotNull(applicationProperties.FALLBACK_REGEX_CLIENTSIDE_MODS());
    Assertions.assertEquals(applicationProperties.FALLBACK_REGEX_CLIENTSIDE_MODS(),
        listFallbackRegex);
    Assertions.assertEquals(applicationProperties.getListFallbackModsRegex(), listFallbackRegex);

    Assertions.assertNotNull(applicationProperties.getDefaultListFallbackMods());
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

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_GAME_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_GAME_MANIFEST(),
        new File("legacy-fabric-game-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST(),
        new File("legacy-fabric-loader-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST(),
        new File("legacy-fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST(),
        new File("fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_GAME_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_GAME_MANIFEST_LOCATION(),
        new File("./manifests/legacy-fabric-game-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST_LOCATION(),
        new File("./manifests/legacy-fabric-loader-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION(),
        new File("./manifests/legacy-fabric-installer-manifest.xml"));

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
        new File("./manifests/minecraft-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FORGE_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FORGE_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/forge-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FABRIC_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FABRIC_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/fabric-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.QUILT_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/quilt-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/quilt-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.getDirectoryServerPacks());

    Assertions.assertNotNull(applicationProperties.getDirectoriesToExclude());
    applicationProperties.addDirectoryToExclude("test");
    Assertions.assertTrue(applicationProperties.getDirectoriesToExclude().contains("test"));

    Assertions.assertFalse(applicationProperties.getSaveLoadedConfiguration());
    Assertions.assertFalse(applicationProperties.checkForAvailablePreReleases());

    Assertions.assertEquals(90, applicationProperties.getQueueMaxDiskUsage());

    Assertions.assertEquals("dev", applicationProperties.SERVERPACKCREATOR_VERSION());
  }
}
