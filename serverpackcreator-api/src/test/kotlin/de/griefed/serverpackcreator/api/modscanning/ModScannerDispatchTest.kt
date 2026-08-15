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
package de.griefed.serverpackcreator.api.modscanning

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins [ModScanner.scannerFor], the single place a modloader and Minecraft version are turned into
 * the scanner that reads a pack. Both a generation ([de.griefed.serverpackcreator.api.serverpack.ModListCompiler])
 * and the clientside engine's metadata signal dispatch through it, so a wrong answer here makes the
 * two disagree about what a jar declared — and picking a scanner for the wrong descriptor era fails
 * *silently*, since a scanner that finds no descriptor reports the never-drop-a-jar default rather
 * than an error.
 *
 * Asserted on identity: what matters is which collaborator is returned, not that something was.
 */
internal class ModScannerDispatchTest {

    private val modScanner = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).modScanner

    /** Fabric and LegacyFabric both read `fabric.mod.json`, on every Minecraft version. */
    @Test
    fun fabricAndLegacyFabricAlwaysUseTheFabricScanner() {
        for (minecraftVersion in listOf("1.12.2", "1.20.1", "26.2")) {
            Assertions.assertSame(
                modScanner.fabricScanner, modScanner.scannerFor("Fabric", minecraftVersion),
                "Fabric/$minecraftVersion"
            )
            Assertions.assertSame(
                modScanner.fabricScanner, modScanner.scannerFor("LegacyFabric", minecraftVersion),
                "LegacyFabric/$minecraftVersion"
            )
        }
    }

    /** A Quilt pack mixes both descriptors, so it gets the composite rather than either scanner alone. */
    @Test
    fun quiltUsesTheCompositeScanner() {
        Assertions.assertSame(modScanner.quiltPackScanner, modScanner.scannerFor("Quilt", "1.20.1"))
    }

    /**
     * Forge replaced the FML annotation-cache with `mods.toml` in Minecraft 1.13, and the boundary
     * has to be read from the whole version: `26.2` belongs to the newer `YY.x.y` scheme, so testing
     * its minor of `2` on its own would place it in the 1.2 era and pick the 1.12-and-older scanner.
     */
    @Test
    fun theForgeDescriptorEraIsReadFromTheWholeVersion() {
        for (minecraftVersion in listOf("1.7.10", "1.12", "1.12.2")) {
            Assertions.assertSame(
                modScanner.forgeAnnotationScanner, modScanner.scannerFor("Forge", minecraftVersion),
                "Forge/$minecraftVersion predates mods.toml"
            )
        }
        for (minecraftVersion in listOf("1.13", "1.16.5", "1.20.1", "1.21.1", "26.1", "26.2")) {
            Assertions.assertSame(
                modScanner.forgeTomlScanner, modScanner.scannerFor("Forge", minecraftVersion),
                "Forge/$minecraftVersion carries mods.toml"
            )
        }
    }

    /** NeoForge renamed the descriptor in 1.20.5; before that it is Forge's, on both schemes. */
    @Test
    fun theNeoForgeDescriptorEraIsReadFromTheWholeVersion() {
        for (minecraftVersion in listOf("1.20.1", "1.20.4")) {
            Assertions.assertSame(
                modScanner.forgeTomlScanner, modScanner.scannerFor("NeoForge", minecraftVersion),
                "NeoForge/$minecraftVersion still uses Forge's mods.toml"
            )
        }
        for (minecraftVersion in listOf("1.20.5", "1.21.1", "26.2")) {
            Assertions.assertSame(
                modScanner.neoForgeTomlScanner, modScanner.scannerFor("NeoForge", minecraftVersion),
                "NeoForge/$minecraftVersion uses neoforge.mods.toml"
            )
        }
    }

    /**
     * An unparseable Minecraft version falls back to the modern Forge scanner. The annotation cache
     * exists only in jars a decade old, so it is never the safer guess for a version nothing could
     * be read from — and this must not throw, which is what the bare-component parsing used to do.
     */
    @Test
    fun anUnparseableMinecraftVersionFallsBackToTheModernForgeScanner() {
        for (minecraftVersion in listOf("", "26", "not-a-version", "1.x.y")) {
            Assertions.assertSame(
                modScanner.forgeTomlScanner, modScanner.scannerFor("Forge", minecraftVersion),
                "Forge/'$minecraftVersion'"
            )
        }
    }

    /**
     * No scanner knows an unrecognised loader, and the `null` is deliberate: each caller decides
     * what that means, and both keep every mod rather than silently producing an empty server pack.
     */
    @Test
    fun anUnrecognisedModloaderHasNoScanner() {
        for (modloader in listOf("", "forge", "Rift", "Cauldron")) {
            Assertions.assertNull(modScanner.scannerFor(modloader, "1.20.1"), "modloader '$modloader'")
        }
    }
}
