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

import de.griefed.serverpackcreator.clientside.verdict.Verdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.apache.logging.log4j.Level
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * A streamed export commits its 200 before the first row is written, so a failure part-way cannot become an error
 * status. It must still be visible: the client gets a response that does not complete, never a well-formed shorter
 * document, and the failure reaches the application log.
 */
internal class StreamedExportFailureTest {

    /** Serialising this fails, as a row the serializer or the CSV writer cannot render would. */
    private class Unrenderable {
        @Suppress("unused")
        val boom: String get() = throw IllegalStateException("cannot render this row")

        override fun toString(): String = throw IllegalStateException("cannot render this row")
    }

    @Suppress("UNCHECKED_CAST")
    private fun storeWithAnUnrenderableRow() = InMemoryVerdictStore().apply {
        record(grindVerdict("creativecore", "Fabric", verdict = Verdict.CONFIRMED))
        record(grindVerdict("bookshelf", "Forge", verdict = Verdict.CLEAR))
        record(
            grindVerdict("prism", "Quilt", verdict = Verdict.CLEAR)
                .copy(stagedDependencies = listOf<Any>(Unrenderable()) as List<String>)
        )
    }

    private fun assertFailsVisibly(path: String) = capturingLogsOf(ReportServer::class.java) { log ->
        val server = ReportServer(storeWithAnUnrenderableRow(), requestedPort = 0).start()
        try {
            val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:${server.port}$path")).build()
            Assertions.assertThrows(IOException::class.java, {
                HttpClient.newHttpClient().send(request, BodyHandlers.ofString())
            }, "$path completed although a row failed to render")
            Assertions.assertTrue(log.hasThrowableAt(Level.ERROR), "$path failed part-way without an ERROR in the log")
        } finally {
            server.stop()
        }
    }

    @Test
    fun aJsonFeedThatFailsPartWayDoesNotArriveComplete() = assertFailsVisibly("/verdicts.json")

    @Test
    fun aCsvExportThatFailsPartWayDoesNotArriveComplete() = assertFailsVisibly("/export.csv")
}
