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
 * `quilt.mod.json`-based scanning of Quilt-Minecraft mods.
 *
 * If `minecraft.environment` specifies `client`, and the mod is not listed as a dependency of
 * another mod, it is later excluded from the server pack. Sideness and id reading live in
 * [FabricFamilyScanner], which Fabric shares; only Quilt's `depends`-block shape is specific to this
 * scanner.
 *
 * @param objectMapper For JSON-parsing.
 * @param utilities    Common utilities used across ServerPackCreator.
 *
 * @author Griefed
 */
class QuiltScanner(objectMapper: ObjectMapper, utilities: Utilities) : FabricFamilyScanner(
    descriptor = "quilt.mod.json",
    idPath = arrayOf(QUILT_LOADER, "id"),
    environmentPath = arrayOf("minecraft", "environment"),
    objectMapper = objectMapper,
    utilities = utilities
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val depends = "depends"

    override val scanAnnouncement = "Scanning Quilt mods for sideness..."

    /** Dependency ids that are the platform rather than a mod, so they never pull a jar into the keep-list. */
    val dependencyExclusions: Regex
        get() = "(quilt_loader|quilt_base|quilted_fabric_api|java|minecraft)".toRegex()

    /**
     * Quilt declares `quilt_loader.depends` as an array whose entries are either an object carrying
     * an `id`, or the bare id as a string — both forms occur in the wild, so both are read. A
     * descriptor without the block declares no dependencies and yields an empty list.
     */
    override fun readDependencies(modConfig: JsonNode, modId: String): List<ModDependency> {
        val modDependencies = mutableListOf<ModDependency>()
        try {
            for (dependency in utilities.jsonUtilities.getNestedElement(modConfig, QUILT_LOADER, depends)) {
                try {
                    val dependencyId = if (dependency.isContainerNode) {
                        utilities.jsonUtilities.getNestedText(dependency, "id")
                    } else {
                        dependency.asText()
                    }
                    if (!dependencyId.matches(dependencyExclusions)) {
                        log.debug("Added dependency $dependencyId for $modId.")
                        modDependencies.add(ModDependency(dependencyId))
                    }
                } catch (_: NullPointerException) {
                    log.debug("No dependencies for $modId.")
                }
            }
        } catch (_: NullPointerException) {
            // No "depends" block in this quilt.mod.json -> the mod declares no
            // dependencies, so there is nothing to record.
        }
        return modDependencies
    }

    private companion object {
        /** The descriptor block Quilt nests a mod's own identity and dependencies under. */
        const val QUILT_LOADER = "quilt_loader"
    }
}
