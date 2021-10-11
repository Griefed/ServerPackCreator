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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moandjiezana.toml.Toml;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.VersionLister;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * <strong>Table of methods</strong><p>
 * 1. {@link #ServerPackHandler(LocalizationManager, CurseCreateModpack, AddonsHandler, ConfigurationHandler, Properties, VersionLister)}<br>
 * 2. {@link #cleanupEnvironment()}<br>
 * 3. {@link #cleanUpServerPack(File, File, String, String, String)}<br>
 * 4. {@link #copyFiles(String, List, List, String)}<br>
 * 5. {@link #copyIcon()}<br>
 * 6. {@link #copyProperties()}<br>
 * 7. {@link #createEula()}<br>
 * 8. {@link #createStartScripts(String, String, String, String)}<br>
 * 9. {@link #downloadFabricJar()}<br>
 * 10.{@link #downloadForgeJar(String, String)}<br>
 * 11.{@link #excludeClientMods(String, List, String)}<br>
 * 12.{@link #excludeFileOrDirectory(String)}<br>
 * 13.{@link #getFILE_SERVERPACKCREATOR_PROPERTIES()}<br>
 * 14.{@link #getLinuxFile()}<br>
 * 15.{@link #getWindowsFile()}<br>
 * 16.{@link #getIconFile()}<br>
 * 17.{@link #getMinecraftServerJarUrl(String)}<br>
 * 18.{@link #getObjectMapper()}<br>
 * 19.{@link #getPropertiesFile()}<br>
 * 20.{@link #getSERVER_PACKS_DIR()}<br>
 * 21.{@link #installServer(String, String, String, String)}<br>
 * 22.{@link #run(ConfigurationModel)}<br>
 * 23.{@link #run(File, ConfigurationModel)}<br>
 * 24.{@link #scanAnnotations(File[])}<br>
 * 25.{@link #scanTomls(File[])}<br>
 * 26.{@link #zipBuilder(String, boolean)}<p>
 * Requires an instance of {@link ConfigurationHandler} from which to get all required information about the modpack and the
 * then to be generated server pack.
 * <p>
 * Requires an instance of {@link LocalizationManager} for use of localization, but creates one if injected one is null.
 * <p>
 * Create a server pack from a modpack by copying all specified or required files from the modpack to the server pack
 * as well as installing the modloader server for the specified modloader, modloader version and Minecraft version.
 * Create a ZIP-archive of the server pack, excluding the Minecraft server JAR, for immediate upload to CurseForge or
 * other platforms.
 * @author Griefed
 */
@Component
public class ServerPackHandler {

    private static final Logger LOG = LogManager.getLogger(DefaultFiles.class);
    private static final Logger LOG_INSTALLER = LogManager.getLogger("InstallerLogger");

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final AddonsHandler ADDONSHANDLER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final VersionLister VERSIONLISTER;

    private final File FILE_SERVERPACKCREATOR_PROPERTIES = new File("serverpackcreator.properties");
    private final File FILE_PROPERTIES = new File("server.properties");
    private final File FILE_ICON = new File("server-icon.png");
    private final File FILE_WINDOWS = new File("start.bat");
    private final File FILE_LINUX = new File("start.sh");
    private final File FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS = new File("user_jvm_args.txt");

    private final String SERVER_PACKS_DIR;

    private Properties serverPackCreatorProperties;

    private String serverPackDestination;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} required for creating a modpack from CurseForge.
     * @param injectedAddonsHandler Instance of {@link AddonsHandler} required for accessing installed addons, if any exist.
     * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler} required for accessing checks.
     * @param injectedServerPackCreatorProperties Instance of {@link Properties} required for various different things.
     * @param injectedVersionLister Instance of {@link VersionLister} required for everything version related.
     */
    @Autowired
    public ServerPackHandler(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack,
                             AddonsHandler injectedAddonsHandler, ConfigurationHandler injectedConfigurationHandler,
                             Properties injectedServerPackCreatorProperties, VersionLister injectedVersionLister) {

        if (injectedServerPackCreatorProperties == null) {
            try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
                this.serverPackCreatorProperties = new Properties();
                this.serverPackCreatorProperties.load(inputStream);
            } catch (IOException ex) {
                LOG.error("Couldn't read properties file.", ex);
            }
        } else {
            this.serverPackCreatorProperties = injectedServerPackCreatorProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedAddonsHandler == null) {
            this.ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        } else {
            this.ADDONSHANDLER = injectedAddonsHandler;
        }

        if (injectedVersionLister == null) {
            this.VERSIONLISTER = new VersionLister(serverPackCreatorProperties);
        } else {
            this.VERSIONLISTER = injectedVersionLister;
        }

        if (injectedConfigurationHandler == null) {
            this.CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, serverPackCreatorProperties);
        } else {
            this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        }

        String tempDir = null;
        try {
            tempDir = serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
        } catch (NullPointerException npe) {
            serverPackCreatorProperties.setProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
            tempDir = "server-packs";
        } finally {
            if (tempDir != null && !tempDir.equals("") && new File(tempDir).isDirectory()) {
                serverPackCreatorProperties.setProperty("de.griefed.serverpackcreator.dir.serverpacks",tempDir);
                SERVER_PACKS_DIR = tempDir;

                try (OutputStream outputStream = new FileOutputStream(getFILE_SERVERPACKCREATOR_PROPERTIES())) {
                    serverPackCreatorProperties.store(outputStream, null);
                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } else {
                SERVER_PACKS_DIR = "server-packs";
            }
        }
    }

    /**
     * Getter for the serverpackcreator.properties-file.
     * @author Griefed
     * @return File. Returns the serverpackcreator.properties-file.
     */
    public File getFILE_SERVERPACKCREATOR_PROPERTIES() {
        return FILE_SERVERPACKCREATOR_PROPERTIES;
    }

    /**
     * Getter for the directory in which server-packs will be generated and stored in.
     * @author Griefed
     * @return String. Returns the path to the server-packs directory as a string.
     */
    public String getSERVER_PACKS_DIR() {
        return SERVER_PACKS_DIR;
    }

    /**
     * Getter for server.properties.
     * @author Griefed
     * @return Returns the server.properties-file for use in {@link #copyProperties()}
     */
    public File getPropertiesFile() {
        return FILE_PROPERTIES;
    }

    /**
     * Getter for server-icon.png
     * @author Griefed
     * @return Returns the server-icon.png-file for use in {@link #copyIcon()}
     */
    public File getIconFile() {
        return FILE_ICON;
    }

    /**
     * Getter for start.bat.
     * @author Griefed
     * @return Returns the start.bat-file for use in {@link #createStartScripts(String, String, String, String)}
     */
    public File getWindowsFile() {
        return FILE_WINDOWS;
    }

    /**
     * Getter for start.sh
     * @author Griefed
     * @return Returns the start.sh-file for use in {@link #createStartScripts(String, String, String, String)}
     */
    public File getLinuxFile() {
        return FILE_LINUX;
    }

    /**
     * Getter for user_jvm_args.txt.
     * @author Griefed
     * @return Returns the user_jvm_args.txt-file for use in {@link #createStartScripts(String, String, String, String)}
     */
    public File getForgeOneSevenUserJvmArgsFile() {
        return FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS;
    }

    /**
     * Getter for the object-mapper used for working with JSON-data.
     * @author Griefed
     * @return ObjectMapper. Returns the object-mapper used for working with JSON-data.
     */
    public ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return objectMapper;
    }

    /**
     * Getter for the destination of the server pack to be generated.
     * @author Griefed
     * @return String. Returns the destination of the server pack to be generated.
     */
    public String getServerPackDestination() {
        return serverPackDestination;
    }

    /**
     * Setter for the destination of the server pack to be generated.
     * @author Griefed
     * @param modpackDir String. The destination of the server pack to be generated.
     * @param serverPackSuffix String. The suffix for the server pack to be generated.
     */
    public void setServerPackDestination(String modpackDir, String serverPackSuffix) {
        this.serverPackDestination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1) + serverPackSuffix;
    }

    /**
     * Create a server pack if the check of the configuration-file was successfull.
     * @author Griefed
     * @param configFileToUse A ServerPackCreator-configuration-file for {@link ConfigurationHandler} to check and  generate a
     * server pack from.
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @return Boolean. Returns true if the server pack was successfully generated.
     */
    public boolean run(File configFileToUse, ConfigurationModel configurationModel) {
        // TODO: Once API and webUI are implemented, test parallel runs. Parallel runs MUST be possible.

        if (!CONFIGURATIONHANDLER.checkConfiguration(configFileToUse, true, configurationModel)) {

            /*
             * Check whether the server pack for the specified modpack already exists and whether overwrite is disabled.
             * If the server pack exists and overwrite is disabled, no new server pack will be generated.
             */
            if (serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.serverpack.overwrite.enabled").equals("false") &&
                    new File(String.format("%s/%s",
                            getSERVER_PACKS_DIR(),
                            getServerPackDestination())
                    ).exists()) {

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.overwrite"));
                return true;

            } else {

                setServerPackDestination(configurationModel.getModpackDir(), configurationModel.getServerPackSuffix());

                // Make sure no files from previously generated server packs interrupt us.
                cleanupEnvironment();

                // Recursively copy all specified directories and files, excluding clientside-only mods, to server pack.
                copyFiles(configurationModel.getModpackDir(), configurationModel.getCopyDirs(), configurationModel.getClientMods(), configurationModel.getMinecraftVersion());

                // Copy start scripts for specified modloader from server_files to server pack.
                createStartScripts(configurationModel.getModLoader(), configurationModel.getJavaArgs(), configurationModel.getMinecraftVersion(), configurationModel.getModLoaderVersion());

                // If true, Install the modloader software for the specified Minecraft version, modloader, modloader version
                if (configurationModel.getIncludeServerInstallation()) {
                    installServer(configurationModel.getModLoader(), configurationModel.getMinecraftVersion(), configurationModel.getModLoaderVersion(), configurationModel.getJavaPath());
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.server"));
                }

                // If true, copy the server-icon.png from server_files to the server pack.
                if (configurationModel.getIncludeServerIcon()) {
                    copyIcon();
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.icon"));
                }

                // If true, copy the server.properties from server_files to the server pack.
                if (configurationModel.getIncludeServerProperties()) {
                    copyProperties();
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.properties"));
                }

                createEula();

                // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
                if (configurationModel.getIncludeZipCreation()) {
                    zipBuilder(configurationModel.getMinecraftVersion(), configurationModel.getIncludeServerInstallation());
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.zip"));
                }

                // Inform user about location of newly generated server pack.
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.serverpack"), getServerPackDestination()));
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.archive"), getServerPackDestination()));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.finish"));

                if (ADDONSHANDLER.getListOfServerPackAddons().isEmpty() || ADDONSHANDLER.getListOfServerPackAddons() == null) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.noaddonstoexecute"));
                } else {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.executingaddons"));
                    ADDONSHANDLER.runServerPackAddons(configurationModel, CONFIGURATIONHANDLER);
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.addonsexecuted"));
                }

                return true;
            }

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("main.log.error.runincli"));

            return false;
        }
    }

    /**
     * Create a server pack if the check of the {@link ConfigurationModel} was successfull.
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @return Boolean. Returns true if the server pack was successfully generated.
     */
    public boolean run(ConfigurationModel configurationModel) {
        // TODO: Once API and webUI are implemented, test parallel runs. Parallel runs MUST be possible.

        if (!CONFIGURATIONHANDLER.checkConfiguration( true, configurationModel)) {

            /*
             * Check whether the server pack for the specified modpack already exists and whether overwrite is disabled.
             * If the server pack exists and overwrite is disabled, no new server pack will be generated.
             */
            if (serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.serverpack.overwrite.enabled").equals("false") &&
                    new File(String.format("%s/%s",
                            getSERVER_PACKS_DIR(),
                            getServerPackDestination())
                    ).exists()) {

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.overwrite"));
                return true;

            } else {

                setServerPackDestination(configurationModel.getModpackDir(), configurationModel.getServerPackSuffix());

                // Make sure no files from previously generated server packs interrupt us.
                cleanupEnvironment();

                // Recursively copy all specified directories and files, excluding clientside-only mods, to server pack.
                copyFiles(configurationModel.getModpackDir(), configurationModel.getCopyDirs(), configurationModel.getClientMods(), configurationModel.getMinecraftVersion());

                // Copy start scripts for specified modloader from server_files to server pack.
                createStartScripts(configurationModel.getModLoader(), configurationModel.getJavaArgs(), configurationModel.getMinecraftVersion(), configurationModel.getModLoaderVersion());

                // If true, Install the modloader software for the specified Minecraft version, modloader, modloader version
                if (configurationModel.getIncludeServerInstallation()) {
                    installServer(configurationModel.getModLoader(), configurationModel.getMinecraftVersion(), configurationModel.getModLoaderVersion(), configurationModel.getJavaPath());
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.server"));
                }

                // If true, copy the server-icon.png from server_files to the server pack.
                if (configurationModel.getIncludeServerIcon()) {
                    copyIcon();
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.icon"));
                }

                // If true, copy the server.properties from server_files to the server pack.
                if (configurationModel.getIncludeServerProperties()) {
                    copyProperties();
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.properties"));
                }

                // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
                if (configurationModel.getIncludeZipCreation()) {
                    zipBuilder(configurationModel.getMinecraftVersion(), configurationModel.getIncludeServerInstallation());
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.zip"));
                }

                // Inform user about location of newly generated server pack.
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.serverpack"), getServerPackDestination()));
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.archive"), getServerPackDestination()));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.finish"));

                if (ADDONSHANDLER.getListOfServerPackAddons().isEmpty() || ADDONSHANDLER.getListOfServerPackAddons() == null) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.noaddonstoexecute"));
                } else {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.executingaddons"));
                    ADDONSHANDLER.runServerPackAddons(configurationModel, CONFIGURATIONHANDLER);
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.addonsexecuted"));
                }

                return true;
            }

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("main.log.error.runincli"));

            return false;
        }
    }

    /**
     * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure newly generated
     * server pack is as clean as possible.
     * @author Griefed
     */
    void cleanupEnvironment() {

        if (new File(String.format("%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination())).exists()) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.folder.enter"));
            Path serverPack = Paths.get(String.format("%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination()));

            try {

                Files.walkFileTree(serverPack,

                        new SimpleFileVisitor<Path>() {

                            @Override
                            public FileVisitResult postVisitDirectory(
                                    Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(
                                    Path file, BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                        });

            } catch (IOException ex) {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupenvironment.folder.delete"), getServerPackDestination()));

            } finally {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.folder.complete"));
            }
        }

        if (new File(String.format("%s/%s_server_pack.zip", getSERVER_PACKS_DIR(), getServerPackDestination())).exists()) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.zip.enter"));

            boolean isZipDeleted = new File(String.format("%s/%s_server_pack.zip", getSERVER_PACKS_DIR(), getServerPackDestination())).delete();

            if (isZipDeleted) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.zip.complete"));
            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupenvironment.zip.delete"));
            }
        }
    }

    /**
     * Create start-scripts for the generated server pack.
     * @author Griefed
     * @param modLoader String. Whether to copy the Forge or Fabric scripts into the server pack.
     * @param javaArguments String. Java arguments to write the start-scripts with.
     * @param minecraftVersion String. The Minecraft version the modpack uses.
     * @param modloaderVersion String. The modloader version the modpack uses.
     */
    void createStartScripts(String modLoader, String javaArguments, String minecraftVersion, String modloaderVersion) {

        if (javaArguments.equals("empty")) {
            javaArguments = "";
        }

        if (modLoader.equalsIgnoreCase("Forge")) {

            String[] minecraft = minecraftVersion.split("\\.");

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copystartscripts.forge"));

            if (Integer.parseInt(minecraft[1]) < 17) {

                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(
                                String.valueOf(
                                        Paths.get(
                                                String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getWindowsFile())
                                        )
                                )
                        )
                )) {

                    /*
                     * FORGE 1.16 AND OLDER!
                     * If the specified Minecraft version is older than 1.17.1, then we need to generate scripts which run
                     * Forge the old way, with the Minecraft server-jar and the Forge server-jar, executing the Forge server-jar
                     * with the given Java args.
                     *
                     * Forge Batch
                     */
                    writer.write(
                            ":: Start script generated by ServerPackCreator.\n" +
                            ":: This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n" +
                            ":: If everything is in order, the server is started.\n" +
                            "@ECHO off\n" +
                            "\n" +
                            "SET MINECRAFT=\"" + minecraftVersion + "\"\n" +
                            "SET FORGE=\"" + modloaderVersion + "\"\n" +
                            "SET ARGS=" + javaArguments + "\n" +
                            "\n" +
                            "IF NOT EXIST forge.jar (\n" +
                            "\n" +
                            "  ECHO Forge Server JAR-file not found. Downloading installer...\n" +
                            "  powershell -Command \"(New-Object Net.WebClient).DownloadFile('https://files.minecraftforge.net/maven/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-installer.jar', 'forge-installer.jar')\"\n" +
                            "\n" +
                            "  IF EXIST forge-installer.jar (\n" +
                            "\n" +
                            "    ECHO Installer downloaded. Installing...\n" +
                            "    java -jar forge-installer.jar --installServer\n" +
                            "    MOVE forge-%MINECRAFT%-%FORGE%.jar forge.jar\n" +
                            "\n" +
                            "    IF EXIST forge.jar (\n" +
                            "      DEL forge-installer.jar\n" +
                            "      ECHO Installation complete. forge-installer.jar deleted.\n" +
                            "    )\n" +
                            "\n" +
                            "  ) ELSE (\n" +
                            "    ECHO forge-installer.jar not found. Maybe the Forges servers are having trouble.\n" +
                            "    ECHO Please try again in a couple of minutes.\n" +
                            "  )\n" +
                            ") ELSE (\n" +
                            "  ECHO forge.jar present. Moving on...\n" +
                            ")\n" +
                            "\n" +
                            "IF NOT EXIST minecraft_server.%MINECRAFT%.jar (\n" +
                            "  ECHO Minecraft Server JAR-file not found. Downloading...\n" +
                            "  powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + getMinecraftServerJarUrl(minecraftVersion) + "', 'minecraft_server.%MINECRAFT%.jar')\"\n" +
                            ") ELSE (\n" +
                            "  ECHO minecraft_server.%MINECRAFT%.jar present. Moving on...\n" +
                            ")\n" +
                            "\n" +
                            "IF NOT EXIST eula.txt (\n" +
                            "  ECHO eula.txt not found. Creating...\n" +
                            "  ECHO #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://account.mojang.com/documents/minecraft_eula^).> eula.txt\n" +
                            "  ECHO eula=true>> eula.txt\n" +
                            ") ELSE (\n" +
                            "  ECHO eula.txt present. Moving on...\n" +
                            ")\n" +
                            "\n" +
                            "ECHO Starting server...\n" +
                            "ECHO Minecraft version: %MINECRAFT%\n" +
                            "ECHO Forge version: %FORGE%\n" +
                            "ECHO Java args: %ARGS%\n" +
                            "\n" +
                            "java %ARGS% -jar forge.jar --nogui\n" +
                            "\n" +
                            "PAUSE"
                    );

                } catch (IOException ex) {
                    LOG.error("Error generating batch-script for Forge.", ex);
                }

                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(
                                String.valueOf(
                                        Paths.get(
                                                String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getLinuxFile())
                                        )
                                )
                        )
                )) {

                    /*
                     * FORGE 1.16 AND OLDER!
                     * If the specified Minecraft version is older than 1.17.1, then we need to generate scripts which run
                     * Forge the old way, with the Minecraft server-jar and the Forge server-jar, executing the Forge server-jar
                     * with the given Java args.
                     *
                     * Forge Bash file
                     */
                    writer.write(
                            "#!/usr/bin/env bash\n" +
                            "# Start script generated by ServerPackCreator.\n" +
                            "# This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n" +
                            "# If everything is in order, the server is started.\n" +
                            "\n" +
                            "MINECRAFT=\"" + minecraftVersion + "\"\n" +
                            "FORGE=\"" + modloaderVersion + "\"\n" +
                            "ARGS=\"" + javaArguments + "\"\n" +
                            "\n" +
                            "if [[ ! -s \"forge.jar\" ]];then\n" +
                            "\n" +
                            "  echo \"Forge Server JAR-file not found. Downloading installer...\";\n" +
                            "  wget -O forge-installer.jar https://files.minecraftforge.net/maven/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-installer.jar;\n" +
                            "\n" +
                            "  if [[ -s \"forge-installer.jar\" ]]; then\n" +
                            "\n" +
                            "    echo \"Installer downloaded. Installing...\";\n" +
                            "    java -jar forge-installer.jar --installServer;\n" +
                            "    mv forge-$MINECRAFT-$FORGE.jar forge.jar;\n" +
                            "\n" +
                            "    if [[ -s \"forge.jar\" ]];then\n" +
                            "      rm -f forge-installer.jar;\n" +
                            "      echo \"Installation complete. forge-installer.jar deleted.\";\n" +
                            "    fi\n" +
                            "\n" +
                            "  else\n" +
                            "    echo \"forge-installer.jar not found. Maybe the Forges servers are having trouble.\";\n" +
                            "    echo \"Please try again in a couple of minutes.\";\n" +
                            "  fi\n" +
                            "else\n" +
                            "  echo \"forge.jar present. Moving on...\"\n" +
                            "fi\n" +
                            "\n" +
                            "if [[ ! -s \"minecraft_server.$MINECRAFT.jar\" ]];then\n" +
                            "  echo \"Minecraft Server JAR-file not found. Downloading...\";\n" +
                            "  wget -O minecraft_server.$MINECRAFT.jar " + getMinecraftServerJarUrl(minecraftVersion) + ";\n" +
                            "else\n" +
                            "  echo \"minecraft_server.$MINECRAFT.jar present. Moving on...\"\n" +
                            "fi\n" +
                            "\n" +
                            "if [[ ! -s \"eula.txt\" ]];then\n" +
                            "  echo \"eula.txt not found. Creating...\";\n" +
                            "  echo \"#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\" > eula.txt;\n" +
                            "  echo \"eula=true\" >> eula.txt;\n" +
                            "else\n" +
                            "  echo \"eula.txt present. Moving on...\";\n" +
                            "fi\n" +
                            "\n" +
                            "echo \"Starting server...\";\n" +
                            "echo \"Minecraft version: $MINECRAFT\";\n" +
                            "echo \"Forge version: $FORGE\";\n" +
                            "echo \"Java args: $ARGS\";\n" +
                            "\n" +
                            "java $ARGS -jar forge.jar --nogui"
                    );

                } catch (IOException ex) {
                    LOG.error("Error generating shell-script for Forge.", ex);
                }

            } else {

                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(
                                String.valueOf(
                                        Paths.get(
                                                String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getWindowsFile())
                                        )
                                )
                        )
                )) {

                    /*
                     * FORGE 1.17 AND NEWER!
                     * If the specified Minecraft version is newer than or equal to 1.17.1, then we need to generate scripts which run
                     * Forge the new way, by running @user_jvm_args.txt and @libraries[...] etc.
                     *
                     * Forge Batch
                     */
                    writer.write(
                            ":: Start script generated by ServerPackCreator.\n" +
                            ":: This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n" +
                            ":: If everything is in order, the server is started.\n" +
                            "@ECHO off\n" +
                            "\n" +
                            "SET MINECRAFT=\"" + minecraftVersion + "\"\n" +
                            "SET FORGE=\"" + modloaderVersion + "\"\n" +
                            "SET ARGS=" + javaArguments + "\n" +
                            "\n" +
                            "IF NOT EXIST libraries/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-server.jar (\n" +
                            "\n" +
                            "  ECHO Forge Server JAR-file not found. Downloading installer...\n" +
                            "  powershell -Command \"(New-Object Net.WebClient).DownloadFile('https://files.minecraftforge.net/maven/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-installer.jar', 'forge-installer.jar')\"\n" +
                            "\n" +
                            "  IF EXIST forge-installer.jar (\n" +
                            "\n" +
                            "    ECHO Installer downloaded. Installing...\n" +
                            "    java -jar forge-installer.jar --installServer\n" +
                            "\n" +
                            "    IF EXIST libraries/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-server.jar (\n" +
                            "      DEL forge-installer.jar\n" +
                            "      ECHO Installation complete. forge-installer.jar deleted.\n" +
                            "    )\n" +
                            "\n" +
                            "  ) ELSE (\n" +
                            "    ECHO forge-installer.jar not found. Maybe the Forges servers are having trouble.\n" +
                            "    ECHO Please try again in a couple of minutes.\n" +
                            "  )\n" +
                            ") ELSE (\n" +
                            "  ECHO Forge server present. Moving on...\n" +
                            ")\n" +
                            "\n" +
                            "IF NOT EXIST libraries/net/minecraft/server/%MINECRAFT%/server-%MINECRAFT%.jar (\n" +
                            "  ECHO Minecraft Server JAR-file not found. Downloading...\n" +
                            "  powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + getMinecraftServerJarUrl(minecraftVersion) + "', 'libraries/net/minecraft/server/%MINECRAFT%/server-%MINECRAFT%.jar')\"\n" +
                            ") ELSE (\n" +
                            "  ECHO Minecraft server present. Moving on...\n" +
                            ")\n" +
                            "\n" +
                            "IF NOT EXIST eula.txt (\n" +
                            "  ECHO eula.txt not found. Creating...\n" +
                            "  ECHO #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://account.mojang.com/documents/minecraft_eula^).> eula.txt\n" +
                            "  ECHO eula=true>> eula.txt\n" +
                            ") ELSE (\n" +
                            "  ECHO eula.txt present. Moving on...\n" +
                            ")\n" +
                            "\n" +
                            "IF EXIST run.bat (\n" +
                            "  DEL run.bat\n" +
                            "  ECHO Deleted run.bat as we already have start.bat\n" +
                            ")\n" +
                            "IF EXIST run.sh (\n" +
                            "  DEL run.sh\n" +
                            "  ECHO Deleted run.sh as we already have start.sh\n" +
                            ")\n" +
                            "\n" +
                            "ECHO Starting server...\n" +
                            "ECHO Minecraft version: %MINECRAFT%\n" +
                            "ECHO Forge version: %FORGE%\n" +
                            "ECHO Java args in user_jvm_args.txt: %ARGS%\n" +
                            "\n" +
                            "REM Forge requires a configured set of both JVM and program arguments.\n" +
                            "REM Add custom JVM arguments to the user_jvm_args.txt\n" +
                            "REM Add custom program arguments {such as nogui} to this file in the next line before the %* or\n" +
                            "REM  pass them to this script directly\n" +
                            "\n" +
                            "java  @user_jvm_args.txt @libraries/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/win_args.txt nogui %*\n" +
                            "\n" +
                            "PAUSE"
                    );

                } catch (IOException ex) {
                    LOG.error("Error generating batch-script for Forge.", ex);
                }

                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(
                                String.valueOf(
                                        Paths.get(
                                                String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getLinuxFile())
                                        )
                                )
                        )
                )) {

                    /*
                     * FORGE 1.17 AND NEWER!
                     * If the specified Minecraft version is newer than or equal to 1.17.1, then we need to generate scripts which run
                     * Forge the new way, by running @user_jvm_args.txt and @libraries[...] etc.
                     *
                     * Forge Bash
                     */
                    writer.write(
                            "#!/usr/bin/env bash\n" +
                            "# Start script generated by ServerPackCreator.\n" +
                            "# This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n" +
                            "# If everything is in order, the server is started.\n" +
                            "\n" +
                            "MINECRAFT=\"" + minecraftVersion + "\"\n" +
                            "FORGE=\"" + modloaderVersion + "\"\n" +
                            "ARGS=\"" + javaArguments + "\"\n" +
                            "\n" +
                            "if [[ ! -s \"libraries/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-server.jar\" ]];then\n" +
                            "\n" +
                            "  echo \"Forge Server JAR-file not found. Downloading installer...\";\n" +
                            "  wget -O forge-installer.jar https://files.minecraftforge.net/maven/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-installer.jar;\n" +
                            "\n" +
                            "  if [[ -s \"forge-installer.jar\" ]]; then\n" +
                            "\n" +
                            "    echo \"Installer downloaded. Installing...\";\n" +
                            "    java -jar forge-installer.jar --installServer;\n" +
                            "\n" +
                            "    if [[ -s \"libraries/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-server.jar\" ]];then\n" +
                            "      rm -f forge-installer.jar;\n" +
                            "      echo \"Installation complete. forge-installer.jar deleted.\";\n" +
                            "    fi\n" +
                            "\n" +
                            "  else\n" +
                            "    echo \"forge-installer.jar not found. Maybe the Forges servers are having trouble.\";\n" +
                            "    echo \"Please try again in a couple of minutes.\";\n" +
                            "  fi\n" +
                            "else\n" +
                            "  echo \"Forge server present. Moving on...\"\n" +
                            "fi\n" +
                            "\n" +
                            "if [[ ! -s \"libraries/net/minecraft/server/$MINECRAFT/server-$MINECRAFT.jar\" ]];then\n" +
                            "  echo \"Minecraft Server JAR-file not found. Downloading...\";\n" +
                            "  wget -O libraries/net/minecraft/server/$MINECRAFT/server-$MINECRAFT.jar " + getMinecraftServerJarUrl(minecraftVersion) + ";\n" +
                            "else\n" +
                            "  echo \"Minecraft server present. Moving on...\"\n" +
                            "fi\n" +
                            "\n" +
                            "if [[ ! -s \"eula.txt\" ]];then\n" +
                            "  echo \"eula.txt not found. Creating...\";\n" +
                            "  echo \"#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\" > eula.txt;\n" +
                            "  echo \"eula=true\" >> eula.txt;\n" +
                            "else\n" +
                            "  echo \"eula.txt present. Moving on...\";\n" +
                            "fi\n" +
                            "\n" +
                            "if [[ -s \"run.bat\" ]];then\n" +
                            "  rm -f run.bat;\n" +
                            "  echo \"Deleted run.bat as we already have start.bat\";\n" +
                            "fi\n" +
                            "if [[ -s \"run.sh\" ]];then\n" +
                            "  rm -f run.sh;\n" +
                            "  echo \"Deleted run.sh as we already have start.sh\";\n" +
                            "fi\n" +
                            "\n" +
                            "echo \"Starting server...\";\n" +
                            "echo \"Minecraft version: $MINECRAFT\";\n" +
                            "echo \"Forge version: $FORGE\";\n" +
                            "echo \"Java args in user_jvm_args.txt: $ARGS\";\n" +
                            "\n" +
                            "# Forge requires a configured set of both JVM and program arguments.\n" +
                            "# Add custom JVM arguments to the user_jvm_args.txt\n" +
                            "# Add custom program arguments {such as nogui} to this file in the next line before the \"$@\" or\n" +
                            "#  pass them to this script directly\n" +
                            "\n" +
                            "java @user_jvm_args.txt @libraries/net/minecraftforge/forge/$MINECRAFT-$FORGE/unix_args.txt nogui \"$@\""
                    );

                } catch (IOException ex) {
                    LOG.error("Error generating shell-script for Forge.", ex);
                }

                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(
                                String.valueOf(
                                        Paths.get(
                                                String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getForgeOneSevenUserJvmArgsFile())
                                        )
                                )
                        )
                )) {

                    // User Java Args file for Minecraft 1.17+ Forge servers
                    writer.write(
                                "# Xmx and Xms set the maximum and minimum RAM usage, respectively.\n" +
                                    "# They can take any number, followed by an M or a G.\n" +
                                    "# M means Megabyte, G means Gigabyte.\n" +
                                    "# For example, to set the maximum to 3GB: -Xmx3G\n" +
                                    "# To set the minimum to 2.5GB: -Xms2500M\n" +
                                    "\n" +
                                    "# A good default for a modded server is 4GB.\n" +
                                    "# Uncomment the next line to set it.\n" +
                                    "# -Xmx4G\n" +
                                        javaArguments
                    );

                } catch (IOException ex) {
                    LOG.error("Error generating user_jvm_args.txt for Forge.", ex);
                }
            }

        } else if (modLoader.equalsIgnoreCase("Fabric")) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copystartscripts.fabric"));

            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            String.valueOf(
                                    Paths.get(
                                            String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getWindowsFile())
                                    )
                            )
                    )
            )) {

                // Fabric Batch file
                writer.write(
                        ":: Start script generated by ServerPackCreator.\n" +
                        ":: This script checks for the Minecraft and Fabric JAR-files, and if they are not found, they are downloaded and installed.\n" +
                        ":: If everything is in order, the server is started.\n" +
                        "@ECHO off\n" +
                        "\n" +
                        "SET MINECRAFT=\"" + minecraftVersion + "\"\n" +
                        "SET FABRIC=\"" + modloaderVersion + "\"\n" +
                        "SET INSTALLER=\"" + VERSIONLISTER.getFabricReleaseInstallerVersion() + "\"\n" +
                        "SET ARGS=" + javaArguments + "\n" +
                        "\n" +
                        "IF NOT EXIST fabric-server-launch.jar (\n" +
                        "\n" +
                        "  ECHO Fabric Server JAR-file not found. Downloading installer...\n" +
                        "  powershell -Command \"(New-Object Net.WebClient).DownloadFile('https://maven.fabricmc.net/net/fabricmc/fabric-installer/%INSTALLER%/fabric-installer-%INSTALLER%.jar', 'fabric-installer.jar')\"\n" +
                        "\n" +
                        "  IF EXIST fabric-installer.jar (\n" +
                        "\n" +
                        "    ECHO Installer downloaded. Installing...\n" +
                        "    java -jar fabric-installer.jar server -mcversion %MINECRAFT% -loader %FABRIC% -downloadMinecraft\n" +
                        "\n" +
                        "    IF EXIST fabric-server-launch.jar (\n" +
                        "      RMDIR /S /Q .fabric-installer\n" +
                        "      DEL fabric-installer.jar\n" +
                        "      ECHO Installation complete. fabric-installer.jar and installation files deleted.\n" +
                        "    )\n" +
                        "\n" +
                        "  ) ELSE (\n" +
                        "    ECHO fabric-installer.jar not found. Maybe the Fabric servers are having trouble.\n" +
                        "    ECHO Please try again in a couple of minutes.\n" +
                        "  )\n" +
                        ") ELSE (\n" +
                        "  ECHO fabric-server-launch.jar present. Moving on...\n" +
                        ")\n" +
                        "\n" +
                        "IF NOT EXIST server.jar (\n" +
                        "  ECHO Minecraft Server JAR-file not found. Downloading...\n" +
                        "  powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + getMinecraftServerJarUrl(minecraftVersion) + "', 'server.jar')\"\n" +
                        ") ELSE (\n" +
                        "  ECHO server.jar present. Moving on...\n" +
                        ")\n" +
                        "\n" +
                        "IF NOT EXIST eula.txt (\n" +
                        "  ECHO eula.txt not found. Creating...\n" +
                        "  ECHO #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://account.mojang.com/documents/minecraft_eula^).> eula.txt\n" +
                        "  ECHO eula=true>> eula.txt\n" +
                        ") ELSE (\n" +
                        "  ECHO eula.txt present. Moving on...\n" +
                        ")\n" +
                        "\n" +
                        "ECHO Starting server...\n" +
                        "ECHO Minecraft version: %MINECRAFT%\n" +
                        "ECHO Fabric version: %FABRIC%\n" +
                        "ECHO Java args: %ARGS%\n" +
                        "\n" +
                        "java %ARGS% -jar fabric-server-launch.jar --nogui\n" +
                        "\n" +
                        "PAUSE"
                );

            } catch (IOException ex) {
                LOG.error("Error generating batch-script for Forge.", ex);
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            String.valueOf(
                                    Paths.get(
                                            String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getLinuxFile())
                                    )
                            )
                    )
            )) {

                // Fabric Bash file
                writer.write(
                        "#!/usr/bin/env bash\n" +
                        "# Start script generated by ServerPackCreator.\n" +
                        "# This script checks for the Minecraft and Forge JAR-Files, and if they are not found, they are downloaded and installed.\n" +
                        "# If everything is in order, the server is started.\n" +
                        "\n" +
                        "MINECRAFT=\"" + minecraftVersion + "\"\n" +
                        "FABRIC=\"" + modloaderVersion + "\"\n" +
                        "INSTALLER=\"" + VERSIONLISTER.getFabricReleaseInstallerVersion() + "\"\n" +
                        "ARGS=\"" + javaArguments + "\"\n" +
                        "\n" +
                        "if [[ ! -s \"fabric-server-launch.jar\" ]];then\n" +
                        "\n" +
                        "  echo \"Fabric Server JAR-file not found. Downloading installer...\";\n" +
                        "  wget -O fabric-installer.jar https://maven.fabricmc.net/net/fabricmc/fabric-installer/$INSTALLER/fabric-installer-$INSTALLER.jar;\n" +
                        "\n" +
                        "  if [[ -s \"fabric-installer.jar\" ]];then\n" +
                        "\n" +
                        "    echo \"Installer downloaded. Installing...\";\n" +
                        "    java -jar fabric-installer.jar server -mcversion $MINECRAFT -loader $FABRIC -downloadMinecraft;\n" +
                        "\n" +
                        "    if [[ -s \"fabric-server-launch.jar\" ]];then\n" +
                        "      rm -rf .fabric-installer;\n" +
                        "      rm -f fabric-installer.jar;\n" +
                        "      echo \"Installation complete. fabric-installer.jar deleted.\";\n" +
                        "    fi\n" +
                        "\n" +
                        "  else\n" +
                        "    echo \"fabric-installer.jar not found. Maybe the Fabric server are having trouble.\";\n" +
                        "    echo \"Please try again in a couple of minutes.\";\n" +
                        "  fi\n" +
                        "else\n" +
                        "  echo \"fabric-server-launch.jar present. Moving on...\";\n" +
                        "fi\n" +
                        "\n" +
                        "if [[ ! -s \"server.jar\" ]];then\n" +
                        "  echo \"Minecraft Server JAR-file not found. Downloading...\";\n" +
                        "  wget -O server.jar " + getMinecraftServerJarUrl(minecraftVersion) + ";\n" +
                        "else\n" +
                        "  echo \"server.jar present. Moving on...\";\n" +
                        "fi\n" +
                        "\n" +
                        "if [[ ! -s \"eula.txt\" ]];then\n" +
                        "  echo \"eula.txt not found. Creating...\";\n" +
                        "  echo \"#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\" > eula.txt;\n" +
                        "  echo \"eula=true\" >> eula.txt;\n" +
                        "else\n" +
                        "  echo \"eula.txt present. Moving on...\";\n" +
                        "fi\n" +
                        "\n" +
                        "echo \"Starting server...\";\n" +
                        "echo \"Minecraft version: $MINECRAFT\";\n" +
                        "echo \"Fabric version: $FABRIC\";\n" +
                        "echo \"Java args: $ARGS\";\n" +
                        "\n" +
                        "java $ARGS -jar fabric-server-launch.jar --nogui"
                );

            } catch (IOException ex) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error("Error generating shell-script for Forge.", ex);
            }

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"));
        }
    }

    /**
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack directory into the
     * server pack directory.
     * If a <code>source/file;destination/file</code>-combination is provided, the specified source-file is copied to
     * the specified destination-file.
     * Calls {@link #excludeClientMods(String, List, String)} to generate a list of all mods to copy to server pack, excluding
     * clientside-only mods.
     * @author Griefed
     * @param modpackDir String. Files and directories are copied into the server_pack directory inside the modpack directory.
     * @param directoriesToCopy String List. All directories and files therein to copy to the server pack.
     * @param clientMods String List. List of clientside-only mods to exclude from the server pack.
     * @param minecraftVersion String. The Minecraft version the modpack uses.
     */
    void copyFiles(String modpackDir, List<String> directoriesToCopy, List<String> clientMods, String minecraftVersion) {

        String serverPath = String.format("%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination());

        try {

            Files.createDirectories(Paths.get(serverPath));

        } catch (IOException ex) {

            LOG.error(String.format("Failed to create directory %s", serverPath));
        }

        directoriesToCopy.removeIf(n -> n.startsWith("!"));

        for (String directory : directoriesToCopy) {

            String clientDir = String.format("%s/%s", modpackDir, directory);
            String serverDir = String.format("%s/%s", serverPath, directory);

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyfiles.setup"), directory));

            /*
             * Check for semicolon. If a semicolon is found, it means a user specified a source/path/to_file.foo;destination/path/to_file.bar-combination
             * for a file they specifically want to include in their server pack.
             */
            if (directory.contains(";")) {

                String[] sourceFileDestinationFileCombination = directory.split(";");

                File sourceFile = new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));
                File destinationFile = new File(String.format("%s/%s", serverPath, sourceFileDestinationFileCombination[1]));

                try {
                    FileUtils.copyFile(sourceFile, destinationFile, REPLACE_EXISTING);
                } catch (IOException ex) {
                    LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                }

            /*
             * Check whether the entry starts with saves, and if it does, change the destination path to NOT include
             * saves in it, so when a world is specified inside the saves-directory, it is copied to the base-directory
             * of the server pack, instead of a saves-directory inside the modpack.
             */
            } else if (directory.startsWith("saves/")) {

                String savesDir = String.format("%s/%s", serverPath, directory.substring(6));
                try {

                    Stream<Path> files = Files.walk(Paths.get(clientDir));

                    files.forEach(file -> {
                        try {

                            Files.copy(
                                    file,
                                    Paths.get(savesDir).resolve(Paths.get(clientDir).relativize(file)),
                                    REPLACE_EXISTING
                            );

                            LOG.debug(String.format("Copying: %s", file.toAbsolutePath()));

                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                LOG.error("An error occurred during copy operation.", ex);
                            }
                        }
                    });

                } catch (IOException ex) {
                    LOG.error("An error occurred copying the specified world.", ex);
                }

            /*
             * If the entry starts with mods, we need to run our checks for clientside-only mods as well as exclude any
             * user-specified clientside-only mods from the list of mods in the mods-directory.
             */
            } else if (directory.startsWith("mods") && clientMods.size() > 0) {

                List<String> listOfFiles = excludeClientMods(clientDir, clientMods, minecraftVersion);

                try {
                    Files.createDirectories(Paths.get(serverDir));
                } catch (IOException ex) {
                    LOG.info(String.format("Setting up %s file(s).", serverDir));
                }

                for (String file : listOfFiles) {
                    try {

                        Files.copy(
                                Paths.get(file),
                                Paths.get(String.format("%s/%s", serverDir, new File(file).getName())),
                                REPLACE_EXISTING
                        );

                        LOG.debug(String.format("Copying: %s", file));

                    } catch (IOException ex) {
                        if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                            LOG.error("An error occurred copying files to the serverpack.", ex);
                        }
                    }
                }

            } else if (new File(directory).isFile() && !new File(directory).isDirectory()) {

                File sourceFile = new File(String.format("%s/%s", modpackDir, directory));
                File destinationFile = new File(String.format("%s/%s", serverPath, directory));

                try {
                    FileUtils.copyFile(sourceFile, destinationFile, REPLACE_EXISTING);
                } catch (IOException ex) {
                    LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                }

            } else {

                try {

                    Stream<Path> files = Files.walk(Paths.get(clientDir));

                    files.forEach(file -> {
                        if (excludeFileOrDirectory(file.toString())) {

                            LOG.debug("Excluding " + file + " from server pack");

                        } else {
                            try {

                                Files.copy(
                                        file,
                                        Paths.get(serverDir).resolve(Paths.get(clientDir).relativize(file)),
                                        REPLACE_EXISTING
                                );

                                LOG.debug(String.format("Copying: %s", file.toAbsolutePath()));
                            } catch (IOException ex) {
                                if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                    LOG.error("An error occurred copying files to the serverpack.", ex);
                                }
                            }
                        }
                    });

                    files.close();

                } catch (IOException ex) {
                    LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                }
            }

        }
    }

    /**
     * Generates a list of all mods to include in the server pack excluding clientside-only mods. Automatically detects
     * clientside-only mods for Minecraft 1.13+, if the mods <code>mods.toml</code>-file is correctly set up.
     * @author Griefed
     * @param modsDir String. The mods-directory of the modpack of which to generate a list of all it's contents.
     * @param userSpecifiedClientMods List String. A list of all clientside-only mods.
     * @param minecraftVersion String. The Minecraft version the modpack uses. Determines whether mods are scanned for sideness.
     * @return List String. A list of all mods to include in the server pack.
     */
    List<String> excludeClientMods(String modsDir, List<String> userSpecifiedClientMods, String minecraftVersion) {
        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.excludeclientmods"));

        File[] filesInModsDir = new File(modsDir).listFiles();
        assert filesInModsDir != null;

        List<String> modsInModpack = new ArrayList<>();
        List<String> autodiscoveredClientMods = new ArrayList<>();

        // Check whether scanning mods for sideness is activated.
        if (serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.serverpack.autodiscoverenabled").equals("true")) {

            String[] split = minecraftVersion.split("\\.");

            // If Minecraft version is 1.12 or newer, scan Tomls, else scan annotations.
            if (Integer.parseInt(split[1]) > 12) {
                autodiscoveredClientMods.addAll(scanTomls(filesInModsDir));
            } else {
                autodiscoveredClientMods.addAll(scanAnnotations(filesInModsDir));
            }

        }

        // Gather a list of all mod-JAR-files.
        try {
            for (File mod : filesInModsDir) {
                if (mod.isFile() && mod.toString().endsWith("jar")) {
                    modsInModpack.add(mod.getAbsolutePath());
                }
            }
        } catch (NullPointerException ex) {
            LOG.error("Error: There was an error during the acquisition of files in mods directory.", ex);
        }

        // Exclude user-specified mods from copying.
        if (!userSpecifiedClientMods.get(0).equals("")) {
            for (int m = 0; m < userSpecifiedClientMods.size(); m++) {

                int i = m;

                if (modsInModpack.removeIf(n -> (n.contains(userSpecifiedClientMods.get(i))))) {
                    LOG.debug("Removed user-specified mod from mods list as per input: " + userSpecifiedClientMods.get(i));
                }

            }
        }

        // Exclude scanned mods from copying if said functionality is enabled.
        if (serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.serverpack.autodiscoverenabled").equals("true") && autodiscoveredClientMods.size() > 0) {
            for (int m = 0; m < autodiscoveredClientMods.size(); m++) {

                int i = m;

                if (modsInModpack.removeIf(n -> (n.replace("\\", "/").contains(autodiscoveredClientMods.get(i))))) {
                    LOG.debug("Automatically excluding mod: " + autodiscoveredClientMods.get(i));
                }
            }
        }

        return modsInModpack;
    }

    /**
     * Check whether the given file is present in the list of directories to exclude from the server pack.
     * @author Griefed
     * @param fileToCheckFor String. The string to check for.
     * @return Boolean. Returns true if the file is found in the list of directories to exclude, false if not.
     */
    private boolean excludeFileOrDirectory(String fileToCheckFor) {
        boolean isPresentInList = false;
        for (String entry : CONFIGURATIONHANDLER.getDIRECTORIESTOEXCLUDELIST()) {
            if (fileToCheckFor.contains(entry)) {
                isPresentInList = true;
                break;
            }
        }
        return isPresentInList;
    }

    /**
     * Copies the server-icon.png into server_pack.
     * @author Griefed
     */
    void copyIcon() {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyicon"));

        try {

            Files.copy(
                    Paths.get(String.format("server_files/%s", getIconFile())),
                    Paths.get(String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getIconFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            LOG.error("An error occurred trying to copy the server-icon.", ex);
        }
    }

    /**
     * Copies the server.properties into server_pack.
     * @author Griefed
     */
    void copyProperties() {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyproperties"));

        try {

            Files.copy(
                    Paths.get(String.format("server_files/%s", getPropertiesFile())),
                    Paths.get(String.format("%s/%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination(), getPropertiesFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            LOG.error("An error occurred trying to copy the server.properties-file.", ex);
        }
    }

    /**
     * Create the <code>eula.txt</code> containing <code>eula=true</code> so the generated server pack can be used immediately.
     * @author Griefed
     */
    private void createEula() {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            String.valueOf(
                                    Paths.get(
                                            String.format("%s/%s/eula.txt", getSERVER_PACKS_DIR(), getServerPackDestination())
                                    )
                            )
                    )
            );

            String EULA = "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\neula=true";
            writer.write(EULA);
            writer.close();

        } catch (IOException ex) {
            LOG.error("Error writing eula.txt.", ex);
        }
    }

    /**
     * Installs the modloader server for the specified modloader, modloader version and Minecraft version.
     * Calls<p>
     * {@link #downloadFabricJar()} to download the Fabric installer into the server_pack directory.<p>
     * {@link #downloadForgeJar(String, String)} to download the Forge installer for the specified Forge version
     * and Minecraft version.<p>
     * {@link #cleanUpServerPack(File, File, String, String, String)} to delete no longer needed files generated
     * by the installation process of the modloader server software.
     * @author Griefed
     * @param modLoader String. The modloader for which to install the server software. Either Forge or Fabric.
     * @param minecraftVersion String. The Minecraft version for which to install the modloader and Minecraft server.
     * @param modLoaderVersion String. The modloader version for which to install the modloader and Minecraft server.
     * @param javaPath String. The path to the Java executable/binary which is needed to execute the Forge/Fabric installers.
     */
    void installServer(String modLoader, String minecraftVersion, String modLoaderVersion, String javaPath) {

        File fabricInstaller = new File(String.format("%s/%s/fabric-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination()));

        File forgeInstaller = new File(String.format("%s/%s/forge-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination()));

        List<String> commandArguments = new ArrayList<>();

        Process process = null;
        BufferedReader reader = null;

        if (modLoader.equalsIgnoreCase("Fabric")) {
            try {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.enter"));
                LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.enter"));

                if (downloadFabricJar()) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.download"));

                    commandArguments.add(javaPath);
                    commandArguments.add("-jar");
                    commandArguments.add("fabric-installer.jar");
                    commandArguments.add("server");
                    commandArguments.add("-mcversion");
                    commandArguments.add(minecraftVersion);
                    commandArguments.add("-loader");
                    commandArguments.add(modLoaderVersion);
                    commandArguments.add("-downloadMinecraft");

                    ProcessBuilder processBuilder = new ProcessBuilder(commandArguments).directory(new File(String.format("%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination())));

                    LOG.debug("ProcessBuilder command: " + processBuilder.command());

                    processBuilder.redirectErrorStream(true);
                    process = processBuilder.start();

                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;

                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        LOG_INSTALLER.info(line);
                    }

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.details"));
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                } else {

                    LOG.error("Something went wrong during the installation of Fabric. Maybe the Fabric server are down or unreachable? Skipping...");
                }

            } catch (IOException ex) {

                LOG.error("An error occurred during Fabric installation.", ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
                if (process != null) {
                    process.destroy();
                }
            }
        } else if (modLoader.equalsIgnoreCase("Forge")) {

            try {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.enter"));
                LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.enter"));

                if (downloadForgeJar(minecraftVersion, modLoaderVersion)) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.download"));
                    commandArguments.add(javaPath);
                    commandArguments.add("-jar");
                    commandArguments.add("forge-installer.jar");
                    commandArguments.add("--installServer");

                    ProcessBuilder processBuilder = new ProcessBuilder(commandArguments).directory(new File(String.format("%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination())));

                    LOG.debug("ProcessBuilder command: " + processBuilder.command());

                    processBuilder.redirectErrorStream(true);
                    process = processBuilder.start();

                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;

                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        LOG_INSTALLER.info(line);
                    }

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.details"));
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                } else {

                    LOG.error("Something went wrong during the installation of Forge. Maybe the Forge servers are down or unreachable? Skipping...");
                }
            } catch (IOException ex) {

                LOG.error("An error occurred during Forge installation.", ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
                if (process != null) {
                    process.destroy();
                }
            }
        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"), modLoader));
        }

        commandArguments.clear();

        if (serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.serverpack.cleanup.enabled").equalsIgnoreCase("true")) {
            cleanUpServerPack(
                    fabricInstaller,
                    forgeInstaller,
                    modLoader,
                    minecraftVersion,
                    modLoaderVersion);
        } else {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanup"));
        }
    }

    /**
     * Creates a ZIP-archive of the server_pack directory excluding the Minecraft server JAR.<p>
     * @author Griefed
     * @param minecraftVersion String. Determines the name of the Minecraft server JAR to exclude from the ZIP-archive if the modloader is Forge.
     * @param includeServerInstallation Boolean. Determines whether the Minecraft server JAR info should be printed.
     */
    void zipBuilder(String minecraftVersion, boolean includeServerInstallation) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.zipbuilder.enter"));

        List<File> filesToExclude = new ArrayList<>(Arrays.asList(
                new File(String.format("%s/%s/minecraft_server.%s.jar", getSERVER_PACKS_DIR(), getServerPackDestination(), minecraftVersion)),
                new File(String.format("%s/%s/server.jar", getSERVER_PACKS_DIR(), getServerPackDestination())),
                new File(String.format("%s/%s/libraries/net/minecraft/server/%s/server-%s.jar", getSERVER_PACKS_DIR(), getServerPackDestination(), minecraftVersion, minecraftVersion))
        ));

        ExcludeFileFilter excludeFileFilter = filesToExclude::contains;

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setExcludeFileFilter(excludeFileFilter);
        zipParameters.setIncludeRootFolder(false);

        zipParameters.setFileComment("Server pack made with ServerPackCreator by Griefed.");

        try {

            new ZipFile(String.format("%s/%s_server_pack.zip", getSERVER_PACKS_DIR(), getServerPackDestination())).addFolder(new File(String.format("%s/%s", getSERVER_PACKS_DIR(), getServerPackDestination())), zipParameters);

        } catch (IOException ex) {

            LOG.error("There was an error during zip creation.", ex);
        }

        if (includeServerInstallation) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.warn.zipbuilder.minecraftjar1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.warn.zipbuilder.minecraftjar2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.warn.zipbuilder.minecraftjar3"));
        }

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.zipbuilder.finish"));
    }

    /**
     * Retrieves the URL to the Minecraft server for the specified Minecraft version.
     * @author Griefed
     * @param minecraftVersion String. The Minecraft version for which to retrieve the URL for the server-jar.
     * @return String. The URL to the server-jar of the specified Minecraft version as a string.
     */
    private String getMinecraftServerJarUrl(String minecraftVersion) {
        String minecraftVersionJarUrl = null;

        try {
            JsonNode minecraftJson = getObjectMapper().readTree(Files.readAllBytes(new File("./work/minecraft-manifest.json").toPath()));

            JsonNode versions = minecraftJson.get("versions");

            for (JsonNode version : versions) {
                try {
                    if (version.get("id").asText().equals(minecraftVersion)) {

                        try (InputStream inputStream = new URL(version.get("url").asText()).openStream()) {
                            JsonNode serverNode = getObjectMapper().readTree(inputStream);

                            minecraftVersionJarUrl = serverNode.get("downloads").get("server").get("url").asText();
                        }

                    }
                } catch (NullPointerException ignored) {}
            }

        } catch (IOException ex) {
            LOG.error("Couldn't read Minecraft manifest.", ex);
        }

        return minecraftVersionJarUrl;
    }

    /**
     * Downloads the release Fabric installer into the server pack.<p>
     * @author Griefed
     * @return Boolean. Returns true if the download was successfull.
     */
    boolean downloadFabricJar() {

        boolean downloaded = false;

        try {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.downloadfabricjar.enter"));

            URL downloadFabric = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", VERSIONLISTER.getFabricReleaseInstallerVersion(), VERSIONLISTER.getFabricReleaseInstallerVersion()));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadFabric.openStream());

            FileOutputStream downloadFabricFileOutputStream = new FileOutputStream(String.format("%s/%s/fabric-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination()));
            FileChannel downloadFabricFileChannel = downloadFabricFileOutputStream.getChannel();
            downloadFabricFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadFabricFileOutputStream.flush();
            downloadFabricFileOutputStream.close();
            readableByteChannel.close();
            downloadFabricFileChannel.close();

        } catch (IOException ex) {
            LOG.error("An error occurred downloading Fabric.", ex);

            if (new File(String.format("%s/%s/fabric-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination())).exists()) {
                try {
                    Files.delete(Paths.get(String.format("%s/%s/fabric-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination())));

                } catch (IOException exc) {
                    LOG.error("Couldn't delete corrupted Fabric installer.", exc);
                }
            }
        }

        if (new File(String.format("%s/%s/fabric-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination())).exists()) {
            downloaded = true;
        }
        return downloaded;
    }

    /**
     * Downloads the modloader server installer for Forge, for the specified modloader version.
     * @author Griefed
     * @param minecraftVersion String. The Minecraft version for which to download the modloader server installer.
     * @param modLoaderVersion String. The Forge version for which to download the modloader server installer.
     * @return Boolean. Returns true if the download was successful.
     */
    boolean downloadForgeJar(String minecraftVersion, String modLoaderVersion) {

        boolean downloaded = false;

        try {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.downloadforgejar.enter"));
            URL downloadForge = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar", minecraftVersion, modLoaderVersion, minecraftVersion, modLoaderVersion));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadForge.openStream());

            FileOutputStream downloadForgeFileOutputStream = new FileOutputStream(String.format("%s/%s/forge-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination()));
            FileChannel downloadForgeFileChannel = downloadForgeFileOutputStream.getChannel();
            downloadForgeFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadForgeFileOutputStream.flush();
            downloadForgeFileOutputStream.close();
            readableByteChannel.close();
            downloadForgeFileChannel.close();

        } catch (IOException ex) {
            LOG.error("An error occurred downloading Forge.", ex);

            if (new File(String.format("%s/%s/forge-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination())).exists()) {

                if (new File(String.format("%s/%s/forge-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination())).delete()) {
                    LOG.error("Deleted incomplete Forge-installer...");
                }
            }
        }

        if (new File(String.format("%s/%s/forge-installer.jar", getSERVER_PACKS_DIR(), getServerPackDestination())).exists()) {
            downloaded = true;
        }
        return downloaded;
    }

    /**
     * Cleans up the server_pack directory by deleting left-over files from modloader installations and version checking.
     * @author Griefed
     * @param fabricInstaller File. The Fabric installer file which is to be deleted.
     * @param forgeInstaller File. The Forge installer file which is to be deleted.
     * @param modLoader String. Whether Forge or Fabric files are to be deleted.
     * @param minecraftVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     * @param modLoaderVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     */
    void cleanUpServerPack(File fabricInstaller, File forgeInstaller, String modLoader, String minecraftVersion, String modLoaderVersion) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.enter"));

        if (modLoader.equalsIgnoreCase("Fabric")) {

            boolean isInstallerDeleted = fabricInstaller.delete();

            if (isInstallerDeleted) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.deleted"), fabricInstaller.getName()));
            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupserverpack.delete"), fabricInstaller.getName()));
            }

        } else if (modLoader.equalsIgnoreCase("Forge")) {

            String[] minecraft = minecraftVersion.split("\\.");

            boolean isInstallerDeleted = forgeInstaller.delete();

            boolean isInstallerLogDeleted = new File(String.format("%s/%s/installer.log", getSERVER_PACKS_DIR(), getServerPackDestination())).delete();

            if (isInstallerDeleted) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.deleted"), forgeInstaller.getName()));
            } else {
                LOG.error(String.format("Could not delete %s.", forgeInstaller.getName()));
            }

            if (isInstallerLogDeleted) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.forgelog"));
            } else {
                LOG.error("Error deleting Forge installer log.");
            }

            if (Integer.parseInt(minecraft[1]) < 17) {

                try {

                    Files.copy(
                            Paths.get(String.format("%s/%s/forge-%s-%s.jar", getSERVER_PACKS_DIR(), getServerPackDestination(), minecraftVersion, modLoaderVersion)),
                            Paths.get(String.format("%s/%s/forge.jar", getSERVER_PACKS_DIR(), getServerPackDestination())),
                            REPLACE_EXISTING);

                    boolean isOldJarDeleted = (new File(
                            String.format("%s/%s/forge-%s-%s.jar",
                                    getSERVER_PACKS_DIR(),
                                    getServerPackDestination(),
                                    minecraftVersion,
                                    modLoaderVersion))).delete();


                    if ((isOldJarDeleted) && (new File(String.format("%s/%s/forge.jar", getSERVER_PACKS_DIR(), getServerPackDestination())).exists())) {
                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.rename"));
                    } else {
                        LOG.error("There was an error during renaming or deletion of the forge server jar.");
                    }

                } catch (IOException ex) {
                    LOG.error("Error during Forge cleanup.", ex);
                }

            } else {

                boolean deleteRunBat = new File(String.format("%s/%s/%s",getSERVER_PACKS_DIR(),getServerPackDestination(),"run.bat")).delete();
                boolean deleteRunSh = new File(String.format("%s/%s/%s",getSERVER_PACKS_DIR(),getServerPackDestination(),"run.sh")).delete();

                // TODO: Replace with lang keys
                if (deleteRunBat) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info("Deleted run.bat.");
                } else {
                    LOG.error(String.format("Could not delete %s.", "run.bat"));
                }

                if (deleteRunSh) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info("Deleted run.sh.");
                } else {
                    LOG.error(String.format("Could not delete %s.", "run.sh"));
                }

            }

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"), modLoader));
        }
    }

    /**
     * Scan the <code>mods.toml</code>-files in mod JAR-files of a given directory for their sideness.<br>
     * If <code>[[mods]]</code> specifies <code>side=BOTH|SERVER</code>, it is added.<br>
     * If <code>[[dependencies.modId]]</code> for Forge|Minecraft specifies <code>side=BOTH|SERVER</code>, it is added.<br>
     * Any modId of a dependency specifying <code>side=BOTH|SERVER</code> is added.<br>
     * If no sideness can be found for a given mod, it is added to prevent false positives.
     * @author Griefed
     * @param filesInModsDir A list of in which to check the <code>mods.toml</code>-files.
     * @return List String. List of mods not to include in server pack based on mods.toml-configuration.
     */
    private List<String> scanTomls(File[] filesInModsDir) {

        /*
         * Can I just say:
         * WHAT THE EVERLOVING FUCK IS THIS METHOD? try catch if try catch if else try catch what the actual fucking fuck?
         */

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.scantoml"));

        List<String> serverMods = new ArrayList<>();
        List<String> modsDelta = new ArrayList<>();

        for (File mod : filesInModsDir) {
            if (mod.toString().endsWith("jar")) {

                try {

                    String modToCheck = mod.toString().replace("\\", "/");
                    URL urlToToml = new URL(String.format("jar:file:%s!/META-INF/mods.toml", modToCheck));
                    InputStream inputStream = urlToToml.openStream();
                    Toml modToml = new Toml().read(inputStream);
                    String modId = modToml.getString("mods[0].modId");

                    // Check whether the sideness is specified in [[mods]]
                    try {
                        if (modToml.getString("mods[0].side").toLowerCase().matches("(server|both)")) {

                            if (!serverMods.contains(modId)) {

                                LOG.debug("Adding modId to list of server mods: " + modId);
                                serverMods.add(modId);
                            }

                        }
                    } catch (NullPointerException ignored) {
                    }

                    try {
                        if (modToml.getList("dependencies." + modId) != null) {
                            for (int i = 0; i < modToml.getList("dependencies." + modId).size(); i++) {

                                // If sideness in FORGE|MINECRAFT is BOTH|SERVER, add the mod to the list.
                                try {
                                    if (modToml.getString("dependencies." + modId + "[" + i + "].modId").toLowerCase().matches("(forge|minecraft)")) {

                                        /*
                                         * If Forge|Minecraft are listed as dependencies, but no sideness is specified, we need to add this mod just in case,
                                         * so we do not exclude any mods which MAY need to be on the server.
                                         */
                                        if (modToml.getString("dependencies." + modId + "[" + i + "].side") == null) {

                                            if (!serverMods.contains(modId)) {

                                                LOG.debug("Adding modId to list of server mods: " + modId);
                                                serverMods.add(modId);
                                            }
                                        }

                                        /*
                                         * If Forge|Minecraft sideness is BOTH|SERVER, add the mod to the list.
                                         */
                                        if (modToml.getString("dependencies." + modId + "[" + i + "].side").toLowerCase().matches("(server|both)")) {

                                            if (!serverMods.contains(modId)) {

                                                LOG.debug("Adding modId to list of server mods: " + modId);
                                                serverMods.add(modId);
                                            }
                                        }

                                    } else {
                                        /*
                                         * I know this looks stupid. If a mod does not specify BOTH|SERVER in either of the Forge or Minecraft dependencies,
                                         * and NO sideness was detected in [[mods]], we need to add this mod just in case, so we do not exclude any mods
                                         * which MAY need to be on the server.
                                         */
                                        if (!serverMods.contains(modId) && modToml.getString("mods[0].side") == null) {

                                            LOG.debug("Adding modId to list of server mods: " + modId);
                                            serverMods.add(modId);
                                        }
                                    }

                                } catch (NullPointerException ignored) {
                                }

                                /*
                                 * Add every dependency of the mod we are currently checking, which specifies BOTH|SERVER as their sideness, to the list.
                                 */
                                try {

                                    if (!modToml.getString("dependencies." + modId + "[" + i + "].modId").toLowerCase().matches("(forge|minecraft)")) {
                                        if (modToml.getString("dependencies." + modId + "[" + i + "].side").toLowerCase().matches("(server|both)")) {

                                            if (!serverMods.contains(modToml.getString("dependencies." + modId + "[" + i + "].modId"))) {

                                                LOG.debug("Adding modId to list of server mods: " + modToml.getString("dependencies." + modId + "[" + i + "].modId"));
                                                serverMods.add(modToml.getString("dependencies." + modId + "[" + i + "].modId"));
                                            }
                                        }
                                    }

                                } catch (NullPointerException ignored) {
                                }

                            }
                        }
                    } catch (NullPointerException | ClassCastException ex) {
                        try {

                            for (int i = 0; i < modToml.getList("dependencies").size(); i++) {

                                LOG.warn(modId + " does not contain valid dependency definitions. Please contact the mod maker and ask them to fix their shit. :D");

                                String subDependencyId = null;
                                String subDependencySide = null;

                                for (String element : modToml.getList("dependencies").get(i).toString().replace("{", "").replace("}", "").split(",")) {
                                    // If sideness in FORGE|MINECRAFT is BOTH|SERVER, add the mod to the list.
                                    if (element.contains("modId")) {
                                        subDependencyId = element.substring(element.lastIndexOf("=") + 1);
                                    } else if (element.contains("side")) {
                                        subDependencySide = element.substring(element.lastIndexOf("=") + 1);
                                    }
                                }

                                if (subDependencyId != null && subDependencySide != null) {

                                    if (!subDependencyId.equalsIgnoreCase("minecraft") && !subDependencyId.equalsIgnoreCase("forge")) {

                                        if (subDependencySide.equalsIgnoreCase("both") || subDependencySide.equalsIgnoreCase("server")) {
                                            if (!serverMods.contains(subDependencyId)) {

                                                LOG.debug("Adding modId to list of server mods: " + subDependencyId);
                                                serverMods.add(subDependencyId);
                                            }
                                        }

                                    } else {

                                        if (subDependencySide.equalsIgnoreCase("both") || subDependencySide.equalsIgnoreCase("server")) {
                                            if (!serverMods.contains(modId)) {

                                                LOG.debug("Adding modId to list of server mods: " + modId);
                                                serverMods.add(modId);
                                            }
                                        }
                                    }
                                }
                            }

                        } catch (ClassCastException | NullPointerException ignored) {
                        }
                    }

                    /*
                     * If sideness is neither specified in [[mods]] and no dependencies exist, we need to add this mod just in case, so we do
                     * not exclude any mods which MAY need to be on the server.
                     */
                    try {
                        if (modToml.getString("mods[0].side") == null) {

                            try {

                                if (modToml.getList("dependencies." + modId) == null) {

                                    try {

                                        if (modToml.getList("dependencies") == null) {

                                            if (!serverMods.contains(modId)) {

                                                LOG.debug("Adding modId to list of server mods: " + modId);
                                                serverMods.add(modId);
                                            }
                                        }

                                    } catch (ClassCastException ex) {

                                        if (!serverMods.contains(modId)) {

                                            LOG.debug("Adding modId to list of server mods: " + modId);
                                            serverMods.add(modId);
                                        }
                                    }

                                    if (!serverMods.contains(modId)) {

                                        LOG.debug("Adding modId to list of server mods: " + modId);
                                        serverMods.add(modId);
                                    }

                                }

                            } catch (NullPointerException ex) {

                                if (!serverMods.contains(modId)) {

                                    LOG.debug("Adding modId to list of server mods: " + modId);
                                    serverMods.add(modId);
                                }
                            }
                        }
                    } catch (ClassCastException | NullPointerException ignored) {
                    }

                    inputStream.close();

                } catch (IOException ignored) {
                }

            }
        }

        for (File mod : filesInModsDir) {
            try {

                String modToCheck = mod.toString().replace("\\", "/");
                URL urlToToml = new URL(String.format("jar:file:%s!/META-INF/mods.toml", modToCheck));
                InputStream inputStream = urlToToml.openStream();
                Toml modToml = new Toml().read(inputStream);
                boolean addToDelta = true;

                for (String modId : serverMods) {

                    if (modToml.getString("mods[0].modId").toLowerCase().matches(modId)) {
                        addToDelta = false;
                    }

                }

                if (addToDelta) {
                    modsDelta.add(modToCheck);
                }

                inputStream.close();
            } catch (IOException ignored) {
            }
        }

        return modsDelta;
    }

    /**
     * Scan the <code>fml-cache-annotation.json</code>-files in mod JAR-files of a given directory for their sideness.<br>
     * If <code>clientSideOnly</code> specifies <code>"value": "true"</code>, and is not listed as a dependency
     * for another mod, it is added and therefore later on excluded from the server pack.
     * @author Griefed
     * @param filesInModsDir A list of in which to check the <code>mods.toml</code>-files.
     * @return List String. List of mods not to include in server pack based on fml-cache-annotation.json-content.
     */
    private List<String> scanAnnotations(File[] filesInModsDir) {

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.scanannotation"));

        List<String> modDependencies = new ArrayList<>();
        List<String> clientMods = new ArrayList<>();
        List<String> modsDelta = new ArrayList<>();

        for (File mod : filesInModsDir) {
            if (mod.toString().endsWith("jar")) {

                try {
                    String modToCheck = mod.toString().replace("\\", "/");
                    URL urlToJson = new URL(String.format("jar:file:%s!/META-INF/fml_cache_annotation.json", modToCheck));
                    InputStream inputStream = urlToJson.openStream();
                    JsonNode modJson = getObjectMapper().readTree(inputStream);
                    String modId = null;

                    //base of json
                    for (JsonNode node : modJson) {

                        try {
                            // iterate though annotations
                            for (JsonNode child : node.get("annotations")) {

                                // Get the modId
                                try {
                                    if (!child.get("values").get("modid").get("value").asText().isEmpty()) {
                                        modId = child.get("values").get("modid").get("value").asText();
                                    }
                                } catch (NullPointerException ignored) {}

                                // Add mod to list of clientmods if clientSideOnly is true
                                try {
                                    if (child.get("values").get("clientSideOnly").get("value").asText().equalsIgnoreCase("true")) {
                                        if (!clientMods.contains(modId)) {
                                            clientMods.add(modId);

                                            LOG.debug("Added clientMod: " + modId);
                                        }
                                    }
                                } catch (NullPointerException ignored) {}

                                // Get dependency modIds
                                try {
                                    if (!child.get("values").get("dependencies").get("value").asText().isEmpty()) {

                                        if (child.get("values").get("dependencies").get("value").asText().contains(";")) {

                                            String [] dependencies = child.get("values").get("dependencies").get("value").asText().split(";");

                                            for (String dependency : dependencies) {

                                                if (dependency.matches("(before:.*|after:.*|)")) {

                                                    dependency = dependency.substring(dependency.lastIndexOf(":") + 1).replaceAll("(@.*|\\[.*)", "");

                                                    if (!modDependencies.contains(dependency) && !dependency.equalsIgnoreCase("forge") && !dependency.equals("*")) {
                                                        modDependencies.add(dependency);

                                                        LOG.debug("Added dependency " + dependency);
                                                    }

                                                }

                                            }
                                        } else {
                                            if (child.get("values").get("dependencies").get("value").asText().matches("(before:.*|after:.*|)")) {

                                                String dependency = child.get("values").get("dependencies").get("value").asText().substring(child.get("values").get("dependencies").get("value").asText().lastIndexOf(":") + 1).replaceAll("(@.*|\\[.*)", "");

                                                if (!modDependencies.contains(dependency) && !dependency.equalsIgnoreCase("forge") && !dependency.equals("*")) {
                                                    modDependencies.add(dependency);

                                                    LOG.debug("Added dependency " + dependency);
                                                }

                                            }
                                        }

                                    }
                                } catch (NullPointerException ignored) {}

                            }

                        } catch (NullPointerException ignored) {}

                    }

                    inputStream.close();
                } catch(IOException ignored) {}

            }

        }

        for (String dependency : modDependencies) {

            clientMods.removeIf(n -> (n.contains(dependency)));
            LOG.debug("Removing " + dependency + " from list of clientmods as it is a dependency for another mod.");
        }

        for (File mod : filesInModsDir) {
            try {

                String modToCheck = mod.toString().replace("\\", "/");
                URL urlToJson = new URL(String.format("jar:file:%s!/META-INF/fml_cache_annotation.json", modToCheck));
                InputStream inputStream = urlToJson.openStream();
                JsonNode modJson = getObjectMapper().readTree(inputStream);
                String modIdTocheck = null;

                boolean addToDelta = false;

                //base of json
                for (JsonNode node : modJson) {

                    try {
                        // iterate though annotations
                        for (JsonNode child : node.get("annotations")) {

                            // Get the modId
                            try {
                                if (!child.get("values").get("modid").get("value").asText().isEmpty()) {
                                    modIdTocheck = child.get("values").get("modid").get("value").asText();
                                }
                            } catch (NullPointerException ignored) {}

                            // Add mod to list of clientmods if clientSideOnly is true
                            try {
                                if (child.get("values").get("clientSideOnly").get("value").asText().equalsIgnoreCase("true")) {
                                    if (clientMods.contains(modIdTocheck)) {
                                        addToDelta = true;
                                    }
                                }
                            } catch (NullPointerException ignored) {}

                        }
                    } catch (NullPointerException ignored) {}

                }

                if (addToDelta) {
                    modsDelta.add(modToCheck);

                }

            } catch (IOException ignored) {
            }
        }

        return modsDelta;
    }
}