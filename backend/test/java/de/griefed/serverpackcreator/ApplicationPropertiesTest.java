package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.ApplicationProperties.ExclusionFilter;
import de.griefed.serverpackcreator.utilities.common.FileUtilities;
import de.griefed.serverpackcreator.utilities.common.ListUtilities;
import de.griefed.serverpackcreator.utilities.common.SystemUtilities;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationPropertiesTest {
  private final FileUtilities fileUtilities = new FileUtilities();
  private final SystemUtilities systemUtilities = new SystemUtilities();
  private final ListUtilities listUtilities = new ListUtilities();

  ApplicationPropertiesTest() {

  }

  @Test
  void test() {
    ApplicationProperties applicationProperties;

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/contains.properties"),
        fileUtilities, systemUtilities, listUtilities);

    Assertions.assertEquals(ExclusionFilter.CONTAIN, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/either.properties"),
        fileUtilities, systemUtilities, listUtilities);

    Assertions.assertEquals(ExclusionFilter.EITHER, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/end.properties"),
        fileUtilities, systemUtilities, listUtilities);

    Assertions.assertEquals(ExclusionFilter.END, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/regex.properties"),
        fileUtilities, systemUtilities, listUtilities);

    Assertions.assertEquals(ExclusionFilter.REGEX, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/testresources/properties/filters/start.properties"),
        fileUtilities, systemUtilities, listUtilities);

    Assertions.assertEquals(ExclusionFilter.START, applicationProperties.exclusionFilter());

    applicationProperties = new ApplicationProperties(
        new File("backend/test/resources/serverpackcreator.properties"), fileUtilities,
        systemUtilities, listUtilities);

    Assertions.assertNotNull(applicationProperties.java());
    Assertions.assertTrue(new File(applicationProperties.java()).exists());

    Assertions.assertNotNull(applicationProperties.FALLBACK_CLIENTSIDE_MODS());
    Assertions.assertNotNull(applicationProperties.FALLBACK_REGEX_CLIENTSIDE_MODS());

    Assertions.assertNotNull(applicationProperties.getDefaultListFallbackMods());
    applicationProperties.setProperty(
        "de.griefed.serverpackcreator.configuration.fallbackmodslist", "bla");
    applicationProperties.updateFallback();
    Assertions.assertNotEquals(
        applicationProperties.getProperty(
            "de.griefed.serverpackcreator.configuration.fallbackmodslist"),
        "bla");

    Assertions.assertNotNull(applicationProperties.DEFAULT_CONFIG());
    Assertions.assertEquals(
        applicationProperties.DEFAULT_CONFIG(), new File("serverpackcreator.conf"));

    Assertions.assertNotNull(applicationProperties.DEFAULT_SERVER_PROPERTIES());
    Assertions.assertEquals(
        applicationProperties.DEFAULT_SERVER_PROPERTIES(), new File("server.properties"));

    Assertions.assertNotNull(applicationProperties.DEFAULT_SERVER_ICON());
    Assertions.assertEquals(
        applicationProperties.DEFAULT_SERVER_ICON(), new File("server-icon.png"));

    Assertions.assertNotNull(applicationProperties.MINECRAFT_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.MINECRAFT_VERSION_MANIFEST(), new File("minecraft-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FORGE_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.FORGE_VERSION_MANIFEST(), new File("forge-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FABRIC_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.FABRIC_VERSION_MANIFEST(), new File("fabric-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_GAME_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_GAME_MANIFEST(),
        new File("legacy-fabric-game-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST(),
        new File("legacy-fabric-loader-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST(),
        new File("legacy-fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST(),
        new File("fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_GAME_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_GAME_MANIFEST_LOCATION(),
        new File("./manifests/legacy-fabric-game-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST_LOCATION(),
        new File("./manifests/legacy-fabric-loader-manifest.json"));

    Assertions.assertNotNull(applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION(),
        new File("./manifests/legacy-fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.QUILT_VERSION_MANIFEST(), new File("quilt-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST());
    Assertions.assertEquals(
        applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST(),
        new File("quilt-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.SERVERPACKCREATOR_DATABASE());
    Assertions.assertEquals(
        applicationProperties.SERVERPACKCREATOR_DATABASE(), new File("serverpackcreator.db"));

    Assertions.assertNotNull(applicationProperties.MINECRAFT_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.MINECRAFT_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/minecraft-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FORGE_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FORGE_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/forge-manifest.json"));

    Assertions.assertNotNull(applicationProperties.FABRIC_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FABRIC_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/fabric-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/fabric-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.QUILT_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/quilt-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    Assertions.assertEquals(
        applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION(),
        new File("./manifests/quilt-installer-manifest.xml"));

    Assertions.assertNotNull(applicationProperties.getDirectoryServerPacks());

    Assertions.assertNotNull(applicationProperties.getDirectoriesToExclude());
    applicationProperties.addDirectoryToExclude("test");
    Assertions.assertTrue(applicationProperties.getDirectoriesToExclude().contains("test"));

    Assertions.assertFalse(applicationProperties.getSaveLoadedConfiguration());
    Assertions.assertFalse(applicationProperties.checkForAvailablePreReleases());

    Assertions.assertEquals(90, applicationProperties.getQueueMaxDiskUsage());

    Assertions.assertEquals("dev", applicationProperties.SERVERPACKCREATOR_VERSION());
  }
}
