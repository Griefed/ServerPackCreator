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
package de.griefed.serverpackcreator.app.web.serverpack.runconfiguration

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

/**
 * Tests for [RunConfigurationService.createRunConfig], which the controller test mocks away.
 *
 * What is pinned is the *outcome* — which entries the built configuration ends up holding. The three
 * look-up-or-store loops these tests were originally written for no longer exist: the lists are plain
 * strings embedded in the document, so there is nothing to resolve and nothing to store separately.
 * The tests that described that resolution were removed with it; see the commit that flattened them.
 *
 * One count *is* pinned now — [buildingAConfigurationCostsTwoRepositoryCalls] — because "one query per
 * mod" is exactly the defect the flattening removed, and a silent return to it is the regression worth
 * catching.
 */
internal class RunConfigurationServiceTest {

    private val runConfigurationRepository: RunConfigurationRepository = mockk()
    private val apiProperties: ApiProperties = mockk()

    private val service = RunConfigurationService(runConfigurationRepository, apiProperties)

    /**
     * The fall-back sources for blank input, plus a repository that knows no existing configuration and
     * echoes back whatever it is asked to save — which is what makes the built object observable.
     */
    @BeforeEach
    fun defaultToAnEmptyRepository() {
        // The fall-back sources for a blank input. Individual tests override what they are about.
        every { apiProperties.aikarsFlags } returns "-Xdefault"
        every { apiProperties.clientSideMods() } returns mutableListOf()
        every { apiProperties.whitelistedMods() } returns mutableListOf()
        every {
            runConfigurationRepository
                .findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsAndClientModsAndWhitelistedMods(
                    any(), any(), any(), any(), any(), any()
                )
        } returns Optional.empty()
        every { runConfigurationRepository.save(any()) } answers { firstArg() }
    }

    private fun createWith(startArgs: String, clientMods: String, whitelistedMods: String) =
        service.createRunConfig("1.20.1", "Forge", "47.3.0", startArgs, clientMods, whitelistedMods)

    /** Whitespace-separated start arguments each become their own entry. */
    @Test
    fun startArgumentsAreSplitOnWhitespace() {
        val config = createWith("-Xmx4G  -Xms4G", "", "")

        Assertions.assertEquals(listOf("-Xmx4G", "-Xms4G"), config.startArgs)
    }

    /** Client mods are comma-separated, with surrounding spaces tolerated. */
    @Test
    fun clientModsAreSplitOnCommas() {
        val config = createWith("", "optifine, journeymap", "")

        Assertions.assertEquals(listOf("optifine", "journeymap"), config.clientMods)
    }

    /** Whitelisted mods are comma-separated too. */
    @Test
    fun whitelistedModsAreSplitOnCommas() {
        val config = createWith("", "", "jei,journeymap")

        Assertions.assertEquals(listOf("jei", "journeymap"), config.whitelistedMods)
    }

    /**
     * Pins that building a configuration costs **two** repository calls regardless of list size: the
     * duplicate lookup and the save.
     *
     * Each list used to be resolved entry by entry — one `findBy` per entry plus a `save` per miss — so
     * with the default clientside list this was ~550 sequential round-trips to create one configuration.
     */
    @Test
    fun buildingAConfigurationCostsTwoRepositoryCalls() {
        every { apiProperties.clientSideMods() } returns (1..550).map { "mod-$it" }.toMutableList()

        val config = createWith("-Xmx4G", "", "")

        Assertions.assertEquals(550, config.clientMods.size)
        verify(exactly = 1) {
            runConfigurationRepository
                .findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsAndClientModsAndWhitelistedMods(
                    any(), any(), any(), any(), any(), any()
                )
        }
        verify(exactly = 1) { runConfigurationRepository.save(any()) }
    }

    /** Blank start arguments fall back to the configured Aikar's flags rather than staying empty. */
    @Test
    fun blankStartArgumentsFallBackToAikarsFlags() {
        every { apiProperties.aikarsFlags } returns "-Xmx4G -Xms4G"

        val config = createWith("", "", "")

        Assertions.assertEquals(listOf("-Xmx4G", "-Xms4G"), config.startArgs)
    }

    /** Blank mod-lists fall back to the configured defaults, for both lists. */
    @Test
    fun blankModListsFallBackToTheConfiguredDefaults() {
        every { apiProperties.clientSideMods() } returns mutableListOf("optifine")
        every { apiProperties.whitelistedMods() } returns mutableListOf("jei")

        val config = createWith("-Xmx4G", "", "")

        Assertions.assertEquals(listOf("optifine"), config.clientMods)
        Assertions.assertEquals(listOf("jei"), config.whitelistedMods)
    }

    /** An existing run-configuration is reused instead of a duplicate being stored. */
    @Test
    fun anExistingRunConfigurationIsReturnedInsteadOfSavingADuplicate() {
        val existing = RunConfiguration(
            minecraftVersion = "1.20.1",
            modloader = "Forge",
            modloaderVersion = "47.3.0",
            startArgs = mutableListOf(),
            clientMods = mutableListOf(),
            whitelistedMods = mutableListOf()
        )
        every {
            runConfigurationRepository
                .findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsAndClientModsAndWhitelistedMods(
                    any(), any(), any(), any(), any(), any()
                )
        } returns Optional.of(existing)

        val config = createWith("-Xmx4G", "optifine", "jei")

        Assertions.assertSame(existing, config)
        verify(exactly = 0) { runConfigurationRepository.save(any()) }
    }
}
