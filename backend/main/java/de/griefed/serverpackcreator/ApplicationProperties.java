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
import java.nio.file.Paths;
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
public class ApplicationProperties extends Properties {

  private static final Logger LOG = LogManager.getLogger(ApplicationProperties.class);

  // ServerPackHandler related
  private final File SERVERPACKCREATOR_PROPERTIES = new File("serverpackcreator.properties");

  private final String FALLBACK_MODS_DEFAULT_ASSTRING =
      "3dSkinLayers-,"
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
          + "yisthereautojump-";
  private final List<String> FALLBACK_CLIENTSIDE_MODS =
      new ArrayList<>(Arrays.asList(FALLBACK_MODS_DEFAULT_ASSTRING.split(",")));
  private final String SERVERPACKCREATOR_VERSION;
  private final String[] SUPPORTED_MODLOADERS = new String[] {"Fabric", "Forge", "Quilt"};
  private final String FALLBACK_DIRECTORIES_INCLUDE_ASSTRING = "mods,config,defaultconfigs,scripts";
  private final List<String> FALLBACK_DIRECTORIES_INCLUDE =
      new ArrayList<>(Arrays.asList(FALLBACK_DIRECTORIES_INCLUDE_ASSTRING.split(",")));
  private final String FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING =
      "overrides,packmenu,resourcepacks,server_pack,fancymenu,libraries";
  private final List<String> FALLBACK_DIRECTORIES_EXCLUDE =
      new ArrayList<>(Arrays.asList(FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING.split(",")));
  private final String FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING =
      "minecraft_server.MINECRAFT_VERSION.jar,server.jar,libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar";
  private final List<String> FALLBACK_FILES_EXCLUDE_ZIP =
      new ArrayList<>(Arrays.asList(FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING.split(",")));
  private final String DEFAULT_SHELL_TEMPALTE = "default_template.sh";
  private final String DEFAULT_POWERSHELL_TEMPLATE = "default_template.ps1";

  private final List<File> FALLBACK_SCRIPT_TEMPLATES =
      new ArrayList<>(
          Arrays.asList(
              new File("server_files/" + DEFAULT_SHELL_TEMPALTE),
              new File("server_files/" + DEFAULT_POWERSHELL_TEMPLATE)));

  // DefaultFiles related
  private final File DEFAULT_CONFIG = new File("serverpackcreator.conf");
  private final File OLD_CONFIG = new File("creator.conf");
  private final File DEFAULT_SERVER_PROPERTIES = new File("server.properties");
  private final File DEFAULT_SERVER_ICON = new File("server-icon.png");
  private final File MINECRAFT_VERSION_MANIFEST = new File("minecraft-manifest.json");
  private final File FORGE_VERSION_MANIFEST = new File("forge-manifest.json");
  private final File FABRIC_VERSION_MANIFEST = new File("fabric-manifest.xml");
  private final File FABRIC_INSTALLER_VERSION_MANIFEST = new File("fabric-installer-manifest.xml");
  private final File QUILT_VERSION_MANIFEST = new File("quilt-manifest.xml");
  private final File QUILT_INSTALLER_VERSION_MANIFEST = new File("quilt-installer-manifest.xml");
  private final File SERVERPACKCREATOR_DATABASE = new File("serverpackcreator.db");

  // VersionLister related
  private final File MINECRAFT_VERSION_MANIFEST_LOCATION =
      new File("./work/minecraft-manifest.json");
  private final File FORGE_VERSION_MANIFEST_LOCATION = new File("./work/forge-manifest.json");
  private final File FABRIC_VERSION_MANIFEST_LOCATION = new File("./work/fabric-manifest.xml");
  private final File FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION =
      new File("./work/fabric-installer-manifest.xml");
  private final File QUILT_VERSION_MANIFEST_LOCATION = new File("./work/quilt-manifest.xml");
  private final File QUILT_INSTALLER_VERSION_MANIFEST_LOCATION =
      new File("./work/quilt-installer-manifest.xml");

  /**
   * The directory in which server packs will be generated and stored in, as well as server pack
   * ZIP-archives. Default is ./server-packs
   */
  private String directoryServerPacks;

