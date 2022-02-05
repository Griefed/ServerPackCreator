package de.griefed.serverpackcreator.utilities;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class SystemUtilitiesTest {

    private final SystemUtilities SYSTEMUTILITIES;

    SystemUtilitiesTest() {
        this.SYSTEMUTILITIES = new SystemUtilities();
    }

    @Test
    void acquireJavaPathFromSystemTest() {
        Assertions.assertNotNull(SYSTEMUTILITIES.acquireJavaPathFromSystem());
        Assertions.assertTrue(new File(SYSTEMUTILITIES.acquireJavaPathFromSystem()).exists());
        Assertions.assertTrue(new File(SYSTEMUTILITIES.acquireJavaPathFromSystem()).isFile());
        Assertions.assertTrue(SYSTEMUTILITIES.acquireJavaPathFromSystem().replace("\\","/").contains("bin/java"));
    }

    @Test
    void downloadFileTest() {
        try {
            SYSTEMUTILITIES.downloadFile("Fabric-Server-Launcher.jar",new URL("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar"));
        } catch (Exception ignored) {}
        Assertions.assertTrue(new File("Fabric-Server-Launcher.jar").exists());
        FileUtils.deleteQuietly(new File("Fabric-Server-Launcher.jar"));
        try {
            SYSTEMUTILITIES.downloadFile("some_foooooolder/foooobar/Fabric-Server-Launcher.jar",new URL("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar"));
        } catch (Exception ignored) {}
        Assertions.assertTrue(new File("some_foooooolder/foooobar/Fabric-Server-Launcher.jar").exists());
        FileUtils.deleteQuietly(new File("some_foooooolder"));
    }

    @Test
    void unzipArchiveTest() {
        String modpackDir = "backend/test/resources/curseforge_tests";
        String zipFile = "backend/test/resources/curseforge_tests/modpack.zip";
        SYSTEMUTILITIES.unzipArchive(zipFile, modpackDir);
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
        SYSTEMUTILITIES.replaceFile(source,destination);
        Assertions.assertFalse(source.exists());
        Assertions.assertTrue(destination.exists());
        FileUtils.deleteQuietly(destination);
    }
}
