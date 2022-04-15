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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.versionmeta.Type;

import java.net.URL;

/**
 * Representation of a Minecraft server, containing information about its Minecraft-version, release-type, download-url
 * and the java-version.
 * @author Griefed
 */
public class MinecraftServer {

    private final String VERSION;
    private final Type TYPE;
    private final URL URL;
    private final byte JAVA_VERSION;

    /**
     * Constructor
     * @author Griefed
     * @param mcVersion {@link String} The Minecraft version of this server.
     * @param mcType {@link Type} The release-type of this server. Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     * @param mcUrl {@link URL} The URL to the download of this servers JAR-file.
     * @throws Exception if the servers manifest could not be acquired, the download URL could not be parsed, or the
     * Java-version could not be parsed.
     */
    protected MinecraftServer(String mcVersion, Type mcType, URL mcUrl) throws Exception {

        this.VERSION = mcVersion;
        this.TYPE = mcType;

        JsonNode serverJson = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .readTree(mcUrl.openStream());

        this.URL = new URL(serverJson.get("downloads").get("server").get("url").asText());
        this.JAVA_VERSION = Byte.parseByte(serverJson.get("javaVersion").get("majorVersion").asText());

    }

    /**
     * Get the Minecraft-version of this {@link MinecraftServer}.
     * @author Griefed
     * @return {@link String}
     */
    public String version() {
        return VERSION;
    }

    /**
     * Get the release-type of this Minecraft-server. Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     * @author Griefed
     * @return {@link Type}
     */
    public Type type() {
        return TYPE;
    }

    /**
     * Get the {@link URL} to the download of this Minecraft-servers JAR-file.
     * @author Griefed
     * @return {@link URL}
     */
    public URL url() {
        return URL;
    }

    /**
     * Get the Java-version of this Minecraft-server.
     * @author Griefed
     * @return {@link Byte}.
     */
    public byte javaVersion() {
        return JAVA_VERSION;
    }
}
