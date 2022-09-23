/* Copyright (C) 2022  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.utilities.common.FileUtilities;
import de.griefed.serverpackcreator.utilities.common.InvalidFileTypeException;
import de.griefed.serverpackcreator.utilities.common.ListUtilities;
import de.griefed.serverpackcreator.utilities.common.SystemUtilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Our properties-class. Extends {@link java.util.Properties}. Sets up default properties loaded
 * from the local serverpackcreator.properties and allows reloading of said properties if the file
 * has changed.
 *
 * @author Griefed
 */
@SuppressWarnings("FieldCanBeLocal")
@Component
public final class ApplicationProperties extends Properties {

  private static final Logger LOG = LogManager.getLogger(ApplicationProperties.class);
  private final FileUtilities FILE_UTILITIES;
  private final SystemUtilities SYSTEM_UTILITIES;
  private final ListUtilities LIST_UTILITIES;
  /**
   * Default properties.
   */
  private final String SERVERPACKCREATOR_PROPERTIES = "serverpackcreator.properties";
  /**
   * Default properties file.
   */
  private final File SERVERPACKCREATOR_PROPERTIES_FILE = new File(SERVERPACKCREATOR_PROPERTIES);
  /**
   * Default fallback clientside-only mods.
   */
  private final String FALLBACK_MODS_DEFAULT_ASSTRING =
      "3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedChatCore-,AdvancedChatHUD-,AdvancedCompas-,Ambience,AmbientEnvironment-,AmbientSounds_,AreYouBlind-,Armor Status HUD-,ArmorSoundTweak-,BH-Menu-,Batty's Coordinates PLUS Mod,BetterAdvancements-,BetterAnimationsCollection-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,Blur-,BorderlessWindow-,CTM-,ChunkAnimator-,ClientTweaks_,Controller Support-,Controlling-,CraftPresence-,CustomCursorMod-,CustomMainMenu-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,DetailArmorBar-,Ding-,DistantHorizons-,DripSounds-,Durability101-,DurabilityNotifier-,DynamicSurroundings-,DynamicSurroundingsHuds-,EffectsLeft-,EiraMoticons_,EnchantmentDescriptions-,EnhancedVisuals_,EquipmentCompare-,FPS-Monitor-,FabricCustomCursorMod-,Fallingleaves-,FancySpawnEggs,FancyVideo-API-,FirstPersonMod,FogTweaker-,ForgeCustomCursorMod-,FpsReducer-,FpsReducer2-,FullscreenWindowed-,GameMenuModOption-,HealthOverlay-,HorseStatsMod-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,InventorySpam-,InventoryTweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,JBRA-Client-,JustEnoughCalculation-,JustEnoughEffects-,JustEnoughProfessions-,JustEnoughResources-,LLOverlayReloaded-,LOTRDRP-,LegendaryTooltips,LegendaryTooltips-,LightOverlay-,MoBends,MouseTweaks-,MyServerIsCompatible-,Neat ,Neat-,NekosEnchantedBooks-,NoAutoJump-,NoFog-,Notes-,NotifMod-,OldJavaWarning-,OptiFine,OptiFine_,OptiForge,OptiForge-,PackMenu-,PackModeMenu-,PickUpNotifier-,Ping-,PresenceFootsteps-,RPG-HUD-,ReAuth-,ResourceLoader-,ResourcePackOrganizer,ShoulderSurfing-,ShulkerTooltip-,SimpleDiscordRichPresence-,SimpleWorldTimer-,SoundFilters-,SpawnerFix-,TRansliterationLib-,TipTheScales-,Tips-,Toast Control-,Toast-Control-,ToastControl-,TravelersTitles-,VoidFog-,WindowedFullscreen-,WorldNameRandomizer-,[1.12.2]DamageIndicatorsMod-,[1.12.2]bspkrscore-,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,auditory-,authme-,auto-reconnect-,autojoin-,autoreconnect-,axolotl-item-fix-,backtools-,bannerunlimited-,beenfo-1.19-,better-recipe-book-,betterbiomeblend-,bhmenu-,blur-,borderless-mining-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,combat_music-,configured-,controllable-,cullleaves-,cullparticles-,custom-crosshair-mod-,customdiscordrpc-,darkness-,dashloader-,defaultoptions-,desiredservers-,discordrpc-,drippyloadingscreen-,drippyloadingscreen_,dynamic-fps-,dynamic-music-,dynamiclights-,dynmus-,effective-,eggtab-,eguilib-,eiramoticons-,enchantment-lore-,entity-texture-features-,entityculling-,exhaustedstamina-,extremesoundmuffler-,fabricemotes-,fancymenu_,fancymenu_video_extension,findme-,flickerfix-,fm_audio_extension_,forgemod_VoxelMap-,freelook-,galacticraft-rpc-,gamestagesviewer-,grid-,helium-,hiddenrecipebook_,infinitemusic-,inventoryprofiles,invtweaks-,itemzoom,itlt-,jeed-,jehc-,jeiintegration_,just-enough-harvestcraft-,justenoughbeacons-,justenoughdrags-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,light-overlay-,lightfallclient-,loadmyresources_,lock_minecart_view-,lootbeams-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,mousewheelie-,movement-vision-,multihotbar-,music-duration-reducer-,musicdr-,neiRecipeHandlers-,ngrok-lan-expose-mod-,nopotionshift_,notenoughanimations-,oculus-,ornaments-,overloadedarmorbar-,panorama-,paperdoll-,phosphor-,preciseblockplacing-,realm-of-lost-souls-,rebrand-,replanter-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simple-rpc-,simpleautorun-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,soundreloader-,spoticraft-,tconplanner-,timestamps-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,wisla-,xlifeheartcolors-,yisthereautojump-";
  /**
   * Default fallback clientside-only mods list.
   */
  private final TreeSet<String> FALLBACK_CLIENTSIDE_MODS =
      new TreeSet<>(Arrays.asList(FALLBACK_MODS_DEFAULT_ASSTRING.split(",")));
  /**
   * List of mods which should be excluded from server packs.
   */
  private final TreeSet<String> FALLBACK_MODS = new TreeSet<>(FALLBACK_CLIENTSIDE_MODS);
  /**
   * Default fallback clientside-only mods, regex.
   */
  private final String FALLBACK_MODS_DEFAULT_REGEX_ASSTRING =
      "^3dskinlayers-.*$,^Absolutely-Not-A-Zoom-Mod-.*$,^AdvancedChat-.*$,^AdvancedChatCore-.*$,^AdvancedChatHUD-.*$,^AdvancedCompas-.*$,^Ambience.*$,^AmbientEnvironment-.*$,^AmbientSounds_.*$,^AreYouBlind-.*$,^Armor Status HUD-.*$,^ArmorSoundTweak-.*$,^BH-Menu-.*$,^Batty's Coordinates PLUS Mod.*$,^BetterAdvancements-.*$,^BetterAnimationsCollection-.*$,^BetterDarkMode-.*$,^BetterF3-.*$,^BetterFoliage-.*$,^BetterPingDisplay-.*$,^BetterPlacement-.*$,^BetterTaskbar-.*$,^BetterThirdPerson.*$,^BetterTitleScreen-.*$,^Blur-.*$,^BorderlessWindow-.*$,^CTM-.*$,^ChunkAnimator-.*$,^ClientTweaks_.*$,^Controller Support-.*$,^Controlling-.*$,^CraftPresence-.*$,^CustomCursorMod-.*$,^CustomMainMenu-.*$,^DefaultOptions_.*$,^DefaultSettings-.*$,^DeleteWorldsToTrash-.*$,^DetailArmorBar-.*$,^Ding-.*$,^DistantHorizons-.*$,^DripSounds-.*$,^Durability101-.*$,^DurabilityNotifier-.*$,^DynamicSurroundings-.*$,^DynamicSurroundingsHuds-.*$,^EffectsLeft-.*$,^EiraMoticons_.*$,^EnchantmentDescriptions-.*$,^EnhancedVisuals_.*$,^EquipmentCompare-.*$,^FPS-Monitor-.*$,^FabricCustomCursorMod-.*$,^Fallingleaves-.*$,^FancySpawnEggs.*$,^FancyVideo-API-.*$,^FirstPersonMod.*$,^FogTweaker-.*$,^ForgeCustomCursorMod-.*$,^FpsReducer-.*$,^FpsReducer2-.*$,^FullscreenWindowed-.*$,^GameMenuModOption-.*$,^HealthOverlay-.*$,^HorseStatsMod-.*$,^InventoryEssentials_.*$,^InventoryHud_[1.17.1].forge-.*$,^InventorySpam-.*$,^InventoryTweaks-.*$,^ItemBorders-.*$,^ItemPhysicLite_.*$,^ItemStitchingFix-.*$,^JBRA-Client-.*$,^JustEnoughCalculation-.*$,^JustEnoughEffects-.*$,^JustEnoughProfessions-.*$,^JustEnoughResources-.*$,^LLOverlayReloaded-.*$,^LOTRDRP-.*$,^LegendaryTooltips-.*$,^LegendaryTooltips.*$,^LightOverlay-.*$,^MoBends.*$,^MouseTweaks-.*$,^MyServerIsCompatible-.*$,^Neat .*$,^Neat-.*$,^NekosEnchantedBooks-.*$,^NoAutoJump-.*$,^NoFog-.*$,^Notes-.*$,^NotifMod-.*$,^OldJavaWarning-.*$,^OptiFine.*$,^OptiFine_.*$,^OptiForge-.*$,^OptiForge.*$,^PackMenu-.*$,^PackModeMenu-.*$,^PickUpNotifier-.*$,^Ping-.*$,^PresenceFootsteps-.*$,^RPG-HUD-.*$,^ReAuth-.*$,^ResourceLoader-.*$,^ResourcePackOrganizer.*$,^ShoulderSurfing-.*$,^ShulkerTooltip-.*$,^SimpleDiscordRichPresence-.*$,^SimpleWorldTimer-.*$,^SoundFilters-.*$,^SpawnerFix-.*$,^TRansliterationLib-.*$,^TipTheScales-.*$,^Tips-.*$,^Toast Control-.*$,^Toast-Control-.*$,^ToastControl-.*$,^TravelersTitles-.*$,^VoidFog-.*$,^WindowedFullscreen-.*$,^WorldNameRandomizer-.*$,^[1.12.2]DamageIndicatorsMod-.*$,^[1.12.2]bspkrscore-.*$,^antighost-.*$,^anviltooltipmod-.*$,^appleskin-.*$,^armorchroma-.*$,^armorpointspp-.*$,^auditory-.*$,^authme-.*$,^auto-reconnect-.*$,^autojoin-.*$,^autoreconnect-.*$,^axolotl-item-fix-.*$,^backtools-.*$,^bannerunlimited-.*$,^beenfo-1.19-.*$,^better-recipe-book-.*$,^betterbiomeblend-.*$,^bhmenu-.*$,^blur-.*$,^borderless-mining-.*$,^catalogue-.*$,^charmonium-.*$,^chat_heads-.*$,^cherishedworlds-.*$,^cirback-1.0-.*$,^classicbar-.*$,^clickadv-.*$,^clienttweaks-.*$,^combat_music-.*$,^configured-.*$,^controllable-.*$,^cullleaves-.*$,^cullparticles-.*$,^custom-crosshair-mod-.*$,^customdiscordrpc-.*$,^darkness-.*$,^dashloader-.*$,^defaultoptions-.*$,^desiredservers-.*$,^discordrpc-.*$,^drippyloadingscreen-.*$,^drippyloadingscreen_.*$,^dynamic-fps-.*$,^dynamic-music-.*$,^dynamiclights-.*$,^dynmus-.*$,^effective-.*$,^eggtab-.*$,^eguilib-.*$,^eiramoticons-.*$,^enchantment-lore-.*$,^entity-texture-features-.*$,^entityculling-.*$,^exhaustedstamina-.*$,^extremesoundmuffler-.*$,^fabricemotes-.*$,^fancymenu_.*$,^fancymenu_video_extension.*$,^findme-.*$,^flickerfix-.*$,^fm_audio_extension_.*$,^forgemod_VoxelMap-.*$,^freelook-.*$,^galacticraft-rpc-.*$,^gamestagesviewer-.*$,^grid-.*$,^helium-.*$,^hiddenrecipebook_.*$,^infinitemusic-.*$,^inventoryprofiles.*$,^invtweaks-.*$,^itemzoom.*$,^itlt-.*$,^jeed-.*$,^jehc-.*$,^jeiintegration_.*$,^just-enough-harvestcraft-.*$,^justenoughbeacons-.*$,^justenoughdrags-.*$,^justzoom_.*$,^keymap-.*$,^keywizard-.*$,^konkrete_.*$,^konkrete_forge_.*$,^lazydfu-.*$,^light-overlay-.*$,^lightfallclient-.*$,^loadmyresources_.*$,^lock_minecart_view-.*$,^lootbeams-.*$,^lwl-.*$,^magnesium_extras-.*$,^maptooltip-.*$,^massunbind.*$,^mcbindtype-.*$,^mcwifipnp-.*$,^medievalmusic-.*$,^mightyarchitect-.*$,^mindful-eating-.*$,^minetogether-.*$,^mobplusplus-.*$,^modcredits-.*$,^modernworldcreation_.*$,^modmenu-.*$,^modnametooltip-.*$,^modnametooltip_.*$,^moreoverlays-.*$,^mousewheelie-.*$,^movement-vision-.*$,^multihotbar-.*$,^music-duration-reducer-.*$,^musicdr-.*$,^neiRecipeHandlers-.*$,^ngrok-lan-expose-mod-.*$,^nopotionshift_.*$,^notenoughanimations-.*$,^oculus-.*$,^ornaments-.*$,^overloadedarmorbar-.*$,^panorama-.*$,^paperdoll-.*$,^phosphor-.*$,^preciseblockplacing-.*$,^realm-of-lost-souls-.*$,^rebrand-.*$,^replanter-.*$,^rubidium-.*$,^rubidium_extras-.*$,^screenshot-to-clipboard-.*$,^shutupexperimentalsettings-.*$,^shutupmodelloader-.*$,^signtools-.*$,^simple-rpc-.*$,^simpleautorun-.*$,^smartcursor-.*$,^smoothboot-.*$,^smoothfocus-.*$,^sounddeviceoptions-.*$,^soundreloader-.*$,^spoticraft-.*$,^tconplanner-.*$,^timestamps-.*$,^tooltipscroller-.*$,^torchoptimizer-.*$,^torohealth-.*$,^totaldarkness.*$,^toughnessbar-.*$,^wisla-.*$,^xlifeheartcolors-.*$,^yisthereautojump-.*$";
  /**
   * Default fallback clientside-only mods list, regex.
   */
  private final TreeSet<String> FALLBACK_REGEX_CLIENTSIDE_MODS =
      new TreeSet<>(Arrays.asList(FALLBACK_MODS_DEFAULT_REGEX_ASSTRING.split(",")));
  /**
   * List of mods which should be excluded from server packs, in regex format.
   */
  private final TreeSet<String> FALLBACK_MODS_REGEX = new TreeSet<>(FALLBACK_REGEX_CLIENTSIDE_MODS);
  /**
   * ServerPackCreator version.
   */
  private final String SERVERPACKCREATOR_VERSION;
  /**
   * Supported modloaders.
   */
  private final String[] SUPPORTED_MODLOADERS = new String[]{"Fabric", "Forge", "Quilt",
      "LegacyFabric"};
  /**
   * Default directories to include in the server pack.
   */
  private final String FALLBACK_DIRECTORIES_INCLUDE_ASSTRING = "mods,config,kubejs,defaultconfigs,scripts";
  /**
   * Default list of directories to include in the server pack.
   */
  private final TreeSet<String> FALLBACK_DIRECTORIES_INCLUDE =
      new TreeSet<>(Arrays.asList(FALLBACK_DIRECTORIES_INCLUDE_ASSTRING.split(",")));
  /**
   * List of directories which must not be excluded from server packs. Default is mods, config,
   * defaultconfigs, scripts, saves, seeds, libraries.
   */
  private final TreeSet<String> DIRECTORIES_TO_INCLUDE = new TreeSet<>(
      FALLBACK_DIRECTORIES_INCLUDE);
  /**
   * Default directories to exclude from the server pack.
   */
  private final String FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING =
      "overrides,packmenu,resourcepacks,server_pack,fancymenu,libraries,downloads,logs,profileImage,resourcepacks,screenshots,shaderpacks,tv-cache,asm";
  /**
   * Default list of directories to exclude from the server pack.
   */
  private final TreeSet<String> FALLBACK_DIRECTORIES_EXCLUDE =
      new TreeSet<>(Arrays.asList(FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING.split(",")));
  /**
   * List of directories which should be excluded from server packs. Default is overrides, packmenu,
   * resourcepacks, server_pack, fancymenu.
   */
  private final TreeSet<String> DIRECTORIES_TO_EXCLUDE = new TreeSet<>(
      FALLBACK_DIRECTORIES_EXCLUDE);
  /**
   * Default files to exclude from a server pack ZIP-archive.
   */
  private final String FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING =
      "minecraft_server.MINECRAFT_VERSION.jar,server.jar,libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar";
  /**
   * Default list of files to exclude from a server pack ZIP-archive.
   */
  private final TreeSet<String> FALLBACK_FILES_EXCLUDE_ZIP =
      new TreeSet<>(Arrays.asList(FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING.split(",")));
  /**
   * List of files to be excluded from ZIP-archives.
   */
  private final TreeSet<String> FILES_TO_EXCLUDE_FROM_ZIP = new TreeSet<>(
      FALLBACK_FILES_EXCLUDE_ZIP);
  /**
   * Default shell-script template.
   */
  private final String DEFAULT_SHELL_TEMPLATE = "default_template.sh";
  /**
   * Default PowerShell script template.
   */
  private final String DEFAULT_POWERSHELL_TEMPLATE = "default_template.ps1";
  /**
   * Default script templates list as string.
   */
  private final String FALLBACK_SCRIPT_TEMPLATES_ASSTRING =
      DEFAULT_SHELL_TEMPLATE + "," + DEFAULT_POWERSHELL_TEMPLATE;
  /**
   * Default list of script templates in the server_files-directory.
   */
  private final TreeSet<File> FALLBACK_SCRIPT_TEMPLATES =
      new TreeSet<>(
          Arrays.asList(
              new File("server_files/" + DEFAULT_SHELL_TEMPLATE),
              new File("server_files/" + DEFAULT_POWERSHELL_TEMPLATE)));
  /**
   * List of templates used for start-script creation.
   */
  private final TreeSet<File> SCRIPT_TEMPLATES = new TreeSet<>(FALLBACK_SCRIPT_TEMPLATES);
  /**
   * Default configuration file from which to load a server pack configuration.
   */
  private final File DEFAULT_CONFIG = new File("serverpackcreator.conf");
  /**
   * Default properties file for a Minecraft server.
   */
  private final File DEFAULT_SERVER_PROPERTIES = new File("server.properties");
  /**
   * Default server-icon for a Minecraft server.
   */
  private final File DEFAULT_SERVER_ICON = new File("server-icon.png");
  /**
   * Minecraft version manifest file.
   */
  private final File MINECRAFT_VERSION_MANIFEST = new File("minecraft-manifest.json");
  /**
   * Forge version manifest file.
   */
  private final File FORGE_VERSION_MANIFEST = new File("forge-manifest.json");
  /**
   * Fabric version manifest file.
   */
  private final File FABRIC_VERSION_MANIFEST = new File("fabric-manifest.xml");
  private final File FABRIC_INTERMEDIARIES_MANIFEST = new File(
      "fabric-intermediaries-manifest.json");
  /**
   * Storage location of manifests used by the VersionMeta.
   */
  private final String MANIFEST_LOCATION = "./manifests/";
  private final String MINECRAFT_SERVER_MANIFESTS_LOCATION = MANIFEST_LOCATION + "mcserver/";
  /**
   * Fabric intermediaries manifest file.
   */
  private final File FABRIC_INTERMEDIARIES_MANIFEST_LOCATION =
      new File(MANIFEST_LOCATION + FABRIC_INTERMEDIARIES_MANIFEST.getName());
  /**
   * Fabric installer version manifest file.
   */
  private final File FABRIC_INSTALLER_VERSION_MANIFEST = new File("fabric-installer-manifest.xml");
  /**
   * Quilt version manifest file.
   */
  private final File QUILT_VERSION_MANIFEST = new File("quilt-manifest.xml");
  /**
   * Quilt installer version manifest file.
   */
  private final File QUILT_INSTALLER_VERSION_MANIFEST = new File("quilt-installer-manifest.xml");
  /**
   * ServerPackCreator webservice database file.
   */
  private final File SERVERPACKCREATOR_DATABASE = new File("serverpackcreator.db");
  /**
   * Storage location for Minecraft version manifest file.
   */
  private final File MINECRAFT_VERSION_MANIFEST_LOCATION =
      new File(MANIFEST_LOCATION + MINECRAFT_VERSION_MANIFEST.getName());
  /**
   * Storage location for Forge version manifest file.
   */
  private final File FORGE_VERSION_MANIFEST_LOCATION = new File(
      MANIFEST_LOCATION + "forge-manifest.json");
  /**
   * Storage location for Fabric version manifest file.
   */
  private final File FABRIC_VERSION_MANIFEST_LOCATION = new File(
      MANIFEST_LOCATION + "fabric-manifest.xml");
  /**
   * Legacy Fabric Game version manifest file.
   */
  private final File LEGACY_FABRIC_GAME_MANIFEST = new File("legacy-fabric-game-manifest.json");
  /**
   * Legacy Fabric Loader version manifest file.
   */
  private final File LEGACY_FABRIC_LOADER_MANIFEST = new File("legacy-fabric-loader-manifest.json");
  /**
   * Legacy Fabric Installer version manifest file.
   */
  private final File LEGACY_FABRIC_INSTALLER_MANIFEST = new File(
      "legacy-fabric-installer-manifest.xml");
  /**
   * Storage location for Legacy Fabric Game version manifest file.
   */
  private final File LEGACY_FABRIC_GAME_MANIFEST_LOCATION = new File(
      MANIFEST_LOCATION + LEGACY_FABRIC_GAME_MANIFEST.getName());
  /**
   * Storage location for Legacy Fabric Loader version manifest file.
   */
  private final File LEGACY_FABRIC_LOADER_MANIFEST_LOCATION = new File(
      MANIFEST_LOCATION + LEGACY_FABRIC_LOADER_MANIFEST.getName());
  /**
   * Storage location for Legacy Fabric Installer version manifest file.
   */
  private final File LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION = new File(
      MANIFEST_LOCATION + LEGACY_FABRIC_INSTALLER_MANIFEST.getName());
  /**
   * Storage location for Fabric installer version manifest file.
   */
  private final File FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION =
      new File(MANIFEST_LOCATION + FABRIC_INSTALLER_VERSION_MANIFEST.getName());
  /**
   * Storage location for Quilt version manifest file.
   */
  private final File QUILT_VERSION_MANIFEST_LOCATION = new File(
      MANIFEST_LOCATION + QUILT_VERSION_MANIFEST.getName());
  /**
   * Storage location for Quilt installer version manifest file.
   */
  private final File QUILT_INSTALLER_VERSION_MANIFEST_LOCATION =
      new File(MANIFEST_LOCATION + QUILT_INSTALLER_VERSION_MANIFEST.getName());
  /**
   * Default Aikars flags.
   */
  private final String AIKARS_FLAGS =
      "-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 "
          + "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 "
          + "-XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 "
          + "-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 "
          + "-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem "
          + "-XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true";
  /**
   * Map containing any Java paths from properties with the format
   * {@code de.griefed.serverpackcreator.script.javaN} where N is the version of the Java
   * installation.
   */
  private final HashMap<String, String> JAVA_PATHS = new HashMap<>(256);
  private final String PROPERTY_VERSIONCHECK_PRERELEASE = "de.griefed.serverpackcreator.versioncheck.prerelease";
  private final String PROPERTY_LANGUAGE = "de.griefed.serverpackcreator.language";
  private final String PROPERTY_CONFIGURATION_FALLBACKMODSLIST = "de.griefed.serverpackcreator.configuration.fallbackmodslist";
  private final String PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX = "de.griefed.serverpackcreator.configuration.fallbackmodslist.regex";
  private final String PROPERTY_CONFIGURATION_HASTEBINSERVER = "de.griefed.serverpackcreator.configuration.hastebinserver";
  private final String PROPERTY_CONFIGURATION_AIKAR = "de.griefed.serverpackcreator.configuration.aikar";
  private final String PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED = "de.griefed.serverpackcreator.serverpack.autodiscovery.enabled";
  private final String PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY = "de.griefed.serverpackcreator.serverpack.autodiscoverenabled";
  private final String PROPERTY_GUI_DARKMODE = "de.griefed.serverpackcreator.gui.darkmode";
  private final String PROPERTY_CONFIGURATION_DIRECTORIES_SERVERPACKS = "de.griefed.serverpackcreator.configuration.directories.serverpacks";
  private final String PROPERTY_SERVERPACK_CLEANUP_ENABLED = "de.griefed.serverpackcreator.serverpack.cleanup.enabled";
  private final String PROPERTY_SERVERPACK_OVERWRITE_ENABLED = "de.griefed.serverpackcreator.serverpack.overwrite.enabled";
  private final String PROPERTY_CONFIGURATION_DIRECTORIES_SHOULDEXCLUDE = "de.griefed.serverpackcreator.configuration.directories.shouldexclude";
  private final String PROPERTY_SPRING_SCHEDULES_DATABASE_CLEANUP = "de.griefed.serverpackcreator.spring.schedules.database.cleanup";
  private final String PROPERTY_SPRING_SCHEDULES_FILES_CLEANUP = "de.griefed.serverpackcreator.spring.schedules.files.cleanup";
  private final String PROPERTY_SPRING_SCHEDULES_VERSIONS_REFRESH = "de.griefed.serverpackcreator.spring.schedules.versions.refresh";
  private final String PROPERTY_SPRING_ARTEMIS_QUEUE_MAX_DISK_USAGE = "de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage";
  private final String PROPERTY_CONFIGURATION_SAVELOADEDCONFIG = "de.griefed.serverpackcreator.configuration.saveloadedconfig";
  private final String PROPERTY_CONFIGURATION_DIRECTORIES_MUSTINCLUDE = "de.griefed.serverpackcreator.configuration.directories.mustinclude";
  private final String PROPERTY_SERVERPACK_ZIP_EXCLUDE = "de.griefed.serverpackcreator.serverpack.zip.exclude";
  private final String PROPERTY_SERVERPACK_ZIP_EXCLUDE_ENABLED = "de.griefed.serverpackcreator.serverpack.zip.exclude.enabled";
  private final String PROPERTY_SERVERPACK_SCRIPT_TEMPLATE = "de.griefed.serverpackcreator.serverpack.script.template";
  private final String PROPERTY_MINECRAFT_SNAPSHOTS = "de.griefed.serverpackcreator.minecraft.snapshots";
  private final String PROPERTY_SERVERPACK_AUTODISCOVERY_FILTER = "de.griefed.serverpackcreator.serverpack.autodiscovery.filter";
  private final String PROPERTY_JAVA = "de.griefed.serverpackcreator.java";
  private final String PROPERTY_SCRIPT_JAVA = "de.griefed.serverpackcreator.script.java";
  private final String PROPERTY_SCRIPT_JAVA_AUTOUPDATE = "de.griefed.serverpackcreator.script.java.autoupdate";
  /**
   * The directory in which server packs will be generated and stored in, as well as server pack
   * ZIP-archives. Default is ./server-packs
   */
  private String directoryServerPacks = "server-packs";
  /**
   * When running as a webservice: Maximum disk usage in % at which JMS/Artemis will stop storing
   * message in the queue saved on disk. Default is 90%.
   */
  private int queueMaxDiskUsage = 90;
  /**
   * Whether the manually loaded configuration file should be saved as well as the default
   * serverpackcreator.conf. Setting this to true will make ServerPackCreator save
   * serverpackcreator.conf as well as the last loaded configuration-file. Default is false.
   */
  private boolean saveLoadedConfiguration = false;
  /**
   * Whether ServerPackCreator should check for available PreReleases. Set to {@code true} to get
   * notified about available PreReleases. Set to {@code false} if you only want stable releases.
   */
  private boolean checkForPreReleases = false;
  /**
   * Aikars flags recommended for running a Minecraft server, from <a
   * href=https://aikar.co/mcflags.html>aikar.co</a>
   */
  private String aikarsFlags = AIKARS_FLAGS;
  /**
   * Whether the exclusion of files from the server pack is enabled.
   */
  private boolean isZipFileExclusionEnabled = true;
  /**
   * Whether clientside-only mods should automatically be excluded *
   */
  private boolean autoExcludingModsEnabled = true;
  /**
   * Whether overwriting of already existing server packs is enabled
   */
  private boolean serverPacksOverwriteEnabled = true;
  /**
   * Whether cleanup procedures of server packs after generation are enabled. See
   * {@code ServerPackHandler#cleanUpServerPack(...)} for details.
   */
  private boolean serverPackCleanupEnabled = true;
  /**
   * The language currently being used.
   */
  private String language = "en_us";
  /**
   * URL to the hastebin server documents endpoint.
   */
  private String hasteBinServerUrl = "https://haste.zneix.eu/documents";
  /**
   * Whether pre-releases and snapshots should be available to the user.
   */
  private boolean minecraftPreReleases = false;
  /**
   * In which way user-specified clientside-only mods should be excluded.
   */
  private ExclusionFilter exclusionFilter = ExclusionFilter.START;
  /**
   * The path to a viable Java executable or binary for use in modloader server installation.
   */
  private String javaPath = "java";
  /**
   * Whether to automatically update the {@code SPC_JAVA_SPC}-placeholder in the script variables
   * table with a Java path matching the required Java version for the Minecraft server.
   */
  private boolean javaScriptAutoupdate = true;

