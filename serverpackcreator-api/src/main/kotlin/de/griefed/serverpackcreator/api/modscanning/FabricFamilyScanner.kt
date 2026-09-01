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
import java.io.File

/**
 * Base for the two scanners reading a Fabric-format descriptor: Fabric's own `fabric.mod.json` and
 * the `quilt.mod.json` Quilt derived from it.
 *
 * Both state sideness the same way — a single `environment` string that either says `client` or does
 * not — and both name the mod with an `id`. Only the *paths* to those fields differ (Quilt nests them
 * under `quilt_loader` and `minecraft`), so they are constructor parameters here rather than a second
 * copy of the logic. Dependencies are **not** shared: Fabric declares them as an object keyed by mod
 * id, Quilt as an array of either objects or bare strings, so [readDependencies] stays abstract.
 *
 * @param descriptor      Path of the descriptor inside the jar.
 * @param idPath          Path to the mod's id within the descriptor.
 * @param environmentPath Path to the declared environment within the descriptor.
 * @param objectMapper    For JSON-parsing.
 * @param utilities       Common utilities used across ServerPackCreator.
 */
abstract class FabricFamilyScanner(
    private val descriptor: String,
    private val idPath: Array<String>,
    private val environmentPath: Array<String>,
    private val objectMapper: ObjectMapper,
    protected val utilities: Utilities
) : JsonDescriptorScanner() {

    /** The value of `environment` that marks a mod client-only. */
    private val client = "client"

    /**
     * The non-platform mods this descriptor names as dependencies.
     *
     * Abstract because the two formats disagree on the shape of the block, not merely its path. A
     * descriptor declaring none must yield an empty list rather than throwing.
     *
     * @param modConfig The parsed descriptor.
     * @param modId     Id of the mod being read, for logging.
     */
    protected abstract fun readDependencies(modConfig: JsonNode, modId: String): List<ModDependency>

    /**
     * The other ids this descriptor says the mod answers to.
     *
     * Open rather than abstract, and empty by default, because a descriptor without the block is the
     * common case and must not be an error. Overridden where the loader has the concept.
     *
     * @param modConfig The parsed descriptor.
     * @param modId     Id of the mod being read, for logging.
     */
    protected open fun readProvides(modConfig: JsonNode, modId: String): List<String> = emptyList()

    /**
     * The Minecraft range this descriptor declares, or `null` when it declares none.
     *
     * Open rather than abstract, and `null` by default, because a descriptor stating nothing about Minecraft
     * is ordinary and must never be read as a contradiction — a caller comparing this against a pack's
     * version has to be able to tell "the jar says nothing" from "the jar says something else".
     */
    protected open fun readMinecraftConstraint(modConfig: JsonNode): String? = null

    final override fun read(modJar: File): ScannedMod {
        val modConfig: JsonNode = getJarJson(modJar, descriptor, objectMapper)
        val modId = utilities.jsonUtilities.getNestedText(modConfig, *idPath)
        return ScannedMod(
            modJar, modId, readSideness(modConfig), readDependencies(modConfig, modId),
            readProvides(modConfig, modId), readMinecraftConstraint(modConfig), descriptorRead = true
        )
    }

    /**
     * The side the descriptor declares: [Sideness.CLIENT] only when `environment` explicitly says
     * `client`.
     *
     * A descriptor with no environment entry has not declared itself client-only, and a mod nothing
     * could be determined about is kept — so every other outcome, the missing entry included, is
     * [Sideness.SERVER].
     */
    private fun readSideness(modConfig: JsonNode): Sideness = try {
        if (utilities.jsonUtilities.nestedTextEqualsIgnoreCase(modConfig, client, *environmentPath)) {
            Sideness.CLIENT
        } else {
            Sideness.SERVER
        }
    } catch (_: NullPointerException) {
        Sideness.SERVER
    }
}
