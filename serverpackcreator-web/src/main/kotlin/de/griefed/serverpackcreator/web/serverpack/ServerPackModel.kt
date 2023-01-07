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

import de.griefed.serverpackcreator.api.PackConfig
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * Class containing all fields and therefore all information gathered from a submitted CurseForge
 * project and fileID, or modpack export. By extending [PackConfig] we inherit all
 * basic fields required for the generation of a server pack and can add only those we require in
 * the REST API portion of ServerPackCreator.<br></br> We mark this class with [Entity] because we
 * also use this class for storing information in our database.
 *
 * @author Griefed
 */
@Entity
@Suppress("unused")
class ServerPackModel : PackConfig {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    var id = 0

    @Column
    override var projectName: String? = null

    @Column
    override var fileName: String? = null

    @Column
    override var fileDiskName: String? = null

    @Column
    var size: Double

    @Column
    var downloads: Int

    @Column
    var confirmedWorking: Int

    @Column
    var status: String? = null

    @Column
    var path: String? = null

    @CreationTimestamp
    @Column(updatable = false)
    var dateCreated: Timestamp? = null

    @UpdateTimestamp
    var lastModified: Timestamp? = null

    constructor() {
        projectName = ""
        fileName = ""
        fileDiskName = ""
        size = 0.0
        downloads = 0
        confirmedWorking = 0
        status = "Queued"
    }

    constructor(
        id: Int,
        fileName: String,
        displayName: String,
        size: Double,
        downloads: Int,
        confirmedWorking: Int,
        status: String?,
        dateCreated: Timestamp?,
        lastModified: Timestamp?
    ) {
        this.id = id
        this.fileName = fileName
        this.fileDiskName = displayName
        this.size = size
        this.downloads = downloads
        this.confirmedWorking = confirmedWorking
        this.status = status
        this.dateCreated = dateCreated
        this.lastModified = lastModified
    }

    override fun toString(): String {
        return "ServerPackModel(" +
                "id=$id, " +
                "projectName=$projectName, " +
                "fileName=$fileName, " +
                "fileDiskName=$fileDiskName, " +
                "size=$size, " +
                "downloads=$downloads, " +
                "confirmedWorking=$confirmedWorking, " +
                "status=$status, " +
                "path=$path, " +
                "dateCreated=$dateCreated, " +
                "lastModified=$lastModified)"
    }
}