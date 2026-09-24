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

import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.util.*

/**
 * One recorded server-pack download — a row per download rather than a counter, which is what makes the
 * download *history* possible. The counter on the pack itself is kept alongside for the cheap case.
 */
@Document
class ServerPackDownload {

    /**
     * The document id, assigned by MongoDB. `private set` so only Spring Data's persistence constructor
     * fills it.
     *
     * Deliberately separate from [downloadedAt], which used to *be* the id: that made the id a global
     * millisecond rather than a per-pack one, so two downloads of any two packs in the same millisecond
     * collided and `save` overwrote the earlier row.
     */
    @MongoId(FieldType.STRING)
    var id: String? = null
        private set

    /** When the download happened, set on construction so a retry keeps the original time. */
    var downloadedAt: Date = Date(System.currentTimeMillis())
        private set

    /** Which pack was downloaded. */
    @DBRef
    var serverPack: ServerPack

    constructor(serverPack: ServerPack) {
        this.serverPack = serverPack
    }

    @Suppress("unused")
    @PersistenceCreator
    private constructor(id: String?, downloadedAt: Date, serverPack: ServerPack) {
        this.id = id
        this.downloadedAt = downloadedAt
        this.serverPack = serverPack
    }
}