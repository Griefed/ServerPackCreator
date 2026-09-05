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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the **filename pattern**: the second, narrower entry derived from the one file actually sampled,
 * which keeps the loader token the historical stem loses.
 *
 * `iris` is the reported case, and it shows why one column was not enough. Measured against the live API:
 *
 * | loader   | files | published stem   |
 * |----------|-------|------------------|
 * | Fabric   | 191   | `iris-`          |
 * | NeoForge | 42    | `iris-neoforge-` |
 * | Quilt    | 143   | `iris-`          |
 *
 * The existing `suggestedEntry` is the longest common prefix over a project's **whole history**, which is
 * right for a `startsWith` fallback list — it must match every build ever published. But iris's oldest
 * Fabric files are `iris-mc1.16.5-1.0.0.jar`, from before the loader went into the name, so the prefix
 * collapses to `iris-`. NeoForge kept `iris-neoforge-` only because it has no such history: all 42 of its
 * files carry the loader.
 *
 * The filename pattern is derived from the **sampled file alone**, so it keeps whatever that file is called:
 * `iris-fabric-`, `iris-neoforge-`. And because Quilt boots Fabric builds, a Quilt row shows `iris-fabric-`
 * — the loader stays as the file spells it, not as the row is labelled.
 */
internal class FilenamePatternTest {

    /** iris' Fabric build, verbatim from the platform. */
    @Test
    fun theLoaderIsKeptWhenTheFileNameCarriesIt() {
        Assertions.assertEquals(
            "iris-fabric-",
            FilenameStemDeriver.deriveStem(listOf("iris-fabric-1.11.3+mc26.1.2.jar"))
        )
        Assertions.assertEquals(
            "iris-neoforge-",
            FilenameStemDeriver.deriveStem(listOf("iris-neoforge-1.11.3+mc26.1.2.jar"))
        )
    }

    /**
     * **The Quilt case the request calls out.** Quilt boots the Fabric build, so the sampled file is a
     * Fabric one and its pattern says so. The row's loader is Quilt; the file's loader is fabric; the
     * pattern follows the file.
     */
    @Test
    fun aQuiltRowKeepsFabricWhenItBootsAFabricBuild() {
        Assertions.assertEquals(
            "iris-fabric-",
            FilenameStemDeriver.deriveStem(listOf("iris-fabric-1.10.6+mc1.21.11.jar")),
            "the pattern describes the file, not the loader the row is labelled with"
        )
    }

    /** A file with no loader token yields no loader — nothing is invented. */
    @Test
    fun aFileWithoutALoaderTokenKeepsWhatItHas() {
        Assertions.assertEquals(
            "iris-",
            FilenameStemDeriver.deriveStem(listOf("iris-mc1.16.5-1.0.0.jar")),
            "no loader token in the name, so none in the pattern -- and `mc` is stripped as the Minecraft " +
                "marker it is, which is why this file's own pattern is already the broad one"
        )
    }

    /**
     * **The two columns are deliberately different, and this is the pin that says so.** The historical stem
     * stays broad enough to match every build — that is what the published fallback list needs — while the
     * filename pattern is narrow enough to tell a maintainer which artifact was actually looked at.
     */
    @Test
    fun theHistoricalStemStaysBroaderThanTheFilenamePattern() {
        val wholeHistory = listOf(
            "iris-fabric-1.11.3+mc26.1.2.jar",
            "iris-fabric-1.10.6+mc1.21.11.jar",
            "iris-mc1.16.5-1.0.0.jar"
        )

        Assertions.assertEquals("iris-", FilenameStemDeriver.deriveStem(wholeHistory))
        Assertions.assertEquals(
            "iris-fabric-",
            FilenameStemDeriver.deriveStem(listOf(wholeHistory.first())),
            "sampling one file is what preserves the loader; the history is what loses it"
        )
    }

    /** No sample, no pattern — the column is empty rather than guessed at. */
    @Test
    fun noSampledFileYieldsNoPattern() {
        Assertions.assertNull(FilenameStemDeriver.deriveStem(emptyList()))
    }
}
