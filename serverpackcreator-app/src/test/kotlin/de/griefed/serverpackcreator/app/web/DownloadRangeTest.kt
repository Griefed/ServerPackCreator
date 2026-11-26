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
package de.griefed.serverpackcreator.app.web

import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackController
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackController
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.nio.file.Path
import java.util.Optional

/**
 * The `Range` behaviour both archive download routes inherit from Spring's handling of a `Resource` return
 * value, pinned per route: what a resuming or segmenting client is sent, and which requests count as a download.
 */
class DownloadRangeTest {

    @TempDir
    lateinit var tempDir: Path

    private val modpackService: ModPackService = mockk()
    private val serverPackService: ServerPackService = mockk()

    /** The archive both routes serve: byte `n` holds the value `n`, so a slice names its own offsets. */
    private val archiveBytes = ByteArray(ARCHIVE_LENGTH) { it.toByte() }

    /** The two archive download routes. */
    enum class Route(val path: String) {
        MODPACK("/api/v2/modpacks/download/known"),
        SERVER_PACK("/api/v2/serverpacks/download/known")
    }

    /** Performs [request] against [route]'s controller, stubbed to serve [archiveBytes] under the id `known`. */
    private fun perform(route: Route, request: MockHttpServletRequestBuilder): MockHttpServletResponse {
        val archive = tempDir.resolve("archive.zip").toFile().apply { writeBytes(archiveBytes) }
        val controller: Any = when (route) {
            Route.MODPACK -> {
                val modpack = ModPack().apply { assignEntityId(this, "known"); name = "Test Pack.zip" }
                every { modpackService.getModpack("known") } returns Optional.of(modpack)
                every { modpackService.getModPackArchive(modpack) } returns Optional.of(archive)
                every { modpackService.updateDownloadStats(any()) } returns Optional.of(modpack)
                ModPackController(modpackService, mockk(), mockk())
            }

            Route.SERVER_PACK -> {
                val serverPack = ServerPack(0, null, "fileId", "Test Pack_server_pack.zip", "sha", "modpackId")
                    .apply { assignEntityId(this, "known") }
                every { serverPackService.getServerPack("known") } returns Optional.of(serverPack)
                every { serverPackService.getServerPackArchive(serverPack) } returns Optional.of(archive)
                every { serverPackService.updateDownloadStats("known") } returns Optional.of(serverPack)
                ServerPackController(serverPackService, modpackService)
            }
        }
        return MockMvcBuilders.standaloneSetup(controller).build().perform(request).andReturn().response
    }

    private fun getWithRange(route: Route, range: String): MockHttpServletResponse =
        perform(route, MockMvcRequestBuilders.get(route.path).header("Range", range))

    @ParameterizedTest
    @EnumSource(Route::class)
    fun aRangeStartingPastTheEndIsUnsatisfiable(route: Route) {
        val response = getWithRange(route, "bytes=100-")

        Assertions.assertEquals(416, response.status)
        Assertions.assertEquals("bytes */$ARCHIVE_LENGTH", response.getHeader("Content-Range"))
    }

    @ParameterizedTest
    @EnumSource(Route::class)
    fun aSuffixRangeIsAnsweredWithTheLastBytes(route: Route) {
        val response = getWithRange(route, "bytes=-10")

        Assertions.assertEquals(206, response.status)
        Assertions.assertEquals("bytes 54-63/$ARCHIVE_LENGTH", response.getHeader("Content-Range"))
        Assertions.assertArrayEquals(archiveBytes.copyOfRange(54, 64), response.contentAsByteArray)
    }

    /** The controller sets the whole archive's length; a partial answer must carry the range's length instead. */
    @ParameterizedTest
    @EnumSource(Route::class)
    fun aPartialAnswerCarriesTheLengthOfTheRangeNotOfTheArchive(route: Route) {
        val response = getWithRange(route, "bytes=10-19")

        Assertions.assertEquals(206, response.status)
        Assertions.assertEquals(10, response.contentLength)
        Assertions.assertArrayEquals(archiveBytes.copyOfRange(10, 20), response.contentAsByteArray)
    }

