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

import de.griefed.serverpackcreator.clientside.AttemptDirectory
import de.griefed.serverpackcreator.clientside.BootResult
import de.griefed.serverpackcreator.clientside.BootVerifier
import de.griefed.serverpackcreator.clientside.DeclaredSupport
import de.griefed.serverpackcreator.clientside.JarScan
import de.griefed.serverpackcreator.clientside.GrindTargetVerdict
import de.griefed.serverpackcreator.grinder.report.BootLogStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

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

    /** Stage a finished attempt's kept console exactly where `BootVerifier` and the reaper leave it. */
    private fun stagedConsole(bootRoot: File, platform: String, slug: String, loader: String, text: String) {
        File(bootRoot, AttemptDirectory.nameFor(platform, slug, loader, "1.20"))
            .apply { mkdirs() }
            .resolve("boot.log")
            .writeText(text)
    }

    private fun verdict(loader: String, bootResult: BootResult?) = GrindTargetVerdict(
        loader = loader,
        suggestedEntry = "$loader-",
        declaredClientSide = DeclaredSupport.UNKNOWN,
        declaredServerSide = DeclaredSupport.REQUIRED,
        jarScan = JarScan.SERVER_OR_BOTH,
        bootResult = bootResult,
        bootedLoader = loader,
        bootCrashExcerpt = null,
        sampleFile = null,
        note = null
    )

    /**
     * **A boot's evidence is kept as the boot finishes, because staging is about to be reused.** The attempt
     * directory is wiped and re-created by the *next attempt* — not merely the next re-grind — so a
     * re-check destroys the console of the boot it was checking. Retention is
     * [de.griefed.serverpackcreator.clientside.BootArtifacts.worthKeeping]: a boot that reached its
     * ready-line proves nothing about sideness and explains nothing either.
     */
    @Test
    fun aNonSurvivedAttemptsEvidenceIsKeptOutsideStaging(@TempDir work: File) {
        val store = BootLogStore(File(work, "boot-logs"))
        val pack = preparedAttempt(work, "creativecore", "Fabric")
        File(pack.serverPack, "logs").mkdirs()
        File(pack.serverPack, "logs/latest.log").writeText("the server's own log")
        val kept = mutableListOf<String>()

        ContainerCandidateVerifier.keepAttemptArtifacts(
            pack,
            BootVerifier.BootOutcome(BootResult.CRASHED, null, "crashed", console = "NoClassDefFoundError: net/minecraft/client/Minecraft"),
            store,
            kept
        )

        Assertions.assertEquals(3, kept.size, "console, the server's log, and the index naming both")
        Assertions.assertTrue(
            kept.any { store.read(it)?.contains("net/minecraft/client/Minecraft") == true },
            "the console has to be among what was kept"
        )
        Assertions.assertTrue(
            kept.any { store.read(it) == "the server's own log" },
            "the server's own log is the half the console does not have"
        )
    }

    /** A clean boot keeps nothing: it proves nothing about sideness and explains nothing either. */
    @Test
    fun aSurvivedAttemptKeepsNothing(@TempDir work: File) {
        val store = BootLogStore(File(work, "boot-logs"))
        val pack = preparedAttempt(work, "creativecore", "NeoForge")
        File(pack.serverPack, "logs").mkdirs()
        File(pack.serverPack, "logs/latest.log").writeText("Done (21.5s)! For help")
        val kept = mutableListOf<String>()

        ContainerCandidateVerifier.keepAttemptArtifacts(
            pack,
            BootVerifier.BootOutcome(BootResult.SURVIVED, null, "survived", console = "Done (21.5s)! For help"),
            store,
            kept
        )

        Assertions.assertTrue(kept.isEmpty())
        Assertions.assertTrue(store.list().isEmpty())
    }

    /**
     * An attempt whose pack holds nothing and whose console never reached memory keeps nothing and says
     * nothing — collecting evidence must not fail a grind that already has its verdict.
     */
    @Test
    fun anAttemptWithNothingToKeepDoesNotThrow(@TempDir work: File) {
        val store = BootLogStore(File(work, "boot-logs"))
        val kept = mutableListOf<String>()

        ContainerCandidateVerifier.keepAttemptArtifacts(
            preparedAttempt(work, "ghost", "Forge"),
            BootVerifier.BootOutcome(BootResult.CRASHED, null, "crashed", console = null),
            store,
            kept
        )

        Assertions.assertTrue(kept.isEmpty())
        Assertions.assertTrue(store.list().isEmpty())
    }

    /** One staged attempt, shaped as `BootVerifier` leaves it: a pack beside the attempt's own log file. */
    private fun preparedAttempt(work: File, slug: String, loader: String): BootVerifier.Prepared.Ready {
        val attemptDir = File(work, "boot/" + AttemptDirectory.nameFor(ModPlatforms.MODRINTH, slug, loader, "1.20"))
        val pack = File(attemptDir, "serverpack").apply { mkdirs() }
        return BootVerifier.Prepared.Ready(pack, File(attemptDir, "boot.log"), "1.20.1", loader, "47.2.0")
    }

    /** The report resolved the project, so its identity is the one the directories carry. */
    @Test
    fun theResolvedReportsIdentityIsWhatGetsReaped() {
        val queued = candidate(ModPlatforms.CURSEFORGE, "creative-core")
        val resolved = clientsideReport(slug = "creativecore", perTarget = emptyList(), platform = ModPlatforms.MODRINTH)

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
