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
package de.griefed.serverpackcreator.versionmeta.forge;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.versionmeta.Type;
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Forge meta containing information about available Forge releases.
 *
 * @author Griefed
 */
public class ForgeMeta {

  private final File FORGE_MANIFEST;
  private final ObjectMapper OBJECTMAPPER =
      new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

  private ForgeLoader forgeLoader;

  public ForgeMeta(File forgeManifest) {
    this.FORGE_MANIFEST = forgeManifest;
  }

  /**
   * Update this instances {@link ForgeLoader} with new information. Usually called after the Forge
   * manifest has been refreshed.
   *
   * @param injectedMinecraftMeta {@link File} Minecraft manifest file.
   * @throws IOException if the manifest could not be parsed into a {@link
   *     com.fasterxml.jackson.databind.JsonNode}.
   * @author Griefed
   */
  public void initialize(MinecraftMeta injectedMinecraftMeta) throws IOException {
    if (this.forgeLoader == null) {
      this.forgeLoader =
          new ForgeLoader(OBJECTMAPPER.readTree(this.FORGE_MANIFEST), injectedMinecraftMeta);
    }
  }

  /**
   * Update this instances {@link ForgeLoader} with new information. Usually called after the Forge
   * manifest has been refreshed.
   *
   * @return This instance of {@link ForgeMeta}
   * @throws IOException if the manifest could not be parsed into a {@link
   *     com.fasterxml.jackson.databind.JsonNode}.
   * @author Griefed
   */
  public ForgeMeta update() throws IOException {
    this.forgeLoader.update(OBJECTMAPPER.readTree(this.FORGE_MANIFEST));

    return this;
  }

  /**
   * Get the list of Forge supported Minecraft versions, with either {@link Type#ASCENDING} or
   * {@link Type#DESCENDING} order.
   *
   * @param order {@link Type} The order of the resulting list. Either {@link Type#ASCENDING} or
   *     {@link Type#DESCENDING}.
   * @return {@link String}-list Forge supported Minecraft versions, in either {@link
   *     Type#ASCENDING} or {@link Type#DESCENDING} order.
   * @author Griefed
   */
  private List<String> supportedMinecraftVersionsList(Type order) {
    if (order == Type.DESCENDING) {
      return Lists.reverse(forgeLoader.minecraftVersions());
    } else {
      return forgeLoader.minecraftVersions();
    }
  }

  /**
   * Get the list of available Forge versions, with either {@link Type#ASCENDING} or {@link
   * Type#DESCENDING} order.
   *
   * @param order {@link Type} The order of the resulting list. Either {@link Type#ASCENDING} or
   *     {@link Type#DESCENDING}.
   * @return {@link String}-list Forge versions, in either {@link Type#ASCENDING} or {@link
   *     Type#DESCENDING} order.
   * @author Griefed
   */
  private List<String> supportedForgeVersionsList(Type order) {
    if (order == Type.DESCENDING) {
      return Lists.reverse(forgeLoader.forgeVersions());
    } else {
      return forgeLoader.forgeVersions();
    }
  }

