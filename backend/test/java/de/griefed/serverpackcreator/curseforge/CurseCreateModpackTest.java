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
package de.griefed.serverpackcreator.curseforge;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.ApplicationProperties;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Disabled
class CurseCreateModpackTest {

    private final CurseCreateModpack CURSECREATEMODPACK;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    CurseCreateModpackTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.APPLICATIONPROPERTIES = new ApplicationProperties();

        LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        LOCALIZATIONMANAGER.init();
        CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
    }

    @Test
    void getsetProjectNameTest() {
        Assertions.assertEquals("Vanilla Forge", CURSECREATEMODPACK.retrieveProjectName(238298));
        Assertions.assertEquals("999999", CURSECREATEMODPACK.retrieveProjectName(999999));
    }

    @Test
    void getsetFileNameAndDiskNameTest() {
        Assertions.assertEquals("Vanilla Forge 1.16.5", CURSECREATEMODPACK.retrieveFileName(238298,3174854));
        Assertions.assertEquals("Vanilla Forge 1.16.5-1.0.zip", CURSECREATEMODPACK.retrieveFileDiskName(238298,3174854));
        Assertions.assertEquals("9999999", CURSECREATEMODPACK.retrieveFileName(238298,9999999));
        Assertions.assertEquals("9999999", CURSECREATEMODPACK.retrieveFileDiskName(238298,9999999));
    }

    @Test
    void setModloaderCaseTest() {
        String forge = "fOrGe";
        String fabric = "fAbRiC";
        Assertions.assertEquals("Forge", CURSECREATEMODPACK.setModloaderCase(forge));
        Assertions.assertEquals("Fabric", CURSECREATEMODPACK.setModloaderCase(fabric));
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ResultOfMethodCallIgnored"})
    @Test
    void curseForgeModpackTest() throws CurseException, IOException {
        int projectID = 238298;
        int fileID = 3174854;
        ConfigurationModel configurationModel = new ConfigurationModel();
        String projectName = CurseAPI.project(projectID).get().name();
        String displayName = Objects.requireNonNull(CurseAPI.project(projectID)
                .get()
                .files()
                .fileWithID(fileID))
                .displayName();
        configurationModel.setModpackDir(String.format("./backend/test/resources/forge_tests/%s/%s", projectName, displayName));
        /*Assertions.assertTrue(*/CURSECREATEMODPACK.curseForgeModpack(configurationModel, projectID, fileID);/*);*/
        String deleteFile = String.format("./backend/test/resources/forge_tests/%s/%s", projectName, displayName);
        if (new File(deleteFile).isDirectory()) {
            Path pathToBeDeleted = Paths.get(deleteFile);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        String deleteProject = String.format("./backend/test/resources/forge_tests/%s", projectName);
        if (new File(deleteProject).isDirectory()) {
            Path pathToBeDeleted = Paths.get(deleteProject);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        String deleteFolder = "Vanilla Forge";
        if (new File(deleteFolder).isDirectory()) {
            Path pathToBeDeleted = Paths.get(deleteFolder);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void initializeModpackTest() throws CurseException {
        int projectID = 238298;
        int fileID = 3174854;
        ConfigurationModel configurationModel = new ConfigurationModel();
        String modpackDir = "./backend/test/resources/forge_tests/Vanilla Forge/Vanilla Forge 1.16.5";
        configurationModel.setModpackDir(modpackDir);
        configurationModel.setProjectName(CurseAPI.project(projectID).get().name());
        configurationModel.setFileName(CurseAPI.file(projectID, fileID).orElseThrow(NullPointerException::new).displayName());
        configurationModel.setFileDiskName(CurseAPI.file(projectID, fileID).orElseThrow(NullPointerException::new).nameOnDisk());
        CURSECREATEMODPACK.initializeModpack(modpackDir, projectID, fileID, configurationModel);
        Assertions.assertTrue(new File("./backend/test/resources/forge_tests/Vanilla Forge").isDirectory());
        Assertions.assertTrue(new File("./backend/test/resources/forge_tests/Vanilla Forge/Vanilla Forge 1.16.5").isDirectory());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadModsTest() throws IOException {
        String modpackDir = "./backend/test/resources/forge_tests";
        Files.copy(Paths.get("./backend/test/resources/testresources/manifest.json"), Paths.get("./backend/test/resources/forge_tests/manifest.json"), REPLACE_EXISTING);
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setCurseModpack(CURSECREATEMODPACK.getObjectMapper().readTree(Files.readAllBytes(Paths.get("./backend/test/resources/forge_tests/manifest.json"))));
        CURSECREATEMODPACK.downloadMods(modpackDir, configurationModel);
        Assertions.assertTrue(new File("./backend/test/resources/forge_tests/mods/BetterTitleScreen-1.16.4-1.10.2.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/forge_tests/mods/jei-professions-1.0.0-1.16.4.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/forge_tests/mods/ftb-backups-2.1.1.6.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/forge_tests/mods/ftb-gui-library-1604.2.0.29-forge.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/forge_tests/mods/JEIEnchantmentInfo-1.16.4-1.2.1.jar").exists());
        new File("./backend/test/resources/forge_tests/mods/BetterTitleScreen-1.16.4-1.10.2.jar").delete();
        new File("./backend/test/resources/forge_tests/mods/jei-professions-1.0.0-1.16.4.jar").delete();
        new File("./backend/test/resources/forge_tests/mods/ftb-backups-2.1.1.6.jar").delete();
        new File("./backend/test/resources/forge_tests/mods/ftb-gui-library-1604.2.0.29-forge.jar").delete();
        new File("./backend/test/resources/forge_tests/mods/JEIEnchantmentInfo-1.16.4-1.2.1.jar").delete();
        new File("./backend/test/resources/forge_tests/manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyOverrideTest() throws IOException {
        String modpackDir = "./backend/test/resources/overridestest";
        CURSECREATEMODPACK.copyOverride(modpackDir);
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/config").isDirectory());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/defaultconfigs").isDirectory());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods").isDirectory());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/scripts").isDirectory());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/seeds").isDirectory());

        Assertions.assertTrue(new File("./backend/test/resources/overridestest/config/testfile.txt").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/defaultconfigs/testfile.txt").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/aaaaa.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/bbbbb.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/ccccc.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/fffff.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/ggggg.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/hhhhh.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/iiiii.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/jjjjj.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/kkkkk.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/lllll.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/nnnnn.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/ppppp.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/qqqqq.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/rrrrr.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/testmod.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/uuuuu.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/vvvvv.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/wwwww.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/xxxxx.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/yyyyy.jar").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/mods/zzzzz.jar").exists());

        Assertions.assertTrue(new File("./backend/test/resources/overridestest/scripts/testscript.zs").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/seeds/seed1.json").exists());
        Assertions.assertTrue(new File("./backend/test/resources/overridestest/seeds/testjson.json").exists());

        if (new File("./backend/test/resources/overridestest/config").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./backend/test/resources/overridestest/config");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);}

        if (new File("./backend/test/resources/overridestest/defaultconfigs").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./backend/test/resources/overridestest/defaultconfigs");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);}

        if (new File("./backend/test/resources/overridestest/mods").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./backend/test/resources/overridestest/mods");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);}

        if (new File("./backend/test/resources/overridestest/scripts").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./backend/test/resources/overridestest/scripts");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);}

        if (new File("./backend/test/resources/overridestest/seeds").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./backend/test/resources/overridestest/seeds");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);}

        new File("./backend/test/resources/overridestest/config/testfile.txt").delete();
        new File("./backend/test/resources/overridestest/defaultconfigs/testfile.txt").delete();
        new File("./backend/test/resources/overridestest/mods/aaaaa.jar").delete();
        new File("./backend/test/resources/overridestest/mods/bbbbb.jar").delete();
        new File("./backend/test/resources/overridestest/mods/ccccc.jar").delete();
        new File("./backend/test/resources/overridestest/mods/fffff.jar").delete();
        new File("./backend/test/resources/overridestest/mods/ggggg.jar").delete();
        new File("./backend/test/resources/overridestest/mods/hhhhh.jar").delete();
        new File("./backend/test/resources/overridestest/mods/iiiii.jar").delete();
        new File("./backend/test/resources/overridestest/mods/jjjjj.jar").delete();
        new File("./backend/test/resources/overridestest/mods/kkkkk.jar").delete();
        new File("./backend/test/resources/overridestest/mods/lllll.jar").delete();
        new File("./backend/test/resources/overridestest/mods/nnnnn.jar").delete();
        new File("./backend/test/resources/overridestest/mods/ppppp.jar").delete();
        new File("./backend/test/resources/overridestest/mods/qqqqq.jar").delete();
        new File("./backend/test/resources/overridestest/mods/rrrrr.jar").delete();
        new File("./backend/test/resources/overridestest/mods/testmod.jar").delete();
        new File("./backend/test/resources/overridestest/mods/uuuuu.jar").delete();
        new File("./backend/test/resources/overridestest/mods/vvvvv.jar").delete();
        new File("./backend/test/resources/overridestest/mods/wwwww.jar").delete();
        new File("./backend/test/resources/overridestest/mods/xxxxx.jar").delete();
        new File("./backend/test/resources/overridestest/mods/yyyyy.jar").delete();
        new File("./backend/test/resources/overridestest/mods/zzzzz.jar").delete();

        new File("./backend/test/resources/overridestest/scripts/testscript.zs").delete();
        new File("./backend/test/resources/overridestest/seeds/seed.json").delete();
        new File("./backend/test/resources/overridestest/seeds/testjson.json").delete();
    }

    @Test
    void checkCurseForgeDirTest() {
        String modpackdir = "./backend/test/resources/forge_tests/overrides";
        Assertions.assertFalse(CURSECREATEMODPACK.checkCurseForgeDir(modpackdir));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void unzipArchiveTest() throws IOException {
        String modpackDir = "backend/test/resources/curseforge_tests";
        String zipFile = "backend/test/resources/curseforge_tests/modpack.zip";
        CURSECREATEMODPACK.unzipArchive(zipFile, modpackDir);
        Assertions.assertTrue(new File("./backend/test/resources/curseforge_tests/manifest.json").exists());
        Assertions.assertTrue(new File("./backend/test/resources/curseforge_tests/modlist.html").exists());
        Assertions.assertTrue(new File("./backend/test/resources/curseforge_tests/overrides").isDirectory());
        new File("./backend/test/resources/curseforge_tests/manifest.json").delete();
        new File("./backend/test/resources/curseforge_tests/modlist.html").delete();
        if (new File("./backend/test/resources/curseforge_tests/overrides").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./backend/test/resources/curseforge_tests/overrides");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);}
    }
}
