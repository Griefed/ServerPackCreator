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

import java.util.Date;
import java.util.List;

/**
 * Fabric loader details
 *
 * @author Griefed
 */
public class FabricDetails {

  private String id;
  private String inheritsFrom;
  private Date releaseTime;
  private Date time;
  private String type;
  private String mainClass;
  private FabricArguments arguments;
  private List<FabricLibrary> libraries;

  private FabricDetails() {
  }

  /**
   * Fabric loader ID, in the format of <code>fabric-loader-FABRIC_VERSION-MINECRAFT_VERSION
   * </code>.
   *
   * @return {@link String} The Fabric loader ID for the requested Minecraft and Fabric versions.
   * @author Griefed
   */
  public String getId() {
    return id;
  }

  /**
   * The Minecraft version from which this Fabric loader version inherits from.
   *
   * @return {@link String} The Minecrat version of this Fabric loader.
   * @author Griefed
   */
  public String getInheritsFrom() {
    return inheritsFrom;
  }

  /**
   * The date at which this loader was released.
   *
   * @return {@link Date} The release date of this Fabric loader.
   * @author Griefed
   */
  public Date getReleaseTime() {
    return releaseTime;
  }

  /**
   * Probably the same as {@link #getReleaseTime()}. Not sure. It's a field in the JSON you receive
   * from Fabric. - Griefed.
   *
   * @return {@link Date} The date of this Fabric loader.
   * @author Griefed
   */
  public Date getTime() {
    return time;
  }

  /**
   * The release type of this Fabric loader.
   *
   * @return {@link String} Release type of this Fabric loader.
   * @author Griefed
   */
  public String getType() {
    return type;
  }

  /**
   * The Main class of this Fabric loader.
   *
   * @return {@link String} Main class.
   * @author Griefed
   */
  public String getMainClass() {
    return mainClass;
  }

  /**
   * {@link FabricArguments} used by this Fabric loader.
   *
   * @return {@link FabricArguments} of this Fabric loader.
   * @author Griefed
   */
  public FabricArguments getArguments() {
    return arguments;
  }

  /**
   * {@link FabricLibrary}-list used by this Fabric loader.
   *
   * @return {@link FabricLibrary}-list used by this Fabric loader.
   * @author Griefed
   */
  public List<FabricLibrary> getLibraries() {
    return libraries;
  }
}
