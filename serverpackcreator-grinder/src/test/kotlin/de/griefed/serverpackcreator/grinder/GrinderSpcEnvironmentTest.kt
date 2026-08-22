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
package de.griefed.serverpackcreator.grinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that the daemon decides *where* SPC keeps its files, and decides it before anything can log.
 *
 * Both halves cost a real outage. The daemon pinned its `Preferences` node but never its home directory, so SPC
 * resolved one on its own — and for a source build (every locally built artifact: `version=dev`) that fallback is
 * the process working directory, which `systemd` sets to `/` unless the unit file says otherwise. Reproduced
 * 2026-08-22 by running the installed distribution from `/`: `log4j2.xml` could not be written and the daemon died
 * on `java.io.FileNotFoundException: /log4j2.xml`, having reached neither Docker nor a single verdict.
 *
 * Ordering is the other half, and it is why the claim cannot simply live somewhere in `main`: `ApiProperties` is
 * registered as log4j's own `ConfigurationFactory` (`@Plugin`), so the *first log statement in the process*
 * constructs one. Anything claimed after that first line is claimed too late — the stack in the reported crash
 * starts in `GrinderApplication.getLog`, before `main` had wired anything at all. That ordering cannot be observed
 * from a test in a JVM whose logging is long since initialised, so it is asserted against the source instead — the
 * same approach [ReadmeConfigurationTest] takes to documentation drift.
 */
internal class GrinderSpcEnvironmentTest {

    private val entryPoint =
        File("src/main/kotlin/de/griefed/serverpackcreator/grinder/GrinderApplication.kt")

    /**
     * Both claims must precede every `log` use in `main`, since the first one builds an `ApiProperties`.
     */
    @Test
    fun theSpcEnvironmentIsClaimedBeforeTheFirstLogStatement() {
        Assertions.assertTrue(entryPoint.isFile, "entry point not found at ${entryPoint.absolutePath}")
        val body = entryPoint.readText().substringAfter("fun main(args: Array<String>) {")

        val firstLog = body.indexOf("log.")
        Assertions.assertTrue(firstLog > 0, "no log statement found in main — did the entry point change shape?")

        for (claim in listOf("claimSpcPreferencesNode()", "pinSpcHomeDirectory(")) {
            val at = body.indexOf(claim)
            Assertions.assertTrue(at > 0, "main() no longer calls $claim")
            Assertions.assertTrue(
                at < firstLog,
                "$claim must come before main()'s first log statement: ApiProperties is log4j's " +
                    "ConfigurationFactory, so the first log line in the process builds one, and whatever it " +
                    "resolves is what the daemon runs on"
            )
        }
    }
}
