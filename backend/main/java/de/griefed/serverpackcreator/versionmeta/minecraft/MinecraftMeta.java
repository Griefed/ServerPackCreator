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

import com.fasterxml.jackson.databind.ObjectMapper;
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
   * @param objectMapper          Object mapper for JSON parsing.
   * @param utilities             Instance of commonly used utilities.
   * @param applicationProperties ServerPackCreator settings.
   * @author Griefed
   */
  public MinecraftMeta(
      File minecraftManifest, ForgeMeta injectedForgeMeta, ObjectMapper objectMapper,
      Utilities utilities, ApplicationProperties applicationProperties) {
    MINECRAFT_CLIENT_META =
        new MinecraftClientMeta(minecraftManifest, injectedForgeMeta, objectMapper, utilities,
            applicationProperties);
    MINECRAFT_SERVER_META = new MinecraftServerMeta(this.MINECRAFT_CLIENT_META);
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
   * @return <code>true</code> if a {@link MinecraftClient} is available.
   * @author Griefed
   */
  public boolean checkMinecraftVersion(String minecraftVersion) {
    return getClient(minecraftVersion).isPresent();
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#RELEASE} in
   * {@link Type#DESCENDING} order.
   *
   * @return List of all available Minecraft {@link Type#RELEASE} versions in
   * {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public List<String> releaseVersionsDescending() {
    List<String> list = new ArrayList<>();
    releasesDescending().forEach(client -> list.add(client.version()));
    return list;
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#RELEASE} in
   * {@link Type#ASCENDING} order.
   *
   * @return List of all available Minecraft {@link Type#RELEASE} versions in {@link Type#ASCENDING}
   * order.
   * @author Griefed
   */
  public List<String> releaseVersionsAscending() {
    List<String> list = new ArrayList<>();
    releasesDescending().forEach(client -> list.add(client.version()));
    return Lists.reverse(list);
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#RELEASE} in
   * {@link Type#DESCENDING} order.
   *
   * @return Array of all available Minecraft {@link Type#RELEASE} versions in
   * {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public String[] releaseVersionsArrayDescending() {
    return releaseVersionsDescending().toArray(new String[0]);
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#RELEASE} in
   * {@link Type#ASCENDING} order.
   *
   * @return Array of all available Minecraft {@link Type#RELEASE} versions in
   * {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public String[] releaseVersionsArrayAscending() {
    return releaseVersionsAscending().toArray(new String[0]);
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#SNAPSHOT} in
   * {@link Type#DESCENDING} order.
   *
   * @return List of all available Minecraft {@link Type#SNAPSHOT} versions in
   * {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public List<String> snapshotVersionsDescending() {
    List<String> list = new ArrayList<>();
    snapshotsDescending().forEach(client -> list.add(client.version()));
    return list;
  }

  /**
   * Get the list of available Minecraft version of the {@link Type#SNAPSHOT} in
   * {@link Type#ASCENDING} order.
   *
   * @return List of all available Minecraft {@link Type#SNAPSHOT} versions in
   * {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public List<String> snapshotVersionsAscending() {
    List<String> list = new ArrayList<>();
    snapshotsDescending().forEach(client -> list.add(client.version()));
    return Lists.reverse(list);
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#SNAPSHOT} in
   * {@link Type#DESCENDING} order.
   *
   * @return Array of all available Minecraft {@link Type#SNAPSHOT} versions in
   * {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public String[] snapshotVersionsArrayDescending() {
    return snapshotVersionsDescending().toArray(new String[0]);
  }

  /**
   * Get the array of available Minecraft version of the {@link Type#SNAPSHOT} in
   * {@link Type#ASCENDING} order.
   *
   * @return Array of all available Minecraft {@link Type#SNAPSHOT} versions in
   * {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public String[] snapshotVersionsArrayAscending() {
    return snapshotVersionsAscending().toArray(new String[0]);
  }

  /**
   * Get an array of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in {@link Type#DESCENDING} order.
   *
   * @return All available Minecraft versions in descending order.
   * @author Griefed
   */
  public String[] allVersionsArrayDescending() {
    return allVersionsDescending().toArray(new String[0]);
  }

  /**
   * Get an array of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in {@link Type#ASCENDING} order.
   *
   * @return All available Minecraft versions in ascending order.
   * @author Griefed
   */
  public String[] allVersionsArrayAscending() {
    return allVersionsAscending().toArray(new String[0]);
  }

  /**
   * Get a list of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in {@link Type#DESCENDING} order.
   *
   * @return All available Minecraft versions in descending order.
   * @author Griefed
   */
  public List<String> allVersionsDescending() {
    List<String> versions = new ArrayList<>();
    allDescending().forEach(client -> versions.add(client.version()));
    return versions;
  }

  /**
   * Get a list of all available Minecraft versions of the {@link Type#RELEASE} and
   * {@link Type#SNAPSHOT} in {@link Type#ASCENDING} order.
   *
   * @return All available Minecraft versions in ascending order.
   * @author Griefed
   */
  public List<String> allVersionsAscending() {
    List<String> versions = new ArrayList<>(releaseVersionsAscending());
    allAscending().forEach(client -> versions.add(client.version()));
    return versions;
  }

  /*
   * #==============================================================================================================#
   * #..............................................................................................................#
   * #...................................................CLIENTS....................................................#
   * #..............................................................................................................#
   * #==============================================================================================================#
   */

  /**
   * Get the latest Minecraft release as a {@link MinecraftClient}.
   *
   * @return Latest Minecraft release.
   * @author Griefed
   */
  public MinecraftClient latestRelease() {
    return MINECRAFT_CLIENT_META.latestRelease();
  }

  /**
   * Get the latest Minecraft snapshot as a {@link MinecraftClient}.
   *
   * @return Latest Minecraft snapshot.
   * @author Griefed
   */
  public MinecraftClient latestSnapshot() {
    return MINECRAFT_CLIENT_META.latestSnapshot();
  }

  /**
   * Get a specific Minecraft client as a {@link MinecraftClient} wrapped in an {@link Optional}.
   *
   * @param minecraftVersion Minecraft version.
   * @return Client wrapped in an {@link Optional}
   * @author Griefed
   */
  public Optional<MinecraftClient> getClient(String minecraftVersion) {
    return Optional.ofNullable(MINECRAFT_CLIENT_META.meta().get(minecraftVersion));
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in ascending
   * order.
   *
   * @return Release client-list
   * @author Griefed
   */
  public List<MinecraftClient> releasesDescending() {
    return MINECRAFT_CLIENT_META.releases();
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in descending
   * order.
   *
   * @return Release client-list
   * @author Griefed
   */
  public List<MinecraftClient> releasesAscending() {
    return Lists.reverse(MINECRAFT_CLIENT_META.releases());
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in ascending
   * order.
   *
   * @return Release client-array
   * @author Griefed
   */
  public MinecraftClient[] releasesArrayDescending() {
    return releasesDescending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in
   * descending order.
   *
   * @return Release client-array
   * @author Griefed
   */
  public MinecraftClient[] releasesArrayAscending() {
    return releasesAscending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in ascending
   * order.
   *
   * @return Snapshot client-list
   * @author Griefed
   */
  public List<MinecraftClient> snapshotsDescending() {
    return MINECRAFT_CLIENT_META.snapshots();
  }

  /**
   * Get a list of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in descending
   * order.
   *
   * @return Snapshot client-list
   * @author Griefed
   */
  public List<MinecraftClient> snapshotsAscending() {
    return Lists.reverse(MINECRAFT_CLIENT_META.snapshots());
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in
   * ascending order.
   *
   * @return Snapshot client-array
   * @author Griefed
   */
  public MinecraftClient[] snapshotsArrayDescending() {
    return snapshotsDescending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get an array of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in
   * descending order.
   *
   * @return Snapshot client-array
   * @author Griefed
   */
  public MinecraftClient[] snapshotsArrayAscending() {
    return snapshotsAscending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * {@link Type#DESCENDING} order.
   *
   * @return All available Minecraft releases in descending order.
   * @author Griefed
   */
  public List<MinecraftClient> allDescending() {
    return MINECRAFT_CLIENT_META.all();
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * {@link Type#ASCENDING} order.
   *
   * @return All available Minecraft releases in ascending order.
   * @author Griefed
   */
  public List<MinecraftClient> allAscending() {
    return Lists.reverse(MINECRAFT_CLIENT_META.all());
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * {@link Type#DESCENDING} order.
   *
   * @return All available Minecraft releases in descending order.
   * @author Griefed
   */
  public MinecraftClient[] allDescendingArray() {
    return allDescending().toArray(new MinecraftClient[0]);
  }

  /**
   * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
   * {@link Type#ASCENDING} order.
   *
   * @return All available Minecraft releases in ascending order.
   * @author Griefed
   */
  public MinecraftClient[] allAscendingArray() {
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
   * Get a specific {@link MinecraftServer} for the specified Minecraft-version, wrapped in an
   * {@link Optional}.
   *
   * @param minecraftVersion The Minecraft-version.
   * @return Server wrapped in an {@link Optional}
   * @author Griefed
   */
  public Optional<MinecraftServer> getServer(String minecraftVersion) {
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
   * Check whether a {@link MinecraftServer} is available for the specified Minecraft-version.
   *
   * @param minecraftVersion The Minecraft-version.
   * @return <code>true</code> if a {@link MinecraftServer} is available.
   * @author Griefed
   */
  public boolean checkServerAvailability(String minecraftVersion) {
    return getServer(minecraftVersion).isPresent();
  }

  /**
   * Get the latest {@link MinecraftServer} of the {@link Type#RELEASE}, wrapped in an
   * {@link Optional}.
   *
   * @return Server wrapped in an {@link Optional}
   * @author Griefed
   */
  public MinecraftServer latestReleaseServer() {
    return MINECRAFT_CLIENT_META.latestRelease().server();
  }

  /**
   * Get the latest {@link MinecraftServer} of the {@link Type#SNAPSHOT}, wrapped in an
   * {@link Optional}.
   *
   * @return Server wrapped in an {@link Optional}
   * @author Griefed
   */
  public MinecraftServer latestSnapshotServer() {
    return MINECRAFT_CLIENT_META.latestSnapshot().server();
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * {@link Type#DESCENDING} order.
   *
   * @return Server-list
   * @author Griefed
   */
  public List<MinecraftServer> releasesServersDescending() {
    return MINECRAFT_SERVER_META.releases();
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * {@link Type#ASCENDING} order.
   *
   * @return Server-list
   * @author Griefed
   */
  public List<MinecraftServer> releasesServersAscending() {
    return Lists.reverse(MINECRAFT_SERVER_META.releases());
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * {@link Type#DESCENDING} order.
   *
   * @return Server-array
   * @author Griefed
   */
  public MinecraftServer[] releasesServersArrayDescending() {
    return releasesServersDescending().toArray(new MinecraftServer[0]);
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in
   * {@link Type#ASCENDING} order.
   *
   * @return Server-array
   * @author Griefed
   */
  public MinecraftServer[] releasesServersArrayAscending() {
    return releasesServersAscending().toArray(new MinecraftServer[0]);
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in
   * {@link Type#DESCENDING} order.
   *
   * @return Server-list
   * @author Griefed
   */
  public List<MinecraftServer> snapshotsServersDescending() {
    return MINECRAFT_SERVER_META.snapshots();
  }

  /**
   * Get a list of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in descending
   * order.
   *
   * @return Server-list
   * @author Griefed
   */
  public List<MinecraftServer> snapshotsServersAscending() {
    return Lists.reverse(MINECRAFT_SERVER_META.snapshots());
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in
   * ascending order.
   *
   * @return Server-array
   * @author Griefed
   */
  public MinecraftServer[] snapshotsServersArrayDescending() {
    return snapshotsServersDescending().toArray(new MinecraftServer[0]);
  }

  /**
   * Get an array of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in
   * descending order.
   *
   * @return Server-array
   * @author Griefed
   */
  public MinecraftServer[] snapshotsServersArrayAscending() {
    return snapshotsServersAscending().toArray(new MinecraftServer[0]);
  }
}
