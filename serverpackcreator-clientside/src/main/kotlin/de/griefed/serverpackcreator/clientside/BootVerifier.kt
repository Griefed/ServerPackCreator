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
import de.griefed.serverpackcreator.api.modscanning.ScannedMod
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
    private val learnedModIds: LearnedModIds = LearnedModIds(),
    /**
     * What a modloader build declares it **provides**, as id → version, so a staged jar demanding one of
     * those ids is judged rather than skipped.
     *
     * Defaults to knowing nothing, which is the pre-2026-09-12 behaviour and correct for any caller that
     * cannot see an install: `DependencyBacktrack` then treats such a demand as naming something absent, as
     * it always did. The grinder supplies it by reading the cached install layer's own loader jar, which is
     * the only place the answer actually lives — quilt-loader's `quilt.mod.json` declares
     * `provides: [{ "id": "fabricloader", "version": "0.19.3" }]`, and it differs per build.
     */
    private val loaderProvides:
        (loader: String, loaderVersion: String, minecraftVersion: String) -> Map<String, String> =
            { _, _, _ -> emptyMap() },
    private val alternatePlatforms: List<ModPlatform> = emptyList(),
    private val bootArtifactSink: ((Prepared.Ready, BootOutcome) -> Unit)? = null
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Maximum dependency-graph depth to resolve, guarding against cycles/runaway graphs. */
    private val maxDependencyDepth = 4

    /**
     * [refuseForSelfDeclaration] with this verifier's scanner supplying the jar's declared Minecraft range.
     * Split so the decision itself stays testable without an [ApiWrapper].
     */
    private fun refuseForSelfDeclaration(jar: File, loader: String, minecraftVersion: String): Prepared.Failed? =
        refuseForSelfDeclaration(jar, loader, minecraftVersion) { candidate ->
            apiWrapper.modScanner.scannerFor(loader, minecraftVersion)
                ?.scan(listOf(candidate))?.singleOrNull()?.minecraftConstraint
        }

    /**
     * Boot one prepared attempt with this verifier's collaborators. **The single call site of
     * [runPrepared]**, and deliberately so: an attempt happens three times over — the first boot, the
     * newest-loader-build re-check and each other-version re-check — and anything that must happen per
     * attempt has to be added in exactly one place or it silently covers two of the three. Staging wipes
     * the attempt directory, so a re-check's evidence is gone by the time [verify] returns.
     */
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
         * The published file name of the artifact this attempt actually staged, or `null` when nothing
         * staged. Its sibling [bootedLoader]'s counterpart, and needed for the same reason: staging
         * re-selects — on a loader or Minecraft range the jar declares, and on a crash re-check that boots
         * another build entirely — so the file a caller *chose* and the file that *ran* are routinely
         * different, and a verdict naming the former attributes one build's evidence to another.
         */
        val bootedFile: String? = null,
        /**
         * The Minecraft version this attempt actually booted on, or `null` when nothing staged. The third
         * of the triple with [bootedLoader] and [bootedFile], and carried for the same reason: a re-check
         * may settle the verdict from a boot on a different version entirely, and a row naming the version
         * it *asked* for describes a run that did not happen.
         */
        val minecraftVersion: String? = null,
        /**
         * The modloader build the console says actually started, or `null` when it never announced one.
         *
         * Kept beside the requested build rather than replacing it: measured 2026-09-11, all 16 Quilt boots
         * ran `0.30.1` while staging had asked for `0.31.0-beta.4`, and the two provide *different*
         * `fabricloader` versions — so a row naming only one of them cannot be audited either way.
         */
        val observedLoaderVersion: String? = null,
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
        val decidedBy: BootDecision? = null,
        /**
         * Non-`null` when staging stopped before any container ran, naming *whose problem* that was — so
         * this outcome describes the engine, the platform or the ecosystem rather than the mod.
         *
         * Without it a refusal and a boot that learned nothing are the same `INCONCLUSIVE`, which is how a
         * host-wide defect came to be published as one verdict per candidate, overwriting decisive ones
         * that a TTL would otherwise have left alone. `Verdict.ERROR`, `Verdict.LOCKED` and
         * `Verdict.UNVERIFIABLE` are what this feeds, and the cause is which.
         */
        val prevention: PreventionCause? = null
    ) {
        /**
         * Whether staging stopped before any container ran — [prevention] having an answer at all.
         *
         * Derived rather than stored, so the flag and the cause cannot disagree about whether a grind
         * happened. Kept because "did anything run?" is a question several readers ask without caring why.
         */
        val stagingPrevented: Boolean get() = prevention != null
    }

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
    fun verify(project: ProjectFiles, loader: String, metadataDeclaresServerSupport: Boolean = false): BootOutcome =
        verifyPrepared(project, loader, prepareBootPack(project, loader), metadataDeclaresServerSupport) {
            prepareBootPack(project, loader, it)
        }

    /**
     * [verify] for a combination the caller chose — one loader on one Minecraft version-line, as
     * [BootCandidateSelector.pickGrindTargets] picks them.
     *
     * **The newest-build re-check re-stages this same target**, never a fresh selection: re-selecting would
     * answer a crash on one line with a boot on another, which is a different mod's worth of code and
     * exactly the confusion the per-line axis exists to remove.
     */
    fun verify(
        project: ProjectFiles,
        target: BootCandidateSelector.GrindTarget,
        metadataDeclaresServerSupport: Boolean = false
    ): BootOutcome =
        verifyPrepared(project, target.loader, prepareBootPack(project, target), metadataDeclaresServerSupport) {
            prepareBootPack(project, target, it)
        }

    /**
     * The shared body of both [verify] entry points: boot [prepared], run the two crash re-checks, and put
     * the decisive attempt's console back.
     *
     * [restageOnLoaderVersion] re-stages the *same* combination on a given loader build, which is what the
     * newest-build re-check needs and is the one step the two entry points must not share a selection for.
     */
    private fun verifyPrepared(
        project: ProjectFiles,
        loader: String,
        prepared: Prepared,
        metadataDeclaresServerSupport: Boolean,
        restageOnLoaderVersion: (String) -> Prepared
    ): BootOutcome {
        if (prepared is Prepared.Failed) {
            // Say so out loud. This reason used to be returned as a detail string and then dropped by
            // `ClientsideVerifier.aggregate` whenever the metadata already decided the confidence, which made a
            // *silently un-booted* catalogue indistinguishable from a booted one — the boot is the only decisive
            // signal this engine has, so "it did not run, and here is why" has to reach the log.
            log.info("Not booting ${project.slug} on $loader: ${prepared.detail}")
            return BootOutcome(BootResult.INCONCLUSIVE, null, prepared.detail, prevention = prepared.cause)
        }
        val ready = prepared as Prepared.Ready
        val outcome = boot(ready)
        val loaderChecked = recheckCrashOnNewestVersion(project, loader, ready, outcome, restageOnLoaderVersion)
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
        outcome: BootOutcome,
        restageOnLoaderVersion: (String) -> Prepared
    ): BootOutcome {
        val newest = loaderVersionPolicy.latestVersion(loader, first.minecraftVersion)
        // The null check is redundant with shouldRecheckOnNewestBuild (which is false for a null newest) but
        // stated here so the non-nullness is visible where it is used, rather than resting on another
        // function's contract.
        if (newest == null || !shouldRecheckOnNewestBuild(outcome, first.loaderVersion, newest)) {
            return outcome
        }
        log.info(
            "${project.slug}: $loader ${first.loaderVersion} did not boot cleanly and is not the newest build — " +
                "re-checking on $loader $newest before trusting the outcome."
        )
        val restaged = restageOnLoaderVersion(newest)
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
     * and not in `ClientsideVerifier.targetDisprovingTheCrash`. Every attempt nonetheless stages into the
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
                // The crashing attempt's directory, so its pack and console survive the re-check -- which
                // means the crashing target's Minecraft line, never this candidate's.
                attemptDirName = AttemptDirectory.nameFor(
                    project.platform, project.slug, loader, BootCandidateSelector.minecraftLine(booted.minecraftVersion)
                )
            )
            if (staged is Prepared.Failed) {
                log.warn("Could not re-stage ${project.slug} as $label: ${staged.detail}")
                attempts.add(
                    OtherVersionAttempt(
                        label, BootOutcome(BootResult.INCONCLUSIVE, null, staged.detail, prevention = staged.cause)
                    )
                )
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
     *
     * [provided] accumulates every id the staged jars answer to, which is what stops the same dependency
     * being looked up twice under two refs and refusing a boot it is already sitting in — see
     * [stageableRequirements].
     */
    private fun downloadWithDependencies(
        file: ModFile,
        loader: String,
        minecraftVersion: String,
        modsDir: File,
        visited: MutableSet<String>,
        depth: Int,
        unsatisfied: MutableMap<String, UnmetReason>,
        unmapped: MutableSet<String>,
        injected: MutableList<InjectedDependency>,
        excluded: Set<String>,
        provided: MutableSet<String>,
        stagedFromRef: String? = null,
        /**
         * Which platform [stagedFromRef] belongs to. Defaults to this verifier's own; a cross-platform
         * dependency says so, because a ref learned under the wrong platform resolves to nothing there and
         * the next candidate would trust it.
         */
        stagedFromPlatform: String = platform.name
    ): Boolean {
        val staged = httpDownloader.download(file, modsDir)
            ?: return false
        // Read once, here, because three things need it: the id-to-ref bridge below, the platform loop's
        // question of whether this jar actually wants what its project page attributes to it, and
        // `stageManifestDependencies`. `null` means the descriptor could not be read at all, which every
        // reader treats as "no opinion" rather than as "nothing declared".
        val scan = scanStagedJar(staged, loader, minecraftVersion)
        val identity = identityIn(scan)
        // What the pack now answers to. Recorded for every staged jar including the candidate: the candidate
        // is what the loader loads first, and a mod declaring its own id as a dependency of a submodule is
        // not this engine's problem to invent a refusal over.
        provided.addAll(identity.map { it.trim().lowercase() })
        // The jar is here and says what it is, so the id-to-ref bridge this platform needs is now a fact
        // rather than a guess. Only for something fetched *by ref* -- the candidate itself was resolved from
        // a project URL and teaches nothing about how to find it by id.
        if (stagedFromRef != null) {
            learnedModIds.learn(stagedFromPlatform, stagedFromRef, identity)
        }
        if (depth > 0 && injected.none { it.fileName == file.fileName }) {
            // Only dependencies count towards the cap and the recorded set; the candidate is not one.
            //
            // Deduped by FILE, not by ref, because a project is reachable under two equally-valid
            // identifiers -- the platform ref the author linked (`P7dR8mSH`) and the mod id its manifest
            // declares (`fabric`, which ModIdRegistry maps to the slug `fabric-api`). `stageableRequirements`
            // dedupes by ref and cannot see that those are one project. The duplicate is cosmetic in the
            // report but not against the cap: it refuses a pack that is within it, and a refusal is scored
            // INCONCLUSIVE, so the mod quietly stops being verified.
            injected.add(InjectedDependency(file.fileName, null, file.pageUrl, file.version))
        }
        if (depth >= maxDependencyDepth) {
            return true
        }
        val declared = scan?.flatMap { it.dependencies }
        val declaredIds = declared?.map { it.modID }?.toSet()
        for (dependencyRef in file.requiredDependencies) {
            if (!visited.add(dependencyRef)) {
                continue
            }
            val dependencyProject = resolveDependencyAcrossTheLine(dependencyRef, loader, minecraftVersion, excluded)
            if (dependencyProject == null) {
                // Previously a silent `continue`, which is how missing dependencies went unnoticed for so long.
                log.warn("Required dependency '$dependencyRef' could not be resolved on its platform.")
                unsatisfied[unsatisfiedLabel(dependencyRef, null, platform.name)] = UnmetReason.UNRESOLVED
                continue
            }
            val dependencyFile = BootCandidateSelector.pickDependencyFile(
                dependencyProject.withoutExcluded(excluded).files, loader, minecraftVersion
            )
            if (dependencyFile == null) {
                // Asked twice on purpose: the same pick over the *unfiltered* list separates "this project
                // publishes nothing usable" from "it does, and an earlier backtrack excluded all of it".
                // Both refuse, but only one of them is the project's fault, and the second reads as the
                // opposite of the truth. Free — the project is already resolved and in hand.
                val reason = backtrackReason(dependencyProject, excluded, loader, minecraftVersion)
                // The platform says this file needs it; the descriptor is what the loader will enforce. When
                // the jar never names it, an unstageable dependency is a fact about the project page, not
                // about this boot -- `CurseForge/aether` on Forge was refused for `owo-lib`, which only its
                // Fabric and Quilt builds declare.
                if (!PlatformDependencyDemand.isDemanded(declaredIds, dependencyProject)) {
                    log.info(
                        "Platform dependency '${dependencyProject.slug}' could not be staged " +
                            "(${reason.explain(platform.name)}), but ${file.fileName}'s own descriptor does not " +
                            "ask for it — booting without it."
                    )
                    unmapped.add(dependencyProject.slug)
                    continue
                }
                log.warn(
                    "Required dependency '${dependencyProject.slug}' ($dependencyRef) could not be staged " +
                        "for $loader / Minecraft $minecraftVersion: ${reason.explain(platform.name)}."
                )
                unsatisfied[unsatisfiedLabel(dependencyRef, dependencyProject, platform.name)] = reason
                continue
            }
            if (!downloadWithDependencies(
                    dependencyFile, loader, minecraftVersion, modsDir, visited, depth + 1, unsatisfied, unmapped,
                    injected, excluded, provided, stagedFromRef = dependencyRef
                )
            ) {
                val reason = if (dependencyFile.locked) {
                    UnmetReason.DISTRIBUTION_LOCKED
                } else {
                    UnmetReason.DOWNLOAD_FAILED
                }
                if (!PlatformDependencyDemand.isDemanded(declaredIds, dependencyProject)) {
                    log.info(
                        "Platform dependency '${dependencyProject.slug}' could not be staged " +
                            "(${reason.explain(platform.name)}), but ${file.fileName}'s own descriptor does not " +
                            "ask for it — booting without it."
                    )
                    unmapped.add(dependencyProject.slug)
                } else {
                    log.warn(
                        "Required dependency '${dependencyProject.slug}' (${dependencyFile.fileName}) could not be " +
                            "staged: ${reason.explain(platform.name)}."
                    )
                    unsatisfied[unsatisfiedLabel(dependencyRef, dependencyProject, platform.name)] = reason
                }
            }
        }
        stageManifestDependencies(
            declared, file, loader, minecraftVersion, modsDir, visited, depth, unsatisfied, unmapped, injected,
            excluded, provided, staged
        )
        return true
    }

    /**
     * Download the projects [file]'s page links, read what they are, and stop as soon as one turns out to
     * provide [modId]. Returns whether anything now answers for that id.
     *
     * **The last resort, and priced accordingly.** Reached only from a requirement that is required, that
     * the jar declares, and that neither the learned map nor `KnownModIds` nor a slug guess could resolve —
     * a state whose only other outcome is booting without the library and letting the loader refuse the
     * pack, which costs a whole container. Against that, a jar download is cheap.
     *
     * **Everything it reads is kept, matched or not** ([LearnedModIds.learn]), so the cost amortises: the
     * next candidate needing any of those projects by id pays nothing. [probed] stops one jar's several
     * unresolved ids from fetching the same links again within a single pass.
     *
     * The probe copy is downloaded outside `mods/` and deleted immediately — a project that turns out to
     * provide something else must not end up in the pack, and the matching one is staged by the ordinary
     * path so that its own dependencies, its cap accounting and its injection record all still happen.
     */
    private fun askLinkedProjects(
        modId: String,
        file: ModFile,
        loader: String,
        minecraftVersion: String,
        modsDir: File,
        excluded: Set<String>,
        probed: MutableSet<String>
    ): Boolean {
        val wanted = modId.trim().lowercase()
        val probeDir = File(modsDir.parentFile.parentFile, "probe")
        for (ref in file.relatedDependencies) {
            if (learnedModIds.refFor(wanted, platform.name) != null) {
                return true
            }
            if (!probed.add(ref)) {
                continue
            }
            val project = platform.resolveDependency(ref, minecraftVersion) ?: continue
            val candidate = BootCandidateSelector.pickDependencyFile(
                project.withoutExcluded(excluded).files, loader, minecraftVersion
            ) ?: continue
            val jar = httpDownloader.download(candidate, probeDir) ?: continue
            val ids = identityIn(scanStagedJar(jar, loader, minecraftVersion))
            jar.delete()
            learnedModIds.learn(platform.name, ref, ids)
            if (ids.any { it.trim().lowercase() == wanted }) {
                log.info(
                    "'$modId' maps to no project by name, but ${project.slug} — linked from " +
                        "${file.fileName}'s page — declares it. Staging it."
                )
                return true
            }
        }
        return false
    }

    /**
     * Read [jar]'s own descriptor **once**, or `null` when it could not be read at all.
     *
     * The one scan every reader of a staged jar shares, because three of them ask different questions of the
     * same bytes: what the jar answers to ([identityIn]), whether a dependency the project page attributes
     * to it is one it actually wants, and what it declares that the platform never mentioned
     * ([stageManifestDependencies]). They used to scan the same file twice over, which is also two chances
     * to disagree about what it said.
     *
     * **`null` and empty are different answers.** No scanner for the loader, or a scan that threw, means
     * *we do not know*, and everything downstream then defers to the platform. An empty result means the
     * descriptor was read and asks for nothing.
     */
    private fun scanStagedJar(jar: File, loader: String, minecraftVersion: String): List<ScannedMod>? {
        val scanner = apiWrapper.modScanner.scannerFor(loader, minecraftVersion) ?: return null
        return runCatching { scanner.scan(listOf(jar)) }
            .onFailure { log.debug("Could not read ${jar.name}'s descriptor: ${it.message}") }
            .getOrNull()
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
        declared: List<ModDependency>?,
        file: ModFile,
        loader: String,
        minecraftVersion: String,
        modsDir: File,
        visited: MutableSet<String>,
        depth: Int,
        unsatisfied: MutableMap<String, UnmetReason>,
        unmapped: MutableSet<String>,
        injected: MutableList<InjectedDependency>,
        excluded: Set<String>,
        provided: MutableSet<String>,
        staged: File
    ) {
        if (depth >= maxDependencyDepth) {
            return
        }
        // `null` is "the descriptor could not be read", which is nothing to stage from -- the same early
        // return this made for itself when it owned the scan.
        val requirements = declared ?: return

        val bundled = BundledJars.idsIn(staged)
        // A bundled library's own demands bind exactly like a staged one's, and nothing else reads them:
        // `highlight` declares `resourcefullib: "*"`, which the bundled copy satisfies -- while that copy
        // declares `fabric-api: "*"`, which was never staged and killed the boot, charged to `highlight`.
        // Appended rather than merged, so the host's own declaration still leads and `stageableRequirements`
        // applies one dedupe to the pair.
        val declaredAndBundled = requirements + BundledJars.requirementsIn(staged)
        // Scoped to this jar, which is also the scope of `file.relatedDependencies`: several unresolved ids
        // in one descriptor share a single round of probing instead of re-fetching the same links each time.
        val probed = mutableSetOf<String>()
        for (requirement in stageableRequirements(declaredAndBundled, visited, bundled, provided) { platformRefFor(it) }) {
            // `visited` is claimed here rather than inside the planner, which keeps the planner pure: a ref
            // seen once must not be resolved twice even when the first attempt came to nothing.
            val alreadySeen = platformRefFor(requirement.modID)?.let { !visited.add(it) } ?: false
            if (alreadySeen) {
                continue
            }
            val planFor = {
                planManifestDependency(
                    requirement, loader, minecraftVersion,
                    // Learned first: a descriptor this process actually read outranks a table entry and a
                    // slug guess alike, and it is the half that grows on its own.
                    mappingsFor = {
                        learnedModIds.mappingsFor(it, platform.name) { id -> KnownModIds.mappingsFor(id, platform.name) }
                    },
                    // Deliberately UNfiltered: the planner applies `excluded` itself, so it can tell a project
                    // publishing nothing usable from one whose builds staging dropped.
                    resolveRef = { resolveDependencyAcrossTheLine(it, loader, minecraftVersion, excluded) },
                    excluded = excluded
                )
            }
            val firstPlan = planFor()
            // The alternative is only reached when the primary could not be staged, which is the order the
            // descriptor implies: `unless` names a substitute, not a preference.
            val locally = alternativeFor(requirement, firstPlan, loader, minecraftVersion, excluded)
            // Cheaper than the probe below -- a request or two against the other site, rather than a jar
            // download -- and it fires on a disjoint state anyway: this answers "resolvable here, nothing
            // usable", the probe answers "resolvable nowhere".
            val elsewhere = acrossPlatforms(requirement, locally, loader, minecraftVersion, excluded)
            val planned = elsewhere.plan
            // Only here, and only for a requirement that is REQUIRED (optional ones never reach this loop)
            // and unresolvable by every cheaper route, is a download worth spending to find out what a
            // linked project is. Re-planning afterwards rather than using the probe's answer directly keeps
            // one code path deciding what gets staged.
            // The probe only ever asks this platform, so a plan it produces belongs to this one.
            val reprobed = planned is ManifestDependencyPlan.Unmapped &&
                askLinkedProjects(requirement.modID, file, loader, minecraftVersion, modsDir, excluded, probed)
            val plan = if (reprobed) planFor() else planned
            val plannedBy = if (reprobed) platform.name else elsewhere.platformName
            val dependencyFile = when (plan) {
                is ManifestDependencyPlan.Unmapped -> {
                    log.info("Manifest dependency '${plan.modID}' maps to nothing this platform carries.")
                    unmapped.add(plan.modID)
                    continue
                }

                is ManifestDependencyPlan.Unsatisfied -> {
                    // An alias we resolved and then could not stage: a project we know the id names, so it refuses.
                    log.warn(
                        "Manifest dependency '${plan.modID}' could not be staged for $loader / Minecraft " +
                            "$minecraftVersion: ${plan.reason.explain(platform.name)}."
                    )
                    unsatisfied[plan.modID] = plan.reason
                    continue
                }

                // The ref that actually staged is claimed too: with several mappings per id, the one
                // `platformRefFor` claimed above need not be the one that won, and an unclaimed ref lets a
                // later requirement resolve the same project again.
                is ManifestDependencyPlan.Stage -> plan.file.also { visited.add(plan.ref) }
            }
            if (!downloadWithDependencies(
                    dependencyFile, loader, minecraftVersion, modsDir, visited, depth + 1, unsatisfied, unmapped,
                    injected, excluded, provided, stagedFromRef = plan.ref,
                    // Whoever published the file is who the ref belongs to: a CurseForge id filed under
                    // Modrinth is a mapping that resolves to nothing, and the next candidate would trust it.
                    stagedFromPlatform = plannedBy
                )
            ) {
                log.warn("Manifest dependency '${requirement.modID}' (${dependencyFile.fileName}) could not be downloaded.")
                // Same rule as the plan itself: an alias's failed download is a real gap and refuses; a
                // guess's is only a guess that got further than most, and must not cost the boot.
                if (plan.confident) {
                    unsatisfied[requirement.modID] =
                        if (dependencyFile.locked) UnmetReason.DISTRIBUTION_LOCKED else UnmetReason.DOWNLOAD_FAILED
                } else {
                    unmapped.add(requirement.modID)
                }
            }
        }
    }

    /**
     * The dependency project behind [ref], widened to the whole Minecraft **version-line** if the version
     * being booted turns up nothing usable — or `null` when the ref resolves to no project at all.
     *
     * **Why a caller has to ask for this rather than the selector finding it.** `pickDependencyFile` already
     * falls back to a neighbouring patch release, but it can only search the files it is handed, and
     * `CurseForgePlatform.resolveDependency` answers one page narrowed by `gameVersion=<exact>` — so every
     * file in hand carries the exact version and the neighbour rung can never match anything the exact rung
     * did not. The fallback was therefore **inert on CurseForge from the day it shipped**: measured
     * 2026-09-10, `better-combat-by-daedelus` and `combat-roll` were still published `UNVERIFIABLE` for
     * `playeranimator` on Forge 1.20.2 while PlayerAnimator publishes Forge builds for 1.20.1 and 1.20.
     *
     * **The exact version is asked for first and alone**, so the common case stays one request; the
     * neighbours are fetched only where the boot would otherwise be refused outright. They come from SPC's
     * own Minecraft release list rather than from the files, because on CurseForge the files cannot name a
     * version nobody asked about, and they are ordered by [BootCandidateSelector.patchNeighboursIn] — the
     * same nearest-first rule the in-hand fallback uses, so the two cannot drift.
     *
     * Modrinth ignores the extra versions (it returns a whole history in one response), so this costs that
     * platform nothing and the widened call is simply the same answer again.
     */
    private fun resolveDependencyAcrossTheLine(
        ref: String,
        loader: String,
        minecraftVersion: String,
        excluded: Set<String>,
        askedPlatform: ModPlatform = platform
    ): ProjectFiles? {
        val exact = askedPlatform.resolveDependency(ref, minecraftVersion) ?: return null
        if (BootCandidateSelector.pickDependencyFile(
                exact.withoutExcluded(excluded).files, loader, minecraftVersion
            ) != null
        ) {
            return exact
        }
        val neighbours = BootCandidateSelector.patchNeighboursIn(bootableReleases(), minecraftVersion)
        if (neighbours.isEmpty()) {
            return exact
        }
        // Handing the exact version back as well keeps the answer a superset: a widened resolve must never
        // lose a file the narrow one had, or a project whose only usable build the excluded set had dropped
        // would report a different reason on the second look.
        return askedPlatform.resolveDependency(ref, minecraftVersion, neighbours) ?: exact
    }

    /** A dependency plan together with the platform whose catalog produced it. */
    private data class PlatformPlan(val plan: ManifestDependencyPlan, val platformName: String)

    /**
     * [primary] unless the **other** platform publishes a build of the same mod that this one does not.
     *
     * **Why a dependency may cross and a candidate may not.** The candidate is the subject of the
     * experiment and its platform is part of the question being asked; a dependency is scenery — the pack
     * needs the library loaded, and which site hosts the jar says nothing about whether the pack boots with
     * it. Measured: `tacz` resolves to `timeless-and-classics-guns`, whose Minecraft 1.21.1 build is
     * published on CurseForge only, so a Modrinth candidate was refused for a jar any launcher installs.
     *
     * **Only the manifest route can cross**, because only it knows the mod *id*: a platform ref is that
     * platform's own identifier and names nothing on the other side. That is also what bounds the cost —
     * this is reached from a requirement that is required, declared by the jar, and already unsatisfiable
     * here, whose only other outcome is a refused boot. Where no other platform is configured (no
     * CurseForge key, say) the list is empty and nothing changes at all.
     *
     * The mappings and the learned refs are taken **per platform**, since neither travels.
     */
    private fun acrossPlatforms(
        requirement: ModDependency,
        primary: ManifestDependencyPlan,
        loader: String,
        minecraftVersion: String,
        excluded: Set<String>
    ): PlatformPlan {
        // Both failing states cross, because the difference between them is about *our* platform's
        // confidence, not about whether the other one has the mod: an id that mapped nowhere here
        // (`Unmapped`) and one whose only local project publishes nothing usable (`Unsatisfied`) are the
        // same question asked of the other site. `Unmapped` is the commoner of the two, which is why this
        // sits *above* `askLinkedProjects` -- that already fires on it and pays a whole jar download.
        if (primary is ManifestDependencyPlan.Stage) {
            return PlatformPlan(primary, platform.name)
        }
        for (other in alternatePlatforms) {
            val plan = planManifestDependency(
                requirement, loader, minecraftVersion,
                mappingsFor = {
                    learnedModIds.mappingsFor(it, other.name) { id -> KnownModIds.mappingsFor(id, other.name) }
                },
                resolveRef = { resolveDependencyAcrossTheLine(it, loader, minecraftVersion, excluded, other) },
                excluded = excluded
            )
            if (plan is ManifestDependencyPlan.Stage) {
                log.info(
                    "Manifest dependency '${requirement.modID}' has no usable build on ${platform.name} for " +
                        "$loader / Minecraft $minecraftVersion, and ${other.name} publishes " +
                        "${plan.file.fileName} — staging that."
                )
                return PlatformPlan(plan, other.name)
            }
        }
        return PlatformPlan(primary, platform.name)
    }

    /**
     * [primary] unless the requirement names an alternative that can be staged where the primary cannot.
     *
     * Quilt's `unless` clause says *"this requirement is met if that id is present instead"*, and Quilt
     * Loader honours it — so a mod written for either library declares *"QSL, unless Fabric API is here"*
     * and runs with either. Reading only the primary id makes such a requirement look hard: measured on the
     * live grinder 2026-09-10, `geophilic`, `terralith`, `trek` and `true-ending` were each refused for
     * `quilt_resource_loader` while QSL publishes nothing past Minecraft 1.21 and Fabric API publishes for
     * every version of it.
     *
     * **Only reached when the primary failed**, which is the order the descriptor implies — `unless` names a
     * substitute, not a preference — and only for a plan that is `Unsatisfied`, i.e. one that would refuse
     * the boot. An `Unmapped` primary already never refuses, so spending resolves on its alternatives would
     * buy nothing.
     *
     * The alternative is planned by the **same** [planManifestDependency] the primary went through, so it
     * inherits the whole mapping ladder (learned refs, the registry, the version constraint) and the same
     * confidence rule. First alternative that stages wins; if none does, [primary] is handed back untouched
     * so the refusal still names the id the descriptor actually asked for.
     */
    private fun alternativeFor(
        requirement: ModDependency,
        primary: ManifestDependencyPlan,
        loader: String,
        minecraftVersion: String,
        excluded: Set<String>
    ): ManifestDependencyPlan {
        if (primary !is ManifestDependencyPlan.Unsatisfied || requirement.unlessProvided.isEmpty()) {
            return primary
        }
        for (alternative in requirement.unlessProvided) {
            val plan = planManifestDependency(
                ModDependency(alternative, versionConstraint = requirement.versionConstraint),
                loader, minecraftVersion,
                mappingsFor = {
                    learnedModIds.mappingsFor(it, platform.name) { id -> KnownModIds.mappingsFor(id, platform.name) }
                },
                resolveRef = { resolveDependencyAcrossTheLine(it, loader, minecraftVersion, excluded) },
                excluded = excluded
            )
            if (plan is ManifestDependencyPlan.Stage) {
                log.info(
                    "'${requirement.modID}' could not be staged, and the descriptor's `unless` names " +
                        "'$alternative' as satisfying it instead — staging ${plan.file.fileName}."
                )
                return plan
            }
        }
        return primary
    }

    /**
     * This platform's ref for a manifest mod id — what a staged jar proved, else what [KnownModIds] knows.
     *
     * Used for deduping against what is already staged, so it has to agree with the mapping staging itself
     * uses; answering only from the table would re-download a project the learned map had already matched.
     */
    private fun platformRefFor(modId: String): String? =
        learnedModIds.refFor(modId, platform.name) ?: KnownModIds.refFor(modId, platform.name)

    /**
     * The ids a [scanStagedJar] result answers to — its own and everything it `provides` — or empty when the
     * descriptor could not be read.
     *
     * **Its own identity only, never what it bundles.** A nested `fabric-api-base` is on the classpath
     * because this jar carries it, but the id belongs to Fabric API; recording this project as its home
     * would send a later candidate to download the wrong mod.
     */
    private fun identityIn(scan: List<ScannedMod>?): Set<String> = scan.orEmpty()
        .filter { it.descriptorRead }
        .flatMap { listOf(it.modID) + it.provides }
        .filter { it.isNotBlank() }
        .toSet()

    /**
     * The first staged **dependency** whose own descriptor excludes [minecraftVersion], or `null` when every
     * one of them accepts it.
     *
     * The dependency half of what `refuseForSelfDeclaration` does for the candidate, and the gate the
     * patch-version fallback needs: `pickDependencyFile` stages a build from a neighbouring patch release,
     * and neither the cross-loader nor the untagged fallback guarantees the version either, so a jar built
     * against another Minecraft can reach the pack. The loader then refuses the whole pack and the
     * *candidate* wears the verdict — the "never got a fair run" shape, one layer earlier than every guard
     * that already covers it.
     *
     * **Everything uncertain accepts.** A descriptor that could not be read is already filtered out by
     * `descriptorRead`, a jar declaring no range yields `null`, and [VersionConstraint] accepts any range it
     * cannot parse — so this can only ever fire on a positive, readable contradiction. A gate that refused
     * on doubt is the mass-INCONCLUSIVE shape this module has paid for twice.
     *
     * [mainFile] is excluded rather than merely deprioritised: demoting the candidate would verify a
     * different mod, and dropping a *dependency* over a range the candidate declared would blame the wrong
     * jar entirely.
     */
    private fun outsideThePacksMinecraft(
        scanned: List<ScannedMod>,
        mainFile: ModFile,
        minecraftVersion: String
    ): ScannedMod? = scanned.firstOrNull { mod ->
        mod.file.name != mainFile.fileName && excludesTheVersion(mod, minecraftVersion)
    }

    /**
     * Whether [mod]'s own descriptor — **or any jar it bundles** — positively excludes [minecraftVersion].
     *
     * The nested half is not a refinement: a bundled library is on the classpath exactly like a staged one,
     * and its declared range binds exactly like a staged one's, while the *host* jar's descriptor may say
     * nothing at all. Measured live on Quilt — `quilted-fabric-api-11.0.0-alpha.3+0.102.0-1.21.jar` bundles
     * `qsl_base-10.0.0-alpha.1+1.21.jar`, which pins `minecraft [1.21, 1.21]` exactly. Staged into a
     * Minecraft 1.21.1 pack it refuses the whole pack, and the *candidate* wore the INCONCLUSIVE.
     *
     * Same fail-toward-accepting rule as the top-level read: an unreadable jar yields no demands at all, and
     * a range [VersionConstraint] cannot parse is accepted.
     */
    private fun excludesTheVersion(mod: ScannedMod, minecraftVersion: String): Boolean {
        val declared = listOfNotNull(mod.minecraftConstraint) +
            BundledJars.minecraftDemandsIn(mod.file).values
        return declared.any { !VersionConstraint.satisfies(minecraftVersion, it) }
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
        val bootable = bootableCombination()
        val candidate = BootCandidateSelector.pickBootableCandidate(project.files, loader) { bootable(loader, it) }
            ?: return Prepared.Failed(
                "No bootable file/Minecraft/loader combination for $loader.",
                // Nothing published that this loader can run on a Minecraft we support -- not our doing,
                // and not a statement about the mod.
                cause = PreventionCause.UPSTREAM_UNAVAILABLE
            )
        val (mainFile, minecraftVersion) = candidate
        return prepareChosen(project, loader, mainFile, minecraftVersion, loaderVersionOverride, bootable)
    }

    /**
     * [prepareBootPack] for a combination the **caller** chose — one loader on one Minecraft version-line,
     * as [BootCandidateSelector.pickGrindTargets] picks them.
     *
     * The selection is not repeated here. `pickGrindTargets` is handed [bootableCombination] to choose with,
     * so a target already satisfies the same gate, and asking twice would be a second predicate free to
     * disagree with the first. A combination that *is* unbootable still refuses honestly one step later,
     * where `stageBootPack` finds no loader build for it.
     */
    fun prepareBootPack(
        project: ProjectFiles,
        target: BootCandidateSelector.GrindTarget,
        loaderVersionOverride: String? = null
    ): Prepared = prepareChosen(
        project, target.loader, target.file, target.minecraftVersion, loaderVersionOverride, bootableCombination()
    )

    /**
     * Stage [mainFile] for [loader] on [minecraftVersion], then apply at most one re-selection retry when the
     * downloaded jar's own descriptor contradicts the choice. The shared tail of both [prepareBootPack]
     * entry points, so a caller choosing its own combination gets the same retries as one that let selection
     * choose.
     */
    private fun prepareChosen(
        project: ProjectFiles,
        loader: String,
        mainFile: ModFile,
        minecraftVersion: String,
        loaderVersionOverride: String?,
        bootable: (String, String) -> Boolean
    ): Prepared {
        val staged = stageBootPack(project, loader, mainFile, minecraftVersion, loaderVersionOverride)
        // At most one retry, enforced here rather than by which channel a refusal carries: the loader
        // mismatch is tried first, because where a jar disagrees about both, no other Minecraft version
        // makes it a mod for this loader. Only when that retry does not apply -- nothing declared, or
        // nothing declared that this Minecraft can boot -- does the version retry get its turn, which is
        // what keeps a `mods.toml`-only jar's genuine NeoForge boot on an older Minecraft reachable.
        reselectOnLoaderContradiction(staged, project, loader, mainFile, minecraftVersion, loaderVersionOverride, bootable)
            ?.let { return it }
        return reselectOnMinecraftContradiction(staged, project, loader, mainFile, loaderVersionOverride, bootable)
    }

    /**
     * Answer a "the jar carries another loader's descriptor" refusal by verifying it under the loader it
     * really declares, or `null` when that is not the refusal and the Minecraft retry should have its turn.
     *
     * **Why the jar wins over the page** (Griefed's call). A platform's loader tick is a web form; the
     * descriptor is what the file was built against, and it is what the loader reads at runtime. Measured on
     * the public grinder 2026-09-10, ten `UNVERIFIABLE` rows are nothing but a mis-tick —
     * `bellsandwhistles-0.4.5-1.21.1.jar` carries only `META-INF/neoforge.mods.toml` and is ticked Forge,
     * `Highlighter-1.19.4-forge-1.1.5.jar` is ticked Fabric — and every launcher installs those jars under
     * the loader they name. Refusing them publishes a verdict about our reading of the page.
     *
     * **What keeps it from becoming an amnesty.** The declared loader must have a build for the Minecraft
     * being booted (`bootable`), so a jar declaring loaders none of which can run there still refuses with
     * its original reason. The retry calls [stageBootPack], not [prepareBootPack], so a second contradiction
     * surfaces rather than loops. And it stages into the **requested** loader's scratch directory: staging
     * wipes the directory it uses, and the loader whose descriptor was borrowed has its own verdict to build
     * from its own pack and console — the same reasoning as the cross-loader crash re-check's.
     *
     * The verdict still says what ran: `BootOutcome.bootedLoader` is stamped from the staged pack, so a
     * re-selected boot has `bootedLoader != loader`, which `ClientsideVerifier.targetDisprovingTheCrash`
     * already requires to be equal before one loader may clear another's crash.
     */
    private fun reselectOnLoaderContradiction(
        staged: Prepared,
        project: ProjectFiles,
        loader: String,
        mainFile: ModFile,
        minecraftVersion: String,
        loaderVersionOverride: String?,
        bootable: (String, String) -> Boolean
    ): Prepared? {
        if (staged !is Prepared.Failed || staged.declaredLoaders.isEmpty()) {
            return null
        }
        val reselected = loaderToVerifyUnder(staged.declaredLoaders, mainFile.loaders) {
            bootable(it, minecraftVersion)
        } ?: return null
        log.info(
            "${mainFile.fileName} carries ${staged.declaredLoaders.sorted().joinToString("/")} descriptor(s) " +
                "while ${project.platform} ticked it $loader, so verifying ${project.slug} under $reselected " +
                "on Minecraft $minecraftVersion — the loader its own descriptor names."
        )
        return stageBootPack(
            project, reselected, mainFile, minecraftVersion, loaderVersionOverride,
            attemptDirName = AttemptDirectory.nameFor(
                project.platform, project.slug, loader, BootCandidateSelector.minecraftLine(minecraftVersion)
            )
        )
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
        // First among the versions the platform tagged — the pick both sources agree on, where one exists.
        val tagged = BootCandidateSelector.newestVersionSatisfying(mainFile, constraint) { bootable(loader, it) }
        // Then, when they agree on nothing, bump to a real Minecraft release the *jar* accepts. A file
        // tagged for exactly one version its own descriptor excludes has no agreed pick to fall back on,
        // and refusing it throws the candidate away over a web-form tick the loader does not honour.
        val agreed = tagged
            ?: BootCandidateSelector.newestReleaseSatisfying(constraint, bootableReleases()) { bootable(loader, it) }
            ?: return staged
        log.info(
            "${mainFile.fileName} declares Minecraft '$constraint', so re-staging ${project.slug} on " +
                "$loader $agreed — the newest version its own descriptor accepts" +
                (if (tagged == null) ", which its platform never tagged." else ".")
        )
        return stageBootPack(project, loader, mainFile, agreed, loaderVersionOverride)
    }

    /**
     * Every stable Minecraft release SPC knows a server for — the set a jar's own declared range is searched
     * against when its platform tagged nothing the jar accepts.
     */
    private fun bootableReleases(): List<String> =
        apiWrapper.versionMeta.minecraft.serverReleases().map { it.minecraftVersion }

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
    fun bootableCombination(): (String, String) -> Boolean {
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
        attemptDirName: String = AttemptDirectory.nameFor(
            project.platform, project.slug, loader, BootCandidateSelector.minecraftLine(minecraftVersion)
        ),
        excludedDependencies: Set<String> = emptySet()
    ): Prepared {
        val loaderVersion = loaderVersionOverride
            ?: loaderVersionPolicy.preferredVersion(loader, minecraftVersion)
            ?: return Prepared.Failed(
                "No $loader version for Minecraft $minecraftVersion.",
                cause = PreventionCause.UPSTREAM_UNAVAILABLE
            )

        val attemptDir = File(workDirectory, attemptDirName).apply { deleteRecursively() }
        val modsDir = File(attemptDir, "modpack/mods").apply { mkdirs() }

        val unsatisfied = mutableMapOf<String, UnmetReason>()
        val unmapped = mutableSetOf<String>()
        val injected = mutableListOf<InjectedDependency>()
        if (!downloadWithDependencies(
                mainFile, loader, minecraftVersion, modsDir, mutableSetOf(), 0, unsatisfied, unmapped, injected,
                excludedDependencies, mutableSetOf()
            )
        ) {
            return Prepared.Failed(
                downloadFailureDetail(mainFile),
                // A locked file has no URL and never will; anything else is a fetch that can be retried.
                cause = if (mainFile.locked) PreventionCause.DISTRIBUTION_LOCKED else PreventionCause.HOST
            )
        }
        // Ask the jar what it says about itself before spending a container on it. The platform's declared
        // loader and Minecraft sets are what an author ticked; the descriptor is what the jar was built
        // against, and where the two disagree the boot can only fail for reasons that are not sideness.
        refuseForSelfDeclaration(File(modsDir, mainFile.fileName), loader, minecraftVersion)?.let { return it }
        refuseForMissingDependencies(unsatisfied, loader, minecraftVersion, platform.name)?.let { return it }
        refuseForTooManyDependencies(injected.map { it.fileName }, loader, minecraftVersion)?.let { return it }
        unmappedDependencyNote(unmapped)?.let { log.warn(it) }
        // Judge the staged jars against each other before spending a container on them: a set whose own
        // descriptors contradict each other is refused by the loader, and the CANDIDATE wears the verdict.
        dependencyToDemote(modsDir, mainFile, injected, loader, loaderVersion, minecraftVersion, excludedDependencies)
            ?.let { demoted ->
                return stageBootPack(
                    project, loader, mainFile, minecraftVersion, loaderVersionOverride, attemptDirName,
                    excludedDependencies + demoted
                )
            }

        val serverPack = generateServerPack(File(attemptDir, "modpack"), File(attemptDir, "serverpack"), minecraftVersion, loader, loaderVersion)
            ?: return Prepared.Failed(
                "Server-pack generation failed for $loader $minecraftVersion.",
                cause = PreventionCause.HOST
            )

        return Prepared.Ready(
            serverPack, File(attemptDir, "boot.log"), minecraftVersion, loader, loaderVersion,
            injectedDependencies = injected.toList(),
            candidateStem = FilenameStemDeriver.deriveStem(listOf(mainFile.fileName)),
            bootedFile = mainFile.fileName
        )
    }

    /**
     * Which staged **dependency** file to drop to an older build because the pack's own descriptors
     * contradict each other, or `null` when the set is coherent, nothing may be dropped, or the backtrack
     * budget is spent.
     *
     * Reads the staged jars with the same `ModScanner.scannerFor` staging already uses — on a Quilt pack
     * that is `QuiltPackScanner`, which merges the Fabric descriptor most Quilt mods actually ship — and
     * hands [DependencyBacktrack] the two halves it needs: what each jar *declares it needs*, and what
     * version of each mod id is *really staged*. A jar whose descriptor could not be read contributes
     * nothing: `ScannedMod` falls back to the file name and an empty dependency list, which is
     * indistinguishable by value from a mod that declared nothing.
     *
     * **Optional dependencies are excluded.** The loader loads the mod without them, so one being older
     * than a `recommends` asked for cannot be why a pack is refused — demoting over it would spend the
     * budget and change nothing.
     *
     * Everything here fails toward *proceeding*: no scanner, an unreadable jar, a version the platform
     * never reported, an unparseable range. A pack that cannot be judged is booted, exactly as before.
     */
    private fun dependencyToDemote(
        modsDir: File,
        mainFile: ModFile,
        injected: List<InjectedDependency>,
        loader: String,
        loaderVersion: String,
        minecraftVersion: String,
        alreadyExcluded: Set<String>
    ): String? {
        if (alreadyExcluded.size >= DependencyBacktrack.MAX_BACKTRACKS) {
            log.warn(
                "Giving up on making the dependency set coherent after ${alreadyExcluded.size} attempts; " +
                    "booting ${mainFile.fileName} on $loader / Minecraft $minecraftVersion anyway."
            )
            return null
        }
        val scanner = apiWrapper.modScanner.scannerFor(loader, minecraftVersion) ?: return null
        val stagedJars = modsDir.listFiles()?.toList() ?: return null
        val scanned = runCatching { scanner.scan(stagedJars) }
            .onFailure { log.debug("Could not scan the staged pack for dependency conflicts: ${it.message}") }
            .getOrDefault(emptyList())
            .filter { it.descriptorRead }
        val publishedVersionOf = (injected.map { it.fileName to it.version } + (mainFile.fileName to mainFile.version))
            .toMap()

        // The loader's own `provides` first of all, so a jar demanding one of them is judged instead of
        // being skipped as naming something absent -- and last in precedence, because a staged jar claiming
        // the same id is a real file the loader will load. Measured 2026-09-11: quilt-loader 0.30.1 provides
        // `fabricloader 0.19.3` while 0.31.0-beta.4 provides `0.19.5`, and `fabric-language-kotlin` demands
        // `[0.19.5, ∞)` -- twelve published rows died on that, invisibly, because `fabricloader` is
        // environment-provided and therefore never staged for anything to compare against.
        //
        // Nested next, so a top-level jar of the same id wins: that is the copy staging deliberately
        // chose and the one a demotion would act on. Nested entries can therefore only fill a gap.
        val provided = loaderProvides(loader, loaderVersion, minecraftVersion)
        val stagedVersions = provided + nestedVersions(stagedJars) + scanned.flatMap { mod ->
            val version = publishedVersionOf[mod.file.name] ?: return@flatMap emptyList()
            // A dependency names an id, and one jar answers to several: its own, plus everything it
            // `provides` -- Fabric API declares `id: fabric-api` and `provides: [fabric]`.
            (listOf(mod.modID) + mod.provides).map { it to version }
        }.toMap()
        // The platform ids the scanners strip, read back off each staged jar for exactly the ids the
        // loader was able to describe -- so a demand on `fabricloader` is judged, and nothing else is.
        val platformDemands = scanned.flatMap { mod ->
            BundledJars.demandsOn(mod.file, provided.keys).mapNotNull { requirement ->
                requirement.versionConstraint?.let { constraint ->
                    DependencyBacktrack.Requirement(
                        mod.file.name, mod.file.name == mainFile.fileName, requirement.modID, constraint
                    )
                }
            }
        }
        val requirements = platformDemands + scanned.flatMap { mod ->
            mod.dependencies
                .filterNot { it.optional }
                .mapNotNull { requirement ->
                    requirement.versionConstraint?.let { constraint ->
                        DependencyBacktrack.Requirement(
                            mod.file.name, mod.file.name == mainFile.fileName, requirement.modID, constraint
                        )
                    }
                }
        }

        // Asked before the version conflicts, because it is the more certain defect: a jar whose own
        // descriptor names another Minecraft is one the loader refuses outright, where a version range is
        // one mod's opinion about another. The candidate is excluded on purpose -- it is the subject of the
        // experiment, and `refuseForSelfDeclaration` plus `reselectOnMinecraftContradiction` already answer
        // its disagreement by re-selecting a version it accepts.
        outsideThePacksMinecraft(scanned, mainFile, minecraftVersion)?.let { mismatch ->
            log.info(
                "${mismatch.file.name} declares Minecraft '${mismatch.minecraftConstraint}', which does not " +
                    "include $minecraftVersion — re-staging ${mainFile.fileName} without it."
            )
            return mismatch.file.name
        }

        val conflicts = DependencyBacktrack.conflicts(requirements, stagedVersions)
        val demoted = DependencyBacktrack.fileToDemote(conflicts) ?: return null
        val reason = conflicts.first { it.requiringFileName == demoted }
        log.info(
            "$demoted requires ${reason.requiredModId} '${reason.versionConstraint}' but the pack holds " +
                "${reason.stagedVersion}, and Minecraft $minecraftVersion publishes nothing newer — " +
                "re-staging ${mainFile.fileName} without it."
        )
        return demoted
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
            val candidateStem: String? = null,
            /**
             * The published name of the candidate file this attempt staged, verbatim — what the verdict
             * reports as the artifact it is about. Carried rather than derived from [candidateStem], which
             * is a *stem* and has already dropped the version that identifies the build.
             */
            val bootedFile: String? = null
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
             * Whose problem [detail] describes, which is what decides the published verdict.
             *
             * Defaults to [PreventionCause.HOST], the loudest reading and the one every refusal carried
             * before the causes were told apart — so a site that forgets to say stays visible in the bucket
             * an operator reads rather than filing itself quietly as nobody's fault.
             */
            val cause: PreventionCause = PreventionCause.HOST,
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
            val declaredMinecraftConstraint: String? = null,
            /**
             * The loaders the staged jar's descriptors actually name, set **only** when *that* is why
             * staging stopped — i.e. the jar carries no descriptor the loader being booted reads.
             * Empty for every other refusal, including the Minecraft-range disagreement above.
             *
             * The sibling of [declaredMinecraftConstraint], and deliberately a separate channel rather
             * than a widening of it: re-selecting a *version* cannot answer a *loader* mismatch, and a
             * refusal that offered the Minecraft retry this set would re-stage the jar down its whole
             * version list, learning nothing each time. Exactly one of the two may be non-empty per
             * refusal, and [prepareBootPack] tries at most one retry.
             */
            val declaredLoaders: Set<String> = emptySet()
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
                    // Nothing booted: the hook runs before the container, and in the grinder it *is* the
                    // loader-cache overlay -- so this fails when the host is broken, not when the mod is.
                    // Without a `prevention` a broken cache publishes as a verdict about every mod that
                    // wanted it, which is the missing-runtime-image outage in miniature.
                    return BootOutcome(
                        BootResult.INCONCLUSIVE, null,
                        "Pack post-processing failed: ${processing.exceptionOrNull()?.message}",
                        prevention = PreventionCause.HOST
                    )
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
                .let { classified ->
                    // Read from the console rather than from the pack: what staging asked for is already
                    // known, and the whole point is that the two can disagree.
                    val observed = BootLoaderVersion.observedIn(classified.console?.lines().orEmpty())
                    classified.copy(
                        bootedLoader = pack.loader,
                        bootedFile = pack.bootedFile,
                        minecraftVersion = pack.minecraftVersion,
                        observedLoaderVersion = observed,
                        detail = listOfNotNull(
                            classified.detail,
                            BootLoaderVersion.disagreementNote(pack.loaderVersion, observed)
                        ).joinToString(" "),
                        stagedDependencies = pack.injectedDependencies.map { it.fileName }
                    )
                }
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
         * Which of the loaders a jar's descriptors [declared] to verify it under, or `null` when none of
         * them can be booted on the Minecraft version in question.
         *
         * Prefers one the platform also [tagged] for the file — the author's two statements agreeing is
         * better evidence than either alone, and a file ticked Forge *and* NeoForge whose jar only declares
         * NeoForge should be verified as NeoForge rather than as whatever sorts first. Failing that it is
         * alphabetical, purely so the choice is deterministic: a jar declaring two bootable loaders neither
         * of which its page mentions offers nothing to choose on, and picking by file name is the
         * silently-plausible-value trap this module has already paid for.
         *
         * [bootable] is what stops this becoming an amnesty — a `mods.toml` names Forge and NeoForge on
         * Minecraft 1.19.4, where NeoForge published nothing at all.
         */
        internal fun loaderToVerifyUnder(
            declared: Set<String>,
            tagged: Set<String>,
            bootable: (String) -> Boolean
        ): String? {
            val usable = declared.filter(bootable)
            return usable.firstOrNull { it in tagged } ?: usable.minOrNull()
        }

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
            // Asked again rather than parsed back out of `contradiction`: the acceptability rule lives in
            // JarSelfDeclaration and must have one home, and this costs a second read of the archive only
            // on the refusal path.
            //
            // **Both channels are filled when the jar disagrees about both**, and which retry to spend is
            // `prepareBootPack`'s decision, not this function's. Nulling the Minecraft range here to
            // enforce "exactly one retry" lost a reachable boot: where the declared loader has no build for
            // this Minecraft the loader retry cannot fire, and the version retry that could have has been
            // erased. A `mods.toml`-only jar requested as NeoForge on 1.20.6 is that shape -- at 1.20.4 the
            // same file *is* a NeoForge descriptor, so re-selecting the version finds a genuine NeoForge
            // boot instead of borrowing Forge's.
            val mismatchedLoaders = JarSelfDeclaration.contradictingLoaders(jar, loader, minecraftVersion)
            return Prepared.Failed(
                "Refusing to boot $loader on Minecraft $minecraftVersion: $contradiction. " +
                    "The platform's declared versions are what its author ticked, not what the jar was built for.",
                // A web-form tick contradicting the jar is the author's mistake: nothing we can retry, and
                // no statement about whether the mod belongs on a server.
                cause = PreventionCause.UPSTREAM_UNAVAILABLE,
                declaredMinecraftConstraint = minecraftDisagreement,
                declaredLoaders = mismatchedLoaders
            )
        }

        /**
         * How one unmet dependency is named in the refusal an operator reads.
         *
         * A platform ref is an *identifier*, not a name: Modrinth's is an opaque base62 `project_id`
         * (`MBAkmtvl`) and CurseForge's a bare number. Recording the ref made refusals read as gibberish —
         * `waystones` reported its missing `balm` and `shogi` as `MBAkmtvl` and `bi4iCmsw`, while the very
         * same two mods came out readably from the manifest half of staging.
         *
         * A [resolved] project is named by its slug, which is what the author, the platform page and the
         * manifest all call it. That also **collapses the duplicate**: `unsatisfied` is keyed by this name,
         * so a mod missing by both routes was two entries and is now one.
         *
         * An unresolved ref keeps the ref — it is all we have — but says which platform it belongs to, so a
         * reader can look it up instead of mistaking it for a strange mod name.
         *
         * **The name only.** *Why* it is unmet is an [UnmetReason] carried beside it, precisely so that the
         * dedupe above survives two routes disagreeing about the reason.
         */
        internal fun unsatisfiedLabel(
            ref: String,
            resolved: ProjectFiles?,
            platformName: String
        ): String = resolved?.slug?.takeIf { it.isNotBlank() }
            ?: "$ref (unresolved $platformName project)"

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
        internal fun refuseForMissingDependencies(
            unsatisfied: Map<String, UnmetReason>,
            loader: String,
            minecraftVersion: String,
            platformName: String
        ): Prepared.Failed? =
            if (unsatisfied.isEmpty()) {
                null
            } else {
                val named = unsatisfied.entries.sortedBy { it.key }.joinToString(", ") { (name, reason) ->
                    if (reason.worthAppending) "$name (${reason.explain(platformName)})" else name
                }
                Prepared.Failed(
                    "Required ${if (unsatisfied.size == 1) "dependency" else "dependencies"} unavailable for " +
                        "$loader / Minecraft $minecraftVersion: $named. " +
                        "Not booting — a mod refused for missing dependencies says nothing about sideness.",
                    cause = preventionCauseFor(unsatisfied)
                )
            }

        /**
         * Whose problem a set of [unsatisfied] dependencies is, folded to the **most actionable** cause
         * present.
         *
         * The order is `HOST` → `DISTRIBUTION_LOCKED` → `UPSTREAM_UNAVAILABLE`, and it ranks by what a
         * reader can do about it. A refusal mixing a failed download with a permanent upstream gap has to
         * reach the operator who can retry the download, so ours wins outright; between the other two, a
         * distribution opt-out names a project, a file and an author's decision, where "nothing published"
         * names an absence — so the identifiable fact wins.
         *
         * A `UnmetReason` maps to its cause and nothing else does: keeping the mapping on the reason means
         * a new reason cannot be added without deciding whose problem it is.
         *
         * **Total, including for an empty set**, which answers [PreventionCause.HOST] for the same reason
         * every prevention default does: the loud, actionable reading is the safe one when nothing said
         * otherwise. It is unreachable from the one call site, and that is exactly why it is stated — a
         * helper that throws on a value its name admits, guarded only by a caller, is how
         * `UnmetReason.explain` came to return `null` for something two log sites would have interpolated.
         */
        internal fun preventionCauseFor(unsatisfied: Map<String, UnmetReason>): PreventionCause {
            val causes = unsatisfied.values.map { it.preventionCause }.toSet()
            return PreventionCause.entries.firstOrNull { it in causes } ?: PreventionCause.HOST
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
         * **The rule keys on how the ref was arrived at, not on how far it got** (2026-09-06). An
         * [ModIdMapping.Alias] is a project we know the id names, so failing to stage it is a real gap and
         * refuses. An [ModIdMapping.Guess] is an optimistic slug that may name nothing or something else, so
         * it never refuses however far it gets. It used to key on distance — mapped-then-unstageable
         * refused, unmappable did not — which made *being almost resolvable worse than being unknown*, and
         * is why CurseForge was given no guess at all.
         *
         * **Several mappings are tried in turn, because one mod id is genuinely served by several
         * projects** (2026-09-09): a fork or an unofficial port keeps the original's id, so
         * [LearnedModIds.mappingsFor] can offer both, and the first of them having no build for this boot is
         * not the same thing as the dependency being unavailable. The first mapping that yields a file wins;
         * a refusal needs *every* mapping to have failed, and even then only an alias may raise one — the
         * reason quoted is the first alias's, since that is the project a reader will go and look up.
         *
         * @param mappingsFor Everything worth trying for a mod id, best first, each carrying how much it can
         *                    be trusted.
         * @param resolveRef  The project behind a ref, or `null` when the platform does not carry it.
         */
        internal fun planManifestDependency(
            requirement: ModDependency,
            loader: String,
            minecraftVersion: String,
            mappingsFor: (String) -> List<ModIdMapping>,
            resolveRef: (String) -> ProjectFiles?,
            excluded: Set<String> = emptySet()
        ): ManifestDependencyPlan {
            // An accumulator rather than a `val`, and deliberately: the loop must stop at the first mapping
            // that stages (so a second project is never resolved for nothing) while remembering the first
            // *alias*'s reason in case none does. A functional form either resolves every ref or needs two
            // passes over them.
            var refusal: UnmetReason? = null
            for (mapping in mappingsFor(requirement.modID)) {
                val ref = mapping.ref ?: continue
                val project = resolveRef(ref) ?: continue
                val confident = mapping is ModIdMapping.Alias
                val file = BootCandidateSelector.pickDependencyFile(
                    project.withoutExcluded(excluded).files, loader, minecraftVersion,
                    requirement.versionConstraint
                )
                if (file != null) {
                    return ManifestDependencyPlan.Stage(ref, file, confident)
                }
                // Remembered rather than returned: a later mapping may still stage this id, and only once
                // none has is there anything to refuse over.
                if (confident && refusal == null) {
                    refusal = backtrackReason(project, excluded, loader, minecraftVersion)
                }
            }
            // A guess that hit a real project publishing nothing usable. Being *almost* resolvable must
            // not be worse than being unknown — the `xaerolib` case — so it is filed, not fatal.
            return refusal?.let { ManifestDependencyPlan.Unsatisfied(requirement.modID, it) }
                ?: ManifestDependencyPlan.Unmapped(requirement.modID)
        }

        /**
         * The mod ids [stagedJars] carry **inside** themselves, mapped to the versions those nested descriptors
         * state — the rest of the classpath, as far as judging the pack's coherence goes.
         *
         * A jar-in-jar library is loaded exactly like a staged file but appears in neither of the two sources
         * `dependencyToDemote` otherwise has: it is not a top-level file, and the platform never published it,
         * so `InjectedDependency.version` has nothing for it. Without this a requirement contradicting a bundled
         * copy read as a requirement naming something *absent*, which `DependencyBacktrack` skips by design.
         *
         * **One id bundled at two versions by two different jars is dropped**, for the reason
         * [BundledJars.versionsIn] drops it within a single jar: which copy the loader picks is its own
         * resolution behaviour, and no opinion costs a missed conflict where a wrong one manufactures a demotion.
         * Both levels are the same rule, so both go through [BundledJars.unambiguous] rather than folding twice.
         */
        internal fun nestedVersions(stagedJars: List<File>): Map<String, String> = BundledJars.unambiguous(
            stagedJars.flatMap { jar -> BundledJars.versionsIn(jar).toList() }
        )

        /**
         * Whether a project that yielded no file yielded none *of its own accord*, or because staging had
         * already excluded the builds that would have served.
         *
         * Re-asks [BootCandidateSelector.pickDependencyFile] over the unfiltered list: a pick that succeeds
         * there and fails against [excluded] means `DependencyBacktrack` demoted its way through everything
         * usable. Both outcomes still refuse — the difference is whether the refusal blames the project or
         * names what we did — and the extra call touches no network, since the project is already resolved.
         */
        internal fun backtrackReason(
            project: ProjectFiles,
            excluded: Set<String>,
            loader: String,
            minecraftVersion: String
        ): UnmetReason {
            val wouldHavePicked = excluded.isNotEmpty() &&
                BootCandidateSelector.pickDependencyFile(project.files, loader, minecraftVersion) != null
            return if (wouldHavePicked) UnmetReason.DROPPED_BY_BACKTRACK else UnmetReason.NO_USABLE_FILE
        }

        /**
         * [ProjectFiles] with every file staging has already ruled out removed, so a re-stage picks the next
         * one down. In the companion because both the platform route and the manifest planner need it.
         */
        internal fun ProjectFiles.withoutExcluded(excluded: Set<String>): ProjectFiles =
            if (excluded.isEmpty()) this else copy(files = files.filterNot { it.fileName in excluded })

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
            bundledIds: Set<String> = emptySet(),
            providedIds: Set<String> = emptySet(),
            refFor: (String) -> String? = { it }
        ): List<ModDependency> = requirements.filterNot { requirement ->
            // An optional dependency is neither staged nor allowed to refuse a boot: the descriptor itself
            // says the mod loads without it. `advancement-plaques` declares `prism` and `toastcontrol`
            // `mandatory=false` and was refused for "Required dependency unavailable ... prism", which cost
            // an INCONCLUSIVE on a mod that never required it. Both platforms already filter their own side
            // (`dependency_type == "required"`, `relationType == 3`); this is the manifest half of that rule.
            requirement.optional ||
                // Already inside the candidate as a nested jar, which the loader puts on the classpath: it
                // needs no download and can never be missing. `xaeros-world-map` was refused for `xaerolib`
                // while shipping it, because a Modrinth project of that name exists (so the id *mapped*) but
                // publishes nothing tagged Quilt or 26.2 (so nothing could be staged) -- and a
                // mapped-then-unstageable id refuses where an unmappable one would not have. Bundled wins
                // unconditionally: the author shipped that exact build, and fetching another version of the
                // same id manufactures a conflict to blame on the mod.
                requirement.modID in bundledIds ||
                // Already in mods/, staged under some ref, and answering to this id -- so the loader will
                // find it whatever project a second lookup of the same id would reach. The ref dedupe above
                // cannot see this: one project is reachable under the ref its platform page links AND under
                // whatever `LearnedModIds`/`KnownModIds` maps the manifest id to, and where those differ the
                // id was resolved a second time against a DIFFERENT project, whose "publishes nothing for
                // this loader and Minecraft version" then refused a boot the dependency was sitting in.
                // Ten published ERROR verdicts were that, measured 2026-09-09: `create` (copycats,
                // create-steam-n-rails, createaddition), `farmersdelight` (ends-delight) and
                // `sophisticatedcore` (both unofficial Fabric ports).
                //
                // Lowercased on both sides because descriptors spell ids inconsistently and a miss here
                // costs the whole boot, whereas `bundledIds` above compares two ids read by the same scanner.
                requirement.modID.trim().lowercase() in providedIds ||
                // Quilt's `unless`: the descriptor itself says this requirement is met if that id is here
                // instead, and the loader honours it. `geophilic`, `terralith`, `trek` and `true-ending`
                // all declare `quilt_resource_loader unless fabric-resource-loader-v0`.
                //
                // **Both sets, because the alternative is usually a jar-in-jar.** Read from the live
                // `fabric-api-0.116.17+1.21.1.jar`: its descriptor declares `id=fabric-api` and
                // `provides=["fabric"]`, while `fabric-resource-loader-v0` exists only as
                // `META-INF/jars/fabric-resource-loader-v0-0.116.17.jar`. An arm testing `providedIds`
                // alone therefore could not fire for the case it was written for -- the requirement
                // survived and `alternativeFor` re-downloaded a library the loader already had.
                requirement.unlessProvided.any {
                    val alternative = it.trim()
                    alternative in bundledIds || alternative.lowercase() in providedIds
                } ||
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
                        "anything about this mod specifically.",
                    // Our cap, our judgement call, and an operator may want to know it bit.
                    cause = PreventionCause.HOST
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
         * Whether a boot deserves a second run on the newest loader build: only when a newest build is known
         * and differs from the one that ran, and only for an outcome the loader build could be responsible
         * for — a **crash**, or any outcome whose console says the loader itself was too old for a mod.
         *
         * **The second arm is why this is no longer called `shouldRecheckCrash`.** That name was accurate
         * while a loader too old for the pack produced a non-zero exit and read as CRASHED; since
         * `dependencyFailureMarkers` was widened (2026-08-29) it reads as INCONCLUSIVE, and the guard
         * silently stopped covering the case its own tests describe. Measured 2026-09-08: 17 of 42
         * dependency failures on the live daemon, with all 511 Fabric boots pinned to loader 0.19.3 while
         * 0.19.5 was current.
         *
         * A SURVIVED boot is never re-run whatever its console holds: it already answered the question, and
         * a mod that booted cleanly on an old build has nothing to gain from a newer one.
         */
        internal fun shouldRecheckOnNewestBuild(
            outcome: BootOutcome,
            bootedVersion: String,
            latestVersion: String?
        ): Boolean {
            if (latestVersion == null || latestVersion == bootedVersion || outcome.result == BootResult.SURVIVED) {
                return false
            }
            return outcome.result == BootResult.CRASHED ||
                LoaderVersionDemand.unmetIn(outcome.console?.lines().orEmpty())
        }

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
                detail = "${first.detail} (the boot on $bootedVersion could not be re-checked on $latestVersion: ${second.detail})"
            )
            BootResult.CRASHED -> second.copy(
                detail = "${second.detail} (the failure on $bootedVersion is confirmed on the newest build $latestVersion)"
            )
            BootResult.SURVIVED -> second.copy(
                detail = "${second.detail} (failed on $bootedVersion but not on the newest build $latestVersion — " +
                    "treating that as a loader-build artefact, not the mod)"
            )
        }

        /**
         * Whether a crash is worth spending boots on *other versions of the mod*: only a CRASHED outcome that
         * client-only evidence has not already settled, within a non-zero boot budget, and then for one of
         * two reasons.
         *
         * **The metadata contradicts it.** The mod claims server support and the server died, so one of the
         * two signals must be wrong. Deliberately narrow: where the metadata already leans clientside the
         * crash *confirms* it, and in a catalog sweep that agreement is the common case — re-checking it
         * would spend boots to learn nothing while the crawl falls behind.
         *
         * **Or it is about to be published.** A crash a *decisive* rung explains reaches `CONFIRMED`, which
         * strips the mod from every server pack built against the fallback list, and that is worth one boot
         * whatever the metadata says. This arm exists because the axis moved: a project used to be ground
         * under every loader it publishes for, so a wrong crash routinely met a clean boot from a sibling
         * loader in the same run (`iron-chests`, 2026-08-23) and `ClientsideVerifier.targetDisprovingTheCrash`
         * threw it out for free. One loader per Minecraft line means that sibling is no longer booted unless
         * something asks for it, and this is what asks.
         *
         * In practice the second arm reaches `OPERATOR_RULE` alone — the other decisive rungs all prove
         * client-only and are excluded above — which is exactly right: a hand-written rule is the one
         * decisive signal nobody has cross-checked.
         */
        internal fun shouldRecheckAgainstOtherVersions(
            outcome: BootOutcome,
            metadataDeclaresServerSupport: Boolean,
            limit: Int
        ): Boolean = outcome.result == BootResult.CRASHED &&
            // Already answered. The re-check exists to tell "this build crashed" from "this mod cannot
            // run on a server"; client-only evidence has settled that, so the boots would buy nothing and
            // a survivor among them would actively discard the proof.
            outcome.decidedBy?.provesClientOnly != true &&
            (metadataDeclaresServerSupport || outcome.decidedBy?.decisive == true) &&
            limit > 0

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
            // Defence in depth: `shouldRecheckAgainstOtherVersions` already declines to sample a
            // client-only-proven crash, but if one is ever reconciled anyway, a clean boot elsewhere must not
            // erase the proof. `sodium`'s LWJGL crash was discarded exactly here, by a Fabric build starting.
            if (first.decidedBy?.provesClientOnly == true) {
                return first
            }
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
         * Turn a [RunResult] into the reported [BootOutcome]: a [RunResult.NotStarted] is a *prevented*
         * grind (nothing ran, so `Verdict.ERROR`) rather than INCONCLUSIVE
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
            // The runner never started the server, so there is no console and nothing was learned about the
            // mod. An operator's problem, however late it surfaced.
            is RunResult.NotStarted ->
                BootOutcome(BootResult.INCONCLUSIVE, null, runResult.detail, prevention = PreventionCause.HOST)
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
    /**
     * Stage [file], fetched under [ref].
     *
     * [confident] is the mapping's confidence carried forward, because the *download* can still fail and the
     * same rule has to apply there: an alias whose jar could not be fetched is a real gap, a guess's is not.
     */
    data class Stage(val ref: String, val file: ModFile, val confident: Boolean) : ManifestDependencyPlan

    /** A guess that missed: nothing maps, or the platform does not carry it. Reported, never fatal. */
    data class Unmapped(val modID: String) : ManifestDependencyPlan

    /**
     * An **alias** we resolved and then could not stage. A project we know the id names, so failing to
     * honour it is a real gap: this is the only outcome that refuses the boot.
     *
     * [reason] defaults to [UnmetReason.NO_USABLE_FILE] because that is what "could not stage" meant for as
     * long as this type existed; a caller that knows better says so.
     */
    data class Unsatisfied(
        val modID: String,
        val reason: UnmetReason = UnmetReason.NO_USABLE_FILE
    ) : ManifestDependencyPlan
}

