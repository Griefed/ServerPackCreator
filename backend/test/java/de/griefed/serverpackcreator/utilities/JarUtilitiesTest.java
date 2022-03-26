package de.griefed.serverpackcreator.utilities;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class JarUtilitiesTest {

    private final JarUtilities JARUTILITIES;

    JarUtilitiesTest() {
        this.JARUTILITIES = new JarUtilities();
    }

    @Test
    void copyFileFromJarTest() {
        JARUTILITIES.copyFileFromJar(new File("banner.txt"));
        Assertions.assertTrue(new File("banner.txt").exists());
    }

    @Test
    void getApplicationHomeForClassTest() {
        Assertions.assertNotNull(JARUTILITIES.getApplicationHomeForClass(JarUtilitiesTest.class));
    }

    @Test
    void systemInformationTest() {
        HashMap<String, String> system = JARUTILITIES.systemInformation(JARUTILITIES.getApplicationHomeForClass(JarUtilitiesTest.class));
        Assertions.assertNotNull(system);
        Assertions.assertNotNull(system.get("jarPath"));
        Assertions.assertTrue(system.get("jarPath").length() > 0);
        Assertions.assertNotNull(system.get("jarName"));
        Assertions.assertTrue(system.get("jarName").length() > 0);
        Assertions.assertNotNull(system.get("javaVersion"));
        Assertions.assertTrue(system.get("javaVersion").length() > 0);
        Assertions.assertNotNull(system.get("osArch"));
        Assertions.assertTrue(system.get("osArch").length() > 0);
        Assertions.assertNotNull(system.get("osName"));
        Assertions.assertTrue(system.get("osName").length() > 0);
        Assertions.assertNotNull(system.get("osVersion"));
        Assertions.assertTrue(system.get("osVersion").length() > 0);
    }

    @Test
    void copyFolderFromJarTest() {
        new File("testruns").mkdir();
        try {
            JARUTILITIES.copyFolderFromJar("main","/de/griefed/resources/lang", "testruns/langTest","", ".properties");
        } catch (Exception ignored) {}
        Assertions.assertTrue(new File("testruns/langTest").isDirectory());
        Assertions.assertTrue(new File("testruns/langTest/lang_de_de.properties").exists());
        Assertions.assertTrue(new File("testruns/langTest/lang_en_us.properties").exists());
        Assertions.assertTrue(new File("testruns/langTest/lang_uk_ua.properties").exists());
        FileUtils.deleteQuietly(new File("testruns/langTest"));
    }

}
