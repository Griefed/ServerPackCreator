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
package de.griefed.serverpackcreator.versionmeta.fabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.versionmeta.ManifestParser;
import de.griefed.serverpackcreator.versionmeta.Meta;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Fabric meta containing information about available Quilt versions and installers.
 *
 * @author Griefed
 */
public final class FabricMeta extends ManifestParser implements Meta {

  private final File FABRIC_MANIFEST;
  private final File FABRIC_INSTALLER_MANIFEST;
  private final FabricLoader FABRIC_LOADER;
  private final FabricLoaderDetails FABRIC_LOADER_DETAILS;
  private final FabricInstaller FABRIC_INSTALLER;
  private final FabricIntermediaries FABRIC_INTERMEDIARIES;
  private final HashMap<String, FabricDetails> LOADER_DETAILS = new HashMap<>(100);

  /**
   * Create a new Fabric Meta instance, giving you access to available loader and installer
   * versions, as well as URLs to installer for further processing.
   *
   * @param fabricManifest               Fabric manifest file.
   * @param fabricInstallerManifest      Fabric-installer manifest file.
   * @param injectedFabricIntermediaries Fabric Intermediary instance.
   * @param objectMapper                 Object mapper for JSON parsing.
   * @author Griefed
   */
  public FabricMeta(
      File fabricManifest,
      File fabricInstallerManifest,
      FabricIntermediaries injectedFabricIntermediaries,
      ObjectMapper objectMapper) {

    FABRIC_LOADER_DETAILS = new FabricLoaderDetails(objectMapper);
    FABRIC_MANIFEST = fabricManifest;
    FABRIC_INSTALLER_MANIFEST = fabricInstallerManifest;
    FABRIC_LOADER = new FabricLoader(FABRIC_MANIFEST);
    FABRIC_INTERMEDIARIES = injectedFabricIntermediaries;
    FABRIC_INSTALLER = new FabricInstaller(FABRIC_INSTALLER_MANIFEST);
  }

  @Override
  public void update() throws ParserConfigurationException, IOException, SAXException {
    FABRIC_LOADER.update();
    FABRIC_INSTALLER.update();
  }

  @Override
  public String latestLoader() {
    return FABRIC_LOADER.latestLoaderVersion();
  }

  @Override
  public String releaseLoader() {
    return FABRIC_LOADER.releaseLoaderVersion();
  }

  @Override
  public String latestInstaller() {
    return FABRIC_INSTALLER.latestInstallerVersion();
  }

  @Override
  public String releaseInstaller() {
    return FABRIC_INSTALLER.releaseInstallerVersion();
  }

  @Override
  public List<String> loaderVersionsListAscending() {
    return FABRIC_LOADER.loaders();
  }

  @Override
  public List<String> loaderVersionsListDescending() {
    return Lists.reverse(FABRIC_LOADER.loaders());
  }

  @Override
  public String[] loaderVersionsArrayAscending() {
    return FABRIC_LOADER.loaders().toArray(new String[0]);
  }

  @Override
  public String[] loaderVersionsArrayDescending() {
    return Lists.reverse(FABRIC_LOADER.loaders()).toArray(new String[0]);
  }

  @Override
  public List<String> installerVersionsListAscending() {
    return FABRIC_INSTALLER.installers();
  }

  @Override
  public List<String> installerVersionsListDescending() {
    return Lists.reverse(FABRIC_INSTALLER.installers());
  }

  @Override
  public String[] installerVersionsArrayAscending() {
    return FABRIC_INSTALLER.installers().toArray(new String[0]);
  }

  @Override
  public String[] installerVersionsArrayDescending() {
    return Lists.reverse(FABRIC_INSTALLER.installers()).toArray(new String[0]);
  }

  @Override
  public URL latestInstallerUrl() {
    return FABRIC_INSTALLER.latestInstallerUrl();
  }

  @Override
  public URL releaseInstallerUrl() {
    return FABRIC_INSTALLER.releaseInstallerUrl();
  }

  @Override
  public boolean isInstallerUrlAvailable(String fabricVersion) {
    return Optional.ofNullable(FABRIC_INSTALLER.meta().get(fabricVersion)).isPresent();
  }

  @Override
  public Optional<URL> getInstallerUrl(String fabricVersion) {
    return Optional.ofNullable(FABRIC_INSTALLER.meta().get(fabricVersion));
  }

  @Override
  public boolean isVersionValid(String fabricVersion) {
    return FABRIC_LOADER.loaders().contains(fabricVersion);
  }

  @Override
  public boolean isMinecraftSupported(String minecraftVersion) {
    return FABRIC_INTERMEDIARIES.getIntermediary(minecraftVersion).isPresent();
  }

  /**
   * Get the {@link URL} to the Fabric launcher for the specified Minecraft and Fabric version.
   *
   * @param minecraftVersion Minecraft version.
   * @param fabricVersion    Fabric version.
   * @return URL to the Fabric launcher for the specified Minecraft and Fabric version.
   * @author Griefed
   */
  public Optional<URL> improvedLauncherUrl(String minecraftVersion,
                                           String fabricVersion) {
    return FABRIC_INSTALLER.improvedLauncherUrl(minecraftVersion, fabricVersion);
  }

  /**
   * Get details for a Fabric loader.
   *
   * @param minecraftVersion Minecraft version.
   * @param fabricVersion    Fabric version.
   * @return Details of a Fabric loader for the given Minecraft and Fabric version, wrapped in an
   * {@link Optional}.
   * @author Griefed
   */
  public Optional<FabricDetails> getLoaderDetails(String minecraftVersion,
                                                  String fabricVersion) {
    String key = minecraftVersion + "-" + fabricVersion;

    if (LOADER_DETAILS.containsKey(key)) {

      return Optional.of(LOADER_DETAILS.get(key));

    } else if (FABRIC_LOADER_DETAILS.getDetails(minecraftVersion, fabricVersion).isPresent()) {

      LOADER_DETAILS.put(
          key, FABRIC_LOADER_DETAILS.getDetails(minecraftVersion, fabricVersion).get());
      return Optional.of(LOADER_DETAILS.get(key));

    } else {

      return Optional.empty();
    }
  }
}
