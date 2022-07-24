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
 * Library information.
 *
 * @author Griefed
 */
public class FabricLibrary {

  private String name;
  private String url;

  private FabricLibrary() {}

  /**
   * The name of this library.
   *
   * @return {@link String} Library-name.
   * @author Griefed
   */
  public String getName() {
    return name;
  }

  /**
   * The URL to this library.
   *
   * @return {@link String} Library-URL as a String.
   * @author Griefed
   */
  public String getUrl() {
    return url;
  }
}
