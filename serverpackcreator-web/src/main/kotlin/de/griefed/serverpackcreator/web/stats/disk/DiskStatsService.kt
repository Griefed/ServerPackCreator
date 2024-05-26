package de.griefed.serverpackcreator.web.stats.disk

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.size
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class DiskStatsService @Autowired constructor(
    private val apiProperties: ApiProperties
) {
    val stats: MutableList<DiskStatsData> = mutableListOf()

    init {
        val task = object : TimerTask() {
            override fun run() {
                refreshStats()
            }
        }
        val timer = Timer("DiskStatsTimer")
        timer.scheduleAtFixedRate(task, 0, 300000L)
    }

    private fun refreshStats() {
        stats.clear()
        stats.addAll(computeStats())
    }

    private fun computeStats(): List<DiskStatsData> {
        val stats = mutableListOf<DiskStatsData>()
        stats.add(computeStatsFromDirectory(apiProperties.homeDirectory, "Home"))
        stats.add(computeStatsFromDirectory(apiProperties.modpacksDirectory, "ModPacks"))
        stats.add(computeStatsFromDirectory(apiProperties.serverPacksDirectory, "Server Packs"))
        stats.add(computeStatsFromDirectory(apiProperties.propertiesDirectory, "Properties"))
        stats.add(computeStatsFromDirectory(apiProperties.configsDirectory, "Configs"))
        stats.add(computeStatsFromDirectory(apiProperties.serverFilesDirectory, "Server Files"))
        stats.add(computeStatsFromDirectory(apiProperties.iconsDirectory, "Icons"))
        stats.add(computeStatsFromDirectory(apiProperties.pluginsDirectory, "Plugins"))
        stats.add(computeStatsFromDirectory(apiProperties.pluginsConfigsDirectory, "Plugin Configs"))
        stats.add(computeStatsFromDirectory(apiProperties.manifestsDirectory, "Manifests"))
        stats.add(computeStatsFromDirectory(apiProperties.minecraftServerManifestsDirectory, "MC Server Manifests"))
        stats.add(computeStatsFromDirectory(apiProperties.installerCacheDirectory, "Installer Cache"))
        stats.add(computeStatsFromDirectory(apiProperties.logsDirectory, "Logs"))
        stats.add(computeStatsFromDirectory(apiProperties.tomcatBaseDirectory, "Tomcat Base"))
        stats.add(computeStatsFromDirectory(apiProperties.tomcatLogsDirectory, "Tomcat Logs"))
        stats.add(computeStatsFromDirectory(apiProperties.workDirectory, "Work"))
        stats.add(computeStatsFromDirectory(apiProperties.tempDirectory, "Temp"))
        return stats
    }

    private fun computeStatsFromDirectory(directory: File, identifier: String): DiskStatsData {
        val root: File = directory.absoluteFile.toPath().root.toFile()
        val directorySize = directory.size()
        val stats = DiskStatsData(identifier, directory.name, root.toString(), root.totalSpace, root.freeSpace, directorySize.toLong())
        return stats
    }
}