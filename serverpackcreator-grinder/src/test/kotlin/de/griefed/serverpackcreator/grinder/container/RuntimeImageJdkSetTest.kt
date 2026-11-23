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
package de.griefed.serverpackcreator.grinder.container

import de.griefed.serverpackcreator.clientside.boot.ImageJavaRuntimes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Holds `docker/Dockerfile`'s `/opt/java-<major>` links to [ImageJavaRuntimes.IMAGE_BUNDLED_MAJORS]. A major the
 * gate believes in but the image lacks fails every boot of that Minecraft line on a missing `java`; a major the image
 * ships but the gate omits is silently never ground.
 */
internal class RuntimeImageJdkSetTest {

    private val dockerfile = File("docker/Dockerfile")

    @Test
    fun theImageLinksExactlyTheMajorsTheGateBootsWith() {
        Assertions.assertTrue(dockerfile.isFile, "Dockerfile not found at ${dockerfile.absolutePath}")
        val linked = Regex("""\s/opt/java-(\d+)\b""")
            .findAll(dockerfile.readText().lineSequence().filter { "ln -s" in it }.joinToString("\n"))
            .map { it.groupValues[1].toInt() }
            .toSet()

        Assertions.assertEquals(ImageJavaRuntimes.IMAGE_BUNDLED_MAJORS, linked)
    }
}
