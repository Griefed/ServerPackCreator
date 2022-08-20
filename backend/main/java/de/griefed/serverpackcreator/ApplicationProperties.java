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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
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
@Component
public final class ApplicationProperties extends Properties {

  private static final Logger LOG = LogManager.getLogger(ApplicationProperties.class);

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
      "Armor Status HUD-,[1.12.2]bspkrscore-,[1.12.2]DamageIndicatorsMod-,3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedCompas-,AdvancementPlaques-,Ambience,AmbientEnvironment-,AmbientSounds_,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,ArmorSoundTweak-,AromaBackup-,authme-,autobackup-,autoreconnect-,auto-reconnect-,axolotl-item-fix-,backtools-,Backups-,bannerunlimited-,Batty's Coordinates PLUS Mod,beenfo-1.19-,BetterAdvancements-,BetterAnimationsCollection-,betterbiomeblend-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,better-recipe-book-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,bhmenu-,BH-Menu-,blur-,Blur-,borderless-mining-,BorderlessWindow-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,ChunkAnimator-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,ClientTweaks_,combat_music-,configured-,controllable-,Controller Support-,Controlling-,CraftPresence-,CTM-,cullleaves-,cullparticles-,custom-crosshair-mod-,CustomCursorMod-,customdiscordrpc-,CustomMainMenu-,darkness-,dashloader-,defaultoptions-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,desiredservers-,DetailArmorBar-,Ding-,discordrpc-,DistantHorizons-,drippyloadingscreen-,drippyloadingscreen_,DripSounds-,Durability101-,DurabilityNotifier-,dynamic-fps-,dynamiclights-,dynamic-music-,DynamicSurroundings-,DynamicSurroundingsHuds-,dynmus-,effective-,EffectsLeft-,eggtab-,eguilib-,eiramoticons-,EiraMoticons_,EnchantmentDescriptions-,enchantment-lore-,EnhancedVisuals_,entityculling-,entity-texture-features-,EquipmentCompare-,exhaustedstamina-,extremesoundmuffler-,FabricCustomCursorMod-,fabricemotes-,Fallingleaves-,fancymenu_,fancymenu_video_extension,FancySpawnEggs,FancyVideo-API-,findme-,FirstPersonMod,flickerfix-,fm_audio_extension_,FogTweaker-,ForgeCustomCursorMod-,forgemod_VoxelMap-,FPS-Monitor-,FpsReducer-,FpsReducer2-,freelook-,ftb-backups-,ftbbackups2-,FullscreenWindowed-,galacticraft-rpc-,GameMenuModOption-,gamestagesviewer-,grid-,HealthOverlay-,hiddenrecipebook_,HorseStatsMod-,infinitemusic-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,inventoryprofiles,InventorySpam-,InventoryTweaks-,invtweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,itemzoom,itlt-,JBRA-Client-,jeed-,jehc-,jeiintegration_,justenoughbeacons-,JustEnoughCalculation-,justenoughdrags-,JustEnoughEffects-,just-enough-harvestcraft-,JustEnoughProfessions-,JustEnoughResources-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,LegendaryTooltips,LegendaryTooltips-,lightfallclient-,LightOverlay-,light-overlay-,LLOverlayReloaded-,loadmyresources_,lock_minecart_view-,lootbeams-,LOTRDRP-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,MoBends,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,MouseTweaks-,mousewheelie-,movement-vision-,multihotbar-,musicdr-,music-duration-reducer-,MyServerIsCompatible-,Neat-,Neat ,neiRecipeHandlers-,NekosEnchantedBooks-,ngrok-lan-expose-mod-,NoAutoJump-,NoFog-,nopotionshift_,notenoughanimations-,Notes-,NotifMod-,oculus-,OldJavaWarning-,openbackup-,OptiFine,OptiForge,OptiForge-,ornaments-,overloadedarmorbar-,PackMenu-,PackModeMenu-,panorama-,paperdoll-,phosphor-,PickUpNotifier-,Ping-,preciseblockplacing-,PresenceFootsteps-,realm-of-lost-souls-,ReAuth-,rebrand-,replanter-,ResourceLoader-,ResourcePackOrganizer,RPG-HUD-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,ShoulderSurfing-,ShulkerTooltip-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simpleautorun-,simplebackup-,SimpleBackups-,SimpleDiscordRichPresence-,simple-rpc-,SimpleWorldTimer-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,SoundFilters-,soundreloader-,SpawnerFix-,spoticraft-,tconplanner-,textile_backup-,timestamps-,Tips-,TipTheScales-,Toast Control-,ToastControl-,Toast-Control-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,TRansliterationLib-,TravelersTitles-,VoidFog-,WindowedFullscreen-,wisla-,WorldNameRandomizer-,xlifeheartcolors-,yisthereautojump-";
  /**
   * Default fallback clientside-only mods list.
   */
  private final List<String> FALLBACK_CLIENTSIDE_MODS =
      new ArrayList<>(Arrays.asList(FALLBACK_MODS_DEFAULT_ASSTRING.split(",")));
  /**
   * List of mods which should be excluded from server packs.
   */
  private List<String> listFallbackMods = FALLBACK_CLIENTSIDE_MODS;
  /**
   * Default fallback clientside-only mods, regex.
   */
  private final String FALLBACK_MODS_DEFAULT_REGEX_ASSTRING =
      "^Armor Status HUD-.*$,^[1.12.2]bspkrscore-.*$,^[1.12.2]DamageIndicatorsMod-.*$,^3dskinlayers-.*$,^Absolutely-Not-A-Zoom-Mod-.*$,^AdvancedChat-.*$,^AdvancedCompas-.*$,^AdvancementPlaques-.*$,^Ambience.*$,^AmbientEnvironment-.*$,^AmbientSounds_.*$,^antighost-.*$,^anviltooltipmod-.*$,^appleskin-.*$,^armorchroma-.*$,^armorpointspp-.*$,^ArmorSoundTweak-.*$,^AromaBackup-.*$,^authme-.*$,^autobackup-.*$,^autoreconnect-.*$,^auto-reconnect-.*$,^axolotl-item-fix-.*$,^backtools-.*$,^Backups-.*$,^bannerunlimited-.*$,^Batty's Coordinates PLUS Mod.*$,^beenfo-1.19-.*$,^BetterAdvancements-.*$,^BetterAnimationsCollection-.*$,^betterbiomeblend-.*$,^BetterDarkMode-.*$,^BetterF3-.*$,^BetterFoliage-.*$,^BetterPingDisplay-.*$,^BetterPlacement-.*$,^better-recipe-book-.*$,^BetterTaskbar-.*$,^BetterThirdPerson.*$,^BetterTitleScreen-.*$,^bhmenu-.*$,^BH-Menu-.*$,^blur-.*$,^borderless-mining-.*$,^BorderlessWindow-.*$,^catalogue-.*$,^charmonium-.*$,^chat_heads-.*$,^cherishedworlds-.*$,^ChunkAnimator-.*$,^cirback-1.0-.*$,^classicbar-.*$,^clickadv-.*$,^clienttweaks-.*$,^ClientTweaks_.*$,^combat_music-.*$,^configured-.*$,^controllable-.*$,^Controller Support-.*$,^Controlling-.*$,^CraftPresence-.*$,^CTM-.*$,^cullleaves-.*$,^cullparticles-.*$,^custom-crosshair-mod-.*$,^CustomCursorMod-.*$,^customdiscordrpc-.*$,^CustomMainMenu-.*$,^darkness-.*$,^dashloader-.*$,^defaultoptions-.*$,^DefaultOptions_.*$,^DefaultSettings-.*$,^DeleteWorldsToTrash-.*$,^desiredservers-.*$,^DetailArmorBar-.*$,^Ding-.*$,^discordrpc-.*$,^DistantHorizons-.*$,^drippyloadingscreen-.*$,^drippyloadingscreen_.*$,^DripSounds-.*$,^Durability101-.*$,^DurabilityNotifier-.*$,^dynamic-fps-.*$,^dynamiclights-.*$,^dynamic-music-.*$,^DynamicSurroundings-.*$,^DynamicSurroundingsHuds-.*$,^dynmus-.*$,^effective-.*$,^EffectsLeft-.*$,^eggtab-.*$,^eguilib-.*$,^eiramoticons-.*$,^EiraMoticons_.*$,^EnchantmentDescriptions-.*$,^enchantment-lore-.*$,^EnhancedVisuals_.*$,^entityculling-.*$,^entity-texture-features-.*$,^EquipmentCompare-.*$,^exhaustedstamina-.*$,^extremesoundmuffler-.*$,^FabricCustomCursorMod-.*$,^fabricemotes-.*$,^Fallingleaves-.*$,^fancymenu_.*$,^fancymenu_video_extension.*$,^FancySpawnEggs.*$,^FancyVideo-API-.*$,^findme-.*$,^FirstPersonMod.*$,^flickerfix-.*$,^fm_audio_extension_.*$,^FogTweaker-.*$,^ForgeCustomCursorMod-.*$,^forgemod_VoxelMap-.*$,^FPS-Monitor-.*$,^FpsReducer-.*$,^FpsReducer2-.*$,^freelook-.*$,^ftb-backups-.*$,^ftbbackups2-.*$,^FullscreenWindowed-.*$,^galacticraft-rpc-.*$,^GameMenuModOption-.*$,^gamestagesviewer-.*$,^grid-.*$,^HealthOverlay-.*$,^hiddenrecipebook_.*$,^HorseStatsMod-.*$,^infinitemusic-.*$,^InventoryEssentials_.*$,^InventoryHud_[1.17.1].forge-.*$,^inventoryprofiles.*$,^InventorySpam-.*$,^InventoryTweaks-.*$,^invtweaks-.*$,^ItemBorders-.*$,^ItemPhysicLite_.*$,^ItemStitchingFix-.*$,^itemzoom.*$,^itlt-.*$,^JBRA-Client-.*$,^jeed-.*$,^jehc-.*$,^jeiintegration_.*$,^justenoughbeacons-.*$,^JustEnoughCalculation-.*$,^justenoughdrags-.*$,^JustEnoughEffects-.*$,^just-enough-harvestcraft-.*$,^JustEnoughProfessions-.*$,^JustEnoughResources-.*$,^justzoom_.*$,^keymap-.*$,^keywizard-.*$,^konkrete_.*$,^konkrete_forge_.*$,^lazydfu-.*$,^LegendaryTooltips.*$,^LegendaryTooltips-.*$,^lightfallclient-.*$,^LightOverlay-.*$,^light-overlay-.*$,^LLOverlayReloaded-.*$,^loadmyresources_.*$,^lock_minecart_view-.*$,^lootbeams-.*$,^LOTRDRP-.*$,^lwl-.*$,^magnesium_extras-.*$,^maptooltip-.*$,^massunbind.*$,^mcbindtype-.*$,^mcwifipnp-.*$,^medievalmusic-.*$,^mightyarchitect-.*$,^mindful-eating-.*$,^minetogether-.*$,^MoBends.*$,^mobplusplus-.*$,^modcredits-.*$,^modernworldcreation_.*$,^modmenu-.*$,^modnametooltip-.*$,^modnametooltip_.*$,^moreoverlays-.*$,^MouseTweaks-.*$,^mousewheelie-.*$,^movement-vision-.*$,^multihotbar-.*$,^musicdr-.*$,^music-duration-reducer-.*$,^MyServerIsCompatible-.*$,^Neat-.*$,^Neat .*$,^neiRecipeHandlers-.*$,^NekosEnchantedBooks-.*$,^ngrok-lan-expose-mod-.*$,^NoAutoJump-.*$,^NoFog-.*$,^nopotionshift_.*$,^notenoughanimations-.*$,^Notes-.*$,^NotifMod-.*$,^oculus-.*$,^OldJavaWarning-.*$,^openbackup-.*$,^OptiFine.*$,^OptiForge.*$,^OptiForge-.*$,^ornaments-.*$,^overloadedarmorbar-.*$,^PackMenu-.*$,^PackModeMenu-.*$,^panorama-.*$,^paperdoll-.*$,^phosphor-.*$,^PickUpNotifier-.*$,^Ping-.*$,^preciseblockplacing-.*$,^PresenceFootsteps-.*$,^realm-of-lost-souls-.*$,^ReAuth-.*$,^rebrand-.*$,^replanter-.*$,^ResourceLoader-.*$,^ResourcePackOrganizer.*$,^RPG-HUD-.*$,^rubidium-.*$,^rubidium_extras-.*$,^screenshot-to-clipboard-.*$,^ShoulderSurfing-.*$,^ShulkerTooltip-.*$,^shutupexperimentalsettings-.*$,^shutupmodelloader-.*$,^signtools-.*$,^simpleautorun-.*$,^simplebackup-.*$,^SimpleBackups-.*$,^SimpleDiscordRichPresence-.*$,^simple-rpc-.*$,^SimpleWorldTimer-.*$,^smartcursor-.*$,^smoothboot-.*$,^smoothfocus-.*$,^sounddeviceoptions-.*$,^SoundFilters-.*$,^soundreloader-.*$,^SpawnerFix-.*$,^spoticraft-.*$,^tconplanner-.*$,^textile_backup-.*$,^timestamps-.*$,^Tips-.*$,^TipTheScales-.*$,^Toast Control-.*$,^ToastControl-.*$,^Toast-Control-.*$,^tooltipscroller-.*$,^torchoptimizer-.*$,^torohealth-.*$,^totaldarkness.*$,^toughnessbar-.*$,^TRansliterationLib-.*$,^TravelersTitles-.*$,^VoidFog-.*$,^WindowedFullscreen-.*$,^wisla-.*$,^WorldNameRandomizer-.*$,^xlifeheartcolors-.*$,^yisthereautojump-.*$";
  /**
   * Default fallback clientside-only mods list, regex.
   */
  private final List<String> FALLBACK_REGEX_CLIENTSIDE_MODS =
      new ArrayList<>(Arrays.asList(FALLBACK_MODS_DEFAULT_REGEX_ASSTRING.split(",")));
  /**
   * List of mods which should be excluded from server packs, in regex format.
   */
  private List<String> listFallbackModsRegex = FALLBACK_REGEX_CLIENTSIDE_MODS;
  /**
   * ServerPackCreator version.
   */
  private final String SERVERPACKCREATOR_VERSION;
  /**
   * Supported modloaders.
   */
  private final String[] SUPPORTED_MODLOADERS = new String[]{"Fabric", "Forge", "Quilt"};
  /**
   * Default directories to include in the server pack.
   */
  private final String FALLBACK_DIRECTORIES_INCLUDE_ASSTRING = "mods,config,defaultconfigs,scripts";
  /**
   * Default list of directories to include in the server pack.
   */
  private final List<String> FALLBACK_DIRECTORIES_INCLUDE =
      new ArrayList<>(Arrays.asList(FALLBACK_DIRECTORIES_INCLUDE_ASSTRING.split(",")));
  /**
   * List of directories which must not be excluded from server packs. Default is mods, config,
   * defaultconfigs, scripts, saves, seeds, libraries.
   */
  private List<String> directoriesToInclude = FALLBACK_DIRECTORIES_INCLUDE;
  /**
   * Default directories to exclude from the server pack.
   */
  private final String FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING =
      "overrides,packmenu,resourcepacks,server_pack,fancymenu,libraries";
  /**
   * Default list of directories to exclude from the server pack.
   */
  private final List<String> FALLBACK_DIRECTORIES_EXCLUDE =
      new ArrayList<>(Arrays.asList(FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING.split(",")));
  /**
   * List of directories which should be excluded from server packs. Default is overrides, packmenu,
   * resourcepacks, server_pack, fancymenu.
   */
  private List<String> directoriesToExclude = FALLBACK_DIRECTORIES_EXCLUDE;
  /**
   * Default files to exclude from a server pack ZIP-archive.
   */
  private final String FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING =
      "minecraft_server.MINECRAFT_VERSION.jar,server.jar,libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar";
  /**
   * Default list of files to exclude from a server pack ZIP-archive.
   */
  private final List<String> FALLBACK_FILES_EXCLUDE_ZIP =
      new ArrayList<>(Arrays.asList(FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING.split(",")));
  /**
   * List of files to be excluded from ZIP-archives.
   */
  private List<String> filesToExcludeFromZipArchive = FALLBACK_FILES_EXCLUDE_ZIP;
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
  private final List<File> FALLBACK_SCRIPT_TEMPLATES =
      new ArrayList<>(
          Arrays.asList(
              new File("server_files/" + DEFAULT_SHELL_TEMPLATE),
              new File("server_files/" + DEFAULT_POWERSHELL_TEMPLATE)));
  /**
   * List of templates used for start-script creation.
   */
  private List<File> scriptTemplates = FALLBACK_SCRIPT_TEMPLATES;
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
  /**
   * Fabric intermediaries manifest file.
   */
  private final File FABRIC_INTERMEDIARIES_MANIFEST_LOCATION =
      new File("./work/fabric-intermediaries-manifest.json");
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
      new File("./work/minecraft-manifest.json");
  /**
   * Storage location for Forge version manifest file.
   */
  private final File FORGE_VERSION_MANIFEST_LOCATION = new File("./work/forge-manifest.json");
  /**
   * Storage location for Fabric version manifest file.
   */
  private final File FABRIC_VERSION_MANIFEST_LOCATION = new File("./work/fabric-manifest.xml");
  /**
   * Storage location for Fabric installer version manifest file.
   */
  private final File FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION =
      new File("./work/fabric-installer-manifest.xml");
  /**
   * Storage location for Quilt version manifest file.
   */
  private final File QUILT_VERSION_MANIFEST_LOCATION = new File("./work/quilt-manifest.xml");
  /**
   * Storage location for Quilt installer version manifest file.
   */
  private final File QUILT_INSTALLER_VERSION_MANIFEST_LOCATION =
      new File("./work/quilt-installer-manifest.xml");
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
   * Whether ServerPackCreator should check for available PreReleases. Set to <code>true</code> to
   * get notified about available PreReleases. Set to <code>false</code> if you only want stable
   * releases.
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
   * <code>ServerPackHandler#cleanUpServerPack(...)</code> for details.
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
   * Initialize an instance of our application properties using the default
   * <code>serverpackcreator.properties</code>.
   *
   * @author Griefed
   */
  @Autowired
  public ApplicationProperties() {
    this(new File("serverpackcreator.properties"));
  }

