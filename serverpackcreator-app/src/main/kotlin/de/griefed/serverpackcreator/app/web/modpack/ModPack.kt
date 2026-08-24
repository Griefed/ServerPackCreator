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
package de.griefed.serverpackcreator.app.web.modpack

import de.griefed.serverpackcreator.api.config.ModpackSource
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.util.*

/**
 * An uploaded modpack: where it came from, what it weighs, and how far its processing has got.
 * 
 * The no-argument constructor is what Spring Data instantiates; the `@PersistenceCreator` one below it is how a
 * stored document is read back.
 */
@Document
class ModPack() {

    /** The document id, assigned by MongoDB. `private set` so only Spring Data's persistence constructor fills it. */
    @MongoId(FieldType.STRING)
    var id: String? = null
        private set
    /** The platform's project id when the pack came from CurseForge or Modrinth; empty for a plain upload. */
    var projectID: String = ""
    /** The platform's version id, alongside [projectID]. */
    var versionID: String = ""
    /** When this pack was uploaded. Set on construction, not on save, so a retry keeps the original time. */
    var dateCreated: Date = Date(System.currentTimeMillis())
    /** Display name, which for an upload is the archive's file name. */
    var name: String = ""
    /** Archive size in bytes, kept so the disk stats need not stat every file. */
    var size: Int = 0
    /** How often the archive has been downloaded. The getter maps `null` to `0`, so documents written before the field existed read as zero rather than blowing up a response. */
    var downloads: Int? = 0
        get() {
            return field ?: 0
        }
    /** Where this pack is in the queue — the field the SPA polls. */
    var status: ModPackStatus = ModPackStatus.QUEUED
    /** How it arrived: a ZIP upload, or fetched from a platform. */
    var source: ModpackSource = ModpackSource.ZIP
    /** The stored archive's name on disk, which is *not* the display [name]. */
    var fileID: String? = null
    /** SHA256 of the uploaded archive. Indexed: it is the key the upload duplicate-check looks up. */
    @Indexed
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
        fileID: String?,
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

    /**
     * Two modpacks are the same upload when their project, version, name, source and hash match.
     * 
     * Deliberately **not** id-based, and deliberately ignoring [dateCreated], [size], [downloads], [status],
     * [fileID] and the server packs: those are either assigned by the database or mutable processing state, and the
     * question this answers is "have we already got this pack?" — which is what the upload duplicate-check asks.
     */
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

    /** Hashes exactly the five fields [equals] compares, so the two cannot disagree. */
    override fun hashCode(): Int {
        var result = projectID.hashCode()
        result = 31 * result + versionID.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (sha256.hashCode())
        return result
    }

    /** Every field, for a log line — including the ones [equals] ignores, which is the point when diagnosing a queue. */
    override fun toString(): String {
        return "ModPack(id=$id, projectID='$projectID', versionID='$versionID', dateCreated=$dateCreated, name='$name', size=$size, downloads=$downloads, status=$status, source=$source, fileID=$fileID, sha256=$sha256, serverPacks=$serverPacks)"
    }
}