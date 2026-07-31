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

/**
 * Pins that the loader cache knows *which templates* produced an install, not merely that one succeeded.
 *
 * The install boot runs the pack's own `start.sh`, so a cached tuple is a product of the start-script templates in
 * force at the time. When those templates change what an install *produces*, the cached layer is stale — and the
 * completion marker recorded only loader/version/Minecraft, so `ensureInstalled` served it regardless. That cost a
 * hand-invalidation of two Forge tuples during the launcher-era fix, with nothing warning that it was needed.
 *
 * Deliberately **not** retroactive: a marker written before provenance existed carries none, and is tolerated
 * rather than invalidated. Treating an absent field as a mismatch would re-install every cached tuple — 74 of them
 * at ~150 MB and a networked boot each — to answer a question about a change that may not affect them. Today's
 * Forge fix is the case in point: its cached tuples turned out to be perfectly bootable, because the argfile the
 * new launch path uses is what the installer had already produced.
 */
internal class TemplateProvenanceTest {

    /** An installer that must never run: these tests are about what the cache decides before installing. */
    private val neverRuns = LoaderInstaller { _, _, _, _ -> Assertions.fail("the cache must not install here") }

    /** Write a completion marker by hand, with or without provenance, standing in for an earlier install. */
    private fun markInstalled(cacheRoot: File, provenance: String?) {
        val baseDir = File(cacheRoot, "26.2/Forge/65.1.0").apply { mkdirs() }
        val recorded = buildString {
            append("loader=Forge\nloaderVersion=65.1.0\nminecraftVersion=26.2\n")
            provenance?.let { append("templates=$it\n") }
        }
        File(baseDir, LoaderCache.MARKER).writeText(recorded)
    }

    /** Same templates as produced it — the cached tuple is used, which is the whole point of the cache. */
    @Test
    fun anInstallFromTheSameTemplatesIsStillInstalled(@TempDir cacheRoot: File) {
        markInstalled(cacheRoot, "abc123")
        val cache = LoaderCache(cacheRoot, neverRuns, templateProvenance = { "abc123" })

        Assertions.assertTrue(cache.isInstalled("Forge", "65.1.0", "26.2"))
    }

    /** Different templates — the cached layer may no longer be what a boot needs, so it counts as a miss. */
    @Test
    fun anInstallFromDifferentTemplatesCountsAsAMiss(@TempDir cacheRoot: File) {
        markInstalled(cacheRoot, "abc123")
        val cache = LoaderCache(cacheRoot, neverRuns, templateProvenance = { "def456" })

        Assertions.assertFalse(
            cache.isInstalled("Forge", "65.1.0", "26.2"),
            "the templates that produced this install have changed; serving it is what required a hand-invalidation"
        )
    }

    /** A marker predating provenance is tolerated, not invalidated — see the class comment for why. */
    @Test
    fun aMarkerWithoutProvenanceIsTolerated(@TempDir cacheRoot: File) {
        markInstalled(cacheRoot, null)
        val cache = LoaderCache(cacheRoot, neverRuns, templateProvenance = { "abc123" })

        Assertions.assertTrue(
            cache.isInstalled("Forge", "65.1.0", "26.2"),
            "an unknown provenance must not re-install the whole cache; it is unknown, not known-different"
        )
    }

    /** With no provenance available at all, the cache behaves exactly as it did before this existed. */
    @Test
    fun withoutAProvenanceSupplierNothingChanges(@TempDir cacheRoot: File) {
        markInstalled(cacheRoot, "abc123")
        val cache = LoaderCache(cacheRoot, neverRuns)

        Assertions.assertTrue(cache.isInstalled("Forge", "65.1.0", "26.2"))
    }

    /** The digest must actually track content, or recording it proves nothing. */
    @Test
    fun theDigestChangesWithTemplateContent(@TempDir workDirectory: File) {
        val template = File(workDirectory, "default_template.sh").apply { writeText("echo original\n") }
        val other = File(workDirectory, "default_template.fish").apply { writeText("echo fish\n") }

        val before = TemplateProvenance.digestOf(listOf(template, other))
        Assertions.assertNotNull(before, "two readable templates must yield a digest")

        template.writeText("echo changed\n")
        val after = TemplateProvenance.digestOf(listOf(template, other))

        Assertions.assertNotEquals(before, after, "a changed template must change the digest")
        Assertions.assertEquals(
            after,
            TemplateProvenance.digestOf(listOf(other, template)),
            "the digest must not depend on the order files happen to be listed in"
        )
    }

    /** Missing templates yield no digest, so an unreadable home degrades to today's behaviour instead of thrashing. */
    @Test
    fun absentTemplatesYieldNoDigest(@TempDir workDirectory: File) {
        Assertions.assertNull(TemplateProvenance.digestOf(listOf(File(workDirectory, "absent.sh"))))
        Assertions.assertNull(TemplateProvenance.digestOf(emptyList()))
    }
}
