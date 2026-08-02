package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI

/**
 * Tests for [UpdateConfig], the update- and release-info settings-group extracted from
 * ApiProperties in refactor Phase 1b. Pins the update-URL handling, the pre-release
 * version-check flag, old-version tracking for migrations, and the fallback-list update from
 * a remote (here: file://) properties-source.
 */
internal class UpdateConfigTest {

    /**
     * Pins that the update-URL defaults to the main-repository properties-file and round-trips
     * through the store.
     */
    @Test
    fun updateUrlDefaultsToMainRepositoryAndRoundTrips() {
        val store = PropertyStore()
        val updateConfig = UpdateConfig(store, GenerationConfig(store)) {}
        Assertions.assertEquals(UpdateConfig.FALLBACK_UPDATE_URL, updateConfig.updateUrl.toString())
        val customUrl = URI("https://example.com/custom.properties").toURL()
        updateConfig.updateUrl = customUrl
        Assertions.assertEquals(customUrl, updateConfig.updateUrl)
    }

    /**
     * Pins that the pre-release version-check flag defaults to disabled and round-trips.
     */
    @Test
    fun preReleaseCheckDefaultsToDisabledAndRoundTrips() {
        val store = PropertyStore()
        val updateConfig = UpdateConfig(store, GenerationConfig(store)) {}
        Assertions.assertFalse(updateConfig.isCheckingForPreReleasesEnabled)
        updateConfig.isCheckingForPreReleasesEnabled = true
        Assertions.assertTrue(updateConfig.isCheckingForPreReleasesEnabled)
    }

    /**
     * Pins old-version tracking: empty before any upgrade, stored value returned afterwards,
     * and every store triggers a save-to-disk.
     */
    @Test
    fun oldVersionIsTrackedAndTriggersSave() {
        val store = PropertyStore()
        var saves = 0
        val updateConfig = UpdateConfig(store, GenerationConfig(store)) { saves++ }
        Assertions.assertEquals("", updateConfig.oldVersion())
        updateConfig.setOldVersion("5.0.0")
        Assertions.assertEquals("5.0.0", updateConfig.oldVersion())
        Assertions.assertEquals(1, saves)
    }

    /**
     * Pins the fallback-list update: a changed remote list replaces the stored clientside-mods,
     * reports the update and saves to disk; an unchanged list reports no update.
     */
    @Test
    fun updateFallbackAppliesChangedRemoteListsAndSaves(@TempDir tempDir: File) {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        var saves = 0
        val updateConfig = UpdateConfig(store, generationConfig) { saves++ }

        val remoteProperties = File(tempDir, "remote.properties")
        remoteProperties.writeText(
            "${GenerationConfig.FALLBACK_MODS_LIST_KEY}=FreshClientMod-,AnotherFreshMod-\n" +
                    "${GenerationConfig.MODS_WHITELIST_KEY}=FreshWhitelisted-\n"
        )
        updateConfig.updateUrl = remoteProperties.toURI().toURL()

        Assertions.assertTrue(updateConfig.updateFallback(), "Changed remote lists must report an update")
        Assertions.assertTrue(updateConfig.fallbackUpdated)
        Assertions.assertTrue(generationConfig.clientsideMods.contains("FreshClientMod-"))
        Assertions.assertTrue(generationConfig.modsWhitelist.contains("FreshWhitelisted-"))
        Assertions.assertEquals(1, saves)

        Assertions.assertFalse(updateConfig.updateFallback(), "Unchanged remote lists must report no update")
        Assertions.assertEquals(1, saves)
    }
}
