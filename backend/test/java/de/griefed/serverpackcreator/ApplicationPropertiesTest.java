package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.ApplicationProperties.ExclusionFilter;
import de.griefed.serverpackcreator.utilities.common.FileUtilities;
import de.griefed.serverpackcreator.utilities.common.JarUtilities;
import de.griefed.serverpackcreator.utilities.common.ListUtilities;
import de.griefed.serverpackcreator.utilities.common.SystemUtilities;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPropertiesTest {

  private final FileUtilities fileUtilities = new FileUtilities();
  private final SystemUtilities systemUtilities = new SystemUtilities();
  private final ListUtilities listUtilities = new ListUtilities();
  private final JarUtilities jarUtilities = new JarUtilities();

  ApplicationPropertiesTest() {

  }

  @Test
  void test() {
    ApplicationProperties applicationProperties;

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/contains.properties"),
        fileUtilities, systemUtilities, listUtilities, jarUtilities);

    Assertions.assertEquals(
        ExclusionFilter.CONTAIN,
        applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/either.properties"),
        fileUtilities, systemUtilities, listUtilities, jarUtilities);

    Assertions.assertEquals(
        ExclusionFilter.EITHER,
        applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/end.properties"),
        fileUtilities, systemUtilities, listUtilities, jarUtilities);

    Assertions.assertEquals(
        ExclusionFilter.END,
        applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/regex.properties"),
        fileUtilities, systemUtilities, listUtilities, jarUtilities);

    Assertions.assertEquals(
        ExclusionFilter.REGEX,
        applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/start.properties"),
        fileUtilities, systemUtilities, listUtilities, jarUtilities);

    Assertions.assertEquals(
        ExclusionFilter.START,
        applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/serverpackcreator.properties"), fileUtilities,
        systemUtilities, listUtilities, jarUtilities);

    Assertions.assertNotNull(applicationProperties.java());
    Assertions.assertTrue(new File(applicationProperties.java()).exists());

    Assertions.assertNotNull(applicationProperties.serverPackCreatorPropertiesFile());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "serverpackcreator.properties"),
        applicationProperties.serverPackCreatorPropertiesFile()
    );

    Assertions.assertNotNull(applicationProperties.manifestsDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "manifests"),
        applicationProperties.manifestsDirectory()
    );

    Assertions.assertNotNull(applicationProperties.minecraftServerManifestsDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "manifests" + File.separator + "mcserver"),
        applicationProperties.minecraftServerManifestsDirectory()
    );

    Assertions.assertNotNull(applicationProperties.fabricIntermediariesManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(),
                 "manifests" + File.separator + "fabric-intermediaries-manifest.json"),
        applicationProperties.fabricIntermediariesManifest()
    );

    Assertions.assertNotNull(applicationProperties.legacyFabricGameManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(),
                 "manifests" + File.separator + "legacy-fabric-game-manifest.json"),
        applicationProperties.legacyFabricGameManifest()
    );

    Assertions.assertNotNull(applicationProperties.legacyFabricLoaderManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(),
                 "manifests" + File.separator + "legacy-fabric-loader-manifest.json"),
        applicationProperties.legacyFabricLoaderManifest()
    );

    Assertions.assertNotNull(applicationProperties.legacyFabricInstallerManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(),
                 "manifests" + File.separator + "legacy-fabric-installer-manifest.xml"),
        applicationProperties.legacyFabricInstallerManifest()
    );

    Assertions.assertNotNull(applicationProperties.fabricInstallerManifest());
    Assertions.assertEquals(
        new File(
            applicationProperties.homeDirectory(), "manifests" + File.separator
            + "fabric-installer-manifest.xml"),
        applicationProperties.fabricInstallerManifest()
    );

    Assertions.assertNotNull(applicationProperties.quiltVersionManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "manifests" + File.separator
            + "quilt-manifest.xml"),
        applicationProperties.quiltVersionManifest()
    );

    Assertions.assertNotNull(applicationProperties.quiltInstallerManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "manifests" + File.separator
            + "quilt-installer-manifest.xml"),
        applicationProperties.quiltInstallerManifest()
    );

    Assertions.assertNotNull(applicationProperties.forgeVersionManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "manifests" + File.separator
            + "forge-manifest.json"),
        applicationProperties.forgeVersionManifest()
    );

    Assertions.assertNotNull(applicationProperties.fabricVersionManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "manifests" + File.separator
            + "fabric-manifest.xml"),
        applicationProperties.fabricVersionManifest()
    );

    Assertions.assertNotNull(applicationProperties.minecraftVersionManifest());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "manifests" + File.separator
            + "minecraft-manifest.json"),
        applicationProperties.minecraftVersionManifest()
    );

    Assertions.assertNotNull(applicationProperties.workDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "work"),
        applicationProperties.workDirectory()
    );

    Assertions.assertNotNull(applicationProperties.tempDirectory());
    Assertions.assertEquals(
        new File(
            applicationProperties.workDirectory(), "temp"),
        applicationProperties.tempDirectory()
    );

    Assertions.assertNotNull(applicationProperties.modpacksDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.tempDirectory(), "modpacks"),
        applicationProperties.modpacksDirectory()
    );

    Assertions.assertNotNull(applicationProperties.logsDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "logs"),
        applicationProperties.logsDirectory()
    );

    Assertions.assertNotNull(applicationProperties.defaultConfig());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "serverpackcreator.conf"),
        applicationProperties.defaultConfig());

    Assertions.assertNotNull(applicationProperties.serverFilesDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "server_files"),
        applicationProperties.serverFilesDirectory()
    );

    Assertions.assertNotNull(applicationProperties.defaultServerProperties());
    Assertions.assertEquals(
        new File(applicationProperties.serverFilesDirectory(), "server.properties"),
        applicationProperties.defaultServerProperties());

    Assertions.assertNotNull(applicationProperties.defaultServerIcon());
    Assertions.assertEquals(
        new File(applicationProperties.serverFilesDirectory(), "server-icon.png"),
        applicationProperties.defaultServerIcon());

    Assertions.assertNotNull(applicationProperties.serverPackCreatorDatabase());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "serverpackcreator.db"),
        applicationProperties.serverPackCreatorDatabase());

    Assertions.assertNotNull(applicationProperties.addonsDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.homeDirectory(), "plugins"),
        applicationProperties.addonsDirectory()
    );

    Assertions.assertNotNull(applicationProperties.addonConfigsDirectory());
    Assertions.assertEquals(
        new File(applicationProperties.addonsDirectory(), "config"),
        applicationProperties.addonConfigsDirectory()
    );

    Assertions.assertNotNull(applicationProperties.serverPacksDirectory());

    Assertions.assertNotNull(applicationProperties.getDirectoriesToExclude());
    applicationProperties.addDirectoryToExclude("test");
    Assertions.assertTrue(applicationProperties.getDirectoriesToExclude().contains("test"));

    Assertions.assertFalse(applicationProperties.getSaveLoadedConfiguration());
    Assertions.assertFalse(applicationProperties.checkForAvailablePreReleases());

    Assertions.assertEquals(90, applicationProperties.getQueueMaxDiskUsage());

    Assertions.assertEquals("dev", applicationProperties.serverPackCreatorVersion());
  }
}
