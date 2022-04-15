package de.griefed.serverpackcreator.plugins;

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
        Assertions.assertEquals(1,applicationPlugins.pluginsPostGenExtension().size());
        Assertions.assertEquals(1,applicationPlugins.pluginsTabExtension().size());
        Assertions.assertEquals(1,applicationPlugins.pluginsPreGenExtension().size());
        Assertions.assertEquals(1,applicationPlugins.pluginsPreZipExtension().size());
    }

}
