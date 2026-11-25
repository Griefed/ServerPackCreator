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
package de.griefed.serverpackcreator.api.utilities.common

import de.griefed.serverpackcreator.api.ApiProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** Pins what [WebUtilities.getResponseAsString] returns, and that it releases its connection when a read fails. */
internal class GetResponseAsStringTest {

    /** An [ApiProperties] whose network timeouts are short, so a stalled server fails a read quickly. */
    private fun timedProperties(): ApiProperties {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.networkConnectTimeout } returns 2_000
        every { apiProperties.networkReadTimeout } returns 2_000
        every { apiProperties.networkDownloadReadTimeout } returns 2_000
        return apiProperties
    }

    /**
     * Characterises the published behaviour: lines are concatenated with no separator. Pinned so that adding
     * the separator is a visible behaviour change rather than an accident.
     */
    @Test
    fun lineBreaksAreDropped(@TempDir tempDir: File) {
        val body = File(tempDir, "body.txt").apply { writeText("a\nb\r\nc") }

        Assertions.assertEquals("abc", WebUtilities(timedProperties()).getResponseAsString(body.toURI().toURL()))
    }

    /**
     * Pins that a read failing part-way through the body still closes the connection. The server sends one
     * chunk of a chunked body and stalls, so the client's read times out; the server then sees its socket
     * closed only if the client released it, rather than whenever the stream is garbage-collected.
     */
    @Test
    fun aReadThatFailsMidBodyStillClosesTheConnection() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val clientClosed = CompletableFuture.supplyAsync {
                server.accept().use { socket ->
                    val request = socket.getInputStream().bufferedReader()
                    while (request.readLine().let { it != null && it.isNotEmpty() }) {
                        // Consume the request headers.
                    }
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nab\ncd\r\n".toByteArray())
                        flush()
                    }
                    socket.soTimeout = 20_000
                    try {
                        socket.getInputStream().read() == -1
                    } catch (_: SocketTimeoutException) {
                        false
                    }
                }
            }
            val url = URI("http://127.0.0.1:${server.localPort}/").toURL()

            Assertions.assertThrows(IOException::class.java) {
                WebUtilities(timedProperties()).getResponseAsString(url)
            }

            Assertions.assertTrue(
                clientClosed.get(30, TimeUnit.SECONDS),
                "the client must close its connection when the read fails"
            )
        }
    }
}
