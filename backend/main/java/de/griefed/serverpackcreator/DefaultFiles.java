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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
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
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Objects;
import java.util.Properties;

/**
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
    private final ApplicationProperties APPLICATIONPROPERTIES;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection. Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     */
    @Autowired
    public DefaultFiles(LocalizationManager injectedLocalizationManager, ApplicationProperties injectedApplicationProperties) {
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

        filesSetup();
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
            minecraftURL = new URL(minecraftManifest);
        } catch (IOException ex) {
            LOG.error("Error assigning the Minecraft manifest URL.",ex);
        }
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
            forgeURL = new URL(forgeManifest);
        } catch (IOException ex) {
            LOG.error("Error assigning the Forge manifest URL.",ex);
        }
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
            fabricURL = new URL(fabricManifest);
        } catch (IOException ex) {
            LOG.error("Error assigning the Fabric manifest URL.",ex);
        }
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
        try {
            downloadURL = new URL(fabricInstallerManifest);
        } catch (IOException ex) {
            LOG.error("Error assigning the Fabric-Installer manifest URL.",ex);
        }
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
    public void filesSetup() {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.filessetup.enter"));

        try {
            Files.createDirectories(Paths.get("./server_files"));
        } catch (IOException ex) {
            LOG.error("Could not create server_files directory.", ex);
        }

        try {
            Files.createDirectories(Paths.get("./work"));
        } catch (IOException ex) {
            LOG.error("Could not create work directory.", ex);
        }

        try {
            Files.createDirectories(Paths.get("./work/temp"));
        } catch (IOException ex) {
            LOG.error("Could not create work/temp directory.", ex);
        }

        try {
            Files.createDirectories(Paths.get("./work/modpacks"));
        } catch (IOException ex) {
            LOG.error("Could not create work/temp directory.", ex);
        }

        try {
            Files.createDirectories(Paths.get("./server-packs"));
        } catch (IOException ex) {
            LOG.error("Could not create server-packs directory.", ex);
        }

        preparePluginsDir();

        refreshManifestFile(getMinecraftManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT);
        refreshManifestFile(getForgeManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE);
        refreshManifestFile(getFabricManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC);
        refreshManifestFile(getFabricInstallerManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER);

        boolean doesConfigExist         = checkForConfig();
        boolean doesPropertiesExist     = checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES);
        boolean doesIconExist           = checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_ICON);

        // Inform user about customization of files if any of them were generated from the template.
        if (doesConfigExist            ||
                doesPropertiesExist    ||
                doesIconExist) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning0"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning3"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning0"));

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.filessetup.finish"));
        }
    }

    private void preparePluginsDir() {

        try {
            Files.createDirectories(Paths.get(System.getProperty("pf4j.pluginsDir", "./plugins")));
        } catch (IOException ex) {
            LOG.error("Could not create plugins directory.", ex);
        }

        if (!new File(System.getProperty("pf4j.pluginsDir", "./plugins") + "/disabled.txt").isFile()) {
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            Paths.get(
                                    System.getProperty("pf4j.pluginsDir", "./plugins")) + "/disabled.txt"
                    )
            )) {

                writer.write("########################################\n");
                writer.write("# - Load all plugins except these.   - #\n");
                writer.write("# - Add one plugin-id per line.      - #\n");
                writer.write("########################################\n");
                writer.write("#example-plugin\n");

            } catch (IOException ex) {
                LOG.error("Error generating disable.txt in the plugins directory.", ex);
            }
        }
    }

    /**
     * Check for old config file, if found rename to new name. If neither old nor new config file can be found, a new
     * config file is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     * @author Griefed
     */
    private boolean checkForConfig() {
        boolean firstRun = false;
        if (APPLICATIONPROPERTIES.FILE_CONFIG_OLD.exists()) {
            try {
                Files.copy(APPLICATIONPROPERTIES.FILE_CONFIG_OLD.getAbsoluteFile().toPath(), APPLICATIONPROPERTIES.FILE_CONFIG.getAbsoluteFile().toPath());

                boolean isOldConfigDeleted = APPLICATIONPROPERTIES.FILE_CONFIG_OLD.delete();
                if (isOldConfigDeleted) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforconfig.old"));
                }

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error("Error renaming creator.conf to serverpackcreator.conf.", ex);
                }
            }
        } else if (!APPLICATIONPROPERTIES.FILE_CONFIG.exists()) {
            try {

                FileUtils.copyInputStreamToFile(
                        Objects.requireNonNull(DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/%s", APPLICATIONPROPERTIES.FILE_CONFIG.getName()))),
                        APPLICATIONPROPERTIES.FILE_CONFIG);

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforconfig.config"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error("Could not extract default config-file.", ex);
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
     public boolean checkForFile(File fileToCheckFor) {
        boolean firstRun = false;
        if (!new File(String.format("server_files/%s", fileToCheckFor)).exists()) {
            try {

                FileUtils.copyInputStreamToFile(
                        Objects.requireNonNull(DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", fileToCheckFor))),
                        new File(String.format("./server_files/%s", fileToCheckFor)));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforfile"), fileToCheckFor));

                firstRun = true;

            } catch (IOException ex) {

                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {

                    LOG.error(String.format("Could not extract default %s file.", fileToCheckFor), ex);
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
    public void refreshManifestFile(URL manifestUrl, File manifestToRefresh) {

        File fileName = new File(String.format("./work/%s", manifestToRefresh));

        if (fileName.exists()) {

            try (InputStream existing = new FileInputStream(fileName); InputStream newManifest = manifestUrl.openStream()) {

                int countOldFile = 0;
                int countNewFile = 0;

                if (manifestToRefresh.toString().endsWith("json")) {

                    if (manifestToRefresh.toString().contains("forge")) {

                        for (JsonNode mcVer : getJson(existing)) {
                            countOldFile += mcVer.size();
                        }
                        for (JsonNode mcVer : getJson(newManifest)) {
                            countNewFile += mcVer.size();
                        }

                    } else {

                        countOldFile = getJson(existing).get("versions").size();
                        countNewFile = getJson(newManifest).get("versions").size();
                    }

                } else {

                    countOldFile = getXml(existing).getElementsByTagName("version").getLength();
                    countNewFile = getXml(newManifest).getElementsByTagName("version").getLength();
                }

                LOG.debug("Nodes/Versions/Size in/of old " + manifestToRefresh + ": " + countOldFile);
                LOG.debug("Nodes/Versions/Size in/of new " + manifestToRefresh + ": " + countNewFile);

                if (countNewFile > countOldFile) {

                    LOG.info("Refreshing " + manifestToRefresh + ".");

                    refresh(manifestUrl, fileName);

                } else {

                    LOG.info("Manifest " + manifestToRefresh + " does not need to be refreshed.");

                }

            } catch (IOException ex) {

                LOG.error("Couldn't refresh manifest " + manifestToRefresh, ex);

            }

        } else {

            refresh(manifestUrl, fileName);
        }


    }

    private void refresh(URL manifestUrl, File manifestToRefresh) {
        FileUtils.deleteQuietly(manifestToRefresh);

        try {
            FileUtils.createParentDirectories(manifestToRefresh);
        } catch (IOException ignored) {

        }

        ReadableByteChannel readableByteChannel = null;
        FileOutputStream fileOutputStream = null;
        FileChannel fileChannel = null;

        try {

            readableByteChannel = Channels.newChannel(manifestUrl.openStream());

            fileOutputStream = new FileOutputStream(manifestToRefresh);

            fileChannel = fileOutputStream.getChannel();

            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

        } catch (IOException ex) {

            LOG.error("An error occurred downloading " + manifestToRefresh + ".", ex);
            FileUtils.deleteQuietly(manifestToRefresh);

        } finally {

            try {
                //noinspection ConstantConditions
                fileOutputStream.flush();
            } catch (Exception ignored) {

            }

            try {
                fileOutputStream.close();
            } catch (Exception ignored) {

            }

            try {
                //noinspection ConstantConditions
                readableByteChannel.close();
            } catch (Exception ignored) {

            }

            try {
                //noinspection ConstantConditions
                fileChannel.close();
            } catch (Exception ignored) {

            }

        }
    }

    /**
     * Reads the Fabric manifest-file into a {@link Document} and {@link Document#normalize()} it.
     * @author Griefed
     * @param manifest The xml-file to parse into a Document.
     * @return Document. Returns the file parsed into a Document.
     */
    private Document getXml(InputStream manifest) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        Document xml = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            assert documentBuilder != null;
            xml = documentBuilder.parse(manifest);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        assert xml != null;
        xml.normalize();
        return xml;
    }

    /**
     * Acquire a {@link JsonNode} from the given json file.
     * @author Griefed
     * @param inputStream {@link InputStream}. The file to read.
     * @return {@link JsonNode} containing the files json data.
     * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
     */
    private JsonNode getJson(InputStream inputStream) throws IOException {
        return getObjectMapper().readTree(inputStream);
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
     * Ensures serverpackcreator.db exists. If the database does not exist, it is created.
     * @author Griefed
     */
    public void checkDatabase() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_DATABASE);

            DatabaseMetaData databaseMetaData = connection.getMetaData();
            LOG.debug("Database driver name: " + databaseMetaData.getDriverName());
            LOG.debug("Database driver version: " + databaseMetaData.getDriverVersion());
            LOG.debug("Database product name: " + databaseMetaData.getDatabaseProductName());
            LOG.debug("Database product version: " + databaseMetaData.getDatabaseProductVersion());

            //Statement statement = connection.createStatement();
            //statement.executeUpdate("CREATE TABLE server_pack_model (id INTEGER, projectID INTEGER, fileID INTEGER, fileName STRING, size DOUBLE, downloads INTEGER, created DATE, confirmedWorking INTEGER)");

        } catch (SQLException ignored) {
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}