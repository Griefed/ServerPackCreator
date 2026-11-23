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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.griefed.serverpackcreator.clientside.verdict.Verdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.Instant

/**
 * Pins `/verdicts.json` — the machine-readable verdict feed the GUI plugin reads, as opposed to
 * `/export.csv`, which flattens every field to a string and has to be re-parsed to get lists back.
 *
 * The endpoint exists because [VerdictField], [VerdictQuery] and [VerdictSelection] are `internal` to
 * this module: nothing outside it can reuse the selection, so the selection has to travel over the wire.
 * These guards are therefore as much about *agreement* as about shape — the JSON, the CSV and the table
 * must select the same rows for the same query, and they do so by sharing [VerdictSelection.select]
 * rather than by three renderers being kept in step by hand.
 */
internal class VerdictsJsonEndpointTest {

    private fun get(port: Int, path: String) = HttpClient.newHttpClient()
        .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).build(), BodyHandlers.ofString())

    private val mapper = jacksonObjectMapper()

    /**
     * A store holding one verdict of each class, so every filter guard below has both a row that must
     * match and rows that must not.
     */
    private fun populatedStore() = InMemoryVerdictStore().apply {
        record(grindVerdict("creativecore", "Fabric", verdict = Verdict.CONFIRMED))
        record(grindVerdict("bookshelf", "Forge", verdict = Verdict.CLEAR))
        record(grindVerdict("iceberg", "NeoForge", verdict = Verdict.INCONCLUSIVE))
        record(grindVerdict("prism", "Quilt", verdict = Verdict.ERROR))
    }

    /**
     * **The unpaged endpoints must be written as they are produced, not built in heap first.**
     *
     * A bare `/verdicts.json` and `/export.csv` return every row by documented design, so their documents grow
     * with the store without any bound a query can impose, and a materialised response holds one three times
     * over. An absent `Content-Length` is the transport's half: the JDK's server sends a response of declared
     * length 0 chunked, which it can only do if the body was never sized up front. That the streamed bodies are
     * the right documents is pinned by the two `theStreamed…` guards below.
     */
    @Test
    fun theUnpagedEndpointsAreStreamedRatherThanBuiltInHeap() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            for (path in listOf("/verdicts.json", "/export.csv")) {
                val response = get(server.port, path)
                Assertions.assertEquals(200, response.statusCode(), "$path must still answer")
                Assertions.assertTrue(
                    response.headers().firstValue("Content-Length").isEmpty,
                    "$path declared a Content-Length, so the whole document was built in heap before it was sent"
                )
            }
        } finally {
            server.stop()
        }
    }

    /** Rows whose text needs quoting: a comma, a quote and a newline, in fields both renderings carry. */
    private fun storeNeedingEscapes() = InMemoryVerdictStore().apply {
        record(grindVerdict("comma", "Fabric", verdict = Verdict.CONFIRMED, detail = "crashed, then exited"))
        record(grindVerdict("quote", "Forge", verdict = Verdict.CLEAR, detail = "said \"no\""))
        record(grindVerdict("newline", "NeoForge", verdict = Verdict.INCONCLUSIVE, detail = "line one\nline two"))
    }

    /** What streams out of `/export.csv` is byte for byte what the exporter renders for the same rows. */
    @Test
    fun theStreamedCsvIsExactlyTheExportersRendering() {
        val store = storeNeedingEscapes()
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            val expected = VerdictCsvExporter.toCsv(
                VerdictSelection.select(store.all(), VerdictQuery.parse(emptyMap(), null)).rows, preOrdered = true
            )
            Assertions.assertEquals(expected, get(server.port, "/export.csv").body())
        } finally {
            server.stop()
        }
    }

    /** What streams out of `/verdicts.json` is the document the same mapper writes for the same page in one go. */
    @Test
    fun theStreamedJsonIsTheDocumentTheMapperWritesWhole() {
        val store = storeNeedingEscapes()
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            val page = VerdictSelection.select(store.all(), VerdictQuery.parse(emptyMap(), null))
            val wireMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            val actual = wireMapper.readTree(get(server.port, "/verdicts.json").body())
            // The snapshot's version is the cache's own counter, pinned by the two version guards below.
            val expected = wireMapper.readTree(
                wireMapper.writeValueAsString(
                    linkedMapOf(
                        "total" to page.total, "matched" to page.matched, "page" to page.page, "pages" to page.pages,
                        "version" to versionOf(actual), "verdicts" to page.rows
                    )
                )
            )
            Assertions.assertEquals(expected, actual)
        } finally {
            server.stop()
        }
    }

    /** The feed serves JSON, off the live store, carrying the paging metadata a client needs to page. */
    @Test
    fun servesEveryVerdictAsJsonWithItsPagingMetadata() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            val response = get(server.port, "/verdicts.json")
            Assertions.assertEquals(200, response.statusCode())
            Assertions.assertTrue(
                response.headers().firstValue("Content-Type").orElse("").contains("application/json"),
                "The feed must announce itself as JSON, or a client is entitled to refuse to parse it."
            )

            val document = mapper.readTree(response.body())
            Assertions.assertEquals(4, document["total"].asInt())
            Assertions.assertEquals(4, document["matched"].asInt())
            Assertions.assertEquals(1, document["page"].asInt())
            Assertions.assertEquals(1, document["pages"].asInt())

            val slugs = document["verdicts"].map { it["slug"].asText() }
            Assertions.assertEquals(
                setOf("creativecore", "bookshelf", "iceberg", "prism"), slugs.toSet()
            )
        } finally {
            server.stop()
        }
    }

    /**
     * The plugin's whole premise: ask for one verdict class and get only that class. Same `f.<field>`
     * spelling the table and the CSV take, because it is the same parser.
     */
    @Test
    fun filtersByVerdictTheSameWayTheTableDoes() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            val document = mapper.readTree(get(server.port, "/verdicts.json?f.verdict=CONFIRMED").body())
            Assertions.assertEquals(4, document["total"].asInt(), "total is the store, not the selection")
            Assertions.assertEquals(1, document["matched"].asInt())

            val rows = document["verdicts"]
            Assertions.assertEquals(1, rows.size())
            Assertions.assertEquals("creativecore", rows[0]["slug"].asText())
            Assertions.assertEquals("CONFIRMED", rows[0]["verdict"].asText())
        } finally {
            server.stop()
        }
    }

    /**
     * The row a plugin actually consumes: `suggestedEntry` is the clientside-mods entry — the very field
     * [FallbackPropertiesRenderer] publishes — and `stagedDependencies` must arrive as a JSON array
     * rather than the comma-joined string the CSV flattens it to. Those two are the reason this endpoint
     * exists at all, so they are asserted rather than assumed.
     */
    @Test
    fun carriesTheEntryAndTheDependenciesInTheirOwnShapes() {
        val store = InMemoryVerdictStore().apply {
            record(
                grindVerdict("creativecore", "Fabric", verdict = Verdict.CONFIRMED)
                    .copy(stagedDependencies = listOf("cloth-config", "fabric-api"))
            )
        }
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            val row = mapper.readTree(get(server.port, "/verdicts.json").body())["verdicts"][0]
            Assertions.assertEquals("creativecore-", row["suggestedEntry"].asText())
            Assertions.assertTrue(row["stagedDependencies"].isArray)
            Assertions.assertEquals(
                listOf("cloth-config", "fabric-api"), row["stagedDependencies"].map { it.asText() }
            )
        } finally {
            server.stop()
        }
    }

    /**
     * `ReportServer`'s mapper had no `JavaTimeModule`, which serialises an [Instant] as an object of
     * `{"epochSecond":…,"nano":…}` — parseable, but not a timestamp any client would recognise, and not
     * what the store writes to disk. Pinned as a string so the wire shape and the on-disk shape agree.
     */
    @Test
    fun writesTheScanTimestampAsAnIsoStringRatherThanAnEpochObject() {
        val store = InMemoryVerdictStore().apply {
            record(grindVerdict("creativecore", "Fabric", verifiedAt = Instant.parse("2026-09-04T12:30:00Z")))
        }
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            val scannedAt = mapper.readTree(get(server.port, "/verdicts.json").body())["verdicts"][0]["verifiedAt"]
            Assertions.assertTrue(scannedAt.isTextual, "an epoch object is not a timestamp a client can read")
            Assertions.assertEquals("2026-09-04T12:30:00Z", scannedAt.asText())
        } finally {
            server.stop()
        }
    }

    /**
     * The agreement guard. The JSON and the CSV are two renderings of one selection, so for the same
     * query they must return the same rows — and they do because both call [VerdictSelection.select],
     * not because two row-pickers were kept in step. A bare call is unpaged in both, which is the
     * documented `/export.csv` behaviour operators already script against.
     */
    @Test
    fun selectsExactlyTheRowsTheCsvExportSelects() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            for (query in listOf("", "?f.verdict=CONFIRMED", "?q=ice", "?sort=name&dir=desc")) {
                val fromJson = mapper.readTree(get(server.port, "/verdicts.json$query").body())["verdicts"]
                    .map { it["slug"].asText() }
                // Drop the header row; the Name column is first, per VerdictField's declaration order.
                val fromCsv = get(server.port, "/export.csv$query").body()
                    .lineSequence().drop(1).filter { it.isNotBlank() }.map { it.substringBefore(',') }.toList()
                Assertions.assertEquals(fromCsv, fromJson, "the two renderings disagreed for '$query'")
            }
        } finally {
            server.stop()
        }
    }

    /**
     * The status endpoint is scripted against, and adding a date module to the shared mapper is exactly
     * the kind of change that quietly reshapes a neighbouring document. It writes only primitives, so
     * the fix must be inert here — asserted rather than reasoned about.
     */
    @Test
    fun leavesTheStatusDocumentUntouched() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            val status = mapper.readTree(get(server.port, "/status").body())
            Assertions.assertEquals(4, status["verdicts"].asInt())
            Assertions.assertTrue(status["activity"].isNull)
        } finally {
            server.stop()
        }
    }

    /**
     * The paging metadata is only meaningful when a page is actually asked for — every other guard here
     * runs unpaged, where `page` and `pages` are trivially 1 and a renderer emitting constants would
     * satisfy them. A client that pages needs to know where in the set it landed.
     */
    @Test
    fun reportsWhereInTheSetAPageSits() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            val second = mapper.readTree(get(server.port, "/verdicts.json?size=2&page=2").body())
            Assertions.assertEquals(4, second["total"].asInt())
            Assertions.assertEquals(4, second["matched"].asInt())
            Assertions.assertEquals(2, second["page"].asInt())
            Assertions.assertEquals(2, second["pages"].asInt())
            Assertions.assertEquals(2, second["verdicts"].size())

            // The two pages must partition the set rather than overlap or drop a row.
            val first = mapper.readTree(get(server.port, "/verdicts.json?size=2&page=1").body())
            val paged = first["verdicts"].map { it["slug"].asText() } + second["verdicts"].map { it["slug"].asText() }
            Assertions.assertEquals(
                mapper.readTree(get(server.port, "/verdicts.json").body())["verdicts"].map { it["slug"].asText() },
                paged,
                "paging must slice the same ordering the unpaged call returns"
            )
        } finally {
            server.stop()
        }
    }

    /** The `version` of a page, failing with the whole document when it carries none. */
    private fun versionOf(document: JsonNode): Long {
        val version = document["version"]
        Assertions.assertTrue(
            version != null && version.isIntegralNumber,
            "the page carries no integral version, so a paging client cannot tell its pages apart: $document"
        )
        return version.asLong()
    }

    /**
     * **Every page names the snapshot it was cut from, and that name moves when the rows do.** Offset paging
     * over a store a grind records into can shift a row past a client's cursor while `matched` stays put —
     * one key removed ahead of the cursor, one added behind it — so the count cannot reveal the skip. Equal
     * versions across a client's pages are what tells it every page was sliced from the same rows.
     */
    @Test
    fun carriesTheVersionOfTheSnapshotEachPageWasCutFrom() {
        val store = populatedStore()
        val server = ReportServer(store, requestedPort = 0, reportCacheMaxAge = Duration.ZERO).start()
        try {
            val first = versionOf(mapper.readTree(get(server.port, "/verdicts.json?size=2&page=1").body()))
            val second = versionOf(mapper.readTree(get(server.port, "/verdicts.json?size=2&page=2").body()))
            Assertions.assertEquals(first, second, "nothing was recorded between the two pages")

            store.record(grindVerdict("sodium", "Fabric", verdict = Verdict.CONFIRMED))

            val after = versionOf(mapper.readTree(get(server.port, "/verdicts.json?size=2&page=2").body()))
            Assertions.assertNotEquals(first, after, "a verdict was recorded, yet the page claims the same rows")
        } finally {
            server.stop()
        }
    }

    /**
     * **The version is the snapshot's, not the store's.** Inside the coalescing window a page is cut from the
     * held snapshot although the store has moved on; the store's counter would then name rows the page does not
     * contain, and send a client restarting over a feed that never changed under it.
     *
     * Uses the default window and the real clock, as `theDefaultWindowCoalescesRatherThanRebuildingPerVerdict`
     * does: two requests milliseconds apart are inside any usable window.
     */
    @Test
    fun aPageCutFromTheHeldSnapshotCarriesThatSnapshotsVersion() {
        val store = populatedStore()
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            val first = mapper.readTree(get(server.port, "/verdicts.json?size=2&page=1").body())

            store.record(grindVerdict("sodium", "Fabric", verdict = Verdict.CONFIRMED))

            val second = mapper.readTree(get(server.port, "/verdicts.json?size=2&page=2").body())
            Assertions.assertEquals(4, second["matched"].asInt(), "the second page was not cut from the held snapshot")
            Assertions.assertEquals(
                versionOf(first), versionOf(second),
                "both pages were cut from one snapshot, so they must carry one version"
            )
        } finally {
            server.stop()
        }
    }

    /**
     * **A page asked for with `version=` is cut from the snapshot that version names, whatever was recorded since.**
     * Without it, a read that outlasts the coalescing window during a grind sees a new version and restarts,
     * possibly every time; pinned, it reads one snapshot from first page to last.
     */
    @Test
    fun servesAPinnedPageFromTheSnapshotItsVersionNames() {
        val store = populatedStore()
        val server = ReportServer(store, requestedPort = 0, reportCacheMaxAge = Duration.ZERO).start()
        try {
            val unpinned = mapper.readTree(get(server.port, "/verdicts.json").body())["verdicts"].map { it["slug"].asText() }
            val first = mapper.readTree(get(server.port, "/verdicts.json?size=2&page=1").body())
            val version = versionOf(first)

            store.record(grindVerdict("aaa-sorts-first", "Fabric", verdict = Verdict.CONFIRMED))

            val second = mapper.readTree(get(server.port, "/verdicts.json?size=2&page=2&version=$version").body())
            Assertions.assertEquals(version, versionOf(second), "the pinned page came from another snapshot")
            Assertions.assertEquals(4, second["matched"].asInt(), "the pinned page counted rows recorded after it")
            Assertions.assertEquals(
                unpinned,
                first["verdicts"].map { it["slug"].asText() } + second["verdicts"].map { it["slug"].asText() },
                "the two pages must partition the snapshot the read started from"
            )
        } finally {
            server.stop()
        }
    }

    /**
     * **A version no longer retained is answered `410 Gone`, distinctly from every other outcome.** The client must
     * be able to tell "start over" apart from "the daemon is broken" and from a page of a different snapshot, so
     * the daemon never quietly substitutes the current snapshot for an evicted one.
     */
    @Test
    fun answersGoneForAVersionNoLongerRetained() {
        val server = ReportServer(populatedStore(), requestedPort = 0, reportCacheMaxAge = Duration.ZERO).start()
        try {
            val response = get(server.port, "/verdicts.json?size=2&page=2&version=12345")

            Assertions.assertEquals(410, response.statusCode(), response.body())
            val body = mapper.readTree(response.body())
            Assertions.assertEquals(12345L, body["version"]?.asLong(), "the answer must name the version it refused")
            Assertions.assertTrue(body["error"]?.isTextual == true, "the answer must say why: ${response.body()}")
        } finally {
            server.stop()
        }
    }

    /** A `version=` that is not a number is ignored like any unreadable parameter: the current snapshot answers. */
    @Test
    fun anUnreadableVersionFallsBackToTheCurrentSnapshot() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            val response = get(server.port, "/verdicts.json?size=2&page=1&version=banana")

            Assertions.assertEquals(200, response.statusCode())
            versionOf(mapper.readTree(response.body()))
        } finally {
            server.stop()
        }
    }

    /**
     * A page past the end clamps rather than answering with a negative offset or an error. These arrive
     * from bookmarks and hand-edited URLs, the same reason the table's own query parser never throws.
     */
    @Test
    fun clampsAPageBeyondTheEnd() {
        val server = ReportServer(populatedStore(), requestedPort = 0).start()
        try {
            val document = mapper.readTree(get(server.port, "/verdicts.json?size=2&page=99").body())
            Assertions.assertEquals(2, document["pages"].asInt())
            Assertions.assertEquals(2, document["page"].asInt(), "the last page, not page 99")
            Assertions.assertEquals(2, document["verdicts"].size())
        } finally {
            server.stop()
        }
    }
}
