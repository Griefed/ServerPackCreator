package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for [GenerationConfig], the server pack-generation settings-group extracted from
 * ApiProperties in refactor Phase 1b. Pins list-merging with fallbacks, the
 * include-wins-over-exclude rule, exclusion-filter parsing, the legacy auto-discovery
 * migration and the regex-variants of the mod-lists.
 */
internal class GenerationConfigTest {

    /**
     * Pins that included-directories merge store-entries with the fallback-defaults.
     */
    @Test
    fun directoriesToIncludeMergeStoreEntriesWithDefaults() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        store.define(GenerationConfig.DIRECTORIES_MUST_INCLUDE_KEY, "config,mods,verycustomdir")
        val included = generationConfig.directoriesToInclude
        Assertions.assertTrue(included.contains("verycustomdir"))
        Assertions.assertTrue(included.contains("config"))
        Assertions.assertTrue(included.contains("mods"))
    }

    /**
     * Pins that excluded-directories never contain directories which must be included.
     */
    @Test
    fun directoriesToExcludeOmitIncludedDirectories() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        store.define(GenerationConfig.DIRECTORIES_SHOULD_EXCLUDE_KEY, "overrides,config")
        val excluded = generationConfig.directoriesToExclude
        Assertions.assertTrue(excluded.contains("overrides"))
        Assertions.assertFalse(excluded.contains("config"), "Included directories must win over exclusions")
    }

    /**
     * Pins that setting included-directories writes the comma-joined list to the store.
     */
    @Test
    fun directoriesToIncludeSetterWritesStore() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        generationConfig.directoriesToInclude = TreeSet(listOf("alpha", "beta"))
        Assertions.assertEquals(
            "alpha,beta",
            store.properties.getProperty(GenerationConfig.DIRECTORIES_MUST_INCLUDE_KEY)
        )
    }

    /**
     * Pins that pre- and post-install cleanup-files merge store-entries with the defaults.
     */
    @Test
    fun cleanupFilesMergeStoreEntriesWithDefaults() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        store.define(GenerationConfig.POST_INSTALL_CLEANUP_KEY, "my-leftover.jar")
        store.define(GenerationConfig.PRE_INSTALL_CLEANUP_KEY, "my-old-stuff.jar")
        Assertions.assertTrue(generationConfig.postInstallCleanupFiles.contains("my-leftover.jar"))
        Assertions.assertTrue(generationConfig.postInstallCleanupFiles.contains("forge-installer.jar"))
        Assertions.assertTrue(generationConfig.preInstallCleanupFiles.contains("my-old-stuff.jar"))
        Assertions.assertTrue(generationConfig.preInstallCleanupFiles.contains("libraries"))
    }

    /**
     * Pins that zip-archive-exclusions merge store-entries with the defaults and write back on
     * assignment.
     */
    @Test
    fun zipArchiveExclusionsMergeAndWriteBack() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        store.define(GenerationConfig.ZIP_EXCLUSIONS_KEY, "do-not-zip-me.txt")
        Assertions.assertTrue(generationConfig.zipArchiveExclusions.contains("do-not-zip-me.txt"))
        generationConfig.zipArchiveExclusions = TreeSet(listOf("only-me.txt"))
        Assertions.assertEquals(
            "only-me.txt",
            store.properties.getProperty(GenerationConfig.ZIP_EXCLUSIONS_KEY)
        )
    }

    /**
     * Pins exclusion-filter parsing for every valid value and the fallback to START for garbage.
     */
    @Test
    fun exclusionFilterParsesValidValuesAndDefaultsToStart() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        for (filter in ExclusionFilter.entries) {
            store.define(GenerationConfig.AUTO_DISCOVERY_FILTER_KEY, filter.name)
            Assertions.assertEquals(filter, generationConfig.exclusionFilter)
        }
        store.define(GenerationConfig.AUTO_DISCOVERY_FILTER_KEY, "NONSENSE")
        Assertions.assertEquals(ExclusionFilter.START, generationConfig.exclusionFilter)
    }

    /**
     * Pins the migration of the legacy auto-discovery property to its current key: the legacy
     * value wins and the legacy key is removed.
     */
    @Test
    fun legacyAutoDiscoveryPropertyIsMigrated() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        store.define(GenerationConfig.AUTO_DISCOVERY_ENABLED_LEGACY_KEY, "false")
        Assertions.assertFalse(generationConfig.isAutoExcludingModsEnabled)
        Assertions.assertNull(
            store.properties.getProperty(GenerationConfig.AUTO_DISCOVERY_ENABLED_LEGACY_KEY),
            "Legacy key must be removed after migration"
        )
        Assertions.assertEquals(
            "false",
            store.properties.getProperty(GenerationConfig.AUTO_DISCOVERY_ENABLED_KEY)
        )
    }

    /**
     * Pins that the generation-flags round-trip through the store.
     */
    @Test
    fun generationFlagsRoundTripThroughStore() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        generationConfig.isZipFileExclusionEnabled = false
        generationConfig.isServerPacksOverwriteEnabled = false
        generationConfig.isServerPackCleanupEnabled = false
        generationConfig.isMinecraftPreReleasesAvailabilityEnabled = true
        generationConfig.isUpdatingServerPacksEnabled = true
        Assertions.assertFalse(generationConfig.isZipFileExclusionEnabled)
        Assertions.assertFalse(generationConfig.isServerPacksOverwriteEnabled)
        Assertions.assertFalse(generationConfig.isServerPackCleanupEnabled)
        Assertions.assertTrue(generationConfig.isMinecraftPreReleasesAvailabilityEnabled)
        Assertions.assertTrue(generationConfig.isUpdatingServerPacksEnabled)
    }

    /**
     * Pins that Aikar's flags fall back to the well-known default and can be overridden.
     */
    @Test
    fun aikarsFlagsDefaultAndOverride() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        Assertions.assertTrue(generationConfig.aikarsFlags.contains("-XX:+UseG1GC"))
        generationConfig.aikarsFlags = "-Xms8G -Xmx8G"
        Assertions.assertEquals("-Xms8G -Xmx8G", generationConfig.aikarsFlags)
    }

    /**
     * Pins that the fallback mod-lists are merged from the store and that the regex-variants
     * wrap every entry in start-and-anything regex-markers.
     */
    @Test
    fun modListsMergeFromStoreAndDeriveRegexVariants() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        store.define(GenerationConfig.FALLBACK_MODS_LIST_KEY, "MyClientMod-,AnotherClientMod-")
        store.define(GenerationConfig.MODS_WHITELIST_KEY, "MyServersideMod-")
        generationConfig.loadFallbackModsList()
        generationConfig.loadFallbackWhitelist()
        Assertions.assertTrue(generationConfig.clientsideMods.contains("MyClientMod-"))
        Assertions.assertTrue(generationConfig.modsWhitelist.contains("MyServersideMod-"))
        Assertions.assertTrue(generationConfig.clientsideModsRegex.contains("^MyClientMod-.*$"))
        Assertions.assertTrue(generationConfig.modsWhitelistRegex.contains("^MyServersideMod-.*$"))
    }

    /**
     * Pins that clientSideMods() and whitelistedMods() return the regex-variants when the
     * exclusion-filter is REGEX, and the plain lists otherwise.
     */
    @Test
    fun modListAccessorsHonorRegexFilter() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        store.define(GenerationConfig.AUTO_DISCOVERY_FILTER_KEY, "START")
        Assertions.assertTrue(generationConfig.clientSideMods().none { mod -> mod.startsWith("^") })
        store.define(GenerationConfig.AUTO_DISCOVERY_FILTER_KEY, "REGEX")
        Assertions.assertTrue(generationConfig.clientSideMods().all { mod -> mod.startsWith("^") })
        Assertions.assertTrue(generationConfig.whitelistedMods().all { mod -> mod.startsWith("^") })
    }
}
