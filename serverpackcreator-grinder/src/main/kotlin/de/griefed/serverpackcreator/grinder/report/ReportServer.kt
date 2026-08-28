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
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import de.griefed.serverpackcreator.grinder.GrinderStatus
import de.griefed.serverpackcreator.grinder.ModPlatforms
import de.griefed.serverpackcreator.grinder.loader.LoaderCache
import de.griefed.serverpackcreator.grinder.source.CursorStore
import de.griefed.serverpackcreator.grinder.source.RequeueStore
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The grinder's tiny web interface: serves the live verdict table (`/`) and the CSV export
 * (`/export.csv`) straight off the [VerdictStore], using the JDK's built-in HTTP server so the
 * standalone service needs **no web framework** (no Spring, no new dependency). Bound to loopback by
 * default; pass a concrete port or `0` for an ephemeral one.
 *
 * The browser-tab icon ships *inside* the jar and is served from there, keeping the promise the rest of the
 * page already keeps: nothing here reaches out to an external asset.
 *
 * @param store The verdicts to render; read live on each request so the table reflects the running grind.
 * @param requestedPort The port to bind (0 = pick a free one; read it back from [port] after [start]).
 * @param host The interface to bind; loopback by default so the report isn't exposed beyond the box.
 * @param fallbackLists Supplies the lists `/as-properties` publishes alongside the grinder's findings, read
 *                      per request so a refreshed list is served without a restart. `null` serves the
 *                      grinder's own findings only — the report server stays constructible without SPC.
 * @param crashLogs The kept consoles of crashed boots, linked from the table and served by name. `null`
 *                  simply offers no links, so the report stays constructible without a log store.
 * @param requeue The immediate re-grind queue, reported as a backlog count on `/status` so a queued
 *                re-verification is visible rather than inferred from the logs.
 * @author Griefed
 */
