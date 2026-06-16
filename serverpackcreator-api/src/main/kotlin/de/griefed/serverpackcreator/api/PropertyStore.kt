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
package de.griefed.serverpackcreator.api

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * Property-storage core of ServerPackCreator: loads properties-files with blank-value filtering,
 * offers typed accessors with define-if-absent-semantics, manages custom user-properties under a
 * dedicated prefix, and saves the merged state back to every tracked file. Extracted from
 * ApiProperties (refactor Phase 1b) so configuration-groups can share one storage-engine.
 */
class PropertyStore {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val trackedPropertyFiles: MutableList<File> = mutableListOf()

    /**
     * Prefix under which [storeCustomProperty] and [retrieveCustomProperty] store user-defined
     * properties, preventing clashes with ServerPackCreator's own keys.
     */
    val customPropertyPrefix = "custom.property."

    /**
     * The merged properties this store manages. Exposed so the orchestrating ApiProperties can
     * keep its existing load-ordering; treat as owned by this store everywhere else.
     */
    val properties = Properties()

    /**
     * Loads the given properties-file into [target], dropping blank values and remembering the
     * file for later [save]-calls. A missing file is logged and skipped.
     */
    fun loadInto(propertiesFile: File, target: Properties) {
        if (!propertiesFile.isFile) {
            log.warn("Properties-file does not exist: ${propertiesFile.absolutePath}.")
            return
        }
        try {
            val stagedProps = Properties()
            propertiesFile.inputStream().use {
                stagedProps.load(it)
            }
            stagedProps.entries.removeIf { entry -> entry.value.toString().isBlank() }
            for ((key, value) in stagedProps.entries) {
                target[key] = value
            }
            target.entries.removeIf { entry -> entry.value.toString().isBlank() }
            trackedPropertyFiles.add(propertiesFile)
            log.info("Loaded properties from ${propertiesFile.absolutePath}.")
        } catch (ex: Exception) {
            log.error("Couldn't read properties from ${propertiesFile.absolutePath}.", ex)
        }
    }

    /**
     * Loads the given overrides-file directly into [properties], replacing already-loaded values.
     * Every overridden key is logged; values of keys containing credentials are masked.
     */
    fun loadOverrides(overridesFile: File) {
        val overrideProps = Properties()
        if (overridesFile.isFile) {
            overridesFile.inputStream().use {
                overrideProps.load(it)
            }
        }
        for ((key, value) in overrideProps) {
            log.warn("Overriding:")
            log.warn("  $key")
            if (key.toString().contains("(username|password)")) {
                log.warn("  ************************************")
            } else {
                log.warn("  $value")
            }
        }
        properties.putAll(overrideProps)
    }

    /**
     * Returns the value for [key], defining it with [defaultValue] first when the key is absent
     * or blank, thus guaranteeing subsequent calls find the key present.
     */
    fun acquire(key: String, defaultValue: String): String =
        if (properties.getProperty(key).isNullOrBlank()) {
            define(key, defaultValue)
        } else {
            properties.getProperty(key, defaultValue)
        }

    /**
     * Sets [key] to [value] and returns the value for convenient chaining.
     */
    fun define(key: String, value: String): String {
        properties.setProperty(key, value)
        return value
    }

    /**
     * Returns the comma-separated value of [key] as a list, dropping empty trailing entries; a
     * value without separator yields a singleton-list. Absent keys are defined with [defaultValue].
     */
    fun getList(key: String, defaultValue: String): List<String> =
        if (acquire(key, defaultValue).contains(",")) {
            acquire(key, defaultValue)
                .split(",")
                .dropLastWhile { it.isEmpty() }
        } else {
            listOf(acquire(key, defaultValue))
        }

    /**
     * Sets [key] to [values] joined by [separator].
     */
    fun setList(key: String, values: List<String>, separator: String) {
        properties.setProperty(key, values.joinToString(separator))
    }

    /**
     * Returns the integer-value of [key]; an unparseable value resets the key to [defaultValue]
     * and returns it.
     */
    fun getInt(key: String, defaultValue: Int): Int =
        try {
            acquire(key, defaultValue.toString()).toInt()
        } catch (ex: NumberFormatException) {
            define(key, defaultValue.toString())
            defaultValue
        }

    /**
     * Sets the integer-property [key] to [value].
     */
    fun setInt(key: String, value: Int): String = define(key, value.toString())

    /**
     * Returns the entries of the comma-separated list in [key] as files, each prefixed with
     * [filePrefix]. Absent keys are defined with [defaultValue].
     */
    fun getFileList(key: String, defaultValue: String, filePrefix: String): List<File> {
        val files: MutableList<File> = ArrayList(4)
        val entries = getList(key, defaultValue)
        for (entry in entries) {
            files.add(File(filePrefix + entry))
        }
        return files
    }

    /**
     * Returns the boolean-value of [key]; any value other than "true" (case-insensitive) is
     * false. Absent keys are defined with [defaultValue].
     */
    fun getBool(key: String, defaultValue: Boolean): Boolean =
        acquire(key, defaultValue.toString()).toBoolean()

    /**
     * Sets the boolean-property [key] to [value].
     */
    fun setBool(key: String, value: Boolean): String = define(key, value.toString())

    /**
     * Stores a user-defined property under the [customPropertyPrefix] to prevent clashes with
     * ServerPackCreator's own keys, returning the stored value.
     */
    fun storeCustomProperty(property: String, value: String): String =
        define("$customPropertyPrefix$property", value)

    /**
     * Retrieves a user-defined property stored under the [customPropertyPrefix], or null when
     * undefined.
     */
    fun retrieveCustomProperty(property: String): String? =
        properties.getProperty("$customPropertyPrefix$property")

    /**
     * The files from which properties were loaded and to which [save] writes back.
     */
    fun trackedFiles(): List<File> = trackedPropertyFiles

    /**
     * Forgets all tracked properties-files, so subsequent [save]-calls only write the
     * always-written file.
     */
    fun clearTrackedFiles() {
        trackedPropertyFiles.clear()
    }

    /**
     * Saves the merged properties to [propertiesFile] and every tracked file, dropping
     * [removeKeys] (legacy-keys) first. Tracked files which no longer exist are skipped, except
     * [alwaysWrite], which is created when missing.
     */
    fun save(propertiesFile: File, alwaysWrite: File, removeKeys: List<String> = emptyList()) {
        for (legacyKey in removeKeys) {
            properties.remove(legacyKey)
        }
        val toSave = TreeSet<File>()
        toSave.addAll(trackedPropertyFiles)
        toSave.add(propertiesFile)

        for (file in toSave) {
            if (!file.isFile && file != alwaysWrite) {
                //Skip if the file no longer exists
                continue
            }
            try {
                file.outputStream().use {
                    properties.store(
                        it,
                        "For details about each property, see https://help.serverpackcreator.de/settings-and-configs.html"
                    )
                }
                log.info("Saved properties to: $file")
            } catch (ex: FileNotFoundException) {
                log.error("Couldn't write properties-file ${file.absolutePath}. File either doesn't exist or we don't have write-permission.")
            } catch (ex: IOException) {
                log.error("Couldn't write properties-file ${file.absolutePath}.", ex)
            }
        }
    }
}
