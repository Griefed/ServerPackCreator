package de.griefed.serverpackcreator.app.web.stats

import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackDownload
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackDownload
import de.griefed.serverpackcreator.app.web.stats.creation.AmountPerDate
import de.griefed.serverpackcreator.app.web.stats.creation.CreationStatsService
import de.griefed.serverpackcreator.app.web.stats.disk.DiskStatsService
import de.griefed.serverpackcreator.app.web.stats.downloads.DownloadStatsService
import de.griefed.serverpackcreator.app.web.stats.packs.AmountStatsData
import de.griefed.serverpackcreator.app.web.stats.packs.AmountStatsService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Characterization tests pinning the request-mappings and response-shapes of [StatsController]
 * with mocked statistics-services, using a standalone MockMvc — no Spring context, no MongoDB.
 * Also pins the fix of the duplicate-route bug where server pack download-history-by-id was
 * mapped to /downloads/modpacks/{id} instead of /downloads/serverpacks/{id}.
 */
internal class StatsControllerTest {
    private val downloadStatsService: DownloadStatsService = mockk()
    private val diskStatsService: DiskStatsService = mockk()
    private val creationStatsService: CreationStatsService = mockk()
    private val amountStatsService: AmountStatsService = mockk()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(
            StatsController(downloadStatsService, diskStatsService, creationStatsService, amountStatsService)
        ).build()

    /**
     * Pins that modpack-download counts are served as a JSON-list of amount-per-date entries.
     */
    @Test
    fun modPackDownloadsReturnAmountsPerDate() {
        every { downloadStatsService.modPackDownloads() } returns listOf(AmountPerDate(3, "2026-06-11"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/downloads/modpacks"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].creations").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].date").value("2026-06-11"))
    }

    /**
     * Pins that download-history for a specific modpack is served below /downloads/modpacks/{id}.
     */
    @Test
    fun downloadHistoryForModPackIsServedById() {
        every { downloadStatsService.downloadHistoryForModPack("someModPack") } returns listOf(ModPackDownload(ModPack()))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/downloads/modpacks/someModPack"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
    }

    /**
     * Pins that download-history for a specific server pack is served below
     * /downloads/serverpacks/{id} — before the route-fix this endpoint was unreachable because it
     * was mapped to /downloads/modpacks/{id}, colliding with the modpack-history endpoint.
     */
    @Test
    fun downloadHistoryForServerPackIsServedById() {
        every { downloadStatsService.downloadHistoryForServerPack("someServerPack") } returns listOf(
            ServerPackDownload(ServerPack(0, null, null, "test", null, "modpack1"))
        )
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/downloads/serverpacks/someServerPack"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
    }

    /**
     * Pins that the literal history-endpoints still win over the id-endpoints for the path-segment
     * "history", for both modpacks and serverpacks.
     */
    @Test
    fun historyEndpointsWinOverIdEndpoints() {
        every { downloadStatsService.allModPackDownloadsHistory() } returns listOf()
        every { downloadStatsService.allServerPackDownloadsHistory() } returns listOf()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/downloads/modpacks/history"))
            .andExpect(MockMvcResultMatchers.status().isOk)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/downloads/serverpacks/history"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    /**
     * Pins that disk-statistics are served from the disk-stats-service's stats-list.
     */
    @Test
    fun diskStatsReturnStatsList() {
        every { diskStatsService.stats } returns mutableListOf()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/disk"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
    }

    /**
     * Pins that creation-statistics for modpacks and server packs are served as JSON-lists.
     */
    @Test
    fun creationStatsReturnAmountsPerDate() {
        every { creationStatsService.getModpackTimeStamps() } returns listOf(AmountPerDate(1, "2026-06-10"))
        every { creationStatsService.getServerPackTimeStamps() } returns listOf(AmountPerDate(2, "2026-06-11"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/creation/modpacks"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].creations").value(1))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/creation/serverpacks"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].creations").value(2))
    }

    /**
     * Pins that pack-amount statistics are served with modpack-, serverpack- and
     * runconfiguration-counts.
     */
    @Test
    fun amountStatsReturnPackCounts() {
        every { amountStatsService.stats } returns AmountStatsData(5, 7, 3, hashMapOf(), hashMapOf(), hashMapOf())
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/stats/packs"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.modPacks").value(5))
            .andExpect(MockMvcResultMatchers.jsonPath("$.serverPacks").value(7))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runConfigurations").value(3))
    }
}
