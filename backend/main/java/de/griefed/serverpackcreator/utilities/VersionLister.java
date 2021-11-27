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
import de.griefed.serverpackcreator.ApplicationProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Create lists of versions for Minecraft, Fabric and Forge. Provides getters for retrieving Minecraft and Fabric version
 * lists and {@link #getForgeVersionsList(String)} to retrieve a list of available Forge versions for a given Minecraft
 * version. Instantiating this class automatically creates the Minecraft and Fabric lists, for immediate access through
 * {@link #getMinecraftReleaseVersions()} and {@link #getFabricVersions()} respectively. Also provides getters for retrieving
 * the <code>latest</code> or <code>release</code> versions of Fabric.
 * @author Griefed
 */
@Component
public class VersionLister {

    private static final Logger LOG = LogManager.getLogger(VersionLister.class);

    private String minecraftReleaseVersion;
    private List<String> minecraftReleaseVersions;

    private String minecraftSnapshotVersion;
    private List<String> minecraftSnapshotVersions;

    private List<String> fabricVersions;
    private String fabricLatestVersion;
    private String fabricReleaseVersion;

    private String fabricLatestInstallerVersion;
    private String fabricReleaseInstallerVersion;

    private HashMap<String, String[]> forgeMeta;

    private ApplicationProperties applicationProperties;

    /**
     * Creates the Minecraft and Fabric version lists as well as Fabric-Latest and Fabric-Release versions.
     * @author Griefed
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     */
    @Autowired
    public VersionLister(ApplicationProperties injectedApplicationProperties) {
        if (injectedApplicationProperties == null) {
            this.applicationProperties = new ApplicationProperties();
        } else {
            this.applicationProperties = injectedApplicationProperties;
        }

        this.minecraftReleaseVersion = setMinecraftSpecificVersion("release");
        this.minecraftReleaseVersions = getMinecraftVersionsList("release");

        this.minecraftSnapshotVersion = setMinecraftSpecificVersion("snapshot");
        this.minecraftSnapshotVersions = getMinecraftVersionsList("snapshot");

        this.fabricVersions = setFabricVersionList();
        this.fabricLatestVersion = setFabricSpecificVersion("latest", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC));
        this.fabricReleaseVersion = setFabricSpecificVersion("release", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC));

        this.fabricLatestInstallerVersion = setFabricSpecificVersion("latest", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC_INSTALLER));
        this.fabricReleaseInstallerVersion = setFabricSpecificVersion("release", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC_INSTALLER));

        this.forgeMeta = setForgeMeta();
    }

    /**
     *
     * @author Griefed
     */
    public void refreshVersions() {
        this.minecraftReleaseVersion = setMinecraftSpecificVersion("release");
        this.minecraftReleaseVersions = getMinecraftVersionsList("release");

        this.minecraftSnapshotVersion = setMinecraftSpecificVersion("snapshot");
        this.minecraftSnapshotVersions = getMinecraftVersionsList("snapshot");

        this.fabricVersions = setFabricVersionList();
        this.fabricLatestVersion = setFabricSpecificVersion("latest", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC));
        this.fabricReleaseVersion = setFabricSpecificVersion("release", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC));

        this.fabricLatestInstallerVersion = setFabricSpecificVersion("latest", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC_INSTALLER));
        this.fabricReleaseInstallerVersion = setFabricSpecificVersion("release", getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC_INSTALLER));

        this.forgeMeta = setForgeMeta();
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
     * Getter for the Forge version meta in convenient HashMap format. Keys are Minecraft versions, values are String arrays
     * containing all available Forge versions, or <code>None</code> if Forge is not available for a given Minecraft version.
     * @author Griefed
     * @return HashMap String, String Array. Returns the HashMap with all Minecraft versions as keys and the Forge versions
     * as values, in arrays.
     */
    public HashMap<String, String[]> getForgeMeta() {
        return forgeMeta;
    }

    /**
     * Create a HashMap of all Minecraft versions and their available Forge versions.
     * @author Griefed
     * @return HashMap String, String Array. Returns the HashMap with all Minecraft versions as keys and the Forge versions
     * as values, in arrays.
     */
    private HashMap<String, String[]> setForgeMeta() {
        HashMap<String, String[]> hashMap = new HashMap<>();

        for (String version : minecraftReleaseVersions) {
            hashMap.put(version, reverseOrderArray(getForgeVersionsAsArray(version)));
        }

        return hashMap;
    }

    /**
     * Getter for the release version of the Fabric installer.
     * @author Griefed
     * @return String. Returns the latest installer version for Fabric.
     */
    public String getFabricLatestInstallerVersion() {
        return fabricLatestInstallerVersion;
    }

    /**
     * Getter for the release version of the Fabric installer.
     * @author Griefed
     * @return String. Returns the release installer version for Fabric.
     */
    public String getFabricReleaseInstallerVersion() {
        return fabricReleaseInstallerVersion;
    }

    /**
     * Reverses the order of a passed String List.
     * @author Griefed
     * @param listToReverse The String List to reverseOrderArray-order.
     * @return String List. The passed String List in reverseOrderArray-order.
     */
    public List<String> reverseOrderList(List<String> listToReverse) {
        Collections.reverse(listToReverse);
        return listToReverse;
    }

    /**
     * Reverses the order of a passed String array.
     * @author Griefed
     * @param arrayToReverse The String array to reverseOrderArray-order.
     * @return String Array. The passed String array in reverseOrderArray-order.
     */
    public String[] reverseOrderArray(String[] arrayToReverse) {
        int arrayLength = arrayToReverse.length;
        String entry;
        for (int i = 0; i < arrayLength / 2; i++) {
            entry = arrayToReverse[i];
            arrayToReverse[i] = arrayToReverse[arrayLength - i - 1];
            arrayToReverse[arrayLength - i - 1] = entry;
        }
        return arrayToReverse;
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
            JsonNode minecraftJson = getJson(applicationProperties.PATH_FILE_MANIFEST_MINECRAFT);

            JsonNode versions = minecraftJson.get("versions");

            for (JsonNode version : versions) {
                try {
                    if (version.get("type").asText().equals(type)) {
                        minecraftReleases.add(version.get("id").asText());
                    }
                } catch (NullPointerException ignored) {}
            }

        } catch (IOException ex) {
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
            JsonNode minecraftJson = getJson(applicationProperties.PATH_FILE_MANIFEST_MINECRAFT);

            minecraftVersion = minecraftJson.get("latest").get(type).asText();

        } catch (IOException ex) {
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
            JsonNode forgeJson = getJson(applicationProperties.PATH_FILE_MANIFEST_FORGE);

            try {
                JsonNode versions = forgeJson.get(selectedMinecraftVersion);

                for (JsonNode version : versions) {
                    forgeReleases.add(version.asText().replace(selectedMinecraftVersion + "-", ""));
                }
            } catch (NullPointerException ignored) {
                forgeReleases.add("None");
            }

        } catch (IOException ex) {
            LOG.error("Couldn't read Forge manifest.", ex);
        }

        return forgeReleases;
    }

    /**
     * Helper method for {@link #setFabricVersionList()} and {@link #setFabricSpecificVersion(String, Document)}. Reads the Fabric
     * manifest-file into a {@link Document} and {@link Document#normalize()} it.
     * @author Griefed
     * @param manifest The xml-file to parse into a Document.
     * @return Document. Returns the file parsed into a Document.
     */
    @NotNull
    private Document getXml(File manifest) {
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
     * Retrieve a list of all available Fabric versions from the Fabric manifest-file.
     * @author Griefed
     * @return List String. Returns a list of all available Fabric versions.
     */
    private List<String> setFabricVersionList() {

        List<String> fabricReleases = new ArrayList<>();

        Document fabricXml = getXml(applicationProperties.PATH_FILE_MANIFEST_FABRIC);

        NodeList versions = fabricXml.getElementsByTagName("version");

        for (int i = 0; i < versions.getLength(); i++) {
            fabricReleases.add(versions.item(i).getChildNodes().item(0).getNodeValue());
        }

        LOG.debug("Fabric versions: " + fabricReleases);

        return fabricReleases;
    }

    /**
     * Retrieve the Fabric version for the specified release-type.
     * @author Griefed
     * @param versionSpecifier String. Release-type which specifies which Fabric version is retrieved. Can be <code>latest, release</code>
     * @param manifest Document. The document from which to gather information from.
     * @return String. Returns the Fabric version for the specified release-type.
     */
    private String setFabricSpecificVersion(String versionSpecifier, Document manifest) {
        return manifest.getElementsByTagName(versionSpecifier).item(0).getChildNodes().item(0).getNodeValue();
    }
}
