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

/**
 * Keeps `SPC_GRINDER_HOST` connected to the socket it names.
 *
 * The knob is worthless unless `main` actually hands it to the report server, and that join is the one part
 * of this feature no unit test can execute — `main` boots Docker. It is also precisely where the original
 * defect lived: `ReportServer` had accepted a `host` for as long as it had existed, and `main` simply never
 * passed one, so the report bound loopback and no reverse proxy could reach it. Reading `main`'s own text is
 * the only guard available for that, so it is the guard used, bounded to `main`'s body by [grinderMainBody].
 */
internal class ReportBindWiringTest {

    @Test
    fun theConfiguredBindHostReachesTheReportServer() {
        val body = grinderMainBody()

        val read = Regex("""val\s+(\w+)\s*=\s*env\("SPC_GRINDER_HOST"""").find(body)
            ?: Assertions.fail("main() no longer reads SPC_GRINDER_HOST")
        val variable = read.groupValues[1]

        // Across newlines: the construction is wrapped, and a single-line pattern would report it missing.
        val construction = Regex("""ReportServer\((.*?)\)\s*\.start\(\)""", RegexOption.DOT_MATCHES_ALL)
            .find(body) ?: Assertions.fail("main() no longer constructs a ReportServer")
        Assertions.assertTrue(
            construction.groupValues[1].contains("host = $variable"),
            "main() reads SPC_GRINDER_HOST into `$variable` but never passes it as ReportServer's host — " +
                "the report would bind loopback and stay unreachable through a reverse proxy. Construction " +
                "was: ${construction.value}"
        )
    }

    /** The default is loopback: the report has no authentication, so exposure must be an explicit act. */
    @Test
    fun theDefaultBindHostIsLoopback() {
        Assertions.assertTrue(
            grinderMainBody().contains("""env("SPC_GRINDER_HOST", "127.0.0.1")"""),
            "the report's bind address must default to loopback — it serves verdicts and the CSV export " +
                "to anyone who can reach the port"
        )
    }
}
