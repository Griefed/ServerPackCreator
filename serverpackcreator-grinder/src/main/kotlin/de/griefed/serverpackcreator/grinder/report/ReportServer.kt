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
import de.griefed.serverpackcreator.grinder.GrinderStatus
import de.griefed.serverpackcreator.grinder.ModPlatforms
import de.griefed.serverpackcreator.grinder.loader.LoaderCache
import de.griefed.serverpackcreator.grinder.source.CursorStore
import java.io.File
import com.sun.net.httpserver.HttpServer
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The grinder's tiny web interface: serves the live verdict table (`/`) and the CSV export
 * (`/export.csv`) straight off the [VerdictStore], using the JDK's built-in HTTP server so the
 * standalone service needs **no web framework** (no Spring, no new dependency). Bound to loopback by
 * default; pass a concrete port or `0` for an ephemeral one.
 *
 * @param store The verdicts to render; read live on each request so the table reflects the running grind.
 * @param requestedPort The port to bind (0 = pick a free one; read it back from [port] after [start]).
 * @param host The interface to bind; loopback by default so the report isn't exposed beyond the box.
 * @author Griefed
 */
class ReportServer(
    private val store: VerdictStore,
    requestedPort: Int = 8757,
    host: String = "127.0.0.1",
    private val status: GrinderStatus? = null,
    private val cursors: CursorStore? = null,
    private val cacheRoot: File? = null
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
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
        server.createContext("/status") { exchange ->
            respond(exchange, "application/json; charset=utf-8", statusJson())
        }
        server.createContext("/") { exchange ->
            respond(exchange, "text/html; charset=utf-8", VerdictReportRenderer.toHtml(store.all()))
        }
        pool = Executors.newFixedThreadPool(2).also { server.executor = it }
        server.start()
        log.info("Grinder report available at http://${server.address.hostString}:$port/")
        return this
    }

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

    /** Write [body] as a 200 response with the given [contentType], closing the exchange. */
    private fun respond(exchange: HttpExchange, contentType: String, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
