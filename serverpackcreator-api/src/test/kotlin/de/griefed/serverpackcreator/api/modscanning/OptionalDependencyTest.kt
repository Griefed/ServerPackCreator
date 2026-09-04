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
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that a scan reports **whether the descriptor actually demanded a dependency**, rather than treating
 * every declared entry as required.
 *
 * The two loader families spell it differently and both were being ignored: Forge's `mods.toml` uses
 * `mandatory = true|false`, while NeoForge's `neoforge.mods.toml` replaced that with `type`, a string
 * defaulting to `"required"` and also taking `"optional"`, `"incompatible"` and `"discouraged"`. Neither
 * word appeared anywhere in this module, so `ModDependency` could not carry the distinction and every
 * consumer necessarily read an optional dependency as a hard requirement.
 *
 * Measured on `advancement-plaques` 1.7.2 for Forge / Minecraft 26.2, whose own `META-INF/mods.toml` declares
 * `iceberg` with `mandatory=true` and both `prism` and `toastcontrol` with `mandatory=false` — and Modrinth
 * agrees, listing prism (`1OE8wbN0`) as `optional` and iceberg (`5faXoLqX`) as `required`. The grinder
 * refused to boot it with *"Required dependency unavailable … prism"*, which cost a `BootResult.INCONCLUSIVE`
 * on a mod that never required prism at all.
 *
 * **Absent means required, deliberately.** That is NeoForge's own documented default, and it is the safe
 * direction here: wrongly treating a required dependency as optional boots a mod without something it needs,
 * which fails as a crash and can publish a *wrong* verdict, whereas wrongly treating an optional one as
 * required only refuses the boot and learns nothing.
 */
internal class OptionalDependencyTest {

    // The PROCESSED copy under build/, never src/test/resources -- see MinecraftConstraintTest for why.
    private val modScanner = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).modScanner

    private fun jarContaining(dir: File, jarName: String, entry: String, content: String): File =
        File(dir, jarName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry(entry))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }

    /** The dependency [modId] the scan recorded, so an assertion can name what it is checking. */
    private fun ScannedMod.dependency(modId: String) =
        dependencies.firstOrNull { it.modID.equals(modId, ignoreCase = true) }

    /**
     * `advancement-plaques`' real descriptor, verbatim in shape: one genuinely required dependency and two
     * the author explicitly marked `mandatory=false`.
     */
    @Test
    fun forgeMarksAMandatoryFalseDependencyOptional(@TempDir tempDir: File) {
        val toml = """
            modLoader="javafml"
            loaderVersion="[65,)"
            license="MIT"
            [[mods]]
            modId="advancementplaques"
            [[dependencies.advancementplaques]]
                modId="minecraft"
                mandatory=true
                versionRange="[26.2,)"
                side="BOTH"
            [[dependencies.advancementplaques]]
                modId="iceberg"
                mandatory=true
                versionRange="[1.4.2,)"
                side="BOTH"
            [[dependencies.advancementplaques]]
                modId="prism"
                mandatory=false
                versionRange="[1.1.2,)"
                side="BOTH"
            [[dependencies.advancementplaques]]
                modId="toastcontrol"
                mandatory=false
                versionRange="[9.0,)"
                side="BOTH"
        """.trimIndent()
        val jar = jarContaining(tempDir, "AdvancementPlaques-26.2-forge-1.7.2.jar", "META-INF/mods.toml", toml)

        val scanned = modScanner.forgeTomlScanner.scan(listOf(jar)).single()

        Assertions.assertEquals(false, scanned.dependency("iceberg")?.optional, "iceberg is mandatory=true")
        Assertions.assertEquals(true, scanned.dependency("prism")?.optional, "prism is mandatory=false")
        Assertions.assertEquals(true, scanned.dependency("toastcontrol")?.optional, "toastcontrol is mandatory=false")
    }

    /** A dependency stating nothing is required — NeoForge's documented default, and the safe direction. */
    @Test
    fun aDependencyDeclaringNeitherFieldIsRequired(@TempDir tempDir: File) {
        val toml = """
            modLoader="javafml"
            loaderVersion="[65,)"
            license="MIT"
            [[mods]]
            modId="terse"
            [[dependencies.terse]]
                modId="somelib"
                versionRange="[1,)"
                side="BOTH"
        """.trimIndent()
        val jar = jarContaining(tempDir, "terse.jar", "META-INF/mods.toml", toml)

        Assertions.assertEquals(
            false, modScanner.forgeTomlScanner.scan(listOf(jar)).single().dependency("somelib")?.optional,
            "a dependency that says nothing must stay required — treating it as optional risks a wrong verdict"
        )
    }

    /**
     * NeoForge dropped `mandatory` entirely for `type`, so a scanner reading only the boolean sees every
     * modern NeoForge dependency as required.
     */
    @Test
    fun neoForgeReadsTheTypeFieldInsteadOfMandatory(@TempDir tempDir: File) {
        val toml = """
            modLoader="javafml"
            loaderVersion="[4,)"
            license="MIT"
            [[mods]]
            modId="neomod"
            [[dependencies.neomod]]
                modId="hardlib"
                type="required"
                versionRange="[1,)"
                side="BOTH"
            [[dependencies.neomod]]
                modId="softlib"
                type="optional"
                versionRange="[1,)"
                side="BOTH"
        """.trimIndent()
        val jar = jarContaining(tempDir, "neomod.jar", "META-INF/neoforge.mods.toml", toml)

        val scanned = modScanner.neoForgeTomlScanner.scan(listOf(jar)).single()

        Assertions.assertEquals(false, scanned.dependency("hardlib")?.optional, """type="required"""")
        Assertions.assertEquals(true, scanned.dependency("softlib")?.optional, """type="optional"""")
    }

    /**
     * `"incompatible"` and `"discouraged"` are the other two `type` values. Neither is a thing to go and
     * fetch — incompatible means the mod must *not* be present — so neither may be treated as required.
     */
    @Test
    fun neoForgeTreatsIncompatibleAndDiscouragedAsNotRequired(@TempDir tempDir: File) {
        val toml = """
            modLoader="javafml"
            loaderVersion="[4,)"
            license="MIT"
            [[mods]]
            modId="picky"
            [[dependencies.picky]]
                modId="enemy"
                type="incompatible"
                side="BOTH"
            [[dependencies.picky]]
                modId="frowned"
                type="discouraged"
                side="BOTH"
        """.trimIndent()
        val jar = jarContaining(tempDir, "picky.jar", "META-INF/neoforge.mods.toml", toml)

        val scanned = modScanner.neoForgeTomlScanner.scan(listOf(jar)).single()

        Assertions.assertEquals(true, scanned.dependency("enemy")?.optional, "an incompatible mod is never fetched")
        Assertions.assertEquals(true, scanned.dependency("frowned")?.optional, "discouraged is not required")
    }
}
