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

import de.griefed.serverpackcreator.versionmeta.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Meta containing information about Minecraft servers.
 *
 * @author Griefed
 */
class MinecraftServerMeta {

  private final MinecraftClientMeta MINECRAFT_CLIENT_META;
  private final List<MinecraftServer> RELEASES = new ArrayList<>();
  private final List<MinecraftServer> SNAPSHOTS = new ArrayList<>();
  private HashMap<String, MinecraftServer> meta;

  /**
   * Create a new Minecraft Server Meta instance.
   *
   * @param minecraftClientMeta Instance of {@link MinecraftClientMeta}.
   * @author Griefed
   */
  protected MinecraftServerMeta(MinecraftClientMeta minecraftClientMeta) {
    this.MINECRAFT_CLIENT_META = minecraftClientMeta;
  }

  /**
   * Update this instance of {@link MinecraftServerMeta} with new information.
   *
   * @author Griefed
   */
  protected void update() {

    this.RELEASES.clear();
    MINECRAFT_CLIENT_META.releases().forEach(client -> this.RELEASES.add(client.server()));

    this.SNAPSHOTS.clear();
    MINECRAFT_CLIENT_META.snapshots().forEach(client -> this.SNAPSHOTS.add(client.server()));

    this.meta = new HashMap<>();
    MINECRAFT_CLIENT_META
        .releases()
        .forEach(client -> this.meta.put(client.version(), client.server()));
    MINECRAFT_CLIENT_META
        .snapshots()
        .forEach(client -> this.meta.put(client.version(), client.server()));
  }

  /**
   * Get a list of {@link MinecraftServer} of the {@link Type#RELEASE}.
   *
   * @return {@link MinecraftServer}-list of the {@link Type#RELEASE}.
   * @author Griefed
   */
  protected List<MinecraftServer> releases() {
    return RELEASES;
  }

  /**
   * Get a list of {@link MinecraftServer} of the {@link Type#SNAPSHOT}.
   *
   * @return {@link MinecraftServer}-list of the {@link Type#SNAPSHOT}.
   * @author Griefed
   */
  protected List<MinecraftServer> snapshots() {
    return SNAPSHOTS;
  }

  /**
   * Get the {@link MinecraftServer} meta.<br>
   * key: {@link String} Minecraft version<br>
   * value: {@link MinecraftServer} for said Minecraft version
   *
   * @return {@link HashMap} containing the {@link MinecraftServerMeta}.
   * @author Griefed
   */
  protected HashMap<String, MinecraftServer> meta() {
    return meta;
  }
}
