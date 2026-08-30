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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.AttemptDirectory
import de.griefed.serverpackcreator.clientside.BootArtifacts
import de.griefed.serverpackcreator.grinder.ModPlatforms
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the durable home for what a boot leaves behind — the evidence a verdict cannot be re-derived
 * without.
 *
 * Staging is not that home. `BootWorkspaceReaper` keeps one `boot.log` per attempt directory, but staging
 * *wipes and re-creates* that directory on every stage, so the next attempt destroys the last one's console
 * and the next re-grind destroys the evidence for a verdict that is still published.
 *
 * Truncation and the never-read-a-huge-file-whole guarantee moved to `BootArtifacts` in `-clientside`,
 * where the reading now happens; they are pinned there.
 */
internal class BootLogStoreTest {

    @TempDir
    lateinit var directory: File

    private fun store(budgetBytes: Long = Long.MAX_VALUE) = BootLogStore(directory, budgetBytes)

    private fun artifacts(vararg names: String) =
        names.map { BootArtifacts.Artifact(it, "body of $it", truncated = false) }

    private fun attempt(loader: String = "Forge", loaderVersion: String = "47.2.0", minecraft: String = "1.20.1") =
        BootLogStore.attemptKey(loader, loaderVersion, minecraft)

    @Test
    fun keepsEveryArtifactOfAnAttemptUnderItsOwnTuple() {
        val kept = store().keep(
            AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge"),
            attempt(),
            artifacts(BootArtifacts.CONSOLE_NAME, "logs-latest.log", "crash-reports-crash.txt")
        )

        Assertions.assertEquals(3, kept.size)
        Assertions.assertEquals(
            setOf("body of ${BootArtifacts.CONSOLE_NAME}", "body of logs-latest.log", "body of crash-reports-crash.txt"),
            kept.map { store().read(it) }.toSet()
        )
        Assertions.assertEquals(
            kept.toSet(),
            store().namesFor(ModPlatforms.MODRINTH, "jei", "Forge").toSet(),
            "every kept name must be findable from the tuple the report has"
        )
    }

