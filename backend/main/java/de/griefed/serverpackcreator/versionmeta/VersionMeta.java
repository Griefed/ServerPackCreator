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
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.utilities.common.JarUtilities;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.utilities.common.WebUtilities;
import de.griefed.serverpackcreator.versionmeta.fabric.FabricIntermediaries;
import de.griefed.serverpackcreator.versionmeta.fabric.FabricMeta;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import de.griefed.serverpackcreator.versionmeta.legacyfabric.LegacyFabricMeta;
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
import de.griefed.serverpackcreator.versionmeta.quilt.QuiltMeta;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * VersionMeta containing available versions and important details for Minecraft, Fabric and Forge.
 *
 * @author Griefed
 */
@Service
public final class VersionMeta extends ManifestParser {

  private static final Logger LOG = LogManager.getLogger(VersionMeta.class);

  private final ObjectMapper OBJECT_MAPPER;
  private final File MINECRAFT_MANIFEST;
  private final File FORGE_MANIFEST;
  private final File FABRIC_MANIFEST;
  private final File FABRIC_INTERMEDIARIES_MANIFEST;
  private final File FABRIC_INSTALLER_MANIFEST;
  private final File QUILT_MANIFEST;
  private final File QUILT_INSTALLER_MANIFEST;
  private final File LEGACY_FABRIC_GAME_MANIFEST;
  private final File LEGACY_FABRIC_LOADER_MANIFEST;
  private final File LEGACY_FABRIC_INSTALLER_MANIFEST;
  private final String FABRIC_LEGACY_BASE_URL = "https://meta.legacyfabric.net/";
  private final URL LEGACY_FABRIC_GAME_MANIFEST_URL = new URL(
      FABRIC_LEGACY_BASE_URL + "v2/versions/game");
  private final URL LEGACY_FABRIC_LOADER_MANIFEST_URL = new URL(
      FABRIC_LEGACY_BASE_URL + "v2/versions/loader");
  private final URL LEGACY_FABRIC_INSTALLER_MANIFEST_URL = new URL(
      "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/maven-metadata.xml");
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
  private final FabricIntermediaries FABRIC_INTERMEDIARIES;
  private final LegacyFabricMeta LEGACY_FABRIC_META;
  private final WebUtilities WEB_UTILITIES;
  private final JarUtilities JAR_UTILITIES;

