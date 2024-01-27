/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.api.versionmeta.forge

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.createDirectories
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Forge meta containing information about available Forge releases.
 *
 * @param forgeManifest The manifest from which to acquire version information.
 * @param utilities     Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
@Suppress("unused")
actual class ForgeMeta actual constructor(
    private val forgeManifest: File,
    private val utilities: Utilities,
    installerCacheDirectory: File
) {
    private var forgeLoader: ForgeLoader? = null
    private val installerDirectory: File = File(installerCacheDirectory, "forge")

    init {
        installerDirectory.createDirectories(create = true, directory = true)
    }

    /**
     * Update this instances [ForgeLoader] with new information. Usually called after the Forge
     * manifest has been refreshed.
     *
     * @param injectedMinecraftMeta Minecraft manifest file.
     * @throws IOException if the manifest could not be parsed into a
     * [com.fasterxml.jackson.databind.JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun initialize(injectedMinecraftMeta: MinecraftMeta) {
        if (forgeLoader == null) {
            forgeLoader = ForgeLoader(forgeManifest, utilities, injectedMinecraftMeta)
        }
    }

    /**
     * Update this instances [ForgeLoader] with new information. Usually called after the Forge
     * manifest has been refreshed.
     *
     * @throws IOException if the manifest could not be parsed into a
     * [com.fasterxml.jackson.databind.JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() = forgeLoader!!.update()

    /**
     * Check whether the given Minecraft and Forge versions are valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @param forgeVersion     Forge version.
     * @return `true` if the given Minecraft and Forge versions are valid/supported/available.
     * @author Griefed
     */
    actual fun isForgeAndMinecraftCombinationValid(minecraftVersion: String, forgeVersion: String) =
        isMinecraftVersionSupported(minecraftVersion) && isForgeVersionValid(forgeVersion)

    /**
     * Check whether a given Minecraft version is valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @return `true` if the given Minecraft version is valid/supported/available.
     * @author Griefed
     */
    actual fun isMinecraftVersionSupported(minecraftVersion: String) =
        Optional.ofNullable(forgeLoader!!.versionMeta[minecraftVersion]).isPresent

    /**
     * Check whether a given Forge version is valid/supported/available.
     *
     * @param forgeVersion Forge version.
     * @return `true` if the given Forge version is valid/supported/available.
     * @author Griefed
     */
    actual fun isForgeVersionValid(forgeVersion: String) =
        Optional.ofNullable(forgeLoader!!.forgeToMinecraftMeta[forgeVersion]).isPresent

    /**
     * Check whether Forge is available for a given Forge- and Minecraft version.
     *
     * @param minecraftVersion Minecraft version.
     * @param forgeVersion     Forge version.
     * @return `true` if Forge is available for the given Forge- and Minecraft version.
     * @author Griefed
     */
    actual fun isForgeInstanceAvailable(minecraftVersion: String, forgeVersion: String) =
        getForgeInstance(minecraftVersion, forgeVersion).isPresent

    /**
     * Get a [ForgeInstance] for a given Minecraft and Forge version, wrapped in an
     * [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @param forgeVersion     Forge version.
     * @return Forge instance for the given Minecraft and Forge version, wrapped in an
     * [Optional]
     * @author Griefed
     */
    fun getForgeInstance(minecraftVersion: String, forgeVersion: String) =
        Optional.ofNullable(forgeLoader!!.instanceMeta["$minecraftVersion-$forgeVersion"])

    /**
     * Check whether Forge is available for a given Forge version
     *
     * @param forgeVersion Forge version.
     * @return `true` if Forge is available for the given Forge version.
     * @author Griefed
     */
    actual fun isForgeInstanceAvailable(forgeVersion: String) = getForgeInstance(forgeVersion).isPresent

    /**
     * Get a [ForgeInstance] for a given Forge version, wrapped in an [Optional].
     *
     * @param forgeVersion Forge version.
     * @return Forge instance for the given Forge version, wrapped in an [Optional]
     * @author Griefed
     */
    fun getForgeInstance(forgeVersion: String) =
        if (!isForgeVersionValid(forgeVersion)) {
            Optional.empty()
        } else if (minecraftVersion(forgeVersion).isPresent) {
            val mcVersion = minecraftVersion(forgeVersion).get()
            getForgeInstance(mcVersion, forgeVersion)
        } else {
            Optional.empty()
        }

    /**
     * Get a list of all available [ForgeInstance] for a given Minecraft version, wrapped in an
     * [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Forge instance-list for the given Minecraft version.
     * @author Griefed
     */
    fun getForgeInstances(minecraftVersion: String): Optional<List<ForgeInstance>> {
        val list: MutableList<ForgeInstance> = ArrayList(100)
        val instance = forgeLoader!!.versionMeta[minecraftVersion]
        return if (Optional.ofNullable(instance).isPresent) {
            for (version in instance!!) {
                val specificInstance = forgeLoader!!.instanceMeta["$minecraftVersion-$version"]
                if (specificInstance != null) {
                    list.add(specificInstance)
                }
            }
            Optional.of(list)
        } else {
            Optional.empty()
        }
    }

    /**
     * Latest Forge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Latest Forge version for the given Minecraft version, wrapped in an [Optional]
     * @author Griefed
     */
    actual fun newestForgeVersion(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else if (supportedForgeVersionsAscending(minecraftVersion).isPresent) {
            val latestMCVersion = supportedForgeVersionsAscending(minecraftVersion).get().size - 1
            val supported = supportedForgeVersionsAscending(minecraftVersion).get()
            Optional.of(supported[latestMCVersion])
        } else {
            Optional.empty()
        }

    /**
     * Oldest Forge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Oldest Forge version for the given Minecraft version, wrapped in [Optional]
     * @author Griefed
     */
    actual fun oldestForgeVersion(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else if (supportedForgeVersionsAscending(minecraftVersion).isPresent) {
            val supported = supportedForgeVersionsAscending(minecraftVersion).get()
            Optional.ofNullable(supported[0])
        } else {
            Optional.empty()
        }

    /**
     * Get the list of available Forge versions, in ascending order.
     *
     * @return List of available Forge versions.
     * @author Griefed
     */
    actual fun forgeVersionsAscending() = forgeLoader!!.forgeVersions

    /**
     * Get the list of available Forge versions, in descending order.
     *
     * @return List of available Forge versions.
     * @author Griefed
     */
    actual fun forgeVersionsDescending() = forgeVersionsAscending().reversed()

    /**
     * Get the array of available Forge versions, in ascending order.
     *
     * @return Array of available Forge versions.
     * @author Griefed
     */
    actual fun forgeVersionsAscendingArray() = forgeVersionsAscending().toTypedArray()

    /**
     * Get the array of available Forge versions, in descending order.
     *
     * @return Array of available Forge versions.
     * @author Griefed
     */
    actual fun forgeVersionsDescendingArray() = forgeVersionsDescending().toTypedArray()

    /**
     * Get a list of available Forge version for a given Minecraft version in ascending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available Forge versions for the given Minecraft version in ascending order.
     * @author Griefed
     */
    actual fun supportedForgeVersionsAscending(minecraftVersion: String) =
        Optional.ofNullable(forgeLoader?.versionMeta?.get(minecraftVersion))

    /**
     * Get a list of available Forge version for a given Minecraft version in descending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available Forge versions for the given Minecraft version in descending order.
     * @author Griefed
     */
    actual fun supportedForgeVersionsDescending(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else {
            val supported = supportedForgeVersionsAscending(minecraftVersion).get()
            Optional.ofNullable(supported.reversed())
        }


    /**
     * Get an array of available Forge version for a given Minecraft version, in ascending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available Forge versions for the given Minecraft version, in ascending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    actual fun supportedForgeVersionsAscendingArray(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else {
            val supported = supportedForgeVersionsAscending(minecraftVersion).get()
            Optional.of(
                supported.toTypedArray()
            )
        }

    /**
     * Get an array of available Forge version for a given Minecraft version, in descending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available Forge versions for the given Minecraft version, in descending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    actual fun supportedForgeVersionsDescendingArray(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else {
            val supported = supportedForgeVersionsDescending(minecraftVersion).get()
            Optional.of(supported.toTypedArray())
        }

    /**
     * Get the Minecraft version for a given Forge version, wrapped in an [Optional].
     *
     * @param forgeVersion Forge version.
     * @return Minecraft version for the given Forge version, wrapped in an [Optional].
     * @author Griefed
     */
    actual fun minecraftVersion(forgeVersion: String) =
        Optional.ofNullable(forgeLoader!!.forgeToMinecraftMeta[forgeVersion])

    /**
     * Get the list of Forge supported Minecraft versions, in ascending order.
     *
     * @return List of Forge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsAscending() = forgeLoader!!.minecraftVersions

    /**
     * Get the list of Forge supported Minecraft versions, in descending order.
     *
     * @return List of Forge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsDescending() = supportedMinecraftVersionsAscending().reversed()

    /**
     * Get the array of Forge supported Minecraft versions, in ascending order.
     *
     * @return Array of Forge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsAscendingArray() = supportedMinecraftVersionsAscending().toTypedArray()

    /**
     * Get the array of Forge supported Minecraft versions, in descending order.
     *
     * @return Array of Forge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsDescendingArray() = supportedMinecraftVersionsDescending().toTypedArray()

    /**
     * Get the Forge server installer URL for a given Forge version, wrapped in an [Optional].
     *
     * @param forgeVersion Forge version.
     * @return Forge server installer URL for the given Forge version, wrapped in an [Optional].
     * @author Griefed
     */
    actual fun installerUrl(forgeVersion: String) =
        if (isForgeVersionValid(forgeVersion) && getForgeInstance(forgeVersion).isPresent) {
            val instance = getForgeInstance(forgeVersion).get()
            Optional.of(instance.installerUrl)
        } else {
            Optional.empty()
        }

    fun getForgeMeta(): HashMap<String,List<String>> {
        return forgeLoader!!.versionMeta
    }

    /**
     * Installer file for the specified [forgeVersion] and [minecraftVersion] version, wrapped in an [Optional], so you
     * can check whether it is available first.
     * @author Griefed
     */
    actual fun installerFor(forgeVersion: String, minecraftVersion: String) =
        if (isForgeInstanceAvailable(minecraftVersion, forgeVersion)) {
            val destination = File(installerDirectory, "$forgeVersion-$minecraftVersion.jar")
            if (!destination.isFile) {
                val url = installerUrl(forgeVersion).get()
                val downloaded = utilities.webUtilities.downloadFile(destination, url)
                if (downloaded) {
                    Optional.of(destination)
                } else {
                    Optional.empty()
                }
            } else {
                Optional.of(destination)
            }
        } else {
            Optional.empty()
        }
}