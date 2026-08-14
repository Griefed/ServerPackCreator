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
import de.griefed.serverpackcreator.api.config.SupportedModloaders.quilt
import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.modscanning.ScannedMod
import de.griefed.serverpackcreator.api.modscanning.Sideness
import de.griefed.serverpackcreator.api.utilities.SimpleStopWatch
import de.griefed.serverpackcreator.api.utilities.common.Comparison
import de.griefed.serverpackcreator.api.utilities.common.FilterType
import de.griefed.serverpackcreator.api.utilities.common.ListUtilities
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
        val serverMods: MutableList<ScannedMod> = mutableListOf()
        val disabledMods: MutableList<ScannedMod> = mutableListOf()
        val scannedMods: MutableList<ScannedMod> = mutableListOf()
        val scanningStopWatch = SimpleStopWatch().start()

        when (modloader) {
            "LegacyFabric", "Fabric" -> {
                scannedMods.addAll(modScanner.fabricScanner.scan(filesInModsDir))
            }

            "Forge" -> {
                val mcVersions = minecraftVersion.split(".").dropLastWhile { it.isEmpty() }.toTypedArray()
                if (mcVersions[1].toInt() > 12) {
                    scannedMods.addAll(modScanner.forgeTomlScanner.scan(filesInModsDir))
                } else {
                    scannedMods.addAll(modScanner.forgeAnnotationScanner.scan(filesInModsDir))
                }
            }

            "NeoForge" -> {
                if (SemanticVersionComparator.compareSemantics(
                        "1.20.5",
                        minecraftVersion,
                        Comparison.EQUAL_OR_NEW
                    )
                ) {
                    log.debug("Scanning using NeoForge scanner.")
                    scannedMods.addAll(modScanner.neoForgeTomlScanner.scan(filesInModsDir))
                } else {
                    log.debug("Scanning using Forge scanner.")
                    scannedMods.addAll(modScanner.forgeTomlScanner.scan(filesInModsDir))
                }
            }

            "Quilt" -> {
                val quiltScan = modScanner.quiltScanner.scan(filesInModsDir).toMutableList()
                val fabricScan = modScanner.fabricScanner.scan(filesInModsDir)
                for (i in quiltScan.indices) {
                    val match = fabricScan.find { fabric -> fabric.file.name == quiltScan[i].file.name }
                    if (match == null) { continue }
                    if (quiltScan[i].sideness == Sideness.SERVER && match.sideness == Sideness.CLIENT) {
                        quiltScan[i] = match
                        log.info("${quiltScan[i].file.name} Quilt-scan yielded sideness SERVER, but Fabric-scan yielded CLIENT. Using Fabric-scan result instead.")
                    }
                }
                for (fabric in fabricScan) {
                    if (quiltScan.find { quilt -> quilt.file.name == fabric.file.name } == null) {
                        log.info("Quilt-scan did not have a scan for ${fabric.file.name}, Fabric-scan did, though. Copying entry. ")
                        quiltScan.add(fabric)
                    }
                }
                scannedMods.addAll(quiltScan)
            }

            else -> {
                // No scanner knows this loader, so nothing can be judged clientside. Keeping every mod
                // leaves a pack the user can trim; returning none would look like a successful run that
                // silently produced nothing.
                log.warn("Unrecognised modloader '$modloader'. Skipping sideness detection and including every mod.")
                scannedMods.addAll(filesInModsDir.map { ScannedMod(it) })
            }
        }


        log.info("Scanned mods: ${scannedMods.size}")
        log.debug("Scanning of ${filesInModsDir.size} mods took ${scanningStopWatch.stop().getTime()}")

        if (apiProperties.isAutoExcludingModsEnabled) {
            for (mod in scannedMods) {
                val modName = mod.file.name
                if (mod.sideness == Sideness.CLIENT) {
                    log.warn("Automatically disabling mod: $modName")
                    disabledMods.add(mod)
                } else {
                    serverMods.add(mod)
                }
            }
        } else {
            log.info("Automatic clientside-only mod detection disabled.")
        }

        log.info("Performing ${apiProperties.exclusionFilter}-type checks for user-specified clientside-only mod exclusion.")
        for (mod in scannedMods) {
            log.debug("Checking ${mod.file.name} (${mod.modID})")
            val modName = mod.file.name
            var foundExclusionMatch: Boolean = false
            var exclusionMatch = "N/A"

            //Perform exclusions based on clientside-mods list
            for (userSpecifiedExclusion in clientsideModsList) {
                foundExclusionMatch = when (apiProperties.exclusionFilter) {
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
                if (foundExclusionMatch) {
                    exclusionMatch = userSpecifiedExclusion
                    break
                }
            }

            if (foundExclusionMatch) {
                if (disabledMods.find { it.file.name == mod.file.name } == null) {
                    disabledMods.add(mod)
                }
                serverMods.removeIf { it.file.name == mod.file.name }
                log.info("Disabling ${mod.file.name}. It matched clientside-mod: $exclusionMatch")
            } else  if (disabledMods.find { it.file.name == mod.file.name } == null) {
                if (serverMods.find { it.file.name == mod.file.name } == null) {
                    serverMods.add(mod)
                    log.debug("No clientside-match, no whitelist-match. Keeping ${mod.file.name} enabled.")
                }
            } else {
                log.debug("${mod.file.name} already disabled.")
            }
        }

        while (disabledMods.any { disabledMod ->
                modWhitelist.any { entry ->
                    when (apiProperties.exclusionFilter) {
                        ExclusionFilter.START -> disabledMod.file.name.startsWith(entry)
                        ExclusionFilter.END -> disabledMod.file.name.endsWith(entry)
                        ExclusionFilter.CONTAIN -> disabledMod.file.name.contains(entry)
                        ExclusionFilter.REGEX -> disabledMod.file.name.matches(entry.toRegex())
                        ExclusionFilter.EITHER -> (
                                disabledMod.file.name.startsWith(entry) ||
                                        disabledMod.file.name.endsWith(entry) ||
                                        disabledMod.file.name.contains(entry) ||
                                        disabledMod.file.name.matches(entry.toRegex()))
                    }}}) {

            disabledMods.removeIf { disabledMod ->
                val match = modWhitelist.find { entry ->
                    when (apiProperties.exclusionFilter) {
                        ExclusionFilter.START -> disabledMod.file.name.startsWith(entry)
                        ExclusionFilter.END -> disabledMod.file.name.endsWith(entry)
                        ExclusionFilter.CONTAIN -> disabledMod.file.name.contains(entry)
                        ExclusionFilter.REGEX -> disabledMod.file.name.matches(entry.toRegex())
                        ExclusionFilter.EITHER -> (
                                disabledMod.file.name.startsWith(entry) ||
                                        disabledMod.file.name.endsWith(entry) ||
                                        disabledMod.file.name.contains(entry) ||
                                        disabledMod.file.name.matches(entry.toRegex()))
                    }
                }
                return@removeIf if (match != null) {
                    log.info("Disabled mod ${disabledMod.file.name} is whitelisted by $match. Not disabling.")
                    serverMods.add(disabledMod)
                    true
                } else {
                    false
                }
            }
        }

        while (disabledMods.any { disabledMod ->                                    // Rip and tear until it is done.
                serverMods.find { serverMod ->                                      // There mustn't be a single dependency
                    serverMod.dependencies.filter { dependency ->                   // of a server mod left in the list of
                        dependency.sideness == Sideness.SERVER }.map { dep ->       // disabled mods.
                            dep.modID }.contains(disabledMod.modID)} != null}) {

            disabledMods.removeIf { disabledMod ->
                val match = serverMods.find { serverMod ->
                    serverMod.dependencies.filter { dependency ->
                        dependency.sideness == Sideness.SERVER }.map { dep ->
                            dep.modID }.contains(disabledMod.modID)}

                return@removeIf if (match != null) {
                    log.info("Disabled mod ${disabledMod.file.name} is a dependency for ${match.file.name}. Not disabling.")
                    serverMods.add(disabledMod)
                    true
                } else {
                    false
                }
            }
        }

        log.info("Mods included:")
        ListUtilities.printListToLogChunked(serverMods.map { it.file.name }, 5, "    ", true)
        log.info("Mods disabled:")
        ListUtilities.printListToLogChunked(disabledMods.map { it.file.name }, 5, "    ", true)

        return Pair(
            TreeSet<File>(serverMods.map { it.file }).toList(),
            TreeSet<File>(disabledMods.map { it.file }).toList()
        )
    }

}
