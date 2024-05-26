package de.griefed.serverpackcreator.web.stats.packs

import de.griefed.serverpackcreator.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.web.serverpack.runconfiguration.RunConfigurationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AmountStatsService @Autowired constructor(
    private val serverPackRepository: ServerPackRepository,
    private val modpackRepository: ModPackRepository,
    private val runConfigurationRepository: RunConfigurationRepository
) {
    val stats: AmountStatsData
        get() {
            return AmountStatsData(modpackRepository.findAll().size, serverPackRepository.findAll().size, runConfigurationRepository.findAll().size)
        }
}