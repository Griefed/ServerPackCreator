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
import kotlin.test.Test

/**
 * **A build variant of the same version is not a newer version, and some of them cannot boot alone.**
 *
 * malilib publishes `0.10.0-dev.23` and `0.10.0-dev.23.nomixin` for Forge 1.12.2, and Modrinth returns the
 * `nomixin` one first because it is newer *by date*. `pickForLoader` takes the first match, so every mod
 * depending on malilib was staged the variant that declares the Mixin tweaker without carrying Mixin —
 * `java.lang.ClassNotFoundException: org.spongepowered.asm.launch.MixinTweaker`, and the server never
 * launched.
 *
 * Measured on the public grinder 2026-09-13: `litematica`, `minihud`, `tweakeroo` and `zume`, all
 * Forge 1.12, all four staged `malilib-forge-1.12.2-0.10.0-dev.23.nomixin.jar`, all four published
 * INCONCLUSIVE. Confirmed against the live Modrinth API — the plain `0.10.0-dev.23` build is right there.
 *
 * @author Griefed
 */
internal class PlainBuildPreferenceTest {

    private fun file(version: String) = ModFile(
        fileName = "malilib-forge-1.12.2-$version.jar",
        loaders = LoaderNames.canonicalLoaders(listOf("forge")),
        minecraftVersions = setOf("1.12.2"),
        downloadUrl = "https://example.invalid/$version.jar",
        pageUrl = null,
        requiredDependencies = emptyList(),
        version = version
    )

    private fun pick(files: List<ModFile>) =
        BootCandidateSelector.pickDependencyFile(files, "Forge", "1.12.2")?.version

    /** The exact shape and order Modrinth returns for malilib. */
    @Test
    fun aPlainBuildWinsOverItsOwnVariant() {
        Assertions.assertEquals(
            "0.10.0-dev.23",
            pick(listOf(file("0.10.0-dev.23.nomixin"), file("0.10.0-dev.23"), file("0.10.0-dev.21.pre2")))
        )
    }

    /** Still only a preference: with no plain build published, the variant is better than no dependency. */
    @Test
    fun aVariantIsStillPickedWhenItIsAllThereIs() {
        Assertions.assertEquals("0.10.0-dev.23.nomixin", pick(listOf(file("0.10.0-dev.23.nomixin"))))
    }

    /**
     * **A newer version is not demoted just because an older plain build exists.** `dev.23` is not a variant
     * of `dev.21`: the extra segment has to hang off *that same version*, and `pre2` carries a digit anyway.
     */
    @Test
    fun anOrdinaryNewerVersionIsNotAVariant() {
        Assertions.assertEquals(
            "0.10.0-dev.23",
            pick(listOf(file("0.10.0-dev.23"), file("0.10.0-dev.21")))
        )
    }

    /**
     * **Build metadata is not a variant.** `1.6.1+1.21.1` hangs a Minecraft version off `1.6.1`, and Fabric
     * API publishes that shape by the thousand — demoting it would invert the whole catalogue. The
     * discriminator is that the extra segment carries digits.
     */
    @Test
    fun buildMetadataIsNotAVariant() {
        val files = listOf(file("1.6.1+1.21.1"), file("1.6.1"))
        Assertions.assertEquals("1.6.1+1.21.1", pick(files))
    }
}
