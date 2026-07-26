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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*

/**
 * Settings-group for the start- and java-script-templates used during server pack generation:
 * the per-type template-maps stored under prefixed property-keys, their defaults inside the
 * server_files-directory, and the deprecated list-based template-handling. Extracted from
 * ApiProperties (refactor Phase 1b); ApiProperties remains the facade through which consumers
 * access these values.
 */
class ScriptTemplatesConfig(
    private val store: PropertyStore,
    private val paths: PathsConfig
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    companion object {
        /**
         * Property-key prefix under which the start-script templates are stored per script-type.
         */
        const val START_SCRIPT_TEMPLATES_PREFIX = "de.griefed.serverpackcreator.serverpack.script.template."

        /**
         * Property-key prefix under which the java-install-script templates are stored per
         * script-type.
         */
        const val JAVA_SCRIPT_TEMPLATES_PREFIX = "de.griefed.serverpackcreator.serverpack.java.template."

        /**
         * Legacy property-key of the deprecated list-based script-templates.
         */
        const val LEGACY_SCRIPT_TEMPLATES_KEY = "de.griefed.serverpackcreator.serverpack.script.template"
    }

    /**
     * Default list of script templates used by ServerPackCreator.
     *
     * @author Griefed
     */
    @Deprecated("Deprecated as of 6.0.0", ReplaceWith("defaultScriptTemplateMap"))
    fun defaultScriptTemplates(): List<File> {
        // See whether we have custom files.
        val currentFiles = paths.serverFilesDirectory.walk().maxDepth(1).filter {
            it.name.endsWith("sh", ignoreCase = true) ||
                    it.name.endsWith("fish", ignoreCase = true) ||
                    it.name.endsWith("ps1", ignoreCase = true) ||
                    it.name.endsWith("bat", ignoreCase = true)
        }.toList()
        val customTemplates = currentFiles.filter {
            !it.name.contains("default_template", ignoreCase = true)
        }

        val newTemplates = mutableListOf<File>()
        var shellPresent = false
        var fishPresent = false
        var powershellPresent = false
        var batchPresent = false
        for (customTemplate in customTemplates) {
            when {
                customTemplate.name.endsWith("sh", ignoreCase = true) && !shellPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    shellPresent = true
                }

                customTemplate.name.endsWith("fish", ignoreCase = true) && !shellPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    fishPresent = true
                }

                customTemplate.name.endsWith("ps1", ignoreCase = true) && !powershellPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    powershellPresent = true
                }

                customTemplate.name.endsWith("bat", ignoreCase = true) && !batchPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    batchPresent = true
                }

                else -> {
                    newTemplates.add(customTemplate.absoluteFile)
                }
            }
        }

        if (!shellPresent) {
            newTemplates.add(File(paths.serverFilesDirectory.absolutePath, paths.defaultShellScriptTemplate.name).absoluteFile)
        }
        if (!fishPresent) {
            newTemplates.add(File(paths.serverFilesDirectory.absolutePath, paths.defaultFishScriptTemplate.name).absoluteFile)
        }
        if (!powershellPresent) {
            newTemplates.add(File(paths.serverFilesDirectory.absolutePath, paths.defaultPowerShellScriptTemplate.name).absoluteFile)
        }
        if (!batchPresent) {
            newTemplates.add(File(paths.serverFilesDirectory.absolutePath, paths.defaultBatchScriptTemplate.name).absoluteFile)
        }

        return newTemplates.toList()
    }

    @Deprecated("Deprecated as of 6.0.0", ReplaceWith("startScriptTemplates"))
    var scriptTemplates: TreeSet<File> = TreeSet()
        get() {
            val scriptSetting = store.properties.getProperty(LEGACY_SCRIPT_TEMPLATES_KEY)
            val entries =
                if (scriptSetting != null && scriptSetting == "default_template.ps1,default_template.sh,default_template.fish,default_template.bat") {
                    defaultScriptTemplates()
                } else {
                    store.getList(
                        LEGACY_SCRIPT_TEMPLATES_KEY,
                        defaultScriptTemplates().joinToString(",") { it.absolutePath }
                    ).map { File(it).absoluteFile }
                }
            field.clear()
            field.addAll(entries)
            return field
        }
        set(value) {
            val entries = value.map { it.absolutePath }
            store.setList(LEGACY_SCRIPT_TEMPLATES_KEY, entries, ",")
            field.clear()
            field.addAll(value.map { it.absoluteFile })
            log.info("Using script templates:")
            for (template in field) {
                log.info("    " + template.path)
            }
        }

    /**
     * Default map of start-script templates: sh, ps1, bat.
     */
    fun defaultStartScriptTemplates(): HashMap<String, String> {
        return hashMapOf(
            Pair("sh", File(paths.serverFilesDirectory.absolutePath, paths.defaultShellScriptTemplate.name).absolutePath),
            Pair("ps1", File(paths.serverFilesDirectory.absolutePath, paths.defaultPowerShellScriptTemplate.name).absolutePath),
            Pair("bat", File(paths.serverFilesDirectory.absolutePath, paths.defaultBatchScriptTemplate.name).absolutePath),
            Pair("fish", File(paths.serverFilesDirectory.absolutePath, paths.defaultFishScriptTemplate.name).absolutePath)
        )
    }

    /**
     * Start-script templates to use during server pack generation.
     * Each key represents a different template and script-type.
     */
    var startScriptTemplates: HashMap<String, String> = hashMapOf()
        get() {
            val templateProps = store.properties.keys
                .filter { entry -> (entry as String).startsWith(START_SCRIPT_TEMPLATES_PREFIX) }
                .map { entry -> entry as String }
            var type: String
            if (templateProps.isEmpty() || templateProps.any { entry ->
                    entry.replace(START_SCRIPT_TEMPLATES_PREFIX, "").isBlank()
                }) {
                log.warn("Found empty definitions for start script templates. Using defaults.")
                field = defaultStartScriptTemplates()
            } else {
                for (templateProp in templateProps) {
                    type = templateProp.replace(START_SCRIPT_TEMPLATES_PREFIX, "")
                    field[type] = File(store.properties[templateProp] as String).absolutePath
                }
            }
            if (field.isEmpty()) {
                log.error("No start script templates defined. Using defaults.")
                field = defaultStartScriptTemplates()
            }
            return field
        }
        set(map) {
            for ((key, value) in map) {
                store.define("$START_SCRIPT_TEMPLATES_PREFIX$key", value)
                log.info("Set $START_SCRIPT_TEMPLATES_PREFIX$key to $value")
            }
            field = map
        }

    /**
     * Default map of start-script templates: sh, ps1, bat.
     */
    fun defaultJavaScriptTemplates(): HashMap<String, String> {
        return hashMapOf(
            Pair("sh", File(paths.serverFilesDirectory.absolutePath, paths.defaultJavaShellScriptTemplate.name).absolutePath),
            Pair("fish", File(paths.serverFilesDirectory.absolutePath, paths.defaultJavaFishScriptTemplate.name).absolutePath),
            Pair("ps1", File(paths.serverFilesDirectory.absolutePath, paths.defaultJavaPowerShellScriptTemplate.name).absolutePath)
        )
    }

    /**
     * Start-script templates to use during server pack generation.
     * Each key represents a different template and script-type.
     */
    var javaScriptTemplates: HashMap<String, String> = hashMapOf()
        get() {
            val templateProps = store.properties.keys
                .filter { entry -> (entry as String).startsWith(JAVA_SCRIPT_TEMPLATES_PREFIX) }
                .map { entry -> entry as String }
            var type: String
            if (templateProps.isEmpty() || templateProps.any { entry ->
                    entry.replace(
                        JAVA_SCRIPT_TEMPLATES_PREFIX,
                        ""
                    ).isBlank()
                }) {
                log.warn("Found empty definitions for java script templates. Using defaults.")
                field = defaultJavaScriptTemplates()
            } else {
                for (templateProp in templateProps) {
                    type = templateProp.replace(JAVA_SCRIPT_TEMPLATES_PREFIX, "")
                    field[type] = File(store.properties[templateProp] as String).absolutePath
                }
            }
            if (field.isEmpty()) {
                log.error("No java script templates defined. Using defaults.")
                field = defaultJavaScriptTemplates()
            }
            return field
        }
        set(map) {
            for ((key, value) in map) {
                store.define("$JAVA_SCRIPT_TEMPLATES_PREFIX$key", value)
                log.info("Set $JAVA_SCRIPT_TEMPLATES_PREFIX$key to $value")
            }
            field = map
        }
}
