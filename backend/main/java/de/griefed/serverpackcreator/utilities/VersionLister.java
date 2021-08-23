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
package de.griefed.serverpackcreator.utilities;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * <strong>table of methods</strong><p>
 * 1. {@link #getMinecraftManifest()}<br>
 * 2. {@link #getForgeManifest()}<br>
 * 3. {@link #getFabricManifest()}<br>
 * 4. {@link #getObjectMapper()}<br>
 * 5. {@link #getMinecraftReleaseVersion()}<br>
 * 6. {@link #getMinecraftReleaseVersions()}<br>
 * 7. {@link #getMinecraftSnapshotVersion()}<br>
 * 8. {@link #getMinecraftSnapshotVersions()}<br>
 * 9. {@link #getFabricVersions()}<br>
 * 10.{@link #getFabricLatestVersion()}<br>
 * 11.{@link #getFabricReleaseVersion()}<br>
 * 12.{@link #getMinecraftReleaseVersionsAsArray()}<br>
 * 13.{@link #getFabricVersionsAsArray()}<br>
 * 14.{@link #getForgeVersionsAsArray(String)}<br>
 * 15.{@link #getMinecraftVersionsList(String)}<br>
 * 16.{@link #setMinecraftSpecificVersion(String)}<br>
 * 17.{@link #getForgeVersionsList(String)}<br>
 * 18.{@link #setFabricVersionList()}<br>
 * 19.{@link #setFabricSpecificVersion(String)}<br>
 * <p>
 * Create lists of versions for Minecraft, Fabric and Forge. Provides getters for retrieving Minecraft and Fabric version
 * lists and {@link #getForgeVersionsList(String)} to retrieve a list of available Forge versions for a given Minecraft
 * version. Instantiating this class automatically creates the Minecraft and Fabric lists, for immediate access through
 * {@link #getMinecraftReleaseVersions()} and {@link #getFabricVersions()} respectively. Also provides getters for retrieving
 * the <code>latest</code> or <code>release</code> versions of Fabric.
 * @author Griefed
 */
public class VersionLister {

    private static final Logger LOG = LogManager.getLogger(VersionLister.class);

    private final File minecraftManifest = new File("./work/minecraft-manifest.json");
    private final File forgeManifest = new File("./work/forge-manifest.json");
    private final File fabricManifest = new File("./work/fabric-manifest.xml");

    private final String minecraftReleaseVersion;
    private final List<String> minecraftReleaseVersions;

    private final String minecraftSnapshotVersion;
    private final List<String> minecraftSnapshotVersions;

    private final List<String> fabricVersions;
    private final String fabricLatestVersion;
    private final String fabricReleaseVersion;

    /**
     * <strong>Constructor</strong><p>
     *     Creates the Minecraft and Fabric version lists as well as Fabric-Latest and Fabric-Release versions.
     * </p>
     * @author Griefed
     */
    public VersionLister() {
        this.minecraftReleaseVersion = setMinecraftSpecificVersion("release");
        this.minecraftReleaseVersions = getMinecraftVersionsList("release");

        this.minecraftSnapshotVersion = setMinecraftSpecificVersion("snapshot");
        this.minecraftSnapshotVersions = getMinecraftVersionsList("snapshot");

        this.fabricVersions = setFabricVersionList();
        this.fabricLatestVersion = setFabricSpecificVersion("latest");
        this.fabricReleaseVersion = setFabricSpecificVersion("release");
    }

    /**
     * Getter for the Minecraft manifest-file.
     * @author Griefed
     * @return File. Returns the location to the Minecraft manifest-file.
     */
    public File getMinecraftManifest() {
        return minecraftManifest;
    }

    /**
     * Getter for the Forge manifest-file.
     * @author Griefed
     * @return File. Returns the location to the Forge manifest-file.
     */
    public File getForgeManifest() {
        return forgeManifest;
    }

    /**
     * Getter for the Fabric manifest-file.
     * @author Griefed
     * @return File. Returns the location to the Fabric manifest-file.
     */
    public File getFabricManifest() {
        return fabricManifest;
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
     * Getter for the latest release version of Minecraft.
     * @author Griefed
     * @return String. Returns the latest release version of Minecraft.
     */
    public String getMinecraftReleaseVersion() {
        return minecraftReleaseVersion;
    }

    /**
     * Getter for the list of available Minecraft versions of type <code>release</code>
     * @author Griefed
     * @return List String. Returns the list of available Minecraft versions.
     */
    public List<String> getMinecraftReleaseVersions() {
        return minecraftReleaseVersions;
    }

    /**
     * Getter for the latest Snapshot version of Minecraft.
     * @author Griefed
     * @return String. Returns the latest snapshot version of Minecraft.
     */
    public String getMinecraftSnapshotVersion() {
        return minecraftSnapshotVersion;
    }

    /**
     * Getter for the list of all available Snapshot version for Minecraft.
     * @author Griefed
     * @return List String. Returns a list of all available Snapshot versions for Minecraft.
     */
    public List<String> getMinecraftSnapshotVersions() {
        return minecraftSnapshotVersions;
    }

    /**
     * Getter for the list of available Fabric versions.
     * @author Griefed
     * @return List String. Returns the list of available Fabric versions.
     */
    public List<String> getFabricVersions() {
        return fabricVersions;
    }

    /**
     * Getter for the latest Fabric version.
     * @author Griefed
     * @return String. Returns the latest Fabric version.
     */
    public String getFabricLatestVersion() {
        return fabricLatestVersion;
    }

    /**
     * Getter for the latest release version of Fabric.
     * @author Griefed
     * @return String. Returns the latest release version of Fabric.
     */
    public String getFabricReleaseVersion() {
        return fabricReleaseVersion;
    }

    /**
     * Getter for the list of Minecraft release versions as an array.
     * @author Griefed
     * @return String Array. Returns the list of Minecraft release versions as an array.
     */
    public String[] getMinecraftReleaseVersionsAsArray() {
        String[] array = new String[this.minecraftReleaseVersions.size()];
        array = this.minecraftReleaseVersions.toArray(array);
        return array;
    }

    /**
     * Getter for the list of Fabric versions as an array.
     * @author Griefed
     * @return String Array. Returns the list of Fabric versions as an array.
     */
    public String[] getFabricVersionsAsArray() {
        String[] array = new String[this.fabricVersions.size()];
        array = this.fabricVersions.toArray(array);
        return array;
    }

    /**
     * Getter for the list of Forge versions for the specified Minecraft version as an array.
     * @author Griefed
     * @param selectedMinecraftVersion The Minecraft version for which to check for Forge versions.
     * @return String Array. Returns the list of Forge versions for the specified Minecraft versions as an array.
     */
    public String[] getForgeVersionsAsArray(String selectedMinecraftVersion) {
        String[] array = new String[getForgeVersionsList(selectedMinecraftVersion).size()];
        array = getForgeVersionsList(selectedMinecraftVersion).toArray(array);
        return array;
    }

    /**
     * Helper method for {@link #getMinecraftVersionsList(String)} and {@link #setMinecraftSpecificVersion(String)}.
     * Reads the passed manifest-file into a byte array and returns a JsonNode containing said byte array.
     * @author Griefed
     * @param manifestFile The file to read into the byte array.
     * @return JsonNode. Returns the JsonNode of the passed manifest-file.
     * @throws IOException Throws an exception when the passed file could not be found/read/parsed etc.
     */
    private JsonNode getJson(File manifestFile) throws IOException {
        byte[] jsonData = Files.readAllBytes(manifestFile.toPath());
        return getObjectMapper().readTree(jsonData);
    }

    /**
     * Parses the Minecraft manifest-file to retrieve a list of all available Minecraft versions for a specified release-type.
     * @author Griefed
     * @param type Release type which determines which version get added to the list returned. Can be <code>release, snapshot, old_beta, old_alpha</code>
     * @return List String. Returns a list of all available Minecraft versions for the specified type.
     */
    private List<String> getMinecraftVersionsList(String type) {

        List<String> minecraftReleases = new ArrayList<>();

        try {
            JsonNode minecraftJson = getJson(getMinecraftManifest());

            JsonNode versions = minecraftJson.get("versions");

            for (JsonNode version : versions) {
                try {
                    if (version.get("type").asText().equals(type)) {
                        minecraftReleases.add(version.get("id").asText());
                    }
                } catch (NullPointerException ignored) {}
            }

        } catch (IOException ex) {
            // TODO: Replace with lang key
            LOG.error("Couldn't read Minecraft manifest.", ex);
        }

        return minecraftReleases;
    }

    /**
     * Retrieve the Minecraft version for the specified release-type.
     * @author Griefed
     * @param type String. Release-type which specifies which Fabric version is retrieved. Can be <code>release, snapshot</code>
     * @return String. Returns the Minecraft version for the specified release-type.
     */
    private String setMinecraftSpecificVersion(String type) {

        String minecraftVersion = null;

        try {
            JsonNode minecraftJson = getJson(getMinecraftManifest());

            minecraftVersion = minecraftJson.get("latest").get(type).asText();

        } catch (IOException ex) {
            // TODO: Replace with lang key
            LOG.error("Couldn't read Minecraft manifest.", ex);
        }

        return minecraftVersion;
    }

    /**
     * Retrieve a list of available Forge versions for the specified Minecraft version. If Forge is not available for a
     * given Minecraft version, <code>"None"</code> is returned as the sole content of the list.
     * @author Griefed
     * @param selectedMinecraftVersion String. The Minecraft version for which to search for available Forge version.
     * @return List String. Returns a list of available Forge versions for the specified Minecraft version.
     */
    public List<String> getForgeVersionsList(String selectedMinecraftVersion) {

        List<String> forgeReleases = new ArrayList<>();

        try {
            JsonNode forgeJson = getJson(getForgeManifest());

            try {
                JsonNode versions = forgeJson.get(selectedMinecraftVersion);

                for (JsonNode version : versions) {
                    forgeReleases.add(version.asText().replace(selectedMinecraftVersion + "-", ""));
                }
            } catch (NullPointerException ignored) {
                forgeReleases.add("None");
            }

        } catch (IOException ex) {
            // TODO: Replace with lang key
            LOG.error("Couldn't read Forge manifest.", ex);
        }

        return forgeReleases;
    }

    /**
     * Helper method for {@link #setFabricVersionList()} and {@link #setFabricSpecificVersion(String)}. Reads the Fabric
     * manifest-file into a {@link Document} and {@link Document#normalize()} it.
     * @author Griefed
     * @return Document. Returns the file parsed into a Document.
     * @throws ParserConfigurationException Throws if the file could not be parsed.
     * @throws SAXException Throws if the file could not be parsed.
     * @throws IOException Throws if the file could not be found/read/parsed etc.
     */
    @NotNull
    private Document getFabricXml() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document xml = documentBuilder.parse(getFabricManifest());
        xml.normalize();
        return xml;
    }

    /**
     * Retrieve a list of all available Fabric versions from the Fabric manifest-file.
     * @author Griefed
     * @return List String. Returns a list of all available Fabric versions.
     */
    private List<String> setFabricVersionList() {

        List<String> fabricReleases = new ArrayList<>();

        try {
            Document fabricXml = getFabricXml();

            LOG.debug("Root node: " + fabricXml.getDocumentElement().getNodeName());

            NodeList versions = fabricXml.getElementsByTagName("version");

            for (int i = 0; i < versions.getLength(); i++) {
                fabricReleases.add(versions.item(i).getChildNodes().item(0).getNodeValue());
            }


        } catch (IOException | ParserConfigurationException | SAXException ex) {
            LOG.error("Couldn't read Fabric manifest.", ex);
        }

        LOG.debug("Fabric versions: " + fabricReleases);

        return fabricReleases;
    }

    /**
     * Retrieve the Fabric version for the specified release-type.
     * @author Griefed
     * @param versionSpecifier String. Release-type which specifies which Fabric version is retrieved. Can be <code>latest, release</code>
     * @return String. Returns the Fabric version for the specified release-type.
     */
    private String setFabricSpecificVersion(String versionSpecifier) {
        String fabricLatestOrRelease = null;

        try {
            Document fabricXml = getFabricXml();

            fabricLatestOrRelease = fabricXml.getElementsByTagName(versionSpecifier).item(0).getChildNodes().item(0).getNodeValue();

        } catch (IOException | ParserConfigurationException | SAXException ex) {
            LOG.error("Couldn't read Fabric manifest.", ex);
        }

        LOG.debug("Fabric versions: " + fabricLatestOrRelease);

        return fabricLatestOrRelease;
    }
}
