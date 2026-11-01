package de.griefed.serverpackcreator.app.web.serverpack

import de.griefed.serverpackcreator.app.web.assignEntityId
import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File
import java.util.*

/**
 * Characterization tests pinning the request-mappings, status-codes and download-behavior of
 * [ServerPackController] with mocked services, using a standalone MockMvc — no Spring context,
 * no MongoDB.
 */
internal class ServerPackControllerTest {
    private val serverPackService: ServerPackService = mockk()
    private val modpackService: ModPackService = mockk()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(ServerPackController(serverPackService, modpackService))
        .build()

    /**
     * Builds a minimal server pack-entity for stubbing service-responses, assigning the
     * normally database-managed ID via reflection.
     */
    private fun serverPack(id: String, fileName: String? = "test_pack"): ServerPack {
        val pack = ServerPack(0, null, null, fileName, null, "modpack1")
        assignEntityId(pack, id)
        return pack
    }

    /**
     * Pins that downloading an unknown server pack yields a 404.
     */
    @Test
    fun downloadUnknownServerPackIsNotFound() {
        every { serverPackService.getServerPack("missing") } returns Optional.empty()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/missing"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Pins the download happy-path: the archive is streamed as application/zip with the
     * "_server_pack.zip"-suffixed attachment-filename, and the download-counter is updated.
     */
    @Test
    fun downloadServerPackStreamsArchive(@TempDir tempDir: File) {
        val archive = File(tempDir, "pack.zip")
        archive.writeText("zip-content")
        // Production-shaped: TaskExecutionServiceImpl stores the generated archive's name, which
        // zipBuilder already suffixed with "_server_pack.zip". The controller must not add a second one.
        val pack = serverPack("serverpack1", "My Pack_server_pack.zip")
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(pack)
        every { serverPackService.getServerPackArchive(pack) } returns Optional.of(archive)
        every { serverPackService.updateDownloadStats("serverpack1") } returns Optional.of(pack)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/serverpack1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("application/zip"))
            .andExpect(
                MockMvcResultMatchers.header().string(
                    "Content-Disposition",
                    "attachment; filename=\"My Pack_server_pack.zip\""
                )
            )
    }

    /**
     * Until this guard, `ServerPack.fileName` held the generated archive's **absolute path** and the
     * controller appended `_server_pack.zip` to it, so every downloader was told to save
     * `/srv/spc/server-packs/My Pack_server_pack.zip_server_pack.zip` -- the server's filesystem layout
     * plus a doubled suffix. It stayed green because the only fixture was a bare name no production row
     * ever carries.
     */
    @Test
    fun theServedNameIsTheArchivesBaseNameNotTheServersPath(@TempDir tempDir: File) {
        val archive = File(tempDir, "pack.zip")
        archive.writeText("zip-content")
        val pack = serverPack("serverpack1", "/srv/spc/server-packs/My Pack_server_pack.zip")
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(pack)
        every { serverPackService.getServerPackArchive(pack) } returns Optional.of(archive)
        every { serverPackService.updateDownloadStats("serverpack1") } returns Optional.of(pack)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/serverpack1"))
            .andExpect(
                MockMvcResultMatchers.header().string(
                    "Content-Disposition",
                    "attachment; filename=\"My Pack_server_pack.zip\""
                )
            )
    }

    /**
     * The archive's name reaches the header, and a legacy row carries a path the operator chose. A
     * quote in it closes the value early and a semicolon appends a parameter, on a route with no
     * authentication and CORS "*" -- the modpack half of this is pinned in `ArchiveStreamingTest`.
     */
    @Test
    fun aHostileArchiveNameCannotBreakOutOfTheContentDispositionHeader(@TempDir tempDir: File) {
        val archive = File(tempDir, "pack.zip")
        archive.writeText("zip-content")
        val pack = serverPack("serverpack1", """evil".zip"; filename="other.exe""")
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(pack)
        every { serverPackService.getServerPackArchive(pack) } returns Optional.of(archive)
        every { serverPackService.updateDownloadStats("serverpack1") } returns Optional.of(pack)

        val disposition = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/serverpack1"))
            .andReturn().response.getHeader("Content-Disposition")!!

        // Asserted on the RAW header for the reason ArchiveStreamingTest records: ContentDisposition.parse
        // recovers the same filename from the escaped and the unescaped form alike, so a round-trip
        // assertion passes against the very injection it is meant to catch.
        val unescaped = disposition.replace("\\\"", "").count { it == '"' }
        Assertions.assertEquals(
            2, unescaped,
            "the value carries unescaped quotes, so a reader sees more than one filename: $disposition"
        )
    }

