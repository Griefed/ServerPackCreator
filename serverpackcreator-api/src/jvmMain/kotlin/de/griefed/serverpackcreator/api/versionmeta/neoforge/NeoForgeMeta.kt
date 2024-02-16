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
package de.griefed.serverpackcreator.api.versionmeta.neoforge

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.createDirectories
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import java.io.File
import java.io.IOException
import java.util.*

/**
 * NeoForge meta containing information about available NeoForge releases.
 *
 * @param neoForgeManifest The manifest from which to acquire version information.
 * @param utilities     Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
@Suppress("unused")
actual class NeoForgeMeta actual constructor(
    private val neoForgeManifest: File,
    private val utilities: Utilities,
    installerCacheDirectory: File
) {
    private var neoForgeLoader: NeoForgeLoader? = null
    private val installerDirectory: File = File(installerCacheDirectory, "neoforge")

    init {
        installerDirectory.createDirectories(create = true, directory = true)
    }

    /**
     * Update this instances [NeoForgeLoader] with new information. Usually called after the NeoForge
     * manifest has been refreshed.
     *
     * @param injectedMinecraftMeta Minecraft manifest file.
     * @throws IOException if the manifest could not be parsed into a
     * [com.fasterxml.jackson.databind.JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun initialize(injectedMinecraftMeta: MinecraftMeta) {
        if (neoForgeLoader == null) {
            neoForgeLoader = NeoForgeLoader(neoForgeManifest, utilities, injectedMinecraftMeta)
        }
    }

    /**
     * Update this instances [NeoForgeLoader] with new information. Usually called after the NeoForge
     * manifest has been refreshed.
     *
     * @throws IOException if the manifest could not be parsed into a
     * [com.fasterxml.jackson.databind.JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() = neoForgeLoader!!.update()

    /**
     * Check whether the given Minecraft and NeoForge versions are valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @param neoForgeVersion     NeoForge version.
     * @return `true` if the given Minecraft and NeoForge versions are valid/supported/available.
     * @author Griefed
     */
    actual fun isNeoForgeAndMinecraftCombinationValid(minecraftVersion: String, neoForgeVersion: String) =
        isMinecraftVersionSupported(minecraftVersion) && isNeoForgeVersionValid(neoForgeVersion)

    /**
     * Check whether a given Minecraft version is valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @return `true` if the given Minecraft version is valid/supported/available.
     * @author Griefed
     */
    actual fun isMinecraftVersionSupported(minecraftVersion: String) =
        Optional.ofNullable(neoForgeLoader!!.versionMeta[minecraftVersion]).isPresent

    /**
     * Check whether a given NeoForge version is valid/supported/available.
     *
     * @param neoForgeVersion NeoForge version.
     * @return `true` if the given NeoForge version is valid/supported/available.
     * @author Griefed
     */
    actual fun isNeoForgeVersionValid(neoForgeVersion: String) =
        Optional.ofNullable(neoForgeLoader!!.neoForgeToMinecraftMeta[neoForgeVersion]).isPresent

    /**
     * Check whether NeoForge is available for a given NeoForge- and Minecraft version.
     *
     * @param minecraftVersion Minecraft version.
     * @param neoForgeVersion     NeoForge version.
     * @return `true` if NeoForge is available for the given NeoForge- and Minecraft version.
     * @author Griefed
     */
    actual fun isNeoForgeInstanceAvailable(minecraftVersion: String, neoForgeVersion: String) =
        getNeoForgeInstance(minecraftVersion, neoForgeVersion).isPresent

    /**
     * Get a [NeoForgeInstance] for a given Minecraft and NeoForge version, wrapped in an
     * [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @param neoForgeVersion     NeoForge version.
     * @return NeoForge instance for the given Minecraft and NeoForge version, wrapped in an
     * [Optional]
     * @author Griefed
     */
    fun getNeoForgeInstance(minecraftVersion: String, neoForgeVersion: String) =
        Optional.ofNullable(neoForgeLoader!!.instanceMeta["$minecraftVersion-$neoForgeVersion"])

    /**
     * Check whether NeoForge is available for a given NeoForge version
     *
     * @param neoForgeVersion NeoForge version.
     * @return `true` if NeoForge is available for the given NeoForge version.
     * @author Griefed
     */
    actual fun isNeoForgeInstanceAvailable(neoForgeVersion: String) = getNeoForgeInstance(neoForgeVersion).isPresent

    /**
     * Get a [NeoForgeInstance] for a given NeoForge version, wrapped in an [Optional].
     *
     * @param neoForgeVersion NeoForge version.
     * @return NeoForge instance for the given NeoForge version, wrapped in an [Optional]
     * @author Griefed
     */
    fun getNeoForgeInstance(neoForgeVersion: String) =
        if (!isNeoForgeVersionValid(neoForgeVersion)) {
            Optional.empty()
        } else if (minecraftVersion(neoForgeVersion).isPresent) {
            val mcVersion = minecraftVersion(neoForgeVersion).get()
            getNeoForgeInstance(mcVersion, neoForgeVersion)
        } else {
            Optional.empty()
        }

    /**
     * Get a list of all available [NeoForgeInstance] for a given Minecraft version, wrapped in an
     * [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return NeoForge instance-list for the given Minecraft version.
     * @author Griefed
     */
    fun getNeoForgeInstances(minecraftVersion: String): Optional<List<NeoForgeInstance>> {
        val list: MutableList<NeoForgeInstance> = ArrayList(100)
        val instance = neoForgeLoader!!.versionMeta[minecraftVersion]
        return if (Optional.ofNullable(instance).isPresent) {
            for (version in instance!!) {
                val specificInstance = neoForgeLoader!!.instanceMeta["$minecraftVersion-$version"]
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
     * Latest NeoForge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Latest NeoForge version for the given Minecraft version, wrapped in an [Optional]
     * @author Griefed
     */
    actual fun newestNeoForgeVersion(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else if (supportedNeoForgeVersionsAscending(minecraftVersion).isPresent) {
            val latestMCVersion = supportedNeoForgeVersionsAscending(minecraftVersion).get().size - 1
            val supported = supportedNeoForgeVersionsAscending(minecraftVersion).get()
            Optional.of(supported[latestMCVersion])
        } else {
            Optional.empty()
        }

    /**
     * Oldest NeoForge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Oldest NeoForge version for the given Minecraft version, wrapped in [Optional]
     * @author Griefed
     */
    actual fun oldestNeoForgeVersion(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else if (supportedNeoForgeVersionsAscending(minecraftVersion).isPresent) {
            val supported = supportedNeoForgeVersionsAscending(minecraftVersion).get()
            Optional.ofNullable(supported[0])
        } else {
            Optional.empty()
        }

    /**
     * Get the list of available NeoForge versions, in ascending order.
     *
     * @return List of available NeoForge versions.
     * @author Griefed
     */
    actual fun neoForgeVersionsAscending() = neoForgeLoader!!.neoForgeVersions

    /**
     * Get the list of available NeoForge versions, in descending order.
     *
     * @return List of available NeoForge versions.
     * @author Griefed
     */
    actual fun neoForgeVersionsDescending() = neoForgeVersionsAscending().reversed()

    /**
     * Get the array of available NeoForge versions, in ascending order.
     *
     * @return Array of available NeoForge versions.
     * @author Griefed
     */
    actual fun neoForgeVersionsAscendingArray() = neoForgeVersionsAscending().toTypedArray()

    /**
     * Get the array of available NeoForge versions, in descending order.
     *
     * @return Array of available NeoForge versions.
     * @author Griefed
     */
    actual fun neoForgeVersionsDescendingArray() = neoForgeVersionsDescending().toTypedArray()

    /**
     * Get a list of available NeoForge version for a given Minecraft version in ascending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available NeoForge versions for the given Minecraft version in ascending order.
     * @author Griefed
     */
    actual fun supportedNeoForgeVersionsAscending(minecraftVersion: String) =
        Optional.ofNullable(neoForgeLoader?.versionMeta?.get(minecraftVersion))

    /**
     * Get a list of available NeoForge version for a given Minecraft version in descending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available NeoForge versions for the given Minecraft version in descending order.
     * @author Griefed
     */
    actual fun supportedNeoForgeVersionsDescending(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else {
            val supported = supportedNeoForgeVersionsAscending(minecraftVersion).get()
            Optional.ofNullable(supported.reversed())
        }


    /**
     * Get an array of available NeoForge version for a given Minecraft version, in ascending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available NeoForge versions for the given Minecraft version, in ascending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    actual fun supportedNeoForgeVersionsAscendingArray(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else {
            val supported = supportedNeoForgeVersionsAscending(minecraftVersion).get()
            Optional.of(
                supported.toTypedArray()
            )
        }

    /**
     * Get an array of available NeoForge version for a given Minecraft version, in descending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available NeoForge versions for the given Minecraft version, in descending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    actual fun supportedNeoForgeVersionsDescendingArray(minecraftVersion: String) =
        if (!isMinecraftVersionSupported(minecraftVersion)) {
            Optional.empty()
        } else {
            val supported = supportedNeoForgeVersionsDescending(minecraftVersion).get()
            Optional.of(supported.toTypedArray())
        }

    /**
     * Get the Minecraft version for a given NeoForge version, wrapped in an [Optional].
     *
     * @param neoForgeVersion NeoForge version.
     * @return Minecraft version for the given Neo version, wrapped in an [Optional].
     * @author Griefed
     */
    actual fun minecraftVersion(neoForgeVersion: String) =
        Optional.ofNullable(neoForgeLoader!!.neoForgeToMinecraftMeta[neoForgeVersion])

    /**
     * Get the list of NeoForge supported Minecraft versions, in ascending order.
     *
     * @return List of NeoForge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsAscending() = neoForgeLoader!!.minecraftVersions

    /**
     * Get the list of NeoForge supported Minecraft versions, in descending order.
     *
     * @return List of NeoForge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsDescending() = supportedMinecraftVersionsAscending().reversed()

    /**
     * Get the array of NeoForge supported Minecraft versions, in ascending order.
     *
     * @return Array of NeoForge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsAscendingArray() = supportedMinecraftVersionsAscending().toTypedArray()

    /**
     * Get the array of NeoForge supported Minecraft versions, in descending order.
     *
     * @return Array of NeoForge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    actual fun supportedMinecraftVersionsDescendingArray() = supportedMinecraftVersionsDescending().toTypedArray()

    /**
     * Get the NeoForge server installer URL for a given NeoForge version, wrapped in an [Optional].
     *
     * @param neoForgeVersion NeoForge version.
     * @return NeoForge server installer URL for the given NeoForge version, wrapped in an [Optional].
     * @author Griefed
     */
    actual fun installerUrl(neoForgeVersion: String) =
        if (isNeoForgeVersionValid(neoForgeVersion) && getNeoForgeInstance(neoForgeVersion).isPresent) {
            val instance = getNeoForgeInstance(neoForgeVersion).get()
            Optional.of(instance.installerUrl)
        } else {
            Optional.empty()
        }


    /**
     * Installer file for the specified [neoForgeVersion] and [minecraftVersion] version, wrapped in an [Optional], so you
     * can check whether it is available first.
     * @author Griefed
     */
    actual fun installerFor(neoForgeVersion: String, minecraftVersion: String) =
        if (isNeoForgeInstanceAvailable(minecraftVersion, neoForgeVersion)) {
            val destination = File(installerDirectory, "$neoForgeVersion-$minecraftVersion.jar")
            if (!destination.isFile) {
                val url = installerUrl(neoForgeVersion).get()
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