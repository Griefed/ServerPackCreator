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
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * <strong>Table of tests</strong>
 * <p>
 * 1. {@link #DefaultFilesTest()} tests {@link DefaultFiles#DefaultFiles(LocalizationManager)}<br>
 * 2. {@link #getConfigFileTest()} tests {@link DefaultFiles#getConfigFile()}<br>
 * 3. {@link #getOldConfigFileTest()} tests {@link DefaultFiles#getOldConfigFile()}<br>
 * 4. {@link #getPropertiesFileTest()} tests {@link DefaultFiles#getPropertiesFile()}<br>
 * 5. {@link #getIconFileTest()} tests {@link DefaultFiles#getIconFile()}<br>
 * 6. {@link #getForgeWindowsFileTest()} tests {@link DefaultFiles#getForgeWindowsFile()}<br>
 * 7. {@link #getForgeLinuxFileTest()} tests {@link DefaultFiles#getForgeLinuxFile()}<br>
 * 8. {@link #getFabricWindowsFileTest()} tests {@link DefaultFiles#getFabricWindowsFile()}<br>
 * 9. {@link #getFabricLinuxFileTest()} tests {@link DefaultFiles#getFabricLinuxFile()}<br>
 * 10.{@link #getMinecraftManifestUrlTest()} tests {@link DefaultFiles#getMinecraftManifestUrl()}<br>
 * 11.{@link #getForgeManifestUrlTest()} tests {@link DefaultFiles#getForgeManifestUrl()}<br>
 * 12.{@link #getFabricManifestUrlTest()} tests {@link DefaultFiles#getFabricManifestUrl()}<br>
 * 13.{@link #getFabricInstallerManifestUrlTest()} tests {@link DefaultFiles#getFabricInstallerManifestUrl()}<br>
 * 14.{@link #checkForConfigTestOld} tests {@link DefaultFiles#checkForConfig()}<br>
 * 15.{@link #checkForConfigTest} tests {@link DefaultFiles#checkForConfig()}<br>
 * 16.{@link #checkForConfigTestNew} tests {@link DefaultFiles#checkForConfig()}<br>
 * 17.{@link #checkForFabricLinuxTest} tests {@link DefaultFiles#checkForFabricLinux()}<br>
 * 18.{@link #checkForFabricLinuxTestNew} tests {@link DefaultFiles#checkForForgeLinux()}<br>
 * 19.{@link #checkForFabricWindowsTest} tests {@link DefaultFiles#checkForFabricWindows()}<br>
 * 20.{@link #checkForFabricWindowsTestNew} tests {@link DefaultFiles#checkForFabricWindows()}<br>
 * 21.{@link #checkForForgeLinuxTest} tests {@link DefaultFiles#checkForForgeLinux()}<br>
 * 22.{@link #checkForForgeLinuxTestNew} tests {@link DefaultFiles#checkForForgeLinux()}<br>
 * 23.{@link #checkForForgeWindowsTest} tests {@link DefaultFiles#checkForForgeWindows()}<br>
 * 24.{@link #checkForForgeWindowsTestNew} tests {@link DefaultFiles#checkForForgeWindows()}<br>
 * 25.{@link #checkForPropertiesTest} tests {@link DefaultFiles#checkForProperties()}<br>
 * 26.{@link #checkForPropertiesTestNew} tests {@link DefaultFiles#checkForProperties()}<br>
 * 27.{@link #checkForIconTest} tests {@link DefaultFiles#checkForIcon()}<br>
 * 28.{@link #checkForIconTestNew} tests {@link DefaultFiles#checkForIcon()}<br>
 * 29.{@link #filesSetupTest} tests {@link DefaultFiles#filesSetup()}<br>
 * 30.{@link #downloadMinecraftManifestTest} tests {@link DefaultFiles#downloadMinecraftManifest()}<br>
 * 31.{@link #downloadFabricManifestTest} tests {@link DefaultFiles#downloadFabricManifest()}<br>
 * 32.{@link #downloadForgeManifestTest} tests {@link DefaultFiles#downloadForgeManifest()}<br>
 * 33.{@link #downloadFabricInstallerManifestTest} tests {@link DefaultFiles#downloadFabricInstallerManifest()}
 * 34.{@link #refreshValidationFilesTest} tests {@link DefaultFiles#refreshValidationFiles()}
 */
class DefaultFilesTest {

    private final DefaultFiles defaultFiles;
    private final LocalizationManager localizationManager;

    DefaultFilesTest() {
        localizationManager = new LocalizationManager();
        defaultFiles = new DefaultFiles(localizationManager);
    }

    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getConfigFileTest() {
        File file = defaultFiles.getConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getOldConfigFileTest() {
        File file = defaultFiles.getOldConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getPropertiesFileTest() {
        File file = defaultFiles.getPropertiesFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getIconFileTest() {
        File file = defaultFiles.getIconFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getForgeWindowsFileTest() {
        File file = defaultFiles.getForgeWindowsFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getForgeLinuxFileTest() {
        File file = defaultFiles.getForgeLinuxFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getFabricWindowsFileTest() {
        File file = defaultFiles.getFabricWindowsFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getFabricLinuxFileTest() {
        File file = defaultFiles.getFabricLinuxFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getMinecraftManifestUrlTest() {
        URL url = defaultFiles.getMinecraftManifestUrl();
        Assertions.assertNotNull(url);
    }

    @Test
    void getForgeManifestUrlTest() {
        URL url = defaultFiles.getForgeManifestUrl();
        Assertions.assertNotNull(url);
    }

    @Test
    void getFabricManifestUrlTest() {
        URL url = defaultFiles.getFabricManifestUrl();
        Assertions.assertNotNull(url);
    }

    @Test
    void getFabricInstallerManifestUrlTest() {
        URL url = defaultFiles.getFabricInstallerManifestUrl();
        Assertions.assertNotNull(url);
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
        File fabricLinux = new File("start-fabric.sh");
        fabricLinux.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFabricLinux());
        fabricLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricLinuxTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File fabricLinux = new File("./server_files/start-fabric.sh");
        fabricLinux.delete();
        Assertions.assertTrue(defaultFiles.checkForFabricLinux());
        fabricLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricWindowsTest() throws IOException {
        File fabricWindows = new File("start-fabric.bat");
        fabricWindows.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForFabricWindows());
        fabricWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricWindowsTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File fabricWindows = new File("./server_files/start-fabric.bat");
        fabricWindows.delete();
        Assertions.assertTrue(defaultFiles.checkForFabricWindows());
        fabricWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeLinuxTest() throws IOException {
        File forgeLinux = new File("start-forge.sh");
        forgeLinux.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForForgeLinux());
        forgeLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeLinuxTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File forgeLinux = new File("./server_files/start-forge.sh");
        forgeLinux.delete();
        Assertions.assertTrue(defaultFiles.checkForForgeLinux());
        forgeLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeWindowsTest() throws IOException {
        File forgeWindows = new File("start-forge.bat");
        forgeWindows.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForForgeWindows());
        forgeWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeWindowsTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File forgeWindows = new File("./server_files/start-forge.bat");
        forgeWindows.delete();
        Assertions.assertTrue(defaultFiles.checkForForgeWindows());
        forgeWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTest() throws IOException {
        File properties = new File("server.properties");
        properties.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForProperties());
        properties.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File properties = new File("./server_files/server.properties");
        properties.delete();
        Assertions.assertTrue(defaultFiles.checkForProperties());
        properties.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTest() throws IOException {
        File icon = new File("server-icon.png");
        icon.createNewFile();
        Assertions.assertFalse(defaultFiles.checkForIcon());
        icon.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File icon = new File("./server_files/server-icon.png");
        icon.delete();
        Assertions.assertTrue(defaultFiles.checkForIcon());
        icon.delete();
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
        Files.createDirectories(Paths.get("./work"));
        defaultFiles.downloadFabricManifest();
        Assertions.assertTrue(new File("./work/minecraft-manifest.json").exists());
        String work = "./work";
        if (new File(work).isDirectory()) {
            Path pathToBeDeleted = Paths.get(work);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricManifestTest() throws IOException {
        Files.createDirectories(Paths.get("./work"));
        defaultFiles.downloadFabricManifest();
        Assertions.assertTrue(new File("./work/fabric-manifest.xml").exists());
        String work = "./work";
        if (new File(work).isDirectory()) {
            Path pathToBeDeleted = Paths.get(work);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadForgeManifestTest() throws IOException {
        Files.createDirectories(Paths.get("./work"));
        defaultFiles.downloadForgeManifest();
        Assertions.assertTrue(new File("./work/forge-manifest.json").exists());
        String work = "./work";
        if (new File(work).isDirectory()) {
            Path pathToBeDeleted = Paths.get(work);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricInstallerManifestTest() throws IOException {
        Files.createDirectories(Paths.get("./work"));
        defaultFiles.downloadFabricInstallerManifest();
        Assertions.assertTrue(new File("./work/fabric-installer-manifest.xml").exists());
        String work = "./work";
        if (new File(work).isDirectory()) {
            Path pathToBeDeleted = Paths.get(work);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void refreshValidationFilesTest() throws IOException {
        File minecraft = new File("./work/minecraft-manifest.json");
        File fabric = new File("./work/fabric-manifest.json");
        File forge = new File("./work/forge-manifest.json");
        File fabricInstaller = new File("./work/fabric-installer-manifest.xml");

        Files.createDirectories(Paths.get("./work"));

        minecraft.createNewFile();
        fabric.createNewFile();
        forge.createNewFile();
        fabricInstaller.createNewFile();

        defaultFiles.refreshValidationFiles();

        Assertions.assertTrue(minecraft.exists());
        Assertions.assertTrue(fabric.exists());
        Assertions.assertTrue(forge.exists());
        Assertions.assertTrue(fabricInstaller.exists());

        File work = new File("./work");
        if (work.isDirectory()) {
            Path pathToBeDeleted = Paths.get("./work");
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
