package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.config.ExclusionFilter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ApiPropertiesTest internal constructor() {
    @Test
    fun test() {
        val propFiles = listOf(
            /*0*/File("src/test/resources/testresources/properties/filters/contains.properties"),
            /*1*/File("src/test/resources/testresources/properties/filters/either.properties"),
            /*2*/File("src/test/resources/testresources/properties/filters/end.properties"),
            /*3*/File("src/test/resources/testresources/properties/filters/regex.properties"),
            /*4*/File("src/test/resources/testresources/properties/filters/start.properties"),
            /*5*/File("src/test/resources/serverpackcreator.properties")
        )
        val apiProperties = ApiProperties(File("src/test/resources/serverpackcreator.properties"))
        apiProperties.clearPropertyFileList()

        apiProperties.loadOverrides(propFiles[0])
        Assertions.assertEquals(
            ExclusionFilter.CONTAIN,
            apiProperties.exclusionFilter
        )

        apiProperties.loadOverrides(propFiles[1])
        Assertions.assertEquals(
            ExclusionFilter.EITHER,
            apiProperties.exclusionFilter
        )

        apiProperties.loadOverrides(propFiles[2])
        Assertions.assertEquals(
            ExclusionFilter.END,
            apiProperties.exclusionFilter
        )

        apiProperties.loadOverrides(propFiles[3])
        Assertions.assertEquals(
            ExclusionFilter.REGEX,
            apiProperties.exclusionFilter
        )

        apiProperties.loadOverrides(propFiles[4])
        Assertions.assertEquals(
            ExclusionFilter.START,
            apiProperties.exclusionFilter
        )

        apiProperties.loadOverrides(propFiles[5])
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
            File(apiProperties.homeDirectory, "modpacks"),
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
        Assertions.assertEquals("dev", apiProperties.apiVersion)
    }
}