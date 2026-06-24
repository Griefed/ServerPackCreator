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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Branch coverage for [ServerPackProvisioner] that complements the existing characterization tests
 * (which cover the default/missing icon, the scaled-custom icon, the default/missing properties and
 * the basic Java/restart placeholder replacement). Added here: copying an existing custom
 * server.properties, the already-64x64 icon direct-copy (no scaling) path, and the generic-key and
 * Windows-path-escaping branches of placeholder replacement.
 */
internal class ServerPackProvisionerTest {
    private val api = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val provisioner = api.serverPackHandler.provisioner

    /**
     * An existing custom server.properties is copied verbatim into the server pack.
     */
    @Test
    fun copyPropertiesCopiesExistingCustomFile(@TempDir tempDir: File) {
        val custom = File(tempDir, "custom.properties")
        custom.writeText("motd=Hello from a custom properties file")
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        provisioner.copyProperties(destination.absolutePath, custom.absolutePath)

        val copied = File(destination, apiProperties.defaultServerProperties.name)
        Assertions.assertTrue(copied.isFile, "Custom properties must be copied")
        Assertions.assertEquals(custom.readText(), copied.readText())
    }

    /**
     * A custom icon that is already 64x64 is copied directly, without going through the scaling
     * path.
     */
    @Test
    fun copyIconCopiesAlready64x64IconWithoutScaling(@TempDir tempDir: File) {
        val icon = File(tempDir, "icon64.png")
        ImageIO.write(BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", icon)
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        provisioner.copyIcon(destination.absolutePath, icon.absolutePath)

        val copied = File(destination, apiProperties.defaultServerIcon.name)
        Assertions.assertTrue(copied.isFile, "64x64 icon must be copied")
        val image = ImageIO.read(copied)
        Assertions.assertEquals(64, image.width)
        Assertions.assertEquals(64, image.height)
    }

    /**
     * For a local server pack, a Windows-style Java path is escaped (colons and backslashes), and
     * generic placeholders are replaced with their configured values.
     */
    @Test
    fun replacePlaceholdersEscapesWindowsPathAndReplacesGenericKey() {
        val scriptSettings = hashMapOf(
            "SPC_JAVA_SPC" to "C:\\Program Files\\Java\\bin\\java.exe",
            "SPC_FOO_SPC" to "bar"
        )
        val result = provisioner.replacePlaceholders(true, "JAVA=SPC_JAVA_SPC FOO=SPC_FOO_SPC", scriptSettings)

        Assertions.assertTrue(result.contains("C\\:"), "Local Java path must have its colon escaped; got $result")
        Assertions.assertTrue(result.contains("FOO=bar"), "Generic placeholder must be replaced; got $result")
    }

    /**
     * For a zipped server pack, the Java-placeholder collapses to plain "java" regardless of the
     * configured path.
     */
    @Test
    fun replacePlaceholdersUsesPlainJavaForZippedPack() {
        val scriptSettings = hashMapOf("SPC_JAVA_SPC" to "C:\\Program Files\\Java\\bin\\java.exe")
        val result = provisioner.replacePlaceholders(false, "JAVA=SPC_JAVA_SPC", scriptSettings)

        Assertions.assertEquals("JAVA=java", result)
    }
}
