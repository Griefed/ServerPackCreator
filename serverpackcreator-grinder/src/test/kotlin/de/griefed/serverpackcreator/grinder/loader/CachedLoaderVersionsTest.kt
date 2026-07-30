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

import de.griefed.serverpackcreator.clientside.LoaderVersionPolicy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Pins the install-cache-preferring loader-version policy. Loaders ship builds constantly, and booting the
 * *newest* every time means a fresh ~150 MB networked install for a build that almost always behaves
 * identically to the one already cached. This policy reuses what is installed instead — bounded by two
 * properties that must not slip:
 *
 * 1. it must never weaken the **support gate** (`latestVersion` always delegates, so a cached build cannot
 *    smuggle an unsupported loader/Minecraft combination back into selection), and
 * 2. `BootVerifier` re-checks any crash on a non-newest build against the newest one, which is what stops a
 *    reused build turning "needs a newer loader" into a false HIGH-confidence clientside verdict.
 */
internal class CachedLoaderVersionsTest {

    @TempDir
    lateinit var tempDir: Path

    /** A policy that always answers with the newest build, standing in for `LoaderVersionResolver`. */
    private class FakeNewest(private val newest: Map<Pair<String, String>, String>) : LoaderVersionPolicy {
        var preferredCalls = 0
        override fun preferredVersion(loader: String, minecraftVersion: String): String? {
            preferredCalls++
            return newest[loader to minecraftVersion]
        }
        override fun latestVersion(loader: String, minecraftVersion: String): String? = newest[loader to minecraftVersion]
    }

    private val installer = LoaderInstaller { target, _, _, _ ->
        File(target, "server.jar").writeText("jar")
        true
    }

    @Test
    fun prefersAnAlreadyInstalledBuildOverTheNewestOne() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.ensureInstalled("Forge", "52.1.16", "1.21.1")
        val newest = FakeNewest(mapOf(("Forge" to "1.21.1") to "52.1.20"))

        val policy = CachedLoaderVersions(newest, cache)

        Assertions.assertEquals("52.1.16", policy.preferredVersion("Forge", "1.21.1"), "reuse the cached install")
        Assertions.assertEquals("52.1.20", policy.latestVersion("Forge", "1.21.1"), "the newest is still reported truthfully")
    }

    @Test
    fun fallsBackToTheNewestWhenNothingIsCachedForThatPair() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.ensureInstalled("Forge", "52.1.16", "1.21.1") // a different pair
        val newest = FakeNewest(mapOf(("Fabric" to "1.21.1") to "0.16.9"))

        val policy = CachedLoaderVersions(newest, cache)

        Assertions.assertEquals("0.16.9", policy.preferredVersion("Fabric", "1.21.1"))
    }

    /**
     * The gate must stay the delegate's business. A cached build for a Minecraft the loader no longer supports
     * must not resurrect that combination — `BootVerifier` gates selection on `latestVersion`, so answering
     * `null` there is what keeps the combo dropped.
     */
    @Test
    fun aCachedBuildCannotReviveAnUnsupportedCombination() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.ensureInstalled("Quilt", "0.26.0", "1.16.1")
        val newest = FakeNewest(emptyMap()) // the loader supports nothing any more

        val policy = CachedLoaderVersions(newest, cache)

        Assertions.assertNull(policy.latestVersion("Quilt", "1.16.1"), "support is the delegate's answer, not the cache's")
    }

    /** With several builds cached for one pair, the most recently used one wins — it stays warm under eviction. */
    @Test
    fun picksTheMostRecentlyUsedCachedBuild() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.ensureInstalled("Forge", "52.1.16", "1.21.1")
        cache.ensureInstalled("Forge", "52.1.20", "1.21.1")
        // Make the older build the more recently *used* one.
        File(cache.baseDirFor("Forge", "52.1.20", "1.21.1"), LoaderCache.MARKER)
            .setLastModified(Instant.now().minus(Duration.ofDays(3)).toEpochMilli())
        val policy = CachedLoaderVersions(FakeNewest(mapOf(("Forge" to "1.21.1") to "52.1.30")), cache)

        Assertions.assertEquals("52.1.16", policy.preferredVersion("Forge", "1.21.1"))
    }

    /** A half-installed tuple is not a usable cache entry, so it must not be offered as a preference. */
    @Test
    fun ignoresTuplesWhoseInstallNeverCompleted()

    {
        val cache = LoaderCache(tempDir.toFile(), installer)
        cache.baseDirFor("Forge", "52.1.16", "1.21.1").mkdirs() // no completion marker
        val newest = FakeNewest(mapOf(("Forge" to "1.21.1") to "52.1.20"))

        Assertions.assertEquals("52.1.20", CachedLoaderVersions(newest, cache).preferredVersion("Forge", "1.21.1"))
    }

    /**
     * The raw loader-version is recovered from the completion marker rather than the directory name, because
     * the directory name is sanitized — anything unusual in a version string would otherwise come back mangled
     * and be handed to pack generation as a version that does not exist.
     */
    @Test
    fun recoversTheRawVersionStringNotTheSanitizedDirectoryName() {
        val cache = LoaderCache(tempDir.toFile(), installer)
        val awkward = "1.21.1+build 7"
        cache.ensureInstalled("Fabric", awkward, "1.21.1")

        Assertions.assertNotEquals(
            awkward, cache.baseDirFor("Fabric", awkward, "1.21.1").name,
            "precondition: this version must actually be sanitized on disk"
        )
        Assertions.assertEquals(listOf(awkward), cache.installedVersions("Fabric", "1.21.1"))
    }

    @Test
    fun installedVersionsIsEmptyForAnUntouchedCache() {
        val cache = LoaderCache(File(tempDir.toFile(), "nothing-here"), installer)

        Assertions.assertTrue(cache.installedVersions("Forge", "1.21.1").isEmpty())
    }
}
