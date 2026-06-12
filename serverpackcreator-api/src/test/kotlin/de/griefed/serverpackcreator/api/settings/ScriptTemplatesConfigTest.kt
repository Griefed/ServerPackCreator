package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.utilities.common.JarInformation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.prefs.Preferences

/**
 * Tests for [ScriptTemplatesConfig], the start- and java-script-template settings-group
 * extracted from ApiProperties in refactor Phase 1b. Pins the default-template maps, the
 * prefixed-property mapping of custom templates, and the deprecated list-based template
 * handling including custom-file discovery in the server_files-directory.
 */
internal class ScriptTemplatesConfigTest {
    private val scratchPreferences = Preferences.userRoot().node("ServerPackCreatorScriptTemplatesConfigTest")

    /**
     * Builds a ScriptTemplatesConfig whose PathsConfig points at the given home-directory via
     * the scratch Preferences-node.
     */
    private fun templatesConfig(homeDirectory: File, store: PropertyStore = PropertyStore()): ScriptTemplatesConfig {
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, homeDirectory.absolutePath)
        val paths = PathsConfig(store, scratchPreferences, JarInformation(ScriptTemplatesConfigTest::class.java), devBuild = true)
        return ScriptTemplatesConfig(store, paths)
    }

    /**
     * Wipes the scratch Preferences-node so each test starts without a stored home-directory.
     */
    @AfterEach
    fun clearScratchPreferences() {
        scratchPreferences.clear()
        scratchPreferences.sync()
    }

    /**
     * Pins that the default start-script templates map sh, ps1 and bat to the default templates
     * in the server_files-directory, and the java-templates map sh and ps1.
     */
    @Test
    fun defaultTemplateMapsPointIntoServerFilesDirectory(@TempDir tempDir: File) {
        val templates = templatesConfig(tempDir)
        val serverFiles = File(tempDir.absoluteFile, "server_files")
        val startDefaults = templates.defaultStartScriptTemplates()
        Assertions.assertEquals(File(serverFiles, "default_template.sh").absolutePath, startDefaults["sh"])
        Assertions.assertEquals(File(serverFiles, "default_template.ps1").absolutePath, startDefaults["ps1"])
        Assertions.assertEquals(File(serverFiles, "default_template.bat").absolutePath, startDefaults["bat"])
        val javaDefaults = templates.defaultJavaScriptTemplates()
        Assertions.assertEquals(File(serverFiles, "default_java_template.sh").absolutePath, javaDefaults["sh"])
        Assertions.assertEquals(File(serverFiles, "default_java_template.ps1").absolutePath, javaDefaults["ps1"])
        Assertions.assertEquals(2, javaDefaults.size, "Java-templates default to sh and ps1 only")
    }

    /**
     * Pins that start-script templates fall back to the defaults when no template-properties are
     * defined, and that custom templates are read from and written to prefixed properties.
     */
    @Test
    fun startScriptTemplatesUseDefaultsAndPrefixedProperties(@TempDir tempDir: File) {
        val store = PropertyStore()
        val templates = templatesConfig(tempDir, store)
        Assertions.assertEquals(templates.defaultStartScriptTemplates(), templates.startScriptTemplates)

        val customTemplate = File(tempDir, "my_template.sh")
        customTemplate.writeText("#!/usr/bin/env bash")
        templates.startScriptTemplates = hashMapOf("sh" to customTemplate.absolutePath)
        Assertions.assertEquals(
            customTemplate.absolutePath,
            store.properties.getProperty("${ScriptTemplatesConfig.START_SCRIPT_TEMPLATES_PREFIX}sh")
        )
        Assertions.assertEquals(customTemplate.absolutePath, templates.startScriptTemplates["sh"])
    }

    /**
     * Pins that java-script templates fall back to the defaults when no template-properties are
     * defined, and that custom templates are read from and written to prefixed properties.
     */
    @Test
    fun javaScriptTemplatesUseDefaultsAndPrefixedProperties(@TempDir tempDir: File) {
        val store = PropertyStore()
        val templates = templatesConfig(tempDir, store)
        Assertions.assertEquals(templates.defaultJavaScriptTemplates(), templates.javaScriptTemplates)

        val customTemplate = File(tempDir, "my_java_template.ps1")
        customTemplate.writeText("Write-Host java")
        templates.javaScriptTemplates = hashMapOf("ps1" to customTemplate.absolutePath)
        Assertions.assertEquals(
            customTemplate.absolutePath,
            store.properties.getProperty("${ScriptTemplatesConfig.JAVA_SCRIPT_TEMPLATES_PREFIX}ps1")
        )
        Assertions.assertEquals(customTemplate.absolutePath, templates.javaScriptTemplates["ps1"])
    }

    /**
     * Pins the deprecated default-template discovery: custom sh/ps1/bat-files in the
     * server_files-directory are picked up, and default-templates fill the remaining types.
     */
    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDefaultTemplatesDiscoverCustomFiles(@TempDir tempDir: File) {
        val templates = templatesConfig(tempDir)
        val serverFiles = File(tempDir.absoluteFile, "server_files")
        serverFiles.mkdirs()
        val customShell = File(serverFiles, "custom_template.sh")
        customShell.writeText("#!/usr/bin/env bash")

        val discovered = templates.defaultScriptTemplates()

        Assertions.assertTrue(discovered.contains(customShell.absoluteFile), "Custom shell-template must be discovered")
        Assertions.assertTrue(
            discovered.contains(File(serverFiles, "default_template.ps1").absoluteFile),
            "Missing types must fall back to default-templates"
        )
        Assertions.assertTrue(
            discovered.contains(File(serverFiles, "default_template.bat").absoluteFile),
            "Missing types must fall back to default-templates"
        )
    }
}
