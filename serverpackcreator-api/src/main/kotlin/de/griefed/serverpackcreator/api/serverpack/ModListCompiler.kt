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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.modscanning.Dependency
import de.griefed.serverpackcreator.api.modscanning.Exclusion
import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.modscanning.ScanResult
import de.griefed.serverpackcreator.api.utilities.SimpleStopWatch
import de.griefed.serverpackcreator.api.utilities.common.Comparison
import de.griefed.serverpackcreator.api.utilities.common.FilterType
import de.griefed.serverpackcreator.api.utilities.common.SemanticVersionComparator
import de.griefed.serverpackcreator.api.utilities.common.filteredWalk
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*

/**
 * Compiler of the list of mods to include in a server pack: walks the mods-directory, excludes
 * user-specified and automatically discovered clientside-only mods, and honors the
 * mod-whitelist. Extracted from ServerPackHandler (refactor Phase 1d); ServerPackHandler
 * remains the facade through which consumers access these operations.
 */
class ModListCompiler(
    private val apiProperties: ApiProperties,
    private val modScanner: ModScanner
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Extensions treated as mod files. `disabled` is included deliberately: a launcher marks a mod off by
     * renaming it, and such a file must still be recognised so it can be excluded rather than copied blindly.
     *
     * The single source of truth: [ServerPackHandler.modFileEndings] reads this rather than holding its
     * own copy, so the published constant and the list generation walks with cannot drift apart.
     */
    val modFileEndings = listOf("jar", "disabled")

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param packConfig The configurationModel containing the modpack directory, list of
     * clientside-only mods to exclude, Minecraft version used by the
     * modpack and server pack and the modloader used by the modpack and
     * server pack.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    @Suppress("unused")
    fun compileModList(packConfig: PackConfig) = compileModList(
        "${packConfig.modpackDir}${File.separator}mods",
        packConfig.clientMods,
        packConfig.modsWhitelist,
        packConfig.minecraftVersion,
        packConfig.modloader
    )

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param modsDir The mods-directory of the modpack of which to generate a list of all its contents.
     * @param clientsideModsList A list of all clientside-only mods.
     * @param modWhitelist A list of mods to include regardless if a match was found in [clientsideModsList].
     * @param minecraftVersion The Minecraft version the modpack uses. When the modloader is Forge, this determines
     * whether Annotations or Tomls are scanned.
     * @param modloader The modloader the modpack uses.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    fun compileModList(
        modsDir: String,
        clientsideModsList: List<String>,
        modWhitelist: List<String>,
        minecraftVersion: String,
        modloader: String
    ): Pair<List<File>,List<File>> {
        log.info("Preparing a list of mods to include in server pack...")
        val filesInModsDir: Collection<File> = File(modsDir).filteredWalk(modFileEndings, FilterType.ENDS_WITH, FileWalkDirection.TOP_DOWN, recursive = false)
        val modsForServerPack = TreeSet(filesInModsDir)
        val disabledMods = TreeSet<File>()
        val autoDiscoveredClientMods: MutableList<Exclusion> = ArrayList(100)
        val modDependencies: MutableList<Dependency> = ArrayList(100)
        var scanResults: ScanResult

        // Check whether scanning mods for sideness is activated.
        if (apiProperties.isAutoExcludingModsEnabled) {
            val scanningStopWatch = SimpleStopWatch().start()
            when (modloader) {
                "LegacyFabric", "Fabric" -> {
                    scanResults = modScanner.fabricScanner.scan(filesInModsDir)
                    autoDiscoveredClientMods.addAll(scanResults.exclusions)
                    modDependencies.addAll(scanResults.dependencies)
                }

                "Forge" -> {
                    val mcVersions = minecraftVersion.split(".").dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (mcVersions[1].toInt() > 12) {
                        scanResults = modScanner.forgeTomlScanner.scan(filesInModsDir)
                        autoDiscoveredClientMods.addAll(scanResults.exclusions)
                        modDependencies.addAll(scanResults.dependencies)
                    } else {
                        scanResults = modScanner.forgeAnnotationScanner.scan(filesInModsDir)
                        autoDiscoveredClientMods.addAll(scanResults.exclusions)
                        modDependencies.addAll(scanResults.dependencies)
                    }
                }

                "NeoForge" -> {
                    if (SemanticVersionComparator.compareSemantics("1.20.5", minecraftVersion, Comparison.EQUAL_OR_NEW)) {
                        log.debug("Scanning using NeoForge scanner.")
                        scanResults = modScanner.neoForgeTomlScanner.scan(filesInModsDir)
                        autoDiscoveredClientMods.addAll(scanResults.exclusions)
                        modDependencies.addAll(scanResults.dependencies)
                    } else {
                        log.debug("Scanning using Forge scanner.")
                        scanResults = modScanner.forgeTomlScanner.scan(filesInModsDir)
                        autoDiscoveredClientMods.addAll(scanResults.exclusions)
                        modDependencies.addAll(scanResults.dependencies)
                    }
                }

                "Quilt" -> {
                    scanResults = modScanner.fabricScanner.scan(filesInModsDir)
                    autoDiscoveredClientMods.addAll(scanResults.exclusions)
                    modDependencies.addAll(scanResults.dependencies)

                    scanResults = modScanner.quiltScanner.scan(filesInModsDir)
                    autoDiscoveredClientMods.addAll(scanResults.exclusions)
                    modDependencies.addAll(scanResults.dependencies)
                }
            }

            // Exclude scanned mods from copying
            if (autoDiscoveredClientMods.isNotEmpty()) {
                log.info("Automatically detected mods: ${autoDiscoveredClientMods.size}")
                for (discoveredMod in autoDiscoveredClientMods) {
                    @Suppress("VariableInitializerIsRedundant")
                    var whitelistMatch = "N/A"
                    val modName = discoveredMod.excludedMod.name
                    val isWhitelistedMod = modWhitelist.any { whitelistEntry ->
                        if (when (apiProperties.exclusionFilter) {
                                ExclusionFilter.START -> modName.startsWith(whitelistEntry)
                                ExclusionFilter.END -> modName.endsWith(whitelistEntry)
                                ExclusionFilter.CONTAIN -> modName.contains(whitelistEntry)
                                ExclusionFilter.REGEX -> modName.matches(whitelistEntry.toRegex())
                                ExclusionFilter.EITHER -> (
                                        modName.startsWith(whitelistEntry) ||
                                                modName.endsWith(whitelistEntry) ||
                                                modName.contains(whitelistEntry) ||
                                                modName.matches(whitelistEntry.toRegex()))
                            }) {
                            whitelistMatch = whitelistEntry
                            log.warn("Prevented automated exclusion of $modName. It's whitelisted with entry: $whitelistMatch")
                            true
                        } else {
                            false
                        }
                    }

                    modsForServerPack.removeIf {
                        if (it.name.contains(modName) && !isWhitelistedMod) {
                            log.warn("Automatically excluding mod: $modName")
                            disabledMods.add(it)
                            return@removeIf true
                        } else {
                            return@removeIf false
                        }
                    }
                }
            } else {
                log.info("No clientside-only mods detected.")
            }

            log.debug(
                "Scanning and excluding of ${filesInModsDir.size} mods took ${scanningStopWatch.stop().getTime()}"
            )
        } else {
            log.info("Automatic clientside-only mod detection disabled.")
        }

        // Exclude user-specified mods from copying.
        if (clientsideModsList.isNotEmpty()) {
            log.info("Performing ${apiProperties.exclusionFilter}-type checks for user-specified clientside-only mod exclusion.")

            modsForServerPack.removeIf { modToCheck ->
                var excludeMod = false
                val isDependencyMod: Boolean
                var isWhitelistedMod: Boolean
                var exclusionMatch = "N/A"
                var whitelistMatch = "N/A"
                var dependant: Dependency? = null
                val modName = modToCheck.name
                for (userSpecifiedExclusion in clientsideModsList) {
                    excludeMod = when (apiProperties.exclusionFilter) {
                        ExclusionFilter.START -> modName.startsWith(userSpecifiedExclusion)
                        ExclusionFilter.END -> modName.endsWith(userSpecifiedExclusion)
                        ExclusionFilter.CONTAIN -> modName.contains(userSpecifiedExclusion)
                        ExclusionFilter.REGEX -> modName.matches(userSpecifiedExclusion.toRegex())
                        ExclusionFilter.EITHER -> (
                                (modName.startsWith(userSpecifiedExclusion)) ||
                                        (modName.endsWith(userSpecifiedExclusion)) ||
                                        (modName.contains(userSpecifiedExclusion)) ||
                                        (modName.matches(userSpecifiedExclusion.toRegex())))
                    }
                    if (excludeMod) {
                        exclusionMatch = userSpecifiedExclusion
                        break
                    }
                }
                if (excludeMod) {
                    isDependencyMod = modDependencies.any { dependency ->
                        if (modName.startsWith(dependency.dependencyID, ignoreCase = true)) {
                            dependant = dependency
                            true
                        } else {
                            false
                        }
                    }

                    isWhitelistedMod = modWhitelist.any { whitelistEntry ->
                        if (when (apiProperties.exclusionFilter) {
                                ExclusionFilter.START -> modName.startsWith(whitelistEntry)
                                ExclusionFilter.END -> modName.endsWith(whitelistEntry)
                                ExclusionFilter.CONTAIN -> modName.contains(whitelistEntry)
                                ExclusionFilter.REGEX -> modName.matches(whitelistEntry.toRegex())
                                ExclusionFilter.EITHER -> (
                                        modName.startsWith(whitelistEntry) ||
                                                modName.endsWith(whitelistEntry) ||
                                                modName.contains(whitelistEntry) ||
                                                modName.matches(whitelistEntry.toRegex()))
                            }) {
                            whitelistMatch = whitelistEntry
                            true
                        } else {
                            false
                        }
                    }
                    if (isDependencyMod &&
                        disabledMods.none { entry -> dependant!!.dependencyID.contains(entry.name,ignoreCase = true) } &&
                        !dependant!!.dependencyID.contains(modToCheck.name,ignoreCase = true) ) {
                        log.info("Not excluding $exclusionMatch. It's a dependency for ${dependant.fileName}.")
                        log.debug("$dependant")
                        excludeMod = false
                    } else if (isWhitelistedMod) {
                        log.info("Not excluding $modToCheck. It's whitelisted with entry: $whitelistMatch")
                        excludeMod = false
                    } else {
                        log.info("Excluding ${modToCheck.name}. It matched clientside-mod entry: $exclusionMatch")
                        disabledMods.add(modToCheck)
                    }
                }
                excludeMod
            }

        } else {
            log.warn("User specified no clientside-only mods.")
        }
        return Pair(modsForServerPack.toList(), disabledMods.toList())
    }

}
