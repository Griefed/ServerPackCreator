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
package de.griefed.serverpackcreator.app.web.integration

import de.griefed.serverpackcreator.app.web.WebService
import de.griefed.serverpackcreator.app.web.migration.RunConfigurationListMigrationRunner
import de.griefed.serverpackcreator.app.web.modpack.ModPackDownload
import de.griefed.serverpackcreator.app.web.modpack.ModPackDownloadRepository
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.storage.StorageException
import org.bson.Document
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The questions a mocked test cannot answer, asked of a **real** MongoDB started in-process.
 *
 * Everything else in this module pins persistence against Spring Data's own machinery — the mapping
 * context, `QueryMapper`, `PartTree` — which is the right substitute for "does this declaration mean
 * what I think" and no substitute at all for "does the database do it". These four do the latter: an
 * index that exists, a migration that rewrites a document, an upload that survives a round-trip through
 * GridFS and the filesystem, and a row written before a schema change that still reads back.
 *
 * Skipped rather than failed where `mongod` cannot start — see [EmbeddedMongoAvailable], which also
 * explains why that trade is not free.
 */
@ExtendWith(EmbeddedMongoAvailable::class)
@SpringBootTest(
    classes = [WebService::class],
    properties = [
        "de.griefed.serverpackcreator.spring.schedules.database.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.files.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.versions.refresh=-",
        EmbeddedMongoAvailable.VERSION_PROPERTY
    ]
)
internal class WebPersistenceIT {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var modPackService: ModPackService

    @Autowired
    private lateinit var modPackDownloadRepository: ModPackDownloadRepository

    @Autowired
    private lateinit var migrationRunner: RunConfigurationListMigrationRunner

    /** A minimal but valid modpack archive — `mods/` and `config/` at the root is what passes validation. */
    private fun modpackBytes(marker: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { out ->
            out.putNextEntry(ZipEntry("mods/somemod.jar")); out.write(marker.toByteArray()); out.closeEntry()
            out.putNextEntry(ZipEntry("config/someconfig.toml")); out.write("a = 1".toByteArray()); out.closeEntry()
        }
        return bytes.toByteArray()
    }

    @Test
    fun theSha256IndexIsActuallyCreated() {
        // DeclaredIndexCreator runs on ApplicationReadyEvent rather than during context refresh, because
        // creating indexes at refresh makes a reachable MongoDB a condition of starting up at all. That
        // trade is only worth anything if the index really does appear afterwards.
        val indexed = mongoTemplate.indexOps("modPack").indexInfo.flatMap { it.indexFields }.map { it.key }

        Assertions.assertTrue(
            indexed.contains("sha256"),
            "the duplicate-check's index is absent; the upload hash lookup is a collection scan. Found: $indexed"
        )
    }

    @Test
    fun anUploadSurvivesAFullRoundTripThroughGridFsAndTheFilesystem() {
        val upload = MockMultipartFile("file", "Round Trip.zip", "application/zip", modpackBytes("roundtrip"))

        val stored = modPackService.saveUploadedFile(upload)

        Assertions.assertNotNull(stored.id, "MongoDB assigned no id")
        Assertions.assertEquals("Round Trip.zip", stored.name)
        Assertions.assertEquals(
            modpackBytes("roundtrip").size.toLong(), stored.size,
            "size is not the archive's byte count"
        )
        // Written to both tiers: the filesystem copy is what load() prefers...
        val archive = modPackService.getModPackArchive(stored)
        Assertions.assertTrue(archive.isPresent, "the stored archive could not be read back")
        Assertions.assertEquals(modpackBytes("roundtrip").size.toLong(), archive.get().length())
        // ...and GridFS holds the second, which is what delete() now has to reclaim as well.
        Assertions.assertEquals(
            1L, mongoTemplate.getCollection("fs.files").countDocuments(),
            "the GridFS copy was not written"
        )

        modPackService.deleteModpack(stored.id!!)

        Assertions.assertEquals(
            0L, mongoTemplate.getCollection("fs.files").countDocuments(),
            "delete left the GridFS copy behind -- the leak this pass closed"
        )
    }

    @Test
    fun aDuplicateUploadIsRefusedAndCostsNothing() {
        val first = MockMultipartFile("file", "Original.zip", "application/zip", modpackBytes("dupe"))
        val again = MockMultipartFile("file", "Copy.zip", "application/zip", modpackBytes("dupe"))
        val stored = modPackService.saveUploadedFile(first)
        val gridFsAfterFirst = mongoTemplate.getCollection("fs.files").countDocuments()

        Assertions.assertThrows(StorageException::class.java) { modPackService.saveUploadedFile(again) }

        Assertions.assertEquals(
            gridFsAfterFirst, mongoTemplate.getCollection("fs.files").countDocuments(),
            "the refused duplicate still wrote a GridFS document, which nothing would ever reclaim"
        )
        modPackService.deleteModpack(stored.id!!)
    }

    @Test
    fun theRunConfigurationMigrationRewritesALegacyDocumentAndSkipsAMigratedOne() {
        // The legacy shape: DBRef arrays whose $id IS the value, which is why the rewrite needs no join.
        mongoTemplate.getCollection("runConfiguration").insertOne(
            Document(
                mapOf(
                    "_id" to "legacy",
                    "minecraftVersion" to "1.20.1",
                    "clientMods" to listOf(Document(mapOf("\$ref" to "clientMod", "\$id" to "OptiFine"))),
                    "startArgs" to listOf(Document(mapOf("\$ref" to "startArgument", "\$id" to "-Xmx4G"))),
                    "whitelistedMods" to listOf<Document>()
                )
            )
        )
        mongoTemplate.getCollection("runConfiguration").insertOne(
            Document(
                mapOf(
                    "_id" to "current",
                    "minecraftVersion" to "1.20.1",
                    "clientMods" to listOf("AlreadyAString"),
                    "startArgs" to listOf<String>(),
                    "whitelistedMods" to listOf<String>()
                )
            )
        )

        migrationRunner.migrate()

        val legacy = mongoTemplate.getCollection("runConfiguration").find(Document("_id", "legacy")).first()!!
        Assertions.assertEquals(listOf("OptiFine"), legacy["clientMods"], "the DBRef array was not flattened")
        Assertions.assertEquals(listOf("-Xmx4G"), legacy["startArgs"])
        val current = mongoTemplate.getCollection("runConfiguration").find(Document("_id", "current")).first()!!
        Assertions.assertEquals(
            listOf("AlreadyAString"), current["clientMods"],
            "an already-migrated document was rewritten again"
        )
    }

    @Test
    fun aDownloadRowWrittenBeforeTheIdChangeStillReadsBack() {
        // Before this pass the timestamp WAS the @MongoId, so an old row carries no downloadedAt field
        // at all. Reading one into the new shape is exactly the kind of thing no mocked test can answer.
        mongoTemplate.getCollection("modPackDownload").insertOne(
            Document(mapOf("_id" to Date(1_700_000_000_000L).toString()))
        )

        val rows = modPackDownloadRepository.findAll()

        Assertions.assertEquals(1, rows.size, "the legacy row did not come back")
        Assertions.assertInstanceOf(ModPackDownload::class.java, rows.single())
    }
}