  /** List of mods which should be excluded from server packs. */
  private List<String> listFallbackMods;

  /**
   * List of directories which should be excluded from server packs. Default is overrides, packmenu,
   * resourcepacks, server_pack, fancymenu.
   */
  private List<String> directoriesToExclude;

  /**
   * List of directories which must not be excluded from server packs. Default is mods, config,
   * defaultconfigs, scripts, saves, seeds, libraries.
   */
  private List<String> directoriesToInclude;

  /**
   * When running as a webservice: Maximum disk usage in % at which JMS/Artemis will stop storing
   * message in the queue saved on disk. Default is 90%.
   */
  private int queueMaxDiskUsage;

  /**
   * Whether the manually loaded configuration file should be saved as well as the default
   * serverpackcreator.conf. Setting this to true will make ServerPackCreator save
   * serverpackcreator.conf as well as the last loaded configuration-file. Default is false.
   */
  private boolean saveLoadedConfiguration;

  /**
   * Whether ServerPackCreator should check for available PreReleases. Set to <code>true</code> to
   * get notified about available PreReleases. Set to <code>false</code> if you only want stable
   * releases.
   */
  private boolean versioncheck_prerelease;

  /**
   * Aikars flags recommended for running a Minecraft server, from <a
   * href=https://aikar.co/mcflags.html>aikar.co</a>
   */
  private String aikarsFlags;

  /** List of files to be excluded from ZIP-archives. */
  private List<String> filesToExcludeFromZipArchive;

  /** Whether the exclusion of files from the server pack is enabled. */
  private boolean isZipFileExclusionEnabled;

  /** List of templates used for start-script creation. */
  private List<File> scriptTemplates;

  /**
   * Constructor for our properties. Sets a couple of default values for use in ServerPackCreator.
   *
   * @author Griefed
   */
  @Autowired
  public ApplicationProperties() {
    super();

    // Load the properties file from the classpath, providing default values.
    try (InputStream inputStream =
        new ClassPathResource("serverpackcreator.properties").getInputStream()) {
      this.load(inputStream);
    } catch (IOException ex) {
      LOG.error("Couldn't read properties file.", ex);
    }

    String version = ApplicationProperties.class.getPackage().getImplementationVersion();
    if (version != null) {
      this.SERVERPACKCREATOR_VERSION = version;
    } else {
      this.SERVERPACKCREATOR_VERSION = "dev";
    }

    reload();
  }

