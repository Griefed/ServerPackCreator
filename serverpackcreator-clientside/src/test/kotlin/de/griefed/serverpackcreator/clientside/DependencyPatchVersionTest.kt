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
 * Pins that a dependency publishing nothing for the **exact** Minecraft version being booted falls back to
 * another **patch** release of the same version-line, and that the fallback widens nothing else.
 *
 * `pickDependencyFile` refused any other version outright, on the reasoning that a near-miss dependency
 * guarantees a loader-level version conflict which is then blamed on the mod under test. That reasoning
 * holds across a *line* — a 1.19.2 jar in a 1.21 pack helps nobody — and is too strict inside one: 1.20.1,
 * 1.20.2 and 1.20.3 run each other's mods in practice, and a library that skipped a patch release is not a
 * missing dependency.
 *
 * **What it costs today, measured against the live Modrinth API on 2026-09-09** — every one of these is a
 * published `ERROR` on `grinder.serverpackcreator.de` whose dependency exists one patch away:
 *
 * | Refusal | Asked for | Published |
 * |---|---|---|
 * | `better-combat` … `playeranimator` | Forge 1.20.2 | Forge 1.20, 1.20.1 |
 * | `bettergrassify` … `yacl` | Forge 1.20.6 | Forge 1.20, 1.20.1, 1.20.2 |
 * | `bettergrassify` … `forgified-fabric-api` | Forge 1.20.6 | Forge 1.20.1 |
 * | `shatterbyte-lib` … `quilt_base` (→ `qsl`) | Quilt 1.21.1 | Quilt 1.21 |
 * | `terralith` … `quilt_resource_loader` (→ `qsl`) | Quilt 1.21.11 | Quilt 1.21 |
 * | `cobblemon-additions` … `cobblemon` | Fabric 1.21.11 | Fabric 1.21.1 |
 *
 * QSL is the case that shows why a *line* is the right width: its last Modrinth release is Minecraft 1.21,
 * so every Quilt mod declaring a `quilt_*` module on 1.21.1 or later was refused permanently, and no amount
 * of waiting fixes it because the project is discontinued.
 *
 * @author Griefed
 */
internal class DependencyPatchVersionTest {

    /** A published dependency file, obtainable unless [locked] says otherwise. */
    private fun file(
        name: String,
        loaders: Set<String>,
        mcVersions: Set<String>,
        locked: Boolean = false
    ) = ModFile(name, loaders, mcVersions, if (locked) null else "https://cdn/$name", null, emptyList())

    /**
     * The QSL case verbatim: the pack is Quilt 1.21.1 and the only build is Quilt 1.21, one patch below.
     * Staging it is what lets the boot happen at all.
     */
    @Test
    fun aDependencyFallsBackToAPatchNeighbourInTheSameLine() {
        val qsl = listOf(file("qsl-7.0.0+1.21.jar", setOf("Quilt"), setOf("1.21")))

        Assertions.assertEquals(
            "qsl-7.0.0+1.21.jar",
            BootCandidateSelector.pickDependencyFile(qsl, "Quilt", "1.21.1")?.fileName,
            "QSL's newest release is Minecraft 1.21; refusing it refuses every Quilt mod on 1.21.1 forever"
        )
    }

    /** The exact version still wins wherever the dependency publishes one — the fallback only adds a pick. */
    @Test
    fun theExactVersionStillWinsOverAPatchNeighbour() {
        val files = listOf(
            file("dep-1.21.jar", setOf("Fabric"), setOf("1.21")),
            file("dep-1.21.1.jar", setOf("Fabric"), setOf("1.21.1"))
        )

        Assertions.assertEquals(
            "dep-1.21.1.jar",
            BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.21.1")?.fileName
        )
    }

    /**
     * The nearest patch is taken, and a tie goes to the newer one: a build closer to the version being
     * booted is closer to the Minecraft it was compiled against, which is the whole reason the exact match
     * is preferred in the first place.
     */
    @Test
    fun theNearestPatchIsPreferred() {
        val files = listOf(
            file("dep-1.20.jar", setOf("Forge"), setOf("1.20")),
            file("dep-1.20.1.jar", setOf("Forge"), setOf("1.20.1")),
            file("dep-1.20.4.jar", setOf("Forge"), setOf("1.20.4"))
        )

        Assertions.assertEquals(
            "dep-1.20.1.jar",
            BootCandidateSelector.pickDependencyFile(files, "Forge", "1.20.2")?.fileName,
            "1.20.1 is one patch away, 1.20 and 1.20.4 are two"
        )
        Assertions.assertEquals(
            "dep-1.20.4.jar",
            BootCandidateSelector.pickDependencyFile(files, "Forge", "1.20.3")?.fileName,
            "1.20.4 is one patch away and 1.20.1 is two — the fallback bumps up as readily as down"
        )
    }

    /** Equidistant patches are broken toward the newer build, which is the more likely to be maintained. */
    @Test
    fun anEquidistantTieGoesToTheNewerBuild() {
        val eitherSide = listOf(
            file("dep-1.20.jar", setOf("Forge"), setOf("1.20")),
            file("dep-1.20.4.jar", setOf("Forge"), setOf("1.20.4"))
        )

        Assertions.assertEquals(
            "dep-1.20.4.jar",
            BootCandidateSelector.pickDependencyFile(eitherSide, "Forge", "1.20.2")?.fileName,
            "1.20 and 1.20.4 are both two patches from 1.20.2"
        )
    }

    /**
     * The fallback never crosses the version-line, which is the boundary the old refusal was right about:
     * `minecraftLine` is the first two components, so `1.19.4` is no neighbour of `1.20.1` and neither is
     * `26.1.2` of `26.2`.
     */
    @Test
    fun theFallbackNeverCrossesTheMinecraftLine() {
        val oldLine = listOf(file("dep-1.19.4.jar", setOf("Forge"), setOf("1.19.4")))
        val otherLine = listOf(file("dep-26.1.2.jar", setOf("NeoForge"), setOf("26.1.2")))

        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(oldLine, "Forge", "1.20.1"),
            "a 1.19.4 jar in a 1.20.1 pack is the conflict the exact-version rule exists to prevent"
        )
        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(otherLine, "NeoForge", "26.2"),
            "26.1 and 26.2 are different lines, exactly as 1.20 and 1.21 are"
        )
    }

    /**
     * **The fallback must not widen the loader rule.** Cross-loading is a property of the Minecraft version
     * the pack is *booted at*, not of the version the dependency file happens to carry — NeoForge loads
     * Forge builds on 1.20.1 and on no other version, so a Forge 1.20.1 file is still not a dependency for
     * a NeoForge 1.20.2 pack.
     *
     * Written because the obvious implementation — re-run the whole loader ladder at the neighbour version —
     * gets this wrong and silently re-creates the mismatch
     * `BootCandidateSelectorTest.theNeoForgeFallbackToForgeAppliesOnMinecraft1201Only` pins against.
     */
    @Test
    fun theFallbackDoesNotWidenTheLoaderRule() {
        val forgeOnly = listOf(file("dep-forge-1.20.1.jar", setOf("Forge"), setOf("1.20.1")))

        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(forgeOnly, "NeoForge", "1.20.2"),
            "NeoForge renamed its packages away from Forge's at 1.20.2; the patch fallback does not undo that"
        )
    }

    /**
     * Obtainability outranks the version match, for the same reason it already outranks the loader match: a
     * distribution-locked file has no download URL at all, so picking one guarantees the dependency is
     * reported unmet — whereas a patch neighbour that can actually be fetched is a working dependency.
     *
     * This is the `better-combat-by-daedelus` case, where `player-animation-library`'s CurseForge file is
     * locked and the whole boot was refused for it.
     */
    @Test
    fun anObtainableNeighbourBeatsALockedExactMatch() {
        val files = listOf(
            file("dep-1.20.2.jar", setOf("Forge"), setOf("1.20.2"), locked = true),
            file("dep-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )

        Assertions.assertEquals(
            "dep-1.20.1.jar",
            BootCandidateSelector.pickDependencyFile(files, "Forge", "1.20.2")?.fileName,
            "a locked exact match cannot be staged; an obtainable neighbour can"
        )
    }

    /**
     * When every candidate is locked, one is still returned — so the refusal can say *distribution-locked*,
     * which is true and actionable, instead of *publishes nothing for this version*, which would be false.
     * The same guarantee the exact-version ladder already made, kept across the fallback.
     */
    @Test
    fun aLockedNeighbourIsStillReturnedWhenNothingElseIs() {
        val locked = listOf(file("dep-1.20.1.jar", setOf("Forge"), setOf("1.20.1"), locked = true))

        Assertions.assertEquals(
            "dep-1.20.1.jar",
            BootCandidateSelector.pickDependencyFile(locked, "Forge", "1.20.2")?.fileName,
            "naming the opt-out beats claiming the project publishes nothing"
        )
    }

    /**
     * A version component the parser cannot read is not a patch number, and a snapshot or pre-release is not
     * a build to stage a dependency from. Both are skipped rather than guessed at.
     */
    @Test
    fun anUnreadablePatchComponentIsNotANeighbour() {
        val prereleases = listOf(
            file("dep-pre.jar", setOf("Fabric"), setOf("1.21.4-pre3")),
            file("dep-snapshot.jar", setOf("Fabric"), setOf("22w24a"))
        )

        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(prereleases, "Fabric", "1.21.1"),
            "'4-pre3' is not a patch release and '22w24a' is not even in the line"
        )
    }
}
