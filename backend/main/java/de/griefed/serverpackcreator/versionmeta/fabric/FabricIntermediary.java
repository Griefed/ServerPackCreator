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

/**
 * A Fabric intermediary.
 *
 * @author Griefed
 */
public final class FabricIntermediary {

  private String maven;
  private String version;
  private boolean stable;

  FabricIntermediary() {
  }

  /**
   * The maven mapping of this intermediary.
   *
   * @return maven mapping.
   * @author Griefed
   */
  public String getMaven() {
    return maven;
  }

  /**
   * The version of this intermediary.
   *
   * @return The version of this intermediary.
   * @author Griefed
   */
  public String getVersion() {
    return version;
  }

  /**
   * Whether this intermediary is considered stable.
   *
   * @return Whether this intermediary is considered stable.
   * @author Griefed
   */
  public boolean isStable() {
    return stable;
  }
}
