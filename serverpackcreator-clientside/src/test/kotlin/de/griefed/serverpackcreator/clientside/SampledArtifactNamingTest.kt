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

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins how the **one sampled artifact** is named in a verdict — both halves, because they are different
 * things and were briefly conflated.
 *
 * `LoaderVerdict.sampleFile` carries that artifact's published name **verbatim**, extension and all: it is
 * what a maintainer types into a platform's search box. It was a *derived stem* of that file until
 * 2026-09-10, under the name `filenamePattern`, and Griefed's report was exactly that — the column "most
 * often equals some sort of pattern" instead of the filename it was meant to be.
 *
 * `FilenameStemDeriver.deriveStem` over a **single** file is the other half, and it still matters: it is
 * what `Prepared.Ready.candidateStem` is built from, which is how blame attribution tells the candidate's
 * stack frames from a dependency's. So the guards below keep their teeth — what changed is which consumer
 * they speak for.
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
 * A single-file stem keeps whatever that file is called: `iris-fabric-`, `iris-neoforge-`. And because
 * Quilt boots Fabric builds, a Quilt row's sample is a Fabric build — the loader stays as the file spells
 * it, not as the row is labelled.
 *
 * @author Griefed
 */
internal class SampledArtifactNamingTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /**
     * **The producer, which nothing asserted.** Every existing guard for this column either *injects* the
     * value into a grinder fixture or pins the mapping one layer downstream — the arrangement
     * `DependencySlugTest` exists to warn about, since a test constructing the value under test cannot see a
     * producer constructing it wrongly. And the regression it cannot see is the code that was just removed:
     * re-deriving a stem from the sampled file.
     *
     * The fixture name is a real one, from `hybrid-aquatic`: spaces, brackets and a version, all of which a
     * stem deriver strips. Asserted beside `suggestedEntry` from the **same** run, so the two fields are
     * shown to be different things rather than described as different.
     */
    @Test
    fun theSampledFileIsTheArtifactsOwnNameVerbatim(@TempDir workDir: File) {
        val published = "[1.20.1-Forge] Hybrid Aquatic 1.6.9.jar"
        val platform = object : ModPlatform {
            override val name: String = "Modrinth"
            override fun handles(projectUrl: String): Boolean = true
            override fun resolve(projectUrl: String): ProjectFiles = ProjectFiles(
                platform = name, slug = "hybrid-aquatic", projectUrl = projectUrl,
                clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
                files = listOf(
                    ModFile(published, setOf("Forge"), setOf("1.20.1"), "https://cdn/ha.jar", null, emptyList()),
                    ModFile(
                        "[1.20.1-Forge] Hybrid Aquatic 1.6.8.jar", setOf("Forge"), setOf("1.20.1"),
                        "https://cdn/ha168.jar", null, emptyList()
                    )
                )
            )

            override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
        }

        val verdict = ClientsideVerifier(
            platforms = listOf(platform),
            metadataScanner = MetadataScanner(apiWrapper.modScanner),
            // No download, so the jar scan degrades and only the naming is under test.
            jarDownloader = JarDownloader { _, _ -> null },
            workDirectory = workDir
        ).report("https://modrinth.com/mod/hybrid-aquatic").perLoader.single()

        Assertions.assertEquals(
            published, verdict.sampleFile,
            "the column has to be the name as downloaded -- brackets, spaces, extension and all"
        )
        Assertions.assertEquals(
            "[1.20.1-Forge] Hybrid Aquatic ", verdict.suggestedEntry,
            "while the published entry is the whole history's stem, version token dropped -- a different " +
                "thing, and one that would match nothing typed into a search box"
        )
    }

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
     * **The whole history's stem is broader than one file's, and that is the point.** The published entry
     * has to match every build ever released; a single file's stem keeps the loader token, which is what
     * makes `candidateStem` able to separate the candidate's frames from a dependency's in a crash.
     */
    @Test
    fun theHistoricalStemStaysBroaderThanASingleFilesStem() {
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

    /** No file, no stem — nothing is invented for a project that published nothing. */
    @Test
    fun noSampledFileYieldsNoStem() {
        Assertions.assertNull(FilenameStemDeriver.deriveStem(emptyList()))
    }
}
