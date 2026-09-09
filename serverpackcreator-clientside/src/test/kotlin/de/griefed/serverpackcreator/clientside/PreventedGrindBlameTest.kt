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
 * Pins **who a prevented grind is blamed on**, which is the one thing [Verdict.ERROR] promises and was not
 * keeping.
 *
 * Its own contract reads *"An operator's problem, never evidence about the mod"* — and it was carrying three
 * unrelated things: the host being broken, CurseForge withholding a download, and a loader/Minecraft
 * combination nothing upstream ever published for. Only the first is an operator's problem. The other two
 * are permanent, actionable by nobody, and mixed into the one bucket an operator is expected to read and
 * fix, which is what makes the bucket unreadable.
 *
 * **Measured on `grinder.serverpackcreator.de`, 2026-09-09**, over its 53 `ERROR` rows:
 *
 * | Cause | Rows |
 * |---|---|
 * | The mod's own file is distribution-locked (`corail-tombstone`, `entityculling`, `not-enough-animations`, `skin-layers-3d`, `structory`) | 15 |
 * | A required dependency is distribution-locked (`better-combat-by-daedelus`) | 2 |
 * | Upstream published nothing for the loader and Minecraft being booted | ~14 |
 * | The jar carries only another loader's descriptor — the author mis-ticked the web form | 4 |
 * | Genuinely ours | the rest |
 *
 * These guards deliberately assert what a prevented grind is **not**, because that is the whole claim they
 * could make before the verdicts that replace it existed — and it stays the claim worth guarding afterwards:
 * whatever the vocabulary grows into, a CurseForge opt-out must never be filed as ServerPackCreator's
 * failure. `PreventionCauseVerdictTest` is where the positive answers live.
 *
 * **Nothing here constructs the cause it then asserts on.** Two guards drive the real
 * `BootVerifier.prepareBootPack` — a locked candidate file and a pack that will not generate — and two
 * drive the real `refuseForMissingDependencies`, which is where an `UnmetReason` set becomes one cause. A
 * fixture that passed the cause in would assert only that `when` branches on its argument.
 *
 * @author Griefed
 */
