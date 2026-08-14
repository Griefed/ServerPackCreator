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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * `quilt.mod.json`-based scanning of Fabric-Minecraft mods.
 *
 * @param objectMapper For JSON-parsing.
 * @param utilities    Common utilities used across ServerPackCreator.
 *
 * @author Griefed
 */
class QuiltScanner(
    private val objectMapper: ObjectMapper,
    private val utilities: Utilities
) : JsonBasedScanner(), Scanner<List<ScannedMod>, Collection<File>> {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val quiltModJson = "quilt.mod.json"
    private val quiltLoader = "quilt_loader"
    private val id = "id"
    private val client = "client"
    private val minecraft = "minecraft"
    private val environment = "environment"
    private val depends = "depends"

    /** Dependency ids that are the platform rather than a mod, so they never pull a jar into the keep-list. */
    val dependencyExclusions: Regex
        get() = "(quilt_loader|quilt_base|quilted_fabric_api|java|minecraft)".toRegex()

    private var currentModID: String? = null

    /**
     * Scan the `quilt.mod.json`-files in mod JAR-files of a given directory for their sideness.
     *
     * If `minecraft.environment` specifies `client`, and is not listed as a dependency for another mod, it is added
     * and therefore later on excluded from the server pack.
     *
     * @param jarFiles A list of files in which to check the `fabric.mod.json`-files.
     * @return List of mods not to include in server pack based on fabric.mod.json-content.
     * @author Griefed
     */
    override fun scan(jarFiles: Collection<File>): List<ScannedMod> {
        log.info("Scanning Quilt mods for sideness...")

        val scannedMods = mutableListOf<ScannedMod>()

        for (modJar in jarFiles) {
            try {
                currentModID = null
                val modConfig: JsonNode = getJarJson(modJar, quiltModJson, objectMapper)
                currentModID = utilities.jsonUtilities.getNestedText(modConfig, quiltLoader, id)
                val scannedMod = ScannedMod(modJar)

                val sidesAndDeps = getSidenessesAndDependencies(modConfig)
                scannedMod.modID = currentModID!!

                scannedMod.sideness = if (sidesAndDeps.first.any { it == Sideness.SERVER }) {
                    Sideness.SERVER
                } else {
                    Sideness.CLIENT
                }

                scannedMod.dependencies.addAll(sidesAndDeps.second)
                scannedMods.add(scannedMod)
            } catch (e: Exception) {
                log.error("Could not scan ${modJar.name}. Consider reporting this: ${e.cause}: ${e.message}")
                scannedMods.add(ScannedMod(modJar))
            }
        }

        return scannedMods
    }

    private fun getSidenessesAndDependencies(modConfig: JsonNode): Pair<List<Sideness>, List<ModDependency>> {
        val sidesForModloader = mutableListOf<Sideness>()
        val modDependencies = mutableListOf<ModDependency>()

        try {
            if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(modConfig, client, minecraft,  environment)) {
                sidesForModloader.add(Sideness.CLIENT)
            } else {
                sidesForModloader.add(Sideness.SERVER)
            }
        } catch (_: NullPointerException) {
            // No "environment" entry in this fabric.mod.json -> the mod is not declared client-only.
            sidesForModloader.add(Sideness.SERVER)
        }

        // Get this mods dependencies
        try {
            val dependencies = utilities.jsonUtilities.getNestedElement(modConfig, quiltLoader, depends)
            for (dependency in dependencies) {
                if (dependency.isContainerNode) {
                    try {
                        val dependencyId = utilities.jsonUtilities.getNestedText(dependency, id)
                        if (!dependencyId.matches(dependencyExclusions)) {
                            log.debug("Added dependency $dependencyId for $currentModID.")
                            modDependencies.add(ModDependency(dependencyId))
                        }
                    } catch (_: NullPointerException) {
                        log.debug("No dependencies for $currentModID.")
                    }
                } else {
                    try {
                        val dependencyText = dependency.asText()
                        if (!dependencyText.matches(dependencyExclusions)) {
                            log.debug("Added dependency $dependencyText for $currentModID.")
                            modDependencies.add(ModDependency(dependencyText))
                        }
                    } catch (_: NullPointerException) {
                        log.debug("No dependencies for $currentModID.")
                    }
                }
            }
        } catch (_: NullPointerException) {
            // No "depends" block in this fabric.mod.json -> the mod declares no
            // dependencies, so there is nothing to record.
        }

        return Pair(sidesForModloader, modDependencies)
    }
}