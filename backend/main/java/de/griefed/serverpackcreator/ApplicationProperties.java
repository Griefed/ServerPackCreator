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
import de.griefed.serverpackcreator.utilities.common.JarUtilities;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Base settings of ServerPackCreator, such as working directories, default list of clientside-only
 * mods, default list of directories to include in a server pack, script templates, java paths and
 * much more.
 *
 * @author Griefed
 */
@SuppressWarnings("FieldCanBeLocal")
@Component
public final class ApplicationProperties {

  private static final Logger LOG = LogManager.getLogger(ApplicationProperties.class);
  private final Properties PROPERTIES = new Properties();
  private final FileUtilities FILE_UTILITIES;
  private final SystemUtilities SYSTEM_UTILITIES;
  private final ListUtilities LIST_UTILITIES;
  private final JarUtilities JAR_UTILITIES;
  private final JarInformation JAR_INFORMATION;
  private final String SERVERPACKCREATOR_PROPERTIES = "serverpackcreator.properties";
  private final String FALLBACK_MODS_DEFAULT_ASSTRING =
      "3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedChatCore-,AdvancedChatHUD-,AdvancedCompas-,Ambience,AmbientEnvironment-,AmbientSounds_,AreYouBlind-,Armor Status HUD-,ArmorSoundTweak-,BH-Menu-,Batty's Coordinates PLUS Mod,BetterAdvancements-,BetterAnimationsCollection-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,Blur-,BorderlessWindow-,CTM-,ChunkAnimator-,ClientTweaks_,Controller Support-,Controlling-,CraftPresence-,CustomCursorMod-,CustomMainMenu-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,DetailArmorBar-,Ding-,DistantHorizons-,DripSounds-,Durability101-,DurabilityNotifier-,DynamicSurroundings-,DynamicSurroundingsHuds-,EffectsLeft-,EiraMoticons_,EnchantmentDescriptions-,EnhancedVisuals_,EquipmentCompare-,FPS-Monitor-,FabricCustomCursorMod-,Fallingleaves-,FancySpawnEggs,FancyVideo-API-,FirstPersonMod,FogTweaker-,ForgeCustomCursorMod-,FpsReducer-,FpsReducer2-,FullscreenWindowed-,GameMenuModOption-,HealthOverlay-,HorseStatsMod-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,InventorySpam-,InventoryTweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,JBRA-Client-,JustEnoughCalculation-,JustEnoughEffects-,JustEnoughProfessions-,JustEnoughResources-,LLOverlayReloaded-,LOTRDRP-,LegendaryTooltips,LegendaryTooltips-,LightOverlay-,MoBends,MouseTweaks-,MyServerIsCompatible-,Neat ,Neat-,NekosEnchantedBooks-,NoAutoJump-,NoFog-,Notes-,NotifMod-,OldJavaWarning-,OptiFine,OptiFine_,OptiForge,OptiForge-,PackMenu-,PackModeMenu-,PickUpNotifier-,Ping-,PresenceFootsteps-,RPG-HUD-,ReAuth-,ResourceLoader-,ResourcePackOrganizer,ShoulderSurfing-,ShulkerTooltip-,SimpleDiscordRichPresence-,SimpleWorldTimer-,SoundFilters-,SpawnerFix-,TRansliterationLib-,TipTheScales-,Tips-,Toast Control-,Toast-Control-,ToastControl-,TravelersTitles-,VoidFog-,WindowedFullscreen-,WorldNameRandomizer-,[1.12.2]DamageIndicatorsMod-,[1.12.2]bspkrscore-,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,auditory-,authme-,auto-reconnect-,autojoin-,autoreconnect-,axolotl-item-fix-,backtools-,bannerunlimited-,beenfo-1.19-,better-recipe-book-,betterbiomeblend-,bhmenu-,blur-,borderless-mining-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,combat_music-,configured-,controllable-,cullleaves-,cullparticles-,custom-crosshair-mod-,customdiscordrpc-,darkness-,dashloader-,defaultoptions-,desiredservers-,discordrpc-,drippyloadingscreen-,drippyloadingscreen_,dynamic-fps-,dynamic-music-,dynamiclights-,dynmus-,effective-,eggtab-,eguilib-,eiramoticons-,enchantment-lore-,entity-texture-features-,entityculling-,exhaustedstamina-,extremesoundmuffler-,fabricemotes-,fancymenu_,fancymenu_video_extension,findme-,flickerfix-,fm_audio_extension_,forgemod_VoxelMap-,freelook-,galacticraft-rpc-,gamestagesviewer-,grid-,helium-,hiddenrecipebook_,infinitemusic-,inventoryprofiles,invtweaks-,itemzoom,itlt-,jeed-,jehc-,jeiintegration_,just-enough-harvestcraft-,justenoughbeacons-,justenoughdrags-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,light-overlay-,lightfallclient-,loadmyresources_,lock_minecart_view-,lootbeams-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,mousewheelie-,movement-vision-,multihotbar-,music-duration-reducer-,musicdr-,neiRecipeHandlers-,ngrok-lan-expose-mod-,nopotionshift_,notenoughanimations-,oculus-,ornaments-,overloadedarmorbar-,panorama-,paperdoll-,phosphor-,preciseblockplacing-,realm-of-lost-souls-,rebrand-,replanter-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simple-rpc-,simpleautorun-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,soundreloader-,spoticraft-,tconplanner-,timestamps-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,wisla-,xlifeheartcolors-,yisthereautojump-";
  private final TreeSet<String> FALLBACK_CLIENTSIDE_MODS =
      new TreeSet<>(Arrays.asList(FALLBACK_MODS_DEFAULT_ASSTRING.split(",")));
  private final TreeSet<String> FALLBACK_MODS = new TreeSet<>(FALLBACK_CLIENTSIDE_MODS);
  private final String FALLBACK_MODS_DEFAULT_REGEX_ASSTRING =
      "^3dskinlayers-.*$,^Absolutely-Not-A-Zoom-Mod-.*$,^AdvancedChat-.*$,^AdvancedChatCore-.*$,^AdvancedChatHUD-.*$,^AdvancedCompas-.*$,^Ambience.*$,^AmbientEnvironment-.*$,^AmbientSounds_.*$,^AreYouBlind-.*$,^Armor Status HUD-.*$,^ArmorSoundTweak-.*$,^BH-Menu-.*$,^Batty's Coordinates PLUS Mod.*$,^BetterAdvancements-.*$,^BetterAnimationsCollection-.*$,^BetterDarkMode-.*$,^BetterF3-.*$,^BetterFoliage-.*$,^BetterPingDisplay-.*$,^BetterPlacement-.*$,^BetterTaskbar-.*$,^BetterThirdPerson.*$,^BetterTitleScreen-.*$,^Blur-.*$,^BorderlessWindow-.*$,^CTM-.*$,^ChunkAnimator-.*$,^ClientTweaks_.*$,^Controller Support-.*$,^Controlling-.*$,^CraftPresence-.*$,^CustomCursorMod-.*$,^CustomMainMenu-.*$,^DefaultOptions_.*$,^DefaultSettings-.*$,^DeleteWorldsToTrash-.*$,^DetailArmorBar-.*$,^Ding-.*$,^DistantHorizons-.*$,^DripSounds-.*$,^Durability101-.*$,^DurabilityNotifier-.*$,^DynamicSurroundings-.*$,^DynamicSurroundingsHuds-.*$,^EffectsLeft-.*$,^EiraMoticons_.*$,^EnchantmentDescriptions-.*$,^EnhancedVisuals_.*$,^EquipmentCompare-.*$,^FPS-Monitor-.*$,^FabricCustomCursorMod-.*$,^Fallingleaves-.*$,^FancySpawnEggs.*$,^FancyVideo-API-.*$,^FirstPersonMod.*$,^FogTweaker-.*$,^ForgeCustomCursorMod-.*$,^FpsReducer-.*$,^FpsReducer2-.*$,^FullscreenWindowed-.*$,^GameMenuModOption-.*$,^HealthOverlay-.*$,^HorseStatsMod-.*$,^InventoryEssentials_.*$,^InventoryHud_[1.17.1].forge-.*$,^InventorySpam-.*$,^InventoryTweaks-.*$,^ItemBorders-.*$,^ItemPhysicLite_.*$,^ItemStitchingFix-.*$,^JBRA-Client-.*$,^JustEnoughCalculation-.*$,^JustEnoughEffects-.*$,^JustEnoughProfessions-.*$,^JustEnoughResources-.*$,^LLOverlayReloaded-.*$,^LOTRDRP-.*$,^LegendaryTooltips-.*$,^LegendaryTooltips.*$,^LightOverlay-.*$,^MoBends.*$,^MouseTweaks-.*$,^MyServerIsCompatible-.*$,^Neat .*$,^Neat-.*$,^NekosEnchantedBooks-.*$,^NoAutoJump-.*$,^NoFog-.*$,^Notes-.*$,^NotifMod-.*$,^OldJavaWarning-.*$,^OptiFine.*$,^OptiFine_.*$,^OptiForge-.*$,^OptiForge.*$,^PackMenu-.*$,^PackModeMenu-.*$,^PickUpNotifier-.*$,^Ping-.*$,^PresenceFootsteps-.*$,^RPG-HUD-.*$,^ReAuth-.*$,^ResourceLoader-.*$,^ResourcePackOrganizer.*$,^ShoulderSurfing-.*$,^ShulkerTooltip-.*$,^SimpleDiscordRichPresence-.*$,^SimpleWorldTimer-.*$,^SoundFilters-.*$,^SpawnerFix-.*$,^TRansliterationLib-.*$,^TipTheScales-.*$,^Tips-.*$,^Toast Control-.*$,^Toast-Control-.*$,^ToastControl-.*$,^TravelersTitles-.*$,^VoidFog-.*$,^WindowedFullscreen-.*$,^WorldNameRandomizer-.*$,^[1.12.2]DamageIndicatorsMod-.*$,^[1.12.2]bspkrscore-.*$,^antighost-.*$,^anviltooltipmod-.*$,^appleskin-.*$,^armorchroma-.*$,^armorpointspp-.*$,^auditory-.*$,^authme-.*$,^auto-reconnect-.*$,^autojoin-.*$,^autoreconnect-.*$,^axolotl-item-fix-.*$,^backtools-.*$,^bannerunlimited-.*$,^beenfo-1.19-.*$,^better-recipe-book-.*$,^betterbiomeblend-.*$,^bhmenu-.*$,^blur-.*$,^borderless-mining-.*$,^catalogue-.*$,^charmonium-.*$,^chat_heads-.*$,^cherishedworlds-.*$,^cirback-1.0-.*$,^classicbar-.*$,^clickadv-.*$,^clienttweaks-.*$,^combat_music-.*$,^configured-.*$,^controllable-.*$,^cullleaves-.*$,^cullparticles-.*$,^custom-crosshair-mod-.*$,^customdiscordrpc-.*$,^darkness-.*$,^dashloader-.*$,^defaultoptions-.*$,^desiredservers-.*$,^discordrpc-.*$,^drippyloadingscreen-.*$,^drippyloadingscreen_.*$,^dynamic-fps-.*$,^dynamic-music-.*$,^dynamiclights-.*$,^dynmus-.*$,^effective-.*$,^eggtab-.*$,^eguilib-.*$,^eiramoticons-.*$,^enchantment-lore-.*$,^entity-texture-features-.*$,^entityculling-.*$,^exhaustedstamina-.*$,^extremesoundmuffler-.*$,^fabricemotes-.*$,^fancymenu_.*$,^fancymenu_video_extension.*$,^findme-.*$,^flickerfix-.*$,^fm_audio_extension_.*$,^forgemod_VoxelMap-.*$,^freelook-.*$,^galacticraft-rpc-.*$,^gamestagesviewer-.*$,^grid-.*$,^helium-.*$,^hiddenrecipebook_.*$,^infinitemusic-.*$,^inventoryprofiles.*$,^invtweaks-.*$,^itemzoom.*$,^itlt-.*$,^jeed-.*$,^jehc-.*$,^jeiintegration_.*$,^just-enough-harvestcraft-.*$,^justenoughbeacons-.*$,^justenoughdrags-.*$,^justzoom_.*$,^keymap-.*$,^keywizard-.*$,^konkrete_.*$,^konkrete_forge_.*$,^lazydfu-.*$,^light-overlay-.*$,^lightfallclient-.*$,^loadmyresources_.*$,^lock_minecart_view-.*$,^lootbeams-.*$,^lwl-.*$,^magnesium_extras-.*$,^maptooltip-.*$,^massunbind.*$,^mcbindtype-.*$,^mcwifipnp-.*$,^medievalmusic-.*$,^mightyarchitect-.*$,^mindful-eating-.*$,^minetogether-.*$,^mobplusplus-.*$,^modcredits-.*$,^modernworldcreation_.*$,^modmenu-.*$,^modnametooltip-.*$,^modnametooltip_.*$,^moreoverlays-.*$,^mousewheelie-.*$,^movement-vision-.*$,^multihotbar-.*$,^music-duration-reducer-.*$,^musicdr-.*$,^neiRecipeHandlers-.*$,^ngrok-lan-expose-mod-.*$,^nopotionshift_.*$,^notenoughanimations-.*$,^oculus-.*$,^ornaments-.*$,^overloadedarmorbar-.*$,^panorama-.*$,^paperdoll-.*$,^phosphor-.*$,^preciseblockplacing-.*$,^realm-of-lost-souls-.*$,^rebrand-.*$,^replanter-.*$,^rubidium-.*$,^rubidium_extras-.*$,^screenshot-to-clipboard-.*$,^shutupexperimentalsettings-.*$,^shutupmodelloader-.*$,^signtools-.*$,^simple-rpc-.*$,^simpleautorun-.*$,^smartcursor-.*$,^smoothboot-.*$,^smoothfocus-.*$,^sounddeviceoptions-.*$,^soundreloader-.*$,^spoticraft-.*$,^tconplanner-.*$,^timestamps-.*$,^tooltipscroller-.*$,^torchoptimizer-.*$,^torohealth-.*$,^totaldarkness.*$,^toughnessbar-.*$,^wisla-.*$,^xlifeheartcolors-.*$,^yisthereautojump-.*$";
  private final TreeSet<String> FALLBACK_REGEX_CLIENTSIDE_MODS =
      new TreeSet<>(Arrays.asList(FALLBACK_MODS_DEFAULT_REGEX_ASSTRING.split(",")));
  private final TreeSet<String> FALLBACK_MODS_REGEX = new TreeSet<>(FALLBACK_REGEX_CLIENTSIDE_MODS);
  private final String SERVERPACKCREATOR_VERSION;
  private final String[] SUPPORTED_MODLOADERS = new String[]{"Fabric", "Forge", "Quilt",
      "LegacyFabric"};
  private final String FALLBACK_DIRECTORIES_INCLUDE_ASSTRING = "addonpacks,blueprints,config,configs,customnpcs,defaultconfigs,global_data_packs,global_packs,kubejs,maps,mods,openloader,scripts,schematics,shrines-saves,structures,structurize,worldshape,Zoestria";
  private final TreeSet<String> FALLBACK_DIRECTORIES_INCLUDE =
      new TreeSet<>(Arrays.asList(FALLBACK_DIRECTORIES_INCLUDE_ASSTRING.split(",")));
  private final TreeSet<String> DIRECTORIES_TO_INCLUDE = new TreeSet<>(
      FALLBACK_DIRECTORIES_INCLUDE);
  private final String FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING =
      "animation,asm,cache,changelogs,craftpresence,crash-reports,downloads,icons,libraries,local,logs,overrides,packmenu,profileImage,profileImage,resourcepacks,screenshots,server_pack,shaderpacks,simple-rpc,tv-cache";
  private final TreeSet<String> FALLBACK_DIRECTORIES_EXCLUDE =
      new TreeSet<>(Arrays.asList(FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING.split(",")));
  private final TreeSet<String> DIRECTORIES_TO_EXCLUDE = new TreeSet<>(
      FALLBACK_DIRECTORIES_EXCLUDE);
  private final String FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING =
      "minecraft_server.MINECRAFT_VERSION.jar,server.jar,libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar";
  private final TreeSet<String> FALLBACK_FILES_EXCLUDE_ZIP =
      new TreeSet<>(Arrays.asList(FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING.split(",")));
  private final TreeSet<String> FILES_TO_EXCLUDE_FROM_ZIP = new TreeSet<>(
      FALLBACK_FILES_EXCLUDE_ZIP);
  private final String DEFAULT_SHELL_TEMPLATE = "default_template.sh";
  private final String DEFAULT_POWERSHELL_TEMPLATE = "default_template.ps1";
  private final String FALLBACK_SCRIPT_TEMPLATES_ASSTRING =
      DEFAULT_SHELL_TEMPLATE + "," + DEFAULT_POWERSHELL_TEMPLATE;
  private final String AIKARS_FLAGS =
      "-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 "
          + "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 "
          + "-XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 "
          + "-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 "
          + "-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem "
          + "-XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true";
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
  private final String PROPERTY_HOME_DIRECTORY = "de.griefed.serverpackcreator.home";
  private final String PROPERTY_OLD_VERSION = "de.griefed.serverpackcreator.version.old";
  private File directoryServerPacks;
  private int queueMaxDiskUsage = 90;
  private boolean saveLoadedConfiguration = false;
  private boolean checkForPreReleases = false;
  private String aikarsFlags = AIKARS_FLAGS;
  private boolean isZipFileExclusionEnabled = true;
  private boolean autoExcludingModsEnabled = true;
  private boolean serverPacksOverwriteEnabled = true;
  private boolean serverPackCleanupEnabled = true;
  private String language = "en_us";
  private String hasteBinServerUrl = "https://haste.zneix.eu/documents";
  private boolean minecraftPreReleases = false;
  private ExclusionFilter exclusionFilter = ExclusionFilter.START;
  private String javaPath = "java";
  private boolean javaScriptAutoupdate = true;
  private File serverPackCreatorPropertiesFile = null;
  private File minecraftServerManifestsDirectory = null;
  private File fabricIntermediariesManifest = null;
  private File legacyFabricGameManifest = null;
  private File legacyFabricLoaderManifest = null;
  private File legacyFabricInstallerManifest = null;
  private File fabricInstallerManifest = null;
  private File quiltVersionManifest = null;
  private File quiltInstallerManifest = null;
  private File forgeVersionManifest = null;
  private File fabricVersionManifest = null;
  private File minecraftVersionManifest = null;
  private File manifestsDirectory = null;
  private File workDirectory = null;
  private File modpacksDirectory = null;
  private File tempDirectory = null;
  private File defaultConfig = null;
  private File defaultServerProperties = null;
  private File defaultServerIcon = null;
  private File serverPackCreatorDatabase = null;
  private File addonsConfigsDirectory = null;
  private File addonsDirectory = null;
  private File homeDirectory;
  private final TreeSet<File> FALLBACK_SCRIPT_TEMPLATES =
      new TreeSet<>(
          Arrays.asList(
              new File(serverFilesDirectory(), DEFAULT_SHELL_TEMPLATE),
              new File(serverFilesDirectory(), DEFAULT_POWERSHELL_TEMPLATE)));
  private final TreeSet<File> SCRIPT_TEMPLATES = new TreeSet<>(FALLBACK_SCRIPT_TEMPLATES);
  private File langDirectory = null;

