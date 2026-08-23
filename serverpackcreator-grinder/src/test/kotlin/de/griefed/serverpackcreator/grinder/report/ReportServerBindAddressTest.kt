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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.opentest4j.TestAbortedException
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

/**
 * Pins *where* the report listens, by connecting from an address that is not loopback.
 *
 * This is the reverse-proxy contract. A proxy running in a container reaches the host over the bridge
 * gateway, never over `127.0.0.1`, so a loopback-bound report is unreachable from one no matter how the
 * proxy is configured — the connection is refused before any HTTP happens. The default stays loopback
 * because the report has no authentication of any kind; making it reachable has to be a deliberate act.
 *
 * Both guards need a real non-loopback IPv4 on the box and skip without one, since the whole point is to
 * cross an interface boundary that no amount of `127.0.0.1` can simulate.
 */
internal class ReportServerBindAddressTest {

    /**
     * A non-loopback IPv4 this host actually owns, aborting the test where there is none — the guards below
     * cross an interface boundary that no amount of `127.0.0.1` can simulate, so without one there is nothing
     * to measure. Aborting rather than returning null keeps the callers free of `!!`, and JUnit reports it as
     * a skip exactly as an assumption would. Binding to a concrete address rather than `0.0.0.0` keeps the
     * test from opening the box up any wider than the one interface it measures.
     */
    private fun nonLoopbackIpv4(): String = NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
        ?.hostAddress
        ?: throw TestAbortedException("no non-loopback IPv4 on this host")

    /** A short timeout: a refused connection is immediate, and a hang here would mean the opposite verdict. */
    private fun get(host: String, port: Int) = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create("http://$host:$port/status"))
            .timeout(Duration.ofSeconds(5)).build(),
        BodyHandlers.ofString()
    )

    /**
     * The default refuses everything that is not loopback — the exact symptom behind a proxy returning 502
     * while the report answers perfectly well over SSH on the same box.
     */
    @Test
    fun theDefaultIsReachableOnLoopbackOnly() {
        val address = nonLoopbackIpv4()

        val server = ReportServer(InMemoryVerdictStore(), requestedPort = 0).start()
        try {
            Assertions.assertEquals(200, get("127.0.0.1", server.port).statusCode())
            // IOException, not ConnectException: the claim is "unreachable", and a host that DROPs rather
            // than REJECTs delivers that verdict as a connect *timeout*. Pinning the narrower type would
            // fail on a box where the property being guarded holds perfectly well.
            Assertions.assertThrows(IOException::class.java, {
                get(address, server.port)
            }, "the default bind must not be reachable off loopback — the report carries no authentication")
        } finally {
            server.stop()
        }
    }

    /** Given an address, the report binds *there* and answers there — which is what a proxy needs. */
    @Test
    fun aConfiguredAddressIsReachableFromOffLoopback() {
        val address = nonLoopbackIpv4()

        val server = ReportServer(InMemoryVerdictStore(), requestedPort = 0, host = address).start()
        try {
            val response = get(address, server.port)
            Assertions.assertEquals(200, response.statusCode())
            Assertions.assertTrue(response.body().contains("\"activity\""), response.body())
        } finally {
            server.stop()
        }
    }
}
