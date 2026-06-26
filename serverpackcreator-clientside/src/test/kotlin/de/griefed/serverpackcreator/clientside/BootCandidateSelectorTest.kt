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
 * Pins the boot file/version-selection that drives which combination the boot-test installs, free of
 * any [de.griefed.serverpackcreator.api.ApiWrapper] or running server.
 */
internal class BootCandidateSelectorTest {

    private fun file(name: String, loaders: Set<String>, mcVersions: Set<String>) =
        ModFile(name, loaders, mcVersions, "https://cdn/$name", null, emptyList())

    @Test
    fun minecraftComparatorOrdersNumericallyNotLexically() {
        Assertions.assertTrue(BootCandidateSelector.minecraftComparator.compare("1.20", "1.9") > 0)
        Assertions.assertTrue(BootCandidateSelector.minecraftComparator.compare("1.20.1", "1.20") > 0)
    }

    @Test
    fun picksNewestMinecraftVersionForLoader() {
        val files = listOf(
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )
        val candidate = BootCandidateSelector.pickBootableCandidate(files, "Forge") { true }
        Assertions.assertEquals("1.20.1", candidate?.second)
    }

    @Test
    fun skipsMinecraftVersionsWithoutAnAvailableLoaderVersion() {
        val files = listOf(
            file("mod-1.21.jar", setOf("Forge"), setOf("1.21")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )
        // The newest (1.21) has no loader version yet; selection falls back to 1.20.1.
        val candidate = BootCandidateSelector.pickBootableCandidate(files, "Forge") { it != "1.21" }
        Assertions.assertEquals("1.20.1", candidate?.second)
    }

    @Test
    fun returnsNullWhenNoLoaderVersionIsEverAvailable() {
        val files = listOf(file("mod-1.20.1.jar", setOf("Fabric"), setOf("1.20.1")))
        Assertions.assertNull(BootCandidateSelector.pickBootableCandidate(files, "Fabric") { false })
        // also null for a loader the project does not ship
        Assertions.assertNull(BootCandidateSelector.pickBootableCandidate(files, "Forge") { true })
    }

    @Test
    fun dependencyFilePrefersExactMinecraftMatchThenFallsBack() {
        val files = listOf(
            file("dep-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("dep-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )
        Assertions.assertEquals("dep-1.20.1.jar", BootCandidateSelector.pickDependencyFile(files, "Forge", "1.20.1")?.fileName)
        // no exact match -> first file for the loader
        Assertions.assertEquals("dep-1.19.2.jar", BootCandidateSelector.pickDependencyFile(files, "Forge", "1.21")?.fileName)
        Assertions.assertNull(BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.1"))
    }
}
