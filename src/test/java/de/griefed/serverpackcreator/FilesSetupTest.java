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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * <strong>Table of tests</strong>
 * <p>
 * 1. {@link #FilesSetupTest()} tests {@link FilesSetup#FilesSetup(LocalizationManager)}<br>
 * 2. {@link #getConfigFileTest()} tests {@link FilesSetup#getConfigFile()}<br>
 * 3. {@link #getOldConfigFileTest()} tests {@link FilesSetup#getOldConfigFile()}<br>
 * 4. {@link #getPropertiesFileTest()} tests {@link FilesSetup#getPropertiesFile()}<br>
 * 5. {@link #getIconFileTest()} tests {@link FilesSetup#getIconFile()}<br>
 * 6. {@link #getForgeWindowsFileTest()} tests {@link FilesSetup#getForgeWindowsFile()}<br>
 * 7. {@link #getForgeLinuxFileTest()} tests {@link FilesSetup#getForgeLinuxFile()}<br>
 * 8. {@link #getFabricWindowsFileTest()} tests {@link FilesSetup#getFabricWindowsFile()}<br>
 * 9. {@link #getFabricLinuxFileTest()} tests {@link FilesSetup#getFabricLinuxFile()}<br>
 * 10.{@link #checkForConfigTestOld} tests {@link FilesSetup#checkForConfig()}<br>
 * 11.{@link #checkForConfigTest} tests {@link FilesSetup#checkForConfig()}<br>
 * 12.{@link #checkForConfigTestNew} tests {@link FilesSetup#checkForConfig()}<br>
 * 13.{@link #checkForFabricLinuxTest} tests {@link FilesSetup#checkForFabricLinux()}<br>
 * 14.{@link #checkForFabricLinuxTestNew} tests {@link FilesSetup#checkForForgeLinux()}<br>
 * 15.{@link #checkForFabricWindowsTest} tests {@link FilesSetup#checkForFabricWindows()}<br>
 * 16.{@link #checkForFabricWindowsTestNew} tests {@link FilesSetup#checkForFabricWindows()}<br>
 * 17.{@link #checkForForgeLinuxTest} tests {@link FilesSetup#checkForForgeLinux()}<br>
 * 18.{@link #checkForForgeLinuxTestNew} tests {@link FilesSetup#checkForForgeLinux()}<br>
 * 19.{@link #checkForForgeWindowsTest} tests {@link FilesSetup#checkForForgeWindows()}<br>
 * 20.{@link #checkForForgeWindowsTestNew} tests {@link FilesSetup#checkForForgeWindows()}<br>
 * 21.{@link #checkForPropertiesTest} tests {@link FilesSetup#checkForProperties()}<br>
 * 22.{@link #checkForPropertiesTestNew} tests {@link FilesSetup#checkForProperties()}<br>
 * 23.{@link #checkForIconTest} tests {@link FilesSetup#checkForIcon()}<br>
 * 24.{@link #checkForIconTestNew} tests {@link FilesSetup#checkForIcon()}<br>
 * 25.{@link #filesSetupTest} tests {@link FilesSetup#filesSetup()}
 */
class FilesSetupTest {

    private FilesSetup filesSetup;
    private LocalizationManager localizationManager;

    FilesSetupTest() {
        localizationManager = new LocalizationManager();
        filesSetup = new FilesSetup(localizationManager);
    }

    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getConfigFileTest() {
        File file = filesSetup.getConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getOldConfigFileTest() {
        File file = filesSetup.getOldConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getPropertiesFileTest() {
        File file = filesSetup.getPropertiesFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getIconFileTest() {
        File file = filesSetup.getIconFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getForgeWindowsFileTest() {
        File file = filesSetup.getForgeWindowsFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getForgeLinuxFileTest() {
        File file = filesSetup.getForgeLinuxFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getFabricWindowsFileTest() {
        File file = filesSetup.getFabricWindowsFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getFabricLinuxFileTest() {
        File file = filesSetup.getFabricLinuxFile();
        Assertions.assertNotNull(file);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTestOld() throws IOException {
        File oldConfigFile = new File("creator.conf");
        oldConfigFile.createNewFile();
        Assertions.assertFalse(filesSetup.checkForConfig());
        Assertions.assertTrue(new File("serverpackcreator.conf").exists());
        new File("serverpackcreator.conf").delete();
        new File("creator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTest() throws IOException {
        File configFile = new File("serverpackcreator.conf");
        configFile.createNewFile();
        Assertions.assertFalse(filesSetup.checkForConfig());
        configFile.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForConfigTestNew() {
        File configFile = new File("./serverpackcreator.conf");
        configFile.delete();
        Assertions.assertTrue(filesSetup.checkForConfig());
        configFile.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricLinuxTest() throws IOException {
        File fabricLinux = new File("start-fabric.sh");
        fabricLinux.createNewFile();
        Assertions.assertFalse(filesSetup.checkForFabricLinux());
        fabricLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricLinuxTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File fabricLinux = new File("./server_files/start-fabric.sh");
        fabricLinux.delete();
        Assertions.assertTrue(filesSetup.checkForFabricLinux());
        fabricLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricWindowsTest() throws IOException {
        File fabricWindows = new File("start-fabric.bat");
        fabricWindows.createNewFile();
        Assertions.assertFalse(filesSetup.checkForFabricWindows());
        fabricWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForFabricWindowsTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File fabricWindows = new File("./server_files/start-fabric.bat");
        fabricWindows.delete();
        Assertions.assertTrue(filesSetup.checkForFabricWindows());
        fabricWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeLinuxTest() throws IOException {
        File forgeLinux = new File("start-forge.sh");
        forgeLinux.createNewFile();
        Assertions.assertFalse(filesSetup.checkForForgeLinux());
        forgeLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeLinuxTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File forgeLinux = new File("./server_files/start-forge.sh");
        forgeLinux.delete();
        Assertions.assertTrue(filesSetup.checkForForgeLinux());
        forgeLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeWindowsTest() throws IOException {
        File forgeWindows = new File("start-forge.bat");
        forgeWindows.createNewFile();
        Assertions.assertFalse(filesSetup.checkForForgeWindows());
        forgeWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForForgeWindowsTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File forgeWindows = new File("./server_files/start-forge.bat");
        forgeWindows.delete();
        Assertions.assertTrue(filesSetup.checkForForgeWindows());
        forgeWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTest() throws IOException {
        File properties = new File("server.properties");
        properties.createNewFile();
        Assertions.assertFalse(filesSetup.checkForProperties());
        properties.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File properties = new File("./server_files/server.properties");
        properties.delete();
        Assertions.assertTrue(filesSetup.checkForProperties());
        properties.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTest() throws IOException {
        File icon = new File("server-icon.png");
        icon.createNewFile();
        Assertions.assertFalse(filesSetup.checkForIcon());
        icon.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForIconTestNew() throws IOException {
        Files.createDirectories(Paths.get("./server_files"));
        File icon = new File("./server_files/server-icon.png");
        icon.delete();
        Assertions.assertTrue(filesSetup.checkForIcon());
        icon.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void filesSetupTest() throws IOException {
        filesSetup.filesSetup();
        Assertions.assertTrue(new File("./server_files").isDirectory());
        Assertions.assertTrue(new File("./server_files/server.properties").exists());
        Assertions.assertTrue(new File("./server_files/server-icon.png").exists());
        Assertions.assertTrue(new File("./server_files/start-fabric.bat").exists());
        Assertions.assertTrue(new File("./server_files/start-fabric.sh").exists());
        Assertions.assertTrue(new File("./server_files/start-forge.bat").exists());
        Assertions.assertTrue(new File("./server_files/start-forge.sh").exists());
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
        String delete = "./server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();
    }
}
