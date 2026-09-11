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

/**
 * Pure file/version-selection logic for the boot-test, factored out of [BootVerifier] so it is
 * unit-testable without an [de.griefed.serverpackcreator.api.ApiWrapper] or a running server: which
 * file+Minecraft-version to boot for a loader, and which dependency-file to pull alongside it.
 *
 * @author Griefed
 */
object BootCandidateSelector {

    /** Numeric Minecraft-version ordering (so `1.20` sorts above `1.9`, unlike a string compare). */
    val minecraftComparator = Comparator<String> { left, right ->
        val leftParts = left.split('.').map { it.toIntOrNull() ?: 0 }
        val rightParts = right.split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val difference = leftParts.getOrElse(index) { 0 }.compareTo(rightParts.getOrElse(index) { 0 })
            if (difference != 0) return@Comparator difference
        }
        0
    }

    /**
     * Choose the newest (file, Minecraft-version) pair among [files] for [loader] for which
     * [loaderVersionAvailable] holds, so the boot uses a combination that can actually install.
     * Returns `null` when no such combination exists.
     *
     * **A file declaring no loader at all is a fallback, not a match.** CurseForge had no modloader facet
     * before Minecraft 1.13, so a pre-1.13 file carries an empty loader set — and a project whose files are
     * *all* untagged was therefore never selected under any loader, i.e. never ground. Measured against the
     * live API 2026-09-06: all 15 of mtlib's files are untagged and it returned no candidate at all.
     *
     * **Why that is safe here, which is a different argument than for a dependency.** Picking an untagged
     * file for the wrong loader could stage a jar that loader ignores, boot cleanly and publish a false
     * `CLEAR` — the worst outcome this engine has, since it claims proof about a mod that never loaded. Two
     * things prevent it: [loaderVersionAvailable] covers the dominant case, because untagged files are
     * overwhelmingly pre-1.13 where Fabric and Quilt have no builds, so only Forge is reachable and untagged
     * *means* Forge; and for anything newer, `BootVerifier.refuseForSelfDeclaration` reads the downloaded
     * jar's own descriptor before the boot and refuses one carrying only another loader's. The cost of being
     * wrong is a refused attempt, not a wrong verdict.
     *
     * [untaggedFallback] turns that last arm off. [pickGrindTargets] needs it: it asks several loaders about
     * one Minecraft line in priority order, and an untagged file matches *every* loader — so with the arm on,
     * the highest-priority loader would claim an untagged file while a loader further down had a file its
     * author actually tagged. Off for the first pass, on for the second, which keeps "a file whose author did
     * state a loader always wins" true across loaders as well as within one.
     */
    fun pickBootableCandidate(
        files: List<ModFile>,
        loader: String,
        untaggedFallback: Boolean = true,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): Pair<ModFile, String>? =
        // The channel is the outermost preference, so a stable build on an older Minecraft beats a beta on a
        // newer one. Newest-Minecraft-first does not merely permit a beta -- authors publish their
        // experimental newer-Minecraft ports on that channel while the stable line sits on an older version,
        // so it prefers one for exactly the projects that have a stable alternative. Measured on
        // `hybrid-aquatic`: 16 stable Forge releases on 1.20.1, and a `[Sinytra]` beta on 1.20.4 was booted.
        //
        // A preference, never a filter: every channel is tried in turn, so a project publishing only betas
        // (or only an alpha, as `faster-random` does for Forge) is ground exactly as deeply as before.
        ReleaseChannel.entries.firstNotNullOfOrNull { channel ->
            pickBootableCandidateFrom(
                files.filter { it.channel == channel }, loader, untaggedFallback, loaderVersionAvailable
            )
        }

    /**
     * [pickBootableCandidate]'s loader resolution, over one channel's files: the tagged files first, then the
     * untagged ones.
     *
     * An untagged file states no loader rather than stating another one — see [pickUntagged]. Last resort, so
     * a file whose author did tag it always wins and this only adds a candidate where there was none: an
     * all-untagged project (every one of mtlib's 15 files) was never ground at all. [untaggedFallback] drops
     * that arm entirely, for a caller comparing several loaders over one file set.
     */
    private fun pickBootableCandidateFrom(
        files: List<ModFile>,
        loader: String,
        untaggedFallback: Boolean,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): Pair<ModFile, String>? =
        newestOf(files.filter { loader in it.loaders }, loaderVersionAvailable)
            ?: newestOf(files.filter { untaggedFallback && it.loaders.isEmpty() }, loaderVersionAvailable)

    /** The newest bootable (file, Minecraft version) pair among [files], or `null`. */
    private fun newestOf(
        files: List<ModFile>,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): Pair<ModFile, String>? =
        files.flatMap { file -> file.minecraftVersions.map { file to it } }
            .sortedWith { left, right -> minecraftComparator.compare(right.second, left.second) }
            .firstOrNull { loaderVersionAvailable(it.second) }

    /**
     * One unit of grinding: which Minecraft version-line, which loader, which published file, and which
     * exact version inside the line.
     *
     * The loader is carried because it is a *choice* rather than the subject — one loader per line is what
     * the project is ground on, and which one it was is evidence a reader needs when the verdict is argued
     * with.
     */
    data class GrindTarget(
        /** The Minecraft version-line this target is about, as [minecraftLine] spells it. */
        val minecraftLine: String,
        /** The loader chosen for this line, the first of [LOADER_PRIORITY] the line publishes a build for. */
        val loader: String,
        /** The published file to boot. */
        val file: ModFile,
        /** The exact Minecraft version inside [minecraftLine] the pack boots at. */
        val minecraftVersion: String
    )

    /**
     * Which loader to grind a Minecraft line under, most-preferred first.
     *
     * **Why one loader per line rather than all of them.** A project's Forge and NeoForge builds of one era
     * are usually the same source compiled twice, so booting both re-asks a question already answered; its
     * 1.12.2 and 1.21 builds are different code, and that difference was never asked about at all. The order
     * favours the loader whose build is most likely to be the maintained one: NeoForge is where modern Forge
     * development went, Forge still carries the older eras, and Fabric precedes Quilt because Quilt boots
     * Fabric builds anyway (see [LoaderCompatibility]) while the reverse is false. `LegacyFabric` is last
     * because it exists only for versions the others predate.
     *
     * **Every supported modloader must appear here**, which `GrindTargetSelectionTest` fails the build over:
     * a loader missing from the order is silently never ground, exactly as a verdict missing from the
     * grinder's rank sorts behind everything without saying so.
     */
    val LOADER_PRIORITY = listOf("NeoForge", "Forge", "Fabric", "Quilt", "LegacyFabric")

    /**
     * The targets to grind for a project publishing [files]: one per Minecraft version-line [linePolicy]
     * selects, each under the first loader of [LOADER_PRIORITY] that line has a bootable build for.
     *
     * A line no loader can boot is dropped rather than reported — there is nothing to run, so a verdict about
     * it would be a verdict about our own selection. Newest line first, which is [linePolicy]'s ordering.
     *
     * **Two passes over the priority order, and the second is what keeps a stated loader winning.** An
     * untagged file (CurseForge published no modloader facet before Minecraft 1.13) matches every loader, so
     * a single pass would let NeoForge claim one while Forge had a file its author actually tagged. The first
     * pass therefore asks for tagged files only, and the untagged fallback runs after every loader has been
     * asked — which is the same "a tag is a statement, an empty set is the absence of one" rule
     * [pickBootableCandidate] applies within one loader, applied across them.
     */
    fun pickGrindTargets(
        files: List<ModFile>,
        linePolicy: MinecraftLinePolicy = MinecraftLinePolicy(),
        loaderVersionAvailable: (loader: String, minecraftVersion: String) -> Boolean
    ): List<GrindTarget> {
        val lines = files.flatMap { it.minecraftVersions }.map { minecraftLine(it) }.distinct()
        return linePolicy.select(lines).mapNotNull { line ->
            val within = filesWithin(files, line)
            targetFor(line, within, untaggedFallback = false, loaderVersionAvailable)
                ?: targetFor(line, within, untaggedFallback = true, loaderVersionAvailable)
        }
    }

    /** The first loader of [LOADER_PRIORITY] with a bootable build of [files] on [line], or `null`. */
    private fun targetFor(
        line: String,
        files: List<ModFile>,
        untaggedFallback: Boolean,
        loaderVersionAvailable: (loader: String, minecraftVersion: String) -> Boolean
    ): GrindTarget? = LOADER_PRIORITY.firstNotNullOfOrNull { loader ->
        pickBootableCandidate(files, loader, untaggedFallback) { loaderVersionAvailable(loader, it) }
            ?.let { (file, version) -> GrindTarget(line, loader, file, version) }
    }

    /**
     * [files] narrowed to [line], each file keeping only the Minecraft versions that belong to it.
     *
     * The versions are narrowed and not merely the files, because one published file is routinely tagged
     * across lines — `aether-1.20.1-1.5.2-neoforge.jar` carries `1.20.1` alone but JEI's builds commonly
     * carry several — and [pickBootableCandidate] picks the newest version it is *shown*. Handing it the
     * whole set would let a 1.20 line's pick boot at 1.21, which is the one thing a per-line axis exists to
     * stop.
     */
    private fun filesWithin(files: List<ModFile>, line: String): List<ModFile> =
        files.mapNotNull { file ->
            file.minecraftVersions.filter { minecraftLine(it) == line }
                .takeIf { it.isNotEmpty() }
                ?.let { file.copy(minecraftVersions = it.toSet()) }
        }

    /**
     * The newest Minecraft version of [file] that both [loaderVersionAvailable] allows and the jar's own
     * declared [minecraftConstraint] accepts, or `null` when the file declares none the jar agrees with.
     *
     * **Why a second pass instead of folding this into [pickBootableCandidate].** The constraint is the
     * jar's, and the jar does not exist until it has been downloaded — which happens after selection. So
     * the first pick is necessarily made from platform metadata alone, and this narrows it once the
     * descriptor can actually be read.
     *
     * Measured on JEI: `jei-1.21.1-forge-19.52.0.422.jar` is tagged for 1.21 and 1.21.1 while declaring
     * `[1.21, 1.21.1)`, so the newest tagged version is excluded by the jar itself and 1.21 is the answer.
     * Returning `null` rather than the excluded version is deliberate — the caller then keeps its original
     * refusal, which is the honest outcome when platform and jar genuinely share no version.
     */
    fun newestVersionSatisfying(
        file: ModFile,
        minecraftConstraint: String,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): String? =
        file.minecraftVersions
            .sortedWith { left, right -> minecraftComparator.compare(right, left) }
            .firstOrNull { VersionConstraint.satisfies(it, minecraftConstraint) && loaderVersionAvailable(it) }

    /**
     * The newest Minecraft **release** in [releases] that the jar's [minecraftConstraint] accepts and the
     * loader can boot, or `null` when there is none.
     *
     * The wider fallback behind [newestVersionSatisfying]: that one reconsiders only versions the *platform*
     * tagged, so it rescues a jar tagged for two versions whose descriptor accepts one of them (JEI) and
     * does nothing for a jar tagged for exactly one version its descriptor excludes.
     * `moonlight-1.20.4-2.9.9-forge.jar` is tagged 1.20.4 and declares `[1.20,1.20.2)`; platform and jar
     * share nothing, and the candidate was refused rather than booted at the version it was built for.
     *
     * **The jar is the better authority when the two disagree**, because the loader enforces this range at
     * runtime: booting inside it is what gets the mod loaded, while booting at a version the author merely
     * ticked on a web form gets the mod rejected by FML before it runs.
     *
     * **A constraint that constrains nothing never bumps.** [VersionConstraint] accepts anything it cannot
     * read — deliberately, so a grammar gap cannot mass-refuse — which means an empty, wildcard or
     * unparseable descriptor would otherwise "satisfy" the newest release in existence and silently
     * relocate every candidate there. Such a constraint is answered with `null`, leaving the caller's
     * original refusal in place.
     */
    fun newestReleaseSatisfying(
        minecraftConstraint: String,
        releases: Collection<String>,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): String? {
        if (!constrainsAnything(minecraftConstraint, releases)) {
            return null
        }
        return releases
            .sortedWith { left, right -> minecraftComparator.compare(right, left) }
            .firstOrNull { VersionConstraint.satisfies(it, minecraftConstraint) && loaderVersionAvailable(it) }
    }

    /**
     * Whether [minecraftConstraint] actually excludes something out of [releases].
     *
     * A constraint every candidate satisfies carries no information — it is blank, a wildcard, or a string
     * the parser could not read and therefore accepted. Asked of the same set the caller is about to search,
     * so the question is decided by the constraint's observed effect rather than by trying to re-detect the
     * shapes [VersionConstraint] chooses to tolerate.
     */
    private fun constrainsAnything(minecraftConstraint: String, releases: Collection<String>): Boolean =
        releases.any { !VersionConstraint.satisfies(it, minecraftConstraint) }

    /**
     * One member of the sample the other-version crash re-check boots: which file, under which loader, on
     * which Minecraft version. The loader is carried explicitly because the sample deliberately leaves the
     * crashing loader — see [pickRecheckCandidates].
     */
    data class RecheckCandidate(
        /** The published file to stage. */
        val file: ModFile,
        /** The loader to boot it under; not necessarily the one that crashed. */
        val loader: String,
        /** The Minecraft version to boot it on. */
        val minecraftVersion: String
    )

    /**
     * The sample the other-version crash re-check boots, capped at [limit]: a *diverse* set of published
     * combinations other than the crashing `bootedLoader` / [bootedMinecraftVersion] one, gated by
     * [loaderVersionAvailable] exactly as selection is — a combination the loader has no build for can
     * never be staged either.
     *
     * Each pick prefers a candidate introducing both a [minecraftLine] and a loader that no earlier pick
     * used (the crashing combination's own line counts as used from the start), then relaxes to a new line,
     * then to a new loader, and finally takes whatever is left. So the diversity is a *preference*: a
     * project publishing one loader and one Minecraft line still spends its whole budget, on the same
     * newest-first versions it always did. Candidates are considered most-recent-Minecraft-first and only
     * the newest file of each (loader, Minecraft version) is ever one — two rebuilds for one Minecraft are
     * near-identical code. That relies on [files] arriving newest-first, which both platforms do and the
     * stable sort preserves.
     *
     * **Why diverse and not simply newest.** Measured 2026-08-23 on `creativecore`: a Fabric crash on
     * Minecraft 26.2 was re-checked on Fabric 26.1.2 and Fabric 26.1 — same loader, same loader version
     * `0.19.3`, and the two Minecraft versions either side of the crashing one. Both were INCONCLUSIVE and
     * the crash was published HIGH, while NeoForge had booted a server for the same project in the same
     * run. Two boots that close to the crashing combination re-test its environment, not the mod.
     *
     * **Crossing the loader is a wider claim than [pickBootableCandidate] makes, and it is gated to match.**
     * A mod really can be client-only on one loader, which is why `ClientsideVerifier.targetDisprovingTheCrash`
     * refuses to let any survival clear any crash. This sample is spent only where the crash already
     * *contradicts* a declared server support (`BootVerifier.shouldRecheckAgainstOtherVersions`), i.e. where
     * one of the two signals is known to be wrong — and a project the author declares server-capable, that
     * boots a server under another loader, is far better explained by a broken build than by sideness.
     */
    fun pickRecheckCandidates(
        files: List<ModFile>,
        bootedLoader: String,
        bootedMinecraftVersion: String,
        limit: Int,
        loaderVersionAvailable: (loader: String, minecraftVersion: String) -> Boolean
    ): List<RecheckCandidate> {
        if (limit <= 0) {
            return emptyList()
        }
        val pool = files
            .flatMap { file -> file.loaders.flatMap { loader -> file.minecraftVersions.map { RecheckCandidate(file, loader, it) } } }
            .filter { candidate ->
                !(candidate.loader == bootedLoader && candidate.minecraftVersion == bootedMinecraftVersion) &&
                    loaderVersionAvailable(candidate.loader, candidate.minecraftVersion)
            }
            .sortedWith { left, right -> minecraftComparator.compare(right.minecraftVersion, left.minecraftVersion) }
            .distinctBy { it.loader to it.minecraftVersion }
            .toMutableList()

        val bootedLine = minecraftLine(bootedMinecraftVersion)
        val usedLines = mutableSetOf(bootedLine)
        val usedLoaders = mutableSetOf<String>()
        val picked = ArrayList<RecheckCandidate>(limit)
        // **The crashing line's own other loader goes first, and nothing else can supply it.** Since a
        // project is ground once per Minecraft line under a single loader, every *other* line is already a
        // first-class verdict that `ClientsideVerifier` reconciles against for free — so spending the budget
        // there re-buys evidence the run produces anyway, while the sibling loader of this era is booted by
        // nobody unless this asks for it. That sibling is what used to throw out a wrong crash in the same
        // run (`iron-chests`: Forge crashed, NeoForge booted, same entry).
        pool.firstOrNull { minecraftLine(it.minecraftVersion) == bootedLine && it.loader != bootedLoader }
            ?.let { sibling ->
                pool.remove(sibling)
                usedLoaders.add(sibling.loader)
                picked.add(sibling)
            }
        while (picked.size < limit && pool.isNotEmpty()) {
            val next = pool.firstOrNull { it.loader !in usedLoaders && minecraftLine(it.minecraftVersion) !in usedLines }
                ?: pool.firstOrNull { minecraftLine(it.minecraftVersion) !in usedLines }
                ?: pool.firstOrNull { it.loader !in usedLoaders }
                ?: pool.first()
            pool.remove(next)
            usedLoaders.add(next.loader)
            usedLines.add(minecraftLine(next.minecraftVersion))
            picked.add(next)
        }
        return picked
    }

    /**
     * The Minecraft *version-line* [minecraftVersion] belongs to — its first two components, so `26.1.2` and
     * `26.1` are one line while `26.2` is another, and `1.21.11` is separate from `1.20.1`.
     *
     * A line is the granularity at which mod code actually differs: builds within one are ports of the same
     * source across a patch release, which is why re-checking a crash on the version next to it learns so
     * little — and, since 2026-09-11, why it is the axis a project is ground on.
     *
     * Public because the grinder needs it too: `BootLogStore` reads a kept artifact's recorded Minecraft
     * version back to the line its owner is filed under. A second copy of this rule in that module is
     * exactly the duplication this repository has paid for three times.
     */
    fun minecraftLine(minecraftVersion: String): String =
        minecraftVersion.split('.').take(2).joinToString(".")

    /**
     * Pick a dependency-file from [files] for the same [loader] at [minecraftVersion], falling back to
     * another **patch** release of the same version-line when the project published nothing for that exact
     * version, or `null` when even that finds nothing.
     *
     * Unlike the candidate under test, a dependency is never staged across a version-*line*: a near-miss
     * candidate still tests the candidate, but a 1.19.4 dependency in a 1.20.1 pack guarantees a
     * loader-level version conflict that kills the boot and is then blamed on the mod under test. `null`
     * becomes a staging refusal, which is the honest outcome for a mod that never got a fair run.
     *
     * **Inside a line, that strictness cost boots it had no reason to.** 1.20.1, 1.20.2 and 1.20.3 run each
     * other's mods in practice, and a library that skipped a patch release is not a missing dependency.
     * Measured against the live Modrinth API on 2026-09-09, six published `ERROR` verdicts named a
     * dependency that exists one patch away — `playeranimator` for Forge 1.20.2, `yacl` and
     * `forgified-fabric-api` for Forge 1.20.6, `cobblemon` for Fabric 1.21.11, and QSL for Quilt 1.21.1 and
     * 1.21.11. QSL shows why the line is the right width rather than a wider band: its newest release is
     * Minecraft 1.21 and the project is discontinued, so every Quilt mod declaring a `quilt_*` module on
     * 1.21.1 or later was refused permanently.
     *
     * The three preferences are ordered — obtainability, then the exact Minecraft version, then the
     * declared constraint — and [preferenceLadder] is where that order is written down.
     */
    fun pickDependencyFile(
        files: List<ModFile>,
        loader: String,
        minecraftVersion: String,
        versionConstraint: String? = null
    ): ModFile? {
        // A constraint is a PREFERENCE, never a filter. Preferring a satisfying file is an improvement;
        // returning null where this used to return a file would turn a bootable candidate into a refusal,
        // and `refuseForMissingDependencies` scores a refusal INCONCLUSIVE -- so the mod would quietly stop
        // being verified rather than fail loudly. Narrow first, then fall back to the whole set.
        val satisfying = files.filter { VersionConstraint.satisfies(it.version, versionConstraint) }
        return preferenceLadder(files, satisfying, minecraftVersion).firstNotNullOfOrNull { (candidates, version) ->
            // Cross-loading is asked about the version the pack BOOTS at, never the one the file carries:
            // NeoForge runs Forge builds on Minecraft 1.20.1 and on no other version, so re-running the
            // loader ladder at a neighbour would make a Forge 1.20.1 file a dependency for a NeoForge
            // 1.20.2 pack -- the exact mismatch `theNeoForgeFallbackToForgeAppliesOnMinecraft1201Only` pins
            // against.
            pickFrom(candidates, loader, version, compatibleAt = minecraftVersion)
        }
    }

    /**
     * Every (candidate set, Minecraft version) pair [pickDependencyFile] tries, in the order it tries them:
     * obtainability outermost, then the Minecraft version, then the declared constraint.
     *
     * **Obtainability is the strongest of the three.** A distribution-locked file has no `downloadUrl` at
     * all, so picking one guarantees the dependency is reported unmet — whereas Quilt genuinely runs Fabric
     * mods, making an obtainable Fabric build a working dependency where a locked Quilt build is nothing
     * (`306612`, Fabric API on CurseForge, was refused for a Quilt boot on exactly that ordering), and an
     * obtainable patch neighbour is a working dependency where a locked exact match is nothing.
     *
     * Still a preference and never a filter: the locked half of the ladder runs last but it does run, so
     * when every candidate is locked one is returned anyway and the refusal can say "distribution-locked" —
     * true and actionable — instead of "publishes no <loader> file", which would be false.
     */
    private fun preferenceLadder(
        files: List<ModFile>,
        satisfying: List<ModFile>,
        minecraftVersion: String
    ): Sequence<Pair<List<ModFile>, String>> = sequence {
        for (obtainableOnly in listOf(true, false)) {
            val narrow = if (obtainableOnly) satisfying.filterNot { it.locked } else satisfying
            val whole = if (obtainableOnly) files.filterNot { it.locked } else files
            // Neighbours are gathered from `whole` alone: `narrow` is a subset of it, so adding it back in
            // could only ever repeat versions `patchNeighboursOf` already de-duplicates.
            for (version in listOf(minecraftVersion) + patchNeighboursOf(whole, minecraftVersion)) {
                yield(narrow to version)
                yield(whole to version)
            }
        }
    }

    /**
     * The other patch releases of [minecraftVersion]'s own line that [files] actually publish for, nearest
     * first and a tie going to the newer build.
     *
     * Only what the files carry is offered, so the search is bounded by the project's real history rather
     * than by an invented range. A version whose patch component is not a number is skipped: `1.21.4-pre3`
     * is a pre-release rather than a patch of `1.21.4`, and staging a dependency from one is the near-miss
     * this fallback is narrow in order to avoid.
     *
     * Nearest-first because a build closer to the version being booted is closer to the Minecraft it was
     * compiled against — the same reason the exact match is preferred at all — and the newer build wins a
     * tie because it is the more likely of the two to still be maintained.
     */
    private fun patchNeighboursOf(files: List<ModFile>, minecraftVersion: String): List<String> =
        patchNeighboursIn(files.flatMap { it.minecraftVersions }, minecraftVersion)

    /**
     * The patch releases of [minecraftVersion]'s own line present in [versions], nearest first and a tie
     * going to the newer build — the ordering rule [patchNeighboursOf] applies to a project's files, exposed
     * for a caller holding a list of *versions* instead.
     *
     * `BootVerifier` needs exactly that: a CurseForge dependency's files are fetched **per version**
     * (`gameVersion=<exact>`), so the neighbours cannot be read off the files in hand and have to come from
     * the Minecraft releases SPC knows about. One rule for both, or the two orderings drift.
     */
    fun patchNeighboursIn(versions: Collection<String>, minecraftVersion: String): List<String> {
        val wantedPatch = patchOf(minecraftVersion) ?: return emptyList()
        val line = minecraftLine(minecraftVersion)
        return versions
            .distinct()
            .filter { it != minecraftVersion && minecraftLine(it) == line }
            .mapNotNull { version -> patchOf(version)?.let { version to it } }
            .sortedWith(compareBy({ kotlin.math.abs(it.second - wantedPatch) }, { -it.second }))
            .map { it.first }
    }

    /**
     * [minecraftVersion]'s patch component as a number — `0` for a two-component version such as `1.21`,
     * and `null` when the component is not a plain number and therefore not a patch release.
     */
    private fun patchOf(minecraftVersion: String): Int? {
        val components = minecraftVersion.split('.')
        if (components.size < 2 || components.take(2).any { it.toIntOrNull() == null }) {
            return null
        }
        return if (components.size == 2) 0 else components[2].toIntOrNull()
    }

    /**
     * [pickDependencyFile]'s loader resolution: the loader itself, then whichever other loaders'
     * builds it can actually run **at [compatibleAt]**, then an untagged file.
     *
     * The Minecraft version is fixed across every attempt, which is what makes the fallback reachable. It used
     * to be a *preference* inside each attempt, so a Quilt-tagged file for the wrong version satisfied the first
     * attempt and the Fabric build carrying the right version was never considered.
     *
     * [compatibleAt] is the version the pack boots at and defaults to the one being matched; they differ only
     * for a patch neighbour, where the cross-loader question still belongs to the pack.
     */
    private fun pickFrom(
        files: List<ModFile>,
        loader: String,
        minecraftVersion: String,
        compatibleAt: String = minecraftVersion
    ): ModFile? =
        pickForLoader(files, loader, minecraftVersion)
            ?: LoaderCompatibility.alsoRuns(loader, compatibleAt)
                .firstNotNullOfOrNull { pickForLoader(files, it, minecraftVersion) }
            ?: pickUntagged(files, minecraftVersion).takeIf { predatesTheLoaderFacet(minecraftVersion) }

    /**
     * Whether [minecraftVersion] is old enough that an untagged file *means* Forge, rather than merely
     * saying nothing.
     *
     * **The assumption this guards was empirically false.** [pickUntagged]'s safety argument is that
     * untagged files are pre-1.13, from before CurseForge had a modloader facet, so only Forge is reachable
     * anyway. Measured against the live API on 2026-09-11, `TerraBlender (Forge)` publishes
     * `TerraBlender-forge-26.2-26.2.0.0.2.jar` with `gameVersions=['26.2']` — **untagged, for Minecraft
     * 26.2, in 2026**. So an untagged file matched a *Fabric* and a *NeoForge* boot alike, and
     * `biomes-o-plenty` was staged the Forge build of its own dependency on both: the loaders could not see
     * it, and `terrablender` came out `[MISSING]` in two published rows.
     *
     * Where the facet exists, an untagged file is genuinely unknown and the fallback is dropped — a refusal
     * naming the real gap beats a jar the loader will ignore. Below it the fallback stays, which is what
     * keeps `mtlib` (all 15 of its files untagged, all 1.12.2) gradeable at all.
     *
     * **The candidate's own fallback is deliberately untouched.** `pickBootableCandidate` keeps it for every
     * version, because `BootVerifier.refuseForSelfDeclaration` reads the downloaded jar's descriptor before
     * the boot and refuses one carrying another loader's — a guard no *dependency* gets.
     */
    private fun predatesTheLoaderFacet(minecraftVersion: String): Boolean =
        minecraftComparator.compare(minecraftVersion, LOADER_FACET_SINCE) < 0

    /**
     * The Minecraft version from which CurseForge tags a file's modloader, so an absent tag stops being
     * evidence of Forge and becomes an absence of information.
     */
    private const val LOADER_FACET_SINCE = "1.13"

    /** Newest file carrying both [loader] and [minecraftVersion], or `null` when the project publishes none. */
    private fun pickForLoader(files: List<ModFile>, loader: String, minecraftVersion: String): ModFile? =
        files.firstOrNull { loader in it.loaders && minecraftVersion in it.minecraftVersions }

    /**
     * Newest file for [minecraftVersion] that declares **no loader at all**, or `null`.
     *
     * CurseForge had no modloader facet before Minecraft 1.13, so a pre-1.13 file carries an empty loader
     * set and `pickForLoader` — which asks `loader in it.loaders` — can never match one. Measured against
     * the live API 2026-09-06: all 15 of mtlib's files are untagged, and 106 of iron-chests' 138. That made
     * every such dependency unpickable and refused the boot, which is what
     * *"Required dependency unavailable for Forge / Minecraft 1.12.2: mtlib"* was.
     *
     * **Untagged is unknown, not incompatible**, and it is the *last* arm on purpose: the exact loader and
     * the cross-loader fallback are both tried first, so a file whose author did state a loader always wins
     * and this can only add a pick where there was none. A file tagged for a *different* loader is still
     * refused — that tag is a statement, and an empty set is the absence of one.
     */
    private fun pickUntagged(files: List<ModFile>, minecraftVersion: String): ModFile? =
        files.firstOrNull { it.loaders.isEmpty() && minecraftVersion in it.minecraftVersions }

}
