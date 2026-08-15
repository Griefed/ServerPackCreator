/* Copyright (C) 2026 Griefed
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
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.jar.JarFile

/**
 * `mods.toml`-based scanning of Forge-Minecraft mods for Minecraft 1.16.5 and newer.
 *
 * @param tomlParser To parse .toml-files.
 * @Griefed
 */
open class ForgeTomlScanner(private val tomlParser: TomlParser) : DescriptorScanner() {
    private val mods = "mods"
    private val modId = "modId"
    private val dependencies = "dependencies"
    private val side = "side"
    private val both = "BOTH"

    /** Path of the descriptor inside a Forge jar. `open` because NeoForge moved it, and that subclass overrides it. */
    open val modsToml = "META-INF/mods.toml"

    /**
     * Dependency ids that are the platform itself rather than another mod. A mod declaring these is not depending on
     * anything the server pack has to keep, so they must not pull a jar into the dependency list.
     */
    val neoForgeMinecraft: Regex
        get() = "^(neoforge|forge|minecraft)$".toRegex()

    val client: Regex
        get() = "^CLIENT$".toRegex()

    /**
     * Read one mod's `mods.toml` for its sideness.
     *
     * The side a mod demands of the platform (`Forge`/`NeoForge`/`Minecraft`) is taken as its own:
     * a mod requiring Minecraft `side=CLIENT` is clientside. Every other dependency is recorded as a
     * dependency instead. A mod declaring no dependencies at all is treated as server-side, to
     * prevent false positives.
     *
     * @param modJar The jar whose `mods.toml` to read.
     * @return What this mod declared.
     * @author Griefed
     */
    override fun read(modJar: File): ScannedMod {
        val modConfig: CommentedConfig = getConfig(modJar)
        val modId = getModId((modConfig.valueMap()[mods] as ArrayList<*>)[0] as CommentedConfig)
        val (sidenesses, dependencies) = getSidenessesAndDependencies(modConfig, modId)
        return ScannedMod(modJar, modId, sidenessOf(sidenesses), dependencies)
    }

    @Throws(ScanningException::class)
    private fun getSidenessesAndDependencies(modConfig: CommentedConfig, modId: String): Pair<List<Sideness>, List<ModDependency>> {
        val dependencies: Map<String, ArrayList<CommentedConfig>> = getMapOfDependencyLists(modConfig)
        val sidesForModloader = mutableListOf<Sideness>()
        val modDependencies = mutableListOf<ModDependency>()
        try {
            val declaredDependencies = dependencies[modId]
            if (declaredDependencies != null) {
                //check all dependencies in mod
                for (declared in declaredDependencies) {
                    val dependencyModId = getModId(declared)
                    val side = getSide(declared)
                    val dependencySideness =
                        if (side.uppercase().matches(client)) Sideness.CLIENT else Sideness.SERVER

                    if (dependencyModId.matches(neoForgeMinecraft)) {
                        // The platform itself. What side this mod demands of Minecraft/Forge IS its sideness.
                        sidesForModloader.add(dependencySideness)
                    } else {
                        modDependencies.add(ModDependency(dependencyModId, dependencySideness))
                    }
                }
            } else {
                //no dependencies specified, assume required
                sidesForModloader.add(Sideness.SERVER)
            }

        } catch (_: NullPointerException) {
            // A dependency was missing a modId/side mid-evaluation, so we can't conclude the mod is
            // confidently client-side. Assume SERVER.
            sidesForModloader.add(Sideness.SERVER)
        }
        return Pair(sidesForModloader,modDependencies)
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
     * @param config Base-config toml of the mod which contains all * information.
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
     * Acquire the side of the passed dependency.
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
}