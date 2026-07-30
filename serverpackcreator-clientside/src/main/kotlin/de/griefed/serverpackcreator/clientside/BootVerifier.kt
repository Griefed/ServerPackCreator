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

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration
import java.util.*

/**
 * The production-near half of the signal: for one loader it force-includes the candidate mod (and its
 * required dependencies) into a freshly generated server pack and boots it, watching for a crash.
 *
 * This is the only signal that catches a mod that *declares* server/both yet actually crashes a
 * server — the metadata can't, because the declaration itself is the lie. Reuses SPC's own
 * generation + the ServerStarterJar (which self-installs the loader server on first run), so no
 * loader-installer machinery is reinvented here.
 *
 * @param apiWrapper            Generation + config + version-meta + properties.
 * @param platform             The hosting platform, for recursive dependency resolution.
 * @param httpDownloader       Downloads freely-distributable files.
 * @param browserDownloader    Downloads distribution-locked files (headless browser).
 * @param loaderVersionPolicy  Picks the loader-version to install. A policy may prefer an older build it
 *                             already has installed (the grinder does, to reuse its install cache); a crash on
 *                             such a build is re-checked against the policy's newest before it counts.
 * @param workDirectory        Scratch root for the synthetic modpack and generated server pack.
 * @param serverRunner         Executes the prepared pack; defaults to the host-process runner, swapped
 *                             for a container-backed one by the grinder.
 * @param packPostProcessor    Optional hook invoked on the staged pack **after** preparation and
 *                             **before** the boot — e.g. the grinder overlays the cached loader install
 *                             and sets the offline-boot levers. A thrown hook is reported INCONCLUSIVE.
 * @param minecraftAcceptable  Extra gate on the Minecraft version to boot, AND-ed into selection.
 *                             Defaults to accept-all (the host process can run whatever Java it has);
 *                             the grinder passes its image's supported-Java check so a version whose
 *                             JDK the runtime image lacks is never selected (and thus never mis-scored).
 * @param bootTimeout          Budget for install + boot before declaring the run inconclusive.
 * @author Griefed
 */
