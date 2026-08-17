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
package de.griefed.serverpackcreator.api.versionmeta

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.JsonUtilities
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.api.utilities.common.XmlUtilities
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Tests for [ManifestUpdater] against a real, local HTTP server.
 *
 * These pin the *cost* of a manifest check, not its wall-clock: twelve of these run on the blocking
 * GUI startup path, so how many requests each one issues and whether an unchanged manifest is
 * re-downloaded is what a user actually waits for. A local `com.sun.net.httpserver.HttpServer` (JDK
 * built-in, no new dependency) can answer that exactly, where a timing assertion could not.
 */
internal class ManifestUpdaterTest {

    /** Minimal Minecraft-shaped manifest declaring [versions] versions, counted via `TAG_VERSIONS`. */
    private fun minecraftManifest(versions: Int): String {
        val entries = (1..versions).joinToString(",") { """{"id":"1.0.$it","type":"release"}""" }
        return """{"latest":{"release":"1.0.1","snapshot":"1.0.1"},"versions":[$entries]}"""
    }

    /**
     * What one run of the server observed: how many requests arrived, and the conditional-request
     * header of the most recent one.
     */
    private class Observed {
        /** Requests served since the server started. */
        val requests = AtomicInteger(0)

        /** The `If-Modified-Since` of the most recent request, or `null` if it carried none. */
        @Volatile
        var lastIfModifiedSince: String? = null
    }

    /**
     * Serves [body] on `/manifest.json` and hands [test] the URL plus what the server observed.
     *
     * When [honourConditional] is set, a request carrying `If-Modified-Since` is answered `304` with
     * no body — which is what the real upstreams do, and what makes an unchanged manifest free.
     */
    private fun withServer(
        body: String,
        honourConditional: Boolean = true,
        test: (URI, Observed) -> Unit
    ) {
        val observed = Observed()
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/manifest.json") { exchange: HttpExchange ->
            observed.requests.incrementAndGet()
            observed.lastIfModifiedSince = exchange.requestHeaders.getFirst("If-Modified-Since")
            exchange.responseHeaders.add("Last-Modified", "Mon, 01 Jan 2001 00:00:00 GMT")
            if (honourConditional && observed.lastIfModifiedSince != null) {
                // 304: no body at all, which is the entire point of asking.
                exchange.sendResponseHeaders(304, -1)
            } else {
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        server.start()
        try {
            test(URI("http://127.0.0.1:${server.address.port}/manifest.json"), observed)
        } finally {
            server.stop(0)
        }
    }

    /** A [ManifestUpdater] whose HTTP calls carry short timeouts, so nothing here can hang the suite. */
    private fun updater(): ManifestUpdater {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.networkConnectTimeout } returns 2_000
        every { apiProperties.networkReadTimeout } returns 2_000
        every { apiProperties.networkDownloadReadTimeout } returns 2_000
        return ManifestUpdater(
            Utilities(
                WebUtilities(apiProperties),
                JsonUtilities(ObjectMapper()),
                XmlUtilities(DocumentBuilderFactory.newInstance())
            )
        )
    }

    /**
     * Pins that checking one manifest costs **one** request.
     *
     * It used to cost two: a reachability pre-check whose response body was discarded, immediately
     * followed by the real fetch. Worse than merely doubling the count — the pre-check calls
     * `disconnect()` without draining the body, so the connection cannot be pooled and the real
     * request pays a fresh TCP and TLS handshake. Twelve manifests therefore meant 24 requests and 24
     * handshakes across seven hosts before the splash screen moved.
     */
    @Test
    fun aManifestCheckCostsOneRequest(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraft-manifest.json")
        manifest.writeText(minecraftManifest(1))
        withServer(minecraftManifest(1)) { url, observed ->
            updater().checkManifest(manifest, url.toURL(), Type.MINECRAFT)
            Assertions.assertEquals(
                1,
                observed.requests.get(),
                "One manifest check must cost one request; a reachability pre-check doubles it"
            )
        }
    }

    /**
     * Pins that the check asks to be told only about changes.
     *
     * Without `If-Modified-Since` every startup re-downloads all twelve manifests in full — roughly
     * half a megabyte — and parses both the old and the new copy purely to compare version counts,
     * then throws the result away, which is the common case because the manifests rarely change.
     */
    @Test
    fun anUnchangedManifestIsFetchedConditionally(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraft-manifest.json")
        manifest.writeText(minecraftManifest(1))
        withServer(minecraftManifest(1)) { url, observed ->
            updater().checkManifest(manifest, url.toURL(), Type.MINECRAFT)
            Assertions.assertNotNull(
                observed.lastIfModifiedSince,
                "The check must send If-Modified-Since so an unchanged manifest costs headers only"
            )
        }
    }

    /**
     * Pins that a `304` leaves the local manifest exactly as it was — the short-circuit must not
     * truncate or rewrite the file it decided not to update.
     */
    @Test
    fun aNotModifiedResponseLeavesTheLocalManifestAlone(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraft-manifest.json")
        val original = minecraftManifest(3)
        manifest.writeText(original)
        withServer(minecraftManifest(3)) { url, _ ->
            updater().checkManifest(manifest, url.toURL(), Type.MINECRAFT)
            Assertions.assertEquals(original, manifest.readText(), "A 304 must not touch the local file")
        }
    }

    /**
     * Pins that the refresh still happens when the upstream genuinely has more versions. The
     * conditional request is an optimisation, not a new policy: a server which ignores
     * `If-Modified-Since` and answers `200` must behave exactly as before.
     */
    @Test
    fun aLargerUpstreamManifestStillRefreshesTheLocalCopy(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraft-manifest.json")
        manifest.writeText(minecraftManifest(1))
        val upstream = minecraftManifest(5)
        withServer(upstream, honourConditional = false) { url, _ ->
            updater().checkManifest(manifest, url.toURL(), Type.MINECRAFT)
            Assertions.assertEquals(upstream, manifest.readText(), "More versions upstream must refresh the local copy")
        }
    }

    /**
     * Pins that a smaller upstream manifest does **not** overwrite the local copy, which is the rule
     * protecting a good local manifest from a truncated or half-published upstream one.
     */
    @Test
    fun aSmallerUpstreamManifestDoesNotReplaceTheLocalCopy(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraft-manifest.json")
        val original = minecraftManifest(9)
        manifest.writeText(original)
        withServer(minecraftManifest(2), honourConditional = false) { url, _ ->
            updater().checkManifest(manifest, url.toURL(), Type.MINECRAFT)
            Assertions.assertEquals(original, manifest.readText(), "Fewer versions upstream must be ignored")
        }
    }

    /**
     * Pins that an absent local manifest is downloaded, and costs one request rather than a
     * reachability probe followed by the download.
     */
    @Test
    fun anAbsentManifestIsDownloadedInOneRequest(@TempDir tempDir: File) {
        val manifest = File(tempDir, "minecraft-manifest.json")
        val upstream = minecraftManifest(4)
        withServer(upstream, honourConditional = false) { url, observed ->
            updater().checkManifest(manifest, url.toURL(), Type.MINECRAFT)
            Assertions.assertTrue(manifest.isFile, "A missing manifest must be fetched")
            Assertions.assertEquals(upstream, manifest.readText())
            Assertions.assertEquals(
                1,
                observed.requests.get(),
                "Fetching a missing manifest must cost one request, not a probe plus a download"
            )
        }
    }
}
