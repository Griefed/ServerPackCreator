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

    /**
     * Every path the fixture was asked for, in order. The context below is registered on `"/"`, which is
     * a *prefix* match for everything — so without recording this, a client requesting `/nonsense` would
     * satisfy every guard in this class and a wrong endpoint constant would be invisible.
     */
    private val requestedPaths = mutableListOf<String>()

    /** A server answering `body` with `status` on every path, on an ephemeral port. */
    private fun serving(status: Int = 200, body: String): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                requestedPaths += exchange.requestURI.path
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
              "declared": "CLIENT", "jarScan": "CLIENT",
              "fileName": "CreativeCore_FABRIC_v2.11.34_mc1.21.1.jar", "stagedDependencies": ["cloth-config"]
            },
            {
              "platform": "CurseForge", "slug": "bookshelf",
              "projectUrl": "https://curseforge.com/minecraft/mc-mods/bookshelf", "loader": "Forge",
              "suggestedEntry": "bookshelf-", "detail": "server reached its ready line",
              "verifiedAt": "2026-09-05T08:00:00Z", "verdict": "CLEAR",
              "declared": "CONTRADICTORY", "jarScan": "SERVER_OR_BOTH",
              "fileName": null, "stagedDependencies": []
            }
          ]
        }
    """.trimIndent()

    /** Every request URI the fixture was asked for, query included — what the paging guards assert on. */
    private val requestedUris = mutableListOf<String>()

    /** A server answering each `?page=` with its own document, so paging can be exercised end to end. */
    private fun servingByPage(pages: Map<Int, String>): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                requestedPaths += exchange.requestURI.path
                requestedUris += exchange.requestURI.toString()
                val asked = exchange.requestURI.query.orEmpty().split('&')
                    .firstOrNull { it.startsWith("page=") }?.substringAfter('=')?.toIntOrNull() ?: 1
                val bytes = (pages[asked] ?: "{}").toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }

    /**
     * One page of the feed, carrying the paging envelope and one minimal row per slug. [matched] is omitted unless
     * given, as an older daemon's envelope omits it, so only the guards about it make the client check it.
     */
    private fun pageDocument(
        page: Int,
        pages: Int,
        vararg slugs: String,
        loader: String = "Fabric",
        matched: Int? = null,
        verdict: String = "CONFIRMED",
        version: Long? = null
    ): String {
        val rows = slugs.joinToString(",") { slug ->
            """{"platform":"Modrinth","slug":"$slug","loader":"$loader","minecraftLine":"1.20",""" +
                """"verdict":"$verdict","suggestedEntry":"$slug-","detail":"","verifiedAt":"2026-09-04T12:30:00Z"}"""
        }
        val matchedField = matched?.let { "\"matched\":$it," }.orEmpty()
        val versionField = version?.let { "\"version\":$it," }.orEmpty()
        return """{"total":99,$matchedField$versionField"page":$page,"pages":$pages,"verdicts":[$rows]}"""
    }

    /**
     * **A key removed ahead of the cursor is not silently lost.** Between page 1 and page 2, `alpha` is
     * superseded by `echo` under a new key — one row out ahead of the cursor, one in behind it — so page 2 starts
     * one row late, `charlie` is on no page the client read, and `matched` still says 4: the count cannot see it.
     * The changed `version` can, and the read starts over from page 1, against the one snapshot.
     */
    @Test
    fun restartsTheReadWhenTheFeedVersionChangesBetweenPages() {
        val server = servingInSequence(
            pageDocument(1, 2, "alpha", "bravo", matched = 4, version = 7),
            pageDocument(2, 2, "delta", "echo", matched = 4, version = 8),
            pageDocument(1, 2, "bravo", "charlie", matched = 4, version = 8),
            pageDocument(2, 2, "delta", "echo", matched = 4, version = 8)
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(
                listOf("bravo", "charlie", "delta", "echo"), slugsOf(result),
                "every row of the one snapshot exactly once, and nothing from the read that was abandoned"
            )
            Assertions.assertEquals(4, requestedUris.size, "one abandoned read of two pages, one complete one")
            Assertions.assertEquals("/verdicts.json?size=2&page=1", requestedUris[2], "the restart begins at page 1")
        } finally {
            server.stop(0)
        }
    }

    /** A feed whose version never holds still across a read is a failure, never a list stitched across changes. */
    @Test
    fun failsWhenTheFeedVersionKeepsChangingOnEveryAttempt() {
        var version = 0L
        val documents = (1..GrinderClient.MAX_READ_ATTEMPTS).flatMap {
            listOf(
                pageDocument(1, 2, "alpha", "bravo", matched = 4, version = ++version),
                pageDocument(2, 2, "charlie", "delta", matched = 4, version = ++version)
            )
        }
        val server = servingInSequence(*documents.toTypedArray())
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertTrue(result is FetchResult.Failed, "a feed read across changes was returned: $result")
            Assertions.assertTrue(
                (result as FetchResult.Failed).reason.contains("changed while they were being read"),
                "the reason must name the instability: ${result.reason}"
            )
            Assertions.assertEquals(
                2 * GrinderClient.MAX_READ_ATTEMPTS, requestedUris.size, "a bounded number of attempts, no more"
            )
        } finally {
            server.stop(0)
        }
    }

    /** A server answering each request with the next of [answers] — a status and a body — whatever is asked. */
    private fun servingAnswers(vararg answers: Pair<Int, String>): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            val next = java.util.concurrent.atomic.AtomicInteger()
            createContext("/") { exchange ->
                requestedUris += exchange.requestURI.toString()
                val (status, body) = answers[minOf(next.getAndIncrement(), answers.lastIndex)]
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }

    private val gone = 410 to """{"error":"This snapshot is no longer retained","version":7}"""

    /**
     * **Every page after the first is pinned to page 1's snapshot.** Page 1 asks for whatever is current; each later
     * page sends its `version`, so a daemon that retains snapshots answers the whole read from one of them however
     * much a grind records meanwhile, and no restart is needed.
     */
    @Test
    fun pinsEveryPageAfterTheFirstToPageOnesVersion() {
        val server = servingByPage(
            mapOf(
                1 to pageDocument(1, 3, "alpha", matched = 3, version = 42),
                2 to pageDocument(2, 3, "bravo", matched = 3, version = 42),
                3 to pageDocument(3, 3, "charlie", matched = 3, version = 42)
            )
        )
        try {
            val result = GrinderClient(mapper, pageSize = 1).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("alpha", "bravo", "charlie"), slugsOf(result))
            Assertions.assertEquals(
                listOf(
                    "/verdicts.json?size=1&page=1",
                    "/verdicts.json?size=1&page=2&version=42",
                    "/verdicts.json?size=1&page=3&version=42"
                ),
                requestedUris
            )
        } finally {
            server.stop(0)
        }
    }

    /** A daemon that sends no version gets no `version=`: there is nothing to pin to. */
    @Test
    fun pinsNothingForAnUnversionedFeed() {
        val server = servingByPage(
            mapOf(1 to pageDocument(1, 2, "alpha"), 2 to pageDocument(2, 2, "bravo"))
        )
        try {
            GrinderClient(mapper, pageSize = 1).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("/verdicts.json?size=1&page=1", "/verdicts.json?size=1&page=2"), requestedUris)
        } finally {
            server.stop(0)
        }
    }

    /**
     * **An evicted snapshot restarts the read rather than failing it.** The daemon answers `410 Gone` for a version
     * it no longer retains; the read starts over at page 1 and is pinned to the new snapshot.
     */
    @Test
    fun restartsWhenThePinnedSnapshotIsGone() {
        val server = servingAnswers(
            200 to pageDocument(1, 2, "alpha", "bravo", matched = 4, version = 7),
            gone,
            200 to pageDocument(1, 2, "bravo", "charlie", matched = 4, version = 8),
            200 to pageDocument(2, 2, "delta", "echo", matched = 4, version = 8)
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("bravo", "charlie", "delta", "echo"), slugsOf(result))
            Assertions.assertEquals(
                listOf(
                    "/verdicts.json?size=2&page=1",
                    "/verdicts.json?size=2&page=2&version=7",
                    "/verdicts.json?size=2&page=1",
                    "/verdicts.json?size=2&page=2&version=8"
                ),
                requestedUris
            )
        } finally {
            server.stop(0)
        }
    }

    /** Eviction on every attempt is bounded like any other instability, and ends in the same failure. */
    @Test
    fun failsWhenThePinnedSnapshotIsGoneOnEveryAttempt() {
        val documents = (1..GrinderClient.MAX_READ_ATTEMPTS).flatMap {
            listOf(200 to pageDocument(1, 2, "alpha", matched = 2, version = 7), gone)
        }
        val server = servingAnswers(*documents.toTypedArray())
        try {
            val result = GrinderClient(mapper, pageSize = 1).fetchVerdicts(server.baseUrl())

            Assertions.assertTrue(
                (result as? FetchResult.Failed)?.reason?.contains("changed while they were being read") == true,
                "expected the instability failure: $result"
            )
            Assertions.assertEquals(2 * GrinderClient.MAX_READ_ATTEMPTS, requestedUris.size)
        } finally {
            server.stop(0)
        }
    }

    /** Page 1 carries no `version=`, so a 410 there is not an eviction: it is whatever answered, and a failure. */
    @Test
    fun aGoneFirstPageIsAFailureRatherThanARestart() {
        val server = servingAnswers(gone)
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertTrue(
                (result as? FetchResult.Failed)?.reason?.contains("HTTP 410") == true, "expected the status: $result"
            )
            Assertions.assertEquals(1, requestedUris.size, "one request, no restart loop")
        } finally {
            server.stop(0)
        }
    }

    /**
     * A version appearing mid-read is a daemon upgraded or restarted under the read, and the rows on either side
     * of it are no more continuous than a changed number's.
     */
    @Test
    fun aVersionAppearingMidReadIsAChange() {
        val server = servingInSequence(
            pageDocument(1, 2, "alpha", "bravo", matched = 4),
            pageDocument(2, 2, "delta", "echo", matched = 4, version = 5),
            pageDocument(1, 2, "bravo", "charlie", matched = 4, version = 5),
            pageDocument(2, 2, "delta", "echo", matched = 4, version = 5)
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("bravo", "charlie", "delta", "echo"), slugsOf(result))
            Assertions.assertEquals(4, requestedUris.size)
        } finally {
            server.stop(0)
        }
    }

    /** A version vanishing mid-read is a daemon downgraded or replaced under the read: also a change. */
    @Test
    fun aVersionVanishingMidReadIsAChange() {
        val server = servingInSequence(
            pageDocument(1, 2, "alpha", "bravo", matched = 4, version = 5),
            pageDocument(2, 2, "delta", "echo", matched = 4),
            pageDocument(1, 2, "bravo", "charlie", matched = 4),
            pageDocument(2, 2, "delta", "echo", matched = 4)
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("bravo", "charlie", "delta", "echo"), slugsOf(result))
            Assertions.assertEquals(4, requestedUris.size)
        } finally {
            server.stop(0)
        }
    }

    /** A version that holds still across the read costs nothing: one pass, no restart. */
    @Test
    fun readsAVersionedFeedThatHoldsStillOnce() {
        val server = servingByPage(
            mapOf(
                1 to pageDocument(1, 2, "alpha", "bravo", matched = 3, version = 42),
                2 to pageDocument(2, 2, "charlie", matched = 3, version = 42)
            )
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("alpha", "bravo", "charlie"), slugsOf(result))
            Assertions.assertEquals(2, requestedUris.size, "an unchanged version must not restart the read")
        } finally {
            server.stop(0)
        }
    }

    /** A server answering each request with the next of [documents] in turn, whatever page it asks for. */
    private fun servingInSequence(vararg documents: String): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            val next = java.util.concurrent.atomic.AtomicInteger()
            createContext("/") { exchange ->
                requestedUris += exchange.requestURI.toString()
                val bytes = documents[minOf(next.getAndIncrement(), documents.lastIndex)].toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }

    /**
     * **A row the store moved past the cursor while the client paged is not silently lost.** Offset paging over a
     * selection the daemon re-derives per request: `prism` is re-recorded and sorts to the front between page 1
     * and page 2, so page 2 starts one row late and `prism` is on no page the client read. Fewer rows than the
     * daemon says it matched is the tell, and the whole feed is read again.
     */
    @Test
    fun readsTheFeedAgainWhenAMovedRowWasSkipped() {
        val server = servingInSequence(
            pageDocument(1, 2, "creativecore", "iceberg", matched = 4),
            pageDocument(2, 2, "iceberg", "bookshelf", matched = 4),
            pageDocument(1, 2, "prism", "creativecore", matched = 4),
            pageDocument(2, 2, "iceberg", "bookshelf", matched = 4)
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(setOf("creativecore", "iceberg", "bookshelf", "prism"), slugsOf(result).toSet())
            Assertions.assertEquals(4, requestedUris.size, "one complete re-read, no more")
        } finally {
            server.stop(0)
        }
    }

    /** A store that keeps shifting under the client is a failure, never a short list presented as the feed. */
    @Test
    fun failsWhenTheFeedStaysShortOfWhatTheDaemonMatched() {
        val shortPage = pageDocument(1, 1, "creativecore", matched = 2)
        val server = servingInSequence(shortPage, shortPage)
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertTrue(result is FetchResult.Failed, "a short feed was returned as complete: $result")
        } finally {
            server.stop(0)
        }
    }

    /** A verdict re-recorded while the client paged is shown in its newer state, the one served last. */
    @Test
    fun aRowSeenTwiceKeepsItsNewerCopy() {
        val server = servingByPage(
            mapOf(
                1 to pageDocument(1, 2, "creativecore", verdict = "INCONCLUSIVE"),
                2 to pageDocument(2, 2, "creativecore", verdict = "CONFIRMED")
            )
        )
        try {
            val result = GrinderClient(mapper, pageSize = 1).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("CONFIRMED"), (result as FetchResult.Ok).value.map { it.verdict })
        } finally {
            server.stop(0)
        }
    }

    private fun slugsOf(result: FetchResult<List<GrinderVerdict>>): List<String> =
        (result as FetchResult.Ok).value.map { it.slug }

    /**
     * **The feed is paged, not swallowed whole.** The daemon returns every row for a bare
     * `/verdicts.json` by documented design, which at the live store's 319,904 verdicts is a ~248 MiB
     * document against this client's 15-second timeout — so the tab simply failed, and the one thing it
     * must never do instead is show a truncated list a maintainer then ticks exclusions from.
     */
    @Test
    fun pagesThroughTheWholeFeed() {
        val server = servingByPage(
            mapOf(
                1 to pageDocument(1, 3, "creativecore", "iceberg"),
                2 to pageDocument(2, 3, "bookshelf"),
                3 to pageDocument(3, 3, "prism")
            )
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(
                listOf("creativecore", "iceberg", "bookshelf", "prism"), slugsOf(result),
                "every page's rows must be collected, in the order the daemon served them"
            )
            Assertions.assertEquals(
                listOf("/verdicts.json?size=2&page=1", "/verdicts.json?size=2&page=2", "/verdicts.json?size=2&page=3"),
                requestedUris,
                "each page must be asked for explicitly, and none beyond the last"
            )
        } finally {
            server.stop(0)
        }
    }

    /**
     * A daemon publishing the bare array has no envelope to page by, so one request is the whole answer.
     * Asking for a second page would re-request the same rows for ever.
     */
    @Test
    fun stopsAfterOneRequestWhenThereIsNoPagingEnvelope() {
        val server = serving(body = """[{"slug":"creativecore","platform":"Modrinth","loader":"Fabric"}]""")
        try {
            val result = GrinderClient(mapper, pageSize = 1).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("creativecore"), slugsOf(result))
            Assertions.assertEquals(1, requestedPaths.size, "a bare array must not be paged")
        } finally {
            server.stop(0)
        }
    }

    /**
     * **A row repeated across pages is collected once.** The daemon re-derives its selection per request
     * and a grind is recording verdicts while the client pages, so the row set shifts underneath: the same
     * verdict can land on two pages. Keyed on the identity [GrinderVerdict] itself declares — platform,
     * slug, loader and Minecraft line — because a duplicate row is a mod offered for ticking twice.
     */
    @Test
    fun collectsARowRepeatedAcrossPagesOnlyOnce() {
        val server = servingByPage(
            mapOf(
                1 to pageDocument(1, 2, "creativecore", "iceberg"),
                2 to pageDocument(2, 2, "iceberg", "bookshelf")
            )
        )
        try {
            val result = GrinderClient(mapper, pageSize = 2).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(listOf("creativecore", "iceberg", "bookshelf"), slugsOf(result))
        } finally {
            server.stop(0)
        }
    }

    /**
     * The same row on a *different* loader is a different verdict, so de-duplication must not collapse it
     * — that is the difference between "ground twice" and "ground on Fabric and on Forge".
     */
    @Test
    fun keepsTheSameSlugOnADifferentLoader() {
        val server = servingByPage(
            mapOf(
                1 to pageDocument(1, 2, "creativecore", loader = "Fabric"),
                2 to pageDocument(2, 2, "creativecore", loader = "Forge")
            )
        )
        try {
            val result = GrinderClient(mapper, pageSize = 1).fetchVerdicts(server.baseUrl())

            Assertions.assertEquals(
                listOf("Fabric", "Forge"), (result as FetchResult.Ok).value.map { it.loader },
                "loader is part of the row identity, so both must survive"
            )
        } finally {
            server.stop(0)
        }
    }

    /**
     * A page that fails part-way through is a failure, never a partial answer: a short list presented as
     * the grinder's findings is the truncation this whole change exists to avoid.
     */
    @Test
    fun reportsAFailurePartWayThroughRatherThanAPartialList() {
        val server = servingByPage(mapOf(1 to pageDocument(1, 3, "creativecore"), 2 to "not json at all"))
        try {
            val result = GrinderClient(mapper, pageSize = 1).fetchVerdicts(server.baseUrl())

            Assertions.assertInstanceOf(
                FetchResult.Failed::class.java, result,
                "a broken page must not be reported as the whole feed minus that page"
            )
        } finally {
            server.stop(0)
        }
    }

    /**
     * A daemon claiming more pages than it will ever serve must stop the client, and must stop it with a
     * *failure* — truncating silently at the ceiling would be the defect this guard exists to prevent,
     * wearing a success.
     */
    @Test
    fun refusesAFeedThatNeverEnds() {
        val endless = (1..10).associateWith { page -> pageDocument(page, Int.MAX_VALUE, "mod$page") }
        val server = servingByPage(endless)
        try {
            val result = GrinderClient(mapper, pageSize = 1, maxPages = 3).fetchVerdicts(server.baseUrl())

            Assertions.assertInstanceOf(FetchResult.Failed::class.java, result)
            Assertions.assertEquals(
                3, requestedPaths.size,
                "the client must stop at its own ceiling rather than paging for ever"
            )
        } finally {
            server.stop(0)
        }
    }

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
            Assertions.assertEquals("CLIENT", confirmed.declared, "what the mod says about itself")
            Assertions.assertEquals("CLIENT", confirmed.jarScan, "what its jar was read as")
            Assertions.assertEquals("CreativeCore_FABRIC_v2.11.34_mc1.21.1.jar", confirmed.fileName)
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
            Assertions.assertNull(row.fileName)
            // 18 of 2057 rows on the live feed carry `declared: null` — a row nothing declared anything
            // about must not put the word "null" in a table cell.
            Assertions.assertNull(row.declared)
            Assertions.assertNull(row.jarScan)
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

    /**
     * The endpoints are only correct if the client actually requests them, and every other guard here
     * passes whatever path is asked for — the fixture's context matches all of them. `GrinderUrlTest`
     * pins the derivation as strings; nothing joined that to the client until this.
     */
    @Test
    fun requestsTheDocumentedEndpoints() {
        val server = serving(body = documentedResponse)
        try {
            val client = GrinderClient(mapper)
            client.fetchVerdicts(server.baseUrl())
            client.fetchStatus(server.baseUrl())
            Assertions.assertEquals(listOf("/verdicts.json", "/status"), requestedPaths)
        } finally {
            server.stop(0)
        }
    }

    /**
     * A 200 carrying valid JSON that is not a verdict document — a proxy's error envelope, a future
     * daemon's reshaped answer — must be a failure, not an empty list. Reported as "no verdicts found"
     * it is indistinguishable from a grinder that has genuinely ground nothing, which is the one thing
     * an operator debugging an empty tab needs told apart.
     */
    @Test
    fun reportsAWrongShapedDocumentRatherThanNoVerdictsFound() {
        val server = serving(body = """{"error":"the grinder is starting up"}""")
        try {
            val result = GrinderClient(mapper).fetchVerdicts(server.baseUrl())
            Assertions.assertTrue(result is FetchResult.Failed, "expected Failed, got $result")
        } finally {
            server.stop(0)
        }
    }

    /** A genuinely empty grinder is still a success carrying nothing — the other half of the guard above. */
    @Test
    fun readsAnEmptyVerdictListAsSuccess() {
        val server = serving(body = """{"total":0,"matched":0,"page":1,"pages":1,"verdicts":[]}""")
        try {
            val result = GrinderClient(mapper).fetchVerdicts(server.baseUrl())
            Assertions.assertTrue(result is FetchResult.Ok, "expected Ok, got $result")
            Assertions.assertTrue((result as FetchResult.Ok).value.isEmpty())
        } finally {
            server.stop(0)
        }
    }

    /**
     * A bare array is accepted too — a daemon publishing the rows without the paging envelope. The
     * branch is documented in `readVerdicts` and was unreachable from any guard until now.
     */
    @Test
    fun readsABareArrayOfVerdicts() {
        val server = serving(body = """[{"slug":"creativecore","verdict":"CONFIRMED","suggestedEntry":"creativecore-"}]""")
        try {
            val result = GrinderClient(mapper).fetchVerdicts(server.baseUrl())
            Assertions.assertTrue(result is FetchResult.Ok, "expected Ok, got $result")
            Assertions.assertEquals("creativecore", (result as FetchResult.Ok).value.single().slug)
        } finally {
            server.stop(0)
        }
    }
}
