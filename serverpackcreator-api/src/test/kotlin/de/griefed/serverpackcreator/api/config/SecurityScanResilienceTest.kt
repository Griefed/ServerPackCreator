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
package de.griefed.serverpackcreator.api.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that the malware scan cannot take generation down with it.
 *
 * Nekodetector is a third-party scanner resolved from **jitpack**, and it is an *optional safety net*, not
 * a precondition for building a server pack. Reported 2026-08-29: a host whose runtime classpath lacked
 * `SecurityScans` died with an unhandled `NoClassDefFoundError` straight out of `checkConfiguration`,
 * killing the generation coroutine (`Exception in thread "pool-5-thread-1"`). The user's modpack was fine;
 * the scanner was not there.
 *
 * Two things made it fatal, and both are pinned here:
 *  - `scanUsingNekodetector` caught `Exception`, and `NoClassDefFoundError` is an **`Error`**.
 *  - The failure happened while *resolving the call*, before any code inside that method ran — so no
 *    amount of catching **inside** it could ever have helped. The guard has to sit at the call site.
 */
internal class SecurityScanResilienceTest {

    /**
     * The reported failure exactly: the scanner class is absent, so touching it throws a `LinkageError`.
     * A missing optional scanner must cost the *scan*, never the generation.
     */
    @Test
    fun aScannerThatCannotBeLoadedDoesNotAbortTheConfigurationCheck(@TempDir dir: File) {
        val findings = ConfigurationHandler.nekodetectorFindings(dir.toPath()) {
            throw NoClassDefFoundError("de/griefed/serverpackcreator/api/utilities/SecurityScans")
        }

        Assertions.assertTrue(findings.isEmpty(), "an unavailable scanner reports no findings rather than throwing")
    }

    /** A scanner that loads but blows up mid-scan is the same class of problem, and must also be contained. */
    @Test
    fun aScannerThatThrowsMidScanDoesNotAbortTheConfigurationCheck(@TempDir dir: File) {
        val findings = ConfigurationHandler.nekodetectorFindings(dir.toPath()) {
            throw IllegalStateException("the scanner fell over")
        }

        Assertions.assertTrue(findings.isEmpty())
    }

    /**
     * The guard must not swallow the thing it exists to report. A scan that *works* and finds an infection
     * has to hand every finding back — this is the malware path, so a silently emptied result would be far
     * worse than the crash being fixed.
     */
    @Test
    fun realFindingsAreStillReported(@TempDir dir: File) {
        val infections = listOf("Nekodetector infections found!", "Stage 1 infections:", "evil.jar")

        Assertions.assertEquals(infections, ConfigurationHandler.nekodetectorFindings(dir.toPath()) { infections })
    }
}
