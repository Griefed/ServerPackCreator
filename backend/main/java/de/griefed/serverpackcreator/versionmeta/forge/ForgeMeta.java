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

import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Forge meta containing information about available Forge releases.
 *
 * @author Griefed
 */
public final class ForgeMeta {

  private final File FORGE_MANIFEST;
  private final Utilities UTILITIES;

  private ForgeLoader forgeLoader;

  /**
   * Create a new Forge Meta, using a manifest file.
   *
   * @param forgeManifest The manifest from which to acquire version information.
   * @param utilities     Commonly used utilities across ServerPackCreator.
   * @author Griefed
   */
  @Contract(pure = true)
  public ForgeMeta(@NotNull File forgeManifest,
                   @NotNull Utilities utilities) {
    FORGE_MANIFEST = forgeManifest;
    UTILITIES = utilities;
  }

  /**
   * Update this instances {@link ForgeLoader} with new information. Usually called after the Forge
   * manifest has been refreshed.
   *
   * @param injectedMinecraftMeta Minecraft manifest file.
   * @throws IOException if the manifest could not be parsed into a
   *                     {@link com.fasterxml.jackson.databind.JsonNode}.
   * @author Griefed
   */
  public void initialize(@NotNull MinecraftMeta injectedMinecraftMeta) throws IOException {
    if (forgeLoader == null) {
      forgeLoader =
          new ForgeLoader(FORGE_MANIFEST, UTILITIES, injectedMinecraftMeta);
    }
  }

  /**
   * Update this instances {@link ForgeLoader} with new information. Usually called after the Forge
   * manifest has been refreshed.
   *
   * @throws IOException if the manifest could not be parsed into a
   *                     {@link com.fasterxml.jackson.databind.JsonNode}.
   * @author Griefed
   */
  public void update() throws IOException {
    this.forgeLoader.update();
  }

  /**
   * Check whether the given Minecraft and Forge versions are valid/supported/available.
   *
   * @param minecraftVersion Minecraft version.
   * @param forgeVersion     Forge version.
   * @return {@code true} if the given Minecraft and Forge versions are valid/supported/available.
   * @author Griefed
   */
  public boolean checkForgeAndMinecraftVersion(@NotNull String minecraftVersion,
                                               @NotNull String forgeVersion) {
    return checkMinecraftVersion(minecraftVersion) && checkForgeVersion(forgeVersion);
  }

  /**
   * Check whether a given Minecraft version is valid/supported/available.
   *
   * @param minecraftVersion Minecraft version.
   * @return {@code true} if the given Minecraft version is valid/supported/available.
   * @author Griefed
   */
  public boolean checkMinecraftVersion(@NotNull String minecraftVersion) {
    return Optional.ofNullable(forgeLoader.versionMeta().get(minecraftVersion)).isPresent();
  }

  /**
   * Check whether a given Forge version is valid/supported/available.
   *
   * @param forgeVersion Forge version.
   * @return {@code true} if the given Forge version is valid/supported/available.
   * @author Griefed
   */
  public boolean checkForgeVersion(@NotNull String forgeVersion) {
    return Optional.ofNullable(forgeLoader.forgeToMinecraftMeta().get(forgeVersion)).isPresent();
  }

  /**
   * Check whether Forge is available for a given Forge- and Minecraft version.
   *
   * @param minecraftVersion Minecraft version.
   * @param forgeVersion     Forge version.
   * @return {@code true} if Forge is available for the given Forge- and Minecraft version.
   * @author Griefed
   */
  public boolean isForgeInstanceAvailable(@NotNull String minecraftVersion,
                                          @NotNull String forgeVersion) {
    return getForgeInstance(minecraftVersion, forgeVersion).isPresent();
  }

