package de.griefed.serverpackcreator.utilities.commonutilities;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class FileUtilitiesTest {

    private final FileUtilities FILE_UTILITIES;

    FileUtilitiesTest() {
        this.FILE_UTILITIES = new FileUtilities();
    }

    @Test
    void unzipArchiveTest() {
        String modpackDir = "backend/test/resources/curseforge_tests";
        String zipFile = "backend/test/resources/curseforge_tests/modpack.zip";
        FILE_UTILITIES.unzipArchive(zipFile, modpackDir);
        Assertions.assertTrue(new File("./backend/test/resources/curseforge_tests/manifest.json").exists());
        Assertions.assertTrue(new File("./backend/test/resources/curseforge_tests/modlist.html").exists());
        Assertions.assertTrue(new File("./backend/test/resources/curseforge_tests/overrides").isDirectory());
        FileUtils.deleteQuietly(new File("./backend/test/resources/curseforge_tests/manifest.json"));
        FileUtils.deleteQuietly(new File("./backend/test/resources/curseforge_tests/modlist.html"));
        FileUtils.deleteQuietly(new File("./backend/test/resources/curseforge_tests/overrides"));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void replaceFileTest() throws IOException {
        File source = new File("source.file");
        File destination = new File("destination.file");
        source.createNewFile();
        destination.createNewFile();
        FILE_UTILITIES.replaceFile(source,destination);
        Assertions.assertFalse(source.exists());
        Assertions.assertTrue(destination.exists());
        FileUtils.deleteQuietly(destination);
    }
}
