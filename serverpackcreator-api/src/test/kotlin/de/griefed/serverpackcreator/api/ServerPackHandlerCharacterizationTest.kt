package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.serverpack.ServerPackFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.imageio.ImageIO

/**
 * Characterization tests pinning the current behavior of [de.griefed.serverpackcreator.api.serverpack.ServerPackHandler]
 * before its decomposition into explicit pipeline steps (refactor Phase 1d). These tests document
 * file-gathering, cleanup, icon/properties handling and placeholder replacement as-is, so the
 * upcoming restructuring can be verified to be behavior-preserving.
 */
internal class ServerPackHandlerCharacterizationTest {
    private val serverPackHandler =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).serverPackHandler
    private val apiProperties =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).apiProperties

    /**
     * Pins destination computation: pack name plus suffix, spaces replaced with underscores,
     * resolved inside the server-packs directory.
     */
    @Test
    fun destinationReplacesSpacesAndAppendsSuffix() {
        val packConfig = PackConfig()
        packConfig.name = "My Test Pack"
        packConfig.serverPackSuffix = "-server"
        val destination = serverPackHandler.getServerPackDestination(packConfig)
        Assertions.assertEquals(
            File(apiProperties.serverPacksDirectory, "My_Test_Pack-server").absolutePath,
            destination
        )
    }

    /**
     * Pins that the modpack-directory name is used as destination when no pack name is set.
     */
    @Test
    fun destinationFallsBackToModpackDirectoryName() {
        val packConfig = PackConfig()
        packConfig.modpackDir = File("src/test/resources/forge_tests").absolutePath
        val destination = serverPackHandler.getServerPackDestination(packConfig)
        Assertions.assertEquals(
            File(apiProperties.serverPacksDirectory, "forge_tests").absolutePath,
            destination
        )
    }

    /**
     * Pins that pre-installation cleanup deletes every configured leftover file or directory
     * from the given destination.
     */
    @Test
    fun preInstallationCleanupDeletesConfiguredLeftovers(@TempDir tempDir: File) {
        File(tempDir, "server.jar").writeText("dummy")
        File(tempDir, "libraries").mkdirs()
        File(tempDir, "libraries/keepsake.jar").writeText("dummy")
        serverPackHandler.preInstallationCleanup(tempDir.absolutePath)
        Assertions.assertFalse(File(tempDir, "server.jar").exists())
        Assertions.assertFalse(File(tempDir, "libraries").exists())
    }

    /**
     * Pins that post-installation cleanup deletes configured installer leftovers but keeps
     * unrelated files.
     */
    @Test
    fun postInstallCleanupDeletesInstallerLeftovers(@TempDir tempDir: File) {
        File(tempDir, "forge-installer.jar").writeText("dummy")
        File(tempDir, "unrelated.txt").writeText("keep me")
        serverPackHandler.postInstallCleanup(tempDir.absolutePath)
        Assertions.assertFalse(File(tempDir, "forge-installer.jar").exists())
        Assertions.assertTrue(File(tempDir, "unrelated.txt").exists())
    }

    /**
     * Pins explicit file-gathering for all four source-variants: file/directory relative to the
     * modpack and file/directory by absolute path.
     */
    @Test
    fun explicitFilesGathersFilesAndDirectories(@TempDir tempDir: File) {
        val modpack = File(tempDir, "modpack")
        val serverPack = File(tempDir, "serverpack")
        modpack.mkdirs()
        File(modpack, "some.file").writeText("content")
        File(modpack, "somedir").mkdirs()
        File(modpack, "somedir/inner.txt").writeText("content")
        val external = File(tempDir, "external")
        external.mkdirs()
        File(external, "external.txt").writeText("content")

        val relativeFile = serverPackHandler.getExplicitFiles(
            "some.file", "renamed.file", modpack.absolutePath, serverPack.absolutePath
        )
        Assertions.assertEquals(1, relativeFile.size)
        Assertions.assertEquals(File(serverPack, "renamed.file"), relativeFile.first().destinationFile)

        val relativeDirectory = serverPackHandler.getExplicitFiles(
            "somedir", "targetdir", modpack.absolutePath, serverPack.absolutePath
        )
        Assertions.assertTrue(
            relativeDirectory.any { serverPackFile -> serverPackFile.sourceFile.name == "inner.txt" },
            "Directory contents must be gathered"
        )

        val absoluteFile = serverPackHandler.getExplicitFiles(
            File(external, "external.txt").absolutePath, "external.txt", modpack.absolutePath, serverPack.absolutePath
        )
        Assertions.assertEquals(1, absoluteFile.size)

        val absoluteDirectory = serverPackHandler.getExplicitFiles(
            external.absolutePath, "external", modpack.absolutePath, serverPack.absolutePath
        )
        Assertions.assertTrue(
            absoluteDirectory.any { serverPackFile -> serverPackFile.sourceFile.name == "external.txt" },
            "Absolute directory contents must be gathered"
        )
    }

    /**
     * Pins save-file gathering: the world-directory contents map into the destination under the
     * directory-spec with its leading "saves/" stripped.
     */
    @Test
    fun saveFilesMapWorldIntoDestinationWithoutSavesPrefix(@TempDir tempDir: File) {
        val world = File(tempDir, "saves/myworld")
        world.mkdirs()
        File(world, "level.dat").writeText("dummy")
        val destination = File(tempDir, "serverpack").absolutePath

        val saveFiles = serverPackHandler.getSaveFiles(world.absolutePath, "saves/myworld", destination)

        Assertions.assertTrue(
            saveFiles.any { serverPackFile ->
                serverPackFile.destinationFile == File(destination, "myworld/level.dat")
            },
            "World files must land in <destination>/myworld; got ${saveFiles.map { serverPackFile -> serverPackFile.destinationFile }}"
        )
    }

    /**
     * Pins regex-walking: only files matching the regex relative to the source-directory are
     * gathered, destined for a directory named after the source.
     */
    @Test
    fun regexWalkGathersOnlyMatchingFiles(@TempDir tempDir: File) {
        val source = File(tempDir, "walkme")
        source.mkdirs()
        File(source, "match.txt").writeText("dummy")
        File(source, "ignore.ogg").writeText("dummy")
        val destination = File(tempDir, "serverpack").absolutePath
        val gathered: MutableList<ServerPackFile> = mutableListOf()

        serverPackHandler.regexWalk(source, destination, Regex(".*\\.txt"), gathered)

        Assertions.assertEquals(1, gathered.size)
        Assertions.assertEquals(File(destination, "walkme/match.txt").absolutePath, gathered.first().destinationFile.absolutePath)
    }

    /**
     * Pins clientside-mod exclusion during mod-list compilation: a mod matching the
     * clientside-list is excluded while a whitelisted mod stays included.
     */
    @Test
    fun compileModListExcludesClientsideAndKeepsWhitelisted() {
        val packConfig = PackConfig()
        packConfig.modpackDir = File("src/test/resources/forge_tests").absolutePath
        packConfig.minecraftVersion = "1.16.5"
        packConfig.modloader = "Forge"
        packConfig.setClientMods(arrayListOf("Ping-"))
        packConfig.setModsWhitelist(arrayListOf("Ping-Wheel-"))

        val (includedMods, excludedMods) = serverPackHandler.compileModList(packConfig)
        val includedNames = includedMods.map { mod -> mod.name }

        Assertions.assertFalse(includedNames.contains("Ping-1.19-1.9.1.jar"), "Clientside mod must be excluded")
        Assertions.assertTrue(includedNames.contains("Ping-Wheel-1.6.1-forge-1.20.1.jar"), "Whitelisted mod must stay")
        Assertions.assertTrue(
            excludedMods.any { mod -> mod.name == "Ping-1.19-1.9.1.jar" },
            "Excluded mod must be reported in the second list; got ${excludedMods.map { mod -> mod.name }}"
        )
    }

    /**
     * Pins placeholder replacement for non-local (zipped) server packs: the Java-placeholder
     * becomes plain "java" and the restart-placeholder becomes "true".
     */
    @Test
    fun placeholderReplacementForZippedPackUsesPlainJavaAndRestart() {
        val scriptSettings = hashMapOf(
            "SPC_JAVA_SPC" to "/usr/lib/jvm/java-21/bin/java",
            "SPC_RESTART_SPC" to "false"
        )
        val replaced = serverPackHandler.replacePlaceholders(
            false, "JAVA=SPC_JAVA_SPC RESTART=SPC_RESTART_SPC", scriptSettings
        )
        Assertions.assertEquals("JAVA=java RESTART=true", replaced)
    }

    /**
     * Pins placeholder replacement for local server packs: the configured Java-path and
     * restart-setting are used verbatim.
     */
    @Test
    fun placeholderReplacementForLocalPackUsesConfiguredValues() {
        val scriptSettings = hashMapOf(
            "SPC_JAVA_SPC" to "/usr/lib/jvm/java-21/bin/java",
            "SPC_RESTART_SPC" to "false"
        )
        val replaced = serverPackHandler.replacePlaceholders(
            true, "JAVA=SPC_JAVA_SPC RESTART=SPC_RESTART_SPC", scriptSettings
        )
        Assertions.assertEquals("JAVA=/usr/lib/jvm/java-21/bin/java RESTART=false", replaced)
    }

    /**
     * Pins icon handling: an empty icon-path copies the default server-icon, a non-64x64 custom
     * icon is scaled to 64x64, and a non-existent path produces no icon at all.
     */
    @Test
    fun copyIconHandlesDefaultCustomAndMissingIcon(@TempDir tempDir: File) {
        val defaultDestination = File(tempDir, "default")
        defaultDestination.mkdirs()
        serverPackHandler.copyIcon(defaultDestination.absolutePath, "")
        Assertions.assertTrue(File(defaultDestination, apiProperties.defaultServerIcon.name).isFile)

        val customDestination = File(tempDir, "custom")
        customDestination.mkdirs()
        serverPackHandler.copyIcon(
            customDestination.absolutePath,
            File("src/test/resources/testresources/SCP_icon.png").absolutePath
        )
        val scaledIcon = File(customDestination, apiProperties.defaultServerIcon.name)
        Assertions.assertTrue(scaledIcon.isFile)
        val image = ImageIO.read(scaledIcon)
        Assertions.assertEquals(64, image.width)
        Assertions.assertEquals(64, image.height)

        val missingDestination = File(tempDir, "missing")
        missingDestination.mkdirs()
        serverPackHandler.copyIcon(missingDestination.absolutePath, File(tempDir, "no_such_icon.png").absolutePath)
        Assertions.assertFalse(File(missingDestination, apiProperties.defaultServerIcon.name).exists())
    }

    /**
     * Pins properties handling: an empty path copies the default server.properties, a
     * non-existent path produces no file.
     */
    @Test
    fun copyPropertiesHandlesDefaultAndMissingProperties(@TempDir tempDir: File) {
        val defaultDestination = File(tempDir, "default")
        defaultDestination.mkdirs()
        serverPackHandler.copyProperties(defaultDestination.absolutePath, "")
        Assertions.assertTrue(File(defaultDestination, apiProperties.defaultServerProperties.name).isFile)

        val missingDestination = File(tempDir, "missing")
        missingDestination.mkdirs()
        serverPackHandler.copyProperties(
            missingDestination.absolutePath,
            File(tempDir, "no_such.properties").absolutePath
        )
        Assertions.assertFalse(File(missingDestination, apiProperties.defaultServerProperties.name).exists())
    }

    /**
     * Pins that an unknown modloader is never considered downloadable.
     */
    @Test
    fun unknownModloaderIsNotDownloadable() {
        Assertions.assertFalse(serverPackHandler.serverDownloadable("1.20.1", "UnknownLoader", "1.0.0"))
    }
}
