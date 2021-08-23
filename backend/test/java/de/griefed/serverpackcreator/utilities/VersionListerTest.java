package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.DefaultFiles;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
 * 15.{@link #getForgeVersionsAsArrayTest()}
 */
public class VersionListerTest {

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final DefaultFiles DEFAULTFILES;
    private final VersionLister VERSIONLISTER;
    private static final Logger LOG = LogManager.getLogger(VersionListerTest.class);

    public VersionListerTest() {
        LOCALIZATIONMANAGER = new LocalizationManager();
        LOCALIZATIONMANAGER.init();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER);
        DEFAULTFILES.filesSetup();
        this.VERSIONLISTER = new VersionLister();
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

}