  /**
   * Initialize an instance of our application properties using a custom properties-file.
   *
   * @param propertiesFile The properties file from which to load the settings and configuration.
   * @author Griefed
   */
  public ApplicationProperties(File propertiesFile) {
    super();

    // Load the properties file from the classpath, providing default values.
    try (InputStream inputStream =
        new ClassPathResource(SERVERPACKCREATOR_PROPERTIES).getInputStream()) {
      load(inputStream);
    } catch (IOException ex) {
      LOG.error("Couldn't read properties file.", ex);
    }

    String version = ApplicationProperties.class.getPackage().getImplementationVersion();
    if (version != null) {
      SERVERPACKCREATOR_VERSION = version;
    } else {
      SERVERPACKCREATOR_VERSION = "dev";
    }

    reload(propertiesFile);
  }

  /**
   * Reload serverpackcreator.properties.
   *
   * @author Griefed
   */
  public void reload() {
    reload(SERVERPACKCREATOR_PROPERTIES_FILE);
  }

  /**
   * Reload from a specific properties-file.
   *
   * @param propertiesFile The properties-file with which to reload the settings and configuration.
   * @author Griefed
   */
  private void reload(File propertiesFile) {
    if (SERVERPACKCREATOR_PROPERTIES_FILE.exists()) {
      /*
       * If our properties-file exists, load it to ensure we always have base settings available.
       */
      try (InputStream inputStream =
          Files.newInputStream(SERVERPACKCREATOR_PROPERTIES_FILE.toPath())) {
        load(inputStream);
      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    if (propertiesFile.exists()) {
      /*
       * Load the specified properties-file.
       */
      try (InputStream inputStream =
          Files.newInputStream(propertiesFile.toPath())) {
        load(inputStream);
      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    setServerPacksDir();

    setFallbackModsList();

    setDirsToExcludeList();

    setDirsToIncludeList();

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

    saveToDisk(propertiesFile);
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
    List<File> files = new ArrayList<>();
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
    LOG.debug("Server packs directory set to: " + directoryServerPacks);
  }

  /**
   * Set up our fallback list of clientside-only mods.
   *
   * @author Griefed
   */
  private void setFallbackModsList() {
    listFallbackMods = getListProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST,
        FALLBACK_MODS_DEFAULT_ASSTRING);
    LOG.debug("Fallback modslist set to: " + listFallbackMods);

    listFallbackModsRegex = getListProperty(PROPERTY_CONFIGURATION_FALLBACKMODSLIST_REGEX,
        FALLBACK_MODS_DEFAULT_REGEX_ASSTRING);
    LOG.debug("Fallback regex modslist set to: " + listFallbackModsRegex);
  }

  /**
   * List of directories which can be excluded from server packs
   *
   * @author Griefed
   */
  private void setDirsToExcludeList() {
    directoriesToExclude = getListProperty(PROPERTY_CONFIGURATION_DIRECTORIES_SHOULDEXCLUDE,
        FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING);
    LOG.debug("Directories to exclude set to: " + directoriesToExclude);
  }

  /**
   * List of directories which should always be included in a server pack, no matter what the users
   * specify
   *
   * @author Griefed
   */
  private void setDirsToIncludeList() {
    directoriesToInclude = getListProperty(PROPERTY_CONFIGURATION_DIRECTORIES_MUSTINCLUDE,
        FALLBACK_DIRECTORIES_INCLUDE_ASSTRING);
    LOG.debug("Directories which must always be included set to: " + directoriesToInclude);
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
    LOG.debug("Queue max disk usage set to: " + queueMaxDiskUsage);
  }

  /**
   * Whether the last loaded configuration should be saved to as well.
   *
   * @author Griefed
   */
  private void setSaveLoadedConfiguration() {
    saveLoadedConfiguration = getBoolProperty(PROPERTY_CONFIGURATION_SAVELOADEDCONFIG, false);
    LOG.debug("Save last loaded config set to: " + saveLoadedConfiguration);
  }

  /**
   * Whether to check for pre-releases as well.
   *
   * @author Griefed
   */
  private void setCheckForPreReleases() {
    checkForPreReleases = getBoolProperty(PROPERTY_VERSIONCHECK_PRERELEASE, false);
    LOG.debug("Set check for pre-releases to: " + checkForPreReleases);
  }

  /**
   * Aikars flags to set when the user so desires.
   *
   * @author Griefed
   */
  private void setAikarsFlags() {
    aikarsFlags = acquireProperty(PROPERTY_CONFIGURATION_AIKAR, AIKARS_FLAGS);
    LOG.debug("Aikars flags set to: " + aikarsFlags);
  }

  /**
   * Files or folders which should be excluded from a ZIP-archive.
   *
   * @author Griefed
   */
  private void setFilesToExcludeFromZip() {
    filesToExcludeFromZipArchive = getListProperty(PROPERTY_SERVERPACK_ZIP_EXCLUDE,
        FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING);
    LOG.debug(
        "Files which must be excluded from ZIP-archives set to: " + filesToExcludeFromZipArchive);
  }

  /**
   * Files and folders which should be included in a server pack.
   *
   * @author Griefed
   */
  private void setZipFileExclusionEnabled() {
    isZipFileExclusionEnabled = getBoolProperty(PROPERTY_SERVERPACK_ZIP_EXCLUDE_ENABLED, true);
    LOG.debug("Zip file exclusion enabled set to: " + isZipFileExclusionEnabled);
  }

  /**
   * Script templates to generate start scripts with.
   *
   * @author Griefed
   */
  private void setScriptTemplates() {
    scriptTemplates = getFileListProperty(PROPERTY_SERVERPACK_SCRIPT_TEMPLATE,
        FALLBACK_SCRIPT_TEMPLATES_ASSTRING, "server_files/");
    LOG.debug("Script templates set to: " + scriptTemplates);
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
      autoExcludingModsEnabled =
          Boolean.parseBoolean(
              getProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY));

      setProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED,
          String.valueOf(autoExcludingModsEnabled));

      remove(PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY);

      LOG.info(
          "Migrated '" + PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED_LEGACY + "' to '"
              + PROPERTY_SERVERPACK_AUTODISCOVERY_ENABLED + "'.");

    } catch (Exception ignored) {
      // No legacy declaration present, so we can safely ignore any exception.
    }

    LOG.debug("Auto-discovery of clientside-only mods set to: " + autoExcludingModsEnabled);
  }

