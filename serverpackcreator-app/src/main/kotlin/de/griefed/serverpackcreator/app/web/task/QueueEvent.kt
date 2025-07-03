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
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.util.Date

@Document
class QueueEvent() {

    @MongoId(FieldType.STRING)
    var id: String? = null
        private set
    var modPackId: String? = null
    var serverPackId: String? = null
    var status: ModPackStatus? = null
    var message: String = ""
    var timestamp: Date = Date(System.currentTimeMillis())

    @DBRef
    var errors: MutableList<ErrorEntry> = mutableListOf()

    @PersistenceCreator
    private constructor(
        id: String,
        modPackId: String,
        serverPackId: String?,
        status: ModPackStatus?,
        message: String,
        timestamp: Date,
        errors: MutableList<ErrorEntry>
    ) : this() {
        this.id = id
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
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + errors.hashCode()
        return result
    }

    override fun toString(): String {
        return "QueueEvent(id=$id, modPackId=$modPackId, serverPackId=$serverPackId, status=$status, message='$message', timestamp=$timestamp, errors=$errors)"
    }
}