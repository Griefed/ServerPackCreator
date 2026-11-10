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

import de.griefed.serverpackcreator.clientside.boot.AttemptDirectory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins reclamation of the per-attempt scratch space. Staging keeps one full server pack (plus the mod jars it
 * downloaded) per `(slug, loader)` attempted, and deleting it only when that *same* pair is retried grows the work
 * tree without bound across a catalog sweep — measured live at **98 GB across 1750 attempt directories**,
 * ~23 GB/h, which fills any host long before a sweep finishes.
 *
 * Reclaiming all of it, the attempt directory included, is what [leavesNoAttemptDirectoryBehind] exists for.
 * Keeping the console here as well as in the [de.griefed.serverpackcreator.grinder.report.BootLogStore] left
 * one directory per attempt standing for ever, because a directory holding a kept file is never empty:
 * **255,591 of them, 511,183 inodes and 2.3 GB, measured on the live daemon on 2026-11-09**, 82% of whose
 * logs were zero bytes. Startup sweeps that tree before the report can bind, which is what grew
 * time-to-bind from 1.1 s to 81 minutes over six weeks. The consoles worth keeping are copied into the
 * budget-bounded log store *during* the boots, so there is nothing here left to lose.
 */
internal class BootWorkspaceReaperTest {

    @TempDir
    lateinit var work: File

    /**
     * The staging-directory name of one attempt, built through the same [AttemptDirectory] helper production
     * uses, so the fixture cannot drift away from the layout under test.
     */
    private fun attempt(slug: String, loader: String, platform: String = ModPlatforms.MODRINTH) =
        AttemptDirectory.nameFor(platform, slug, loader, "1.20")

    /** Build one attempt's staging exactly as `BootVerifier`/`ClientsideVerifier` lay it out. */
    private fun stageAttempt(
        slug: String,
        loader: String,
        packBytes: Int = 4096,
        platform: String = ModPlatforms.MODRINTH
    ) {
        val dir = attempt(slug, loader, platform)
        File(work, "boot/$dir/serverpack/libraries").apply { mkdirs() }
        File(work, "boot/$dir/serverpack/libraries/loader.jar").writeBytes(ByteArray(packBytes))
        File(work, "boot/$dir/modpack/mods").apply { mkdirs() }
        File(work, "boot/$dir/modpack/mods/mod.jar").writeBytes(ByteArray(packBytes))
        File(work, "boot/$dir/boot.log").writeText("[Server thread/INFO]: Done (4.2s)! For help")
        File(work, "verify/$dir").apply { mkdirs() }
        File(work, "verify/$dir/mod.jar").writeBytes(ByteArray(packBytes))
    }

    private fun reaper() = BootWorkspaceReaper(work)

    /**
     * An entry that cannot be fully removed contributes NOTHING to the reported total, even though some of its
     * files may well have gone. The figure is "bytes this sweep is known to have freed", so a partial delete is
     * counted as zero rather than guessed at, and the entry is left for the next sweep to retry.
     *
     * Pinned because the accounting is the subtle half of [BootWorkspaceReaper]: sizing and deleting are two
     * traversals of the same tree, and any change that fuses them has to keep the all-or-nothing rule or it
     * starts reporting bytes for packs that are still on disk.
     *
     * The failure is manufactured by clearing write permission on a directory, which root ignores — so the
     * precondition is *asserted* after the sweep, and the test skips rather than passes where it did not hold.
     */
    @Test
    fun anEntryThatCannotBeFullyDeletedContributesNothingToTheTotal() {
        stageAttempt("jei", "Forge", packBytes = 1024)
        val libraries = File(work, "boot/${attempt("jei", "Forge")}/serverpack/libraries")
        val pinnedJar = File(libraries, "loader.jar")
        Assertions.assertTrue(libraries.setWritable(false), "could not clear write permission on the fixture")
        try {
            val reclaimed = reaper().reap(ModPlatforms.MODRINTH, "jei")

            Assumptions.assumeTrue(
                pinnedJar.isFile,
                "the fixture's unwritable directory was deleted anyway (running as root?), so there is no " +
                    "partial failure to characterise"
            )
            Assertions.assertEquals(
                2048L + BOOT_LOG_BYTES, reclaimed,
                "only the modpack, the console and the downloaded-jar scratch were fully removed; the " +
                    "serverpack must not be counted"
            )
            Assertions.assertTrue(
                File(work, "boot/${attempt("jei", "Forge")}/serverpack").exists(),
                "what could not be reclaimed must stay, for the next sweep to retry"
            )
        } finally {
            libraries.setWritable(true)
        }
    }

