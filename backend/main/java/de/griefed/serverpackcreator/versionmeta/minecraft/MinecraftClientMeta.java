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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.versionmeta.Manifests;
import de.griefed.serverpackcreator.versionmeta.Type;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Minecraft client meta containing informatiom about available Minecraft releases.
 *
 * @author Griefed
 */
final class MinecraftClientMeta extends Manifests {

  private static final Logger LOG = LogManager.getLogger(MinecraftClientMeta.class);
  private final ObjectMapper OBJECTMAPPER;

  private final ForgeMeta FORGE_META;
  private final File MINECRAFT_MANIFEST;
  private final List<MinecraftClient> RELEASES = new ArrayList<>();
  private final List<MinecraftClient> SNAPSHOTS = new ArrayList<>();
  private final List<MinecraftClient> ALL = new ArrayList<>();

  private MinecraftClient latestRelease;
  private MinecraftClient latestSnapshot;
  private HashMap<String, MinecraftClient> meta = new HashMap<>();

  /**
   * Create a new Minecraft Client Meta.
   *
   * @param injectedForgeMeta To acquire Forge instances for this {@link MinecraftClient} version.
   * @param minecraftManifest Minecraft manifest file.
   * @param objectMapper      Object mapper for JSON parsing.
   * @author Griefed
   */
  MinecraftClientMeta(
      File minecraftManifest, ForgeMeta injectedForgeMeta, ObjectMapper objectMapper) {
    this.MINECRAFT_MANIFEST = minecraftManifest;
    this.FORGE_META = injectedForgeMeta;
    this.OBJECTMAPPER = objectMapper;
  }

  /**
   * Update the meta information.
   *
   * @throws IOException if the manifest could not be read.
   * @author Griefed
   */
  void update() throws IOException {

    RELEASES.clear();
    SNAPSHOTS.clear();
    meta = new HashMap<>();

    JsonNode minecraftManifest = getJson(MINECRAFT_MANIFEST, OBJECTMAPPER);

    minecraftManifest
        .get("versions")
        .forEach(
            minecraftVersion -> {

              MinecraftClient client = null;

              if (minecraftVersion.get("type").asText().equalsIgnoreCase("release")) {

                try {
                  client = new MinecraftClient(
                      minecraftVersion.get("id").asText(),
                      Type.RELEASE,
                      new URL(minecraftVersion.get("url").asText()),
                      this.FORGE_META,
                      OBJECTMAPPER);

                  RELEASES.add(client);

                } catch (IOException ex) {
                  LOG.debug(
                      "No server available for MinecraftClient version "
                          + minecraftVersion.get("id").asText(),
                      ex);
                }

              } else if (minecraftVersion.get("type").asText().equalsIgnoreCase("snapshot")) {

                try {
                  client = new MinecraftClient(
                      minecraftVersion.get("id").asText(),
                      Type.SNAPSHOT,
                      new URL(minecraftVersion.get("url").asText()),
                      this.FORGE_META,
                      OBJECTMAPPER);

                  SNAPSHOTS.add(client);
                } catch (IOException ex) {
                  LOG.debug(
                      "No server available for MinecraftClient version "
                          + minecraftVersion.get("id").asText(),
                      ex);
                }
              }

              if (client != null) {
                meta.put(client.version(), client);
                ALL.add(client);
              }
            });

    this.latestRelease =
        new MinecraftClient(
            minecraftManifest.get("latest").get("release").asText(),
            Type.RELEASE,
            meta.get(minecraftManifest.get("latest").get("release").asText()).url(),
            meta.get(minecraftManifest.get("latest").get("release").asText()).server(),
            this.FORGE_META);
    this.latestSnapshot =
        new MinecraftClient(
            minecraftManifest.get("latest").get("snapshot").asText(),
            Type.SNAPSHOT,
            meta.get(minecraftManifest.get("latest").get("snapshot").asText()).url(),
            meta.get(minecraftManifest.get("latest").get("snapshot").asText()).server(),
            this.FORGE_META);
  }

  /**
   * All available Minecraft releases.
   *
   * @return All available Minecraft releases.
   * @author Griefed
   */
  List<MinecraftClient> all() {
    return ALL;
  }

  /**
   * Get a list of {@link MinecraftClient} of the {@link Type#RELEASE}.
   *
   * @return Client-list of the {@link Type#RELEASE}.
   * @author Griefed
   */
  List<MinecraftClient> releases() {
    return RELEASES;
  }

  /**
   * Get a list of {@link MinecraftClient} of the {@link Type#SNAPSHOT}.
   *
   * @return Client-list of the {@link Type#SNAPSHOT}.
   * @author Griefed
   */
  List<MinecraftClient> snapshots() {
    return SNAPSHOTS;
  }

  /**
   * Get the latest Minecraft {@link Type#RELEASE} as a {@link MinecraftClient}.
   *
   * @return Latest release as a {@link MinecraftClient}
   * @author Griefed
   */
  MinecraftClient latestRelease() {
    return latestRelease;
  }

  /**
   * Get the latest Minecraft {@link Type#SNAPSHOT} as a {@link MinecraftClient}.
   *
   * @return Latest snapshot as a {@link MinecraftClient}
   * @author Griefed
   */
  MinecraftClient latestSnapshot() {
    return latestSnapshot;
  }

  /**
   * Get the {@link MinecraftClient} meta.<br> key: {@link String} Minecraft version<br> value:
   * {@link MinecraftClient} for said Minecraft version
   *
   * @return Map containing the {@link MinecraftClientMeta}.
   * @author Griefed
   */
  HashMap<String, MinecraftClient> meta() {
    return meta;
  }
}
