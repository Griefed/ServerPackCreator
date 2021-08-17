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
import java.nio.file.Files;
import java.nio.file.Paths;
/*import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;*/
import java.util.Objects;

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
 * 14.{@link #getMANIFEST_MINECRAFT()}<br>
 * 14.{@link #getMANIFEST_FORGE()}<br>
 * 14.{@link #getMANIFEST_FABRIC()}<br>
 * 14.{@link #getMANIFEST_FABRIC_INSTALLER()}<br>
 * 14.{@link #filesSetup()}<br>
 * 15.{@link #checkForConfig()}<br>
 * 16.{@link #checkForFile(File)}<br>
 * 17.{@link #refreshManifestFile(URL, File)}
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
    private final File MANIFEST_MINECRAFT = new File("minecraft-manifest.json");
    private final File MANIFEST_FORGE = new File("forge-manifest.json");
    private final File MANIFEST_FABRIC = new File("fabric-manifest.xml");
    private final File MANIFEST_FABRIC_INSTALLER = new File("fabric-installer-manifest.xml");
    //private final File SERVERPACKCREATOR_DATABASE = new File ("serverpackcreator.db");

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
     * @return File. Returns the server.properties-file.
     */
    public File getPropertiesFile() {
        return FILE_PROPERTIES;
    }

    /**
     * Getter for server-icon.png
     * @author Griefed
     * @return File. Returns the server-icon.png-file.
     */
    public File getIconFile() {
        return FILE_ICON;
    }

    /**
     * Getter for start-forge.bat.
     * @author Griefed
     * @return File. Returns the start-forge.bat-file.
     */
    public File getForgeWindowsFile() {
        return FILE_FORGE_WINDOWS;
    }

    /**
     * Getter for start-forge.sh.
     * @author Griefed
     * @return File. Returns the start-forge.sh-file.
     */
    public File getForgeLinuxFile() {
        return FILE_FORGE_LINUX;
    }

    /**
     * Getter for start-fabric.bat.
     * @author Griefed
     * @return File. Returns the start-fabric.bat-file.
     */
    public File getFabricWindowsFile() {
        return FILE_FABRIC_WINDOWS;
    }

    /**
     * Getter for start-fabric.sh.
     * @author Griefed
     * @return File. Returns the start-fabric.sh-file.
     */
    public File getFabricLinuxFile() {
        return FILE_FABRIC_LINUX;
    }

    /**
     * Getter for Mojang's Minecraft version-manifest.
     * @author Griefed
     * @return URL. Returns the URL to the JSON-file.
     */
    public URL getMinecraftManifestUrl() {
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
     * @return URL. Returns the URL to the JSON-file.
     */
    public URL getForgeManifestUrl() {
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
     * @return URL. Returns the URL to the XML-file.
     */
    public URL getFabricManifestUrl() {
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
     * @return URL. Returns the URL to the XML-file.
     */
    public URL getFabricInstallerManifestUrl() {
        URL downloadURL = null;

        String fabricInstallerManifest = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml";

        try { downloadURL = new URL(fabricInstallerManifest); }
        catch (IOException ex) { LOG.error(ex); }

        return downloadURL;
    }

    /**
     * Getter for the Minecraft version manifest file.
     * @author Griefed
     * @return File. Returns the name of the Minecraft version manifest file.
     */
    public File getMANIFEST_MINECRAFT() {
        return MANIFEST_MINECRAFT;
    }

    /**
     * Getter for the Forge version manifest file.
     * @author Griefed
     * @return File. Returns the name of the Forge version manifest file.
     */
    public File getMANIFEST_FORGE() {
        return MANIFEST_FORGE;
    }

    /**
     * Getter for the Fabric version manifest file.
     * @author Griefed
     * @return File. Returns the name of the Fabric version manifest file.
     */
    public File getMANIFEST_FABRIC() {
        return MANIFEST_FABRIC;
    }

    /**
     * Getter for the Fabric installer version manifest file.
     * @author Griefed
     * @return File. Returns the name of the Fabric installer version manifest file.
     */
    public File getMANIFEST_FABRIC_INSTALLER() {
        return MANIFEST_FABRIC_INSTALLER;
    }

    /*
     * Getter for the database containing a list of modIds.
     * @return File. Returns the database-file.
     */
    /*public File getSERVERPACKCREATOR_DATABASE() {
        return SERVERPACKCREATOR_DATABASE;
    }*/

    /**
     * Calls individual methods which check for existence of default files. Only this method should be called to check
     * for existence of all default files.<p>
     * If any file was newly generated from its template, a warning is printed informing the user about said newly
     * generated file. If every file was present and none was generated, "Setup completed." is printed to the console
     * and log.
     * @author Griefed
     */
    public void filesSetup() {
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

        //refreshValidationFiles();
        refreshManifestFile(getMinecraftManifestUrl(), getMANIFEST_MINECRAFT());
        refreshManifestFile(getForgeManifestUrl(), getMANIFEST_FORGE());
        refreshManifestFile(getFabricManifestUrl(), getMANIFEST_FABRIC());
        refreshManifestFile(getFabricInstallerManifestUrl(), getMANIFEST_FABRIC_INSTALLER());

        //checkDatabase();

        boolean doesConfigExist         = checkForConfig();
        boolean doesFabricLinuxExist    = checkForFile(getFabricLinuxFile());
        boolean doesFabricWindowsExist  = checkForFile(getFabricWindowsFile());
        boolean doesForgeLinuxExist     = checkForFile(getForgeLinuxFile());
        boolean doesForgeWindowsExist   = checkForFile(getForgeWindowsFile());
        boolean doesPropertiesExist     = checkForFile(getPropertiesFile());
        boolean doesIconExist           = checkForFile(getIconFile());

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
     * Checks for existence of defaults files. If it is not found, it is generated.
     * @author Griefed
     * @param fileToCheckFor The file which is to be checked for whether it exists and if it doesn't, should be created.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForFile(File fileToCheckFor) {
        boolean firstRun = false;
        if (!new File(String.format("server_files/%s", fileToCheckFor)).exists()) {
            try {

                FileUtils.copyInputStreamToFile(
                        Objects.requireNonNull(DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", fileToCheckFor))),
                        new File(String.format("./server_files/%s", fileToCheckFor)));

                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforfile"), fileToCheckFor));

                firstRun = true;

            } catch (IOException ex) {

                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {

                    LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.checkforfile"), fileToCheckFor), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Deletes the specified manifest if it is found, then downloads the specified manifest file again. Ensures we always
     * have the latest manifest for version validation available.
     * @author whitebear60
     * @param manifestUrl The URL to the file which is to be downloaded.
     * @param manifestToRefresh The manifest file to delete and then download, "refreshing" it
     * @author Griefed
     */
    void refreshManifestFile(URL manifestUrl, File manifestToRefresh) {

        File fileName = new File(String.format("./work/%s", manifestToRefresh));

        if (fileName.delete()) {
            LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.manifest.delete"), manifestToRefresh));
        }

        try {

            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestUrl.openStream());
            FileOutputStream downloadManifestOutputStream;

            try {
                downloadManifestOutputStream = new FileOutputStream(fileName);
            } catch (FileNotFoundException ex) {

                LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.manifest"), fileName), ex);

                if (!fileName.exists()) {
                    LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.manifest.creating"), fileName));

                    if (fileName.createNewFile()) {
                        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.debug.manifest.created"), fileName));

                    } else {
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.manifest.create"), fileName));
                    }
                }
                downloadManifestOutputStream = new FileOutputStream(fileName);
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();

            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();

            downloadManifestOutputStream.close();
            readableByteChannel.close();
            downloadManifestOutputStreamChannel.close();

        } catch (Exception ex) {

            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.manifest.download"), fileName), ex);
        }
    }

    /*
     * Ensures serverpackcreator.db exists and checks whether all tables required are present in the database. If the database
     * does not exist, it is created.
     * @author Griefed
     */
    /*private void checkDatabase() {

        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + getSERVERPACKCREATOR_DATABASE());

            if (connection != null) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                LOG.debug("Database driver name: " + databaseMetaData.getDriverName());
                LOG.debug("Database driver version: " + databaseMetaData.getDriverVersion());
                LOG.debug("Database product name: " + databaseMetaData.getDatabaseProductName());
                LOG.debug("Database product version: " + databaseMetaData.getDatabaseProductVersion());
                connection.close();
            }

        } catch (SQLException ex) {
            LOG.error("Error creating/accessing clientmods-database.", ex);
        }

    }*/
}