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
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins reclamation of the per-attempt scratch space. Staging keeps one full server pack (plus the mod jars it
 * downloaded) per `(slug, loader)` attempted, and only ever deleted it when that *same* pair was retried — so a
 * catalog sweep grew the work tree without bound. Measured live on 2026-07-30: **98 GB across 1750 attempt
 * directories**, ~23 GB/h, which fills any host long before a sweep finishes. The logs are the part worth keeping
 * (a verdict's detail is read from them); the packs are reproducible and must go.
 */
internal class BootWorkspaceReaperTest {

    @TempDir
    lateinit var work: File

    /** Build one attempt's staging exactly as `BootVerifier`/`ClientsideVerifier` lay it out. */
    private fun stageAttempt(slug: String, loader: String, packBytes: Int = 4096) {
        File(work, "boot/$slug-$loader/serverpack/libraries").apply { mkdirs() }
        File(work, "boot/$slug-$loader/serverpack/libraries/loader.jar").writeBytes(ByteArray(packBytes))
        File(work, "boot/$slug-$loader/modpack/mods").apply { mkdirs() }
        File(work, "boot/$slug-$loader/modpack/mods/mod.jar").writeBytes(ByteArray(packBytes))
        File(work, "boot/$slug-$loader/boot.log").writeText("[Server thread/INFO]: Done (4.2s)! For help")
        File(work, "verify/$slug-$loader").apply { mkdirs() }
        File(work, "verify/$slug-$loader/mod.jar").writeBytes(ByteArray(packBytes))
    }

    private fun reaper() = BootWorkspaceReaper(work)

    /** The whole point: the pack goes, the diagnosis stays. */
    @Test
    fun dropsThePackButKeepsTheBootLog() {
        stageAttempt("jei", "Forge")

        reaper().reap("jei")

        Assertions.assertFalse(File(work, "boot/jei-Forge/serverpack").exists(), "the staged pack must be gone")
        Assertions.assertFalse(File(work, "boot/jei-Forge/modpack").exists(), "the staged modpack must be gone")
        Assertions.assertTrue(File(work, "boot/jei-Forge/boot.log").isFile, "the boot log is the evidence — keep it")
        Assertions.assertTrue(
            File(work, "boot/jei-Forge/boot.log").readText().contains("Done (4.2s)"),
            "the kept log must be the original, not a truncated stand-in"
        )
    }

    /** The downloaded jars have no diagnostic value once the verdict is in, so that directory goes entirely. */
    @Test
    fun removesTheDownloadedJarScratchCompletely() {
        stageAttempt("jei", "Forge")

        reaper().reap("jei")

        Assertions.assertFalse(File(work, "verify/jei-Forge").exists(), "nothing worth keeping among downloaded jars")
    }

    /** Every loader of the finished candidate is reaped, not just the first one found. */
    @Test
    fun reapsAllLoadersOfTheSameCandidate() {
        listOf("Forge", "NeoForge", "Fabric", "Quilt").forEach { stageAttempt("jei", it) }

        reaper().reap("jei")

        listOf("Forge", "NeoForge", "Fabric", "Quilt").forEach { loader ->
            Assertions.assertFalse(File(work, "boot/jei-$loader/serverpack").exists(), "pack for $loader survived")
            Assertions.assertTrue(File(work, "boot/jei-$loader/boot.log").isFile, "log for $loader was lost")
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

        reaper().reap("jei")

        Assertions.assertTrue(
            File(work, "boot/sodium-Fabric/serverpack/libraries/loader.jar").isFile,
            "an in-flight boot of another candidate must be untouched"
        )
        Assertions.assertTrue(File(work, "verify/sodium-Fabric/mod.jar").isFile)
    }

    /**
     * Slugs nest (`jei` vs `jei-extras`), so matching must be exact per attempt directory rather than by prefix —
     * a prefix match would reap a different, possibly in-flight, project.
     */
    @Test
    fun aSlugThatPrefixesAnotherDoesNotReapIt() {
        stageAttempt("jei", "Forge")
        stageAttempt("jei-extras", "Forge")

        reaper().reap("jei")

        Assertions.assertFalse(File(work, "boot/jei-Forge/serverpack").exists(), "the named candidate is reaped")
        Assertions.assertTrue(
            File(work, "boot/jei-extras-Forge/serverpack/libraries/loader.jar").isFile,
            "a longer slug sharing the prefix must survive"
        )
    }

    /** Reports what it freed, so the daemon can log reclamation instead of the operator guessing. */
    @Test
    fun reportsTheBytesItReclaimed() {
        stageAttempt("jei", "Forge", packBytes = 1024)

        val reclaimed = reaper().reap("jei")

        // Two 1 KiB pack files plus the 1 KiB downloaded jar; the kept log is excluded.
        Assertions.assertEquals(3072, reclaimed)
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

        Assertions.assertFalse(File(work, "boot/jei-Forge/serverpack").exists())
        Assertions.assertFalse(File(work, "boot/sodium-Fabric/modpack").exists())
        Assertions.assertFalse(File(work, "verify/sodium-Fabric").exists())
        Assertions.assertTrue(File(work, "boot/jei-Forge/boot.log").isFile, "logs survive a startup sweep too")
        Assertions.assertTrue(reclaimed > 0, "a sweep that found packs must report freeing something")
    }

    /** Reaping is bookkeeping, never a failure path: nothing staged, nothing to do, no exception. */
    @Test
    fun anAbsentWorkTreeIsANoOp() {
        val reaper = BootWorkspaceReaper(File(work, "never-created"))

        Assertions.assertEquals(0, reaper.reap("jei"))
        Assertions.assertEquals(0, reaper.reapAll())
    }

    /** Reaping twice must be harmless — the second pass simply finds the log it is meant to keep. */
    @Test
    fun reapingAnAlreadyReapedCandidateIsHarmless() {
        stageAttempt("jei", "Forge")
        val reaper = reaper()

        reaper.reap("jei")

        Assertions.assertEquals(0, reaper.reap("jei"), "nothing left to free the second time")
        Assertions.assertTrue(File(work, "boot/jei-Forge/boot.log").isFile, "and the log still stands")
    }
}
