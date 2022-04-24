package de.griefed.serverpackcreator.utilities.commonutilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

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
}
