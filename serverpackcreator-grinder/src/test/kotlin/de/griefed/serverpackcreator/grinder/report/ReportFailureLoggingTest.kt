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
package de.griefed.serverpackcreator.grinder.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.griefed.serverpackcreator.grinder.source.CatalogCursor
import de.griefed.serverpackcreator.grinder.source.CursorStore
import org.apache.logging.log4j.Level
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpTimeoutException
import java.time.Duration

/**
 * The report degrades a failing collaborator to a thinner answer rather than an error, and that degradation must
 * reach the log: an endpoint polled unattended that hides its failures hides them for ever. Only an [Exception]
 * degrades; an [Error] such as heap exhaustion is not something to answer 200 over.
 */
internal class ReportFailureLoggingTest {

    /** A bounded request: a server that never answers must fail a guard, not hang the suite. */
    private fun get(port: Int, path: String) = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).timeout(Duration.ofSeconds(30)).build(),
        BodyHandlers.ofString()
    )

    private val brokenCursors = object : CursorStore {
        override fun cursor(source: String): CatalogCursor = throw IllegalStateException("cursors unreadable")
        override fun store(source: String, cursor: CatalogCursor) = Unit
    }

    @Test
    fun anUnreadableCrawlPositionIsAbsentFromStatusAndLogged() = capturingLogsOf(ReportServer::class.java) { log ->
        val server = ReportServer(InMemoryVerdictStore(), requestedPort = 0, cursors = brokenCursors).start()
        try {
            val response = get(server.port, "/status")

            Assertions.assertEquals(200, response.statusCode())
            Assertions.assertTrue(jacksonObjectMapper().readTree(response.body())["crawl"].isNull, response.body())
            Assertions.assertTrue(log.hasThrowableAt(Level.WARN), "the crawl position failed without a WARN")
        } finally {
            server.stop()
        }
    }

    @Test
    fun aFailingListSourceDegradesAsPropertiesAndIsLogged() = capturingLogsOf(ReportServer::class.java) { log ->
        val server = ReportServer(
            InMemoryVerdictStore(), requestedPort = 0, fallbackLists = { throw IllegalStateException("lists unreadable") }
        ).start()
        try {
            Assertions.assertEquals(200, get(server.port, "/as-properties").statusCode())
            Assertions.assertTrue(log.hasThrowableAt(Level.WARN), "the list source failed without a WARN")
        } finally {
            server.stop()
        }
    }

    @Test
    fun anErrorIsNotAnsweredAsIfNothingHappened() {
        val server = ReportServer(
            InMemoryVerdictStore(), requestedPort = 0, fallbackLists = { throw OutOfMemoryError("simulated") }
        ).start()
        try {
            val failure = Assertions.assertThrows(IOException::class.java) { get(server.port, "/as-properties") }
            Assertions.assertFalse(failure is HttpTimeoutException, "the connection was left hanging, not dropped")
        } finally {
            server.stop()
        }
    }
}
