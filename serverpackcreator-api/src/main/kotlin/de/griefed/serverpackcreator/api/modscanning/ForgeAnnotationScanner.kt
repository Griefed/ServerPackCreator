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
import de.griefed.serverpackcreator.api.utilities.common.JsonException
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*

/**
 * `fml-cache-annotation.json`-based scanning of Forge-Minecraft mods of older versions.
 *
 * @param objectMapper For JSON-parsing.
 * @param utilities    Common utilities used across ServerPackCreator.
 *
 * @author Griefed
 */
class ForgeAnnotationScanner(private val objectMapper: ObjectMapper, private val utilities: Utilities) : JsonBasedScanner(), Scanner<List<ScannedMod>, Collection<File>> {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val additionalDependencyRegex = "(@.*|\\[.*)".toRegex()
    private val caches = "META-INF/fml_cache_annotation.json"
    private val annotations = "annotations"
    private val values = "values"
    private val modid = "modid"
    private val value = "value"
    private val clientSideOnly = "clientSideOnly"
    private val dependencies = "dependencies"

    /** Matches a dependency entry worth recording, filtering out the malformed ones older packs contain. */
    val dependencyCheck: Regex
        get() = "(before:.*|after:.*|required-after:.*|)".toRegex()
    /** Strips the version range off a dependency entry, leaving the mod id the scanner matches on. */
    val dependencyReplace: Regex
        get() = "(@.*|\\[.*)".toRegex()

    private var currentModID: String? = null

    /**
     * Scan the `fml-cache-annotation.json`-files in mod JAR-files of a given directory for their sideness.
     *
     * If `clientSideOnly` specifies `"value": "true"`, and is not listed as a dependency for another mod, it is added
     * and therefore later on excluded from the server pack.
     *
     * @param jarFiles A list of files in which to check the `fml-cache-annotation.json `-files.
     * @return List of mods not to include in server pack based on fml-cache-annotation.json-content.
     * @author Griefed
     */
    override fun scan(jarFiles: Collection<File>): List<ScannedMod> {
        log.info("Scanning Minecraft 1.12.x and older mods for sideness...")

        val scannedMods = mutableListOf<ScannedMod>()

        for (modJar in jarFiles) {
            try {
                currentModID = null
                val modConfig: JsonNode = getJarJson(modJar, caches, objectMapper)
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
                log.error("Could not scan ${modJar.name}. Consider reporting this:", e)
                scannedMods.add(ScannedMod(modJar))
            }
        }

        return scannedMods
    }

    @Throws(Exception::class)
    private fun getSidenessesAndDependencies(modConfig: JsonNode): Pair<List<Sideness>, List<ModDependency>> {
        val additionalMods = TreeSet<String>()
        val sidesForModloader = mutableListOf<Sideness>()
        val modDependencies = mutableListOf<ModDependency>()
        // base of json
        for (node in modConfig) {
            try {
                // iterate though annotations
                val cacheAnnotations = node.get(annotations)
                for (child in cacheAnnotations) {

                    if (currentModID == null) {
                        // Get the mod ID and check for clientside only, if we have not yet received a
                        // modID
                        try {
                            currentModID = getModId(child)
                            sidesForModloader.add(getSide(child))
                        } catch (_: NullPointerException) {
                            // This annotation child carries no modId/clientside annotation
                            // -> skip it; another child in the same cache may provide one.
                        } catch (_: JsonException) {
                            // Malformed annotation entry -> skip it.
                        }

                        try {
                            // Get dependency modIds
                            modDependencies.addAll(getDependencies(child))
                        } catch (_: Exception) {
                            log.warn("No dependencies for mod ")
                        }
                    } else {
                        try {
                            // Get the additional modID
                            val idsAndSidenesses = getAdditionalModIDsAndSidenesses(child)
                            additionalMods.addAll(idsAndSidenesses.first)
                            sidesForModloader.addAll(idsAndSidenesses.second)
                        } catch (_: NullPointerException) {
                            // This child declares no additional modId -> nothing to add.
                        }
                    }
                }
            } catch (_: NullPointerException) {
                // This node has no "annotations" array -> skip it and continue with the
                // next node in the cache.
            }
        }
        if (!additionalMods.isEmpty()) {
            sidesForModloader.addAll(getNestedModsSides(additionalMods, modConfig))
        }

        return Pair(sidesForModloader, modDependencies)
    }

