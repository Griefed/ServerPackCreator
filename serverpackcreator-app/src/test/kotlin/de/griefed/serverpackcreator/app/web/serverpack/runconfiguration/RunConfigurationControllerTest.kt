package de.griefed.serverpackcreator.app.web.serverpack.runconfiguration

import de.griefed.serverpackcreator.app.web.assignEntityId
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.*

/**
 * Characterization tests pinning the request-mappings and status-codes of
 * [RunConfigurationController] with a mocked service, using a standalone MockMvc — no Spring
 * context, no MongoDB.
 */
internal class RunConfigurationControllerTest {
    private val runConfigurationService: RunConfigurationService = mockk()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(RunConfigurationController(runConfigurationService))
        .build()

    /**
     * Builds a minimal run-configuration entity for stubbing service-responses, assigning the
     * normally database-managed ID via reflection.
     */
    private fun runConfig(id: String): RunConfiguration {
        val runConfiguration = RunConfiguration()
        assignEntityId(runConfiguration, id)
        runConfiguration.minecraftVersion = "1.20.1"
        return runConfiguration
    }

    /**
     * Pins that all run-configurations are served as a JSON-list.
     */
    @Test
    fun allRunConfigurationsAreServedAsList() {
        every { runConfigurationService.loadAll() } returns listOf(runConfig("runconfig1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/runconfigs/all"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("runconfig1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].minecraftVersion").value("1.20.1"))
    }

    /**
     * Pins that a run-configuration by ID returns 200 and an unknown ID returns 404.
     */
    @Test
    fun runConfigurationIsServedById() {
        every { runConfigurationService.load("runconfig1") } returns Optional.of(runConfig("runconfig1"))
        every { runConfigurationService.load("missing") } returns Optional.empty()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/runconfigs/runconfig1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("runconfig1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/runconfigs/missing"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
