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

import de.griefed.serverpackcreator.api.utilities.common.Comparison
import de.griefed.serverpackcreator.api.utilities.common.SemanticVersionComparator

/**
 * Brings the per-loader mod-scanners to one place, and owns the rule for choosing between them.
 *
 * Which scanner reads a pack depends on the modloader *and* the Minecraft version, because loaders
 * have changed their descriptor over time. That rule lives here — see [scannerFor] — rather than at
 * the call-sites, so a pack's mod list and the clientside engine's metadata signal cannot disagree
 * about what a jar declared.
 *
 * @param forgeAnnotationScanner For scanning `fml-cache-annotation.json`
 * @param fabricScanner     For scanning `fabric.mod.json`
 * @param quiltScanner      For scanning `quilt.mod.json`
 * @param forgeTomlScanner       For scanning `mods.toml`
 *
 * @author Griefed
 */
class ModScanner(
    val forgeAnnotationScanner: ForgeAnnotationScanner,
    val fabricScanner: FabricScanner,
    val quiltScanner: QuiltScanner,
    val forgeTomlScanner: ForgeTomlScanner,
    /** Scanner for NeoForge jars, which moved the descriptor and so cannot reuse Forge's path. */
    val neoForgeTomlScanner: NeoForgeTomlScanner
) {
    /** Scans a Quilt pack, whose jars may carry either descriptor or both. */
    val quiltPackScanner = QuiltPackScanner(quiltScanner, fabricScanner)

    /**
     * The scanner that reads a pack on [modloader] and [minecraftVersion], or `null` when no scanner
     * knows the loader.
     *
     * A `null` is deliberately not an error: the caller decides what an unknown loader means, and
     * both of them keep every mod rather than silently producing an empty result.
     *
     * @param modloader        Canonical modloader name, as [de.griefed.serverpackcreator.api.config.SupportedModloaders] spells it.
     * @param minecraftVersion The pack's Minecraft version, which decides the descriptor era.
     */
    fun scannerFor(modloader: String, minecraftVersion: String): ModJarScanner? = when (modloader) {
        "LegacyFabric", "Fabric" -> fabricScanner
        "Quilt" -> quiltPackScanner
        "Forge" -> if (forgeUsesToml(minecraftVersion)) forgeTomlScanner else forgeAnnotationScanner
        "NeoForge" -> if (neoForgeUsesNeoToml(minecraftVersion)) neoForgeTomlScanner else forgeTomlScanner
        else -> null
    }

    /**
     * Whether Forge on [minecraftVersion] carries a `mods.toml` rather than the annotation-cache the
     * 1.12-and-older scanner reads. Forge switched with Minecraft 1.13.
     *
     * Compares every version component through [SemanticVersionComparator] rather than testing the
     * minor on its own: Minecraft has two versioning schemes (`1.x.y` and the newer `YY.x.y`), so
     * `26.2`'s minor of `2` reads as the 1.2 era and would pick the wrong scanner. An unparseable
     * version falls back to the modern scanner — the annotation cache exists only in jars a decade
     * old, so it is never the safer guess.
     */
    private fun forgeUsesToml(minecraftVersion: String) = runCatching {
        SemanticVersionComparator.compareSemantics(FORGE_TOML_MINIMUM_MINECRAFT, minecraftVersion, Comparison.EQUAL_OR_NEW)
    }.getOrDefault(true)

    /** Whether NeoForge on [minecraftVersion] uses `neoforge.mods.toml` rather than Forge's `mods.toml`. */
    private fun neoForgeUsesNeoToml(minecraftVersion: String) =
        SemanticVersionComparator.compareSemantics(NEOFORGE_TOML_MINIMUM_MINECRAFT, minecraftVersion, Comparison.EQUAL_OR_NEW)

    /** Minecraft versions at which a loader changed the descriptor its scanner has to read. */
    private companion object {
        /** Forge replaced the FML annotation-cache with `META-INF/mods.toml` in Minecraft 1.13. */
        const val FORGE_TOML_MINIMUM_MINECRAFT = "1.13"

        /** NeoForge renamed `mods.toml` to `META-INF/neoforge.mods.toml` in Minecraft 1.20.5. */
        const val NEOFORGE_TOML_MINIMUM_MINECRAFT = "1.20.5"
    }
}