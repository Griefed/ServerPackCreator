package de.griefed.serverpackcreator.utilities.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListUtilitiesTest {

  ListUtilities listUtilities;

  ListUtilitiesTest() {
    listUtilities = new ListUtilities();
  }

  @Test
  void encapsulateListElementsTest() {
    List<String> clientMods =
        new ArrayList<>(
            Arrays.asList(
                "A[mbient]Sounds",
                "Back[Tools",
                "Bett[er[][]Advancement",
                "Bett   erPing",
                "cheri[ ]shed",
                "ClientT&/$weaks",
                "Control§!%(?=)ling",
                "Defau/()&=?ltOptions",
                "durabi!§/&?lity",
                "DynamicS[]urroundings",
                "itemz\\oom",
                "jei-/($?professions",
                "jeiinteg}][ration",
                "JustEnoughResources",
                "MouseTweaks",
                "Neat",
                "OldJavaWarning",
                "PackMenu",
                "preciseblockplacing",
                "SimpleDiscordRichPresence",
                "SpawnerFix",
                "TipTheScales",
                "WorldNameRandomizer"));
    System.out.println(listUtilities.encapsulateListElements(clientMods));
    Assertions.assertNotNull(listUtilities.encapsulateListElements(clientMods));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"A[mbient]Sounds\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Back[Tools\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Bett[er[][]Advancement\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Bett   erPing\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"cheri[ ]shed\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"ClientT&/$weaks\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Control§!%(?=)ling\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Defau/()&=?ltOptions\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"durabi!§/&?lity\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"DynamicS[]urroundings\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"itemz/oom\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"jei-/($?professions\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"jeiinteg}][ration\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"JustEnoughResources\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"MouseTweaks\""));
    Assertions.assertTrue(listUtilities.encapsulateListElements(clientMods).contains("\"Neat\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"OldJavaWarning\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"PackMenu\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"preciseblockplacing\""));
    Assertions.assertTrue(
        listUtilities
            .encapsulateListElements(clientMods)
            .contains("\"SimpleDiscordRichPresence\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"SpawnerFix\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"WorldNameRandomizer\""));
  }

  @Test
  void cleanListTest() {
    Assertions.assertEquals(
        1,
        listUtilities
            .cleanList(
                new ArrayList<>(
                    Arrays.asList(
                        "", " ", "  ", "   ", "    ", "     ", "", "", "", "", " ", "hamlo")))
            .size());
  }

  @Test
  void chunkyTest() {
    List<String> mods = new ArrayList<>(
        Arrays.asList(
            ("Armor Status HUD-,[1.12.2]bspkrscore-,[1.12.2]DamageIndicatorsMod-,3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedCompas-,AdvancementPlaques-,Ambience,AmbientEnvironment-,AmbientSounds_,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,ArmorSoundTweak-,AromaBackup-,authme-,autobackup-,autoreconnect-,auto-reconnect-,axolotl-item-fix-,backtools-,Backups-,bannerunlimited-,Batty's Coordinates PLUS Mod,beenfo-1.19-,BetterAdvancements-,BetterAnimationsCollection-,betterbiomeblend-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,better-recipe-book-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,bhmenu-,BH-Menu-,blur-,Blur-,borderless-mining-,BorderlessWindow-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,ChunkAnimator-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,ClientTweaks_,combat_music-,configured-,controllable-,Controller Support-,Controlling-,CraftPresence-,CTM-,cullleaves-,cullparticles-,custom-crosshair-mod-,CustomCursorMod-,customdiscordrpc-,CustomMainMenu-,darkness-,dashloader-,defaultoptions-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,desiredservers-,DetailArmorBar-,Ding-,discordrpc-,DistantHorizons-,drippyloadingscreen-,drippyloadingscreen_,DripSounds-,Durability101-,DurabilityNotifier-,dynamic-fps-,dynamiclights-,dynamic-music-,DynamicSurroundings-,DynamicSurroundingsHuds-,dynmus-,effective-,EffectsLeft-,eggtab-,eguilib-,eiramoticons-,EiraMoticons_,EnchantmentDescriptions-,enchantment-lore-,EnhancedVisuals_,entityculling-,entity-texture-features-,EquipmentCompare-,exhaustedstamina-,extremesoundmuffler-,FabricCustomCursorMod-,fabricemotes-,Fallingleaves-,fancymenu_,fancymenu_video_extension,FancySpawnEggs,FancyVideo-API-,findme-,FirstPersonMod,flickerfix-,fm_audio_extension_,FogTweaker-,ForgeCustomCursorMod-,forgemod_VoxelMap-,FPS-Monitor-,FpsReducer-,FpsReducer2-,freelook-,ftb-backups-,ftbbackups2-,FullscreenWindowed-,galacticraft-rpc-,GameMenuModOption-,gamestagesviewer-,grid-,HealthOverlay-,hiddenrecipebook_,HorseStatsMod-,infinitemusic-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,inventoryprofiles,InventorySpam-,InventoryTweaks-,invtweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,itemzoom,itlt-,JBRA-Client-,jeed-,jehc-,jeiintegration_,justenoughbeacons-,JustEnoughCalculation-,justenoughdrags-,JustEnoughEffects-,just-enough-harvestcraft-,JustEnoughProfessions-,JustEnoughResources-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,LegendaryTooltips,LegendaryTooltips-,lightfallclient-,LightOverlay-,light-overlay-,LLOverlayReloaded-,loadmyresources_,lock_minecart_view-,lootbeams-,LOTRDRP-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,MoBends,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,MouseTweaks-,mousewheelie-,movement-vision-,multihotbar-,musicdr-,music-duration-reducer-,MyServerIsCompatible-,Neat-,Neat ,neiRecipeHandlers-,NekosEnchantedBooks-,ngrok-lan-expose-mod-,NoAutoJump-,NoFog-,nopotionshift_,notenoughanimations-,Notes-,NotifMod-,oculus-,OldJavaWarning-,openbackup-,OptiFine,OptiForge,OptiForge-,ornaments-,overloadedarmorbar-,PackMenu-,PackModeMenu-,panorama-,paperdoll-,phosphor-,PickUpNotifier-,Ping-,preciseblockplacing-,PresenceFootsteps-,realm-of-lost-souls-,ReAuth-,rebrand-,replanter-,ResourceLoader-,ResourcePackOrganizer,RPG-HUD-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,ShoulderSurfing-,ShulkerTooltip-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simpleautorun-,simplebackup-,SimpleBackups-,SimpleDiscordRichPresence-,simple-rpc-,SimpleWorldTimer-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,SoundFilters-,soundreloader-,SpawnerFix-,spoticraft-,tconplanner-,textile_backup-,timestamps-,Tips-,TipTheScales-,Toast Control-,ToastControl-,Toast-Control-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,TRansliterationLib-,TravelersTitles-,VoidFog-,WindowedFullscreen-,wisla-,WorldNameRandomizer-,xlifeheartcolors-,yisthereautojump-")
                .split(",")));

    listUtilities.printListToConsoleChunked(mods, 5, "    ", true);
    listUtilities.printListToLogChunked(mods,5,"    ",true);
  }
}
