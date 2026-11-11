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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that `-verifyclientside` boots each pack on the Java its Minecraft version requires.
 *
 * Asserted against the source for the reason `VerifyClientsideArtifactSinkTest` gives: a run needs an
 * `ApiWrapper`, a real generation and a booting server. Losing the wiring is silent in the same way — every
 * pack would run on whatever `java` is on `PATH`, and each Minecraft line needing another major would end in
 * `start.sh`'s Jabba prompt instead of booting.
 */
internal class VerifyClientsideJavaSelectionTest {

    private val source =
        File("src/main/kotlin/de/griefed/serverpackcreator/app/cli/commands/VerifyClientsideCommand.kt")

    @Test
    fun everyBootedPackIsPointedAtTheJavaItsMinecraftVersionRequires() {
        Assertions.assertTrue(source.isFile, "VerifyClientsideCommand.kt not found at ${source.absolutePath}")
        val code = source.readText()
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""//.*"""), "")

        Assertions.assertTrue(
            Regex("""packPostProcessor\s*=\s*HostJavaSelection\(""").containsMatchIn(code),
            "the verb's BootVerifier must apply HostJavaSelection to every staged pack"
        )
        Assertions.assertTrue(
            code.contains("ImageJavaRuntimes.javaHomesIn(System.getenv())"),
            "the JDKs a CI runner advertises must be offered, or only the running JVM's major can boot"
        )
    }
}
