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
 * A published version string routinely leads with the **Minecraft** version, and comparing that against a
 * dependant's range compares the wrong number entirely.
 *
 * Measured against the live Modrinth API on 2026-09-13, Create publishes both spellings within one
 * loader/Minecraft pair — `mc1.20.1-6.0.8` and `1.20.1-6.0.6` — so the set is judged inconsistently: the
 * `mc`-prefixed one is unreadable and *accepts*, the bare one reads as `1.20.1` and is compared as though
 * the mod were at version 1.20. Neither answer is about Create's version.
 *
 * @author Griefed
 */
internal class VersionOfFileTest {

    private fun file(version: String?, minecraft: Set<String>) = ModFile(
        fileName = "x.jar",
        loaders = setOf("forge"),
        minecraftVersions = minecraft,
        downloadUrl = "https://example.invalid/x.jar",
        pageUrl = null,
        requiredDependencies = emptyList(),
        version = version
    )

    /** The file's own Minecraft versions are the evidence — no guessing which component is which. */
    @Test
    fun stripsAMinecraftVersionTheFileItselfDeclares() {
        Assertions.assertEquals("6.0.8", VersionOfFile.of(file("mc1.20.1-6.0.8", setOf("1.20.1"))))
        Assertions.assertEquals("6.0.6", VersionOfFile.of(file("1.20.1-6.0.6", setOf("1.20.1"))))
        Assertions.assertEquals("5.4.0", VersionOfFile.of(file("1.21.4-NeoForge-5.4.0", setOf("1.21.4"))))
    }

    /** A version that does not lead with one of the file's own Minecraft versions is left alone. */
    @Test
    fun leavesAnOrdinaryVersionUntouched() {
        Assertions.assertEquals("6.0.8", VersionOfFile.of(file("6.0.8", setOf("1.20.1"))))
        Assertions.assertEquals("0.92.2+1.20.1", VersionOfFile.of(file("0.92.2+1.20.1", setOf("1.20.1"))))
        Assertions.assertEquals("1.6.1+1.21.1", VersionOfFile.of(file("1.6.1+1.21.1", setOf("1.21.1"))))
        Assertions.assertNull(VersionOfFile.of(file(null, setOf("1.20.1"))))
    }

    /**
     * **Stripping must never empty the string.** A file published under nothing but its Minecraft version
     * has no mod version to read, and returning `""` would make it compare as `0.0.0` — a refusal
     * manufactured out of a naming convention, which is the direction [VersionConstraint] forbids.
     */
    @Test
    fun neverStripsAwayTheWholeVersion() {
        Assertions.assertEquals("1.20.1", VersionOfFile.of(file("1.20.1", setOf("1.20.1"))))
        Assertions.assertEquals("mc1.20.1", VersionOfFile.of(file("mc1.20.1", setOf("1.20.1"))))
    }

    /** The whole point: the range now narrows on the mod's version instead of Minecraft's. */
    @Test
    fun theStrippedVersionIsWhatTheRangeSees() {
        val newest = file("mc1.20.1-6.0.8", setOf("1.20.1"))
        val wanted = file("1.20.1-0.5.1.f", setOf("1.20.1"))
        Assertions.assertFalse(VersionConstraint.satisfies(VersionOfFile.of(newest), "[0.5.1.e,0.5.2)"))
        Assertions.assertTrue(VersionConstraint.satisfies(VersionOfFile.of(wanted), "[0.5.1.e,0.5.2)"))
    }
}
