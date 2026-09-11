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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.clientside.*
import de.griefed.serverpackcreator.grinder.container.ContainerEngine
import de.griefed.serverpackcreator.grinder.container.ContainerResources
import de.griefed.serverpackcreator.grinder.container.ContainerServerRunner
import de.griefed.serverpackcreator.grinder.container.ContainerUser
import de.griefed.serverpackcreator.grinder.loader.CachedLoaderVersions
import de.griefed.serverpackcreator.grinder.loader.ImageJavaRuntimes
import de.griefed.serverpackcreator.grinder.loader.LoaderCache
import de.griefed.serverpackcreator.grinder.loader.LoaderStepDown
import de.griefed.serverpackcreator.grinder.loader.PackVariables
import de.griefed.serverpackcreator.grinder.report.BootLogStore
import java.io.File
import java.util.Collections
import java.time.Duration

/**
 * The production [CandidateVerifier]: drives the clientside `ClientsideVerifier` (metadata + boot)
 * with the boot wired to run in an **isolated container** instead of a host process. The decisive
 * piece is the `packPostProcessor` hook — between staging and boot it ensures the loader is installed
 * (once per tuple, with network), overlays that cached install layer into the pack, and sets the
 * offline-boot levers, so the actual mod-boot runs under `--network none`.
 *
 * Integration-only: a live daemon, the runtime image and a real `ApiWrapper` are required, so this is
 * exercised end-to-end, not unit-tested; the pieces it composes are individually tested.
 *
 * @param apiWrapper      SPC resolution + generation + version metadata.
 * @param loaderCache     The per-tuple install cache (its installer boots-with-network on a miss).
 * @param containerEngine The container runtime the mod-boots use.
 * @param runtimeImage    The image carrying the JDKs + SPC's shell tooling.
 * @param imageJava       The image's supported-Java gate + per-version JDK resolution.
 * @param workDirectory   Scratch root for verification + boot staging.
 * @param bootTimeout     Per-boot budget.
 * @param resources       CPU/memory/pid caps per mod-boot container.
 * @param curseForgeApiKey CurseForge key (CF resolution); Modrinth needs none.
 * @param containerUser   The `uid:gid` every boot container runs as; must own the staged pack, or the
 *                        mod-boot cannot write into it. Resolved from the host by `ContainerUser`.
 * @author Griefed
 */