/**
 * Why one required dependency could not be staged — the evidence a staging refusal publishes alongside the
 * dependency's name.
 *
 * **The refusal used to name only the mod.** Five distinct failures reached `unsatisfied` and three of them
 * printed the bare slug, so `Required dependency unavailable … balm` meant *"the project publishes nothing
 * usable"*, *"the download died"* and *"we dropped every build ourselves while backtracking"* alike. That is
 * the same standard [BootDecision.decidedBy] enforces on a boot verdict — a verdict that cannot name its own
 * evidence cannot be audited — reaching the one refusal that publishes `ERROR` without ever booting.
 *
 * It travels **beside** the name rather than inside it, because `unsatisfied` is keyed by name so that one
 * mod missing by both the platform and the manifest route stays one entry (the `waystones` case).
 *
 * @author Griefed
 */
internal enum class UnmetReason {

    /** The ref resolved to no project at all; the label itself already says so, so this adds nothing. */
    UNRESOLVED,

    /** The project resolved and publishes nothing this loader and Minecraft version can use. */
    NO_USABLE_FILE,

    /**
     * The project publishes something usable and **staging excluded it** — every candidate build was demoted
     * trying to make the pack coherent. Reporting this as [NO_USABLE_FILE] states the opposite of the truth,
     * and is what hid 1014 re-stagings a day behind 47 verdicts on 2026-09-07.
     *
     * Two things reach it, which is why the sentence says *coherent* rather than naming one of them: a
     * version range one staged jar declares about another (`DependencyBacktrack`), and a jar whose own
     * descriptor excludes the Minecraft being booted (`outsideThePacksMinecraft`, 2026-09-09).
     */
    DROPPED_BY_BACKTRACK,

