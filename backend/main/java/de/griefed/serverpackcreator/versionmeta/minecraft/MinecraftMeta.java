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
package de.griefed.serverpackcreator.versionmeta.minecraft;

import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.Type;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Minecraft meta containing information about available client- and server releases.
 *
 * @author Griefed
 */
public final class MinecraftMeta {

  private final MinecraftClientMeta MINECRAFT_CLIENT_META;
  private final MinecraftServerMeta MINECRAFT_SERVER_META;

  /**
   * Create a new Minecraft Meta instance.
   *
   * @param minecraftManifest     Minecraft manifest file.
   * @param injectedForgeMeta     To acquire Forge instances for this {@link MinecraftClient}
   *                              version.
   * @param utilities             Commonly used utilities across ServerPackCreator.
   * @param applicationProperties ServerPackCreator settings.
   * @author Griefed
   */
  public MinecraftMeta(
      @NotNull File minecraftManifest,
      @NotNull ForgeMeta injectedForgeMeta,
      @NotNull Utilities utilities,
      @NotNull ApplicationProperties applicationProperties) {

    MINECRAFT_CLIENT_META = new MinecraftClientMeta(
        minecraftManifest,
        injectedForgeMeta,
        utilities,
        applicationProperties);

    MINECRAFT_SERVER_META = new MinecraftServerMeta(MINECRAFT_CLIENT_META);
  }

  /**
   * Update the {@link MinecraftClientMeta} and {@link MinecraftServerMeta}. Usually called after
   * the manifest-files have been refreshed.
   *
   * @throws IOException if the {@link MinecraftClientMeta} could not be initialized.
   * @author Griefed
   */
  public void update() throws IOException {
    MINECRAFT_CLIENT_META.update();
    MINECRAFT_SERVER_META.update();
  }

  /*
   * #==============================================================================================================#
   * #..............................................................................................................#
   * #...................................................STRINGS....................................................#
   * #..............................................................................................................#
   * #==============================================================================================================#
   */

  /**
   * Check whether a {@link MinecraftClient} is available for the specified Minecraft-version.
   *
   * @param minecraftVersion The Minecraft-version.
   * @return {@code true} if a {@link MinecraftClient} is available.
   * @author Griefed
   */
  public boolean checkMinecraftVersion(@NotNull String minecraftVersion) {
    return getClient(minecraftVersion).isPresent();
  }

