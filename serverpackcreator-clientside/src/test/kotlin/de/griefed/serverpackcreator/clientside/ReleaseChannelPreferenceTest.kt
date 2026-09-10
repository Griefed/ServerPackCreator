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

    /** The loader gate is untouched: a channel preference cannot promote a file for another loader. */
    @Test
    fun theChannelPreferenceDoesNotWidenTheLoaderRule() {
        Assertions.assertNull(
            pick(versionsJson, loader = "Fabric"),
            "every file here is Forge-tagged, so a Fabric boot still has no candidate"
        )
    }
}