    /** The whole point: once a candidate's verdicts are in, its staging goes — all of it. */
    @Test
    fun dropsTheWholeAttempt() {
        stageAttempt("jei", "Forge")

        reaper().reap(ModPlatforms.MODRINTH, "jei")

        Assertions.assertFalse(File(work, "boot/${attempt("jei", "Forge")}/serverpack").exists(), "the staged pack must be gone")
        Assertions.assertFalse(File(work, "boot/${attempt("jei", "Forge")}/modpack").exists(), "the staged modpack must be gone")
        Assertions.assertFalse(File(work, "boot/${attempt("jei", "Forge")}/boot.log").exists(), "the console is kept in the log store, not here")
    }

    /**
     * **The accumulation guard.** A reaped attempt must leave no directory standing, because one surviving
     * inode per attempt is unbounded growth: a catalog sweep never retries the same `(platform, slug, loader,
     * line)`, so nothing else ever removes it. Asserted on the staging *root* rather than on the attempt's own
     * path, since that is the tree startup has to walk before the report can bind.
     */
    @Test
    fun leavesNoAttemptDirectoryBehind() {
        stageAttempt("jei", "Forge")
        stageAttempt("sodium", "Fabric")

        reaper().reapAll()

        Assertions.assertEquals(
            emptyList<String>(),
            File(work, "boot").listFiles().orEmpty().map { it.name }.sorted(),
            "every attempt directory must be gone, not merely emptied"
        )
        Assertions.assertEquals(
            emptyList<String>(),
            File(work, "verify").listFiles().orEmpty().map { it.name }.sorted(),
            "the downloaded-jar scratch too"
        )
    }

    /** The downloaded jars have no diagnostic value once the verdict is in, so that directory goes entirely. */
    @Test
    fun removesTheDownloadedJarScratchCompletely() {
        stageAttempt("jei", "Forge")

        reaper().reap(ModPlatforms.MODRINTH, "jei")

        Assertions.assertFalse(File(work, "verify/${attempt("jei", "Forge")}").exists(), "nothing worth keeping among downloaded jars")
    }

    /** Every loader of the finished candidate is reaped, not just the first one found. */
    @Test
    fun reapsAllLoadersOfTheSameCandidate() {
        listOf("Forge", "NeoForge", "Fabric", "Quilt").forEach { stageAttempt("jei", it) }

        reaper().reap(ModPlatforms.MODRINTH, "jei")

        listOf("Forge", "NeoForge", "Fabric", "Quilt").forEach { loader ->
            Assertions.assertFalse(File(work, "boot/${attempt("jei", loader)}/serverpack").exists(), "pack for $loader survived")
            Assertions.assertFalse(File(work, "boot/${attempt("jei", loader)}").exists(), "attempt directory for $loader survived")
        }
    }

    /**
     * Workers run in parallel, so reaping one finished candidate must not touch a pack another worker is
     * currently booting — that would delete the server out from under a running container.
     */
    @Test
    fun leavesOtherCandidatesAlone() {
        stageAttempt("jei", "Forge")
        stageAttempt("sodium", "Fabric")

        reaper().reap(ModPlatforms.MODRINTH, "jei")

        Assertions.assertTrue(
            File(work, "boot/${attempt("sodium", "Fabric")}/serverpack/libraries/loader.jar").isFile,
            "an in-flight boot of another candidate must be untouched"
        )
        Assertions.assertTrue(File(work, "verify/${attempt("sodium", "Fabric")}/mod.jar").isFile)
    }

    /**
     * Slugs nest (`jei` vs `jei-extras`), so matching must be exact per attempt directory rather than by prefix —
     * a prefix match would reap a different, possibly in-flight, project.
     */
    @Test
    fun aSlugThatPrefixesAnotherDoesNotReapIt() {
        stageAttempt("jei", "Forge")
        stageAttempt("jei-extras", "Forge")

        reaper().reap(ModPlatforms.MODRINTH, "jei")

        Assertions.assertFalse(File(work, "boot/${attempt("jei", "Forge")}/serverpack").exists(), "the named candidate is reaped")
        Assertions.assertTrue(
            File(work, "boot/${attempt("jei-extras", "Forge")}/serverpack/libraries/loader.jar").isFile,
            "a longer slug sharing the prefix must survive"
        )
    }

