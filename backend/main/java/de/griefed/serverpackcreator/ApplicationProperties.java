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
 *
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
    private final List<String> LIST_FALLBACK_MODS_DEFAULT = new ArrayList<>(Arrays.asList(FALLBACK_MODS_DEFAULT_ASSTRING.split(",")));
    private String DIRECTORY_SERVER_PACKS;

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
    public final File FILE_LOG4J2XML = new File("log4j2.xml");
    public final File DEFAULT_DIRECTORY_ADDONS = new File("addons");
    public final File DEFAULT_DIRECTORY_LOGS = new File("logs");
    public final File DEFAULT_DIRECTORY_SERVER_PACKS = new File("server-packs");
    public final File DEFAULT_DIRECTORY_WORK = new File("work");
    public final File DEFAULT_DIRECTORY_TEMP = new File("temp");
    public final File DEFAULT_DIRECTORY_WORK_TEMP = new File("work/temp");

    //ConfigurationHandler related
    private List<String> LIST_FALLBACK_MODS;
    private List<String> LIST_DIRECTORIES_EXCLUDE;

    //VersionLister related
    public final File PATH_FILE_MANIFEST_MINECRAFT = new File("./work/minecraft-manifest.json");
    public final File PATH_FILE_MANIFEST_FORGE = new File("./work/forge-manifest.json");
    public final File PATH_FILE_MANIFEST_FABRIC = new File("./work/fabric-manifest.xml");
    public final File PATH_FILE_MANIFEST_FABRIC_INSTALLER = new File("./work/fabric-installer-manifest.xml");

    //CurseController related
    private boolean CURSE_CONTROLLER_REGENERATION_ENABLED = false;

    //Check related
    private final List<String> LIST_CHECK_AGAINST_NEW_ENTRY = new ArrayList<>(
            Arrays.asList(
                    "mods",
                    "config",
                    "defaultconfigs",
                    "scripts",
                    "saves",
                    "seeds",
                    "libraries"
            )
    );

    /**
     *
     * @author Griefed
     */
    @Autowired
    public ApplicationProperties() {
        try (
        InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            new Properties();
            load(inputStream);
        } catch (
        IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }

        String tempDir = null;
        try {
            tempDir = this.getProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
        } catch (NullPointerException npe) {
            this.setProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
            tempDir = "server-packs";
        } finally {
            if (tempDir != null && !tempDir.equals("") && new File(tempDir).isDirectory()) {
                this.setProperty("de.griefed.serverpackcreator.dir.serverpacks",tempDir);
                this.DIRECTORY_SERVER_PACKS = tempDir;

                try (OutputStream outputStream = new FileOutputStream(this.FILE_SERVERPACKCREATOR_PROPERTIES)) {
                    this.store(outputStream, null);
                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } else {
                this.DIRECTORY_SERVER_PACKS = "server-packs";
            }
        }

        if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist") == null) {

            this.LIST_FALLBACK_MODS = this.LIST_FALLBACK_MODS_DEFAULT;

            LOG.debug("Fallbackmodslist property null. Using fallback: " + this.LIST_FALLBACK_MODS);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist").contains(",")) {

            this.LIST_FALLBACK_MODS = new ArrayList<>(
                    Arrays.asList(this.getProperty(
                            "de.griefed.serverpackcreator.configuration.fallbackmodslist",
                            this.FALLBACK_MODS_DEFAULT_ASSTRING)));

            LOG.debug("Fallbackmodslist set to: " + this.LIST_FALLBACK_MODS);

        } else {

            this.LIST_FALLBACK_MODS = Collections.singletonList((this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")));

            LOG.debug("Fallbackmodslist set to: " + this.LIST_FALLBACK_MODS);
        }

        if (this.getProperty("de.griefed.serverpackcreator.configuration.copydirs.exclude") == null) {

            this.LIST_DIRECTORIES_EXCLUDE = new ArrayList<>(Arrays.asList(
                    "overrides","packmenu","resourcepacks","server_pack","fancymenu"
            ));

            LOG.debug("copydirs.exclude property null. Using fallback: " + this.LIST_DIRECTORIES_EXCLUDE);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.copydirs.exclude").contains(",")) {
            this.LIST_DIRECTORIES_EXCLUDE = new ArrayList<>(Arrays.asList(this.getProperty(
                            "de.griefed.serverpackcreator.configuration.copydirs.exclude",
                            "overrides,packmenu,resourcepacks,server_pack,fancymenu"
                    ).split(",")
            ));

            LOG.debug("Directories to exclude set to: " + this.LIST_DIRECTORIES_EXCLUDE);

        } else {

            this.LIST_DIRECTORIES_EXCLUDE = Collections.singletonList(this.getProperty("de.griefed.serverpackcreator.configuration.copydirs.exclude"));

            LOG.debug("Directories to exclude set to: " + this.LIST_DIRECTORIES_EXCLUDE);

        }

        this.CURSE_CONTROLLER_REGENERATION_ENABLED = Boolean.parseBoolean(this.getProperty("de.griefed.serverpackcreator.spring.cursecontroller.regenerate.enabled", "false"));
    }

    /**
     *
     * @author Griefed
     */
    public void reload() {
        try (
                InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            new Properties();
            load(inputStream);
        } catch (
                IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }

        String tempDir = null;
        try {
            tempDir = this.getProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
        } catch (NullPointerException npe) {
            this.setProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
            tempDir = "server-packs";
        } finally {
            if (tempDir != null && !tempDir.equals("") && new File(tempDir).isDirectory()) {
                this.setProperty("de.griefed.serverpackcreator.dir.serverpacks",tempDir);
                this.DIRECTORY_SERVER_PACKS = tempDir;

                try (OutputStream outputStream = new FileOutputStream(this.FILE_SERVERPACKCREATOR_PROPERTIES)) {
                    this.store(outputStream, null);
                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } else {
                this.DIRECTORY_SERVER_PACKS = "server-packs";
            }
        }

        if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist") == null) {

            this.LIST_FALLBACK_MODS = this.LIST_FALLBACK_MODS_DEFAULT;

            LOG.debug("Fallbackmodslist property null. Using fallback: " + this.LIST_FALLBACK_MODS);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist").contains(",")) {

            this.LIST_FALLBACK_MODS = new ArrayList<>(
                    Arrays.asList(this.getProperty(
                            "de.griefed.serverpackcreator.configuration.fallbackmodslist",
                            this.FALLBACK_MODS_DEFAULT_ASSTRING)));

            LOG.debug("Fallbackmodslist set to: " + this.LIST_FALLBACK_MODS);

        } else {

            this.LIST_FALLBACK_MODS = Collections.singletonList((this.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")));

            LOG.debug("Fallbackmodslist set to: " + this.LIST_FALLBACK_MODS);
        }

        if (this.getProperty("de.griefed.serverpackcreator.configuration.copydirs.exclude") == null) {

            this.LIST_DIRECTORIES_EXCLUDE = new ArrayList<>(Arrays.asList(
                    "overrides","packmenu","resourcepacks","server_pack","fancymenu"
            ));

            LOG.debug("copydirs.exclude property null. Using fallback: " + this.LIST_DIRECTORIES_EXCLUDE);

        } else if (this.getProperty("de.griefed.serverpackcreator.configuration.copydirs.exclude").contains(",")) {
            this.LIST_DIRECTORIES_EXCLUDE = new ArrayList<>(Arrays.asList(this.getProperty(
                            "de.griefed.serverpackcreator.configuration.copydirs.exclude",
                            "overrides,packmenu,resourcepacks,server_pack,fancymenu"
                    ).split(",")
            ));

            LOG.debug("Directories to exclude set to: " + this.LIST_DIRECTORIES_EXCLUDE);

        } else {

            this.LIST_DIRECTORIES_EXCLUDE = Collections.singletonList(this.getProperty("de.griefed.serverpackcreator.configuration.copydirs.exclude"));

            LOG.debug("Directories to exclude set to: " + this.LIST_DIRECTORIES_EXCLUDE);

        }

        this.CURSE_CONTROLLER_REGENERATION_ENABLED = Boolean.parseBoolean(this.getProperty("de.griefed.serverpackcreator.spring.cursecontroller.regenerate.enabled", "false"));
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public String getDIRECTORY_SERVER_PACKS() {
        return DIRECTORY_SERVER_PACKS;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public List<String> getLIST_FALLBACK_MODS() {
        return LIST_FALLBACK_MODS;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public List<String> getLIST_DIRECTORIES_EXCLUDE() {
        return LIST_DIRECTORIES_EXCLUDE;
    }

    /**
     *
     * @author Griefed
     * @param entry
     */
    void addLIST_DIRECTORIES_EXCLUDE(String entry) {
        if (!this.LIST_DIRECTORIES_EXCLUDE.contains(entry) && !this.LIST_CHECK_AGAINST_NEW_ENTRY.contains(entry)) {
            this.LIST_DIRECTORIES_EXCLUDE.add(entry);
        }
    }

    public boolean getCURSE_CONTROLLER_REGENERATION_ENABLED() {
        return this.CURSE_CONTROLLER_REGENERATION_ENABLED;
    }
}