  /**
   * Initialize an instance of our application properties using the default
   * {@code serverpackcreator.properties}.
   *
   * @param fileUtilities   Instance of {@link FileUtilities} for file-operations.
   * @param systemUtilities Instance of {@link SystemUtilities} to acquire the Java path
   *                        automatically.
   * @param listUtilities   Used to print the configured fallback modlists in chunks.
   * @author Griefed
   */
  @Autowired
  public ApplicationProperties(
      FileUtilities fileUtilities,
      SystemUtilities systemUtilities,
      ListUtilities listUtilities) {

    this(new File("serverpackcreator.properties"), fileUtilities, systemUtilities, listUtilities);
  }

  /**
   * Initialize an instance of our application properties using a custom properties-file.
   *
   * @param propertiesFile  The properties file from which to load the settings and configuration.
   * @param fileUtilities   Instance of {@link FileUtilities} for file-operations.
   * @param systemUtilities Instance of {@link SystemUtilities} to acquire the Java path
   *                        automatically.
   * @param listUtilities   Used to print the configured fallback modlists in chunks.
   * @author Griefed
   */
  public ApplicationProperties(
      File propertiesFile,
      FileUtilities fileUtilities,
      SystemUtilities systemUtilities,
      ListUtilities listUtilities) {

    super();

    FILE_UTILITIES = fileUtilities;
    SYSTEM_UTILITIES = systemUtilities;
    LIST_UTILITIES = listUtilities;

    String version = ApplicationProperties.class.getPackage().getImplementationVersion();
    if (version != null) {
      SERVERPACKCREATOR_VERSION = version;
    } else {
      SERVERPACKCREATOR_VERSION = "dev";
    }

    loadProperties(propertiesFile);
  }

