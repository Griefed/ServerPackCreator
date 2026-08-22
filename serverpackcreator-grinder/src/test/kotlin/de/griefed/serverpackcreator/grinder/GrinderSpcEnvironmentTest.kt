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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.settings.PathsConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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

    private var homeProperty: String? = null
    private var nodeProperty: String? = null

    /** Saves the SPC properties this suite's JVM runs on, so a test here cannot redirect another one's writes. */
    @BeforeEach
    fun rememberSpcProperties() {
        homeProperty = System.getProperty(PathsConfig.HOME_DIRECTORY_KEY)
        nodeProperty = System.getProperty(ApiProperties.PREFERENCES_NODE_PROPERTY)
    }

    /** Puts both properties back exactly as they were, including having been unset. */
    @AfterEach
    fun restoreSpcProperties() {
        homeProperty?.let { System.setProperty(PathsConfig.HOME_DIRECTORY_KEY, it) }
            ?: System.clearProperty(PathsConfig.HOME_DIRECTORY_KEY)
        nodeProperty?.let { System.setProperty(ApiProperties.PREFERENCES_NODE_PROPERTY, it) }
            ?: System.clearProperty(ApiProperties.PREFERENCES_NODE_PROPERTY)
    }

    /** With nothing configured, SPC's home is the daemon's own base — where the README says its logs are. */
    @Test
    fun theDaemonPinsSpcsHomeToItsOwnBase(@TempDir base: File) {
        System.clearProperty(PathsConfig.HOME_DIRECTORY_KEY)

        GrinderApplication.pinSpcHomeDirectory(base)

        Assertions.assertEquals(
            base.absolutePath,
            System.getProperty(PathsConfig.HOME_DIRECTORY_KEY),
            "the daemon must name SPC's home itself; left to resolve one, a source build takes the working " +
                "directory, which is `/` under systemd"
        )
    }

    /** An operator who names a home keeps it — the daemon fills a gap, it does not overrule a choice. */
    @Test
    fun anOperatorsOwnHomeIsNotOverruled(@TempDir base: File, @TempDir chosen: File) {
        System.setProperty(PathsConfig.HOME_DIRECTORY_KEY, chosen.absolutePath)

        GrinderApplication.pinSpcHomeDirectory(base)

        Assertions.assertEquals(
            chosen.absolutePath,
            System.getProperty(PathsConfig.HOME_DIRECTORY_KEY),
            "-D${PathsConfig.HOME_DIRECTORY_KEY} is the documented escape hatch and must win"
        )
    }

    /** A blank home is a misconfiguration, not a choice, and must be replaced rather than passed on to SPC. */
    @Test
    fun aBlankHomeIsTreatedAsUnset(@TempDir base: File) {
        System.setProperty(PathsConfig.HOME_DIRECTORY_KEY, "   ")

        GrinderApplication.pinSpcHomeDirectory(base)

        Assertions.assertEquals(base.absolutePath, System.getProperty(PathsConfig.HOME_DIRECTORY_KEY))
    }

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
