/* Copyright (C) 2025 Griefed
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
import java.io.IOException
import java.util.*

/**
 * `fml-cache-annotation.json`-based scanning of Forge-Minecraft mods of older versions.
 *
 * @param objectMapper For JSON-parsing.
 * @param utilities    Common utilities used across ServerPackCreator.
 *
 * @author Griefed
 */
class ForgeAnnotationScanner(
    private val objectMapper: ObjectMapper,
    private val utilities: Utilities
) : JsonBasedScanner(), Scanner<Pair<Collection<File>, Collection<Pair<String,String>>>, Collection<File>> {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val additionalDependencyRegex = "(@.*|\\[.*)".toRegex()
    private val caches = "META-INF/fml_cache_annotation.json"
    private val annotations = "annotations"
    private val jar = "jar"
    private val values = "values"
    private val modid = "modid"
    private val value = "value"
    private val clientSideOnly = "clientSideOnly"
    private val dependencies = "dependencies"

    val dependencyCheck: Regex
        get() = "(before:.*|after:.*|required-after:.*|)".toRegex()
    val dependencyReplace: Regex
        get() = "(@.*|\\[.*)".toRegex()

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
    override fun scan(jarFiles: Collection<File>):  Pair<Collection<File>, Collection<Pair<String,String>>> {
        log.info("Scanning Minecraft 1.12.x and older mods for sideness...")
        val modDependencies = ArrayList<Pair<String, Pair<String, String>>>()
        val clientMods = TreeSet<String>()

        /*
         * Go through all mods in our list and acquire a list of clientside-only mods as well as any
         * dependencies of the mods.
         */
        checkForClientModsAndDeps(jarFiles, clientMods, modDependencies)

        //Remove any dependency from our list of clientside-only mods, so we do not exclude any dependency.
        cleanupClientMods(modDependencies, clientMods)

        /*
         * After removing dependencies from the list of potential clientside mods, we can check whether
         * any of the remaining clientmods is available in our list of files. The resulting set is the
         * set of mods we can safely exclude from our server pack.
         */
        return Pair(
            getModsDelta(jarFiles, clientMods),
            modDependencies.map { entry ->
                Pair(
                    entry.first,
                    "${entry.second.first} (${entry.second.second})"
                )
            })
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
            var modId: String? = null
            val additionalMods = TreeSet<String>()
            try {
                val modJson: JsonNode = getJarJson(mod, caches, objectMapper)

                // base of json
                for (node in modJson) {
                    try {
                        // iterate though annotations
                        val cacheAnnotations = node.get(annotations)
                        for (child in cacheAnnotations) {

                            // Get the mod ID and check for clientside only, if we have not yet received a
                            // modID
                            if (modId == null) {
                                try {
                                    modId = getModId(child)
                                    // Get the modId

                                    // Add mod to list of clientmods if clientSideOnly is true
                                    checkForClientSide(child, modId, clientMods)
                                } catch (ignored: NullPointerException) {
                                } catch (ignored: JsonException) {
                                }

                                // We already received a modId, perform additional checks to prevent false
                                // positives
                            } else {
                                try {
                                    // Get the additional modID
                                    checkAdditionalId(child, modId, clientMods, additionalMods)
                                } catch (ignored: NullPointerException) {
                                }
                            }

                            // Get dependency modIds
                            checkDependencies(child, modDependencies, mod.name, modId!!)
                        }
                    } catch (ignored: NullPointerException) {
                    }
                }
                if (!additionalMods.isEmpty()) {
                    checkAdditionalMods(modId!!, additionalMods, modJson, clientMods)
                }
            } catch (ex: NullPointerException) {
                log.warn("Couldn't scan $mod as it contains no fml_cache_annotation.json.")
            } catch (ex: Exception) {
                log.error("Couldn't scan $mod", ex)
            }

        }
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

    /**
     * Check whether the mod is clientside only.
     *
     * @param jsonNode   The JSON node containing information about the sideness.
     * @param modId      The id of the mod.
     * @param clientMods Set to the `modId` if the mod is clientside-only.
     * @throws NullPointerException if the JSON node does not contain sideness information.
     * @throws JsonException        if the text in the boolean-field is neither `true` nor
     * `false`.
     * @author Griefed
     */
    @Throws(NullPointerException::class, JsonException::class)
    private fun checkForClientSide(jsonNode: JsonNode, modId: String, clientMods: TreeSet<String>) {
        if (utilities.jsonUtilities.getNestedBoolean(jsonNode, values, clientSideOnly, value)) {
            clientMods.add(modId)
            log.debug("Added clientMod: $modId")
        }
    }

    /**
     * Compare the additional id in a JSON node for match with the parent modId. If the additional id
     * is the same as the parent id, check for sideness and add it to our set of clientMods, otherwise
     * add the id to our set of additionalMods.
     *
     * @param child          JSON node containing information about the additional id.
     * @param modId          The id of the parent mod.
     * @param clientMods     Set containing our clientside-only mod ids.
     * @param additionalMods Set containing our additional mod ids.
     * @throws NullPointerException if the JSON node contains no additional mod id.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    private fun checkAdditionalId(
        child: JsonNode,
        modId: String,
        clientMods: TreeSet<String>,
        additionalMods: TreeSet<String>
    ) {
        if (!utilities.jsonUtilities.nestedTextIsEmpty(child, values, modid, value)) {

            // ModIDs are the same, so check for clientside-only
            if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(child, modId, values, modid, value)
            ) {
                try {
                    // Add mod to list of clientmods if clientSideOnly is true
                    if (utilities.jsonUtilities.getNestedBoolean(child, values, clientSideOnly, value)
                    ) {
                        clientMods.add(modId)
                        log.debug("Added clientMod: $modId")
                    }
                } catch (ignored: NullPointerException) {
                } catch (ignored: JsonException) {
                }

                // ModIDs are different, possibly two mods in one JAR-file.......
            } else {

                // Add additional modId to list, so we can check those later
                additionalMods.add(
                    utilities.jsonUtilities.getNestedText(child, values, modid, value)
                )
            }
        }
    }

    /**
     * Check the dependencies of our mod for sideness. Any dependency that is not `forge`, and
     * whose sideness is clientside-only, gets added to the list of required dependencies.
     *
     * @param child           JSON node containing information about our dependencies.
     * @param modDependencies Set containing our dependency ids.
     * @param modFileName     The filename of the mod being checked.
     * @author Griefed
     */
    private fun checkDependencies(child: JsonNode, modDependencies: ArrayList<Pair<String, Pair<String, String>>>, modFileName: String, modId: String) {
        try {
            if (!utilities.jsonUtilities.nestedTextIsEmpty(child, values, dependencies, value)) {

                // There are multiple dependencies for this mod
                if (utilities.jsonUtilities
                        .nestedTextContains(child, ";", values, dependencies, value)
                ) {
                    val dependencies: Array<String> = utilities.jsonUtilities
                        .getNestedTexts(
                            child, ";", values, dependencies,
                            value
                        )
                    for (dependency in dependencies) {
                        if (dependency.matches(dependencyCheck)) {
                            addDependency(getDependency(dependency), child, modDependencies, modFileName, modId)
                        }
                    }

                    // There is only one dependency, or it is a regular minecraft/forge dependency.
                } else {
                    if (utilities.jsonUtilities.nestedTextMatches(
                            child,
                            dependencyCheck,
                            values, dependencies, value
                        )
                    ) {
                        val dependencies: String = utilities.jsonUtilities
                            .getNestedText(child, values, dependencies, value)
                        val dependency = getDependency(dependencies)
                        addDependency(dependency, child, modDependencies, modFileName, modId)
                    }
                }
            }
        } catch (ignored: NullPointerException) {
        }
    }

    /**
     * Check for additional mods in the mod-jar. Sometimes, a single mod-jar can contain multiple mods
     * at once.
     *
     * @param modId          The id of the parent mod.
     * @param additionalMods A set of additional mod ids found so far, to which any additional mod
     * will be added to.
     * @param modJson        The JsonNode containing all relevant information about any additional
     * mods.
     * @param clientMods     A set of already discovered clientside-only mods, to which any additional
     * mod will be added to.
     * @author Griefed
     */
    private fun checkAdditionalMods(
        modId: String,
        additionalMods: TreeSet<String>,
        modJson: JsonNode,
        clientMods: TreeSet<String>
    ) {
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
                            if (utilities.jsonUtilities
                                    .nestedTextEqualsIgnoreCase(
                                        child,
                                        additionalModId,
                                        values, modid, value
                                    )
                                &&
                                !utilities.jsonUtilities
                                    .nestedTextIsEmpty(
                                        child,
                                        values, dependencies, value
                                    )
                            ) {
                                if (utilities.jsonUtilities
                                        .nestedTextContains(child, ";", values, dependencies, value)
                                ) {
                                    if (additionalDependenciesDepend(child, modId)) {
                                        additionalModDependsOnFirst = true
                                    }
                                } else {
                                    if (additionalDependencyDepends(child, modId)) {
                                        additionalModDependsOnFirst = true
                                    }
                                }
                            }
                        } catch (ignored: NullPointerException) {
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
                            if (!isAdditionalModClientSide(node, additionalModId)) {
                                if (clientMods.removeIf { n: String -> n == modId }) {
                                    log.info(
                                        "Removing $modId from list of clientside-only mods. It contains multiple mods at once, and one of them is NOT clientside-only."
                                    )
                                }
                            }
                        }
                    }
                } catch (ignored: NullPointerException) {
                }
            }
        }
    }

    /**
     * Check whether the mod-jar should be added to the modsDelta list.
     *
     * @param file       The mod-jar to check.
     * @param clientMods A set of modIds of clientside-only mods already discovered previously..
     * @return `true` if the modJar can be added to the modsDelta set.
     * @throws IOException if the fml_cache_annotation could not be read.
     * @author Griefed
     */
    @Throws(IOException::class)
    private fun addToDelta(file: File, clientMods: TreeSet<String>): Boolean {
        val modJson: JsonNode = getJarJson(file, caches, objectMapper)
        var addToDelta = false
        for (node in modJson) {
            try {
                // iterate though annotations
                val cacheAnnotations = node.get(annotations)
                for (child in cacheAnnotations) {

                    // Get the modId
                    try {
                        val modIdToCheck = getModId(child)

                        // Add mod to list of clientmods if clientSideOnly is true
                        if (utilities.jsonUtilities.getNestedBoolean(child, values, clientSideOnly, value)) {
                            if (clientMods.contains(modIdToCheck)) {
                                addToDelta = true
                            }
                        }
                    } catch (ignored: NullPointerException) {
                    } catch (ignored: JsonException) {
                    }
                }
            } catch (ignored: NullPointerException) {
            }
        }
        return addToDelta
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
     * Add a dependency to our set of dependencies.
     *
     * @param dependency      The dependency to add
     * @param child           The JSON node containing information about dependencies and ids.
     * @param modDependencies The set of dependencies to add the new dependency to.
     * @param modFileName     The filename of the mod being checked.
     * @author Griefed
     */
    private fun addDependency(
        dependency: String,
        child: JsonNode,
        modDependencies: ArrayList<Pair<String, Pair<String, String>>>,
        modFileName: String,
        modId: String
    ) {
        val pair: Pair<String, Pair<String, String>>
        if (!dependency.equals("forge", ignoreCase = true) && dependency != "*") {
            pair = Pair(dependency, Pair(modFileName, modId))
            if (modDependencies.add(pair)) {
                try {
                    val addedFor = utilities.jsonUtilities.getNestedText(child, values, modid, value)
                    log.debug("Added dependency ${pair.first} for $addedFor (${pair.second.first}).")
                } catch (ex: NullPointerException) {
                    log.debug("Added dependency ${pair.first} (${pair.second.first}).")
                }
            }
        }
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
                } catch (ignored: NullPointerException) {
                } catch (ignored: JsonException) {
                }
            }
        } catch (ignored: NullPointerException) {
        }
        return clientSide
    }

    override fun getModsDelta(filesInModsDir: Collection<File>, clientMods: TreeSet<String>): TreeSet<File> {
        val modsDelta = TreeSet<File>()
        for (mod in filesInModsDir) {
            try {
                if (addToDelta(mod, clientMods)) {
                    modsDelta.add(mod)
                }
            } catch (ignored: Exception) {
            }
        }
        return modsDelta
    }
}