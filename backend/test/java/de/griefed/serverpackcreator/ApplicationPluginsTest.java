package de.griefed.serverpackcreator;

import org.apache.logging.log4j.core.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ApplicationPluginsTest {

    @Test
    void test() {
        try {
            FileUtils.mkdir(new File("./plugins"),true);
        } catch (Exception ignored) {}
        try {
            Files.copy(Paths.get("backend/test/resources/testresources/serverpackcreatorexampleaddon.jar"), Paths.get("./plugins/serverpackcreatorexampleaddon.jar"));
        } catch (Exception ignored) {}
        ApplicationPlugins applicationPlugins = new ApplicationPlugins();
        Assertions.assertNotNull(applicationPlugins);
        Assertions.assertNotNull(applicationPlugins.PLUGINS_SERVERPACKARCHIVECREATED);
        Assertions.assertNotNull(applicationPlugins.PLUGINS_SERVERPACKCREATED);
        Assertions.assertNotNull(applicationPlugins.PLUGINS_SERVERPACKSTART);
        Assertions.assertNotNull(applicationPlugins.PLUGINS_TABBEDPANE);
    }

}
