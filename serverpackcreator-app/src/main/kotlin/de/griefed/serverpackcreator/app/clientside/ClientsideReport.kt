/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.clientside

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
    CLIENT,
    SERVER_OR_BOTH,
    /** Skipped now because the file is locked; the jar-scan happens in the boot-phase. */
    DEFERRED,
    ERROR
}

/**
 * The per-loader verdict: the suggested list-entry plus every signal that produced the [confidence].
 *
 * @param loader            Canonical loader-name (Forge, Fabric, …).
 * @param suggestedEntry    Derived clientside-list entry (file-name stem), or `null` if not derivable.
 * @param declaredClientSide Platform-declared client support (Modrinth; UNKNOWN for CurseForge).
 * @param declaredServerSide Platform-declared server support (Modrinth; UNKNOWN for CurseForge).
 * @param jarScan           Sideness read from the jar metadata via SPC's scanners.
 * @param bootResult        Outcome of the server-boot test, or `null` when boot was not run.
 * @param confidence        Aggregate confidence for this loader.
 * @param sampleFile        The file-name the jar-scan ran against (for traceability).
 * @param note              Optional caveat (e.g. a metadata/jar-scan contradiction, or a boot detail).
 * @author Griefed
 */
data class LoaderVerdict(
    val loader: String,
    val suggestedEntry: String?,
    val declaredClientSide: Sideness,
    val declaredServerSide: Sideness,
    val jarScan: JarScan,
    val bootResult: BootResult?,
    val bootCrashExcerpt: String?,
    val confidence: Confidence,
    val sampleFile: String?,
    val note: String?
)

/**
 * Machine-readable report a maintainer reviews before accepting a clientside-mod request. Rendered to
 * Markdown by [renderMarkdown] for the issue-comment, with the raw data embedded as JSON for the
 * acceptance-workflow to read back.
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
