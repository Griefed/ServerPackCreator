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
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Pins the unattended-boot levers: in-place key replacement (not `JAVA_ARGS`), append-if-absent, eula, and the offline force-fetch toggle. */
internal class PackVariablesTest {

    @Test
    fun replacesAnExistingKeyInPlaceWithoutTouchingSimilarKeys() {
        val before = "JAVA=java\nJAVA_ARGS=-Xmx4G\nWAIT_FOR_USER_INPUT=true\n"
        val after = PackVariables.set(before, "JAVA", "/opt/java-8/bin/java")
        Assertions.assertTrue(after.contains("JAVA=/opt/java-8/bin/java"))
        Assertions.assertTrue(after.contains("JAVA_ARGS=-Xmx4G"), "JAVA_ARGS must be untouched")
        Assertions.assertEquals(1, Regex("(?m)^JAVA=").findAll(after).count(), "no duplicate JAVA key")
    }

    @Test
    fun appendsAKeyThatIsAbsent() {
        val after = PackVariables.set("WAIT_FOR_USER_INPUT=true\n", "SERVERSTARTERJAR_FORCE_FETCH", "false")
        Assertions.assertTrue(after.trimEnd().endsWith("SERVERSTARTERJAR_FORCE_FETCH=false"))
    }

    @Test
    fun prepareUnattendedWritesEulaAndOfflineLevers(@TempDir dir: File) {
        File(dir, "variables.txt").writeText("JAVA=java\nWAIT_FOR_USER_INPUT=true\nSERVERSTARTERJAR_FORCE_FETCH=true\n")

        PackVariables.prepareUnattended(dir, "/opt/java-8/bin/java", offline = true)

        val vars = File(dir, "variables.txt").readText()
        Assertions.assertEquals("eula=true\n", File(dir, "eula.txt").readText())
        Assertions.assertTrue(vars.contains("WAIT_FOR_USER_INPUT=false"))
        Assertions.assertTrue(vars.contains("JAVA=/opt/java-8/bin/java"), "the resolved bundled JDK path is written verbatim")
        Assertions.assertTrue(vars.contains("SERVERSTARTERJAR_FORCE_FETCH=false"), "offline must not re-fetch server.jar")
    }

    @Test
    fun installBootKeepsForceFetchOn(@TempDir dir: File) {
        File(dir, "variables.txt").writeText("JAVA=java\nSERVERSTARTERJAR_FORCE_FETCH=true\n")

        PackVariables.prepareUnattended(dir, "/opt/java-21/bin/java", offline = false)

        Assertions.assertTrue(File(dir, "variables.txt").readText().contains("SERVERSTARTERJAR_FORCE_FETCH=true"),
            "the install boot still needs to fetch server.jar")
    }
}