  /**
   * Constructor.
   *
   * @param minecraftManifest             Minecraft manifest file.
   * @param forgeManifest                 Forge manifest file.
   * @param fabricManifest                Fabric manifest file.
   * @param fabricIntermediariesManifest  Fabric Intermediary manifest-file.
   * @param fabricInstallerManifest       Fabric-installer manifest file.
   * @param quiltManifest                 Quilt manifest file.
   * @param quiltInstallerManifest        Quilt-installer manifest file.
   * @param injectedObjectMapper          Object mapper-instance for JSON parsing.
   * @param legacyFabricGameManifest      Fabric Legacy Game manifest file.
   * @param legacyFabricLoaderManifest    Fabric Legacy Loader manifest file.
   * @param legacyFabricInstallerManifest Fabric Legacy Installer manifest file.
   * @param injectedUtilities             Instance of commonly used utilities.
   * @param injectedApplicationProperties ServerPackCreator settings.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException                  if any IO errors occur.
   * @throws SAXException                 if any parse errors occur.
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
      File legacyFabricGameManifest,
      File legacyFabricLoaderManifest,
      File legacyFabricInstallerManifest,
      ObjectMapper injectedObjectMapper,
      Utilities injectedUtilities,
      ApplicationProperties injectedApplicationProperties)
      throws IOException, ParserConfigurationException, SAXException {

    MINECRAFT_MANIFEST = minecraftManifest;
    FORGE_MANIFEST = forgeManifest;
    FABRIC_MANIFEST = fabricManifest;
    LEGACY_FABRIC_GAME_MANIFEST = legacyFabricGameManifest;
    LEGACY_FABRIC_LOADER_MANIFEST = legacyFabricLoaderManifest;
    LEGACY_FABRIC_INSTALLER_MANIFEST = legacyFabricInstallerManifest;
    FABRIC_INTERMEDIARIES_MANIFEST = fabricIntermediariesManifest;
    FABRIC_INSTALLER_MANIFEST = fabricInstallerManifest;
    QUILT_MANIFEST = quiltManifest;
    QUILT_INSTALLER_MANIFEST = quiltInstallerManifest;
    OBJECT_MAPPER = injectedObjectMapper;
    WEB_UTILITIES = injectedUtilities.WebUtils();
    JAR_UTILITIES = injectedUtilities.JarUtils();

    checkManifests();

    FORGE_META = new ForgeMeta(
        FORGE_MANIFEST,
        OBJECT_MAPPER);

    MINECRAFT_META = new MinecraftMeta(
        MINECRAFT_MANIFEST,
        FORGE_META,
        OBJECT_MAPPER,
        injectedUtilities,
        injectedApplicationProperties);

    FABRIC_INTERMEDIARIES = new FabricIntermediaries(
        FABRIC_INTERMEDIARIES_MANIFEST,
        OBJECT_MAPPER);

    LEGACY_FABRIC_META = new LegacyFabricMeta(
        LEGACY_FABRIC_GAME_MANIFEST,
        LEGACY_FABRIC_LOADER_MANIFEST,
        LEGACY_FABRIC_INSTALLER_MANIFEST,
        OBJECT_MAPPER);

    FABRIC_META = new FabricMeta(
        FABRIC_MANIFEST,
        FABRIC_INSTALLER_MANIFEST,
        FABRIC_INTERMEDIARIES,
        OBJECT_MAPPER);

    FORGE_META.initialize(MINECRAFT_META);

    QUIL_META = new QuiltMeta(
        QUILT_MANIFEST,
        QUILT_INSTALLER_MANIFEST,
        FABRIC_INTERMEDIARIES);

    MINECRAFT_META.update();
    FABRIC_INTERMEDIARIES.update();
    FABRIC_META.update();
    LEGACY_FABRIC_META.update();
    FORGE_META.update();
    QUIL_META.update();
  }

  /**
   * Check all our manifests, those being Minecraft, Forge, Fabric and Fabric Installer, for whether
   * updated manifests are available, by comparing their locally stored ones against freshly
   * downloaded ones. If a manifest does not exist yet, it is downloaded to the specified file with
   * which this instance of the version meta was created.
   *
   * @author Griefed
   */
  private void checkManifests() {
    checkManifest(
        MINECRAFT_MANIFEST,
        MINECRAFT_MANIFEST_URL,
        Type.MINECRAFT);

    checkManifest(
        FORGE_MANIFEST,
        FORGE_MANIFEST_URL,
        Type.FORGE);

    checkManifest(
        FABRIC_INTERMEDIARIES_MANIFEST,
        FABRIC_INTERMEDIARIES_MANIFEST_URL,
        Type.FABRIC_INTERMEDIARIES);

    checkManifest(
        LEGACY_FABRIC_GAME_MANIFEST,
        LEGACY_FABRIC_GAME_MANIFEST_URL,
        Type.LEGACY_FABRIC);

    checkManifest(
        LEGACY_FABRIC_LOADER_MANIFEST,
        LEGACY_FABRIC_LOADER_MANIFEST_URL,
        Type.LEGACY_FABRIC);

    checkManifest(
        LEGACY_FABRIC_INSTALLER_MANIFEST,
        LEGACY_FABRIC_INSTALLER_MANIFEST_URL,
        Type.LEGACY_FABRIC);

    checkManifest(
        FABRIC_MANIFEST,
        FABRIC_MANIFEST_URL,
        Type.FABRIC);

    checkManifest(
        FABRIC_INSTALLER_MANIFEST,
        FABRIC_INSTALLER_MANIFEST_URL,
        Type.FABRIC_INSTALLER);

    checkManifest(
        QUILT_MANIFEST,
        QUILT_MANIFEST_URL,
        Type.QUILT);

    checkManifest(
        QUILT_INSTALLER_MANIFEST,
        QUILT_INSTALLER_MANIFEST_URL,
        Type.QUILT_INSTALLER);
  }

