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
 * How strongly the evidence supports "this mod is clientside-only". Phase 1 (metadata only) reaches
 * at most [MEDIUM]; the boot-test (Phase 2) is what promotes a crashing mod to [HIGH].
 *
 * @author Griefed
 */
enum class Confidence {
    HIGH,
    MEDIUM,
    LOW,
    INCONCLUSIVE
}

/**
 * Result of the jar metadata scan for one loader, including the case where it was deliberately put
 * off because the file is distribution-locked (its jar is only fetched in the boot-phase).
 *
 * @author Griefed
 */
enum class JarScan {
    /** The jar's own descriptor says client-only. */
    CLIENT,

    /** The descriptor says server, says both, or said nothing usable — all of which keep the mod. */
    SERVER_OR_BOTH,

    /** Skipped now because the file is locked; the jar-scan happens in the boot-phase. */
    DEFERRED,

    /** The jar could not be read at all (corrupt archive, unparseable descriptor). Not a sideness claim. */
    ERROR
}

/**
 * The per-loader verdict: the suggested list-entry plus every signal that produced the [confidence].
 *
 * @param loader            Canonical loader-name (Forge, Fabric, …).
 * @param suggestedEntry    Derived clientside-list entry (file-name stem), or `null` if not derivable.
 * @param declaredClientSide Platform-declared client support (Modrinth; UNKNOWN for CurseForge).
 * @param declaredServerSide Platform-declared server support (Modrinth; UNKNOWN for CurseForge).
 * @param jarScan           DeclaredSupport read from the jar metadata via SPC's scanners.
 * @param bootResult        Outcome of the server-boot test, or `null` when boot was not run.
 * @param bootedLoader      Which loader actually produced [bootResult], or `null` when no boot ran. Usually
 *                          [loader]; it differs when a cross-loader crash re-check decided the outcome, and
 *                          the difference is load-bearing — only a loader's *own* clean boot may disprove
 *                          another loader's crash (see `ClientsideVerifier.loaderDisprovingTheCrash`).
 * @param bootCrashExcerpt  The slice of the crashed console a maintainer reads to judge *why* it crashed, or
 *                          `null` when the boot did not crash. Kept even when a later pass strips the crash of
 *                          its standing: the server did crash, and that is still worth diagnosing.
 * @param confidence        Aggregate confidence for this loader.
 * @param sampleFile        The file-name the jar-scan ran against (for traceability).
 * @param note              Optional caveat (e.g. a metadata/jar-scan contradiction, or a boot detail).
 * @author Griefed
 */
data class LoaderVerdict(
    val loader: String,
    val suggestedEntry: String?,
    val declaredClientSide: DeclaredSupport,
    val declaredServerSide: DeclaredSupport,
    val jarScan: JarScan,
    val bootResult: BootResult?,
    val bootedLoader: String?,
    val bootCrashExcerpt: String?,
    val confidence: Confidence,
    val sampleFile: String?,
    val note: String?,
    /**
     * The id of the operator console rule that decided or annotated this loader's boot, or `null` when the
     * built-in ladder settled it alone. Carried so "which verdicts did rule X decide?" is answerable from
     * the data rather than by reading prose.
     */
    val firedRule: String? = null,
    /**
     * The injected dependency this loader's crash names, or `null`. Annotation only — the verdict is the
     * boot's own; the grinder requeues this dependency so the question is answered by grinding it.
     */
    val blamedDependency: String? = null,
    /** The blamed dependency's project link, so the grinder can queue it for its own verification. */
    val blamedDependencyUrl: String? = null,
    /**
     * Which classifier rung settled this loader's boot, or `null` when none ran. Only a decision marked
     * `BootDecision.decisive` may publish a clientside entry.
     */
    val decidedBy: BootDecision? = null,
    /** The dependency jars staged beside the candidate, so a verdict names the pack it was booted with. */
    val stagedDependencies: List<String> = emptyList()
)

/**
 * Machine-readable report a maintainer reviews before accepting a clientside-mod request. Rendered to
 * Markdown by [ClientsideReportRenderer.renderMarkdown] for the issue-comment, with the raw data
 * embedded as JSON for the acceptance-workflow to read back.
 *
 * @param platform         "Modrinth" or "CurseForge".
 * @param slug             Project-slug.
 * @param projectUrl       Original issue-link.
 * @param phase            Which signals were collected ("metadata-only" in Phase 1).
 * @param suggestedEntries Distinct list-entries across all loaders.
 * @param perLoader        Per-loader verdicts.
 * @param fileNames        Every published file-name, so the maintainer can sanity-check the stems.
 * @author Griefed
 */
data class ClientsideReport(
    val platform: String,
    val slug: String,
    val projectUrl: String,
    val phase: String,
    val suggestedEntries: List<String>,
    val perLoader: List<LoaderVerdict>,
    val fileNames: List<String>
)
