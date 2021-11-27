/* Copyright (C) 2021  Griefed
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
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Our properties-class. Extends {@link java.util.Properties}. Sets up default properties loaded from the local serverpackcreator.properties
 * and allows reloading of said properties if the file has changed.
 * @author Griefed
 */
@Component
public class ApplicationProperties extends Properties {

    private final Logger LOG = LogManager.getLogger(ApplicationProperties.class);

    // ServerPackHandler related
    public final File FILE_SERVERPACKCREATOR_PROPERTIES = new File("serverpackcreator.properties");
    public final File FILE_WINDOWS = new File("start.bat");
    public final File FILE_LINUX = new File("start.sh");
    public final File FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS = new File("user_jvm_args.txt");
    private final String FALLBACK_MODS_DEFAULT_ASSTRING =
            "AdvancementPlaques-,AmbientSounds_,backtools-,BetterAdvancements-,BetterAnimationsCollection-," +
            "BetterDarkMode-,betterf3-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-," +
            "Blur-,catalogue-,cherishedworlds-,classicbar-,clickadv-,ClientTweaks_,configured-," +
            "Controlling-,CTM-,customdiscordrpc-,CustomMainMenu-,defaultoptions-,DefaultOptions_," +
            "desiredservers-,Ding-,drippyloadingscreen-,drippyloadingscreen_,Durability101-,dynmus-," +
            "dynamic-music-,DynamicSurroundings-,DynamicSurroundingsHuds-,eiramoticons-,EiraMoticons_," +
            "EnchantmentDescriptions-,EquipmentCompare-,extremesoundmuffler-,extremeSoundMuffler-," +
            "Fallingleaves-,fallingleaves-,fancymenu_,findme-,flickerfix-,FpsReducer-,FullscreenWindowed-," +
            "WindowedFullscreen-,InventoryEssentials_,InventorySpam-,invtweaks-,InventoryTweaks-,ItemBorders-," +
            "itemzoom,itlt-,jeed-,jeiintegration_,JustEnoughProfessions-,JEITweaker-,justenoughbeacons-," +
            "JustEnoughCalculation-,jehc-,just-enough-harvestcraft-,JustEnoughProfessions-,JustEnoughResources-," +
            "keywizard-,konkrete_,lazydfu-,LegendaryTooltips-,LightOverlay-,light-overlay-,LLOverlayReloaded-," +
            "loadmyresources_,lootbeams-,mcbindtype-,modnametooltip_,modnametooltip-,MouseTweaks-," +
            "multihotbar-,MyServerIsCompatible-,Neat,NotifMod-,OldJavaWarning-,ornaments-,overloadedarmorbar-," +
            "PackMenu-,PickUpNotifier-,Ping-,preciseblockplacing-,presencefootsteps-,PresenceFootsteps-," +
            "ReAuth-,ResourceLoader-,shutupexperimentalsettings-,SimpleDiscordRichPresence-,smoothboot-," +
            "sounddeviceoptions-,SpawnerFix-,spoticraft-,tconplanner-,timestamps-,Tips-,TipTheScales-," +
            "Toast Control-,Toast-Control-,torohealth-,toughnessbar-,TravelersTitles-,WorldNameRandomizer-";
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
     * Default is AdvancementPlaques-,AmbientSounds_,backtools-,BetterAdvancements-,BetterAnimationsCollection-,BetterDarkMode-,betterf3-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,Blur-,catalogue-,cherishedworlds-,classicbar-,clickadv-,ClientTweaks_,configured-,Controlling-,CTM-,customdiscordrpc-,CustomMainMenu-,defaultoptions-,DefaultOptions_,desiredservers-,Ding-,drippyloadingscreen-,drippyloadingscreen_,Durability101-,dynmus-,dynamic-music-,DynamicSurroundings-,DynamicSurroundingsHuds-,eiramoticons-,EiraMoticons_,EnchantmentDescriptions-,EquipmentCompare-,extremesoundmuffler-,extremeSoundMuffler-,Fallingleaves-,fallingleaves-,fancymenu_,findme-,flickerfix-,FpsReducer-,FullscreenWindowed-,WindowedFullscreen-,InventoryEssentials_,InventorySpam-,invtweaks-,InventoryTweaks-,ItemBorders-,itemzoom,itlt-,jeed-,jeiintegration_,JustEnoughProfessions-,JEITweaker-,justenoughbeacons-,JustEnoughCalculation-,jehc-,just-enough-harvestcraft-,JustEnoughProfessions-,JustEnoughResources-,keywizard-,konkrete_,lazydfu-,LegendaryTooltips-,LightOverlay-,light-overlay-,LLOverlayReloaded-,loadmyresources_,lootbeams-,mcbindtype-,modnametooltip_,modnametooltip-,MouseTweaks-,multihotbar-,MyServerIsCompatible-,Neat,NotifMod-,OldJavaWarning-,ornaments-,overloadedarmorbar-,PackMenu-,PickUpNotifier-,Ping-,preciseblockplacing-,presencefootsteps-,PresenceFootsteps-,ReAuth-,ResourceLoader-,shutupexperimentalsettings-,SimpleDiscordRichPresence-,smoothboot-,sounddeviceoptions-,SpawnerFix-,spoticraft-,tconplanner-,timestamps-,Tips-,TipTheScales-,Toast Control-,Toast-Control-,torohealth-,toughnessbar-,TravelersTitles-,WorldNameRandomizer-
     */
    private List<String> listFallbackMods = LIST_FALLBACK_MODS_DEFAULT;