internal class PreventedGrindBlameTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Fabric-capable server release from the cached metadata, so the test stays version-agnostic. */
    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Selection passes and generation does not, so staging is exercised and no server is ever launched. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    /** A project of one Fabric file, obtainable unless [locked] says otherwise. */
    private fun candidate(slug: String, fileName: String, locked: Boolean = false) = ProjectFiles(
        "CurseForge", slug, "https://www.curseforge.com/minecraft/mc-mods/$slug",
        DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN,
        listOf(
            ModFile(
                fileName, setOf("Fabric"), setOf(fabricRelease),
                if (locked) null else "https://cdn/$fileName", null, emptyList()
            )
        )
    )

    /** Writes a real jar for anything with a URL, and nothing at all for a locked file. */
    private val downloader = JarDownloader { file, targetDirectory ->
        if (file.locked) {
            return@JarDownloader null
        }
        targetDirectory.mkdirs()
        File(targetDirectory, file.fileName).also { it.writeText("not really a jar") }
    }

    private fun verifierFor(workDir: File) = BootVerifier(
        apiWrapper = apiWrapper,
        platform = object : ModPlatform {
            override val name: String = "CurseForge"
            override fun handles(projectUrl: String): Boolean = true
            override fun resolve(projectUrl: String): ProjectFiles = error("not used")
            override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
        },
        httpDownloader = downloader,
        loaderVersionPolicy = unbootableLoaderVersion,
        workDirectory = workDir
    )

    /** The published verdict for a staging refusal, folded exactly as `verdictFor` folds it in production. */
    private fun verdictFor(failed: BootVerifier.Prepared.Failed, jarScan: JarScan = JarScan.ERROR) =
        ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNKNOWN,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = jarScan,
            bootOutcome = BootVerifier.BootOutcome(
                BootResult.INCONCLUSIVE, null, failed.detail, prevention = failed.cause
            ),
            bootAttempted = true
        ).verdict

    /** The refusal `prepareBootPack` produces for [project], which must be one. */
    private fun refusalFor(project: ProjectFiles, workDir: File): BootVerifier.Prepared.Failed {
        val prepared = verifierFor(workDir).prepareBootPack(project, "Fabric")
        Assertions.assertTrue(prepared is BootVerifier.Prepared.Failed, "staging was supposed to refuse: $prepared")
        return prepared as BootVerifier.Prepared.Failed
    }

    /**
     * `corail-tombstone`, `entityculling`, `not-enough-animations`, `skin-layers-3d` and `structory`: the
     * author set `allowModDistribution=false`, so CurseForge publishes no download URL and there is nothing
     * to fetch. Nothing about that is ours, and no operator can fix it.
     */
    @Test
    fun aDistributionLockedFileIsNotOurFailure(@TempDir workDir: File) {
        val locked = candidate("corail-tombstone", "tombstone-forge-26.2-9.9.3.jar", locked = true)

        val refusal = refusalFor(locked, workDir)

        Assertions.assertTrue(
            refusal.detail.contains("distribution-locked"), "the refusal has to say why: ${refusal.detail}"
        )
        Assertions.assertNotEquals(
            Verdict.ERROR,
            verdictFor(refusal, jarScan = JarScan.DEFERRED),
            "a CurseForge opt-out is CurseForge's decision, not a defect in ServerPackCreator"
        )
    }

    /**
     * `better-combat-by-daedelus` on Fabric and NeoForge: the mod itself is obtainable and
     * `player-animation-library` is not, so the pack can never be assembled. Same cause, one level down.
     */
    @Test
    fun aDistributionLockedDependencyIsNotOurFailureEither() {
        val refusal = BootVerifier.refuseForMissingDependencies(
            mapOf("player-animation-library" to UnmetReason.DISTRIBUTION_LOCKED),
            "Fabric", "26.2", "CurseForge"
        )

        Assertions.assertNotEquals(
            Verdict.ERROR,
            verdictFor(refusal!!),
            "the pack cannot be assembled and nobody involved can change that"
        )
    }

    /**
     * `cobblemon-additions` on Fabric 1.21.11 needs `cobblemon`, whose newest Fabric build is 1.21.1;
     * `shatterbyte-lib` on Quilt 1.21.1 needs QSL, whose last release is Minecraft 1.21 and which is
     * discontinued. Neither is a grind that failed — it is a grind that was never possible.
     */
    @Test
    fun anUpstreamGapIsNotOurFailure() {
        val refusal = BootVerifier.refuseForMissingDependencies(
            mapOf("cobblemon" to UnmetReason.NO_USABLE_FILE),
            "Fabric", "1.21.11", "Modrinth"
        )

        Assertions.assertNotEquals(
            Verdict.ERROR,
            verdictFor(refusal!!),
            "a dependency nobody ever published is not a host problem and not evidence about the mod"
        )
    }

    /**
     * And the host's own trouble stays exactly where it was, or the split has achieved nothing: `ERROR` has
     * to keep meaning "somebody can go and fix this".
     */
    @Test
    fun theHostsOwnTroubleIsStillAnError(@TempDir workDir: File) {
        val obtainable = candidate("some-mod", "some-mod-1.0.0.jar")

        val refusal = refusalFor(obtainable, workDir)

        Assertions.assertTrue(
            refusal.detail.contains("generation failed"), "generation was supposed to fail: ${refusal.detail}"
        )
        Assertions.assertEquals(
            Verdict.ERROR,
            verdictFor(refusal),
            "generation is ours, and an operator reads this column to find out what broke"
        )
        Assertions.assertEquals(
            Verdict.ERROR,
            verdictFor(
                BootVerifier.refuseForMissingDependencies(
                    mapOf("balm" to UnmetReason.DOWNLOAD_FAILED), "Fabric", "26.2", "Modrinth"
                )!!
            ),
            "a fetch that died is retryable, which is exactly what ERROR is for"
        )
    }
}