  /**
   * Check a given manifest for updates.<br> If it does not exist, it is downloaded and stored.<br>
   * If it exists, it is compared to the online manifest.<br> If the online version contains more
   * versions, the local manifests are replaced by the online ones.
   *
   * @param manifestToCheck The manifest to check.
   * @param urlToManifest   The URL to the manifest.
   * @param manifestType    The type of the manifest, either {@link Type#MINECRAFT},
   *                        {@link Type#FORGE}, {@link Type#FABRIC} or
   *                        {@link Type#FABRIC_INSTALLER}.
   * @author Griefed
   */
  private void checkManifest(File manifestToCheck, URL urlToManifest, Type manifestType) {
    if (manifestToCheck.isFile()) {
      if (!WEB_UTILITIES.isReachable(urlToManifest)) {
        LOG.warn(
            "Can not connect to " + urlToManifest + " to check for update(s) of " + manifestToCheck
                + ".");
        return;
      }
      try (InputStream existing = Files.newInputStream(manifestToCheck.toPath());
          InputStream newManifest = urlToManifest.openStream()) {

        int countOldFile = 0;
        int countNewFile = 0;

        String oldContent = StreamUtils.copyToString(
            existing,
            StandardCharsets.UTF_8);

        String newContent = StreamUtils.copyToString(
            newManifest,
            StandardCharsets.UTF_8);

        switch (manifestType) {
          case MINECRAFT:
            countOldFile = getJson(oldContent, OBJECT_MAPPER).get("versions").size();
            countNewFile = getJson(newContent, OBJECT_MAPPER).get("versions").size();

            break;

          case FORGE:
            for (JsonNode mcVer : getJson(oldContent, OBJECT_MAPPER)) {
              countOldFile += mcVer.size();
            }
            for (JsonNode mcVer : getJson(newContent, OBJECT_MAPPER)) {
              countNewFile += mcVer.size();
            }

            break;

          case FABRIC_INTERMEDIARIES:
            countOldFile = getJson(oldContent, OBJECT_MAPPER).size();
            countNewFile = getJson(newContent, OBJECT_MAPPER).size();

            break;

          case FABRIC:
          case FABRIC_INSTALLER:
          case QUILT:
          case QUILT_INSTALLER:
            countOldFile = getXml(oldContent).getElementsByTagName("version").getLength();
            countNewFile = getXml(newContent).getElementsByTagName("version").getLength();

            break;

          case LEGACY_FABRIC:
            if (manifestToCheck.getName().endsWith(".json")) {

              countOldFile = getJson(oldContent, OBJECT_MAPPER).size();
              countNewFile = getJson(newContent, OBJECT_MAPPER).size();

            } else {

              Document oldXML = getXml(oldContent);
              Document newXML = getXml(newContent);

              countOldFile = oldXML.getElementsByTagName("version").getLength();
              countNewFile = newXML.getElementsByTagName("version").getLength();

              if (countOldFile == countNewFile) {

                if (!oldXML.getElementsByTagName("version").item(0).getChildNodes().item(0)
                    .getNodeValue()
                    .equals(
                        newXML.getElementsByTagName("version").item(0).getChildNodes().item(0)
                            .getNodeValue())) {

                  countNewFile += 1;
                }
              }
            }
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

          updateManifest(manifestToCheck, newContent);

        } else {

          LOG.info("Manifest " + manifestToCheck + " does not need to be refreshed.");
        }

      } catch (SAXException ex) {

        JAR_UTILITIES.copyFileFromJar(
            "de/griefed/resources/manifests/" + manifestToCheck.getName(),
            manifestToCheck,
            true,
            VersionMeta.class
        );
        LOG.error("Unexpected end of file in XML-manifest. Restoring default "
            + manifestToCheck.getPath());

      } catch (ParserConfigurationException | IOException |
               InvalidTypeException ex) {

        LOG.error("Couldn't refresh manifest " + manifestToCheck, ex);

      }

    } else {
      if (!WEB_UTILITIES.isReachable(urlToManifest)) {
        LOG.error("CRITICAL!" + manifestToCheck + " not present and " + urlToManifest
            + " unreachable. Exiting...");
        LOG.error(
            "ServerPackCreator should have provided default manifests. Please report this on GitHub at https://github.com/Griefed/ServerPackCreator/issues/new?assignees=Griefed&labels=bug&template=bug-report.yml&title=%5BBug%5D%3A+");
        LOG.error("Make sure you include this log when reporting an error! Please....");
        System.exit(1);
      } else {
        updateManifest(manifestToCheck, urlToManifest);
      }
    }
  }

  /**
   * Ensures we always have the latest manifest for version validation available.
   *
   * @param manifestToRefresh The manifest file to update.
   * @param content           The content to write to the new manifest.
   * @author whitebear60
   * @author Griefed
   */
  private void updateManifest(File manifestToRefresh, String content) throws IOException {
    try {
      FileUtils.createParentDirectories(manifestToRefresh);
    } catch (IOException ignored) {

    }
    FileUtils.writeStringToFile(manifestToRefresh, content, StandardCharsets.UTF_8);
  }

  /**
   * Ensures we always have the latest manifest for version validation available.
   *
   * @param manifestToRefresh The manifest file to update.
   * @param urlToManifest     The URL to the file which is to be downloaded.
   * @author whitebear60
   * @author Griefed
   */
  private void updateManifest(File manifestToRefresh, URL urlToManifest) {
    try (InputStream stream = urlToManifest.openStream()) {

      String manifestText = StreamUtils.copyToString(stream,
          StandardCharsets.UTF_8);

      updateManifest(manifestToRefresh, manifestText);

    } catch (IOException ex) {
      LOG.error("An error occurred refreshing " + manifestToRefresh + ".", ex);
    }
  }

  /**
   * Update the Minecraft, Forge and Fabric metas. Usually called when the manifest files have been
   * refreshed.
   *
   * @return The instance of of this version meta, updated.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException                  if any IO errors occur.
   * @throws SAXException                 if any parse errors occur.
   * @author Griefed
   */
  public VersionMeta update() throws IOException, ParserConfigurationException, SAXException {
    checkManifests();
    MINECRAFT_META.update();
    FABRIC_INTERMEDIARIES.update();
    FABRIC_META.update();
    LEGACY_FABRIC_META.update();
    FORGE_META.update();
    QUIL_META.update();
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

  /**
   * The LegacyFabric-instance for working with Legacy Fabric versions and information about them.
   *
   * @return Instance of {@link LegacyFabricMeta}.
   */
  public LegacyFabricMeta legacyFabric() {
    return LEGACY_FABRIC_META;
  }
}
