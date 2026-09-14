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
 * **A Forge-family loader provides its own id, and nothing was saying so.**
 *
 * `LoaderProvidedIds` reads a `provides` block out of an installed loader jar, which Quilt and Fabric
 * publish and Forge and NeoForge do not — so for those two the map came back empty and
 * `DependencyBacktrack` treated a demand on `forge` or `neoforge` as naming something absent, which it
 * skips by design. The loader itself disagrees, in as many words:
 *
 * ```
 * Mod ID: 'forge', Requested by: 'iceberg', Expected range: '[47.2,)', Actual version: '47.1.106'
 * ```
 *
 * Measured on the public grinder 2026-09-13: `advancement-plaques` and `item-highlighter` (on both
 * platforms) were each staged `Iceberg-1.20.1-forge-1.1.25.jar`, which demands `forge [47.2,)`, into a
 * NeoForge `47.1.106` pack — the 1.20.1 fork froze there — and all three were published INCONCLUSIVE.
 * With the pair in hand the judge can demote Iceberg and backtrack to a build that fits.
 *
 * The id is **not** constant per loader, which is why this borrows [JarSelfDeclaration]'s existing mapping
 * rather than writing a second one: NeoForge registers as `forge` on Minecraft 1.20.1, where it runs Forge
 * builds, and as `neoforge` everywhere after.
 *
 * @author Griefed
 */
internal class PlatformProvidesTest {

    /** The case from the grinder: NeoForge on 1.20.1 answers to `forge`. */
    @Test
    fun neoForgeOnMinecraft1201ProvidesForge() {
        Assertions.assertEquals(
            mapOf("forge" to "47.1.106"),
            JarSelfDeclaration.platformProvides("NeoForge", "47.1.106", "1.20.1")
        )
    }

    /** And after 1.20.1 it answers to its own name. */
    @Test
    fun neoForgeProvidesNeoforgeEverywhereElse() {
        Assertions.assertEquals(
            mapOf("neoforge" to "21.1.250"),
            JarSelfDeclaration.platformProvides("NeoForge", "21.1.250", "1.21.1")
        )
    }

    /** Forge is `forge` on every version it ever shipped for. */
    @Test
    fun forgeProvidesForge() {
        Assertions.assertEquals(
            mapOf("forge" to "47.4.23"),
            JarSelfDeclaration.platformProvides("Forge", "47.4.23", "1.20.1")
        )
    }

    /**
     * **Fabric and Quilt are deliberately empty here.** Their loaders publish a real `provides` block whose
     * contents differ per build — quilt-loader 0.30.1 provides `fabricloader 0.19.3` and 0.31.0-beta.4
     * provides `0.19.5` — so the answer has to be read off the install, and inventing one here would
     * shadow the true reading with a guess.
     */
    @Test
    fun theLoadersThatPublishAProvidesBlockAreLeftToIt() {
        Assertions.assertEquals(emptyMap<String, String>(), JarSelfDeclaration.platformProvides("Fabric", "0.16.9", "1.20.1"))
        Assertions.assertEquals(emptyMap<String, String>(), JarSelfDeclaration.platformProvides("Quilt", "0.30.1", "1.20.1"))
    }

    /** A blank build tells us nothing, and a map claiming a version we do not have would be worse than none. */
    @Test
    fun anUnknownLoaderBuildProvidesNothing() {
        Assertions.assertEquals(emptyMap<String, String>(), JarSelfDeclaration.platformProvides("NeoForge", "", "1.20.1"))
    }
}
