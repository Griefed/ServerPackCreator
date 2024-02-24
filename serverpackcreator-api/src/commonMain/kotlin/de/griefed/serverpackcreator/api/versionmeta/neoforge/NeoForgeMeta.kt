package de.griefed.serverpackcreator.api.versionmeta.neoforge

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import java.io.File
import java.net.URL
import java.util.*

/**
 * Forge meta containing information about available Forge releases.
 *
 * @param neoForgeManifest The manifest from which to acquire version information.
 * @param utilities     Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
@Suppress("unused")
expect class NeoForgeMeta(
    oldNeoForgeManifest: File,
    newNeoForgeManifest: File,
    utilities: Utilities,
    installerCacheDirectory: File
) {
    /**
     * Check whether the given Minecraft and Forge versions are valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @param neoForgeVersion     Forge version.
     * @return `true` if the given Minecraft and Forge versions are valid/supported/available.
     * @author Griefed
     */
    fun isNeoForgeAndMinecraftCombinationValid(
        minecraftVersion: String,
        neoForgeVersion: String
    ): Boolean

    /**
     * Check whether a given Minecraft version is valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @return `true` if the given Minecraft version is valid/supported/available.
     * @author Griefed
     */
    fun isMinecraftVersionSupported(minecraftVersion: String): Boolean

    /**
     * Check whether a given Forge version is valid/supported/available.
     *
     * @param neoForgeVersion Forge version.
     * @return `true` if the given Forge version is valid/supported/available.
     * @author Griefed
     */
    fun isNeoForgeVersionValid(neoForgeVersion: String): Boolean

    /**
     * Check whether Forge is available for a given Forge- and Minecraft version.
     *
     * @param minecraftVersion Minecraft version.
     * @param neoForgeVersion     Forge version.
     * @return `true` if Forge is available for the given Forge- and Minecraft version.
     * @author Griefed
     */
    fun isNeoForgeInstanceAvailable(minecraftVersion: String, neoForgeVersion: String): Boolean

    /**
     * Check whether Forge is available for a given Forge version
     *
     * @param neoForgeVersion Forge version.
     * @return `true` if Forge is available for the given Forge version.
     * @author Griefed
     */
    fun isNeoForgeInstanceAvailable(neoForgeVersion: String): Boolean

    /**
     * Latest Forge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Latest Forge version for the given Minecraft version, wrapped in an [Optional]
     * @author Griefed
     */
    fun newestNeoForgeVersion(minecraftVersion: String): Optional<String>

    /**
     * Oldest Forge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Oldest Forge version for the given Minecraft version, wrapped in [Optional]
     * @author Griefed
     */
    fun oldestNeoForgeVersion(minecraftVersion: String): Optional<String>

    /**
     * Get the list of available Forge versions, in ascending order.
     *
     * @return List of available Forge versions.
     * @author Griefed
     */
    fun neoForgeVersionsAscending(): MutableList<String>

    /**
     * Get the list of available Forge versions, in descending order.
     *
     * @return List of available Forge versions.
     * @author Griefed
     */
    fun neoForgeVersionsDescending(): List<String>

    /**
     * Get the array of available Forge versions, in ascending order.
     *
     * @return Array of available Forge versions.
     * @author Griefed
     */
    fun neoForgeVersionsAscendingArray(): Array<String>

    /**
     * Get the array of available Forge versions, in descending order.
     *
     * @return Array of available Forge versions.
     * @author Griefed
     */
    fun neoForgeVersionsDescendingArray(): Array<String>

    /**
     * Get a list of available Forge version for a given Minecraft version in ascending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available Forge versions for the given Minecraft version in ascending order.
     * @author Griefed
     */
    fun supportedNeoForgeVersionsAscending(minecraftVersion: String): Optional<List<String>>

    /**
     * Get a list of available Forge version for a given Minecraft version in descending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available Forge versions for the given Minecraft version in descending order.
     * @author Griefed
     */
    fun supportedNeoForgeVersionsDescending(minecraftVersion: String): Optional<List<String>>

    /**
     * Get an array of available Forge version for a given Minecraft version, in ascending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available Forge versions for the given Minecraft version, in ascending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    fun supportedNeoForgeVersionsAscendingArray(minecraftVersion: String): Optional<Array<String>>

    /**
     * Get an array of available Forge version for a given Minecraft version, in descending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available Forge versions for the given Minecraft version, in descending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    fun supportedNeoForgeVersionsDescendingArray(minecraftVersion: String): Optional<Array<String>>

    /**
     * Get the Minecraft version for a given Forge version, wrapped in an [Optional].
     *
     * @param neoForgeVersion Forge version.
     * @return Minecraft version for the given Forge version, wrapped in an [Optional].
     * @author Griefed
     */
    fun minecraftVersion(neoForgeVersion: String): Optional<String>

    /**
     * Get the list of Forge supported Minecraft versions, in ascending order.
     *
     * @return List of Forge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsAscending(): MutableList<String>

    /**
     * Get the list of Forge supported Minecraft versions, in descending order.
     *
     * @return List of Forge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsDescending(): List<String>

    /**
     * Get the array of Forge supported Minecraft versions, in ascending order.
     *
     * @return Array of Forge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsAscendingArray(): Array<String>

    /**
     * Get the array of Forge supported Minecraft versions, in descending order.
     *
     * @return Array of Forge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsDescendingArray(): Array<String>

    /**
     * Get the Forge server installer URL for a given Forge version, wrapped in an [Optional].
     *
     * @param neoForgeVersion Forge version.
     * @return Forge server installer URL for the given Forge version, wrapped in an [Optional].
     * @author Griefed
     */
    fun installerUrl(neoForgeVersion: String): Optional<URL>

    /**
     * Installer file for the specified [neoForgeVersion] and [minecraftVersion] version, wrapped in an [Optional], so you
     * can check whether it is available first.
     * @author Griefed
     */
    fun installerFor(neoForgeVersion: String, minecraftVersion: String): Optional<File>


}