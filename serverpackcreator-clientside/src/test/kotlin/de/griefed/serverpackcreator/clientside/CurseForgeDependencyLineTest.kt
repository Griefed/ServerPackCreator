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

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that a **CurseForge** dependency is found on a neighbouring patch release — the half of the
 * patch-version fallback that was inert.
 *
 * **The fallback shipped on 2026-09-09 and was dead on CurseForge by construction.**
 * `CurseForgePlatform.resolveDependency` narrows its single page with `gameVersion=<exact>`, so every file
 * it returns carries the exact version. `BootCandidateSelector.patchNeighboursOf` sources neighbours *only
 * from the files in hand*, and `preferenceLadder` tries the exact rung first with the same `compatibleAt` —
 * so a neighbour version can only ever appear as a co-tag on a file the exact rung already matched, and the
 * neighbour rung can never find anything the exact rung did not. Not "rarely useful": logically
 * unreachable-productive.
 *
 * Measured consequence, 2026-09-10: `better-combat-by-daedelus` and `combat-roll`, both **CurseForge**
 * candidates, are published `UNVERIFIABLE` for `playeranimator` on Forge 1.20.2 — while PlayerAnimator
 * publishes a Forge build for 1.20.1 and 1.20. Both rows are exactly what the fallback was written to close,
 * and neither moved, because the six rows it did close were all Modrinth.
 *
 * **The narrowing itself must stay** — it is what fixed the `architectury-api` window bug, where a library
 * publishing 1000+ files has nothing older than current Minecraft in its newest 50 (`DependencyFileWindowTest`
 * pins that). So the fix is to ask for the neighbours as well, not to stop asking for the exact version.
 *
 * Driven through the **real** `CurseForgePlatform` over a recording fetcher, wired into real staging: a fake
 * platform cannot exhibit the defect, since the defect *is* the query CurseForge is sent.
 *
 * @author Griefed
 */
internal class CurseForgeDependencyLineTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /**
     * Two Forge-capable releases of one version-line, oldest first, taken from SPC's real metadata — the
     * pack boots [newer] while the dependency publishes only for [older].
     */
    private val line = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .filter { resolver.latest("Forge", it) != null }
        .groupBy { BootCandidateSelector.minecraftLine(it) }
        .values.first { it.size >= 2 }
        .sortedWith(BootCandidateSelector.minecraftComparator)

    private val older = line.first()
    private val newer = line.last()

    /** Selection passes and generation does not, so staging is exercised and no server is ever launched. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    /** The requests the platform actually sent, so the cost of the fix is asserted rather than assumed. */
    private val requested = mutableListOf<String>()

    /**
     * CurseForge as it really answers: `/files` honours `gameVersion`, so a request for [newer] returns
     * nothing and a request for [older] returns the Forge build. The dependency is reachable only by asking.
     */
    private val fetcher = HttpFetcher { url, _ ->
        requested.add(url)
        when {
            url.contains("/mods/search") ->
                """{"data":[{"id":900,"slug":"combat-roll","links":{"websiteUrl":"https://cf/combat-roll"}}]}"""

            url.contains("/mods/900/files") -> filesJson(
                id = 7001, name = "combat-roll-1.0.0.jar", version = newer,
                dependencies = """[{"modId":800,"relationType":3}]"""
            )

            // Matched on the parsed value, never with `contains`: one release of a line is often a prefix
            // of another (`1.20` of `1.20.6`), so a substring test answers the older file to a request for
            // the newer version and the whole fixture passes against unfixed code.
            url.contains("/mods/800/files") ->
                if (askedVersion(url) == older) {
                    filesJson(id = 7002, name = "player-animation-lib-forge-1.0.2.jar", version = older)
                } else {
                    """{"data":[]}"""
                }

            url.contains("/mods/800") -> """{"data":{"id":800,"slug":"playeranimator","links":{}}}"""
            url.contains("/mods/900") -> """{"data":{"id":900,"slug":"combat-roll","links":{}}}"""
            else -> throw IllegalStateException("unexpected url $url")
        }
    }

    /** The `gameVersion` a `/files` query asks for, or `null` when it is un-narrowed. */
    private fun askedVersion(url: String): String? =
        url.substringAfter("gameVersion=", "").takeIf { it.isNotEmpty() }?.substringBefore('&')

    private fun filesJson(id: Int, name: String, version: String, dependencies: String = "[]") = """
        {"data":[{"id":$id,"fileName":"$name","gameVersions":["$version","Forge"],"releaseType":1,
                  "downloadUrl":"https://edge.forgecdn.net/files/$id/$name","dependencies":$dependencies}]}
    """.trimIndent()

    private val platform = CurseForgePlatform("test-key", fetcher, ObjectMapper())

    /** Writes a Forge descriptor so the staged jars scan cleanly and nothing else refuses the boot. */
    private val downloader = JarDownloader { modFile, targetDirectory ->
        targetDirectory.mkdirs()
        File(targetDirectory, modFile.fileName).also { it.writeText("not really a jar") }
    }

    private fun stagedMods(workDir: File): List<String> =
        File(workDir, AttemptDirectory.nameFor("CurseForge", "combat-roll", "Forge", BootCandidateSelector.minecraftLine(line.first())) + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    /**
     * **The two live rows.** The pack boots the newer patch release, the dependency publishes only for the
     * older one in the same line, and CurseForge answers only what it is asked for — so the dependency has
     * to be asked for.
     */
    @Test
    fun aCurseForgeDependencyIsFoundOnANeighbouringPatchRelease(@TempDir workDir: File) {
        val project = platform.resolve("https://www.curseforge.com/minecraft/mc-mods/combat-roll")
        val prepared = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloader,
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(project, "Forge")

        Assertions.assertEquals(
            listOf("combat-roll-1.0.0.jar", "player-animation-lib-forge-1.0.2.jar"), stagedMods(workDir),
            "the dependency exists one patch away and CurseForge will hand it over when asked"
        )
        val detail = (prepared as? BootVerifier.Prepared.Failed)?.detail.orEmpty()
        Assertions.assertFalse(
            detail.contains("Required dependency"),
            "a dependency reachable by one more request is not unavailable ($detail)"
        )
    }

    /**
     * **The exact version is still asked for first, and it is still one request when that answers.**
     * The neighbours are a fallback paid for only where the boot would otherwise be refused — asking for
     * the whole line up front would multiply what a catalog sweep spends of the API key's quota.
     */
    @Test
    fun theExactVersionIsAskedForFirst(@TempDir workDir: File) {
        val project = platform.resolve("https://www.curseforge.com/minecraft/mc-mods/combat-roll")
        requested.clear()

        BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloader,
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(project, "Forge")

        val askedFor = requested.filter { it.contains("/mods/800/files") }.map { askedVersion(it) }
        Assertions.assertEquals(
            newer, askedFor.firstOrNull(),
            "the version being booted is the first thing asked for: $askedFor"
        )
        Assertions.assertTrue(
            older in askedFor,
            "and the neighbour is asked for only because the exact version answered nothing: $askedFor"
        )
    }
}