  /**
   * Initialize an instance of our application properties using the default
   * {@code serverpackcreator.properties}.
   *
   * @param fileUtilities   Instance of {@link FileUtilities} for file-operations.
   * @param systemUtilities Instance of {@link SystemUtilities} to acquire the Java path
   *                        automatically.
   * @param listUtilities   Used to print the configured fallback modlists in chunks.
   * @param jarUtilities    Instance of {@link JarUtilities} used to acquire .exe or JAR-, as well
   *                        as system information.
   * @author Griefed
   */
  @Autowired
  public ApplicationProperties(
      @NotNull FileUtilities fileUtilities,
      @NotNull SystemUtilities systemUtilities,
      @NotNull ListUtilities listUtilities,
      @NotNull JarUtilities jarUtilities) {

    this(
        new File("serverpackcreator.properties"),
        fileUtilities,
        systemUtilities,
        listUtilities,
        jarUtilities);
  }

  /**
   * Initialize an instance of our application properties using a custom properties-file.
   *
   * @param propertiesFile  The properties file from which to load the settings and configuration.
   * @param fileUtilities   Instance of {@link FileUtilities} for file-operations.
   * @param systemUtilities Instance of {@link SystemUtilities} to acquire the Java path
   *                        automatically.
   * @param listUtilities   Used to print the configured fallback modlists in chunks.
   * @param jarUtilities    Instance of {@link JarUtilities} used to acquire .exe or JAR-, as well
   *                        as system information.
   * @author Griefed
   */
  public ApplicationProperties(
      @NotNull File propertiesFile,
      @NotNull FileUtilities fileUtilities,
      @NotNull SystemUtilities systemUtilities,
      @NotNull ListUtilities listUtilities,
      @NotNull JarUtilities jarUtilities) {

    FILE_UTILITIES = fileUtilities;
    SYSTEM_UTILITIES = systemUtilities;
    LIST_UTILITIES = listUtilities;
    JAR_UTILITIES = jarUtilities;
    JAR_INFORMATION = new JarInformation();

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
    loadProperties(new File(SERVERPACKCREATOR_PROPERTIES));
  }

