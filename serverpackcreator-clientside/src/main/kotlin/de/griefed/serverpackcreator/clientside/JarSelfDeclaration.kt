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

import com.electronwill.nightconfig.core.UnmodifiableConfig
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

    /**
     * The descriptors this object *reads* rather than merely lists, newest spelling first.
     *
     * Both, and only these two, because the placeholder marker lives in a TOML `[properties]` table and
     * these are the only two descriptors that have one — and because NeoForge renamed its file on Minecraft
     * 1.20.5, so which of the pair a wrapped jar carries is a fact about its Minecraft era, not about
     * whether it is wrapped. Version-blind on purpose: the marker means the same thing wherever it appears,
     * so asking [LoaderDescriptors.descriptorsFor] here would need a Minecraft version this question does
     * not have and would answer with a subset of what it must search.
     */
    private val PROPERTY_BEARING_DESCRIPTORS = listOf(LoaderDescriptors.NEOFORGE_TOML, LoaderDescriptors.FORGE_TOML)

    /** The `mods.toml` table free-form mod properties live under. */
    private const val TOML_PROPERTIES = "properties"

    /** The property Sinytra Connector stamps into a wrapped Fabric mod's stub descriptor. */
    private const val CONNECTOR_PLACEHOLDER_PROPERTY = "connector:placeholder"

    /** The `mods.toml` table a mod's dependency entries live under, either shape — see [dependencyEntriesIn]. */
    private const val TOML_DEPENDENCIES = "dependencies"

    /** The key naming what one dependency entry is about. */
    private const val TOML_MOD_ID = "modId"

    /** The key carrying one dependency entry's accepted range. */
    private const val TOML_VERSION_RANGE = "versionRange"

    /**
     * The mod id each loader answers to in a `mods.toml` dependency entry.
     *
     * NeoForge is two answers, not one: on the single Minecraft version it and Forge share builds it *is*
     * Forge — `forge-1.20.1-47.1.106-universal.jar` registers as `forge 47.1.106` — and everywhere after the
     * package rename it registers as `neoforge`. The era boundary is [LoaderCompatibility]'s single
     * statement, asked rather than restated, because a second copy of "1.20.1" is exactly the duplication
     * this module has already paid for three times.
     */
    private fun platformIdsFor(loader: String, minecraftVersion: String): Set<String> = when (loader) {
        "Forge" -> setOf("forge")
        "NeoForge" ->
            if ("Forge" in LoaderCompatibility.alsoRuns(loader, minecraftVersion)) setOf("forge")
            else setOf("neoforge")

        else -> emptySet()
    }

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
     *
     * **A Connector placeholder's TOML declares nothing**, which is a third way and the reason
     * [isConnectorPlaceholder] exists: the stub is there to get the file past Forge's mod discovery, not to
     * describe a Forge mod, so what the jar declares is whatever its *other* descriptor says. Only the
     * stub's own path is discounted — a placeholder that somehow carried a real second TOML would still
     * name that loader.
     */
    fun declaredLoaders(jar: File, minecraftVersion: String): Set<String> = runCatching {
        ZipFile(jar).use { archive ->
            // A Connector placeholder's TOML is a stub, so it declares nothing about the loader that reads
            // it -- see `isConnectorPlaceholder`. Read once, here, rather than re-opened per loader.
            val stubbedDescriptors = if (carriesPlaceholderMarker(archive)) PROPERTY_BEARING_DESCRIPTORS else emptyList()
            declaringLoaders(minecraftVersion).filterTo(mutableSetOf()) { loader ->
                LoaderDescriptors.descriptorsFor(loader, minecraftVersion)
                    .filterNot { it in stubbedDescriptors }
                    .any { archive.getEntry(it) != null }
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
     * **Both TOML spellings are searched, and that is the whole of the NeoForge half.** A wrapped jar for
     * Minecraft 1.20.5 or later stamps the identical marker into `META-INF/neoforge.mods.toml`, because that
     * is where NeoForge reads from — see [PROPERTY_BEARING_DESCRIPTORS].
     *
     * Fails toward `false` like everything else in this object: an unopenable jar, an absent descriptor or a
     * descriptor the parser chokes on all mean *"nothing said so"*. Note that an unparseable *first*
     * descriptor does not mask a marker in the second: each is parsed inside its own `runCatching`.
     */
    fun isConnectorPlaceholder(jar: File): Boolean = runCatching {
        ZipFile(jar).use { carriesPlaceholderMarker(it) }
    }.getOrDefault(false)

    /**
     * [isConnectorPlaceholder]'s question against an already-open [archive], so [declaredLoaders] can ask it
     * without a second open of the same file.
     */
    private fun carriesPlaceholderMarker(archive: ZipFile): Boolean =
        PROPERTY_BEARING_DESCRIPTORS.any { path ->
            val descriptor = archive.getEntry(path) ?: return@any false
            runCatching {
                // Addressed as a path rather than by walking `valueMap()`: the key carries a colon, not a
                // dot, so nightconfig's own path splitting cannot mistake it for two segments.
                archive.getInputStream(descriptor).use { TomlParser().parse(it) }
                    .get<Any?>(listOf(TOML_PROPERTIES, CONNECTOR_PLACEHOLDER_PROPERTY)) == true
            }.getOrDefault(false)
        }

    /**
     * The loader build [jar] demands of [loader] on [minecraftVersion], or `null` when it demands none.
     *
     * Read here rather than through `ForgeTomlScanner` because that scanner *consumes* the platform entry —
     * the `side` on it is what decides the mod's own sideness — and discards its `versionRange`, so the one
     * number this question needs never reaches a `ScannedMod`. Reading it here also keeps a `-clientside`
     * gate from putting a requirement on the published `-api`.
     *
     * Both descriptor spellings are searched and the first demand wins: a jar carries at most one real TOML,
     * and the pair exists only because NeoForge renamed the file. Every lookup addresses a single-element
     * path rather than `valueMap()`: the latter is deprecated upstream, and a list path cannot be split on a
     * `.` the way a string one can.
     */
    fun demandedLoaderVersion(jar: File, loader: String, minecraftVersion: String): String? {
        val platformIds = platformIdsFor(loader, minecraftVersion)
        if (platformIds.isEmpty()) {
            return null
        }
        return runCatching {
            ZipFile(jar).use { archive ->
                PROPERTY_BEARING_DESCRIPTORS.firstNotNullOfOrNull { path ->
                    val descriptor = archive.getEntry(path) ?: return@firstNotNullOfOrNull null
                    runCatching {
                        val config = archive.getInputStream(descriptor).use { TomlParser().parse(it) }
                        dependencyEntriesIn(config).firstNotNullOfOrNull { declared ->
                            declared.takeIf { entry ->
                                entry.get<Any?>(listOf(TOML_MOD_ID))?.toString()?.lowercase() in platformIds
                            }?.get<Any?>(listOf(TOML_VERSION_RANGE))?.toString()?.takeIf { it.isNotBlank() }
                        }
                    }.getOrNull()
                }
            }
        }.getOrNull()
    }

    /**
     * Every dependency entry in [config], flattened across both shapes a `mods.toml` uses: a bare
     * `[[dependencies]]` array, and the `[[dependencies.<modId>]]` table-of-arrays a multi-mod jar needs.
     * Which one an author wrote says nothing about the mod, so neither should this.
     */
    private fun dependencyEntriesIn(config: UnmodifiableConfig): List<UnmodifiableConfig> =
        when (val declared = config.get<Any?>(listOf(TOML_DEPENDENCIES))) {
            is Collection<*> -> declared.filterIsInstance<UnmodifiableConfig>()
            is UnmodifiableConfig ->
                declared.entrySet().map { it.getRawValue<Any?>() }
                    .filterIsInstance<Collection<*>>().flatten()
                    .filterIsInstance<UnmodifiableConfig>()

            else -> emptyList()
        }

    /**
     * Whether [loader], at the build [available] on [minecraftVersion], can satisfy what [jar] demands of
     * it. **Every uncertainty answers `true`**: an unknown build, no demand at all, an unparseable range —
     * [VersionConstraint] fails toward accept on its own, and so does the missing half here.
     */
    private fun satisfiesItsOwnDemand(
        jar: File,
        loader: String,
        minecraftVersion: String,
        available: String?
    ): Boolean {
        val demanded = demandedLoaderVersion(jar, loader, minecraftVersion) ?: return true
        return available == null || VersionConstraint.satisfies(available, demanded)
    }

    /**
     * The loaders [jar]'s descriptors name when **none** of them can run under [loader] on
     * [minecraftVersion] — i.e. the set a caller may re-select from — or empty when there is no such
     * disagreement.
     *
     * Split out of [contradiction] so the *rule* has one home: a caller answering the mismatch by verifying
     * the jar under the loader it really declares needs the same acceptability question the refusal asked,
     * and a second copy of it would be free to drift into accepting what the gate refuses.
     */
    fun contradictingLoaders(
        jar: File,
        loader: String,
        minecraftVersion: String,
        loaderVersionFor: (loader: String) -> String? = { null }
    ): Set<String> {
        val declared = declaredLoaders(jar, minecraftVersion)
        // A loader whose newest build on this Minecraft cannot satisfy what the jar demands *of that
        // loader* is not a loader this jar can run under, however plainly its descriptor names it.
        val reachable = declared.filterTo(mutableSetOf()) {
            satisfiesItsOwnDemand(jar, it, minecraftVersion, loaderVersionFor(it))
        }
        // Asked before the descriptor rule because it is the only one that can fire while the requested
        // loader IS declared. Never fires when nothing is reachable: there would be nothing to re-select
        // to, and throwing the candidate away is the expensive outcome, not the safe one.
        if (loader in declared && loader !in reachable && reachable.isNotEmpty()) {
            return reachable
        }
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
        minecraftConstraint: String?,
        loaderVersionFor: (loader: String) -> String? = { null }
    ): String? {
        val mismatched = contradictingLoaders(jar, loader, minecraftVersion, loaderVersionFor)
        if (mismatched.isNotEmpty()) {
            // Two refusals reach here and an operator has to tell them apart: the jar carries the wrong
            // descriptor, or it carries the right one and asks for a build that was never published.
            val demanded = demandedLoaderVersion(jar, loader, minecraftVersion)
            if (demanded != null && loader in declaredLoaders(jar, minecraftVersion)) {
                return "${jar.name} declares $loader '$demanded', but the newest build for Minecraft " +
                    "$minecraftVersion is ${loaderVersionFor(loader)}"
            }
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
