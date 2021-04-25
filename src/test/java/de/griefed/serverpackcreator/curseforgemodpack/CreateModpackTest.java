package de.griefed.serverpackcreator.curseforgemodpack;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.FilesSetup;
import de.griefed.serverpackcreator.Reference;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;

class CreateModpackTest {
    @Mock
    Logger appLogger;

    @InjectMocks
    CreateModpack createModpack;

    @BeforeEach
    void setUp() {
        FilesSetup.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }


    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ResultOfMethodCallIgnored"})
    @Test
    void testCurseForgeModpack() throws CurseException, IOException {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            int projectID = 238298;
            int fileID = 3174854;
            String projectName = CurseAPI.project(projectID).get().name();
            String displayName = Objects.requireNonNull(CurseAPI.project(projectID)
                    .get()
                    .files()
                    .fileWithID(fileID))
                    .displayName();
            String modpackDir = String.format("./src/test/resources/forge_tests/%s/%s", projectName, displayName);
            boolean result = Reference.createModpack.curseForgeModpack(modpackDir, projectID, fileID);
            Assertions.assertTrue(result);
            String deleteFile = String.format("./src/test/resources/forge_tests/%s/%s", projectName, displayName);
            if (new File(deleteFile).isDirectory()) {
                Path pathToBeDeleted = Paths.get(deleteFile);
                Files.walk(pathToBeDeleted)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            String deleteProject = String.format("./src/test/resources/forge_tests/%s", projectName);
            if (new File(deleteProject).isDirectory()) {
                Path pathToBeDeleted = Paths.get(deleteProject);
                Files.walk(pathToBeDeleted)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    void testSetModloaderFabric() {
        String result = Reference.createModpack.setModloader("fAbRiC");
        Assertions.assertEquals("Fabric", result);
    }

    @Test
    void testSetModloaderForge() {
        String result = Reference.createModpack.setModloader("fOrGe");
        Assertions.assertEquals("Forge", result);
    }
}