  /**
   * Get a specific Minecraft client as a {@link MinecraftClient} wrapped in an {@link Optional}.
   *
   * @param minecraftVersion Minecraft version.
   * @return Client wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<MinecraftClient> getClient(@NotNull String minecraftVersion) {
    return Optional.ofNullable(MINECRAFT_CLIENT_META.meta().get(minecraftVersion));
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#RELEASE} in
   * descending order.
   *
   * @return Array of all available Minecraft {@link Type#RELEASE} versions in
   * descending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] releaseVersionsArrayDescending() {
    return releaseVersionsDescending().toArray(new String[0]);
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#RELEASE} in
   * descending order.
   *
   * @return List of all available Minecraft {@link Type#RELEASE} versions in
   * descending order.
   * @author Griefed
   */
  public @NotNull List<String> releaseVersionsDescending() {
    List<String> list = new ArrayList<>(100);
    releasesDescending().forEach(client -> list.add(client.version()));
    return list;
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in ascending
   * order.
   *
   * @return Release client-list
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<MinecraftClient> releasesDescending() {
    return MINECRAFT_CLIENT_META.releases();
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#RELEASE} in
   * ascending order.
   *
   * @return Array of all available Minecraft {@link Type#RELEASE} versions in
   * ascending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] releaseVersionsArrayAscending() {
    return releaseVersionsAscending().toArray(new String[0]);
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#RELEASE} in
   * ascending order.
   *
   * @return List of all available Minecraft {@link Type#RELEASE} versions in ascending
   * order.
   * @author Griefed
   */
  public @NotNull List<String> releaseVersionsAscending() {
    List<String> list = new ArrayList<>(100);
    releasesDescending().forEach(client -> list.add(client.version()));
    return Lists.reverse(list);
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#SNAPSHOT} in
   * descending order.
   *
   * @return Array of all available Minecraft {@link Type#SNAPSHOT} versions in
   * descending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] snapshotVersionsArrayDescending() {
    return snapshotVersionsDescending().toArray(new String[0]);
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#SNAPSHOT} in
   * descending order.
   *
   * @return List of all available Minecraft {@link Type#SNAPSHOT} versions in
   * descending order.
   * @author Griefed
   */
  public @NotNull List<String> snapshotVersionsDescending() {
    List<String> list = new ArrayList<>(100);
    snapshotsDescending().forEach(client -> list.add(client.version()));
    return list;
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in ascending
   * order.
   *
   * @return Snapshot client-list
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<MinecraftClient> snapshotsDescending() {
    return MINECRAFT_CLIENT_META.snapshots();
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#SNAPSHOT} in
   * ascending order.
   *
   * @return Array of all available Minecraft {@link Type#SNAPSHOT} versions in
   * ascending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] snapshotVersionsArrayAscending() {
    return snapshotVersionsAscending().toArray(new String[0]);
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#SNAPSHOT} in
   * ascending order.
   *
   * @return List of all available Minecraft {@link Type#SNAPSHOT} versions in
   * ascending order.
   * @author Griefed
   */
  public @NotNull List<String> snapshotVersionsAscending() {
    List<String> list = new ArrayList<>(100);
    snapshotsDescending().forEach(client -> list.add(client.version()));
    return Lists.reverse(list);
  }

  /**
   * Get an array of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in descending order.
   *
   * @return All available Minecraft versions in descending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] allVersionsArrayDescending() {
    return allVersionsDescending().toArray(new String[0]);
  }

  /*
   * #==============================================================================================================#
   * #..............................................................................................................#
   * #...................................................CLIENTS....................................................#
   * #..............................................................................................................#
   * #==============================================================================================================#
   */

  /**
   * Get a list of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in descending order.
   *
   * @return All available Minecraft versions in descending order.
   * @author Griefed
   */
  public @NotNull List<String> allVersionsDescending() {
    List<String> versions = new ArrayList<>(100);
    allDescending().forEach(client -> versions.add(client.version()));
    return versions;
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * descending order.
   *
   * @return All available Minecraft releases in descending order.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<MinecraftClient> allDescending() {
    return MINECRAFT_CLIENT_META.all();
  }

  /**
   * Get an array of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in ascending order.
   *
   * @return All available Minecraft versions in ascending order.
   * @author Griefed
   */
  public @NotNull String @NotNull [] allVersionsArrayAscending() {
    return allVersionsAscending().toArray(new String[0]);
  }

  /**
   * Get a list of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in ascending order.
   *
   * @return All available Minecraft versions in ascending order.
   * @author Griefed
   */
  public @NotNull List<String> allVersionsAscending() {
    List<String> versions = new ArrayList<>(releaseVersionsAscending());
    allAscending().forEach(client -> versions.add(client.version()));
    return versions;
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * ascending order.
   *
   * @return All available Minecraft releases in ascending order.
   * @author Griefed
   */
  public @NotNull List<MinecraftClient> allAscending() {
    return Lists.reverse(MINECRAFT_CLIENT_META.all());
  }

  /**
   * Get the latest Minecraft release as a {@link MinecraftClient}.
   *
   * @return Latest Minecraft release.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull MinecraftClient latestRelease() {
    return MINECRAFT_CLIENT_META.latestRelease();
  }

  /**
   * Get the latest Minecraft snapshot as a {@link MinecraftClient}.
   *
   * @return Latest Minecraft snapshot.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull MinecraftClient latestSnapshot() {
    return MINECRAFT_CLIENT_META.latestSnapshot();
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in ascending
   * order.
   *
   * @return Release client-array
   * @author Griefed
   */
  public @NotNull MinecraftClient @NotNull [] releasesArrayDescending() {
    return releasesDescending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in
   * descending order.
   *
   * @return Release client-array
   * @author Griefed
   */
  public @NotNull MinecraftClient @NotNull [] releasesArrayAscending() {
    return releasesAscending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in descending
   * order.
   *
   * @return Release client-list
   * @author Griefed
   */
  public @NotNull List<MinecraftClient> releasesAscending() {
    return Lists.reverse(MINECRAFT_CLIENT_META.releases());
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in
   * ascending order.
   *
   * @return Snapshot client-array
   * @author Griefed
   */
  public @NotNull MinecraftClient @NotNull [] snapshotsArrayDescending() {
    return snapshotsDescending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in
   * descending order.
   *
   * @return Snapshot client-array
   * @author Griefed
   */
  public @NotNull MinecraftClient @NotNull [] snapshotsArrayAscending() {
    return snapshotsAscending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in descending
   * order.
   *
   * @return Snapshot client-list
   * @author Griefed
   */
  public @NotNull List<MinecraftClient> snapshotsAscending() {
    return Lists.reverse(MINECRAFT_CLIENT_META.snapshots());
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * descending order.
   *
   * @return All available Minecraft releases in descending order.
   * @author Griefed
   */
  public @NotNull MinecraftClient @NotNull [] allDescendingArray() {
    return allDescending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * ascending order.
   *
   * @return All available Minecraft releases in ascending order.
   * @author Griefed
   */
  public @NotNull MinecraftClient @NotNull [] allAscendingArray() {
    return allAscending().toArray(new MinecraftClient[0]);
  }

  /*
   * #==============================================================================================================#
   * #..............................................................................................................#
   * #...................................................SERVERS....................................................#
   * #..............................................................................................................#
   * #==============================================================================================================#
   */

  /**
   * Check whether a {@link MinecraftServer} is available for the specified Minecraft-version.
   *
   * @param minecraftVersion The Minecraft-version.
   * @return {@code true} if a {@link MinecraftServer} is available.
   * @author Griefed
   */
  public boolean checkServerAvailability(@NotNull String minecraftVersion) {
    return getServer(minecraftVersion).isPresent();
  }

  /**
   * Get a specific {@link MinecraftServer} for the specified Minecraft-version, wrapped in an
   * {@link Optional}.
   *
   * @param minecraftVersion The Minecraft-version.
   * @return Server wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull Optional<MinecraftServer> getServer(@NotNull String minecraftVersion) {
    try {
      if (MINECRAFT_SERVER_META.meta().get(minecraftVersion).url().isPresent()
          && MINECRAFT_SERVER_META.meta().get(minecraftVersion).javaVersion().isPresent()) {

        return Optional.ofNullable(MINECRAFT_SERVER_META.meta().get(minecraftVersion));
      }
    } catch (Exception ignored) {

    }

    return Optional.empty();
  }

  /**
   * Get the latest {@link MinecraftServer} of the {@link Type#RELEASE}, wrapped in an
   * {@link Optional}.
   *
   * @return Server wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull MinecraftServer latestReleaseServer() {
    return MINECRAFT_CLIENT_META.latestRelease().server();
  }

  /**
   * Get the latest {@link MinecraftServer} of the {@link Type#SNAPSHOT}, wrapped in an
   * {@link Optional}.
   *
   * @return Server wrapped in an {@link Optional}
   * @author Griefed
   */
  public @NotNull MinecraftServer latestSnapshotServer() {
    return MINECRAFT_CLIENT_META.latestSnapshot().server();
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * descending order.
   *
   * @return Server-array
   * @author Griefed
   */
  public @NotNull MinecraftServer @NotNull [] releasesServersArrayDescending() {
    return releasesServersDescending().toArray(new MinecraftServer[0]);
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * descending order.
   *
   * @return Server-list
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<MinecraftServer> releasesServersDescending() {
    return MINECRAFT_SERVER_META.releases();
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * ascending order.
   *
   * @return Server-array
   * @author Griefed
   */
  public @NotNull MinecraftServer @NotNull [] releasesServersArrayAscending() {
    return releasesServersAscending().toArray(new MinecraftServer[0]);
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * ascending order.
   *
   * @return Server-list
   * @author Griefed
   */
  public @NotNull List<MinecraftServer> releasesServersAscending() {
    return Lists.reverse(MINECRAFT_SERVER_META.releases());
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in
   * ascending order.
   *
   * @return Server-array
   * @author Griefed
   */
  public @NotNull MinecraftServer @NotNull [] snapshotsServersArrayDescending() {
    return snapshotsServersDescending().toArray(new MinecraftServer[0]);
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in
   * descending order.
   *
   * @return Server-list
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<MinecraftServer> snapshotsServersDescending() {
    return MINECRAFT_SERVER_META.snapshots();
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in
   * descending order.
   *
   * @return Server-array
   * @author Griefed
   */
  public @NotNull MinecraftServer @NotNull [] snapshotsServersArrayAscending() {
    return snapshotsServersAscending().toArray(new MinecraftServer[0]);
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in descending
   * order.
   *
   * @return Server-list
   * @author Griefed
   */
  public @NotNull List<MinecraftServer> snapshotsServersAscending() {
    return Lists.reverse(MINECRAFT_SERVER_META.snapshots());
  }
}
