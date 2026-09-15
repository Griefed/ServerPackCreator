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
package de.griefed.serverpackcreator.app.web.migration

import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

/**
 * The four database operations a migration needs, behind an interface.
 *
 * Exists so a migration's *safety decisions* can be tested without a database. Those decisions — rewrite
 * before dropping, never drop when nothing was rewritten, keep going when one collection refuses — are the
 * part that can lose a user's data if it is wrong, and they were unreachable from a test while the
 * migration talked to `MongoTemplate` directly. Same reasoning as `ModpackZipInspector`'s injected
 * `openZip`: the interesting property is invisible from outside unless the collaborator can be observed.
 */
interface MigrationStore {
    /** Every document in [collection]. */
    fun findAll(collection: String): List<Document>

    /** Replaces the document in [collection] carrying [id] with [replacement]. */
    fun replace(collection: String, id: Any?, replacement: Document)

    /** Whether [collection] exists. */
    fun exists(collection: String): Boolean

    /** Drops [collection]. */
    fun drop(collection: String)
}

/**
 * The real [MigrationStore], talking to MongoDB through [MongoTemplate].
 *
 * `MongoTemplate` rather than a repository, necessarily: a migration exists because the mapped type can no
 * longer read the stored shape, so the repository is useless to it by definition.
 */
@Component
class MongoMigrationStore(private val mongoTemplate: MongoTemplate) : MigrationStore {

    /**
     * Reads the whole collection into a list up front, deliberately, rather than streaming the cursor.
     *
     * Writing while iterating a live cursor can hand the same document back twice if it moves, which is
     * only harmless as long as every rewrite stays idempotent. Materialising first removes that coupling —
     * a future migration that is *not* idempotent cannot be broken by it.
     */
    override fun findAll(collection: String): List<Document> =
        mongoTemplate.getCollection(collection).find().toList()

    override fun replace(collection: String, id: Any?, replacement: Document) {
        mongoTemplate.getCollection(collection).replaceOne(Document("_id", id), replacement)
    }

    override fun exists(collection: String): Boolean = mongoTemplate.collectionExists(collection)

    override fun drop(collection: String) = mongoTemplate.dropCollection(collection)
}
