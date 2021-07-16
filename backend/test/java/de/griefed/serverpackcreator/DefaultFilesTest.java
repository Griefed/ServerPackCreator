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
 * 1. {@link #DefaultFilesTest()} tests {@link DefaultFiles#DefaultFiles(LocalizationManager)}<br>
 * 2. {@link #getConfigFileTest()} tests {@link DefaultFiles#getConfigFile()}<br>
 * 3. {@link #getOldConfigFileTest()} tests {@link DefaultFiles#getOldConfigFile()}<br>
 * 4. {@link #getPropertiesFileTest()} tests {@link DefaultFiles#getPropertiesFile()}<br>
 * 5. {@link #getIconFileTest()} tests {@link DefaultFiles#getIconFile()}<br>
 * 6. {@link #getForgeWindowsFileTest()} tests {@link DefaultFiles#getForgeWindowsFile()}<br>
 * 7. {@link #getForgeLinuxFileTest()} tests {@link DefaultFiles#getForgeLinuxFile()}<br>
 * 8. {@link #getFabricWindowsFileTest()} tests {@link DefaultFiles#getFabricWindowsFile()}<br>
 * 9. {@link #getFabricLinuxFileTest()} tests {@link DefaultFiles#getFabricLinuxFile()}<br>
 * 10.{@link #checkForConfigTestOld} tests {@link DefaultFiles#checkForConfig()}<br>
 * 11.{@link #checkForConfigTest} tests {@link DefaultFiles#checkForConfig()}<br>
 * 12.{@link #checkForConfigTestNew} tests {@link DefaultFiles#checkForConfig()}<br>
 * 13.{@link #checkForFabricLinuxTest} tests {@link DefaultFiles#checkForFabricLinux()}<br>
 * 14.{@link #checkForFabricLinuxTestNew} tests {@link DefaultFiles#checkForForgeLinux()}<br>
 * 15.{@link #checkForFabricWindowsTest} tests {@link DefaultFiles#checkForFabricWindows()}<br>
 * 16.{@link #checkForFabricWindowsTestNew} tests {@link DefaultFiles#checkForFabricWindows()}<br>
 * 17.{@link #checkForForgeLinuxTest} tests {@link DefaultFiles#checkForForgeLinux()}<br>
 * 18.{@link #checkForForgeLinuxTestNew} tests {@link DefaultFiles#checkForForgeLinux()}<br>
 * 19.{@link #checkForForgeWindowsTest} tests {@link DefaultFiles#checkForForgeWindows()}<br>
 * 20.{@link #checkForForgeWindowsTestNew} tests {@link DefaultFiles#checkForForgeWindows()}<br>
 * 21.{@link #checkForPropertiesTest} tests {@link DefaultFiles#checkForProperties()}<br>
 * 22.{@link #checkForPropertiesTestNew} tests {@link DefaultFiles#checkForProperties()}<br>
 * 23.{@link #checkForIconTest} tests {@link DefaultFiles#checkForIcon()}<br>
 * 24.{@link #checkForIconTestNew} tests {@link DefaultFiles#checkForIcon()}<br>
 * 25.{@link #filesSetupTest} tests {@link DefaultFiles#filesSetup()}
 */
class DefaultFilesTest {

    private DefaultFiles defaultFiles;
    private LocalizationManager localizationManager;

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
