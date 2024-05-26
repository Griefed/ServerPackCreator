package de.griefed.serverpackcreator.web.modpack

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ModPackDownloadRepository : JpaRepository<ModPackDownload, Int> {
    fun findAllByModPack(modPack: ModPack): List<ModPackDownload>
}