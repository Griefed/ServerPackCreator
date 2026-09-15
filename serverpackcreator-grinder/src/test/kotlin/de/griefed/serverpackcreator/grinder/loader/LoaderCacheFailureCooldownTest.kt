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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the failed-install cooldown. Observed live on 2026-07-30: NeoForge `21.1.247` is listed in NeoForge's
 * version metadata but its `-installer.jar` 404s, so the install could never succeed — and because
 * `1.21.1 + NeoForge` is one of the most common combinations in the catalogue, *every* candidate wanting it paid
 * a full download-and-boot (~46 s of container time) before failing, all sweep long, silently turning decisive
 * boots into INCONCLUSIVE.
 *
 * A tuple that just failed is therefore left alone for a cooldown: candidates fail fast, and the reason is
 * logged once rather than once per candidate.
 */
internal class LoaderCacheFailureCooldownTest {

    @TempDir
    lateinit var tempDir: Path

    private var now = Instant.parse("2026-07-30T12:00:00Z")
    private val attempts = AtomicInteger(0)

    /** An installer that always fails, counting how often it was actually invoked. */
    private val brokenInstaller = LoaderInstaller { _, _, _, _ ->
        attempts.incrementAndGet()
        false
    }

    private fun cache(installer: LoaderInstaller, cooldown: Duration = Duration.ofHours(1)) =
        LoaderCache(tempDir.toFile(), installer, failureCooldown = cooldown, clock = { now })

    @Test
    fun aFailedTupleIsNotReAttemptedDuringTheCooldown() {
        val cache = cache(brokenInstaller)

        Assertions.assertNull(cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1"))
        now = now.plusSeconds(600)
        Assertions.assertNull(cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1"), "still unavailable")
        Assertions.assertNull(cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1"))

        Assertions.assertEquals(1, attempts.get(), "one install attempt, not one per candidate")
    }

    @Test
    fun theTupleIsRetriedOnceTheCooldownHasPassed() {
        val cache = cache(brokenInstaller, cooldown = Duration.ofMinutes(30))

        cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1")
        now = now.plus(Duration.ofMinutes(31))
        cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1")

        Assertions.assertEquals(2, attempts.get(), "upstream may have been fixed — try again after the cooldown")
    }

    /** A cooldown on one tuple must not hold back any other. */
    @Test
    fun onlyTheFailingTupleIsOnCooldown() {
        val working = LoaderInstaller { target, _, _, _ ->
            attempts.incrementAndGet()
            File(target, "libraries").mkdirs()
            true
        }
        val cache = LoaderCache(tempDir.toFile(), { target, loader, version, minecraft ->
            if (loader == "NeoForge") brokenInstaller.install(target, loader, version, minecraft)
            else working.install(target, loader, version, minecraft)
        }, failureCooldown = Duration.ofHours(1), clock = { now })

        Assertions.assertNull(cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1"))
        Assertions.assertNotNull(cache.ensureInstalled("Forge", "52.1.16", "1.21.1"), "a healthy tuple is unaffected")
        Assertions.assertNull(cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1"), "still on cooldown")
    }

    /** A tuple that recovers must not stay marked — the cooldown is a memory of failure, not a blacklist. */
    @Test
    fun aSuccessfulInstallClearsThePreviousFailure() {
        var healthy = false
        val flaky = LoaderInstaller { target, _, _, _ ->
            attempts.incrementAndGet()
            if (!healthy) return@LoaderInstaller false
            File(target, "libraries").mkdirs()
            true
        }
        val cache = cache(flaky, cooldown = Duration.ofMinutes(10))

        Assertions.assertNull(cache.ensureInstalled("Fabric", "0.19.3", "1.21.1"))
        healthy = true
        now = now.plus(Duration.ofMinutes(11))
        Assertions.assertNotNull(cache.ensureInstalled("Fabric", "0.19.3", "1.21.1"))
        // Now installed: further calls are hits, and no failure memory lingers.
        Assertions.assertNotNull(cache.ensureInstalled("Fabric", "0.19.3", "1.21.1"))
        Assertions.assertEquals(2, attempts.get(), "the successful install is cached; nothing re-attempted")
    }

    /** Zero cooldown keeps the old behaviour for anyone who wants every candidate to retry. */
    @Test
    fun aZeroCooldownRetriesEveryTime() {
        val cache = cache(brokenInstaller, cooldown = Duration.ZERO)

        cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1")
        cache.ensureInstalled("NeoForge", "21.1.247", "1.21.1")

        Assertions.assertEquals(2, attempts.get())
    }
}
