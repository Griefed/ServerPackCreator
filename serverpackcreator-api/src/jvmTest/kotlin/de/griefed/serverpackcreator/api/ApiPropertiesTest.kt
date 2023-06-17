package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import de.griefed.serverpackcreator.api.utilities.common.ListUtilities
import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ApiPropertiesTest internal constructor() {
    private val fileUtilities: FileUtilities =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).fileUtilities
    private val systemUtilities: SystemUtilities =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).systemUtilities
    private val listUtilities: ListUtilities =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).listUtilities
    private val jarUtilities: JarUtilities =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).jarUtilities

    @Test
    fun test() {
        var apiProperties = ApiProperties(
            fileUtilities, systemUtilities, listUtilities, jarUtilities,
            File("src/jvmTest/resources/testresources/properties/filters/contains.properties")
        )
        Assertions.assertEquals(
            ExclusionFilter.CONTAIN,
            apiProperties.exclusionFilter
        )
        apiProperties = ApiProperties(
            fileUtilities, systemUtilities, listUtilities, jarUtilities,
            File("src/jvmTest/resources/testresources/properties/filters/either.properties")
        )
        Assertions.assertEquals(
            ExclusionFilter.EITHER,
            apiProperties.exclusionFilter
        )
        apiProperties = ApiProperties(
            fileUtilities, systemUtilities, listUtilities, jarUtilities,
            File("src/jvmTest/resources/testresources/properties/filters/end.properties")
        )
        Assertions.assertEquals(
            ExclusionFilter.END,
            apiProperties.exclusionFilter
        )
        apiProperties = ApiProperties(
            fileUtilities, systemUtilities, listUtilities, jarUtilities,
            File("src/jvmTest/resources/testresources/properties/filters/regex.properties")
        )
        Assertions.assertEquals(
            ExclusionFilter.REGEX,
            apiProperties.exclusionFilter
        )
        apiProperties = ApiProperties(
            fileUtilities, systemUtilities, listUtilities, jarUtilities,
            File("src/jvmTest/resources/testresources/properties/filters/start.properties")
        )
        Assertions.assertEquals(
            ExclusionFilter.START,
            apiProperties.exclusionFilter
        )
        apiProperties = ApiProperties(
            fileUtilities, systemUtilities, listUtilities, jarUtilities,
            File("src/jvmTest/resources/serverpackcreator.properties")
        )
        Assertions.assertNotNull(apiProperties.javaPath)
        Assertions.assertTrue(
            File(apiProperties.javaPath).exists(),
            "Java not found: ${File(apiProperties.javaPath).path}"
        )
        Assertions.assertNotNull(apiProperties.serverPackCreatorPropertiesFile)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "serverpackcreator.properties"),
            apiProperties.serverPackCreatorPropertiesFile
        )
        Assertions.assertNotNull(apiProperties.manifestsDirectory)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "manifests"),
            apiProperties.manifestsDirectory
        )
        Assertions.assertNotNull(apiProperties.minecraftServerManifestsDirectory)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "manifests${File.separator}mcserver"),
            apiProperties.minecraftServerManifestsDirectory
        )
        Assertions.assertNotNull(apiProperties.fabricIntermediariesManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory,
                "manifests${File.separator}fabric-intermediaries-manifest.json"
            ),
            apiProperties.fabricIntermediariesManifest
        )
        Assertions.assertNotNull(apiProperties.legacyFabricGameManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory,
                "manifests${File.separator}legacy-fabric-game-manifest.json"
            ),
            apiProperties.legacyFabricGameManifest
        )
        Assertions.assertNotNull(apiProperties.legacyFabricLoaderManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory,
                "manifests${File.separator}legacy-fabric-loader-manifest.json"
            ),
            apiProperties.legacyFabricLoaderManifest
        )
        Assertions.assertNotNull(apiProperties.legacyFabricInstallerManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory,
                "manifests${File.separator}legacy-fabric-installer-manifest.xml"
            ),
            apiProperties.legacyFabricInstallerManifest
        )
        Assertions.assertNotNull(apiProperties.fabricInstallerManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory, "manifests${File.separator}fabric-installer-manifest.xml"
            ),
            apiProperties.fabricInstallerManifest
        )
        Assertions.assertNotNull(apiProperties.quiltVersionManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory, "manifests${File.separator}quilt-manifest.xml"
            ),
            apiProperties.quiltVersionManifest
        )
        Assertions.assertNotNull(apiProperties.quiltInstallerManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory, "manifests${File.separator}quilt-installer-manifest.xml"
            ),
            apiProperties.quiltInstallerManifest
        )
        Assertions.assertNotNull(apiProperties.forgeVersionManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory, "manifests${File.separator}forge-manifest.json"
            ),
            apiProperties.forgeVersionManifest
        )
        Assertions.assertNotNull(apiProperties.fabricVersionManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory, "manifests${File.separator}fabric-manifest.xml"
            ),
            apiProperties.fabricVersionManifest
        )
        Assertions.assertNotNull(apiProperties.minecraftVersionManifest)
        Assertions.assertEquals(
            File(
                apiProperties.homeDirectory, "manifests${File.separator}minecraft-manifest.json"
            ),
            apiProperties.minecraftVersionManifest
        )
        Assertions.assertNotNull(apiProperties.workDirectory)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "work"),
            apiProperties.workDirectory
        )
        Assertions.assertNotNull(apiProperties.tempDirectory)
        Assertions.assertEquals(
            File(
                apiProperties.workDirectory, "temp"
            ),
            apiProperties.tempDirectory
        )
        Assertions.assertNotNull(apiProperties.modpacksDirectory)
        Assertions.assertEquals(
            File(apiProperties.tempDirectory, "modpacks"),
            apiProperties.modpacksDirectory
        )
        Assertions.assertNotNull(apiProperties.logsDirectory)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "logs"),
            apiProperties.logsDirectory
        )
        Assertions.assertNotNull(apiProperties.defaultConfig)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "serverpackcreator.conf"),
            apiProperties.defaultConfig
        )
        Assertions.assertNotNull(apiProperties.serverFilesDirectory)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "server_files"),
            apiProperties.serverFilesDirectory
        )
        Assertions.assertNotNull(apiProperties.defaultServerProperties)
        Assertions.assertEquals(
            File(apiProperties.serverFilesDirectory, "server.properties"),
            apiProperties.defaultServerProperties
        )
        Assertions.assertNotNull(apiProperties.defaultServerIcon)
        Assertions.assertEquals(
            File(apiProperties.serverFilesDirectory, "server-icon.png"),
            apiProperties.defaultServerIcon
        )
        Assertions.assertNotNull(apiProperties.serverPackCreatorDatabase)
        Assertions.assertNotNull(apiProperties.pluginsDirectory)
        Assertions.assertEquals(
            File(apiProperties.homeDirectory, "plugins"),
            apiProperties.pluginsDirectory
        )
        Assertions.assertNotNull(apiProperties.pluginsConfigsDirectory)
        Assertions.assertEquals(
            File(apiProperties.pluginsDirectory, "config"),
            apiProperties.pluginsConfigsDirectory
        )
        Assertions.assertNotNull(apiProperties.serverPacksDirectory)
        Assertions.assertNotNull(apiProperties.directoriesToExclude)
        Assertions.assertFalse(apiProperties.isCheckingForPreReleasesEnabled)
        Assertions.assertEquals(90, apiProperties.queueMaxDiskUsage)
        Assertions.assertEquals("dev", apiProperties.apiVersion)
    }
}