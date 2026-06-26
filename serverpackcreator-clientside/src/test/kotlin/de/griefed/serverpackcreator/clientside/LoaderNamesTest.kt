/* Copyright (C) 2025 Griefed
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
 * Pins the normalization of platform loader-tokens onto SPC's canonical names and, crucially, that
 * non-loader tokens (Minecraft versions, "Client") are dropped — CurseForge mixes them into the same
 * `gameVersions` list.
 */
internal class LoaderNamesTest {

    @Test
    fun normalizesKnownLoaderTokensCaseAndSeparatorInsensitively() {
        Assertions.assertEquals("NeoForge", LoaderNames.canonical("neoforge"))
        Assertions.assertEquals("NeoForge", LoaderNames.canonical("NeoForge"))
        Assertions.assertEquals("LegacyFabric", LoaderNames.canonical("legacy-fabric"))
        Assertions.assertEquals("Fabric", LoaderNames.canonical("Fabric"))
    }

    @Test
    fun dropsNonLoaderTokens() {
        Assertions.assertNull(LoaderNames.canonical("1.20.1"))
        Assertions.assertNull(LoaderNames.canonical("Client"))
        Assertions.assertEquals(
            setOf("Forge", "Fabric"),
            LoaderNames.canonicalLoaders(listOf("1.20.1", "Forge", "Fabric", "Client"))
        )
    }
}
