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
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;

class DefaultFilesTest {

    private final DefaultFiles DEFAULTFILES;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    DefaultFilesTest() {
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
    }

    @Test
    void getMinecraftManifestUrlTest() {
        Assertions.assertNotNull(DEFAULTFILES.getMinecraftManifestUrl());
    }

    @Test
    void getForgeManifestUrlTest() {
        Assertions.assertNotNull(DEFAULTFILES.getForgeManifestUrl());
    }

    @Test
    void getFabricManifestUrlTest() {
        Assertions.assertNotNull(DEFAULTFILES.getFabricManifestUrl());
    }

    @Test
    void getFabricInstallerManifestUrlTest() {
        Assertions.assertNotNull(DEFAULTFILES.getFabricInstallerManifestUrl());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTestOld() throws IOException {
        File oldConfigFile = new File("creator.conf");
        oldConfigFile.createNewFile();
        Assertions.assertFalse(DEFAULTFILES.checkForConfig());
        Assertions.assertTrue(new File("serverpackcreator.conf").exists());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTest() throws IOException {
        File configFile = new File("serverpackcreator.conf");
        configFile.createNewFile();
        Assertions.assertFalse(DEFAULTFILES.checkForConfig());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTestNew() {
        File configFile = new File("./serverpackcreator.conf");
        configFile.delete();
        Assertions.assertTrue(DEFAULTFILES.checkForConfig());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("./server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES)));
        new File(String.format("server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES)).createNewFile();
        Assertions.assertFalse(DEFAULTFILES.checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTestNew() {
        new File(String.format("./server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES)).delete();
        Assertions.assertTrue(DEFAULTFILES.checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("./server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_ICON)));
        new File(String.format("server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_ICON)).createNewFile();
        Assertions.assertFalse(DEFAULTFILES.checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_ICON));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTestNew() {
        new File(String.format("./server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_ICON)).delete();
        Assertions.assertTrue(DEFAULTFILES.checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_ICON));
    }

    @Test
    void filesSetupTest() {
        DEFAULTFILES.filesSetup();
        Assertions.assertTrue(new File("./server_files").isDirectory());
        Assertions.assertTrue(new File("./work").isDirectory());
        Assertions.assertTrue(new File("./work/temp").isDirectory());
        Assertions.assertTrue(new File("./server-packs").isDirectory());
        Assertions.assertTrue(new File("./server_files/server.properties").exists());
        Assertions.assertTrue(new File("./server_files/server-icon.png").exists());
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
        Assertions.assertTrue(new File("./work/minecraft-manifest.json").exists());
        Assertions.assertTrue(new File("./work/fabric-manifest.xml").exists());
        Assertions.assertTrue(new File("./work/forge-manifest.json").exists());
    }

    @Test
    void downloadMinecraftManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT)));
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getMinecraftManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT);
        Assertions.assertTrue(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT)).exists());
    }

    @Test
    void downloadFabricManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC)));
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getFabricManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC);
        Assertions.assertTrue(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC)).exists());
    }

    @Test
    void downloadForgeManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE)));
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getForgeManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE);
        Assertions.assertTrue(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE)).exists());
    }

    @Test
    void downloadFabricInstallerManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER)));
        DEFAULTFILES.refreshManifestFile(DEFAULTFILES.getFabricInstallerManifestUrl(), APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER);
        Assertions.assertTrue(new File(String.format("./work/%s", APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER)).exists());
    }
}
