package de.griefed.serverpackcreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPropertiesTest {

  ApplicationPropertiesTest() {

  }

  @Test
  void test() {
    ApplicationProperties applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/serverpackcreator.properties"));

    Assertions.assertNotNull(applicationProperties.FALLBACK_CLIENTSIDE_MODS());
    Assertions.assertEquals(
        applicationProperties.FALLBACK_CLIENTSIDE_MODS(),
        new ArrayList<>(
            Arrays.asList(
                ("Armor Status HUD-,[1.12.2]bspkrscore-,[1.12.2]DamageIndicatorsMod-,3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedCompas-,AdvancementPlaques-,Ambience,AmbientEnvironment-,AmbientSounds_,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,ArmorSoundTweak-,AromaBackup-,authme-,autobackup-,autoreconnect-,auto-reconnect-,axolotl-item-fix-,backtools-,Backups-,bannerunlimited-,Batty's Coordinates PLUS Mod,beenfo-1.19-,BetterAdvancements-,BetterAnimationsCollection-,betterbiomeblend-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,better-recipe-book-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,bhmenu-,BH-Menu-,blur-,Blur-,borderless-mining-,BorderlessWindow-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,ChunkAnimator-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,ClientTweaks_,combat_music-,configured-,controllable-,Controller Support-,Controlling-,CraftPresence-,CTM-,cullleaves-,cullparticles-,custom-crosshair-mod-,CustomCursorMod-,customdiscordrpc-,CustomMainMenu-,darkness-,dashloader-,defaultoptions-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,desiredservers-,DetailArmorBar-,Ding-,discordrpc-,DistantHorizons-,drippyloadingscreen-,drippyloadingscreen_,DripSounds-,Durability101-,DurabilityNotifier-,dynamic-fps-,dynamiclights-,dynamic-music-,DynamicSurroundings-,DynamicSurroundingsHuds-,dynmus-,effective-,EffectsLeft-,eggtab-,eguilib-,eiramoticons-,EiraMoticons_,EnchantmentDescriptions-,enchantment-lore-,EnhancedVisuals_,entityculling-,entity-texture-features-,EquipmentCompare-,exhaustedstamina-,extremesoundmuffler-,FabricCustomCursorMod-,fabricemotes-,Fallingleaves-,fancymenu_,fancymenu_video_extension,FancySpawnEggs,FancyVideo-API-,findme-,FirstPersonMod,flickerfix-,fm_audio_extension_,FogTweaker-,ForgeCustomCursorMod-,forgemod_VoxelMap-,FPS-Monitor-,FpsReducer-,FpsReducer2-,freelook-,ftb-backups-,ftbbackups2-,FullscreenWindowed-,galacticraft-rpc-,GameMenuModOption-,gamestagesviewer-,grid-,HealthOverlay-,hiddenrecipebook_,HorseStatsMod-,infinitemusic-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,inventoryprofiles,InventorySpam-,InventoryTweaks-,invtweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,itemzoom,itlt-,JBRA-Client-,jeed-,jehc-,jeiintegration_,justenoughbeacons-,JustEnoughCalculation-,justenoughdrags-,JustEnoughEffects-,just-enough-harvestcraft-,JustEnoughProfessions-,JustEnoughResources-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,LegendaryTooltips,LegendaryTooltips-,lightfallclient-,LightOverlay-,light-overlay-,LLOverlayReloaded-,loadmyresources_,lock_minecart_view-,lootbeams-,LOTRDRP-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,MoBends,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,MouseTweaks-,mousewheelie-,movement-vision-,multihotbar-,musicdr-,music-duration-reducer-,MyServerIsCompatible-,Neat-,Neat ,neiRecipeHandlers-,NekosEnchantedBooks-,ngrok-lan-expose-mod-,NoAutoJump-,NoFog-,nopotionshift_,notenoughanimations-,Notes-,NotifMod-,oculus-,OldJavaWarning-,openbackup-,OptiFine,OptiForge,OptiForge-,ornaments-,overloadedarmorbar-,PackMenu-,PackModeMenu-,panorama-,paperdoll-,phosphor-,PickUpNotifier-,Ping-,preciseblockplacing-,PresenceFootsteps-,realm-of-lost-souls-,ReAuth-,rebrand-,replanter-,ResourceLoader-,ResourcePackOrganizer,RPG-HUD-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,ShoulderSurfing-,ShulkerTooltip-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simpleautorun-,simplebackup-,SimpleBackups-,SimpleDiscordRichPresence-,simple-rpc-,SimpleWorldTimer-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,SoundFilters-,soundreloader-,SpawnerFix-,spoticraft-,tconplanner-,textile_backup-,timestamps-,Tips-,TipTheScales-,Toast Control-,ToastControl-,Toast-Control-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,TRansliterationLib-,TravelersTitles-,VoidFog-,WindowedFullscreen-,wisla-,WorldNameRandomizer-,xlifeheartcolors-,yisthereautojump-")
                    .split(","))));

    Assertions.assertNotNull(applicationProperties.FALLBACK_REGEX_CLIENTSIDE_MODS());
    Assertions.assertEquals(
        applicationProperties.FALLBACK_REGEX_CLIENTSIDE_MODS(),
        new ArrayList<>(
            Arrays.asList(
                ("^Armor Status HUD-.*$,^[1.12.2]bspkrscore-.*$,^[1.12.2]DamageIndicatorsMod-.*$,^3dskinlayers-.*$,^Absolutely-Not-A-Zoom-Mod-.*$,^AdvancedChat-.*$,^AdvancedCompas-.*$,^AdvancementPlaques-.*$,^Ambience.*$,^AmbientEnvironment-.*$,^AmbientSounds_.*$,^antighost-.*$,^anviltooltipmod-.*$,^appleskin-.*$,^armorchroma-.*$,^armorpointspp-.*$,^ArmorSoundTweak-.*$,^AromaBackup-.*$,^authme-.*$,^autobackup-.*$,^autoreconnect-.*$,^auto-reconnect-.*$,^axolotl-item-fix-.*$,^backtools-.*$,^Backups-.*$,^bannerunlimited-.*$,^Batty's Coordinates PLUS Mod.*$,^beenfo-1.19-.*$,^BetterAdvancements-.*$,^BetterAnimationsCollection-.*$,^betterbiomeblend-.*$,^BetterDarkMode-.*$,^BetterF3-.*$,^BetterFoliage-.*$,^BetterPingDisplay-.*$,^BetterPlacement-.*$,^better-recipe-book-.*$,^BetterTaskbar-.*$,^BetterThirdPerson.*$,^BetterTitleScreen-.*$,^bhmenu-.*$,^BH-Menu-.*$,^blur-.*$,^borderless-mining-.*$,^BorderlessWindow-.*$,^catalogue-.*$,^charmonium-.*$,^chat_heads-.*$,^cherishedworlds-.*$,^ChunkAnimator-.*$,^cirback-1.0-.*$,^classicbar-.*$,^clickadv-.*$,^clienttweaks-.*$,^ClientTweaks_.*$,^combat_music-.*$,^configured-.*$,^controllable-.*$,^Controller Support-.*$,^Controlling-.*$,^CraftPresence-.*$,^CTM-.*$,^cullleaves-.*$,^cullparticles-.*$,^custom-crosshair-mod-.*$,^CustomCursorMod-.*$,^customdiscordrpc-.*$,^CustomMainMenu-.*$,^darkness-.*$,^dashloader-.*$,^defaultoptions-.*$,^DefaultOptions_.*$,^DefaultSettings-.*$,^DeleteWorldsToTrash-.*$,^desiredservers-.*$,^DetailArmorBar-.*$,^Ding-.*$,^discordrpc-.*$,^DistantHorizons-.*$,^drippyloadingscreen-.*$,^drippyloadingscreen_.*$,^DripSounds-.*$,^Durability101-.*$,^DurabilityNotifier-.*$,^dynamic-fps-.*$,^dynamiclights-.*$,^dynamic-music-.*$,^DynamicSurroundings-.*$,^DynamicSurroundingsHuds-.*$,^dynmus-.*$,^effective-.*$,^EffectsLeft-.*$,^eggtab-.*$,^eguilib-.*$,^eiramoticons-.*$,^EiraMoticons_.*$,^EnchantmentDescriptions-.*$,^enchantment-lore-.*$,^EnhancedVisuals_.*$,^entityculling-.*$,^entity-texture-features-.*$,^EquipmentCompare-.*$,^exhaustedstamina-.*$,^extremesoundmuffler-.*$,^FabricCustomCursorMod-.*$,^fabricemotes-.*$,^Fallingleaves-.*$,^fancymenu_.*$,^fancymenu_video_extension.*$,^FancySpawnEggs.*$,^FancyVideo-API-.*$,^findme-.*$,^FirstPersonMod.*$,^flickerfix-.*$,^fm_audio_extension_.*$,^FogTweaker-.*$,^ForgeCustomCursorMod-.*$,^forgemod_VoxelMap-.*$,^FPS-Monitor-.*$,^FpsReducer-.*$,^FpsReducer2-.*$,^freelook-.*$,^ftb-backups-.*$,^ftbbackups2-.*$,^FullscreenWindowed-.*$,^galacticraft-rpc-.*$,^GameMenuModOption-.*$,^gamestagesviewer-.*$,^grid-.*$,^HealthOverlay-.*$,^hiddenrecipebook_.*$,^HorseStatsMod-.*$,^infinitemusic-.*$,^InventoryEssentials_.*$,^InventoryHud_[1.17.1].forge-.*$,^inventoryprofiles.*$,^InventorySpam-.*$,^InventoryTweaks-.*$,^invtweaks-.*$,^ItemBorders-.*$,^ItemPhysicLite_.*$,^ItemStitchingFix-.*$,^itemzoom.*$,^itlt-.*$,^JBRA-Client-.*$,^jeed-.*$,^jehc-.*$,^jeiintegration_.*$,^justenoughbeacons-.*$,^JustEnoughCalculation-.*$,^justenoughdrags-.*$,^JustEnoughEffects-.*$,^just-enough-harvestcraft-.*$,^JustEnoughProfessions-.*$,^JustEnoughResources-.*$,^justzoom_.*$,^keymap-.*$,^keywizard-.*$,^konkrete_.*$,^konkrete_forge_.*$,^lazydfu-.*$,^LegendaryTooltips.*$,^LegendaryTooltips-.*$,^lightfallclient-.*$,^LightOverlay-.*$,^light-overlay-.*$,^LLOverlayReloaded-.*$,^loadmyresources_.*$,^lock_minecart_view-.*$,^lootbeams-.*$,^LOTRDRP-.*$,^lwl-.*$,^magnesium_extras-.*$,^maptooltip-.*$,^massunbind.*$,^mcbindtype-.*$,^mcwifipnp-.*$,^medievalmusic-.*$,^mightyarchitect-.*$,^mindful-eating-.*$,^minetogether-.*$,^MoBends.*$,^mobplusplus-.*$,^modcredits-.*$,^modernworldcreation_.*$,^modmenu-.*$,^modnametooltip-.*$,^modnametooltip_.*$,^moreoverlays-.*$,^MouseTweaks-.*$,^mousewheelie-.*$,^movement-vision-.*$,^multihotbar-.*$,^musicdr-.*$,^music-duration-reducer-.*$,^MyServerIsCompatible-.*$,^Neat-.*$,^Neat .*$,^neiRecipeHandlers-.*$,^NekosEnchantedBooks-.*$,^ngrok-lan-expose-mod-.*$,^NoAutoJump-.*$,^NoFog-.*$,^nopotionshift_.*$,^notenoughanimations-.*$,^Notes-.*$,^NotifMod-.*$,^oculus-.*$,^OldJavaWarning-.*$,^openbackup-.*$,^OptiFine.*$,^OptiForge.*$,^OptiForge-.*$,^ornaments-.*$,^overloadedarmorbar-.*$,^PackMenu-.*$,^PackModeMenu-.*$,^panorama-.*$,^paperdoll-.*$,^phosphor-.*$,^PickUpNotifier-.*$,^Ping-.*$,^preciseblockplacing-.*$,^PresenceFootsteps-.*$,^realm-of-lost-souls-.*$,^ReAuth-.*$,^rebrand-.*$,^replanter-.*$,^ResourceLoader-.*$,^ResourcePackOrganizer.*$,^RPG-HUD-.*$,^rubidium-.*$,^rubidium_extras-.*$,^screenshot-to-clipboard-.*$,^ShoulderSurfing-.*$,^ShulkerTooltip-.*$,^shutupexperimentalsettings-.*$,^shutupmodelloader-.*$,^signtools-.*$,^simpleautorun-.*$,^simplebackup-.*$,^SimpleBackups-.*$,^SimpleDiscordRichPresence-.*$,^simple-rpc-.*$,^SimpleWorldTimer-.*$,^smartcursor-.*$,^smoothboot-.*$,^smoothfocus-.*$,^sounddeviceoptions-.*$,^SoundFilters-.*$,^soundreloader-.*$,^SpawnerFix-.*$,^spoticraft-.*$,^tconplanner-.*$,^textile_backup-.*$,^timestamps-.*$,^Tips-.*$,^TipTheScales-.*$,^Toast Control-.*$,^ToastControl-.*$,^Toast-Control-.*$,^tooltipscroller-.*$,^torchoptimizer-.*$,^torohealth-.*$,^totaldarkness.*$,^toughnessbar-.*$,^TRansliterationLib-.*$,^TravelersTitles-.*$,^VoidFog-.*$,^WindowedFullscreen-.*$,^wisla-.*$,^WorldNameRandomizer-.*$,^xlifeheartcolors-.*$,^yisthereautojump-.*$")
                    .split(","))));

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
