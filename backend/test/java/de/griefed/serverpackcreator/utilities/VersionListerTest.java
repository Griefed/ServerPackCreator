/* Copyright (C) 2021  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.DefaultFiles;
import de.griefed.serverpackcreator.VersionLister;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.ApplicationProperties;
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
        LOCALIZATIONMANAGER.init();
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

}
