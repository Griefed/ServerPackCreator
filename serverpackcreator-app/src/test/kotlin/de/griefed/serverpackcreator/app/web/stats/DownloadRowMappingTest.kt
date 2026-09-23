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
package de.griefed.serverpackcreator.app.web.stats

import de.griefed.serverpackcreator.app.web.modpack.ModPackDownload
import de.griefed.serverpackcreator.app.web.modpack.ModPackDownloadRepository
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackDownload
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackDownloadRepository
import de.griefed.serverpackcreator.app.web.stats.downloads.DownloadStatsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

/**
 * Guards how a recorded download is identified and ordered, asked of Spring Data's own mapping context
 * rather than of a database — the approach this module already uses for its index and collection-name
 * declarations.
 *
 * Two things can go wrong here and neither shows up as an error. An id that is not unique silently
 * overwrites rows, and a sort naming a field the document does not have is not rejected by MongoDB —
 * it simply does not order anything.
 */
class DownloadRowMappingTest {

    private val mappingContext = MongoMappingContext()

    private fun entity(type: Class<*>) = mappingContext.getRequiredPersistentEntity(type)

    @Test
    fun aRecordedDownloadIsNotIdentifiedByItsTimestamp() {
        // downloadedAt was the @MongoId. The id is global, not per-pack, so any two downloads of any
        // two packs in the same millisecond collided and save() overwrote the earlier row -- silent
        // loss in the history, invisible because the counters on the packs are kept separately.
        for (type in listOf(ModPackDownload::class.java, ServerPackDownload::class.java)) {
            val idProperty = entity(type).idProperty
            Assertions.assertNotNull(idProperty, "${type.simpleName} has no id property")
            Assertions.assertNotEquals(
                "downloadedAt", idProperty!!.name,
                "${type.simpleName} is identified by its timestamp, so same-millisecond downloads overwrite each other"
            )
        }
    }

    @Test
    fun theDownloadStatsSortNamesAFieldTheDocumentActuallyHas() {
        val modPackDownloadRepository: ModPackDownloadRepository = mockk()
        val serverPackDownloadRepository: ServerPackDownloadRepository = mockk()
        val sort = slot<Sort>()
        every { modPackDownloadRepository.findAll(capture(sort)) } returns emptyList()
        every { serverPackDownloadRepository.findAll(any<Sort>()) } returns emptyList()

        DownloadStatsService(
            modPackDownloadRepository, serverPackDownloadRepository, mockk(), mockk()
        ).modPackDownloads()

        val sortedOn = sort.captured.map { it.property }.toList()
        val persistent = entity(ModPackDownload::class.java).map { it.name }.toSet()
        Assertions.assertTrue(
            persistent.containsAll(sortedOn),
            "sorted on $sortedOn, but ModPackDownload only has $persistent -- MongoDB does not reject this, it just does not order"
        )
    }
}
