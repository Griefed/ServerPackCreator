package de.griefed.serverpackcreator.cli

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.cli.InteractiveCommandLine
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class InteractiveCommandLineTest {
    private val apiWrapper = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))
    private val interactiveCommandLine = InteractiveCommandLine(apiWrapper, UpdateChecker(apiWrapper.apiProperties))

    @Test
    fun feelingLuckyTest() {
        val modpacks = listOf(
            Triple("src/test/resources/modpacks/fabric_tests","src/test/resources/server-packs/fabric","Fabric-1.20.6"),
            Triple("src/test/resources/modpacks/forge_tests","src/test/resources/server-packs/forge","Forge-1-20-6"),
            /*Triple("src/test/resources/modpacks/legacyfabric_tests","src/test/resources/server-packs/legacyfabric","legacyfabric_tests"),*/
            Triple("src/test/resources/modpacks/neoforge_tests","src/test/resources/server-packs/neoforge","NeoForge-1.21"),
            Triple("src/test/resources/modpacks/quilt_tests","src/test/resources/server-packs/quilt","Quilt")
        )
        for (modpack in modpacks) {
            interactiveCommandLine.cliCommands.feelingLucky(modpack.first,null)
            var serverPackDir = File(apiWrapper.apiProperties.serverPacksDirectory,modpack.third)
            var serverPackFiles = serverPackDir.listFiles()
            Assertions.assertTrue(serverPackDir.isDirectory)
            Assertions.assertTrue(serverPackFiles.isNotEmpty())
            Assertions.assertTrue(File(apiWrapper.apiProperties.serverPacksDirectory,"${modpack.third}_server_pack.zip").isFile)
            Assertions.assertTrue(File(serverPackDir, "config").isDirectory)
            Assertions.assertTrue(File(serverPackDir, "defaultconfigs").isDirectory)
            Assertions.assertTrue(File(serverPackDir, "mods").isDirectory)
            Assertions.assertTrue(File(serverPackDir, "start.ps1").isFile)
            Assertions.assertTrue(File(serverPackDir, "start.sh").isFile)
            Assertions.assertTrue(File(serverPackDir, "start.bat").isFile)
            Assertions.assertTrue(File(serverPackDir, "variables.txt").isFile)

            interactiveCommandLine.cliCommands.feelingLucky(modpack.first,modpack.second)
            serverPackDir = File(modpack.second)
            serverPackFiles = serverPackDir.listFiles()
            Assertions.assertTrue(serverPackDir.isDirectory)
            Assertions.assertTrue(serverPackFiles.isNotEmpty())
            Assertions.assertTrue(File("${modpack.second}_server_pack.zip").isFile)
            Assertions.assertTrue(File(serverPackDir, "config").isDirectory)
            Assertions.assertTrue(File(serverPackDir, "defaultconfigs").isDirectory)
            Assertions.assertTrue(File(serverPackDir, "mods").isDirectory)
            Assertions.assertTrue(File(serverPackDir, "start.ps1").isFile)
            Assertions.assertTrue(File(serverPackDir, "start.sh").isFile)
            Assertions.assertTrue(File(serverPackDir, "start.bat").isFile)
            Assertions.assertTrue(File(serverPackDir, "variables.txt").isFile)
        }
    }

    @Test
    fun configGenGenerateFromModpackTest() {
        val modpacks = listOf(
            Pair("src/test/resources/modpacks/fabric_tests","Fabric-1.20.6"),
            Pair("src/test/resources/modpacks/forge_tests","Forge-1-20-6"),
            Pair("src/test/resources/modpacks/neoforge_tests","NeoForge-1.21"),
            Pair("src/test/resources/modpacks/quilt_tests","Quilt")
        )
        for (modpack in modpacks) {
            interactiveCommandLine.configGenCommand.generateConfFromModpack(Optional.of(File(modpack.first)))
            val config = File(apiWrapper.apiProperties.configsDirectory,"${modpack.second}.conf")
            val content = config.readText()
            Assertions.assertTrue(File(apiWrapper.apiProperties.configsDirectory,"${modpack.second}.conf").isFile)
            Assertions.assertTrue(content.isNotBlank())
        }
    }

    @Test
    fun runHeadlessTest() {
        File("src/test/resources/configs/serverpackcreator.conf").copyTo(
            apiWrapper.apiProperties.defaultConfig,
            overwrite = true
        )
        interactiveCommandLine.runHeadlessCommand.runHeadless()
        var serverPackDir = File(apiWrapper.apiProperties.serverPacksDirectory,"Forge-1-20-6")
        var serverPackFiles = serverPackDir.listFiles()
        Assertions.assertTrue(serverPackDir.isDirectory)
        Assertions.assertTrue(serverPackFiles.isNotEmpty())
        Assertions.assertTrue(File("${serverPackDir}_server_pack.zip").isFile)
        Assertions.assertTrue(File(serverPackDir, "config").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "defaultconfigs").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "mods").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "start.ps1").isFile)
        Assertions.assertTrue(File(serverPackDir, "start.sh").isFile)
        Assertions.assertTrue(File(serverPackDir, "start.bat").isFile)
        Assertions.assertTrue(File(serverPackDir, "variables.txt").isFile)
    }

    @Test
    fun runHeadlessWithSpecificConfigTest() {
        interactiveCommandLine.runHeadlessCommand.runHeadless(File("src/test/resources/configs/serverpackcreator.conf"))
        var serverPackDir = File(apiWrapper.apiProperties.serverPacksDirectory,"Forge-1-20-6")
        var serverPackFiles = serverPackDir.listFiles()
        Assertions.assertTrue(serverPackDir.isDirectory)
        Assertions.assertTrue(serverPackFiles.isNotEmpty())
        Assertions.assertTrue(File("${serverPackDir}_server_pack.zip").isFile)
        Assertions.assertTrue(File(serverPackDir, "config").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "defaultconfigs").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "mods").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "start.ps1").isFile)
        Assertions.assertTrue(File(serverPackDir, "start.sh").isFile)
        Assertions.assertTrue(File(serverPackDir, "start.bat").isFile)
        Assertions.assertTrue(File(serverPackDir, "variables.txt").isFile)

        interactiveCommandLine.runHeadlessCommand.runHeadless(File(
            "src/test/resources/configs/serverpackcreator.conf"),
            Optional.of(File("src/test/resources/server-packs/forge")))
        serverPackDir = File("src/test/resources/server-packs/forge")
        serverPackFiles = serverPackDir.listFiles()
        Assertions.assertTrue(serverPackDir.isDirectory)
        Assertions.assertTrue(serverPackFiles.isNotEmpty())
        Assertions.assertTrue(File("${serverPackDir}_server_pack.zip").isFile)
        Assertions.assertTrue(File(serverPackDir, "config").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "defaultconfigs").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "mods").isDirectory)
        Assertions.assertTrue(File(serverPackDir, "start.ps1").isFile)
        Assertions.assertTrue(File(serverPackDir, "start.sh").isFile)
        Assertions.assertTrue(File(serverPackDir, "start.bat").isFile)
        Assertions.assertTrue(File(serverPackDir, "variables.txt").isFile)
    }
}