package de.griefed.serverpackcreator.utilities.commonutilities;

import de.griefed.serverpackcreator.utilities.commonutilities.WebUtilities;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

public class WebUtilitiesTest {

    private final WebUtilities WEB_UTILITIES;

    WebUtilitiesTest() {
        this.WEB_UTILITIES = new WebUtilities();
    }

    @Test
    void downloadFileTest() {
        try {
            WEB_UTILITIES.downloadFile("Fabric-Server-Launcher.jar",new URL("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar"));
        } catch (Exception ignored) {}
        Assertions.assertTrue(new File("Fabric-Server-Launcher.jar").exists());
        FileUtils.deleteQuietly(new File("Fabric-Server-Launcher.jar"));
        try {
            WEB_UTILITIES.downloadFile("some_foooooolder/foooobar/Fabric-Server-Launcher.jar",new URL("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar"));
        } catch (Exception ignored) {}
        Assertions.assertTrue(new File("some_foooooolder/foooobar/Fabric-Server-Launcher.jar").exists());
        FileUtils.deleteQuietly(new File("some_foooooolder"));
    }
}
