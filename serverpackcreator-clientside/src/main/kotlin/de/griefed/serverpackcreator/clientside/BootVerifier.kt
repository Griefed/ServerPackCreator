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
import de.griefed.serverpackcreator.api.modscanning.ModDependency
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
 * @param otherVersionRecheckLimit How many *other* versions of the mod may be booted to disprove a crash
 *                             that contradicts a declared server support. Each one is a full boot, so this
 *                             is a hard budget; `0` switches the second re-check off entirely.
 * @param consoleRules         Supplies the operator's console rules, asked **per attempt** so a rule file
 *                             edited during a run takes effect on the next boot rather than the next
 *                             restart. Defaults to none, i.e. the built-in ladder alone.
 * @param bootArtifactSink     Optional hook handed every attempt's staged pack and its classified outcome,
 *                             **per attempt** — staging wipes the attempt directory, so this is the only
 *                             point at which a re-check's evidence still exists. A thrown sink is logged
 *                             and ignored: keeping evidence must never fail a boot that already ran. Kept
 *                             **last** so a caller can pass it as a trailing lambda.
 * @author Griefed
 */
class BootVerifier(
    private val apiWrapper: ApiWrapper,
    private val platform: ModPlatform,
    private val httpDownloader: JarDownloader,
    private val loaderVersionPolicy: LoaderVersionPolicy,
    private val workDirectory: File,
    private val serverRunner: ServerRunner = HostProcessServerRunner(),
    private val packPostProcessor: ((Prepared.Ready) -> Unit)? = null,
    private val minecraftAcceptable: (String) -> Boolean = { true },
    private val bootTimeout: Duration = Duration.ofMinutes(12),
    private val otherVersionRecheckLimit: Int = 2,
    private val consoleRules: () -> ConsoleRuleSet = { ConsoleRuleSet.EMPTY },
    private val bootArtifactSink: ((Prepared.Ready, BootOutcome) -> Unit)? = null
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Maximum dependency-graph depth to resolve, guarding against cycles/runaway graphs. */
    private val maxDependencyDepth = 4

    /**
     * Boot one prepared attempt with this verifier's collaborators. **The single call site of
     * [runPrepared]**, and deliberately so: an attempt happens three times over — the first boot, the
     * newest-loader-build re-check and each other-version re-check — and anything that must happen per
     * attempt has to be added in exactly one place or it silently covers two of the three. Staging wipes
     * the attempt directory, so a re-check's evidence is gone by the time [verify] returns.
     */
    /**
     * [refuseForSelfDeclaration] with this verifier's scanner supplying the jar's declared Minecraft range.
     * Split so the decision itself stays testable without an [ApiWrapper].
     */
    private fun refuseForSelfDeclaration(jar: File, loader: String, minecraftVersion: String): Prepared.Failed? =
        refuseForSelfDeclaration(jar, loader, minecraftVersion) { candidate ->
            apiWrapper.modScanner.scannerFor(loader, minecraftVersion)
                ?.scan(listOf(candidate))?.singleOrNull()?.minecraftConstraint
        }

    private fun boot(pack: Prepared.Ready): BootOutcome =
        // The rules are asked for per attempt rather than captured once, which is what makes an edit during
        // a multi-day run take effect on the next boot instead of the next restart.
        runPrepared(pack, serverRunner, packPostProcessor, bootTimeout, consoleRules(), bootArtifactSink)

    /**
     * Result of a single boot-attempt: the verdict, the captured log-file, a human-readable note, and
     * (on a crash) the excerpt of the console-output around the failure for in-comment analysis.
     */
    data class BootOutcome(
        /** What the boot proved, as classified from its console and exit status. */
        val result: BootResult,
        /** Where this attempt's console was written, or `null` when the server never launched. */
        val logFile: File?,
        /** Human-readable evidence: the combination booted, the outcome, and how the attempt was reached. */
        val detail: String,
        /** On a crash, the console around the failure, so a report carries the evidence without the whole log. */
        val crashExcerpt: String? = null,
        /**
         * The full console this attempt produced. Held because every attempt for one candidate writes the
         * *same* `boot.log` — a re-check overwrites it — so the reported verdict has to be able to put its
         * own console back; see [restoreDecisiveConsole].
         */
        val console: String? = null,
        /**
         * The loader this attempt actually booted, or `null` when no server ran (staging failed, a hook
         * threw). Stamped by [runPrepared], the one place that knows it.
         *
         * **Why it has to be carried rather than inferred from the caller.** The other-version crash
         * re-check samples across loaders, and [reconcileOtherVersionRecheck] returns the *surviving
         * attempt's own outcome* — so a verdict for one loader can be decided by a boot of another. That is
         * fine as evidence about the mod, and wrong as evidence about the loader: only a loader's own clean
         * boot may disprove another loader's crash, because the entry comparison that guards the published
         * stem is about the build that actually booted.
         */
        val bootedLoader: String? = null,
        /**
         * The id of the operator rule that decided or annotated this outcome, or `null` when the built-in
         * ladder settled it alone. A field rather than only a sentence in [detail], because finding a rule
         * that fires too broadly means *counting* the verdicts it decided.
         */
        val firedRule: String? = null,
        /**
         * The injected dependency this crash appears to belong to, or `null`. **Annotation only** — it never
         * changes [result]; the grinder requeues the named dependency as its own candidate so the question
         * gets answered by grinding it rather than by trusting a string match.
         */
        val blamedDependency: String? = null,
        /** The blamed dependency's project link, so the grinder can queue it for its own verification. */
        val blamedDependencyUrl: String? = null,
        /** The dependency jars staged alongside the candidate, so a verdict names the pack it booted with. */
        val stagedDependencies: List<String> = emptyList(),
        /**
         * Which rung of the classifier ladder settled [result], or `null` when no boot ran. Carried so the
         * grinder's publication gate can refuse a `CRASHED` that is not evidence of sideness — a mixin that
         * would not apply, a solver that gave up, a bare non-zero exit — rather than treating every crash alike.
         */
        val decidedBy: BootDecision? = null
    )

    /**
     * A single re-check attempt on another version of the mod: what was booted, and what came of it. The
     * [label] is what the report shows, so it names the file, the loader and the Minecraft version rather
     * than an index — the loader because the sample deliberately spans loaders, which makes a bare file-name
     * ambiguous about what actually ran.
     */
    data class OtherVersionAttempt(
        /** What was booted, e.g. `ironchest-1.20.1.jar (Forge, Minecraft 1.20.1)`. */
        val label: String,
        /** What that boot produced — including an INCONCLUSIVE standing in for staging that never got to boot. */
        val outcome: BootOutcome
    )

    /**
     * Stage the [project]'s newest file for [loader] (with its required dependencies) into a server
     * pack, run it, and classify the outcome. Any preparation failure (no bootable loader/MC combo,
     * download or generation failure) is reported as [BootResult.INCONCLUSIVE] rather than thrown.
     *
     * [metadataDeclaresServerSupport] is the caller's combined metadata verdict (the platform's self-report
     * plus SPC's jar scan — see `ClientsideVerifier.declaresServerSupport`). When it is `true` and the boot
     * crashes anyway, the two contradict each other, and the crash is re-checked against other versions of
     * the mod before it may stand. It defaults to `false`, which spends no extra boots: a caller that does
     * not know the metadata has nothing for the crash to contradict.
     */
    fun verify(project: ProjectFiles, loader: String, metadataDeclaresServerSupport: Boolean = false): BootOutcome {
        val prepared = prepareBootPack(project, loader)
        if (prepared is Prepared.Failed) {
            // Say so out loud. This reason used to be returned as a detail string and then dropped by
            // `ClientsideVerifier.aggregate` whenever the metadata already decided the confidence, which made a
            // *silently un-booted* catalogue indistinguishable from a booted one — the boot is the only decisive
            // signal this engine has, so "it did not run, and here is why" has to reach the log.
            log.info("Not booting ${project.slug} on $loader: ${prepared.detail}")
            return BootOutcome(BootResult.INCONCLUSIVE, null, prepared.detail)
        }
        val ready = prepared as Prepared.Ready
        val outcome = boot(ready)
        val loaderChecked = recheckCrashOnNewestVersion(project, loader, ready, outcome)
        val decided = recheckCrashOnOtherModVersions(project, loader, ready, loaderChecked, metadataDeclaresServerSupport)
        // Every attempt above wrote the same boot.log, so the file currently holds the *last* boot's console
        // while `decided` may be an earlier one. Put the reported verdict's own console back.
        restoreDecisiveConsole(decided)
        // An inconclusive boot learned nothing, so the *reason* is the whole value of the attempt — a missing
        // loader build, an overlay that could not be staged, a timeout. Without this the log said only
        // "boot:INCONCLUSIVE" and the reason had to be dug out of the per-boot console.
        if (decided.result == BootResult.INCONCLUSIVE) {
            log.info("Boot of ${project.slug} on $loader was inconclusive: ${decided.detail}")
        }
        return decided
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
        // The null check is redundant with shouldRecheckCrash (which is false for a null newest) but stated here so
        // the non-nullness is visible where it is used, rather than resting on another function's contract.
        if (newest == null || !shouldRecheckCrash(outcome, first.loaderVersion, newest)) {
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
        val second = boot(restaged as Prepared.Ready)
        return reconcileRecheck(outcome, second, first.loaderVersion, newest)
    }

    /**
     * When a crash **contradicts** the metadata — the mod claims to support servers, yet the server died —
     * boot up to [otherVersionRecheckLimit] other published combinations of the mod and let them settle the
     * contradiction. A mod that cannot run server-side cannot run server-side in *any* build, so one clean
     * boot elsewhere proves the crash belonged to the build, not to the mod's sideness.
     *
     * **Why it exists:** measured live on 2026-08-23, `iron-chests` — a mod nobody would call clientside —
     * was published `HIGH` off a single crashing build (`Forge 48.1.0 / Minecraft 1.20.2`). One boot cannot
     * tell a broken build from a clientside mod, and the engine resolved that in the direction that writes a
     * wrong entry into the fallback list, which silently strips the mod from every server pack built against
     * it. Stops at the first clean boot, and anything that fails to disprove the crash leaves it standing.
     *
     * **The sample spans loaders, so a candidate is staged under its own loader, not [loader].** See
     * [BootCandidateSelector.pickRecheckCandidates] for why, and why crossing the loader is admissible here
     * and not in `ClientsideVerifier.loaderDisprovingTheCrash`. Every attempt nonetheless stages into the
     * *crashing* loader's directory: all attempts for one candidate share one `boot.log`, which
     * [restoreDecisiveConsole] repairs at the end of [verify], and staging into another loader's directory
     * would wipe the pack and console that loader's own verdict is about to be built from.
     */
    private fun recheckCrashOnOtherModVersions(
        project: ProjectFiles,
        loader: String,
        booted: Prepared.Ready,
        outcome: BootOutcome,
        metadataDeclaresServerSupport: Boolean
    ): BootOutcome {
        if (!shouldRecheckAgainstOtherVersions(outcome, metadataDeclaresServerSupport, otherVersionRecheckLimit)) {
            return outcome
        }
        val candidates = BootCandidateSelector.pickRecheckCandidates(
            project.files,
            loader,
            booted.minecraftVersion,
            otherVersionRecheckLimit,
            bootableCombination()
        )
        val contradiction = "${project.slug}: $loader crashed on Minecraft ${booted.minecraftVersion} " +
            "although the metadata declares server support"
        if (candidates.isEmpty()) {
            log.info("$contradiction, and the mod publishes no other bootable combination to re-check against.")
        } else {
            log.info(
                "$contradiction — re-checking ${candidates.size} other combination(s) before trusting the crash: " +
                    candidates.joinToString(", ") { "${it.loader} / Minecraft ${it.minecraftVersion}" }
            )
        }
        val attempts = mutableListOf<OtherVersionAttempt>()
        for ((file, candidateLoader, minecraftVersion) in candidates) {
            val label = "${file.fileName} ($candidateLoader, Minecraft $minecraftVersion)"
            val staged = stageBootPack(
                project,
                candidateLoader,
                file,
                minecraftVersion,
                loaderVersionOverride = null,
                attemptDirName = AttemptDirectory.nameFor(project.platform, project.slug, loader)
            )
            if (staged is Prepared.Failed) {
                log.warn("Could not re-stage ${project.slug} as $label: ${staged.detail}")
                attempts.add(OtherVersionAttempt(label, BootOutcome(BootResult.INCONCLUSIVE, null, staged.detail)))
                continue
            }
            val attempt = boot(staged as Prepared.Ready)
            attempts.add(OtherVersionAttempt(label, attempt))
            // A single clean boot is all the proof needed, and every further one costs a full boot.
            if (attempt.result == BootResult.SURVIVED) {
                break
            }
        }
        return reconcileOtherVersionRecheck(outcome, attempts)
    }

    /**
     * Download [file] and, recursively up to [maxDependencyDepth], its required dependencies into
     * [modsDir] via the [httpDownloader]. Returns false only if the main file itself could not be obtained,
     * which for a distribution-locked file is certain — it has no published URL.
     *
     * Every required dependency that could **not** be staged — unresolvable ref, no usable file, or a failed
     * download — is collected into [unsatisfied] instead of being shrugged off. The caller refuses to boot when that
     * set is non-empty (see [refuseForMissingDependencies]): a mod the loader rejects for missing dependencies never
     * runs its own code, so the boot cannot say anything about sideness.
     */
    private fun downloadWithDependencies(
        file: ModFile,
        loader: String,
        minecraftVersion: String,
        modsDir: File,
        visited: MutableSet<String>,
        depth: Int,
        unsatisfied: MutableSet<String>,
        unmapped: MutableSet<String>,
        injected: MutableList<InjectedDependency>
    ): Boolean {
        val staged = httpDownloader.download(file, modsDir)
            ?: return false
        if (depth > 0 && injected.none { it.fileName == file.fileName }) {
            // Only dependencies count towards the cap and the recorded set; the candidate is not one.
            //
            // Deduped by FILE, not by ref, because a project is reachable under two equally-valid
            // identifiers -- the platform ref the author linked (`P7dR8mSH`) and the mod id its manifest
            // declares (`fabric`, which ModIdRegistry maps to the slug `fabric-api`). `stageableRequirements`
            // dedupes by ref and cannot see that those are one project. The duplicate is cosmetic in the
            // report but not against the cap: it refuses a pack that is within it, and a refusal is scored
            // INCONCLUSIVE, so the mod quietly stops being verified.
            injected.add(InjectedDependency(file.fileName, null, file.pageUrl))
        }
        if (depth >= maxDependencyDepth) {
            return true
        }
        for (dependencyRef in file.requiredDependencies) {
            if (!visited.add(dependencyRef)) {
                continue
            }
            val dependencyProject = platform.resolveDependency(dependencyRef)
            if (dependencyProject == null) {
                // Previously a silent `continue`, which is how missing dependencies went unnoticed for so long.
                log.warn("Required dependency '$dependencyRef' could not be resolved on its platform.")
                unsatisfied.add(dependencyRef)
                continue
            }
            val dependencyFile = BootCandidateSelector.pickDependencyFile(dependencyProject.files, loader, minecraftVersion)
            if (dependencyFile == null) {
                log.warn("Required dependency '$dependencyRef' publishes no $loader file for Minecraft $minecraftVersion.")
                unsatisfied.add(dependencyRef)
                continue
            }
            if (!downloadWithDependencies(
                    dependencyFile, loader, minecraftVersion, modsDir, visited, depth + 1, unsatisfied, unmapped, injected
                )
            ) {
                log.warn("Required dependency '$dependencyRef' (${dependencyFile.fileName}) could not be downloaded.")
                unsatisfied.add(dependencyRef)
            }
        }
        stageManifestDependencies(staged, file, loader, minecraftVersion, modsDir, visited, depth, unsatisfied, unmapped, injected)
        return true
    }

    /**
     * Stage the dependencies [staged]'s **jar manifest** declares but its platform metadata did not.
     *
     * The two sources overlap heavily — a well-formed project declares its dependencies in both — so
     * anything already visited is skipped rather than downloaded twice. What this adds is the case the
     * platform never sees: an author who declared a dependency only in `fabric.mod.json`. Fabric API is
     * the one that matters, and until the `-api` exclusion fix it was not even reported as a dependency.
     *
     * **An id that maps to no project goes to [unmapped], not [unsatisfied]**, so it can never refuse the
     * boot: a manifest id may name something bundled inside another jar, provided by the loader, or
     * optional in practice, and refusing on it would turn working boots into INCONCLUSIVE.
     */
    private fun stageManifestDependencies(
        staged: File,
        file: ModFile,
        loader: String,
        minecraftVersion: String,
        modsDir: File,
        visited: MutableSet<String>,
        depth: Int,
        unsatisfied: MutableSet<String>,
        unmapped: MutableSet<String>,
        injected: MutableList<InjectedDependency>
    ) {
        if (depth >= maxDependencyDepth) {
            return
        }
        val scanner = apiWrapper.modScanner.scannerFor(loader, minecraftVersion) ?: return
        val declared = runCatching { scanner.scan(listOf(staged)).flatMap { it.dependencies } }
            .onFailure { log.debug("Could not read ${file.fileName}'s manifest dependencies: ${it.message}") }
            .getOrDefault(emptyList())

        for (requirement in stageableRequirements(declared, visited) { platformRefFor(it) }) {
            // `visited` is claimed here rather than inside the planner, which keeps the planner pure: a ref
            // seen once must not be resolved twice even when the first attempt came to nothing.
            val alreadySeen = platformRefFor(requirement.modID)?.let { !visited.add(it) } ?: false
            if (alreadySeen) {
                continue
            }
            val plan = planManifestDependency(
                requirement, loader, minecraftVersion,
                refFor = { platformRefFor(it) },
                resolveRef = { platform.resolveDependency(it) }
            )
            val dependencyFile = when (plan) {
                is ManifestDependencyPlan.Unmapped -> {
                    log.info("Manifest dependency '${plan.modID}' maps to nothing this platform carries.")
                    unmapped.add(plan.modID)
                    continue
                }

                is ManifestDependencyPlan.Unsatisfied -> {
                    // Mapped AND resolved, then nothing usable: a case we chose to trust, so it refuses.
                    log.warn("Manifest dependency '${plan.modID}' publishes no $loader file for Minecraft $minecraftVersion.")
                    unsatisfied.add(plan.modID)
                    continue
                }

                is ManifestDependencyPlan.Stage -> plan.file
            }
            if (!downloadWithDependencies(
                    dependencyFile, loader, minecraftVersion, modsDir, visited, depth + 1, unsatisfied, unmapped, injected
                )
            ) {
                // Mapped AND resolved, then failed to stage: a case we chose to trust, so it refuses.
                log.warn("Manifest dependency '${requirement.modID}' (${dependencyFile.fileName}) could not be downloaded.")
                unsatisfied.add(requirement.modID)
            }
        }
    }

    /** This platform's ref for a manifest mod id, via [KnownModIds]. */
    private fun platformRefFor(modId: String): String? = KnownModIds.refFor(modId, platform.name)

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
        val bootable = bootableCombination()
        val candidate = BootCandidateSelector.pickBootableCandidate(project.files, loader) { bootable(loader, it) }
            ?: return Prepared.Failed("No bootable file/Minecraft/loader combination for $loader.")
        val (mainFile, minecraftVersion) = candidate
        val staged = stageBootPack(project, loader, mainFile, minecraftVersion, loaderVersionOverride)
        return reselectOnMinecraftContradiction(staged, project, loader, mainFile, loaderVersionOverride, bootable)
    }

    /**
     * Answer a "the jar excludes this Minecraft version" refusal by re-staging on the newest version the
     * jar *does* accept, or hand [staged] back untouched when that is not the refusal or there is no such
     * version.
     *
     * **Why this exists.** Selection can only see platform metadata — the jar is not downloaded yet — so it
     * takes the newest tagged version, and the descriptor gate may then contradict it. Measured on JEI:
     * `jei-1.21.1-forge-19.52.0.422.jar` is tagged for 1.21 and 1.21.1 but declares `[1.21, 1.21.1)`, so
     * the pick was vetoed and the boot lost even though 1.21 satisfies platform and jar alike. A refusal
     * costs a `BootResult.INCONCLUSIVE`, which overwrites a decisive verdict, so throwing the candidate
     * away is the expensive outcome, not the safe one.
     *
     * Exactly one retry, by calling [stageBootPack] rather than [prepareBootPack]: the re-selected version
     * satisfies the constraint that caused this refusal, so a second contradiction would be a different
     * fault and must surface rather than loop.
     */
    private fun reselectOnMinecraftContradiction(
        staged: Prepared,
        project: ProjectFiles,
        loader: String,
        mainFile: ModFile,
        loaderVersionOverride: String?,
        bootable: (String, String) -> Boolean
    ): Prepared {
        if (staged !is Prepared.Failed) {
            return staged
        }
        val constraint = staged.declaredMinecraftConstraint ?: return staged
        val agreed = BootCandidateSelector.newestVersionSatisfying(mainFile, constraint) { bootable(loader, it) }
            ?: return staged
        log.info(
            "${mainFile.fileName} declares Minecraft '$constraint', so re-staging ${project.slug} on " +
                "$loader $agreed — the newest version its own descriptor accepts."
        )
        return stageBootPack(project, loader, mainFile, agreed, loaderVersionOverride)
    }

    /**
     * Whether a given loader can actually be booted on a given Minecraft version. Only a stable Minecraft
     * *release* qualifies — a mod's newest file may target a pre-release (a `-pre`/`-rc`/`-snapshot`), which
     * is unstable and a waste to boot — AND-ed with [minecraftAcceptable] (the host's own constraint, e.g.
     * the grinder's supported-Java gate) and the loader actually having a build. The release set is read
     * once per call, so a selection that probes many combinations does not re-read SPC's metadata for each.
     *
     * Takes the loader per call rather than closing over one, because the crash re-check's sample spans
     * loaders and has to gate each candidate against its own.
     */
    private fun bootableCombination(): (String, String) -> Boolean {
        val releaseVersions = apiWrapper.versionMeta.minecraft.serverReleases().map { it.minecraftVersion }.toHashSet()
        return { loader, minecraftVersion ->
            minecraftVersion in releaseVersions &&
                minecraftAcceptable(minecraftVersion) &&
                loaderVersionPolicy.latestVersion(loader, minecraftVersion) != null
        }
    }

    /**
     * Stage one *chosen* (file, loader, Minecraft-version) combination: download [mainFile] plus its
     * required dependencies and generate the self-installing server pack. Split out of [prepareBootPack] so
     * a re-check can stage a combination it picked itself instead of the newest one selection would return.
     *
     * [attemptDirName] names the scratch directory, which staging wipes. It defaults to this [loader]'s own,
     * and the cross-loader crash re-check overrides it with the *crashing* loader's — a candidate booted
     * under another loader must not wipe the pack and console that loader's own verdict is built from.
     */
    private fun stageBootPack(
        project: ProjectFiles,
        loader: String,
        mainFile: ModFile,
        minecraftVersion: String,
        loaderVersionOverride: String?,
        attemptDirName: String = AttemptDirectory.nameFor(project.platform, project.slug, loader)
    ): Prepared {
        val loaderVersion = loaderVersionOverride
            ?: loaderVersionPolicy.preferredVersion(loader, minecraftVersion)
            ?: return Prepared.Failed("No $loader version for Minecraft $minecraftVersion.")

        val attemptDir = File(workDirectory, attemptDirName).apply { deleteRecursively() }
        val modsDir = File(attemptDir, "modpack/mods").apply { mkdirs() }

        val unsatisfied = mutableSetOf<String>()
        val unmapped = mutableSetOf<String>()
        val injected = mutableListOf<InjectedDependency>()
        if (!downloadWithDependencies(
                mainFile, loader, minecraftVersion, modsDir, mutableSetOf(), 0, unsatisfied, unmapped, injected
            )
        ) {
            return Prepared.Failed(downloadFailureDetail(mainFile))
        }
        // Ask the jar what it says about itself before spending a container on it. The platform's declared
        // loader and Minecraft sets are what an author ticked; the descriptor is what the jar was built
        // against, and where the two disagree the boot can only fail for reasons that are not sideness.
        refuseForSelfDeclaration(File(modsDir, mainFile.fileName), loader, minecraftVersion)?.let { return it }
        refuseForMissingDependencies(unsatisfied, loader, minecraftVersion)?.let { return it }
        refuseForTooManyDependencies(injected.map { it.fileName }, loader, minecraftVersion)?.let { return it }
        unmappedDependencyNote(unmapped)?.let { log.warn(it) }

        val serverPack = generateServerPack(File(attemptDir, "modpack"), File(attemptDir, "serverpack"), minecraftVersion, loader, loaderVersion)
            ?: return Prepared.Failed("Server-pack generation failed for $loader $minecraftVersion.")

        return Prepared.Ready(
            serverPack, File(attemptDir, "boot.log"), minecraftVersion, loader, loaderVersion,
            injectedDependencies = injected.toList(),
            candidateStem = FilenameStemDeriver.deriveStem(listOf(mainFile.fileName))
        )
    }

    /** Result of [prepareBootPack]: a ready-to-run pack, or the reason staging could not finish. */
    sealed interface Prepared {
        /** A generated, self-installing pack ready to boot, plus where its console log should land. */
        data class Ready(
            /** The generated pack's root, mounted or launched as-is; its `start.sh` installs the loader itself. */
            val serverPack: File,
            /** Where this attempt's console is written. One per attempt directory, so a re-stage overwrites it. */
            val logFile: File,
            /** The Minecraft version this attempt boots, which decides the required Java. */
            val minecraftVersion: String,
            /** The modloader this attempt boots — the loader the resulting verdict is about. */
            val loader: String,
            /** The loader build being booted. May be older than the newest; a crash on one is re-checked. */
            val loaderVersion: String,
            /** The dependency jars staged beside the candidate, for attribution and for the verdict record. */
            val injectedDependencies: List<InjectedDependency> = emptyList(),
            /** The candidate's own file-name stem, so attribution can tell its frames from a dependency's. */
            val candidateStem: String? = null
        ) : Prepared {
            /**
             * This attempt's staging directory name — the `(platform, slug, loader)` tuple
             * [AttemptDirectory] builds every staging path from. Derived from [logFile]'s parent rather
             * than carried as three more fields, and correct for the other-version re-check too, which
             * deliberately stages into the *crashing* loader's directory rather than its own.
             */
            val attemptName: String get() = logFile.parentFile?.name.orEmpty()
        }

        /** Staging failed (no combo, download or generation failure); [detail] explains why. */
        data class Failed(
            /** The named reason staging stopped, carried into the report instead of a bare "could not boot". */
            val detail: String,
            /**
             * The staged jar's own declared Minecraft range, verbatim, set **only** when that range is why
             * staging stopped — i.e. the jar excludes the version being staged. `null` for every other
             * refusal, including a loader-descriptor mismatch.
             *
             * Carried so [prepareBootPack] can re-select a version the jar *does* accept rather than throw
             * the candidate away. It is deliberately narrower than "the refusal reason": a jar carrying the
             * wrong loader's descriptor offers no second version to try, whereas a Minecraft range usually
             * does, because the platform commonly tags more versions than the descriptor admits.
             */
            val declaredMinecraftConstraint: String? = null
        ) : Prepared
    }

    /**
     * The verifier's logger plus the pure decision helpers the crash re-checks are built from — they live here
     * precisely because they need no verifier state, which is what makes them unit-testable while `verify`
     * itself needs an `ApiWrapper` and a running server.
     */
    companion object {
        /**
         * Why a candidate jar could not be staged, phrased so a reader can tell a *permanent* refusal from a
         * transient one. A [ModFile.locked] file is CurseForge's `allowModDistribution=false`: the author
         * opted out of third-party distribution, so no `downloadUrl` is published and the file is simply not
         * obtainable — not a host problem, and not a fault of the mod. Saying so matters because 21 live
         * verdicts once read only "could not download", which a 404, a flaky link and a deliberate opt-out
         * all produce identically.
         */
        internal fun downloadFailureDetail(file: ModFile): String =
            if (file.locked) {
                "Could not download ${file.fileName}: the file is distribution-locked " +
                    "(allowModDistribution=false), so CurseForge publishes no download URL for it and this " +
                    "mod cannot be boot-verified from that platform. A project published on Modrinth is " +
                    "verified from there instead, where files carry a URL."
            } else {
                "Could not download ${file.fileName}."
            }

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
            bootTimeout: Duration,
            rules: ConsoleRuleSet = ConsoleRuleSet.EMPTY,
            bootArtifactSink: ((Prepared.Ready, BootOutcome) -> Unit)? = null
        ): BootOutcome {
            if (packPostProcessor != null) {
                val processing = runCatching { packPostProcessor.invoke(pack) }
                if (processing.isFailure) {
                    return BootOutcome(BootResult.INCONCLUSIVE, null, "Pack post-processing failed: ${processing.exceptionOrNull()?.message}")
                }
            }
            log.info(
                "Booting ${pack.loader} ${pack.loaderVersion} (Minecraft ${pack.minecraftVersion}) — " +
                    "live console: ${pack.logFile.absolutePath}"
            )
            // Stream the console into the attempt's log file as it arrives, so an operator can `tail -f` a boot
            // that is still running, and so a boot killed mid-flight still leaves its output behind. `outcomeFor`
            // rewrites the same file from the complete list afterwards. A failing sink must never fail a boot.
            val liveLog = runCatching { pack.logFile.also { it.parentFile?.mkdirs() }.bufferedWriter() }.getOrNull()
            val runResult = try {
                serverRunner.run(pack.serverPack, bootTimeout) { line ->
                    runCatching {
                        liveLog?.appendLine(line)
                        liveLog?.flush()
                    }
                }
            } finally {
                runCatching { liveLog?.close() }
            }
            // Stamped here rather than inside `outcomeFor`, which classifies a console and has no business
            // knowing what was booted; this is the one place that does.
            val outcome = outcomeFor(runResult, pack.logFile, "${pack.loader} ${pack.loaderVersion} / Minecraft ${pack.minecraftVersion}", rules)
                .copy(bootedLoader = pack.loader, stagedDependencies = pack.injectedDependencies.map { it.fileName })
                // Annotation only: `attribute` returns an outcome whose result is this one's, always.
                .let { attribute(it, pack.injectedDependencies, pack.candidateStem) }
            // Per attempt, and here rather than after `verify` returns: staging wipes and re-creates the
            // attempt directory, so by the time a verdict is decided every earlier attempt's pack is gone.
            // Guarded like the live-log sink above -- keeping evidence must never fail a boot that already ran.
            if (bootArtifactSink != null) {
                runCatching { bootArtifactSink.invoke(pack, outcome) }
                    .onFailure { log.warn("Could not keep the boot artifacts for ${pack.attemptName}: ${it.message}") }
            }
            return outcome
        }

        /**
         * Refuse to boot when a required dependency could not be staged, returning the reason — or `null` when
         * everything needed is present and the boot may proceed.
         *
         * **Why refuse rather than boot anyway:** a loader that rejects a mod for missing dependencies never runs the
         * mod's code, so the run cannot distinguish client-only from server-safe; it just produces a non-zero exit
         * that *looks* like a crash. Measured 2026-07-30 across 112 kept boot logs, 36 failed exactly that way — the
         * largest failure class — each burning a full boot (~70 s) to learn nothing. Reporting the unmet dependency
         * is both honest and actionable, where a "crash" would have been neither.
         */
        /**
         * Refuse a boot the staged jar's own descriptor contradicts, or `null` to go ahead.
         *
         * Scans the jar for its declared Minecraft range and compares that, plus the descriptors it carries,
         * against what is about to be booted. **Every uncertainty accepts** — see [JarSelfDeclaration]; a
         * scan that throws is caught here for the same reason, because a gate that refuses on doubt turns a
         * descriptor gap into a catalog-wide mass-INCONCLUSIVE event.
         */
        internal fun refuseForSelfDeclaration(
            jar: File,
            loader: String,
            minecraftVersion: String,
            minecraftConstraint: (File) -> String?
        ): Prepared.Failed? {
            val declared = runCatching { minecraftConstraint(jar) }.getOrNull()
            val contradiction = JarSelfDeclaration.contradiction(jar, loader, minecraftVersion, declared)
                ?: return null
            // Re-asked rather than inferred from `contradiction` being non-null: that string is also how a
            // loader-descriptor mismatch reports itself, and only the Minecraft disagreement can be answered
            // by trying another version. Getting this wrong would re-select on a refusal re-selection cannot fix.
            val minecraftDisagreement = declared?.takeIf { !VersionConstraint.satisfies(minecraftVersion, it) }
            return Prepared.Failed(
                "Refusing to boot $loader on Minecraft $minecraftVersion: $contradiction. " +
                    "The platform's declared versions are what its author ticked, not what the jar was built for.",
                declaredMinecraftConstraint = minecraftDisagreement
            )
        }

        internal fun refuseForMissingDependencies(
            unsatisfied: Set<String>,
            loader: String,
            minecraftVersion: String
        ): Prepared.Failed? =
            if (unsatisfied.isEmpty()) {
                null
            } else {
                Prepared.Failed(
                    "Required ${if (unsatisfied.size == 1) "dependency" else "dependencies"} unavailable for " +
                        "$loader / Minecraft $minecraftVersion: ${unsatisfied.sorted().joinToString(", ")}. " +
                        "Not booting — a mod refused for missing dependencies says nothing about sideness."
                )
            }

        /**
         * Where one manifest-declared requirement lands, without touching the network or the disk.
         *
         * Extracted because the decision used to live in three adjacent branches of the staging loop and
         * they had drifted: the download failure refused, while "resolved but nothing usable" did not,
         * even though both are the same case by the rule below. `CurseForge/attributefix` at Minecraft
         * 1.21.11 booted without the Fabric API its manifest hard-requires because of it, and Quilt Loader
         * blamed the mod.
         *
         * The rule, and it is the one [stageManifestDependencies] documents: an id we mapped to a real
         * project and then failed to stage is a case we chose to trust, so failing it is a real gap and
         * refuses. An id that maps to nothing, or to a project this platform does not carry, is only a
         * guess that missed and never refuses.
         *
         * @param refFor     This platform's ref for a mod id, or `null` when the registry knows none.
         * @param resolveRef The project behind a ref, or `null` when the platform does not carry it.
         */
        internal fun planManifestDependency(
            requirement: ModDependency,
            loader: String,
            minecraftVersion: String,
            refFor: (String) -> String?,
            resolveRef: (String) -> ProjectFiles?
        ): ManifestDependencyPlan {
            val ref = refFor(requirement.modID) ?: return ManifestDependencyPlan.Unmapped(requirement.modID)
            val project = resolveRef(ref) ?: return ManifestDependencyPlan.Unmapped(requirement.modID)
            val file = BootCandidateSelector.pickDependencyFile(
                project.files, loader, minecraftVersion, requirement.versionConstraint
            ) ?: return ManifestDependencyPlan.Unsatisfied(requirement.modID)
            return ManifestDependencyPlan.Stage(ref, file)
        }

        /**
         * Ids the environment provides rather than the pack: never staged, whatever a descriptor says.
         *
         * **`quilt_base` is not one of them**, though it was listed here until 2026-09-01. It is QSL's base
         * module, shipped by QFAPI, so a mod declaring it needs a jar staged exactly as one declaring
         * `quilt_resource_loader` does — `KnownModIds` resolves both to QSL. Only the loaders and the
         * runtime belong here.
         */
        private val environmentProvidedIds = setOf(
            "minecraft", "java", "fabricloader", "forge", "neoforge", "quilt_loader"
        )

        /**
         * Largest number of dependency jars that may be staged alongside a candidate.
         *
         * Beyond it the boot is refused rather than attempted: a forty-jar pack that fails says nothing
         * about the candidate, because any one of the forty could be the cause.
         */
        const val MAX_INJECTED_DEPENDENCIES = 12

        /**
         * The manifest-declared [requirements] worth *staging*: the environment's own ids dropped, and
         * anything the platform already resolved dropped too.
         *
         * [alreadyResolved] holds the platform refs staged from `ModFile.requiredDependencies`, so a
         * requirement mapping onto one of them is not downloaded a second time — the two sources overlap
         * heavily, since a well-formed project declares its dependencies in both places.
         */
        internal fun stageableRequirements(
            requirements: List<ModDependency>,
            alreadyResolved: Set<String> = emptySet(),
            refFor: (String) -> String? = { it }
        ): List<ModDependency> = requirements.filterNot { requirement ->
            // An optional dependency is neither staged nor allowed to refuse a boot: the descriptor itself
            // says the mod loads without it. `advancement-plaques` declares `prism` and `toastcontrol`
            // `mandatory=false` and was refused for "Required dependency unavailable ... prism", which cost
            // an INCONCLUSIVE on a mod that never required it. Both platforms already filter their own side
            // (`dependency_type == "required"`, `relationType == 3`); this is the manifest half of that rule.
            requirement.optional ||
                requirement.modID.lowercase() in environmentProvidedIds ||
                refFor(requirement.modID)?.let { it in alreadyResolved } == true
        }

        /**
         * A note naming the manifest ids that mapped to no project, or `null` when every one resolved.
         *
         * These deliberately do **not** refuse the boot — see the class doc on the refusal split — but they
         * must still be *said*, or a gap in [KnownModIds] is invisible: the boot would simply be a little
         * less faithful for reasons nobody could see in the verdict.
         */
        internal fun unmappedDependencyNote(unmapped: Set<String>): String? =
            unmapped.takeIf { it.isNotEmpty() }?.let {
                "Manifest dependencies that could not be resolved to a project (booted without them): " +
                    it.sorted().joinToString(", ") + "."
            }

        /**
         * Refuse a boot whose dependency graph grew past [MAX_INJECTED_DEPENDENCIES], or `null` when it did
         * not. A pack this size cannot produce evidence about the candidate specifically.
         */
        internal fun refuseForTooManyDependencies(
            injected: List<String>,
            loader: String,
            minecraftVersion: String
        ): Prepared.Failed? {
            // Counted by distinct FILE. The cap asks "how big is this pack", and one jar reachable under two
            // refs is one jar -- see `aJarStagedUnderTwoRefsCountsOnceAgainstTheCap`. The staging site dedupes
            // too; this keeps the cap's own contract honest for any caller.
            val distinct = injected.distinct()
            return if (distinct.size <= MAX_INJECTED_DEPENDENCIES) {
                null
            } else {
                Prepared.Failed(
                    "Staging ${distinct.size} dependencies for $loader / Minecraft $minecraftVersion exceeds " +
                        "the cap of $MAX_INJECTED_DEPENDENCIES. Not booting — a pack that large cannot say " +
                        "anything about this mod specifically."
                )
            }
        }

        /**
         * Annotate [outcome] with the injected dependency its crash names, if any.
         *
         * **Returns an outcome whose [BootOutcome.result] is always the input's.** Only a crash is
         * considered at all, and even then the verdict is untouched: this exists to make a suspicion
         * *visible and countable*, not to overrule the boot. `attributionNeverChangesTheBootResult` pins it.
         */
        internal fun attribute(
            outcome: BootOutcome,
            injected: List<InjectedDependency>,
            candidateStem: String?
        ): BootOutcome {
            if (outcome.result != BootResult.CRASHED) {
                return outcome
            }
            val blamed = DependencyAttribution.blame(
                outcome.console?.lines().orEmpty(), injected, candidateStem
            ) ?: return outcome
            return outcome.copy(
                detail = outcome.detail + " [crash names the injected dependency ${blamed.fileName}; " +
                    "it has been queued for its own verification]",
                blamedDependency = blamed.fileName,
                blamedDependencyUrl = blamed.projectUrl
            )
        }

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

        /**
         * Whether a crash is worth spending boots on *other versions of the mod*: only a CRASHED outcome,
         * only when the metadata claims server support (so the two signals contradict each other), and only
         * within a non-zero boot budget.
         *
         * Deliberately narrow. Where the metadata already leans clientside the crash *confirms* it, and in a
         * catalog sweep that agreement is the common case — re-checking it would spend boots to learn nothing
         * while the crawl falls behind. The contradiction is the only case where one of the signals must be
         * wrong, and therefore the only case worth paying to resolve.
         */
        internal fun shouldRecheckAgainstOtherVersions(
            outcome: BootOutcome,
            metadataDeclaresServerSupport: Boolean,
            limit: Int
        ): Boolean = outcome.result == BootResult.CRASHED && metadataDeclaresServerSupport && limit > 0

        /**
         * Fold the other-version [attempts] into the verdict for the crash in [first]. One clean boot wins
         * outright — a mod that cannot run server-side cannot run server-side in any build, so the crash was
         * that build's — while crashes elsewhere corroborate it and attempts that learned nothing leave it
         * exactly as it was. Every branch records what was tried, so a reader of the report can see how large
         * the sample behind the verdict was instead of guessing.
         *
         * The winning attempt's own outcome is what gets returned, so a verdict may report a boot run under a
         * *different* loader than the one it is about. That is why every attempt's `label` names its loader:
         * the detail has to say what actually ran, not merely that something did.
         */
        internal fun reconcileOtherVersionRecheck(first: BootOutcome, attempts: List<OtherVersionAttempt>): BootOutcome {
            val survivor = attempts.firstOrNull { it.outcome.result == BootResult.SURVIVED }
            if (survivor != null) {
                return survivor.outcome.copy(
                    detail = "${survivor.outcome.detail} (${first.detail}, but ${survivor.label} booted cleanly — " +
                        "the crash belongs to that build of the mod, not to its sideness)"
                )
            }
            val crashed = attempts.filter { it.outcome.result == BootResult.CRASHED }
            if (crashed.isNotEmpty()) {
                return first.copy(
                    detail = "${first.detail} (also crashed on ${crashed.joinToString(", ") { it.label }})"
                )
            }
            val unusable = if (attempts.isEmpty()) {
                "no other version of the mod was bootable to re-check against"
            } else {
                "could not be re-checked on other versions: " +
                    attempts.joinToString("; ") { "${it.label}: ${it.outcome.detail}" }
            }
            return first.copy(detail = "${first.detail} ($unusable)")
        }

        /**
         * Turn a [RunResult] into the reported [BootOutcome]: a [RunResult.NotStarted] is INCONCLUSIVE
         * with no log; a [RunResult.Completed] is written to [logFile], classified by
         * [BootLogClassifier], and — only on a crash — given a [BootLogExcerpt]. [label] prefixes the
         * human-readable detail. This is the verdict seam every runner (host or container) shares.
         */
        internal fun outcomeFor(
            runResult: RunResult,
            logFile: File,
            label: String,
            rules: ConsoleRuleSet = ConsoleRuleSet.EMPTY
        ): BootOutcome = when (runResult) {
            is RunResult.NotStarted -> BootOutcome(BootResult.INCONCLUSIVE, null, runResult.detail)
            is RunResult.Completed -> {
                val console = runResult.lines.joinToString("\n")
                // Persisting the console must never fail the verification: the verdict comes from the lines in
                // memory, and an unwritable log (a full disk, a path that is a directory) is a diagnostics
                // problem, not a reason to lose a boot that already ran.
                runCatching { logFile.writeText(console) }
                    .onFailure { log.warn("Could not write the boot log ${logFile.absolutePath}: ${it.message}") }
                val classified = BootLogClassifier.classify(runResult.lines, runResult.exitCode, runResult.timedOut, rules)
                val result = classified.result
                val crashExcerpt = if (result == BootResult.CRASHED) BootLogExcerpt.crashExcerpt(runResult.lines) else null
                // The exit status is *the* input that decides CRASHED vs INCONCLUSIVE when no ready-line appeared, so
                // record it. Without it an INCONCLUSIVE verdict is undiagnosable from the report alone: a run that
                // crashed loudly in its console but reported exit 0 looks identical to one that never started.
                val exitDetail = if (runResult.timedOut) "timed out" else "exit ${runResult.exitCode ?: "unknown"}"
                // A rule that had a hand in this says so in the detail *and* in the field beside it: the
                // sentence is for whoever reads the report, the field is for whoever has to count how often
                // a rule fired before deciding it is too broad.
                val ruleNote = classified.firedRule?.let { match ->
                    " [rule '${match.rule.id}'" + (match.rule.note?.let { ": $it" } ?: "") + "]"
                } ?: ""
                BootOutcome(
                    result, logFile, "$label → $result ($exitDetail)$ruleNote", crashExcerpt, console,
                    firedRule = classified.firedRule?.rule?.id,
                    decidedBy = classified.decidedBy
                )
            }
        }

        /**
         * Put [outcome]'s own console back into its log file, undoing a later attempt's overwrite.
         *
         * Every attempt for one candidate stages into the same directory — staging wipes it — so all of them
         * write the same `boot.log`, while the *reported* verdict is frequently not the last one booted: both
         * crash re-checks keep the original crash. The grinder's reaper then keeps that single file and
         * deletes the staging around it, so the console a HIGH is diagnosed from would be a different boot's.
         * Best-effort, exactly like the write it repairs: an unwritable log is a diagnostics problem, never a
         * reason to lose a verdict.
         */
        internal fun restoreDecisiveConsole(outcome: BootOutcome) {
            val logFile = outcome.logFile ?: return
            val console = outcome.console ?: return
            runCatching { logFile.writeText(console) }
                .onFailure { log.warn("Could not restore the boot log ${logFile.absolutePath}: ${it.message}") }
        }
    }
}

/**
 * What [BootVerifier.planManifestDependency] decided about one manifest-declared requirement.
 *
 * A type rather than a pair of booleans because the three outcomes carry different things and are treated
 * differently: only [Unsatisfied] refuses the boot.
 *
 * @author Griefed
 */
internal sealed interface ManifestDependencyPlan {
    /** Stage [file], fetched under [ref]. */
    data class Stage(val ref: String, val file: ModFile) : ManifestDependencyPlan

    /** A guess that missed: nothing maps, or the platform does not carry it. Reported, never fatal. */
    data class Unmapped(val modID: String) : ManifestDependencyPlan

    /** Mapped and resolved, then nothing usable for this loader and Minecraft version. Refuses the boot. */
    data class Unsatisfied(val modID: String) : ManifestDependencyPlan
}
