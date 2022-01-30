package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VersionListerTest {

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final DefaultFiles DEFAULTFILES;
    private final VersionLister VERSIONLISTER;
    private static final Logger LOG = LogManager.getLogger(VersionListerTest.class);
    private final ApplicationProperties APPLICATIONPROPERTIES;

    public VersionListerTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.APPLICATIONPROPERTIES = new ApplicationProperties();
        LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        LOCALIZATIONMANAGER.initialize();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        DEFAULTFILES.filesSetup();
        this.VERSIONLISTER = new VersionLister(APPLICATIONPROPERTIES);
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

    @Test
    void reverseOrderListTest() {
        Assertions.assertNotNull(VERSIONLISTER.reverseOrderList(VERSIONLISTER.getFabricVersions()));
        Assertions.assertEquals(
                VERSIONLISTER.reverseOrderList(VERSIONLISTER.getFabricVersions()).get(VERSIONLISTER.reverseOrderList(VERSIONLISTER.getFabricVersions()).size() - 1),
                VERSIONLISTER.getFabricVersions().get(0)
        );
    }

    @Test
    void reverseOrderArrayTest() {
        Assertions.assertNotNull(VERSIONLISTER.reverseOrderArray(VERSIONLISTER.getForgeVersionsAsArray(VERSIONLISTER.getMinecraftReleaseVersion())));
        Assertions.assertEquals(
                VERSIONLISTER.reverseOrderArray(VERSIONLISTER.getForgeVersionsAsArray(VERSIONLISTER.getMinecraftReleaseVersion()))[VERSIONLISTER.reverseOrderArray(VERSIONLISTER.getForgeVersionsAsArray(VERSIONLISTER.getMinecraftReleaseVersion())).length - 1],
                VERSIONLISTER.getForgeVersionsAsArray(VERSIONLISTER.getMinecraftReleaseVersion())[0]
        );
    }

    @Test
    void refreshVersionsTest() {
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getMinecraftManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT);
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getForgeManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE);
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getFabricManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC);
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getFabricInstallerManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER);

        VERSIONLISTER.refreshVersions();

        Assertions.assertNotNull(VERSIONLISTER.getFabricReleaseInstallerVersion());
        Assertions.assertNotNull(VERSIONLISTER.getFabricLatestInstallerVersion());
        Assertions.assertTrue(VERSIONLISTER.getFabricVersions().size() > 1);
        Assertions.assertTrue(VERSIONLISTER.getMinecraftReleaseVersions().size() > 1);
        for (int i = 0; i < VERSIONLISTER.getMinecraftReleaseVersions().size(); i++) {
            Assertions.assertNotNull(VERSIONLISTER.getForgeVersionsAsArray(VERSIONLISTER.getMinecraftReleaseVersions().get(i)));
        }
    }

}
