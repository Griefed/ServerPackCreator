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
import de.griefed.serverpackcreator.app.web.serverpack.customizing.ClientMod
import de.griefed.serverpackcreator.app.web.serverpack.customizing.ClientModRepository
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import de.griefed.serverpackcreator.app.web.serverpack.customizing.StartArgument
import de.griefed.serverpackcreator.app.web.serverpack.customizing.StartArgumentRepository
import de.griefed.serverpackcreator.app.web.serverpack.customizing.WhitelistedMod
import de.griefed.serverpackcreator.app.web.serverpack.customizing.WhitelistedModRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

/**
 * Characterization tests for [RunConfigurationService.createRunConfig]'s three
 * look-up-or-store loops, which had no coverage: the controller test mocks this service away.
 *
 * What is pinned is the *outcome* — which entries the built configuration ends up holding, and which
 * of them reach `save` — deliberately **not** how many times each repository is queried. The number
 * of lookups is an implementation detail; pinning it would make any future change of that shape red
 * for no reason.
 */
internal class RunConfigurationServiceTest {

    private val runConfigurationRepository: RunConfigurationRepository = mockk()
    private val apiProperties: ApiProperties = mockk()
    private val clientModRepository: ClientModRepository = mockk()
    private val whitelistedModRepository: WhitelistedModRepository = mockk()
    private val startArgumentRepository: StartArgumentRepository = mockk()

    private val service = RunConfigurationService(
        runConfigurationRepository,
        apiProperties,
        clientModRepository,
        whitelistedModRepository,
        startArgumentRepository
    )

    /**
     * Every repository answers "not known" and echoes back whatever it is asked to save, so a test
     * only has to override the one lookup it is actually about. The final `save(config)` is stubbed
     * to return the configuration unchanged, which is what makes the built object observable.
     */
    @BeforeEach
    fun defaultToAnEmptyRepository() {
        // The fall-back sources for a blank input. Individual tests override what they are about.
        every { apiProperties.aikarsFlags } returns "-Xdefault"
        every { apiProperties.clientSideMods() } returns mutableListOf()
        every { apiProperties.whitelistedMods() } returns mutableListOf()
        every { startArgumentRepository.findByArgument(any()) } returns Optional.empty()
        every { clientModRepository.findByMod(any()) } returns Optional.empty()
        every { whitelistedModRepository.findByMod(any()) } returns Optional.empty()
        every { startArgumentRepository.save(any()) } answers { firstArg() }
        every { clientModRepository.save(any()) } answers { firstArg() }
        every { whitelistedModRepository.save(any()) } answers { firstArg() }
        every {
            runConfigurationRepository
                .findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsInAndClientModsInAndWhitelistedModsIn(
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

        Assertions.assertEquals(listOf("-Xmx4G", "-Xms4G"), config.startArgs.map { it.argument })
    }

    /** A start argument the repository already knows comes back as the stored entry, not a new one. */
    @Test
    fun aKnownStartArgumentIsReplacedByTheStoredEntry() {
        val stored = StartArgument("-Xmx4G")
        every { startArgumentRepository.findByArgument("-Xmx4G") } returns Optional.of(stored)

        val config = createWith("-Xmx4G", "", "")

        Assertions.assertSame(stored, config.startArgs.single())
        verify(exactly = 0) { startArgumentRepository.save(any()) }
    }

    /** An unknown start argument is saved, and the saved entry is the one kept. */
    @Test
    fun anUnknownStartArgumentIsSaved() {
        val config = createWith("-Xmx4G", "", "")

        Assertions.assertEquals("-Xmx4G", config.startArgs.single().argument)
        verify(exactly = 1) { startArgumentRepository.save(any()) }
    }

    /** Client mods are comma-separated, and a known one is reused rather than re-saved. */
    @Test
    fun aKnownClientModIsReplacedByTheStoredEntry() {
        val stored = ClientMod("optifine")
        every { clientModRepository.findByMod("optifine") } returns Optional.of(stored)

        val config = createWith("", "optifine, journeymap", "")

        Assertions.assertEquals(listOf("optifine", "journeymap"), config.clientMods.map { it.mod })
        Assertions.assertSame(stored, config.clientMods.first())
        verify(exactly = 1) { clientModRepository.save(any()) }
    }

    /** Whitelisted mods follow the same look-up-or-store rule as client mods. */
    @Test
    fun aKnownWhitelistedModIsReplacedByTheStoredEntry() {
        val stored = WhitelistedMod("jei")
        every { whitelistedModRepository.findByMod("jei") } returns Optional.of(stored)

        val config = createWith("", "", "jei,journeymap")

        Assertions.assertEquals(listOf("jei", "journeymap"), config.whitelistedMods.map { it.mod })
        Assertions.assertSame(stored, config.whitelistedMods.first())
        verify(exactly = 1) { whitelistedModRepository.save(any()) }
    }

    /** Blank start arguments fall back to the configured Aikar's flags rather than staying empty. */
    @Test
    fun blankStartArgumentsFallBackToAikarsFlags() {
        every { apiProperties.aikarsFlags } returns "-Xmx4G -Xms4G"

        val config = createWith("", "", "")

        Assertions.assertEquals(listOf("-Xmx4G", "-Xms4G"), config.startArgs.map { it.argument })
    }

    /** Blank mod-lists fall back to the configured defaults, for both lists. */
    @Test
    fun blankModListsFallBackToTheConfiguredDefaults() {
        every { apiProperties.clientSideMods() } returns mutableListOf("optifine")
        every { apiProperties.whitelistedMods() } returns mutableListOf("jei")

        val config = createWith("-Xmx4G", "", "")

        Assertions.assertEquals(listOf("optifine"), config.clientMods.map { it.mod })
        Assertions.assertEquals(listOf("jei"), config.whitelistedMods.map { it.mod })
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
                .findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsInAndClientModsInAndWhitelistedModsIn(
                    any(), any(), any(), any(), any(), any()
                )
        } returns Optional.of(existing)

        val config = createWith("-Xmx4G", "optifine", "jei")

        Assertions.assertSame(existing, config)
        verify(exactly = 0) { runConfigurationRepository.save(any()) }
    }
}
