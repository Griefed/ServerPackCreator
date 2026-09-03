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

import de.griefed.serverpackcreator.grinder.loader.LoaderCache
import de.griefed.serverpackcreator.grinder.loader.LoaderInstaller
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * **"It was not retried" must only be said about an install that was, in fact, not retried.**
 *
 * `ContainerCandidateVerifier` has two messages for an unavailable install — one for a failure, one for a
 * suppressed retry — and the distinction was undone by the order the question was asked in: it read
 * `isInstallOnCooldown` *after* `ensureInstalled`, which records the cooldown on its way out of a failure. So
 * the tuple that had just been attempted answered "on cooldown" too, and every candidate was told the install
 * "was not retried" no matter which had happened. The failure branch was unreachable in production.
 *
 * That distinction is exactly what an operator needs during an outage: how many of the thousands of identical
 * verdicts are attempts failing right now, and how many are cheap skips behind them. This asks the state
 * *before* the attempt, which is the only moment the two differ.
 */
internal class LoaderInstallAttemptDistinctionTest {

    @TempDir
    lateinit var tempDir: Path

    private var now = Instant.parse("2026-09-03T21:00:00Z")
    private val attempts = AtomicInteger(0)

    /** Stands in for the outage: the daemon has no runtime image, so every install throws. */
    private val brokenInstaller = LoaderInstaller { _, _, _, _ ->
        attempts.incrementAndGet()
        throw IllegalStateException("Status 404: {\"message\":\"No such image: spc-grinder-runtime:latest\"}")
    }

    private fun cache() = LoaderCache(
        tempDir.toFile(), brokenInstaller, failureCooldown = Duration.ofHours(1), clock = { now }
    )

    @Test
    fun theCandidateThatPaidForTheFailedInstallIsNotToldItWasSkipped() {
        val cache = cache()

        val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
            ContainerCandidateVerifier.installedBase(cache, "NeoForge", "26.2.0.26-beta", "26.2")
        }

        Assertions.assertEquals(1, attempts.get(), "this candidate's boot did attempt the install")
        val message = thrown.message ?: Assertions.fail("the refusal must carry a message; it becomes the verdict")
        Assertions.assertTrue(
            message.contains("failed", ignoreCase = true),
            "the install was attempted and failed, here, now. Was: $message"
        )
        Assertions.assertFalse(
            message.contains("not retried"),
            "it *was* tried — saying otherwise is what made an outage's real attempts indistinguishable from " +
                "the skips behind them. Was: $message"
        )
    }

    @Test
    fun theCandidatesBehindItAreToldTheRetryWasSuppressed() {
        val cache = cache()

        runCatching { ContainerCandidateVerifier.installedBase(cache, "NeoForge", "26.2.0.26-beta", "26.2") }
        now = now.plusSeconds(60)
        val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
            ContainerCandidateVerifier.installedBase(cache, "NeoForge", "26.2.0.26-beta", "26.2")
        }

        Assertions.assertEquals(1, attempts.get(), "the cooldown is what makes the second candidate cheap")
        val message = thrown.message ?: Assertions.fail("the refusal must carry a message")
        Assertions.assertTrue(
            message.contains("cooldown"),
            "a suppressed retry must say so; nothing else logs above DEBUG. Was: $message"
        )
    }

    /** The normal case still hands back the installed base, or the overlay has nothing to copy. */
    @Test
    fun aSuccessfulInstallReturnsItsBaseDirectory() {
        val cache = LoaderCache(tempDir.toFile(), { target, _, _, _ ->
            java.io.File(target, "libraries").mkdirs()
            true
        }, failureCooldown = Duration.ofHours(1), clock = { now })

        val base = ContainerCandidateVerifier.installedBase(cache, "Forge", "52.1.16", "1.21.1")

        Assertions.assertTrue(java.io.File(base, "libraries").isDirectory, "the install layer must be reachable")
    }
}
