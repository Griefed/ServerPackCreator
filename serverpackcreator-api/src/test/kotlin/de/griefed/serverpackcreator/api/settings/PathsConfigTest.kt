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
 * Tests for [PathsConfig], the home-directory and derived-paths settings-group extracted from
 * ApiProperties in refactor Phase 1b. Uses a scratch Preferences-node so the developer-machine's
 * real ServerPackCreator-preferences stay untouched. Pins home-directory resolution, the
 * derived-directory layout, the server-packs override, and the Tomcat-directory quirks.
 */
internal class PathsConfigTest {
    private val scratchPreferences = Preferences.userRoot().node("ServerPackCreatorPathsConfigTest")

    /**
     * Builds a PathsConfig against a fresh store and the scratch Preferences-node. The
     * JarInformation of the test-class resolves to a directory, putting the config in
     * dev-environment-mode.
     */
    private fun pathsConfig(store: PropertyStore = PropertyStore()): PathsConfig =
        PathsConfig(store, scratchPreferences, JarInformation(PathsConfigTest::class.java), devBuild = true)

    /**
     * Wipes the scratch Preferences-node so each test starts without a stored home-directory.
     */
    @AfterEach
    fun clearScratchPreferences() {
        scratchPreferences.clear()
        scratchPreferences.sync()
    }

    /**
     * Pins that in a dev-environment without stored preference the home-directory resolves to
     * the current working-directory and is persisted as preference.
     */
    @Test
    fun homeDirectoryResolvesToWorkingDirectoryInDevEnvironment() {
        scratchPreferences.clear()
        val paths = pathsConfig()
        Assertions.assertEquals(File("").absoluteFile, paths.homeDirectory)
        Assertions.assertEquals(
            File("").absolutePath,
            scratchPreferences.get(PathsConfig.HOME_DIRECTORY_KEY, null)
        )
    }

    /**
     * Pins that a stored home-directory preference wins over the dev-environment resolution and
     * that the setter persists a new home-directory.
     */
    @Test
    fun homeDirectoryPrefersAndStoresPreference(@TempDir tempDir: File) {
        val firstHome = File(tempDir, "firstHome")
        firstHome.mkdirs()
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, firstHome.absolutePath)
        val paths = pathsConfig()
        Assertions.assertEquals(firstHome.absoluteFile, paths.homeDirectory)

