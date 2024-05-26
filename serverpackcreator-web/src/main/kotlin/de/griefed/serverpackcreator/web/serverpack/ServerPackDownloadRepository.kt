package de.griefed.serverpackcreator.web.serverpack

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ServerPackDownloadRepository : JpaRepository<ServerPackDownload, Int> {
    fun findAllByServerPack(serverPack: ServerPack): List<ServerPackDownload>
}