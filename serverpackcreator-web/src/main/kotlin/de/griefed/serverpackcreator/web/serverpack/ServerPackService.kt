/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.web.serverpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.web.data.ServerPack
import de.griefed.serverpackcreator.web.data.ServerPackView
import de.griefed.serverpackcreator.web.storage.StorageSystem
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.moveTo

/**
 * Class revolving around with server packs, like downloading, retrieving, deleting, voting etc.
 *
 * @author Griefed
 */
@Service
class ServerPackService @Autowired constructor(
    private val serverPackRepository: ServerPackRepository,
    messageDigestInstance: MessageDigest,
    apiProperties: ApiProperties
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val rootLocation: Path = apiProperties.serverPacksDirectory.toPath()
    private val storage: StorageSystem = StorageSystem(rootLocation, messageDigestInstance)

    fun getServerPack(id: Int): Optional<ServerPack> {
        return serverPackRepository.findById(id)
    }

    /**
     * Increment the download counter for a given server pack entry in the database identified by the
     * database id.
     *
     * @param id The database id of the server pack.
     * @author Griefed
     */
    fun updateDownloadCounter(id: Int): Optional<ServerPack> {
        val request = serverPackRepository.findById(id)
        if (request.isPresent) {
            val pack = request.get()
            pack.downloads++
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
    fun updateDownloadCounter(serverPack: ServerPack) {
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
    fun voteForServerPack(id: Int, vote: String): ResponseEntity<Any> {
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

    /**
     * Get a list of all available server packs.
     *
     * @return List ServerPackModel. Returns a list of all available server packs.
     * @author Griefed
     */
    fun getServerPacks(): List<ServerPackView> {
        return serverPackRepository.findAllProjectedBy(Sort.by(Sort.Direction.DESC, "dateCreated"))
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
     * [de.griefed.serverpackcreator.web.data.RunConfiguration].
     *
     * @author Griefed
     */
    fun moveServerPack(serverPackFile: File) : File {
        val id = System.currentTimeMillis()
        val newLocation = File(rootLocation.toFile(),"${id}.zip").absoluteFile.toPath()
        return serverPackFile.absoluteFile.toPath().moveTo(
            newLocation
        ).toFile()
    }

    /**
     * Deletes a server pack with the given id.
     *
     * @param serverPack The server pack to delete.
     * @author Griefed
     */
    fun deleteServerPack(serverPack: ServerPack) {
        serverPackRepository.deleteById(serverPack.id)
    }

    fun deleteServerPack(id: Int) {
        val serverpack = serverPackRepository.findById(id)
        if (serverpack.isPresent) {
            serverPackRepository.deleteById(id)
            storage.delete(serverpack.get().fileID!!)
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

    fun getServerPackArchive(id: Long): Optional<File> {
        return storage.load(id)
    }

    fun getServerPackView(id: Int): Optional<ServerPackView> {
        return serverPackRepository.findProjectedById(id)
    }
}
