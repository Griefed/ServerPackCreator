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
package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.clientside.boot.BootVerifier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that `-verifyclientside` boots each pack on the Java its Minecraft version requires, by running the
 * post-processor the verb hands every `BootVerifier` against a staged pack and reading the `JAVA=` it wrote.
 * Uses SPC's real Minecraft metadata, from the API module's cached manifests, for the required majors.
 */
internal class VerifyClientsideJavaSelectionTest {

    private val minecraftMeta = ApiWrapper.api(
        File(File("").absoluteFile.parent, "serverpackcreator-api/build/resources/test/serverpackcreator.properties")
    ).versionMeta.minecraft

    private fun stagedPack(directory: File, minecraftVersion: String): BootVerifier.Prepared.Ready {
        val serverPack = File(directory, "serverpack").apply { mkdirs() }
        File(serverPack, "variables.txt").writeText("JAVA=java\nJAVA_ARGS=\nWAIT_FOR_USER_INPUT=true\n")
        return BootVerifier.Prepared.Ready(serverPack, File(directory, "boot.log"), minecraftVersion, "Forge", "47.2.0")
    }

    private fun javaOf(pack: BootVerifier.Prepared.Ready): String? =
        File(pack.serverPack, "variables.txt").readLines().firstOrNull { it.startsWith("JAVA=") }?.removePrefix("JAVA=")

    @Test
    fun aPackIsPointedAtTheJdkTheHostAdvertisesForItsMinecraftVersion(@TempDir directory: File) {
        val selection = VerifyClientsideCommand.packJavaSelection(
            minecraftMeta,
            environment = mapOf("JAVA_HOME_17_X64" to "/jdks/17", "PATH" to "/usr/bin"),
            runningJavaMajor = 21,
            runningJavaHome = File("/jdks/21")
        )
        val pack = stagedPack(directory, "1.20.1")

        selection.invoke(pack)

        Assertions.assertEquals(File(File("/jdks/17"), "bin/java").path, javaOf(pack))
    }

    @Test
    fun theRunningJvmServesTheVersionsItsMajorCovers(@TempDir directory: File) {
        val selection = VerifyClientsideCommand.packJavaSelection(
            minecraftMeta, environment = emptyMap(), runningJavaMajor = 21, runningJavaHome = File("/jdks/21")
        )
        val pack = stagedPack(directory, "1.21.1")

        selection.invoke(pack)

        Assertions.assertEquals(File(File("/jdks/21"), "bin/java").path, javaOf(pack))
    }

    @Test
    fun aVersionWhoseJdkNobodyProvidesIsRefusedNamingIt(@TempDir directory: File) {
        val selection = VerifyClientsideCommand.packJavaSelection(
            minecraftMeta,
            environment = mapOf("JAVA_HOME_17_X64" to "/jdks/17"),
            runningJavaMajor = 21,
            runningJavaHome = File("/jdks/21")
        )

        val failure = assertThrows<IllegalStateException> { selection.invoke(stagedPack(directory, "1.12.2")) }

        Assertions.assertTrue(failure.message.orEmpty().contains("Java 8"), failure.message)
    }
}
