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

/**
 * Pins that Fabric and Quilt can step down to an older loader build when the newest will not install.
 *
 * Reported live: *"Loader install for Quilt 0.31.0-beta.3 / Minecraft 1.20.6 failed, so the pack could not
 * be completed."* `CachedLoaderVersions.firstInstallableVersion` already handles this — it walks
 * `availableVersions` for the first build not on install cooldown — but the supplier returned
 * `emptyList()` for Fabric, Quilt and LegacyFabric, so there was nothing to walk and the tuple stayed dead
 * for every candidate that wanted it.
 *
 * **The premise behind that empty list was wrong.** It read: they "ship a single Minecraft-independent
 * loader line, so there is no sibling build to fall back to". True of *per-Minecraft* builds — Quilt does
 * not publish a 1.20.6-specific loader the way Forge does — but the loader **line** is versioned, and
 * measured against the live metadata on 2026-09-04 Quilt publishes **306** builds and Fabric **253**, with
 * Quilt's `/v3/versions/loader/1.20.6` listing all 306 as valid for that Minecraft. There are 305 siblings.
 *
 * **Prevention was ruled out first, and this is the remaining lever.** Every published source says the
 * failing combination is fine: it is in the per-Minecraft list, the intermediary exists, and
 * `.../loader/1.20.6/0.31.0-beta.3/server/json` answers 200. The start scripts' own checks — Fabric's
 * `server/json` and Quilt's intermediary array — are the same signal `LoaderVersionResolver` already gates
 * on, and Fabric's 400 tracks Minecraft support rather than the pairing (an ancient loader with the newest
 * Minecraft still answers 200). Nothing in metadata predicts an installer that fails to run, so recovery is
 * what is left.
 */
internal class FabricQuiltStepDownTest {

    @TempDir
    lateinit var tempDir: Path

    /** Stands in for `LoaderVersionResolver`: always answers with the real newest build. */
    private class FakeNewest(private val newest: String) : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = newest
        override fun latestVersion(loader: String, minecraftVersion: String) = newest
    }

    /**
     * A real [LoaderCache] driven through a real failure, so the cooldown under test is the production one
     * rather than a fake that merely agrees with it. Installing the named versions fails, which is what puts
     * each on cooldown.
     */
    private fun cacheRefusing(vararg failing: String): LoaderCache {
        val cache = LoaderCache(tempDir.toFile(), LoaderInstaller { target, _, version, _ ->
            if (version in failing) {
                false
            } else {
                File(target, "server.jar").writeText("jar")
                true
            }
        })
        failing.forEach { cache.ensureInstalled("Quilt", it, "1.20.6") }
        return cache
    }

    /** The Quilt line as the live manifest publishes it: ascending, newest last. */
    private val quiltAscending =
        listOf("0.30.1", "0.30.2-beta.1", "0.31.0-beta.1", "0.31.0-beta.2", "0.31.0-beta.3")

    /** Newest first, which is the order the step-down walks. */
    @Test
    fun theLoaderLineIsOfferedNewestFirst() {
        Assertions.assertEquals(
            listOf("0.31.0-beta.3", "0.31.0-beta.2", "0.31.0-beta.1", "0.30.2-beta.1", "0.30.1"),
            LoaderStepDown.newestFirst(quiltAscending),
            "the manifest is ascending; stepping down must start at the newest, not the oldest"
        )
    }

    /**
     * The reported case: the newest is refusing to install, so the next build down is used. Without a
     * non-empty line this returned the newest again and the tuple never recovered.
     */
    @Test
    fun aFailedNewestStepsDownToTheNextBuild() {
        val policy = CachedLoaderVersions(
            newest = FakeNewest("0.31.0-beta.3"),
            cache = cacheRefusing("0.31.0-beta.3"),
            availableVersions = { _, _ -> LoaderStepDown.newestFirst(quiltAscending) }
        )

        Assertions.assertEquals("0.31.0-beta.2", policy.preferredVersion("Quilt", "1.20.6"))
    }

    /** Repeated failures keep walking down rather than sticking on the second build. */
    @Test
    fun successiveFailuresKeepWalkingDown() {
        val policy = CachedLoaderVersions(
            newest = FakeNewest("0.31.0-beta.3"),
            cache = cacheRefusing("0.31.0-beta.3", "0.31.0-beta.2", "0.31.0-beta.1", "0.30.2-beta.1"),
            availableVersions = { _, _ -> LoaderStepDown.newestFirst(quiltAscending) }
        )

        Assertions.assertEquals(
            "0.30.1", policy.preferredVersion("Quilt", "1.20.6"),
            "four dead betas above the newest stable is exactly the shape that produced the report"
        )
    }

    /**
     * **`latestVersion` stays truthful.** The support gate and the crash re-check must still measure against
     * the real newest, or a crash on a stepped-down build would be cleared by re-checking the same build.
     */
    @Test
    fun steppingDownDoesNotChangeWhatTheNewestIs() {
        val policy = CachedLoaderVersions(
            newest = FakeNewest("0.31.0-beta.3"),
            cache = cacheRefusing("0.31.0-beta.3"),
            availableVersions = { _, _ -> LoaderStepDown.newestFirst(quiltAscending) }
        )

        Assertions.assertEquals("0.31.0-beta.3", policy.latestVersion("Quilt", "1.20.6"))
    }

    /** An empty line still behaves as before — no line, no step-down, no crash. */
    @Test
    fun aLoaderWithNoKnownLineIsUnchanged() {
        Assertions.assertEquals(emptyList<String>(), LoaderStepDown.newestFirst(emptyList()))
    }
}
