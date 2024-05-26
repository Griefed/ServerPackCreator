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
package de.griefed.serverpackcreator.web.modpack

import de.griefed.serverpackcreator.api.ModpackSource
import de.griefed.serverpackcreator.web.serverpack.ServerPack
import jakarta.persistence.*
import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType
import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp

@Entity
class ModPack {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    var id : Int = 0

    @Column
    var projectID: String = ""

    @Column
    var versionID: String = ""

    @CreationTimestamp
    @Column
    var dateCreated: Timestamp? = null

    @Column
    var name: String = ""

    @Column
    var size: Int = 0

    @Column
    var downloads: Int? = 0
        get() {
            return field ?: 0
        }

    @Column
    var status: ModPackStatus = ModPackStatus.QUEUED

    @Column
    var source: ModpackSource = ModpackSource.ZIP

    @Column
    var fileID: Long? = null

    @Column
    var sha256: String? = null

    @OneToMany(fetch = FetchType.EAGER)
    @Cascade(value = [CascadeType.ALL])
    var serverPacks: MutableList<ServerPack> = mutableListOf()

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