class BootVerifier(
    private val apiWrapper: ApiWrapper,
    private val platform: ModPlatform,
    private val httpDownloader: JarDownloader,
    private val browserDownloader: JarDownloader,
    private val loaderVersionPolicy: LoaderVersionPolicy,
    private val workDirectory: File,
    private val serverRunner: ServerRunner = HostProcessServerRunner(),
    private val packPostProcessor: ((Prepared.Ready) -> Unit)? = null,
    private val minecraftAcceptable: (String) -> Boolean = { true },
    private val bootTimeout: Duration = Duration.ofMinutes(12)
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Maximum dependency-graph depth to resolve, guarding against cycles/runaway graphs. */
    private val maxDependencyDepth = 4

    /**
     * Result of a single boot-attempt: the verdict, the captured log-file, a human-readable note, and
     * (on a crash) the excerpt of the console-output around the failure for in-comment analysis.
     */
    data class BootOutcome(
        val result: BootResult,
        val logFile: File?,
        val detail: String,
        val crashExcerpt: String? = null
    )

    /**
     * Stage the [project]'s newest file for [loader] (with its required dependencies) into a server
     * pack, run it, and classify the outcome. Any preparation failure (no bootable loader/MC combo,
     * download or generation failure) is reported as [BootResult.INCONCLUSIVE] rather than thrown.
     */
    fun verify(project: ProjectFiles, loader: String): BootOutcome {
        val prepared = prepareBootPack(project, loader)
        if (prepared is Prepared.Failed) {
            return BootOutcome(BootResult.INCONCLUSIVE, null, prepared.detail)
        }
        val ready = prepared as Prepared.Ready
        val outcome = runPrepared(ready, serverRunner, packPostProcessor, bootTimeout)
        return recheckCrashOnNewestVersion(project, loader, ready, outcome)
    }

    /**
     * When [outcome] is a crash produced on a build that is *not* the newest, boot the newest build once and
     * let that decide. This is what makes an install-cache-preferring [LoaderVersionPolicy] safe: without it,
     * a mod that merely needs a newer loader than the cached build would be published as a HIGH-confidence
     * clientside mod. Re-staging repeats the download for that one candidate — crashes are rare, and a wrong
     * HIGH is far more expensive than one extra boot.
     *
     * Anything that stops the re-check from happening leaves the original crash untouched: a crash we cannot
     * disprove stays a crash.
     */
    private fun recheckCrashOnNewestVersion(
        project: ProjectFiles,
        loader: String,
        first: Prepared.Ready,
        outcome: BootOutcome
    ): BootOutcome {
        val newest = loaderVersionPolicy.latestVersion(loader, first.minecraftVersion)
        if (!shouldRecheckCrash(outcome, first.loaderVersion, newest)) {
            return outcome
        }
        log.info(
            "${project.slug}: $loader ${first.loaderVersion} crashed, but that is not the newest build — " +
                "re-checking on $loader $newest before trusting the crash."
        )
        val restaged = prepareBootPack(project, loader, loaderVersionOverride = newest)
        if (restaged is Prepared.Failed) {
            log.warn("Could not re-stage ${project.slug} on $loader $newest (${restaged.detail}); keeping the crash.")
            return outcome
        }
        val second = runPrepared(restaged as Prepared.Ready, serverRunner, packPostProcessor, bootTimeout)
        return reconcileRecheck(outcome, second, first.loaderVersion, newest!!)
    }

    /**
     * Download [file] and, recursively up to [maxDependencyDepth], its required dependencies into
     * [modsDir]. Locked files go through the [browserDownloader], everything else through the
     * [httpDownloader]. Returns false only if the main file itself could not be obtained; a missing
     * dependency is logged but does not abort (the boot may still be meaningful).
     */
    private fun downloadWithDependencies(
        file: ModFile,
        loader: String,
        minecraftVersion: String,
        modsDir: File,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (selectDownloader(file, httpDownloader, browserDownloader).download(file, modsDir) == null) {
            return false
        }
        if (depth >= maxDependencyDepth) {
            return true
        }
        for (dependencyRef in file.requiredDependencies) {
            if (!visited.add(dependencyRef)) {
                continue
            }
            val dependencyProject = platform.resolveDependency(dependencyRef) ?: continue
            val dependencyFile = BootCandidateSelector.pickDependencyFile(dependencyProject.files, loader, minecraftVersion)
            if (dependencyFile == null) {
                log.warn("No $loader file for dependency '$dependencyRef'; booting without it.")
                continue
            }
            downloadWithDependencies(dependencyFile, loader, minecraftVersion, modsDir, visited, depth + 1)
        }
        return true
    }

    /**
     * Generate a self-installing server pack from the synthetic [modpackDir] with mod auto-exclusion
     * disabled (so the candidate mod is kept). Returns the server-pack directory, or `null` on a
     * failed config-check or generation.
     */
    private fun generateServerPack(
        modpackDir: File,
        destination: File,
        minecraftVersion: String,
        loader: String,
        loaderVersion: String
    ): File? {
        // The candidate mod must survive generation, so turn off the scanner-driven exclusion and
        // start from an empty clientside-list for this dedicated verification run.
        apiWrapper.apiProperties.isAutoExcludingModsEnabled = false

        val packConfig = PackConfig()
        packConfig.modpackDir = modpackDir.absolutePath
        packConfig.minecraftVersion = minecraftVersion
        packConfig.modloader = loader
        packConfig.modloaderVersion = loaderVersion
        packConfig.clientMods.clear()
        // Without inclusions the config-check rejects the pack as "empty". Auto-detect the modpack's
        // directories (here: mods) the same way the CLI/GUI would, so the candidate mod is copied in.
        packConfig.inclusions.clear()
        packConfig.inclusions.addAll(apiWrapper.configurationHandler.suggestInclusions(modpackDir.absolutePath))
        packConfig.customDestination = Optional.of(destination)

        val check = apiWrapper.configurationHandler.checkConfiguration(packConfig)
        if (!check.allChecksPassed) {
            log.warn("Config-check failed for $loader $minecraftVersion: ${check.encounteredErrors}")
            return null
        }
        val generation = apiWrapper.serverPackHandler.run(packConfig)
        if (!generation.success) {
            log.warn("Generation failed for $loader $minecraftVersion: ${generation.errors}")
            return null
        }
        return generation.serverPack
    }

    /**
     * Host-side staging shared by every runner: pick the newest bootable file/Minecraft/loader combo,
     * download it plus its required dependencies, and generate a self-installing server pack. Returns a
     * [Prepared.Ready] pointing at the pack (and its log-file target), or [Prepared.Failed] with the
     * reason. Public so the grinder can stage on the host and then hand the pack to its own
     * container-backed [ServerRunner]. [loaderVersionOverride] forces a specific loader-version instead of the
     * policy's preference — used by the crash re-check to re-stage on the newest build.
     */
    fun prepareBootPack(project: ProjectFiles, loader: String, loaderVersionOverride: String? = null): Prepared {
        // Only ever boot a stable Minecraft *release* — a mod's newest file may target a pre-release
        // (a `-pre`/`-rc`/`-snapshot` of the current version), which is unstable and a waste to boot.
        // [minecraftAcceptable] adds the host's own constraint (e.g. the grinder's supported-Java gate).
        val releaseVersions = apiWrapper.versionMeta.minecraft.serverReleases().map { it.minecraftVersion }.toHashSet()
        val candidate = BootCandidateSelector.pickBootableCandidate(project.files, loader) { minecraftVersion ->
            minecraftVersion in releaseVersions &&
                minecraftAcceptable(minecraftVersion) &&
                loaderVersionPolicy.latestVersion(loader, minecraftVersion) != null
        } ?: return Prepared.Failed("No bootable file/Minecraft/loader combination for $loader.")
        val (mainFile, minecraftVersion) = candidate
        val loaderVersion = loaderVersionOverride
            ?: loaderVersionPolicy.preferredVersion(loader, minecraftVersion)
            ?: return Prepared.Failed("No $loader version for Minecraft $minecraftVersion.")

        val attemptDir = File(workDirectory, "${project.slug}-$loader").apply { deleteRecursively() }
        val modsDir = File(attemptDir, "modpack/mods").apply { mkdirs() }

        if (!downloadWithDependencies(mainFile, loader, minecraftVersion, modsDir, mutableSetOf(), 0)) {
            return Prepared.Failed("Could not download ${mainFile.fileName} (or a dependency).")
        }

        val serverPack = generateServerPack(File(attemptDir, "modpack"), File(attemptDir, "serverpack"), minecraftVersion, loader, loaderVersion)
            ?: return Prepared.Failed("Server-pack generation failed for $loader $minecraftVersion.")

        return Prepared.Ready(serverPack, File(attemptDir, "boot.log"), minecraftVersion, loader, loaderVersion)
    }

    /** Result of [prepareBootPack]: a ready-to-run pack, or the reason staging could not finish. */
    sealed interface Prepared {
        /** A generated, self-installing pack ready to boot, plus where its console log should land. */
        data class Ready(
            val serverPack: File,
            val logFile: File,
            val minecraftVersion: String,
            val loader: String,
            val loaderVersion: String
        ) : Prepared

        /** Staging failed (no combo, download or generation failure); [detail] explains why. */
        data class Failed(val detail: String) : Prepared
    }

    companion object {
        private val log by lazy { cachedLoggerOf(BootVerifier::class.java) }

        /**
         * Post-process (optionally), boot, and classify a staged pack — the path shared by [verify] and
         * unit-tested directly (it needs no [ApiWrapper], unlike staging). [packPostProcessor] runs
         * first; a thrown hook is reported INCONCLUSIVE rather than propagated, so a grinder overlay
         * failure can't crash the worker.
         */
        internal fun runPrepared(
            pack: Prepared.Ready,
            serverRunner: ServerRunner,
            packPostProcessor: ((Prepared.Ready) -> Unit)?,
            bootTimeout: Duration
        ): BootOutcome {
            if (packPostProcessor != null) {
                val processing = runCatching { packPostProcessor.invoke(pack) }
                if (processing.isFailure) {
                    return BootOutcome(BootResult.INCONCLUSIVE, null, "Pack post-processing failed: ${processing.exceptionOrNull()?.message}")
                }
            }
            log.info("Booting ${pack.loader} ${pack.loaderVersion} (Minecraft ${pack.minecraftVersion}) server pack at ${pack.serverPack.absolutePath}")
            val runResult = serverRunner.run(pack.serverPack, bootTimeout)
            return outcomeFor(runResult, pack.logFile, "${pack.loader} ${pack.loaderVersion} / Minecraft ${pack.minecraftVersion}")
        }

        /**
         * Turn a [RunResult] into the reported [BootOutcome]: a [RunResult.NotStarted] is INCONCLUSIVE
         * with no log; a [RunResult.Completed] is written to [logFile], classified by
         * [BootLogClassifier], and — only on a crash — given a [BootLogExcerpt]. [label] prefixes the
         * human-readable detail. This is the verdict seam every runner (host or container) shares.
         */
        /**
         * Whether a crash deserves a second boot on the newest loader build: only a CRASHED outcome, only when
         * a newest build is known, and only when it differs from the one that actually crashed.
         */
        internal fun shouldRecheckCrash(outcome: BootOutcome, bootedVersion: String, latestVersion: String?): Boolean =
            outcome.result == BootResult.CRASHED && latestVersion != null && latestVersion != bootedVersion

        /**
         * Combine the original crash with the newest build's re-check. The newest build decides when it says
         * something usable — a clean boot there means the crash belonged to the older build, not the mod — while
         * an INCONCLUSIVE re-check proves nothing and leaves the crash standing. Either way the note records
         * both builds, so nobody reading the report has to guess why two versions are involved.
         */
        internal fun reconcileRecheck(
            first: BootOutcome,
            second: BootOutcome,
            bootedVersion: String,
            latestVersion: String
        ): BootOutcome = when (second.result) {
            BootResult.INCONCLUSIVE -> first.copy(
                detail = "${first.detail} (crash on $bootedVersion could not be re-checked on $latestVersion: ${second.detail})"
            )
            BootResult.CRASHED -> second.copy(
                detail = "${second.detail} (crash on $bootedVersion confirmed on the newest build $latestVersion)"
            )
            BootResult.SURVIVED -> second.copy(
                detail = "${second.detail} (crashed on $bootedVersion but not on the newest build $latestVersion — " +
                    "treating the crash as a loader-build artefact, not the mod)"
            )
        }

        internal fun outcomeFor(runResult: RunResult, logFile: File, label: String): BootOutcome = when (runResult) {
            is RunResult.NotStarted -> BootOutcome(BootResult.INCONCLUSIVE, null, runResult.detail)
            is RunResult.Completed -> {
                logFile.writeText(runResult.lines.joinToString("\n"))
                val result = BootLogClassifier.classify(runResult.lines, runResult.exitCode, runResult.timedOut)
                val crashExcerpt = if (result == BootResult.CRASHED) BootLogExcerpt.crashExcerpt(runResult.lines) else null
                BootOutcome(result, logFile, "$label → $result", crashExcerpt)
            }
        }
    }
}
