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
package de.griefed.serverpackcreator.versionmeta.forge;

import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftClient;
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

/**
 * An instance of a complete Forge combination, containing a Minecraft version, related Forge
 * version and the URL to the server installer.
 *
 * @author Griefed
 */
public class ForgeInstance {

  private final String MINECRAFT_VERSION;
  private final String FORGE_VERSION;
  private final URL INSTALLER_URL;
  private final MinecraftMeta MINECRAFT_META;

  /**
   * Create a new Forge Instance instance.
   *
   * @param minecraftVersion Minecraft version.
   * @param forgeVersion     Forge version.
   * @param minecraftMeta    The corresponding Minecraft client for this Forge version.
   * @throws MalformedURLException if the URL to the download of the Forge server installer could
   *                               not be created.
   * @author Griefed
   */
  public ForgeInstance(String minecraftVersion, String forgeVersion, MinecraftMeta minecraftMeta)
      throws MalformedURLException {
    this.MINECRAFT_VERSION = minecraftVersion;
    this.FORGE_VERSION = forgeVersion;
    this.INSTALLER_URL =
        new URL(
            String.format(
                "https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar",
                minecraftVersion, forgeVersion, minecraftVersion, forgeVersion));
    this.MINECRAFT_META = minecraftMeta;
  }

  /**
   * Get the Minecraft version of this Forge instance.
   *
   * @return Minecraft version.
   * @author Griefed
   */
  public String minecraftVersion() {
    return MINECRAFT_VERSION;
  }

  /**
   * Get the Forge version of this Forge instance.
   *
   * @return Forge version.
   * @author Griefed
   */
  public String forgeVersion() {
    return FORGE_VERSION;
  }

  /**
   * Get the URL to the Forge server installer for this instances Minecraft and Forge version.
   *
   * @return Download {@link URL} to the Forge server installer JAR-file.
   * @author Griefed
   */
  public URL installerUrl() {
    return INSTALLER_URL;
  }

  /**
   * Get this Forge instances corresponding Minecraft client instance, wrapped in an
   * {@link Optional}
   *
   * @return Client wrapped in an {@link Optional}.
   * @author Griefed
   */
  public Optional<MinecraftClient> minecraftClient() {
    return MINECRAFT_META.getClient(this.MINECRAFT_VERSION);
  }
}
