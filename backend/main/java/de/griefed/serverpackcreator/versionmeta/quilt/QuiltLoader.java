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
package de.griefed.serverpackcreator.versionmeta.quilt;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;

/**
 * Information about releases of the Quilt loader.
 *
 * @author Griefed
 */
public class QuiltLoader {

  private final List<String> loaders = new ArrayList<>();
  private String latest;
  private String release;

  /**
   * Create a new Quilt Loader instance.
   *
   * @param loaderManifest {@link Document} containing Quilts manifest.
   * @author Griefed
   */
  protected QuiltLoader(Document loaderManifest) {

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
   * Update the latest, release and releases information.
   *
   * @param loaderManifest {@link Document} containing Quilts manifest.
   * @author Griefed
   */
  protected void update(Document loaderManifest) {
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
   * Get the latest Quilt loader version.
   *
   * @return {@link String} The latest Quilt loader version.
   * @author Griefed
   */
  protected String latestLoaderVersion() {
    return latest;
  }

  /**
   * Get the release Quilt loader version.
   *
   * @return {@link String} The release Quilt loader version.
   * @author Griefed
   */
  protected String releaseLoaderVersion() {
    return release;
  }

  /**
   * Get the list of available Quilt loader versions.
   *
   * @return {@link String}-list of the available Quilt loader versions.
   * @author Griefed
   */
  protected List<String> loaders() {
    return loaders;
  }
}
