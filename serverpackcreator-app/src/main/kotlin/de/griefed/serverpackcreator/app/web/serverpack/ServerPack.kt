/* Copyright (C) 2026 Griefed
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

import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.util.*

/**
 * A generated server pack: the archive, the modpack it came from, and the run configuration that produced it.
 * 
 * Two packs from one modpack differ only by their [runConfiguration], which is why that field is part of equality.
 */
@Document
class ServerPack {

    /** The document id, assigned by MongoDB. `private set` so only Spring Data's persistence constructor fills it. */
    @MongoId(FieldType.STRING)
    var id: String? = null
        private set
    /** Which [ModPack] this was generated from. */
    var modpackId: String = ""
    /** Archive size in bytes. */
    var size: Int = 0
    /** How often this server pack has been downloaded. */
    var downloads: Int = 0
    /** How many users have voted that this pack actually runs — a counter, not a flag. */
    var confirmedWorking: Int = 0
    /** When the generation finished. */
    var dateCreated: Date = Date(System.currentTimeMillis())
    /** The stored archive's name on disk. */
    var fileID: String? = null
    /** The name the archive is served under, which need not match [fileID]. */
    var fileName: String? = null
    /** SHA256 of the archive. Indexed, because it is what the de-duplication looks up. */
    var sha256: String? = null

    /** The configuration this pack was generated with — what makes two packs from one modpack different. */
    @DBRef
    var runConfiguration: RunConfiguration? = null

    constructor(
        size: Int,
        runConfiguration: RunConfiguration?,
        fileID: String?,
        fileName: String?,
        sha256: String?,
        modpackId: String
    ) {
        this.size = size
        this.runConfiguration = runConfiguration
        this.fileID = fileID
        this.fileName = fileName
        this.sha256 = sha256
        this.modpackId = modpackId
    }

    @PersistenceCreator
    private constructor(
        id: String,
        size: Int,
        downloads: Int,
        confirmedWorking: Int,
        dateCreated: Date,
        runConfiguration: RunConfiguration?,
        fileID: String?,
        fileName: String?,
        sha256: String?,
        modpackId: String
    ) : this(size, runConfiguration, fileID, fileName, sha256, modpackId) {
        this.id = id
        this.downloads = downloads
        this.confirmedWorking = confirmedWorking
        this.dateCreated = dateCreated
    }

    /**
     * Two server packs are the same artefact when their stored file, hash and run configuration match.
     * 
     * Ignores the counters and [dateCreated] on purpose: a pack that has been downloaded since is still the same
     * pack, and treating it otherwise would defeat the de-duplication this equality exists for.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ServerPack

        if (fileID != other.fileID) return false
        if (sha256 != other.sha256) return false
        if (runConfiguration != other.runConfiguration) return false

        return true
    }

    /** Hashes the same three fields [equals] compares. */
    override fun hashCode(): Int {
        var result = fileID.hashCode()
        result = 31 * result + (sha256.hashCode())
        result = 31 * result + (runConfiguration.hashCode())
        return result
    }

    /** Every field, for a log line. */
    override fun toString(): String {
        return "ServerPack(id=$id, size=$size, downloads=$downloads, confirmedWorking=$confirmedWorking, dateCreated=$dateCreated, fileID=$fileID, sha256=$sha256, runConfiguration=$runConfiguration)"
    }
}