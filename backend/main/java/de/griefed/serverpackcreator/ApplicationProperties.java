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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Our properties-class. Extends {@link java.util.Properties}. Sets up default properties loaded from the local serverpackcreator.properties
 * and allows reloading of said properties if the file has changed.
 * @author Griefed
 */
@SuppressWarnings("UnusedAssignment")
@Component
public class ApplicationProperties extends Properties {

    private static final Logger LOG = LogManager.getLogger(ApplicationProperties.class);

    // ServerPackHandler related
    public final File FILE_SERVERPACKCREATOR_PROPERTIES = new File("serverpackcreator.properties");
    public final File FILE_WINDOWS = new File("start.bat");
    public final File FILE_LINUX = new File("start.sh");
    public final File FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS = new File("user_jvm_args.txt");
    private final String FALLBACK_MODS_DEFAULT_ASSTRING =
            "3dSkinLayers-," +
            "3dskinlayers-," +
            "Absolutely-Not-A-Zoom-Mod-," +
            "AdvancementPlaques-," +
            "AmbientEnvironment-," +
            "AmbientSounds_," +
            "antighost-," +
            "armorchroma-," +
            "armorpointspp-," +
            "ArmorSoundTweak-," +
            "authme-," +
            "autoreconnect-," +
            "auto-reconnect-," +
            "axolotl-item-fix-," +
            "backtools-," +
            "BetterAdvancements-," +
            "BetterAnimationsCollection-," +
            "betterbiomeblend-," +
            "BetterDarkMode-," +
            "BetterF3-," +
            "BetterFoliage-," +
            "BetterPingDisplay-," +
            "BetterPlacement-," +
            "BetterTaskbar-," +
            "bhmenu-," +
            "BH-Menu-," +
            "blur-," +
            "Blur-," +
            "borderless-mining-," +
            "catalogue-," +
            "charmonium-," +
            "Charmonium-," +
            "chat_heads-," +
            "cherishedworlds-," +
            "classicbar-," +
            "clickadv-," +
            "ClientTweaks_," +
            "configured-," +
            "Controlling-," +
            "CraftPresence-," +
            "CTM-," +
            "cullleaves-," +
            "customdiscordrpc-," +
            "CustomMainMenu-," +
            "dashloader-," +
            "DefaultOptions_," +
            "defaultoptions-," +
            "DefaultSettings-," +
            "DeleteWorldsToTrash-," +
            "desiredservers-," +
            "Ding-," +
            "drippyloadingscreen_," +
            "drippyloadingscreen-," +
            "DripSounds-," +
            "Durability101-," +
            "DurabilityNotifier-," +
            "dynamic-fps-," +
            "dynamic-music-," +
            "DynamicSurroundings-," +
            "DynamicSurroundingsHuds-," +
            "dynmus-," +
            "effective-," +
            "eggtab-," +
            "EiraMoticons_," +
            "eiramoticons-," +
            "EnchantmentDescriptions-," +
            "entity-texture-features-," +
            "EquipmentCompare-," +
            "extremesoundmuffler-," +
            "extremeSoundMuffler-," +
            "fabricemotes-," +
            "Fallingleaves-," +
            "fallingleaves-," +
            "fancymenu_," +
            "findme-," +
            "flickerfix-," +
            "FPS-Monitor-," +
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
            "keymap-," +
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
            "modcredits-," +
            "modmenu-," +
            "modnametooltip_," +
            "modnametooltip-," +
            "moreoverlays-," +
            "MouseTweaks-," +
            "movement-vision-," +
            "multihotbar-," +
            "musicdr-," +
            "music-duration-reducer-," +
            "MyServerIsCompatible-," +
            "Neat ," +
            "ngrok-lan-expose-mod-," +
            "NotifMod-," +
            "OldJavaWarning-," +
            "OptiFine," +
            "OptiForge," +
            "ornaments-," +
            "overloadedarmorbar-," +
            "PackMenu-," +
            "PickUpNotifier-," +
            "Ping-," +
            "preciseblockplacing-," +
            "presencefootsteps-," +
            "PresenceFootsteps-," +
            "ReAuth-," +
            "rebrand-," +
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
            "yisthereautojump-";
    public final List<String> LIST_FALLBACK_MODS_DEFAULT = new ArrayList<>(Arrays.asList(FALLBACK_MODS_DEFAULT_ASSTRING.split(",")));

