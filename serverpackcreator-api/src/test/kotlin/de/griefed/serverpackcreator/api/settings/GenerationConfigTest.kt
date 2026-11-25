package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.util.*

/**
 * Tests for [GenerationConfig], the server pack-generation settings-group. Pins list-merging with fallbacks,
 * the include-wins-over-exclude rule, exclusion-filter parsing, the legacy auto-discovery migration and the
 * regex-variants of the mod-lists.
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
     * Pins the paths an update must leave alone: they default to what a running Minecraft server
     * writes into the directory it was started from, and a user's own entries merge with — rather
     * than replace — that default, so protection can only ever be widened.
     */
    @Test
    fun protectedUpdatePathsDefaultToAServersOwnFilesAndCanOnlyBeWidened() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)

        Assertions.assertTrue(generationConfig.updateProtectedPaths.contains("world"), "A world is protected")
        Assertions.assertTrue(generationConfig.updateProtectedPaths.contains("ops.json"), "so is the ops-list")
        Assertions.assertTrue(
            generationConfig.updateProtectedPaths.contains("server.properties"),
            "and so are the two files an operator tunes by hand"
        )
        Assertions.assertTrue(generationConfig.updateProtectedPaths.contains("variables.txt"))

        generationConfig.updateProtectedPaths = TreeSet(listOf("plugins"))
        Assertions.assertTrue(generationConfig.updateProtectedPaths.contains("plugins"), "A user entry is kept")
        Assertions.assertTrue(
            generationConfig.updateProtectedPaths.contains("world"),
            "and the shipped defaults merge back in, so a world can not be un-protected by accident"
        )
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

    /**
     * Pins that replacing a fallback list publishes a NEW set rather than emptying the live one.
     *
     * The replacement is reachable while the list is being read: the Edit menu's *Update Default Modslist*
     * calls `updateFallback()` on the event dispatch thread, while a generation or a config-check tick reads
     * the same list from a coroutine. `TreeSet` is not thread-safe, so clearing and refilling the live set
     * would not merely risk a stale read — a structural modification during another thread's traversal is
     * undefined.
     *
     * Identity is what is asserted, because it is the property that makes the window impossible: a
     * reader holding the old set keeps a complete old set, and one reading the field afterwards gets a
     * complete new one. There is no moment at which either is partial.
     */
    @Test
    fun replacingAFallbackListPublishesANewSetInsteadOfEmptyingTheLiveOne() {
        val generationConfig = GenerationConfig(PropertyStore())

        val heldClientside = generationConfig.clientsideMods
        Assertions.assertTrue(heldClientside.isNotEmpty(), "precondition: the fallback list is not empty")
        generationConfig.replaceClientsideMods(listOf("AmbientSounds", "BetterPing"))

        Assertions.assertNotSame(
            heldClientside, generationConfig.clientsideMods,
            "a replace must publish a new set, not rewrite the one readers are holding"
        )
        Assertions.assertTrue(
            heldClientside.isNotEmpty(),
            "the set a reader was already holding must not have been emptied underneath it"
        )
        Assertions.assertEquals(
            sortedSetOf("AmbientSounds", "BetterPing"), generationConfig.clientsideMods,
            "and the field must now hold exactly what was published"
        )

        val heldWhitelist = generationConfig.modsWhitelist
        generationConfig.replaceModsWhitelist(listOf("Ping"))
        Assertions.assertNotSame(heldWhitelist, generationConfig.modsWhitelist)
        Assertions.assertEquals(sortedSetOf("Ping"), generationConfig.modsWhitelist)
    }

    /**
     * Pins that each read of the regex-variant lists yields its **own** set.
     *
     * A getter refilling one shared set would empty and rewrite a result its caller still holds, and,
     * the refill not being atomic, let a concurrent reader observe the set mid-clear. Both
     * `ApiProperties.clientsideModsRegex` and `modsWhitelistRegex` are published, and the GUI reads settings
     * from a `parallelStream` walk.
     */
    @Test
    fun regexVariantListsAreNotSharedBetweenReads() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)

        val firstClientside = generationConfig.clientsideModsRegex
        val secondClientside = generationConfig.clientsideModsRegex
        Assertions.assertNotSame(firstClientside, secondClientside, "Each read must yield its own set")
        Assertions.assertEquals(firstClientside, secondClientside, "...holding equal contents")
        Assertions.assertTrue(firstClientside.isNotEmpty(), "precondition: the fallback list is not empty")

        val firstWhitelist = generationConfig.modsWhitelistRegex
        Assertions.assertNotSame(firstWhitelist, generationConfig.modsWhitelistRegex)

        // A caller mutating what it was handed must not disturb the next reader.
        firstClientside.clear()
        Assertions.assertTrue(
            generationConfig.clientsideModsRegex.isNotEmpty(),
            "A caller emptying its own copy must not empty the source"
        )
    }

    /**
     * Pins that the five directory and file lists hand each reader a set of its own. Each one is read during
     * config checks and generation while another thread may read or replace it, so a set the config keeps
     * refilling in place would be emptied under its holder or seen half-filled.
     */
    @Test
    fun directoryAndFileListsAreNotSharedBetweenReadsOrWrites() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        val lists: Map<String, Pair<() -> TreeSet<String>, (TreeSet<String>) -> Unit>> = mapOf(
            "directoriesToInclude" to Pair({ generationConfig.directoriesToInclude }, { generationConfig.directoriesToInclude = it }),
            "directoriesToExclude" to Pair({ generationConfig.directoriesToExclude }, { generationConfig.directoriesToExclude = it }),
            "postInstallCleanupFiles" to Pair({ generationConfig.postInstallCleanupFiles }, { generationConfig.postInstallCleanupFiles = it }),
            "preInstallCleanupFiles" to Pair({ generationConfig.preInstallCleanupFiles }, { generationConfig.preInstallCleanupFiles = it }),
            "zipArchiveExclusions" to Pair({ generationConfig.zipArchiveExclusions }, { generationConfig.zipArchiveExclusions = it })
        )

        for ((name, accessors) in lists) {
            val (read, write) = accessors
            val held = read()
            val heldContents = TreeSet(held)
            Assertions.assertTrue(held.isNotEmpty(), "precondition: $name has defaults")

            Assertions.assertNotSame(held, read(), "$name: each read yields its own set")
            Assertions.assertEquals(heldContents, held, "$name: another read leaves a held set alone")

            write(TreeSet(setOf("replacement-for-$name")))
            Assertions.assertEquals(heldContents, held, "$name: a write leaves a held set alone")

            val afterWrite = read()
            afterWrite.clear()
            Assertions.assertTrue(read().contains("replacement-for-$name"), "$name: emptying a copy leaves the source")
        }
    }

    /**
     * A blank entry in a directory or file list is never a name: in a cleanup list it names the server pack
     * itself. The GUI hands the setters whatever was typed, a trailing comma included, so they drop blanks before
     * keeping or storing anything.
     */
    @Test
    fun directoryAndFileListsNeverKeepABlankEntry() {
        val store = PropertyStore()
        val generationConfig = GenerationConfig(store)
        val lists: Map<String, Pair<() -> TreeSet<String>, (TreeSet<String>) -> Unit>> = mapOf(
            "directoriesToInclude" to Pair({ generationConfig.directoriesToInclude }, { generationConfig.directoriesToInclude = it }),
            "directoriesToExclude" to Pair({ generationConfig.directoriesToExclude }, { generationConfig.directoriesToExclude = it }),
            "postInstallCleanupFiles" to Pair({ generationConfig.postInstallCleanupFiles }, { generationConfig.postInstallCleanupFiles = it }),
            "preInstallCleanupFiles" to Pair({ generationConfig.preInstallCleanupFiles }, { generationConfig.preInstallCleanupFiles = it }),
            "zipArchiveExclusions" to Pair({ generationConfig.zipArchiveExclusions }, { generationConfig.zipArchiveExclusions = it })
        )

        Assertions.assertAll(lists.map { (name, accessors) ->
            Executable {
                val (read, write) = accessors
                write(TreeSet(setOf("", " ", "kept-by-$name")))
                val current = read()
                Assertions.assertTrue(current.contains("kept-by-$name"), "$name keeps the real entry, got $current")
                Assertions.assertTrue(current.none { it.isBlank() }, "$name holds no blank entry, got $current")
            }
        })
    }
}
