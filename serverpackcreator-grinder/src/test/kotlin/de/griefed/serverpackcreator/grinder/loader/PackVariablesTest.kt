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
package de.griefed.serverpackcreator.grinder.loader

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

    /**
     * `JAVA_INSTALLER` is written only when supplied, and never disturbs `JAVA` — the server keeps its own
     * (possibly older) JDK while modloader installers that need Java 17+ get a newer one.
     */
    @Test
    fun writesTheInstallerJavaOverrideOnlyWhenGiven(@TempDir dir: File) {
        File(dir, "variables.txt").writeText("JAVA=java\n")
        PackVariables.prepareUnattended(dir, "/opt/java-8/bin/java", offline = true)
        Assertions.assertFalse(
            File(dir, "variables.txt").readText().contains("JAVA_INSTALLER"),
            "absent by default, so existing packs are untouched"
        )

        PackVariables.prepareUnattended(dir, "/opt/java-8/bin/java", offline = true, installerJavaPath = "/opt/java-21/bin/java")
        val vars = File(dir, "variables.txt").readText()
        Assertions.assertTrue(vars.contains("JAVA_INSTALLER=/opt/java-21/bin/java"))
        Assertions.assertTrue(vars.contains("JAVA=/opt/java-8/bin/java"), "the server's Java must stay as-is")
        Assertions.assertEquals(1, Regex("(?m)^JAVA=").findAll(vars).count(), "no duplicate JAVA key")
    }

    /**
     * Forge must launch from the installer's own argfile, not through the ServerStarterJar.
     *
     * `USE_SSJ` defaults to `true` and `HELP.md` already records the incompatibility it exists to work around —
     * "people ran into trouble when using Forge and Minecraft 1.20.2 and 1.20.3". A human reads that and flips
     * the knob; an unattended grinder never can, so every Forge 1.20.2/1.20.3 candidate booted a server that
     * died in `BootstrapLauncher` and learned nothing about the mod (`CurseForge-ars-nouveau-Forge.log`,
     * 2026-08-23). NeoForge is unaffected either way — the templates only consult this in `setupForge`.
     */
    @Test
    fun forgeIsLaunchedFromItsArgfileRatherThanTheStarterJar(@TempDir dir: File) {
        File(dir, "variables.txt").writeText("JAVA=java\nUSE_SSJ=true\n")

        PackVariables.prepareUnattended(dir, "/opt/java-17/bin/java", offline = true)

        val vars = File(dir, "variables.txt").readText()
        Assertions.assertTrue(vars.contains("USE_SSJ=false"), "the grinder cannot flip this knob by hand: $vars")
        Assertions.assertEquals(1, Regex("(?m)^USE_SSJ=").findAll(vars).count(), "no duplicate USE_SSJ key")
    }

    /** Set on the install boot too, so the cached layer is the one the offline boot then launches from. */
    @Test
    fun theInstallBootAlsoAvoidsTheStarterJarForForge(@TempDir dir: File) {
        File(dir, "variables.txt").writeText("JAVA=java\n")

        PackVariables.prepareUnattended(dir, "/opt/java-17/bin/java", offline = false)

        Assertions.assertTrue(
            File(dir, "variables.txt").readText().contains("USE_SSJ=false"),
            "installing one way and booting the other would cache a layer the boot cannot use"
        )
    }

    @Test
    fun installBootKeepsForceFetchOn(@TempDir dir: File) {
        File(dir, "variables.txt").writeText("JAVA=java\nSERVERSTARTERJAR_FORCE_FETCH=true\n")

        PackVariables.prepareUnattended(dir, "/opt/java-21/bin/java", offline = false)

        Assertions.assertTrue(File(dir, "variables.txt").readText().contains("SERVERSTARTERJAR_FORCE_FETCH=true"),
            "the install boot still needs to fetch server.jar")
    }
}
