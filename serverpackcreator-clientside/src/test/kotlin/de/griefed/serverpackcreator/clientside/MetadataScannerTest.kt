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
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).modScanner
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
}