  /**
   * Get an array of available Forge version for a given Minecraft version, either {@link
   * Type#ASCENDING} or {@link Type#DESCENDING}, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @param order {@link Type} Either {@link Type#DESCENDING}, wrapped in an {@link Optional}.
   * @return {@link String}-array of available Forge versions for the given Minecraft version,
   *     either {@link Type#ASCENDING} or {@link Type#DESCENDING}, wrapped in an {@link Optional}.
   * @author Griefed
   */
  private Optional<String[]> getForgeVersionsArray(String minecraftVersion, Type order) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }
    if (order == Type.DESCENDING) {
      return Optional.of(
          Lists.reverse(forgeLoader.versionMeta().get(minecraftVersion)).toArray(new String[0]));
    } else {
      return Optional.of(forgeLoader.versionMeta().get(minecraftVersion).toArray(new String[0]));
    }
  }

  /**
   * Get a list of available Forge version for a given Minecraft version, with either {@link
   * Type#ASCENDING} or {@link Type#DESCENDING} order.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @param order {@link Type} The order of the resulting list. Either {@link Type#ASCENDING} or
   *     {@link Type#DESCENDING}.
   * @return {@link String}-list of available Forge versions for the given Minecraft version, with
   *     either {@link Type#ASCENDING} or {@link Type#DESCENDING} order.
   * @author Griefed
   */
  private Optional<List<String>> getForgeVersions(String minecraftVersion, Type order) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }
    if (order == Type.DESCENDING) {
      return Optional.ofNullable(Lists.reverse(forgeLoader.versionMeta().get(minecraftVersion)));
    } else {
      return Optional.ofNullable(forgeLoader.versionMeta().get(minecraftVersion));
    }
  }

  /**
   * Check whether a given Forge version is valid/supported/available.
   *
   * @param forgeVersion {@link String} Forge version.
   * @return {@link Boolean} <code>true</code> if the given Forge version is
   *     valid/supported/available.
   * @author Griefed
   */
  public boolean checkForgeVersion(String forgeVersion) {
    return Optional.ofNullable(forgeLoader.forgeToMinecraftMeta().get(forgeVersion)).isPresent();
  }

  /**
   * Check whether a given Minecraft version is valid/supported/available.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link Boolean} <code>true</code> if the given Minecraft version is
   *     valid/supported/available.
   * @author Griefed
   */
  public boolean checkMinecraftVersion(String minecraftVersion) {
    return Optional.ofNullable(forgeLoader.versionMeta().get(minecraftVersion)).isPresent();
  }

  /**
   * Check whether the given Minecraft and Forge versions are valid/supported/available.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @param forgeVersion {@link String} Forge version.
   * @return {@link Boolean} <code>true</code> if the given Minecraft and Forge versions are
   *     valid/supported/available.
   * @author Griefed
   */
  public boolean checkForgeAndMinecraftVersion(String minecraftVersion, String forgeVersion) {
    return checkMinecraftVersion(minecraftVersion) && checkForgeVersion(forgeVersion);
  }

  /**
   * Check whether Forge is available for a given Forge- and Minecraft version.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @param forgeVersion {@link String} Forge version.
   * @return {@link Boolean} <code>true</code> if Forge is available for the given Forge- and
   *     Minecraft version.
   * @author Griefed
   */
  public boolean isForgeInstanceAvailable(String minecraftVersion, String forgeVersion) {
    return getForgeInstance(minecraftVersion, forgeVersion).isPresent();
  }

  /**
   * Check whether Forge is available for a given Forge version
   *
   * @param forgeVersion {@link String} Forge version.
   * @return {@link Boolean} <code>true</code> if Forge is available for the given Forge version.
   * @author Griefed
   */
  public boolean isForgeInstanceAvailable(String forgeVersion) {
    return getForgeInstance(forgeVersion).isPresent();
  }

  /**
   * Get the Minecraft version for a given Forge version, wrapped in an {@link Optional}.
   *
   * @param forgeVersion {@link String} Forge version.
   * @return {@link String} Minecraft version for the given Forge version, wrapped in an {@link
   *     Optional}.
   * @author Griefed
   */
  public Optional<String> supportedMinecraftVersion(String forgeVersion) {
    return Optional.ofNullable(forgeLoader.forgeToMinecraftMeta().get(forgeVersion));
  }

  /**
   * Get a {@link ForgeInstance} for a given Minecraft and Forge version, wrapped in an {@link
   * Optional}.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @param forgeVersion {@link String} Forge version.
   * @return {@link ForgeInstance} for the given Minecraft and Forge version, wrapped in an {@link
   *     Optional}
   * @author Griefed
   */
  public Optional<ForgeInstance> getForgeInstance(String minecraftVersion, String forgeVersion) {
    return Optional.ofNullable(
        forgeLoader.instanceMeta().get(minecraftVersion + "-" + forgeVersion));
  }

  /**
   * Get a {@link ForgeInstance} for a given Forge version, wrapped in an {@link Optional}.
   *
   * @param forgeVersion {@link String} Forge version.
   * @return {@link ForgeInstance} for the given Forge version, wrapped in an {@link Optional}
   * @author Griefed
   */
  public Optional<ForgeInstance> getForgeInstance(String forgeVersion) {
    if (!checkForgeVersion(forgeVersion)) {
      return Optional.empty();
    }

    if (supportedMinecraftVersion(forgeVersion).isPresent()) {

      return getForgeInstance(supportedMinecraftVersion(forgeVersion).get(), forgeVersion);

    } else {

      return Optional.empty();
    }
  }

  /**
   * Get a list of all available {@link ForgeInstance} for a given Minecraft version, wrapped in an
   * {@link Optional}
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link ForgeInstance}-list for the given Minecraft version.
   * @author Griefed
   */
  public Optional<List<ForgeInstance>> getForgeInstances(String minecraftVersion) {
    List<ForgeInstance> list = new ArrayList<>();
    if (Optional.ofNullable(forgeLoader.versionMeta().get(minecraftVersion)).isPresent()) {

      forgeLoader
          .versionMeta()
          .get(minecraftVersion)
          .forEach(
              forgeVersion -> {
                if (forgeLoader.instanceMeta().get(minecraftVersion + "-" + forgeVersion) != null) {
                  list.add(forgeLoader.instanceMeta().get(minecraftVersion + "-" + forgeVersion));
                }
              });

      return Optional.of(list);

    } else {
      return Optional.empty();
    }
  }

  /**
   * Latest Forge version for a given Minecraft version, wrapped in {@link Optional}
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link String} Latest Forge version for the given Minecraft version, wrapped in an
   *     {@link Optional}
   * @author Griefed
   */
  public Optional<String> latestForgeVersion(String minecraftVersion) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }

    if (getForgeVersions(minecraftVersion, Type.ASCENDING).isPresent()) {

      return Optional.of(
          getForgeVersions(minecraftVersion, Type.ASCENDING)
              .get()
              .get(getForgeVersions(minecraftVersion, Type.ASCENDING).get().size() - 1));

    } else {

      return Optional.empty();
    }
  }

  /**
   * Oldest Forge version for a given Minecraft version, wrapped in {@link Optional}
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link String} Oldest Forge version for the given Minecraft version, wrapped in {@link
   *     Optional}
   * @author Griefed
   */
  public Optional<String> oldestForgeVersion(String minecraftVersion) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }
    if (getForgeVersions(minecraftVersion, Type.ASCENDING).isPresent()) {

      return Optional.ofNullable(getForgeVersions(minecraftVersion, Type.ASCENDING).get().get(0));

    } else {

      return Optional.empty();
    }
  }

  /**
   * Get the list of Forge supported Minecraft versions, in {@link Type#ASCENDING} order.
   *
   * @return {@link String}-list Forge supported Minecraft versions, in ascending order.
   * @author Griefed
   */
  public List<String> minecraftVersionsAscending() {
    return supportedMinecraftVersionsList(Type.ASCENDING);
  }

  /**
   * Get the list of Forge supported Minecraft versions, in {@link Type#DESCENDING} order.
   *
   * @return {@link String}-list Forge supported Minecraft versions, in descending order.
   * @author Griefed
   */
  public List<String> minecraftVersionsDescending() {
    return supportedMinecraftVersionsList(Type.DESCENDING);
  }

  /**
   * Get the array of Forge supported Minecraft versions, in {@link Type#ASCENDING} order.
   *
   * @return {@link String}-array Forge supported Minecraft versions, in descending order.
   * @author Griefed
   */
  public String[] minecraftVersionsArrayAscending() {
    return supportedMinecraftVersionsList(Type.ASCENDING).toArray(new String[0]);
  }

  /**
   * Get the array of Forge supported Minecraft versions, in {@link Type#DESCENDING} order.
   *
   * @return {@link String}-array Forge supported Minecraft versions, in descending order.
   * @author Griefed
   */
  public String[] minecraftVersionsArrayDescending() {
    return supportedMinecraftVersionsList(Type.DESCENDING).toArray(new String[0]);
  }

  /**
   * Get the list of available Forge versions, in {@link Type#ASCENDING} order.
   *
   * @return {@link String}-list Available Forge versions.
   * @author Griefed
   */
  public List<String> forgeVersions() {
    return supportedForgeVersionsList(Type.ASCENDING);
  }

  /**
   * Get the list of available Forge versions, in {@link Type#DESCENDING} order.
   *
   * @return {@link String}-list Available Forge versions.
   * @author Griefed
   */
  public List<String> forgeVersionsDescending() {
    return supportedForgeVersionsList(Type.DESCENDING);
  }

  /**
   * Get the array of available Forge versions, in {@link Type#ASCENDING} order.
   *
   * @return {@link String}-array Available Forge versions.
   * @author Griefed
   */
  public String[] forgeVersionsArray() {
    return supportedForgeVersionsList(Type.ASCENDING).toArray(new String[0]);
  }

  /**
   * Get the array of available Forge versions, in {@link Type#DESCENDING} order.
   *
   * @return {@link String}-array Available Forge versions.
   * @author Griefed
   */
  public String[] forgeVersionsArrayDescending() {
    return supportedForgeVersionsList(Type.DESCENDING).toArray(new String[0]);
  }

  /**
   * Get a list of available Forge version for a given Minecraft version, in {@link Type#ASCENDING}
   * order, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link String}-list of available Forge versions for the given Minecraft version, in
   *     ascending order, wrapped in an {@link Optional}
   * @author Griefed
   */
  public Optional<List<String>> availableForgeVersionsAscending(String minecraftVersion) {
    return getForgeVersions(minecraftVersion, Type.ASCENDING);
  }

  /**
   * Get a list of available Forge version for a given Minecraft version, in {@link Type#DESCENDING}
   * order, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link String}-list of available Forge versions for the given Minecraft version, in
   *     descending order, wrapped in an {@link Optional}
   * @author Griefed
   */
  public Optional<List<String>> availableForgeVersionsDescending(String minecraftVersion) {
    return getForgeVersions(minecraftVersion, Type.DESCENDING);
  }

  /**
   * Get an array of available Forge version for a given Minecraft version, in {@link
   * Type#ASCENDING} order, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link String}-array of available Forge versions for the given Minecraft version, in
   *     ascending order, wrapped in an {@link Optional}
   * @author Griefed
   */
  public Optional<String[]> availableForgeVersionsArrayAscending(String minecraftVersion) {
    return getForgeVersionsArray(minecraftVersion, Type.ASCENDING);
  }

  /**
   * Get an array of available Forge version for a given Minecraft version, in {@link
   * Type#DESCENDING} order, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return {@link String}-array of available Forge versions for the given Minecraft version, in
   *     descending order, wrapped in an {@link Optional}
   * @author Griefed
   */
  public Optional<String[]> availableForgeVersionsArrayDescending(String minecraftVersion) {
    return getForgeVersionsArray(minecraftVersion, Type.DESCENDING);
  }

  /**
   * Get the Forge server installer URL for a given Forge version, wrapped in an {@link Optional}.
   *
   * @param forgeVersion {@link String} Forge version.
   * @return {@link URL} Forge server installer URL for the given Forge version, wrapped in an
   *     {@link Optional}.
   * @author Griefed
   */
  public Optional<URL> installerUrl(String forgeVersion) {
    if (!checkForgeVersion(forgeVersion)) {
      return Optional.empty();
    }

    if (getForgeInstance(forgeVersion).isPresent()) {

      return Optional.ofNullable(getForgeInstance(forgeVersion).get().installerUrl());

    } else {

      return Optional.empty();
    }
  }
}
