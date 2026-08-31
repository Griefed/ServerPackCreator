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
 * Pins that a scan reports **which Minecraft the jar itself says it needs**.
 *
 * Every scanner already parses that constraint and then throws it away: Fabric and Quilt exclude `minecraft`
 * from `dependencies` as "the platform", and the Forge/NeoForge tomls route the platform entry into the
 * sideness inference instead. So nothing downstream could ask the one question that separates a real
 * clientside crash from a jar booted on the wrong Minecraft.
 *
 * Why it is needed: the grinder boots the **newest** Minecraft a platform declares for a file, without ever
 * asking what the jar was built for. Measured 2026-08-31, that put `create_ltab` on 1.20.6 with 1.20.5-era
 * mappings and `debugify` on 1.19.1 with another 1.19.x's, and both mixin failures were scored CRASHED.
 */
internal class MinecraftConstraintTest {

    private val modScanner = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).modScanner

    private fun jarContaining(dir: File, jarName: String, entry: String, content: String): File =
        File(dir, jarName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry(entry))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }

    @Test
    fun fabricReportsTheMinecraftItDeclares(@TempDir tempDir: File) {
        val descriptor = """
            {"schemaVersion":1,"id":"pinned","version":"1.0.0","environment":"*",
             "depends":{"minecraft":"~1.20.1","fabricloader":">=0.14"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "pinned.jar", "fabric.mod.json", descriptor)

        Assertions.assertEquals("~1.20.1", modScanner.fabricScanner.scan(listOf(jar)).single().minecraftConstraint)
    }

    @Test
    fun quiltReportsTheMinecraftItDeclares(@TempDir tempDir: File) {
        val descriptor = """
            {"schema_version":1,
             "quilt_loader":{"id":"pinnedquilt","version":"1.0.0",
               "depends":[{"id":"minecraft","versions":">=1.21"},"quilt_base"]},
             "minecraft":{"environment":"*"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "pinnedquilt.jar", "quilt.mod.json", descriptor)

        Assertions.assertEquals(">=1.21", modScanner.quiltScanner.scan(listOf(jar)).single().minecraftConstraint)
    }

    /**
     * Forge states it as a Maven range on the platform dependency — the same entry whose `side` becomes the
     * mod's own sideness, so the constraint has to be captured *without* disturbing that inference.
     */
    @Test
    fun forgeReportsTheMinecraftRangeWithoutLosingItsSideness(@TempDir tempDir: File) {
        val toml = """
            modLoader="javafml"
            loaderVersion="[40,)"
            license="MIT"
            [[mods]]
            modId="pinnedforge"
            [[dependencies.pinnedforge]]
            modId="minecraft"
            side="CLIENT"
            versionRange="[1.19.2,1.20)"
            [[dependencies.pinnedforge]]
            modId="forge"
            side="BOTH"
            versionRange="[43,)"
        """.trimIndent()
        val jar = jarContaining(tempDir, "pinnedforge.jar", "META-INF/mods.toml", toml)

        val scanned = modScanner.forgeTomlScanner.scan(listOf(jar)).single()

        Assertions.assertEquals("[1.19.2,1.20)", scanned.minecraftConstraint)
        Assertions.assertEquals(
            Sideness.CLIENT, scanned.sideness,
            "capturing the range must not disturb the sideness the same entry's side= decides"
        )
    }

    /** A descriptor that states nothing about Minecraft reports `null`, never an invented range. */
    @Test
    fun aDescriptorStatingNoMinecraftReportsNull(@TempDir tempDir: File) {
        val descriptor = """{"schemaVersion":1,"id":"unpinned","version":"1.0.0","environment":"*"}"""
        val jar = jarContaining(tempDir, "unpinned.jar", "fabric.mod.json", descriptor)

        Assertions.assertNull(modScanner.fabricScanner.scan(listOf(jar)).single().minecraftConstraint)
    }

    /** The platform entries stay out of `dependencies` — this is additive, not a change to what was there. */
    @Test
    fun theMinecraftEntryIsStillNotADependency(@TempDir tempDir: File) {
        val descriptor = """
            {"schemaVersion":1,"id":"pinned","version":"1.0.0","environment":"*",
             "depends":{"minecraft":"~1.20.1","fabricloader":">=0.14","jei":"*"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "pinned2.jar", "fabric.mod.json", descriptor)

        Assertions.assertEquals(
            listOf("jei"), modScanner.fabricScanner.scan(listOf(jar)).single().dependencies.map { it.modID }
        )
    }
}
