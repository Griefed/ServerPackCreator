/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.api.modscanning

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.toml.TomlParser
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.jar.JarFile

/**
 * `mods.toml`-based scanning of Fabric-Minecraft mods for Minecraft 1.16.5 and newer.
 *
 * @param tomlParser To parse .toml-files.
 * @Griefed
 */
actual class TomlScanner constructor(private val tomlParser: TomlParser) :
    Scanner<TreeSet<File>, Collection<File>> {
    private val log = cachedLoggerOf(this.javaClass)

    /**
     * Scan the `mods.toml`-files in mod JAR-files of a given directory for their sideness.
     *
     * If `mods` specifies `side=BOTH|SERVER`, it is added.
     *
     * If `dependencies.modId` for `Forge|Minecraft` specifies `side=BOTH|SERVER `, it is added.
     *
     * Any modId of a dependency specifying `side=BOTH|SERVER` is added.
     *
     * If no sideness can be found for a given mod, it is added to prevent false positives.
     * @param jarFiles A list of files in which to check the `mods.toml`-files.
     * @return Mods not to include in server pack based on mods.toml-configuration.
     * @author Griefed
     */
    override fun scan(jarFiles: Collection<File>): TreeSet<File> {
        val serverMods = TreeSet<File>()
        val idsRequiredOnServer = TreeSet<String>()
        var config: CommentedConfig
        for (modJar in jarFiles) {
            try {
                config = getConfig(modJar)

                // get all [[dependencies.n]] which are not minecraft|forge, but required by the mod
                idsRequiredOnServer.addAll(getModDependencyIdsRequiredOnServer(config))

                // get all mods required on the server
                idsRequiredOnServer.addAll(getModIdsRequiredOnServer(config))
            } catch (e: Exception) {
                log.debug("Could not fully scan ${modJar.name}. ${e.message}")
                serverMods.add(modJar)
            }
        }
        for (modJar in jarFiles) {
            try {
                config = getConfig(modJar)
                val idsInMod = getModIdsInJar(config)
                for (id in idsInMod) {
                    if (idsRequiredOnServer.contains(id)) {
                        serverMods.add(modJar)
                    }
                }
            } catch (e: Exception) {
                log.debug("Could not fully scan ${modJar.name}. ${e.message}")
                serverMods.add(modJar)
            }
        }
        val excluded = TreeSet(jarFiles)
        excluded.removeAll(serverMods)
        return excluded
    }

    /**
     * Get all ids of mods required for running the server.
     *
     * @param config Base-config of the toml of the mod which contains all information.
     * @return Set of ids of mods required.
     * @throws ScanningException if the mod specifies no mods.
     */
    @Throws(ScanningException::class)
    private fun getModIdsRequiredOnServer(config: CommentedConfig): TreeSet<String> {
        val configs = ArrayList<Map<String, Any>>(100)
        val ids = TreeSet<String>()
        if (config.valueMap()["mods"] == null) {
            throw ScanningException("No mods specified.")
        } else {
            @Suppress("UNCHECKED_CAST")
            for (commentedConfig in config.valueMap()["mods"] as ArrayList<CommentedConfig>) {
                configs.add(commentedConfig.valueMap())
            }
        }
        val dependencies: Map<String, ArrayList<CommentedConfig>> = getMapOfDependencyLists(config)
        var containedForgeOrMinecraft = false
        for (mod in configs) {
            val modId = mod["modId"].toString()
            if (dependencies.containsKey(modId)) {
                for (dependency in dependencies[modId]!!) {
                    try {
                        if (getModId(dependency).matches(forgeMinecraft)) {
                            containedForgeOrMinecraft = true
                            try {
                                if (getSide(dependency).matches(bothServer)) {
                                    ids.add(modId)
                                }
                            } catch (ex: NullPointerException) {
                                // no side specified....assuming both|server
                                ids.add(modId)
                            }
                        }
                    } catch (e: NullPointerException) {
                        // no modId specified in dependency...assuming forge|minecraft and both|server
                        containedForgeOrMinecraft = true
                        ids.add(modId)
                    }
                }
            } else {
                // contains no self referencing dependency...
                ids.add(modId)
            }
            if (!containedForgeOrMinecraft) {
                ids.add(modId)
            }
        }
        return ids
    }

    /**
     * Acquire a list of ids of dependencies required by the passed mod in order to run on a modded
     * server. Only if all dependencies in this mod specify `CLIENT` for either `forge `
     * or `minecraft` is a dependency not added to the list of required dependencies. Otherwise,
     * all modIds mentioned in the dependencies of this mod, which are neither `forge` nor
     * `minecraft` get added to the list.
     *
     * @param config Base-config of the toml of the mod which contains all information.
     * @return Set of ids of mods required as dependencies.
     * @throws ScanningException if the mod has invalid dependency declarations or specifies no mods.
     */
    @Throws(ScanningException::class)
    private fun getModDependencyIdsRequiredOnServer(config: CommentedConfig): TreeSet<String> {
        val ids = TreeSet<String>()
        val dependencies: Map<String, ArrayList<CommentedConfig>> = getMapOfDependencyLists(config)
        val idsInMod = getModIdsInJar(config)
        try {
            var confidentOnClientSide = true
            for (modId in idsInMod) {
                if (dependencies.containsKey(modId)) {
                    for (dependency in dependencies[modId]!!) {
                        if (getModId(dependency).matches(forgeMinecraft)
                            && getSide(dependency).matches(bothServer)
                        ) {
                            confidentOnClientSide = false
                        }
                    }
                } else {
                    confidentOnClientSide = false
                    break
                }
            }
            if (confidentOnClientSide) {
                return ids
            }
        } catch (ignored: NullPointerException) {
        }
        for ((key, value) in dependencies) {
            for (commentedConfig in value) {
                try {

                    // dependency forge|minecraft?
                    if (!getModId(commentedConfig).matches(forgeMinecraft)) {
                        try {

                            // dependency required on the server?
                            if (getSide(commentedConfig).matches(bothServer)) {
                                ids.add(getModId(commentedConfig))
                            }
                        } catch (ex: NullPointerException) {
                            // dependency specifies no side
                            ids.add(getModId(commentedConfig))
                        }
                    }
                } catch (e: NullPointerException) {

                    // dependency specifies no modId, so use parent.
                    if (!key.lowercase(Locale.getDefault()).matches(forgeMinecraft)) {
                        ids.add(key)
                    }
                }
            }
        }
        return ids
    }

    /**
     * Acquire a set of ids of mods required for running the server.
     *
     * @param config Base-config of the toml of the mod which contains all information.
     * @return Set of ids of mods required.
     * @throws ScanningException if the mod specifies no...well...mods.
     */
    @Throws(ScanningException::class)
    private fun getModIdsInJar(config: CommentedConfig): TreeSet<String> {
        val ids = TreeSet<String>()
        if (config.valueMap()["mods"] == null) {
            throw ScanningException("No mods specified.")
        } else {
            @Suppress("UNCHECKED_CAST")
            for (commentedConfig in config.valueMap()["mods"] as ArrayList<CommentedConfig>) {
                ids.add(getModId(commentedConfig))
            }
        }
        return ids
    }

    /**
     * Acquire the base toml-config of a mod.
     *
     * @param file The file from which to acquire the toml config.
     * @return Config read from the toml in the mod.
     * @throws IOException if the mods.toml file could not be read/found.
     */
    @Throws(IOException::class)
    private fun getConfig(file: File): CommentedConfig {
        val jarFile = JarFile(file)
        val tomlStream: InputStream = jarFile.getInputStream(jarFile.getJarEntry("META-INF/mods.toml"))
        val config: CommentedConfig = tomlParser.parse(tomlStream)
        jarFile.close()
        tomlStream.close()
        return config
    }

    /**
     * Acquire a map of all dependencies specified by a mod.
     *
     * @param config Base-config of the toml of the mod which contains all * information.
     * @return Map of dependencies for the passed mod config, String keys are mapped to ArrayLists of
     * CommentedConfigs.
     * @throws ScanningException if the mod declares no dependencies.
     */
    @Throws(ScanningException::class)
    private fun getMapOfDependencyLists(config: CommentedConfig): Map<String, ArrayList<CommentedConfig>> {
        if (config.valueMap()["dependencies"] == null) {
            throw ScanningException("No dependencies specified.")
        }
        val dependencies: MutableMap<String, ArrayList<CommentedConfig>> =
            HashMap<String, ArrayList<CommentedConfig>>(100)
        @Suppress("UNCHECKED_CAST")
        if (config.valueMap()["dependencies"] is ArrayList<*>) {
            val mods = config.valueMap()["mods"] as ArrayList<CommentedConfig>
            val id = getModId(mods[0])
            val entry = config.valueMap()["dependencies"] as ArrayList<CommentedConfig>
            dependencies[id] = entry
        } else {
            for ((key, value) in (config.valueMap()["dependencies"] as CommentedConfig).valueMap().entries) {
                dependencies[key.lowercase()] = value as ArrayList<CommentedConfig>
            }
        }
        return dependencies
    }

    /**
     * Acquire the modId from the passed config.
     *
     * @param config Mod- or dependency-config which contains the modId.
     * @return `modId` from the passed config, in lower-case letters.
     */
    private fun getModId(config: CommentedConfig) = config.valueMap()["modId"].toString().lowercase(Locale.getDefault())

    /**
     * Acquire the side of the config of the passed dependency.
     *
     * @param config Mod- or dependency-config which contains the modId.
     * @return `side` from the passed config, in upper-case letters.
     */
    private fun getSide(config: CommentedConfig) = config.valueMap()["side"].toString().uppercase(Locale.getDefault())
}