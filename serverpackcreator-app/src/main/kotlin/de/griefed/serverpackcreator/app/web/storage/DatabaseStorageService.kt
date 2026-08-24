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

import com.mongodb.BasicDBObject
import com.mongodb.client.gridfs.model.GridFSFile
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * GridFS storage, kept for installations that still hold their files in MongoDB. New files go to the filesystem;
 * this exists so the old ones can still be read and migrated.
 */
class DatabaseStorageService(
    private val gridFsTemplate: GridFsTemplate,
    private val gridFsOperations: GridFsOperations
) {

    private fun query(id: String): Query {
        return Query(Criteria.where("_id").`is`(id))
    }

    private fun determineFilename(filename: String): String {
        return if (
            filename.contains("-orig-") &&
            filename.split("-orig-").size >= 2 &&
            filename.split("-orig-")[1].isNotEmpty()
        ) {
            filename.split("-orig-")[1]
        } else {
            filename
        }
    }

    /** Store a file in GridFS, returning its object id. */
    fun store(file: File): ObjectId {
        val originalName = determineFilename(file.name)
        val metaData = BasicDBObject()
        metaData["type"] = "zip"
        metaData["title"] = originalName
        val objectId = gridFsTemplate.store(
            FileInputStream(file),
            originalName,
            metaData
        )
        return objectId
    }

    /** Read a file back out of GridFS, as the metadata and the resource together. */
    fun load(id: String): Optional<Pair<GridFSFile, GridFsResource>> {
        val result = gridFsTemplate.findOne(query(id))
        return Optional.of(
            Pair(
                result,
                gridFsOperations.getResource(result)
            )
        )
    }
}