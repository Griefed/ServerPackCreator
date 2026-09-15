package de.griefed.serverpackcreator.app.web.modpack

import de.griefed.serverpackcreator.app.web.assignEntityId
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import de.griefed.serverpackcreator.app.web.serverpack.runconfiguration.RunConfigurationService
import de.griefed.serverpackcreator.app.web.task.TaskExecutionServiceImpl
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.*

/**
 * Characterization tests pinning the request-mappings, status-codes and response-shapes of
 * [ModPackController] with mocked services, using a standalone MockMvc — no Spring context,
 * no MongoDB.
 */
internal class ModPackControllerTest {
    private val modpackService: ModPackService = mockk()
    private val runConfigurationService: RunConfigurationService = mockk()
    private val taskExecutionService: TaskExecutionServiceImpl = mockk()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(ModPackController(modpackService, runConfigurationService, taskExecutionService))
        .build()

    /**
     * Builds a minimal modpack-entity for stubbing service-responses, assigning the
     * normally database-managed ID via reflection.
     */
    private fun modPack(id: String, name: String = "Test Pack"): ModPack {
        val modpack = ModPack()
        assignEntityId(modpack, id)
        modpack.name = name
        return modpack
    }

    /**
     * Builds a minimal run-configuration entity for stubbing service-responses, assigning the
     * normally database-managed ID via reflection.
     */
    private fun runConfig(id: String): RunConfiguration {
        val runConfiguration = RunConfiguration()
        assignEntityId(runConfiguration, id)
        return runConfiguration
    }

    /**
     * Pins that downloading an unknown modpack yields a 404.
     */
    @Test
    fun downloadUnknownModpackIsNotFound() {
        every { modpackService.getModpack("missing") } returns Optional.empty()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/modpacks/download/missing"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Pins that a known modpack without a stored archive also yields a 404 on download.
     */
    @Test
    fun downloadModpackWithoutArchiveIsNotFound() {
        val modpack = modPack("known")
        every { modpackService.getModpack("known") } returns Optional.of(modpack)
        every { modpackService.getModPackArchive(modpack) } returns Optional.empty()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/modpacks/download/known"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Pins that an upload with an empty file is rejected with a 400 and an ERROR-status response.
     */
    @Test
    fun uploadWithEmptyFileIsBadRequest() {
        val emptyFile = MockMultipartFile("file", "empty.zip", "application/zip", ByteArray(0))
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v2/modpacks/upload")
                .file(emptyFile)
                .param("minecraftVersion", "1.20.1")
                .param("modloader", "Forge")
                .param("modloaderVersion", "47.2.0")
                .param("startArgs", "")
                .param("clientMods", "")
                .param("whiteListMods", "")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ERROR"))
    }

    /**
     * Pins the happy upload-path: the file is stored, a task is queued, and the response carries
     * modpack- and runconfig-IDs with QUEUED-status.
     */
    @Test
    fun uploadWithValidFileQueuesGeneration() {
        val zipFile = MockMultipartFile("file", "pack.zip", "application/zip", "zipzipzip".toByteArray())
        val modpack = modPack("modpack1", "pack.zip")
        every {
            runConfigurationService.createRunConfig("1.20.1", "Forge", "47.2.0", "", "", "")
        } returns runConfig("runconfig1")
        every { modpackService.saveUploadedFile(any()) } returns modpack
        justRun { taskExecutionService.submitTaskInQueue(any()) }
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v2/modpacks/upload")
                .file(zipFile)
                .param("minecraftVersion", "1.20.1")
                .param("modloader", "Forge")
                .param("modloaderVersion", "47.2.0")
                .param("startArgs", "")
                .param("clientMods", "")
                .param("whiteListMods", "")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("QUEUED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.modPackId").value("modpack1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runConfigId").value("runconfig1"))
    }

    /**
     * Pins that requesting generation for an unknown modpack is rejected with a 400.
     */
    @Test
    fun generationForUnknownModpackIsBadRequest() {
        every { modpackService.getModpack("missing") } returns Optional.empty()
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/modpacks/generate")
                .param("modPackID", "missing")
                .param("minecraftVersion", "1.20.1")
                .param("modloader", "Forge")
                .param("modloaderVersion", "47.2.0")
                .param("startArgs", "")
                .param("clientMods", "")
                .param("whiteListMods", "")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
    }

    /**
     * Pins that requesting generation with a run-configuration for which a server pack already
     * exists is rejected with a 400 and GENERATED-status.
     */
    @Test
    fun generationForExistingServerPackIsBadRequest() {
        val modpack = modPack("modpack1")
        val existingConfig = runConfig("runconfig1")
        val serverPack = ServerPack(0, existingConfig, null, "test", null, "modpack1")
        assignEntityId(serverPack, "serverpack1")
        modpack.serverPacks.add(serverPack)
        every { modpackService.getModpack("modpack1") } returns Optional.of(modpack)
        every {
            runConfigurationService.createRunConfig("1.20.1", "Forge", "47.2.0", "", "", "")
        } returns existingConfig
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/modpacks/generate")
                .param("modPackID", "modpack1")
                .param("minecraftVersion", "1.20.1")
                .param("modloader", "Forge")
                .param("modloaderVersion", "47.2.0")
                .param("startArgs", "")
                .param("clientMods", "")
                .param("whiteListMods", "")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("GENERATED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.serverPackId").value("serverpack1"))
    }

    /**
     * Pins the happy generation-path: a new run-configuration queues a generation-task.
     */
    @Test
    fun generationForNewRunConfigurationIsQueued() {
        val modpack = modPack("modpack1")
        every { modpackService.getModpack("modpack1") } returns Optional.of(modpack)
        every {
            runConfigurationService.createRunConfig("1.20.1", "Forge", "47.2.0", "", "", "")
        } returns runConfig("runconfig2")
        justRun { taskExecutionService.submitTaskInQueue(any()) }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/modpacks/generate")
                .param("modPackID", "modpack1")
                .param("minecraftVersion", "1.20.1")
                .param("modloader", "Forge")
                .param("modloaderVersion", "47.2.0")
                .param("startArgs", "")
                .param("clientMods", "")
                .param("whiteListMods", "")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("QUEUED"))
    }

    /**
     * Pins that all modpacks are served as a JSON-list and a single modpack by ID returns 200,
     * while an unknown ID returns 404.
     */
    @Test
    fun modpacksAreServedByListAndId() {
        every { modpackService.getModpacks() } returns listOf(modPack("modpack1"))
        every { modpackService.getModpack("modpack1") } returns Optional.of(modPack("modpack1"))
        every { modpackService.getModpack("missing") } returns Optional.empty()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/modpacks/all"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("modpack1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/modpacks/modpack1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("modpack1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/modpacks/missing"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Pins that a modpack can be retrieved by the ID of one of its server packs.
     */
    @Test
    fun modpackIsServedByServerPackId() {
        every { modpackService.getByServerPack("serverpack1") } returns Optional.of(modPack("modpack1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/modpacks/byserverpack/serverpack1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("modpack1"))
    }
}
