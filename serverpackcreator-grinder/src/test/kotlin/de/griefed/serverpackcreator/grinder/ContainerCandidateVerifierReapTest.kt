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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins *which identity* the reaper is asked to reclaim, which has to be the one the staging was **named
 * from** — the resolved report's — not the candidate that was queued.
 *
 * The two can disagree, and the codebase says so out loud: `Grinder` logs `"Platform mismatch for …:
 * candidate says 'X', resolved report says 'Y'"` when a source labels a project differently from the
 * platform that resolves it, and a slug is a mutable display name a rename can move out from under a
 * queued candidate. Staging directories are named from `ProjectFiles.platform`/`slug`, so reaping on the
 * candidate's copy of either silently matches nothing and leaks a full server pack per attempt — the
 * disk-growth class `BootWorkspaceReaper` exists for (98 GB across 1750 directories, measured 2026-07-30).
 *
 * The decision is pinned here rather than through `verify`, which needs an `ApiWrapper`, a loader cache and
 * a container engine — the same split the boot verifier's own pure decisions follow.
 */
internal class ContainerCandidateVerifierReapTest {

    private fun candidate(platform: String, slug: String) = GrindCandidate(
        projectUrl = "https://example.invalid/$slug",
        slug = slug,
        popularity = 0,
        platform = platform,
        projectId = "id"
    )

    /** The report resolved the project, so its identity is the one the directories carry. */
    @Test
    fun theResolvedReportsIdentityIsWhatGetsReaped() {
        val queued = candidate(ModPlatforms.CURSEFORGE, "creative-core")
        val resolved = clientsideReport(slug = "creativecore", perLoader = emptyList(), platform = ModPlatforms.MODRINTH)

        Assertions.assertEquals(
            ModPlatforms.MODRINTH to "creativecore",
            ContainerCandidateVerifier.reapTarget(queued, resolved),
            "staging is named from the report, so reclamation has to ask for the same thing"
        )
    }

    /**
     * A verification that threw never produced a report, and that is exactly when staging is most likely to
     * be left behind — so the candidate's identity is the fallback rather than reaping nothing at all.
     */
    @Test
    fun aVerificationThatProducedNoReportFallsBackToTheCandidate() {
        val queued = candidate(ModPlatforms.MODRINTH, "creativecore")

        Assertions.assertEquals(
            ModPlatforms.MODRINTH to "creativecore",
            ContainerCandidateVerifier.reapTarget(queued, null)
        )
    }
}
