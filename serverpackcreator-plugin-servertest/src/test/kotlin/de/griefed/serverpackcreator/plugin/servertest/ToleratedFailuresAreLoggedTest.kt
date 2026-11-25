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
package de.griefed.serverpackcreator.plugin.servertest

import de.griefed.serverpackcreator.plugin.servertest.gui.copyToClipboard
import de.griefed.serverpackcreator.plugin.servertest.session.ServerSession
import de.griefed.serverpackcreator.plugin.servertest.session.SessionRegistry
import io.mockk.every
import io.mockk.mockk
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.HeadlessException
import java.io.File
import java.util.Collections

/**
 * Pins that what the plugin tolerates without failing — a throwing console or close listener, a server that
 * cannot be killed at shutdown, a clipboard it cannot write — still leaves a WARN in the add-ons log naming
 * what was affected.
 */
internal class ToleratedFailuresAreLoggedTest {

    /**
     * The WARN messages the add-ons log records while [block] runs, waiting up to five seconds afterwards for
     * [settled] to hold, because a session's callbacks finish on its reader thread.
     */
    private fun warningsDuring(settled: (List<String>) -> Boolean = { true }, block: () -> Unit): List<String> {
        val target = LogManager.getLogger("AddonsLogger") as Logger
        val events = Collections.synchronizedList(mutableListOf<LogEvent>())
        val appender = object : AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
            override fun append(event: LogEvent) {
                if (event.level == Level.WARN) {
                    events.add(event.toImmutable())
                }
            }
        }.apply { start() }
        target.addAppender(appender)
        try {
            block()
            val deadline = System.currentTimeMillis() + 5_000
            while (!settled(events.map { it.message.formattedMessage }) && System.currentTimeMillis() < deadline) {
                Thread.sleep(25)
            }
        } finally {
            target.removeAppender(appender)
            appender.stop()
        }
        return events.map { it.message.formattedMessage }
    }

    private fun requireBash() =
        Assumptions.assumeTrue(File("/bin/bash").canExecute(), "Needs bash to run a stand-in start script.")

    /** Run a script printing three lines and exiting, and wait for the session to close. */
    private fun runToTheEnd(packDir: File, onLine: (String) -> Unit, onClosed: () -> Unit) {
        File(packDir, "start.sh").writeText("#!/usr/bin/env bash\necho one\necho two\necho three\n")
        val closed = java.util.concurrent.CountDownLatch(1)
        val session = ServerSession(packDir, listOf("bash", "start.sh"), onLine, {}, {
            try {
                onClosed()
            } finally {
                closed.countDown()
            }
        })
        session.start()
        Assertions.assertTrue(closed.await(10, java.util.concurrent.TimeUnit.SECONDS), "the stand-in must exit")
    }

    @Test
    fun aThrowingConsoleListenerIsReportedOncePerSession(@TempDir packDir: File) {
        requireBash()
        val warnings = warningsDuring {
            runToTheEnd(packDir, onLine = { throw IllegalStateException("listener broke") }, onClosed = {})
        }
        Assertions.assertEquals(
            1, warnings.count { packDir.absolutePath in it && "console" in it },
            "one WARN naming the pack, not one per line: $warnings"
        )
    }

    @Test
    fun aThrowingCloseListenerIsReported(@TempDir packDir: File) {
        requireBash()
        val warnings = warningsDuring({ it.isNotEmpty() }) {
            runToTheEnd(packDir, onLine = {}, onClosed = { throw IllegalStateException("port not given back") })
        }
        Assertions.assertTrue(
            warnings.any { packDir.absolutePath in it && "clos" in it.lowercase() },
            "a WARN names the pack whose clean-up failed: $warnings"
        )
    }

    @Test
    fun aServerThatCannotBeKilledAtShutdownIsReported(@TempDir packDir: File) {
        val session = mockk<ServerSession>()
        every { session.kill() } throws IllegalStateException("cannot kill")
        val registry = SessionRegistry()
        registry.register(packDir, session)
        val warnings = warningsDuring { registry.killAll() }
        Assertions.assertTrue(
            warnings.any { packDir.canonicalPath in it },
            "a WARN names the pack whose server may still be running: $warnings"
        )
    }

    @Test
    fun anAddressThatCouldNotBeCopiedIsReported() {
        val warnings = warningsDuring {
            copyToClipboard("127.0.0.1:25565") { throw HeadlessException("no clipboard") }
        }
        Assertions.assertTrue(warnings.any { "127.0.0.1:25565" in it }, "a WARN names the address: $warnings")
    }
}
