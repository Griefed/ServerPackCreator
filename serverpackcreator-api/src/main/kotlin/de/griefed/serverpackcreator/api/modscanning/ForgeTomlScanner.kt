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
 * `mods.toml`-based scanning of Forge-Minecraft mods for Minecraft 1.16.5 and newer.
 *
 * @param tomlParser To parse .toml-files.
 * @Griefed
 */
open class ForgeTomlScanner(private val tomlParser: TomlParser) :
    Scanner<Pair<Collection<File>, Collection<Pair<String,String>>>, Collection<File>> {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val mods = "mods"
    private val modId = "modId"
    private val dependencies = "dependencies"
    private val side = "side"
    private val both = "BOTH"
    private val type = "type"
    private val mandatory = "mandatory"
    private val requiredAsDep = "REQUIRED"

    open val modsToml = "META-INF/mods.toml"

    val neoForgeMinecraft: Regex
        get() = "^(neoforge|forge|minecraft)$".toRegex()
    val bothServer: Regex
        get() = "^(BOTH|SERVER)$".toRegex()

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
    override fun scan(jarFiles: Collection<File>): Pair<Collection<File>, Collection<Pair<String,String>>> {
        val serverMods = TreeSet<File>()
        val dependencies = ArrayList<Pair<String,String>>()
        var modConfig: CommentedConfig
        for (modJar in jarFiles) {
            try {
                modConfig = getConfig(modJar)

                // get all [[dependencies.n]] which are minecraft|forge, to determine sideness of the mod itself
                dependencies.addAll(getModDependencyIdsRequiredOnServer(modConfig, modJar.name))

                // get all mods required on the server
                dependencies.addAll(getModIdsRequiredOnServer(modConfig, modJar.name))
            } catch (e: Exception) {
                log.error("Could not scan ${modJar.name}. Consider reporting this: ${e.cause}: ${e.message}")
                serverMods.add(modJar)
            }
        }
        for (modJar in jarFiles) {
            try {
                modConfig = getConfig(modJar)
                val idsInMod = getModIdsInJar(modConfig)
                for (id in idsInMod) {
                    if (dependencies.map{ it.first }.contains(id)) {
                        serverMods.add(modJar)
                    }
                }
            } catch (e: Exception) {
                log.error("Could not scan ${modJar.name}. Consider reporting this: ${e.cause}: ${e.message}")
                serverMods.add(modJar)
            }
        }
        val excluded = TreeSet(jarFiles)
        excluded.removeAll(serverMods)
        return Pair(excluded,dependencies)
    }

    /**
     * Get all ids of mods required for running the server.
     *
     * @param modConfig Base-config of the toml of the mod which contains all information.
     * @return Set of ids of mods required.
     * @throws ScanningException if the mod specifies no mods.
     */
    @Throws(ScanningException::class)
    private fun getModIdsRequiredOnServer(modConfig: CommentedConfig, fileName: String): ArrayList<Pair<String,String>> {
        val modConfigs = ArrayList<Map<String, Any>>(100)
        val entries = ArrayList<Pair<String, String>>()
        if (modConfig.valueMap()[mods] == null) {
            throw ScanningException("No mods specified.")
        } else {
            val mods = modConfig.valueMap()[mods] as ArrayList<*>
            for (mod in mods) {
                val config = mod as CommentedConfig
                val extracted = config.valueMap()
                modConfigs.add(extracted)
            }
        }
        val dependencies: Map<String, ArrayList<CommentedConfig>> = getMapOfDependencyLists(modConfig)
        var containedForgeOrMinecraft = false
        for (config in modConfigs) {
            val modId = config[modId].toString()
            if (dependencies.containsKey(modId)) {
                val modDependencies = dependencies[modId]!!
                for (dependency in modDependencies) {
                    try {
                        val dependencyModId = getModId(dependency)
                        if (dependencyModId.matches(neoForgeMinecraft)) {
                            containedForgeOrMinecraft = true
                            try {
                                val side = getSide(dependency)
                                if (side.matches(bothServer)) {
                                    entries.add(Pair(modId, fileName))
                                }
                            } catch (ex: NullPointerException) {
                                // no side specified....assuming both|server
                                entries.add(Pair(modId, fileName))
                            }
                        }
                    } catch (e: NullPointerException) {
                        // no modId specified in dependency...assuming forge|minecraft and both|server
                        containedForgeOrMinecraft = true
                        entries.add(Pair(modId,"$fileName ($modId)"))
                    }
                }
            } else {
                // contains no self referencing dependency...
                entries.add(Pair(modId, fileName))
            }
            if (!containedForgeOrMinecraft) {
                entries.add(Pair(modId, fileName))
            }
        }
        return entries
    }

    /**
     * Acquire a list of ids of dependencies required by the passed mod in order to run on a modded
     * server. Only if all dependencies in this mod specify `CLIENT` for either `forge `
     * or `minecraft` is a dependency not added to the list of required dependencies. Otherwise,
     * all modIds mentioned in the dependencies of this mod, which are neither `forge` nor
     * `minecraft` get added to the list.
     *
     * @param modConfig Base-config of the toml of the mod which contains all information.
     * @return Set of ids of mods required as dependencies.
     * @throws ScanningException if the mod has invalid dependency declarations or specifies no mods.
     */
    @Throws(ScanningException::class)
    private fun getModDependencyIdsRequiredOnServer(modConfig: CommentedConfig, fileName: String): ArrayList<Pair<String,String>> {
        val dependencies: Map<String, ArrayList<CommentedConfig>> = getMapOfDependencyLists(modConfig)
        val idsInMod = getModIdsInJar(modConfig)
        val entries = ArrayList<Pair<String, String>>()
        try {
            var confidentOnClientSide = true
            for (modId in idsInMod) {
                if (dependencies.containsKey(modId)) {
                    val modIdDependencies = dependencies[modId]!!
                    //check all dependencies in mod
                    for (dependency in modIdDependencies) {
                        val dependencyModId = getModId(dependency)
                        val side = getSide(dependency)
                        val required = getRequired(dependency)
                        if (dependencyModId.matches(neoForgeMinecraft) && side.matches(bothServer)) { // && required.equals(requiredAsDep,true)
                            confidentOnClientSide = false
                        }
                    }
                } else {
                    //no dependencies specified, assume required
                    confidentOnClientSide = false
                    break
                }
            }
            //if not a single id said server/both, stop and return list of ids
            if (confidentOnClientSide) {
                return entries
            }
        } catch (ignored: NullPointerException) {
        }
        for ((key, value) in dependencies) {
            for (commentedConfig in value) {
                try {
                    val dependencyID = getModId(commentedConfig)
                    // dependency forge|minecraft?
                    if (!dependencyID.matches(neoForgeMinecraft)) {
                        try {
                            // dependency required on the server?
                            val side = getSide(commentedConfig)
                            // Mandatory dependency?
                            val required = getRequired(commentedConfig)
                            if (side.matches(bothServer) && required.equals(requiredAsDep,true)) {
                                for (modID in idsInMod) {
                                    entries.add(Pair(dependencyID,"$fileName ($modID)"))
                                }
                            }
                        } catch (ex: NullPointerException) {
                            // dependency specifies no side
                            for (modID in idsInMod) {
                                entries.add(Pair(dependencyID,"$fileName ($modID)"))
                            }
                        }
                    }
                } catch (e: NullPointerException) {
                    // dependency specifies no modId, so use parent.
                    val lowerKey = key.lowercase()
                    if (!lowerKey.matches(neoForgeMinecraft)) {
                        for (modID in idsInMod) {
                            entries.add(Pair(key,"$fileName ($modID)"))
                        }
                    }
                }
            }
        }
        return entries
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
        if (config.valueMap()[mods] == null) {
            throw ScanningException("No mods specified.")
        } else {
            val commentedConfigs = config.valueMap()[mods] as ArrayList<*>
            for (entry in commentedConfigs) {
                val commentedConfig = entry as CommentedConfig
                val modId = getModId(commentedConfig)
                ids.add(modId)
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
        val jarEntry = jarFile.getJarEntry(modsToml)
        val tomlStream: InputStream = jarFile.getInputStream(jarEntry)
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
        if (config.valueMap()[dependencies] == null) {
            throw ScanningException("No dependencies specified.")
        }
        val modDependencies = HashMap<String, ArrayList<CommentedConfig>>(100)
        val configValueMap = config.valueMap()
        if (configValueMap[dependencies] is ArrayList<*>) {
            val mods = configValueMap[mods] as ArrayList<*>
            val modConfig = mods[0] as CommentedConfig
            val id = getModId(modConfig)

            @Suppress("UNCHECKED_CAST")
            val entry = configValueMap[dependencies] as ArrayList<CommentedConfig>
            modDependencies[id] = entry
        } else {
            val configs = configValueMap[dependencies] as CommentedConfig
            for ((key, value) in configs.valueMap().entries) {
                @Suppress("UNCHECKED_CAST")
                modDependencies[key.lowercase()] = value as ArrayList<CommentedConfig>
            }
        }
        return modDependencies
    }

    /**
     * Acquire the modId from the passed config.
     *
     * @param config Mod- or dependency-config which contains the modId.
     * @return `modId` from the passed config, in lower-case letters.
     */
    private fun getModId(config: CommentedConfig) = config.valueMap()[modId].toString().lowercase()

    /**
     * Acquire the side of the config of the passed dependency.
     *
     * @param config Mod- or dependency-config which contains the modId.
     * @return `side` from the passed config, in upper-case letters.
     */
    private fun getSide(config: CommentedConfig): String {
        return if (config.valueMap()[side] != null) {
            config.valueMap()[side].toString().uppercase()
        } else {
            both
        }
    }

    /**
     * Acquire the side of the config of the passed dependency.
     *
     * @param config Mod- or dependency-config which contains the modId.
     * @return `side` from the passed config, in upper-case letters.
     */
    private fun getRequired(config: CommentedConfig): String {
        return if (config.valueMap()[type] != null) {
            config.valueMap()[type].toString().uppercase()
        } else if (config.valueMap()[mandatory] != null) {
            if (config.valueMap()[mandatory].toString() == "true") {
                requiredAsDep
            } else {
                config.valueMap()[mandatory].toString().uppercase()
            }
        } else {
            requiredAsDep
        }
    }
}