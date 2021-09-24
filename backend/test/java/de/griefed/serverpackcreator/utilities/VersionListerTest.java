package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.DefaultFiles;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * <strong>Table of methods</strong><p>
 * 1. {@link #getMinecraftManifestTest()}<br>
 * 2. {@link #getForgeManifestTest()}<br>
 * 3. {@link #getFabricManifestTest()}<br>
 * 4. {@link #getObjectMapperTest()}<br>
 * 5. {@link #getMinecraftReleaseVersionTest()}<br>
 * 6. {@link #getMinecraftReleaseVersionsTest()}<br>
 * 7. {@link #getMinecraftSnapshotVersionTest()}<br>
 * 8. {@link #getMinecraftReleaseVersionsTest()}<br>
 * 9. {@link #getFabricVersionsTest()}<br>
 * 10.{@link #getFabricLatestVersionTest()}<br>
 * 11.{@link #getFabricReleaseVersionTest()}<br>
 * 12.{@link #getForgeVersionTest()}<br>
 * 13.{@link #getMinecraftReleaseVersionsAsArrayTest()}<br>
 * 14.{@link #getFabricVersionsAsArrayTest()}<br>
 * 15.{@link #getForgeVersionsAsArrayTest()}<br>
 * 16.{@link #getForgeMetaTest()}
 * 17.{@link #getFabricLatestInstallerVersionTest()}<br>
 * 18.{@link #getFabricReleaseInstallerVersionTest()}
 */
public class VersionListerTest {

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final DefaultFiles DEFAULTFILES;
    private final VersionLister VERSIONLISTER;
    private static final Logger LOG = LogManager.getLogger(VersionListerTest.class);
    private Properties serverPackCreatorProperties;

    public VersionListerTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            this.serverPackCreatorProperties = new Properties();
            this.serverPackCreatorProperties.load(inputStream);
        } catch (IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }
        LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        LOCALIZATIONMANAGER.init();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        this.VERSIONLISTER = new VersionLister(serverPackCreatorProperties);
    }

    @Test
    void getMinecraftManifestTest() {
        Assertions.assertNotNull(VERSIONLISTER.getMinecraftManifest());
        Assertions.assertEquals("./work/minecraft-manifest.json",VERSIONLISTER.getMinecraftManifest().toString().replace("\\","/"));
    }

    @Test
    void getForgeManifestTest() {
        Assertions.assertNotNull(VERSIONLISTER.getForgeManifest());
        Assertions.assertEquals("./work/forge-manifest.json",VERSIONLISTER.getForgeManifest().toString().replace("\\","/"));
    }

    @Test
    void getFabricManifestTest() {
        Assertions.assertNotNull(VERSIONLISTER.getFabricManifest());
        Assertions.assertEquals("./work/fabric-manifest.xml",VERSIONLISTER.getFabricManifest().toString().replace("\\","/"));
    }

    @Test
    void getObjectMapperTest() {
        Assertions.assertNotNull(VERSIONLISTER.getObjectMapper());
    }

    @Test
    void getMinecraftReleaseVersionTest() {
        Assertions.assertNotNull(VERSIONLISTER.getMinecraftReleaseVersion());
    }

    @Test
    void getMinecraftReleaseVersionsTest() {
        Assertions.assertNotNull(VERSIONLISTER.getMinecraftReleaseVersions());
    }

    @Test
    void getMinecraftSnapshotVersionTest() {
        Assertions.assertNotNull(VERSIONLISTER.getMinecraftSnapshotVersion());
    }

    @Test
    void getMinecraftSnapshotVersionsTest() {
        Assertions.assertNotNull(VERSIONLISTER.getMinecraftSnapshotVersions());
    }

    @Test
    void getFabricVersionsTest() {
        Assertions.assertNotNull(VERSIONLISTER.getFabricVersions());
    }

    @Test
    void getFabricLatestVersionTest() {
        Assertions.assertNotNull(VERSIONLISTER.getFabricLatestVersion());
    }

    @Test
    void getFabricReleaseVersionTest() {
        Assertions.assertNotNull(VERSIONLISTER.getFabricReleaseVersion());
    }

    @Test
    void getForgeVersionTest() {
        for (int i = 0; i < VERSIONLISTER.getMinecraftReleaseVersions().size(); i++) {
            LOG.debug("Minecraft version: " + VERSIONLISTER.getMinecraftReleaseVersions().get(i) + " Forge versions: " + VERSIONLISTER.getForgeVersionsList(VERSIONLISTER.getMinecraftReleaseVersions().get(i)));
            Assertions.assertNotNull(VERSIONLISTER.getForgeVersionsList(VERSIONLISTER.getMinecraftReleaseVersions().get(i)));
        }
    }

    @Test
    void getMinecraftReleaseVersionsAsArrayTest() {
        Assertions.assertNotNull(VERSIONLISTER.getMinecraftReleaseVersionsAsArray());
    }

    @Test
    void getFabricVersionsAsArrayTest() {
        Assertions.assertNotNull(VERSIONLISTER.getFabricVersionsAsArray());
    }

    @Test
    void getForgeVersionsAsArrayTest() {
        for (int i = 0; i < VERSIONLISTER.getMinecraftReleaseVersions().size(); i++) {
            Assertions.assertNotNull(VERSIONLISTER.getForgeVersionsAsArray(VERSIONLISTER.getMinecraftReleaseVersions().get(i)));
        }
    }

    @Test
    void getForgeMetaTest() {
        List<String> minecraftVersions = VERSIONLISTER.getMinecraftReleaseVersions();
        for (String version : minecraftVersions) {
            Assertions.assertNotNull(VERSIONLISTER.getForgeMeta().get(version));
        }
    }

    @Test
    void getFabricLatestInstallerVersionTest() {
        Assertions.assertNotNull(VERSIONLISTER.getFabricLatestInstallerVersion());
    }

    @Test
    void getFabricReleaseInstallerVersionTest() {
        Assertions.assertNotNull(VERSIONLISTER.getFabricReleaseInstallerVersion());
    }

}
