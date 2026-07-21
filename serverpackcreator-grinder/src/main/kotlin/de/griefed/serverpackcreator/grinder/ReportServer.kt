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
package de.griefed.serverpackcreator.grinder

import com.sun.net.httpserver.HttpExchange
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
    host: String = "127.0.0.1"
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val server: HttpServer = HttpServer.create(InetSocketAddress(host, requestedPort), 0)
    private var pool: ExecutorService? = null

    /** The actually-bound port (meaningful after [start], especially when an ephemeral `0` was asked). */
    val port: Int get() = server.address.port

    /** Register the routes, start serving on a small thread pool, and return `this` for chaining. */
    fun start(): ReportServer {
        // Longest-prefix match means /export.csv wins for that path; everything else renders the table.
        server.createContext("/export.csv") { exchange ->
            respond(exchange, "text/csv; charset=utf-8", VerdictCsvExporter.toCsv(store.all()))
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

    /** Write [body] as a 200 response with the given [contentType], closing the exchange. */
    private fun respond(exchange: HttpExchange, contentType: String, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