    //DefaultFiles related
    public final File FILE_CONFIG = new File("serverpackcreator.conf");
    public final File FILE_CONFIG_OLD = new File("creator.conf");
    public final File FILE_SERVER_PROPERTIES = new File("server.properties");
    public final File FILE_SERVER_ICON = new File("server-icon.png");
    public final File FILE_MANIFEST_MINECRAFT = new File("minecraft-manifest.json");
    public final File FILE_MANIFEST_FORGE = new File("forge-manifest.json");
    public final File FILE_MANIFEST_FABRIC = new File("fabric-manifest.xml");
    public final File FILE_MANIFEST_FABRIC_INSTALLER = new File("fabric-installer-manifest.xml");
    public final File FILE_SERVERPACKCREATOR_DATABASE = new File ("serverpackcreator.db");

    //VersionLister related
    public final File PATH_FILE_MANIFEST_MINECRAFT = new File("./work/minecraft-manifest.json");
    public final File PATH_FILE_MANIFEST_FORGE = new File("./work/forge-manifest.json");
    public final File PATH_FILE_MANIFEST_FABRIC = new File("./work/fabric-manifest.xml");
    public final File PATH_FILE_MANIFEST_FABRIC_INSTALLER = new File("./work/fabric-installer-manifest.xml");

    /**
     * The directory in which server packs will be generated and stored in, as well as server pack ZIP-archives.
     * Default is ./server-packs
     */
    private String directoryServerPacks = "server-packs";

    /**
     * List of mods which should be excluded from server packs.
     */
    private List<String> listFallbackMods = LIST_FALLBACK_MODS_DEFAULT;

    /**
     * List of directories which should be excluded from server packs.
     * Default is overrides, packmenu, resourcepacks, server_pack, fancymenu.
     */
    private List<String> listDirectoriesExclude = new ArrayList<>(
            Arrays.asList(
                    "overrides",
                    "packmenu",
                    "resourcepacks",
                    "server_pack",
                    "fancymenu",
                    "downloads"
            )
    );

    /**
     * List of directories which must not be excluded from server packs.
     * Default is mods, config, defaultconfigs, scripts, saves, seeds, libraries.
     */
    private List<String> listCheckAgainstNewEntry = new ArrayList<>(
            Arrays.asList(
                    "mods",
                    "config",
                    "defaultconfigs",
                    "scripts",
                    "saves",
                    "seeds",
                    "libraries",
                    "kubejs"
            ));

    /**
     * When running as a webservice: Maximum disk usage in % at which JMS/Artemis will stop storing message in the queue saved on disk.
     * Default is 90%.
     */
    private int queueMaxDiskUsage = 90;

    /**
     * Whether the manually loaded configuration file should be saved as well as the default serverpackcreator.conf. Setting this to true will make ServerPackCreator save serverpackcreator.conf as well as the last loaded configuration-file.
     * Default is false.
     */
    private boolean saveLoadedConfiguration = false;

    /**
     * The version of ServerPackCreator.<br>
     * If a JAR-file compiled from a release-job from a CI/CD-pipeline is used, it should contain a VERSION.txt-file which contains the version of said release.
     * If a non-release-version is used, from a regular pipeline or local dev-build, then this will be set to <code>dev</code>.
     */
    private String serverPackCreatorVersion = "dev";

    /**
     * Whether ServerPackCreator should check for available PreReleases.
     * Set to <code>true</code> to get notified about available PreReleases. Set to <code>false</code> if you only want stable releases.
     */
    private boolean versioncheck_prerelease = false;

