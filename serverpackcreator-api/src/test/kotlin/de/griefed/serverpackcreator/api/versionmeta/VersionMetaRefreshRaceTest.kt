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
package de.griefed.serverpackcreator.api.versionmeta

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Pins that reading version metadata while it refreshes is safe — no exception, and never a torn view.
 *
 * **This is a production defect, not a test-only race.** `VersionMeta` moved the manifest refresh onto a
 * background coroutine (`refreshScope.launch { refreshManifests() }`, `Dispatchers.IO`) to take ~392 ms off
 * startup. The structures that refresh mutates are read concurrently and are unsynchronised plain
 * collections: every `update()` in `versionmeta` does `clear()` and then re-`add()`s, and
 * `MinecraftMeta.serverReleases()` hands out **the live list**.
 *
 * A reader therefore gets one of two failures:
 *
 *  - a `ConcurrentModificationException`, if it iterates while the list is structurally modified
 *  - an **empty or partially-filled list**, if it reads in the window between `clear()` and the `add()`s
 *
 * The second is the dangerous one because it does not throw. `BootVerifier.bootableCombination()` builds its
 * release set from `serverReleases()` on every staging call, so an empty read makes every candidate fail the
 * gate and the boot is refused with *"No bootable file/Minecraft/loader combination for <loader>"* — a
 * verdict about the engine's own timing, wearing the shape of a statement about the mod. Both failures were
 * observed on 2026-09-04 in `BootVerifierSelectionTest`, one as the CME and one as exactly that message.
 *
 * The fix is a snapshot swap rather than a lock: `update()` builds new collections and publishes them to a
 * `@Volatile` reference, so a reader sees either the whole previous state or the whole next one.
 */
internal class VersionMetaRefreshRaceTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /**
     * **The deterministic pin, and the property the whole fix rests on.** `serverReleases()` must hand out a
     * snapshot, not the collection the refresh mutates. Before the fix it returns the live `ArrayList`, so a
     * caller can mutate the metadata's own state through it — and, far more often, simply observe it being
     * cleared and refilled underneath them.
     *
     * Deterministic on purpose. The concurrent failure is real and was reproduced with its own stack trace —
     * `VersionMeta$1.invokeSuspend → refreshManifests → MinecraftMeta.update` throwing
     * `ConcurrentModificationException` on the background coroutine — but a timing test that reproduces it
     * only sometimes is not a pin, and one whose assertions live on a different thread from the exception
     * passes for the wrong reason. This asserts the invariant that *makes* the concurrent case safe.
     */
    @Test
    fun theReleaseListHandedToCallersIsNotLiveState() {
        val releases = apiWrapper.versionMeta.minecraft.serverReleases()
        Assertions.assertTrue(releases.isNotEmpty(), "the fixture must start with a populated release list")

        @Suppress("UNCHECKED_CAST")
        val asMutable = releases as? MutableList<Any?>
        // Asserted, never skipped: a guarded assertion that quietly does nothing when the cast fails is the
        // defect class iteration 34 found. If an accessor ever stops presenting as MutableList, this must
        // fail and be re-read, not pass in silence.
        val mutableView = requireNotNull(asMutable) {
            "expected a List that presents as MutableList; the check below relies on it"
        }
        Assertions.assertThrows(
            UnsupportedOperationException::class.java,
            { mutableView.clear() },
            "callers were handed the metadata's own mutable list; a refresh clears and refills exactly this"
        )
    }

    /**
     * A bounded stress net over the real accessors while the Minecraft metas refresh. It cannot *prove*
     * safety — the failing thread may be the refresher's, as it was when this was investigated — but a torn
     * read is something the reader can see, and an empty release list is what makes
     * `BootVerifier.bootableCombination()` refuse a perfectly good candidate with "No bootable
     * file/Minecraft/loader combination".
     */
    @Test
    fun noReaderEverSeesAnEmptyOrTornReleaseList() {
        val meta = apiWrapper.versionMeta
        val expected = meta.minecraft.serverReleases().size
        Assertions.assertTrue(expected > 0)

        val failure = AtomicReference<Throwable?>(null)
        val shortReads = AtomicInteger(0)
        val start = CountDownLatch(1)
        val rounds = 300

        val reader = Thread {
            start.await()
            repeat(rounds) {
                try {
                    val seen = meta.minecraft.serverReleases().count { it.minecraftVersion.isNotBlank() }
                    if (seen < expected) {
                        shortReads.incrementAndGet()
                    }
                } catch (t: Throwable) {
                    failure.compareAndSet(null, t)
                }
            }
        }
        val refresher = Thread {
            start.await()
            repeat(rounds) { runCatching { meta.minecraft.update() } }
        }

        reader.start(); refresher.start(); start.countDown(); reader.join(); refresher.join()

        Assertions.assertNull(failure.get(), "reading during a refresh threw: ${failure.get()}")
        Assertions.assertEquals(
            0, shortReads.get(),
            "a reader saw a partially-filled release list; that is what refuses a good candidate"
        )
    }

    /**
     * **Every loader meta, not just Minecraft.** `ForgeLoader`, `NeoForgeLoader`, `FabricLoader`,
     * `FabricInstaller`, `QuiltLoader`, `QuiltInstaller`, `LegacyFabricInstaller` and
     * `LegacyFabricVersioning` all share the clear-then-refill shape, and `LoaderVersionResolver` reads them
     * on the same threads that read the Minecraft metas. A fix that covered only Minecraft would leave the
     * identical race behind a different accessor.
     */
    @TestFactory
    fun noMetaHandsOutLiveState(): List<DynamicTest> {
        val meta = apiWrapper.versionMeta
        val accessors = listOf<Pair<String, () -> List<*>>>(
            "minecraft.serverReleases" to { meta.minecraft.serverReleases() },
            "minecraft.clientReleases" to { meta.minecraft.clientReleases() },
            "minecraft.allVersions" to { meta.minecraft.allVersions() },
            // Listed so the set is "every list accessor on MinecraftMeta" rather than the three that
            // happened to be written down; an accessor added beside these inherits the guard.
            "minecraft.clientSnapshots" to { meta.minecraft.clientSnapshots() },
            "minecraft.serverSnapshots" to { meta.minecraft.serverSnapshots() },
            "forge.forgeVersions" to { meta.forge.forgeVersions() },
            "forge.supportedMinecraftVersions" to { meta.forge.supportedMinecraftVersions() },
            "neoForge.neoForgeVersions" to { meta.neoForge.neoForgeVersions() },
            "neoForge.supportedMinecraftVersions" to { meta.neoForge.supportedMinecraftVersions() },
            "fabric.loaderVersions" to { meta.fabric.loaderVersions() },
            "fabric.installerVersions" to { meta.fabric.installerVersions() },
            "quilt.loaderVersions" to { meta.quilt.loaderVersions() },
            "quilt.installerVersions" to { meta.quilt.installerVersions() },
            "legacyFabric.loaderVersions" to { meta.legacyFabric.loaderVersions() },
            "legacyFabric.installerVersions" to { meta.legacyFabric.installerVersions() },
            "legacyFabric.supportedMinecraftVersions" to { meta.legacyFabric.supportedMinecraftVersions() }
        )

        return accessors.map { (name, read) ->
            DynamicTest.dynamicTest(name) {
                val values = read()
                @Suppress("UNCHECKED_CAST")
                val asMutable = values as? MutableList<Any?>
                val mutableView = requireNotNull(asMutable) {
                    "$name: expected a List that presents as MutableList"
                }
                Assertions.assertThrows(
                    UnsupportedOperationException::class.java,
                    { mutableView.clear() },
                    "$name hands out the metadata's own mutable list; a refresh clears and refills it"
                )
            }
        }
    }
}
