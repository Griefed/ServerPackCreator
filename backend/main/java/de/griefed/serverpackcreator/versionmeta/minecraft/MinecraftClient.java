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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.Type;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeInstance;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Relevant information about a given Minecraft client.
 *
 * @author Griefed
 */
public final class MinecraftClient {

  private final String VERSION;
  private final Type TYPE;
  private final URL URL;
  private final MinecraftServer MINECRAFT_SERVER;
  private final ForgeMeta FORGE_META;
  private final Utilities UTILITIES;
  private final ApplicationProperties APPLICATIONPROPERTIES;

  /**
   * Constructor using version, type and url.
   *
   * @param version               The Minecraft version.
   * @param type                  Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
   * @param url                   Url to this versions manifest.
   * @param forgeMeta             To acquire Forge instances for this {@link MinecraftClient}
   *                              version.
   * @param objectMapper          Object mapper for JSON parsing.
   * @param utilities             Instance of commonly used utilities.
   * @param applicationProperties ServerPackCreator settings.
   * @author Griefed
   */
  MinecraftClient(
      String version, Type type, URL url, ForgeMeta forgeMeta, ObjectMapper objectMapper,
      Utilities utilities, ApplicationProperties applicationProperties) {
    APPLICATIONPROPERTIES = applicationProperties;
    UTILITIES = utilities;
    VERSION = version;
    TYPE = type;
    URL = url;
    FORGE_META = forgeMeta;
    MINECRAFT_SERVER = new MinecraftServer(version, type, url, objectMapper, UTILITIES,
        APPLICATIONPROPERTIES);
  }

  /**
   * Constructor using version, type, url and a {@link MinecraftServer}.
   *
   * @param version               The Minecraft version.
   * @param type                  Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
   * @param url                   Url to this versions manifest.
   * @param server                Instance of {@link MinecraftServer}
   * @param forgeMeta             To acquire Forge instances for this {@link MinecraftClient}
   *                              version.
   * @param utilities             Instance of commonly used utilities.
   * @param applicationProperties ServerPackCreator settings.
   * @author Griefed
   */
  MinecraftClient(
      String version, Type type, URL url, MinecraftServer server, ForgeMeta forgeMeta,
      Utilities utilities, ApplicationProperties applicationProperties) {
    APPLICATIONPROPERTIES = applicationProperties;
    UTILITIES = utilities;
    VERSION = version;
    TYPE = type;
    URL = url;
    MINECRAFT_SERVER = server;
    FORGE_META = forgeMeta;
  }

  /**
   * The Minecraft version of this {@link MinecraftClient} instance.
   *
   * @return Minecraft version.
   * @author Griefed
   */
  public String version() {
    return VERSION;
  }

  /**
   * Release-type. Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
   *
   * @return Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
   * @author Griefed
   */
  public Type type() {
    return TYPE;
  }

  /**
   * The {@link URL} to this versions manifest.
   *
   * @return URL
   * @author Griefed
   */
  public URL url() {
    return URL;
  }

  /**
   * The {@link MinecraftServer} for this Minecraft version, wrapped in an {@link Optional}.
   *
   * @return Server wrapped in an {@link Optional}
   * @author Griefed
   */
  public MinecraftServer server() {
    return MINECRAFT_SERVER;
  }

  /**
   * Get the {@link ForgeInstance} for this client, wrapped in an {@link Optional}.
   *
   * @return Forge instance for this client, wrapped in an {@link Optional}.
   * @author Griefed
   */
  public Optional<List<ForgeInstance>> forge() {
    return FORGE_META.getForgeInstances(VERSION);
  }
}