    @ParameterizedTest
    @EnumSource(Route::class)
    fun severalRangesAreAnsweredAsMultipartByteRanges(route: Route) {
        val response = getWithRange(route, "bytes=0-1,5-6")
        val body = response.contentAsString

        Assertions.assertEquals(206, response.status)
        Assertions.assertTrue(
            response.contentType.orEmpty().startsWith("multipart/byteranges"),
            "content type was ${response.contentType}"
        )
        Assertions.assertTrue(body.contains("Content-Range: bytes 0-1/$ARCHIVE_LENGTH"), body)
        Assertions.assertTrue(body.contains("Content-Range: bytes 5-6/$ARCHIVE_LENGTH"), body)
    }

    /**
     * `@GetMapping` answers `HEAD` too. Discarding the body is the servlet container's job under Servlet 6.1,
     * and MockMvc has none, so only the headers a client sizes its download by are pinned.
     */
    @ParameterizedTest
    @EnumSource(Route::class)
    fun aHeadRequestDescribesTheArchive(route: Route) {
        val response = perform(route, MockMvcRequestBuilders.head(route.path))

        Assertions.assertEquals(200, response.status)
        Assertions.assertEquals(ARCHIVE_LENGTH.toLong(), response.contentLengthLong)
        Assertions.assertEquals("bytes", response.getHeader("Accept-Ranges"))
    }

    /** Asserts how many times [route]'s download counter was incremented. */
    private fun assertCounted(route: Route, expectedCount: Int) {
        when (route) {
            Route.MODPACK -> verify(exactly = expectedCount) { modpackService.updateDownloadStats(any()) }
            Route.SERVER_PACK -> verify(exactly = expectedCount) { serverPackService.updateDownloadStats("known") }
        }
    }

    /**
     * A download is counted once: a plain GET, or a GET whose first range starts at the first byte (a download
     * manager's opening request). A resume, any further segment and an unsatisfiable or malformed range do not
     * count, or one segmented download would be counted once per segment.
     */
    @ParameterizedTest(name = "{0} with Range {1} counts {2}")
    @MethodSource("countingCases")
    fun aDownloadIsCountedOnceNotOncePerRequest(route: Route, range: String?, expectedCount: Int) {
        val request = MockMvcRequestBuilders.get(route.path)
        if (range != null) {
            request.header("Range", range)
        }

        perform(route, request)

        assertCounted(route, expectedCount)
    }

    /**
     * `bytes=0-0` is a size probe some players and download managers send before the real request, so it does
     * not count; but for a one-byte archive that single byte is the whole file, and that download does.
     */
    @Test
    fun aOneByteProbeCountsOnlyWhenItIsTheWholeArchive() {
        Assertions.assertFalse(DownloadCount.isNewDownload(HttpMethod.GET, "bytes=0-0", ARCHIVE_LENGTH.toLong()))
        Assertions.assertTrue(DownloadCount.isNewDownload(HttpMethod.GET, "bytes=0-0", 1))
        Assertions.assertTrue(DownloadCount.isNewDownload(HttpMethod.GET, "bytes=0-1", ARCHIVE_LENGTH.toLong()))
    }

    @ParameterizedTest
    @EnumSource(Route::class)
    fun aHeadRequestIsNotCountedAsADownload(route: Route) {
        perform(route, MockMvcRequestBuilders.head(route.path))

        assertCounted(route, 0)
    }

    private companion object {
        const val ARCHIVE_LENGTH = 64

        @JvmStatic
        fun countingCases(): List<Arguments> = Route.entries.flatMap { route ->
            listOf(
                Arguments.of(route, null, 1),
                Arguments.of(route, "bytes=0-", 1),
                Arguments.of(route, "bytes=0-0", 0),
                Arguments.of(route, "bytes=0-1,5-6", 1),
                Arguments.of(route, "bytes=10-19", 0),
                Arguments.of(route, "bytes=32-", 0),
                Arguments.of(route, "bytes=-10", 0),
                Arguments.of(route, "bytes=100-", 0),
                Arguments.of(route, "not-a-range", 0)
            )
        }
    }
}