  /**
   * Get a {@link ForgeInstance} for a given Minecraft and Forge version, wrapped in an
   * {@link Optional}.
   *
   * @param minecraftVersion Minecraft version.
   * @param forgeVersion     Forge version.
   * @return Forge instance for the given Minecraft and Forge version, wrapped in an
   * {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<ForgeInstance> getForgeInstance(@NotNull String minecraftVersion,
                                                           @NotNull String forgeVersion) {
    return Optional.ofNullable(
        forgeLoader.instanceMeta().get(minecraftVersion + "-" + forgeVersion));
  }

  /**
   * Check whether Forge is available for a given Forge version
   *
   * @param forgeVersion Forge version.
   * @return {@code true} if Forge is available for the given Forge version.
   * @author Griefed
   */
  public boolean isForgeInstanceAvailable(@NotNull String forgeVersion) {
    return getForgeInstance(forgeVersion).isPresent();
  }

  /**
   * Get a {@link ForgeInstance} for a given Forge version, wrapped in an {@link Optional}.
   *
   * @param forgeVersion Forge version.
   * @return Forge instance for the given Forge version, wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<ForgeInstance> getForgeInstance(@NotNull String forgeVersion) {
    if (!checkForgeVersion(forgeVersion)) {
      return Optional.empty();
    }

    if (minecraftVersion(forgeVersion).isPresent()) {

      return getForgeInstance(minecraftVersion(forgeVersion).get(), forgeVersion);

    } else {

      return Optional.empty();
    }
  }

  /**
   * Get a list of all available {@link ForgeInstance} for a given Minecraft version, wrapped in an
   * {@link Optional}
   *
   * @param minecraftVersion Minecraft version.
   * @return Forge instance-list for the given Minecraft version.
   * @author Griefed
   */
  public @NotNull Optional<List<ForgeInstance>> getForgeInstances(@NotNull String minecraftVersion) {
    List<ForgeInstance> list = new ArrayList<>(100);
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
   * @param minecraftVersion Minecraft version.
   * @return Latest Forge version for the given Minecraft version, wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<String> newestForgeVersion(@NotNull String minecraftVersion) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }

