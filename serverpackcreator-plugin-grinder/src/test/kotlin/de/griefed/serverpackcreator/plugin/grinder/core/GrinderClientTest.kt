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
package de.griefed.serverpackcreator.plugin.grinder.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Pins the client against a **real** loopback HTTP server rather than a mock, the same way the grinder's
 * own `ReportServerTest` pins its endpoints. What is being tested here is behaviour at a socket —
 * timeouts, refused connections, a body that is not the shape we asked for — and a mocked client answers
 * none of those honestly.
 *
 * The governing requirement is that **nothing here throws**. This client is called from the Swing event
 * loop's worker and from a pre-generation extension; an escaped exception in either place is a stack
 * trace in somebody's log and a dead tab, where a `Failed` carrying a reason is a sentence the operator
 * can act on.
 */
internal class GrinderClientTest {

    private val mapper = ObjectMapper()

    /** A server answering `body` with `status` on every path, on an ephemeral port. */
    private fun serving(status: Int = 200, body: String): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }

    private fun HttpServer.baseUrl() = "http://127.0.0.1:${address.port}"

    private val documentedResponse = """
        {
          "total": 2, "matched": 2, "page": 1, "pages": 1,
          "verdicts": [
            {
              "platform": "Modrinth", "slug": "creativecore",
              "projectUrl": "https://modrinth.com/mod/creativecore", "loader": "Fabric",
              "suggestedEntry": "creativecore-", "detail": "crashed on a client-only class",
              "verifiedAt": "2026-09-04T12:30:00Z", "verdict": "CONFIRMED",
              "filenamePattern": "CreativeCore_FABRIC_.*", "stagedDependencies": ["cloth-config"]
            },
            {
              "platform": "CurseForge", "slug": "bookshelf",
              "projectUrl": "https://curseforge.com/minecraft/mc-mods/bookshelf", "loader": "Forge",
              "suggestedEntry": "bookshelf-", "detail": "server reached its ready line",
              "verifiedAt": "2026-09-05T08:00:00Z", "verdict": "CLEAR",
              "filenamePattern": null, "stagedDependencies": []
            }
          ]
        }
    """.trimIndent()

    /** The happy path: the documented document maps onto the plugin's read model, field for field. */
    @Test
    fun readsTheDocumentedVerdictDocument() {
        val server = serving(body = documentedResponse)
        try {
            val result = GrinderClient(mapper).fetchVerdicts(server.baseUrl())
            Assertions.assertTrue(result is FetchResult.Ok, "expected Ok, got $result")

            val verdicts = (result as FetchResult.Ok).value
            Assertions.assertEquals(2, verdicts.size)

            val confirmed = verdicts.first { it.slug == "creativecore" }
            Assertions.assertEquals("Modrinth", confirmed.platform)
            Assertions.assertEquals("Fabric", confirmed.loader)
            Assertions.assertEquals("CONFIRMED", confirmed.verdict)
            Assertions.assertEquals("creativecore-", confirmed.suggestedEntry)
            Assertions.assertEquals("CreativeCore_FABRIC_.*", confirmed.filenamePattern)
            Assertions.assertEquals("https://modrinth.com/mod/creativecore", confirmed.projectUrl)
            Assertions.assertEquals("crashed on a client-only class", confirmed.detail)
            Assertions.assertEquals("2026-09-04T12:30:00Z", confirmed.scannedAt)
            Assertions.assertTrue(confirmed.isConfirmed)

            Assertions.assertFalse(verdicts.first { it.slug == "bookshelf" }.isConfirmed)
        } finally {
            server.stop(0)
        }
    }

    /**
     * A grinder newer than the plugin will carry fields this build has never heard of, and a plugin is
     * updated on the user's schedule, not the daemon's. Reading the document as a tree rather than
     * binding it to a class is what keeps a new field from being a parse failure — which would present
     * to the user as "the grinder is broken".
     */
    @Test
    fun toleratesFieldsThisBuildDoesNotKnow() {
        val server = serving(
            body = """
                {"verdicts":[{"slug":"creativecore","verdict":"CONFIRMED","suggestedEntry":"creativecore-",
                "someFieldFromTheFuture":{"nested":true},"anotherOne":[1,2,3]}]}
            """.trimIndent()
        )
        try {
            val result = GrinderClient(mapper).fetchVerdicts(server.baseUrl())
            Assertions.assertTrue(result is FetchResult.Ok, "expected Ok, got $result")
            Assertions.assertEquals("creativecore", (result as FetchResult.Ok).value.single().slug)
        } finally {
            server.stop(0)
        }
    }

    /**
     * Absent optional fields become absent values, not the string "null". A row whose `suggestedEntry` is
     * missing has nothing that could be added to an exclusion list, and the tab has to be able to tell
     * that apart from an entry that happens to read "null".
     */
    @Test
    fun leavesAMissingEntryNullRatherThanTheStringNull() {
        val server = serving(body = """{"verdicts":[{"slug":"mystery","verdict":"INCONCLUSIVE"}]}""")
        try {
            val row = (GrinderClient(mapper).fetchVerdicts(server.baseUrl()) as FetchResult.Ok).value.single()
            Assertions.assertNull(row.suggestedEntry)
            Assertions.assertNull(row.filenamePattern)
            Assertions.assertNull(row.exclusionEntry)
            Assertions.assertEquals("", row.detail)
        } finally {
            server.stop(0)
        }
    }

    /** Nothing to reach: a refused connection is a sentence for the user, not a thrown ConnectException. */
    @Test
    fun reportsAnUnreachableGrinderRatherThanThrowing() {
        // Bind and immediately release, so the port is almost certainly free and nothing answers on it.
        val port = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).let { it.address.port.also { _ -> it.stop(0) } }
        val result = GrinderClient(mapper, timeout = Duration.ofMillis(500)).fetchVerdicts("http://127.0.0.1:$port")
        Assertions.assertTrue(result is FetchResult.Failed, "expected Failed, got $result")
        Assertions.assertTrue((result as FetchResult.Failed).reason.isNotBlank())
    }

    /** A reverse proxy in front of a stopped daemon answers 502 with an HTML body. Still not an exception. */
    @Test
    fun reportsAnErrorStatusRatherThanParsingItsBody() {
        val server = serving(status = 502, body = "<html><body>Bad Gateway</body></html>")
        try {
            val result = GrinderClient(mapper).fetchVerdicts(server.baseUrl())
            Assertions.assertTrue(result is FetchResult.Failed, "expected Failed, got $result")
            Assertions.assertTrue(
                (result as FetchResult.Failed).reason.contains("502"),
                "the status is the one fact the operator needs; got: ${result.reason}"
            )
        } finally {
            server.stop(0)
        }
    }

    /** A 200 carrying something that is not the document we asked for is a failure, not an empty list. */
    @Test
    fun reportsAnUnparseableBodyRatherThanReturningNothingFound() {
        val server = serving(body = "not json at all")
        try {
            Assertions.assertTrue(GrinderClient(mapper).fetchVerdicts(server.baseUrl()) is FetchResult.Failed)
        } finally {
            server.stop(0)
        }
    }

    /** An unusable address never reaches the network — the URL rule is the client's first gate. */
    @Test
    fun refusesAnAddressItCouldNotRequest() {
        val client = GrinderClient(mapper)
        Assertions.assertTrue(client.fetchVerdicts("") is FetchResult.Failed)
        Assertions.assertTrue(client.fetchVerdicts("ftp://grinder.example.com") is FetchResult.Failed)
    }

    /** The Dashboard reads `/status` as a tree, since its document is a shape, not a list of rows. */
    @Test
    fun readsTheStatusDocumentAsATree() {
        val server = serving(body = """{"verdicts":1483,"requeued":0,"activity":{"pass":7}}""")
        try {
            val result = GrinderClient(mapper).fetchStatus(server.baseUrl())
            Assertions.assertTrue(result is FetchResult.Ok, "expected Ok, got $result")
            Assertions.assertEquals(1483, (result as FetchResult.Ok).value["verdicts"].asInt())
            Assertions.assertEquals(7, result.value["activity"]["pass"].asInt())
        } finally {
            server.stop(0)
        }
    }
}
