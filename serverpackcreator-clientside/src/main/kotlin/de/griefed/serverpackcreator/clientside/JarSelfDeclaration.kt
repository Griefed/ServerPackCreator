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

import com.electronwill.nightconfig.toml.TomlParser
import de.griefed.serverpackcreator.api.config.SupportedModloaders
import de.griefed.serverpackcreator.api.modscanning.LoaderDescriptors
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

    /** Where Forge keeps its descriptor — the one entry this object reads rather than merely lists. */
    private const val FORGE_DESCRIPTOR = LoaderDescriptors.FORGE_TOML

    /** The `mods.toml` table free-form mod properties live under. */
    private const val TOML_PROPERTIES = "properties"

    /** The property Sinytra Connector stamps into a wrapped Fabric mod's stub descriptor. */
    private const val CONNECTOR_PLACEHOLDER_PROPERTY = "connector:placeholder"

    /**
     * The loaders a descriptor can evidence on [minecraftVersion] — the canonical names
     * [SupportedModloaders] spells, minus any whose descriptor set is empty there.
     *
     * A loader outside this set can never be refused (see [contradictingLoaders]' last accept arm), which is
     * exactly right for `LegacyFabric`: it reads Fabric's descriptor, so no jar can carry evidence against
     * it, and `LoaderCompatibility` already accepts a Fabric jar for its boot.
     */
    private fun declaringLoaders(minecraftVersion: String): Set<String> =
        SupportedModloaders.names.filterTo(mutableSetOf()) {
            LoaderDescriptors.descriptorsFor(it, minecraftVersion).isNotEmpty()
        }

    /**
     * The loaders whose descriptors [jar] carries, **as read on [minecraftVersion]**. Empty when the jar
     * cannot be opened, has no descriptor, or is not an archive at all — all of which mean *"this says
     * nothing"*, never *"this says no"*.
     *
     * **The Minecraft version is part of the question, not a refinement of it.** Which file a loader reads
     * has changed twice, and [LoaderDescriptors] is the one place that knows when — so on Minecraft 1.20.4 a
     * `META-INF/mods.toml` names **both** Forge and NeoForge, because both read it there and its presence
     * therefore distinguishes nothing. This object used to hold its own flat, version-blind map and read
     * every `mods.toml` as Forge's, which refused 13 genuine NeoForge jars on 1.20.2–1.20.4.
     *
     * One jar can name several loaders two ways, and they are different: a genuine multi-loader jar carries
     * several descriptors, while an *ambiguous* one carries a single file that several loaders read.
     */
    fun declaredLoaders(jar: File, minecraftVersion: String): Set<String> = runCatching {
        ZipFile(jar).use { archive ->
            declaringLoaders(minecraftVersion).filterTo(mutableSetOf()) { loader ->
                LoaderDescriptors.descriptorsFor(loader, minecraftVersion).any { archive.getEntry(it) != null }
            }
        }
    }.getOrDefault(emptySet())

    /**
     * Whether [jar] is a **Sinytra Connector placeholder** — a Fabric mod wrapped so a platform can tag it
     * Forge, whose `META-INF/mods.toml` is a stub existing only to get the file past Forge's mod discovery
     * until Connector takes it over.
     *
     * **Keyed on the marker, never on carrying two descriptors.** A genuine multi-loader jar ships a real
     * `mods.toml` beside a real `fabric.mod.json` and each speaks for its own loader; only
     * `[properties] "connector:placeholder" = true` says *"the Forge descriptor here is not the mod"*.
     *
     * Fails toward `false` like everything else in this object: an unopenable jar, an absent descriptor or a
     * `mods.toml` the parser chokes on all mean *"nothing said so"*.
     */
    fun isConnectorPlaceholder(jar: File): Boolean = runCatching {
        ZipFile(jar).use { archive ->
            val descriptor = archive.getEntry(FORGE_DESCRIPTOR) ?: return false
            // Addressed as a path rather than by walking `valueMap()`: the key carries a colon, not a dot,
            // so nightconfig's own path splitting cannot mistake it for two segments.
            archive.getInputStream(descriptor).use { TomlParser().parse(it) }
                .get<Any?>(listOf(TOML_PROPERTIES, CONNECTOR_PLACEHOLDER_PROPERTY)) == true
        }
    }.getOrDefault(false)

    /**
     * The loaders [jar]'s descriptors name when **none** of them can run under [loader] on
     * [minecraftVersion] — i.e. the set a caller may re-select from — or empty when there is no such
     * disagreement.
     *
     * Split out of [contradiction] so the *rule* has one home: a caller answering the mismatch by verifying
     * the jar under the loader it really declares needs the same acceptability question the refusal asked,
     * and a second copy of it would be free to drift into accepting what the gate refuses.
     */
    fun contradictingLoaders(jar: File, loader: String, minecraftVersion: String): Set<String> {
        val declared = declaredLoaders(jar, minecraftVersion)
        // The cross-loading claim is [LoaderCompatibility]'s, and it needs the Minecraft version: NeoForge
        // loads a Forge jar on 1.20.1 and on nothing else, so asking without one can only be wrong twice.
        // That is a claim about the *jar* loading unchanged, and stays separate from which descriptor a
        // loader reads -- NeoForge's package rename (1.20.2) and its descriptor rename (1.20.5) are two
        // different dates, and merging them is what made this gate wrong.
        val acceptable = declared.isEmpty() ||
            loader in declared ||
            LoaderCompatibility.alsoRuns(loader, minecraftVersion).any { it in declared } ||
            loader !in declaringLoaders(minecraftVersion)
        return if (acceptable) emptySet() else declared
    }

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
        val mismatched = contradictingLoaders(jar, loader, minecraftVersion)
        if (mismatched.isNotEmpty()) {
            return "${jar.name} carries only ${mismatched.sorted().joinToString("/")} descriptor(s), " +
                "so it is not a $loader mod"
        }
        // VersionConstraint fails toward accept on its own, so an unparseable range never reaches a refusal.
        if (minecraftConstraint != null && !VersionConstraint.satisfies(minecraftVersion, minecraftConstraint)) {
            return "${jar.name} declares Minecraft '$minecraftConstraint', but the pack is $minecraftVersion"
        }
        return null
    }
}
