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
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
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
 * 1. {@link #CreateServerPack(LocalizationManager, Configuration, CurseCreateModpack)}<br>
 * 2. {@link #getPropertiesFile()}<br>
 * 3. {@link #getIconFile()}<br>
 * 4. {@link #getForgeWindowsFile()}<br>
 * 5. {@link #getForgeLinuxFile()}<br>
 * 6. {@link #getFabricWindowsFile()}<br>
 * 7. {@link #getFabricLinuxFile()}<br>
 * 8. {@link #getFabricInstallerManifest()}<br>
 * 9. {@link #run()}<br>
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
 * 24.{@link #latestFabricInstaller(String)}<br>
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
 */
@Component
public class CreateServerPack {
    private static final Logger LOG = LogManager.getLogger(DefaultFiles.class);
    private static final Logger LOG_INSTALLER = LogManager.getLogger("InstallerLogger");

    private final Configuration CONFIGURATION;
    private final LocalizationManager LOCALIZATIONMANAGER;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * Receives an instance of {@link Configuration} required to successfully and correctly create the server pack.<p>
     * Receives an instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedConfiguration Instance of {@link Configuration} required to successfully and correctly create the server pack.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     */
    @Autowired
    public CreateServerPack(LocalizationManager injectedLocalizationManager, Configuration injectedConfiguration, CurseCreateModpack injectedCurseCreateModpack) {
        CurseCreateModpack curseCreateModpack;

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedCurseCreateModpack == null) {
            curseCreateModpack = new CurseCreateModpack(LOCALIZATIONMANAGER);
        } else {
            curseCreateModpack = injectedCurseCreateModpack;
        }

        if (injectedConfiguration == null) {
            this.CONFIGURATION = new Configuration(LOCALIZATIONMANAGER, curseCreateModpack);
        } else {
            this.CONFIGURATION = injectedConfiguration;
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
     * @return Returns the server.properties-file for use in {@link #copyProperties(String)}
     */
    public File getPropertiesFile() {
        return FILE_PROPERTIES;
    }

    /**
     * Getter for server-icon.png
     * @return Returns the server-icon.png-file for use in {@link #copyIcon(String)}
     */
    public File getIconFile() {
        return FILE_ICON;
    }

    /**
     * Getter for start-forge.bat.
     * @return Returns the start-forge.bat-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getForgeWindowsFile() {
        return FILE_FORGE_WINDOWS;
    }

    /**
     * Getter for start-forge.sh
     * @return Returns the start-forge.sh-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getForgeLinuxFile() {
        return FILE_FORGE_LINUX;
    }

    /**
     * Getter for start-fabric.bat.
     * @return Returns the start-fabric.bat-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getFabricWindowsFile() {
        return FILE_FABRIC_WINDOWS;
    }

    /**
     * Getter for start-fabric.sh.
     * @return Returns the start-fabric.sh-file for use in {@link #copyStartScripts(String, String, boolean)}
     */
    public File getFabricLinuxFile() {
        return FILE_FABRIC_LINUX;
    }

    /**
     * Getter for the URL to the Fabric Installer Manifest. Gets the string containing the URL and returns it as a URL.
     * @return Returns the URL to the Fabric Installer Manifest.
     */
    public URL getFabricInstallerManifest() {
        URL downloadURL = null;

        String fabricInstallerManifest = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml";

        try { downloadURL = new URL(fabricInstallerManifest); }
        catch (IOException ex) { LOG.error(ex); }

        return downloadURL;
    }

    /**
     * Create a server pack if the check of the configuration file was successfull.<p>
     * Calls<p>
     * {@link #cleanupEnvironment(String)} to delete any previously generated server packs or ZIP-archives thereof.<p>
     * {@link #copyFiles(String, List, List)} to copy all specified directories and mods, excluding clientside-only mods,
     * to the server pack.<p>
     * {@link #copyStartScripts(String, String, boolean)} to copy the start scripts for the specified modloader to the
     * server pack.<p>
     * {@link #installServer(String, String, String, String, String)} to install the server software for the specified
     * modloader, modloader version and Minecraft version in the server pack.<p>
     * {@link #copyIcon(String)} to copy the server-icon.png to the server pack.<p>
     * {@link #copyProperties(String)} to copy the server.properties to the server pack.<p>
     * {@link #zipBuilder(String, String, boolean)} to create a ZIP-archive of the server pack.
     * @return Boolean. Returns true if the server pack was successfully generated.
     */
    public boolean run() {
        if (!CONFIGURATION.checkConfigFile(CONFIGURATION.getConfigFile(), true)) {

            // Make sure no files from previously generated server packs interrupt us.
            cleanupEnvironment(CONFIGURATION.getModpackDir());

            // Recursively copy all specified directories and files, excluding clientside-only mods, to server pack.
            copyFiles(CONFIGURATION.getModpackDir(), CONFIGURATION.getCopyDirs(), CONFIGURATION.getClientMods());

            // Copy start scripts for specified modloader from server_files to server pack.
            copyStartScripts(CONFIGURATION.getModpackDir(), CONFIGURATION.getModLoader(), CONFIGURATION.getIncludeStartScripts());

            // If true, Install the modloader software for the specified Minecraft version, modloader, modloader version
            if (CONFIGURATION.getIncludeServerInstallation()) {
                installServer(CONFIGURATION.getModLoader(), CONFIGURATION.getModpackDir(), CONFIGURATION.getMinecraftVersion(), CONFIGURATION.getModLoaderVersion(), CONFIGURATION.getJavaPath());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("handler.log.info.runincli.server"));
            }

            // If true, copy the server-icon.png from server_files to the server pack.
            if (CONFIGURATION.getIncludeServerIcon()) {
                copyIcon(CONFIGURATION.getModpackDir());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("handler.log.info.runincli.icon"));
            }

            // If true, copy the server.properties from server_files to the server pack.
            if (CONFIGURATION.getIncludeServerProperties()) {
                copyProperties(CONFIGURATION.getModpackDir());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("handler.log.info.runincli.properties"));
            }

            // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
            if (CONFIGURATION.getIncludeZipCreation()) {
                zipBuilder(CONFIGURATION.getModpackDir(), CONFIGURATION.getMinecraftVersion(), CONFIGURATION.getIncludeServerInstallation());
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("handler.log.info.runincli.zip"));
            }

            // Inform user about location of newly generated server pack.
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("handler.log.info.runincli.serverpack"), CONFIGURATION.getModpackDir()));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("handler.log.info.runincli.archive"), CONFIGURATION.getModpackDir()));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("handler.log.info.runincli.finish"));

            return true;

        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("handler.log.error.runincli"));
            return false;
        }
    }

    /**
     * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure newly generated
     * server pack is as clean as possible.
     * @param modpackDir String. The server_pack directory and ZIP-archive will be deleted inside the modpack directory.
     */
    void cleanupEnvironment(String modpackDir) {
        if (new File(String.format("%s/server_pack", modpackDir)).exists()) {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.cleanupenvironment.folder.enter"));
            Path serverPack = Paths.get(String.format("%s/server_pack", modpackDir));
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
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.cleanupenvironment.folder.delete"), modpackDir));
            } finally {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.cleanupenvironment.folder.complete"));
            }
        }
        if (new File(String.format("%s/server_pack.zip", modpackDir)).exists()) {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.cleanupenvironment.zip.enter"));
            boolean isZipDeleted = new File(String.format("%s/server_pack.zip", modpackDir)).delete();
            if (isZipDeleted) {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.cleanupenvironment.zip.complete"));
            } else {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.cleanupenvironment.zip.delete"));
            }
        }
    }

    /**
     * Copies start scripts for the specified modloader into the server pack.
     * @param modpackDir String. Start scripts are copied into the server_pack directory in the modpack directory.
     * @param modLoader String. Whether to copy the Forge or Fabric scripts into the server pack.
     * @param includeStartScripts Boolean. Whether to copy the start scripts into the server pack.
     */
    void copyStartScripts(String modpackDir, String modLoader, boolean includeStartScripts) {
        if (modLoader.equalsIgnoreCase("Forge") && includeStartScripts) {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.copystartscripts.forge"));
            try {
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getForgeWindowsFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getForgeWindowsFile())),
                        REPLACE_EXISTING
                );
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getForgeLinuxFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getForgeLinuxFile())),
                        REPLACE_EXISTING
                );
            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copystartscripts"), ex);
            }
        } else if (modLoader.equalsIgnoreCase("Fabric") && includeStartScripts) {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.copystartscripts.fabric"));
            try {
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getFabricWindowsFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getFabricWindowsFile())),
                        REPLACE_EXISTING
                );
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getFabricLinuxFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getFabricLinuxFile())),
                        REPLACE_EXISTING
                );
            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copystartscripts"), ex);
            }
        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkmodloader"));
        }
    }

    /**
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack directory into the
     * server pack directory.
     * If a <code>source/file;destination/file</code>-combination is provided, the specified source-file is copied to
     * the specified destination-file.
     * Calls {@link #excludeClientMods(String, List)} to generate a list of all mods to copy to server pack, excluding
     * clientside-only mods.
     * @param modpackDir String. Files and directories are copied into the server_pack directory inside the modpack directory.
     * @param directoriesToCopy String List. All directories and files therein to copy to the server pack.
     * @param clientMods String List. List of clientside-only mods to exclude from the server pack.
     */
    // TODO: Split into methods for dir copy and file copy to make reading easier
    void copyFiles(String modpackDir, List<String> directoriesToCopy, List<String> clientMods) {
        String serverPath = String.format("%s/server_pack", modpackDir);

        try {
            Files.createDirectories(Paths.get(serverPath));
        } catch (IOException ex) {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.unziparchive.createdir"), serverPath));
        }

        for (String directory : directoriesToCopy) {

            String clientDir = String.format("%s/%s", modpackDir, directory);
            String serverDir = String.format("%s/%s", serverPath, directory);

            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.copyfiles.setup"), directory));

            if (directory.contains(";")) {

                String[] sourceFileDestinationFileCombination = directory.split(";");
                File sourceFile = new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));
                File destinationFile = new File(String.format("%s/%s", serverPath, sourceFileDestinationFileCombination[1]));

                try {
                    FileUtils.copyFile(sourceFile, destinationFile, REPLACE_EXISTING);
                } catch (IOException ex) {
                    LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyfiles"), ex));
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

                            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.debug.copyfiles"), file.toAbsolutePath()));

                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyfiles.saves"), ex);
                            }
                        }
                    });

                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyfiles.saves.world"), ex);
                }


            } else if (directory.startsWith("mods") && clientMods.size() > 0) {

                List<String> listOfFiles = excludeClientMods(clientDir, clientMods);

                try {
                    Files.createDirectories(Paths.get(serverDir));
                } catch (IOException ex) {
                    LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.copyfiles.setup"), serverDir));
                }

                for (String file : listOfFiles) {
                    try {

                        Files.copy(
                                Paths.get(file),
                                Paths.get(String.format("%s/%s", serverDir, new File(file).getName())),
                                REPLACE_EXISTING
                        );

                        LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.debug.copyfiles"), file));

                    } catch (IOException ex) {
                        if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyfiles.mods"), ex);
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

                            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.debug.copyfiles"), file.toAbsolutePath()));
                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyfiles.mods"), ex);
                            }
                        }
                    });

                    files.close();

                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyfiles"), ex);
                }
            }


        }
    }

    /**
     * Generates a list of all mods to include in the server pack excluding clientside-only mods.
     * @param modsDir String. The mods directory of the modpack of which to generate a list of all it's contents.
     * @param clientMods List String. A list of all clientside-only mods.
     * @return List String. A list of all mods to include in the server pack.
     */
    List<String> excludeClientMods(String modsDir, List<String> clientMods) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.excludeclientmods"));

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
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.excludeclientmods"), np);
        }

        for (int iclient = 0; iclient < clientMods.size(); iclient++) {

            int i = iclient;

            modsInModpack.removeIf(n -> (n.contains(clientMods.get(i))));

        }

        return modsInModpack;
    }

    /**
     * Copies the server-icon.png into server_pack.
     * @param modpackDir String. The server-icon.png is copied into the server_pack directory inside the modpack directory.
     */
    void copyIcon(String modpackDir) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.copyicon"));
        try {

            Files.copy(
                    Paths.get(String.format("./server_files/%s", getIconFile())),
                    Paths.get(String.format("%s/server_pack/%s", modpackDir, getIconFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyicon"), ex);
        }
    }

    /**
     * Copies the server.properties into server_pack.
     * @param modpackDir String. The server.properties file is copied into the server_pack directory inside the modpack directory.
     */
    void copyProperties(String modpackDir) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.info.copyproperties"));
        try {

            Files.copy(
                    Paths.get(String.format("./server_files/%s", getPropertiesFile())),
                    Paths.get(String.format("%s/server_pack/%s", modpackDir, getPropertiesFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("copyfiles.log.error.copyproperties"), ex);
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
     * @param modLoader String. The modloader for which to install the server software. Either Forge or Fabric.
     * @param modpackDir String. The server software is installed into the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to install the modloader and Minecraft server.
     * @param modLoaderVersion String. The modloader version for which to install the modloader and Minecraft server.
     * @param javaPath String. The path to the Java executable/binary which is needed to execute the Forge/Fabric installers.
     */
    void installServer(String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion, String javaPath) {
        File fabricInstaller = new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir));
        File forgeInstaller = new File(String.format("%s/server_pack/forge-installer.jar", modpackDir));
        if (modLoader.equalsIgnoreCase("Fabric")) {
            try {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.fabric.enter"));
                LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.fabric.enter"));
                if (downloadFabricJar(modpackDir)) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.fabric.download"));
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            javaPath,
                            "-jar",
                            "fabric-installer.jar",
                            "server",
                            String.format("-mcversion %s", minecraftVersion),
                            String.format("-loader %s", modLoaderVersion),
                            "-downloadMinecraft").directory(new File(String.format("%s/server_pack", modpackDir)));
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        LOG_INSTALLER.info(line);
                    }
                    LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver"));
                    reader.close();
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.fabric.details"));
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver"));
                } else {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.error.installserver.fabric"));
                }
            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.error.installserver.fabricfail"), ex);
            }
        } else if (modLoader.equalsIgnoreCase("Forge")) {
            try {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.forge.enter"));
                LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.forge.enter"));
                if (downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir)) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.forge.download"));
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            javaPath,
                            "-jar",
                            "forge-installer.jar",
                            "--installServer")
                            .directory(new File(String.format("%s/server_pack", modpackDir)));
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        LOG_INSTALLER.info(line);
                    }
                    LOG_INSTALLER.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver"));
                    reader.close();
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver.forge.details"));
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.installserver"));
                    process.destroy();
                } else {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.error.installserver.forge"));
                }
            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.error.installserver.forgefail"), ex);
            }
        } else {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkmodloader"), modLoader));
        }

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
     * @param modpackDir String. The directory server_pack will be zipped and placed inside the modpack directory.
     * @param minecraftVersion String. Determines the name of the Minecraft server JAR to exclude from the ZIP-archive if the modloader is Forge.
     * @param includeServerInstallation Boolean. Determines whether the Minecraft server JAR info should be printed.
     */
    void zipBuilder(String modpackDir, String minecraftVersion, boolean includeServerInstallation) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.zipbuilder.enter"));

        List<File> filesToExclude = new ArrayList<>(Arrays.asList(
                new File(String.format("%s/server_pack/minecraft_server.%s.jar", modpackDir, minecraftVersion)),
                new File(String.format("%s/server_pack/server.jar", modpackDir))
        ));

        ExcludeFileFilter excludeFileFilter = filesToExclude::contains;
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setExcludeFileFilter(excludeFileFilter);
        zipParameters.setIncludeRootFolder(false);

        zipParameters.setFileComment("Server pack made with ServerPackCreator by Griefed.");

        try {
            new net.lingala.zip4j.ZipFile(String.format("%s/server_pack.zip", modpackDir)).addFolder(new File(String.format("%s/server_pack", modpackDir)), zipParameters);
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.error.zipbuilder.create"), ex);
        }
        if (includeServerInstallation) {
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar3"));
        }
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serversetup.log.info.zipbuilder.finish"));
    }

    /**
     * Depending on the specified modloader and Minecraft version, this method makes calls to generate the corresponding
     * download scripts for the Minecraft server JAR.<p>
     * Calls<p>
     * {@link #fabricShell(String, String)} if the modloader is Fabric.
     * {@link #fabricBatch(String, String)} if the modloader is Fabric.
     * {@link #forgeShell(String, String)} if the modloader is Forge.
     * {@link #forgeBatch(String, String)} if the modloader is Forge.
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
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkmodloader"), modLoader));
        }
    }

    /**
     * Generates Fabric Linux-shell download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void fabricShell(String modpackDir, String minecraftVersion) {
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
            Path pathSh = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_fabric.sh", modpackDir));
            byte[] strToBytesSh = shFabric.getBytes();
            Files.write(pathSh, strToBytesSh);
            String readSh = Files.readAllLines(pathSh).get(0);
            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.debug.fabricshell"), readSh));
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.fabricshell"), ex);
        }
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.fabricshell"));
    }

    /**
     * Generates Fabric Windows-Batch download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void fabricBatch(String modpackDir, String minecraftVersion) {
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
            Path pathBat = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_fabric.bat", modpackDir));
            byte[] strToBytesBat = batFabric.getBytes();
            Files.write(pathBat, strToBytesBat);
            String readBat = Files.readAllLines(pathBat).get(0);
            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.debug.fabricbatch"), readBat));
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.fabricbatch"), ex);
        }
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.fabricbatch"));
    }

    /**
     * Generates Forge Linux-shell download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void forgeShell(String modpackDir, String minecraftVersion) {
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
            Path pathSh = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir));
            byte[] strToBytesSh = shForge.getBytes();
            Files.write(pathSh, strToBytesSh);
            String readSh = Files.readAllLines(pathSh).get(0);
            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.debug.forgeshell"), readSh));
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.forgeshell"), ex);
        }
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.forgeshell"));
    }

    /**
     * Generates Forge Windows-Batch download scripts for Mojang's Minecraft server JAR for the specified Minecraft version.
     * @param modpackDir String. The script is generated in the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. The Minecraft version for which to download the server JAR.
     */
    void forgeBatch(String modpackDir, String minecraftVersion) {
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
            Path pathBat = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir));
            byte[] strToBytesBat = batForge.getBytes();
            Files.write(pathBat, strToBytesBat);
            String readBat = Files.readAllLines(pathBat).get(0);
            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.debug.forgebatch"), readBat));
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.forgebatch"), ex);
        }
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.forgebatch"));
    }

    /**
     * Downloads the latest Fabric installer into the server pack.<p>
     * Calls<p>
     * {@link #latestFabricInstaller(String)} to acquire the latest version of the Fabric installer.
     * @param modpackDir String. The Fabric installer is downloaded into the server_pack directory inside the modpack directory.
     * @return Boolean. Returns true if the download was successfull.
     */
    boolean downloadFabricJar(String modpackDir) {
        boolean downloaded = false;
        try {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.downloadfabricjar.enter"));
            String latestFabricInstaller = latestFabricInstaller(modpackDir);
            URL downloadFabric = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", latestFabricInstaller, latestFabricInstaller));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadFabric.openStream());
            FileOutputStream downloadFabricFileOutputStream = new FileOutputStream(String.format("%s/server_pack/fabric-installer.jar", modpackDir));
            FileChannel downloadFabricFileChannel = downloadFabricFileOutputStream.getChannel();
            downloadFabricFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadFabricFileOutputStream.flush();
            downloadFabricFileOutputStream.close();
            readableByteChannel.close();
            downloadFabricFileChannel.close();

        } catch (IOException e) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.downloadfabricjar.download"), e);
            if (new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).exists()) {
                try {
                    Files.delete(Paths.get(String.format("%s/server_pack/fabric-installer.jar", modpackDir)));
                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.downloadfabricjar.delete"), ex);
                }
            }
        }
        if (new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).exists()) {
            downloaded = true;
        }
        return downloaded;
    }

    /**
     * Acquires the latest version of the Fabric modloader installer and returns it as a string. If acquisition of the
     * latest version fails, version 0.7.2 is returned by default.
     * @param modpackDir String. The fabric-installer.xml-file is saved inside the server_pack directory inside the modpack
     * directory.
     * @return String. Returns the version of the latest Fabric modloader installer.
     */
    // TODO: Update to 0.7.4
    String latestFabricInstaller(String modpackDir) {
        String result;
        try {
            URL downloadFabricXml = getFabricInstallerManifest();

            ReadableByteChannel downloadFabricXmlReadableByteChannel = Channels.newChannel(downloadFabricXml.openStream());
            FileOutputStream downloadFabricXmlFileOutputStream = new FileOutputStream(String.format("%s/server_pack/fabric-installer.xml", modpackDir));
            FileChannel downloadFabricXmlFileChannel = downloadFabricXmlFileOutputStream.getChannel();
            downloadFabricXmlFileOutputStream.getChannel().transferFrom(downloadFabricXmlReadableByteChannel, 0, Long.MAX_VALUE);

            downloadFabricXmlFileOutputStream.flush();
            downloadFabricXmlFileOutputStream.close();
            downloadFabricXmlReadableByteChannel.close();
            downloadFabricXmlFileChannel.close();

            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document fabricXml = builder.parse(new File(String.format("%s/server_pack/fabric-installer.xml",modpackDir)));
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            result = (String) xpath.evaluate("/metadata/versioning/release", fabricXml, XPathConstants.STRING);
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.latestfabricinstaller"));
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.latestfabricinstaller"), ex);
            result = "0.7.3";
        }
        return result;
    }

    /**
     * Downloads the modloader server installer for Forge, for the specified modloader version.
     * @param minecraftVersion String. The Minecraft version for which to download the modloader server installer.
     * @param modLoaderVersion String. The Forge version for which to download the modloader server installer.
     * @param modpackDir String. The modloader installer is downloaded to the server_pack directory inside the modloader directory.
     * @return Boolean. Returns true if the download was successful.
     */
    boolean downloadForgeJar(String minecraftVersion, String modLoaderVersion, String modpackDir) {
        boolean downloaded = false;
        try {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.downloadforgejar.enter"));
            URL downloadForge = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar", minecraftVersion, modLoaderVersion, minecraftVersion, modLoaderVersion));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadForge.openStream());
            FileOutputStream downloadForgeFileOutputStream = new FileOutputStream(String.format("%s/server_pack/forge-installer.jar", modpackDir));
            FileChannel downloadForgeFileChannel = downloadForgeFileOutputStream.getChannel();
            downloadForgeFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadForgeFileOutputStream.flush();
            downloadForgeFileOutputStream.close();
            readableByteChannel.close();
            downloadForgeFileChannel.close();

        } catch (IOException e) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.downloadforgejar.download"), e);
            if (new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).exists()) {
                if (new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).delete()) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.debug.downloadforgejar"));
                }
            }
        }
        if (new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).exists()) {
            downloaded = true;
        }
        return downloaded;
    }

    /**
     * Cleans up the server_pack directory by deleting left-over files from modloader installations and version checking.
     * @param fabricInstaller File. The Fabric installer file which is to be deleted.
     * @param forgeInstaller File. The Forge installer file which is to be deleted.
     * @param modLoader String. Whether Forge or Fabric files are to be deleted.
     * @param modpackDir String. Cleanup tasks are done inside the server_pack directory inside the modpack directory.
     * @param minecraftVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     * @param modLoaderVersion String. Needed for renaming the Forge server JAR to work with launch scripts provided by ServerPackCreator.
     */
    void cleanUpServerPack(File fabricInstaller, File forgeInstaller, String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.cleanupserverpack.enter"));
        if (modLoader.equalsIgnoreCase("Fabric")) {
            File fabricXML = new File(String.format("%s/server_pack/fabric-installer.xml", modpackDir));

            boolean isXmlDeleted = fabricXML.delete();
            boolean isInstallerDeleted = fabricInstaller.delete();

            if (isXmlDeleted)
            { LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.cleanupserverpack.deleted"), fabricXML.getName())); }
            else
            { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.cleanupserverpack.delete"), fabricXML.getName())); }

            if (isInstallerDeleted)
            { LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.cleanupserverpack.deleted"), fabricInstaller.getName())); }
            else
            { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.cleanupserverpack.delete"), fabricInstaller.getName())); }

        } else if (modLoader.equalsIgnoreCase("Forge")) {
            try {
                Files.copy(
                        Paths.get(String.format("%s/server_pack/forge-%s-%s.jar", modpackDir, minecraftVersion, modLoaderVersion)),
                        Paths.get(String.format("%s/server_pack/forge.jar", modpackDir)),
                        REPLACE_EXISTING);

                boolean isOldJarDeleted = (new File(
                        String.format("%s/server_pack/forge-%s-%s.jar",
                                modpackDir,
                                minecraftVersion,
                                modLoaderVersion))).delete();

                boolean isInstallerDeleted = forgeInstaller.delete();

                if ((isOldJarDeleted) && (new File(String.format("%s/server_pack/forge.jar", modpackDir)).exists()))
                { LOG.info(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.cleanupserverpack.rename")); }
                else
                { LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.cleanupserverpack.rename")); }

                if (isInstallerDeleted)
                { LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.info.cleanupserverpack.deleted"), forgeInstaller.getName())); }
                else
                { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.cleanupserverpack.delete"), forgeInstaller.getName())); }

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("serverutilities.log.error.cleanupserverpack"), ex);
            }
        } else {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkmodloader"), modLoader));
        }
    }
}