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
package de.griefed.serverpackcreator.versionmeta;

/**
 * Enums for various aspects of the VersionMeta.
 *
 * @author Griefed
 */
public enum Type {

  /**
   * Indicates that an object is a full release, meaning no snapshot, alpha, or beta.
   */
  RELEASE,
  /**
   * Indicates that an object is a pre-release, so either a snapshot (duh..), alpha, beta etc.
   */
  SNAPSHOT,
  /**
   * Minecraft client, so that which a users runs on their gaming machine.
   */
  CLIENT,
  /**
   * Minecraft server, usually run on a dedicated server.
   */
  SERVER,
  /**
   * Sort ascending, small to large.
   */
  ASCENDING,
  /**
   * Sort descending, large to small.
   */
  DESCENDING,

  /**
   * Indicates this operation concerns Minecraft.
   */
  MINECRAFT,

  /**
   * Indicates this operation concerns Forge.
   */
  FORGE,

  /**
   * Indicates this operation concerns Fabric.
   */
  FABRIC,

  /**
   * Indicates this operation concerns Fabric Installer.
   */
  FABRIC_INSTALLER,

  /**
   * Indicates this operation concerns Quilt.
   */
  QUILT,

  /**
   * Indicates this operation concerns Fabric Intermediaries.
   */
  FABRIC_INTERMEDIARIES,

  /**
   * Indicates this operation concerns Quilt Installer.
   */
  QUILT_INSTALLER
}
