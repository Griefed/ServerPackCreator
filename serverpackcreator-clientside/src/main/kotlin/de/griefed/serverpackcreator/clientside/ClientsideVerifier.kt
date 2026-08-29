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
 * sideness with SPC's jar metadata scan into a per-loader [Confidence]. The boot signal is added in
 * Phase 2; until then a clientside-looking mod tops out at [Confidence.MEDIUM].
 *
 * @param platforms          The supported hosting platforms, tried in order via [ModPlatform.handles].
 * @param metadataScanner    Reads declared sideness out of a downloaded jar.
 * @param jarDownloader      Fetches freely-distributable files for scanning (locked files are deferred).
 * @param workDirectory      Scratch directory for downloaded jars.
 * @param bootVerifierFactory When non-null, builds a [BootVerifier] for the resolved platform to add
 *                            the server-boot signal (Phase 2); when null, the report is metadata-only.
 * @author Griefed
 */
class ClientsideVerifier(
    private val platforms: List<ModPlatform>,
    private val metadataScanner: MetadataScanner,
    private val jarDownloader: JarDownloader,
    private val workDirectory: File,
    private val bootVerifierFactory: ((ModPlatform) -> BootVerifier)? = null
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

        val assessed = project.loaders.map { loader -> verdictFor(project, loader, bootVerifier) }
        val perLoader = reconcileAcrossLoaders(project, assessed)
        val suggestedEntries = perLoader.mapNotNull { it.suggestedEntry }.distinct().sorted()

        return ClientsideReport(
            platform = project.platform,
            slug = project.slug,
            projectUrl = project.projectUrl,
            phase = if (bootVerifier != null) "metadata + server-boot" else "metadata-only",
            suggestedEntries = suggestedEntries,
            perLoader = perLoader,
            fileNames = project.fileNames.sorted()
        )
    }

    /**
     * One loader's verdict plus the boot detail behind it, kept apart so [reconcileAcrossLoaders] can
     * *rebuild* a superseded verdict's note instead of appending a correction to a claim that is no longer
     * true. Internal to the two passes; the report only ever sees [LoaderVerdict].
     */
    private data class LoaderAssessment(
        /** The verdict as the loader's own signals produced it, before any cross-loader reconciliation. */
        val verdict: LoaderVerdict,
        /** What the boot reported, or `null` when none ran — the one part of the note worth carrying over. */
        val bootDetail: String?
    )

    /**
     * Second pass over the per-loader verdicts: a crash cannot stand as clientside evidence when **another
     * loader of the same project booted a server with the same list-entry**.
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
    private fun reconcileAcrossLoaders(project: ProjectFiles, assessed: List<LoaderAssessment>): List<LoaderVerdict> {
        val verdicts = assessed.map { it.verdict }
        return assessed.map { assessment ->
            val disproving = loaderDisprovingTheCrash(assessment.verdict, verdicts)
                ?: return@map assessment.verdict
            log.info(
                "${project.slug}: ${assessment.verdict.loader} crashed, but ${disproving.loader} booted the same " +
                    "entry '${assessment.verdict.suggestedEntry}' cleanly — the crash is not a sideness signal."
            )
            supersededByLoader(
                verdict = assessment.verdict,
                disproving = disproving,
                metadataOnly = aggregate(project.serverSide, assessment.verdict.jarScan, null),
                bootDetail = assessment.bootDetail
            )
        }
    }

    /** Compute the verdict for a single [loader] of the resolved [project], optionally booting it. */
    private fun verdictFor(project: ProjectFiles, loader: String, bootVerifier: BootVerifier?): LoaderAssessment {
        val loaderFiles = project.files.filter { loader in it.loaders }
        val stem = FilenameStemDeriver.deriveStem(loaderFiles.map { it.fileName })
        val sample = loaderFiles.firstOrNull()

        val jarScan = when {
            sample == null -> JarScan.ERROR
            sample.locked -> JarScan.DEFERRED
            else -> scanSample(sample, loader, project)
        }

        // The boot needs the metadata verdict too: a crash that *contradicts* a declared server support is
        // re-checked against other versions of the mod before it may stand (see BootVerifier.verify).
        val declaresServer = declaresServerSupport(project.serverSide, jarScan)
        val bootOutcome = bootVerifier?.let { verifier ->
            runCatching { verifier.verify(project, loader, declaresServer) }
                .onFailure { log.warn("Boot-test for $loader failed: ${it.message}") }
                .getOrNull()
        }

        val (confidence, note) = aggregate(project.serverSide, jarScan, bootOutcome?.result)
        return LoaderAssessment(
            verdict = LoaderVerdict(
                loader = loader,
                suggestedEntry = stem,
                declaredClientSide = project.clientSide,
                declaredServerSide = project.serverSide,
                jarScan = jarScan,
                bootResult = bootOutcome?.result,
                bootedLoader = bootOutcome?.bootedLoader,
                bootCrashExcerpt = bootOutcome?.crashExcerpt,
                firedRule = bootOutcome?.firedRule,
                confidence = confidence,
                sampleFile = sample?.fileName,
                note = listOfNotNull(note, bootOutcome?.detail).joinToString(" ").ifBlank { null }
            ),
            bootDetail = bootOutcome?.detail
        )
    }

    /** Download a sample file and read its declared sideness, degrading to [JarScan.ERROR] on failure. */
    private fun scanSample(sample: ModFile, loader: String, project: ProjectFiles): JarScan {
        val minecraftVersion = sample.minecraftVersions.maxOrNull() ?: ""
        val jar = jarDownloader.download(
            sample,
            File(workDirectory, AttemptDirectory.nameFor(project.platform, project.slug, loader))
        )
        if (jar == null) {
            log.warn("Could not download ${sample.fileName} for $loader; jar-scan unavailable.")
            return JarScan.ERROR
        }
        return when (metadataScanner.scan(jar, loader, minecraftVersion)) {
            MetadataScanner.Result.CLIENT -> JarScan.CLIENT
            MetadataScanner.Result.SERVER_OR_BOTH -> JarScan.SERVER_OR_BOTH
            MetadataScanner.Result.ERROR -> JarScan.ERROR
        }
    }

    /**
     * Combine the platform-declared server-side support, the jar-scan and (when run) the boot-result
     * into a confidence. A crash is the strongest single signal — it promotes any metadata to
     * [Confidence.HIGH], including the "declares server/both yet crashes" lie the metadata can't
     * catch. Without a crash, a client-leaning metadata signal is [Confidence.MEDIUM], a clear
     * server/both is [Confidence.LOW], and everything unknown/deferred is [Confidence.INCONCLUSIVE].
     */
    private fun aggregate(serverSide: DeclaredSupport, jarScan: JarScan, bootResult: BootResult?): Pair<Confidence, String?> {
        val declaresClient = serverSide == DeclaredSupport.UNSUPPORTED
        val declaresServer = serverSide == DeclaredSupport.REQUIRED
        val jarClient = jarScan == JarScan.CLIENT
        val jarServer = jarScan == JarScan.SERVER_OR_BOTH
        val metadataClient = declaresClient || jarClient
        val metadataServer = declaresServerSupport(serverSide, jarScan)

        val note = when {
            declaresClient && jarServer -> "Platform marks server unsupported but the jar declares server/both."
            declaresServer && jarClient -> "Platform marks server required but the jar declares client-only."
            bootResult == BootResult.CRASHED && metadataServer -> "Declared server/both but the server crashed — a strong clientside signal."
            jarScan == JarScan.DEFERRED && bootResult == null -> "Distribution-locked file; jar-scan deferred to the boot-phase."
            else -> null
        }

        // A crash is decisive regardless of declaration; otherwise fall back to the metadata signal,
        // which boot can confirm but (short of a crash) not overturn.
        val confidence = when {
            bootResult == BootResult.CRASHED -> Confidence.HIGH
            metadataClient -> Confidence.MEDIUM
            metadataServer -> Confidence.LOW
            else -> Confidence.INCONCLUSIVE
        }
        return confidence to note
    }

    /**
     * The pure reconciliation predicates — what counts as a declared server claim, and which of two loaders'
     * outcomes may overrule the other. Stateless on purpose: they are the part of the confidence model that can
     * be unit-tested without a platform, a jar or a boot.
     */
    companion object {
        /**
         * Whether the mod *claims* to support servers — the platform's own `server_side: required`, or SPC's
         * scan of the jar reading server/both. Either source is enough; neither is trusted, which is why the
         * boot exists at all.
         *
         * Shared on purpose between the confidence aggregation and the boot's other-version crash re-check:
         * the same answer both prints "Declared server/both but the server crashed" and decides whether that
         * contradiction is worth re-checking, and a report that states the contradiction while the re-check
         * silently decided there was none would be worse than either behaviour alone. [DeclaredSupport.OPTIONAL]
         * deliberately does not count — "runs with or without the side" is not a claim that the server works.
         */
        internal fun declaresServerSupport(serverSide: DeclaredSupport, jarScan: JarScan): Boolean =
            serverSide == DeclaredSupport.REQUIRED || jarScan == JarScan.SERVER_OR_BOTH

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
         * `sodium-fabric-` is exactly that shape, and it is the one `FilenameStemDeriver.deriveStems`
         * documents.
         */
        internal fun loaderDisprovingTheCrash(verdict: LoaderVerdict, allVerdicts: List<LoaderVerdict>): LoaderVerdict? {
            if (verdict.bootResult != BootResult.CRASHED) {
                return null
            }
            val entry = verdict.suggestedEntry?.trim()?.ifEmpty { null } ?: return null
            return allVerdicts.firstOrNull { other ->
                other.loader != verdict.loader &&
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
        internal fun supersededByLoader(
            verdict: LoaderVerdict,
            disproving: LoaderVerdict,
            metadataOnly: Pair<Confidence, String?>,
            bootDetail: String?
        ): LoaderVerdict {
            val (confidence, metadataNote) = metadataOnly
            val supersedes = "Crashed, but ${disproving.loader} booted a server with the same entry " +
                "'${verdict.suggestedEntry?.trim()}' — the crash belongs to that build, not to the mod's sideness."
            return verdict.copy(
                confidence = confidence,
                note = listOfNotNull(metadataNote, bootDetail, supersedes).joinToString(" ").ifBlank { null }
            )
        }
    }
}
