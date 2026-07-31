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
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins the Minecraft ↔ loader mapping for the loaders that do **not** encode Minecraft into their own version.
 *
 * Forge and NeoForge name the Minecraft version inside their loader version, so their mapping is a parse (covered by
 * `ForgeVersionMappingTest` and `NeoForgeVersionMappingTest`). Fabric, Quilt and LegacyFabric publish one
 * Minecraft-independent list of loader versions instead, so their mapping is a *gate*: `Meta.isMinecraftSupported`,
 * answered from Fabric's intermediaries (Fabric, and Quilt which reuses them) or from LegacyFabric's own game
 * manifest.
 *
 * **Why it needs pinning:** that gate decides whether a Minecraft/loader combination exists at all. Downstream, the
 * clientside engine drops any combination whose loader reports no version — deliberately, so a loader lacking a build
 * for a brand-new Minecraft is never mis-scored as a mod crash. A gate that wrongly answers `false` therefore erases
 * whole Minecraft versions from verification silently, and one that wrongly answers `true` sends a boot at a loader
 * that cannot run.
 *
 * Assertions are written as **rules and permanent anchors**, never "the newest version is X", so refreshing the
 * cached manifests cannot make them fail spuriously. Runs offline against those cached manifests.
 */
internal class LoaderMinecraftSupportTest {

    private val versionMeta: VersionMeta =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).versionMeta

    /** A version string that is not a Minecraft version must never be claimed by any loader. */
    @Test
    fun noLoaderClaimsANonsenseMinecraftVersion() {
        for (nonsense in listOf("not-a-version", "0.0.0", "", "1.99.99")) {
            Assertions.assertFalse(versionMeta.fabric.isMinecraftSupported(nonsense), "Fabric claimed '$nonsense'")
            Assertions.assertFalse(versionMeta.quilt.isMinecraftSupported(nonsense), "Quilt claimed '$nonsense'")
            Assertions.assertFalse(
                versionMeta.legacyFabric.isMinecraftSupported(nonsense),
                "LegacyFabric claimed '$nonsense'"
            )
        }
    }

    /** Fabric supports the modern releases the ecosystem is built on. */
    @Test
    fun fabricSupportsEstablishedModernReleases() {
        for (minecraftVersion in listOf("1.20.1", "1.19.2", "1.18.2", "1.21.1")) {
            Assertions.assertTrue(
                versionMeta.fabric.isMinecraftSupported(minecraftVersion),
                "Fabric must support $minecraftVersion — intermediaries for it have existed for years"
            )
        }
    }

    /**
     * Quilt answers this question with Fabric's intermediaries, so the two must agree exactly. Pinned as an
     * equivalence rather than a version list: if Quilt ever gains its own intermediary source, this is the assertion
     * that should force the decision into the open.
     */
    @Test
    fun quiltTracksFabricsIntermediariesExactly() {
        val sample = listOf("1.20.1", "1.19.2", "1.18.2", "1.16.5", "1.12.2", "1.7.10", "not-a-version")
        for (minecraftVersion in sample) {
            Assertions.assertEquals(
                versionMeta.fabric.isMinecraftSupported(minecraftVersion),
                versionMeta.quilt.isMinecraftSupported(minecraftVersion),
                "Quilt resolves Minecraft support through Fabric's intermediaries, so $minecraftVersion must agree"
            )
        }
    }

    /**
     * LegacyFabric exists precisely to cover the versions Fabric does not, so it must support old Minecraft and must
     * **not** claim modern releases — if it did, the clientside engine would select it for a Minecraft version it
     * cannot run.
     */
    @Test
    fun legacyFabricCoversOldMinecraftAndNotModernReleases() {
        Assertions.assertTrue(
            versionMeta.legacyFabric.isMinecraftSupported("1.12.2") ||
                versionMeta.legacyFabric.isMinecraftSupported("1.8.9"),
            "LegacyFabric must support the old versions it exists for"
        )
        for (modern in listOf("1.20.1", "1.21.1")) {
            Assertions.assertFalse(
                versionMeta.legacyFabric.isMinecraftSupported(modern),
                "LegacyFabric must not claim $modern — that is Fabric's territory"
            )
        }
    }

    /**
     * The gate and the version list must not disagree: a loader that reports Minecraft support has to actually offer
     * loader versions, or selection produces a combination nothing can install.
     */
    @Test
    fun aSupportedMinecraftVersionAlsoHasLoaderVersions() {
        Assertions.assertTrue(versionMeta.fabric.isMinecraftSupported("1.20.1"))
        Assertions.assertTrue(versionMeta.fabric.loaderVersions().isNotEmpty(), "Fabric has no loaders")
        Assertions.assertTrue(versionMeta.quilt.loaderVersions().isNotEmpty(), "Quilt has no loaders")
    }

    /**
     * Forge and NeoForge answer the same question from their parsed mapping rather than a gate, so the two families
     * must at least agree on the obvious: a long-established Minecraft version has builds, and a nonsense one has
     * none. This is the cheap end-to-end check that the parse in `ForgeLoader`/`NeoForgeLoader` actually populated
     * something — a mapping bug that yields empty lists shows up here.
     */
    @Test
    fun forgeAndNeoForgeReportBuildsForEstablishedVersions() {
        Assertions.assertTrue(
            versionMeta.forge.supportedForgeVersions("1.20.1").isPresent,
            "Forge must report builds for 1.20.1"
        )
        Assertions.assertTrue(
            versionMeta.forge.supportedForgeVersions("not-a-version").isEmpty,
            "Forge must report nothing for a nonsense Minecraft version"
        )
        Assertions.assertTrue(
            versionMeta.neoForge.supportedNeoForgeVersions("1.21.1").isPresent,
            "NeoForge must report builds for 1.21.1"
        )
        Assertions.assertTrue(
            versionMeta.neoForge.supportedNeoForgeVersions("not-a-version").isEmpty,
            "NeoForge must report nothing for a nonsense Minecraft version"
        )
    }
}
