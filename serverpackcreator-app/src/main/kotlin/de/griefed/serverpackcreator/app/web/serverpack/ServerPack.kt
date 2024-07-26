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
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp

@Entity
class ServerPack() {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    var id: Int = 0

    @Column
    var size: Int = 0

    @Column
    var downloads: Int = 0

    @Column
    var confirmedWorking: Int = 0

    @CreationTimestamp
    @Column
    var dateCreated: Timestamp? = null

    @Column
    var fileID: Long? = null

    @Column
    var sha256: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var runConfiguration: RunConfiguration? = null

    constructor(
        size: Double,
        downloads: Int,
        confirmedWorking: Int,
        dateCreated: Timestamp?,
        runConfiguration: RunConfiguration?,
        fileID: Long?,
        sha256: String?
    ) : this() {
        this.size = size.toInt()
        this.downloads = downloads
        this.confirmedWorking = confirmedWorking
        this.dateCreated = dateCreated
        this.runConfiguration = runConfiguration
        this.fileID = fileID
        this.sha256 = sha256
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