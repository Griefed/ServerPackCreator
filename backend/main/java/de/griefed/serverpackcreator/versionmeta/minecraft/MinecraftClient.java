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

import de.griefed.serverpackcreator.versionmeta.Type;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeInstance;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Relevant information about a given Minecraft client.
 *
 * @author Griefed
 */
public class MinecraftClient {

    private static final Logger LOG = LogManager.getLogger(MinecraftClient.class);

    private final String VERSION;
    private final Type TYPE;
    private final URL URL;
    private final MinecraftServer MINECRAFT_SERVER;
    private ForgeMeta FORGE_META;

    /**
     * Constructor using version, type and url.
     *
     * @param version   {@link String} The Minecraft version.
     * @param type      {@link Type} Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     * @param url       {@link URL} Url to this versions manifest.
     * @param forgeMeta {@link ForgeMeta} to acquire Forge instances for this {@link MinecraftClient} version.
     * @author Griefed
     */
    protected MinecraftClient(String version, Type type, URL url, ForgeMeta forgeMeta) {
        this.VERSION = version;
        this.TYPE = type;
        this.URL = url;
        this.FORGE_META = forgeMeta;
        this.MINECRAFT_SERVER = new MinecraftServer(version, type, url);
    }

    /**
     * Constructor using version, type, url and a {@link MinecraftServer}.
     *
     * @param version   {@link String} The Minecraft version.
     * @param type      {@link Type} Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     * @param url       {@link URL} Url to this versions manifest.
     * @param server    Instance of {@link MinecraftServer}
     * @param forgeMeta {@link ForgeMeta} to acquire Forge instances for this {@link MinecraftClient} version.
     * @author Griefed
     */
    protected MinecraftClient(String version, Type type, URL url, MinecraftServer server, ForgeMeta forgeMeta) {
        this.VERSION = version;
        this.TYPE = type;
        this.URL = url;
        this.MINECRAFT_SERVER = server;
        this.FORGE_META = forgeMeta;
    }

    /**
     * The Minecraft version of this {@link MinecraftClient} instance.
     *
     * @return {@link String} Minecraft version.
     * @author Griefed
     */
    public String version() {
        return VERSION;
    }

    /**
     * Release-type. Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     *
     * @return {@link Type} Either {@link Type#RELEASE} or {@link Type#SNAPSHOT}.
     * @author Griefed
     */
    public Type type() {
        return TYPE;
    }

    /**
     * The {@link URL} to this versions manifest.
     *
     * @return {@link URL}
     * @author Griefed
     */
    public URL url() {
        return URL;
    }

    /**
     * The {@link MinecraftServer} for this Minecraft version, wrapped in an {@link Optional}.
     *
     * @return {@link MinecraftServer} wrapped in an {@link Optional}
     * @author Griefed
     */
    public MinecraftServer server() {
        return MINECRAFT_SERVER;
    }

    /**
     * Get the {@link ForgeInstance} for this client, wrapped in an {@link Optional}.
     *
     * @return {@link ForgeInstance} for this client, wrapped in an {@link Optional}.
     * @author Griefed
     */
    public Optional<List<ForgeInstance>> forge() {
        return FORGE_META.getForgeInstances(VERSION);
    }
}
