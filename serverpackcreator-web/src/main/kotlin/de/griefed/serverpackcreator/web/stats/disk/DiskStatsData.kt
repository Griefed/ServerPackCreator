package de.griefed.serverpackcreator.web.stats.disk

data class DiskStatsData(
    val identifier: String,
    val dirName: String,
    val rootName: String,
    val totalSpace: Long,
    val freeSpace: Long,
    val usedBySPC: Long
)
