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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the template-provenance check **on the path production actually takes**.
 *
 * [LoaderCache.isInstalled] compares a cached install's recorded template digest against the current one, and
 * [TemplateProvenanceTest] proves it does. Nothing in `src/main` ever called it: `ensureInstalled` decides a
 * cache hit through `markUsed`, which only asks whether the completion marker exists. So the digest was
 * *written* on install and never read back, and a start-script template change kept being served from a stale
 * layer — the exact failure the mechanism was built to prevent, and one the module's own documentation
 * described as fixed.
 *
 * **Why this file exists beside [TemplateProvenanceTest] rather than replacing it.** That one asserts the
 * decision; this one asserts that the decision is reachable. A unit test of a predicate cannot see a caller
 * that never consults it, which is the same boundary that let a dependency-label fix pass its tests and change
 * nothing in production. The evidence here is the **installer call count** — the only observable that
 * distinguishes "served from cache" from "installed again", and the thing a marker check cannot fake.
 *
 * Template changes fail *silently* — a stale layer boots and produces a plausible verdict rather than an
 * error — which is why this is pinned rather than trusted.
 */
internal class ProvenanceReachesEnsureInstalledTest {

    /** Counts installs and writes just enough for the cache to consider the tuple complete. */
    private class CountingInstaller : LoaderInstaller {
        val installs = AtomicInteger(0)

        override fun install(target: File, loader: String, loaderVersion: String, minecraftVersion: String): Boolean {
            installs.incrementAndGet()
            File(target, "installed.txt").writeText("payload")
            return true
        }
    }

    /** Install once under [provenance], returning the cache and its installer. */
    private fun installedUnder(cacheRoot: File, provenance: String?): Pair<CountingInstaller, LoaderCache> {
        val installer = CountingInstaller()
        val cache = LoaderCache(cacheRoot, installer, templateProvenance = { provenance })
        Assertions.assertNotNull(cache.ensureInstalled("Forge", "65.1.0", "26.2"), "the first install must succeed")
        Assertions.assertEquals(1, installer.installs.get())
        return installer to cache
    }

    /** Unchanged templates: the cache is a cache, and must not re-install. */
    @Test
    fun anInstallFromTheSameTemplatesIsServedFromCache(@TempDir cacheRoot: File) {
        installedUnder(cacheRoot, "abc123")

        val second = CountingInstaller()
        val reopened = LoaderCache(cacheRoot, second, templateProvenance = { "abc123" })

        Assertions.assertNotNull(reopened.ensureInstalled("Forge", "65.1.0", "26.2"))
        Assertions.assertEquals(0, second.installs.get(), "an unchanged tuple must be served, not rebuilt")
    }

    /** **The defect.** Changed templates must re-install rather than serve a layer built by the old ones. */
    @Test
    fun anInstallFromDifferentTemplatesIsRebuilt(@TempDir cacheRoot: File) {
        installedUnder(cacheRoot, "abc123")

        val second = CountingInstaller()
        val reopened = LoaderCache(cacheRoot, second, templateProvenance = { "def456" })

        Assertions.assertNotNull(reopened.ensureInstalled("Forge", "65.1.0", "26.2"))
        Assertions.assertEquals(
            1, second.installs.get(),
            "the templates that produced this layer changed; serving it boots a pack built by templates that are gone"
        )
    }

    /** The rebuilt layer records the *new* digest, or the next run rebuilds it again forever. */
    @Test
    fun theRebuiltInstallRecordsTheCurrentProvenance(@TempDir cacheRoot: File) {
        installedUnder(cacheRoot, "abc123")
        LoaderCache(cacheRoot, CountingInstaller(), templateProvenance = { "def456" })
            .ensureInstalled("Forge", "65.1.0", "26.2")

        val third = CountingInstaller()
        LoaderCache(cacheRoot, third, templateProvenance = { "def456" }).ensureInstalled("Forge", "65.1.0", "26.2")

        Assertions.assertEquals(0, third.installs.get(), "the rebuild must record its own provenance")
    }

    /**
     * Not retroactive: a marker written before provenance existed is tolerated. Treating unknown as different
     * would rebuild every cached tuple — ~150 MB and a networked boot each — to answer a question that may not
     * apply to it.
     */
    @Test
    fun aLayerWithoutRecordedProvenanceIsStillServed(@TempDir cacheRoot: File) {
        installedUnder(cacheRoot, null)

        val second = CountingInstaller()
        val reopened = LoaderCache(cacheRoot, second, templateProvenance = { "abc123" })

        Assertions.assertNotNull(reopened.ensureInstalled("Forge", "65.1.0", "26.2"))
        Assertions.assertEquals(0, second.installs.get(), "unknown provenance is unknown, not known-different")
    }

    /** With no provenance supplier at all, behaviour is exactly what it was before any of this existed. */
    @Test
    fun withoutAProvenanceSupplierNothingIsRebuilt(@TempDir cacheRoot: File) {
        installedUnder(cacheRoot, null)

        val second = CountingInstaller()
        val reopened = LoaderCache(cacheRoot, second)

        Assertions.assertNotNull(reopened.ensureInstalled("Forge", "65.1.0", "26.2"))
        Assertions.assertEquals(0, second.installs.get())
    }
}
