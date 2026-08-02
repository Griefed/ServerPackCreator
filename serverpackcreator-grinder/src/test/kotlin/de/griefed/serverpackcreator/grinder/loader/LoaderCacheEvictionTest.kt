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
package de.griefed.serverpackcreator.grinder.loader

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Pins the loader cache's time-based eviction. Without it the cache only ever grows: every tuple costs
 * ~165 MB and a months-long sweep keeps minting new ones as loaders ship builds, so an unattended grinder
 * eventually fills the disk. Eviction is keyed on **last use**, not install time — a tuple the sweep keeps
 * booting must never be deleted underneath it, however old its install is.
 */
internal class LoaderCacheEvictionTest {

    @TempDir
    lateinit var tempDir: Path

    private val installer = LoaderInstaller { target, _, _, _ ->
        File(target, "libraries").mkdirs()
        File(target, "server.jar").writeText("jar")
        true
    }

    /** Backdate a tuple's last-use stamp to [age] ago, as if it had not been booted since. */
    private fun backdate(cache: LoaderCache, loader: String, version: String, minecraft: String, age: Duration) {
        val marker = File(cache.baseDirFor(loader, version, minecraft), LoaderCache.MARKER)
        Assertions.assertTrue(marker.isFile, "expected an installed tuple to backdate")
        Assertions.assertTrue(marker.setLastModified(Instant.now().minus(age).toEpochMilli()))
    }

    @Test
    fun evictsTuplesNotUsedWithinTheRetentionWindow() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.ensureInstalled("Forge", "50.2.10", "1.20.6")
        cache.ensureInstalled("NeoForge", "26.2.0.40-beta", "26.2")
        backdate(cache, "Forge", "50.2.10", "1.20.6", Duration.ofDays(9))

        val evicted = cache.evictUnusedSince(Duration.ofDays(7))

        Assertions.assertEquals(1, evicted, "only the stale tuple is evicted")
        Assertions.assertFalse(cache.isInstalled("Forge", "50.2.10", "1.20.6"), "stale tuple gone")
        Assertions.assertTrue(cache.isInstalled("NeoForge", "26.2.0.40-beta", "26.2"), "fresh tuple kept")
    }

    /** A cache hit is a *use*: it refreshes the stamp, so a tuple in active service is never evicted. */
    @Test
    fun aCacheHitRefreshesTheLastUseStampSoActiveTuplesSurvive() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.ensureInstalled("Forge", "50.2.10", "1.20.6")
        backdate(cache, "Forge", "50.2.10", "1.20.6", Duration.ofDays(30))

        // Boot it again — this is what the grinder does on every candidate sharing the tuple.
        Assertions.assertNotNull(cache.ensureInstalled("Forge", "50.2.10", "1.20.6"), "must still be a hit")

        Assertions.assertEquals(0, cache.evictUnusedSince(Duration.ofDays(7)), "using it made it fresh again")
        Assertions.assertTrue(cache.isInstalled("Forge", "50.2.10", "1.20.6"))
    }

    /** Retention off (zero or negative) must keep everything — the previous behaviour, opt-in eviction. */
    @Test
    fun zeroRetentionDisablesEvictionEntirely() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.ensureInstalled("Forge", "50.2.10", "1.20.6")
        backdate(cache, "Forge", "50.2.10", "1.20.6", Duration.ofDays(400))

        Assertions.assertEquals(0, cache.evictUnusedSince(Duration.ZERO))
        Assertions.assertEquals(0, cache.evictUnusedSince(Duration.ofDays(-1)))
        Assertions.assertTrue(cache.isInstalled("Forge", "50.2.10", "1.20.6"), "retention off ⇒ nothing is deleted")
    }

    /**
     * A half-installed tuple (no marker — a crash mid-install) is swept too, whatever its age: it can never
     * be served, and leaving it behind would leak the disk it occupies.
     */
    @Test
    fun anUnmarkedPartialInstallIsSweptRegardlessOfAge() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        val orphan = cache.baseDirFor("Quilt", "0.29.1", "1.21.1").apply { mkdirs() }
        File(orphan, "libraries").mkdirs()

        val evicted = cache.evictUnusedSince(Duration.ofDays(7))

        Assertions.assertEquals(1, evicted)
        Assertions.assertFalse(orphan.exists(), "an install that never completed is not worth keeping")
    }

    /** Eviction must not trip over an empty or absent cache root. */
    @Test
    fun evictionOnAnEmptyCacheIsANoOp() {
        val absent = LoaderCache(File(tempDir.toFile(), "not-created-yet"), installer)

        Assertions.assertEquals(0, absent.evictUnusedSince(Duration.ofDays(7)))
    }

    /**
     * Eviction takes the same per-tuple lock as installing, so it cannot delete a tree while a worker is
     * installing into it — the failure that would otherwise hand a half-deleted base to a booting container.
     */
    @Test
    fun evictionDoesNotRaceAnInstallOfTheSameTuple() {
        val started = java.util.concurrent.CountDownLatch(1)
        val evictionAttempted = java.util.concurrent.CountDownLatch(1)
        val slowInstaller = LoaderInstaller { target, _, _, _ ->
            started.countDown()
            // Hold the tuple's lock while eviction runs on another thread.
            Assertions.assertTrue(evictionAttempted.await(5, java.util.concurrent.TimeUnit.SECONDS))
            File(target, "server.jar").writeText("jar")
            true
        }
        val cache = LoaderCache(tempDir.toFile(), slowInstaller)
        val installing = Thread { cache.ensureInstalled("Forge", "50.2.10", "1.20.6") }.apply { start() }
        Assertions.assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS))

        val evictor = Thread { cache.evictUnusedSince(Duration.ofDays(7)); }.apply { start() }
        evictionAttempted.countDown()
        installing.join(10_000)
        evictor.join(10_000)

        Assertions.assertTrue(
            cache.isInstalled("Forge", "50.2.10", "1.20.6"),
            "the freshly installed tuple must survive an eviction pass that ran alongside it"
        )
    }
}
