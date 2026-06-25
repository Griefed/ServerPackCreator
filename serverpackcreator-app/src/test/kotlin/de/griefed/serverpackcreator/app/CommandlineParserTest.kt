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
     * Pins that -scan selects SCAN mode and captures the directory plus the loader/Minecraft options
     * regardless of their order relative to the directory.
     */
    @Test
    fun scanModeCapturesDirectoryLoaderAndMinecraftVersion(@TempDir tempDir: File) {
        val modsDir = File(tempDir, "mods")
        modsDir.mkdirs()
        val parser = parse("-scan", modsDir.absolutePath, "--loader", "Forge", "--minecraft", "1.20.1")
        Assertions.assertEquals(Mode.SCAN, parser.mode)
        Assertions.assertEquals(modsDir, parser.scanDirectory.get())
        Assertions.assertEquals("Forge", parser.scanLoader)
        Assertions.assertEquals("1.20.1", parser.scanMinecraftVersion)
    }

    /**
     * Pins that -clientsidereport selects CLIENTSIDE_REPORT mode and captures the project-link.
     */
    @Test
    fun clientsideReportModeCapturesProjectLink() {
        val parser = parse("-clientsidereport", "https://modrinth.com/mod/jei")
        Assertions.assertEquals(Mode.CLIENTSIDE_REPORT, parser.mode)
        Assertions.assertEquals("https://modrinth.com/mod/jei", parser.clientsideLink.get())
    }

    /**
     * Pins that -verifyclientside selects VERIFY_CLIENTSIDE mode with the project-link and output.
     */
    @Test
    fun verifyClientsideModeCapturesLinkAndOutput() {
        val parser = parse("-verifyclientside", "https://modrinth.com/mod/jei", "--output", "report.md")
        Assertions.assertEquals(Mode.VERIFY_CLIENTSIDE, parser.mode)
        Assertions.assertEquals("https://modrinth.com/mod/jei", parser.clientsideVerifyLink.get())
        Assertions.assertEquals("report.md", parser.clientsideVerifyOutput)
    }

    /**
     * Pins that -clientsideapply selects CLIENTSIDE_APPLY mode and captures the report-path from
     * either the --report option or the positional argument.
     */
    @Test
    fun clientsideApplyModeCapturesReportPath() {
        val optionForm = parse("-clientsideapply", "--report", "report.json")
        Assertions.assertEquals(Mode.CLIENTSIDE_APPLY, optionForm.mode)
        Assertions.assertEquals("report.json", optionForm.clientsideApplyReport.get())

        val positionalForm = parse("-clientsideapply", "report.json")
        Assertions.assertEquals("report.json", positionalForm.clientsideApplyReport.get())
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
