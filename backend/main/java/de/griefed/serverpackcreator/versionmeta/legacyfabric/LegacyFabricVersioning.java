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
package de.griefed.serverpackcreator.versionmeta.legacyfabric;

import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * LegacyFabric version parent-class, implemented by {@link LegacyFabricGame} and
 * {@link LegacyFabricLoader} to store and provide version information for game and loader
 * versions.
 */
abstract class LegacyFabricVersioning {

  private final List<String> RELEASES = new ArrayList<>(100);
  private final List<String> SNAPSHOTS = new ArrayList<>(100);
  private final List<String> ALL = new ArrayList<>(200);
  private final Utilities UTILITIES;
  private final File MANIFEST;

  /**
   * @param manifest  The manifest holding the version information for this LegacyFabric type.
   * @param utilities Commonly used utilities across ServerPackCreator.
   */
  LegacyFabricVersioning(@NotNull File manifest,
                         @NotNull Utilities utilities) {
    UTILITIES = utilities;
    MANIFEST = manifest;
  }

  /**
   * Update all lists of available versions with new information gathered from the manifest.
   *
   * @throws IOException When the manifest could not be read.
   */
  void update() throws IOException {
    RELEASES.clear();
    SNAPSHOTS.clear();
    ALL.clear();

    UTILITIES.JsonUtilities().getJson(MANIFEST).forEach(node -> {
      String version = node.get("version").asText();
      ALL.add(version);
      if (node.get("stable").asBoolean()) {
        RELEASES.add(version);
      } else {
        SNAPSHOTS.add(version);
      }
    });
  }

  /**
   * List of release versions for this meta.
   *
   * @return Release versions.
   * @author Griefed
   */
  @NotNull List<String> releases() {
    return RELEASES;
  }

  /**
   * List of snapshot/pre-release versions for this meta.
   *
   * @return Snapshot/pre-release versions.
   * @author Griefed
   */
  @NotNull List<String> snapshots() {
    return SNAPSHOTS;
  }

  /**
   * List of all versions for this meta.
   *
   * @return All versions.
   * @author Griefed
   */
  @NotNull List<String> all() {
    return ALL;
  }

}
