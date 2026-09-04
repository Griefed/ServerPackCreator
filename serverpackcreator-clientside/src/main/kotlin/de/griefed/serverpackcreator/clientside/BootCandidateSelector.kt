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
     */
    fun pickBootableCandidate(
        files: List<ModFile>,
        loader: String,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): Pair<ModFile, String>? =
        files.filter { loader in it.loaders }
            .flatMap { file -> file.minecraftVersions.map { file to it } }
            .sortedWith { left, right -> minecraftComparator.compare(right.second, left.second) }
            .firstOrNull { loaderVersionAvailable(it.second) }

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
     * A mod really can be client-only on one loader, which is why `ClientsideVerifier.loaderDisprovingTheCrash`
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

        val usedLines = mutableSetOf(minecraftLine(bootedMinecraftVersion))
        val usedLoaders = mutableSetOf<String>()
        val picked = ArrayList<RecheckCandidate>(limit)
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
     * little.
     */
    internal fun minecraftLine(minecraftVersion: String): String =
        minecraftVersion.split('.').take(2).joinToString(".")

    /**
     * Pick a dependency-file from [files] for the same [loader] and **exactly** [minecraftVersion], or `null`.
     *
     * Unlike the candidate under test, a dependency is never staged for a different Minecraft version: a
     * near-miss candidate still tests the candidate, but a near-miss dependency guarantees a loader-level
     * version conflict that kills the boot and is then blamed on the mod under test. `null` becomes an
     * INCONCLUSIVE refusal, which is the honest verdict for a mod that never got a fair run.
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
        // Obtainability is the strongest preference of the three, and it outranks even the loader match.
        // A distribution-locked file has no `downloadUrl` at all, so picking one guarantees the dependency
        // is reported unmet -- whereas Quilt genuinely runs Fabric mods, making an obtainable Fabric build a
        // working dependency where a locked Quilt build is nothing. `306612` (Fabric API on CurseForge) was
        // refused for a Quilt boot on exactly that ordering.
        //
        // Still a preference and never a filter: when every candidate file is locked the last arm returns
        // one anyway, so the refusal can say "distribution-locked" -- true and actionable -- instead of
        // "publishes no <loader> file", which would be false.
        return pickFrom(satisfying.filterNot { it.locked }, loader, minecraftVersion)
            ?: pickFrom(files.filterNot { it.locked }, loader, minecraftVersion)
            ?: pickFrom(satisfying, loader, minecraftVersion)
            ?: pickFrom(files, loader, minecraftVersion)
    }

    /**
     * [pickDependencyFile]'s loader resolution: the loader itself, then the one-way Quilt-to-Fabric fallback.
     *
     * The Minecraft version is fixed across both attempts, which is what makes the fallback reachable. It used
     * to be a *preference* inside each attempt, so a Quilt-tagged file for the wrong version satisfied the first
     * attempt and the Fabric build carrying the right version was never considered.
     */
    private fun pickFrom(files: List<ModFile>, loader: String, minecraftVersion: String): ModFile? =
        pickForLoader(files, loader, minecraftVersion)
            ?: fallbackLoaders[loader]?.let { pickForLoader(files, it, minecraftVersion) }

    /** Newest file carrying both [loader] and [minecraftVersion], or `null` when the project publishes none. */
    private fun pickForLoader(files: List<ModFile>, loader: String, minecraftVersion: String): ModFile? =
        files.firstOrNull { loader in it.loaders && minecraftVersion in it.minecraftVersions }

    /**
     * Loaders that can run another loader's mods, used **only** when a dependency publishes nothing for the loader
     * being booted. Quilt deliberately runs Fabric mods — which is precisely why the canonical dependency of a Quilt
     * mod is Fabric API, a project that ships only Fabric-tagged files. Without this, every such dependency was
     * silently dropped and the mod hard-failed with "requires fabric-api", wasting the whole boot: measured
     * 2026-07-30, 210 dropped dependencies, all but 44 of them on Quilt.
     *
     * Deliberately not symmetric and deliberately minimal: Fabric cannot load Quilt mods, and NeoForge only loads
     * Forge mods for a narrow band of Minecraft versions, so guessing there would stage a jar the loader cannot use.
     */
    private val fallbackLoaders = mapOf("Quilt" to "Fabric")
}
