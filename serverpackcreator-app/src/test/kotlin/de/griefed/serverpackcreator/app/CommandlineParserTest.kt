package de.griefed.serverpackcreator.app

import de.griefed.serverpackcreator.api.utilities.common.JarInformation
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.prefs.Preferences

/**
 * Characterization tests pinning the argument-to-mode mapping, the priority ordering and the
 * file/locale parsing of [CommandlineParser] before any app-module restructuring (Phase 2).
 * Only the deterministic branches are pinned — every mode-argument returns before the
 * GraphicsEnvironment.isHeadless()-dependent GUI/failsafe checks, so these tests do not depend
 * on whether the test-JVM has a display.
 */
internal class CommandlineParserTest {
    private val appInfo = JarInformation(CommandlineParserTest::class.java)

    /**
     * Builds a CommandlineParser for the given arguments.
     */
    private fun parse(vararg args: String) = CommandlineParser(arrayOf(*args), appInfo)

    /**
     * Pins that each explicit mode-argument selects its corresponding mode.
     */
    @Test
    fun explicitModeArgumentsSelectTheirMode() {
        Assertions.assertEquals(Mode.HELP, parse("-help").mode)
        Assertions.assertEquals(Mode.UPDATE, parse("-update").mode)
        Assertions.assertEquals(Mode.WITHALLINCONFIGDIR, parse("-withallinconfigdir").mode)
        Assertions.assertEquals(Mode.CLI, parse("-cli").mode)
        Assertions.assertEquals(Mode.WEB, parse("-web").mode)
    }

    /**
     * Pins the priority ordering: HELP wins over UPDATE, UPDATE wins over CLI, and
     * WITHALLINCONFIGDIR wins over CONFIG, regardless of argument order on the commandline.
     */
    @Test
    fun higherPriorityModesWinRegardlessOfOrder() {
        Assertions.assertEquals(Mode.HELP, parse("-cli", "-update", "-help").mode)
        Assertions.assertEquals(Mode.UPDATE, parse("-web", "-update").mode)
        Assertions.assertEquals(Mode.WITHALLINCONFIGDIR, parse("-config", "config.conf", "-withallinconfigdir").mode)
    }

    /**
     * Pins that -config selects CONFIG mode and captures an existing config-file, while a
     * non-existent config-file leaves serverPackConfig empty.
     */
    @Test
    fun configModeCapturesExistingConfigFile(@TempDir tempDir: File) {
        val configFile = File(tempDir, "server pack.conf")
        configFile.writeText("modpackDir = \"x\"")
        val parser = parse("-config", configFile.absolutePath)
        Assertions.assertEquals(Mode.CONFIG, parser.mode)
        Assertions.assertTrue(parser.serverPackConfig.isPresent)
        Assertions.assertEquals(configFile, parser.serverPackConfig.get())

        val missing = parse("-config", File(tempDir, "missing.conf").absolutePath)
        Assertions.assertEquals(Mode.CONFIG, missing.mode)
        Assertions.assertTrue(missing.serverPackConfig.isEmpty)
    }

    /**
     * Pins that --destination alongside -config captures the destination-directory.
     */
    @Test
    fun configModeWithDestinationCapturesDestination(@TempDir tempDir: File) {
        val configFile = File(tempDir, "pack.conf")
        configFile.writeText("modpackDir = \"x\"")
        val destination = File(tempDir, "out")
        val parser = parse("-config", configFile.absolutePath, "--destination", destination.absolutePath)
        Assertions.assertEquals(Mode.CONFIG, parser.mode)
        Assertions.assertTrue(parser.serverPackDestination.isPresent)
        Assertions.assertEquals(destination, parser.serverPackDestination.get())
    }

    /**
     * Pins that -feelinglucky selects FEELINGLUCKY mode and captures an existing modpack-dir and
     * destination.
     */
    @Test
    fun feelingLuckyCapturesModpackDirAndDestination(@TempDir tempDir: File) {
        val modpackDir = File(tempDir, "modpack")
        modpackDir.mkdirs()
        val destination = File(tempDir, "out")
        val parser = parse("-feelinglucky", modpackDir.absolutePath, "--destination", destination.absolutePath)
        Assertions.assertEquals(Mode.FEELINGLUCKY, parser.mode)
        Assertions.assertEquals(modpackDir, parser.modpackDirectory.get())
        Assertions.assertEquals(destination, parser.serverPackDestination.get())
    }

    /**
     * Pins that -cgen selects CGEN mode and captures an existing modpack-directory.
     */
    @Test
    fun cgenCapturesModpackDirectory(@TempDir tempDir: File) {
        val modpackDir = File(tempDir, "modpack")
        modpackDir.mkdirs()
        val parser = parse("-cgen", modpackDir.absolutePath)
        Assertions.assertEquals(Mode.CGEN, parser.mode)
        Assertions.assertEquals(modpackDir, parser.modpackDirectory.get())
    }

    /**
     * Pins that -lang parses the following argument into a locale, and that its absence leaves
     * the language null so the ApiProperties-locale is used downstream.
     */
    @Test
    fun langArgumentParsesLocale() {
        val parser = parse("-lang", "en_us", "-cli")
        Assertions.assertEquals("en_us", parser.language.toString())
        Assertions.assertNull(parse("-cli").language)
    }

    /**
     * Pins the default properties-file selection: overrides.properties next to the JAR wins over
     * serverpackcreator.properties; the test-JVM has neither, so it defaults to the latter.
     */
    @Test
    fun defaultPropertiesFileFallsBackToServerPackCreatorProperties() {
        val parser = parse("-cli")
        Assertions.assertEquals("serverpackcreator.properties", parser.propertiesFile.name)
    }

    /**
     * Pins that --setup with an existing properties-file selects SETUP mode and captures the
     * file as the properties-file to load.
     */
    @Test
    fun setupModeCapturesPropertiesFile(@TempDir tempDir: File) {
        val propertiesFile = File(tempDir, "custom.properties")
        propertiesFile.writeText("de.griefed.serverpackcreator.language=en_GB")
        val parser = parse("--setup", propertiesFile.absolutePath)
        Assertions.assertEquals(Mode.SETUP, parser.mode)
        Assertions.assertEquals(propertiesFile, parser.propertiesFile)
    }

    /**
     * Pins the --home side-effect: a valid home-directory is stored both on the parser and in
     * the ServerPackCreator-preferences node. The preference is saved and restored so the
     * developer-machine's real home-directory setting is left untouched.
     */
    @Test
    fun homeArgumentStoresHomeDirectoryPreference(@TempDir tempDir: File) {
        val preferences = Preferences.userRoot().node("ServerPackCreator")
        val homeKey = "de.griefed.serverpackcreator.home"
        val previous = preferences.get(homeKey, null)
        try {
            val parser = parse("--home", tempDir.absolutePath, "-cli")
            Assertions.assertEquals(tempDir.absoluteFile, parser.homeDir.get())
            Assertions.assertEquals(tempDir.absolutePath, preferences.get(homeKey, null))
        } finally {
            if (previous == null) {
                preferences.remove(homeKey)
            } else {
                preferences.put(homeKey, previous)
            }
            preferences.sync()
        }
    }
}