class ReportServer(
    private val store: VerdictStore,
    requestedPort: Int = 8757,
    host: String = "127.0.0.1",
    private val status: GrinderStatus? = null,
    private val cursors: CursorStore? = null,
    private val cacheRoot: File? = null,
    private val fallbackLists: (() -> FallbackLists)? = null,
    private val crashLogs: BootLogStore? = null,
    private val requeue: RequeueStore? = null
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * The tab icon, read off the classpath once and held: it is a few kilobytes and every page load asks for
     * it, so re-reading the jar entry per request buys nothing. `null` if the resource is somehow absent, in
     * which case the pages simply go without an icon — a missing decoration must not 500 an endpoint.
     */
    private val favicon: ByteArray? by lazy {
        val bytes = javaClass.getResourceAsStream(FAVICON_RESOURCE)?.use { stream -> stream.readBytes() }
        if (bytes == null) {
            log.warn("No $FAVICON_RESOURCE on the classpath; the report is served without a tab icon.")
        }
        bytes
    }
    private val server: HttpServer = HttpServer.create(InetSocketAddress(host, requestedPort), 0)
    private var pool: ExecutorService? = null
    private val mapper = jacksonObjectMapper()

    /** The actually-bound port (meaningful after [start], especially when an ephemeral `0` was asked). */
    val port: Int get() = server.address.port

    /** Register the routes, start serving on a small thread pool, and return `this` for chaining. */
    fun start(): ReportServer {
        // Longest-prefix match means /export.csv wins for that path; everything else renders the table.
        server.createContext("/export.csv") { exchange ->
            respond(exchange, "text/csv; charset=utf-8", VerdictCsvExporter.toCsv(store.all()))
        }
        server.createContext("/as-properties") { exchange ->
            respond(exchange, "text/x-java-properties; charset=iso-8859-1", fallbackProperties())
        }
        server.createContext("/status") { exchange ->
            respond(exchange, "application/json; charset=utf-8", statusJson())
        }
        // Both spellings need their own context: the pages link `/favicon.png`, while a browser asks for
        // `/favicon.ico` unprompted on every endpoint that is not HTML (the plain-text crash consoles). Without
        // a context of its own, either request falls through to `/` and gets the verdict table as its icon.
        for (iconPath in listOf("/favicon.ico", "/favicon.png")) {
            server.createContext(iconPath) { exchange -> respondFavicon(exchange) }
        }
        // Longest-prefix match again: /crash-logs is its own context, so /crash-log cannot swallow it.
        server.createContext("/crash-logs") { exchange ->
            respond(exchange, "text/html; charset=utf-8", crashLogIndex())
        }
        server.createContext("/crash-log") { exchange ->
            val name = queryParameter(exchange.requestURI.rawQuery, "name")
            // `read` is what enforces that a name cannot escape the store; a refusal is indistinguishable
            // from an absent log on purpose, so probing tells an unauthenticated caller nothing.
            val body = name?.let { crashLogs?.read(it) }
            if (body == null) {
                respond(exchange, "text/plain; charset=utf-8", "No such crash log.", status = 404)
            } else {
                respond(exchange, "text/plain; charset=utf-8", body)
            }
        }
        server.createContext("/") { exchange ->
            respond(
                exchange,
                "text/html; charset=utf-8",
                VerdictReportRenderer.toHtml(store.all()) { verdict ->
                    crashLogs?.nameFor(verdict.platform, verdict.slug, verdict.loader)
                }
            )
        }
        pool = Executors.newFixedThreadPool(2).also { server.executor = it }
        server.start()
        log.info("Grinder report available at http://${server.address.hostString}:$port/")
        return this
    }

    /**
     * The `serverpackcreator.properties` fragment an SPC instance polls: the lists this daemon knows plus
     * every crash-proven finding. A failing list-source degrades to the findings alone rather than to a 500 —
     * this endpoint is polled unattended, and an error there is an error in somebody's log forever.
     */
    private fun fallbackProperties(): String {
        val lists = fallbackLists?.let { source -> runCatching { source() }.getOrNull() }
            ?: FallbackLists(emptyList(), emptyList())
        return FallbackPropertiesRenderer.render(lists.clientsideMods, lists.whitelist, store.all())
    }

    /**
     * The index of kept crash consoles: every boot whose server died, linkable without first finding its row
     * in the table. Deliberately plain — it is a list of file names, and the page that gives them meaning is
     * the verdict table this links back to.
     */
    private fun crashLogIndex(): String {
        val names = crashLogs?.list().orEmpty()
        val items = names.joinToString("\n") { name ->
            """  <li><a href="/crash-log?name=${URLEncoder.encode(name, StandardCharsets.UTF_8)}">$name</a></li>"""
        }
        val body = if (names.isEmpty()) "<p>No crashed boots have been recorded yet.</p>" else "<ul>\n$items\n</ul>"
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <title>ServerPackCreator — crash consoles</title>
              <link rel="icon" type="image/png" href="/favicon.png">
            </head>
            <body style="font-family: system-ui, sans-serif; margin: 1.5rem;">
              <h1>Crash consoles (${names.size})</h1>
              <p><a href="/">&larr; back to the verdict table</a></p>
              $body
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Serve the bundled tab icon, or a 404 when the jar carries none. PNG under both `.png` and `.ico`: every
     * current browser reads the bytes, not the extension, and one file beats shipping a second format.
     */
    private fun respondFavicon(exchange: HttpExchange) {
        val icon = favicon
        if (icon == null) {
            respond(exchange, "text/plain; charset=utf-8", "No favicon is bundled with this build.", status = 404)
        } else {
            respondBytes(exchange, "image/png", icon)
        }
    }

    /**
     * The value of [key] in a raw query string, percent-decoded, or `null` when absent.
     *
     * Hand-rolled because the JDK's HTTP server hands over the raw query and this daemon deliberately carries
     * no web framework to parse one. A malformed escape decodes to `null` rather than throwing — a bad query
     * is a 404, never a 500 in somebody's log.
     */
    private fun queryParameter(rawQuery: String?, key: String): String? =
        rawQuery?.split('&')
            ?.firstOrNull { it.substringBefore('=') == key }
            ?.substringAfter('=', "")
            ?.let { runCatching { URLDecoder.decode(it, StandardCharsets.UTF_8) }.getOrNull() }
            ?.ifEmpty { null }

    /** Stop serving and shut the thread pool down. */
    fun stop() {
        server.stop(0)
        pool?.shutdownNow()
    }

    /**
     * The live activity document: what the daemon is doing *now*, as opposed to what it has found. Answers the
     * operator question the verdict table cannot — which pass, which candidate each worker holds and for how
     * long, where the crawl stands per platform, and how big the install cache has grown.
     *
     * Serialized with Jackson rather than hand-built, so a mod slug containing quotes or braces cannot break the
     * document. Anything unavailable (no status/cursors/cache wired, or an unreadable cache dir) is reported as
     * `null`/absent rather than failing the request — a monitoring endpoint that 500s is worse than a thin one.
     */
    private fun statusJson(): String {
        val document = linkedMapOf<String, Any?>(
            "verdicts" to store.all().size,
            // How much work is waiting in the jump-the-crawl lane. Read live: an operator queueing a re-grind
            // wants to see it land, and a backlog that never shrinks is the symptom of a stalled pass.
            "requeued" to requeue?.pending(),
            "activity" to status?.snapshot(),
            "crawl" to cursors?.let { store ->
                ModPlatforms.known.associateWith { platform ->
                    val cursor = store.cursor(platform)
                    linkedMapOf("offset" to cursor.offset, "sweeps" to cursor.sweeps, "partition" to cursor.partition)
                }
            },
            "loaderCache" to cacheRoot?.let { root ->
                runCatching {
                    val tuples = root.walkTopDown().maxDepth(4).count { it.name == LoaderCache.MARKER }
                    linkedMapOf("installedTuples" to tuples, "path" to root.absolutePath)
                }.getOrNull()
            }
        )
        return runCatching { mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document) }
            .getOrElse { "{\"error\":\"status unavailable\"}" }
    }

    /**
     * Write [body] with the given [contentType] and [status] (200 unless a route says otherwise, which only
     * the crash-log lookup does), closing the exchange.
     *
     * UTF-8 for every endpoint, including `/as-properties`: that document declares ISO-8859-1 because
     * `Properties.load(InputStream)` decodes it that way, but `FallbackPropertiesRenderer` escapes everything
     * outside printable ASCII to `\uXXXX`, and the two encodings agree byte for byte there. Encoding it
     * "correctly" would be a branch that can never change an output.
     */
    private fun respond(exchange: HttpExchange, contentType: String, body: String, status: Int = 200) =
        respondBytes(exchange, contentType, body.toByteArray(StandardCharsets.UTF_8), status)

    /**
     * Write [bytes] verbatim with the given [contentType] and [status], closing the exchange. The one endpoint
     * that needs it is the icon — every other response is text, and goes through [respond] above.
     */
    private fun respondBytes(exchange: HttpExchange, contentType: String, bytes: ByteArray, status: Int = 200) {
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    /** Where the bundled tab icon lives. Private: which resource backs the icon routes is nobody else's business. */
    companion object {
        /**
         * Classpath location of the tab icon, resolved relative to this class's package so it travels with the
         * jar. It is ServerPackCreator's own configuration glyph (`img/config.png`), the same mark the app uses.
         */
        private const val FAVICON_RESOURCE = "favicon.png"
    }
}
