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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.griefed.serverpackcreator.clientside.ConsoleRuleSet
import de.griefed.serverpackcreator.grinder.GrindCandidate
import de.griefed.serverpackcreator.grinder.GrinderStatus
import de.griefed.serverpackcreator.grinder.ModPlatforms
import de.griefed.serverpackcreator.grinder.grindVerdict
import de.griefed.serverpackcreator.grinder.source.CatalogCursor
import de.griefed.serverpackcreator.grinder.source.InMemoryCursorStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * Pins the live `/status` dashboard: a hand-written page that polls the JSON endpoint and renders it for a
 * human, with **no framework and no new dependency** — the one property that makes it worth having at all.
 *
 * The interesting failure here is not "the page 404s"; it is the page silently going blank because a field
 * it reads was renamed on the server. Nothing in a static HTML string can fail a build, so the guards below
 * cross-check the two sides against each other instead: what the page *declares* it reads, against what the
 * real `/status` document actually emits over a real socket.
 */
internal class StatusDashboardTest {

    private val mapper = jacksonObjectMapper()

    private fun get(port: Int, path: String) = HttpClient.newHttpClient()
        .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).build(), BodyHandlers.ofString())

    /** A server with every optional collaborator wired, so no field of the document is absent by omission. */
    private fun fullyWiredServer(cacheRoot: File): ReportServer {
        val store = InMemoryVerdictStore().apply { record(grindVerdict("creativecore", "Fabric")) }
        val status = GrinderStatus().apply {
            beginPass(number = 3, candidates = 40)
            beginCandidate(
                GrindCandidate(
                    projectUrl = "https://modrinth.com/mod/creativecore",
                    slug = "creativecore",
                    popularity = 9_000,
                    platform = ModPlatforms.MODRINTH
                )
            )
        }
        val cursors = InMemoryCursorStore().apply {
            store(ModPlatforms.MODRINTH, CatalogCursor(offset = 120, sweeps = 2, partition = null))
        }
        // consoleRules included deliberately: `bootRules` is null without it, and its sub-fields then cannot
        // resolve — which is exactly what this fixture claims to rule out. An error is seeded because the
        // errors list is the half of that panel worth having.
        val rules = ConsoleRuleSet(
            rules = emptyList(),
            errors = listOf("rule 'typo-verdict' names an unreadable verdict; treated as INCONCLUSIVE"),
            source = "deploy/boot-rules.example.json",
            undecidedVerdict = null
        )
        return ReportServer(
            store, requestedPort = 0, status = status, cursors = cursors,
            cacheRoot = cacheRoot, consoleRules = { rules }
        ).start()
    }

    /** Resolve a dotted path such as `activity.workers` against a document, or `null` where it does not exist. */
    private fun at(document: JsonNode, path: String): JsonNode? =
        path.split('.').fold(document as JsonNode?) { node, segment ->
            node?.takeIf { it.has(segment) }?.get(segment)
        }

    /**
     * **The guard that matters.** Every field the dashboard reads has to exist in the document the server
     * really serves. A rename on either side — `passRunningSeconds`, `busySeconds`, `installedTuples` — would
     * otherwise leave a page that loads, polls, returns 200 and quietly renders nothing, which is the one
     * failure mode a static page cannot report for itself.
     */
    @Test
    fun everyFieldTheDashboardReadsExistsInTheStatusDocument(@TempDir cacheRoot: File) {
        val server = fullyWiredServer(cacheRoot)
        try {
            val document = mapper.readTree(get(server.port, "/status").body())

            val missing = StatusDashboardRenderer.READ_FIELDS.filter { at(document, it) == null }

            Assertions.assertTrue(
                missing.isEmpty(),
                "the dashboard reads fields /status does not emit: $missing\ndocument was:\n$document"
            )
        } finally {
            server.stop()
        }
    }

    /**
     * And the declaration cannot drift from the page itself: every field named in [READ_FIELDS] has to appear
     * in the markup that reads it. Without this the list stays green while the page reads something else.
     */
    @Test
    fun theDeclaredFieldsAreTheOnesThePageActuallyNames() {
        val page = StatusDashboardRenderer.toHtml()

        val unnamed = StatusDashboardRenderer.READ_FIELDS
            .map { it.substringAfterLast('.') }
            .filterNot { page.contains(it) }

        Assertions.assertTrue(unnamed.isEmpty(), "declared but never read in the page: $unnamed")
    }

    /** It polls, or it is not a live dashboard — and it has to say where it polls. */
    @Test
    fun theDashboardPollsTheStatusEndpoint() {
        val page = StatusDashboardRenderer.toHtml()

        Assertions.assertTrue(page.contains("/status"), "the page has to fetch /status")
        Assertions.assertTrue(page.contains("setInterval") || page.contains("setTimeout"), "it has to poll")
    }

    /**
     * **No new dependencies, asserted rather than intended.** The report is reachable from a browser that may
     * have no route to a CDN at all — it is documented as loopback-bound behind a reverse proxy — so a page
     * pulling a framework off the network would be blank exactly where it is most needed. Nothing may be
     * fetched from anywhere but this server.
     */
    @Test
    fun theDashboardIsSelfContainedWithNoExternalResources(@TempDir cacheRoot: File) {
        val server = fullyWiredServer(cacheRoot)
        try {
            val response = get(server.port, "/dashboard")

            Assertions.assertEquals(200, response.statusCode())
            Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"))

            val external = Regex("""(src|href)\s*=\s*["'](https?:)?//[^"']+""", RegexOption.IGNORE_CASE)
                .findAll(response.body()).map { it.value }.toList()
            Assertions.assertTrue(external.isEmpty(), "the dashboard must load nothing off the network: $external")
        } finally {
            server.stop()
        }
    }

    /**
     * The page carries no store data at all, which is what makes it un-injectable. Slugs are internet-supplied
     * and this server has no authentication; the dashboard sidesteps escaping entirely by being a constant and
     * inserting every value client-side as text. Pinned by serving it against a hostile slug and asserting the
     * bytes are the same ones the renderer produces with no store in sight.
     */
    @Test
    fun theServedPageIsAConstantAndCarriesNoStoreData(@TempDir cacheRoot: File) {
        val hostile = "<script>alert(1)</script>"
        val store = InMemoryVerdictStore().apply { record(grindVerdict(hostile, "Fabric")) }
        val server = ReportServer(store, requestedPort = 0, cacheRoot = cacheRoot).start()
        try {
            val body = get(server.port, "/dashboard").body()

            Assertions.assertEquals(StatusDashboardRenderer.toHtml(), body)
            Assertions.assertFalse(body.contains("alert(1)"), "no store data may reach the page")
        } finally {
            server.stop()
        }
    }

    /** An endpoint nothing links to is an endpoint nobody finds — the overview carries a button for each. */
    @Test
    fun theOverviewLinksTheDashboard(@TempDir cacheRoot: File) {
        val server = fullyWiredServer(cacheRoot)
        try {
            Assertions.assertTrue(
                get(server.port, "/").body().contains("href=\"/dashboard\""),
                "the overview's toolbar has to link the dashboard"
            )
        } finally {
            server.stop()
        }
    }
}
