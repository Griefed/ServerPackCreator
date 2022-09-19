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

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;

/**
 * Information about releases of the Fabric loader.
 *
 * @author Griefed
 */
final class FabricLoader {

  private final List<String> loaders = new ArrayList<>(100);
  private String latest;
  private String release;

  /**
   * Create a new instance of the Fabric Loader.
   *
   * @param loaderManifest Fabrics manifest.
   * @author Griefed
   */
  FabricLoader(Document loaderManifest) {
    update(loaderManifest);
  }

  /**
   * Update the latest, release and releases information.
   *
   * @param loaderManifest Fabrics manifest.
   * @author Griefed
   */
  void update(Document loaderManifest) {
    this.latest =
        loaderManifest
            .getElementsByTagName("latest")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    this.release =
        loaderManifest
            .getElementsByTagName("release")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    this.loaders.clear();
    for (int i = 0; i < loaderManifest.getElementsByTagName("version").getLength(); i++) {
      loaders.add(
          loaderManifest
              .getElementsByTagName("version")
              .item(i)
              .getChildNodes()
              .item(0)
              .getNodeValue());
    }
  }

  /**
   * Get the latest Fabric loader version.
   *
   * @return The latest Fabric loader version.
   * @author Griefed
   */
  String latestLoaderVersion() {
    return latest;
  }

  /**
   * Get the release Fabric loader version.
   *
   * @return The release Fabric loader version.
   * @author Griefed
   */
  String releaseLoaderVersion() {
    return release;
  }

  /**
   * Get the list of available Fabric loader versions.
   *
   * @return List of the available Fabric loader versions.
   * @author Griefed
   */
  List<String> loaders() {
    return loaders;
  }
}