  /**
   * Reload serverpackcreator.properties.
   *
   * @author Griefed
   */
  public void loadProperties() {
    loadProperties(SERVERPACKCREATOR_PROPERTIES_FILE);
  }

  /**
   * Reload from a specific properties-file.
   *
   * @param propertiesFile The properties-file with which to loadProperties the settings and
   *                       configuration.
   * @author Griefed
   */
  private void loadProperties(File propertiesFile) {
    // Load the properties file from the classpath, providing default values.
    try (InputStream inputStream =
        new ClassPathResource(SERVERPACKCREATOR_PROPERTIES).getInputStream()) {
      load(inputStream);
    } catch (IOException ex) {
      LOG.error("Couldn't read properties file.", ex);
    }

    // If our properties-file exists, load it to ensure we always have base settings available.
    if (SERVERPACKCREATOR_PROPERTIES_FILE.exists()) {
      try (InputStream inputStream =
          Files.newInputStream(SERVERPACKCREATOR_PROPERTIES_FILE.toPath())) {
        load(inputStream);
      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    // Load the specified properties-file.
    if (propertiesFile.exists()) {
      try (InputStream inputStream =
          Files.newInputStream(propertiesFile.toPath())) {

        load(inputStream);
        LOG.info("Loading file: " + propertiesFile.getAbsolutePath());

      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    if (updateFallback()) {
      LOG.info("Fallback lists updated.");
    } else {
      setFallbackModsList();
    }

    setServerPacksDir();

    setDirsToIncludeList();

    setDirsToExcludeList();

    setQueueMaxDiskUsage();

    setSaveLoadedConfiguration();

    setCheckForPreReleases();

    setAikarsFlags();

    setFilesToExcludeFromZip();

    setZipFileExclusionEnabled();

    setScriptTemplates();

    setAutoExclusionOfMods();

    setServerPackOverwrite();

    setServerPackCleanup();

    setLanguage();

    setHasteBinServerUrl();

    setMinecraftPreReleases();

    setModExclusionFilterMethod();

    setJavaPath();

    setJavaScriptsVariablePaths();

    setAutoUpdateScriptVariablesJavaPlaceholder();

    saveToDisk(SERVERPACKCREATOR_PROPERTIES_FILE);
  }

  /**
   * Set a property in our ApplicationProperties.
   *
   * @param key   The key in which to store the property.
   * @param value The value to store in the specified key.
   * @author Griefed
   */
  private String defineProperty(String key, String value) {
    setProperty(key, value);
    return value;
  }

  /**
   * Get a property from our ApplicationProperties. If the property is not available, it is created
   * with the specified value, thus allowing subsequent calls.
   *
   * @param key          The key of the property to acquire.
   * @param defaultValue The default value for the specified key in case the key is not present or
   *                     empty.
   * @return The value stored in the specified key.
   * @author Griefed
   */
  private String acquireProperty(String key, String defaultValue) {
    if (getProperty(key) == null) {
      return defineProperty(key, defaultValue);
    } else {
      return getProperty(key, defaultValue);
    }
  }

  /**
   * Get a list from our properties.
   *
   * @param key          The key of the property which holds the comma-separated list.
   * @param defaultValue The default value to set the property to in case it is undefined.
   * @return The requested list.
   * @author Griefed
   */
  private List<String> getListProperty(String key, String defaultValue) {
    if (acquireProperty(key, defaultValue).contains(",")) {
      return new ArrayList<>(Arrays.asList(acquireProperty(key, defaultValue).split(",")));
    } else {
      return Collections.singletonList(acquireProperty(key, defaultValue));
    }
  }

  /**
   * Get an integer from our properties.
   *
   * @param key          The key of the property which holds the comma-separated list.
   * @param defaultValue The default value to set the property to in case it is undefined.
   * @return The requested integer.
   * @author Griefed
   */
  private int getIntProperty(String key, int defaultValue) {
    try {
      return Integer.parseInt(acquireProperty(key, String.valueOf(defaultValue)));
    } catch (NumberFormatException ex) {
      defineProperty(key, String.valueOf(defaultValue));
      return defaultValue;
    }
  }

  /**
   * Get a list of files from our properties, with each file having a specific prefix.
   *
   * @param key          The key of the property which holds the comma-separated list.
   * @param defaultValue The default value to set the property to in case it is undefined.
   * @param filePrefix   The prefix every file should receive.
   * @return The requested list of files.
   * @author Griefed
   */
  private List<File> getFileListProperty(String key, String defaultValue, String filePrefix) {
    List<File> files = new ArrayList<>(10);
    for (String entry : getListProperty(key, defaultValue)) {
      files.add(new File(filePrefix + entry));
    }
    return files;
  }

  /**
   * Get a boolean from our properties.
   *
   * @param key          The key of the property which holds the comma-separated list.
   * @param defaultValue The default value to set the property to in case it is undefined.
   * @return The requested integer.
   * @author Griefed
   */
  private boolean getBoolProperty(String key, boolean defaultValue) {
    return Boolean.parseBoolean(acquireProperty(key, String.valueOf(defaultValue)));
  }

  /**
   * Set the directory where the generated server packs will be stored in.
   *
   * @author Griefed
   */
  private void setServerPacksDir() {
    if (new File(acquireProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SERVERPACKS,
        "server-packs")).exists()) {
      directoryServerPacks = acquireProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SERVERPACKS,
          "server-packs");
    } else {
      LOG.error("Invalid server-packs directory specified. Defaulting to 'server-packs'.");
      directoryServerPacks = "server-packs";
    }
    LOG.info("Server packs directory set to: " + directoryServerPacks);
  }

  /**
   * Set up our fallback list of clientside-only mods.
   *
   * @author Griefed
   */
  private void setFallbackModsList() {
    FALLBACK_MODS.addAll(getListProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST,
        FALLBACK_MODS_DEFAULT_ASSTRING));
    setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST, String.join(",", FALLBACK_MODS));
    LOG.info("Fallback modslist set to:");
    LIST_UTILITIES.printListToLogChunked(new ArrayList<>(FALLBACK_MODS), 5, "    ", true);

    FALLBACK_MODS_REGEX.addAll(getListProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX,
        FALLBACK_MODS_DEFAULT_REGEX_ASSTRING));
    setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX,
        String.join(",", FALLBACK_MODS_REGEX));
    LOG.info("Fallback regex modslist set to:");
    LIST_UTILITIES.printListToLogChunked(new ArrayList<>(FALLBACK_MODS_REGEX), 5, "    ", true);
  }

  /**
   * List of directories which can be excluded from server packs
   *
   * @author Griefed
   */
  private void setDirsToExcludeList() {
    getListProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SHOULDEXCLUDE,
        FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING).forEach(
        this::addDirectoryToExclude
    );
    LOG.info("Directories to exclude set to: " + DIRECTORIES_TO_EXCLUDE);
  }

  /**
   * List of directories which should always be included in a server pack, no matter what the users
   * specify
   *
   * @author Griefed
   */
  private void setDirsToIncludeList() {
    DIRECTORIES_TO_INCLUDE.addAll(getListProperty(PROPERTY_CONFIGURATION_DIRECTORIES_MUSTINCLUDE,
        FALLBACK_DIRECTORIES_INCLUDE_ASSTRING));
    LOG.info("Directories which must always be included set to: " + DIRECTORIES_TO_INCLUDE);
  }

  /**
   * Max diskspace usage before no more jobs are accepted when running as a webservice.
   *
   * @author Griefed
   */
  private void setQueueMaxDiskUsage() {
    int usage = getIntProperty(PROPERTY_SPRING_ARTEMIS_QUEUE_MAX_DISK_USAGE, 90);
    if (usage >= 0 && usage <= 100) {
      queueMaxDiskUsage = getIntProperty(PROPERTY_SPRING_ARTEMIS_QUEUE_MAX_DISK_USAGE, 90);
    } else {
      LOG.error("Invalid max disk usage set. Defaulting to 90");
      queueMaxDiskUsage = 90;
    }
    LOG.info("Queue max disk usage set to: " + queueMaxDiskUsage);
  }

  /**
   * Whether the last loaded configuration should be saved to as well.
   *
   * @author Griefed
   */
  private void setSaveLoadedConfiguration() {
    saveLoadedConfiguration = getBoolProperty(PROPERTY_CONFIGURATION_SAVELOADEDCONFIG, false);
    LOG.info("Save last loaded config set to: " + saveLoadedConfiguration);
  }

  /**
   * Whether to check for pre-releases as well.
   *
   * @author Griefed
   */
  private void setCheckForPreReleases() {
    checkForPreReleases = getBoolProperty(PROPERTY_VERSIONCHECK_PRERELEASE, false);

    if (SERVERPACKCREATOR_VERSION.matches("(.*alpha.*|.*beta.*)")) {

      checkForPreReleases = true;
      LOG.info(
          "Using pre-release " + SERVERPACKCREATOR_VERSION
              + ". Checking for pre-releases set to true.");

    } else {

      LOG.info("Set check for pre-releases to: " + checkForPreReleases);
    }
  }

  /**
   * Aikars flags to set when the user so desires.
   *
   * @author Griefed
   */
  private void setAikarsFlags() {
    aikarsFlags = acquireProperty(PROPERTY_CONFIGURATION_AIKAR, AIKARS_FLAGS);
    LOG.info("Aikars flags set to: " + aikarsFlags);
  }

  /**
   * Files or folders which should be excluded from a ZIP-archive.
   *
   * @author Griefed
   */
  private void setFilesToExcludeFromZip() {
    FILES_TO_EXCLUDE_FROM_ZIP.addAll(getListProperty(PROPERTY_SERVERPACK_ZIP_EXCLUDE,
        FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING));
    LOG.info(
        "Files which must be excluded from ZIP-archives set to: " + FILES_TO_EXCLUDE_FROM_ZIP);
  }

  /**
   * Files and folders which should be included in a server pack.
   *
   * @author Griefed
   */
  private void setZipFileExclusionEnabled() {
    isZipFileExclusionEnabled = getBoolProperty(PROPERTY_SERVERPACK_ZIP_EXCLUDE_ENABLED, true);
    LOG.info("Zip file exclusion enabled set to: " + isZipFileExclusionEnabled);
  }

  /**
   * Script templates to generate start scripts with.
   *
   * @author Griefed
   */
  private void setScriptTemplates() {
    SCRIPT_TEMPLATES.clear();
    SCRIPT_TEMPLATES.addAll(getFileListProperty(PROPERTY_SERVERPACK_SCRIPT_TEMPLATE,
        FALLBACK_SCRIPT_TEMPLATES_ASSTRING, "server_files/"));
    LOG.info("Using script templates:");
    SCRIPT_TEMPLATES.forEach(template -> LOG.info("    " + template.getPath()));
  }

  /**
   * Whether clientside-only mods should automatically be detected and excluded.
   *
   * @author Griefed
   */
  private void setAutoExclusionOfMods() {
    autoExcludingModsEnabled = getBoolProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED, true);

    // Legacy declaration which may still be present in some serverpackcreator.properties-files.
    try {
      if (getProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY).matches("(true|false)")) {
        autoExcludingModsEnabled =
            Boolean.parseBoolean(
                getProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY));

        setProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED,
            String.valueOf(autoExcludingModsEnabled));

        remove(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY);

        LOG.info(
            "Migrated '" + PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY + "' to '"
                + PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED + "'.");
      }

    } catch (Exception ignored) {
      // No legacy declaration present, so we can safely ignore any exception.
    }

    LOG.info("Auto-discovery of clientside-only mods set to: " + autoExcludingModsEnabled);
  }

  /**
   * Whether existing server packs should be overwritten.
   *
   * @author Griefed
   */
  private void setServerPackOverwrite() {
    serverPacksOverwriteEnabled = getBoolProperty(PROPERTY_SERVERPACK_OVERWRITE_ENABLED, true);
    LOG.info(
        "Overwriting of already existing server packs set to: " + serverPacksOverwriteEnabled);
  }

  /**
   * Whether cleanup procedures should be executed after server pack generation.
   *
   * @author Griefed
   */
  private void setServerPackCleanup() {
    serverPackCleanupEnabled = getBoolProperty(PROPERTY_SERVERPACK_CLEANUP_ENABLED, true);
    LOG.info(
        "Cleanup of already existing server packs set to: " + serverPackCleanupEnabled);
  }

  /**
   * Language used by the Swing frontend.
   *
   * @author Griefed
   */
  private void setLanguage() {
    language = acquireProperty(PROPERTY_LANGUAGE, "en_us");
    LOG.info("Language set to: " + language);
  }

  /**
   * URL to send logs and configs to for easier issue reporting on GitHub.
   *
   * @author Griefed
   */
  private void setHasteBinServerUrl() {
    hasteBinServerUrl = acquireProperty(PROPERTY_CONFIGURATION_HASTEBINSERVER,
        "https://haste.zneix.eu/documents");
    LOG.info("HasteBin documents endpoint set to: " + hasteBinServerUrl);
  }

  /**
   * Whether Minecraft pre-releases and snapshots should be made available to the user.
   *
   * @author Griefed
   */
  private void setMinecraftPreReleases() {
    minecraftPreReleases = getBoolProperty(PROPERTY_MINECRAFT_SNAPSHOTS, false);
    LOG.info("Minecraft pre-releases and snapshots available set to: " + minecraftPreReleases);
  }

  /**
   * Set in which way user-specified clientside-only mods should be excluded.
   *
   * @author Griefed
   */
  private void setModExclusionFilterMethod() {
    try {
      String filterText = acquireProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_FILTER, "START");
      switch (filterText) {
        case "END":
          exclusionFilter = ExclusionFilter.END;
          break;

        case "CONTAIN":
          exclusionFilter = ExclusionFilter.CONTAIN;
          break;

        case "REGEX":
          exclusionFilter = ExclusionFilter.REGEX;
          break;

        case "EITHER":
          exclusionFilter = ExclusionFilter.EITHER;
          break;

        default:
          LOG.error("Invalid filter specified. Defaulting to START.");
        case "START":
          exclusionFilter = ExclusionFilter.START;
          break;
      }
    } catch (NullPointerException ignored) {

    } finally {
      setProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_FILTER, exclusionFilter.toString());
    }
    LOG.info("User specified clientside-only mod exclusion filter set to: " + exclusionFilter);
  }

  /**
   * Set the Java path using the defined property, if it exists. If the setting is incorrect,
   * ServerPackCreator will try to automatically determine the path and set the property
   * accordingly.
   *
   * @author Griefed
   */
  private void setJavaPath() {
    if (checkJavaPath(getProperty(PROPERTY_JAVA, ""))) {
      javaPath = getProperty(PROPERTY_JAVA);
    } else {
      setProperty(PROPERTY_JAVA, getJavaPath(""));
      javaPath = acquireProperty(PROPERTY_JAVA, getJavaPath(""));
    }
    LOG.info("Java path set to: " + javaPath);
  }

  /**
   * Setter for the path to the Java executable/binary. Replaces any occurrences of \ with /. The
   * path to the Java executable or binary {@code MUST NOT} be a symbolic link, or link of any kind.
   * It must be a direct path!
   *
   * @param javaPath The new Java path to store.
   * @author Griefed
   */
  public void setJavaPath(String javaPath) {
    this.javaPath = getJavaPath(javaPath);
    saveToDisk(SERVERPACKCREATOR_PROPERTIES_FILE);
  }

  /**
   * Getter for the path to the Java executable/binary.
   *
   * @return String. Returns the path to the Java executable/binary.
   * @author Griefed
   */
  public String java() {
    return javaPath;
  }

  /**
   * Whether a viable path to a Java executable or binary has been configured for
   * ServerPackCreator.
   *
   * @return {@code true} if a viable path has been set.
   * @author Griefed
   */
  public boolean javaAvailable() {
    return checkJavaPath(javaPath);
  }

  /**
   * Check whether the given path is a valid Java specification.
   *
   * @param pathToJava Path to the Java executable
   * @return {@code true} if the path is valid.
   * @author Griefed
   */
  private boolean checkJavaPath(String pathToJava) {

    if (pathToJava.isEmpty()) {
      return false;
    }

    FileUtilities.FileType type = FILE_UTILITIES.checkFileType(pathToJava);

    switch (type) {
      case FILE:
        return testJava(pathToJava);

      case LINK:
      case SYMLINK:
        try {

          return testJava(FILE_UTILITIES.resolveLink(new File(pathToJava)));

        } catch (InvalidFileTypeException | IOException ex) {
          LOG.error("Could not read Java link/symlink.", ex);
        }

        return false;

      case DIRECTORY:
        LOG.error("Directory specified. Path to Java must lead to a lnk, symlink or file.");

      case INVALID:
      default:
        return false;
    }
  }

  /**
   * Test for a valid Java specification by trying to run {@code java -version}. If the command goes
   * through without errors, it is considered a correct specification.
   *
   * @param pathToJava Path to the java executable/binary.
   * @return {@code true} if the specified file is a valid Java executable/binary.
   * @author Griefed
   */
  private boolean testJava(String pathToJava) {
    boolean testSuccessful;
    try {
      ProcessBuilder processBuilder =
          new ProcessBuilder(new ArrayList<>(Arrays.asList(pathToJava, "-version")));

      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));

      while (bufferedReader.readLine() != null && !bufferedReader.readLine().equals("null")) {
        System.out.println(bufferedReader.readLine());
      }

      bufferedReader.close();
      process.destroyForcibly();

      testSuccessful = true;
    } catch (IOException e) {

      LOG.error("Invalid Java specified.");
      testSuccessful = false;
    }

    return testSuccessful;
  }

  /**
   * Check the given path to a Java installation for validity and return it, if it is valid. If the
   * passed path is a UNIX symlink or Windows lnk, it is resolved, then returned. If the passed path
   * is considered invalid, the system default is acquired and returned.
   *
   * @param pathToJava The path to check for whether it is a valid Java installation.
   * @return String. Returns the path to the Java installation. If user input was incorrect, SPC
   * will try to acquire the path automatically.
   * @author Griefed
   */
  private String getJavaPath(String pathToJava) {

    String checkedJavaPath;

    try {

      if (!pathToJava.isEmpty()) {

        if (checkJavaPath(pathToJava)) {

          return pathToJava;
        }

        if (checkJavaPath(pathToJava + ".exe")) {

          return pathToJava + ".exe";
        }

        if (checkJavaPath(pathToJava + ".lnk")) {

          return FILE_UTILITIES.resolveLink(new File(pathToJava + ".lnk"));
        }
      }

      LOG.info("Java setting invalid or otherwise not usable. Using system default.");
      LOG.debug("Acquiring path to Java installation from system properties...");
      checkedJavaPath = SYSTEM_UTILITIES.acquireJavaPathFromSystem();

      LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);

    } catch (NullPointerException | InvalidFileTypeException | IOException ex) {

      LOG.info("Java setting invalid or otherwise not usable. Using system default.");
      checkedJavaPath = SYSTEM_UTILITIES.acquireJavaPathFromSystem();

      LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath, ex);
    }

    return checkedJavaPath;
  }

  /**
   * Sets the path to the Java 17 executable/binary.
   *
   * @author Griefed
   */
  private void setJavaScriptsVariablePaths() {
    for (int i = 8; i < 256; i++) {
      if (checkJavaPath(getProperty(PROPERTY_SCRIPT_JAVA + i, ""))) {

        if (JAVA_PATHS.containsKey(String.valueOf(i))) {
          JAVA_PATHS.replace(String.valueOf(i),
              getProperty(PROPERTY_SCRIPT_JAVA + i).replace("\\", "/"));
        } else {
          JAVA_PATHS.put(String.valueOf(i),
              getProperty(PROPERTY_SCRIPT_JAVA + i).replace("\\", "/"));
        }

        setProperty(PROPERTY_SCRIPT_JAVA + i, JAVA_PATHS.get(String.valueOf(i)));
      }
    }

    LOG.info("Available Java paths for scripts for local testing and debugging:");
    for (Map.Entry<String, String> entry : JAVA_PATHS.entrySet()) {
      LOG.info("Java " + entry.getKey() + " path: " + entry.getValue());
    }
  }

  /**
   * Set whether to automatically update the {@code SPC_JAVA_SPC}-placeholder in the script
   * variables table with a Java path matching the required Java version for the Minecraft server.
   *
   * @author Griefed
   */
  private void setAutoUpdateScriptVariablesJavaPlaceholder() {
    javaScriptAutoupdate = getBoolProperty(PROPERTY_SCRIPT_JAVA_AUTOUPDATE, true);
    LOG.info("Automatically update SPC_JAVA_SPC-placeholder in script variables table set to: "
        + javaScriptAutoupdate);
  }

  /**
   * Default list of script templates, used in case not a single one was configured.
   *
   * <ul>
   *   <li>default_template.sh
   *   <li>default_template.ps1
   * </ul>
   *
   * @return Default script templates.
   * @author Griefed
   */
  public List<File> FALLBACK_SCRIPT_TEMPLATES() {
    return new ArrayList<>(FALLBACK_SCRIPT_TEMPLATES);
  }

  /**
   * The default shell-script template.
   *
   * @return The default shell-script template.
   * @author Griefed
   */
  public File DEFAULT_SHELL_TEMPLATE() {
    return new File(DEFAULT_SHELL_TEMPLATE);
  }

  /**
   * The default PowerShell-script template.
   *
   * @return The default PowerShell-script template.
   * @author Griefed
   */
  public File DEFAULT_POWERSHELL_TEMPLATE() {
    return new File(DEFAULT_POWERSHELL_TEMPLATE);
  }

  /**
   * Configured list of script templates.
   *
   * @return Configured script templates.
   * @author Griefed
   */
  public List<File> scriptTemplates() {
    return new ArrayList<>(SCRIPT_TEMPLATES);
  }

  /**
   * Properties file used by ServerPackCreator, containing the configuration for this instance of
   * it.
   *
   * @return serverpackcreator.properties-file.
   * @author Griefed
   */
  public File SERVERPACKCREATOR_PROPERTIES() {
    return SERVERPACKCREATOR_PROPERTIES_FILE;
  }

  /**
   * List of fallback clientside-only mods.
   *
   * @return List of fallback clientside-only mods.
   * @author Griefed
   */
  public List<String> FALLBACK_CLIENTSIDE_MODS() {
    return new ArrayList<>(FALLBACK_CLIENTSIDE_MODS);
  }

  /**
   * Regex-list of fallback clientside-only mods.
   *
   * @return Regex list of fallback clientside-only mods.
   * @author Griefed
   */
  public List<String> FALLBACK_REGEX_CLIENTSIDE_MODS() {
    return new ArrayList<>(FALLBACK_REGEX_CLIENTSIDE_MODS);
  }

  /**
   * Default configuration-file for a server pack generation.
   *
   * @return serverpackcreator.conf-file.
   * @author Griefed
   */
  public File DEFAULT_CONFIG() {
    return DEFAULT_CONFIG;
  }

  /**
   * Default server.properties-file used by Minecraft servers.
   *
   * @return server.properties-file.
   * @author Griefed
   */
  public File DEFAULT_SERVER_PROPERTIES() {
    return DEFAULT_SERVER_PROPERTIES;
  }

  /**
   * Default server-icon.png-file used by Minecraft servers.
   *
   * @return server-icon.png-file.
   * @author Griefed
   */
  public File DEFAULT_SERVER_ICON() {
    return DEFAULT_SERVER_ICON;
  }

  /**
   * Minecraft version manifest-file.
   *
   * @return minecraft-manifest.json-file.
   * @author Griefed
   */
  public File MINECRAFT_VERSION_MANIFEST() {
    return MINECRAFT_VERSION_MANIFEST;
  }

  /**
   * Forge version manifest-file.
   *
   * @return forge-manifest.json-file.
   * @author Griefed
   */
  public File FORGE_VERSION_MANIFEST() {
    return FORGE_VERSION_MANIFEST;
  }

  /**
   * Fabric version manifest-file.
   *
   * @return fabric-manifest.xml-file
   * @author Griefed
   */
  public File FABRIC_VERSION_MANIFEST() {
    return FABRIC_VERSION_MANIFEST;
  }

  /**
   * Fabric installer version manifest-file.
   *
   * @return fabric-installer-manifest.xml-file.
   * @author Griefed
   */
  public File FABRIC_INSTALLER_VERSION_MANIFEST() {
    return FABRIC_INSTALLER_VERSION_MANIFEST;
  }

  /**
   * Quilt version manifest-file.
   *
   * @return quilt-manifest.xml-file
   * @author Griefed
   */
  public File QUILT_VERSION_MANIFEST() {
    return QUILT_VERSION_MANIFEST;
  }

  /**
   * Quilt installer version manifest-file.
   *
   * @return quilt-installer-manifest.xml-file.
   * @author Griefed
   */
  public File QUILT_INSTALLER_VERSION_MANIFEST() {
    return QUILT_INSTALLER_VERSION_MANIFEST;
  }

  /**
   * ServerPackCreator-database when running as a webservice.
   *
   * @return serverpackcreator.db-file.
   * @author Griefed
   */
  public File SERVERPACKCREATOR_DATABASE() {
    return SERVERPACKCREATOR_DATABASE;
  }

  /**
   * Path to the Minecraft version manifest-file, as a file.
   *
   * @return ./manifests/minecraft-manifest.json
   * @author Griefed
   */
  public File MINECRAFT_VERSION_MANIFEST_LOCATION() {
    return MINECRAFT_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Forge version manifest-file, as a file.
   *
   * @return ./manifests/forge-manifest.json
   * @author Griefed
   */
  public File FORGE_VERSION_MANIFEST_LOCATION() {
    return FORGE_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric version manifest-file, as a file.
   *
   * @return ./manifests/fabric-manifest.xml
   * @author Griefed
   */
  public File FABRIC_VERSION_MANIFEST_LOCATION() {
    return FABRIC_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric intermediaries manifest-file, as a file.
   *
   * @return ./manifests/fabric-intermediaries-manifest.json
   */
  public File FABRIC_INTERMEDIARIES_MANIFEST_LOCATION() {
    return FABRIC_INTERMEDIARIES_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric installer version manifest-file, as a file.
   *
   * @return ./manifests/fabric-installer-manifest.xml
   * @author Griefed
   */
  public File FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION() {
    return FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Quilt version manifest-file, as a file.
   *
   * @return ./manifests/quilt-manifest.xml
   * @author Griefed
   */
  public File QUILT_VERSION_MANIFEST_LOCATION() {
    return QUILT_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Quilt installer version manifest-file, as a file.
   *
   * @return ./manifests/quilt-installer-manifest.xml
   * @author Griefed
   */
  public File QUILT_INSTALLER_VERSION_MANIFEST_LOCATION() {
    return QUILT_INSTALLER_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Legacy Fabric Game version manifest-file.
   *
   * @return legacy-fabric-game-manifest.json-file
   * @author Griefed
   */
  public File LEGACY_FABRIC_GAME_MANIFEST() {
    return LEGACY_FABRIC_GAME_MANIFEST;
  }

  /**
   * Legacy Fabric Loader version manifest-file.
   *
   * @return legacy-fabric-game-manifest.json-file
   * @author Griefed
   */
  public File LEGACY_FABRIC_LOADER_MANIFEST() {
    return LEGACY_FABRIC_LOADER_MANIFEST;
  }

  /**
   * Legacy Fabric Installer version manifest-file.
   *
   * @return legacy-fabric-installer-manifest.xml-file
   * @author Griefed
   */
  public File LEGACY_FABRIC_INSTALLER_MANIFEST() {
    return LEGACY_FABRIC_INSTALLER_MANIFEST;
  }

  /**
   * Path to the Legacy Fabric Game version manifest-file, as a file.
   *
   * @return ./manifests/legacy-fabric-game-manifest.json
   * @author Griefed
   */
  public File LEGACY_FABRIC_GAME_MANIFEST_LOCATION() {
    return LEGACY_FABRIC_GAME_MANIFEST_LOCATION;
  }

  /**
   * Path to the Legacy Fabric Loader version manifest-file, as a file.
   *
   * @return ./manifests/legacy-fabric-loader-manifest.json
   * @author Griefed
   */
  public File LEGACY_FABRIC_LOADER_MANIFEST_LOCATION() {
    return LEGACY_FABRIC_LOADER_MANIFEST_LOCATION;
  }

  /**
   * Path to the Legacy Fabric Installer version manifest-file, as a file.
   *
   * @return ./manifests/legacy-fabric-installer-manifest.xml
   * @author Griefed
   */
  public File LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION() {
    return LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION;
  }

  /**
   * Getter for the version of ServerPackCreator.<br> If a JAR-file compiled from a release-job from
   * a CI/CD-pipeline is used, it should contain a VERSION.txt-file which contains the version of
   * said release. If a non-release-version is used, from a regular pipeline or local dev-build,
   * then this will be set to {@code dev}.
   *
   * @return String. Returns the version of ServerPackCreator.
   * @author Griefed
   */
  public String SERVERPACKCREATOR_VERSION() {
    return SERVERPACKCREATOR_VERSION;
  }

  /**
   * String-array of modloaders supported by ServerPackCreator.
   *
   * @return Array of modloaders supported by ServerPackCreator.
   * @author Griefed
   */
  public String[] SUPPORTED_MODLOADERS() {
    return SUPPORTED_MODLOADERS;
  }

  /**
   * Directory where server-files are stored in, for example the default server-icon and
   * server.properties.
   *
   * @return server-files directory.
   * @author Griefed
   */
  public String DIRECTORY_SERVER_FILES() {
    return "server_files";
  }

  /**
   * Directory where plugins are stored in.
   *
   * @return plugins directory.
   * @author Griefed
   */
  public String DIRECTORY_PLUGINS() {
    return "plugins";
  }

  /**
   * Directory where plugin configurations are stored in.
   *
   * @return plugin configurations directory.
   * @author Griefed
   */
  public String DIRECTORY_PLUGINS_CONFIG() {
    return DIRECTORY_PLUGINS() + "/config";
  }

  /**
   * Directory where version manifests reside in.
   *
   * @return Directory where manifests reside in.
   * @author Griefed
   */
  public String MANIFEST_LOCATION() {
    return MANIFEST_LOCATION;
  }

  /**
   * Directory where Minecraft server manifests reside in.
   *
   * @return Directory where Minecraft server manifests reside in.
   * @author Griefed
   */
  public String MINECRAFT_SERVER_MANIFEST_LOCATION() {
    return MINECRAFT_SERVER_MANIFESTS_LOCATION;
  }

  /**
   * Getter for the directory in which the server packs are stored/generated in.
   *
   * @return String. Returns the directory in which the server packs are stored/generated in.
   * @author Griefed
   */
  public String getDirectoryServerPacks() {
    return directoryServerPacks;
  }

  /**
   * Acquire the default fallback list of clientside-only mods.
   *
   * @return Returns the fallback list of clientside-only mods.
   * @author Griefed
   */
  public List<String> getDefaultListFallbackMods() {
    return new ArrayList<>(FALLBACK_MODS);
  }

  /**
   * Acquire the default fallback list of clientside-only mods. If
   * {@code de.griefed.serverpackcreator.serverpack.autodiscovery.filter} is set to
   * {@link ExclusionFilter#REGEX}, a regex fallback list is returned.
   *
   * @return The fallback list of clientside-only mods.
   * @author Griefed
   */
  public List<String> getListFallbackMods() {
    if (exclusionFilter.equals(ExclusionFilter.REGEX)) {
      return new ArrayList<>(FALLBACK_MODS_REGEX);
    } else {
      return new ArrayList<>(FALLBACK_MODS);
    }
  }

  /**
   * Getter for the regex fallback list of clientside-only mods.
   *
   * @return The regex fallback list of clientside-only mods.
   * @author Griefed
   */
  public List<String> getListFallbackModsRegex() {
    return new ArrayList<>(FALLBACK_MODS_REGEX);
  }

  /**
   * Getter for the default list of directories to include in a server pack.
   *
   * @return List containing default directories to include in a server pack.
   * @author Griefed
   */
  public List<String> getDirectoriesToInclude() {
    return new ArrayList<>(DIRECTORIES_TO_INCLUDE);
  }

  /**
   * Getter for the list of directories to exclude from server packs.
   *
   * @return Returns the list of directories to exclude from server packs.
   * @author Griefed
   */
  public List<String> getDirectoriesToExclude() {
    return new ArrayList<>(DIRECTORIES_TO_EXCLUDE);
  }

  /**
   * Adder for the list of directories to exclude from server packs.
   *
   * @param entry The directory to add to the list of directories to exclude from server packs.
   * @author Griefed
   */
  public void addDirectoryToExclude(String entry) {
    if (!DIRECTORIES_TO_INCLUDE.contains(entry) && DIRECTORIES_TO_EXCLUDE.add(entry)) {
      LOG.debug("Adding " + entry + " to list of files or directories to exclude.");
    }
  }

  /**
   * Getter for whether the last loaded configuration file should be saved to as well.
   *
   * @return Boolean. Whether the last loaded configuration file should be saved to as well.
   * @author Griefed
   */
  public boolean getSaveLoadedConfiguration() {
    return saveLoadedConfiguration;
  }

  /**
   * Getter for the maximum disk usage at which JMS/Artemis will stop storing queues on disk.
   *
   * @return Integer. The maximum disk usage at which JMS/Artemis will stop storing queues on disk.
   * @author Griefed
   */
  public int getQueueMaxDiskUsage() {
    return queueMaxDiskUsage;
  }

  /**
   * Getter for whether the search for available PreReleases is enabled or disabled.<br> Depending
   * on {@code de.griefed.serverpackcreator.versioncheck.prerelease}, returns {@code true} if checks
   * for available PreReleases are enabled, {@code false} if no checks for available PreReleases
   * should be made.
   *
   * @return Boolean. Whether checks for available PreReleases are enabled.
   * @author Griefed
   */
  public boolean checkForAvailablePreReleases() {
    return checkForPreReleases;
  }

  /**
   * Get this configurations AikarsFlags
   *
   * @return Aikars flags.
   */
  public String getAikarsFlags() {
    return aikarsFlags;
  }

  /**
   * List of files to be excluded from ZIP-archives. Current filters are:
   *
   * <ul>
   *   <li>{@code MINECRAFT_VERSION} - Will be replaced with the Minecraft version of the
   *       server pack
   *   <li>{@code MODLOADER} - Will be replaced with the modloader of the server pack
   *   <li>{@code MODLOADER_VERSION} - Will be replaced with the modloader version of the
   *       server pack
   * </ul>
   * <p>
   * Should you want these filters to be expanded, open an issue on <a
   * href="https://github.com/Griefed/ServerPackCreator/issues">GitHub</a>
   *
   * @return Files and folders to exclude from the ZIP archive of a server pack.
   * @author Griefed
   */
  public List<String> getFilesToExcludeFromZipArchive() {
    return new ArrayList<>(FILES_TO_EXCLUDE_FROM_ZIP);
  }

  /**
   * Whether the exclusion of files from the ZIP-archive of the server pack is enabled.
   *
   * @return {@code true} if the exclusion is enabled.
   * @author Griefed
   */
  public boolean isZipFileExclusionEnabled() {
    return isZipFileExclusionEnabled;
  }

  /**
   * Is auto excluding of clientside-only mods enabled.
   *
   * @return {@code true} if autodiscovery is enabled.
   */
  public boolean isAutoExcludingModsEnabled() {
    return autoExcludingModsEnabled;
  }

  /**
   * Update the fallback clientside-only modlist of our {@code serverpackcreator.properties} from
   * the main-repository or one of its mirrors.
   *
   * @return {@code true} if the fallback-property was updated.
   * @author Griefed
   */
  public boolean updateFallback() {

    Properties properties;

    try (InputStream github =
        new URL(
            "https://raw.githubusercontent.com/Griefed/ServerPackCreator/main/backend/main/resources/serverpackcreator.properties")
            .openStream()) {

      properties = new Properties();
      properties.load(github);

    } catch (IOException e) {

      LOG.debug("GitHub could not be reached. Checking GitLab.", e);
      try (InputStream gitlab =
          new URL(
              "https://gitlab.com/Griefed/ServerPackCreator/-/raw/main/backend/main/resources/serverpackcreator.properties")
              .openStream()) {

        properties = new Properties();
        properties.load(gitlab);

      } catch (IOException ex) {
        LOG.debug("GitLab could not be reached. Checking GitGriefed", ex);
        try (InputStream gitgriefed =
            new URL(
                "https://git.griefed.de/Griefed/ServerPackCreator/-/raw/main/backend/main/resources/serverpackcreator.properties")
                .openStream()) {

          properties = new Properties();
          properties.load(gitgriefed);

        } catch (IOException exe) {
          LOG.debug("GitGriefed could not be reached.", exe);
          properties = null;
        }
      }
    }

    boolean updated = false;

    if (properties != null) {

      if (properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST) != null &&
          !getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST)
              .equals(properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST))
      ) {

        setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST,
            properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST));

        FALLBACK_MODS.clear();
        FALLBACK_MODS.addAll(
            Arrays.asList(getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST).split(",")));

        LOG.info(
            "The fallback-list for clientside only mods has been updated to: " + FALLBACK_MODS);
        updated = true;
      }

      if (properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX) != null
          && !getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX).equals(
          properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX))
      ) {

        setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX,
            properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX));

        FALLBACK_MODS_REGEX.clear();
        FALLBACK_MODS_REGEX.addAll(
            Arrays.asList(getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX).split(",")));

        LOG.info("The fallback regex-list for clientside only mods has been updated to: "
            + FALLBACK_MODS_REGEX);
        updated = true;
      }

    }

    if (updated) {
      saveToDisk(SERVERPACKCREATOR_PROPERTIES_FILE);
    }

    return updated;
  }

  /**
   * Is the Dark Theme currently active?
   *
   * @return {@code true} if the Dark Theme is active, otherwise false.
   * @author Griefed
   */
  public boolean isDarkTheme() {
    return Boolean.parseBoolean(acquireProperty(PROPERTY_GUI_DARKMODE, "true"));
  }

  /**
   * Set the current theme to Dark Theme or Light Theme.
   *
   * @param dark {@code true} to activate Dark Theme, {@code false} otherwise.
   * @author Griefed
   */
  public void setTheme(boolean dark) {
    if (dark) {
      defineProperty(
          PROPERTY_GUI_DARKMODE, "true");
    } else {
      setProperty(
          PROPERTY_GUI_DARKMODE, "false");
    }
  }

  /**
   * Store the ApplicationProperties to disk, overwriting the existing one.
   *
   * @param propertiesFile The file to store the properties to.
   * @author Griefed
   */
  public void saveToDisk(File propertiesFile) {
    try (OutputStream outputStream =
        Files.newOutputStream(propertiesFile.toPath())) {
      store(outputStream,
          "For details about each property, see https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help#serverpackcreatorproperties");
    } catch (IOException ex) {
      LOG.error("Couldn't write properties-file.", ex);
    }
  }

  /**
   * Whether overwriting of already existing server packs is enabled.
   *
   * @return {@code true} if it is enabled.
   * @author Griefed
   */
  public boolean isServerPacksOverwriteEnabled() {
    return serverPacksOverwriteEnabled;
  }

  /**
   * Whether cleanup procedures after server pack generation are enabled.
   *
   * @return {@code true} if it is enabled.
   * @author Griefed
   */
  public boolean isServerPackCleanupEnabled() {
    return serverPackCleanupEnabled;
  }

  /**
   * Get the currently set language.
   *
   * @return The language currently set and used.
   * @author Griefed
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Acquire this instances HasteBin server documents endpoint URL.
   *
   * @return URL to the HasteBin server documents endpoint.
   * @author Griefed
   */
  public String getHasteBinServerUrl() {
    return hasteBinServerUrl;
  }

  /**
   * Whether Minecraft pre-releases and snapshots are available to the user in, for example, the
   * GUI.
   *
   * @return {@code true} if they are available.
   * @author Griefed
   */
  public boolean enableMinecraftPreReleases() {
    return minecraftPreReleases;
  }

  /**
   * The filter method with which to determine whether a user-specified clientside-only mod should
   * be excluded from the server pack. Available settings are:<br>
   * <ul>
   *   <li>{@link ExclusionFilter#START}</li>
   *   <li>{@link ExclusionFilter#END}</li>
   *   <li>{@link ExclusionFilter#CONTAIN}</li>
   *   <li>{@link ExclusionFilter#REGEX}</li>
   *   <li>{@link ExclusionFilter#EITHER}</li>
   * </ul>
   *
   * @return The filter method by which to exclude user-specified clientside-only mods.
   */
  public ExclusionFilter exclusionFilter() {
    return exclusionFilter;
  }

  /**
   * Get the path to the specified Java executable/binary, wrapped in an {@link Optional} for your
   * convenience.
   *
   * @param javaVersion The Java version to acquire the path for.
   * @return The path to the Java executable/binary, if available.
   * @author Griefed
   */
  public Optional<String> javaPath(int javaVersion) {
    if (JAVA_PATHS.containsKey(String.valueOf(javaVersion)) && new File(
        JAVA_PATHS.get(String.valueOf(javaVersion))).isFile()) {
      return Optional.of(JAVA_PATHS.get(String.valueOf(javaVersion)));
    } else {
      return Optional.empty();
    }
  }


  /**
   * Whether to automatically update the {@code SPC_JAVA_SPC}-placeholder in the script variables
   * table with a Java path matching the required Java version for the Minecraft server.
   *
   * @return {@code true} if enabled.
   * @author Griefed
   */
  public boolean isJavaScriptAutoupdateEnabled() {
    return javaScriptAutoupdate;
  }

  public enum ExclusionFilter {
    /**
     * Does the name of a mod start with the user specified string?
     */
    START,

    /**
     * Does the name of a mod end with the user specified string?
     */
    END,

    /**
     * Does the name of a mod contain the user specified string?
     */
    CONTAIN,

    /**
     * Does the name of a mod match the user specified regex?
     */
    REGEX,

    /**
     * Does any of the above hit for the user specified string/regex?
     */
    EITHER
  }
}
