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
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins the pre-boot gate: **a jar is never booted on a runtime it does not claim to support.**
 *
 * A platform's declared loader/Minecraft sets are what an author ticked, and the selector trusts them
 * absolutely — it boots the newest Minecraft in the set and, where one file claims two loaders, whichever
 * the platform listed first. Measured against the live grinder on 2026-08-31 that produced
 * `DamageVignette-2.0.2-**forge**+mc1.20.jar` on a NeoForge boot and `create_ltab` on Minecraft 1.20.6 with
 * older mappings; both died as crashes and were scored as sideness evidence.
 *
 * The jar's own descriptor is the authority the platform metadata is not.
 */
internal class JarSelfDeclarationTest {

    private fun jar(dir: File, name: String, vararg entries: String): File =
        File(dir, name).also { file ->
            JarOutputStream(file.outputStream()).use { out ->
                entries.forEach { entry ->
                    out.putNextEntry(JarEntry(entry))
                    out.write("{}".toByteArray())
                    out.closeEntry()
                }
            }
        }

    @Test
    fun aJarNamesTheLoadersWhoseDescriptorsItCarries(@TempDir dir: File) {
        Assertions.assertEquals(setOf("Fabric"), JarSelfDeclaration.declaredLoaders(jar(dir, "f.jar", "fabric.mod.json")))
        Assertions.assertEquals(setOf("Quilt"), JarSelfDeclaration.declaredLoaders(jar(dir, "q.jar", "quilt.mod.json")))
        Assertions.assertEquals(setOf("Forge"), JarSelfDeclaration.declaredLoaders(jar(dir, "fo.jar", "META-INF/mods.toml")))
        Assertions.assertEquals(
            setOf("NeoForge"),
            JarSelfDeclaration.declaredLoaders(jar(dir, "n.jar", "META-INF/neoforge.mods.toml"))
        )
        Assertions.assertEquals(
            setOf("Forge", "NeoForge"),
            JarSelfDeclaration.declaredLoaders(jar(dir, "both.jar", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")),
            "a jar really can carry both, and then either boot is legitimate"
        )
    }

    /** The live NeoForge failure: a Forge jar, because one platform file claimed both loaders. */
    @Test
    fun aForgeJarIsRefusedForANeoForgeBoot(@TempDir dir: File) {
        val contradiction = JarSelfDeclaration.contradiction(
            jar(dir, "damagevignette-forge.jar", "META-INF/mods.toml"),
            loader = "NeoForge", minecraftVersion = "1.20.4", minecraftConstraint = null
        )

        Assertions.assertNotNull(contradiction)
        Assertions.assertTrue(contradiction!!.contains("NeoForge"), "the reason has to name the boot: $contradiction")
    }

    /** The live Fabric/Quilt failure: a jar whose mappings belong to another Minecraft. */
    @Test
    fun aJarIsRefusedForAMinecraftItDoesNotDeclare(@TempDir dir: File) {
        val contradiction = JarSelfDeclaration.contradiction(
            jar(dir, "create_ltab.jar", "fabric.mod.json"),
            loader = "Fabric", minecraftVersion = "1.20.6", minecraftConstraint = "1.20.1"
        )

        Assertions.assertNotNull(contradiction)
        Assertions.assertTrue(contradiction!!.contains("1.20.6") && contradiction.contains("1.20.1"), contradiction)
    }

    /** Quilt deliberately runs Fabric mods, so a Fabric descriptor is not a contradiction on a Quilt boot. */
    @Test
    fun aQuiltBootAcceptsAFabricJar(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "fabriconly.jar", "fabric.mod.json"),
                loader = "Quilt", minecraftVersion = "1.21.1", minecraftConstraint = null
            )
        )
    }

    /** The reverse does not hold: Fabric cannot load a Quilt-only mod, and pretending otherwise wastes a boot. */
    @Test
    fun aFabricBootDoesNotAcceptAQuiltOnlyJar(@TempDir dir: File) {
        Assertions.assertNotNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "quiltonly.jar", "quilt.mod.json"),
                loader = "Fabric", minecraftVersion = "1.21.1", minecraftConstraint = null
            )
        )
    }

    @Test
    fun aMatchingLoaderAndMinecraftAreAccepted(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "good.jar", "fabric.mod.json"),
                loader = "Fabric", minecraftVersion = "1.20.1", minecraftConstraint = ">=1.20"
            )
        )
    }

    /**
     * **The safety property, and the whole design.** A gate that refuses on doubt turns a gap in descriptor
     * coverage into a catalog-wide mass-INCONCLUSIVE event — the same shape `VersionConstraint`'s own doc
     * warns about, and the same shape a `LoaderSupportMemory` once produced by marking Fabric unusable for
     * 22 Minecraft versions inside minutes. Only a *positive, readable* contradiction may refuse.
     */
    @Test
    fun everythingUnreadableIsAccepted(@TempDir dir: File) {
        val unreadable = listOf(
            jar(dir, "nodescriptor.jar", "META-INF/MANIFEST.MF"),
            jar(dir, "empty.jar"),
            File(dir, "absent.jar"),
            File(dir, "notazip.jar").apply { writeText("I am not a zip archive") },
            File(dir, "adirectory.jar").apply { mkdirs() }
        )

        for (candidate in unreadable) {
            for (loader in listOf("Fabric", "Forge", "NeoForge", "Quilt", "LegacyFabric", "Whatever")) {
                Assertions.assertNull(
                    JarSelfDeclaration.contradiction(candidate, loader, "1.20.1", null),
                    "${candidate.name} on $loader must be booted, not refused — doubt is not a contradiction"
                )
            }
        }
    }

    /** A constraint nobody can parse is doubt, not contradiction — same rule, on the other input. */
    @Test
    fun anUnreadableMinecraftConstraintIsAccepted(@TempDir dir: File) {
        val jar = jar(dir, "odd.jar", "fabric.mod.json")
        val nonsense = listOf("", "   ", "*", "whatever", "not-a-version", "${'$'}{version}", "[", "]", ">=", "~")

        for (constraint in nonsense) {
            Assertions.assertNull(
                JarSelfDeclaration.contradiction(jar, "Fabric", "1.20.1", constraint),
                "'$constraint' is unreadable, so it must accept rather than refuse"
            )
        }
    }

    /** An unknown loader is doubt about *our* vocabulary, not about the jar. */
    @Test
    fun anUnrecognisedLoaderIsAccepted(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "f.jar", "fabric.mod.json"), loader = "SomeNewLoader",
                minecraftVersion = "1.20.1", minecraftConstraint = null
            )
        )
    }

    /**
     * **NeoForge on Minecraft 1.20.1 *is* Forge, so a Forge jar is not a contradiction there.** NeoForge
     * 20.1.x forked Forge 47 and kept the `net.minecraftforge` packages, `javafml` and `META-INF/mods.toml`;
     * the package rename landed with 1.20.2, and from there the two are separate ecosystems. 1.20.1 is
     * therefore the whole band, not the start of one.
     *
     * The live false positive this pins: `Mantle-1.20.1-1.11.117.jar` is ticked Forge **and** NeoForge on
     * CurseForge and carries only `META-INF/mods.toml`, so the gate refused it as "not a NeoForge mod" and
     * the project published an `ERROR` row for a loader that runs it perfectly well — while the very same
     * file booted to a ready-line under Forge minutes earlier.
     */
    @Test
    fun aNeoForgeBootOnMinecraft1201AcceptsAForgeJar(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "Mantle-1.20.1-1.11.117.jar", "META-INF/mods.toml"),
                loader = "NeoForge", minecraftVersion = "1.20.1", minecraftConstraint = null
            )
        )
    }

    /** Above 1.20.1 the packages diverge, so the same jar is refused again — the band is one version wide. */
    @Test
    fun aNeoForgeBootAboveMinecraft1201StillRefusesAForgeJar(@TempDir dir: File) {
        val forgeOnly = jar(dir, "forgeonly.jar", "META-INF/mods.toml")

        for (minecraftVersion in listOf("1.20.2", "1.20.4", "1.20.6", "1.21.1", "26.2")) {
            Assertions.assertNotNull(
                JarSelfDeclaration.contradiction(forgeOnly, "NeoForge", minecraftVersion, null),
                "NeoForge $minecraftVersion renamed its packages away from Forge's and cannot load this jar"
            )
        }
    }

    /**
     * And the concession is one-way, like every other entry: Forge never gained the ability to read
     * `META-INF/neoforge.mods.toml`, so a NeoForge-only jar is refused on a Forge boot at 1.20.1 too.
     */
    @Test
    fun aForgeBootDoesNotAcceptANeoForgeOnlyJarOnMinecraft1201(@TempDir dir: File) {
        Assertions.assertNotNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "neoforgeonly.jar", "META-INF/neoforge.mods.toml"),
                loader = "Forge", minecraftVersion = "1.20.1", minecraftConstraint = null
            )
        )
    }
}
