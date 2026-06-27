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
package de.griefed.serverpackcreator.grinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/** Pins the Minecraft → bundled-JDK mapping, especially the 1.20.4/1.20.5 Java-17→21 boundary. */
internal class JavaForMinecraftTest {

    @Test
    fun mapsEachEraToTheRightMajorJava() {
        mapOf(
            "1.7.10" to 8, "1.12.2" to 8, "1.16.5" to 8,
            "1.17" to 17, "1.17.1" to 17, "1.18.2" to 17, "1.19.4" to 17,
            "1.20" to 17, "1.20.1" to 17, "1.20.4" to 17,
            "1.20.5" to 21, "1.20.6" to 21, "1.21" to 21, "1.21.4" to 21
        ).forEach { (mc, java) ->
            Assertions.assertEquals(java, JavaForMinecraft.majorJava(mc), "Minecraft $mc")
        }
    }

    @Test
    fun javaPathPointsAtTheBundledJdk() {
        Assertions.assertEquals("/opt/java-8/bin/java", JavaForMinecraft.javaPath("1.12.2"))
        Assertions.assertEquals("/opt/java-17/bin/java", JavaForMinecraft.javaPath("1.20.1"))
        Assertions.assertEquals("/opt/java-21/bin/java", JavaForMinecraft.javaPath("1.21"))
    }
}