  /**
   * Reload serverpackcreator.properties.
   *
   * @author Griefed
   */
  public void reload() {

    if (new File("serverpackcreator.properties").exists()) {
      /*
       * Now load the properties file from the local filesystem. This overwrites previously loaded properties
       * but has the advantage of always providing default values if any property in the applications.properties
       * on the filesystem should be commented out.
       */
      try (InputStream inputStream =
          Files.newInputStream(Paths.get("serverpackcreator.properties"))) {
        this.load(inputStream);
      } catch (IOException ex) {
        LOG.error("Couldn't read properties file.", ex);
      }
    }

    // Set the directory where the generated server packs will be stored in.
    String tempDir = null;
    try {

      // Try to use the directory specified in the
      // de.griefed.serverpackcreator.configuration.directories.serverpacks property.
      tempDir =
          this.getProperty(
              "de.griefed.serverpackcreator.configuration.directories.serverpacks", "server-packs");

    } catch (NullPointerException npe) {

      // If setting the directory via property fails, set the property to the default value
      // server-packs.
      this.setProperty(
          "de.griefed.serverpackcreator.configuration.directories.serverpacks", "server-packs");
      tempDir = "server-packs";

    } finally {

      // Check tempDir for correctness. Set property and directory if it is correct and overwrite
      // serverpackcreator.properties
      if (tempDir != null && !tempDir.isEmpty() && new File(tempDir).isDirectory()) {
        this.setProperty(
            "de.griefed.serverpackcreator.configuration.directories.serverpacks", tempDir);
        this.directoryServerPacks = tempDir;

        try (OutputStream outputStream =
            Files.newOutputStream(this.SERVERPACKCREATOR_PROPERTIES.toPath())) {
          this.store(outputStream, null);
        } catch (IOException ex) {
          LOG.error("Couldn't write properties-file.", ex);
        }

        // Use directory server-packs
      } else {
        this.directoryServerPacks = "server-packs";
      }
    }

    // Setup our fallback list of clientside-only mods.
    if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist") == null) {

      this.listFallbackMods = this.FALLBACK_CLIENTSIDE_MODS;
      LOG.debug("Fallbackmodslist property null. Using fallback.");

    } else if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")
        .contains(",")) {

      this.listFallbackMods =
          new ArrayList<>(
              Arrays.asList(
                  this.getProperty(
                          "de.griefed.serverpackcreator.configuration.fallbackmodslist",
                          this.FALLBACK_MODS_DEFAULT_ASSTRING)
                      .split(",")));

    } else {

      this.listFallbackMods =
          Collections.singletonList(
              (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")));
    }
    LOG.debug("Fallbackmodslist set to: " + this.listFallbackMods);

    // List of directories which can be excluded from server packs
    if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude")
        == null) {

      this.directoriesToExclude = FALLBACK_DIRECTORIES_EXCLUDE;
      LOG.debug("directories.shouldexclude-property null. Using fallback.");

    } else if (this.getProperty(
            "de.griefed.serverpackcreator.configuration.directories.shouldexclude")
        .contains(",")) {

      this.directoriesToExclude =
          new ArrayList<>(
              Arrays.asList(
                  this.getProperty(
                          "de.griefed.serverpackcreator.configuration.directories.shouldexclude",
                          FALLBACK_DIRECTORIES_EXCLUDE_ASSTRING)
                      .split(",")));

    } else {

      this.directoriesToExclude =
          Collections.singletonList(
              this.getProperty(
                  "de.griefed.serverpackcreator.configuration.directories.shouldexclude"));
    }
    LOG.debug("Directories to exclude set to: " + this.directoriesToExclude);

    // List of directories which should always be included in a server pack, no matter what the
    // users specify
    if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude")
        == null) {

      this.directoriesToInclude = FALLBACK_DIRECTORIES_INCLUDE;
      LOG.debug("directories.mustinclude-property null. Using fallback.");

    } else if (this.getProperty(
            "de.griefed.serverpackcreator.configuration.directories.mustinclude")
        .contains(",")) {

      this.directoriesToInclude =
          new ArrayList<>(
              Arrays.asList(
                  this.getProperty(
                          "de.griefed.serverpackcreator.configuration.directories.mustinclude",
                          FALLBACK_DIRECTORIES_INCLUDE_ASSTRING)
                      .split(",")));

    } else {

      this.directoriesToInclude =
          Collections.singletonList(
              this.getProperty(
                  "de.griefed.serverpackcreator.configuration.directories.mustinclude"));
    }
    LOG.debug("Directories which must always be included set to: " + this.directoriesToInclude);

    this.queueMaxDiskUsage =
        Integer.parseInt(
            getProperty("de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage", "90"));

    this.saveLoadedConfiguration =
        Boolean.parseBoolean(
            getProperty("de.griefed.serverpackcreator.configuration.saveloadedconfig", "false"));

    this.versioncheck_prerelease =
        Boolean.parseBoolean(
            getProperty("de.griefed.serverpackcreator.versioncheck.prerelease", "false"));

    this.aikarsFlags =
        this.getProperty(
            "de.griefed.serverpackcreator.configuration.aikar",
            "-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 "
                + "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 "
                + "-XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 "
                + "-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 "
                + "-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem "
                + "-XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true");

    if (this.getProperty("de.griefed.serverpackcreator.serverpack.zip.exclude") == null) {

      this.filesToExcludeFromZipArchive = FALLBACK_FILES_EXCLUDE_ZIP;
      LOG.debug("serverpack.zip.exclude-property null. Using fallback.");

    } else if (this.getProperty("de.griefed.serverpackcreator.serverpack.zip.exclude")
        .contains(",")) {

      this.filesToExcludeFromZipArchive =
          new ArrayList<>(
              Arrays.asList(
                  this.getProperty(
                          "de.griefed.serverpackcreator.serverpack.zip.exclude",
                          FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING)
                      .split(",")));

    } else {

      this.filesToExcludeFromZipArchive =
          Collections.singletonList(
              this.getProperty(
                  "de.griefed.serverpackcreator.serverpack.zip.exclude",
                  FALLBACK_FILES_EXCLUDE_ZIP_ASSTRING));
    }
    LOG.debug(
        "Files which must be excluded from ZIP-archives set to: "
            + this.filesToExcludeFromZipArchive);

    this.isZipFileExclusionEnabled =
        Boolean.parseBoolean(
            getProperty("de.griefed.serverpackcreator.serverpack.zip.exclude.enabled", "true"));

    // Setup our start script template list
    if (this.getProperty("de.griefed.serverpackcreator.serverpack.script.template") == null) {

      this.scriptTemplates = this.FALLBACK_SCRIPT_TEMPLATES;
      LOG.debug("Script template property null. Using fallback.");

    } else if (this.getProperty("de.griefed.serverpackcreator.serverpack.script.template")
        .contains(",")) {

      this.scriptTemplates = new ArrayList<>(10);

      for (String template :
          this.getProperty("de.griefed.serverpackcreator.serverpack.script.template").split(",")) {
        scriptTemplates.add(new File("server_files/" + template));
      }

    } else {

      this.scriptTemplates =
          Collections.singletonList(
              (new File(
                  "server_files/"
                      + this.getProperty(
                          "de.griefed.serverpackcreator.configuration.fallbackmodslist"))));
    }
    LOG.debug("Script templates set to: " + this.scriptTemplates);
  }

  /**
   * Default list of script templates, used in case not a single one was configured.
   *
   * <ul>
   *   <li>default_template.sh
   *   <li>default_template.ps1
   * </ul>
   *
   * @return {@link List} {@link File} Default script templates.
   * @author Griefed
   */
  public List<File> FALLBACK_SCRIPT_TEMPLATES() {
    return FALLBACK_SCRIPT_TEMPLATES;
  }

  public File DEFAULT_SHELL_TEMPLATE() {
    return new File(DEFAULT_SHELL_TEMPALTE);
  }

  public File DEFAULT_POWERSHELL_TEMPLATE() {
    return new File(DEFAULT_POWERSHELL_TEMPLATE);
  }

  /**
   * Configured list of script templates.
   *
   * @return {@link List} {@link File} Configured script templates.
   * @author Griefed
   */
  public List<File> scriptTemplates() {
    return scriptTemplates;
  }

  /**
   * Properties file used by ServerPackCreator, containing the configuration for this instance of
   * it.
   *
   * @return {@link File} serverpackcreator.properties-file.
   * @author Griefed
   */
  public File SERVERPACKCREATOR_PROPERTIES() {
    return SERVERPACKCREATOR_PROPERTIES;
  }

  /**
   * String-list of fallback clientside-only mods.
   *
   * @return {@link String}-list of fallback clientside-only mods.
   * @author Griefed
   */
  public List<String> FALLBACK_CLIENTSIDE_MODS() {
    return FALLBACK_CLIENTSIDE_MODS;
  }

  /**
   * Default configuration-file for a server pack generation.
   *
   * @return {@link File} serverpackcreator.conf-file.
   * @author Griefed
   */
  public File DEFAULT_CONFIG() {
    return DEFAULT_CONFIG;
  }

  /**
   * Old configuration-file used for automated migration in case anyone upgrades from 1.x.
   *
   * @return {@link File} creator.conf-file.
   * @author Griefed
   */
  public File OLD_CONFIG() {
    return OLD_CONFIG;
  }

  /**
   * Default server.properties-file used by Minecraft servers.
   *
   * @return {@link File} server.properties-file.
   * @author Griefed
   */
  public File DEFAULT_SERVER_PROPERTIES() {
    return DEFAULT_SERVER_PROPERTIES;
  }

  /**
   * Default server-icon.png-file used by Minecraft servers.
   *
   * @return {@link File} server-icon.png-file.
   * @author Griefed
   */
  public File DEFAULT_SERVER_ICON() {
    return DEFAULT_SERVER_ICON;
  }

  /**
   * Minecraft version manifest-file.
   *
   * @return {@link File} minecraft-manifest.json-file.
   * @author Griefed
   */
  public File MINECRAFT_VERSION_MANIFEST() {
    return MINECRAFT_VERSION_MANIFEST;
  }

  /**
   * Forge version manifest-file.
   *
   * @return {@link File} forge-manifest.json-file.
   * @author Griefed
   */
  public File FORGE_VERSION_MANIFEST() {
    return FORGE_VERSION_MANIFEST;
  }

  /**
   * Fabric version manifest-file.
   *
   * @return {@link File} fabric-manifest.xml-file
   * @author Griefed
   */
  public File FABRIC_VERSION_MANIFEST() {
    return FABRIC_VERSION_MANIFEST;
  }

  /**
   * Fabric installer version manifest-file.
   *
   * @return {@link File} fabric-installer-manifest.xml-file.
   * @author Griefed
   */
  public File FABRIC_INSTALLER_VERSION_MANIFEST() {
    return FABRIC_INSTALLER_VERSION_MANIFEST;
  }

  /**
   * Quilt version manifest-file.
   *
   * @return {@link File} quilt-manifest.xml-file
   * @author Griefed
   */
  public File QUILT_VERSION_MANIFEST() {
    return QUILT_VERSION_MANIFEST;
  }

  /**
   * Quilt installer version manifest-file.
   *
   * @return {@link File} quilt-installer-manifest.xml-file.
   * @author Griefed
   */
  public File QUILT_INSTALLER_VERSION_MANIFEST() {
    return QUILT_INSTALLER_VERSION_MANIFEST;
  }

  /**
   * ServerPackCreator-database when running as a webservice.
   *
   * @return {@link File} serverpackcreator.db-file.
   * @author Griefed
   */
  public File SERVERPACKCREATOR_DATABASE() {
    return SERVERPACKCREATOR_DATABASE;
  }

  /**
   * Path to the Minecraft version manifest-file, as a file.
   *
   * @return {@link File} ./work/minecraft-manifest.json
   * @author Griefed
   */
  public File MINECRAFT_VERSION_MANIFEST_LOCATION() {
    return MINECRAFT_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Forge version manifest-file, as a file.
   *
   * @return {@link File} ./work/forge-manifest.json
   * @author Griefed
   */
  public File FORGE_VERSION_MANIFEST_LOCATION() {
    return FORGE_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric version manifest-file, as a file.
   *
   * @return {@link File} ./work/fabric-manifest.xml
   * @author Griefed
   */
  public File FABRIC_VERSION_MANIFEST_LOCATION() {
    return FABRIC_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Fabric installer version manifest-file, as a file.
   *
   * @return {@link File} ./work/fabric-installer-manifest.xml
   * @author Griefed
   */
  public File FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION() {
    return FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Quilt version manifest-file, as a file.
   *
   * @return {@link File} ./work/quilt-manifest.xml
   * @author Griefed
   */
  public File QUILT_VERSION_MANIFEST_LOCATION() {
    return QUILT_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Path to the Quilt installer version manifest-file, as a file.
   *
   * @return {@link File} ./work/quilt-installer-manifest.xml
   * @author Griefed
   */
  public File QUILT_INSTALLER_VERSION_MANIFEST_LOCATION() {
    return QUILT_INSTALLER_VERSION_MANIFEST_LOCATION;
  }

  /**
   * Getter for the version of ServerPackCreator.<br>
   * If a JAR-file compiled from a release-job from a CI/CD-pipeline is used, it should contain a
   * VERSION.txt-file which contains the version of said release. If a non-release-version is used,
   * from a regular pipeline or local dev-build, then this will be set to <code>dev</code>.
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
   * @return {@link String}-array of modloaders supported by ServerPackCreator.
   * @author Griefed
   */
  public String[] SUPPORTED_MODLOADERS() {
    return SUPPORTED_MODLOADERS;
  }

  /**
   * Directory where server-files are stored in, for example the default server-icon and
   * server.properties.
   *
   * @author Griefed
   * @return {@link String} server-files directory.
   */
  public String DIRECTORY_SERVER_FILES() {
    return "server_files";
  }

  /**
   * Directory where plugins are stored in.
   *
   * @author Griefed
   * @return {@link String} plugins directory.
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
   * Getter for the fallback list of clientside-only mods.
   *
   * @return List String. Returns the fallback list of clientside-only mods.
   * @author Griefed
   */
  public List<String> getListFallbackMods() {
    return listFallbackMods;
  }

  /**
   * Getter for the default list of directories to include in a server pack.
   *
   * @return {@link List} {@link String} containing default directories to include in a server pack.
   * @author Griefed
   */
  public List<String> getDirectoriesToInclude() {
    return directoriesToInclude;
  }

  /**
   * Getter for the list of directories to exclude from server packs.
   *
   * @return List String. Returns the list of directories to exclude from server packs.
   * @author Griefed
   */
  public List<String> getDirectoriesToExclude() {
    return directoriesToExclude;
  }

  /**
   * Adder for the list of directories to exclude from server packs.
   *
   * @param entry String. The directory to add to the list of directories to exclude from server
   *     packs.
   * @author Griefed
   */
  public void addDirectoryToExclude(String entry) {
    if (!this.directoriesToExclude.contains(entry) && !this.directoriesToInclude.contains(entry)) {
      LOG.debug("Adding " + entry + " to list of files or directories to exclude.");
      this.directoriesToExclude.add(entry);
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
   * Getter for whether the search for available PreReleases is enabled or disabled.<br>
   * Depending on <code>de.griefed.serverpackcreator.versioncheck.prerelease</code>, returns <code>
   * true</code> if checks for available PreReleases are enabled, <code>false</code> if no checks
   * for available PreReleases should be made.
   *
   * @return Boolean. Whether checks for available PreReleases are enabled.
   * @author Griefed
   */
  public boolean checkForAvailablePreReleases() {
    return versioncheck_prerelease;
  }

  /**
   * Get this configurations AikarsFlags
   *
   * @return {@link String} Aikars flags.
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
   *
   * Should you want these filters to be expanded, open an issue on <a
   * href="https://github.com/Griefed/ServerPackCreator/issues">GitHub</a>
   *
   * @return {@link List} {@link String} of files to exclude from the ZIP archive of a server pack.
   * @author Griefed
   */
  public List<String> getFilesToExcludeFromZipArchive() {
    return filesToExcludeFromZipArchive;
  }

  /**
   * Whether the exclusion of files from the ZIP-archive of the server pack is enabled.
   *
   * @return {@link Boolean} <code>true</code> if the exclusion is enabled.
   * @author Griefed
   */
  public boolean isZipFileExclusionEnabled() {
    return isZipFileExclusionEnabled;
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

    if (properties != null
        && !getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")
            .equals(
                properties.getProperty(
                    "de.griefed.serverpackcreator.configuration.fallbackmodslist"))) {

      setProperty(
          "de.griefed.serverpackcreator.configuration.fallbackmodslist",
          properties.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist"));

      try (OutputStream outputStream =
          Files.newOutputStream(this.SERVERPACKCREATOR_PROPERTIES.toPath())) {
        this.store(outputStream, null);
      } catch (IOException ex) {
        LOG.error("Couldn't write properties-file.", ex);
      }

      this.listFallbackMods =
          new ArrayList<>(
              Arrays.asList(
                  this.getProperty(
                          "de.griefed.serverpackcreator.configuration.fallbackmodslist",
                          this.FALLBACK_MODS_DEFAULT_ASSTRING)
                      .split(",")));
      LOG.debug("Fallbackmodslist set to: " + this.listFallbackMods);
      LOG.info("The fallback-list for clientside only mods has been updated.");
      return true;

    } else {
      LOG.info("No fallback-list updates available.");
      return false;
    }
  }
}
