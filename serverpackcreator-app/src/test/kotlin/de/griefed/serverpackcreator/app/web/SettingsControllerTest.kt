package de.griefed.serverpackcreator.app.web

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File

/**
 * Characterization tests pinning the response-shape of [SettingsController] using the real
 * [de.griefed.serverpackcreator.api.ApiProperties] from the API-module test resources via a
 * standalone MockMvc — no Spring context required.
 */
internal class SettingsControllerTest {
    private val apiWrapper = ApiWrapper.api(
        File(
            File("").absoluteFile.parent,
            "serverpackcreator-api/src/test/resources/serverpackcreator.properties"
        )
    )
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(SettingsController(apiWrapper.apiProperties))
        .build()

    /**
     * Pins that the current-settings endpoint serves every settings-field the frontend relies on.
     */
    @Test
    fun currentSettingsListAllConfigurationFields() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/settings/current"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.clientsideMods").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.whitelistMods").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.supportedModloaders").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.version").isString)
            .andExpect(MockMvcResultMatchers.jsonPath("$.devBuild").isBoolean)
            .andExpect(MockMvcResultMatchers.jsonPath("$.directoriesToInclude").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.directoriesToExclude").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.zipArchiveExclusions").isArray)
            // The "is"-prefixed names are the frontend-contract (see setting-store.js); the
            // JsonProperty-annotations in SettingsController keep Jackson from stripping them.
            .andExpect(MockMvcResultMatchers.jsonPath("$.isZipFileExclusionEnabled").isBoolean)
            .andExpect(MockMvcResultMatchers.jsonPath("$.isAutoExcludingModsEnabled").isBoolean)
            .andExpect(MockMvcResultMatchers.jsonPath("$.isMinecraftPreReleasesAvailabilityEnabled").isBoolean)
            .andExpect(MockMvcResultMatchers.jsonPath("$.aikarsFlags").isString)
            .andExpect(MockMvcResultMatchers.jsonPath("$.language").isString)
    }

    /**
     * Pins that all five supported modloaders are reported by the settings endpoint.
     */
    @Test
    fun currentSettingsListAllSupportedModloaders() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/settings/current"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.supportedModloaders",
                    org.hamcrest.Matchers.containsInAnyOrder("Forge", "Fabric", "Quilt", "LegacyFabric", "NeoForge")
                )
            )
    }
}