    /**
     * List of directories which should be excluded from server packs.
     * Default is overrides, packmenu, resourcepacks, server_pack, fancymenu.
     */
    private List<String> listDirectoriesExclude = new ArrayList<>(Arrays.asList("overrides","packmenu","resourcepacks","server_pack","fancymenu"));

    /**
     * When running as a webservice: Whether regeneration of server packs, that have already been generated, is enabled.
     * Default is false.
     */
    private boolean curseControllerRegenerationEnabled = false;

    /**
     * List of directories which must not be excluded from server packs.
     * Default is mods, config, defaultconfigs, scripts, saves, seeds, libraries.
     */
    private List<String> listCheckAgainstNewEntry = new ArrayList<>(Arrays.asList("mods","config","defaultconfigs","scripts","saves","seeds","libraries"));

    /**
     * When running as a webservice: Maximum disk usage in % at which JMS/Artemis will stop storing message in the queue saved on disk.
     * Default is 90%.
     */
    private int queueMaxDiskUsage = 90;

    /**
     * When running as a webservice: Cron-schedule for when ServerPackCreator should run a database-cleanup. Set to <code>-</code> to disable.
     * Default is 0 0 24 * *.
     */
    private String scheduleDatabaseCleanup = "0 0 24 * *";

    /**
     * Whether the manually loaded configuration file should be saved as well as the default serverpackcreator.conf. Setting this to true will make ServerPackCreator save serverpackcreator.conf as well as the last loaded configuration-file.
     * Default is false.
     */
    private boolean saveLoadedConfiguration = false;

    /**
     * Constructor for our properties. Sets a couple of default values for use in ServerPackCreator.
     * @author Griefed
     */
    @Autowired
    public ApplicationProperties() {

        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            new Properties();
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

                try (OutputStream outputStream = new FileOutputStream(this.FILE_SERVERPACKCREATOR_PROPERTIES)) {
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

        this.curseControllerRegenerationEnabled = Boolean.parseBoolean(this.getProperty("de.griefed.serverpackcreator.spring.cursecontroller.regenerate.enabled", "false"));

        this.queueMaxDiskUsage = Integer.parseInt(getProperty("de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage", "90"));

        this.scheduleDatabaseCleanup = getProperty("de.griefed.serverpackcreator.spring.schedules.database.cleanup", "0 0 24 * *");

        this.saveLoadedConfiguration = Boolean.parseBoolean(getProperty("de.griefed.serverpackcreator.configuration.saveloadedconfig", "false"));
    }

    /**
     * Reload serverpackcreator.properties.
     * @author Griefed
     */
    public void reload() {

        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            new Properties();
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

                try (OutputStream outputStream = new FileOutputStream(this.FILE_SERVERPACKCREATOR_PROPERTIES)) {
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

        this.curseControllerRegenerationEnabled = Boolean.parseBoolean(this.getProperty("de.griefed.serverpackcreator.spring.cursecontroller.regenerate.enabled", "false"));

        this.queueMaxDiskUsage = Integer.parseInt(getProperty("de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage", "90"));

        this.scheduleDatabaseCleanup = getProperty("de.griefed.serverpackcreator.spring.schedules.database.cleanup", "0 0 24 * *");

        this.saveLoadedConfiguration = Boolean.parseBoolean(getProperty("de.griefed.serverpackcreator.configuration.saveloadedconfig", "false"));
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
    void addToListOfDirectoriesToExclude(String entry) {
        if (!this.listDirectoriesExclude.contains(entry) && !this.listCheckAgainstNewEntry.contains(entry)) {
            this.listDirectoriesExclude.add(entry);
        }
    }

    /**
     * Getter for whether the regeneration of server packs is enabled.
     * @author Griefed
     * @return Boolean. Whether the regeneration of server packs is enabled.
     */
    public boolean getCurseControllerRegenerationEnabled() {
        return curseControllerRegenerationEnabled;
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
     * Getter for the cron-schedule at which ServerPackCreator should clean up the database.
     * @author Griefed
     * @return String. The cron-schedule at which ServerPackCreator should clean up the database.
     */
    public String getScheduleDatabaseCleanup() {
        return scheduleDatabaseCleanup;
    }

    /**
     * Getter for the maximum disk usage at which JMS/Artemis will stop storing queues on disk.
     * @author Griefed
     * @return Integer. The maximum disk usage at which JMS/Artemis will stop storing queues on disk.
     */
    public int getQueueMaxDiskUsage() {
        return queueMaxDiskUsage;
    }
}
