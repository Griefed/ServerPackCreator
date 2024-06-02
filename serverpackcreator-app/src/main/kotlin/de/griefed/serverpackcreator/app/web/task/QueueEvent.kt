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
package de.griefed.serverpackcreator.app.web.task

import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp

@Entity
class QueueEvent() {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    var id: Int = 0

    @Column
    var modPackId: Int? = 0

    @Column
    var serverPackId: Int? = null

    @Column
    var status: ModPackStatus? = null

    @Column
    var message: String = ""

    @CreationTimestamp
    @Column
    var timestamp: Timestamp? = null

    @ManyToMany(fetch = FetchType.EAGER)
    var errors: MutableList<ErrorEntry> = mutableListOf()

    constructor(
        modPackId: Int,
        serverPackId: Int?,
        status: ModPackStatus?,
        message: String,
        timestamp: Timestamp?,
        errors: MutableList<ErrorEntry>
    ) : this() {
        this.modPackId = modPackId
        this.serverPackId = serverPackId
        this.status = status
        this.message = message
        this.timestamp = timestamp
        this.errors = errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueueEvent

        if (modPackId != other.modPackId) return false
        if (serverPackId != other.serverPackId) return false
        if (status != other.status) return false
        if (message != other.message) return false
        if (timestamp != other.timestamp) return false
        if (errors != other.errors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = modPackId.hashCode()
        result = 31 * result + (serverPackId?.hashCode() ?: 0)
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + message.hashCode()
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        result = 31 * result + errors.hashCode()
        return result
    }

    override fun toString(): String {
        return "QueueEvent(id=$id, modPackId=$modPackId, serverPackId=$serverPackId, status=$status, message='$message', timestamp=$timestamp, errors=$errors)"
    }
}