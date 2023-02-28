/* Copyright (C) 2023  Griefed
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

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.net.MalformedURLException
import java.nio.file.Paths
import java.sql.Timestamp
import java.util.*

/**
 * Class revolving around with server packs, like downloading, retrieving, deleting, voting etc.
 *
 * @author Griefed
 */
@Service
class ServerPackService @Autowired constructor(private val serverPackRepository: ServerPackRepository) {
    private val log = cachedLoggerOf(this.javaClass)

    /**
     * Download a server pack with the given database id.
     *
     * @param id The database id of the server pack to download.
     * @return Returns a curseResponse entity with either the server pack as a downloadable file, or a
     * curseResponse entity with a not found body.
     * @author Griefed
     */
    fun downloadServerPackById(id: Int): ResponseEntity<Resource?> {
        return if (serverPackRepository.findById(id).isPresent
            && serverPackRepository.findById(id).get().status!!.matches("Available".toRegex())
        ) {
            val serverPackModel: ServerPackModel = serverPackRepository.findById(id).get()
            val path = Paths.get(serverPackModel.path!!)
            var resource: Resource? = null
            val contentType = "application/zip"
            try {
                resource = UrlResource(path.toUri())
            } catch (ex: MalformedURLException) {
                log.error("Error generating download for server pack with ID$id.", ex)
            }
            updateDownloadCounter(id)
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\""
                            + serverPackModel.fileDiskName!!.replace(".zip", "")
                            + "_server_pack.zip"
                            + "\""
                )
                .body(resource)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Increment the download counter for a given server pack entry in the database identified by the
     * database id.
     *
     * @param id The database id of the server pack.
     * @author Griefed
     */
    fun updateDownloadCounter(id: Int) {
        if (serverPackRepository.findById(id).isPresent) {
            val serverPackModelFromDB: ServerPackModel = serverPackRepository.findById(id).get()
            serverPackModelFromDB.downloads = serverPackModelFromDB.downloads++
            serverPackRepository.save(serverPackModelFromDB)
        }
    }

    /**
     * Either upvote or downvote a given server pack.
     *
     * @param voting The database id of the server pack and whether it should be up- or downvoted.
     * @return Returns ok if the vote went through, bad request if the passed vote was malformed, or
     * not found if the server pack could not be found.
     * @author Griefed
     */
    fun voteForServerPack(voting: String): ResponseEntity<Any> {
        val vote = voting.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
        val id = vote[0].toInt()
        return if (serverPackRepository.findById(id).isPresent
            && serverPackRepository.findById(id).get().status.equals("Available")
        ) {
            if (vote[1].equals("up", ignoreCase = true)) {
                updateConfirmedCounter(id, CounterChange.INCREMENT)
                ResponseEntity.ok().build()
            } else if (vote[1].equals("down", ignoreCase = true)) {
                updateConfirmedCounter(id, CounterChange.DECREMENT)
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.badRequest().build()
            }
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Either increment or decrement the confirmed working value of a given server pack entry in the
     * database, identified by the database id.
     *
     * @param id   The database id of the server pack.
     * @param type [CounterChange.INCREMENT] to up the counter by one. [CounterChange.DECREMENT] to decrease the counter by one.
     * @author Griefed
     */
    fun updateConfirmedCounter(
        id: Int,
        type: CounterChange
    ) {
        when (type) {
            CounterChange.DECREMENT -> {
                if (serverPackRepository.findById(id).isPresent) {
                    val serverPackModelFromDB: ServerPackModel = serverPackRepository.findById(id).get()
                    serverPackModelFromDB.confirmedWorking = serverPackModelFromDB.confirmedWorking--
                    serverPackRepository.save(serverPackModelFromDB)
                }
            }

            CounterChange.INCREMENT -> {
                if (serverPackRepository.findById(id).isPresent) {
                    val serverPackModelFromDB: ServerPackModel = serverPackRepository.findById(id).get()
                    serverPackModelFromDB.confirmedWorking = serverPackModelFromDB.confirmedWorking++
                    serverPackRepository.save(serverPackModelFromDB)
                }
            }
        }
    }

    /**
     * Get a list of all available server packs.
     *
     * @return List ServerPackModel. Returns a list of all available server packs.
     * @author Griefed
     */
    fun getServerPacks(): List<ServerPackModel> {
        val serverPackModels: MutableList<ServerPackModel> = ArrayList(100)
        for (model in serverPackRepository.findAll()) {
            serverPackModels.add(model!!)
        }
        return serverPackModels
    }

    /**
     * Store a server pack in the database.
     *
     * @param serverPackModel Instance of [ServerPackModel] to store in the database.
     * @author Griefed
     */
    fun insert(serverPackModel: ServerPackModel) {
        serverPackRepository.save(serverPackModel)
    }

    /**
     * Update a server pack database entry with the given database id.
     *
     * @param id              Integer. The database id of the server pack to initialize.
     * @param serverPackModel Instance of [ServerPackModel] with which to initialize the entry
     * in the database.
     * @author Griefed
     */
    fun updateServerPackByID(
        id: Int,
        serverPackModel: ServerPackModel
    ) {
        if (serverPackRepository.findById(id).isPresent) {
            val serverPackModelFromDB: ServerPackModel = serverPackRepository.findById(id).get()
            log.debug("Updating database with: $serverPackModel")
            serverPackModelFromDB.projectName = serverPackModel.projectName
            serverPackModelFromDB.fileName = serverPackModel.fileName
            serverPackModelFromDB.fileDiskName = serverPackModel.fileDiskName
            serverPackModelFromDB.size = serverPackModel.size
            serverPackModelFromDB.downloads = serverPackModel.downloads
            serverPackModelFromDB.confirmedWorking = serverPackModel.confirmedWorking
            serverPackModelFromDB.status = serverPackModel.status
            serverPackModelFromDB.lastModified = Timestamp(Date().time)
            serverPackModelFromDB.path = serverPackModel.path
            serverPackRepository.save(serverPackModelFromDB)
        }
    }

    /**
     * Deletes a server pack with the given id.
     *
     * @param id The database id of the server pack to delete.
     * @author Griefed
     */
    fun deleteServerPack(id: Int) {
        serverPackRepository.deleteById(id)
    }
}


enum class CounterChange {
    DECREMENT,
    INCREMENT
}