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

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * <strong>Table of methods</strong><p>
 * 1. {@link #DefaultFiles(LocalizationManager)}<br>
 * 2. {@link #getConfigFile()}<br>
 * 3. {@link #getOldConfigFile()}<br>
 * 4. {@link #getPropertiesFile()}<br>
 * 5. {@link #getIconFile()}<br>
 * 6. {@link #getForgeWindowsFile()}<br>
 * 7. {@link #getForgeLinuxFile()}<br>
 * 8. {@link #getFabricWindowsFile()}<br>
 * 9. {@link #getFabricLinuxFile()}<br>
 * 10.{@link #getMinecraftManifestUrl()}<br>
 * 11.{@link #getForgeManifestUrl()}<br>
 * 12.{@link #getFabricManifestUrl()}<br>
 * 13.{@link #getFabricInstallerManifestUrl()}<br>
 * 14.{@link #filesSetup()}<br>
 * 15.{@link #checkForConfig()}<br>
 * 16.{@link #checkForFabricLinux()}<br>
 * 17.{@link #checkForFabricWindows()}<br>
 * 18.{@link #checkForForgeLinux()}<br>
 * 19.{@link #checkForForgeWindows()}<br>
 * 20.{@link #checkForProperties()}<br>
 * 21.{@link #checkForIcon()}<br>
 * 22.{@link #refreshValidationFiles()}<br>
 * 23.{@link #downloadMinecraftManifest()}<br>
 * 24.{@link #downloadFabricManifest()}<br>
 * 25.{@link #downloadForgeManifest()}<br>
 * 26.{@link #downloadFabricInstallerManifest()}
 * <p>
 * Requires instances of {@link LocalizationManager} for use of localization, but creates one if injected one is null.
 * <p>
 * Ensures all files needed by ServerPackCreator are available. If any one is missing, a new one is generated from the
 * template. Among the default files are:<p>
 * <strong>serverpackcreator.conf</strong><br>
 * <strong>server.properties</strong><br>
 * <strong>server-icon.png</strong><br>
 * <strong>start-forge.bar</strong><br>
 * <strong>start-forge.sh</strong><br>
 * <strong>start-fabric.bat</strong><br>
 * <strong>start-fabric.sh</strong>
 * <p>
 * Should an old configuration file, <em>creator.conf</em>, be detected, it is renamed to <em>serverpackcreator.conf</em>
 * to ensure a configuration file is present at all times.
 * @author Griefed
 */
@Component
public class DefaultFiles {

    private static final Logger LOG = LogManager.getLogger(DefaultFiles.class);

    private final LocalizationManager LOCALIZATIONMANAGER;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection. Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     */
    @Autowired
    public DefaultFiles(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }
    }

    private final File FILE_CONFIG = new File("serverpackcreator.conf");
    private final File FILE_CONFIG_OLD = new File("creator.conf");
    private final File FILE_PROPERTIES = new File("server.properties");
    private final File FILE_ICON = new File("server-icon.png");
    private final File FILE_FORGE_WINDOWS = new File("start-forge.bat");
    private final File FILE_FORGE_LINUX = new File("start-forge.sh");
    private final File FILE_FABRIC_WINDOWS = new File("start-fabric.bat");
    private final File FILE_FABRIC_LINUX = new File("start-fabric.sh");

    /**
     * Getter for serverpackcreator.conf.
     * @author Griefed
     * @return Returns the serverpackcreator.conf-file for use in {@link #checkForConfig()}
     */
    public File getConfigFile() {
        return FILE_CONFIG;
    }

    /**
     * Getter for creator.conf.
     * @author Griefed
     * @return Returns the creator.conf-file for use in {@link #checkForConfig()}.
     */
    public File getOldConfigFile() {
        return FILE_CONFIG_OLD;
    }

    /**
     * Getter for server.properties.
     * @author Griefed
     * @return Returns the server.properties-file for use in {@link #checkForProperties()}
     */
    public File getPropertiesFile() {
        return FILE_PROPERTIES;
    }

    /**
     * Getter for server-icon.png
     * @author Griefed
     * @return Returns the server-icon.png-file for use in {@link #checkForIcon()}
     */
    public File getIconFile() {
        return FILE_ICON;
    }

    /**
     * Getter for start-forge.bat.
     * @author Griefed
     * @return Returns the start-forge.bat-file for use in {@link #checkForForgeWindows()}
     */
    public File getForgeWindowsFile() {
        return FILE_FORGE_WINDOWS;
    }

    /**
     * Getter for start-forge.sh.
     * @author Griefed
     * @return Returns the start-forge.sh-file for use in {@link #checkForForgeLinux()}
     */
    public File getForgeLinuxFile() {
        return FILE_FORGE_LINUX;
    }

    /**
     * Getter for start-fabric.bat.
     * @author Griefed
     * @return Returns the start-fabric.bat-file for use in {@link #checkForFabricWindows()}
     */
    public File getFabricWindowsFile() {
        return FILE_FABRIC_WINDOWS;
    }

    /**
     * Getter for start-fabric.sh.
     * @author Griefed
     * @return Returns the start-fabric.sh-file for use in {@link #checkForFabricLinux()}
     */
    public File getFabricLinuxFile() {
        return FILE_FABRIC_LINUX;
    }

    /**
     * Getter for Mojang's Minecraft version-manifest.
     * @author Griefed
     * @return String. Returns the URL to the JSON-file for use in {@link #downloadMinecraftManifest()}
     */
    URL getMinecraftManifestUrl() {
        URL minecraftURL = null;
        String minecraftManifest = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
        try {
            minecraftURL = new URL(minecraftManifest); }
        catch (IOException ex) { LOG.error(ex); }
        return minecraftURL;
    }

    /**
     * Getter for Forge's version-manifest.
     * @author Griefed
     * @return String. Returns the URL to the JSON-file for use in {@link #downloadForgeManifest()}
     */
    URL getForgeManifestUrl() {
        URL forgeURL = null;
        String forgeManifest = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json";
        try {
            forgeURL = new URL(forgeManifest); }
        catch (IOException ex) { LOG.error(ex); }
        return forgeURL;
    }

    /**
     * Getter for Fabric's version-manifest.
     * @author Griefed
     * @return String. Returns the URL to the JSON-file for use in {@link #downloadFabricManifest()}
     */
    URL getFabricManifestUrl() {
        URL fabricURL = null;
        String fabricManifest = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";
        try {
            fabricURL = new URL(fabricManifest); }
        catch (IOException ex) { LOG.error(ex); }
        return fabricURL;
    }

    /**
     * Getter for the URL to the Fabric Installer Manifest. Gets the string containing the URL and returns it as a URL.
     * @author Griefed
     * @return Returns the URL to the Fabric Installer Manifest.
     */
    public URL getFabricInstallerManifestUrl() {
        URL downloadURL = null;

        String fabricInstallerManifest = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml";

        try { downloadURL = new URL(fabricInstallerManifest); }
        catch (IOException ex) { LOG.error(ex); }

        return downloadURL;
    }

    /**
     * Calls individual methods which check for existence of default files. Only this method should be called to check
     * for existence of all default files.<p>
     * If any file was newly generated from its template, a warning is printed informing the user about said newly
     * generated file. If every file was present and none was generated, "Setup completed." is printed to the console
     * and log.
     * @author Griefed
     */
    void filesSetup() {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.filessetup.enter"));

        try { Files.createDirectories(Paths.get("./server_files")); }
        catch (IOException ex) { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.filessetup"), "server_files"), ex); }

        try { Files.createDirectories(Paths.get("./work")); }
        catch (IOException ex) { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.filessetup"), "work"), ex); }

        try { Files.createDirectories(Paths.get("./work/temp")); }
        catch (IOException ex) { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.filessetup"), "work/temp"), ex); }

        try { Files.createDirectories(Paths.get("./server-packs")); }
        catch (IOException ex) { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.filessetup"), "server-packs"), ex); }

        try { Files.createDirectories(Paths.get("./addons")); }
        catch (IOException ex) { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.filessetup"), "addons"), ex); }

        refreshValidationFiles();

        boolean doesConfigExist         = checkForConfig();
        boolean doesFabricLinuxExist    = checkForFabricLinux();
        boolean doesFabricWindowsExist  = checkForFabricWindows();
        boolean doesForgeLinuxExist     = checkForForgeLinux();
        boolean doesForgeWindowsExist   = checkForForgeWindows();
        boolean doesPropertiesExist     = checkForProperties();
        boolean doesIconExist           = checkForIcon();

        // Inform user about customization of files if any of them were generated from the template.
        if (doesConfigExist            ||
                doesFabricLinuxExist   ||
                doesFabricWindowsExist ||
                doesForgeLinuxExist    ||
                doesForgeWindowsExist  ||
                doesPropertiesExist    ||
                doesIconExist) {

            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning0"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning3"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning0"));

        } else {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.filessetup.finish"));
        }
    }
    /**
     * Check for old config file, if found rename to new name. If neither old nor new config file can be found, a new
     * config file is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     * @author Griefed
     */
    boolean checkForConfig() {
        boolean firstRun = false;
        if (getOldConfigFile().exists()) {
            try {
                Files.copy(getOldConfigFile().getAbsoluteFile().toPath(), getConfigFile().getAbsoluteFile().toPath());

                boolean isOldConfigDeleted = getOldConfigFile().delete();
                if (isOldConfigDeleted) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforconfig.old"));
                }

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforconfig.old"), ex);
            }
        } else if (!getConfigFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/%s", getConfigFile().getName())));

                if (link != null) {
                    Files.copy(link, getConfigFile().getAbsoluteFile().toPath());
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforconfig.config"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforconfig.config"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Checks for existence of Fabric start script for Linux. If it is not found, it is generated.
     * @author Griefed
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForFabricLinux() {
        boolean firstRun = false;
        if (!getFabricLinuxFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getFabricLinuxFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getFabricLinuxFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforfabriclinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforfabriclinux"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Checks for existence of Fabric start script for Windows. If it is not found, it is generated.
     * @author Griefed
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForFabricWindows() {
        boolean firstRun = false;
        if (!getFabricWindowsFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getFabricWindowsFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getFabricWindowsFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforfabricwindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforfabricwindows"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Checks for existence of Forge start script for Linux. If it is not found, it is generated.
     * @author Griefed
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForForgeLinux() {
        boolean firstRun = false;
        if (!getForgeLinuxFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getForgeLinuxFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getForgeLinuxFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforforgelinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforforgelinux"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Checks for existence of Forge start script for Windows. If it is not found, it is generated.
     * @author Griefed
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForForgeWindows() {
        boolean firstRun = false;
        if (!getForgeWindowsFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getForgeWindowsFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getForgeWindowsFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforforgewindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforforgewindows"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Checks for existence of server.properties file. If it is not found, it is generated.
     * @author Griefed
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForProperties() {
        boolean firstRun = false;
        if (!getPropertiesFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getPropertiesFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getPropertiesFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforproperties"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforproperties"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Checks for existence of server-icon.png file. If it is not found, it is generated.
     * @author Griefed
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForIcon() {
        boolean firstRun = false;
        if (!getIconFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getIconFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getIconFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforicon"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforicon"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Checks for existence of minecraft-manifest.json, fabric-manifest.xml and forge-manifest.json and deletes them if
     * they exist. Makes calls to {@link #downloadMinecraftManifest()}, {@link #downloadFabricManifest()}, {@link #getFabricInstallerManifestUrl()} and {@link #downloadForgeManifest()}
     * in order to update them.
     * @author Griefed
     */
    void refreshValidationFiles() {
        if (new File("./work/minecraft-manifest.json").delete()) {

            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.minecraftmanifest.delete"));
        }

        if (new File("./work/fabric-manifest.xml").delete()) {

            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.fabricmanifest.delete"));
        }

        if (new File("./work/forge-manifest.json").delete()) {

            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.forgemanifest.delete"));
        }

        if (new File("./work/fabric-installer-manifest.xml").delete()) {

            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.fabricinstallermanifest.delete"));
        }

        downloadMinecraftManifest();
        downloadFabricManifest();
        downloadFabricInstallerManifest();
        downloadForgeManifest();
    }

    /**
     * Downloads the Minecraft version manifest for version validation.
     * @author whitebear60
     */
    void downloadMinecraftManifest() {
        try {
            URL manifestJsonURL = getMinecraftManifestUrl();
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream;

            try {
                downloadManifestOutputStream = new FileOutputStream("./work/minecraft-manifest.json");
            } catch (FileNotFoundException ex) {

                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.minecraftmanifest"), ex);

                File file = new File("./work/minecraft-manifest.json");

                if (!file.exists()) {

                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.minecraftmanifest.create"));
                    boolean jsonCreated = file.createNewFile();

                    if (jsonCreated) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.minecraftmanifest.created"));

                    } else {

                        LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.minecraftmanifest.parse"));
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("./work/minecraft-manifest.json");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();

            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();

            readableByteChannel.close();
            downloadManifestOutputStreamChannel.close();

        } catch (Exception ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.minecraftmanifest"), ex);
        }
    }

    /**
     * Downloads the Fabric version manifest for version validation.
     * @author whitebear60
     */
    void downloadFabricManifest() {
        try {
            URL manifestJsonURL = getFabricManifestUrl();

            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());

            FileOutputStream downloadManifestOutputStream;

            try {
                downloadManifestOutputStream = new FileOutputStream("./work/fabric-manifest.xml");
            } catch (FileNotFoundException ex) {

                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.fabricmanifest"), ex);
                File file = new File("./work/fabric-manifest.xml");

                if (!file.exists()) {

                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.fabricmanifest.create"));
                    boolean jsonCreated = file.createNewFile();

                    if (jsonCreated) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.fabricmanifest.created"));

                    } else {
                        LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.fabricmanifest.parse"));
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("./work/fabric-manifest.xml");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();

            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();

            readableByteChannel.close();
            downloadManifestOutputStreamChannel.close();

        } catch (Exception ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.fabricmanifest"), ex);
        }
    }

    /**
     * Downloads the Forge version manifest for version validation.
     * @author whitebear60
     */
    void downloadForgeManifest() {
        try {
            URL manifestJsonURL = getForgeManifestUrl();
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream;

            try {

                downloadManifestOutputStream = new FileOutputStream("./work/forge-manifest.json");

            } catch (FileNotFoundException ex) {

                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.forgemanifest"), ex);
                File file = new File("./work/forge-manifest.json");

                if (!file.exists()) {

                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.forgemanifest.create"));

                    boolean jsonCreated = file.createNewFile();

                    if (jsonCreated) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.forgemanifest.created"));
                    } else {

                        LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.forgemanifest.parse"));
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("./work/forge-manifest.json");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();

            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();

            readableByteChannel.close();
            downloadManifestOutputStreamChannel.close();

        } catch (Exception ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.forgemanifest"), ex);
        }
    }

    /**
     * Downloads the Fabric installer manifest in order to acquire the latest installer version.
     * @author whitebear60
     */
    void downloadFabricInstallerManifest() {
        try {
            URL manifestJsonURL = getFabricInstallerManifestUrl();

            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());

            FileOutputStream downloadManifestOutputStream;

            try {
                downloadManifestOutputStream = new FileOutputStream("./work/fabric-installer-manifest.xml");
            } catch (FileNotFoundException ex) {

                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.fabricinstallermanifest"), ex);
                File file = new File("./work/fabric-installer-manifest.xml");

                if (!file.exists()) {

                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.fabricinstallermanifest.create"));
                    boolean jsonCreated = file.createNewFile();

                    if (jsonCreated) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.fabricinstallermanifest.created"));

                    } else {
                        LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.fabricinstallermanifest.parse"));
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("./work/fabric-installer-manifest.xml");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();

            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();

            readableByteChannel.close();
            downloadManifestOutputStreamChannel.close();

        } catch (Exception ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.fabricinstallermanifest"), ex);
        }
    }
}