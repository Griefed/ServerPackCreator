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
package de.griefed.serverpackcreator.versionmeta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.versionmeta.fabric.FabricMeta;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
import de.griefed.serverpackcreator.versionmeta.quilt.QuiltMeta;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * VersionMeta containing available versions and important details for Minecraft, Fabric and Forge.
 *
 * @author Griefed
 */
@Service
public class VersionMeta {

  private static final Logger LOG = LogManager.getLogger(VersionMeta.class);

  private final ObjectMapper OBJECT_MAPPER;
  private final File MINECRAFT_MANIFEST;
  private final File FORGE_MANIFEST;
  private final File FABRIC_MANIFEST;
  private final File FABRIC_INTERMEDIARIES_MANIFEST;
  private final File FABRIC_INSTALLER_MANIFEST;
  private final File QUILT_MANIFEST;
  private final File QUILT_INSTALLER_MANIFEST;
  private final URL MINECRAFT_MANIFEST_URL =
      new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
  private final URL FORGE_MANIFEST_URL =
      new URL("https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json");
  private final URL FABRIC_MANIFEST_URL =
      new URL("https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml");
  private final URL FABRIC_INTERMEDIARIES_MANIFEST_URL =
      new URL("https://meta.fabricmc.net/v2/versions/intermediary");
  private final URL FABRIC_INSTALLER_MANIFEST_URL =
      new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml");
  private final URL QUILT_MANIFEST_URL =
      new URL(
          "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml");
  private final URL QUILT_INSTALLER_MANIFEST_URL =
      new URL(
          "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/maven-metadata.xml");
  private final MinecraftMeta MINECRAFT_META;
  private final FabricMeta FABRIC_META;
  private final ForgeMeta FORGE_META;
  private final QuiltMeta QUIL_META;

  /**
   * Constructor.
   *
   * @param minecraftManifest {@link File} Minecraft manifest file.
   * @param forgeManifest {@link File} Forge manifest file.
   * @param fabricManifest {@link File} Fabric manifest file.
   * @param fabricIntermediariesManifest {@link File} Fabric Intermediary manifest-file.
   * @param fabricInstallerManifest {@link File} Fabric-installer manifest file.
   * @param quiltManifest {@link File} Quilt manifest file.
   * @param quiltInstallerManifest {@link File} Quilt-installer manifest file.
   * @param injectedObjectMapper {@link ObjectMapper}-instance.
   * @throws IOException if one of the metas could not be initialized.
   * @author Griefed
   */
  @Autowired
  public VersionMeta(
      File minecraftManifest,
      File forgeManifest,
      File fabricManifest,
      File fabricInstallerManifest,
      File fabricIntermediariesManifest,
      File quiltManifest,
      File quiltInstallerManifest,
      ObjectMapper injectedObjectMapper)
      throws IOException {

    this.MINECRAFT_MANIFEST = minecraftManifest;
    this.FORGE_MANIFEST = forgeManifest;
    this.FABRIC_MANIFEST = fabricManifest;
    this.FABRIC_INTERMEDIARIES_MANIFEST = fabricIntermediariesManifest;
    this.FABRIC_INSTALLER_MANIFEST = fabricInstallerManifest;
    this.QUILT_MANIFEST = quiltManifest;
    this.QUILT_INSTALLER_MANIFEST = quiltInstallerManifest;
    this.OBJECT_MAPPER = injectedObjectMapper;

    checkManifests();

    this.FORGE_META = new ForgeMeta(forgeManifest, OBJECT_MAPPER);
    this.MINECRAFT_META = new MinecraftMeta(minecraftManifest, this.FORGE_META, OBJECT_MAPPER);
    this.FABRIC_META =
        new FabricMeta(
            fabricManifest, fabricInstallerManifest, fabricIntermediariesManifest, OBJECT_MAPPER);
    this.FORGE_META.initialize(this.MINECRAFT_META);
    this.QUIL_META = new QuiltMeta(quiltManifest, quiltInstallerManifest);

    this.MINECRAFT_META.update();
    this.FABRIC_META.update();
    this.FORGE_META.update();
    this.QUIL_META.update();
  }

