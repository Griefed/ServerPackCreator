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
package de.griefed.serverpackcreator.app.web.modpack

import de.griefed.serverpackcreator.api.config.ModpackSource
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.util.*

@Document
class ModPack() {

    @MongoId(FieldType.STRING)
    var id: String? = null
        private set
    var projectID: String = ""
    var versionID: String = ""
    var dateCreated: Date = Date(System.currentTimeMillis())
    var name: String = ""
    var size: Int = 0
    var downloads: Int? = 0
        get() {
            return field ?: 0
        }
    var status: ModPackStatus = ModPackStatus.QUEUED
    var source: ModpackSource = ModpackSource.ZIP
    var fileID: Long? = null
    var sha256: String? = null

    @DBRef
    var serverPacks: MutableList<ServerPack> = mutableListOf()

    @PersistenceCreator
    private constructor(
        id: String,
        projectID: String,
        versionID: String,
        dateCreated: Date,
        name: String,
        size: Int,
        downloads: Int?,
        status: ModPackStatus,
        source: ModpackSource,
        fileID: Long?,
        sha256: String?,
        serverPacks: MutableList<ServerPack>
    ) : this() {
        this.id = id
        this.projectID = projectID
        this.versionID = versionID
        this.dateCreated = dateCreated
        this.name = name
        this.size = size
        this.downloads = downloads
        this.status = status
        this.source = source
        this.fileID = fileID
        this.sha256 = sha256
        this.serverPacks = serverPacks
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModPack

        if (projectID != other.projectID) return false
        if (versionID != other.versionID) return false
        if (name != other.name) return false
        if (source != other.source) return false
        if (sha256 != other.sha256) return false

        return true
    }

    override fun hashCode(): Int {
        var result = projectID.hashCode()
        result = 31 * result + versionID.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (sha256?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ModPack(id=$id, projectID='$projectID', versionID='$versionID', dateCreated=$dateCreated, name='$name', size=$size, downloads=$downloads, status=$status, source=$source, fileID=$fileID, sha256=$sha256, serverPacks=$serverPacks)"
    }
}