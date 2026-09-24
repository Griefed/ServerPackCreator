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
package de.griefed.serverpackcreator.app.web.storage

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.convert.QueryMapper
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import java.util.Optional

/**
 * Guards the GridFS half of storage: the id round-trip it depends on, and what a miss returns.
 *
 * No database. `StorageSystem` stores a file under the `ObjectId` GridFS minted, then hands that id
 * back as a **String**, so every later read and delete queries `_id` with a String against a field
 * holding an `ObjectId`. Whether that matches is decided by Spring Data's own query mapper, which can
 * be asked directly — the same approach this module already uses for its index and collection-name
 * declarations.
 */
class DatabaseStorageServiceTest {

    private val gridFsTemplate: GridFsTemplate = mockk()
    private val gridFsOperations: GridFsOperations = mockk()
    private val service = DatabaseStorageService(gridFsTemplate, gridFsOperations)

    @Test
    fun springDataConvertsAnIdStringToAnObjectIdSoTheLookupMatches() {
        // The load and delete paths are built on this. If it ever stops holding, both silently stop
        // finding anything -- a query that matches nothing looks exactly like a file that is not there.
        val mapper = QueryMapper(MappingMongoConverter(NoOpDbRefResolver.INSTANCE, MongoMappingContext()))

        val mapped: Document = mapper.getMappedObject(
            Query(Criteria.where("_id").`is`("651f3c0e9a1b2c3d4e5f6071")).queryObject,
            Optional.empty()
        )

        Assertions.assertInstanceOf(
            ObjectId::class.java,
            mapped["_id"],
            "an id String is no longer converted to an ObjectId; GridFS lookups by id will match nothing"
        )
        Assertions.assertEquals(ObjectId("651f3c0e9a1b2c3d4e5f6071"), mapped["_id"])
    }

    @Test
    fun loadingAnIdThatGridFsDoesNotHoldIsEmptyRatherThanAThrow() {
        // Reached whenever the filesystem copy is gone, which is the ordinary state after a modpack has
        // been cleaned up. An NPE here is an unhandled 500 on the download route, not a 404.
        every<Any?> { gridFsTemplate.findOne(any()) } returns null

        Assertions.assertTrue(service.load("651f3c0e9a1b2c3d4e5f6072").isEmpty)
    }

    @Test
    fun deletingRemovesTheFileFromGridFs() {
        val query = slot<Query>()
        every { gridFsTemplate.delete(capture(query)) } returns Unit

        service.delete("651f3c0e9a1b2c3d4e5f6071")

        verify(exactly = 1) { gridFsTemplate.delete(any()) }
        Assertions.assertEquals(
            "651f3c0e9a1b2c3d4e5f6071",
            query.captured.queryObject["_id"],
            "the delete must be keyed on the same id the file was stored under"
        )
    }
}
