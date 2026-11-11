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
package de.griefed.serverpackcreator.clientside.boot

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins what a host boot does to a staged pack before `start.sh` runs: point `JAVA` at the JDK the pack's
 * Minecraft version requires, or refuse with the version it needed. Booting on any other Java ends in the start
 * script's Jabba prompt, which a boot without a terminal can only decline.
 */
internal class HostJavaSelectionTest {

    private val requiredJava = mapOf("1.12.2" to 8, "1.20.1" to 17, "1.21.1" to 21)

    private val runtimes = ImageJavaRuntimes.installed(
        requiredJavaMajor = { requiredJava[it] },
        javaHomes = mapOf(17 to File("/jdks/17"), 21 to File("/jdks/21"))
    )

    private fun stagedPack(dir: File, minecraftVersion: String): BootVerifier.Prepared.Ready {
        val serverPack = File(dir, "serverpack").apply { mkdirs() }
        File(serverPack, "variables.txt").writeText("JAVA=java\nJAVA_ARGS=\nWAIT_FOR_USER_INPUT=true\n")
        return BootVerifier.Prepared.Ready(serverPack, File(dir, "boot.log"), minecraftVersion, "Forge", "47.2.0")
    }

    @Test
    fun pointsThePackAtTheJdkItsMinecraftVersionRequires(@TempDir dir: File) {
        val pack = stagedPack(dir, "1.20.1")

        HostJavaSelection(runtimes).invoke(pack)

        val variables = File(pack.serverPack, "variables.txt").readText()
        Assertions.assertTrue(variables.lines().contains("JAVA=${File(File("/jdks/17"), "bin/java").path}"), variables)
        Assertions.assertTrue(variables.lines().contains("WAIT_FOR_USER_INPUT=false"), variables)
        Assertions.assertEquals("eula=true\n", File(pack.serverPack, "eula.txt").readText())
    }

    @Test
    fun aMissingJdkIsRefusedNamingTheJavaItNeeds(@TempDir dir: File) {
        val failure = assertThrows<IllegalStateException> { HostJavaSelection(runtimes).invoke(stagedPack(dir, "1.12.2")) }

        Assertions.assertTrue(failure.message.orEmpty().contains("Java 8"), failure.message)
        Assertions.assertTrue(failure.message.orEmpty().contains("JAVA_HOME_8_"), "says how to provide it: ${failure.message}")
    }

    @Test
    fun anUnknownRequirementIsRefusedAsUnknown(@TempDir dir: File) {
        val failure = assertThrows<IllegalStateException> { HostJavaSelection(runtimes).invoke(stagedPack(dir, "99.99")) }

        Assertions.assertTrue(failure.message.orEmpty().contains("99.99"), failure.message)
        Assertions.assertFalse(failure.message.orEmpty().contains("JAVA_HOME_"), "no JDK could fix this: ${failure.message}")
    }
}
