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

/**
 * `fabric.mod.json`-based scanning of Fabric-Minecraft mods.
 *
 * If `environment` specifies `client`, and the mod is not listed as a dependency of another mod, it
 * is later excluded from the server pack. Sideness and id reading live in [FabricFamilyScanner],
 * which Quilt shares; only Fabric's `depends`-block shape is specific to this scanner.
 *
 * @param objectMapper For JSON-parsing.
 * @param utilities    Common utilities used across ServerPackCreator.
 *
 * @author Griefed
 */
class FabricScanner(objectMapper: ObjectMapper, utilities: Utilities) : FabricFamilyScanner(
    descriptor = "fabric.mod.json",
    idPath = arrayOf("id"),
    environmentPath = arrayOf("environment"),
    objectMapper = objectMapper,
    utilities = utilities
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val depends = "depends"

    /** The `provides` block: a flat array of ids. */
    private val provides = "provides"

    override val scanAnnouncement = "Scanning Fabric mods for sideness..."

    /**
     * Dependency ids that are the platform rather than a mod, so they never pull a jar into the keep-list.
     *
     * **`fabric` is deliberately NOT here.** It is Fabric API — a mod, and the most-depended-on one in the
     * ecosystem — while `fabricloader` is the platform. Excluding it meant Fabric API could never be
     * reported as the dependency it is, nor rescued back into a pack that had disabled it.
     */
    private val dependencyExclusions: Regex
        get() = "(fabricloader|java|minecraft)".toRegex()

    /**
     * Fabric declares `depends` as an object keyed by mod id, so the ids are the block's field names.
     * A descriptor without the block declares no dependencies and yields an empty list.
     */
    override fun readDependencies(modConfig: JsonNode, modId: String): List<ModDependency> {
        val modDependencies = mutableListOf<ModDependency>()
        try {
            for (dependency in utilities.jsonUtilities.getFieldNames(modConfig, depends)) {
                log.debug("Checking dependency $dependency for $modId.")
                if (!dependency.matches(dependencyExclusions)) {
                    log.debug("Added dependency $dependency for $modId.")
                    modDependencies.add(
                        ModDependency(dependency, versionConstraint = constraintOf(modConfig.path(depends).path(dependency)))
                    )
                }
            }
        } catch (_: NullPointerException) {
            // No "depends" block in this fabric.mod.json -> the mod declares no
            // dependencies, so there is nothing to record.
        }
        return modDependencies
    }

    /**
     * The version constraint a `depends` entry states. Fabric allows either a single string or an array of
     * alternatives; an array is joined with ` || `, the same disjunction the format itself uses, so the
     * original meaning survives as text.
     */
    private fun constraintOf(value: JsonNode): String? = when {
        value.isTextual -> value.asText().takeIf { it.isNotBlank() }
        value.isArray -> value.mapNotNull { it.asText(null) }.filter { it.isNotBlank() }
            .joinToString(" || ").takeIf { it.isNotBlank() }
        else -> null
    }
    /**
     * Fabric declares `provides` as a flat array of ids the mod also answers to. Fabric API 0.92.11+1.20.1
     * uses it to answer to the historical `fabric` while calling itself `fabric-api`; the newest builds have
     * dropped the block, so its absence is normal and yields an empty list.
     */
    override fun readProvides(modConfig: JsonNode, modId: String): List<String> =
        modConfig.path(provides)
            .takeIf { it.isArray }
            ?.mapNotNull { entry -> entry.takeIf { it.isTextual }?.asText()?.takeIf { it.isNotBlank() } }
            .orEmpty()
            .also { aliases -> if (aliases.isNotEmpty()) log.debug("$modId also provides $aliases.") }
}
