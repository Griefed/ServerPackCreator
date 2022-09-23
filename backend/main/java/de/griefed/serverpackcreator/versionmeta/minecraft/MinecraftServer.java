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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.ManifestParser;
import de.griefed.serverpackcreator.versionmeta.Type;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Representation of a Minecraft server, containing information about its Minecraft-version,
 * release-type, download-url and the java-version.
 *
 * @author Griefed
 */
public final class MinecraftServer extends ManifestParser {

  private final Utilities UTILITIES;
  private final ObjectMapper OBJECT_MAPPER;
  private final URL MANIFEST_URL;
  private final File MANIFEST_FILE;
  private final String VERSION;
  private final Type TYPE;

  private JsonNode serverJson = null;

  /**
   * Create a new Minecraft Server.
   *
   * @param mcVersion             The Minecraft version of this server.
   * @param mcType                The release-type of this server. Either {@link Type#RELEASE} or
   *                              {@link Type#SNAPSHOT}.
   * @param mcUrl                 The URL to the download of this servers JAR-file.
   * @param objectMapper          Object mapper for JSON parsing.
   * @param utilities             Instance of commonly used utilities.
   * @param applicationProperties ServerPackCreator settings.
   * @author Griefed
   */
  MinecraftServer(String mcVersion, Type mcType, URL mcUrl, ObjectMapper objectMapper,
      Utilities utilities, ApplicationProperties applicationProperties) {

    UTILITIES = utilities;
    MANIFEST_URL = mcUrl;
    VERSION = mcVersion;
    MANIFEST_FILE = new File(
        applicationProperties.MINECRAFT_SERVER_MANIFEST_LOCATION() + VERSION + ".json");
    TYPE = mcType;
    OBJECT_MAPPER = objectMapper;
  }

  /**
   * Read and store the server manifest.
   *
   * @author Griefed
   */
  private void setServerJson() {
    if (!MANIFEST_FILE.exists()) {
      UTILITIES.WebUtils().downloadFile(MANIFEST_FILE, MANIFEST_URL);
    }

    try {
      serverJson = getJson(MANIFEST_FILE, OBJECT_MAPPER);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the Minecraft-version of this server.
   *
   * @return Version.
   * @author Griefed
   */
  public String version() {
    return VERSION;
  }

  /**
   * Get the release-type of this Minecraft-server. Either {@link Type#RELEASE} or
   * {@link Type#SNAPSHOT}.
   *
   * @return Type.
   * @author Griefed
   */
  public Type type() {
    return TYPE;
  }

  /**
   * Get the {@link URL} to the download of this Minecraft-servers JAR-file.
   *
   * @return URL.
   * @author Griefed
   */
  public Optional<URL> url() {
    if (serverJson == null) {
      setServerJson();
    }
    try {
      return Optional.of(new URL(serverJson.get("downloads").get("server").get("url").asText()));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Get the Java-version of this Minecraft-server.
   *
   * @return Java version.
   * @author Griefed
   */
  public Optional<Byte> javaVersion() {
    if (serverJson == null) {
      setServerJson();
    }
    try {
      return Optional.of(
          Byte.parseByte(serverJson.get("javaVersion").get("majorVersion").asText()));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
