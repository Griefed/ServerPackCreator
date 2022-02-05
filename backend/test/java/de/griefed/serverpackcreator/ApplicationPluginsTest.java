package de.griefed.serverpackcreator;

import org.apache.logging.log4j.core.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ApplicationPluginsTest {

    @Disabled
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
        Assertions.assertEquals(1,applicationPlugins.PLUGINS_SERVERPACKARCHIVECREATED.size());
        Assertions.assertEquals(1,applicationPlugins.PLUGINS_SERVERPACKCREATED.size());
        Assertions.assertEquals(1,applicationPlugins.PLUGINS_SERVERPACKSTART.size());
        Assertions.assertEquals(1,applicationPlugins.PLUGINS_TABBEDPANE.size());
    }

}
