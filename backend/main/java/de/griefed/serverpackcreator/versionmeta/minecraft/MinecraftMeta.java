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

import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.versionmeta.Type;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Minecraft meta containing information about available client- and server releases.
 * @author Griefed
 */
public class MinecraftMeta {

    private final MinecraftClientMeta MINECRAFT_CLIENT_META;
    private final MinecraftServerMeta MINECRAFT_SERVER_META;

    /**
     * Constructor.
     * @author Griefed
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
     * @param injectedForgeMeta {@link ForgeMeta} to acquire Forge instances for this {@link MinecraftClient} version.
     */
    public MinecraftMeta(ApplicationProperties injectedApplicationProperties, ForgeMeta injectedForgeMeta) {
        this.MINECRAFT_CLIENT_META = new MinecraftClientMeta(injectedApplicationProperties, injectedForgeMeta);
        this.MINECRAFT_SERVER_META = new MinecraftServerMeta(this.MINECRAFT_CLIENT_META);
    }

    /**
     * Update the {@link MinecraftClientMeta} and {@link MinecraftServerMeta}. Usually called after the manifest-files
     * have been refreshed.
     * @author Griefed
     * @return This instance of {@link MinecraftMeta}.
     * @throws IOException if the {@link MinecraftClientMeta} could not be initialized.
     */
    public MinecraftMeta update() throws IOException {
        this.MINECRAFT_CLIENT_META.update();
        this.MINECRAFT_SERVER_META.update();
        return this;
    }

/*
 * #==============================================================================================================#
 * #..............................................................................................................#
 * #...................................................STRINGS....................................................#
 * #..............................................................................................................#
 * #==============================================================================================================#
 */
    /**
     * Check whether a {@link MinecraftClient} is available for the specified Minecraft-version.
     * @author Griefed
     * @param minecraftVersion {@link String} The Minecraft-version.
     * @return {@link Boolean} <code>true</code> if a {@link MinecraftClient} is available.
     */
    public boolean checkMinecraftVersion(String minecraftVersion) {
        return getClient(minecraftVersion).isPresent();
    }

    /**
     * Get the list of available Minecraft version of the {@link Type#RELEASE} in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-list of all available Minecraft {@link Type#RELEASE} versions in {@link Type#DESCENDING} order.
     */
    public List<String> releaseVersionsDescending() {
        List<String> list = new ArrayList<>();
        releasesDescending().forEach(client -> list.add(client.version()));
        return list;
    }

    /**
     * Get the list of available Minecraft version of the {@link Type#RELEASE} in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-list of all available Minecraft {@link Type#RELEASE} versions in {@link Type#ASCENDING} order.
     */
    public List<String> releaseVersionsAscending() {
        List<String> list = new ArrayList<>();
        releasesDescending().forEach(client -> list.add(client.version()));
        return Lists.reverse(list);
    }

    /**
     * Get the array of available Minecraft version of the {@link Type#RELEASE} in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-array of all available Minecraft {@link Type#RELEASE} versions in {@link Type#DESCENDING} order.
     */
    public String[] releaseVersionsArrayDescending() {
        return releaseVersionsDescending().toArray(new String[0]);
    }

    /**
     * Get the array of available Minecraft version of the {@link Type#RELEASE} in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-array of all available Minecraft {@link Type#RELEASE} versions in {@link Type#ASCENDING} order.
     */
    public String[] releaseVersionsArrayAscending() {
        return releaseVersionsAscending().toArray(new String[0]);
    }

    /**
     * Get the list of available Minecraft version of the {@link Type#SNAPSHOT} in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-list of all available Minecraft {@link Type#SNAPSHOT} versions in {@link Type#DESCENDING} order.
     */
    public List<String> snapshotsVersionsDescending() {
        List<String> list = new ArrayList<>();
        snapshotsDescending().forEach(client -> list.add(client.version()));
        return list;
    }

