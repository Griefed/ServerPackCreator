package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.config.ModpackSource
import de.griefed.serverpackcreator.api.config.PackConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Characterization tests pinning the current behavior of [de.griefed.serverpackcreator.api.config.ConfigurationHandler]
 * before its decomposition into per-concern validators (refactor Phase 1c). These tests document
 * behavior as-is — including quirks — so the upcoming restructuring can be verified to be
 * behavior-preserving. Quirks worth keeping in mind are marked with "QUIRK" comments.
 */
internal class ConfigurationHandlerCharacterizationTest {
    private val configurationHandler =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).configurationHandler
    private val apiProperties =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).apiProperties

    /**
     * Pins that a destination which exists neither plain nor as `_0`-suffixed directory is
     * suffixed with `_0`.
     */
    @Test
    fun unzipDestinationAppendsZeroForNewDestination(@TempDir tempDir: File) {
        val destination = File(tempDir, "fresh_destination").absolutePath
        Assertions.assertEquals(File("${destination}_0").path, configurationHandler.unzipDestination(destination))
    }

    /**
     * Pins that an already existing destination directory is incremented to the first free
     * `_n`-suffixed variant.
     */
    @Test
    fun unzipDestinationIncrementsExistingDestination(@TempDir tempDir: File) {
        val destination = File(tempDir, "taken_destination").absolutePath
        File(destination).mkdirs()
        File("${destination}_0").mkdirs()
        Assertions.assertEquals(File("${destination}_1").path, configurationHandler.unzipDestination(destination))
    }

    /**
     * Pins modloader-name normalization for all five supported loaders, regardless of input case,
     * and the fallback to Forge for unknown input.
     */
    @Test
    fun modLoaderCaseNormalizesKnownLoadersAndDefaultsToForge() {
        Assertions.assertEquals("Forge", configurationHandler.getModLoaderCase("forge"))
        Assertions.assertEquals("Forge", configurationHandler.getModLoaderCase("FORGE"))
        Assertions.assertEquals("Fabric", configurationHandler.getModLoaderCase("fAbRiC"))
        Assertions.assertEquals("Quilt", configurationHandler.getModLoaderCase("QUILT"))
        Assertions.assertEquals("LegacyFabric", configurationHandler.getModLoaderCase("legacyfabric"))
        Assertions.assertEquals("LegacyFabric", configurationHandler.getModLoaderCase("LegacyFabric"))
        Assertions.assertEquals("NeoForge", configurationHandler.getModLoaderCase("neoforge"))
        Assertions.assertEquals("NeoForge", configurationHandler.getModLoaderCase("NeoForge"))
        // QUIRK: unknown loaders silently fall back to Forge instead of erroring.
        Assertions.assertEquals("Forge", configurationHandler.getModLoaderCase("somethingelse"))
    }

    /**
     * Pins the normalizing setter of [PackConfig.modloader]: recognized loaders are normalized
     * on assignment, while unrecognized values are silently ignored and leave the field unchanged.
     */
    @Test
    fun packConfigModloaderSetterNormalizesAndIgnoresUnknown() {
        val packConfig = PackConfig()
        packConfig.modloader = "forge"
        Assertions.assertEquals("Forge", packConfig.modloader)
        // QUIRK: assigning an unrecognized loader is a silent no-op instead of an error.
        packConfig.modloader = "garbage"
        Assertions.assertEquals("Forge", packConfig.modloader)
    }

    /**
     * Pins parsing of a CurseForge manifest.json: Minecraft version, modloader and its version
     * split from the combined id, and the pack name.
     */
    @Test
    fun curseManifestUpdatesConfigModel() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromCurseManifest(
            packConfig,
            File("src/test/resources/testresources/curseforge/forge_manifest.json")
        )
        Assertions.assertEquals("1.16.5", packConfig.minecraftVersion)
        // The manifest carries "forge", but PackConfig.modloader's setter normalizes on assignment.
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("36.0.1", packConfig.modloaderVersion)
        Assertions.assertEquals("Vanilla Forge 1.16.5", packConfig.name)
    }

    /**
     * Pins that the pack name is read from the stored modpack-JSON when present, and that a
     * missing JSON falls back to the modpacks-directory name.
     */
    @Test
    fun updatePackNameReadsFromJsonAndFallsBackToModpacksDirectory() {
        val withJson = PackConfig()
        configurationHandler.updateConfigModelFromCurseManifest(
            withJson,
            File("src/test/resources/testresources/curseforge/forge_manifest.json")
        )
        Assertions.assertEquals("Vanilla Forge 1.16.5", configurationHandler.updatePackName(withJson, "name"))

        val withoutJson = PackConfig()
        Assertions.assertEquals(
            apiProperties.modpacksDirectory.name,
            configurationHandler.updatePackName(withoutJson)
        )
    }

    /**
     * Pins parsing of a GDLauncher config.json: Minecraft version, normalized modloader, and the
     * modloader version with the leading Minecraft-version prefix stripped.
     */
    @Test
    fun gdLauncherConfigJsonUpdatesConfigModel() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromConfigJson(
            packConfig,
            File("src/test/resources/testresources/gdlauncher/forge_config.json")
        )
        Assertions.assertEquals("1.18.2", packConfig.minecraftVersion)
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("40.1.52", packConfig.modloaderVersion)
    }

    /**
     * Pins parsing of a GDLauncher instance.json: name, versions, project/file IDs and the
     * CurseForge source mapping.
     */
    @Test
    fun gdLauncherInstanceJsonUpdatesConfigModel() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromGDInstanceJson(
            packConfig,
            File("src/test/resources/testresources/gdlauncher/gd_instance.json")
        )
        Assertions.assertEquals("1.20.1", packConfig.minecraftVersion)
        // The instance.json carries "forge", but PackConfig.modloader's setter normalizes on assignment.
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("47.2.0", packConfig.modloaderVersion)
        Assertions.assertEquals("GDLauncher Test Pack", packConfig.name)
        Assertions.assertEquals("123456", packConfig.projectID)
        Assertions.assertEquals("654321", packConfig.versionID)
        Assertions.assertEquals(ModpackSource.CURSEFORGE, packConfig.source)
    }

    /**
     * Pins parsing of an ATLauncher instance.json: Minecraft version from the id-field, loader
     * type/version, pack name and CurseForge project/file IDs.
     */
    @Test
    fun atLauncherInstanceJsonUpdatesConfigModel() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromATLauncherInstance(
            packConfig,
            File("src/test/resources/testresources/atlauncher/instance.json")
        )
        Assertions.assertEquals("1.19.2", packConfig.minecraftVersion)
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("43.2.0", packConfig.modloaderVersion)
        Assertions.assertEquals("ATLauncher Test Pack", packConfig.name)
        Assertions.assertEquals("999111", packConfig.projectID)
        Assertions.assertEquals("888222", packConfig.versionID)
        Assertions.assertEquals(ModpackSource.CURSEFORGE, packConfig.source)
    }

    /**
     * Pins parsing of a MultiMC mmc-pack.json: component list yields Minecraft version and the
     * Quilt loader with its version.
     */
    @Test
    fun mmcPackJsonUpdatesConfigModel() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromMMCPack(
            packConfig,
            File("src/test/resources/testresources/multimc/quilt_mmc-pack.json")
        )
        Assertions.assertEquals("1.19", packConfig.minecraftVersion)
        Assertions.assertEquals("Quilt", packConfig.modloader)
        Assertions.assertEquals("0.17.0", packConfig.modloaderVersion)
    }

    /**
     * Pins that the instance-name is read from a MultiMC instance.cfg's name-property.
     */
    @Test
    fun instanceCfgYieldsInstanceName() {
        Assertions.assertEquals(
            "Better Minecraft [FABRIC] - 1.18.1",
            configurationHandler.updateDestinationFromInstanceCfg(
                File("src/test/resources/testresources/multimc/better_mc_instance.cfg")
            )
        )
    }

    /**
     * Pins that a valid modpack ZIP-archive passes the archive check without errors.
     */
    @Test
    fun zipArchiveCheckPassesForValidModpackZip() {
        val configCheck = configurationHandler.checkZipArchive(
            File("src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip").absolutePath
        )
        Assertions.assertTrue(configCheck.modpackChecksPassed, configCheck.modpackErrors.joinToString("; "))
    }

    /**
     * Pins that a ZIP-archive with an invalid base-directory layout fails the archive check.
     */
    @Test
    fun zipArchiveCheckFailsForInvalidModpackZip() {
        val configCheck = configurationHandler.checkZipArchive(
            File("src/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip").absolutePath
        )
        Assertions.assertFalse(configCheck.modpackChecksPassed)
        Assertions.assertTrue(configCheck.modpackErrors.isNotEmpty())
    }

    /**
     * Pins that inclusion-suggestions list only directories of the modpack, leaving out plain
     * files and the well-known excluded directories.
     */
    @Test
    fun suggestInclusionsListsModpackDirectoriesOnly() {
        val suggestions = configurationHandler.suggestInclusions(
            File("src/test/resources/forge_tests").absolutePath
        )
        val sources = suggestions.map { inclusion -> inclusion.source }
        Assertions.assertTrue(sources.contains("config"))
        Assertions.assertTrue(sources.contains("mods"))
        Assertions.assertTrue(sources.contains("scripts"))
        Assertions.assertFalse(sources.contains("test.txt"), "Plain files must not be suggested")
        for (excluded in apiProperties.directoriesToExclude) {
            Assertions.assertFalse(sources.contains(excluded), "Excluded directory $excluded must not be suggested")
        }
    }

    /**
     * Pins that a non-existent modpack directory fails the modpack-directory check.
     */
    @Test
    fun modpackDirCheckFailsForMissingDirectory(@TempDir tempDir: File) {
        val configCheck = configurationHandler.checkModpackDir(
            File(tempDir, "does_not_exist").absolutePath
        )
        Assertions.assertFalse(configCheck.modpackChecksPassed)
    }

    /**
     * Pins config-generation from a manifest-less modpack directory: the modpack dir is taken
     * as-is and the default include-directories present in the modpack become inclusions.
     */
    @Test
    fun generateConfigFromManifestLessModpackUsesDefaultInclusions() {
        val modpack = File("src/test/resources/forge_tests")
        val packConfig = configurationHandler.generateConfigFromModpack(modpack)
        Assertions.assertEquals(modpack.absolutePath, packConfig.modpackDir)
        val sources = packConfig.inclusions.map { inclusion -> inclusion.source }
        Assertions.assertTrue(sources.contains("config"))
        Assertions.assertTrue(sources.contains("mods"))
    }
}