  /**
   * Check all our manifests, those being Minecraft, Forge, Fabric and Fabric Installer, for whether
   * updated manifests are available, by comparing their locally stored ones against freshly
   * downloaded ones. If a manifest does not exist yet, it is downloaded to the specified file with
   * which this instance of {@link VersionMeta} was created.
   *
   * @author Griefed
   */
  private void checkManifests() {
    checkManifest(MINECRAFT_MANIFEST, MINECRAFT_MANIFEST_URL, Type.MINECRAFT);
    checkManifest(FORGE_MANIFEST, FORGE_MANIFEST_URL, Type.FORGE);
    checkManifest(FABRIC_MANIFEST, FABRIC_MANIFEST_URL, Type.FABRIC);
    checkManifest(
        FABRIC_INTERMEDIARIES_MANIFEST,
        FABRIC_INTERMEDIARIES_MANIFEST_URL,
        Type.FABRIC_INTERMEDIARIES);
    checkManifest(FABRIC_INSTALLER_MANIFEST, FABRIC_INSTALLER_MANIFEST_URL, Type.FABRIC_INSTALLER);
    checkManifest(QUILT_MANIFEST, QUILT_MANIFEST_URL, Type.QUILT);
    checkManifest(QUILT_INSTALLER_MANIFEST, QUILT_INSTALLER_MANIFEST_URL, Type.QUILT_INSTALLER);
  }

  /**
   * Check a given manifest for updates.<br>
   * If it does not exist, it is downloaded and stored.<br>
   * If it exists, it is compared to the online manifest.<br>
   * If the online version contains more versions, the local manifests are replaced by the online
   * ones.
   *
   * @param manifestToCheck {@link File} The manifest to check.
   * @param urlToManifest {@link URL} The URL to the manifest.
   * @param manifestType {@link Type} The type of the manifest, either {@link Type#MINECRAFT},
   *     {@link Type#FORGE}, {@link Type#FABRIC} or {@link Type#FABRIC_INSTALLER}.
   * @author Griefed
   */
  private void checkManifest(File manifestToCheck, URL urlToManifest, Type manifestType) {
    if (manifestToCheck.exists()) {

      try (InputStream existing = Files.newInputStream(manifestToCheck.toPath());
          InputStream newManifest = urlToManifest.openStream()) {

        int countOldFile = 0;
        int countNewFile = 0;

        switch (manifestType) {
          case MINECRAFT:
            countOldFile = getJson(existing).get("versions").size();
            countNewFile = getJson(newManifest).get("versions").size();

            break;

          case FORGE:
            for (JsonNode mcVer : getJson(existing)) {
              countOldFile += mcVer.size();
            }
            for (JsonNode mcVer : getJson(newManifest)) {
              countNewFile += mcVer.size();
            }

            break;

          case FABRIC_INTERMEDIARIES:
            countOldFile = getJson(existing).size();
            countNewFile = getJson(newManifest).size();

            break;

          case FABRIC:
          case FABRIC_INSTALLER:
          case QUILT:
          case QUILT_INSTALLER:
            countOldFile = getXml(existing).getElementsByTagName("version").getLength();
            countNewFile = getXml(newManifest).getElementsByTagName("version").getLength();

            break;

          default:
            throw new InvalidTypeException(
                "Manifest type must be either Type.MINECRAFT, Type.FORGE, Type.FABRIC or Type.FABRIC_INSTALLER. Specified: "
                    + manifestType);
        }

        LOG.debug("Nodes/Versions/Size in/of old " + manifestToCheck + ": " + countOldFile);
        LOG.debug("Nodes/Versions/Size in/of new " + manifestToCheck + ": " + countNewFile);

        if (countNewFile > countOldFile) {

          LOG.info("Refreshing " + manifestToCheck + ".");

          updateManifest(manifestToCheck, urlToManifest);

        } else {

          LOG.info("Manifest " + manifestToCheck + " does not need to be refreshed.");
        }

      } catch (IOException | InvalidTypeException ex) {

        LOG.error("Couldn't refresh manifest " + manifestToCheck, ex);
      }

    } else {

      updateManifest(manifestToCheck, urlToManifest);
    }
  }

