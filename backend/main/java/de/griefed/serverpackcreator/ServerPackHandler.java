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

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moandjiezana.toml.Toml;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.plugins.ApplicationPlugins;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.commonutilities.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Create a server pack from a modpack by copying all specified or required files from the modpack to the server pack
 * as well as installing the modloader server for the specified modloader, modloader version and Minecraft version.
 * Create a ZIP-archive of the server pack, excluding the Minecraft server JAR, for immediate upload to CurseForge or
 * other platforms.
 * @author Griefed
 */
@Component
public class ServerPackHandler {

    private static final Logger LOG = LogManager.getLogger(ServerPackHandler.class);
    private static final Logger LOG_ADDONS = LogManager.getLogger("AddonsLogger");
    private static final Logger LOG_INSTALLER = LogManager.getLogger("InstallerLogger");

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final VersionMeta VERSIONMETA;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final Utilities UTILITIES;
    private final ApplicationPlugins APPLICATIONPLUGINS;
    private final ConfigUtilities CONFIGUTILITIES;
    private final StopWatch STOPWATCH = new StopWatch();

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} required for creating a modpack from CurseForge.
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     * @param injectedVersionMeta Instance of {@link VersionMeta} required for everything version related.
     * @param injectedUtilities Instance of {@link Utilities}.
     * @param injectedApplicationPlugins Instance of {@link ApplicationPlugins}.
     * @param injectedConfigUtilities Instance of {@link ConfigUtilities}
     * @throws IOException if the {@link VersionMeta} could not be instantiated.
     */
    @Autowired
    public ServerPackHandler(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack,
                             ApplicationProperties injectedApplicationProperties, VersionMeta injectedVersionMeta,
                             Utilities injectedUtilities, ApplicationPlugins injectedApplicationPlugins, ConfigUtilities injectedConfigUtilities) throws IOException {

        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedVersionMeta == null) {
            this.VERSIONMETA = new VersionMeta(
                    APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_MINECRAFT,
                    APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FORGE,
                    APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC,
                    APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER
            );
        } else {
            this.VERSIONMETA = injectedVersionMeta;
        }

        if (injectedUtilities == null) {
            this.UTILITIES = new Utilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.UTILITIES = injectedUtilities;
        }

        if (injectedConfigUtilities == null) {
            this.CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, UTILITIES, APPLICATIONPROPERTIES, VERSIONMETA);
        } else {
            this.CONFIGUTILITIES = injectedConfigUtilities;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES, CONFIGUTILITIES);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedApplicationPlugins == null) {
            this.APPLICATIONPLUGINS = new ApplicationPlugins();
        } else {
            this.APPLICATIONPLUGINS = injectedApplicationPlugins;
        }
    }

    /**
     * Getter for the object-mapper used for working with JSON-data.
     * @author Griefed
     * @return ObjectMapper. Returns the object-mapper used for working with JSON-data.
     */
    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return objectMapper;
    }

    /**
     * Create a server pack from a given instance of {@link ServerPackModel} via webUI.
     * @author Griefed
     * @param serverPackModel An instance of {@link ServerPackModel} which contains the configuration of the modpack.
     * @return Returns the passed {@link ServerPackModel} which got altered during the creation of said server pack.
     */
    public ServerPackModel run(@NotNull ServerPackModel serverPackModel) {

        String destination = String.format("%s/%s", APPLICATIONPROPERTIES.getDirectoryServerPacks(), serverPackModel.getModpackDir().substring(serverPackModel.getModpackDir().lastIndexOf("/") + 1) + serverPackModel.getServerPackSuffix());

        run((ConfigurationModel) serverPackModel);

        cleanupEnvironment(false, destination);
        CURSECREATEMODPACK.cleanupEnvironment(serverPackModel.getModpackDir());

        serverPackModel.setStatus("Available");
        serverPackModel.setSize(Double.parseDouble(String.valueOf(FileUtils.sizeOfAsBigInteger(new File(destination + "_server_pack.zip")).divide(BigInteger.valueOf(1048576)))));
        serverPackModel.setPath(destination + "_server_pack.zip");

        return serverPackModel;
    }

    /**
     * Create a server pack from a given instance of {@link ConfigurationModel}.
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack
     *                           from which the server pack is to be created.
     * @return Boolean. Returns true if the server pack was successfully generated.
     */
    public boolean run(@NotNull ConfigurationModel configurationModel) {

        String destination = new File(
                String.format(
                        "%s/%s",
                        APPLICATIONPROPERTIES.getDirectoryServerPacks(),
                        configurationModel.getModpackDir().substring(
                                configurationModel.getModpackDir().lastIndexOf("/") + 1
                        ) + configurationModel.getServerPackSuffix()
                )
        ).getAbsolutePath()
                .replace("\\","/");

        /*
         * Check whether the server pack for the specified modpack already exists and whether overwrite is disabled.
         * If the server pack exists and overwrite is disabled, no new server pack will be generated.
         */
        if (APPLICATIONPROPERTIES.getProperty("de.griefed.serverpackcreator.serverpack.overwrite.enabled").equals("false") &&
                new File(destination).exists()) {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.overwrite"));

        } else {

            // Make sure no files from previously generated server packs interrupt us.
            cleanupEnvironment(true, destination);

            try { Files.createDirectories(Paths.get(destination));
            } catch (IOException ignored) {

            }

            if (!APPLICATIONPLUGINS.pluginsPreGenExtension().isEmpty()) {
                LOG_ADDONS.info("Executing PreGenExtension addons");
                APPLICATIONPLUGINS.pluginsPreGenExtension().forEach(plugin -> {
                    LOG_ADDONS.info("Executing plugin " + plugin.getName());

                    try {
                        plugin.run(APPLICATIONPROPERTIES, configurationModel, destination);
                    } catch (Exception ex) {
                        LOG_ADDONS.error(plugin.getName() + " encountered an error.", ex);
                    }
                });
            } else {
                LOG.info("No PreGenExtension addons to execute.");
            }

            // Recursively copy all specified directories and files, excluding clientside-only mods, to server pack.
            copyFiles(configurationModel.getModpackDir(), configurationModel.getCopyDirs(), configurationModel.getClientMods(), configurationModel.getMinecraftVersion(), destination, configurationModel.getModLoader());

            // Copy start scripts for specified modloader from server_files to server pack.
            createStartScripts(configurationModel.getModLoader(), configurationModel.getJavaArgs(), configurationModel.getMinecraftVersion(), configurationModel.getModLoaderVersion(), destination);

            // If true, Install the modloader software for the specified Minecraft version, modloader, modloader version
            if (configurationModel.getIncludeServerInstallation()) {
                installServer(configurationModel.getModLoader(), configurationModel.getMinecraftVersion(), configurationModel.getModLoaderVersion(), configurationModel.getJavaPath(), destination);
            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.server"));
            }
            // If modloader is fabric, try and replace the old server-launch.jar with the new and improved one which also downloads the Minecraft server.
            if (configurationModel.getModLoader().equalsIgnoreCase("Fabric")) {
                provideImprovedFabricServerLauncher(configurationModel.getMinecraftVersion(),configurationModel.getModLoaderVersion(), destination);
            }


            // If true, copy the server-icon.png from server_files to the server pack.
            if (configurationModel.getIncludeServerIcon()) {
                copyIcon(destination, configurationModel.getServerIconPath());
            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.icon"));
            }

            // If true, copy the server.properties from server_files to the server pack.
            if (configurationModel.getIncludeServerProperties()) {
                copyProperties(destination, configurationModel.getServerPropertiesPath());
            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.properties"));
            }

            if (!APPLICATIONPLUGINS.pluginsPreZipExtension().isEmpty()) {
                LOG_ADDONS.info("Executing PreZipExtension addons");
                APPLICATIONPLUGINS.pluginsPreZipExtension().forEach(plugin -> {
                    LOG_ADDONS.info("Executing plugin " + plugin.getName());

                    try {
                        plugin.run(APPLICATIONPROPERTIES, configurationModel, destination);
                    } catch (Exception ex) {
                        LOG_ADDONS.error(plugin.getName() + " encountered an error.", ex);
                    }
                });
            } else {
                LOG.info("No PreZipExtension addons to execute.");
            }


            // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
            if (configurationModel.getIncludeZipCreation()) {
                zipBuilder(configurationModel.getMinecraftVersion(), configurationModel.getIncludeServerInstallation(), destination);
            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.zip"));
            }

            // Inform user about location of newly generated server pack.
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.serverpack"), destination));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.archive"), destination));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.finish"));

            if (!APPLICATIONPLUGINS.pluginsPostGenExtension().isEmpty()) {
                LOG_ADDONS.info("Executing PostGenExtension addons");
                APPLICATIONPLUGINS.pluginsPostGenExtension().forEach(plugin -> {
                    LOG_ADDONS.info("Executing plugin " + plugin.getName());

                    try {
                        plugin.run(APPLICATIONPROPERTIES, configurationModel, destination);
                    } catch (Exception ex) {
                        LOG_ADDONS.error(plugin.getName() + " encountered an error.", ex);
                    }
                });
            } else {
                LOG.info("No PostGenExtension addons to execute.");
            }

        }
        return true;

    }

    /**
     * Download and provide the improved Fabric Server Launcher, if it is available for the given Minecraft and Fabric version.
     * @author Griefed
     * @param minecraftVersion String. The Minecraft version the modpack uses and the Fabric Server Launcher should be downloaded for.
     * @param fabricVersion String. The modloader version the modpack uses and the Fabric Server Launcher should be downloaded for.
     * @param destination String. The destination of the server pack.
     */
    private void provideImprovedFabricServerLauncher(String minecraftVersion, String fabricVersion, String destination) {
        String fileDestination = String.format("%s/fabric-server-launcher.jar", destination);

        if (VERSIONMETA.fabric().improvedLauncherUrl(minecraftVersion, fabricVersion).isPresent() &&
                UTILITIES.WebUtils().downloadFile(fileDestination, VERSIONMETA.fabric().improvedLauncherUrl(minecraftVersion, fabricVersion).get())) {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.fabric.improved"));

            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            String.valueOf(
                                    Paths.get(
                                            String.format("%s/SERVER_PACK_INFO.txt",destination)
                                    )
                            )
                    )
            )) {

                // Improved Fabric server launcher info
                writer.write("If you are using this server pack on a managed server, meaning you can not execute scripts, please use the fabric-server-launcher.jar instead of the fabric-server-launch.jar. Note the extra \"er\" at the end of \"launcher\".\n");
                writer.write("This is the improved Fabric Server Launcher, which will take care of downloading and installing the Minecraft server and any and all libraries needed for running the Fabric server.\n");
                writer.write("\n");
                writer.write("The downside of this method is the occasional incompatibility of mods with the Fabric version, as the new Fabric Server Launcher always uses the latest available Fabric version.\n");
                writer.write("If a mod is incompatible with said latest Fabric version, contact the mod-author and ask them to remedy the situation.\n");
                writer.write("The official Fabric Discord had the following to add to this:\n");
                writer.write("    Fabric loader however is cross version, so unless there is a mod incompatibility (which usually involves the mod being broken / using non-api internals)\n");
                writer.write("    there is no good reason to use anything but the latest. I.e. the latest loader on any Minecraft version works with the new server launcher.");

            } catch (IOException ex) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error("Error downloading the improved Fabric server launcher.", ex);
            }
        }
    }

    /**
     * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure newly generated
     * server pack is as clean as possible.
     * @author Griefed
     * @param deleteZip Boolean. Whether to delete the server pack ZIP-archive.
     * @param destination String. The destination at which to clean up in.
     */
    private void cleanupEnvironment(boolean deleteZip, String destination) {

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.folder.enter"));

        FileUtils.deleteQuietly(new File(destination));

        if (deleteZip) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.zip.enter"));

            FileUtils.deleteQuietly(new File(String.format("%s_server_pack.zip", destination)));

        }
    }

    /**
     * Create start-scripts for the generated server pack.
     * @author Griefed
     * @param modLoader String. Whether to copy the Forge or Fabric scripts into the server pack.
     * @param javaArguments String. Java arguments to write the start-scripts with.
     * @param minecraftVersion String. The Minecraft version the modpack uses.
     * @param modloaderVersion String. The modloader version the modpack uses.
     * @param destination String. The destination where the scripts should be created in.
     */
    private void createStartScripts(String modLoader, String javaArguments, String minecraftVersion, String modloaderVersion, String destination) {

        if (javaArguments.equals("empty")) {
            javaArguments = "";
        }

        if (modLoader.equalsIgnoreCase("Forge")) {

            String[] minecraft = minecraftVersion.split("\\.");

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copystartscripts.forge"));

            if (Integer.parseInt(minecraft[1]) < 17) {

                forgeBatchScript(javaArguments, minecraftVersion, modloaderVersion, destination);
                forgeShellScript(javaArguments, minecraftVersion, modloaderVersion, destination);

            } else {

                forgeBatchScriptNewMC(javaArguments, minecraftVersion, modloaderVersion, destination);
                forgeShellScriptNewMC(javaArguments, minecraftVersion, modloaderVersion, destination);
                forgeJvmArgsTxt(javaArguments, destination);
            }

        } else if (modLoader.equalsIgnoreCase("Fabric")) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copystartscripts.fabric"));

            fabricBatchScript(javaArguments, minecraftVersion, modloaderVersion, destination);
            fabricShellScript(javaArguments, minecraftVersion, modloaderVersion, destination);

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"));
        }
    }

    /**
     * Create a fabric shell start script
     * @author Griefed
     * @param javaArguments String. Java arguments with which the server should be started
     * @param minecraftVersion String. Minecraft version of this server pack.
     * @param modloaderVersion String. Modloader version of this server pack.
     * @param destination String. Where the script should be written to. Result is a combination of <code>String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_LINUX)</code>
     */
    private void fabricShellScript(String javaArguments, String minecraftVersion, String modloaderVersion, String destination) {

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        String.valueOf(
                                Paths.get(
                                        String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_LINUX)
                                )
                        )
                )
        )) {

            if (!VERSIONMETA.minecraft().getServer(minecraftVersion).isPresent()) {
                LOG.error("A server is not available for the specified Minecraft version.");
                return;
            }

            // Fabric Bash file
            writer.write("#!/usr/bin/env bash\n");
            writer.write("# Start script generated by ServerPackCreator.\n");
            writer.write("# This script checks for the Minecraft and Forge JAR-Files, and if they are not found, they are downloaded and installed.\n");
            writer.write("# If everything is in order, the server is started.\n");
            writer.write("\n");
            writer.write("JAVA=\"java\"\n");
            writer.write("MINECRAFT=\"" + minecraftVersion + "\"\n");
            writer.write("FABRIC=\"" + modloaderVersion + "\"\n");
            writer.write("INSTALLER=\"" + VERSIONMETA.fabric().releaseInstallerVersion() + "\"\n");
            writer.write("ARGS=\"" + javaArguments + "\"\n");
            writer.write("OTHERARGS=\"-Dlog4j2.formatMsgNoLookups=true\"\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"fabric-server-launch.jar\" ]];then\n");
            writer.write("\n");
            writer.write("  echo \"Fabric Server JAR-file not found. Downloading installer...\";\n");
            writer.write("  wget -O fabric-installer.jar https://maven.fabricmc.net/net/fabricmc/fabric-installer/$INSTALLER/fabric-installer-$INSTALLER.jar;\n");
            writer.write("\n");
            writer.write("  if [[ -s \"fabric-installer.jar\" ]];then\n");
            writer.write("\n");
            writer.write("    echo \"Installer downloaded. Installing...\";\n");
            writer.write("    java -jar fabric-installer.jar server -mcversion $MINECRAFT -loader $FABRIC -downloadMinecraft;\n");
            writer.write("\n");
            writer.write("    if [[ -s \"fabric-server-launch.jar\" ]];then\n");
            writer.write("      rm -rf .fabric-installer;\n");
            writer.write("      rm -f fabric-installer.jar;\n");
            writer.write("      echo \"Installation complete. fabric-installer.jar deleted.\";\n");
            writer.write("    fi\n");
            writer.write("\n");
            writer.write("  else\n");
            writer.write("    echo \"fabric-installer.jar not found. Maybe the Fabric server are having trouble.\";\n");
            writer.write("    echo \"Please try again in a couple of minutes.\";\n");
            writer.write("  fi\n");
            writer.write("else\n");
            writer.write("  echo \"fabric-server-launch.jar present. Moving on...\";\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"server.jar\" ]];then\n");
            writer.write("  echo \"Minecraft Server JAR-file not found. Downloading...\";\n");
            writer.write("  wget -O server.jar " + VERSIONMETA.minecraft().getServer(minecraftVersion).get().url() + ";\n");
            writer.write("else\n");
            writer.write("  echo \"server.jar present. Moving on...\";\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"eula.txt\" ]];then\n");
            writer.write("  echo \"Mojang's EULA has not yet been accepted. In order to run a Minecraft server, you must accept Mojang's EULA.\"\n");
            writer.write("  echo \"Mojang's EULA is available to read at https://account.mojang.com/documents/minecraft_eula\"\n");
            writer.write("  echo \"If you agree to Mojang's EULA then type 'I agree'\"\n");
            writer.write("  read ANSWER\n");
            writer.write("  if [[ \"$ANSWER\" = \"I agree\" ]]; then\n");
            writer.write("    echo \"User agreed to Mojang's EULA.\"\n");
            writer.write("    echo \"#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\" > eula.txt;\n");
            writer.write("    echo \"eula=true\" >> eula.txt;\n");
            writer.write("  else\n");
            writer.write("    echo \"User did not agree to Mojang's EULA.\"\n");
            writer.write("  fi\n");
            writer.write("else\n");
            writer.write("  echo \"eula.txt present. Moving on...\";\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("echo \"Starting server...\";\n");
            writer.write("echo \"Minecraft version: $MINECRAFT\";\n");
            writer.write("echo \"Fabric version: $FABRIC\";\n");
            writer.write("echo \"Java version:\"\n");
            // Bash, Linux, we must use -version
            writer.write("$JAVA -version\n");
            writer.write("echo \"Java args: $ARGS\";\n");
            writer.write("\n");
            writer.write("$JAVA $OTHERARGS $ARGS -jar fabric-server-launch.jar nogui");

        } catch (IOException ex) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error("Error generating shell-script for Forge.", ex);
        }
    }

    /**
     * Create a fabric batch start script.
     * @author Griefed
     * @param javaArguments String. Java arguments wich which the server should be started.
     * @param minecraftVersion String. The Minecraft version of this server pack.
     * @param modloaderVersion String. The modloader version of this server pack.
     * @param destination String. Where the script should be written to. Result is a combination of <code>String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_LINUX)</code>
     */
    private void fabricBatchScript(String javaArguments, String minecraftVersion, String modloaderVersion, String destination) {

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        String.valueOf(
                                Paths.get(
                                        String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_WINDOWS)
                                )
                        )
                )
        )) {

            if (!VERSIONMETA.minecraft().getServer(minecraftVersion).isPresent()) {
                LOG.error("A server is not available for the specified Minecraft version.");
                return;
            }

            // Fabric Batch file
            writer.write(":: Start script generated by ServerPackCreator.\n");
            writer.write(":: This script checks for the Minecraft and Fabric JAR-files, and if they are not found, they are downloaded and installed.\n");
            writer.write(":: If everything is in order, the server is started.\n");
            writer.write("@ECHO off\n");
            writer.write("\n");
            writer.write("SET JAVA=\"java\"\n");
            writer.write("SET MINECRAFT=\"" + minecraftVersion + "\"\n");
            writer.write("SET FABRIC=\"" + modloaderVersion + "\"\n");
            writer.write("SET INSTALLER=\"" + VERSIONMETA.fabric().releaseInstallerVersion() + "\"\n");
            writer.write("SET ARGS=" + javaArguments + "\n");
            writer.write("SET OTHERARGS=\"-Dlog4j2.formatMsgNoLookups=true\"\n");
            writer.write("\n");
            writer.write("SET AGREE=\"I agree\"\n");
            writer.write("\n");
            writer.write("IF NOT EXIST fabric-server-launch.jar (\n");
            writer.write("\n");
            writer.write("  ECHO Fabric Server JAR-file not found. Downloading installer...\n");
            writer.write("  powershell -Command \"(New-Object Net.WebClient).DownloadFile('https://maven.fabricmc.net/net/fabricmc/fabric-installer/%INSTALLER%/fabric-installer-%INSTALLER%.jar', 'fabric-installer.jar')\"\n");
            writer.write("\n");
            writer.write("  IF EXIST fabric-installer.jar (\n");
            writer.write("\n");
            writer.write("    ECHO Installer downloaded. Installing...\n");
            writer.write("    java -jar fabric-installer.jar server -mcversion %MINECRAFT% -loader %FABRIC% -downloadMinecraft\n");
            writer.write("\n");
            writer.write("    IF EXIST fabric-server-launch.jar (\n");
            writer.write("      RMDIR /S /Q .fabric-installer\n");
            writer.write("      DEL fabric-installer.jar\n");
            writer.write("      ECHO Installation complete. fabric-installer.jar and installation files deleted.\n");
            writer.write("    )\n");
            writer.write("\n");
            writer.write("  ) ELSE (\n");
            writer.write("    ECHO fabric-installer.jar not found. Maybe the Fabric servers are having trouble.\n");
            writer.write("    ECHO Please try again in a couple of minutes.\n");
            writer.write("  )\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO fabric-server-launch.jar present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("IF NOT EXIST server.jar (\n");
            writer.write("  ECHO Minecraft Server JAR-file not found. Downloading...\n");
            writer.write("  powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + VERSIONMETA.minecraft().getServer(minecraftVersion).get().url() + "', 'server.jar')\"\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO server.jar present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("IF NOT EXIST eula.txt (\n");
            writer.write("  ECHO Mojang's EULA has not yet been accepted. In order to run a Minecraft server, you must accept Mojang's EULA.\n");
            writer.write("  ECHO Mojang's EULA is available to read at https://account.mojang.com/documents/minecraft_eula\n");
            writer.write("  ECHO If you agree to Mojang's EULA then type \"I agree\"\n");
            writer.write("  set /P \"Response=\"\n");
            writer.write("  IF \"%Response%\" == \"%AGREE%\" (\n");
            writer.write("    ECHO User agreed to Mojang's EULA.\n");
            writer.write("    ECHO #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://account.mojang.com/documents/minecraft_eula^).> eula.txt\n");
            writer.write("    ECHO eula=true>> eula.txt\n");
            writer.write("  ) else (\n");
            writer.write("    ECHO User did not agree to Mojang's EULA. \n");
            writer.write("  )\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO eula.txt present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("ECHO Starting server...\n");
            writer.write("ECHO Minecraft version: %MINECRAFT%\n");
            writer.write("ECHO Fabric version: %FABRIC%\n");
            writer.write("ECHO Java version:\n");
            // Batch, Windows, we can use --version
            writer.write("%JAVA% --version\n");
            writer.write("ECHO Java args: %ARGS%\n");
            writer.write("\n");
            writer.write("%JAVA% \"%OTHERARGS%\" %ARGS% -jar fabric-server-launch.jar nogui\n");
            writer.write("\n");
            writer.write("PAUSE");

        } catch (IOException ex) {
            LOG.error("Error generating batch-script for Forge.", ex);
        }
    }

    /**
     * Create a Forge JVM args file used by Forge Minecraft 1.17 and newer.
     * @author Griefed
     * @param javaArguments String. Java arguments with which the server should be started.
     * @param destination String. Where the file should be written to. Result is a combination of <code>String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS)</code>
     */
    private void forgeJvmArgsTxt(String javaArguments, String destination) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        String.valueOf(
                                Paths.get(
                                        String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS)
                                )
                        )
                )
        )) {

            // User Java Args file for Minecraft 1.17+ Forge servers
            writer.write("# Xmx and Xms set the maximum and minimum RAM usage, respectively.\n");
            writer.write("# They can take any number, followed by an M or a G.\n");
            writer.write("# M means Megabyte, G means Gigabyte.\n");
            writer.write("# For example, to set the maximum to 3GB: -Xmx3G\n");
            writer.write("# To set the minimum to 2.5GB: -Xms2500M\n");
            writer.write("\n");
            writer.write("# A good default for a modded server is 4GB.\n");
            writer.write("# Uncomment the next line to set it.\n");
            writer.write("# -Xmx4G\n");
            writer.write(javaArguments);

        } catch (IOException ex) {
            LOG.error("Error generating user_jvm_args.txt for Forge.", ex);
        }
    }

    /**
     * Create a Forge shell start script for Minecraft 1.17 and newer.
     * @author Griefed
     * @param javaArguments String. Java arguments with which the server should be started.
     * @param minecraftVersion String. The Minecraft version of this server pack.
     * @param modloaderVersion String. The modloader version of this server pack.
     * @param destination String. Where the script should be written to. Result is a combination of <code>String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_LINUX)</code>
     */
    private void forgeShellScriptNewMC(String javaArguments, String minecraftVersion, String modloaderVersion, String destination) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        String.valueOf(
                                Paths.get(
                                        String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_LINUX)
                                )
                        )
                )
        )) {

            if (!VERSIONMETA.minecraft().getServer(minecraftVersion).isPresent()) {
                LOG.error("A server is not available for the specified Minecraft version.");
                return;
            }

            /*
             * FORGE 1.17 AND NEWER!
             * If the specified Minecraft version is newer than or equal to 1.17.1, then we need to generate scripts which run
             * Forge the new way, by running @user_jvm_args.txt and @libraries[...] etc.
             *
             * Forge Bash
             */
            writer.write("#!/usr/bin/env bash\n");
            writer.write("# Start script generated by ServerPackCreator.\n");
            writer.write("# This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n");
            writer.write("# If everything is in order, the server is started.\n");
            writer.write("\n");
            writer.write("JAVA=\"java\"\n");
            writer.write("MINECRAFT=\"" + minecraftVersion + "\"\n");
            writer.write("FORGE=\"" + modloaderVersion + "\"\n");
            writer.write("ARGS=\"" + javaArguments + "\"\n");
            writer.write("OTHERARGS=\"-Dlog4j2.formatMsgNoLookups=true\"\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"libraries/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-server.jar\" ]];then\n");
            writer.write("\n");
            writer.write("  echo \"Forge Server JAR-file not found. Downloading installer...\";\n");
            writer.write("  wget -O forge-installer.jar https://files.minecraftforge.net/maven/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-installer.jar;\n");
            writer.write("\n");
            writer.write("  if [[ -s \"forge-installer.jar\" ]]; then\n");
            writer.write("\n");
            writer.write("    echo \"Installer downloaded. Installing...\";\n");
            writer.write("    java -jar forge-installer.jar --installServer;\n");
            writer.write("\n");
            writer.write("    if [[ -s \"libraries/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-server.jar\" ]];then\n");
            writer.write("      rm -f forge-installer.jar;\n");
            writer.write("      echo \"Installation complete. forge-installer.jar deleted.\";\n");
            writer.write("    fi\n");
            writer.write("\n");
            writer.write("  else\n");
            writer.write("    echo \"forge-installer.jar not found. Maybe the Forges servers are having trouble.\";\n");
            writer.write("    echo \"Please try again in a couple of minutes.\";\n");
            writer.write("  fi\n");
            writer.write("else\n");
            writer.write("  echo \"Forge server present. Moving on...\"\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"libraries/net/minecraft/server/$MINECRAFT/server-$MINECRAFT.jar\" ]];then\n");
            writer.write("  echo \"Minecraft Server JAR-file not found. Downloading...\";\n");
            writer.write("  wget -O libraries/net/minecraft/server/$MINECRAFT/server-$MINECRAFT.jar " + VERSIONMETA.minecraft().getServer(minecraftVersion).get().url() + ";\n");
            writer.write("else\n");
            writer.write("  echo \"Minecraft server present. Moving on...\"\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("if [[ -s \"run.bat\" ]];then\n");
            writer.write("  rm -f run.bat;\n");
            writer.write("  echo \"Deleted run.bat as we already have start.bat\";\n");
            writer.write("fi\n");
            writer.write("if [[ -s \"run.sh\" ]];then\n");
            writer.write("  rm -f run.sh;\n");
            writer.write("  echo \"Deleted run.sh as we already have start.sh\";\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"eula.txt\" ]];then\n");
            writer.write("  echo \"Mojang's EULA has not yet been accepted. In order to run a Minecraft server, you must accept Mojang's EULA.\"\n");
            writer.write("  echo \"Mojang's EULA is available to read at https://account.mojang.com/documents/minecraft_eula\"\n");
            writer.write("  echo \"If you agree to Mojang's EULA then type 'I agree'\"\n");
            writer.write("  read ANSWER\n");
            writer.write("  if [[ \"$ANSWER\" = \"I agree\" ]]; then\n");
            writer.write("    echo \"User agreed to Mojang's EULA.\"\n");
            writer.write("    echo \"#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\" > eula.txt;\n");
            writer.write("    echo \"eula=true\" >> eula.txt;\n");
            writer.write("  else\n");
            writer.write("    echo \"User did not agree to Mojang's EULA.\"\n");
            writer.write("  fi\n");
            writer.write("else\n");
            writer.write("  echo \"eula.txt present. Moving on...\";\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("echo \"Starting server...\";\n");
            writer.write("echo \"Minecraft version: $MINECRAFT\";\n");
            writer.write("echo \"Forge version: $FORGE\";\n");
            writer.write("echo \"Java version:\"\n");
            // Bash, Linux, we must use -version
            writer.write("$JAVA -version\n");
            writer.write("echo \"Java args in user_jvm_args.txt: $ARGS\";\n");
            writer.write("\n");
            writer.write("# Forge requires a configured set of both JVM and program arguments.\n");
            writer.write("# Add custom JVM arguments to the user_jvm_args.txt\n");
            writer.write("# Add custom program arguments {such as nogui} to this file in the next line before the \"$@\" or\n");
            writer.write("#  pass them to this script directly\n");
            writer.write("echo \"If you receive the error message 'Error: Could not find or load main class @user_jvm_args.txt' you may be using the wrong Java-version for this modded Minecraft server. Contact the modpack-developer or, if you made the server pack yourself, do a quick google-search for the used Minecraft version to find out which Java-version is required in order to run this server.\"\n");
            writer.write("\n");
            writer.write("$JAVA $OTHERARGS @user_jvm_args.txt @libraries/net/minecraftforge/forge/$MINECRAFT-$FORGE/unix_args.txt nogui \"$@\"");

        } catch (IOException ex) {
            LOG.error("Error generating shell-script for Forge.", ex);
        }
    }

    /**
     * Create a Forge batch script for Minecraft 1.17 and newer.
     * @author Griefed
     * @param javaArguments String. Java arguments with which the server should be started.
     * @param minecraftVersion String. The Minecraft version of this server pack.
     * @param modloaderVersion String. The modloader version of this server pack.
     * @param destination String. Where the script should be written to. Result is a combination of <code>String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_WINDOWS)</code>
     */
    private void forgeBatchScriptNewMC(String javaArguments, String minecraftVersion, String modloaderVersion, String destination) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        String.valueOf(
                                Paths.get(
                                        String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_WINDOWS)
                                )
                        )
                )
        )) {

            if (!VERSIONMETA.minecraft().getServer(minecraftVersion).isPresent()) {
                LOG.error("A server is not available for the specified Minecraft version.");
                return;
            }

            /*
             * FORGE 1.17 AND NEWER!
             * If the specified Minecraft version is newer than or equal to 1.17.1, then we need to generate scripts which run
             * Forge the new way, by running @user_jvm_args.txt and @libraries[...] etc.
             *
             * Forge Batch
             */
            writer.write(":: Start script generated by ServerPackCreator.\n");
            writer.write(":: This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n");
            writer.write(":: If everything is in order, the server is started.\n");
            writer.write("@ECHO off\n");
            writer.write("SetLocal EnableDelayedExpansion\n");
            writer.write("\n");
            writer.write("SET JAVA=\"java\"\n");
            writer.write("SET MINECRAFT=\"" + minecraftVersion + "\"\n");
            writer.write("SET FORGE=\"" + modloaderVersion + "\"\n");
            writer.write("SET ARGS=" + javaArguments + "\n");
            writer.write("SET OTHERARGS=\"-Dlog4j2.formatMsgNoLookups=true\"\n");
            writer.write("\n");
            writer.write("SET AGREE=\"I agree\"\n");
            writer.write("\n");
            writer.write("IF NOT EXIST libraries/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-server.jar (\n");
            writer.write("\n");
            writer.write("  ECHO Forge Server JAR-file not found. Downloading installer...\n");
            writer.write("  powershell -Command \"(New-Object Net.WebClient).DownloadFile('https://files.minecraftforge.net/maven/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-installer.jar', 'forge-installer.jar')\"\n");
            writer.write("\n");
            writer.write("  IF EXIST forge-installer.jar (\n");
            writer.write("\n");
            writer.write("    ECHO Installer downloaded. Installing...\n");
            writer.write("    java -jar forge-installer.jar --installServer\n");
            writer.write("\n");
            writer.write("    IF EXIST libraries/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-server.jar (\n");
            writer.write("      DEL forge-installer.jar\n");
            writer.write("      ECHO Installation complete. forge-installer.jar deleted.\n");
            writer.write("    )\n");
            writer.write("\n");
            writer.write("  ) ELSE (\n");
            writer.write("    ECHO forge-installer.jar not found. Maybe the Forges servers are having trouble.\n");
            writer.write("    ECHO Please try again in a couple of minutes.\n");
            writer.write("  )\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO Forge server present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("IF NOT EXIST libraries/net/minecraft/server/%MINECRAFT%/server-%MINECRAFT%.jar (\n");
            writer.write("  ECHO Minecraft Server JAR-file not found. Downloading...\n");
            writer.write("  powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + VERSIONMETA.minecraft().getServer(minecraftVersion).get().url() + "', 'libraries/net/minecraft/server/%MINECRAFT%/server-%MINECRAFT%.jar')\"\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO Minecraft server present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("IF EXIST run.bat (\n");
            writer.write("  DEL run.bat\n");
            writer.write("  ECHO Deleted run.bat as we already have start.bat\n");
            writer.write(")\n");
            writer.write("IF EXIST run.sh (\n");
            writer.write("  DEL run.sh\n");
            writer.write("  ECHO Deleted run.sh as we already have start.sh\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("IF NOT EXIST eula.txt (\n");
            writer.write("  ECHO Mojang's EULA has not yet been accepted. In order to run a Minecraft server, you must accept Mojang's EULA.\n");
            writer.write("  ECHO Mojang's EULA is available to read at https://account.mojang.com/documents/minecraft_eula\n");
            writer.write("  ECHO If you agree to Mojang's EULA then type \"I agree\"\n");
            writer.write("  set /P \"Response=\"\n");
            writer.write("  IF \"%Response%\" == \"%AGREE%\" (\n");
            writer.write("    ECHO User agreed to Mojang's EULA.\n");
            writer.write("    ECHO #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://account.mojang.com/documents/minecraft_eula^).> eula.txt\n");
            writer.write("    ECHO eula=true>> eula.txt\n");
            writer.write("  ) else (\n");
            writer.write("    ECHO User did not agree to Mojang's EULA. \n");
            writer.write("  )\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO eula.txt present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("ECHO Starting server...\n");
            writer.write("ECHO Minecraft version: %MINECRAFT%\n");
            writer.write("ECHO Forge version: %FORGE%\n");
            writer.write("ECHO Java version:\n");
            // Batch, Windows, we can use --version
            writer.write("%JAVA% --version\n");
            writer.write("ECHO Java args in user_jvm_args.txt: %ARGS%\n");
            writer.write("\n");
            writer.write("REM Forge requires a configured set of both JVM and program arguments.\n");
            writer.write("REM Add custom JVM arguments to the user_jvm_args.txt\n");
            writer.write("REM Add custom program arguments {such as nogui} to this file in the next line before the %* or\n");
            writer.write("REM  pass them to this script directly\n");
            writer.write("ECHO If you receive the error message \"Error: Could not find or load main class @user_jvm_args.txt\" you may be using the wrong Java-version for this modded Minecraft server. Contact the modpack-developer or, if you made the server pack yourself, do a quick google-search for the used Minecraft version to find out which Java-version is required in order to run this server.\n");
            writer.write("\n");
            writer.write("%JAVA% \"%OTHERARGS%\" @user_jvm_args.txt @libraries/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/win_args.txt nogui %*\n");
            writer.write("\n");
            writer.write("PAUSE");

        } catch (IOException ex) {
            LOG.error("Error generating batch-script for Forge.", ex);
        }
    }

    /**
     * Create a Forge shell script for Minecraft 1.16 and older.
     * @author Griefed
     * @param javaArguments String. The Java arguments with which to start the server.
     * @param minecraftVersion String. The Minecraft version of this server pack.
     * @param modloaderVersion String. The modloader version of this server pack.
     * @param destination String. Where the script should be written to. Result is a combination of <code>String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_LINUX)</code>
     */
    private void forgeShellScript(String javaArguments, String minecraftVersion, String modloaderVersion, String destination) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        String.valueOf(
                                Paths.get(
                                        String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_LINUX)
                                )
                        )
                )
        )) {

            if (!VERSIONMETA.minecraft().getServer(minecraftVersion).isPresent()) {
                LOG.error("A server is not available for the specified Minecraft version.");
                return;
            }

            /*
             * FORGE 1.16 AND OLDER!
             * If the specified Minecraft version is older than 1.17.1, then we need to generate scripts which run
             * Forge the old way, with the Minecraft server-jar and the Forge server-jar, executing the Forge server-jar
             * with the given Java args.
             *
             * Forge Bash file
             */
            writer.write("#!/usr/bin/env bash\n");
            writer.write("# Start script generated by ServerPackCreator.\n");
            writer.write("# This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n");
            writer.write("# If everything is in order, the server is started.\n");
            writer.write("\n");
            writer.write("JAVA=\"java\"\n");
            writer.write("MINECRAFT=\"" + minecraftVersion + "\"\n");
            writer.write("FORGE=\"" + modloaderVersion + "\"\n");
            writer.write("ARGS=\"" + javaArguments + "\"\n");
            writer.write("OTHERARGS=\"-Dlog4j2.formatMsgNoLookups=true\"\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"forge.jar\" ]];then\n");
            writer.write("\n");
            writer.write("  echo \"Forge Server JAR-file not found. Downloading installer...\";\n");
            writer.write("  wget -O forge-installer.jar https://files.minecraftforge.net/maven/net/minecraftforge/forge/$MINECRAFT-$FORGE/forge-$MINECRAFT-$FORGE-installer.jar;\n");
            writer.write("\n");
            writer.write("  if [[ -s \"forge-installer.jar\" ]]; then\n");
            writer.write("\n");
            writer.write("    echo \"Installer downloaded. Installing...\";\n");
            writer.write("    java -jar forge-installer.jar --installServer;\n");
            writer.write("    mv forge-$MINECRAFT-$FORGE.jar forge.jar;\n");
            writer.write("\n");
            writer.write("    if [[ -s \"forge.jar\" ]];then\n");
            writer.write("      rm -f forge-installer.jar;\n");
            writer.write("      echo \"Installation complete. forge-installer.jar deleted.\";\n");
            writer.write("    fi\n");
            writer.write("\n");
            writer.write("  else\n");
            writer.write("    echo \"forge-installer.jar not found. Maybe the Forges servers are having trouble.\";\n");
            writer.write("    echo \"Please try again in a couple of minutes.\";\n");
            writer.write("  fi\n");
            writer.write("else\n");
            writer.write("  echo \"forge.jar present. Moving on...\"\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"minecraft_server.$MINECRAFT.jar\" ]];then\n");
            writer.write("  echo \"Minecraft Server JAR-file not found. Downloading...\";\n");
            writer.write("  wget -O minecraft_server.$MINECRAFT.jar " + VERSIONMETA.minecraft().getServer(minecraftVersion).get().url() + ";\n");
            writer.write("else\n");
            writer.write("  echo \"minecraft_server.$MINECRAFT.jar present. Moving on...\"\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("if [[ ! -s \"eula.txt\" ]];then\n");
            writer.write("  echo \"Mojang's EULA has not yet been accepted. In order to run a Minecraft server, you must accept Mojang's EULA.\"\n");
            writer.write("  echo \"Mojang's EULA is available to read at https://account.mojang.com/documents/minecraft_eula\"\n");
            writer.write("  echo \"If you agree to Mojang's EULA then type 'I agree'\"\n");
            writer.write("  read ANSWER\n");
            writer.write("  if [[ \"$ANSWER\" = \"I agree\" ]]; then\n");
            writer.write("    echo \"User agreed to Mojang's EULA.\"\n");
            writer.write("    echo \"#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\" > eula.txt;\n");
            writer.write("    echo \"eula=true\" >> eula.txt;\n");
            writer.write("  else\n");
            writer.write("    echo \"User did not agree to Mojang's EULA.\"\n");
            writer.write("  fi\n");
            writer.write("else\n");
            writer.write("  echo \"eula.txt present. Moving on...\";\n");
            writer.write("fi\n");
            writer.write("\n");
            writer.write("echo \"Starting server...\";\n");
            writer.write("echo \"Minecraft version: $MINECRAFT\";\n");
            writer.write("echo \"Forge version: $FORGE\";\n");
            writer.write("echo \"Java version:\"\n");
            // Bash, Linux, we must use -version
            writer.write("$JAVA -version\n");
            writer.write("echo \"Java args: $ARGS\";\n");
            writer.write("\n");
            writer.write("$JAVA $OTHERARGS $ARGS -jar forge.jar nogui");

        } catch (IOException ex) {
            LOG.error("Error generating shell-script for Forge.", ex);
        }
    }

    /**
     * Create a forge batch script for Minecraft 1.16 and older.
     * @author Griefed
     * @param javaArguments String. The Java arguments with which to start the server.
     * @param minecraftVersion String. The Minecraft version of this server pack.
     * @param modloaderVersion String. The modloader version of this server pack.
     * @param destination String. Where the script should be written to. Result is a combination of <code>String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_WINDOWS)</code>
     */
    private void forgeBatchScript(String javaArguments, String minecraftVersion, String modloaderVersion, String destination) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        String.valueOf(
                                Paths.get(
                                        String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_WINDOWS)
                                )
                        )
                )
        )) {

            if (!VERSIONMETA.minecraft().getServer(minecraftVersion).isPresent()) {
                LOG.error("A server is not available for the specified Minecraft version.");
                return;
            }

            /*
             * FORGE 1.16 AND OLDER!
             * If the specified Minecraft version is older than 1.17.1, then we need to generate scripts which run
             * Forge the old way, with the Minecraft server-jar and the Forge server-jar, executing the Forge server-jar
             * with the given Java args.
             *
             * Forge Batch
             */
            writer.write(":: Start script generated by ServerPackCreator.\n");
            writer.write(":: This script checks for the Minecraft and Forge JAR-files, and if they are not found, they are downloaded and installed.\n");
            writer.write(":: If everything is in order, the server is started.\n");
            writer.write("@ECHO off\n");
            writer.write("SetLocal EnableDelayedExpansion\n");
            writer.write("\n");
            writer.write("SET JAVA=\"java\"\n");
            writer.write("SET MINECRAFT=\"" + minecraftVersion + "\"\n");
            writer.write("SET FORGE=\"" + modloaderVersion + "\"\n");
            writer.write("SET ARGS=" + javaArguments + "\n");
            writer.write("SET OTHERARGS=\"-Dlog4j2.formatMsgNoLookups=true\"\n");
            writer.write("\n");
            writer.write("SET AGREE=\"I agree\"\n");
            writer.write("\n");
            writer.write("IF NOT EXIST forge.jar (\n");
            writer.write("\n");
            writer.write("  ECHO Forge Server JAR-file not found. Downloading installer...\n");
            writer.write("  powershell -Command \"(New-Object Net.WebClient).DownloadFile('https://files.minecraftforge.net/maven/net/minecraftforge/forge/%MINECRAFT%-%FORGE%/forge-%MINECRAFT%-%FORGE%-installer.jar', 'forge-installer.jar')\"\n");
            writer.write("\n");
            writer.write("  IF EXIST forge-installer.jar (\n");
            writer.write("\n");
            writer.write("    ECHO Installer downloaded. Installing...\n");
            writer.write("    java -jar forge-installer.jar --installServer\n");
            writer.write("    MOVE forge-%MINECRAFT%-%FORGE%.jar forge.jar\n");
            writer.write("\n");
            writer.write("    IF EXIST forge.jar (\n");
            writer.write("      DEL forge-installer.jar\n");
            writer.write("      ECHO Installation complete. forge-installer.jar deleted.\n");
            writer.write("    )\n");
            writer.write("\n");
            writer.write("  ) ELSE (\n");
            writer.write("    ECHO forge-installer.jar not found. Maybe the Forges servers are having trouble.\n");
            writer.write("    ECHO Please try again in a couple of minutes.\n");
            writer.write("  )\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO forge.jar present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("IF NOT EXIST minecraft_server.%MINECRAFT%.jar (\n");
            writer.write("  ECHO Minecraft Server JAR-file not found. Downloading...\n");
            writer.write("  powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + VERSIONMETA.minecraft().getServer(minecraftVersion).get().url() + "', 'minecraft_server.%MINECRAFT%.jar')\"\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO minecraft_server.%MINECRAFT%.jar present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("IF NOT EXIST eula.txt (\n");
            writer.write("  ECHO Mojang's EULA has not yet been accepted. In order to run a Minecraft server, you must accept Mojang's EULA.\n");
            writer.write("  ECHO Mojang's EULA is available to read at https://account.mojang.com/documents/minecraft_eula\n");
            writer.write("  ECHO If you agree to Mojang's EULA then type \"I agree\"\n");
            writer.write("  set /P \"Response=\"\n");
            writer.write("  IF \"%Response%\" == \"%AGREE%\" (\n");
            writer.write("    ECHO User agreed to Mojang's EULA.\n");
            writer.write("    ECHO #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://account.mojang.com/documents/minecraft_eula^).> eula.txt\n");
            writer.write("    ECHO eula=true>> eula.txt\n");
            writer.write("  ) else (\n");
            writer.write("    ECHO User did not agree to Mojang's EULA. \n");
            writer.write("  )\n");
            writer.write(") ELSE (\n");
            writer.write("  ECHO eula.txt present. Moving on...\n");
            writer.write(")\n");
            writer.write("\n");
            writer.write("ECHO Starting server...\n");
            writer.write("ECHO Minecraft version: %MINECRAFT%\n");
            writer.write("ECHO Forge version: %FORGE%\n");
            writer.write("ECHO Java version:\n");
            // Batch, Windows, we can use --version
            writer.write("%JAVA% --version\n");
            writer.write("ECHO Java args: %ARGS%\n");
            writer.write("\n");
            writer.write("%JAVA% \"%OTHERARGS%\" %ARGS% -jar forge.jar nogui\n");
            writer.write("\n");
            writer.write("PAUSE");

        } catch (IOException ex) {
            LOG.error("Error generating batch-script for Forge.", ex);
        }
    }

    /**
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack directory into the
     * server pack directory.
     * If a <code>source/file;destination/file</code>-combination is provided, the specified source-file is copied to
     * the specified destination-file.
     * Calls {@link #excludeClientMods(String, List, String, String)} to generate a list of all mods to copy to server pack, excluding
     * clientside-only mods.
     * @author Griefed
     * @param modpackDir String. Files and directories are copied into the server_pack directory inside the modpack directory.
     * @param directoriesToCopy String List. All directories and files therein to copy to the server pack.
     * @param clientMods String List. List of clientside-only mods to exclude from the server pack.
     * @param minecraftVersion String. The Minecraft version the modpack uses.
     * @param destination String. The destination where the files should be copied to.
     */
    private void copyFiles(String modpackDir, List<String> directoriesToCopy, List<String> clientMods, String minecraftVersion, String destination, String modloader) {

        try {

            Files.createDirectories(Paths.get(destination));

        } catch (IOException ex) {

            LOG.error(String.format("Failed to create directory %s", destination));
        }

        if (directoriesToCopy.size() == 1 && directoriesToCopy.get(0).equals("lazy_mode")) {

            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode0"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode3"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode0"));

            try {

                FileUtils.copyDirectory(
                        new File(modpackDir),
                        new File(destination)
                );

            } catch (IOException ex) {
                LOG.error("An error occurred copying the modpack to the server pack in lazy mode.", ex);
            }

        } else {

            List<String> exclusions = APPLICATIONPROPERTIES.getListOfDirectoriesToExclude();
            directoriesToCopy.forEach(entry -> {
                if (entry.startsWith("!")) {
                    exclusions.add(entry.substring(1));
                }
            });
            directoriesToCopy.removeIf(n -> n.startsWith("!"));

            for (String directory : directoriesToCopy) {

                String clientDir = String.format("%s/%s", modpackDir, directory);
                String serverDir = String.format("%s/%s", destination, directory);

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyfiles.setup"), directory));

                /*
                 * Check for semicolon. If a semicolon is found, it means a user specified a source/path/to_file.foo;destination/path/to_file.bar-combination
                 * for a file they specifically want to include in their server pack.
                 */
                if (directory.contains(";")) {

                    String[] sourceFileDestinationFileCombination = directory.split(";");

                    if (new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])).isFile()) {

                        try {
                            FileUtils.copyFile(
                                    new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])),
                                    new File(String.format("%s/%s", destination, sourceFileDestinationFileCombination[1])),
                                    REPLACE_EXISTING
                            );
                        } catch (IOException ex) {
                            LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                        }

                    } else if (new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])).isDirectory()){

                        try {
                            FileUtils.copyDirectory(
                                    new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])),
                                    new File(String.format("%s/%s", destination, sourceFileDestinationFileCombination[1]))
                            );
                        } catch (Exception ex) {
                            LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                        }

                    } else if(new File(sourceFileDestinationFileCombination[0]).isFile()) {

                        try {
                            FileUtils.copyFile(
                                    new File(sourceFileDestinationFileCombination[0]),
                                    new File(String.format("%s/%s", destination, sourceFileDestinationFileCombination[1])),
                                    REPLACE_EXISTING
                            );
                        } catch (IOException ex) {
                            LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                        }

                    } else if (new File(sourceFileDestinationFileCombination[0]).isDirectory()) {

                        try {
                            FileUtils.copyDirectory(
                                    new File(sourceFileDestinationFileCombination[0]),
                                    new File(String.format("%s/%s", destination, sourceFileDestinationFileCombination[1]))
                            );
                        } catch (Exception ex) {
                            LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                        }

                    }

                    /*
                     * Check whether the entry starts with saves, and if it does, change the destination path to NOT include
                     * saves in it, so when a world is specified inside the saves-directory, it is copied to the base-directory
                     * of the server pack, instead of a saves-directory inside the modpack.
                     */
                } else if (directory.startsWith("saves/")) {

                    try {

                        FileUtils.copyDirectory(
                                new File(clientDir),
                                new File(String.format("%s/%s", destination, directory.substring(6))));

                    } catch (IOException ex) {
                        LOG.error("An error occurred copying the specified world.", ex);
                    }

                    /*
                     * If the entry starts with mods, we need to run our checks for clientside-only mods as well as exclude any
                     * user-specified clientside-only mods from the list of mods in the mods-directory.
                     */
                } else if (directory.startsWith("mods")) {

                    List<String> listOfFiles = excludeClientMods(clientDir, clientMods, minecraftVersion, modloader);

                    try {
                        Files.createDirectories(Paths.get(serverDir));
                    } catch (IOException ignored) {

                    }

                    for (String file : listOfFiles) {

                        if (excludeFileOrDirectory(file.replace("\\","/"), exclusions)) {

                            LOG.info("Excluding " + file + " from server pack");

                        } else {

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

                    }

                } else if (new File(directory).isFile() && !new File(directory).isDirectory()) {

                    File sourceFile = new File(String.format("%s/%s", modpackDir, directory));
                    File destinationFile = new File(String.format("%s/%s", destination, directory));

                    try {
                        FileUtils.copyFile(sourceFile, destinationFile, REPLACE_EXISTING);
                    } catch (IOException ex) {
                        LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                    }

                } else {

                    try (Stream<Path> files = Files.walk(Paths.get(clientDir))) {

                        files.forEach(file -> {
                            if (excludeFileOrDirectory(file.toString().replace("\\","/"), exclusions)) {

                                LOG.info("Excluding " + file + " from server pack");

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

                    } catch (IOException ex) {
                        LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
                    }

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
    private List<String> excludeClientMods(String modsDir, List<String> userSpecifiedClientMods, String minecraftVersion, String modloader) {
        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.excludeclientmods"));

        Collection<File> filesInModsDir = FileUtils.listFiles(
                new File(modsDir),
                new String[] {"jar"} ,
                true
        );

        List<String> modsInModpack = new ArrayList<>();
        List<String> autodiscoveredClientMods = new ArrayList<>();

        // Check whether scanning mods for sideness is activated.
        if (APPLICATIONPROPERTIES.getProperty("de.griefed.serverpackcreator.serverpack.autodiscoverenabled").equals("true") && filesInModsDir != null) {

            String[] split = minecraftVersion.split("\\.");

            STOPWATCH.reset();
            STOPWATCH.start();
            // If Minecraft version is 1.12 or newer, scan Tomls, else scan annotations.
            if (modloader.equalsIgnoreCase("Forge")) {

                if (Integer.parseInt(split[1]) > 12) {
                    autodiscoveredClientMods.addAll(scanTomls(filesInModsDir));
                } else {
                    autodiscoveredClientMods.addAll(scanAnnotations(filesInModsDir));
                }

            } else {

                autodiscoveredClientMods.addAll(scanFabricModJson(filesInModsDir));

            }

            STOPWATCH.stop();
            LOG.debug("Scanning of " + filesInModsDir.size() + " mods took " + STOPWATCH);
            STOPWATCH.reset();

        }

        // Gather a list of all mod-JAR-files.
        if (filesInModsDir != null) {

            for (File mod : filesInModsDir) {

                if (mod.isFile() && mod.toString().endsWith("jar")) {
                    modsInModpack.add(mod.getAbsolutePath());
                }
            }

        }


        // Exclude user-specified mods from copying.
        if (userSpecifiedClientMods.size() > 0) {
            for (int m = 0; m < userSpecifiedClientMods.size(); m++) {

                int i = m;

                if (modsInModpack.removeIf(n -> (n.contains(userSpecifiedClientMods.get(i))))) {
                    LOG.debug("Removed user-specified mod from mods list as per input: " + userSpecifiedClientMods.get(i));
                }

            }
        }

        // Exclude scanned mods from copying if said functionality is enabled.
        if (APPLICATIONPROPERTIES.getProperty("de.griefed.serverpackcreator.serverpack.autodiscoverenabled").equals("true") && autodiscoveredClientMods.size() > 0) {
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
    private boolean excludeFileOrDirectory(String fileToCheckFor, List<String> exclusions) {
        boolean isPresentInList = false;
        for (String entry : exclusions) {
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
     * @param destination String. The destination where the icon should be copied to.
     * @param pathToServerIcon String. The path to the custom server-icon.
     */
    private void copyIcon(String destination, String pathToServerIcon) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyicon"));

        File iconFile = new File(String.format("%s/%s", destination, APPLICATIONPROPERTIES.FILE_SERVER_ICON));

        if (new File(pathToServerIcon).exists()) {

            BufferedImage originalImage = null;
            //noinspection UnusedAssignment
            Image scaledImage = null;

            try {
                originalImage = ImageIO.read(new File(pathToServerIcon));
            } catch (IOException ex) {
                LOG.error("Error reading server-icon image.", ex);
            }

            if (originalImage != null) {

                // Scale our image to 64x64
                scaledImage = originalImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                BufferedImage outputImage = new BufferedImage(scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                outputImage.getGraphics().drawImage(scaledImage, 0, 0, null);


                // Save our scaled image to disk.
                try {
                    ImageIO.write(outputImage, "png", iconFile);
                } catch (IOException ex) {
                    LOG.error("Error scaling image.", ex);
                    LOG.error("Using default icon as fallback.");

                    try {

                        FileUtils.copyFile(
                                new File(String.format("server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_ICON)),
                                iconFile
                        );

                    } catch (IOException e) {
                        LOG.error("An error occurred trying to copy the server-icon.", e);
                    }
                }
            } else {
                LOG.error("originalImage is null. Check the source file. Was it wrongly converted to it's current file-format? Is it malformed? Corrupted?");
            }

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.icon"));

            try {

                FileUtils.copyFile(
                        new File(String.format("server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_ICON)),
                        iconFile
                );

            } catch (IOException ex) {
                LOG.error("An error occurred trying to copy the server-icon.", ex);
            }
        }


    }

    /**
     * Copies the server.properties into server_pack.
     * @author Griefed
     * @param destination String. The destination where the properties should be copied to.
     * @param pathToServerProperties String. The path to the custom server.properties.
     */
    private void copyProperties(String destination, String pathToServerProperties) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyproperties"));

        File defaultProperties = new File(String.format("%s/%s",destination, APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES));

        if (new File(pathToServerProperties).exists()) {
            try {

                FileUtils.copyFile(
                        new File(pathToServerProperties),
                        defaultProperties
                );

            } catch (IOException ex) {
                LOG.error("An error occurred trying to copy the server.properties-file.", ex);
            }
        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.properties"));

            try {

                FileUtils.copyFile(
                        new File(String.format("server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES)),
                        defaultProperties
                );

            } catch (IOException ex) {
                LOG.error("An error occurred trying to copy the server.properties-file.", ex);
            }
        }
    }

    /**
     * Installs the modloader server for the specified modloader, modloader version and Minecraft version.
     * @author Griefed
     * @param modLoader String. The modloader for which to install the server software. Either Forge or Fabric.
     * @param minecraftVersion String. The Minecraft version for which to install the modloader and Minecraft server.
     * @param modLoaderVersion String. The modloader version for which to install the modloader and Minecraft server.
     * @param javaPath String. The path to the Java executable/binary which is needed to execute the Forge/Fabric installersList.
     * @param destination String. The destination where the modloader server should be installed into.
     */
    private void installServer(String modLoader, String minecraftVersion, String modLoaderVersion, String javaPath, String destination) {

        List<String> commandArguments = new ArrayList<>();

        Process process = null;
        BufferedReader bufferedReader = null;
        String fileDestination;

        if (modLoader.equalsIgnoreCase("Fabric")) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.enter"));

            fileDestination = String.format("%s/fabric-installer.jar", destination);

            if (UTILITIES.WebUtils().downloadFile(fileDestination, VERSIONMETA.fabric().releaseInstallerUrl())) {
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

            } else {

                LOG.error("Something went wrong during the installation of Fabric. Maybe the Fabric server are down or unreachable? Skipping...");
            }

        } else if (modLoader.equalsIgnoreCase("Forge")) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.enter"));

            fileDestination = String.format("%s/forge-installer.jar", destination);

            if (VERSIONMETA.forge().getForgeInstance(minecraftVersion, modLoaderVersion).isPresent() &&
                    UTILITIES.WebUtils().downloadFile(fileDestination, VERSIONMETA.forge().getForgeInstance(minecraftVersion, modLoaderVersion).get().installerUrl())) {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.download"));
                commandArguments.add(javaPath);
                commandArguments.add("-jar");
                commandArguments.add("forge-installer.jar");
                commandArguments.add("--installServer");

            } else {

                LOG.error("Something went wrong during the installation of Forge. Maybe the Forge servers are down or unreachable? Skipping...");
            }

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"), modLoader));
        }

        try {
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.enter"), minecraftVersion, modLoader, modLoaderVersion));

            ProcessBuilder processBuilder = new ProcessBuilder(commandArguments).directory(new File(destination));

            LOG.debug("ProcessBuilder command: " + processBuilder.command());

            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while (true) {
                line = bufferedReader.readLine();
                if (line == null) { break; }
                LOG_INSTALLER.info(line);
            }

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG_INSTALLER.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"), minecraftVersion, modLoader, modLoaderVersion));

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"), minecraftVersion, modLoader, modLoaderVersion));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.details"));

        } catch (IOException ex) {

            LOG.error("Something went wrong during the installation of Forge. Maybe the Forge servers are down or unreachable? Skipping...",ex);

        } finally {

            try {
                //noinspection ConstantConditions
                bufferedReader.close();
            } catch (Exception ignored) {

            }

            try {
                //noinspection ConstantConditions
                process.destroy();
            } catch (Exception ignored) {

            }

            commandArguments.clear();
        }

        if (APPLICATIONPROPERTIES.getProperty("de.griefed.serverpackcreator.serverpack.cleanup.enabled").equalsIgnoreCase("true")) {
            cleanUpServerPack(
                    new File(String.format("%s/fabric-installer.jar", destination)),
                    new File(String.format("%s/forge-installer.jar", destination)),
                    modLoader,
                    minecraftVersion,
                    modLoaderVersion,
                    destination);
        } else {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanup"));
        }
    }

    /**
     * Creates a ZIP-archive of the server_pack directory excluding the Minecraft server JAR.<p>
     * @author Griefed
     * @param minecraftVersion String. Determines the name of the Minecraft server JAR to exclude from the ZIP-archive if the modloader is Forge.
     * @param includeServerInstallation Boolean. Determines whether the Minecraft server JAR info should be printed.
     * @param destination String. The destination where the ZIP-archive should be created in.
     */
    public void zipBuilder(String minecraftVersion, boolean includeServerInstallation, String destination) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.zipbuilder.enter"));

        List<File> filesToExclude = new ArrayList<>(Arrays.asList(
                new File(String.format("%s/minecraft_server.%s.jar", destination, minecraftVersion)),
                new File(String.format("%s/server.jar", destination)),
                new File(String.format("%s/libraries/net/minecraft/server/%s/server-%s.jar", destination, minecraftVersion, minecraftVersion))
        ));

        ExcludeFileFilter excludeFileFilter = filesToExclude::contains;

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setExcludeFileFilter(excludeFileFilter);
        zipParameters.setIncludeRootFolder(false);

        zipParameters.setFileComment("Server pack made with ServerPackCreator by Griefed.");

        try (ZipFile zip = new ZipFile(String.format("%s_server_pack.zip", destination))) {

            zip.addFolder(new File(destination), zipParameters);

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
     * Cleans up the server_pack directory by deleting left-over files from modloader installations and version checking.
     * @author Griefed
     * @param fabricInstaller File. The Fabric installer file which is to be deleted.
     * @param forgeInstaller File. The Forge installer file which is to be deleted.
     * @param modLoader String. Whether Forge or Fabric files are to be deleted.
     * @param minecraftVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     * @param modLoaderVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     * @param destination String. The destination where we should clean up in.
     */
    private void cleanUpServerPack(File fabricInstaller, File forgeInstaller, String modLoader, String minecraftVersion, String modLoaderVersion, String destination) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.enter"));

        if (modLoader.equalsIgnoreCase("Fabric")) {

            try {
                if (Files.deleteIfExists(fabricInstaller.toPath())) {

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.deleted"), fabricInstaller.getName()));

                } else {

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupserverpack.delete"), fabricInstaller.getName()));

                }
            } catch (IOException ex) {
                LOG.error("Couldn't delete Fabric installer.");
            }

        } else if (modLoader.equalsIgnoreCase("Forge")) {

            String[] minecraft = minecraftVersion.split("\\.");

            try {
                if (Files.deleteIfExists(forgeInstaller.toPath())) {

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.deleted"), forgeInstaller.getName()));

                } else {

                    LOG.error(String.format("Could not delete %s.", forgeInstaller.getName()));
                }

            } catch (IOException ex) {
                LOG.error("Couldn't delete Forge installer.");
            }

            try {
                if (Files.deleteIfExists(Paths.get(String.format("%s/installer.log", destination)))) {

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.forgelog"));

                } else if (Files.deleteIfExists(Paths.get(String.format("%s/forge-installer.jar.log", destination)))) {

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.forgelog"));

                }
            } catch (IOException ex) {
                LOG.error("Couldn't delete Forge server installation log.");
            }

            if (Integer.parseInt(minecraft[1]) < 17) {

                Path path = Paths.get(String.format("%s/forge-%s-%s.jar", destination, minecraftVersion, modLoaderVersion));

                try {

                    Files.copy(
                            path,
                            Paths.get(String.format("%s/forge.jar", destination)),
                            REPLACE_EXISTING);

                } catch (IOException ex) {
                    LOG.error("Error during Forge cleanup.", ex);
                }

                try {
                    if (Files.deleteIfExists(path) &&
                            (new File(String.format("%s/forge.jar", destination)).exists())) {

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.rename"));

                    } else {
                        LOG.error("There was an error during renaming or deletion of the forge server jar.");
                    }
                } catch(IOException ex) {
                    LOG.error("Couldn't delete old Forge jar.");
                }

            } else {

                try {
                    if (Files.deleteIfExists(Paths.get(String.format("%s/run.bat", destination)))) {
                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.bat.delete"));
                    } else {
                        LOG.error("Could not delete run.bat.");
                    }
                } catch (IOException ex) {
                    LOG.error("An error occurred during the deletion of run.bat.", ex);
                }

                try {
                    if (Files.deleteIfExists(Paths.get(String.format("%s/run.sh", destination)))) {
                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.sh.delete"));
                    } else {
                        LOG.error("Could not delete run.sh.");
                    }
                } catch (IOException ex) {
                    LOG.error("An error occurred during the deletion of run.sh.", ex);
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
     * @param filesInModsDir A list of files in which to check the <code>mods.toml</code>-files.
     * @return List String. List of mods not to include in server pack based on mods.toml-configuration.
     */
    private List<String> scanTomls(Collection<File> filesInModsDir) {

        /*
         * Can I just say:
         * WHAT THE EVERLOVING FUCK IS THIS METHOD? try catch if try catch if else try catch what the actual fucking fuck?
         */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.scantoml"));

        List<String> serverMods = new ArrayList<>();
        List<String> modsDelta = new ArrayList<>();

        for (File mod : filesInModsDir) {
            if (mod.toString().endsWith("jar")) {

                JarFile jarFile = null;
                JarEntry jarEntry = null;
                InputStream inputStream = null;

                try {
                    jarFile = new JarFile(mod);
                    jarEntry = jarFile.getJarEntry("META-INF/mods.toml");
                    inputStream = jarFile.getInputStream(jarEntry);
                } catch (Exception ex) {
                    LOG.error("Can not scan " + mod);
                }

                try {

                    if (inputStream != null) {

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

                    }

                } catch (Exception ex) {

                    LOG.error("Error acquiring sideness from mod " + mod,ex);

                } finally {

                    try {
                        //noinspection ConstantConditions
                        jarFile.close();
                    } catch (Exception ignored) {

                    }

                    try {
                        //noinspection ConstantConditions
                        inputStream.close();
                    } catch (Exception ignored) {

                    }

                    jarEntry = null;

                }

            }
        }

        for (File mod : filesInModsDir) {

            String modToCheck = mod.toString().replace("\\", "/");

            boolean addToDelta = true;

            JarFile jarFile = null;
            JarEntry jarEntry = null;
            InputStream inputStream = null;

            try {
                jarFile = new JarFile(mod);
                jarEntry = jarFile.getJarEntry("META-INF/mods.toml");
                inputStream = jarFile.getInputStream(jarEntry);
            } catch (Exception ex) {
                LOG.error("Can not scan " + mod);
            }

            try {

                if (inputStream != null) {

                    Toml modToml = new Toml().read(inputStream);

                    for (String modId : serverMods) {

                        if (modToml.getString("mods[0].modId").toLowerCase().matches(modId)) {
                            addToDelta = false;
                        }

                    }

                    if (addToDelta) {
                        modsDelta.add(modToCheck);
                    }

                }

            } catch (Exception ex) {

                LOG.error("Couldn't acquire modId for mod " + mod,ex);

            } finally {

                try {
                    //noinspection ConstantConditions
                    jarFile.close();
                } catch (Exception ignored) {

                }

                try {
                    //noinspection ConstantConditions
                    inputStream.close();
                } catch (Exception ignored) {

                }

                jarEntry = null;

            }

        }

        return modsDelta;
    }

    /**
     * Scan the <code>fml-cache-annotation.json</code>-files in mod JAR-files of a given directory for their sideness.<br>
     * If <code>clientSideOnly</code> specifies <code>"value": "true"</code>, and is not listed as a dependency
     * for another mod, it is added and therefore later on excluded from the server pack.
     * @author Griefed
     * @param filesInModsDir A list of files in which to check the <code>fml-cache-annotation.json</code>-files.
     * @return List String. List of mods not to include in server pack based on fml-cache-annotation.json-content.
     */
    private List<String> scanAnnotations(Collection<File> filesInModsDir) {

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.scanannotation"));

        List<String> modDependencies = new ArrayList<>();
        List<String> clientMods = new ArrayList<>();
        List<String> modsDelta = new ArrayList<>();

        for (File mod : filesInModsDir) {
            if (mod.toString().endsWith("jar")) {

                String modId = null;

                JarFile jarFile = null;
                JarEntry jarEntry = null;
                InputStream inputStream = null;

                try {
                    jarFile = new JarFile(mod);
                    jarEntry = jarFile.getJarEntry("META-INF/fml_cache_annotation.json");
                    inputStream = jarFile.getInputStream(jarEntry);
                } catch (Exception ex) {
                    LOG.error("Can not scan " + mod);
                }

                try {

                    if (inputStream != null) {

                        JsonNode modJson = getObjectMapper().readTree(inputStream);

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
                                    } catch (NullPointerException ignored) {

                                    }

                                    // Add mod to list of clientmods if clientSideOnly is true
                                    try {
                                        if (child.get("values").get("clientSideOnly").get("value").asText().equalsIgnoreCase("true")) {
                                            if (!clientMods.contains(modId)) {
                                                clientMods.add(modId);

                                                LOG.debug("Added clientMod: " + modId);
                                            }
                                        }
                                    } catch (NullPointerException ignored) {

                                    }

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
                                    } catch (NullPointerException ignored) {

                                    }

                                }

                            } catch (NullPointerException ignored) {

                            }

                        }

                    }

                } catch(IOException ex) {

                    LOG.error("Couldn't acquire sideness for mod " + mod,ex);

                } finally {

                    try {
                        //noinspection ConstantConditions
                        jarFile.close();
                    } catch (Exception ignored) {

                    }

                    try {
                        //noinspection ConstantConditions
                        inputStream.close();
                    } catch (Exception ignored) {

                    }

                    jarEntry = null;

                }

            }

        }

        //Remove dependencies from list of clientmods to ensure we do not, well, exclude a dependency of another mod.
        for (String dependency : modDependencies) {

            clientMods.removeIf(n -> (n.contains(dependency)));
            LOG.debug("Removing " + dependency + " from list of clientmods as it is a dependency for another mod.");
        }

        //After removing dependencies from the list of potential clientside mods, we can remove any mod that says it is clientside-only.
        for (File mod : filesInModsDir) {

            String modToCheck = mod.toString().replace("\\", "/");
            String modIdTocheck = null;

            boolean addToDelta = false;

            JarFile jarFile = null;
            JarEntry jarEntry = null;
            InputStream inputStream = null;

            try {
                jarFile = new JarFile(mod);
                jarEntry = jarFile.getJarEntry("META-INF/fml_cache_annotation.json");
                inputStream = jarFile.getInputStream(jarEntry);
            } catch (Exception ex) {
                LOG.error("Can not scan " + mod);
            }

            try {

                if (inputStream != null) {

                    JsonNode modJson = getObjectMapper().readTree(inputStream);

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
                                } catch (NullPointerException ignored) {

                                }

                                // Add mod to list of clientmods if clientSideOnly is true
                                try {
                                    if (child.get("values").get("clientSideOnly").get("value").asText().equalsIgnoreCase("true")) {
                                        if (clientMods.contains(modIdTocheck)) {
                                            addToDelta = true;
                                        }
                                    }
                                } catch (NullPointerException ignored) {

                                }

                            }
                        } catch (NullPointerException ignored) {

                        }

                    }

                    if (addToDelta) {
                        modsDelta.add(modToCheck);

                    }

                }


            } catch (Exception ex) {

                LOG.error("Couldn't acquire modId for mod " + mod,ex);

            } finally {

                try {
                    //noinspection ConstantConditions
                    jarFile.close();
                } catch (Exception ignored) {

                }

                try {
                    //noinspection ConstantConditions
                    inputStream.close();
                } catch (Exception ignored) {

                }

                jarEntry = null;

            }
        }

        return modsDelta;
    }

    /**
     * Scan the <code>fabric.mod.json</code>-files in mod JAR-files of a given directory for their sideness.<br>
     * If <code>environment</code> specifies <code>client</code>, and is not listed as a dependency
     * for another mod, it is added and therefore later on excluded from the server pack.
     * @author Griefed
     * @param filesInModsDir A list of files in which to check the <code>fabric.mod.json</code>-files.
     * @return List String. List of mods not to include in server pack based on fabric.mod.json-content.
     */
    private List<String> scanFabricModJson(Collection<File> filesInModsDir) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.scanfabricmodjson"));

        List<String> modDependencies = new ArrayList<>();
        List<String> clientMods = new ArrayList<>();
        List<String> modsDelta = new ArrayList<>();


        for (File mod : filesInModsDir) {
            if (mod.toString().endsWith("jar")) {

                String modId = null;

                JarFile jarFile = null;
                JarEntry jarEntry = null;
                InputStream inputStream = null;

                try {
                    jarFile = new JarFile(mod);
                    jarEntry = jarFile.getJarEntry("fabric.mod.json");
                    inputStream = jarFile.getInputStream(jarEntry);
                } catch (Exception ex) {
                    LOG.error("Can not scan " + mod);
                }

                try {

                    if (inputStream != null) {

                        JsonNode modJson = getObjectMapper().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature()).readTree(inputStream);

                        modId = modJson.get("id").asText();

                        //Get this mods id/name
                        try {
                            if (modJson.get("environment").asText().equalsIgnoreCase("client")) {
                                if (!clientMods.contains(modId)) {
                                    clientMods.add(modId);

                                    LOG.debug("Added clientMod: " + modId);
                                }
                            }
                        } catch (NullPointerException ignored) {

                        }

                        //Get this mods dependencies
                        try {
                            modJson.get("depends").fieldNames().forEachRemaining(dependency -> {
                                if (!modDependencies.contains(dependency)) {
                                    modDependencies.add(dependency);
                                }
                            });
                        } catch (NullPointerException ignored) {

                        }

                    }

                } catch(IOException ex) {

                    LOG.error("Couldn't acquire sideness for mod " + mod,ex);

                } finally {

                    try {
                        //noinspection ConstantConditions
                        jarFile.close();
                    } catch (Exception ignored) {

                    }

                    try {
                        //noinspection ConstantConditions
                        inputStream.close();
                    } catch (Exception ignored) {

                    }

                    jarEntry = null;

                }

            }

        }

        //Remove dependencies from list of clientmods to ensure we do not, well, exclude a dependency of another mod.
        for (String dependency : modDependencies) {

            clientMods.removeIf(n -> (n.contains(dependency)));
            LOG.debug("Removing " + dependency + " from list of clientmods as it is a dependency for another mod.");
        }

        //After removing dependencies from the list of potential clientside mods, we can remove any mod that says it is clientside-only.
        for (File mod : filesInModsDir) {

            String modToCheck = mod.toString().replace("\\", "/");
            String modIdTocheck = null;

            boolean addToDelta = false;

            JarFile jarFile = null;
            JarEntry jarEntry = null;
            InputStream inputStream = null;

            try {
                jarFile = new JarFile(mod);
                jarEntry = jarFile.getJarEntry("fabric.mod.json");
                inputStream = jarFile.getInputStream(jarEntry);
            } catch (Exception ex) {
                LOG.error("Can not scan " + mod);
            }

            try {

                if (inputStream != null) {

                    JsonNode modJson = getObjectMapper().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature()).readTree(inputStream);

                    // Get the modId
                    modIdTocheck = modJson.get("id").asText();

                    try {
                        if (modJson.get("environment").asText().equalsIgnoreCase("client")) {
                            if (clientMods.contains(modIdTocheck)) {
                                addToDelta = true;
                            }
                        }
                    } catch (NullPointerException ignored) {

                    }

                    if (addToDelta) {
                        modsDelta.add(modToCheck);
                    }

                }


            } catch (Exception ex) {

                LOG.error("Couldn't acquire modId for mod " + mod,ex);

            } finally {

                try {
                    //noinspection ConstantConditions
                    jarFile.close();
                } catch (Exception ignored) {

                }

                try {
                    //noinspection ConstantConditions
                    inputStream.close();
                } catch (Exception ignored) {

                }

                jarEntry = null;

            }
        }

        return modsDelta;
    }
}