    /**
     * Get the list of available Minecraft version of the {@link Type#SNAPSHOT} in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-list of all available Minecraft {@link Type#SNAPSHOT} versions in {@link Type#ASCENDING} order.
     */
    public List<String> snapshotVersionsAscending() {
        List<String> list = new ArrayList<>();
        snapshotsDescending().forEach(client -> list.add(client.version()));
        return Lists.reverse(list);
    }

    /**
     * Get the array of available Minecraft version of the {@link Type#SNAPSHOT} in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-array of all available Minecraft {@link Type#SNAPSHOT} versions in {@link Type#DESCENDING} order.
     */
    public String[] snapshotVersionsArrayDescending() {
        return snapshotsVersionsDescending().toArray(new String[0]);
    }

    /**
     * Get the array of available Minecraft version of the {@link Type#SNAPSHOT} in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-array of all available Minecraft {@link Type#SNAPSHOT} versions in {@link Type#ASCENDING} order.
     */
    public String[] snapshotVersionsArrayAscending() {
        return snapshotVersionsAscending().toArray(new String[0]);
    }

/*
 * #==============================================================================================================#
 * #..............................................................................................................#
 * #...................................................CLIENTS....................................................#
 * #..............................................................................................................#
 * #==============================================================================================================#
 */
    /**
     * Get the latest Minecraft release as a {@link MinecraftClient}.
     * @author Griefed
     * @return {@link MinecraftClient}
     */
    public MinecraftClient latestRelease() {
        return MINECRAFT_CLIENT_META.latestRelease();
    }

    /**
     * Get the latest Minecraft snapshot as a {@link MinecraftClient}.
     * @author Griefed
     * @return {@link MinecraftClient}
     */
    public MinecraftClient latestSnapshot() {
        return MINECRAFT_CLIENT_META.latestSnapshot();
    }

    /**
     * Get a specific Minecraft client as a {@link MinecraftClient} wrapped in an {@link Optional}.
     * @author Griefed
     * @param minecraftVersion {@link String} Minecraft version.
     * @return {@link MinecraftClient} wrapped in an {@link Optional}
     */
    public Optional<MinecraftClient> getClient(String minecraftVersion) {
        return Optional.ofNullable(MINECRAFT_CLIENT_META.meta().get(minecraftVersion));
    }

    /**
     * Get a list of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in ascending order.
     * @author Griefed
     * @return {@link MinecraftClient}-list
     */
    public List<MinecraftClient> releasesDescending() {
        return MINECRAFT_CLIENT_META.releases();
    }

    /**
     * Get a list of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in descending order.
     * @author Griefed
     * @return {@link MinecraftClient}-list
     */
    public List<MinecraftClient> releasesAscending() {
        return Lists.reverse(MINECRAFT_CLIENT_META.releases());
    }

    /**
     * Get an array of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in ascending order.
     * @author Griefed
     * @return {@link MinecraftClient}-array
     */
    public MinecraftClient[] releasesArrayDescending() {
        return releasesDescending().toArray(new MinecraftClient[0]);
    }

    /**
     * Get an array of all available {@link MinecraftClient} of the {@link Type#RELEASE}, in descending order.
     * @author Griefed
     * @return {@link MinecraftClient}-array
     */
    public MinecraftClient[] releasesArrayAscending() {
        return releasesAscending().toArray(new MinecraftClient[0]);
    }

    /**
     * Get a list of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in ascending order.
     * @author Griefed
     * @return {@link MinecraftClient}-list
     */
    public List<MinecraftClient> snapshotsDescending() {
        return MINECRAFT_CLIENT_META.snapshots();
    }

    /**
     * Get a list of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in descending order.
     * @author Griefed
     * @return {@link MinecraftClient}-list
     */
    public List<MinecraftClient> snapshotsAscending() {
        return Lists.reverse(MINECRAFT_CLIENT_META.snapshots());
    }

    /**
     * Get an array of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in ascending order.
     * @author Griefed
     * @return {@link MinecraftClient}-array
     */
    public MinecraftClient[] snapshotsArrayDescending() {
        return snapshotsDescending().toArray(new MinecraftClient[0]);
    }

