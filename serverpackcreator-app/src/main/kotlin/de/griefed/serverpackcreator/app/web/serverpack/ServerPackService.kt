/* Copyright (C) 2025 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.app.web.serverpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.web.storage.SavedFile
import de.griefed.serverpackcreator.app.web.storage.StorageSystem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*

/**
 * Class revolving around with server packs, like downloading, retrieving, deleting, voting etc.
 *
 * @author Griefed
 */
@Service
class ServerPackService @Autowired constructor(
    private val serverPackRepository: ServerPackRepository,
    private val serverPackDownloadRepository: ServerPackDownloadRepository,
    gridFsTemplate: GridFsTemplate,
    gridFsOperations: GridFsOperations,
    messageDigestInstance: MessageDigest,
    apiProperties: ApiProperties
) {
    private val rootLocation: Path = apiProperties.serverPacksDirectory.toPath()
    private val storage: StorageSystem = StorageSystem(
        rootLocation,
        messageDigestInstance,
        gridFsTemplate,
        gridFsOperations
    )

    fun getServerPack(id: String): Optional<ServerPack> {
        return serverPackRepository.findById(id)
    }

    /**
     * Increment the download counter for a given server pack entry in the database identified by the
     * database id.
     *
     * @param id The database id of the server pack.
     * @author Griefed
     */
    fun updateDownloadStats(id: String): Optional<ServerPack> {
        val request = serverPackRepository.findById(id)
        if (request.isPresent) {
            val pack = request.get()
            pack.downloads++
            serverPackDownloadRepository.save(ServerPackDownload(pack))
            return Optional.of(serverPackRepository.save(pack))
        } else {
            return Optional.empty()
        }
    }

    /**
     * Increment the download counter for a given server pack entry in the database identified by the
     * database id.
     *
     * @param serverPack The server pack for which to update the download counter.
     * @author Griefed
     */
    @Suppress("unused")
    fun updateDownloadStats(serverPack: ServerPack) {
        serverPack.downloads++
        serverPackRepository.save(serverPack)
    }

    /**
     * Either upvote or downvote a given server pack.
     *
     * @param id The database id of the server pack.
     * @param vote Either "up" or "down".
     * @return Returns ok if the vote went through, bad request if the passed vote was malformed, or
     * not found if the server pack could not be found.
     * @author Griefed
     */
    fun voteForServerPack(id: String, vote: String): ResponseEntity<Any> {
        val pack = serverPackRepository.findById(id)
        return if (pack.isPresent) {
            if (vote.equals("up", ignoreCase = true)) {
                pack.get().confirmedWorking++
                serverPackRepository.save(pack.get())
                ResponseEntity.ok().build()
            } else if (vote.equals("down", ignoreCase = true)) {
                pack.get().confirmedWorking--
                serverPackRepository.save(pack.get())
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.badRequest().build()
            }
        } else {
            ResponseEntity.notFound().build()
        }
    }

    fun getServerPacks(sort: Sort = Sort.by(Sort.Direction.DESC, "dateCreated")): List<ServerPack> {
        return serverPackRepository.findAll(sort)
    }

    fun getServerPacks(
        sizedPage: PageRequest,
        sort: Sort = Sort.by(Sort.Direction.DESC, "dateCreated")
    ): Page<ServerPack> {
        return serverPackRepository.findAll(sizedPage.withSort(sort))
    }

    /**
     * Save a server pack to the database.
     *
     * @author Griefed
     */
    fun saveServerPack(serverPack: ServerPack) {
        serverPackRepository.save(serverPack)
    }

    /**
     * Move the specified file to a new one which will receive a new filename based on the current time in milliseconds.
     * This is done to prevent clashes in case a server pack is generated from an existing modpack, using a new
     * [de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration].
     *
     * @author Griefed
     */
    fun storeServerPackFile(file: File): SavedFile {
        return storage.store(file).get()
    }

    /**
     * Deletes a server pack with the given id.
     *
     * @param serverPack The serverpack to delete.
     * @author Griefed
     */
    @Suppress("unused")
    fun deleteServerPack(serverPack: ServerPack) {
        serverPackRepository.deleteById(serverPack.id!!)
    }

    @Suppress("unused")
    fun deleteServerPack(id: String) {
        val serverPack = serverPackRepository.findById(id)
        if (serverPack.isPresent) {
            serverPackRepository.deleteById(id)
            storage.delete(serverPack.get().fileID!!)
        }
    }

    /**
     * Get the ZIP-archive of a server pack.
     *
     * @param serverPack The serverpack to be stored to disk from the database
     * @return The modpack-file, wrapped in an [Optional]
     * @author Griefed
     */
    fun getServerPackArchive(serverPack: ServerPack): Optional<File> {
        return storage.load(serverPack.fileID!!)
    }

    @Suppress("unused")
    fun getServerPackView(id: String): Optional<ServerPack> {
        return serverPackRepository.findById(id)
    }
}
