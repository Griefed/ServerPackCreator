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
package de.griefed.serverpackcreator.grinder.container

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectContainerCmd
import de.griefed.serverpackcreator.grinder.report.capturingLogsOf
import org.apache.logging.log4j.Level
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

/**
 * Pins what a failed exit-code inspect leaves behind: no exit code, and a WARN carrying the daemon's failure.
 *
 * `BootLogClassifier` reads a `null` exit code as "nothing recognised and no exit status", i.e. INCONCLUSIVE at
 * the `EXIT_CODE` rung — the honest reading of missing evidence, and never a published exclusion. What it must
 * not be is silent: a boot that really exited non-zero then reads INCONCLUSIVE rather than CRASHED, which skips
 * the crash re-checks and drops the killed/OOM guard's exit-code half, and nothing in the journal says why.
 */
internal class ExitCodeInspectFailureTest {

    /** A docker-java client whose every container inspect fails, as an unreachable or restarting daemon does. */
    private fun clientWhoseInspectFails(failure: RuntimeException): DockerClient {
        val inspect = Proxy.newProxyInstance(
            InspectContainerCmd::class.java.classLoader,
            arrayOf(InspectContainerCmd::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "exec" -> throw failure
                else -> throw UnsupportedOperationException(method.name)
            }
        }
        return Proxy.newProxyInstance(
            DockerClient::class.java.classLoader,
            arrayOf(DockerClient::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "inspectContainerCmd" -> inspect
                else -> throw UnsupportedOperationException(method.name)
            }
        } as DockerClient
    }

    @Test
    fun aFailedInspectYieldsNoExitCodeAndSaysSo() {
        val engine = DockerJavaContainerEngine(clientWhoseInspectFails(IllegalStateException("daemon went away")))

        val exitCode = capturingLogsOf(DockerJavaContainerEngine::class.java) { log ->
            engine.exitCodeOf("abc123").also {
                Assertions.assertTrue(
                    log.hasThrowableAt(Level.WARN),
                    "a lost exit code changes the boot's reading, so the daemon's failure must reach the journal"
                )
            }
        }

        Assertions.assertNull(exitCode, "an exit code the daemon would not report is missing evidence, never a guess")
    }
}
