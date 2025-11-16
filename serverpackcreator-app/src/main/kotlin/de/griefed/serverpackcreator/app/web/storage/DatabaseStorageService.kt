/* Copyright (C) 2025 Griefed
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

    fun store(file: File): ObjectId {
        val originalName = determineFilename(file.name)
        val metaData = BasicDBObject()
        metaData.put("type", "zip")
        metaData.put("title", originalName)
        val objectId = gridFsTemplate.store(
            FileInputStream(file),
            originalName,
            metaData
        )
        return objectId
    }

    fun load(id: String): Optional<Pair<GridFSFile, GridFsResource>> {
        val result = gridFsTemplate.findOne(query(id))
        if (result != null) {
            return Optional.of(
                Pair(
                    result,
                    gridFsOperations.getResource(result)
                )
            )
        }
        return Optional.empty()
    }
}