  /**
   * Reload from a specific properties-file.
   *
   * @param propertiesFile The properties-file with which to loadProperties the settings and
   *                       configuration.
   * @author Griefed
   */
  private void loadProperties(@NotNull File propertiesFile) {
    // Load the properties file from the classpath, providing default values.
    try (InputStream inputStream =
        new ClassPathResource(SERVERPACKCREATOR_PROPERTIES).getInputStream()) {

      PROPERTIES.load(inputStream);
    } catch (IOException ex) {
      LOG.error("Couldn't read properties file.", ex);
    }

    // If out properties-file exists in SPCs home directory, load it.
    if (new File(JAR_INFORMATION.JAR_FOLDER, SERVERPACKCREATOR_PROPERTIES).exists()) {

      try (InputStream inputStream =
          Files.newInputStream(
              new File(JAR_INFORMATION.JAR_FOLDER, SERVERPACKCREATOR_PROPERTIES).toPath())) {

        PROPERTIES.load(inputStream);
      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    // If our properties-file in the directory from which the user is executing SPC exists, load it.
    if (new File(SERVERPACKCREATOR_PROPERTIES).exists()) {
      try (InputStream inputStream =
          Files.newInputStream(new File(SERVERPACKCREATOR_PROPERTIES).toPath())) {

        PROPERTIES.load(inputStream);
      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    // Load the specified properties-file.
    if (propertiesFile.exists()) {
      try (InputStream inputStream =
          Files.newInputStream(propertiesFile.toPath())) {

        PROPERTIES.load(inputStream);
        LOG.info("Loading file: " + propertiesFile.getPath());

      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    setHome();

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

    saveToDisk(serverPackCreatorPropertiesFile());
  }

  /**
   * Get a list from our properties.
   *
   * @param key          The key of the property which holds the comma-separated list.
   * @param defaultValue The default value to set the property to in case it is undefined.
   * @return The requested list.
   * @author Griefed
   */
  private @NotNull List<String> getListProperty(@NotNull String key,
                                                @NotNull String defaultValue) {
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
  @SuppressWarnings("SameParameterValue")
  private int getIntProperty(@NotNull String key,
                             int defaultValue) {
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
  @SuppressWarnings("SameParameterValue")
  private @NotNull List<File> getFileListProperty(@NotNull String key,
                                                  @NotNull String defaultValue,
                                                  @NotNull String filePrefix) {
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
  private boolean getBoolProperty(@NotNull String key,
                                  boolean defaultValue) {
    return Boolean.parseBoolean(acquireProperty(key, String.valueOf(defaultValue)));
  }

  /**
   * Set ServerPackCreators home-directory. If ServerPackCreator is run for the first time on a
   * given machine, the current containing directory will be used and saved as the
   * home-directory.<br> If ServerPackCreator has been run before, and the home-directory-property
   * is set, then that directory will be used as ServerPackCreators continued home-directory.
   *
   * @author Griefed
   */
  private void setHome() {
    //TODO Replace with user home-directory acquisition in MS4 as per GH#438
    if (PROPERTIES.containsKey(PROPERTY_HOME_DIRECTORY) && new File(
        PROPERTIES.getProperty(PROPERTY_HOME_DIRECTORY)).isDirectory()) {
      homeDirectory = new File(PROPERTIES.getProperty(PROPERTY_HOME_DIRECTORY));
    } else {
      if (JAR_INFORMATION.JAR_FILE.isDirectory() || JAR_INFORMATION.JAR_FOLDER.toString()
                                                                              .matches(
                                                                                  ".*build.classes.java.*")) {
        homeDirectory = new File(new File("").getAbsolutePath());
      } else {
        homeDirectory = JAR_INFORMATION.JAR_FOLDER;
      }

      PROPERTIES.setProperty(PROPERTY_HOME_DIRECTORY, homeDirectory.getAbsolutePath());
    }
    LOG.info("Home directory: " + homeDirectory);
  }

  /**
   * Set the directory where the generated server packs will be stored in.
   *
   * @author Griefed
   */
  private void setServerPacksDir() {
    if (PROPERTIES.getProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SERVERPACKS).isEmpty()
        || PROPERTIES.getProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SERVERPACKS)
                     .matches("^(?:\\./)?server-packs$")) {

      directoryServerPacks = new File(
          homeDirectory, "server-packs");

    } else {
      directoryServerPacks = new File(
          PROPERTIES.getProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SERVERPACKS));
    }

    PROPERTIES.setProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SERVERPACKS,
                           directoryServerPacks.getAbsolutePath());
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
    PROPERTIES.setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST,
                           String.join(",", FALLBACK_MODS));
    LOG.info("Fallback modslist set to:");
    LIST_UTILITIES.printListToLogChunked(new ArrayList<>(FALLBACK_MODS), 5, "    ", true);

    FALLBACK_MODS_REGEX.addAll(getListProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX,
                                               FALLBACK_MODS_DEFAULT_REGEX_ASSTRING));
    PROPERTIES.setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX,
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
                                                FALLBACK_SCRIPT_TEMPLATES_ASSTRING,
                                                homeDirectory + File.separator + "server_files"
                                                    + File.separator));
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
      if (PROPERTIES.getProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY)
                    .matches("(true|false)")) {
        autoExcludingModsEnabled =
            Boolean.parseBoolean(
                PROPERTIES.getProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY));

        PROPERTIES.setProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED,
                               String.valueOf(autoExcludingModsEnabled));

