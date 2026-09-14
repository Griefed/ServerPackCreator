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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins that the boot picks the newest **release**, and reaches for a beta or an alpha only when the project
 * publishes no release for the loader.
 *
 * **The reported case, read from the live Modrinth API on 2026-09-10.** `hybrid-aquatic` publishes **16
 * stable Forge releases** — `1.5.0-forge` … `1.6.9-forge`, every one on Minecraft 1.20.1 and carrying a real
 * `META-INF/mods.toml` — beside **10 `[Sinytra]` betas** on 1.20.1/1.20.2/1.20.4. The grinder booted the
 * beta `[1.20.4] [Sinytra] Hybrid Aquatic 1.4.4.jar`, whose only descriptor is a `fabric.mod.json`, and
 * published `UNVERIFIABLE` for a project with sixteen perfectly ordinary Forge builds.
 *
 * **Why newest-Minecraft-first steers into the beta channel systematically.** `pickBootableCandidate` sorts
 * by Minecraft descending and takes the first bootable pair, and authors publish their *experimental*
 * newer-Minecraft ports as betas while the stable line sits on an older version. So the ordering does not
 * merely permit a beta — it prefers one, for exactly the projects that have a stable alternative.
 *
 * **The channel was read nowhere.** `grep releaseType\|version_type` hit only `-api`'s Mojang version
 * metadata; `ModFile` had no such field, so Modrinth's `version_type` and CurseForge's `releaseType` were
 * both discarded at the platform boundary.
 *
 * Griefed's rule: *"Aim for the newest Release of a mod for any modloader. Only pick Beta or Alpha releases
 * when no regular release is available."* So the channel outranks Minecraft recency — a stable build on an
 * older Minecraft is what a user's pack would install, and it is what the verdict should be about.
 *
 * Driven through the **real** `ModrinthPlatform` over canned JSON rather than hand-built `ModFile`s: the
 * channel has to survive the platform parse to matter, and a test constructing the value under test cannot
 * see a producer that drops it — the lesson `DependencySlugTest` was written for.
 *
 * @author Griefed
 */
internal class ReleaseChannelPreferenceTest {

    /** `hybrid-aquatic`'s real shape: a newer-Minecraft beta beside an older-Minecraft stable line. */
    private val versionsJson = """
        [
          {"version_number":"1.4.4","version_type":"beta","loaders":["forge"],"game_versions":["1.20.4"],
           "files":[{"filename":"[1.20.4] [Sinytra] Hybrid Aquatic 1.4.4.jar","primary":true,
                     "url":"https://cdn/sinytra.jar"}],"dependencies":[]},
          {"version_number":"1.6.9-forge","version_type":"release","loaders":["forge"],
           "game_versions":["1.20.1"],
           "files":[{"filename":"[1.20.1-Forge] Hybrid Aquatic 1.6.9.jar","primary":true,
                     "url":"https://cdn/forge169.jar"}],"dependencies":[]},
          {"version_number":"1.6.8-forge","version_type":"release","loaders":["forge"],
           "game_versions":["1.20.1"],
           "files":[{"filename":"[1.20.1-Forge] Hybrid Aquatic 1.6.8.jar","primary":true,
                     "url":"https://cdn/forge168.jar"}],"dependencies":[]}
        ]
    """.trimIndent()

    private val projectJson = """{"slug":"hybrid-aquatic","client_side":"required","server_side":"required"}"""

    private fun platform(versions: String) = ModrinthPlatform(
        HttpFetcher { url, _ ->
            when {
                url.endsWith("/version") -> versions
                else -> projectJson
            }
        },
        ObjectMapper()
    )

    /** The files the real [CurseForgePlatform] reports for canned `/files` nodes. */
    private fun curseForge(vararg fileNodes: String): List<ModFile> = CurseForgePlatform(
        "test-key",
        HttpFetcher { url, _ ->
            if (url.contains("/files")) {
                """{"data":[${fileNodes.joinToString(",")}],
                    "pagination":{"index":0,"resultCount":${fileNodes.size},"totalCount":${fileNodes.size}}}"""
            } else {
                """{"data":[{"id":900,"slug":"hybrid-aquatic","links":{"websiteUrl":"https://cf/ha"}}]}"""
            }
        },
        ObjectMapper()
    ).resolve("https://www.curseforge.com/minecraft/mc-mods/hybrid-aquatic").files

    private fun pick(versions: String, loader: String = "Forge") = BootCandidateSelector.pickBootableCandidate(
        platform(versions).resolve("https://modrinth.com/mod/hybrid-aquatic").files,
        loader
    ) { true }

    /** **The reported defect.** Sixteen stable Forge builds exist, so the beta is not what gets booted. */
    @Test
    fun aReleaseIsPreferredOverANewerMinecraftBeta() {
        val picked = pick(versionsJson)

        Assertions.assertEquals(
            "[1.20.1-Forge] Hybrid Aquatic 1.6.9.jar", picked?.first?.fileName,
            "a stable build is what a user's pack installs; the beta is a Sinytra experiment"
        )
        Assertions.assertEquals(
            "1.20.1", picked?.second,
            "the pack's Minecraft follows the file that was chosen, not the newest one on offer"
        )
    }

