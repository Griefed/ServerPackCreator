package de.griefed.serverpackcreator.app.web.serverpack

import de.griefed.serverpackcreator.app.web.assignEntityId
import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import io.mockk.every
import io.mockk.mockk
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
    private fun serverPack(id: String, fileName: String = "test_pack"): ServerPack {
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
        val pack = serverPack("serverpack1", "My Pack")
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
