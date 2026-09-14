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
 * **Which descriptor file a modloader reads, at a given Minecraft version.** The single home for that fact.
 *
 * Loaders have moved their descriptor twice in ways that matter, and the version is part of the answer rather
 * than a detail of it:
 *
 * | Loader | Minecraft | Descriptor |
 * |---|---|---|
 * | Fabric | any | `fabric.mod.json` |
 * | LegacyFabric | any | — (reads Fabric's, so nothing distinguishes it) |
 * | Quilt | any | `quilt.mod.json` |
 * | Forge | `< 1.13` | `mcmod.info`, `META-INF/fml_cache_annotation.json` |
 * | Forge | `>= 1.13` | `META-INF/mods.toml` |
 * | NeoForge | `< 1.20.5` | `META-INF/mods.toml` (shared with Forge, so it distinguishes neither) |
 * | NeoForge | `>= 1.20.5` | `META-INF/neoforge.mods.toml` |
 *
 * **Why this exists as its own object.** [ModScanner.scannerFor] already owned the era boundaries, and
 * `serverpackcreator-clientside`'s pre-boot gate held a *second, version-blind* copy of the same knowledge —
 * a flat descriptor→loader map. It therefore read every `META-INF/mods.toml` as Forge's, and refused 13
 * genuine NeoForge jars on Minecraft 1.20.2–1.20.4 as "not a NeoForge mod" (measured on the live grinder,
 * 2026-09-10). That is the `MetadataScanner`/`ModListCompiler` drift this codebase has already paid for
 * twice: two copies of one fact, only one of them maintained. Both callers now ask here.
 *
 * **The two boundaries are different facts about NeoForge and must not be merged.** The *package* rename
 * (`net.minecraftforge` → `net.neoforged`) landed with Minecraft 1.20.2 and is what ends binary jar parity —
 * that claim lives in the clientside engine's `LoaderCompatibility` and stays one version wide. The
 * *descriptor* rename landed with 1.20.5 and is what this object is about. Between the two, Forge and
 * NeoForge read the same file, so its presence tells a reader **nothing** about which of them a jar is for.
 *
 * @author Griefed
 */
object LoaderDescriptors {

    /** Fabric's descriptor, at the archive root. LegacyFabric reads the same file. */
    const val FABRIC = "fabric.mod.json"

    /** Quilt's descriptor, at the archive root. */
    const val QUILT = "quilt.mod.json"

    /** Forge's descriptor from Minecraft 1.13 on, and NeoForge's up to 1.20.4. */
    const val FORGE_TOML = "META-INF/mods.toml"

    /** NeoForge's own descriptor, from Minecraft 1.20.5 on. */
    const val NEOFORGE_TOML = "META-INF/neoforge.mods.toml"

    /** The author-written descriptor of a pre-1.13 Forge mod. */
    const val FORGE_LEGACY = "mcmod.info"

    /**
     * The FML annotation cache a pre-1.13 Forge jar carries, which is what [ForgeAnnotationScanner] reads.
     *
     * Listed beside [FORGE_LEGACY] rather than instead of it because the two do not always travel together:
     * `SkyHanni-6.0.0-mc1.8.9.jar` ships `mcmod.info` and no annotation cache, so a reader that knew only
     * this path would still see nothing.
     */
    const val FORGE_ANNOTATIONS = "META-INF/fml_cache_annotation.json"

    /**
     * The descriptor paths whose presence **evidences that a jar was built for** [modloader] on
     * [minecraftVersion] — empty when nothing distinguishes that loader, and for a loader this does not know.
     *
     * **This is the gate's question, not the scanner's, and the two are not the same.** A scanner needs the
     * one file to *parse* ([ModScanner.scannerFor] asks [forgeUsesToml] / [neoForgeUsesNeoToml] for that).
     * A reader asking "was this jar built for NeoForge?" wants every file that would say so — which is why
     * NeoForge's set carries `neoforge.mods.toml` at **every** version. That file is NeoForge's whenever it
     * appears, even on a Minecraft where the loader would not have read it, and treating its presence as
     * meaningless there would let a NeoForge-only jar pass as a Forge mod.
     *
     * **`LegacyFabric` is deliberately empty.** It reads Fabric's `fabric.mod.json`, so a jar carrying one
     * says nothing that separates the two, and `LoaderCompatibility.alsoRuns` already accepts a Fabric jar
     * for a LegacyFabric boot. Returning `{FABRIC}` here would instead make every Fabric jar declare two
     * loaders and make LegacyFabric refusable on evidence it never had.
     *
     * Empty is deliberately not an error, matching [ModScanner.scannerFor]'s `null`: the caller decides what
     * an unrecognised loader means, and the pre-boot gate treats it as "no opinion", which accepts.
     *
     * @param modloader        Canonical modloader name, as
     *                         [de.griefed.serverpackcreator.api.config.SupportedModloaders] spells it.
     * @param minecraftVersion The Minecraft version being read for, which decides the descriptor era.
     */
    fun descriptorsFor(modloader: String, minecraftVersion: String): Set<String> = when (modloader) {
        "Fabric" -> setOf(FABRIC)
        "Quilt" -> setOf(QUILT)
        "Forge" -> if (forgeUsesToml(minecraftVersion)) setOf(FORGE_TOML) else setOf(FORGE_LEGACY, FORGE_ANNOTATIONS)
        // Its own descriptor always counts; Forge's counts too until 1.20.5, where both loaders read it and
        // its presence therefore distinguishes neither.
        "NeoForge" ->
            if (neoForgeUsesNeoToml(minecraftVersion)) setOf(NEOFORGE_TOML) else setOf(FORGE_TOML, NEOFORGE_TOML)

        else -> emptySet()
    }

    /**
     * Whether Forge on [minecraftVersion] carries a `mods.toml` rather than the annotation-cache the
     * 1.12-and-older scanner reads. Forge switched with Minecraft 1.13.
     *
     * Compares every version component through [SemanticVersionComparator] rather than testing the minor on
     * its own: Minecraft has two versioning schemes (`1.x.y` and the newer `YY.x.y`), so `26.2`'s minor of
     * `2` reads as the 1.2 era and would pick the wrong scanner. An unparseable version falls back to the
     * modern descriptor — the annotation cache exists only in jars a decade old, so it is never the safer
     * guess.
     */
    fun forgeUsesToml(minecraftVersion: String): Boolean = atLeast(FORGE_TOML_MINIMUM_MINECRAFT, minecraftVersion)

    /**
     * Whether NeoForge on [minecraftVersion] uses `neoforge.mods.toml` rather than Forge's `mods.toml`.
     *
     * Falls back to the **modern** descriptor for an unparseable version, for the same reason and by the same
     * route as [forgeUsesToml]. It used to throw instead: the comparison was unwrapped while Forge's was
     * wrapped, so `scannerFor("NeoForge", "26")` propagated an `ArrayIndexOutOfBoundsException` out of
     * `ModScanner` — and `"26"` is a legitimate shape under the newer scheme.
     */
    fun neoForgeUsesNeoToml(minecraftVersion: String): Boolean =
        atLeast(NEOFORGE_TOML_MINIMUM_MINECRAFT, minecraftVersion)

    /**
     * Whether [minecraftVersion] is [boundary] or newer, treating anything unreadable as newer.
     *
     * One helper for both eras so neither can lose the fallback the other has — which is exactly how the
     * NeoForge branch came to throw where the Forge branch returned.
     */
    private fun atLeast(boundary: String, minecraftVersion: String): Boolean = runCatching {
        SemanticVersionComparator.compareSemantics(boundary, minecraftVersion, Comparison.EQUAL_OR_NEW)
    }.getOrDefault(true)

    /** Minecraft versions at which a loader changed the descriptor its scanner has to read. */
    private const val FORGE_TOML_MINIMUM_MINECRAFT = "1.13"

    /** NeoForge renamed `mods.toml` to `META-INF/neoforge.mods.toml` in Minecraft 1.20.5. */
    private const val NEOFORGE_TOML_MINIMUM_MINECRAFT = "1.20.5"
}
