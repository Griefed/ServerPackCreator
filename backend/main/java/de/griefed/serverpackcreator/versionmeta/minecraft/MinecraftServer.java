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

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Representation of a Minecraft server, containing information about its Minecraft-version, release-type, download-url
 * and the java-version.
 *
 * @author Griefed
 */
public class MinecraftServer {

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    private final URL MANIFEST_URL;
    private final String VERSION;
    private final Type TYPE;

    private JsonNode serverJson = null;

    /**
     * Constructor
     *
     * @param mcVersion {@link String} The Minecraft version of this server.
     * @param mcType    {@link Type} The release-type of this server. Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     * @param mcUrl     {@link URL} The URL to the download of this servers JAR-file.
     * @author Griefed
     */
    protected MinecraftServer(String mcVersion, Type mcType, URL mcUrl) {
        this.MANIFEST_URL = mcUrl;
        this.VERSION = mcVersion;
        this.TYPE = mcType;
    }

    private void setServerJson() {
        try {
            this.serverJson = OBJECT_MAPPER.readTree(MANIFEST_URL.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the Minecraft-version of this {@link MinecraftServer}.
     *
     * @return {@link String}
     * @author Griefed
     */
    public String version() {
        return VERSION;
    }

    /**
     * Get the release-type of this Minecraft-server. Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     *
     * @return {@link Type}
     * @author Griefed
     */
    public Type type() {
        return TYPE;
    }

    /**
     * Get the {@link URL} to the download of this Minecraft-servers JAR-file.
     *
     * @return {@link URL}
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
     * @return {@link Byte}.
     * @author Griefed
     */
    public Optional<Byte> javaVersion() {
        if (serverJson == null) {
            setServerJson();
        }
        try {
            return Optional.of(Byte.parseByte(serverJson.get("javaVersion").get("majorVersion").asText()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