    /**
     * Constructor for our properties. Sets a couple of default values for use in ServerPackCreator.
     * @author Griefed
     */
    @Autowired
    public ApplicationProperties() {

        // Load the properties file from the classpath, providing default values.
        try (InputStream inputStream = new ClassPathResource("serverpackcreator.properties").getInputStream()) {
            load(inputStream);
        } catch (IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }
        /*
         * Now load the properties file from the local filesystem. This overwrites previously loaded properties
         * but has the advantage of always providing default values if any property in the applications.properties
         * on the filesystem should be commented out.
         */
        if (new File("serverpackcreator.properties").exists()) {
            try (InputStream inputStream = Files.newInputStream(Paths.get("serverpackcreator.properties"))) {
                load(inputStream);
            } catch (IOException ex) {
                LOG.error("Couldn't read properties file.", ex);
            }
        }

        // Set the directory where the generated server packs will be stored in.
        String tempDir = null;
        try {

            // Try to use the directory specified in the de.griefed.serverpackcreator.configuration.directories.serverpacks property.
            tempDir = this.getProperty("de.griefed.serverpackcreator.configuration.directories.serverpacks","server-packs");

        } catch (NullPointerException npe) {

            // If setting the directory via property fails, set the property to the default value server-packs.
            this.setProperty("de.griefed.serverpackcreator.configuration.directories.serverpacks","server-packs");
            tempDir = "server-packs";

        } finally {

            // Check tempDir for correctness. Set property and directory if it is correct and overwrite serverpackcreator.properties
            if (tempDir != null && !tempDir.equals("") && new File(tempDir).isDirectory()) {
                this.setProperty("de.griefed.serverpackcreator.configuration.directories.serverpacks",tempDir);
                this.directoryServerPacks = tempDir;

                try (OutputStream outputStream = Files.newOutputStream(this.FILE_SERVERPACKCREATOR_PROPERTIES.toPath())) {
                    this.store(outputStream, null);
                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            // Use directory server-packs
            } else {
                this.directoryServerPacks = "server-packs";
            }
        }

        if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist") == null) {

            this.listFallbackMods = this.LIST_FALLBACK_MODS_DEFAULT;
            LOG.debug("Fallbackmodslist property null. Using fallback: " + this.LIST_FALLBACK_MODS_DEFAULT);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist").contains(",")) {

            this.listFallbackMods = new ArrayList<>(Arrays.asList(this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist",this.FALLBACK_MODS_DEFAULT_ASSTRING).split(",")));
            LOG.debug("Fallbackmodslist set to: " + this.listFallbackMods);

        } else {

            this.listFallbackMods = Collections.singletonList((this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")));
            LOG.debug("Fallbackmodslist set to: " + this.listFallbackMods);
        }

        // List of directories which can be excluded from server packs
        if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude") == null) {

            this.listDirectoriesExclude = new ArrayList<>(Arrays.asList("overrides","packmenu","resourcepacks","server_pack","fancymenu"));
            LOG.debug("directories.shouldexclude-property null. Using fallback: " + this.listDirectoriesExclude);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude").contains(",")) {

            this.listDirectoriesExclude = new ArrayList<>(Arrays.asList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude","overrides,packmenu,resourcepacks,server_pack,fancymenu").split(",")));
            LOG.debug("Directories to exclude set to: " + this.listDirectoriesExclude);

        } else {

            this.listDirectoriesExclude = Collections.singletonList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude"));
            LOG.debug("Directories to exclude set to: " + this.listDirectoriesExclude);
        }

        // List of directories which should always be included in a server pack, no matter what the users specify
        if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude") == null) {

            this.listCheckAgainstNewEntry = new ArrayList<>(Arrays.asList("overrides","packmenu","resourcepacks","server_pack","fancymenu"));
            LOG.debug("directories.mustinclude-property null. Using fallback: " + this.listCheckAgainstNewEntry);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude").contains(",")) {

            this.listCheckAgainstNewEntry = new ArrayList<>(Arrays.asList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude","mods,config,defaultconfigs,scripts,saves,seeds,libraries").split(",")));
            LOG.debug("Directories which must always be included set to: " + this.listCheckAgainstNewEntry);

        } else {

            this.listCheckAgainstNewEntry = Collections.singletonList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude"));
            LOG.debug("Directories which must always be included set to: " + this.listCheckAgainstNewEntry);
        }

        this.queueMaxDiskUsage = Integer.parseInt(getProperty("de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage", "90"));

        this.saveLoadedConfiguration = Boolean.parseBoolean(getProperty("de.griefed.serverpackcreator.configuration.saveloadedconfig", "false"));

        this.versioncheck_prerelease = Boolean.parseBoolean(getProperty("de.griefed.serverpackcreator.versioncheck.prerelease", "false"));

        String version = ApplicationProperties.class.getPackage().getImplementationVersion();
        if (version != null) {
            this.serverPackCreatorVersion = version;
        } else {
            this.serverPackCreatorVersion = "dev";
        }

    }

    /**
     * Reload serverpackcreator.properties.
     * @author Griefed
     * @return {@link ApplicationProperties} The updated instance of this object.
     */
    public ApplicationProperties reload() {

        try (InputStream inputStream = Files.newInputStream(Paths.get("serverpackcreator.properties"))) {
            load(inputStream);
        } catch (
                IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }

        String tempDir = null;
        try {

            tempDir = this.getProperty("de.griefed.serverpackcreator.configuration.directories.serverpacks","server-packs");

        } catch (NullPointerException npe) {

            this.setProperty("de.griefed.serverpackcreator.configuration.directories.serverpacks","server-packs");
            tempDir = "server-packs";

        } finally {

            if (tempDir != null && !tempDir.equals("") && new File(tempDir).isDirectory()) {
                this.setProperty("de.griefed.serverpackcreator.configuration.directories.serverpacks",tempDir);
                this.directoryServerPacks = tempDir;

                try (OutputStream outputStream = Files.newOutputStream(this.FILE_SERVERPACKCREATOR_PROPERTIES.toPath())) {
                    this.store(outputStream, null);
                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } else {
                this.directoryServerPacks = "server-packs";
            }
        }

        if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist") == null) {

            this.listFallbackMods = this.LIST_FALLBACK_MODS_DEFAULT;
            LOG.debug("Fallbackmodslist property null. Using fallback: " + this.LIST_FALLBACK_MODS_DEFAULT);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist").contains(",")) {

            this.listFallbackMods = new ArrayList<>(Arrays.asList(this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist",this.FALLBACK_MODS_DEFAULT_ASSTRING).split(",")));
            LOG.debug("Fallbackmodslist set to: " + this.listFallbackMods);

        } else {

            this.listFallbackMods = Collections.singletonList((this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")));
            LOG.debug("Fallbackmodslist set to: " + this.listFallbackMods);
        }

        // List of directories which can be excluded from server packs
        if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude") == null) {

            this.listDirectoriesExclude = new ArrayList<>(Arrays.asList("overrides","packmenu","resourcepacks","server_pack","fancymenu"));
            LOG.debug("directories.shouldexclude-property null. Using fallback: " + this.listDirectoriesExclude);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude").contains(",")) {

            this.listDirectoriesExclude = new ArrayList<>(Arrays.asList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude","overrides,packmenu,resourcepacks,server_pack,fancymenu").split(",")));
            LOG.debug("Directories to exclude set to: " + this.listDirectoriesExclude);

        } else {

            this.listDirectoriesExclude = Collections.singletonList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.shouldexclude"));
            LOG.debug("Directories to exclude set to: " + this.listDirectoriesExclude);
        }

        // List of directories which should always be included in a server pack, no matter what the users specify
        if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude") == null) {

            this.listCheckAgainstNewEntry = new ArrayList<>(Arrays.asList("overrides","packmenu","resourcepacks","server_pack","fancymenu"));
            LOG.debug("directories.mustinclude-property null. Using fallback: " + this.listCheckAgainstNewEntry);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude").contains(",")) {

            this.listCheckAgainstNewEntry = new ArrayList<>(Arrays.asList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude","mods,config,defaultconfigs,scripts,saves,seeds,libraries").split(",")));
            LOG.debug("Directories which must always be included set to: " + this.listCheckAgainstNewEntry);

        } else {

            this.listCheckAgainstNewEntry = Collections.singletonList(this.getProperty("de.griefed.serverpackcreator.configuration.directories.mustinclude"));
            LOG.debug("Directories which must always be included set to: " + this.listCheckAgainstNewEntry);
        }

        this.queueMaxDiskUsage = Integer.parseInt(getProperty("de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage", "90"));

        this.saveLoadedConfiguration = Boolean.parseBoolean(getProperty("de.griefed.serverpackcreator.configuration.saveloadedconfig", "false"));

        this.versioncheck_prerelease = Boolean.parseBoolean(getProperty("de.griefed.serverpackcreator.versioncheck.prerelease", "false"));

        return this;
    }

    /**
     * Getter for the directory in which the server packs are stored/generated in.
     * @author Griefed
     * @return String. Returns the directory in which the server packs are stored/generated in.
     */
    public String getDirectoryServerPacks() {
        return directoryServerPacks;
    }

    /**
     * Getter for the fallback list of clientside-only mods.
     * @author Griefed
     * @return List String. Returns the fallback list of clientside-only mods.
     */
    public List<String> getListFallbackMods() {
        return listFallbackMods;
    }

    /**
     * Getter for the list of directories to exclude from server packs.
     * @author Griefed
     * @return List String. Returns the list of directories to exclude from server packs.
     */
    public List<String> getListOfDirectoriesToExclude() {
        return listDirectoriesExclude;
    }

    /**
     * Adder for the list of directories to exclude from server packs.
     * @author Griefed
     * @param entry String. The directory to add to the list of directories to exclude from server packs.
     */
    public void addToListOfDirectoriesToExclude(String entry) {
        if (!this.listDirectoriesExclude.contains(entry) && !this.listCheckAgainstNewEntry.contains(entry)) {
            LOG.debug("Adding " + entry + " to list of files or directories to exclude.");
            this.listDirectoriesExclude.add(entry);
        }
    }

    /**
     * Getter for whether the last loaded configuration file should be saved to as well.
     * @author Griefed
     * @return Boolean. Whether the last loaded configuration file should be saved to as well.
     */
    public boolean getSaveLoadedConfiguration() {
        return saveLoadedConfiguration;
    }

    /**
     * Getter for the maximum disk usage at which JMS/Artemis will stop storing queues on disk.
     * @author Griefed
     * @return Integer. The maximum disk usage at which JMS/Artemis will stop storing queues on disk.
     */
    public int getQueueMaxDiskUsage() {
        return queueMaxDiskUsage;
    }

    /**
     * Getter for the version of ServerPackCreator.<br>
     * If a JAR-file compiled from a release-job from a CI/CD-pipeline is used, it should contain a VERSION.txt-file which contains the version of said release.
     * If a non-release-version is used, from a regular pipeline or local dev-build, then this will be set to <code>dev</code>.
     * @author Griefed
     * @return String. Returns the version of ServerPackCreator.
     */
    public String getServerPackCreatorVersion() {
        return serverPackCreatorVersion;
    }

    /**
     * Getter for whether the search for available PreReleases is enabled or disabled.<br>
     * Depending on <code>de.griefed.serverpackcreator.versioncheck.prerelease</code>, returns <code>true</code>
     * if checks for available PreReleases are enabled, <code>false</code> if no checks for available PreReleases should
     * be made.
     * @author Griefed
     * @return Boolean. Whether checks for available PreReleases are enabled.
     */
    public boolean checkForAvailablePreReleases() {
        return versioncheck_prerelease;
    }

    /**
     * Update the fallback clientside-only modlist of our <code>serverpackcreator.properties</code> from the main-repository
     * or one of its mirrors.
     * @author Griefed
     * @return <code>true</code> if the fallback-property was updated.
     */
    public boolean updateFallback() {

        Properties properties;

        try (InputStream github = new URL("https://raw.githubusercontent.com/Griefed/ServerPackCreator/main/backend/main/resources/serverpackcreator.properties").openStream()) {

            properties = new Properties();
            properties.load(github);

        } catch (IOException e) {

            LOG.debug("GitHub could not be reached. Checking GitLab.",e);
            properties = null;
            try (InputStream gitlab = new URL("https://gitlab.com/Griefed/ServerPackCreator/-/raw/main/backend/main/resources/serverpackcreator.properties").openStream()) {

                properties = new Properties();
                properties.load(gitlab);

            } catch (IOException ex) {
                LOG.debug("GitLab could not be reached. Checking GitGriefed",ex);
                properties = null;
                try (InputStream gitgriefed = new URL("https://git.griefed.de/Griefed/ServerPackCreator/-/raw/main/backend/main/resources/serverpackcreator.properties").openStream()) {

                    properties = new Properties();
                    properties.load(gitgriefed);

                } catch (IOException exe) {
                    LOG.debug("GitGriefed could not be reached.",exe);
                    properties = null;
                }
            }
        }

        if (properties != null &&
                !getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")
                        .equals(properties.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist"))
        ) {

            setProperty(
                    "de.griefed.serverpackcreator.configuration.fallbackmodslist",
                    properties.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")
            );

            try (OutputStream outputStream = Files.newOutputStream(this.FILE_SERVERPACKCREATOR_PROPERTIES.toPath())) {
                this.store(outputStream, null);
            } catch (IOException ex) {
                LOG.error("Couldn't write properties-file.", ex);
            }

            this.listFallbackMods = new ArrayList<>(Arrays.asList(this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist",this.FALLBACK_MODS_DEFAULT_ASSTRING).split(",")));
            LOG.debug("Fallbackmodslist set to: " + this.listFallbackMods);
            LOG.info("The fallback-list for clientside only mods has been updated.");
            return true;

        } else {
            LOG.info("No fallback-list updates available.");
            return false;
        }
    }

    @Override
    public synchronized String toString() {
        return "ApplicationProperties{" +
                "FILE_SERVERPACKCREATOR_PROPERTIES=" + FILE_SERVERPACKCREATOR_PROPERTIES +
                ", FILE_WINDOWS=" + FILE_WINDOWS +
                ", FILE_LINUX=" + FILE_LINUX +
                ", FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS=" + FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS +
                ", LIST_FALLBACK_MODS_DEFAULT=" + LIST_FALLBACK_MODS_DEFAULT +
                ", FILE_CONFIG=" + FILE_CONFIG +
                ", FILE_CONFIG_OLD=" + FILE_CONFIG_OLD +
                ", FILE_SERVER_PROPERTIES=" + FILE_SERVER_PROPERTIES +
                ", FILE_SERVER_ICON=" + FILE_SERVER_ICON +
                ", FILE_MANIFEST_MINECRAFT=" + FILE_MANIFEST_MINECRAFT +
                ", FILE_MANIFEST_FORGE=" + FILE_MANIFEST_FORGE +
                ", FILE_MANIFEST_FABRIC=" + FILE_MANIFEST_FABRIC +
                ", FILE_MANIFEST_FABRIC_INSTALLER=" + FILE_MANIFEST_FABRIC_INSTALLER +
                ", FILE_SERVERPACKCREATOR_DATABASE=" + FILE_SERVERPACKCREATOR_DATABASE +
                ", PATH_FILE_MANIFEST_MINECRAFT=" + PATH_FILE_MANIFEST_MINECRAFT +
                ", PATH_FILE_MANIFEST_FORGE=" + PATH_FILE_MANIFEST_FORGE +
                ", PATH_FILE_MANIFEST_FABRIC=" + PATH_FILE_MANIFEST_FABRIC +
                ", PATH_FILE_MANIFEST_FABRIC_INSTALLER=" + PATH_FILE_MANIFEST_FABRIC_INSTALLER +
                ", directoryServerPacks='" + getDirectoryServerPacks() + '\'' +
                ", listFallbackMods=" + getListFallbackMods() +
                ", listDirectoriesExclude=" + getListOfDirectoriesToExclude() +
                ", listCheckAgainstNewEntry=" + listCheckAgainstNewEntry +
                ", queueMaxDiskUsage=" + getQueueMaxDiskUsage() +
                ", saveLoadedConfiguration=" + getSaveLoadedConfiguration() +
                ", serverPackCreatorVersion='" + getServerPackCreatorVersion() + '\'' +
                ", versioncheck_prerelease=" + checkForAvailablePreReleases() +
                '}';
    }
}
