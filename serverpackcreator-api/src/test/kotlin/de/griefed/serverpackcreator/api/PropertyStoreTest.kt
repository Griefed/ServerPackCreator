package de.griefed.serverpackcreator.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*

/**
 * Tests for [PropertyStore], the property-storage core extracted from ApiProperties in refactor
 * Phase 1b. Pins loading-precedence, blank-value-filtering, typed accessors with
 * define-if-absent-semantics, custom-property prefixing and the save-behavior for tracked files.
 */
internal class PropertyStoreTest {

    /**
     * Pins that loading a file merges its non-blank values into the target and tracks the file
     * for later saving, while blank values are dropped.
     */
    @Test
    fun loadingMergesNonBlankValuesAndTracksFile(@TempDir tempDir: File) {
        val store = PropertyStore()
        val propertiesFile = File(tempDir, "test.properties")
        propertiesFile.writeText("filled=value\nblank=\n")
        val staging = Properties()

        store.loadInto(propertiesFile, staging)

        Assertions.assertEquals("value", staging.getProperty("filled"))
        Assertions.assertNull(staging.getProperty("blank"), "Blank values must be dropped")
        Assertions.assertTrue(store.trackedFiles().contains(propertiesFile))
    }

    /**
     * Pins that loading a non-existent file neither throws nor tracks the file.
     */
    @Test
    fun loadingMissingFileIsIgnored(@TempDir tempDir: File) {
        val store = PropertyStore()
        val staging = Properties()
        store.loadInto(File(tempDir, "missing.properties"), staging)
        Assertions.assertTrue(staging.isEmpty)
        Assertions.assertTrue(store.trackedFiles().isEmpty())
    }

    /**
     * Pins acquire-semantics: an absent or blank key is defined with the default and the default
     * is returned; a present key returns its stored value.
     */
    @Test
    fun acquireDefinesAbsentKeysAndReturnsPresentValues() {
        val store = PropertyStore()
        Assertions.assertEquals("fallback", store.acquire("some.key", "fallback"))
        Assertions.assertEquals("fallback", store.properties.getProperty("some.key"))
        store.define("some.key", "stored")
        Assertions.assertEquals("stored", store.acquire("some.key", "fallback"))
    }

    /**
     * Pins list-handling: comma-separated values split into a list with empty trailing entries
     * dropped, single values produce a singleton-list, and setList joins with the separator.
     */
    @Test
    fun listPropertiesSplitAndJoinOnSeparator() {
        val store = PropertyStore()
        store.define("list.key", "one,two,three,")
        Assertions.assertEquals(listOf("one", "two", "three"), store.getList("list.key", ""))
        store.define("single.key", "lonely")
        Assertions.assertEquals(listOf("lonely"), store.getList("single.key", ""))
        store.setList("joined.key", listOf("a", "b"), ",")
        Assertions.assertEquals("a,b", store.properties.getProperty("joined.key"))
    }

    /**
     * Pins integer-handling: parseable values are returned, unparseable values reset the key to
     * the default.
     */
    @Test
    fun intPropertiesFallBackToDefaultOnGarbage() {
        val store = PropertyStore()
        store.define("int.key", "42")
        Assertions.assertEquals(42, store.getInt("int.key", 7))
        store.define("int.key", "garbage")
        Assertions.assertEquals(7, store.getInt("int.key", 7))
        Assertions.assertEquals("7", store.properties.getProperty("int.key"))
    }

    /**
     * Pins boolean-handling: any value other than "true" (case-insensitive) is false.
     */
    @Test
    fun boolPropertiesParseLeniently() {
        val store = PropertyStore()
        store.define("bool.key", "TRUE")
        Assertions.assertTrue(store.getBool("bool.key", false))
        store.define("bool.key", "nope")
        Assertions.assertFalse(store.getBool("bool.key", true))
        store.setBool("bool.key", true)
        Assertions.assertEquals("true", store.properties.getProperty("bool.key"))
    }

    /**
     * Pins file-list-handling: every entry of the comma-separated list is prefixed.
     */
    @Test
    fun fileListPropertiesApplyPrefix() {
        val store = PropertyStore()
        store.define("files.key", "one.sh,two.sh")
        val files = store.getFileList("files.key", "", "prefix/")
        Assertions.assertEquals(listOf(File("prefix/one.sh"), File("prefix/two.sh")), files)
    }

    /**
     * Pins custom-property prefixing: stored and retrieved values live under "custom.property.".
     */
    @Test
    fun customPropertiesArePrefixed() {
        val store = PropertyStore()
        store.storeCustomProperty("mine", "myvalue")
        Assertions.assertEquals("myvalue", store.properties.getProperty("custom.property.mine"))
        Assertions.assertEquals("myvalue", store.retrieveCustomProperty("mine"))
        Assertions.assertNull(store.retrieveCustomProperty("undefined"))
    }

    /**
     * Pins override-loading: values from the overrides-file replace already-loaded values.
     */
    @Test
    fun overridesReplaceLoadedValues(@TempDir tempDir: File) {
        val store = PropertyStore()
        store.define("some.key", "original")
        val overrides = File(tempDir, "overrides.properties")
        overrides.writeText("some.key=overridden\n")
        store.loadOverrides(overrides)
        Assertions.assertEquals("overridden", store.properties.getProperty("some.key"))
    }

    /**
     * Pins save-behavior: the always-written file is created even when it does not exist yet,
     * other tracked files are only rewritten when they still exist, and keys marked for removal
     * are dropped before writing.
     */
    @Test
    fun saveWritesTrackedFilesAndRemovesLegacyKeys(@TempDir tempDir: File) {
        val store = PropertyStore()
        store.define("keep.key", "value")
        store.define("legacy.key", "stale")

        val trackedExisting = File(tempDir, "tracked.properties")
        trackedExisting.writeText("keep.key=old\n")
        store.loadInto(trackedExisting, Properties())
        val mainFile = File(tempDir, "main.properties")

        store.save(mainFile, alwaysWrite = mainFile, removeKeys = listOf("legacy.key"))

        Assertions.assertTrue(mainFile.isFile, "Main properties-file must be created")
        val written = Properties()
        mainFile.inputStream().use { written.load(it) }
        Assertions.assertEquals("value", written.getProperty("keep.key"))
        Assertions.assertNull(written.getProperty("legacy.key"), "Legacy keys must be removed on save")
        val tracked = Properties()
        trackedExisting.inputStream().use { tracked.load(it) }
        Assertions.assertEquals("value", tracked.getProperty("keep.key"), "Tracked files must be rewritten")
    }
}
