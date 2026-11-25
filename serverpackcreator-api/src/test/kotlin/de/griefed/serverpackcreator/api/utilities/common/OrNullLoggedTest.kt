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
package de.griefed.serverpackcreator.api.utilities.common

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.Collections
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pins [orNullLogged]: an [Exception] becomes a logged `null`, and nothing that must not be swallowed is —
 * an [Error], a coroutine's cancellation, and the interrupt status an [InterruptedException] cleared.
 */
internal class OrNullLoggedTest {

    private val logger = LogManager.getLogger(OrNullLoggedTest::class.java) as Logger
    private val events: MutableList<LogEvent> = Collections.synchronizedList(mutableListOf())
    private val appender = object : AbstractAppender("orNullLogged", null, null, true, Property.EMPTY_ARRAY) {
        override fun append(event: LogEvent) {
            events.add(event.toImmutable())
        }
    }

    @BeforeEach
    fun attach() {
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun detach() {
        logger.removeAppender(appender)
        appender.stop()
        // Never leak an interrupt into the next test on this thread, whatever the assertion did.
        Thread.interrupted()
    }

    @Test
    fun aValueIsHandedThrough() {
        Assertions.assertEquals("value", orNullLogged(logger, "unused") { "value" })
        Assertions.assertTrue(events.isEmpty(), "a success must log nothing")
    }

    @Test
    fun anExceptionBecomesNullAndIsLoggedWithItsCause() {
        val failure = IOException("disk gone")

        val result = orNullLogged<String>(logger, "The thing is unavailable") { throw failure }

        Assertions.assertNull(result)
        val event = events.single()
        Assertions.assertEquals(Level.WARN, event.level)
        Assertions.assertEquals("The thing is unavailable", event.message.formattedMessage)
        Assertions.assertSame(failure, event.thrown, "the cause must reach the log, not only the message")
    }

    @Test
    fun anErrorPropagates() {
        Assertions.assertThrows(OutOfMemoryError::class.java) {
            orNullLogged(logger, "unused") { throw OutOfMemoryError("heap") }
        }
    }

    /**
     * Catching [InterruptedException] clears nothing the JVM will set again: the flag was already cleared when it
     * was thrown, so a helper that swallows it without re-asserting the flag makes the thread forget it was asked
     * to stop.
     */
    @Test
    fun anInterruptRestoresTheInterruptStatus() {
        val result = orNullLogged<String>(logger, "Interrupted") { throw InterruptedException("stop") }

        Assertions.assertNull(result)
        Assertions.assertTrue(Thread.currentThread().isInterrupted, "the interrupt must survive being caught")
    }

    /** Cancellation is how a coroutine stops; turning it into a `null` keeps a cancelled job running. */
    @Test
    fun cancellationPropagates() {
        Assertions.assertThrows(CancellationException::class.java) {
            orNullLogged(logger, "unused") { throw CancellationException("cancelled") }
        }
        Assertions.assertTrue(events.isEmpty(), "a cancellation is not a failure and must not be logged as one")
    }
}
