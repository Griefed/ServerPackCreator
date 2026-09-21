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

import de.griefed.serverpackcreator.grinder.GrindVerdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins how often the report **walks the whole store**, which is the cost that grows with a long-running
 * grind while the page stays 250 rows.
 *
 * These assert a *count of scans*, not a duration, deliberately: a timing assertion is flaky on CI and says
 * nothing about why it got slow, whereas "this endpoint copied every verdict" is the defect itself and is
 * exactly reproducible. At the deployed scale the difference is not academic — selection was measured at
 * 251 ms for 38,258 verdicts, and the store has since passed the high six figures.
 *
 * @author Griefed
 */
internal class ReportStoreScanTest {

    private fun get(port: Int, path: String) = HttpClient.newHttpClient()
        .send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).build(), BodyHandlers.ofString())

    /**
     * A store that records how often anything asked it for **every** verdict, so a test can assert that an
     * endpoint did not. Delegates everything else to a real [InMemoryVerdictStore] — the point is to observe
     * the production store's traffic, not to reimplement it.
     */
    private class CountingStore(private val delegate: VerdictStore = InMemoryVerdictStore()) : VerdictStore {
        val scans = AtomicInteger(0)

        override fun record(verdict: GrindVerdict) = delegate.record(verdict)

        override fun all(): List<GrindVerdict> {
            scans.incrementAndGet()
            return delegate.all()
        }

        // MUST delegate rather than inherit. VerdictStore.count DEFAULTS to `all().size`, so a double that
        // leaves it alone counts a scan for every count -- which would make this class report the very defect
        // it exists to detect, whatever the production code does.
        override val count: Int get() = delegate.count

        override val version: Long get() = delegate.version
    }

    /**
     * `/status` must not copy the store to report how many verdicts there are.
     *
     * It read `store.all().size`, which allocates a list of every verdict to look at one integer — on a
     * 700k-row store that is a 700k-element copy per request, and `/status` is what the dashboard polls on a
     * timer. The count is available from the map directly.
     */
    @Test
    fun statusReportsTheCountWithoutCopyingTheStore() {
        val store = CountingStore()
        repeat(5) { index -> store.record(grindVerdict("mod$index", "Forge")) }
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            store.scans.set(0)

            val response = get(server.port, "/status")

            Assertions.assertEquals(200, response.statusCode())
            Assertions.assertTrue(
                response.body().contains("\"verdicts\" : 5"),
                "/status must still report the real count, body was: ${response.body()}"
            )
            Assertions.assertEquals(
                0,
                store.scans.get(),
                "/status copied the entire store to read a count"
            )
        } finally {
            server.stop()
        }
    }

    /**
     * `/as-properties` must read through the snapshot cache like every other endpoint.
     *
     * It called `store.all()` directly, so it was the one endpoint the caching never covered — and it is the
     * one **polled unattended by every SPC instance in the wild**, which makes it the worst possible endpoint
     * to leave uncached. Three requests against an unchanged store must cost at most the one scan that
     * builds the shared snapshot.
     */
    @Test
    fun asPropertiesReadsThroughTheSnapshotCache() {
        val store = CountingStore()
        repeat(5) { index -> store.record(grindVerdict("mod$index", "Forge")) }
        val server = ReportServer(store, requestedPort = 0).start()
        try {
            store.scans.set(0)

            repeat(3) { Assertions.assertEquals(200, get(server.port, "/as-properties").statusCode()) }

            Assertions.assertTrue(
                store.scans.get() <= 1,
                "/as-properties scanned the whole store ${store.scans.get()} times for 3 requests against an " +
                    "unchanged store - it is not reading through the snapshot cache"
            )
        } finally {
            server.stop()
        }
    }
}
