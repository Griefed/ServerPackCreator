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
package de.griefed.serverpackcreator.app.web.serverpack

import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.util.*

@Document
class ServerPack {

    @MongoId(FieldType.STRING)
    var id: String? = null
        private set
    var modpackId: String = ""
    var size: Int = 0
    var downloads: Int = 0
    var confirmedWorking: Int = 0
    var dateCreated: Date = Date(System.currentTimeMillis())
    var fileID: Long? = null
    var sha256: String? = null

    @DBRef
    var runConfiguration: RunConfiguration? = null

    constructor(
        size: Int,
        runConfiguration: RunConfiguration?,
        fileID: Long?,
        sha256: String?,
        modpackId: String
    ) {
        this.size = size
        this.runConfiguration = runConfiguration
        this.fileID = fileID
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
        fileID: Long?,
        sha256: String?,
        modpackId: String
    ) : this(size, runConfiguration, fileID, sha256, modpackId) {
        this.id = id
        this.downloads = downloads
        this.confirmedWorking = confirmedWorking
        this.dateCreated = dateCreated
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ServerPack

        if (fileID != other.fileID) return false
        if (sha256 != other.sha256) return false
        if (runConfiguration != other.runConfiguration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileID?.hashCode() ?: 0
        result = 31 * result + (sha256?.hashCode() ?: 0)
        result = 31 * result + (runConfiguration?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ServerPack(id=$id, size=$size, downloads=$downloads, confirmedWorking=$confirmedWorking, dateCreated=$dateCreated, fileID=$fileID, sha256=$sha256, runConfiguration=$runConfiguration)"
    }
}