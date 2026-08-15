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
package de.griefed.serverpackcreator.clientside

import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.modscanning.Sideness
import de.griefed.serverpackcreator.api.utilities.common.Comparison
import de.griefed.serverpackcreator.api.utilities.common.SemanticVersionComparator
import java.io.File

/**
 * Runs ServerPackCreator's own per-loader mod-scanners over a jar to read the sideness the mod
 * *declares* in its metadata (`fabric.mod.json`, `mods.toml`/`neoforge.mods.toml`, `quilt.mod.json`).
 * This is the jar-based half of the metadata signal; the loader→scanner dispatch mirrors
 * [de.griefed.serverpackcreator.api.serverpack.ModListCompiler] so results match a real generation.
 *
 * @param modScanner The bundle of per-loader scanners from [de.griefed.serverpackcreator.api.ApiWrapper].
 * @author Griefed
 */
class MetadataScanner(private val modScanner: ModScanner) {

    /** Outcome of scanning a single jar's declared sideness. */
    enum class Result {
        /** The jar declares itself client-only (would be excluded from a server pack). */
        CLIENT,

        /** The jar declares server/both, or no sideness at all (kept in a server pack). */
        SERVER_OR_BOTH,

        /** The jar could not be scanned (missing/malformed metadata). */
        ERROR
    }

    /**
     * Scan a single [jar] for the given [loader] and [minecraftVersion], returning whether the mod
     * declares itself client-only. The mod is [Result.CLIENT] when the loader's scanner places it in
     * its clientside-delta.
     */
    fun scan(jar: File, loader: String, minecraftVersion: String): Result {
        val files = listOf(jar)
        return try {
            val clientside: Collection<File> = when (loader) {
                "Fabric", "LegacyFabric" -> modScanner.fabricScanner.scan(files).filter { it.sideness == Sideness.CLIENT }.map { entry -> entry.file }

                "Quilt" -> modScanner.fabricScanner.scan(files).filter { it.sideness == Sideness.CLIENT }.map { entry -> entry.file } +
                        modScanner.quiltScanner.scan(files).filter { it.sideness == Sideness.CLIENT }.map { entry -> entry.file }

                "Forge" -> if (forgeUsesToml(minecraftVersion)) {
                    modScanner.forgeTomlScanner.scan(files).filter { it.sideness == Sideness.CLIENT }.map { entry -> entry.file }
                } else {
                    modScanner.forgeAnnotationScanner.scan(files).filter { it.sideness == Sideness.CLIENT }.map { entry -> entry.file }
                }

                "NeoForge" -> if (neoForgeUsesNeoToml(minecraftVersion)) {
                    modScanner.neoForgeTomlScanner.scan(files).filter { it.sideness == Sideness.CLIENT }.map { entry -> entry.file }
                } else {
                    modScanner.forgeTomlScanner.scan(files).filter { it.sideness == Sideness.CLIENT }.map { entry -> entry.file }
                }

                else -> emptyList()
            }
            if (clientside.contains(jar)) Result.CLIENT else Result.SERVER_OR_BOTH
        } catch (ex: Exception) {
            Result.ERROR
        }
    }

    /**
     * Whether Forge on [minecraftVersion] carries a `mods.toml` rather than the annotation-cache the
     * 1.12-and-older scanner reads. Forge switched with Minecraft 1.13.
     *
     * Compares every version component through [SemanticVersionComparator] rather than testing the
     * minor on its own: Minecraft has two versioning schemes (`1.x.y` and the newer `YY.x.y`), so
     * `26.2`'s minor of `2` reads as the 1.2 era and would pick the wrong scanner. An unparseable
     * version keeps the previous fallback to the modern scanner.
     */
    private fun forgeUsesToml(minecraftVersion: String) = runCatching {
        SemanticVersionComparator.compareSemantics(FORGE_TOML_MINIMUM_MINECRAFT, minecraftVersion, Comparison.EQUAL_OR_NEW)
    }.getOrDefault(true)

    /** NeoForge renamed `mods.toml` to `neoforge.mods.toml` starting with Minecraft 1.20.5. */
    private fun neoForgeUsesNeoToml(minecraftVersion: String): Boolean =
        SemanticVersionComparator.compareSemantics(NEOFORGE_TOML_MINIMUM_MINECRAFT, minecraftVersion, Comparison.EQUAL_OR_NEW)

    /** Minecraft versions at which a loader changed the descriptor its scanner has to read. */
    private companion object {
        /** Forge replaced the FML annotation-cache with `META-INF/mods.toml` in Minecraft 1.13. */
        const val FORGE_TOML_MINIMUM_MINECRAFT = "1.13"

        /** NeoForge renamed `mods.toml` to `META-INF/neoforge.mods.toml` in Minecraft 1.20.5. */
        const val NEOFORGE_TOML_MINIMUM_MINECRAFT = "1.20.5"
    }
}