class ContainerCandidateVerifier(
    private val apiWrapper: ApiWrapper,
    private val loaderCache: LoaderCache,
    private val containerEngine: ContainerEngine,
    private val runtimeImage: String,
    private val imageJava: ImageJavaRuntimes,
    private val workDirectory: File,
    private val bootTimeout: Duration = Duration.ofMinutes(15),
    private val resources: ContainerResources = ContainerResources(),
    private val curseForgeApiKey: String? = System.getenv("CURSEFORGE_API_KEY"),
    private val containerUser: String = ContainerUser.IMAGE_DEFAULT,
    private val crashLogs: BootLogStore? = null,
    private val consoleRules: () -> ConsoleRuleSet = { ConsoleRuleSet.EMPTY },
    /**
     * The id-to-project map, shared across every grind so what one candidate's jars prove is not
     * re-derived for the next. The daemon hands in a file-backed one ([JsonLearnedModIds]); the default
     * keeps a verifier constructed in a test free of a home directory.
     */
    private val learnedModIds: LearnedModIds = LearnedModIds()
) : CandidateVerifier {
    /** Reclaims each candidate's staging once its verdicts are in; without it the work tree grows without bound. */
    private val reaper = BootWorkspaceReaper(workDirectory)


    override fun verify(candidate: GrindCandidate): ClientsideReport {
        var resolved: ClientsideReport? = null
        // Per invocation, never a field: ONE verifier instance serves every GrindPool worker, so a shared
        // collection would let one candidate's prune delete the logs another had just written -- the same
        // cross-candidate class that made an unqualified attempt directory wipe a pack mid-boot. The sink
        // writes to it from the boot path while this call reads it afterwards, hence synchronized.
        val keptLogNames = Collections.synchronizedList(mutableListOf<String>())
        try {
            val report = verifyStaged(candidate, keptLogNames)
            resolved = report
            // Artifacts were kept per attempt, during the boots, by the sink below -- staging wipes the attempt
            // directory on every stage, so nothing readable is left by the time we get here. What is left to do
            // is drop the *previous* grind's attempts, which a re-check sampling a different loader or Minecraft
            // line would otherwise strand forever, and then check the store against its ceiling.
            crashLogs?.let { store ->
                val (platform, slug) = reapTarget(candidate, resolved)
                report.perLoader.map { it.loader }.distinct().forEach { loader ->
                    store.pruneExcept(platform, slug, loader, keptLogNames.toSet())
                }
                store.enforceBudget()
            }
            return report
        } finally {
            // In a `finally` because a *thrown* verification is exactly when staging is most likely to be left
            // behind, and the reaper keeps the boot logs the failure will have to be diagnosed from.
            val (platform, slug) = reapTarget(candidate, resolved)
            reaper.reap(platform, slug)
        }
    }

    /**
     * The crash-console retention step, kept here as a function rather than a method because it needs no verifier
     * state and is what `BootWorkspaceReaperTest` exercises directly.
     */
    companion object {
        /**
         * Why a loader install is unavailable, in words that match what actually happened.
         *
         * The predecessor said `No cached loader install for <tuple>`, which describes a cache miss — the one
         * state that *cannot* be the cause, because [LoaderCache.ensureInstalled] responds to a miss by
         * installing. It returns `null` only after an install failed, or while a recent failure is on
         * cooldown. Naming the miss sent a reader looking for an on-demand install that has always been there.
         *
         * This becomes the verdict's detail — `BootVerifier` reports the throw as INCONCLUSIVE — so it is what
         * the report shows for every candidate that wanted the tuple. That matters most in the [onCooldown]
         * case, which logs at DEBUG and is otherwise invisible at default levels.
         *
         * [onCooldown] must describe the state **before** the attempt — see [installedBase], which is the only
         * caller and the reason this takes the flag rather than asking the cache itself.
         */
        internal fun installUnavailableMessage(
            loader: String,
            loaderVersion: String,
            minecraftVersion: String,
            onCooldown: Boolean
        ): String {
            val tuple = "$loader $loaderVersion / Minecraft $minecraftVersion"
            return if (onCooldown) {
                "Loader install for $tuple failed recently and is on cooldown, so it was not retried. " +
                    "Not booting — this says nothing about the mod. The failure itself was logged when it happened."
            } else {
                "Loader install for $tuple failed, so the pack could not be completed. Not booting — this says " +
                    "nothing about the mod. See the install log for this tuple under the cache root."
            }
        }

        /**
         * The installed base for a tuple, or a throw whose message says **which** of the two things happened.
         *
         * The cooldown is read *before* [LoaderCache.ensureInstalled], because that call records the cooldown on
         * its way out of a failed install: asking afterwards, as this did until 2026-09-03, answers "on cooldown"
         * for the candidate that just paid for the attempt as well as for the cheap skips behind it, and the
         * failure branch of [installUnavailableMessage] becomes unreachable. During an outage that difference is
         * the diagnosis — an install still being attempted every hour is a live cause, a skip is its echo.
         */
        internal fun installedBase(
            cache: LoaderCache,
            loader: String,
            loaderVersion: String,
            minecraftVersion: String
        ): File {
            val suppressedBeforeAsking = cache.isInstallOnCooldown(loader, loaderVersion, minecraftVersion)
            return cache.ensureInstalled(loader, loaderVersion, minecraftVersion)
                ?: throw IllegalStateException(
                    installUnavailableMessage(loader, loaderVersion, minecraftVersion, suppressedBeforeAsking)
                )
        }

        /**
         * Keep one finished attempt's evidence, if the boot is worth keeping one for.
         *
         * Called from inside `BootVerifier.runPrepared`, once per attempt, and that is the only point at
         * which it *can* be: staging wipes and re-creates the attempt directory on every stage, so by the
         * time a candidate's verdicts are in, every attempt but the last has had its pack deleted — and the
         * re-check attempts are precisely the ones a contested crash is argued with.
         *
         * Retention is [BootArtifacts.worthKeeping], which lives in `-clientside` so the CLI verb and this
         * daemon cannot disagree about it. A boot that reached its ready-line explains nothing.
         */
        internal fun keepAttemptArtifacts(
            pack: BootVerifier.Prepared.Ready,
            outcome: BootVerifier.BootOutcome,
            store: BootLogStore?,
            into: MutableList<String>
        ) {
            if (store == null || !BootArtifacts.worthKeeping(outcome.result)) {
                return
            }
            val artifacts = BootArtifacts.collect(pack.serverPack, outcome.console)
            if (artifacts.isEmpty()) {
                return
            }
            into += store.keep(
                pack.attemptName,
                BootLogStore.attemptKey(pack.loader, pack.loaderVersion, pack.minecraftVersion),
                artifacts
            )
        }

        /**
         * Which `(platform, slug)` [verify] asks the reaper to reclaim: the **resolved report's**, because that
         * is what the staging directories were named from (`AttemptDirectory` is fed `ProjectFiles.platform`
         * and `ProjectFiles.slug`). Falls back to the [candidate]'s when no [report] exists, which is the
         * thrown-verification case — precisely when staging is most likely to be left behind.
         *
         * **The two really can disagree.** `Grinder` logs `"Platform mismatch for …: candidate says 'X',
         * resolved report says 'Y'"` when a source labels a project differently from the platform that
         * resolves it, and a slug is a mutable display name a rename can move out from under a queued
         * candidate. Asking for the candidate's copy of either matches no directory and leaks a full server
         * pack per attempt — the disk-growth class `BootWorkspaceReaper` exists for (98 GB across 1750
         * directories, measured 2026-07-30).
         */
        internal fun reapTarget(candidate: GrindCandidate, report: ClientsideReport?): Pair<String, String> =
            report?.let { it.platform to it.slug } ?: (candidate.platform to candidate.slug)
    }

    /** Run the actual verification, leaving the staging cleanup to [verify]. */
    private fun verifyStaged(candidate: GrindCandidate, keptLogNames: MutableList<String>): ClientsideReport {
        val httpDownloader = HttpJarDownloader(apiWrapper.webUtilities)
        val platforms = supportedPlatforms(curseForgeApiKey)
        return run {
            ClientsideVerifier(
                platforms = platforms,
                metadataScanner = MetadataScanner(apiWrapper.modScanner),
                jarDownloader = httpDownloader,
                workDirectory = File(workDirectory, "verify"),
                bootVerifierFactory = { platform ->
                    BootVerifier(
                        apiWrapper = apiWrapper,
                        platform = platform,
                        httpDownloader = httpDownloader,
                        // Reuse an installed loader build rather than installing every fresh release; the
                        // policy still reports the newest truthfully, so the support gate and BootVerifier's
                        // crash re-check are unaffected (see CachedLoaderVersions).
                        loaderVersionPolicy = CachedLoaderVersions(
                            LoaderVersionResolver(apiWrapper.versionMeta),
                            loaderCache,
                            ::knownLoaderVersionsNewestFirst
                        ),
                        workDirectory = File(workDirectory, "boot"),
                        serverRunner = ContainerServerRunner(containerEngine, runtimeImage, resources, containerUser),
                        packPostProcessor = ::overlayLoaderInstall,
                        minecraftAcceptable = imageJava::supports,
                        bootTimeout = bootTimeout,
                        consoleRules = consoleRules,
                        learnedModIds = learnedModIds,
                        // A dependency this platform cannot supply may exist on the other one, and the
                        // staged file is just a jar. Empty when no CurseForge key is configured.
                        alternatePlatforms = platforms.filter { it !== platform },
                        bootArtifactSink = { staged, outcome ->
                            keepAttemptArtifacts(staged, outcome, crashLogs, keptLogNames)
                        }
                    )
                }
            ).report(candidate.projectUrl)
        }
    }

    /**
     * The cache-overlay hook: ensure the loader is installed for the pack's tuple, copy that install
     * layer into the staged pack, then set the offline-boot levers. A missing install throws — the
     * [BootVerifier] reports it INCONCLUSIVE rather than booting a pack that would need network.
     */
    private fun overlayLoaderInstall(pack: BootVerifier.Prepared.Ready) {
        val javaPath = imageJava.javaPath(pack.minecraftVersion)
            ?: throw IllegalStateException("No bundled JDK for Minecraft ${pack.minecraftVersion}")
        val base = installedBase(loaderCache, pack.loader, pack.loaderVersion, pack.minecraftVersion)
        copyInstallLayer(base, pack.serverPack)
        PackVariables.prepareUnattended(pack.serverPack, javaPath, offline = true, installerJavaPath = imageJava.installerJavaPathFor(pack.minecraftVersion))
    }

    /**
     * Every known build for a `(loader, Minecraft)` pair, **newest first**, so the version policy can step down from a
     * build whose installer artifact is missing upstream (see `CachedLoaderVersions`).
     *
     * Only Forge and NeoForge are listed, because only they publish per-Minecraft builds where one can be absent while
     * another works; Fabric, Quilt and LegacyFabric ship a single Minecraft-independent loader line, so there is no
     * sibling build to fall back to and an empty list leaves them on the existing behaviour. SPC's metadata returns
     * these ascending, hence the reversal.
     */
    private fun knownLoaderVersionsNewestFirst(loader: String, minecraftVersion: String): List<String> = when (loader) {
        "Forge" -> LoaderStepDown.newestFirst(
            apiWrapper.versionMeta.forge.supportedForgeVersions(minecraftVersion).orElse(emptyList())
        )

        "NeoForge" -> LoaderStepDown.newestFirst(
            apiWrapper.versionMeta.neoForge.supportedNeoForgeVersions(minecraftVersion).orElse(emptyList())
        )

        // Fabric, Quilt and LegacyFabric publish one Minecraft-independent loader *line* rather than
        // per-Minecraft builds, which is why this used to return nothing -- read as "no sibling build to
        // fall back to". The line itself is versioned: Quilt ships 306 builds and Fabric 253, and Quilt's
        // own `/v3/versions/loader/<mc>` lists all 306 as valid for a given Minecraft, so there are 305
        // siblings. Quilt 0.31.0-beta.3 / Minecraft 1.20.6 failed to install with no fallback available.
        //
        // The Minecraft version is not a parameter here on purpose: for these loaders every build of the
        // line applies to every Minecraft that has intermediaries, and `LoaderVersionResolver` has already
        // gated on exactly that before anything reaches this point.
        "Fabric" -> LoaderStepDown.newestFirst(apiWrapper.versionMeta.fabric.loaderVersions())
        "Quilt" -> LoaderStepDown.newestFirst(apiWrapper.versionMeta.quilt.loaderVersions())
        "LegacyFabric" -> LoaderStepDown.newestFirst(apiWrapper.versionMeta.legacyFabric.loaderVersions())
        else -> emptyList()
    }

    /** Copy the cached install layer from [base] into [pack], skipping the cache's completion marker. */
    private fun copyInstallLayer(base: File, pack: File) {
        base.walkTopDown().filter { it.isFile && it.name != LoaderCache.MARKER }.forEach { file ->
            val destination = File(pack, file.relativeTo(base).path)
            destination.parentFile?.mkdirs()
            file.copyTo(destination, overwrite = true)
        }
    }
}
