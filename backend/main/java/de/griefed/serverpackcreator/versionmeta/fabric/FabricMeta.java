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
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.Meta;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

/**
 * Fabric meta containing information about available Quilt versions and installers.
 *
 * @author Griefed
 */
public final class FabricMeta implements Meta {

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
   * @param utilities                    Commonly used utilities across ServerPackCreator.
   * @author Griefed
   */
  public FabricMeta(
      @NotNull File fabricManifest,
      @NotNull File fabricInstallerManifest,
      @NotNull FabricIntermediaries injectedFabricIntermediaries,
      @NotNull ObjectMapper objectMapper,
      @NotNull Utilities utilities) {

    FABRIC_LOADER_DETAILS = new FabricLoaderDetails(objectMapper);
    FABRIC_LOADER = new FabricLoader(fabricManifest, utilities);
    FABRIC_INTERMEDIARIES = injectedFabricIntermediaries;
    FABRIC_INSTALLER = new FabricInstaller(fabricInstallerManifest, utilities);
  }

  @Override
  public void update() throws ParserConfigurationException, IOException, SAXException {
    FABRIC_LOADER.update();
    FABRIC_INSTALLER.update();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String latestLoader() {
    return FABRIC_LOADER.latestLoaderVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String releaseLoader() {
    return FABRIC_LOADER.releaseLoaderVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String latestInstaller() {
    return FABRIC_INSTALLER.latestInstallerVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String releaseInstaller() {
    return FABRIC_INSTALLER.releaseInstallerVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull List<String> loaderVersionsListAscending() {
    return FABRIC_LOADER.loaders();
  }

  @Override
  public @NotNull List<String> loaderVersionsListDescending() {
    return Lists.reverse(FABRIC_LOADER.loaders());
  }

  @Override
  public @NotNull String @NotNull [] loaderVersionsArrayAscending() {
    return FABRIC_LOADER.loaders().toArray(new String[0]);
  }

  @Override
  public @NotNull String @NotNull [] loaderVersionsArrayDescending() {
    return Lists.reverse(FABRIC_LOADER.loaders()).toArray(new String[0]);
  }

  @Contract(pure = true)
  @Override
  public @NotNull List<String> installerVersionsListAscending() {
    return FABRIC_INSTALLER.installers();
  }

  @Override
  public @NotNull List<String> installerVersionsListDescending() {
    return Lists.reverse(FABRIC_INSTALLER.installers());
  }

  @Override
  public @NotNull String @NotNull [] installerVersionsArrayAscending() {
    return FABRIC_INSTALLER.installers().toArray(new String[0]);
  }

  @Override
  public @NotNull String @NotNull [] installerVersionsArrayDescending() {
    return Lists.reverse(FABRIC_INSTALLER.installers()).toArray(new String[0]);
  }

  @Contract(pure = true)
  @Override
  public @NotNull URL latestInstallerUrl() {
    return FABRIC_INSTALLER.latestInstallerUrl();
  }

  @Contract(pure = true)
  @Override
  public @NotNull URL releaseInstallerUrl() {
    return FABRIC_INSTALLER.releaseInstallerUrl();
  }

  @Override
  public boolean isInstallerUrlAvailable(@NotNull String fabricVersion) {
    return Optional.ofNullable(FABRIC_INSTALLER.meta().get(fabricVersion)).isPresent();
  }

  @Override
  public @NotNull Optional<URL> getInstallerUrl(@NotNull String fabricVersion) {
    return Optional.ofNullable(FABRIC_INSTALLER.meta().get(fabricVersion));
  }

  @Override
  public boolean isVersionValid(@NotNull String fabricVersion) {
    return FABRIC_LOADER.loaders().contains(fabricVersion);
  }

  @Override
  public boolean isMinecraftSupported(@NotNull String minecraftVersion) {
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
  public @NotNull Optional<URL> improvedLauncherUrl(@NotNull String minecraftVersion,
                                                    @NotNull String fabricVersion) {
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
  public @NotNull Optional<FabricDetails> getLoaderDetails(@NotNull String minecraftVersion,
                                                           @NotNull String fabricVersion) {
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
