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
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration

/**
 * Pins the host-process runner's "cannot launch" contract — the one branch reachable without spawning a
 * real server. A pack directory with no `start.sh` must report [RunResult.NotStarted] (not a crash),
 * which [BootVerifier.outcomeFor] turns into INCONCLUSIVE. The grinder's container runner must mirror
 * this contract.
 */
internal class HostProcessServerRunnerTest {

    @Test
    fun reportsNotStartedWhenNoStartScript(@TempDir packDir: File) {
        val outcome = HostProcessServerRunner().run(packDir, Duration.ofSeconds(1)) { }
        Assertions.assertTrue(outcome is RunResult.NotStarted)
        Assertions.assertEquals("No start.sh in the generated server pack.", (outcome as RunResult.NotStarted).detail)
    }

    /**
     * The server runs a mod nobody vetted, so it must not inherit this process's credentials. The runner hands
     * `start.sh` an allowlisted environment built from the one it was given, which the fake server prints back.
     */
    @Test
    fun theServerSeesOnlyAnAllowlistedEnvironment(@TempDir packDir: File, @TempDir fakeHome: File) {
        File(packDir, "start.sh").writeText("#!/usr/bin/env bash\nenv\necho 'Done (0.1s)! For help, type \"help\"'\n")
        val parent = mapOf(
            "PATH" to System.getenv("PATH"),
            "HOME" to fakeHome.absolutePath,
            "CURSEFORGE_API_KEY" to "secret-key",
            "GITHUB_TOKEN" to "secret-token",
            "ACTIONS_RUNTIME_TOKEN" to "secret-runtime"
        )

        val outcome = HostProcessServerRunner(parent).run(packDir, Duration.ofSeconds(30)) { }

        val console = (outcome as RunResult.Completed).lines
        Assertions.assertTrue("HOME=${fakeHome.absolutePath}" in console, console.joinToString("\n"))
        Assertions.assertTrue(console.none { it.contains("secret-") }, console.joinToString("\n"))
    }

    @Test
    fun theAllowlistKeepsWhatAServerNeedsAndNothingElse() {
        val parent = mapOf(
            "PATH" to "/usr/bin", "HOME" to "/home/runner", "LANG" to "C.UTF-8", "JAVA_HOME" to "/jdk",
            "CURSEFORGE_API_KEY" to "k", "GITHUB_TOKEN" to "t", "AWS_SECRET_ACCESS_KEY" to "a"
        )

        Assertions.assertEquals(
            mapOf("PATH" to "/usr/bin", "HOME" to "/home/runner", "LANG" to "C.UTF-8", "JAVA_HOME" to "/jdk"),
            HostProcessServerRunner.serverEnvironment(parent)
        )
    }
}
