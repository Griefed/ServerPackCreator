package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;

class DefaultFilesTest {

    private final DefaultFiles DEFAULTFILES;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    DefaultFilesTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.APPLICATIONPROPERTIES = new ApplicationProperties();
        LocalizationManager LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        this.DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkForPropertiesTest() {
        try {
            FileUtils.createParentDirectories(new File(String.format("./server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES)));
        } catch (Exception ignored) {}
        try {
            new File(String.format("server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES)).createNewFile();
        } catch (Exception ignored) {}
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
    void checkForIconTest() {
        try {
            FileUtils.createParentDirectories(new File(String.format("./server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_ICON)));
        } catch (Exception ignored) {}
        try {
            new File(String.format("server_files/%s", APPLICATIONPROPERTIES.FILE_SERVER_ICON)).createNewFile();
        } catch (Exception ignored) {}
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
        FileUtils.deleteQuietly(new File("./server_files"));
        FileUtils.deleteQuietly(new File("./work"));
        FileUtils.deleteQuietly(new File("./work/temp"));
        FileUtils.deleteQuietly(new File("./server-packs"));
        FileUtils.deleteQuietly(new File("./server_files/server.properties"));
        FileUtils.deleteQuietly(new File("./server_files/server-icon.png"));
        FileUtils.deleteQuietly(new File("./serverpackcreator.conf"));
        DEFAULTFILES.filesSetup();
        Assertions.assertTrue(new File("./server_files").isDirectory());
        Assertions.assertTrue(new File("./work").isDirectory());
        Assertions.assertTrue(new File("./work/temp").isDirectory());
        Assertions.assertTrue(new File("./server-packs").isDirectory());
        Assertions.assertTrue(new File("./server_files/server.properties").exists());
        Assertions.assertTrue(new File("./server_files/server-icon.png").exists());
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
    }

}