    /** A file was picked and its author opted out of third-party distribution: there is no URL to fetch. */
    DISTRIBUTION_LOCKED,

    /** A file was picked, it had a URL, and fetching it failed — transient, unlike every other reason here. */
    DOWNLOAD_FAILED;

    /**
     * Whose problem this reason is, which is what decides the published verdict.
     *
     * Stated per reason rather than folded at the call site, so a reason added later cannot reach a refusal
     * without somebody deciding whether it is ours, the platform's or nobody's. `DROPPED_BY_BACKTRACK` is
     * deliberately ours: staging dropped those builds itself trying to make the pack coherent, and an
     * operator seeing it should be asking whether the backtrack was right.
     */
    val preventionCause: PreventionCause
        get() = when (this) {
            UNRESOLVED, NO_USABLE_FILE -> PreventionCause.UPSTREAM_UNAVAILABLE
            DISTRIBUTION_LOCKED -> PreventionCause.DISTRIBUTION_LOCKED
            DROPPED_BY_BACKTRACK, DOWNLOAD_FAILED -> PreventionCause.HOST
        }

    /**
     * How this reads, in a refusal or a log line. **Never `null`** — it used to return `null` for
     * [UNRESOLVED], on the grounds that the label already says so, and two log sites interpolated the
     * result straight into a string. Neither can reach that value today, so both would have printed the
     * literal `null` only after some later edit, with nothing to warn them. Whether a reason is worth
     * *appending to a refusal* is a rendering decision, and it now lives in the renderer
     * ([refuseForMissingDependencies]) rather than in a nullable return.
     *
     * @param platformName Where to look the project up, which is only worth saying for an opt-out.
     */
    fun explain(platformName: String): String = when (this) {
        UNRESOLVED -> "unresolved on $platformName"
        NO_USABLE_FILE -> "nothing published for this loader and Minecraft version"
        DROPPED_BY_BACKTRACK -> "every usable build was dropped making the pack coherent"
        DISTRIBUTION_LOCKED -> "distribution-locked on $platformName"
        DOWNLOAD_FAILED -> "download failed"
    }

    /**
     * Whether a refusal should append [explain] to the dependency's name.
     *
     * Only [UNRESOLVED] says no: `unsatisfiedLabel` already renders an unresolved ref as
     * `<ref> (unresolved <platform> project)`, and a second parenthesis would say it twice.
     */
    val worthAppending: Boolean get() = this != UNRESOLVED
}
