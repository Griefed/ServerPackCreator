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
import de.griefed.serverpackcreator.clientside.AttemptDirectory
import de.griefed.serverpackcreator.clientside.BootArtifacts
import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.GrindCandidate
import de.griefed.serverpackcreator.grinder.GrinderStatus
import de.griefed.serverpackcreator.grinder.ModPlatforms
import de.griefed.serverpackcreator.grinder.grindVerdict
import de.griefed.serverpackcreator.grinder.loader.LoaderCache
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
import java.util.Properties

/**
 * Pins the JDK-HttpServer report endpoint over a real loopback socket (ephemeral port): `/` serves the
 * HTML table, `/export.csv` serves the CSV, both read **live** off the store, with the right content
 * types. No framework — just the built-in HTTP server.
 */
internal class ReportServerTest {

    private fun get(port: Int, path: String) = HttpClient.newHttpClient()
        .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).build(), BodyHandlers.ofString())

    /** The same request, kept as bytes — an icon must be asserted on its own bytes, not on a decoded string. */
    private fun getBytes(port: Int, path: String) = HttpClient.newHttpClient()
        .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).build(), BodyHandlers.ofByteArray())

    /**
     * The crash console a HIGH verdict was reached from, served by name so the overview can link it. The
     * name is untrusted input off a query string, so the traversal case is pinned in the same test as the
     * happy path — they are the same code path with different input.
     */
    @Test
    fun servesAKeptCrashLogAndRefusesToEscapeItsStore(@TempDir logDir: File) {
        val secret = File(logDir.parentFile, "secret.txt").apply { writeText("not yours") }
        val crashLogs = BootLogStore(logDir)
        val name = crashLogs.keep(
            AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "creativecore", "Fabric", "1.20"),
            BootLogStore.attemptKey("Fabric", "0.19.3", "26.2"),
            listOf(BootArtifacts.Artifact("console.log", "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft", false))
        ).single()
        val store = InMemoryVerdictStore().apply { record(grindVerdict("creativecore", "Fabric")) }
        val server = ReportServer(store, requestedPort = 0, crashLogs = crashLogs).start()
        try {
            val kept = get(server.port, "/boot-log?name=$name")
            Assertions.assertEquals(200, kept.statusCode())
            Assertions.assertTrue(kept.headers().firstValue("Content-Type").orElse("").contains("text/plain"))
            Assertions.assertTrue(kept.body().contains("net/minecraft/client/Minecraft"))

            // The superseded routes stay: both are documented, and an operator has them bookmarked.
            val viaAlias = get(server.port, "/crash-log?name=$name")
            Assertions.assertEquals(kept.body(), viaAlias.body(), "/crash-log must keep answering as an alias")

            val escaped = get(server.port, "/boot-log?name=../${secret.name}")
            Assertions.assertEquals(404, escaped.statusCode(), "a traversal must not be served")
            Assertions.assertFalse(escaped.body().contains("not yours"), "and must not leak the file either")

            for (index in listOf("/boot-logs", "/crash-logs")) {
                val listing = get(server.port, index)
                Assertions.assertEquals(200, listing.statusCode())
                Assertions.assertTrue(listing.body().contains(name), "$index lists what is kept")
            }
        } finally {
            server.stop()
        }
    }

    /**
     * Sorting by Logs, through the real handler and a real directory listing.
     *
     * The unit test pins the ordering; this pins the *wiring* — that the count the sort uses comes from the
     * same per-request snapshot the cells are rendered from. Those were two separate lookups in the first
     * draft, which is exactly how a row sorts as having logs and then renders an em-dash.
     */
    @Test
    fun sortsTheTableByHowManyLogsEachRowHas(@TempDir logDir: File) {
        val crashLogs = BootLogStore(logDir)
        crashLogs.keep(
            AttemptDirectory.nameFor(ModPlatforms.MODRINTH, "sodium", "Fabric", "1.20"),
            BootLogStore.attemptKey("Fabric", "0.16.9", "1.21.1"),
            listOf(
                BootArtifacts.Artifact("console.log", "crashed", false),
                BootArtifacts.Artifact("latest.log", "also crashed", false)
            )
        )
        val store = InMemoryVerdictStore().apply {
            record(grindVerdict("sodium", "Fabric"))
            record(grindVerdict("jei", "Forge"))
            record(grindVerdict("iron-chests", "NeoForge"))
        }
        val server = ReportServer(store, requestedPort = 0, crashLogs = crashLogs).start()
        try {
            val body = get(server.port, "/?sort=logs&dir=desc").body()

            Assertions.assertTrue(body.contains("sort=logs"), "the Logs header must render a sort link")
            val withLogs = body.indexOf("sodium")
            val without = listOf("jei", "iron-chests").minOf { body.indexOf(it) }
            Assertions.assertTrue(withLogs in 0..<without, "the row holding logs must lead a descending Logs sort")
            Assertions.assertTrue(body.contains("2 log(s)"), "and must still render the count it was sorted by")
        } finally {
            server.stop()
        }
    }

    @Test
    fun servesTheHtmlTableAndTheCsvExport() {
        val store = InMemoryVerdictStore().apply {
            record(grindVerdict("jei", "Forge", suggestedEntry = "jei-", verdict = Verdict.CONFIRMED))
        }
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            val html = get(server.port, "/")
            Assertions.assertEquals(200, html.statusCode())
            Assertions.assertTrue(html.headers().firstValue("Content-Type").orElse("").contains("text/html"))
            Assertions.assertTrue(html.body().contains(">jei-<"), "table must carry the name-pattern")

            val csv = get(server.port, "/export.csv")
            Assertions.assertEquals(200, csv.statusCode())
            Assertions.assertTrue(csv.headers().firstValue("Content-Type").orElse("").contains("text/csv"))
            Assertions.assertTrue(csv.body().startsWith("Name,Project,NamePattern,Filename,Verdict,Declared,Minecraft,MinecraftVersion,Loader"))
            Assertions.assertTrue(csv.body().contains("jei-"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun reflectsTheLiveStoreState() {
        val store = InMemoryVerdictStore()
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            // Recorded after the server started — the endpoint reads the store on each request.
            store.record(grindVerdict("late", "Fabric", suggestedEntry = "late-"))
            Assertions.assertTrue(get(server.port, "/export.csv").body().contains("late-"))
        } finally {
            server.stop()
        }
    }
    /**
     * The live-activity endpoint. An operator needs "what is it doing *now*" — which the verdict table cannot
     * answer — so `/status` reports the current pass, each busy worker and its candidate, the crawl position per
     * platform and the install-cache size, as JSON.
     */
    @Test
    fun statusReportsLiveActivityAsJson(@TempDir cacheRoot: File) {
        val status = GrinderStatus()
        status.beginPass(number = 7, candidates = 100)
        status.beginCandidate(GrindCandidate("https://modrinth.com/mod/sodium", "sodium", 5, ModPlatforms.MODRINTH))
        val cursors = InMemoryCursorStore().apply {
            store(ModPlatforms.MODRINTH, CatalogCursor(offset = 250, sweeps = 1))
            store(ModPlatforms.CURSEFORGE, CatalogCursor(offset = 50, sweeps = 0, partition = "1.20.1|*|1|desc"))
        }
        File(cacheRoot, "1.21.1/Forge/52.1.16").mkdirs()
        File(cacheRoot, "1.21.1/Forge/52.1.16/${LoaderCache.MARKER}").writeText("marker")
        val server = ReportServer(InMemoryVerdictStore(), 0, status = status, cursors = cursors, cacheRoot = cacheRoot).start()

        try {
            val response = get(server.port, "/status")

            Assertions.assertEquals(200, response.statusCode())
            Assertions.assertTrue(
                response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
                "content type was ${response.headers().firstValue("Content-Type")}"
            )
            val body = response.body()
            listOf("\"pass\" : 7", "\"slug\" : \"sodium\"", "\"platform\" : \"Modrinth\"",
                   "\"offset\" : 250", "1.20.1|*|1|desc", "\"installedTuples\" : 1")
                .forEach { Assertions.assertTrue(body.contains(it), "missing $it in:\n$body") }
        } finally {
            server.stop()
        }
    }

    /** Wired without the optional collaborators it still answers, with the parts it cannot know left null. */
    @Test
    fun statusStaysAvailableWhenNothingIsWiredIntoIt() {
        val server = ReportServer(InMemoryVerdictStore(), 0).start()
        try {
            val response = get(server.port, "/status")
            Assertions.assertEquals(200, response.statusCode(), "a monitoring endpoint must not 500 just because it is thin")
            Assertions.assertTrue(response.body().contains("\"activity\" : null"), response.body())
        } finally {
            server.stop()
        }
    }

    /**
     * Slugs come from the internet. Jackson escapes them, so a hostile one cannot break out of the document —
     * the same discipline the HTML and CSV renderers already follow.
     */
    @Test
    fun aHostileSlugCannotBreakTheJson() {
        val hostile = """evil", "admin": true, "x": """"
        val status = GrinderStatus()
        status.beginCandidate(GrindCandidate("https://x.invalid/a", hostile, 1, ModPlatforms.MODRINTH))
        val server = ReportServer(InMemoryVerdictStore(), 0, status = status).start()
        try {
            val parsed = jacksonObjectMapper().readTree(get(server.port, "/status").body())
            val worker = parsed.path("activity").path("workers").first()
            Assertions.assertEquals(hostile, worker.path("slug").asText(), "the slug must stay data, not become structure")
            Assertions.assertTrue(parsed.path("admin").isMissingNode, "no smuggled sibling keys")
        } finally {
            server.stop()
        }
    }


    /**
     * `/as-properties` is what a ServerPackCreator instance polls through its
     * `de.griefed.serverpackcreator.configuration.fallback.updateurl`. It must therefore be served as a
     * document `java.util.Properties` can load, carrying the repository's own list plus the grinder's
     * crash-proven findings — so the fallback list stops depending on a maintainer editing the repo.
     */
    @Test
    fun servesTheFallbackListAsPollableProperties() {
        val store = InMemoryVerdictStore().apply {
            record(grindVerdict("entityculling", "Fabric", verdict = Verdict.CONFIRMED, suggestedEntry = "entityculling-"))
            record(grindVerdict("inconclusive", "Forge", suggestedEntry = "inconclusive-", verdict = Verdict.CLEAR))
        }
        val server = ReportServer(
            store,
            requestedPort = 0,
            fallbackLists = { FallbackLists(clientsideMods = listOf("jei-"), whitelist = listOf("Ping-Wheel-")) }
        ).start()
        try {
            val response = get(server.port, "/as-properties")
            Assertions.assertEquals(200, response.statusCode())

            val parsed = Properties()
            parsed.load(response.body().byteInputStream(Charsets.ISO_8859_1))
            val entries = parsed.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")
                .orEmpty().split(",").map { it.trim() }
            Assertions.assertTrue(entries.contains("jei-"), "the repository list must be served: $entries")
            Assertions.assertTrue(entries.contains("entityculling-"), "a HIGH finding must be served: $entries")
            Assertions.assertFalse(entries.contains("inconclusive-"), "an unproven finding must never be served: $entries")
        } finally {
            server.stop()
        }
    }

    /**
     * The report's browser-tab icon. Both names are served on purpose: the pages link `/favicon.png`, and a
     * browser asks for `/favicon.ico` on its own on every other endpoint — the plain-text crash consoles
     * included. Neither may fall through to the catch-all context, which would answer an icon request with the
     * whole verdict table.
     *
     * Asserted on the PNG signature rather than on a non-empty body, because an HTML fall-through or a 404 page
     * is also a non-empty body with a 200 in front of it.
     */
    @Test
    fun servesTheFaviconAndReferencesItFromEveryPage() {
        val server = ReportServer(InMemoryVerdictStore(), requestedPort = 0).start()
        try {
            for (path in listOf("/favicon.ico", "/favicon.png")) {
                val icon = getBytes(server.port, path)
                Assertions.assertEquals(200, icon.statusCode(), path)
                Assertions.assertTrue(
                    icon.headers().firstValue("Content-Type").orElse("").contains("image/png"),
                    "$path was served as ${icon.headers().firstValue("Content-Type")}"
                )
                Assertions.assertArrayEquals(
                    byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()),
                    icon.body().take(4).toByteArray(),
                    "$path must be a real PNG, not a page that happens to answer 200"
                )
            }

            for (page in listOf("/", "/crash-logs")) {
                Assertions.assertTrue(
                    get(server.port, page).body().contains("""<link rel="icon" type="image/png" href="/favicon.png">"""),
                    "$page must point a browser at the icon"
                )
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun answersAsPropertiesEvenWithNoListSourceWiredIn() {
        // The report server is constructible without SPC (every other endpoint is), and a 500 on a polled
        // endpoint would have every client log an error forever.
        val server = ReportServer(InMemoryVerdictStore(), requestedPort = 0).start()
        try {
            Assertions.assertEquals(200, get(server.port, "/as-properties").statusCode())
        } finally {
            server.stop()
        }
    }

}