  /**
   * Whether existing server packs should be overwritten.
   *
   * @author Griefed
   */
  private void setServerPackOverwrite() {
    serverPacksOverwriteEnabled = getBoolProperty(PROPERTY_SERVERPACK_OVERWRITE_ENABLED, true);
    LOG.debug(
        "Overwriting of already existing server packs set to: " + serverPacksOverwriteEnabled);
  }

  /**
   * Whether cleanup procedures should be executed after server pack generation.
   *
   * @author Griefed
   */
  private void setServerPackCleanup() {
    serverPackCleanupEnabled = getBoolProperty(PROPERTY_SERVERPACK_CLEANUP_ENABLED, true);
    LOG.debug(
        "Overwriting of already existing server packs set to: " + serverPackCleanupEnabled);
  }

  /**
   * Language used by the Swing frontend.
   *
   * @author Griefed
   */
  private void setLanguage() {
    language = acquireProperty(PROPERTY_LANGUAGE, "en_us");
    LOG.debug("Language set to: " + language);
  }

  /**
   * URL to send logs and configs to for easier issue reporting on GitHub.
   *
   * @author Griefed
   */
  private void setHasteBinServerUrl() {
    hasteBinServerUrl = acquireProperty(PROPERTY_CONFIGURATION_HASTEBINSERVER,
        "https://haste.zneix.eu/documents");
    LOG.debug("HasteBin documents endpoint set to: " + hasteBinServerUrl);
  }

