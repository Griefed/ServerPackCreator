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
        // The era boundaries are `LoaderDescriptors`' to state, not this class's: the clientside engine's
        // pre-boot gate has to answer the same question about the same jars, and it used to hold a second,
        // version-blind copy that read every `mods.toml` as Forge's.
        "Forge" ->
            if (LoaderDescriptors.forgeUsesToml(minecraftVersion)) forgeTomlScanner else forgeAnnotationScanner

        "NeoForge" ->
            if (LoaderDescriptors.neoForgeUsesNeoToml(minecraftVersion)) neoForgeTomlScanner else forgeTomlScanner

        else -> null
    }
}
