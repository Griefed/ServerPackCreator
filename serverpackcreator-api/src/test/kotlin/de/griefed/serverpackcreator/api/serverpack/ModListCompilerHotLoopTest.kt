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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.modscanning.ModJarScanner
import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.modscanning.ScannedMod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Guards the cost of [ModListCompiler.compileModList]'s exclusion loop, which runs once per mod and
 * once more per clientside-list entry for each of them.
 *
 * The default clientside-list ships ~550 entries (`GenerationConfig`), so a 300-mod pack performs on
 * the order of 165,000 comparisons per generation. What each comparison must *not* do is re-consult
 * configuration: `ApiProperties.exclusionFilter`'s getter calls `PropertyStore.acquire`, which reads
 * `java.util.Properties` — a synchronized `Hashtable` — twice. That is two synchronized map lookups
 * per comparison, on the **default** `START` path, for a value that cannot change mid-generation.
 *
 * Pinned by read-count rather than wall-clock: the count is the defect, and it is deterministic.
 */
internal class ModListCompilerHotLoopTest {

    /** Three mod jars, so the exclusion loop has something to iterate. Contents are irrelevant. */
    private fun modsDirectoryWith(tempDir: File, vararg names: String): String {
        val mods = File(tempDir, "mods").apply { mkdirs() }
        names.forEach { File(mods, it).writeText("not a real jar") }
        return mods.absolutePath
    }

    /**
     * An [ApiProperties] reporting [filter] and auto-exclusion off, so only the user-specified
     * exclusion logic runs.
     */
    private fun properties(filter: ExclusionFilter): ApiProperties {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.exclusionFilter } returns filter
        every { apiProperties.isAutoExcludingModsEnabled } returns false
        return apiProperties
    }

    /** A scanner reporting every jar handed to it with the defaults, i.e. no sideness signal. */
    private fun passthroughScanner(): ModScanner {
        val scanner = mockk<ModScanner>()
        val jarScanner = mockk<ModJarScanner>()
        every { jarScanner.scan(any()) } answers {
            firstArg<Collection<File>>().map { ScannedMod(it) }
        }
        every { scanner.scannerFor(any(), any()) } returns jarScanner
        return scanner
    }

    /**
     * Pins that the exclusion-filter setting is consulted **once per generation**, not once per
     * comparison.
     *
     * With three mods and two non-matching clientside entries every entry is compared against every
     * mod, so the old code read the property six times over plus once for its log line.
     */
    @Test
    fun theExclusionFilterIsReadOncePerGeneration(@TempDir tempDir: File) {
        val apiProperties = properties(ExclusionFilter.START)
        val compiler = ModListCompiler(apiProperties, passthroughScanner())
        val modsDir = modsDirectoryWith(tempDir, "alpha.jar", "beta.jar", "gamma.jar")

        val (included, excluded) = compiler.compileModList(
            modsDir,
            listOf("no-such-mod-", "another-non-match-"),
            emptyList(),
            "1.20.1",
            "Forge"
        )

        Assertions.assertEquals(3, included.size, "precondition: nothing matched, so every mod is kept")
        Assertions.assertTrue(excluded.isEmpty())
        verify(exactly = 1) { apiProperties.exclusionFilter }
    }

    /**
     * Pins that a malformed regex entry no longer takes the whole mod-list down with it.
     *
     * Under `REGEX`/`EITHER` each entry was compiled per comparison with `entry.toRegex()`, so a
     * single bad pattern threw `PatternSyntaxException` straight out of `compileModList` — aborting
     * generation rather than reporting one unusable entry. The other entries must still apply.
     */
    @Test
    fun aMalformedRegexEntryIsSkippedRatherThanAbortingTheList(@TempDir tempDir: File) {
        val apiProperties = properties(ExclusionFilter.REGEX)
        val compiler = ModListCompiler(apiProperties, passthroughScanner())
        val modsDir = modsDirectoryWith(tempDir, "keepme.jar", "dropme.jar")

        val (included, excluded) = compiler.compileModList(
            modsDir,
            listOf("^dropme.*$", "*[unclosed("),
            emptyList(),
            "1.20.1",
            "Forge"
        )

        Assertions.assertEquals(listOf("keepme.jar"), included.map { it.name }, "The valid entries must still apply")
        Assertions.assertEquals(listOf("dropme.jar"), excluded.map { it.name })
    }

    /**
     * Pins that a regex entry is compiled once per generation rather than once per comparison.
     *
     * Observed through behaviour rather than a counter: with more mods than entries, a correct
     * implementation still classifies every mod correctly from a single compilation of each pattern.
     * The read-count guard above is what actually holds the per-comparison work down; this one keeps
     * the REGEX path honest while it is being restructured.
     */
    @Test
    fun regexEntriesStillClassifyEveryMod(@TempDir tempDir: File) {
        val apiProperties = properties(ExclusionFilter.REGEX)
        val compiler = ModListCompiler(apiProperties, passthroughScanner())
        val modsDir = modsDirectoryWith(
            tempDir, "clientthing.jar", "clientstuff.jar", "serverthing.jar", "serverstuff.jar"
        )

        val (included, excluded) = compiler.compileModList(
            modsDir,
            listOf("^client.*$"),
            emptyList(),
            "1.20.1",
            "Forge"
        )

        Assertions.assertEquals(listOf("serverstuff.jar", "serverthing.jar"), included.map { it.name }.sorted())
        Assertions.assertEquals(listOf("clientstuff.jar", "clientthing.jar"), excluded.map { it.name }.sorted())
        verify(exactly = 1) { apiProperties.exclusionFilter }
    }
}
