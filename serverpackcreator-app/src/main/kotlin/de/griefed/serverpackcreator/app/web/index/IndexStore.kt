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
package de.griefed.serverpackcreator.app.web.index

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.stereotype.Component

/**
 * The one database operation creating an index needs, behind a seam.
 *
 * Same reason `MigrationStore` exists: what matters about [DeclaredIndexCreator] is *which* indexes it
 * asks for and *when*, and neither is observable while it talks to `MongoTemplate` directly. One method,
 * so a recording double is a few lines.
 */
interface IndexStore {

    /** Creates [definition] on [collection], returning the resulting index's name. */
    fun create(collection: String, definition: IndexDefinition): String
}

/**
 * The [IndexStore] the running application uses, delegating to `MongoTemplate.indexOps`.
 *
 * Creating an existing, identical index is a no-op server-side, which is what makes calling this on every
 * start acceptable rather than merely idempotent-by-luck.
 */
@Component
class MongoIndexStore(private val mongoTemplate: MongoTemplate) : IndexStore {

    override fun create(collection: String, definition: IndexDefinition): String =
        mongoTemplate.indexOps(collection).createIndex(definition)
}
