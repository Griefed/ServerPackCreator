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
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Verifies that [MetadataScanner] reads declared sideness out of a real jar through SPC's own
 * scanners. Uses the offline [ApiWrapper] (cached version-manifests) like the API's `ModScannerTest`.
 */
internal class MetadataScannerTest {

    private val scanner = MetadataScanner(
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).modScanner
    )

    /** Write a minimal Fabric mod-jar containing only the given `fabric.mod.json` body. */
    private fun fabricJar(directory: File, fileName: String, fabricModJson: String): File {
        val jar = File(directory, fileName)
        JarOutputStream(jar.outputStream()).use { jarStream ->
            jarStream.putNextEntry(JarEntry("fabric.mod.json"))
            jarStream.write(fabricModJson.toByteArray())
            jarStream.closeEntry()
        }
        return jar
    }

    @Test
    fun detectsClientOnlyFabricMod(@TempDir tempDir: File) {
        val jar = fabricJar(tempDir, "clientmod-1.0.jar", """{"id":"clientmod","environment":"client"}""")
        Assertions.assertEquals(
            MetadataScanner.Result.CLIENT,
            scanner.scan(jar, "Fabric", "1.20.1")
        )
    }

    @Test
    fun treatsModWithoutClientEnvironmentAsServerOrBoth(@TempDir tempDir: File) {
        val jar = fabricJar(tempDir, "bothmod-1.0.jar", """{"id":"bothmod"}""")
        Assertions.assertEquals(
            MetadataScanner.Result.SERVER_OR_BOTH,
            scanner.scan(jar, "Fabric", "1.20.1")
        )
    }

    /** Write a minimal modern-Forge mod-jar declaring [side] on its `minecraft` dependency. */
    private fun forgeTomlJar(directory: File, fileName: String, modId: String, side: String): File {
        val jar = File(directory, fileName)
        JarOutputStream(jar.outputStream()).use { jarStream ->
            jarStream.putNextEntry(JarEntry("META-INF/mods.toml"))
            jarStream.write(
                """
                modLoader="javafml"
                loaderVersion="[40,)"
                license="MIT"
                [[mods]]
                modId="$modId"
                version="1.0.0"
                [[dependencies.$modId]]
                modId="minecraft"
                mandatory=true
                versionRange="[1.16.5,)"
                ordering="NONE"
                side="$side"
                """.trimIndent().toByteArray()
            )
            jarStream.closeEntry()
        }
        return jar
    }

    /**
     * Forge's scanner is chosen by Minecraft *era*, and Minecraft has two versioning schemes
     * (`1.x.y` and the newer `YY.x.y`), so the choice must not be made from the minor component
     * alone: `26.2`'s minor is `2`, which reads as the 1.2 era and would pick the annotation scanner
     * meant for 1.12-and-older. That scanner finds nothing in a modern jar and the mod comes back
     * SERVER_OR_BOTH, silently weakening the metadata signal the confidence model folds in.
     */
    @Test
    fun forgeScannerSelectionSpansBothMinecraftVersioningSchemes(@TempDir tempDir: File) {
        for (minecraftVersion in listOf("1.20.1", "26.2")) {
            val directory = File(tempDir, minecraftVersion).also { it.mkdirs() }
            Assertions.assertEquals(
                MetadataScanner.Result.CLIENT,
                scanner.scan(forgeTomlJar(directory, "clientmod-1.0.jar", "clientmod", "CLIENT"), "Forge", minecraftVersion),
                "Minecraft $minecraftVersion: a CLIENT-declaring mods.toml must be read as CLIENT"
            )
            Assertions.assertEquals(
                MetadataScanner.Result.SERVER_OR_BOTH,
                scanner.scan(forgeTomlJar(directory, "bothmod-1.0.jar", "bothmod", "BOTH"), "Forge", minecraftVersion),
                "Minecraft $minecraftVersion: a BOTH-declaring mods.toml must be read as SERVER_OR_BOTH"
            )
        }
    }
}
