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
import java.time.Instant

/**
 * A mod queued for verification: the project link to grind plus the [popularity] used to order the
 * queue (most-used mods first, since they are the most likely to land in a modpack). [slug] is the
 * project's stable identifier, used to skip projects already ground.
 *
 * @author Griefed
 */
data class GrindCandidate(
    val projectUrl: String,
    val slug: String,
    val popularity: Long
)

/**
 * The accumulated verdict for one `(project, loader)` — one row behind the eventual sortable / CSV
 * table. [suggestedEntry] is the clientside-list name-pattern (the file-name stem), [confidence] the
 * clientside engine's per-loader verdict; together with the project link they are exactly the columns
 * the table exposes.
 *
 * @author Griefed
 */
data class GrindVerdict(
    val platform: String,
    val slug: String,
    val projectUrl: String,
    val loader: String,
    val suggestedEntry: String?,
    val confidence: Confidence,
    val detail: String,
    val verifiedAt: Instant
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
