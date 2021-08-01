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

import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import net.fabricmc.installer.util.LauncherMeta;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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
 * 1. {@link #CreateServerPack(LocalizationManager, CurseCreateModpack, AddonsHandler)}<br>
 * 2. {@link #getPropertiesFile()}<br>
 * 3. {@link #getIconFile()}<br>
 * 4. {@link #getForgeWindowsFile()}<br>
 * 5. {@link #getForgeLinuxFile()}<br>
 * 6. {@link #getFabricWindowsFile()}<br>
 * 7. {@link #getFabricLinuxFile()}<br>
 * 9. {@link #run(File)}<br>
 * 10.{@link #cleanupEnvironment(String)}<br>
 * 11.{@link #copyStartScripts(String, String, boolean)}<br>
 * 12.{@link #copyFiles(String, List, List)}<br>
 * 13.{@link #excludeClientMods(String, List)}<br>
 * 14.{@link #copyIcon(String)}<br>
 * 15.{@link #copyProperties(String)}<br>
 * 16.{@link #installServer(String, String, String, String, String)}<br>
 * 17.{@link #zipBuilder(String, String, boolean)}<br>
 * 18.{@link #generateDownloadScripts(String, String, String)}<br>
 * 19.{@link #fabricShell(String, String)}<br>
 * 20.{@link #fabricBatch(String, String)}<br>
 * 21.{@link #forgeShell(String, String)}<br>
 * 22.{@link #forgeBatch(String, String)}<br>
 * 23.{@link #downloadFabricJar(String)}<br>
 * 24.{@link #latestFabricInstaller()}<br>
 * 25.{@link #downloadForgeJar(String, String, String)}<br>
 * 27.{@link #cleanUpServerPack(File, File, String, String, String, String)}
 * <p>
 * Requires an instance of {@link Configuration} from which to get all required information about the modpack and the
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
public class CreateServerPack {
    private static final Logger LOG = LogManager.getLogger(DefaultFiles.class);
    private static final Logger LOG_INSTALLER = LogManager.getLogger("InstallerLogger");

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final AddonsHandler ADDONSHANDLER;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} required for creating a modpack from CurseForge.
     * @param injectedAddonsHandler Instance of {@link AddonsHandler} required for accessing installed addons, if any exist.
     */
    @Autowired
    public CreateServerPack(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack, AddonsHandler injectedAddonsHandler) {

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedAddonsHandler == null) {
            this.ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER);
        } else {
            this.ADDONSHANDLER = injectedAddonsHandler;
        }

    }

    private final File FILE_PROPERTIES = new File("server.properties");
    private final File FILE_ICON = new File("server-icon.png");
    private final File FILE_FORGE_WINDOWS = new File("start-forge.bat");
    private final File FILE_FORGE_LINUX = new File("start-forge.sh");
    private final File FILE_FABRIC_WINDOWS = new File("start-fabric.bat");
    private final File FILE_FABRIC_LINUX = new File("start-fabric.sh");

    /**
     * Getter for server.properties.
     * @author Griefed
     * @return Returns the server.properties-file for use in {@link #copyProperties(String)}
     */
    public File getPropertiesFile() {
        return FILE_PROPERTIES;
    }

    /**
     * Getter for server-icon.png
     * @author Griefed
     * @return Returns the server-icon.png-file for use in {@link #copyIcon(String)}
     */
    public File getIconFile() {
        return FILE_ICON;
    }

    /**
     * Getter for start-forge.bat.
     * @author Griefed
     * @return Returns the start-forge.bat-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getForgeWindowsFile() {
        return FILE_FORGE_WINDOWS;
    }

    /**
     * Getter for start-forge.sh
     * @author Griefed
     * @return Returns the start-forge.sh-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getForgeLinuxFile() {
        return FILE_FORGE_LINUX;
    }

    /**
     * Getter for start-fabric.bat.
     * @author Griefed
     * @return Returns the start-fabric.bat-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getFabricWindowsFile() {
        return FILE_FABRIC_WINDOWS;
    }

    /**
     * Getter for start-fabric.sh.
     * @author Griefed
     * @return Returns the start-fabric.sh-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getFabricLinuxFile() {
        return FILE_FABRIC_LINUX;
    }

    /**
     * Create a server pack if the check of the configuration file was successfull.<p>
     * Calls<br>
     * {@link #cleanupEnvironment(String)} to delete any previously generated server packs or ZIP-archives thereof.<br>
     * {@link #copyFiles(String, List, List)} to copy all specified directories and mods, excluding clientside-only mods,
     * to the server pack.<br>
     * {@link #copyStartScripts(String, String, boolean)} to copy the start scripts for the specified modloader to the
     * server pack.<br>
     * {@link #installServer(String, String, String, String, String)} to install the server software for the specified
     * modloader, modloader version and Minecraft version in the server pack.<br>
     * {@link #copyIcon(String)} to copy the server-icon.png to the server pack.<br>
     * {@link #copyProperties(String)} to copy the server.properties to the server pack.<br>
     * {@link #zipBuilder(String, String, boolean)} to create a ZIP-archive of the server pack.<br>
     * {@link AddonsHandler#runServerPackAddons(Configuration)} to run all valid server pack addons.
     * @author Griefed
     * @param configFileToUse A ServerPackCreator-configuration-file for {@link Configuration} to check and  generate a
     * server pack from.
     * @return Boolean. Returns true if the server pack was successfully generated.
     */
    @SuppressWarnings("UnusedAssignment")
    public boolean run(File configFileToUse) {
        // TODO: Once API and webUI are implemented, test parallel runs. Parallel runs MUST be possible.
        Configuration serverPackConfig = new Configuration(LOCALIZATIONMANAGER, CURSECREATEMODPACK);

        if (!serverPackConfig.checkConfigFile(configFileToUse, true)) {

            // Make sure no files from previously generated server packs interrupt us.
            cleanupEnvironment(serverPackConfig.getModpackDir());

            // Recursively copy all specified directories and files, excluding clientside-only mods, to server pack.
            copyFiles(serverPackConfig.getModpackDir(), serverPackConfig.getCopyDirs(), serverPackConfig.getClientMods());

            // Copy start scripts for specified modloader from server_files to server pack.
            copyStartScripts(serverPackConfig.getModpackDir(), serverPackConfig.getModLoader(), serverPackConfig.getIncludeStartScripts());

            // If true, Install the modloader software for the specified Minecraft version, modloader, modloader version
            if (serverPackConfig.getIncludeServerInstallation()) {
                installServer(serverPackConfig.getModLoader(), serverPackConfig.getModpackDir(), serverPackConfig.getMinecraftVersion(), serverPackConfig.getModLoaderVersion(), serverPackConfig.getJavaPath());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.server"));
            }

            // If true, copy the server-icon.png from server_files to the server pack.
            if (serverPackConfig.getIncludeServerIcon()) {
                copyIcon(serverPackConfig.getModpackDir());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.icon"));
            }

            // If true, copy the server.properties from server_files to the server pack.
            if (serverPackConfig.getIncludeServerProperties()) {
                copyProperties(serverPackConfig.getModpackDir());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.properties"));
            }

            // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
            if (serverPackConfig.getIncludeZipCreation()) {
                zipBuilder(serverPackConfig.getModpackDir(), serverPackConfig.getMinecraftVersion(), serverPackConfig.getIncludeServerInstallation());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.zip"));
            }

            // Inform user about location of newly generated server pack.
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.serverpack"), serverPackConfig.getModpackDir().substring(serverPackConfig.getModpackDir().lastIndexOf("/")+1)));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.archive"), serverPackConfig.getModpackDir().substring(serverPackConfig.getModpackDir().lastIndexOf("/")+1)));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.runincli.finish"));

            if (ADDONSHANDLER.getListOfServerPackAddons().isEmpty() || ADDONSHANDLER.getListOfServerPackAddons() == null) {
                LOG.info("No Server Pack addons to execute.");
            } else {
                LOG.info("Starting execution of Server Pack addons. Check addons.log in the logs-directory for details about their execution.");
                ADDONSHANDLER.runServerPackAddons(serverPackConfig);
                LOG.info("Addons executed. Finishing...");
            }

            serverPackConfig = null;
            return true;

        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("main.log.error.runincli"));

            serverPackConfig = null;
            return false;
        }
    }

    /**
     * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure newly generated
     * server pack is as clean as possible.
     * @author Griefed
     * @param modpackDir String. The server_pack directory and ZIP-archive will be deleted inside the modpack directory.
     */
    void cleanupEnvironment(String modpackDir) {
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        if (new File(String.format("server-packs/%s", destination)).exists()) {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.folder.enter"));
            Path serverPack = Paths.get(String.format("server-packs/%s", destination));

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

                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupenvironment.folder.delete"), modpackDir));

            } finally {

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.folder.complete"));
            }
        }

        if (new File(String.format("server-packs/%s_server_pack.zip", destination)).exists()) {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.zip.enter"));

            boolean isZipDeleted = new File(String.format("server-packs/%s_server_pack.zip", destination)).delete();

            if (isZipDeleted) {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupenvironment.zip.complete"));
            } else {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupenvironment.zip.delete"));
            }
        }
    }

    /**
     * Copies start scripts for the specified modloader into the server pack.
     * @author Griefed
     * @param modpackDir String. Start scripts are copied into the server_pack directory in the modpack directory.
     * @param modLoader String. Whether to copy the Forge or Fabric scripts into the server pack.
     * @param includeStartScripts Boolean. Whether to copy the start scripts into the server pack.
     */
    void copyStartScripts(String modpackDir, String modLoader, boolean includeStartScripts) {
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        if (modLoader.equalsIgnoreCase("Forge") && includeStartScripts) {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copystartscripts.forge"));

            try {

                Files.copy(
                        Paths.get(String.format("server_files/%s", getForgeWindowsFile())),
                        Paths.get(String.format("server-packs/%s/%s", destination, getForgeWindowsFile())),
                        REPLACE_EXISTING
                );

                Files.copy(
                        Paths.get(String.format("server_files/%s", getForgeLinuxFile())),
                        Paths.get(String.format("server-packs/%s/%s", destination, getForgeLinuxFile())),
                        REPLACE_EXISTING
                );

            } catch (IOException ex) {

                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copystartscripts"), ex);
            }

        } else if (modLoader.equalsIgnoreCase("Fabric") && includeStartScripts) {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copystartscripts.fabric"));

            try {

                Files.copy(
                        Paths.get(String.format("server_files/%s", getFabricWindowsFile())),
                        Paths.get(String.format("server-packs/%s/%s", destination, getFabricWindowsFile())),
                        REPLACE_EXISTING
                );

                Files.copy(
                        Paths.get(String.format("server_files/%s", getFabricLinuxFile())),
                        Paths.get(String.format("server-packs/%s/%s", destination, getFabricLinuxFile())),
                        REPLACE_EXISTING
                );

            } catch (IOException ex) {

                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copystartscripts"), ex);
            }

        } else {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"));
        }
    }

    /**
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack directory into the
     * server pack directory.
     * If a <code>source/file;destination/file</code>-combination is provided, the specified source-file is copied to
     * the specified destination-file.
     * Calls {@link #excludeClientMods(String, List)} to generate a list of all mods to copy to server pack, excluding
     * clientside-only mods.
     * @author Griefed
     * @param modpackDir String. Files and directories are copied into the server_pack directory inside the modpack directory.
     * @param directoriesToCopy String List. All directories and files therein to copy to the server pack.
     * @param clientMods String List. List of clientside-only mods to exclude from the server pack.
     */
    void copyFiles(String modpackDir, List<String> directoriesToCopy, List<String> clientMods) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        String serverPath = String.format("server-packs/%s", destination);

        try {

            Files.createDirectories(Paths.get(serverPath));

        } catch (IOException ex) {

            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("cursecreatemodpack.log.error.unziparchive.createdir"), serverPath));
        }

        for (String directory : directoriesToCopy) {

            String clientDir = String.format("%s/%s", modpackDir, directory);
            String serverDir = String.format("%s/%s", serverPath, directory);

            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyfiles.setup"), directory));

            if (directory.contains(";")) {

                String[] sourceFileDestinationFileCombination = directory.split(";");

                File sourceFile = new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));
                File destinationFile = new File(String.format("%s/%s", serverPath, sourceFileDestinationFileCombination[1]));

                try {
                    FileUtils.copyFile(sourceFile, destinationFile, REPLACE_EXISTING);
                } catch (IOException ex) {
                    LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyfiles"), ex));
                }


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

                            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.copyfiles"), file.toAbsolutePath()));

                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyfiles.saves"), ex);
                            }
                        }
                    });

                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyfiles.saves.world"), ex);
                }


            } else if (directory.startsWith("mods") && clientMods.size() > 0) {

                List<String> listOfFiles = excludeClientMods(clientDir, clientMods);

                try {
                    Files.createDirectories(Paths.get(serverDir));
                } catch (IOException ex) {
                    LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyfiles.setup"), serverDir));
                }

                for (String file : listOfFiles) {
                    try {

                        Files.copy(
                                Paths.get(file),
                                Paths.get(String.format("%s/%s", serverDir, new File(file).getName())),
                                REPLACE_EXISTING
                        );

                        LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.copyfiles"), file));

                    } catch (IOException ex) {
                        if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyfiles.mods"), ex);
                        }
                    }
                }


            } else {
                try {

                    Stream<Path> files = Files.walk(Paths.get(clientDir));

                    files.forEach(file -> {
                        try {

                            Files.copy(
                                    file,
                                    Paths.get(serverDir).resolve(Paths.get(clientDir).relativize(file)),
                                    REPLACE_EXISTING
                            );

                            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.copyfiles"), file.toAbsolutePath()));
                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyfiles.mods"), ex);
                            }
                        }
                    });

                    files.close();

                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyfiles"), ex);
                }
            }
        }
    }

    /**
     * Generates a list of all mods to include in the server pack excluding clientside-only mods.
     * @author Griefed
     * @param modsDir String. The mods-directory of the modpack of which to generate a list of all it's contents.
     * @param clientMods List String. A list of all clientside-only mods.
     * @return List String. A list of all mods to include in the server pack.
     */
    List<String> excludeClientMods(String modsDir, List<String> clientMods) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.excludeclientmods"));

        File[] listModsInModpack = new File(modsDir).listFiles();
        List<String> modsInModpack = new ArrayList<>();

        try {
            assert listModsInModpack != null;
            for (File mod : listModsInModpack) {
                if (mod.isFile()) {
                    modsInModpack.add(mod.getAbsolutePath());
                }
            }
        } catch (NullPointerException np) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.excludeclientmods"), np);
        }

        for (int iclient = 0; iclient < clientMods.size(); iclient++) {

            int i = iclient;

            modsInModpack.removeIf(n -> (n.contains(clientMods.get(i))));

        }

        return modsInModpack;
    }

    /**
     * Copies the server-icon.png into server_pack.
     * @author Griefed
     * @param modpackDir String. The server-icon.png is copied into the server_pack directory inside the modpack directory.
     */
    void copyIcon(String modpackDir) {

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyicon"));

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        try {

            Files.copy(
                    Paths.get(String.format("server_files/%s", getIconFile())),
                    Paths.get(String.format("server-packs/%s/%s", destination, getIconFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyicon"), ex);
        }
    }

    /**
     * Copies the server.properties into server_pack.
     * @author Griefed
     * @param modpackDir String. The server.properties file is copied into the server_pack directory inside the modpack directory.
     */
    void copyProperties(String modpackDir) {

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.copyproperties"));

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        try {

            Files.copy(
                    Paths.get(String.format("server_files/%s", getPropertiesFile())),
                    Paths.get(String.format("server-packs/%s/%s", destination, getPropertiesFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.copyproperties"), ex);
        }
    }

    /**
     * Installs the modloader server for the specified modloader, modloader version and Minecraft version.
     * Calls<p>
     * {@link #downloadFabricJar(String)} to download the Fabric installer into the server_pack directory.<p>
     * {@link #downloadForgeJar(String, String, String)} to download the Forge installer for the specified Forge version
     * and Minecraft version.<p>
     * {@link #generateDownloadScripts(String, String, String)} to generate the download scripts of the Minecraft server JAR
     * for the specified Minecraft version and file-name depending on whether the modloader is Forge or Fabric.<p>
     * {@link #cleanUpServerPack(File, File, String, String, String, String)} to delete no longer needed files generated
     * by the installation process of the modloader server software.
     * @author Griefed
     * @param modLoader String. The modloader for which to install the server software. Either Forge or Fabric.
     * @param modpackDir String. The server software is installed into the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to install the modloader and Minecraft server.
     * @param modLoaderVersion String. The modloader version for which to install the modloader and Minecraft server.
     * @param javaPath String. The path to the Java executable/binary which is needed to execute the Forge/Fabric installers.
     */
    void installServer(String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion, String javaPath) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        File fabricInstaller = new File(String.format("server-packs/%s/fabric-installer.jar", destination));

        File forgeInstaller = new File(String.format("server-packs/%s/forge-installer.jar", destination));

        List<String> commandArguments = new ArrayList<>();

        if (modLoader.equalsIgnoreCase("Fabric")) {
            try {

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.enter"));
                LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.enter"));

                if (downloadFabricJar(modpackDir)) {
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

                    ProcessBuilder processBuilder = new ProcessBuilder(commandArguments).directory(new File(String.format("server-packs/%s", destination)));

                    LOG.debug(processBuilder.command());

                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;

                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        LOG_INSTALLER.info(line);
                    }

                    LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                    reader.close();

                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.fabric.details"));
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                    process.destroy();
                } else {

                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.installserver.fabric"));
                }

            } catch (IOException ex) {

                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.installserver.fabricfail"), ex);
            }
        } else if (modLoader.equalsIgnoreCase("Forge")) {

            try {

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.enter"));
                LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.enter"));

                if (downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir)) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.download"));
                    commandArguments.add(javaPath);
                    commandArguments.add("-jar");
                    commandArguments.add("forge-installer.jar");
                    commandArguments.add("--installServer");

                    ProcessBuilder processBuilder = new ProcessBuilder(commandArguments).directory(new File(String.format("server-packs/%s", destination)));

                    LOG.debug(processBuilder.command());
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;

                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        LOG_INSTALLER.info(line);
                    }

                    LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                    reader.close();

                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver.forge.details"));
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.installserver"));

                    process.destroy();

                } else {

                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.installserver.forge"));
                }
            } catch (IOException ex) {

                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.installserver.forgefail"), ex);
            }
        } else {

            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"), modLoader));
        }

        commandArguments.clear();

        generateDownloadScripts(modLoader, modpackDir, minecraftVersion);

        cleanUpServerPack(
                fabricInstaller,
                forgeInstaller,
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);
    }

    /**
     * Creates a ZIP-archive of the server_pack directory excluding the Minecraft server JAR.<p>
     * @author Griefed
     * @param modpackDir String. The directory server_pack will be zipped and placed inside the modpack directory.
     * @param minecraftVersion String. Determines the name of the Minecraft server JAR to exclude from the ZIP-archive if the modloader is Forge.
     * @param includeServerInstallation Boolean. Determines whether the Minecraft server JAR info should be printed.
     */
    void zipBuilder(String modpackDir, String minecraftVersion, boolean includeServerInstallation) {

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.zipbuilder.enter"));

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        List<File> filesToExclude = new ArrayList<>(Arrays.asList(
                new File(String.format("server-packs/%s/minecraft_server.%s.jar", destination, minecraftVersion)),
                new File(String.format("server-packs/%s/server.jar", destination))
        ));

        ExcludeFileFilter excludeFileFilter = filesToExclude::contains;

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setExcludeFileFilter(excludeFileFilter);
        zipParameters.setIncludeRootFolder(false);

        zipParameters.setFileComment("Server pack made with ServerPackCreator by Griefed.");

        try {

            new ZipFile(String.format("server-packs/%s_server_pack.zip", destination)).addFolder(new File(String.format("server-packs/%s", destination)), zipParameters);

        } catch (IOException ex) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.zipbuilder.create"), ex);
        }

        if (includeServerInstallation) {

            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.warn.zipbuilder.minecraftjar1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.warn.zipbuilder.minecraftjar2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.warn.zipbuilder.minecraftjar3"));
        }

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.zipbuilder.finish"));
    }

    /**
     * Depending on the specified modloader and Minecraft version, this method makes calls to generate the corresponding
     * download scripts for the Minecraft server JAR.<p>
     * Calls<p>
     * {@link #fabricShell(String, String)} if the modloader is Fabric.
     * {@link #fabricBatch(String, String)} if the modloader is Fabric.
     * {@link #forgeShell(String, String)} if the modloader is Forge.
     * {@link #forgeBatch(String, String)} if the modloader is Forge.
     * @author Griefed
     * @param modLoader String. Determines whether the scripts are generated for Forge or Fabric.
     * @param modpackDir String. The scripts are generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. Determines the Minecraft version for which the scripts are generated.
     */
    void generateDownloadScripts(String modLoader, String modpackDir, String minecraftVersion) {

        if (modLoader.equalsIgnoreCase("Fabric")) {

            fabricShell(modpackDir, minecraftVersion);
            fabricBatch(modpackDir, minecraftVersion);

        } else if (modLoader.equalsIgnoreCase("Forge")) {

            forgeShell(modpackDir, minecraftVersion);
            forgeBatch(modpackDir, minecraftVersion);

        } else {

            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"), modLoader));
        }
    }

    /**
     * Generates Fabric Linux-shell download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @author Griefed
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void fabricShell(String modpackDir, String minecraftVersion) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        try {

            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();

            String shFabric = String.format("#!/bin/bash\n#Download the Minecraft_server.jar for your modpack\n\nwget -O server.jar %s", downloadMinecraftServer);

            Path pathSh = Paths.get(String.format("server-packs/%s/download_minecraft-server.jar_fabric.sh", destination));

            byte[] strToBytesSh = shFabric.getBytes();

            Files.write(pathSh, strToBytesSh);

            String readSh = Files.readAllLines(pathSh).get(0);

            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.fabricshell"), readSh));

        } catch (IOException ex) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.fabricshell"), ex);
        }

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.fabricshell"));
    }

    /**
     * Generates Fabric Windows-Batch download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @author Griefed
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void fabricBatch(String modpackDir, String minecraftVersion) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        try {

            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();

            String batFabric = String.format("powershell -Command \"(New-Object Net.WebClient).DownloadFile('%s', 'server.jar')\"", downloadMinecraftServer);

            Path pathBat = Paths.get(String.format("server-packs/%s/download_minecraft-server.jar_fabric.bat", destination));

            byte[] strToBytesBat = batFabric.getBytes();

            Files.write(pathBat, strToBytesBat);

            String readBat = Files.readAllLines(pathBat).get(0);

            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.fabricbatch"), readBat));

        } catch (IOException ex) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.fabricbatch"), ex);
        }

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.fabricbatch"));
    }

    /**
     * Generates Forge Linux-shell download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @author Griefed
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void forgeShell(String modpackDir, String minecraftVersion) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        try {

            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();

            String shForge = String.format("#!/bin/bash\n# Download the Minecraft_server.jar for your modpack\n\nwget -O minecraft_server.%s.jar %s", minecraftVersion, downloadMinecraftServer);

            Path pathSh = Paths.get(String.format("server-packs/%s/download_minecraft-server.jar_forge.sh", destination));

            byte[] strToBytesSh = shForge.getBytes();

            Files.write(pathSh, strToBytesSh);

            String readSh = Files.readAllLines(pathSh).get(0);

            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.forgeshell"), readSh));

        } catch (IOException ex) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.forgeshell"), ex);
        }

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.forgeshell"));
    }

    /**
     * Generates Forge Windows-Batch download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @author Griefed
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void forgeBatch(String modpackDir, String minecraftVersion) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        try {

            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();

            String batForge = String.format("powershell -Command \"(New-Object Net.WebClient).DownloadFile('%s', 'minecraft_server.%s.jar')\"", downloadMinecraftServer, minecraftVersion);

            Path pathBat = Paths.get(String.format("server-packs/%s/download_minecraft-server.jar_forge.bat", destination));

            byte[] strToBytesBat = batForge.getBytes();

            Files.write(pathBat, strToBytesBat);

            String readBat = Files.readAllLines(pathBat).get(0);

            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.forgebatch"), readBat));

        } catch (IOException ex) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.forgebatch"), ex);
        }

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.forgebatch"));
    }

    /**
     * Downloads the latest Fabric installer into the server pack.<p>
     * Calls<p>
     * {@link #latestFabricInstaller()} to acquire the latest version of the Fabric installer.
     * @author Griefed
     * @param modpackDir String. The Fabric installer is downloaded into the server_pack directory inside the modpack directory.
     * @return Boolean. Returns true if the download was successfull.
     */
    boolean downloadFabricJar(String modpackDir) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        boolean downloaded = false;

        try {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.downloadfabricjar.enter"));

            String latestFabricInstaller = latestFabricInstaller();
            URL downloadFabric = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", latestFabricInstaller, latestFabricInstaller));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadFabric.openStream());

            FileOutputStream downloadFabricFileOutputStream = new FileOutputStream(String.format("server-packs/%s/fabric-installer.jar", destination));
            FileChannel downloadFabricFileChannel = downloadFabricFileOutputStream.getChannel();
            downloadFabricFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadFabricFileOutputStream.flush();
            downloadFabricFileOutputStream.close();
            readableByteChannel.close();
            downloadFabricFileChannel.close();

        } catch (IOException e) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.downloadfabricjar.download"), e);

            if (new File(String.format("server-packs/%s/fabric-installer.jar", destination)).exists()) {
                try {
                    Files.delete(Paths.get(String.format("server-packs/%s/fabric-installer.jar", destination)));

                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.downloadfabricjar.delete"), ex);
                }
            }
        }

        if (new File(String.format("server-packs/%s/fabric-installer.jar", destination)).exists()) {
            downloaded = true;
        }
        return downloaded;
    }

    /**
     * Acquires the latest version of the Fabric modloader installer and returns it as a string. If acquisition of the
     * latest version fails, version 0.7.4 is returned by default.
     * @author whitebear60
     * @return String. Returns the version of the latest Fabric modloader installer.
     */
    String latestFabricInstaller() {
        String result;
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder builder = domFactory.newDocumentBuilder();

            Document fabricXml = builder.parse(new File("./work/fabric-installer-manifest.xml"));

            XPathFactory xPathFactory = XPathFactory.newInstance();

            XPath xpath = xPathFactory.newXPath();

            result = (String) xpath.evaluate("/metadata/versioning/release", fabricXml, XPathConstants.STRING);

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.latestfabricinstaller"));

        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.latestfabricinstaller"), ex);
            result = "0.7.4";
        }
        return result;
    }

    /**
     * Downloads the modloader server installer for Forge, for the specified modloader version.
     * @author Griefed
     * @param minecraftVersion String. The Minecraft version for which to download the modloader server installer.
     * @param modLoaderVersion String. The Forge version for which to download the modloader server installer.
     * @param modpackDir String. The modloader installer is downloaded to the server_pack directory inside the modloader directory.
     * @return Boolean. Returns true if the download was successful.
     */
    boolean downloadForgeJar(String minecraftVersion, String modLoaderVersion, String modpackDir) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        boolean downloaded = false;

        try {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.downloadforgejar.enter"));
            URL downloadForge = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar", minecraftVersion, modLoaderVersion, minecraftVersion, modLoaderVersion));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadForge.openStream());

            FileOutputStream downloadForgeFileOutputStream = new FileOutputStream(String.format("server-packs/%s/forge-installer.jar", destination));
            FileChannel downloadForgeFileChannel = downloadForgeFileOutputStream.getChannel();
            downloadForgeFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadForgeFileOutputStream.flush();
            downloadForgeFileOutputStream.close();
            readableByteChannel.close();
            downloadForgeFileChannel.close();

        } catch (IOException e) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.downloadforgejar.download"), e);

            if (new File(String.format("server-packs/%s/forge-installer.jar", destination)).exists()) {

                if (new File(String.format("server-packs/%s/forge-installer.jar", destination)).delete()) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.debug.downloadforgejar"));
                }
            }
        }

        if (new File(String.format("server-packs/%s/forge-installer.jar", destination)).exists()) {
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
     * @param modpackDir String. Cleanup tasks are done inside the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     * @param modLoaderVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     */
    void cleanUpServerPack(File fabricInstaller, File forgeInstaller, String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion) {

        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.enter"));

        if (modLoader.equalsIgnoreCase("Fabric")) {

            boolean isInstallerDeleted = fabricInstaller.delete();

            if (isInstallerDeleted)
            { LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.deleted"), fabricInstaller.getName())); }
            else
            { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupserverpack.delete"), fabricInstaller.getName())); }

        } else if (modLoader.equalsIgnoreCase("Forge")) {
            try {

                Files.copy(
                        Paths.get(String.format("server-packs/%s/forge-%s-%s.jar", destination, minecraftVersion, modLoaderVersion)),
                        Paths.get(String.format("server-packs/%s/forge.jar", destination)),
                        REPLACE_EXISTING);

                boolean isOldJarDeleted = (new File(
                        String.format("server-packs/%s/forge-%s-%s.jar",
                                destination,
                                minecraftVersion,
                                modLoaderVersion))).delete();

                boolean isInstallerDeleted = forgeInstaller.delete();

                boolean isInstallerLogDeleted = new File(String.format("server-packs/%s/installer.log", destination)).delete();

                if ((isOldJarDeleted) && (new File(String.format("server-packs/%s/forge.jar", destination)).exists())) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.rename"));
                } else {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupserverpack.rename"));
                }

                if (isInstallerDeleted) {
                    LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.deleted"), forgeInstaller.getName()));
                } else {
                    LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupserverpack.delete"), forgeInstaller.getName()));
                }

                if (isInstallerLogDeleted) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.cleanupserverpack.forgelog"));
                } else {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupserverpack.forgelog"));
                }

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.cleanupserverpack"), ex);
            }
        } else {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"), modLoader));
        }
    }
}