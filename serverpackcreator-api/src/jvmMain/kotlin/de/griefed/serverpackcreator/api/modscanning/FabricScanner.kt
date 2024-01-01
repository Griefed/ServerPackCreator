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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*

/**
 * `fabric.mod.json`-based scanning of Fabric-Minecraft mods.
 *
 * @param objectMapper For JSON-parsing.
 * @param utilities    Common utilities used across ServerPackCreator.
 *
 * @author Griefed
 */
actual class FabricScanner(
    private val objectMapper: ObjectMapper,
    private val utilities: Utilities
) : JsonBasedScanner(), Scanner<TreeSet<File>, Collection<File>> {
    private val log = cachedLoggerOf(this.javaClass)
    private val jar = "jar"
    private val fabricModJson = "fabric.mod.json"
    private val id = "id"
    private val client = "client"
    private val environment = "environment"
    private val depends = "depends"

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
    override fun scan(jarFiles: Collection<File>): TreeSet<File> {
        log.info("Scanning Fabric mods for sideness...")
        val modDependencies = ArrayList<Pair<String, Pair<String, String>>>()
        val clientMods = TreeSet<String>()

        /*
        * Go through all mods in our list and acquire a list of clientside-only mods as well as any
        * dependencies of the mods.
        */
        checkForClientModsAndDeps(jarFiles, clientMods, modDependencies)

        //Remove any dependency from out list of clientside-only mods, so we do not exclude any dependency.
        cleanupClientMods(modDependencies, clientMods)

        /*
        * After removing dependencies from the list of potential clientside mods, we can check whether
        * any of the remaining clientmods is available in our list of files. The resulting set is the
        * set of mods we can safely exclude from our server pack.
        */
        return getModsDelta(jarFiles, clientMods)
    }

    override fun checkForClientModsAndDeps(
        filesInModsDir: Collection<File>,
        clientMods: TreeSet<String>,
        modDependencies: ArrayList<Pair<String, Pair<String, String>>>
    ) {
        for (mod in filesInModsDir) {
            if (!mod.name.endsWith(jar)) {
                continue
            }
            var modId: String
            try {
                val modJson: JsonNode = getJarJson(mod, fabricModJson, objectMapper)
                modId = utilities.jsonUtilities.getNestedText(modJson, id)

                // Get this mods' id/name
                try {
                    if (utilities.jsonUtilities
                            .nestedTextEqualsIgnoreCase(modJson, client, environment)
                    ) {
                        clientMods.add(modId)
                        log.debug("Added clientMod: $modId")
                    }
                } catch (ignored: NullPointerException) {
                }

                // Get this mods dependencies
                try {
                    val dependencies = utilities.jsonUtilities.getFieldNames(modJson, depends)
                    for (dependency in dependencies) {
                        log.debug("Checking dependency $dependency for $mod.")
                        if (!dependency.matches(dependencyExclusions)) {
                            try {
                                log.debug("Added dependency $dependency for $modId (${mod.name}).")
                                modDependencies.add(Pair(dependency, Pair(mod.name, modId)))
                            } catch (ex: NullPointerException) {
                                log.debug("No dependencies for $modId (${mod.name}).")
                            }
                        }
                    }
                } catch (ignored: NullPointerException) {
                }
            } catch (ex: NullPointerException) {
                log.warn("Couldn't scan $mod as it contains no fabric.mod.json.")
            } catch (ex: Exception) {
                log.error("Couldn't scan $mod", ex)
            }

        }
    }

    override fun getModsDelta(filesInModsDir: Collection<File>, clientMods: TreeSet<String>): TreeSet<File> {
        val modsDelta = TreeSet<File>()
        for (mod in filesInModsDir) {
            var modIdToCheck: String
            var addToDelta = false
            try {
                val modJson: JsonNode = getJarJson(mod, fabricModJson, objectMapper)

                // Get the modId
                modIdToCheck = utilities.jsonUtilities.getNestedText(modJson, id)
                try {
                    if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(modJson, client, environment)
                        && clientMods.contains(modIdToCheck)
                    ) {
                        addToDelta = true
                    }
                } catch (ignored: NullPointerException) {
                }
                if (addToDelta) {
                    modsDelta.add(mod)
                }
            } catch (ignored: Exception) {
            }
        }
        return modsDelta
    }
}