    /** Within a channel the newest Minecraft still wins — the channel narrows, it does not re-order. */
    @Test
    fun theNewestMinecraftStillWinsWithinAChannel() {
        val twoReleases = versionsJson.replace(""""version_type":"beta"""", """"version_type":"release"""")

        Assertions.assertEquals(
            "[1.20.4] [Sinytra] Hybrid Aquatic 1.4.4.jar", pick(twoReleases)?.first?.fileName,
            "with both on the same channel the ordering is unchanged from before"
        )
    }

    /**
     * **A preference, never a filter.** `faster-random` publishes an `alpha` for Forge and *zero* releases
     * for that loader (measured live), so filtering would have stopped grinding it entirely.
     */
    @Test
    fun aProjectWithoutAnyReleaseStillYieldsACandidate() {
        val betasOnly = versionsJson.replace(""""version_type":"release"""", """"version_type":"alpha"""")

        Assertions.assertEquals(
            "[1.20.4] [Sinytra] Hybrid Aquatic 1.4.4.jar", pick(betasOnly)?.first?.fileName,
            "beta outranks alpha, and nothing is dropped for having no release"
        )
    }

    /** An unknown or absent `version_type` reads as a release, so a platform that stops sending it is safe. */
    @Test
    fun anAbsentChannelReadsAsARelease() {
        val untyped = versionsJson
            .replace(""""version_type":"beta",""", "")
            .replace(""""version_type":"release",""", "")

        Assertions.assertEquals(
            "[1.20.4] [Sinytra] Hybrid Aquatic 1.4.4.jar", pick(untyped)?.first?.fileName,
            "with nothing to distinguish them the newest Minecraft wins, exactly as before"
        )
    }

    /**
     * **CurseForge's half of the rule, which had no assertion at all.** `ReleaseChannel.fromCurseForge` has
     * one call site and the two existing CurseForge fixtures set `releaseType:1` incidentally, so `2`→BETA,
     * `3`→ALPHA and the fail-toward-RELEASE for an absent field were unexercised — on the larger of the two
     * catalogs, and the one with no sideness field, i.e. where booting an unrepresentative build costs most.
     *
     * Driven through the real `CurseForgePlatform` for the same reason the Modrinth guards are: a channel
     * that does not survive the platform parse cannot matter, and a test constructing `ModFile`s itself
     * cannot see a producer dropping it.
     */
    @Test
    fun curseForgeReleaseTypesBecomeChannels() {
        val files = curseForge(
            """{"id":1,"fileName":"stable.jar","gameVersions":["1.20.1","Forge"],"releaseType":1,
                "downloadUrl":"https://edge/stable.jar"}""",
            """{"id":2,"fileName":"beta.jar","gameVersions":["1.20.4","Forge"],"releaseType":2,
                "downloadUrl":"https://edge/beta.jar"}""",
            """{"id":3,"fileName":"alpha.jar","gameVersions":["1.20.6","Forge"],"releaseType":3,
                "downloadUrl":"https://edge/alpha.jar"}""",
            """{"id":4,"fileName":"untyped.jar","gameVersions":["1.19.2","Forge"],
                "downloadUrl":"https://edge/untyped.jar"}"""
        )

        Assertions.assertEquals(
            mapOf(
                "stable.jar" to ReleaseChannel.RELEASE,
                "beta.jar" to ReleaseChannel.BETA,
                "alpha.jar" to ReleaseChannel.ALPHA,
                "untyped.jar" to ReleaseChannel.RELEASE
            ),
            files.associate { it.fileName to it.channel },
            "1/2/3 are CurseForge's own numbering, and an absent field reads as a release like everywhere else"
        )
    }

    /** And the preference then behaves as it does on Modrinth: the stable older build is what gets booted. */
    @Test
    fun aCurseForgeReleaseBeatsANewerMinecraftBeta() {
        val files = curseForge(
            """{"id":1,"fileName":"stable.jar","gameVersions":["1.20.1","Forge"],"releaseType":1,
                "downloadUrl":"https://edge/stable.jar"}""",
            """{"id":2,"fileName":"beta.jar","gameVersions":["1.20.4","Forge"],"releaseType":2,
                "downloadUrl":"https://edge/beta.jar"}"""
        )

        Assertions.assertEquals(
            "stable.jar", BootCandidateSelector.pickBootableCandidate(files, "Forge") { true }?.first?.fileName
        )
    }

    /**
     * **The channel narrows *within* the availability gate, never above it.** The rule is a preference, and
     * a release for a Minecraft the loader cannot boot is not a candidate at all — so the beta gets its
     * turn. Hoisting the channel filter above `loaderVersionAvailable` would pass every other guard here
     * while making a project whose only release targets an unsupported Minecraft unverifiable.
     */
    @Test
    fun theChannelPreferenceNeverOverridesLoaderAvailability() {
        val picked = BootCandidateSelector.pickBootableCandidate(
            platform(versionsJson).resolve("https://modrinth.com/mod/hybrid-aquatic").files,
            "Forge"
        ) { minecraftVersion -> minecraftVersion != "1.20.1" }

        Assertions.assertEquals(
            "[1.20.4] [Sinytra] Hybrid Aquatic 1.4.4.jar", picked?.first?.fileName,
            "the only releases are 1.20.1 builds and this loader has no build there, so the beta is all there is"
        )
    }

    /** The loader gate is untouched: a channel preference cannot promote a file for another loader. */
    @Test
    fun theChannelPreferenceDoesNotWidenTheLoaderRule() {
        Assertions.assertNull(
            pick(versionsJson, loader = "Fabric"),
            "every file here is Forge-tagged, so a Fabric boot still has no candidate"
        )
    }
}