  /**
   * Deletes the specified manifest if it is found, then downloads the specified manifest file
   * again. Ensures we always have the latest manifest for version validation available.
   *
   * @param manifestToRefresh The manifest file to delete and then download, updating it.
   * @param urlToManifest The URL to the file which is to be downloaded.
   * @author whitebear60
   * @author Griefed
   */
  private void updateManifest(File manifestToRefresh, URL urlToManifest) {
    FileUtils.deleteQuietly(manifestToRefresh);

    try {
      FileUtils.createParentDirectories(manifestToRefresh);
    } catch (IOException ignored) {

    }

    ReadableByteChannel readableByteChannel = null;
    FileOutputStream fileOutputStream = null;
    FileChannel fileChannel = null;

    try {

      readableByteChannel = Channels.newChannel(urlToManifest.openStream());

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
   * Acquire a {@link JsonNode} from the given json file.
   *
   * @param inputStream {@link InputStream}. The file to read.
   * @return {@link JsonNode} containing the files json data.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  private JsonNode getJson(InputStream inputStream) throws IOException {
    return OBJECT_MAPPER.readTree(inputStream);
  }

  /**
   * Reads the Fabric manifest-file into a {@link Document} and {@link Document#normalize()} it.
   *
   * @param manifest The xml-file to parse into a Document.
   * @return Document. Returns the file parsed into a Document.
   * @author Griefed
   */
  private Document getXml(InputStream manifest) {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = null;
    Document xml = null;

    try {

      documentBuilder = documentBuilderFactory.newDocumentBuilder();

    } catch (ParserConfigurationException ex) {
      LOG.error("Couldn't read document.", ex);
    }

    try {

      assert documentBuilder != null;
      xml = documentBuilder.parse(manifest);

    } catch (SAXException | IOException ex) {
      LOG.error("Couldn't read document.", ex);
    }

    assert xml != null;
    xml.normalize();
    return xml;
  }

  /**
   * Update the Minecraft, Forge and Fabric metas. Usually called when the manifest files have been
   * refreshed.
   *
   * @return Instance of {@link VersionMeta}.
   * @throws IOException if any of the metas could not be updated.
   * @author Griefed
   */
  public VersionMeta update() throws IOException {
    checkManifests();
    this.MINECRAFT_META.update();
    this.FABRIC_META.update();
    this.FORGE_META.update();
    return this;
  }

  /**
   * The MinecraftMeta instance for working with Minecraft versions and information about them.
   *
   * @return Instance of {@link MinecraftMeta}.
   * @author Griefed
   */
  public MinecraftMeta minecraft() {
    return MINECRAFT_META;
  }

  /**
   * The QuiltMeta-instance for working with Fabric versions and information about them.
   *
   * @return Instance of {@link FabricMeta}.
   * @author Griefed
   */
  public FabricMeta fabric() {
    return FABRIC_META;
  }

  /**
   * The ForgeMeta-instance for working with Forge versions and information about them.
   *
   * @return Instance of {@link ForgeMeta}.
   * @author Griefed
   */
  public ForgeMeta forge() {
    return FORGE_META;
  }

  /**
   * The QuiltMeta-instance for working with Quilt versions and information about them.
   *
   * @return Instance of {@link QuiltMeta}.
   * @author Griefed
   */
  public QuiltMeta quilt() {
    return QUIL_META;
  }
}
