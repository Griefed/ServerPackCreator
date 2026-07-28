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
import java.io.File

/**
 * Pins [LoaderVersionResolver]'s Minecraft-awareness against real (offline) version metadata. The
 * intermediary loaders (Fabric/Quilt/LegacyFabric) return their global-latest loader only when they
 * actually support the Minecraft version — a version they have no intermediary for yields `null`, so
 * the boot-candidate selection skips the combo instead of spinning up a container that would abort
 * "loader is not available for Minecraft ...".
 */
internal class LoaderVersionResolverTest {
    private val resolver = LoaderVersionResolver(
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).versionMeta
    )

    @Test
    fun returnsALoaderForASupportedMinecraftVersion() {
        Assertions.assertNotNull(resolver.latest("Forge", "1.20.1"), "Forge supports 1.20.1")
        Assertions.assertNotNull(resolver.latest("Fabric", "1.20.1"), "Fabric supports 1.20.1")
        Assertions.assertNotNull(resolver.latest("Quilt", "1.20.1"), "Quilt supports 1.20.1")
        Assertions.assertNotNull(resolver.latest("LegacyFabric", "1.8.9"), "LegacyFabric supports 1.8.9")
    }

    @Test
    fun returnsNullForAMinecraftVersionTheLoaderDoesNotSupport() {
        // A version no loader has metadata for — before the Minecraft-support gate, Fabric/Quilt/
        // LegacyFabric wrongly returned their global-latest loader here (the false-bootable combo).
        val unsupported = "1.99.99"
        Assertions.assertNull(resolver.latest("Fabric", unsupported), "Fabric must not claim an unsupported MC")
        Assertions.assertNull(resolver.latest("Quilt", unsupported), "Quilt must not claim an unsupported MC")
        Assertions.assertNull(resolver.latest("LegacyFabric", unsupported), "LegacyFabric must not claim an unsupported MC")
        Assertions.assertNull(resolver.latest("Forge", unsupported))
        Assertions.assertNull(resolver.latest("NeoForge", unsupported))
    }

    @Test
    fun returnsNullForAnUnknownLoader() {
        Assertions.assertNull(resolver.latest("NotALoader", "1.20.1"))
    }
}
