package de.griefed.ServerPackCreator.CurseForgeModpack;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
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
        MockitoAnnotations.openMocks(this);
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ResultOfMethodCallIgnored"})
    @Test
    void testCurseForgeModpack() throws CurseException, IOException {
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
            boolean result = CreateModpack.curseForgeModpack(modpackDir, 238298, 3174854);
            Assertions.assertTrue(result);
            String delete = String.format("./src/test/resources/forge_tests/%s", projectName);
            if (new File(delete).isDirectory()) {
                Path pathToBeDeleted = Paths.get(delete);
                Files.walk(pathToBeDeleted)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
    @Test
    void testSetModloaderFabric() {
        String result = CreateModpack.setModloader("fAbRiC");
        Assertions.assertEquals("Fabric", result);
    }

    @Test
    void testSetModloaderForge() {
        String result = CreateModpack.setModloader("fOrGe");
        Assertions.assertEquals("Forge", result);
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme