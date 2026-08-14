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
 * `fabric.mod.json`-based scanning of Fabric-Minecraft mods.
 *
 * @param objectMapper For JSON-parsing.
 * @param utilities    Common utilities used across ServerPackCreator.
 *
 * @author Griefed
 */
class FabricScanner(private val objectMapper: ObjectMapper, private val utilities: Utilities
) : JsonBasedScanner(), Scanner<List<ScannedMod>, Collection<File>> {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val fabricModJson = "fabric.mod.json"
    private val id = "id"
    private val client = "client"
    private val environment = "environment"
    private val depends = "depends"
    private val dependencyExclusions: Regex
        get() = "(fabric|fabricloader|java|minecraft)".toRegex()

    /**
     * Scan the `fabric.mod.json`-files in mod JAR-files of a given directory for their
     * sideness.
     *
     * If `environment` specifies `client`, and is not listed as a dependency for another mod, it is added and therefore
     * later on excluded from the server pack.
     *
     * @param jarFiles A list of files in which to check the `fabric.mod.json`-files.
     * @return List of mods not to include in server pack based on fabric.mod.json-content.
     * @author Griefed
     */
    override fun scan(jarFiles: Collection<File>): List<ScannedMod> {
        log.info("Scanning Fabric mods for sideness...")

        val scannedMods = mutableListOf<ScannedMod>()

        for (modJar in jarFiles) {
            try {
                val modConfig: JsonNode = getJarJson(modJar, fabricModJson, objectMapper)
                val modId = utilities.jsonUtilities.getNestedText(modConfig, id)
                val (sidenesses, dependencies) = getSidenessesAndDependencies(modConfig, modId)

                scannedMods.add(ScannedMod(modJar, modId, sidenessOf(sidenesses), dependencies))
            } catch (e: Exception) {
                log.error("Could not scan ${modJar.name}. Consider reporting this: ${e.cause}: ${e.message}")
                scannedMods.add(ScannedMod(modJar))
            }
        }

        return scannedMods
    }

    private fun getSidenessesAndDependencies(modConfig: JsonNode, modId: String): Pair<List<Sideness>, List<ModDependency>> {
        val sidesForModloader = mutableListOf<Sideness>()
        val modDependencies = mutableListOf<ModDependency>()

        try {
            if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(modConfig, client, environment)) {
                sidesForModloader.add(Sideness.CLIENT)
            } else {
                sidesForModloader.add(Sideness.SERVER)
            }
        } catch (_: NullPointerException) {
            // No "environment" entry in this fabric.mod.json -> the mod is not declared
            // client-only. Assume server.
            sidesForModloader.add(Sideness.SERVER)
        }

        // Get this mods dependencies
        try {
            val dependencies = utilities.jsonUtilities.getFieldNames(modConfig, depends)
            for (dependency in dependencies) {
                log.debug("Checking dependency $dependency for $modId.")
                if (!dependency.matches(dependencyExclusions)) {
                    try {
                        log.debug("Added dependency $dependency for $modId.")
                        modDependencies.add(ModDependency(dependency))
                    } catch (_: NullPointerException) {
                        log.debug("No dependencies for $modId.")
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