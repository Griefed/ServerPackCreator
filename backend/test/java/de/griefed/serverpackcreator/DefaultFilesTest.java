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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * <strong>Table of tests</strong>
 * <p>
 * 1. {@link #DefaultFilesTest()}<br>
 * 2. {@link #getConfigFileTest()}<br>
 * 3. {@link #getOldConfigFileTest()}<br>
 * 4. {@link #getPropertiesFileTest()}<br>
 * 5. {@link #getIconFileTest()}<br>
 * 6. {@link #getForgeWindowsFileTest()}<br>
 * 7. {@link #getForgeLinuxFileTest()}<br>
 * 8. {@link #getFabricWindowsFileTest()}<br>
 * 9. {@link #getFabricLinuxFileTest()}<br>
 * 10.{@link #getMinecraftManifestUrlTest()}<br>
 * 11.{@link #getForgeManifestUrlTest()}<br>
 * 12.{@link #getFabricManifestUrlTest()}<br>
 * 13.{@link #getFabricInstallerManifestUrlTest()}<br>
 * 14.{@link #getManifestMinecraftTest()}<br>
 * 15.{@link #getManifestForgeTest()}<br>
 * 16.{@link #getManifestFabricTest()}<br>
 * 17.{@link #getManifestFabricInstallerTest()}<br>
 * 18.{@link #getManifestMinecraftTestEquals()}<br>
 * 19.{@link #getManifestForgeTestEquals()}<br>
 * 20.{@link #getManifestFabricTestEquals()}<br>
 * 21.{@link #getManifestFabricInstallerTestEquals()}<br>
 * 22.{@link #checkForConfigTestOld}<br>
 * 23.{@link #checkForConfigTest}<br>
 * 24.{@link #checkForConfigTestNew}<br>
 * 25.{@link #checkForFabricLinuxTest}<br>
 * 26.{@link #checkForFabricLinuxTestNew}<br>
 * 27.{@link #checkForFabricWindowsTest}<br>
 * 28.{@link #checkForFabricWindowsTestNew}<br>
 * 29.{@link #checkForForgeLinuxTest}<br>
 * 30.{@link #checkForForgeLinuxTestNew}<br>
 * 31.{@link #checkForForgeWindowsTest}<br>
 * 32.{@link #checkForForgeWindowsTestNew}<br>
 * 33.{@link #checkForPropertiesTest}<br>
 * 34.{@link #checkForPropertiesTestNew}<br>
 * 35.{@link #checkForIconTest}<br>
 * 36.{@link #checkForIconTestNew}<br>
 * 37.{@link #filesSetupTest}<br>
 * 38.{@link #downloadMinecraftManifestTest}<br>
 * 39.{@link #downloadFabricManifestTest}<br>
 * 40.{@link #downloadForgeManifestTest}<br>
 * 41.{@link #downloadFabricInstallerManifestTest}
 */
class DefaultFilesTest {

    private final DefaultFiles defaultFiles;
    private final LocalizationManager localizationManager;

    DefaultFilesTest() {
        localizationManager = new LocalizationManager();
        localizationManager.init();
        defaultFiles = new DefaultFiles(localizationManager);
    }

    @Test
    void getConfigFileTest() {
        Assertions.assertNotNull(defaultFiles.getConfigFile());
    }

    @Test
    void getOldConfigFileTest() {
        Assertions.assertNotNull(defaultFiles.getOldConfigFile());
    }

    @Test
    void getPropertiesFileTest() {
        Assertions.assertNotNull(defaultFiles.getPropertiesFile());
    }

    @Test
    void getIconFileTest() {
        Assertions.assertNotNull(defaultFiles.getIconFile());
    }

    @Test
    void getForgeWindowsFileTest() {
        Assertions.assertNotNull(defaultFiles.getForgeWindowsFile());
    }

    @Test
    void getForgeLinuxFileTest() {
        Assertions.assertNotNull(defaultFiles.getForgeLinuxFile());
    }

    @Test
    void getFabricWindowsFileTest() {
        Assertions.assertNotNull(defaultFiles.getFabricWindowsFile());
    }

    @Test
    void getFabricLinuxFileTest() {
        Assertions.assertNotNull(defaultFiles.getFabricLinuxFile());
    }

    @Test
    void getMinecraftManifestUrlTest() {
        Assertions.assertNotNull(defaultFiles.getMinecraftManifestUrl());
    }

    @Test
    void getForgeManifestUrlTest() {
        Assertions.assertNotNull(defaultFiles.getForgeManifestUrl());
    }

    @Test
    void getFabricManifestUrlTest() {
        Assertions.assertNotNull(defaultFiles.getFabricManifestUrl());
    }

    @Test
    void getFabricInstallerManifestUrlTest() {
        Assertions.assertNotNull(defaultFiles.getFabricInstallerManifestUrl());
    }

    @Test
    void getManifestMinecraftTest() {
        Assertions.assertNotNull(defaultFiles.getMANIFEST_MINECRAFT());
    }

    @Test
    void getManifestForgeTest() {
        Assertions.assertNotNull(defaultFiles.getMANIFEST_FORGE());
    }

    @Test
    void getManifestFabricTest() {
        Assertions.assertNotNull(defaultFiles.getMANIFEST_FABRIC());
    }

    @Test
    void getManifestFabricInstallerTest() {
        Assertions.assertNotNull(defaultFiles.getMANIFEST_FABRIC_INSTALLER());
    }

    @Test
    void getManifestMinecraftTestEquals() {
        Assertions.assertEquals("minecraft-manifest.json", defaultFiles.getMANIFEST_MINECRAFT().toString());
    }

    @Test
    void getManifestForgeTestEquals() {
        Assertions.assertEquals("forge-manifest.json", defaultFiles.getMANIFEST_FORGE().toString());
    }

    @Test
    void getManifestFabricTestEquals() {
        Assertions.assertEquals("fabric-manifest.xml", defaultFiles.getMANIFEST_FABRIC().toString());
    }

    @Test
    void getManifestFabricInstallerTestEquals() {
        Assertions.assertEquals("fabric-installer-manifest.xml", defaultFiles.getMANIFEST_FABRIC_INSTALLER().toString());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTestOld() throws IOException {
        File oldConfigFile = new File("creator.conf");
        oldConfigFile.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForConfig());
        Assertions.assertTrue(new File("serverpackcreator.conf").exists());
        new File("serverpackcreator.conf").delete();
        new File("creator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTest() throws IOException {
        File configFile = new File("serverpackcreator.conf");
        configFile.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForConfig());
        configFile.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTestNew() {
        File configFile = new File("./serverpackcreator.conf");
        configFile.delete();
        Assertions.assertTrue(defaultFiles.checkForConfig());
        configFile.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricLinuxTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("server_files/%s", defaultFiles.getFabricLinuxFile().toString())));
        new File(String.format("server_files/%s", defaultFiles.getFabricLinuxFile())).createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFile(defaultFiles.getFabricLinuxFile()));
        new File(String.format("./server_files/%s", defaultFiles.getFabricLinuxFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricLinuxTestNew() {
        new File(String.format("./server_files/%s", defaultFiles.getFabricLinuxFile())).delete();
        Assertions.assertTrue(defaultFiles.checkForFile(defaultFiles.getFabricLinuxFile()));
        new File(String.format("./server_files/%s", defaultFiles.getFabricLinuxFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricWindowsTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("./server_files/%s", defaultFiles.getFabricWindowsFile().toString())));
        new File(String.format("server_files/%s", defaultFiles.getFabricWindowsFile())).createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFile(defaultFiles.getFabricWindowsFile()));
        new File(String.format("./server_files/%s", defaultFiles.getFabricWindowsFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricWindowsTestNew() {
        new File(String.format("./server_files/%s", defaultFiles.getFabricWindowsFile())).delete();
        Assertions.assertTrue(defaultFiles.checkForFile(defaultFiles.getFabricWindowsFile()));
        new File(String.format("./server_files/%s", defaultFiles.getFabricWindowsFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeLinuxTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("./server_files/%s", defaultFiles.getForgeLinuxFile().toString())));
        new File(String.format("server_files/%s", defaultFiles.getForgeLinuxFile())).createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFile(defaultFiles.getForgeLinuxFile()));
        new File(String.format("./server_files/%s", defaultFiles.getForgeLinuxFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeLinuxTestNew() {
        new File(String.format("./server_files/%s", defaultFiles.getForgeLinuxFile().toString())).delete();
        Assertions.assertTrue(defaultFiles.checkForFile(defaultFiles.getForgeLinuxFile()));
        new File(String.format("./server_files/%s", defaultFiles.getForgeLinuxFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeWindowsTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("./server_files/%s", defaultFiles.getForgeWindowsFile().toString())));
        new File(String.format("server_files/%s", defaultFiles.getForgeWindowsFile())).createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFile(defaultFiles.getForgeWindowsFile()));
        new File(String.format("./server_files/%s", defaultFiles.getForgeWindowsFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeWindowsTestNew() {
        new File(String.format("./server_files/%s", defaultFiles.getForgeWindowsFile())).delete();
        Assertions.assertTrue(defaultFiles.checkForFile(defaultFiles.getForgeWindowsFile()));
        new File(String.format("./server_files/%s", defaultFiles.getForgeWindowsFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("./server_files/%s", defaultFiles.getPropertiesFile().toString())));
        new File(String.format("server_files/%s", defaultFiles.getPropertiesFile())).createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFile(defaultFiles.getPropertiesFile()));
        new File(String.format("./server_files/%s", defaultFiles.getPropertiesFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTestNew() {
        new File(String.format("./server_files/%s", defaultFiles.getPropertiesFile())).delete();
        Assertions.assertTrue(defaultFiles.checkForFile(defaultFiles.getPropertiesFile()));
        new File(String.format("./server_files/%s", defaultFiles.getPropertiesFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTest() throws IOException {
        FileUtils.createParentDirectories(new File(String.format("./server_files/%s", defaultFiles.getIconFile().toString())));
        new File(String.format("server_files/%s", defaultFiles.getIconFile())).createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFile(defaultFiles.getIconFile()));
        new File(String.format("./server_files/%s", defaultFiles.getIconFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTestNew() {
        new File(String.format("./server_files/%s", defaultFiles.getIconFile())).delete();
        Assertions.assertTrue(defaultFiles.checkForFile(defaultFiles.getIconFile()));
        new File(String.format("./server_files/%s", defaultFiles.getIconFile())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void filesSetupTest() throws IOException {
        defaultFiles.filesSetup();
        Assertions.assertTrue(new File("./server_files").isDirectory());
        Assertions.assertTrue(new File("./work").isDirectory());
        Assertions.assertTrue(new File("./work/temp").isDirectory());
        Assertions.assertTrue(new File("./server-packs").isDirectory());
        Assertions.assertTrue(new File("./server_files/server.properties").exists());
        Assertions.assertTrue(new File("./server_files/server-icon.png").exists());
        Assertions.assertTrue(new File("./server_files/start-fabric.bat").exists());
        Assertions.assertTrue(new File("./server_files/start-fabric.sh").exists());
        Assertions.assertTrue(new File("./server_files/start-forge.bat").exists());
        Assertions.assertTrue(new File("./server_files/start-forge.sh").exists());
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
        Assertions.assertTrue(new File("./work/minecraft-manifest.json").exists());
        Assertions.assertTrue(new File("./work/fabric-manifest.xml").exists());
        Assertions.assertTrue(new File("./work/forge-manifest.json").exists());
        String delete = "./server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        String deleteWork = "./work";
        if (new File(deleteWork).isDirectory()) {
            Path pathToBeDeleted = Paths.get(deleteWork);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        String deleteServerPacks = "./server-packs";
        if (new File(deleteServerPacks).isDirectory()) {
            Path pathToBeDeleted = Paths.get(deleteServerPacks);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadMinecraftManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", defaultFiles.getMANIFEST_MINECRAFT())));
        defaultFiles.refreshManifestFile(defaultFiles.getMinecraftManifestUrl(), defaultFiles.getMANIFEST_MINECRAFT());
        Assertions.assertTrue(new File(String.format("./work/%s", defaultFiles.getMANIFEST_MINECRAFT())).exists());
        new File(String.format("./work/%s", defaultFiles.getMANIFEST_MINECRAFT())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", defaultFiles.getMANIFEST_FABRIC())));
        defaultFiles.refreshManifestFile(defaultFiles.getFabricManifestUrl(), defaultFiles.getMANIFEST_FABRIC());
        Assertions.assertTrue(new File(String.format("./work/%s", defaultFiles.getMANIFEST_FABRIC())).exists());
        new File(String.format("./work/%s", defaultFiles.getMANIFEST_FABRIC())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadForgeManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", defaultFiles.getMANIFEST_FORGE())));
        defaultFiles.refreshManifestFile(defaultFiles.getForgeManifestUrl(), defaultFiles.getMANIFEST_FORGE());
        Assertions.assertTrue(new File(String.format("./work/%s", defaultFiles.getMANIFEST_FORGE())).exists());
        new File(String.format("./work/%s", defaultFiles.getMANIFEST_FORGE())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricInstallerManifestTest() throws IOException {
        //Files.createDirectories(Paths.get("./work"));
        FileUtils.createParentDirectories(new File(String.format("./work/%s", defaultFiles.getMANIFEST_FABRIC_INSTALLER())));
        defaultFiles.refreshManifestFile(defaultFiles.getFabricInstallerManifestUrl(), defaultFiles.getMANIFEST_FABRIC_INSTALLER());
        Assertions.assertTrue(new File(String.format("./work/%s", defaultFiles.getMANIFEST_FABRIC_INSTALLER())).exists());
        new File(String.format("./work/%s", defaultFiles.getMANIFEST_FABRIC_INSTALLER())).delete();
    }
}
