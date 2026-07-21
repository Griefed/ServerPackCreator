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

import de.griefed.serverpackcreator.clientside.Confidence
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * Pins the JDK-HttpServer report endpoint over a real loopback socket (ephemeral port): `/` serves the
 * HTML table, `/export.csv` serves the CSV, both read **live** off the store, with the right content
 * types. No framework — just the built-in HTTP server.
 */
internal class ReportServerTest {

    private fun get(port: Int, path: String) = HttpClient.newHttpClient()
        .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).build(), BodyHandlers.ofString())

    @Test
    fun servesTheHtmlTableAndTheCsvExport() {
        val store = InMemoryVerdictStore().apply {
            record(grindVerdict("jei", "Forge", confidence = Confidence.HIGH, suggestedEntry = "jei-"))
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
            Assertions.assertTrue(csv.body().startsWith("Name,Project,NamePattern,Confidence,Loader,Detail"))
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
}