    /**
     * Get an array of all available {@link MinecraftClient} of the {@link Type#SNAPSHOT}, in descending order.
     * @author Griefed
     * @return {@link MinecraftClient}-array
     */
    public MinecraftClient[] snapshotsArrayAscending() {
        return snapshotsAscending().toArray(new MinecraftClient[0]);
    }

    /*
     * #==============================================================================================================#
     * #..............................................................................................................#
     * #...................................................SERVERS....................................................#
     * #..............................................................................................................#
     * #==============================================================================================================#
     */
    /**
     * Get a specific {@link MinecraftServer} for the specified Minecraft-version, wrapped in an {@link Optional}.
     * @author Griefed
     * @param minecraftVersion {@link String} The Minecraft-version.
     * @return {@link MinecraftServer} wrapped in an {@link Optional}
     */
    public Optional<MinecraftServer> getServer(String minecraftVersion) {
        return Optional.ofNullable(MINECRAFT_SERVER_META.meta().get(minecraftVersion));
    }

    /**
     * Check whether a {@link MinecraftServer} is available for the specified Minecraft-version.
     * @author Griefed
     * @param minecraftVersion {@link String} The Minecraft-version.
     * @return {@link Boolean} <code>true</code> if a {@link MinecraftServer} is available.
     */
    public boolean checkServerAvailability(String minecraftVersion) {
        return getServer(minecraftVersion).isPresent();
    }

    /**
     * Get the latest {@link MinecraftServer} of the {@link Type#RELEASE}, wrapped in an {@link Optional}.
     * @author Griefed
     * @return {@link MinecraftServer} wrapped in an {@link Optional}
     */
    public Optional<MinecraftServer> latestReleaseServer() {
        return MINECRAFT_CLIENT_META.latestRelease().server();
    }

    /**
     * Get the latest {@link MinecraftServer} of the {@link Type#SNAPSHOT}, wrapped in an {@link Optional}.
     * @author Griefed
     * @return {@link MinecraftServer} wrapped in an {@link Optional}
     */
    public Optional<MinecraftServer> latestSnapshotServer() {
        return MINECRAFT_CLIENT_META.latestSnapshot().server();
    }

    /**
     * Get a list of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link MinecraftServer}-list
     */
    public List<MinecraftServer> releasesServersDescending() {
        return MINECRAFT_SERVER_META.releases();
    }

    /**
     * Get a list of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link MinecraftServer}-list
     */
    public List<MinecraftServer> releasesServersAscending() {return Lists.reverse(MINECRAFT_SERVER_META.releases());
    }

    /**
     * Get an array of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link MinecraftServer}-array
     */
    public MinecraftServer[] releasesServersArrayDescending() {
        return releasesServersDescending().toArray(new MinecraftServer[0]);
    }

    /**
     * Get an array of all available {@link MinecraftServer} of the {@link Type#RELEASE}, in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link MinecraftServer}-array
     */
    public MinecraftServer[] releasesServersArrayAscending() {return releasesServersAscending().toArray(new MinecraftServer[0]);
    }

    /**
     * Get a list of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link MinecraftServer}-list
     */
    public List<MinecraftServer> snapshotsServersDescending() {
        return MINECRAFT_SERVER_META.snapshots();
    }

    /**
     * Get a list of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in descending order.
     * @author Griefed
     * @return {@link MinecraftServer}-list
     */
    public List<MinecraftServer> snapshotsServersAscending() {return Lists.reverse(MINECRAFT_SERVER_META.snapshots());
    }

    /**
     * Get an array of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in ascending order.
     * @author Griefed
     * @return {@link MinecraftServer}-array
     */
    public MinecraftServer[] snapshotsServersArrayDescending() {
        return snapshotsServersDescending().toArray(new MinecraftServer[0]);
    }

    /**
     * Get an array of all available {@link MinecraftServer} of the {@link Type#SNAPSHOT}, in descending order.
     * @author Griefed
     * @return {@link MinecraftServer}-array
     */
    public MinecraftServer[] snapshotsServersArrayAscending() {return snapshotsServersAscending().toArray(new MinecraftServer[0]);
    }
}
