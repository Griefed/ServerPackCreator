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
package de.griefed.serverpackcreator.app.web.stats.packs

import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import de.griefed.serverpackcreator.app.web.serverpack.runconfiguration.RunConfigurationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for [AmountStatsService], which serves the public `/api/v2/stats` endpoint.
 *
 * The tallies are what the endpoint is for; the **cost** of producing them is what these pin. Every
 * `findAll()` here drags a whole collection into memory along with its eagerly-resolved `@DBRef`
 * graph — a `ServerPack` pulls its `RunConfiguration`, which pulls that config's start-arguments,
 * clientside mods (~550 on the default list) and whitelist. Doing it four times to answer one request
 * is four times that traffic, three of them only to learn a number the database can count itself.
 *
 * Repository call-shape is deliberately part of the contract here, unlike in `RunConfigurationServiceTest`
 * where lookup counts were left unpinned as an implementation detail: for this service the shape *is*
 * the defect being fixed.
 */
internal class AmountStatsServiceTest {

    private val serverPackRepository = mockk<ServerPackRepository>()
    private val modpackRepository = mockk<ModPackRepository>()
    private val runConfigurationRepository = mockk<RunConfigurationRepository>()
    private val amountStatsService =
        AmountStatsService(serverPackRepository, modpackRepository, runConfigurationRepository)

    /** A server pack carrying the given run-configuration, which is all the tallies read. */
    private fun serverPack(minecraftVersion: String, modloader: String, modloaderVersion: String): ServerPack {
        val runConfiguration = RunConfiguration(
            minecraftVersion, modloader, modloaderVersion, mutableListOf(), mutableListOf(), mutableListOf()
        )
        return ServerPack(0, runConfiguration, null, "pack", null, "modpack1")
    }

    /**
     * Pins that the tallies are correct **and** cost one collection scan plus three counts.
     *
     * `findAll()` was called on the server packs twice — once for the tally, once purely for `.size` —
     * and on the modpacks and run-configurations once each, again only for `.size`.
     */
    @Test
    fun statsAreTalliedFromOneScanAndThreeCounts() {
        val packs = listOf(
            serverPack("1.20.1", "Forge", "47.2.0"),
            serverPack("1.20.1", "Forge", "47.2.0"),
            serverPack("1.20.1", "NeoForge", "47.1.0"),
            serverPack("1.21.1", "Fabric", "0.16.0")
        )
        every { serverPackRepository.findAll() } returns packs
        every { serverPackRepository.count() } returns packs.size.toLong()
        every { modpackRepository.count() } returns 7L
        every { runConfigurationRepository.count() } returns 3L
        // Stubbed but expected unused: without these the guard would fail with a missing-answer
        // exception rather than naming the redundant scan it is actually guarding against.
        every { modpackRepository.findAll() } returns emptyList()
        every { runConfigurationRepository.findAll() } returns emptyList()

        val stats = amountStatsService.stats

        Assertions.assertEquals(7, stats.modPacks)
        Assertions.assertEquals(4, stats.serverPacks)
        Assertions.assertEquals(3, stats.runConfigurations)
        Assertions.assertEquals(mapOf("1.20.1" to 3, "1.21.1" to 1), stats.minecraftVersions)
        Assertions.assertEquals(mapOf("Forge" to 2, "NeoForge" to 1, "Fabric" to 1), stats.modloaders)
        Assertions.assertEquals(
            mapOf("Forge-47.2.0" to 2, "NeoForge-47.1.0" to 1, "Fabric-0.16.0" to 1),
            stats.modloaderVersions
        )

        verify(exactly = 1) { serverPackRepository.findAll() }
        verify(exactly = 1) { serverPackRepository.count() }
        verify(exactly = 1) { modpackRepository.count() }
        verify(exactly = 1) { runConfigurationRepository.count() }
        verify(exactly = 0) { modpackRepository.findAll() }
        verify(exactly = 0) { runConfigurationRepository.findAll() }
    }

    /**
     * Pins that an empty database yields empty tallies and zeroes rather than failing — the counts must
     * come from the repository, not from the (absent) scanned entries.
     */
    @Test
    fun anEmptyDatabaseYieldsEmptyStats() {
        every { serverPackRepository.findAll() } returns emptyList()
        every { serverPackRepository.count() } returns 0L
        every { modpackRepository.count() } returns 0L
        every { runConfigurationRepository.count() } returns 0L
        every { modpackRepository.findAll() } returns emptyList()
        every { runConfigurationRepository.findAll() } returns emptyList()

        val stats = amountStatsService.stats

        Assertions.assertEquals(0, stats.modPacks)
        Assertions.assertEquals(0, stats.serverPacks)
        Assertions.assertEquals(0, stats.runConfigurations)
        Assertions.assertTrue(stats.minecraftVersions.isEmpty())
        Assertions.assertTrue(stats.modloaders.isEmpty())
        Assertions.assertTrue(stats.modloaderVersions.isEmpty())
    }

    /** A modpack, only needed to prove the modpack count is not derived from a scan. */
    @Suppress("unused")
    private fun modPack() = ModPack()
}