    /**
     * A slug contains `-` and so does a loader, so a stored name can never be *parsed* back apart. The
     * lookup rebuilds the prefix instead — which is also why the attempt separator has to be a character
     * neither part uses.
     */
    @Test
    fun aSlugContainingTheSeparatorIsStillFoundByItsTuple() {
        store().keep(AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "iron-chests", "NeoForge"), attempt(), artifacts("console.log"))
        store().keep(AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "iron", "NeoForge"), attempt(), artifacts("console.log"))

        Assertions.assertEquals(1, store().namesFor(ModPlatforms.MODRINTH, "iron-chests", "NeoForge").size)
        Assertions.assertEquals(1, store().namesFor(ModPlatforms.MODRINTH, "iron", "NeoForge").size)
    }

    /** Every attempt of one candidate is kept, because a re-check's boot is the evidence a crash is argued with. */
    @Test
    fun everyAttemptOfOneCandidateIsKeptSideBySide() {
        val owner = AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge")
        store().keep(owner, attempt(loaderVersion = "47.2.0"), artifacts("console.log"))
        store().keep(owner, attempt(loaderVersion = "47.3.11"), artifacts("console.log"))
        store().keep(owner, attempt(loader = "NeoForge", minecraft = "1.21.1"), artifacts("console.log"))

        Assertions.assertEquals(3, store().namesFor(ModPlatforms.MODRINTH, "jei", "Forge").size)
    }

    /** Deterministic naming: the same attempt ground again replaces itself rather than accumulating. */
    @Test
    fun reGrindingTheSameAttemptReplacesItsArtifacts() {
        val owner = AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge")
        store().keep(owner, attempt(), listOf(BootArtifacts.Artifact("console.log", "first", false)))
        val second = store().keep(owner, attempt(), listOf(BootArtifacts.Artifact("console.log", "second", false)))

        Assertions.assertEquals(1, store().namesFor(ModPlatforms.MODRINTH, "jei", "Forge").size)
        Assertions.assertEquals("second", store().read(second.single()))
    }

    /**
     * The load-bearing bound. Deterministic naming only replaces *this* grind's attempts; a re-grind whose
     * re-check samples a different loader or Minecraft line writes new names and would strand the previous
     * grind's files forever. Without this, the feature re-introduces the growth class the reaper exists for.
     */
    @Test
    fun pruningDropsAPreviousGrindsOrphansAndNothingElse() {
        val owner = AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge")
        val other = AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "sodium", "Fabric")
        store().keep(owner, attempt(loaderVersion = "47.2.0"), artifacts("console.log"))
        store().keep(other, attempt(), artifacts("console.log"))
        val current = store().keep(owner, attempt(loaderVersion = "47.3.11"), artifacts("console.log"))

        val removed = store().pruneExcept(ModPlatforms.MODRINTH, "jei", "Forge", current.toSet())

        Assertions.assertEquals(1, removed, "only the stale attempt of this tuple goes")
        Assertions.assertEquals(current.toSet(), store().namesFor(ModPlatforms.MODRINTH, "jei", "Forge").toSet())
        Assertions.assertEquals(1, store().namesFor(ModPlatforms.MODRINTH, "sodium", "Fabric").size, "another candidate is untouched")
    }

    /** The backstop: a fixed disk must survive an operator who grinds for months. Oldest goes first. */
    @Test
    fun theBudgetSweepDeletesOldestFirstUntilUnderTheCeiling() {
        val owner = AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge")
        val oldest = store().keep(owner, attempt(loaderVersion = "1"), listOf(BootArtifacts.Artifact("console.log", "x".repeat(400), false)))
        File(directory, oldest.single()).setLastModified(1_000L)
        val newest = store().keep(owner, attempt(loaderVersion = "2"), listOf(BootArtifacts.Artifact("console.log", "y".repeat(400), false)))
        File(directory, newest.single()).setLastModified(9_000_000L)

        val reclaimed = store(budgetBytes = 500).enforceBudget()

        Assertions.assertTrue(reclaimed > 0, "the sweep must report what it reclaimed")
        Assertions.assertNull(store().read(oldest.single()), "the oldest attempt goes first")
        Assertions.assertNotNull(store().read(newest.single()), "the newest is what an operator is most likely to want")
    }

    /** A budget with room to spare must not delete anything — a backstop that fires constantly is a bug. */
    @Test
    fun aStoreUnderItsBudgetIsLeftAlone() {
        val owner = AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge")
        val kept = store().keep(owner, attempt(), artifacts("console.log"))

        Assertions.assertEquals(0L, store(budgetBytes = 10 * 1024 * 1024).enforceBudget())
        Assertions.assertNotNull(store().read(kept.single()))
    }

    /**
     * The old store's logs are real evidence for verdicts still being published, and its directory name now
     * contradicts what it holds. Moving them once is cheaper than explaining two directories forever.
     */
    @Test
    fun adoptingTheLegacyCrashLogsMovesThemOnceAndIsIdempotent(@TempDir legacy: File) {
        File(legacy, "Modrinth-jei-Forge.log").writeText("an old crash console")

        Assertions.assertEquals(1, store().adoptLegacy(legacy))
        val adopted = store().namesFor(ModPlatforms.MODRINTH, "jei", "Forge").single()
        Assertions.assertEquals(
            "an old crash console", store().read(adopted),
            "an adopted console must be reachable from its row, not merely present on the index"
        )
        Assertions.assertTrue(adopted.contains(BootLogStore.LEGACY_ATTEMPT), "and must say it predates per-attempt keeping")
        Assertions.assertEquals(0, store().adoptLegacy(legacy), "a second run has nothing left to move")
        Assertions.assertFalse(legacy.exists(), "an emptied legacy directory is removed rather than left to confuse")
    }

    /** An absent legacy directory is the normal case on a fresh install, not a failure. */
    @Test
    fun adoptingAnAbsentLegacyDirectoryIsANoOp(@TempDir parent: File) {
        Assertions.assertEquals(0, store().adoptLegacy(File(parent, "never-existed")))
    }

    /** Keeping evidence must never fail a grind that already produced its verdict. */
    @Test
    fun anUnwritableStoreKeepsNothingInsteadOfThrowing(@TempDir parent: File) {
        val blocked = File(parent, "blocked").apply { writeText("I am a file, not a directory") }

        Assertions.assertTrue(
            BootLogStore(blocked, Long.MAX_VALUE)
                .keep(AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge"), attempt(), artifacts("console.log"))
                .isEmpty()
        )
    }

    /** The traversal guard is inherited, not re-derived — but it has to still hold after the rework. */
    @Test
    fun aNameThatEscapesTheStoreStillReadsNothing() {
        File(directory.parentFile, "secret.txt").writeText("not yours")

        Assertions.assertNull(store().read("../secret.txt"))
    }

    /** The same slug on two platforms is two projects and two sets of evidence. */
    @Test
    fun theSameSlugOnAnotherPlatformIsADifferentLog() {
        store().keep(AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "jei", "Forge"), attempt(), artifacts("console.log"))

        Assertions.assertEquals(1, store().namesFor(ModPlatforms.MODRINTH, "jei", "Forge").size)
        Assertions.assertTrue(
            store().namesFor(ModPlatforms.CURSEFORGE, "jei", "Forge").isEmpty(),
            "a CurseForge project must not inherit a Modrinth project's logs"
        )
    }

    /**
     * **Two candidates ground in parallel must not prune each other.** `GrindPool` runs N workers against
     * one verifier instance, so anything tracking "what this grind wrote" that is shared between them lets
     * one candidate's prune delete logs another had just written. That is the same cross-candidate class
     * that once had an unqualified attempt directory wiping a pack out from under a running container, and
     * it produced verdicts that disagreed with themselves for the identical build.
     */
    @Test
    fun candidatesPrunedInParallelDoNotDeleteEachOthersLogs() {
        val tuples = (1..8).map { Triple(ModPlatforms.MODRINTH, "mod$it", "Forge") }
        val written = tuples.associateWith { (platform, slug, loader) ->
            store().keep(AttemptDirectory.nameFor(platform, slug, loader), attempt(), artifacts("console.log", "logs-latest.log"))
        }

        tuples.map { (platform, slug, loader) ->
            Thread {
                store().pruneExcept(platform, slug, loader, written.getValue(Triple(platform, slug, loader)).toSet())
            }.apply { start() }
        }.forEach { it.join() }

        tuples.forEach { (platform, slug, loader) ->
            Assertions.assertEquals(
                2, store().namesFor(platform, slug, loader).size,
                "$slug lost logs to another candidate's prune"
            )
        }
    }

    /** Nothing kept means nothing listed — which is what stops the report linking a 404. */
    @Test
    fun anUnknownTupleHasNoLogs() {
        Assertions.assertTrue(store().namesFor(ModPlatforms.MODRINTH, "never-ground", "Forge").isEmpty())
    }
}
