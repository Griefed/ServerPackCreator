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
package de.griefed.serverpackcreator.app.web.modpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*

/**
 * Guards how a modpack upload checks for duplicates.
 *
 * `existingUploadOf` runs on every upload. It used to load **every** modpack and compare in memory —
 * and because `ModPack.serverPacks` is an eager `@DBRef` whose targets eagerly resolve their own
 * run-configuration and that config's three `@DBRef` lists, one upload read a four-collection graph to
 * compare a single hash. `sha256` is indexed, so one document lookup answers it.
 */
internal class ModPackDuplicateCheckTest {

    private val modpackRepository = mockk<ModPackRepository>()

    /** A stored modpack carrying [sha256]. */
    private fun stored(sha256: String, name: String) = ModPack().apply {
        this.sha256 = sha256
        this.name = name
    }

    /**
     * A service with everything but the modpack repository mocked. Constructing it is cheap: the only
     * work in its initializer is resolving the modpacks directory and building a StorageSystem, which
     * just wires service objects together.
     */
    private fun service(tempDir: File): ModPackService {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.modpacksDirectory } returns tempDir
        return ModPackService(
            modpackRepository,
            mockk<ConfigurationHandler>(relaxed = true),
            mockk<ModPackDownloadRepository>(relaxed = true),
            mockk<ServerPackRepository>(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            apiProperties
        )
    }

    /**
     * Pins that an existing hash is found by an indexed lookup, not by loading the collection.
     */
    @Test
    fun anExistingHashIsFoundWithoutScanningTheCollection(@TempDir tempDir: File) {
        val existing = stored("abc123", "Already Uploaded")
        every { modpackRepository.findFirstBySha256("abc123") } returns Optional.of(existing)
        // Stubbed but expected unused, so the guard fails by naming the scan rather than by a
        // missing-answer exception.
        every { modpackRepository.findAll() } returns listOf(existing)

        val duplicate = service(tempDir).existingUploadOf("abc123")

        Assertions.assertTrue(duplicate.isPresent, "The stored modpack with this hash must be found")
        Assertions.assertEquals("Already Uploaded", duplicate.get().name)
        verify(exactly = 1) { modpackRepository.findFirstBySha256("abc123") }
        verify(exactly = 0) { modpackRepository.findAll() }
    }

    /**
     * Pins that an unknown hash reports no duplicate, still without a scan.
     */
    @Test
    fun anUnknownHashReportsNoDuplicate(@TempDir tempDir: File) {
        every { modpackRepository.findFirstBySha256("nothing-like-it") } returns Optional.empty()
        every { modpackRepository.findAll() } returns listOf(stored("abc123", "Other"))

        val duplicate = service(tempDir).existingUploadOf("nothing-like-it")

        Assertions.assertTrue(duplicate.isEmpty, "An unknown hash is not a duplicate")
        verify(exactly = 0) { modpackRepository.findAll() }
    }

    /**
     * Pins that a null hash — an upload whose hash could not be computed — reports no duplicate rather
     * than matching every stored modpack that also has none.
     */
    @Test
    fun aNullHashReportsNoDuplicate(@TempDir tempDir: File) {
        every { modpackRepository.findFirstBySha256(null) } returns Optional.empty()
        every { modpackRepository.findAll() } returns listOf(ModPack())

        Assertions.assertTrue(service(tempDir).existingUploadOf(null).isEmpty)
        // The assertion above passes even without the short-circuit, because the stub answers empty() for
        // null too — it would be verifying the mock, not the code. What the short-circuit exists for is
        // that the database is never asked: Mongo's own `{sha256: null}` *would* match documents whose
        // field is unset.
        verify(exactly = 0) { modpackRepository.findFirstBySha256(any()) }
    }
}
