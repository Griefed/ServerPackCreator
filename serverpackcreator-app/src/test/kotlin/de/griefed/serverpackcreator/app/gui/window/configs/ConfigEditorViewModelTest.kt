package de.griefed.serverpackcreator.app.gui.window.configs

import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for [ConfigEditorViewModel], the display-independent state-logic extracted from the
 * Swing-coupled ConfigEditor in refactor Phase 2. Pins the unsaved-changes dirty-check that
 * drives the editor's warning-icon and the required-Java-version derivation.
 */
internal class ConfigEditorViewModelTest {
    private val versionMeta = mockk<VersionMeta>()
    private val viewModel = ConfigEditorViewModel(versionMeta)

    /**
     * Builds a fully-populated PackConfig, using the given [inclusions] instances so tests can
     * choose whether two configs share inclusion-references (equal) or not (unequal).
     */
    private fun packConfig(inclusions: ArrayList<InclusionSpecification>): PackConfig {
        val config = PackConfig()
        config.modpackDir = "/modpack"
        config.minecraftVersion = "1.20.1"
        config.modloader = "Forge"
        config.modloaderVersion = "47.2.0"
        config.javaArgs = "-Xmx4G"
        config.serverPackSuffix = "-server"
        config.serverIconPath = "/icon.png"
        config.serverPropertiesPath = "/server.properties"
        config.isServerIconInclusionDesired = true
        config.isServerPropertiesInclusionDesired = true
        config.isZipCreationDesired = true
        config.setClientMods(mutableListOf("SomeClientMod-"))
        config.setModsWhitelist(mutableListOf("SomeWhitelisted-"))
        config.setInclusions(inclusions)
        config.scriptSettings["KEY"] = "value"
        return config
    }

    /**
     * Pins that a never-saved configuration (no last-saved reference) is always considered to
     * have unsaved changes.
     */
    @Test
    fun neverSavedConfigurationHasUnsavedChanges() {
        Assertions.assertTrue(viewModel.hasUnsavedChanges(packConfig(arrayListOf()), null))
    }

    /**
     * Pins that two configurations with identical tracked-fields and shared inclusion-instances
     * are considered unchanged.
     */
    @Test
    fun identicalConfigurationsHaveNoUnsavedChanges() {
        val sharedInclusions = arrayListOf(InclusionSpecification("config"))
        Assertions.assertFalse(
            viewModel.hasUnsavedChanges(packConfig(sharedInclusions), packConfig(sharedInclusions))
        )
    }

    /**
     * Pins that inclusions are compared by value: two configurations with separate-but-equal
     * inclusion-instances are reported as unchanged. (Before InclusionSpecification gained
     * value-equality this over-reported, leaving the warning-icon on whenever inclusions were
     * present even right after a load.)
     */
    @Test
    fun inclusionsAreComparedByValue() {
        val current = packConfig(arrayListOf(InclusionSpecification("config")))
        val lastSaved = packConfig(arrayListOf(InclusionSpecification("config")))
        Assertions.assertFalse(viewModel.hasUnsavedChanges(current, lastSaved))
    }

    /**
     * Pins that a difference in any single tracked field is detected as an unsaved change.
     */
    @Test
    fun anyTrackedFieldDifferenceIsDetected() {
        val sharedInclusions = arrayListOf(InclusionSpecification("config"))
        val mutations: List<(PackConfig) -> Unit> = listOf(
            { it.modpackDir = "/other" },
            { it.minecraftVersion = "1.19.2" },
            { it.modloader = "NeoForge" },
            { it.modloaderVersion = "0.0.0" },
            { it.javaArgs = "-Xmx8G" },
            { it.serverPackSuffix = "-other" },
            { it.serverIconPath = "/other.png" },
            { it.serverPropertiesPath = "/other.properties" },
            { it.isServerIconInclusionDesired = false },
            { it.isServerPropertiesInclusionDesired = false },
            { it.isZipCreationDesired = false },
            { it.setClientMods(mutableListOf("DifferentMod-")) },
            { it.setModsWhitelist(mutableListOf("DifferentWhitelisted-")) },
            { it.setInclusions(arrayListOf(InclusionSpecification("mods"))) },
            { it.scriptSettings["KEY"] = "changed" }
        )
        for (mutate in mutations) {
            val changed = packConfig(sharedInclusions)
            mutate(changed)
            Assertions.assertTrue(
                viewModel.hasUnsavedChanges(changed, packConfig(sharedInclusions)),
                "Expected a change to be detected after mutation"
            )
        }
    }

    /**
     * Pins that fields outside the tracked set (e.g. the pack-name) do not count as unsaved
     * changes — the editor's dirty-check is intentionally scoped to generation-relevant fields.
     */
    @Test
    fun untrackedFieldDifferencesAreIgnored() {
        val sharedInclusions = arrayListOf(InclusionSpecification("config"))
        val changed = packConfig(sharedInclusions)
        changed.name = "A Different Name"
        Assertions.assertFalse(viewModel.hasUnsavedChanges(changed, packConfig(sharedInclusions)))
    }

    /**
     * Pins that the required-Java-version comes from the version-meta. The view-model delegates straight to
     * [de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta.requiredJavaVersion], so that is
     * what gets stubbed — stubbing `getServer` instead left the delegate unanswered and mockk failed.
     */
    @Test
    fun requiredJavaVersionComesFromServerMeta() {
        every { versionMeta.minecraft.requiredJavaVersion("1.20.1") } returns Optional.of("17")
        Assertions.assertEquals("17", viewModel.requiredJavaVersion("1.20.1"))
    }

    /**
     * Pins the fallback: a Minecraft version with no available server yields "?".
     */
    @Test
    fun requiredJavaVersionFallsBackToQuestionMark() {
        every { versionMeta.minecraft.requiredJavaVersion("0.0.0") } returns Optional.empty()
        Assertions.assertEquals("?", viewModel.requiredJavaVersion("0.0.0"))
    }
}
