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

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Orchestrates the Phase-1 (metadata-only) clientside verdict: pick the hosting platform for the
 * issue-link, resolve its files, derive a list-entry per loader, and combine the platform-declared
 * sideness with SPC's jar metadata scan and the boot's console into a per-loader [Verdict]. The console
 * decides and the metadata only declares — a self-report is the unreliable half, and is the whole reason
 * the expensive boot exists.
 *
 * @param platforms          The supported hosting platforms, tried in order via [ModPlatform.handles].
 * @param metadataScanner    Reads declared sideness out of a downloaded jar.
 * @param jarDownloader      Fetches published files for scanning; a locked file has no URL and is
 *                           reported as unverifiable rather than fetched another way.
 * @param workDirectory      Scratch directory for downloaded jars.
 * @param bootVerifierFactory When non-null, builds a [BootVerifier] for the resolved platform to add
 *                            the server-boot signal (Phase 2); when null, the report is metadata-only.
 * @param linePolicy         Which of the project's Minecraft version-lines get ground — one verdict each.
 * @author Griefed
 */
class ClientsideVerifier(
    private val platforms: List<ModPlatform>,
    private val metadataScanner: MetadataScanner,
    private val jarDownloader: JarDownloader,
    private val workDirectory: File,
    private val bootVerifierFactory: ((ModPlatform) -> BootVerifier)? = null,
    private val linePolicy: MinecraftLinePolicy = MinecraftLinePolicy()
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Build the metadata-only report for [projectUrl].
     *
     * @throws IllegalArgumentException if no supported platform recognizes the link.
     */
    fun report(projectUrl: String): ClientsideReport {
        val platform = platforms.firstOrNull { it.handles(projectUrl) }
            ?: throw IllegalArgumentException("No supported platform (Modrinth/CurseForge) for '$projectUrl'.")
        val project = platform.resolve(projectUrl)
        val bootVerifier = bootVerifierFactory?.invoke(platform)

        // A line the boot could never install is not a question worth asking, so selection is handed the
        // very gate the boot applies. Without a boot verifier there is no gate to ask and every line the
        // project publishes for is eligible -- the report is metadata-only, and nothing is installed.
        val bootable = bootVerifier?.bootableCombination() ?: { _, _ -> true }
        val targets = BootCandidateSelector.pickGrindTargets(project.files, linePolicy, bootable)
        val assessed = targets.map { target -> verdictFor(project, target, bootVerifier) }
        val perTarget = reconcileAcrossTargets(project, assessed)
        val suggestedEntries = perTarget.mapNotNull { it.suggestedEntry }.distinct().sorted()

        return ClientsideReport(
            platform = project.platform,
            slug = project.slug,
            projectUrl = project.projectUrl,
            phase = if (bootVerifier != null) "metadata + server-boot" else "metadata-only",
            suggestedEntries = suggestedEntries,
            perTarget = perTarget,
            fileNames = project.fileNames.sorted()
        )
    }

    /**
     * One loader's verdict plus the boot detail behind it, kept apart so [reconcileAcrossTargets] can
     * *rebuild* a superseded verdict's note instead of appending a correction to a claim that is no longer
     * true. Internal to the two passes; the report only ever sees [GrindTargetVerdict].
     */
    private data class LoaderAssessment(
        /** The verdict as the loader's own signals produced it, before any cross-loader reconciliation. */
        val verdict: GrindTargetVerdict,
        /** What the boot reported, or `null` when none ran — the one part of the note worth carrying over. */
        val bootDetail: String?
    )

    /**
     * Second pass over the project's verdicts: a crash cannot stand as clientside evidence when **another
     * target of the same project booted a server with the same list-entry**.
     *
     * "Target", not "loader", since the axis became the Minecraft version-line: two targets of one project
     * may share a loader and differ by line, and a clean boot on the 1.20 line disproves a 1.21 crash for
     * exactly the reason a clean NeoForge boot disproved a Forge crash — the published entry is matched with
     * `startsWith` and would strip both.
     *
     * **Why the entry and not the loader:** what gets published is a loader-agnostic file-name stem, matched
     * with `startsWith`. Measured 2026-08-23 — `iron-chests` produced `Forge → CRASHED` and
     * `NeoForge → SURVIVED` in one run, both deriving `ironchest-`, and the crash was published, which strips
     * the very NeoForge build that had just booted a server. Where the stems differ, the published entry
     * cannot strip the surviving build and the two verdicts do not contradict each other at all — a mod may
     * genuinely be client-only on one loader.
     *
     * The crash itself is preserved (`bootResult`, the excerpt): it happened, and it is worth diagnosing.
     * Only its *standing* changes, down to whatever the metadata alone supports.
     */
    private fun reconcileAcrossTargets(project: ProjectFiles, assessed: List<LoaderAssessment>): List<GrindTargetVerdict> {
        val verdicts = assessed.map { it.verdict }
        val reconciled = assessed.map { assessment ->
            val disproving = targetDisprovingTheCrash(assessment.verdict, verdicts)
                ?: return@map assessment.verdict
            log.info(
                "${project.slug}: ${assessment.verdict.loader} on Minecraft ${assessment.verdict.minecraftLine} " +
                    "crashed, but ${disproving.loader} on ${disproving.minecraftLine} booted the same entry " +
                    "'${assessment.verdict.suggestedEntry}' cleanly — the crash is not a sideness signal."
            )
            supersededByTarget(
                verdict = assessment.verdict,
                disproving = disproving,
                metadataOnly = verdictOf(
                    project.serverSide, project.clientSide, assessment.verdict.jarScan,
                    bootOutcome = null, bootAttempted = false
                ),
                bootDetail = assessment.bootDetail
            )
        }
        // Last, so a proof survives whatever the supersession pass decided about the other loaders.
        return propagateClientOnlyProof(reconciled)
    }

    /** Compute the verdict for one [target] of the resolved [project], optionally booting it. */
    private fun verdictFor(
        project: ProjectFiles,
        target: BootCandidateSelector.GrindTarget,
        bootVerifier: BootVerifier?
    ): LoaderAssessment {
        val loader = target.loader
        // The stem stays derived from the loader's WHOLE history, deliberately: it is what `/as-properties`
        // publishes and matches with `startsWith`, so it has to cover every build ever released. Narrowing
        // it to the line would publish an entry that misses the builds it was never shown.
        val loaderFiles = project.files.filter { loader in it.loaders }
        val stem = FilenameStemDeriver.deriveStem(loaderFiles.map { it.fileName })
        // The artifact this verdict is about is the target's, so the scan reads the jar that boots and the
        // report names it -- rather than the platform's newest *upload*, which is a different file whenever
        // an old build was re-published (the aether row that prompted this).
        val sample = target.file

        val jarScan = when {
            sample.locked -> JarScan.DEFERRED
            else -> scanSample(sample, target, project)
        }

        // The boot needs the metadata verdict too: a crash that *contradicts* a declared server support is
        // re-checked against other versions of the mod before it may stand (see BootVerifier.verify).
        val declaresServer = declaresServerSupport(project.serverSide, jarScan)
        val bootOutcome = bootVerifier?.let { verifier ->
            runCatching { verifier.verify(project, target, declaresServer) }
                .onFailure { log.warn("Boot-test for $loader / Minecraft ${target.minecraftLine} failed: ${it.message}") }
                .getOrNull()
        }

        val assessed = verdictOf(project.serverSide, project.clientSide, jarScan, bootOutcome, bootVerifier != null)
        val note = assessed.note
        return LoaderAssessment(
            verdict = GrindTargetVerdict(
                verdict = assessed.verdict,
                declared = assessed.declared,
                loader = loader,
                suggestedEntry = stem,
                declaredClientSide = project.clientSide,
                declaredServerSide = project.serverSide,
                jarScan = jarScan,
                bootResult = bootOutcome?.result,
                bootedLoader = bootOutcome?.bootedLoader,
                bootCrashExcerpt = bootOutcome?.crashExcerpt,
                firedRule = bootOutcome?.firedRule,
                decidedBy = bootOutcome?.decidedBy,
                blamedDependency = bootOutcome?.blamedDependency,
                blamedDependencyUrl = bootOutcome?.blamedDependencyUrl,
                stagedDependencies = bootOutcome?.stagedDependencies.orEmpty(),
                // The artifact's own published name. The whole history's common prefix
                // (`suggestedEntry`) is what gets published and loses the loader token for any project that
                // ever renamed its files; this is what a maintainer looks up on the platform page.
                //
                // The boot's answer outranks the metadata pick, and only the boot can give it: staging
                // re-selects on a loader or Minecraft contradiction the jar declares, and the crash
                // re-checks boot other builds entirely. Naming the file we guessed at instead of the one
                // that ran is what made a `DEPENDENCY_FAILURE` read as being about a build with no
                // dependencies.
                sampleFile = bootOutcome?.bootedFile ?: sample.fileName,
                minecraftLine = target.minecraftLine,
                // The target's version unless a re-check moved the evidence somewhere else, which is the
                // same precedence `sampleFile` and `bootedLoader` follow: report what ran, and where nothing
                // ran, report what it was going to be.
                minecraftVersion = bootOutcome?.minecraftVersion ?: target.minecraftVersion,
                note = listOfNotNull(note, bootOutcome?.detail).joinToString(" ").ifBlank { null }
            ),
            bootDetail = bootOutcome?.detail
        )
    }

    /**
     * Download [sample] and read its declared sideness at [target]'s Minecraft version, degrading to
     * [JarScan.ERROR] on failure.
     *
     * The version comes from the target rather than from the file: the scanner is chosen per Minecraft
     * version, and this used to ask `sample.minecraftVersions.maxOrNull()` — a *lexicographic* maximum,
     * which answers `1.9` for a file tagged `1.9` and `1.20.1`. It also stages into the target's own
     * directory, since one loader may now own several of them.
     */
    private fun scanSample(sample: ModFile, target: BootCandidateSelector.GrindTarget, project: ProjectFiles): JarScan {
        val loader = target.loader
        val jar = jarDownloader.download(
            sample,
            File(
                workDirectory,
                AttemptDirectory.nameFor(project.platform, project.slug, loader, target.minecraftLine)
            )
        )
        if (jar == null) {
            log.warn("Could not download ${sample.fileName} for $loader; jar-scan unavailable.")
            return JarScan.ERROR
        }
        return when (metadataScanner.scan(jar, loader, target.minecraftVersion)) {
            MetadataScanner.Result.CLIENT -> JarScan.CLIENT
            MetadataScanner.Result.SERVER_OR_BOTH -> JarScan.SERVER_OR_BOTH
            MetadataScanner.Result.ERROR -> JarScan.ERROR
        }
    }


    /**
     * The pure reconciliation predicates — what counts as a declared server claim, and which of two loaders'
     * outcomes may overrule the other. Stateless on purpose: they are the part of the confidence model that can
     * be unit-tested without a platform, a jar or a boot.
     */
    companion object {
        /**
         * One loader's evidence, folded into the four-state [Verdict] plus the [Declaration] it either
         * confirms or contradicts. Pure, so the whole decision is testable without a container.
         *
         * **The console decides and the metadata only declares** — see `ConsoleOutranksMetadataTest`. A
         * declaration is a self-report and is the unreliable half; it is recorded because a *contradicted*
         * one is the finding worth having (a mod claiming the server while calling client classes), never
         * because it can decide anything itself.
         *
         * @param serverSide    What the platform declares of the server side.
         * @param clientSide    What the platform declares of the client side; recorded, not decisive.
         * @param jarScan       What reading the jar's own descriptor concluded.
         * @param bootOutcome   The boot's outcome, or `null` when none ran.
         * @param bootAttempted Whether a boot was even asked for. `false` is the metadata-only report verb
         *                      and is **not** an error: nothing was prevented, so it stays INCONCLUSIVE.
         */
        internal fun verdictOf(
            serverSide: DeclaredSupport,
            clientSide: DeclaredSupport,
            jarScan: JarScan,
            bootOutcome: BootVerifier.BootOutcome?,
            bootAttempted: Boolean
        ): VerdictAssessment {
            val declared = DefaultBootRules.bundled()
                .firstMatch(listOf(MetadataFacts.line(serverSide, clientSide, jarScan)), RuleSource.METADATA)
                ?.rule?.declares

            // Nothing was asked to run, so nothing was prevented either. ERROR must keep meaning "a grind
            // that could not be performed", or an operator can no longer act on it.
            if (!bootAttempted) {
                return VerdictAssessment(Verdict.INCONCLUSIVE, declared, null, noteFor(declared, jarScan, null))
            }

            // A confirmation may only come from a rung that is decisive by construction: the built-in
            // client-class marker, which no broken harness can fabricate, or an operator rule that stated
            // the verdict deliberately. The bare exit-code rung means "nothing recognised why" and is the
            // one that filled the store with unevidenced HIGHs.
            val confirmedByRule = bootOutcome
                ?.takeIf { it.result == BootResult.CRASHED && it.decidedBy?.decisive == true }
                ?.let { outcome ->
                    // Credit the operator's rule only when the *rule* is what decided. `firedRule` also
                    // carries a rule that merely annotated -- one stating no verdict, riding along on the
                    // ladder's own decision -- and naming that as the confirming evidence sends an operator
                    // asking "which rule excluded this mod?" to a rule that declined to.
                    if (outcome.decidedBy == BootDecision.OPERATOR_RULE) {
                        outcome.firedRule ?: outcome.decidedBy.ruleId
                    } else {
                        outcome.decidedBy?.ruleId
                    }
                }

            val verdict = VerdictPolicy.decide(
                staging = bootOutcome?.prevention?.let { StagingOutcome.Prevented(bootOutcome.detail, it) }
                    ?: StagingOutcome.Staged,
                boot = bootOutcome?.result,
                confirmedByRule = confirmedByRule,
                declared = declared
            )
            return VerdictAssessment(verdict, declared, confirmedByRule, noteFor(declared, jarScan, bootOutcome))
        }

        /**
         * The sentence a reader gets beside the verdict, or `null` when the verdict speaks for itself.
         *
         * Carries over the two observations the retired `aggregateFor` made that the verdict alone cannot:
         * that a **contradicted server claim** is what makes a confirmation interesting rather than routine,
         * and that a distribution-locked file was never readable at all — otherwise indistinguishable from a
         * mod nobody has got round to.
         */
        private fun noteFor(
            declared: Declaration?,
            jarScan: JarScan,
            bootOutcome: BootVerifier.BootOutcome?
        ): String? = when {
            declared == Declaration.CONTRADICTORY ->
                "The platform and the jar disagree about server support, so neither is evidence."

            declared == Declaration.SERVER && bootOutcome?.result == BootResult.CRASHED ->
                "Declared server/both but the server crashed — the contradiction this engine exists to find."

            jarScan == JarScan.DEFERRED ->
                "Distribution-locked file (allowModDistribution=false): CurseForge publishes no " +
                    "download URL, so neither the jar-scan nor a boot can read this mod."

            else -> null
        }

        /**
         * Whether the mod *claims* to support servers — the platform's own `server_side: required`, or SPC's
         * scan of the jar reading server/both. Either source is enough; neither is trusted, which is why the
         * boot exists at all.
         *
         * Shared on purpose between the report's note and the boot's other-version crash re-check: the same
         * answer both prints the contradiction and decides whether it is worth re-checking, and a report
         * stating a contradiction the re-check silently decided did not exist would be worse than either
         * behaviour alone. [DeclaredSupport.OPTIONAL] deliberately does not count — "runs with or without the
         * side" is not a claim that the server works.
         */
        internal fun declaresServerSupport(serverSide: DeclaredSupport, jarScan: JarScan): Boolean =
            serverSide == DeclaredSupport.REQUIRED || jarScan == JarScan.SERVER_OR_BOTH

        /**
         * Carry one loader's client-only proof to every other loader of the same project.
         *
         * **A mod's features do not change with the loader; only its implementation does.** So a build that
         * reached client-only code proves the *mod* is client-only, and every loader's published entry is
         * exclusion-worthy — including a loader that booted cleanly, whose entry would otherwise stay
         * publishable and leave half the project un-excluded.
         *
         * Reported on `sodium`: its NeoForge build crashed reaching LWJGL while its Fabric build booted, and
         * the two carry *different* stems (`sodium-neoforge-` and `sodium-fabric-`), so excluding only the
         * proving loader would have left the other shipping into every server pack.
         *
         * Each inheriting verdict **keeps its own `bootResult`** — the report must not claim Fabric crashed
         * when it did not — and its note names the loader and rung that proved it, because a verdict that
         * cannot say where its evidence came from cannot be audited. Returns [verdicts] untouched when
         * nothing proved anything.
         */
        internal fun propagateClientOnlyProof(verdicts: List<GrindTargetVerdict>): List<GrindTargetVerdict> {
            val proof = verdicts.firstOrNull { it.decidedBy?.provesClientOnly == true } ?: return verdicts
            return verdicts.map { verdict ->
                if (verdict === proof) {
                    verdict
                } else {
                    verdict.copy(
                        verdict = Verdict.CONFIRMED,
                        note = listOfNotNull(
                            verdict.note,
                            // A prevented grind being superseded must not vanish: publishing this entry is
                            // right (the mod is client-only and the entry comes from platform metadata, not
                            // from a boot), but nothing ran here and a reader has to see that. Asked as
                            // `grindRan` rather than as a list of verdict names, which is how such a list
                            // comes to be missing one.
                            "This loader's own grind did not run (${verdict.verdict}).".takeIf {
                                !verdict.verdict.grindRan
                            },
                            "${proof.loader} proved this mod reaches client-only code " +
                                "(${proof.decidedBy?.ruleId}); a mod's features do not change with the loader, " +
                                "so this entry is excluded too."
                        ).joinToString(" ")
                    )
                }
            }
        }

        /**
         * The loader whose clean boot disproves [verdict]'s crash, or `null` when nothing in [allVerdicts]
         * does. Disproof takes all of: [verdict] actually crashed, another loader actually **survived under
         * its own loader**, and the two derive the *same* non-blank list-entry — because that shared entry is
         * what would be published, and `startsWith`-matching it would strip a build proven to boot a server.
         *
         * Deliberately not "any survival clears any crash": sideness can genuinely differ per loader, and
         * where the stems differ the published entry harms nothing. Only a clean boot counts, the same rule
         * every other guard here follows — an inconclusive or absent boot learned nothing.
         *
         * **Landmine — `bootResult` alone is not enough; check whose boot it was.** Since the other-version
         * re-check began spanning loaders, `BootVerifier.reconcileOtherVersionRecheck` can decide one
         * loader's verdict from another loader's clean boot, leaving `bootResult == SURVIVED` on a loader
         * that crashed. Accepting that as a disproof breaks the invariant the entry comparison exists to
         * enforce: the build that actually booted belongs to a third loader whose stem may differ, so the
         * published entry would strip nothing that was proven bootable — and the note would say
         * "<loader> booted a server" of a loader that did not. `embeddium-` (Forge/NeoForge) versus
         * `sodium-fabric-` is exactly that shape, and it is the one `FilenameStemDeriver.deriveStem`
         * documents.
         */
        internal fun targetDisprovingTheCrash(
            verdict: GrindTargetVerdict,
            allVerdicts: List<GrindTargetVerdict>
        ): GrindTargetVerdict? {
            // Client-only evidence is about the mod, not the build that produced it, so no other loader's
            // clean boot disproves it. An *unexplained* crash still is disprovable -- that guard is why
            // `iron-chests` stopped publishing off one bad build, and it stays.
            if (verdict.decidedBy?.provesClientOnly == true) {
                return null
            }
            if (verdict.bootResult != BootResult.CRASHED) {
                return null
            }
            val entry = verdict.suggestedEntry?.trim()?.ifEmpty { null } ?: return null
            return allVerdicts.firstOrNull { other ->
                // Another *target*, which two verdicts of one project always are: they differ by Minecraft
                // line, and the same loader routinely wins more than one. Comparing loaders would refuse a
                // 1.20 boot the right to disprove a 1.21 crash of the same loader, whose published entry it
                // shares and would therefore be stripped by.
                other !== verdict &&
                    other.bootResult == BootResult.SURVIVED &&
                    other.bootedLoader == other.loader &&
                    other.suggestedEntry?.trim() == entry
            }
        }

        /**
         * Restate [verdict] once [disproving] has taken the sting out of its crash: the confidence drops to
         * [metadataOnly] — what `aggregate` yields for the same signals with no boot, so there is one
         * confidence ladder and not a second one written here — and the note is **rebuilt** from the metadata
         * caveat, the [bootDetail] and why the crash no longer counts.
         *
         * Rebuilt rather than appended to, because the old note ends in "a strong clientside signal", which is
         * exactly what is no longer true; bolting a correction onto a false sentence is how prose goes stale.
         * `bootResult` and the crash excerpt are kept untouched: the server did crash, and that is worth
         * diagnosing even though it says nothing about which side the mod belongs on.
         */
        internal fun supersededByTarget(
            verdict: GrindTargetVerdict,
            disproving: GrindTargetVerdict,
            metadataOnly: VerdictAssessment,
            bootDetail: String?
        ): GrindTargetVerdict {
            val metadataNote = metadataOnly.note
            val supersedes = "Crashed, but ${disproving.loader} booted a server with the same entry " +
                "'${verdict.suggestedEntry?.trim()}' — the crash belongs to that build, not to the mod's sideness."
            return verdict.copy(
                verdict = metadataOnly.verdict,
                declared = metadataOnly.declared,
                note = listOfNotNull(metadataNote, bootDetail, supersedes).joinToString(" ").ifBlank { null }
            )
        }
    }
}