    if (supportedForgeVersionsAscending(minecraftVersion).isPresent()) {

      return Optional.of(
          supportedForgeVersionsAscending(minecraftVersion)
              .get()
              .get(supportedForgeVersionsAscending(minecraftVersion).get().size() - 1));

    } else {

      return Optional.empty();
    }
  }

  /**
   * Oldest Forge version for a given Minecraft version, wrapped in {@link Optional}
   *
   * @param minecraftVersion Minecraft version.
   * @return Oldest Forge version for the given Minecraft version, wrapped in {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<String> oldestForgeVersion(@NotNull String minecraftVersion) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }
    if (supportedForgeVersionsAscending(minecraftVersion).isPresent()) {

      return Optional.ofNullable(supportedForgeVersionsAscending(minecraftVersion).get().get(0));

    } else {

      return Optional.empty();
    }
  }

  /**
   * Get the list of available Forge versions, in ascending order.
   *
   * @return List of available Forge versions.
   * @author Griefed
   */
  public @NotNull List<String> forgeVersionsAscending() {
    return forgeLoader.forgeVersions();
  }

  /**
   * Get the list of available Forge versions, in descending order.
   *
   * @return List of available Forge versions.
   * @author Griefed
   */
  public @NotNull List<String> forgeVersionsDescending() {
    return Lists.reverse(forgeLoader.forgeVersions());
  }

  /**
   * Get the array of available Forge versions, in ascending order.
   *
   * @return Array of available Forge versions.
   * @author Griefed
   */
  public @NotNull String @NotNull [] forgeVersionsAscendingArray() {
    return forgeLoader.forgeVersions().toArray(new String[0]);
  }

  /**
   * Get the array of available Forge versions, in descending order.
   *
   * @return Array of available Forge versions.
   * @author Griefed
   */
  public @NotNull String @NotNull [] forgeVersionsDescendingArray() {
    return Lists.reverse(forgeLoader.forgeVersions()).toArray(new String[0]);
  }

  /**
   * Get a list of available Forge version for a given Minecraft version in ascending order.
   *
   * @param minecraftVersion Minecraft version.
   * @return List of available Forge versions for the given Minecraft version in ascending order.
   * @author Griefed
   */
  public @NotNull Optional<List<String>> supportedForgeVersionsAscending(@NotNull String minecraftVersion) {
    return Optional.ofNullable(forgeLoader.versionMeta().get(minecraftVersion));
  }

  /**
   * Get a list of available Forge version for a given Minecraft version in descending order.
   *
   * @param minecraftVersion Minecraft version.
   * @return List of available Forge versions for the given Minecraft version in descending order.
   * @author Griefed
   */
  public @NotNull Optional<List<String>> supportedForgeVersionsDescending(@NotNull String minecraftVersion) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }
    return Optional.ofNullable(Lists.reverse(forgeLoader.versionMeta().get(minecraftVersion)));
  }


  /**
   * Get an array of available Forge version for a given Minecraft version, in ascending order,
   * wrapped in an {@link Optional}.
   *
   * @param minecraftVersion Minecraft version.
   * @return Array of available Forge versions for the given Minecraft version, in ascending order,
   * wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<String[]> supportedForgeVersionsAscendingArray(@NotNull String minecraftVersion) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }
    return Optional.of(forgeLoader.versionMeta().get(minecraftVersion).toArray(new String[0]));
  }

  /**
   * Get an array of available Forge version for a given Minecraft version, in descending order,
   * wrapped in an {@link Optional}.
   *
   * @param minecraftVersion Minecraft version.
   * @return Array of available Forge versions for the given Minecraft version, in descending order,
   * wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<String[]> supportedForgeVersionsDescendingArray(@NotNull String minecraftVersion) {
    if (!checkMinecraftVersion(minecraftVersion)) {
      return Optional.empty();
    }
    return Optional.of(
        Lists.reverse(forgeLoader.versionMeta().get(minecraftVersion)).toArray(new String[0]));
  }

  /**
   * Get the Minecraft version for a given Forge version, wrapped in an {@link Optional}.
   *
   * @param forgeVersion Forge version.
   * @return Minecraft version for the given Forge version, wrapped in an {@link Optional}.
   * @author Griefed
   */
  public @NotNull Optional<String> minecraftVersion(@NotNull String forgeVersion) {
    return Optional.ofNullable(forgeLoader.forgeToMinecraftMeta().get(forgeVersion));
  }

  /**
   * Get the list of Forge supported Minecraft versions, in ascending order.
   *
   * @return List of Forge supported Minecraft versions, in ascending order.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<String> supportedMinecraftVersionsAscending() {
    return forgeLoader.minecraftVersions();
  }

  /**
   * Get the list of Forge supported Minecraft versions, in descending order.
   *
   * @return List of Forge supported Minecraft versions, in descending order.
   * @author Griefed
   */
  public @NotNull List<String> supportedMinecraftVersionsDescending() {
    return Lists.reverse(forgeLoader.minecraftVersions());
  }

  /**
   * Get the array of Forge supported Minecraft versions, in ascending order.
   *
   * @return Array of Forge supported Minecraft versions, in ascending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] supportedMinecraftVersionsAscendingArray() {
    return forgeLoader.minecraftVersions().toArray(new String[0]);
  }

  /**
   * Get the array of Forge supported Minecraft versions, in descending order.
   *
   * @return Array of Forge supported Minecraft versions, in descending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] supportedMinecraftVersionsDescendingArray() {
    return Lists.reverse(forgeLoader.minecraftVersions()).toArray(new String[0]);
  }

  /**
   * Get the Forge server installer URL for a given Forge version, wrapped in an {@link Optional}.
   *
   * @param forgeVersion Forge version.
   * @return Forge server installer URL for the given Forge version, wrapped in an {@link Optional}.
   * @author Griefed
   */
  public @NotNull Optional<URL> installerUrl(@NotNull String forgeVersion) {
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
