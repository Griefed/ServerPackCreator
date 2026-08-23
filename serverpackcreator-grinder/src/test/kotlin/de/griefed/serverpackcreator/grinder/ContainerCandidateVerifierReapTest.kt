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
import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.clientside.DeclaredSupport
import de.griefed.serverpackcreator.clientside.JarScan
import de.griefed.serverpackcreator.clientside.LoaderVerdict
import de.griefed.serverpackcreator.grinder.report.CrashLogStore
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
        File(bootRoot, AttemptDirectory.nameFor(platform, slug, loader))
            .apply { mkdirs() }
            .resolve("boot.log")
            .writeText(text)
    }

    private fun verdict(loader: String, bootResult: BootResult?) = LoaderVerdict(
        loader = loader,
        suggestedEntry = "$loader-",
        declaredClientSide = DeclaredSupport.UNKNOWN,
        declaredServerSide = DeclaredSupport.REQUIRED,
        jarScan = JarScan.SERVER_OR_BOTH,
        bootResult = bootResult,
        bootedLoader = loader,
        bootCrashExcerpt = null,
        confidence = Confidence.HIGH,
        sampleFile = null,
        note = null
    )

    /**
     * **A crashed boot's console is copied out of staging, because staging is about to be reused.** The
     * attempt directory is wiped and re-created by the next re-grind of the same tuple, taking with it the
     * only evidence behind a HIGH verdict — most often a server loading a mod that reaches for a client-only
     * class, which is legible from the console and nowhere else.
     */
    @Test
    fun aCrashedBootsConsoleIsKeptOutsideStaging(@TempDir work: File) {
        val bootRoot = File(work, "boot")
        val crashLogs = CrashLogStore(File(work, "crash-logs"))
        stagedConsole(bootRoot, ModPlatforms.MODRINTH, "creativecore", "Fabric", "NoClassDefFoundError: net/minecraft/client/Minecraft")
        stagedConsole(bootRoot, ModPlatforms.MODRINTH, "creativecore", "NeoForge", "Done (21.5s)! For help")
        val report = clientsideReport(
            slug = "creativecore",
            perLoader = listOf(verdict("Fabric", BootResult.CRASHED), verdict("NeoForge", BootResult.SURVIVED))
        )

        ContainerCandidateVerifier.keepCrashConsoles(report, bootRoot, crashLogs)

        Assertions.assertEquals(
            listOf("Modrinth-creativecore-Fabric.log"),
            crashLogs.list(),
            "only the boot that crashed is worth keeping — a clean boot proves nothing and explains nothing"
        )
        Assertions.assertTrue(crashLogs.read("Modrinth-creativecore-Fabric.log")!!.contains("net/minecraft/client/Minecraft"))
    }

    /**
     * A crash whose console never reached disk (a runner that never started, an unwritable log) keeps nothing
     * and says nothing — collecting evidence must not fail a grind that already has its verdict.
     */
    @Test
    fun aCrashWithNoConsoleOnDiskKeepsNothingAndDoesNotThrow(@TempDir work: File) {
        val crashLogs = CrashLogStore(File(work, "crash-logs"))
        val report = clientsideReport(slug = "ghost", perLoader = listOf(verdict("Forge", BootResult.CRASHED)))

        ContainerCandidateVerifier.keepCrashConsoles(report, File(work, "boot"), crashLogs)

        Assertions.assertTrue(crashLogs.list().isEmpty())
    }

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