    private fun getNestedModsSides(additionalMods: TreeSet<String>,modJson: JsonNode): List<Sideness> {
        val sides = mutableListOf<Sideness>()
        for (additionalModId in additionalMods) {
            // base of json
            for (node in modJson) {
                try {
                    // iterate though annotations again but this time for the modID of the second mod
                    for (child in node.get(annotations)) {
                        var additionalModDependsOnFirst = false

                        // check if second mod depends on first
                        try {
                            /*
                            * if the modId is that of our additional mod, check the dependencies whether the
                            * first modId is present
                            */
                            if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(child,additionalModId,values, modid, value)
                                && !utilities.jsonUtilities.nestedTextIsEmpty(child,values, dependencies, value)) {
                                if (utilities.jsonUtilities.nestedTextContains(child, ";", values, dependencies, value)) {
                                    if (additionalDependenciesDepend(child, currentModID!!)) {
                                        additionalModDependsOnFirst = true
                                    }
                                } else {
                                    if (additionalDependencyDepends(child, currentModID!!)) {
                                        additionalModDependsOnFirst = true
                                    }
                                }
                            }
                        } catch (_: NullPointerException) {
                            // This child carries no modId/dependencies value -> it can't establish a
                            // dependency on the first mod, so leave additionalModDependsOnFirst false.
                        }

                        /*
                        * If the additional mod depends on the first one, check if the additional one is
                        * clientside-only
                        */
                        if (additionalModDependsOnFirst) {
                            /*
                            * if the additional mod is NOT clientside-only, we have to remove this mod from the
                            * list of clientside-only mods
                            */
                            if (isAdditionalModClientSide(node, additionalModId)) {
                                sides.add(Sideness.CLIENT)
                            } else {
                                sides.add(Sideness.SERVER)
                            }
                        }
                    }
                } catch (_: NullPointerException) {
                    // This node has no "annotations" array -> skip it and continue with the next
                    // node while resolving additional mods.
                }
            }
        }
        return sides
    }

    @Throws(NullPointerException::class)
    private fun getAdditionalModIDsAndSidenesses(child: JsonNode): Pair<List<String>, List<Sideness>> {
        val ids = ArrayList<String>()
        val sidenessses = ArrayList<Sideness>()
        if (!utilities.jsonUtilities.nestedTextIsEmpty(child, values, modid, value)) {

            // ModIDs are the same, so check for clientside-only
            if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(child, currentModID!!, values, modid, value)
            ) {
                try {
                    // Add mod to list of clientmods if clientSideOnly is true
                    if (utilities.jsonUtilities.getNestedBoolean(child, values, clientSideOnly, value)) {
                        sidenessses.add(Sideness.CLIENT)
                    } else {
                        sidenessses.add(Sideness.SERVER)
                    }
                } catch (_: NullPointerException) {
                    // No "clientSideOnly" flag on this annotation -> treat as not client-only.
                } catch (_: JsonException) {
                    // Malformed "clientSideOnly" value -> treat as not client-only.
                }
            } else {
                // ModIDs are different, possibly two mods in one JAR-file.......
                // Add additional modId to list, so we can check those later
                ids.add(utilities.jsonUtilities.getNestedText(child, values, modid, value))
            }
        }
        return Pair(ids, sidenessses)
    }

    private fun getDependencies(child: JsonNode) : List<ModDependency> {
        val modDependencies = mutableListOf<ModDependency>()
        try {
            if (!utilities.jsonUtilities.nestedTextIsEmpty(child, values, dependencies, value)) {

                // There are multiple dependencies for this mod
                if (utilities.jsonUtilities.nestedTextContains(child, ";", values, dependencies, value)) {
                    val dependencies: Array<String> = utilities.jsonUtilities.getNestedTexts(child, ";", values, dependencies,value)
                    for (dependency in dependencies) {
                        if (dependency.matches(dependencyCheck)) {
                            modDependencies.add(ModDependency(getDependency(dependency)))
                        }
                    }

                    // There is only one dependency, or it is a regular minecraft/forge dependency.
                } else {
                    if (utilities.jsonUtilities.nestedTextMatches(child,dependencyCheck,values, dependencies, value)
                    ) {
                        val dependencies: String = utilities.jsonUtilities.getNestedText(child, values, dependencies, value)
                        modDependencies.add(ModDependency(getDependency(dependencies)))
                    }
                }
            }
        } catch (_: NullPointerException) {
            // This annotation declares no "dependencies" value -> the mod has no dependencies to
            // record.
        }
        return modDependencies
    }

    /**
     * Get the id of the mod currently being checked.
     *
     * @param jsonNode The JSON node containing the modId.
     * @return The id of the mod.
     * @throws NullPointerException if the JSON node does not contain the modId.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    private fun getModId(jsonNode: JsonNode) =
        if (!utilities.jsonUtilities.nestedTextIsEmpty(jsonNode, values, modid, value)) {
            utilities.jsonUtilities.getNestedText(jsonNode, values, modid, value)
        } else {
            throw NullPointerException("No modId present.")
        }

    private fun getSide(jsonNode: JsonNode): Sideness {
        return try {
            if (utilities.jsonUtilities.getNestedBoolean(jsonNode, values, clientSideOnly, value)) {
                Sideness.CLIENT
            } else {
                Sideness.SERVER
            }
        } catch (_: NullPointerException) {
            Sideness.SERVER
        }
    }

    /**
     * Get the id of a dependency.
     *
     * @param dependency The full text of a dependency previously acquired from a JSON node.
     * @return The pure id of the dependency.
     * @author Griefed
     */
    private fun getDependency(dependency: String): String {
        val dependencyIndex = dependency.lastIndexOf(":") + 1
        val dependencySubstring = dependency.substring(dependencyIndex)
        return dependencySubstring.replace(dependencyReplace, "")
    }

    /**
     * Check whether the passed mod id is present as a dependency in any of the mods dependencies. If
     * it is, then the mod of the modId is required.
     *
     * @param child The child-JSON node containing dependency information.
     * @param modId The ID of the mod for which to check for dependencies.
     * @return `true` if the modId is a dependency.
     * @author Griefed
     */
    private fun additionalDependenciesDepend(child: JsonNode, modId: String): Boolean {
        var depends = false
        val dependencies: Array<String> = utilities.jsonUtilities
            .getNestedTexts(child, ";", values, dependencies, value)
        for (dependency in dependencies) {
            if (!dependency.matches(dependencyCheck)) {
                continue
            }
            val dependencyIndex = dependency.lastIndexOf(":") + 1
            val dependencySubstring = dependency.substring(dependencyIndex)
            val checked = dependencySubstring.replace(additionalDependencyRegex, "")
            if (checked == modId) {
                depends = true
            }
        }
        return depends
    }

    /**
     * Check whether the passed mod id is present as a dependency. If it is, then the mod of the modId
     * is required.
     *
     * @param child The child-JSON node containing dependency information.
     * @param modId The ID of the mod for which to check for dependencies.
     * @return `true` if the modId is a dependency.
     * @author Griefed
     */
    private fun additionalDependencyDepends(child: JsonNode, modId: String): Boolean {
        var depends = false
        if (utilities.jsonUtilities.nestedTextMatches(child, dependencyCheck, values, dependencies, value)) {
            val dependencies: String = utilities.jsonUtilities.getNestedText(child, values, dependencies, value)
            val dependencyIndex = dependencies.lastIndexOf(":") + 1
            val dependencySubstring = dependencies.substring(dependencyIndex)
            val dependency = dependencySubstring.replace(additionalDependencyRegex, "")
            if (dependency == modId) {
                depends = true
            }
        }
        return depends
    }

    /**
     * Check whether the additional mod is a clientside-only mod.
     *
     * @param node            The JSON-node containing information about the additional mod.
     * @param additionalModId The ID of the additional mod
     * @return `true` if the additional mod is clientside-only.
     * @author Griefed
     */
    private fun isAdditionalModClientSide(node: JsonNode, additionalModId: String): Boolean {
        var clientSide = false
        try {
            // iterate though annotations
            for (children in node.get(annotations)) {
                try {
                    if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(
                            children,
                            additionalModId,
                            values,
                            modid,
                            value
                        )
                        && utilities.jsonUtilities.getNestedBoolean(children, values, clientSideOnly, value)
                    ) {
                        clientSide = true
                    }
                } catch (_: NullPointerException) {
                    // This annotation has no matching modId / no clientSideOnly flag -> it does not
                    // mark the additional mod client-side, so leave clientSide false.
                } catch (_: JsonException) {
                    // Malformed annotation entry -> leave clientSide false.
                }
            }
        } catch (_: NullPointerException) {
            // This node has no "annotations" array -> nothing to inspect, leave clientSide false.
        }
        return clientSide
    }
}