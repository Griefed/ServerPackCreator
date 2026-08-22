package de.griefed.serverpackcreator.app.gui.window.configs

import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.serverpack.ServerPackHandler
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*

/**
 * Tests for [ConfigEditorViewModel], the display-independent state-logic extracted from the
 * Swing-coupled ConfigEditor in refactor Phase 2. Pins the unsaved-changes dirty-check that
 * drives the editor's warning-icon and the required-Java-version derivation.
 */
internal class ConfigEditorViewModelTest {
    private val versionMeta = mockk<VersionMeta>()
    private val configurationHandler = mockk<ConfigurationHandler>()
    private val serverPackHandler = mockk<ServerPackHandler>()
    private val viewModel = ConfigEditorViewModel(versionMeta, configurationHandler, serverPackHandler)

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

    /**
     * Pins that a version-triple is probed **once**, however often it is asked about.
     *
     * `serverDownloadable` performs an HTTP request against the modloader's maven — and the only
     * caller is the editor's 500 ms debounce, which fires after every pause in typing, in *any* field,
     * for *every* open tab. So editing the suffix used to send a request for the Forge installer URL
     * every time the user stopped typing for half a second. The answer depends on nothing but the
     * three versions, so asking twice is waste by construction.
     */
    @Test
    fun aVersionTripleIsProbedOnlyOnce() {
        every { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.2.0") } returns true
        repeat(20) {
            Assertions.assertTrue(viewModel.isServerDownloadable("1.20.1", "Forge", "47.2.0"))
        }
        verify(exactly = 1) { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.2.0") }
    }

    /**
     * Pins that each distinct triple is still probed — the memo must key on all three versions, not
     * collapse to "asked once, answer forever".
     */
    @Test
    fun eachDistinctVersionTripleIsProbedOnItsOwn() {
        every { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.2.0") } returns true
        every { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.3.0") } returns false
        every { serverPackHandler.serverDownloadable("1.21.1", "NeoForge", "21.1.0") } returns true
        Assertions.assertTrue(viewModel.isServerDownloadable("1.20.1", "Forge", "47.2.0"))
        Assertions.assertFalse(viewModel.isServerDownloadable("1.20.1", "Forge", "47.3.0"))
        Assertions.assertTrue(viewModel.isServerDownloadable("1.21.1", "NeoForge", "21.1.0"))
        verify(exactly = 1) { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.2.0") }
        verify(exactly = 1) { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.3.0") }
        verify(exactly = 1) { serverPackHandler.serverDownloadable("1.21.1", "NeoForge", "21.1.0") }
    }

    /**
     * Pins that a **failed** probe is retried rather than remembered.
     *
     * Deliberately asymmetric with the success case: a published installer does not vanish, so a
     * `true` is safe to keep forever, but a `false` may only mean the network was down for a moment.
     * Caching that would leave the editor showing "server unavailable" until the app is restarted.
     */
    @Test
    fun aFailedProbeIsRetried() {
        every { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.2.0") } returns false
        Assertions.assertFalse(viewModel.isServerDownloadable("1.20.1", "Forge", "47.2.0"))
        every { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.2.0") } returns true
        Assertions.assertTrue(
            viewModel.isServerDownloadable("1.20.1", "Forge", "47.2.0"),
            "A transient failure must not be remembered — the editor would stay stuck on 'unavailable'"
        )
        // The assertion above is not enough on its own, and that is the point of this line: if failures
        // were cached, the second call would short-circuit to `return true` and satisfy it *without*
        // probing. Only the call count distinguishes "re-probed" from "wrongly remembered".
        verify(exactly = 2) { serverPackHandler.serverDownloadable("1.20.1", "Forge", "47.2.0") }
    }

    /**
     * Pins that an unchanged modpack directory is read **once**.
     *
     * `checkManifests` parses the launcher manifest into a Jackson tree, and a real CurseForge
     * `minecraftinstance.json` is multi-megabyte — the copy in this repo's own
     * `misc/launcher-manifests/curseforge/` is 2.7 MB. Re-parsing that on every debounce tick, per
     * tab, is the single largest allocation on the editing path.
     */
    @Test
    fun anUnchangedModpackDirectoryIsReadOnce(@TempDir tempDir: File) {
        val manifest = File(tempDir, "manifest.json")
        manifest.writeText("""{"name":"My Pack"}""")
        every { configurationHandler.checkManifests(tempDir.absolutePath, any(), any()) } answers {
            secondArg<PackConfig>().name = "My Pack"
            "My Pack"
        }
        every { configurationHandler.manifestCandidates(tempDir.absolutePath) } returns listOf(manifest)
        repeat(20) {
            Assertions.assertEquals("My Pack", viewModel.packName(tempDir.absolutePath))
        }
        verify(exactly = 1) { configurationHandler.checkManifests(tempDir.absolutePath, any(), any()) }
    }

    /**
     * Pins that touching the manifest re-reads it. The memo is an optimisation, not a one-shot: a
     * user who edits their modpack while the editor is open must see the new name.
     */
    @Test
    fun aChangedManifestIsReadAgain(@TempDir tempDir: File) {
        val manifest = File(tempDir, "manifest.json")
        manifest.writeText("""{"name":"Before"}""")
        every { configurationHandler.manifestCandidates(tempDir.absolutePath) } returns listOf(manifest)
        every { configurationHandler.checkManifests(tempDir.absolutePath, any(), any()) } answers {
            secondArg<PackConfig>().name = manifest.readText().substringAfter(":\"").substringBefore("\"")
            null
        }
        Assertions.assertEquals("Before", viewModel.packName(tempDir.absolutePath))

        manifest.writeText("""{"name":"After"}""")
        manifest.setLastModified(manifest.lastModified() + 5_000)

        Assertions.assertEquals(
            "After",
            viewModel.packName(tempDir.absolutePath),
            "A manifest edited under a running editor must be re-read"
        )
    }

    /**
     * Pins the fallback when no manifest declares anything: the directory's own name, and still only
     * one read per unchanged state.
     */
    @Test
    fun aDirectoryWithoutManifestsFallsBackToItsName(@TempDir tempDir: File) {
        every { configurationHandler.manifestCandidates(tempDir.absolutePath) } returns emptyList()
        every { configurationHandler.checkManifests(tempDir.absolutePath, any(), any()) } returns null
        repeat(5) {
            Assertions.assertEquals(tempDir.name, viewModel.packName(tempDir.absolutePath))
        }
        verify(exactly = 1) { configurationHandler.checkManifests(tempDir.absolutePath, any(), any()) }
    }
}