  /**
   * Whether Minecraft pre-releases and snapshots should be made available to the user.
   *
   * @author Griefed
   */
  private void setMinecraftPreReleases() {
    minecraftPreReleases = getBoolProperty(PROPERTY_MINECRAFT_SNAPSHOTS,false);
    LOG.debug("Minecraft pre-releases and snapshots available set to: " + minecraftPreReleases);
  }

  /**
   * Set in which way user-specified clientside-only mods should be excluded.
   *
   * @author Griefed
   */
  private void setModExclusionFilterMethod() {
    ExclusionFilter filter = ExclusionFilter.START;

    try {
      String filterText = acquireProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_FILTER, "START");
      switch (filterText) {
        case "END":
          filter = ExclusionFilter.END;
          break;

        case "CONTAIN":
          filter = ExclusionFilter.CONTAIN;
          break;

        case "REGEX":
          filter = ExclusionFilter.REGEX;
          break;

        case "EITHER":
          filter = ExclusionFilter.EITHER;
          break;

        default:
          LOG.error("Invalid filter specified. Defaulting to START.");
        case "START":
          filter = ExclusionFilter.START;
          break;
      }
    } catch (NullPointerException ignored) {

    } finally {
      this.exclusionFilter = filter;
      setProperty(PROPERTY_SERVERPACK_AUTODISCOVERY_FILTER, filter.toString());
    }
    LOG.debug("User specified clientside-only mod exclusion filter set to: " + exclusionFilter);
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
    return FALLBACK_SCRIPT_TEMPLATES;
  }

  public File DEFAULT_SHELL_TEMPLATE() {
    return new File(DEFAULT_SHELL_TEMPLATE);
  }

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
    return scriptTemplates;
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
    return FALLBACK_CLIENTSIDE_MODS;
  }

  /**
   * Regex-list of fallback clientside-only mods.
   *
   * @return Regex list of fallback clientside-only mods.
   * @author Griefed
   */
  public List<String> FALLBACK_REGEX_CLIENTSIDE_MODS() {
    return FALLBACK_REGEX_CLIENTSIDE_MODS;
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
   * @return ./work/minecraft-manifest.json
   * @author Griefed
   */
  public File MINECRAFT_VERSION_MANIFEST_LOCATION() {
    return MINECRAFT_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Forge version manifest-file, as a file.
   *
   * @return ./work/forge-manifest.json
   * @author Griefed
   */
  public File FORGE_VERSION_MANIFEST_LOCATION() {
    return FORGE_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric version manifest-file, as a file.
   *
   * @return ./work/fabric-manifest.xml
   * @author Griefed
   */
  public File FABRIC_VERSION_MANIFEST_LOCATION() {
    return FABRIC_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric intermediaries manifest-file, as a file.
   *
   * @return ./work/fabric-intermediaries-manifest.json
   */
  public File FABRIC_INTERMEDIARIES_MANIFEST_LOCATION() {
    return FABRIC_INTERMEDIARIES_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric installer version manifest-file, as a file.
   *
   * @return ./work/fabric-installer-manifest.xml
   * @author Griefed
   */
  public File FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION() {
    return FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Quilt version manifest-file, as a file.
   *
   * @return ./work/quilt-manifest.xml
   * @author Griefed
   */
  public File QUILT_VERSION_MANIFEST_LOCATION() {
    return QUILT_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Quilt installer version manifest-file, as a file.
   *
   * @return ./work/quilt-installer-manifest.xml
   * @author Griefed
   */
  public File QUILT_INSTALLER_VERSION_MANIFEST_LOCATION() {
    return QUILT_INSTALLER_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Getter for the version of ServerPackCreator.<br> If a JAR-file compiled from a release-job from
   * a CI/CD-pipeline is used, it should contain a VERSION.txt-file which contains the version of
   * said release. If a non-release-version is used, from a regular pipeline or local dev-build,
   * then this will be set to <code>dev</code>.
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
    return listFallbackMods;
  }

  /**
   * Acquire the default fallback list of clientside-only mods. If
   * <code>de.griefed.serverpackcreator.serverpack.autodiscovery.filter</code> is set to
   * {@link ExclusionFilter#REGEX}, a regex fallback list is returned.
   *
   * @return The fallback list of clientside-only mods.
   * @author Griefed
   */
  public List<String> getListFallbackMods() {
    if (exclusionFilter.equals(ExclusionFilter.REGEX)) {
      return listFallbackModsRegex;
    } else {
      return listFallbackMods;
    }
  }

  /**
   * Getter for the regex fallback list of clientside-only mods.
   *
   * @return The regex fallback list of clientside-only mods.
   * @author Griefed
   */
  public List<String> getListFallbackModsRegex() {
    return listFallbackModsRegex;
  }

  /**
   * Getter for the default list of directories to include in a server pack.
   *
   * @return List containing default directories to include in a server pack.
   * @author Griefed
   */
  public List<String> getDirectoriesToInclude() {
    return directoriesToInclude;
  }

  /**
   * Getter for the list of directories to exclude from server packs.
   *
   * @return Returns the list of directories to exclude from server packs.
   * @author Griefed
   */
  public List<String> getDirectoriesToExclude() {
    return directoriesToExclude;
  }

  /**
   * Adder for the list of directories to exclude from server packs.
   *
   * @param entry The directory to add to the list of directories to exclude from server packs.
   * @author Griefed
   */
  public void addDirectoryToExclude(String entry) {
    if (!directoriesToExclude.contains(entry) && !directoriesToInclude.contains(entry)) {
      LOG.debug("Adding " + entry + " to list of files or directories to exclude.");
      directoriesToExclude.add(entry);
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
   * on <code>de.griefed.serverpackcreator.versioncheck.prerelease</code>, returns <code>
   * true</code> if checks for available PreReleases are enabled, <code>false</code> if no checks
   * for available PreReleases should be made.
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
   *   <li><code>MINECRAFT_VERSION</code> - Will be replaced with the Minecraft version of the
   *       server pack
   *   <li><code>MODLOADER</code> - Will be replaced with the modloader of the server pack
   *   <li><code>MODLOADER_VERSION</code> - Will be replaced with the modloader version of the
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
    return filesToExcludeFromZipArchive;
  }

  /**
   * Whether the exclusion of files from the ZIP-archive of the server pack is enabled.
   *
   * @return <code>true</code> if the exclusion is enabled.
   * @author Griefed
   */
  public boolean isZipFileExclusionEnabled() {
    return isZipFileExclusionEnabled;
  }

  /**
   * Is auto excluding of clientside-only mods enabled.
   *
   * @return <code>true</code> if autodiscovery is enabled.
   */
  public boolean isAutoExcludingModsEnabled() {
    return autoExcludingModsEnabled;
  }

  /**
   * Update the fallback clientside-only modlist of our <code>serverpackcreator.properties</code>
   * from the main-repository or one of its mirrors.
   *
   * @return <code>true</code> if the fallback-property was updated.
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
      String fallback = "de.griefed.serverpackcreator.configuration.fallbackmodslist";
      String fallbackRegex = "de.griefed.serverpackcreator.configuration.fallbackmodslist.regex";

      if (properties.getProperty(fallback) != null && !getProperty(fallback).equals(
          properties.getProperty(fallback))) {

        setProperty(fallback, properties.getProperty(fallback));
        listFallbackMods = new ArrayList<>(Arrays.asList(getProperty(fallback).split(",")));

        LOG.info("The fallback-list for clientside only mods has been updated.");
        updated = true;
      }

      if (properties.getProperty(fallbackRegex) != null && !getProperty(fallbackRegex).equals(
          properties.getProperty(fallbackRegex))) {

        setProperty(fallbackRegex, properties.getProperty(fallbackRegex));
        listFallbackModsRegex = new ArrayList<>(
            Arrays.asList(getProperty(fallbackRegex).split(",")));

        LOG.info("The fallback regex-list for clientside only mods has been updated.");
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
   * @return <code>true</code> if the Dark Theme is active, otherwise false.
   * @author Griefed
   */
  public boolean isDarkTheme() {
    return Boolean.parseBoolean(acquireProperty("de.griefed.serverpackcreator.gui.darkmode","true"));
  }

  /**
   * Set the current theme to Dark Theme or Light Theme.
   *
   * @param dark <code>true</code> to activate Dark Theme, <code>false</code> otherwise.
   * @author Griefed
   */
  public void setTheme(boolean dark) {
    if (dark) {
      defineProperty(
          "de.griefed.serverpackcreator.gui.darkmode", "true");
    } else {
      setProperty(
          "de.griefed.serverpackcreator.gui.darkmode", "false");
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
      store(outputStream, null);
    } catch (IOException ex) {
      LOG.error("Couldn't write properties-file.", ex);
    }
  }

  /**
   * Whether overwriting of already existing server packs is enabled.
   *
   * @return <code>true</code> if it is enabled.
   * @author Griefed
   */
  public boolean isServerPacksOverwriteEnabled() {
    return serverPacksOverwriteEnabled;
  }

  /**
   * Whether cleanup procedures after server pack generation are enabled.
   *
   * @return <code>true</code> if it is enabled.
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
   * @return <code>true</code> if they are available.
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
