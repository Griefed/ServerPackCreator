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
package de.griefed.serverpackcreator.app.web.task

import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.util.*

/** One moment in a modpack's processing, written as the queue advances. Append-only: the SPA polls these to show progress. */
@Document
class QueueEvent() {

    /** The document id, assigned by MongoDB. `private set` so only Spring Data's persistence constructor fills it. */
    @MongoId(FieldType.STRING)
    var id: String? = null
        private set
    /** Which modpack this event is about, or `null` for an event that precedes one. */
    var modPackId: String? = null
    /** Which server pack this event is about, once one exists. */
    var serverPackId: String? = null
    /** The status being reported. */
    var status: ModPackStatus? = null
    /** Human-readable detail the SPA shows beside the status. */
    var message: String = ""
    /** When the event happened, which is what the SPA orders by. */
    var timestamp: Date = Date(System.currentTimeMillis())

    /** The errors that came with a failed status; empty for every other status. */
    @DBRef
    var errors: MutableList<ErrorEntry> = mutableListOf()

    @Suppress("unused")
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

    /**
     * Compares everything except the id, so the same event written twice is recognised as one. Two events with
     * identical content but different ids are the same report, and the queue does re-report.
     */
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

    /** Hashes the same fields [equals] compares. */
    override fun hashCode(): Int {
        var result = modPackId.hashCode()
        result = 31 * result + (serverPackId.hashCode())
        result = 31 * result + (status.hashCode())
        result = 31 * result + message.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + errors.hashCode()
        return result
    }

    /** Every field, for a log line. */
    override fun toString(): String {
        return "QueueEvent(id=$id, modPackId=$modPackId, serverPackId=$serverPackId, status=$status, message='$message', timestamp=$timestamp, errors=$errors)"
    }
}