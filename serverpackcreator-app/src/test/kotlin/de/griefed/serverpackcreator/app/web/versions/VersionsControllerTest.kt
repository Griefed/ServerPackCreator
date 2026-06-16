package de.griefed.serverpackcreator.app.web.versions

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File

/**
 * Characterization tests pinning the request-mappings and response-shapes of [VersionsController]
 * before the app-module refactor (Phase 2). Uses a standalone MockMvc with the real [VersionMeta]
 * backed by the cached version-manifests from the API-module test resources, so no Spring context
 * and no database are required.
 */
internal class VersionsControllerTest {
    private val apiWrapper = ApiWrapper.api(
        File(
            File("").absoluteFile.parent,
            "serverpackcreator-api/src/test/resources/serverpackcreator.properties"
        )
    )
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(VersionsController(apiWrapper.versionMeta))
        .build()

    /**
     * Pins that the all-versions endpoint responds with every loader-family present in the JSON.
     */
    @Test
    fun allVersionsEndpointListsEveryLoaderFamily() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/versions/all"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.minecraft").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.fabric").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.legacyFabric").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.quilt").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.forge").isMap)
            .andExpect(MockMvcResultMatchers.jsonPath("$.neoForge").isMap)
    }

    /**
     * Pins that the Minecraft-versions endpoint returns a non-empty list of release versions.
     */
    @Test
    fun minecraftVersionsEndpointReturnsReleases() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/versions/minecraft"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0]").isString)
    }

    /**
     * Pins that Forge-versions for a known Minecraft version return a non-empty list.
     */
    @Test
    fun forgeVersionsForKnownMinecraftVersionReturnList() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/versions/forge/1.16.5"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0]").isString)
    }

    /**
     * Pins that Forge-versions for an unknown Minecraft version yield a 404.
     */
    @Test
    fun forgeVersionsForUnknownMinecraftVersionAreNotFound() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/versions/forge/0.0.0"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Pins that the Fabric-, LegacyFabric- and Quilt-endpoints each return loader versions.
     */
    @Test
    fun fabricFamilyEndpointsReturnLoaderVersions() {
        for (endpoint in listOf("fabric", "legacyfabric", "quilt")) {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/versions/$endpoint"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
                .andExpect(MockMvcResultMatchers.jsonPath("$[0]").isString)
        }
    }

    /**
     * Pins that the NeoForge-versions endpoint returns the Minecraft-to-versions map.
     */
    @Test
    fun neoForgeVersionsEndpointReturnsMap() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/versions/neoforge"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isMap)
    }
}
