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
 * Pins [BootLoaderVersion] — which modloader build a console says actually started.
 *
 * **Every line below is verbatim from a kept console on the public grinder**, read 2026-09-11, because the
 * whole value of this reader is that it matches what the loaders really print rather than what a pattern
 * written from memory expects.
 *
 * The reason it exists: all **16 of 16** Quilt boots in that sample printed `Quilt Loader 0.30.1` while
 * their verdicts reported `Quilt 0.31.0-beta.4`, the build staging had chosen. The two are not
 * interchangeable — quilt-loader `0.30.1` declares `provides: fabricloader 0.19.3` and `0.31.0-beta.4`
 * declares `0.19.5`, while `fabric-language-kotlin` demands `[0.19.5, ∞)` — so twelve published rows failed
 * on a library the build they *claimed* to run would have satisfied.
 *
 * @author Griefed
 */
internal class BootLoaderVersionTest {

    /** `Modrinth/zoomify`, Quilt, Minecraft 1.20.5 — the announcement every Fabric-family boot prints. */
    @Test
    fun readsTheBuildQuiltAnnounces() {
        Assertions.assertEquals(
            "0.30.1",
            BootLoaderVersion.observedIn(
                listOf("[08:30:36] [main/INFO]: Loading Minecraft 1.20.5 with Quilt Loader 0.30.1")
            )
        )
    }

    /** The same line in Fabric's spelling, where the observed build really was the one asked for. */
    @Test
    fun readsTheBuildFabricAnnounces() {
        Assertions.assertEquals(
            "0.19.5",
            BootLoaderVersion.observedIn(
                listOf("[19:22:21] [main/INFO]: Loading Minecraft 26.1 with Fabric Loader 0.19.5")
            )
        )
    }

    /** A crash report quotes it too, which is what survives when the live line scrolled past a cap. */
    @Test
    fun fallsBackToTheCrashReportsOwnHeader() {
        Assertions.assertEquals(
            "0.30.1",
            BootLoaderVersion.observedIn(
                listOf("---- Quilt Loader: Failed to load ----", "Quilt Loader Version: 0.30.1")
            )
        )
    }

    /** `CurseForge/yungs-better-caves`, Forge 1.20.1 — the one phrasing Forge offers, and only late. */
    @Test
    fun readsTheBuildForgeAnnounces() {
        Assertions.assertEquals(
            "47.4.23",
            BootLoaderVersion.observedIn(
                listOf(
                    "[19:48:06] [modloading-worker-0/INFO] [ne.mi.co.ForgeMod/FORGEMOD]: " +
                        "Forge mod loading, version 47.4.23, for MC 1.20.1 with MCP 20230612.114412"
                )
            )
        )
    }

    /**
     * **A boot that died before the loader spoke yields nothing**, and nothing is inferred from the
     * classpath instead: a pack carries several loader jars and picking one would be a guess dressed as an
     * observation. Every caller reads `null` as "fall back to the build we asked for".
     */
    @Test
    fun aConsoleThatNeverAnnouncedOneYieldsNothing() {
        Assertions.assertNull(
            BootLoaderVersion.observedIn(
                listOf(
                    "[12:30:52] [main/WARN]: Mod file /srv/pack/libraries/net/minecraftforge/fmlcore/" +
                        "1.20.2-48.1.0/fmlcore-1.20.2-48.1.0.jar is missing mods.toml",
                    "Exception in thread \"main\" java.lang.RuntimeException"
                )
            ),
            "a jar path on the classpath is not an announcement"
        )
        Assertions.assertNull(BootLoaderVersion.observedIn(emptyList()))
    }

    /** **The reported case.** A disagreement is stated, so the row stops claiming a build it never ran. */
    @Test
    fun aDisagreementBetweenTheStagedAndTheBootedBuildIsStated() {
        Assertions.assertEquals(
            "(the pack was staged for 0.31.0-beta.4 but booted 0.30.1)",
            BootLoaderVersion.disagreementNote("0.31.0-beta.4", "0.30.1")
        )
    }

    /** Agreement is the common case and says nothing: a note on every row would be noise, not evidence. */
    @Test
    fun agreementIsSilent() {
        Assertions.assertNull(BootLoaderVersion.disagreementNote("0.19.5", "0.19.5"))
        Assertions.assertNull(BootLoaderVersion.disagreementNote("0.19.5", null))
        Assertions.assertNull(BootLoaderVersion.disagreementNote(null, "0.19.5"))
    }
}