    /**
     * A row whose `fileName` was never set still has to produce a usable name rather than the string
     * "null", because the browser saves whatever the header says.
     */
    @Test
    fun aServerPackWithoutAStoredNameIsStillServedUnderAUsableName(@TempDir tempDir: File) {
        val archive = File(tempDir, "pack.zip")
        archive.writeText("zip-content")
        val pack = serverPack("serverpack1", fileName = null)
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(pack)
        every { serverPackService.getServerPackArchive(pack) } returns Optional.of(archive)
        every { serverPackService.updateDownloadStats("serverpack1") } returns Optional.of(pack)

        val disposition = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/serverpack1"))
            .andReturn().response.getHeader("Content-Disposition")!!

        Assertions.assertEquals("attachment; filename=\"serverpack1_server_pack.zip\"", disposition)
    }

    /**
     * The server-pack half of what `ArchiveStreamingTest` pins for modpacks: the bytes on the wire are
     * the archive's, and `Content-Length` matches, so a browser can draw a real progress bar.
     */
    @Test
    fun downloadServesTheArchivesActualBytesAndLength(@TempDir tempDir: File) {
        val archive = File(tempDir, "pack.zip")
        archive.writeBytes(ByteArray(64) { 7 })
        val pack = serverPack("serverpack1", "My Pack_server_pack.zip")
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(pack)
        every { serverPackService.getServerPackArchive(pack) } returns Optional.of(archive)
        every { serverPackService.updateDownloadStats("serverpack1") } returns Optional.of(pack)

        val response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/serverpack1"))
            .andReturn().response

        Assertions.assertEquals(archive.length(), response.contentLength.toLong())
        Assertions.assertArrayEquals(archive.readBytes(), response.contentAsByteArray)
    }

    /**
     * Spring answers a `Range` request over a `Resource` return value itself, in
     * `AbstractMessageConverterMethodProcessor` -- verified against the pinned spring-webmvc. That is
     * what lets an interrupted multi-gigabyte download resume instead of restarting, so it is pinned
     * here rather than left as an accident of the return type.
     */
    @Test
    fun aRangeRequestIsAnsweredWithThatRangeSoADownloadCanResume(@TempDir tempDir: File) {
        val archive = File(tempDir, "pack.zip")
        archive.writeBytes(ByteArray(64) { it.toByte() })
        val pack = serverPack("serverpack1", "My Pack_server_pack.zip")
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(pack)
        every { serverPackService.getServerPackArchive(pack) } returns Optional.of(archive)
        every { serverPackService.updateDownloadStats("serverpack1") } returns Optional.of(pack)

        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v2/serverpacks/download/serverpack1")
                .header("Range", "bytes=10-19")
        ).andReturn().response

        Assertions.assertEquals(206, response.status)
        Assertions.assertEquals("bytes 10-19/64", response.getHeader("Content-Range"))
        Assertions.assertArrayEquals(archive.readBytes().copyOfRange(10, 20), response.contentAsByteArray)
    }

    /**
     * Pins downloading by modpack- and runconfiguration-ID: the modpack's server pack matching
     * the run-configuration is resolved and streamed; unknown combinations yield a 404.
     */
    @Test
    fun downloadByModpackAndRunConfigResolvesServerPack(@TempDir tempDir: File) {
        val archive = File(tempDir, "pack.zip")
        archive.writeText("zip-content")
        val runConfiguration = RunConfiguration()
        assignEntityId(runConfiguration, "55")
        val pack = serverPack("serverpack1")
        pack.runConfiguration = runConfiguration
        val modpack = ModPack()
        assignEntityId(modpack, "77")
        modpack.serverPacks.add(pack)
        every { modpackService.getModpack("77") } returns Optional.of(modpack)
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(pack)
        every { serverPackService.getServerPackArchive(pack) } returns Optional.of(archive)
        every { serverPackService.updateDownloadStats("serverpack1") } returns Optional.of(pack)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/77&55"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("application/zip"))

        every { modpackService.getModpack("88") } returns Optional.empty()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/download/88&55"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Pins that all server packs are served as a JSON-list and a single server pack by ID returns
     * 200, while an unknown ID returns 404.
     */
    @Test
    fun serverPacksAreServedByListAndId() {
        every { serverPackService.getServerPacks() } returns listOf(serverPack("serverpack1"))
        every { serverPackService.getServerPack("serverpack1") } returns Optional.of(serverPack("serverpack1"))
        every { serverPackService.getServerPack("missing") } returns Optional.empty()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/all"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("serverpack1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/serverpack1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("serverpack1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/missing"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Pins that votes are delegated to the service verbatim, including the id,vote path-format.
     */
    @Test
    fun votesAreDelegatedToService() {
        every { serverPackService.voteForServerPack("serverpack1", "up") } returns ResponseEntity.ok().build()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/serverpacks/vote/serverpack1&up"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}