        PROPERTIES.remove(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY);

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
    setLanguage(acquireProperty(PROPERTY_LANGUAGE, "en_us"));
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
      PROPERTIES.setProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_FILTER, exclusionFilter.toString());
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
    if (checkJavaPath(PROPERTIES.getProperty(PROPERTY_JAVA, ""))) {
      javaPath = PROPERTIES.getProperty(PROPERTY_JAVA);
    } else {
      PROPERTIES.setProperty(PROPERTY_JAVA, getJavaPath(""));
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
  public void setJavaPath(@NotNull String javaPath) {
    this.javaPath = getJavaPath(javaPath);
    saveToDisk(serverPackCreatorPropertiesFile());
  }

  /**
   * Check the given path to a Java installation for validity and return it, if it is valid. If the
   * passed path is a UNIX symlink or Windows lnk, it is resolved, then returned. If the passed path
   * is considered invalid, the system default is acquired and returned.
   *
   * @param pathToJava The path to check for whether it is a valid Java installation.
   * @return Returns the path to the Java installation. If user input was incorrect, SPC will try to
   * acquire the path automatically.
   * @author Griefed
   */
  private @NotNull String getJavaPath(@NotNull String pathToJava) {

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
   * Store the ApplicationProperties to disk, overwriting the existing one.
   *
   * @param propertiesFile The file to store the properties to.
   * @author Griefed
   */
  public void saveToDisk(@NotNull File propertiesFile) {
    try (OutputStream outputStream =
        Files.newOutputStream(propertiesFile.toPath())) {
      PROPERTIES.store(outputStream,
                       "For details about each property, see https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help#serverpackcreatorproperties");
    } catch (IOException ex) {
      LOG.error("Couldn't write properties-file.", ex);
    }
  }

  /**
   * The {@code serverpackcreator.properties}-file which both resulted from starting
   * ServerPackCreator and provided the settings, properties and configurations for the currently
   * running instance.
   *
   * @return serverpackcreator.properties in ServerPackCreators home-directory.
   * @author Griefed
   */
  public @NotNull File serverPackCreatorPropertiesFile() {
    if (serverPackCreatorPropertiesFile == null) {
      serverPackCreatorPropertiesFile = new File(
          homeDirectory, SERVERPACKCREATOR_PROPERTIES);
    }
    return serverPackCreatorPropertiesFile;
  }

  /**
   * Check whether the given path is a valid Java specification.
   *
   * @param pathToJava Path to the Java executable
   * @return {@code true} if the path is valid.
   * @author Griefed
   */
  private boolean checkJavaPath(@NotNull String pathToJava) {

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
  private boolean testJava(@NotNull String pathToJava) {
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
   * Sets the path to the Java 17 executable/binary.
   *
   * @author Griefed
   */
  private void setJavaScriptsVariablePaths() {
    for (int i = 8; i < 256; i++) {
      if (checkJavaPath(PROPERTIES.getProperty(PROPERTY_SCRIPT_JAVA + i, ""))) {

        if (JAVA_PATHS.containsKey(String.valueOf(i))) {
          JAVA_PATHS.replace(String.valueOf(i),
                             PROPERTIES.getProperty(PROPERTY_SCRIPT_JAVA + i));
        } else {
          JAVA_PATHS.put(String.valueOf(i),
                         PROPERTIES.getProperty(PROPERTY_SCRIPT_JAVA + i));
        }

        PROPERTIES.setProperty(PROPERTY_SCRIPT_JAVA + i, JAVA_PATHS.get(String.valueOf(i)));
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
   * Directory to which Minecraft server manifests are copied during the startup of
   * ServerPackCreator.
   * <p>
   * When the {@link de.griefed.serverpackcreator.versionmeta.VersionMeta} is initialized, the
   * manifests copied to this directory will provide ServerPackCreator with the information required
   * to check and create your server packs.
   * <p>
   * The Minecraft server manifests contain information about the Java version required, the
   * download-URL of the server-JAR and much more.
   * <p>
   * By default, this is the {@code mcserver}-directory inside the {@code manifests}-directory
   * inside ServerPackCreators home-directory.
   *
   * @return manifests/mcserver
   * @author Griefed
   */
  public @NotNull File minecraftServerManifestsDirectory() {
    if (minecraftServerManifestsDirectory == null) {
      minecraftServerManifestsDirectory = new File(
          manifestsDirectory(), "mcserver");
    }
    return minecraftServerManifestsDirectory;
  }

  /**
   * Directory to which default/fallback manifests are copied to during the startup of
   * ServerPackCreator.
   * <p>
   * When the {@link de.griefed.serverpackcreator.versionmeta.VersionMeta} is initialized, the
   * manifests copied to this directory will provide ServerPackCreator with the information required
   * to check and create your server packs.
   * <p>
   * By default, this is the {@code manifests}-directory inside ServerPackCreators home-directory.
   *
   * @return manifests
   * @author Griefed
   */
  public @NotNull File manifestsDirectory() {
    if (manifestsDirectory == null) {
      manifestsDirectory = new File(homeDirectory, "manifests");
    }
    return manifestsDirectory;
  }

  /**
   * The Fabric intermediaries manifest containing all required information about Fabrics
   * intermediaries. These intermediaries are used by Quilt, Fabric and LegacyFabric.
   * <p>
   * By default, the {@code fabric-intermediaries-manifest.json}-file resides in the
   * {@code manifests}-directory inside ServerPackCreators home-directory.
   *
   * @return manifests/fabric-intermediaries-manifest.json
   * @author Griefed
   */
  public @NotNull File fabricIntermediariesManifest() {
    if (fabricIntermediariesManifest == null) {
      fabricIntermediariesManifest = new File(
          manifestsDirectory(), "fabric-intermediaries-manifest.json");
    }
    return fabricIntermediariesManifest;
  }

  /**
   * The LegacyFabric game version manifest containing information about which Minecraft version
   * LegacyFabric is available for.
   * <p>
   * By default, the {@code legacy-fabric-game-manifest.json}-file resides in the
   * {@code manifests}-directory inside ServerPackCreators home-directory.
   *
   * @return manifests/legacy-fabric-game-manifest.json
   * @author Griefed
   */
  public @NotNull File legacyFabricGameManifest() {
    if (legacyFabricGameManifest == null) {
      legacyFabricGameManifest = new File(
          manifestsDirectory(), "legacy-fabric-game-manifest.json");
    }
    return legacyFabricGameManifest;
  }

  /**
   * LegacyFabric loader manifest containing information about Fabric loader maven versions.
   * <p>
   * By default, the {@code legacy-fabric-loader-manifest.json}-file resides in the
   * {@code manifests}-directory inside ServerPackCreators home-directory.
   *
   * @return manifests/legacy-fabric-loader-manifest.json
   * @author Griefed
   */
  public @NotNull File legacyFabricLoaderManifest() {
    if (legacyFabricLoaderManifest == null) {
      legacyFabricLoaderManifest = new File(
          manifestsDirectory(), "legacy-fabric-loader-manifest.json");
    }
    return legacyFabricLoaderManifest;
  }

  /**
   * LegacyFabric installer manifest containing information about available LegacyFabric installers
   * with which to install a server.
   * <p>
   * By default, the {@code legacy-fabric-installer-manifest.xml}-file resides in the
   * {@code manifests}-directory inside ServerPackCreators home-directory.
   *
   * @return manifests/legacy-fabric-installer-manifest.xml
   * @author Griefed
   */
  public @NotNull File legacyFabricInstallerManifest() {
    if (legacyFabricInstallerManifest == null) {
      legacyFabricInstallerManifest = new File(
          manifestsDirectory(), "legacy-fabric-installer-manifest.xml");
    }
    return legacyFabricInstallerManifest;
  }

  /**
   * Fabric installer manifest containing information about available Fabric installers with which
   * to install a server.
   * <p>
   * By default, the {@code fabric-installer-manifest.xml}-file resides in the
   * {@code manifests}-directory inside ServerPackCreators home-directory.
   *
   * @return manifests/fabric-installer-manifest.xml
   * @author Griefed
   */
  public @NotNull File fabricInstallerManifest() {
    if (fabricInstallerManifest == null) {
      fabricInstallerManifest = new File(
          manifestsDirectory(), "fabric-installer-manifest.xml");
    }
    return fabricInstallerManifest;
  }

  /**
   * Quilt version manifest containing information about available Quilt loader versions.
   * <p>
   * By default, the {@code quilt-manifest.xml}-file resides in the {@code manifests}-directory
   * inside ServerPackCreators home-directory.
   *
   * @return manifests/quilt-manifest.xml
   * @author Griefed
   */
  public @NotNull File quiltVersionManifest() {
    if (quiltVersionManifest == null) {
      quiltVersionManifest = new File(
          manifestsDirectory(), "quilt-manifest.xml");
    }
    return quiltVersionManifest;
  }

  /**
   * Quilt installer manifest containing information about available Quilt installers with which to
   * install a server.
   * <p>
   * By default, the {@code quilt-installer-manifest.xml}-file resides in the
   * {@code manifests}-directory inside ServerPackCreators home-directory.
   *
   * @return manifests/quilt-installer-manifest.xml
   * @author Griefed
   */
  public @NotNull File quiltInstallerManifest() {
    if (quiltInstallerManifest == null) {
      quiltInstallerManifest = new File(
          manifestsDirectory(), "quilt-installer-manifest.xml");
    }
    return quiltInstallerManifest;
  }

  /**
   * Forge version manifest containing information about available Forge loader versions.
   * <p>
   * By default, the {@code forge-manifest.json}-file resides in the {@code manifests}-directory
   * inside ServerPackCreators home-directory.
   *
   * @return manifests/forge-manifest.json
   * @author Griefed
   */
  public @NotNull File forgeVersionManifest() {
    if (forgeVersionManifest == null) {
      forgeVersionManifest = new File(
          manifestsDirectory(), "forge-manifest.json");
    }
    return forgeVersionManifest;
  }

  /**
   * Fabric version manifest containing information about available Fabric loader versions.
   * <p>
   * By default, the {@code fabric-manifest.xml}-file resides in the {@code manifests}-directory
   * inside ServerPackCreators home-directory.
   *
   * @return manifests/fabric-manifest.xml
   * @author Griefed
   */
  public @NotNull File fabricVersionManifest() {
    if (fabricVersionManifest == null) {
      fabricVersionManifest = new File(
          manifestsDirectory(), "fabric-manifest.xml");
    }
    return fabricVersionManifest;
  }

  /**
   * Minecraft version manifest containing information about available Minecraft versions.
   * <p>
   * By default, the {@code minecraft-manifest.json}-file resides in the {@code manifests}-directory
   * inside ServerPackCreators home-directory.
   *
   * @return manifests/minecraft-manifest.json
   * @author Griefed
   */
  public @NotNull File minecraftVersionManifest() {
    if (minecraftVersionManifest == null) {
      minecraftVersionManifest = new File(
          manifestsDirectory(), "minecraft-manifest.json");
    }
    return minecraftVersionManifest;
  }

  /**
   * Modpacks directory in which uploaded modpack ZIP-archives and extracted modpacks are stored.
   * <p>
   * By default, this is the {@code modpacks}-directory inside the {@code temp}-directory inside
   * ServerPackCreators home-directory.
   *
   * @return work/temp/modpacks
   * @author Griefed
   */
  public @NotNull File modpacksDirectory() {
    if (modpacksDirectory == null) {
      modpacksDirectory = new File(tempDirectory(), "modpacks");
    }
    return modpacksDirectory;
  }

  /**
   * Temp-directory storing files and folders required temporarily during the run of a server pack
   * generation or other operations.
   * <p>
   * One example would be when running ServerPackCreator as a webservice and uploading a zipped
   * modpack for the automatic creation of a server pack from said modpack.
   * <p>
   * Any file and/or directory inside the work-directory is considered {@code safe-to-delete},
   * meaning that it can safely be emptied when ServerPackCreator is not running, without running
   * the risk of corrupting anything. It is not recommended to empty this directory whilst
   * ServerPackCreator is running, as in that case, it may interfere with any currently running
   * operation.
   * <p>
   * By default, this directory is {@code work/temp} inside ServerPackCreators home-directory.
   *
   * @return work/temp
   * @author Griefed
   */
  public @NotNull File tempDirectory() {
    if (tempDirectory == null) {
      tempDirectory = new File(workDirectory(), "temp");
    }
    return tempDirectory;
  }

  /**
   * Work-directory for storing temporary, non-critical, files and directories.
   * <p>
   * Any file and/or directory inside the work-directory is considered {@code safe-to-delete},
   * meaning that it can safely be emptied when ServerPackCreator is not running, without running
   * the risk of corrupting anything. It is not recommended to empty this directory whilst
   * ServerPackCreator is running, as in that case, it may interfere with any currently running
   * operation.
   * <p>
   * By default, this is the {@code work}-directory inside ServerPackCreators home-directory.
   *
   * @return work
   * @author Griefed
   */
  public @NotNull File workDirectory() {
    if (workDirectory == null) {
      workDirectory = new File(homeDirectory, "work");
    }
    return workDirectory;
  }

  /**
   * Storage location for logs created by ServerPackCreator. This is the {@code logs}-directory
   * inside ServerPackCreators home-directory.
   *
   * @return logs
   * @author Griefed
   */
  public @NotNull File logsDirectory() {
    return new File(homeDirectory, "logs");
  }

  /**
   * The default shell-template for the modded server start scripts. The file returned by this
   * method does not represent the script-template in the {@code server_files}-directory. If you
   * wish access the configured script templates inside the {@code server_files}-directory, use
   * {@link #scriptTemplates()}.
   *
   * @return The default shell-script template.
   * @author Griefed
   */
  public @NotNull File defaultShellTemplate() {
    return new File(DEFAULT_SHELL_TEMPLATE);
  }

  /**
   * The default PowerShell-template for the modded server start scripts. The file returned by this
   * method does not represent the script-template in the {@code server_files}-directory. If you
   * wish access the configured script templates inside the {@code server_files}-directory, use
   * {@link #scriptTemplates()}.
   *
   * @return The default PowerShell-script template.
   * @author Griefed
   */
  public @NotNull File defaultPowershellTemplate() {
    return new File(DEFAULT_POWERSHELL_TEMPLATE);
  }

  /**
   * Configured list of script templates. Every script template resides inside the
   * {@code server_files}-directory.
   *
   * @return server_files/script templates
   * @author Griefed
   */
  public @NotNull List<File> scriptTemplates() {
    return new ArrayList<>(SCRIPT_TEMPLATES);
  }

  /**
   * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every
   * subsequent start of serverpackcreator is executed using said locale.
   *
   * @param locale The locale the user specified when they ran serverpackcreator with -lang
   *               -your_locale.
   * @author Griefed
   */
  void writeLocaleToFile(String locale) {
    if (!language.equals(locale)) {
      setLanguage(locale);
      saveToDisk(serverPackCreatorPropertiesFile());
    }
  }

  /**
   * Default configuration-file for a server pack generation inside ServerPackCreators
   * home-directory.
   *
   * @return home-directory/serverpackcreator.conf
   * @author Griefed
   */
  public @NotNull File defaultConfig() {
    if (defaultConfig == null) {
      defaultConfig = new File(homeDirectory, "serverpackcreator.conf");
    }
    return defaultConfig;
  }

  /**
   * Default server.properties-file used by Minecraft servers. This file resides in the
   * {@code server_files}-directory inside ServerPackCreators home-directory.
   *
   * @return server_files/server.properties
   * @author Griefed
   */
  public @NotNull File defaultServerProperties() {
    if (defaultServerProperties == null) {
      defaultServerProperties = new File(
          serverFilesDirectory(), "server.properties");
    }
    return defaultServerProperties;
  }

  /**
   * Directory in which default server-files are stored in.
   * <p>
   * Default server-files are, for example, the {@code server.properties}, {@code server-icon.png},
   * {@code default_template.sh} and {@code default_template.ps1}.
   * <p>
   * The properties and icon are placeholders and/or templates for the user to change to their
   * liking, should they so desire. The script-templates serve as a one-size-fits-all template for
   * supporting {@code Forge}, {@code Fabric}, {@code LegacyFabric} and {@code Quilt}.
   * <p>
   * By default, this directory is {@code server_files} inside ServerPackCreators home-directory.
   *
   * @return server_files
   * @author Griefed
   */
  public @NotNull File serverFilesDirectory() {
    return new File(homeDirectory, "server_files");
  }

  /**
   * Default server-icon.png-file used by Minecraft servers. This file resides in the
   * {@code server_files}-directory inside ServerPackCreators home-directory.
   *
   * @return server_files/server-icon.png
   * @author Griefed
   */
  public @NotNull File defaultServerIcon() {
    if (defaultServerIcon == null) {
      defaultServerIcon = new File(serverFilesDirectory(), "server-icon.png");
    }
    return defaultServerIcon;
  }

  /**
   * ServerPackCreator-database when running as a webservice. Resides inside ServerPackCreators
   * home-directory.
   *
   * @return home-directory/serverpackcreator.db
   * @author Griefed
   */
  public @NotNull File serverPackCreatorDatabase() {
    if (serverPackCreatorDatabase == null) {
      serverPackCreatorDatabase = new File(
          homeDirectory, "serverpackcreator.db");
    }
    return serverPackCreatorDatabase;
  }

  /**
   * ServerPackCreators home directory, in which all important files and folders are stored in.
   * <p>
   * Stored in {@code serverpackcreator.properties} under the
   * {@code de.griefed.serverpackcreator.home}- property.
   * <p>
   * Every operation is based on this home-directory, with the exception being the
   * {@link #serverPacksDirectory()}, which can be configured independently of ServerPackCreators
   * home-directory.
   *
   * @return home-directory of SPC.
   * @author Griefed
   */
  public @NotNull File homeDirectory() {
    return homeDirectory;
  }

  /**
   * Directory in which the language-properties for internationalization are stored in.
   * <p>
   * These are copied from the JAR-file to this directory during the startup of ServerPackCreator.
   * Users may edit them to their liking or use them as examples for adding additional languages to
   * ServerPackCreator.
   * <p>
   * By default, this is the {@code lang}-directory inside ServerPackCreators home-directory.
   *
   * @return lang
   * @author Griefed
   */
  public @NotNull File langDirectory() {
    if (langDirectory == null) {
      langDirectory = new File(homeDirectory, "lang");
    }
    return langDirectory;
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
  public @NotNull String serverPackCreatorVersion() {
    return SERVERPACKCREATOR_VERSION;
  }

  /**
   * Modloaders supported by ServerPackCreator.
   *
   * @return Array of modloaders supported by ServerPackCreator.
   * @author Griefed
   */
  public @NotNull String @NotNull [] supportedModloaders() {
    return SUPPORTED_MODLOADERS;
  }

  /**
   * Directory in which addon-specific configurations are stored in.
   * <p>
   * When ServerPackCreator starts and loads all available addons, it will also extract an addons
   * config-file, if available. This file will be stored inside the config-directory using the ID of
   * the addon as its name, with {@code .toml} appended to it. Think of this like the
   * config-directory in a modded Minecraft server. Do the names of the config-files there look
   * familiar to the mods they belong to? Well, they should!
   * <p>
   * By default, this is the {@code config}-directory inside the {@code plugins}-directory inside
   * ServerPackCreators home-directory.
   *
   * @return plugins/config
   * @author Griefed
   */
  public @NotNull File addonConfigsDirectory() {
    if (addonsConfigsDirectory == null) {
      addonsConfigsDirectory = new File(addonsDirectory(), "config");
    }
    return addonsConfigsDirectory;
  }

  /**
   * Directory in which addons for ServerPackCreator are to be placed in.
   * <p>
   * This directory not only holds any potential addons for ServerPackCreator, but also contains the
   * directory in which addon-specific config-files are stored in, as well as the
   * {@code disabled.txt}-file, which allows a user to disable any installed addon.
   * <p>
   * By default, this is the {@code plugins}-directory inside the ServerPackCreator home-directory.
   *
   * @return plugins
   * @author Griefed
   */
  public @NotNull File addonsDirectory() {
    if (addonsDirectory == null) {
      addonsDirectory = new File(homeDirectory, "plugins");
    }
    return addonsDirectory;
  }

  /**
   * Directory in which generated server packs, or server packs being generated, are stored in, as
   * well as their ZIP-archives, if created.
   * <p>
   * By default, this directory will be the {@code server-packs}-directory in the home-directory of
   * ServerPackCreator, but it can be configured using the property
   * {@code de.griefed.serverpackcreator.configuration.directories.serverpacks} and can even be
   * configured to be completely independent of ServerPackCreators home-directory.
   *
   * @return Directory in which the server packs are stored/generated in.
   * @author Griefed
   */
  public @NotNull File serverPacksDirectory() {
    return directoryServerPacks;
  }

  /**
   * Acquire the default fallback list of clientside-only mods. If
   * {@code de.griefed.serverpackcreator.serverpack.autodiscovery.filter} is set to
   * {@link ExclusionFilter#REGEX}, a regex fallback list is returned.
   *
   * @return The fallback list of clientside-only mods.
   * @author Griefed
   */
  public @NotNull List<String> getListFallbackMods() {
    if (exclusionFilter.equals(ExclusionFilter.REGEX)) {
      return new ArrayList<>(FALLBACK_MODS_REGEX);
    } else {
      return new ArrayList<>(FALLBACK_MODS);
    }
  }

  /**
   * Getter for the default list of directories to include in a server pack.
   *
   * @return List containing default directories to include in a server pack.
   * @author Griefed
   */
  public @NotNull List<String> getDirectoriesToInclude() {
    return new ArrayList<>(DIRECTORIES_TO_INCLUDE);
  }

  /**
   * Getter for the list of directories to exclude from server packs.
   *
   * @return Returns the list of directories to exclude from server packs.
   * @author Griefed
   */
  public @NotNull List<String> getDirectoriesToExclude() {
    return new ArrayList<>(DIRECTORIES_TO_EXCLUDE);
  }

  /**
   * Adder for the list of directories to exclude from server packs.
   *
   * @param entry The directory to add to the list of directories to exclude from server packs.
   * @author Griefed
   */
  private void addDirectoryToExclude(@NotNull String entry) {
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
  public @NotNull String getAikarsFlags() {
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
  public @NotNull List<String> getFilesToExcludeFromZipArchive() {
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
          !PROPERTIES.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST)
                     .equals(properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST))
      ) {

        PROPERTIES.setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST,
                               properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST));

        FALLBACK_MODS.clear();
        FALLBACK_MODS.addAll(
            Arrays.asList(
                PROPERTIES.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST).split(",")));

        LOG.info(
            "The fallback-list for clientside only mods has been updated to: " + FALLBACK_MODS);
        updated = true;
      }

      if (properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX) != null
          && !PROPERTIES.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX).equals(
          properties.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX))
      ) {

        PROPERTIES.setProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX,
                               properties.getProperty(
                                   PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX));

        FALLBACK_MODS_REGEX.clear();
        FALLBACK_MODS_REGEX.addAll(
            Arrays.asList(
                PROPERTIES.getProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX).split(",")));

        LOG.info("The fallback regex-list for clientside only mods has been updated to: "
                     + FALLBACK_MODS_REGEX);
        updated = true;
      }

    }

    if (updated) {
      saveToDisk(serverPackCreatorPropertiesFile());
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
   * Get a property from our ApplicationProperties. If the property is not available, it is created
   * with the specified value, thus allowing subsequent calls.
   *
   * @param key          The key of the property to acquire.
   * @param defaultValue The default value for the specified key in case the key is not present or
   *                     empty.
   * @return The value stored in the specified key.
   * @author Griefed
   */
  private @NotNull String acquireProperty(@NotNull String key,
                                          @NotNull String defaultValue) {
    if (PROPERTIES.getProperty(key) == null) {
      return defineProperty(key, defaultValue);
    } else {
      return PROPERTIES.getProperty(key, defaultValue);
    }
  }

  /**
   * Set a property in our ApplicationProperties.
   *
   * @param key   The key in which to store the property.
   * @param value The value to store in the specified key.
   * @author Griefed
   */
  private @NotNull String defineProperty(@NotNull String key,
                                         @NotNull String value) {
    PROPERTIES.setProperty(key, value);
    return value;
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
      PROPERTIES.setProperty(
          PROPERTY_GUI_DARKMODE, "false");
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
  public @NotNull String getLanguage() {
    return language;
  }

  /**
   * Overwrite the language used so the next run of ServerPackCreator uses that language setting.
   *
   * @param locale The language to set for the next run.
   * @author Griefed
   */
  public void setLanguage(@NotNull String locale) {
    language = locale;
    PROPERTIES.setProperty(PROPERTY_LANGUAGE, language);
    LOG.info("Language set to: " + language);
  }

  /**
   * Acquire this instances HasteBin server documents endpoint URL.
   *
   * @return URL to the HasteBin server documents endpoint.
   * @author Griefed
   */
  public @NotNull String getHasteBinServerUrl() {
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
  public @NotNull ExclusionFilter exclusionFilter() {
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
  public @NotNull Optional<String> javaPath(int javaVersion) {
    if (JAVA_PATHS.containsKey(String.valueOf(javaVersion)) && new File(
        JAVA_PATHS.get(String.valueOf(javaVersion))).isFile()) {
      return Optional.of(JAVA_PATHS.get(String.valueOf(javaVersion)));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Set the old version of ServerPackCreator used to perform necessary migrations between the old
   * and the current version.
   *
   * @param version Old version used before upgrading to the current version.
   * @author Griefed
   */
  void setOldVersion(@NotNull String version) {
    PROPERTIES.setProperty(PROPERTY_OLD_VERSION, version);
    saveToDisk(serverPackCreatorPropertiesFile());
  }

  /**
   * Get the old version of ServerPackCreator used to perform necessary migrations between the old
   * and the current version.
   *
   * @return Old version used before updating. Empty if this is the first run of ServerPackCreator.
   */
  @NotNull String oldVersion() {
    return PROPERTIES.getProperty(PROPERTY_OLD_VERSION, "");
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

  /**
   * The folder containing the ServerPackCreator.exe or JAR-file.
   *
   * @return Folder containing the ServerPackCreator.exe or JAR-file.
   * @author Griefed
   */
  public @NotNull File getJarFolder() {
    return JAR_INFORMATION.JAR_FOLDER;
  }

  /**
   * The .exe or JAR-file of ServerPackCreator.
   *
   * @return The .exe or JAR-file of ServerPackCreator.
   * @author Griefed
   */
  public @NotNull File getJarFile() {
    return JAR_INFORMATION.JAR_FILE;
  }

  /**
   * The name of the .exe or JAR-file.
   *
   * @return The name of the .exe or JAR-file.
   * @author Griefed
   */
  public @NotNull String getJarName() {
    return JAR_INFORMATION.JAR_NAME;
  }

  /**
   * The Java version used to run ServerPackCreator.
   *
   * @return Java version.
   * @author Griefed
   */
  public @NotNull String getJavaVersion() {
    return JAR_INFORMATION.JAVA_VERSION;
  }

  /**
   * Architecture of the operating system on which ServerPackCreator is running on.
   *
   * @return Arch.
   * @author Griefed
   */
  public @NotNull String getOSArch() {
    return JAR_INFORMATION.OS_ARCH;
  }

  /**
   * The name of the operating system on which ServerPackCreator is running on.
   *
   * @return OS name.
   * @author Griefed
   */
  public @NotNull String getOSName() {
    return JAR_INFORMATION.OS_NAME;
  }

  /**
   * The version of the OS on which ServerPackCreator is running on.
   *
   * @return Version of the OS.
   * @author Griefed
   */
  public @NotNull String getOSVersion() {
    return JAR_INFORMATION.OS_VERSION;
  }

  /**
   * Whether a .exe or JAR-file was used for running ServerPackCreator.
   *
   * @return {@code true} if a .exe was/is used.
   * @author Griefed
   */
  public boolean isExe() {
    return JAR_INFORMATION.IS_EXE;
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

  /**
   * Stores values gathered by {@link JarUtilities#jarInformation(Class)} for easy access. Values
   * stored and provided by this class are:
   * <ul>
   *   <li>The directory in which the exe or JAR reside in</li>
   *   <li>The file used to start ServerPackCreator. Either a exe or a JAR</li>
   *   <li>The filename of the exe or JAR</li>
   *   <li>The Java version with which ServerPackCreator is being used</li>
   *   <li>The operating systems architecture</li>
   *   <li>The operating systems name</li>
   *   <li>The operating systems version</li>
   *   <li>Whether a exe is being used for running ServerPackCreator</li>
   * </ul>
   */
  private class JarInformation {

    private final File JAR_FOLDER;
    private final File JAR_FILE;
    private final String JAR_NAME;
    private final String JAVA_VERSION;
    private final String OS_ARCH;
    private final String OS_NAME;
    private final String OS_VERSION;
    private final boolean IS_EXE;

    private JarInformation() {
      HashMap<String, String> sysInfo = new HashMap<>(10);
      sysInfo.putAll(JAR_UTILITIES.jarInformation(ApplicationProperties.class));

      JAR_FILE = new File(sysInfo.get("jarPath"));
      JAR_FOLDER = JAR_FILE.getParentFile();
      JAR_NAME = sysInfo.get("jarName");
      JAVA_VERSION = sysInfo.get("javaVersion");
      OS_ARCH = sysInfo.get("osArch");
      OS_NAME = sysInfo.get("osName");
      OS_VERSION = sysInfo.get("osVersion");
      IS_EXE = JAR_NAME.endsWith(".exe");
    }
  }
}
