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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.util.*
import java.util.jar.JarFile

/**
 * Helper-class containing methods implemented and used by JSON-based scanners, like the
 * [ForgeAnnotationScanner], [FabricScanner] and [QuiltScanner].
 *
 * @author Griefed
 */
abstract class JsonBasedScanner {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Acquire a JsonNode from the specified file in the specified file.
     *
     * @param file         The file from which to get the JsonNode from.
     * @param entryInJar   The file in the jar from which to get the JsonNode form.
     * @param objectMapper The ObjectMapper with which to parse the Json to a JsonNode.
     * @return A JsonNode containing all information from the requested file in the specified jar.
     * @throws IOException           if the file could not be opened or read from, if the file could
     * not be read or if the json from the file could not be parsed into
     * a JsonNode.
     * @throws SecurityException     if an error occurs reading the file in the jar.
     * @throws IllegalStateException if an error occurs reading the file in the jar.
     * @throws NullPointerException  if the jar does not contain the specified entry.
     * @author Griefed
     */
    @Throws(NullPointerException::class, IOException::class, SecurityException::class, IllegalStateException::class)
    fun getJarJson(file: File, entryInJar: String, objectMapper: ObjectMapper): JsonNode {
        val jsonNode: JsonNode
        JarFile(file).use { jar ->
            jar.getInputStream(jar.getJarEntry(entryInJar)).use {
                jsonNode = objectMapper.readTree(it)
            }
        }
        return jsonNode
    }

    /**
     * Remove any dependency from the list of clientside-only mods to prevent excluding dependencies
     * of other mods, resulting in a potentially broken server pack.
     *
     * @param modDependencies A set of modIds of dependencies.
     * @param clientMods      A set of modIds of clientside-only mods.
     * @author Griefed
     */
    fun cleanupClientMods(modDependencies: ArrayList<Pair<String, Pair<String, String>>>, clientMods: TreeSet<String>) {
        for (dependency in modDependencies) {
            clientMods.removeIf { clientMod: String ->
                if (clientMod == dependency.first && !clientMods.contains(dependency.second.second)) {
                    log.info("$clientMod is a dependency for ${dependency.second.first} (${dependency.second.second}), therefor it was not automatically removed.")
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * Check every file and fill the `clientMods` and `modDependencies` sets with ids of
     * mods which are clientside-only or dependencies of a mod.
     *
     * @param filesInModsDir  Collection of files to check.
     * @param clientMods      Set of clientside-only mod-ids.
     * @param modDependencies Set dependencies of other mods.
     * @author Griefed
     */
    abstract fun checkForClientModsAndDeps(
        filesInModsDir: Collection<File>,
        clientMods: TreeSet<String>,
        modDependencies: ArrayList<Pair<String, Pair<String, String>>>
    )

    /**
     * Get a list of mod-jars which can safely be excluded from the server pack.
     *
     * @param filesInModsDir A collection of files from which to exclude mods.
     * @param clientMods     A set of modIds which are clientside-only.
     * @return A set of all files from the passed collection which can safely be excluded from the
     * server pack.
     * @author Griefed
     */
    abstract fun getModsDelta(filesInModsDir: Collection<File>, clientMods: TreeSet<String>): TreeSet<File>
}