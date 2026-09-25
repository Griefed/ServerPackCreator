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
package de.griefed.serverpackcreator.plugin.servertest.core

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Boots a **real** server pack through its **real** start script, end to end.
 *
 * Everything else in this module is exercised against stand-ins, which is right for pinning logic and wrong
 * for answering the question this plugin exists to answer: does a generated pack actually come up when
 * ServerPackCreator launches it? That is a question about a shell script, a modloader installer, a network
 * download and a JVM, and no mock answers any of it.
 *
 * **Off by default** — it downloads a modloader and boots a Minecraft server, which is neither fast nor
 * offline. Run it deliberately:
 *
 * ```
 * ./gradlew :serverpackcreator-plugin-servertest:test --tests "*RealPackBootTest*" \
 *     -Dservertest.integration=true \
 *     -Dservertest.pack=/path/to/a/generated/server-pack
 * ```
 *
 * The pack is **copied** before it is touched, so the original keeps its mods, its world and its
 * `server.properties`. The copy's `mods/` is emptied: what is under test is the plugin's launch path, and a
 * pack whose mods crash the server would prove nothing about it either way.
 */
internal class RealPackBootTest {

    /** Ready-line, EULA answer and shutdown all have to happen inside this, installer download included. */
    private val bootBudget = TimeUnit.MINUTES.toMillis(12)

    /** Poll until [condition] holds, or fail naming what never happened. */
    private fun awaitUntil(what: String, timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) {
                return
            }
            Thread.sleep(250)
        }
        Assertions.fail<Unit>("Timed out after ${timeoutMillis}ms waiting for: $what")
    }

    /**
     * The whole path a Start button takes: discover the pack, take a port, borrow `server.properties`, run
     * the start script, answer Mojang's EULA on the console, reach ready, `stop`, and give everything back.
     */
    @Test
    fun aGeneratedPackBootsThroughItsOwnStartScriptAndStopsCleanly(@TempDir workspace: File) {
        Assumptions.assumeTrue(
            System.getProperty("servertest.integration") == "true",
            "Set -Dservertest.integration=true to boot a real server."
        )
        val source = System.getProperty("servertest.pack")?.let(::File)
        Assumptions.assumeTrue(source != null && source.isDirectory, "Set -Dservertest.pack to a server pack.")

        val pack = File(workspace, source!!.name).also { source.copyRecursively(it, overwrite = true) }
        File(pack, "mods").listFiles()?.forEach { it.delete() }

        val discovered = ServerPackCatalog(ObjectMapper()).packsIn(workspace).single()
        val kind = StartScriptKind.defaultFor()
        Assertions.assertTrue(
            kind in discovered.scriptsPresent,
            "The pack must carry this host's default script, but has ${discovered.scriptsPresent}."
        )

        // Through ServerLauncher rather than hand-wired, so this exercises the sequence the Start button
        // actually runs -- taking the ports, borrowing the properties, registering before starting, and
        // giving all of it back -- instead of a copy of it that could drift.
        val allocator = PortAllocator()
        val patch = ServerPropertiesPatch(pack)
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val states = Collections.synchronizedList(mutableListOf<SessionState>())
        val closed = IntArray(1)

        val outcome = ServerLauncher(allocator, SessionRegistry()).launch(
            pack = discovered,
            kind = kind,
            onLine = { line -> lines.add(line); println("[pack] $line") },
            onState = { states.add(it) },
            onClosed = { closed[0] = closed[0] + 1 }
        )
        val started = Assertions.assertInstanceOf(LaunchOutcome.Started::class.java, outcome)
        val port = started.port
        val session = started.session

        Assertions.assertTrue(
            patch.propertiesFile.readText().contains("server-port=$port"),
            "The borrowed properties must carry the allocated port."
        )

        try {
            session.start()

            // The script asks for the EULA on the console exactly as it would in a terminal, and answering it
            // here is the whole point of holding standard input open.
            awaitUntil("the EULA prompt", TimeUnit.MINUTES.toMillis(3)) {
                lines.any { it.contains("type 'I agree'") } || session.state == SessionState.Ready
            }
            if (session.state != SessionState.Ready) {
                Assertions.assertTrue(session.send("I agree"), "The console must accept the EULA answer.")
            }

            awaitUntil("the server to report itself ready", bootBudget) { session.state == SessionState.Ready }
            Assertions.assertTrue(
                File(pack, "eula.txt").isFile,
                "The script writes eula.txt itself once the user agrees; the plugin never writes it."
            )

            Assertions.assertTrue(session.stop(), "A ready server must accept a stop.")

            // A pack with WAIT_FOR_USER_INPUT=true ends on `read -n 1 -s -r -p "Press any key to continue"`,
            // and bash prints that prompt only to a terminal -- so over this pipe the script goes silent
            // instead. Observed on a real NeoForge boot before the console learned to say so. Answering it is
            // what a user does, guided by ConsoleHints.PRESS_ENTER_HINT.
            if (PackVariables.read(pack).waitsForUserInput) {
                awaitUntil("the script to reach its exit prompt", TimeUnit.MINUTES.toMillis(3)) {
                    lines.any { it.trim() == "Exiting..." } || session.state is SessionState.Exited
                }
                session.send("")
            }

            awaitUntil("the script to exit", TimeUnit.MINUTES.toMillis(3)) { session.state is SessionState.Exited }
        } finally {
            session.kill()
        }

        awaitUntil("the port and properties to be given back", TimeUnit.SECONDS.toMillis(30)) { closed[0] == 1 }
        Assertions.assertFalse(patch.backupFile.exists(), "The borrow must leave no backup behind.")
        Assertions.assertEquals(
            File(source, "server.properties").readText(),
            patch.propertiesFile.readText(),
            "server.properties must come back exactly as the pack shipped it."
        )
    }
}