        val secondHome = File(tempDir, "secondHome")
        secondHome.mkdirs()
        paths.homeDirectory = secondHome
        Assertions.assertEquals(secondHome.absoluteFile, paths.homeDirectory)
        Assertions.assertEquals(
            secondHome.absolutePath,
            scratchPreferences.get(PathsConfig.HOME_DIRECTORY_KEY, null)
        )
    }

    /**
     * Pins the derived directory-layout below the home-directory.
     */
    @Test
    fun derivedDirectoriesFollowHomeDirectory(@TempDir tempDir: File) {
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, tempDir.absolutePath)
        val paths = pathsConfig()
        val home = tempDir.absoluteFile
        Assertions.assertEquals(File(home, "serverpackcreator.properties"), paths.serverPackCreatorPropertiesFile)
        Assertions.assertEquals(File(home, "overrides.properties"), paths.overridesPropertiesFile)
        Assertions.assertEquals(File(home, "serverpackcreator.conf"), paths.defaultConfig)
        Assertions.assertEquals(File(home, "configs"), paths.configsDirectory)
        Assertions.assertEquals(File(home, "logs"), paths.logsDirectory)
        Assertions.assertEquals(File(home, "manifests"), paths.manifestsDirectory)
        Assertions.assertEquals(File(home, "work"), paths.workDirectory)
        Assertions.assertEquals(File(home, "work/installers"), paths.installerCacheDirectory)
        Assertions.assertEquals(File(home, "work/temp"), paths.tempDirectory)
        Assertions.assertEquals(File(home, "modpacks"), paths.modpacksDirectory)
        Assertions.assertEquals(File(home, "server_files"), paths.serverFilesDirectory)
        Assertions.assertEquals(File(home, "server_files/properties"), paths.propertiesDirectory)
        Assertions.assertEquals(File(home, "server_files/icons"), paths.iconsDirectory)
        Assertions.assertEquals(File(home, "server_files/server.properties"), paths.defaultServerProperties)
        Assertions.assertEquals(File(home, "server_files/server-icon.png"), paths.defaultServerIcon)
        Assertions.assertEquals(File(home, "plugins"), paths.pluginsDirectory)
        Assertions.assertEquals(File(home, "plugins/config"), paths.pluginsConfigsDirectory)
    }

    /**
     * Pins that every version-manifest file resides in the manifests-directory under its
     * well-known name.
     */
    @Test
    fun manifestFilesResideInManifestsDirectory(@TempDir tempDir: File) {
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, tempDir.absolutePath)
        val paths = pathsConfig()
        val manifests = File(tempDir.absoluteFile, "manifests")
        Assertions.assertEquals(File(manifests, "minecraft-manifest.json"), paths.minecraftVersionManifest)
        Assertions.assertEquals(File(manifests, "forge-manifest.json"), paths.forgeVersionManifest)
        Assertions.assertEquals(File(manifests, "fabric-manifest.xml"), paths.fabricVersionManifest)
        Assertions.assertEquals(File(manifests, "fabric-installer-manifest.xml"), paths.fabricInstallerManifest)
        Assertions.assertEquals(File(manifests, "fabric-intermediaries-manifest.json"), paths.fabricIntermediariesManifest)
        Assertions.assertEquals(File(manifests, "quilt-manifest.xml"), paths.quiltVersionManifest)
        Assertions.assertEquals(File(manifests, "quilt-installer-manifest.xml"), paths.quiltInstallerManifest)
        Assertions.assertEquals(File(manifests, "legacy-fabric-game-manifest.json"), paths.legacyFabricGameManifest)
        Assertions.assertEquals(File(manifests, "legacy-fabric-loader-manifest.json"), paths.legacyFabricLoaderManifest)
        Assertions.assertEquals(File(manifests, "legacy-fabric-installer-manifest.xml"), paths.legacyFabricInstallerManifest)
        Assertions.assertEquals(File(manifests, "neoforge-manifest.xml"), paths.oldNeoForgeVersionManifest)
        Assertions.assertEquals(File(manifests, "neoforge-manifest-new.xml"), paths.newNeoForgeVersionManifest)
        Assertions.assertEquals(File(manifests, "mcserver"), paths.minecraftServerManifestsDirectory)
    }

    /**
     * Pins that the default script-templates reside in the server_files-directory.
     */
    @Test
    fun defaultScriptTemplatesResideInServerFilesDirectory(@TempDir tempDir: File) {
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, tempDir.absolutePath)
        val paths = pathsConfig()
        val serverFiles = File(tempDir.absoluteFile, "server_files")
        Assertions.assertEquals(File(serverFiles, "default_template.sh"), paths.defaultShellScriptTemplate)
        Assertions.assertEquals(File(serverFiles, "default_template.fish"), paths.defaultFishScriptTemplate)
        Assertions.assertEquals(File(serverFiles, "default_template.ps1"), paths.defaultPowerShellScriptTemplate)
        Assertions.assertEquals(File(serverFiles, "default_template.bat"), paths.defaultBatchScriptTemplate)
        Assertions.assertEquals(File(serverFiles, "default_java_template.sh"), paths.defaultJavaShellScriptTemplate)
        Assertions.assertEquals(File(serverFiles, "default_java_template.fish"), paths.defaultJavaFishScriptTemplate)
        Assertions.assertEquals(File(serverFiles, "default_java_template.ps1"), paths.defaultJavaPowerShellScriptTemplate)
        Assertions.assertEquals(File(serverFiles, "default_java_template.bat"), paths.defaultJavaBatchScriptTemplate)
    }

    /**
     * Pins the server-packs directory override: the default and the literal "./server-packs"
     * resolve to the home-relative default, a custom path is honored, and the setter persists.
     */
    @Test
    fun serverPacksDirectoryHonorsOverrideAndFallsBackToDefault(@TempDir tempDir: File) {
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, tempDir.absolutePath)
        val store = PropertyStore()
        val paths = pathsConfig(store)
        Assertions.assertEquals(File(tempDir.absoluteFile, "server-packs"), paths.serverPacksDirectory)

        store.define(PathsConfig.SERVER_PACKS_DIRECTORY_KEY, "./server-packs")
        Assertions.assertEquals(File(tempDir.absoluteFile, "server-packs"), paths.serverPacksDirectory)

        val customServerPacks = File(tempDir, "elsewhere")
        paths.serverPacksDirectory = customServerPacks
        Assertions.assertEquals(customServerPacks.absoluteFile, paths.serverPacksDirectory)
        Assertions.assertEquals(
            customServerPacks.absolutePath,
            store.properties.getProperty(PathsConfig.SERVER_PACKS_DIRECTORY_KEY)
        )
    }

    /**
     * Pins the Tomcat base-directory quirk: reading always resets a deviating value back to the
     * home-directory, making the home-directory the only effective base-directory.
     */
    @Test
    fun tomcatBaseDirectoryAlwaysResetsToHomeDirectory(@TempDir tempDir: File) {
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, tempDir.absolutePath)
        val store = PropertyStore()
        val paths = pathsConfig(store)
        // QUIRK: a deviating Tomcat base-directory is reset to the home-directory on read.
        store.define(PathsConfig.TOMCAT_BASE_DIRECTORY_KEY, File(tempDir, "elsewhere").absolutePath)
        Assertions.assertEquals(tempDir.absoluteFile, paths.tomcatBaseDirectory)
        Assertions.assertEquals(tempDir.absoluteFile, paths.defaultTomcatBaseDirectory())
    }

    /**
     * Pins that the Tomcat logs-directory falls back to the logs-directory when the configured
     * directory is not writable, and honors writable configured directories.
     */
    @Test
    fun tomcatLogsDirectoryFallsBackWhenNotWritable(@TempDir tempDir: File) {
        scratchPreferences.put(PathsConfig.HOME_DIRECTORY_KEY, tempDir.absolutePath)
        val store = PropertyStore()
        val paths = pathsConfig(store)
        store.define(PathsConfig.TOMCAT_LOGS_DIRECTORY_KEY, File(tempDir, "does-not-exist").absolutePath)
        Assertions.assertEquals(File(tempDir.absoluteFile, "logs"), paths.tomcatLogsDirectory)

        val writableLogs = File(tempDir, "writable-logs")
        writableLogs.mkdirs()
        store.define(PathsConfig.TOMCAT_LOGS_DIRECTORY_KEY, writableLogs.absolutePath)
        Assertions.assertEquals(writableLogs.absoluteFile, paths.tomcatLogsDirectory)
        Assertions.assertEquals(File(tempDir.absoluteFile, "logs"), paths.defaultTomcatLogsDirectory())
    }
}
