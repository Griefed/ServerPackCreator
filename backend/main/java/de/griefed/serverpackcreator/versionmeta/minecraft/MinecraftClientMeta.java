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
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Minecraft client meta containing informatiom about available Minecraft releases.
 *
 * @author Griefed
 */
public class MinecraftClientMeta {

    private static final Logger LOG = LogManager.getLogger(MinecraftClientMeta.class);
    private final ObjectMapper OBJECTMAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    private final ForgeMeta FORGE_META;
    private final File MINECRAFT_MANIFEST;
    private final List<MinecraftClient> RELEASES = new ArrayList<>();
    private final List<MinecraftClient> SNAPSHOTS = new ArrayList<>();

    private MinecraftClient latestRelease;
    private MinecraftClient latestSnapshot;
    private HashMap<String, MinecraftClient> meta = new HashMap<>();

    /**
     * Constructor.
     *
     * @param injectedForgeMeta {@link ForgeMeta} to acquire Forge instances for this {@link MinecraftClient} version.
     * @param minecraftManifest {@link File} Minecraft manifest file.
     * @author Griefed
     */
    protected MinecraftClientMeta(File minecraftManifest, ForgeMeta injectedForgeMeta) {
        this.MINECRAFT_MANIFEST = minecraftManifest;
        this.FORGE_META = injectedForgeMeta;
    }

    /**
     * Update the meta information.
     *
     * @return This instance of {@link MinecraftClientMeta}
     * @throws IOException if the manifest could not be read.
     * @author Griefed
     */
    protected MinecraftClientMeta update() throws IOException {

        RELEASES.clear();
        SNAPSHOTS.clear();
        meta = new HashMap<>();

        JsonNode minecraftManifest = OBJECTMAPPER.readTree(this.MINECRAFT_MANIFEST);

        minecraftManifest.get("versions").forEach(minecraftVersion -> {

            if (minecraftVersion.get("type").asText().equalsIgnoreCase("release")) {

                try {
                    RELEASES.add(new MinecraftClient(
                            minecraftVersion.get("id").asText(),
                            Type.RELEASE,
                            new URL(minecraftVersion.get("url").asText()),
                            this.FORGE_META
                    ));
                } catch (IOException ex) {
                    LOG.debug("No server available for MinecraftClient version " + minecraftVersion.get("id").asText(), ex);
                }

            } else if (minecraftVersion.get("type").asText().equalsIgnoreCase("snapshot")) {

                try {
                    SNAPSHOTS.add(new MinecraftClient(
                            minecraftVersion.get("id").asText(),
                            Type.SNAPSHOT,
                            new URL(minecraftVersion.get("url").asText()),
                            this.FORGE_META
                    ));
                } catch (IOException ex) {
                    LOG.debug("No server available for MinecraftClient version " + minecraftVersion.get("id").asText(), ex);
                }
            }

        });

        RELEASES.forEach(minecraftClient -> meta.put(minecraftClient.version(), minecraftClient));
        SNAPSHOTS.forEach(minecraftClient -> meta.put(minecraftClient.version(), minecraftClient));

        this.latestRelease = new MinecraftClient(
                minecraftManifest.get("latest").get("release").asText(),
                Type.RELEASE,
                meta.get(minecraftManifest.get("latest").get("release").asText()).url(),
                meta.get(minecraftManifest.get("latest").get("release").asText()).server(),
                this.FORGE_META
        );
        this.latestSnapshot = new MinecraftClient(
                minecraftManifest.get("latest").get("snapshot").asText(),
                Type.SNAPSHOT,
                meta.get(minecraftManifest.get("latest").get("snapshot").asText()).url(),
                meta.get(minecraftManifest.get("latest").get("snapshot").asText()).server(),
                this.FORGE_META
        );

        return this;
    }

    /**
     * Get a list of {@link MinecraftClient} of the {@link Type#RELEASE}.
     *
     * @return {@link MinecraftClient}-list of the {@link Type#RELEASE}.
     * @author Griefed
     */
    protected List<MinecraftClient> releases() {
        return RELEASES;
    }

    /**
     * Get a list of {@link MinecraftClient} of the {@link Type#SNAPSHOT}.
     *
     * @return {@link MinecraftClient}-list of the {@link Type#SNAPSHOT}.
     * @author Griefed
     */
    protected List<MinecraftClient> snapshots() {
        return SNAPSHOTS;
    }

    /**
     * Get the latest Minecraft {@link Type#RELEASE} as a {@link MinecraftClient}.
     *
     * @return {@link MinecraftClient} {@link Type#RELEASE} as a {@link MinecraftClient}
     * @author Griefed
     */
    protected MinecraftClient latestRelease() {
        return latestRelease;
    }

    /**
     * Get the latest Minecraft {@link Type#SNAPSHOT} as a {@link MinecraftClient}.
     *
     * @return {@link MinecraftClient} {@link Type#SNAPSHOT} as a {@link MinecraftClient}
     * @author Griefed
     */
    protected MinecraftClient latestSnapshot() {
        return latestSnapshot;
    }

    /**
     * Get the {@link MinecraftClient} meta.<br>
     * key: {@link String} Minecraft version<br>
     * value: {@link MinecraftClient} for said Minecraft version
     *
     * @return {@link HashMap} containing the {@link MinecraftClientMeta}.
     * @author Griefed
     */
    protected HashMap<String, MinecraftClient> meta() {
        return meta;
    }
}