    /**
     * **The same slug on two platforms is two candidates, and the grinder runs them in parallel.** Verdict
     * freshness is already keyed on `(platform, slug)` for exactly that reason, so reaping one platform's
     * finished candidate must leave the other platform's in-flight staging completely alone.
     *
     * Observed on `creativecore`, whose two platform runs finished 71 seconds apart: NeoForge 26.2.0.66 on
     * Minecraft 26.2 SURVIVED for one and CRASHED (exit 1) for the other — same loader build, same Minecraft,
     * same mod — a Fabric boot exited **127** (a shell that could not find the command it was
     * given), and two re-checks came back INCONCLUSIVE on files another run had booted to a ready-line.
     */
    @Test
    fun reapingOnePlatformLeavesTheSameSlugOnAnotherPlatformAlone() {
        stageAttempt("creativecore", "Fabric", platform = ModPlatforms.MODRINTH)
        stageAttempt("creativecore", "Fabric", platform = ModPlatforms.CURSEFORGE)

        reaper().reap(ModPlatforms.MODRINTH, "creativecore")

        val stillBooting = attempt("creativecore", "Fabric", ModPlatforms.CURSEFORGE)
        Assertions.assertTrue(
            File(work, "boot/$stillBooting/serverpack/libraries/loader.jar").isFile,
            "the other platform's candidate was still booting from this pack"
        )
        Assertions.assertTrue(File(work, "verify/$stillBooting/mod.jar").isFile)
        Assertions.assertFalse(
            File(work, "boot/${attempt("creativecore", "Fabric", ModPlatforms.MODRINTH)}/serverpack").exists(),
            "the finished candidate is still reaped"
        )
    }

    /** Reports what it freed, so the daemon can log reclamation instead of the operator guessing. */
    @Test
    fun reportsTheBytesItReclaimed() {
        stageAttempt("jei", "Forge", packBytes = 1024)

        val reclaimed = reaper().reap(ModPlatforms.MODRINTH, "jei")

        // Two 1 KiB pack files, the 1 KiB downloaded jar, and the 43-byte console that is no longer exempt.
        Assertions.assertEquals(3072L + BOOT_LOG_BYTES, reclaimed)
    }

    /**
     * A run killed mid-boot leaves staging behind that no `reap(slug)` will ever be called for, so startup has to
     * sweep whatever it inherits — otherwise every crash leaks a pack permanently.
     */
    @Test
    fun reapAllSweepsLeftoversFromAPreviousRun() {
        stageAttempt("jei", "Forge")
        stageAttempt("sodium", "Fabric")

        val reclaimed = reaper().reapAll()

        Assertions.assertFalse(File(work, "boot/${attempt("jei", "Forge")}/serverpack").exists())
        Assertions.assertFalse(File(work, "boot/${attempt("sodium", "Fabric")}/modpack").exists())
        Assertions.assertFalse(File(work, "verify/${attempt("sodium", "Fabric")}").exists())
        Assertions.assertFalse(File(work, "boot/${attempt("jei", "Forge")}").exists(), "a startup sweep leaves nothing standing either")
        Assertions.assertTrue(reclaimed > 0, "a sweep that found packs must report freeing something")
    }

    /** Reaping is bookkeeping, never a failure path: nothing staged, nothing to do, no exception. */
    @Test
    fun anAbsentWorkTreeIsANoOp() {
        val reaper = BootWorkspaceReaper(File(work, "never-created"))

        Assertions.assertEquals(0, reaper.reap(ModPlatforms.MODRINTH, "jei"))
        Assertions.assertEquals(0, reaper.reapAll())
    }

    /** Reaping twice must be harmless — the second pass simply finds nothing left to do. */
    @Test
    fun reapingAnAlreadyReapedCandidateIsHarmless() {
        stageAttempt("jei", "Forge")
        val reaper = reaper()

        reaper.reap(ModPlatforms.MODRINTH, "jei")

        Assertions.assertEquals(0, reaper.reap(ModPlatforms.MODRINTH, "jei"), "nothing left to free the second time")
        Assertions.assertFalse(File(work, "boot/${attempt("jei", "Forge")}").exists(), "and nothing was resurrected")
    }

    private companion object {
        /** Length of the console [stageAttempt] writes, so the reclaimed-bytes pin states its own arithmetic. */
        const val BOOT_LOG_BYTES = 43
    }
}
