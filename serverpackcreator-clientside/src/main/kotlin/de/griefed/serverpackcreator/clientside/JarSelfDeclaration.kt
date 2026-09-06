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

import java.io.File
import java.util.zip.ZipFile

/**
 * Asks the staged jar what it says about itself, and refuses a boot the jar's own descriptor contradicts.
 *
 * **Why the platform cannot be trusted for this.** A platform's loader and Minecraft sets are what an author
 * ticked, and `BootCandidateSelector` trusts them absolutely: it boots the *newest* Minecraft in the set,
 * and where one file claims two loaders it takes whichever the platform listed first. Measured against the
 * live grinder on 2026-08-31, that booted `DamageVignette-2.0.2-forge+mc1.20.jar` under **NeoForge**
 * (`Missing language javafml version [46,)`) and `create_ltab` on **Minecraft 1.20.6** against older
 * mappings (`@Inject … could not find any targets`). Both died, both were scored as sideness evidence, and
 * neither run had anything to do with the mod being client-only.
 *
 * **Fail toward ACCEPT — this is the whole design, not a detail.** An unreadable jar, an absent descriptor,
 * an unparseable constraint, a loader this does not recognise: every one of them boots. Only a *positive,
 * readable* contradiction refuses. A gate that refused on doubt would turn a gap in descriptor coverage into
 * a catalog-wide mass-INCONCLUSIVE event — the same shape [VersionConstraint]'s own documentation warns
 * about, and the shape a `LoaderSupportMemory` once produced by marking Fabric unusable for 22 Minecraft
 * versions within minutes.
 *
 * @author Griefed
 */
object JarSelfDeclaration {

    /** Descriptor path → the loader that reads it. Presence only; the contents are `-api`'s business. */
    private val descriptorLoaders = mapOf(
        "fabric.mod.json" to "Fabric",
        "quilt.mod.json" to "Quilt",
        "META-INF/mods.toml" to "Forge",
        "META-INF/neoforge.mods.toml" to "NeoForge"
    )

    /**
     * The loaders whose descriptors [jar] carries. Empty when the jar cannot be opened, has no descriptor, or
     * is not an archive at all — all of which mean *"this says nothing"*, never *"this says no"*.
     */
    fun declaredLoaders(jar: File): Set<String> = runCatching {
        ZipFile(jar).use { archive ->
            descriptorLoaders.filterKeys { archive.getEntry(it) != null }.values.toSet()
        }
    }.getOrDefault(emptySet())

    /** Not implemented yet — see `JarSelfDeclarationTest.aConnectorPlaceholderNamesItselfInItsModsToml`. */
    fun isConnectorPlaceholder(jar: File): Boolean = TODO("the placeholder marker is not read yet")

    /**
     * Why [jar] must not be booted as [loader] on [minecraftVersion], or `null` to go ahead.
     *
     * [minecraftConstraint] is the jar's own declared Minecraft range, from
     * `ScannedMod.minecraftConstraint`; `null` means the descriptor stated none, which is ordinary and
     * accepts. Both checks only ever fire on a *positive* disagreement — see the class doc.
     */
    fun contradiction(
        jar: File,
        loader: String,
        minecraftVersion: String,
        minecraftConstraint: String?
    ): String? {
        val declared = declaredLoaders(jar)
        // The cross-loading claim is [LoaderCompatibility]'s, and it needs the Minecraft version: NeoForge
        // loads a Forge jar on 1.20.1 and on nothing else, so asking without one can only be wrong twice.
        val acceptable = declared.isEmpty() ||
            loader in declared ||
            LoaderCompatibility.alsoRuns(loader, minecraftVersion).any { it in declared } ||
            loader !in descriptorLoaders.values
        if (!acceptable) {
            return "${jar.name} carries only ${declared.sorted().joinToString("/")} descriptor(s), " +
                "so it is not a $loader mod"
        }
        // VersionConstraint fails toward accept on its own, so an unparseable range never reaches a refusal.
        if (minecraftConstraint != null && !VersionConstraint.satisfies(minecraftVersion, minecraftConstraint)) {
            return "${jar.name} declares Minecraft '$minecraftConstraint', but the pack is $minecraftVersion"
        }
        return null
    }
}
