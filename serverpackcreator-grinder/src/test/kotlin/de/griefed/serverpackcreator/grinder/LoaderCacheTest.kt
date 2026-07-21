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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the pre-bake cache contract with a fake [LoaderInstaller] (no network/containers): a miss
 * installs exactly once and marks the tuple, a subsequent hit skips the installer, a failed/throwing
 * install yields `null` and leaves nothing installed, and concurrent requests for one tuple still
 * install only once (so parallel workers don't re-install the same loader).
 */
internal class LoaderCacheTest {

    /** Counts invocations and writes a sentinel file so a "successful" install is observable. */
    private class CountingInstaller(
        private val succeed: Boolean = true,
        private val throwIt: Boolean = false,
        private val delayMillis: Long = 0
    ) : LoaderInstaller {
        val calls = AtomicInteger(0)
        override fun install(target: File, loader: String, loaderVersion: String, minecraftVersion: String): Boolean {
            calls.incrementAndGet()
            if (delayMillis > 0) Thread.sleep(delayMillis)
            if (throwIt) throw IllegalStateException("boom")
            if (succeed) File(target, "server.jar").writeText("installed")
            return succeed
        }
    }

    @Test
    fun missInstallsOnceThenHitsSkipTheInstaller(@TempDir root: File) {
        val installer = CountingInstaller()
        val cache = LoaderCache(root, installer)

        val first = cache.ensureInstalled("Forge", "47.2.0", "1.20.1")
        Assertions.assertNotNull(first)
        Assertions.assertTrue(File(first, "server.jar").isFile, "installer must have populated the base")
        Assertions.assertTrue(cache.isInstalled("Forge", "47.2.0", "1.20.1"))

        val second = cache.ensureInstalled("Forge", "47.2.0", "1.20.1")
        Assertions.assertEquals(first, second)
        Assertions.assertEquals(1, installer.calls.get(), "a cache hit must not re-run the installer")
    }

    @Test
    fun failedInstallReturnsNullAndLeavesNothingInstalled(@TempDir root: File) {
        val cache = LoaderCache(root, CountingInstaller(succeed = false))

        Assertions.assertNull(cache.ensureInstalled("Fabric", "0.15.11", "1.21"))
        Assertions.assertFalse(cache.isInstalled("Fabric", "0.15.11", "1.21"))
        Assertions.assertFalse(cache.baseDirFor("Fabric", "0.15.11", "1.21").exists(), "partial tree must be cleaned")
    }

    @Test
    fun throwingInstallIsTreatedAsFailure(@TempDir root: File) {
        val cache = LoaderCache(root, CountingInstaller(throwIt = true))

        Assertions.assertNull(cache.ensureInstalled("Quilt", "0.26.0", "1.21"))
        Assertions.assertFalse(cache.isInstalled("Quilt", "0.26.0", "1.21"))
    }

    @Test
    fun concurrentRequestsForOneTupleInstallOnce(@TempDir root: File) {
        val installer = CountingInstaller(delayMillis = 75)
        val cache = LoaderCache(root, installer)
        val threadCount = 6
        val ready = CountDownLatch(threadCount)
        val go = CountDownLatch(1)
        val results = java.util.Collections.synchronizedList(mutableListOf<File?>())

        val threads = (1..threadCount).map {
            Thread {
                ready.countDown()
                go.await()
                results.add(cache.ensureInstalled("NeoForge", "21.1.0", "1.21.1"))
            }.apply { start() }
        }
        ready.await(5, TimeUnit.SECONDS)
        go.countDown()
        threads.forEach { it.join(5_000) }

        Assertions.assertEquals(1, installer.calls.get(), "parallel workers must share a single install")
        Assertions.assertTrue(results.all { it != null && it == results.first() }, "all callers get the same base dir")
    }

    @Test
    fun distinctTuplesAreCachedIndependently(@TempDir root: File) {
        val installer = CountingInstaller()
        val cache = LoaderCache(root, installer)

        cache.ensureInstalled("Forge", "47.2.0", "1.20.1")
        cache.ensureInstalled("Fabric", "0.15.11", "1.20.1")

        Assertions.assertEquals(2, installer.calls.get())
        Assertions.assertNotEquals(
            cache.baseDirFor("Forge", "47.2.0", "1.20.1"),
            cache.baseDirFor("Fabric", "0.15.11", "1.20.1")
        )
    }
}
