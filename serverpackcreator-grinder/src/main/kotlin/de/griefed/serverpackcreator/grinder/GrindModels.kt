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

import de.griefed.serverpackcreator.clientside.ClientsideReport
import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.clientside.DeclaredSupport
import de.griefed.serverpackcreator.clientside.JarScan
import java.time.Instant

/**
 * A mod queued for verification: the project link to grind plus the [popularity] used to order the
 * queue (most-used mods first, since they are the most likely to land in a modpack). [slug] is the
 * project's identifier **within its [platform]** — slugs are *not* globally unique (`jei` exists on both
 * Modrinth and CurseForge), so the pair identifies a project and the store keys on both. [platform] must
 * match the name the corresponding clientside `ModPlatform` reports (see [ModPlatforms]) or the same
 * project would be re-ground every pass.
 *
 * @author Griefed
 */
data class GrindCandidate(
    /** Canonical link to the project page, which is what the verification engine resolves files from. */
    val projectUrl: String,
    /** The project's slug on its platform — a *mutable* display name; see [projectId] for stable identity. */
    val slug: String,
    /** Download count, used only to rank a batch so the most-used mods are ground first. */
    val popularity: Long,
    /** Which platform this came from (`Modrinth`, `CurseForge`). Part of a project's identity: slugs collide across platforms. */
    val platform: String,
    /**
     * The platform's own immutable project identifier (Modrinth's `project_id`, CurseForge's numeric `id`), or
     * `null` when the candidate came from somewhere that does not know it — a URL passed on the command line, for
     * instance. A slug is a mutable display name, so this is what makes a renamed project recognisable as itself.
     */
    val projectId: String? = null
)

/**
 * The platform names shared by the candidate sources and the clientside `ModPlatform` implementations.
 * They must agree: a candidate's [GrindCandidate.platform] is what the store's freshness check looks up,
 * while the recorded [GrindVerdict.platform] comes from the resolved clientside report. `Grinder` warns
 * when the two disagree rather than silently re-grinding forever.
 *
 * @author Griefed
 */
object ModPlatforms {
    /** As reported by clientside's `ModrinthPlatform`. */
    const val MODRINTH = "Modrinth"

    /** As reported by clientside's `CurseForgePlatform`. */
    const val CURSEFORGE = "CurseForge"

    /** Fallback for a hand-passed project URL whose host matches no known platform. */
    const val UNKNOWN = "Unknown"

    /** The platforms a crawl can have a position for — the order the status document lists them in. */
    val known = listOf(MODRINTH, CURSEFORGE)

    /** Best-effort platform for an arbitrary project [url] — used for URLs passed on the command line. */
    fun ofUrl(url: String): String = when {
        url.contains("modrinth.com", ignoreCase = true) -> MODRINTH
        url.contains("curseforge.com", ignoreCase = true) -> CURSEFORGE
        else -> UNKNOWN
    }
}

/**
 * The accumulated verdict for one `(project, loader)` — one row behind the eventual sortable / CSV
 * table. [suggestedEntry] is the clientside-list name-pattern (the file-name stem), [confidence] the
 * clientside engine's per-loader verdict; together with the project link they are exactly the columns
 * the table exposes.
 *
 * @author Griefed
 */
data class GrindVerdict(
    /** Platform the project was ground on. Part of the dedup identity, since the same slug exists on both. */
    val platform: String,
    /** The project's slug *at the time of verification* — recorded for the report; identity lives in [projectId]. */
    val slug: String,
    /** Link to the project, carried through so a reader of the report can check the verdict against the source. */
    val projectUrl: String,
    /** The modloader this verdict is about. One project yields one verdict per loader, since sideness can differ. */
    val loader: String,
    /** The line to add to the clientside fallback-list if accepted, or `null` when nothing is being suggested. */
    val suggestedEntry: String?,
    /** How strongly the evidence says "clientside". Only a crash is decisive; a clean boot proves nothing. */
    val confidence: Confidence,
    /** Human-readable evidence behind [confidence] — the boot outcome and exit detail, as shown in the report. */
    val detail: String,
    /** When this verdict was reached, which the re-verify TTL compares against to decide staleness. */
    val verifiedAt: Instant,
    /**
     * The platform's immutable project identifier, or `null` for verdicts recorded before it was tracked. Dedup
     * falls back to [slug] when absent, so a store written by an older build stays readable and correct.
     */
    val projectId: String? = null,
    /**
     * Client support as the *platform* declares it, or `null` for a verdict recorded before this was carried.
     * Nullable rather than [DeclaredSupport.UNKNOWN] on purpose: `UNKNOWN` is a real answer the platform gives —
     * CurseForge gives it for *every* project, since it publishes no sideness at all — while `null` means nobody
     * ever asked. Collapsing the two would make a legacy row indistinguishable from a CurseForge row.
     */
    val declaredClientSide: DeclaredSupport? = null,
    /** Server support as the platform declares it, or `null` when unrecorded. See [declaredClientSide]. */
    val declaredServerSide: DeclaredSupport? = null,
    /** What SPC's own scan of the jar descriptor concluded, or `null` for a verdict recorded before this was carried. */
    val jarScan: JarScan? = null,
    /**
     * The loader whose boot actually produced the evidence, which is not always [loader]: a cross-loader
     * re-check can settle one loader's verdict from another loader's clean boot.
     */
    val bootedLoader: String? = null,
    /**
     * The operator console rule that decided or annotated this verdict, or `null` when the built-in ladder
     * settled it alone. A column rather than only a phrase in [detail], because finding a rule that fires
     * too broadly means counting the verdicts it produced.
     */
    val firedRule: String? = null,
    /**
     * The dependency jars staged beside the candidate for the decisive boot, so a verdict can be traced to
     * the pack that produced it rather than only to the mod it is about.
     */
    val stagedDependencies: List<String> = emptyList(),
    /**
     * Which classifier rung settled this verdict's boot, by name, or `null` for a verdict recorded before it
     * was tracked. **The publication gate reads this**: only a decision `BootDecision.decisive` marks may
     * reach the fallback list, so a crash that was really a mixin failure, a solver give-up or a bare
     * non-zero exit can never publish. A `String` rather than the enum, so a rung added by a newer build
     * leaves the store readable to an older one.
     */
    val decidedBy: String? = null
)

/**
 * Runs the full boot pipeline for one candidate and returns the clientside engine's per-loader report.
 * The production implementation wires a `ClientsideVerifier` with a container-backed `BootVerifier`
 * (over a `ContainerServerRunner` + `LoaderCache`); tests supply a fake. Collapsing the
 * integration-bound pipeline behind this one seam is what keeps [Grinder]'s orchestration unit-testable.
 *
 * @author Griefed
 */
fun interface CandidateVerifier {
    /**
     * Resolve, scan and boot-verify [candidate], returning the per-loader [ClientsideReport]. May throw;
     * [Grinder] treats a thrown verification as a skipped candidate.
     */
    fun verify(candidate: GrindCandidate): ClientsideReport
}
