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
import de.griefed.serverpackcreator.grinder.loader.PackVariables
import de.griefed.serverpackcreator.grinder.report.CrashLogStore
import java.io.File
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
    private val crashLogs: CrashLogStore? = null
) : CandidateVerifier {
    /** Reclaims each candidate's staging once its verdicts are in; without it the work tree grows without bound. */
    private val reaper = BootWorkspaceReaper(workDirectory)

    override fun verify(candidate: GrindCandidate): ClientsideReport {
        var resolved: ClientsideReport? = null
        try {
            val report = verifyStaged(candidate)
            resolved = report
            // Before the reaper runs, and before the next re-grind of this tuple wipes the staging: the console
            // of a boot that crashed is the only evidence the verdict cannot be re-derived without.
            keepCrashConsoles(report, File(workDirectory, "boot"), crashLogs)
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
         * Copy the console of every **crashed** boot in [report] out of the staging under [bootRoot] and into
         * [crashLogs], returning how many were kept.
         *
         * **Why it has to happen here.** `BootWorkspaceReaper` keeps one `boot.log` per attempt directory, but
         * staging *wipes and re-creates* that directory, so the next re-grind of the same tuple destroys the
         * console belonging to the verdict still being published. A crash is the only outcome that reaches
         * HIGH, and its usual cause — a server loading a mod that reaches for a client-only class — is legible
         * from the console and from nothing else.
         *
         * Only CRASHED is kept: a clean boot proves nothing about sideness and explains nothing either, and
         * an INCONCLUSIVE one learned nothing by definition. The console is read from the *crashing loader's*
         * own directory, which is where `BootVerifier.restoreDecisiveConsole` has just put the decided boot's
         * output — including when a cross-loader re-check ran in the same directory.
         *
         * Nothing here may fail a grind that already has its answer, so a missing or unreadable console keeps
         * nothing and reports nothing.
         */
        internal fun keepCrashConsoles(report: ClientsideReport, bootRoot: File, crashLogs: CrashLogStore?): Int {
            if (crashLogs == null) {
                return 0
            }
            return report.perLoader
                .filter { it.bootResult == BootResult.CRASHED }
                .count { verdict ->
                    val console = File(
                        bootRoot,
                        AttemptDirectory.nameFor(report.platform, report.slug, verdict.loader) + "/" + BootWorkspaceReaper.KEPT_LOG
                    )
                    console.isFile && crashLogs.keep(report.platform, report.slug, verdict.loader, console) != null
                }
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
    private fun verifyStaged(candidate: GrindCandidate): ClientsideReport {
        val httpDownloader = HttpJarDownloader(apiWrapper.webUtilities)
        // The browser is only launched for distribution-locked CurseForge files; disposed after the run.
        return BrowserDownloader().use { browserDownloader ->
            ClientsideVerifier(
                platforms = supportedPlatforms(curseForgeApiKey),
                metadataScanner = MetadataScanner(apiWrapper.modScanner),
                jarDownloader = httpDownloader,
                workDirectory = File(workDirectory, "verify"),
                bootVerifierFactory = { platform ->
                    BootVerifier(
                        apiWrapper = apiWrapper,
                        platform = platform,
                        httpDownloader = httpDownloader,
                        browserDownloader = browserDownloader,
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
                        bootTimeout = bootTimeout
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
        val base = loaderCache.ensureInstalled(pack.loader, pack.loaderVersion, pack.minecraftVersion)
            ?: throw IllegalStateException("No cached loader install for ${pack.loader} ${pack.loaderVersion} / Minecraft ${pack.minecraftVersion}")
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
        "Forge" -> apiWrapper.versionMeta.forge.supportedForgeVersions(minecraftVersion).orElse(emptyList()).reversed()
        "NeoForge" -> apiWrapper.versionMeta.neoForge.supportedNeoForgeVersions(minecraftVersion).orElse(emptyList()).reversed()
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
