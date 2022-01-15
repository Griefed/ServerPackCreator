package de.griefed.serverpackcreator.utilities.versionchecker.github;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GitHubCheckerTests {

    private final GitHubChecker gitHubChecker = new GitHubChecker(new LocalizationManager());

    @Test
    void checkForNewUpdate() {
        Assertions.assertNotEquals(
                "2.0.0",
                gitHubChecker.checkForUpdate("2.0.0", false)
        );
        Assertions.assertNotEquals(
                "No updates available.",
                gitHubChecker.checkForUpdate("2.0.0", false)
        );
    }

    @Test
    void checkForNewAlpha() {
        Assertions.assertNotEquals(
                "2.0.0",
                gitHubChecker.checkForUpdate("2.0.0", true)
        );
        Assertions.assertNotEquals(
                "No updates available. No PreReleases available.",
                gitHubChecker.checkForUpdate("2.0.0", true)
        );
    }

    @Test
    void downloadUrlTest() {
        Assertions.assertEquals(
                "https://github.com/Griefed/ServerPackCreator/releases/tag/3.0.0-alpha.14",
                gitHubChecker.getDownloadUrl("3.0.0-alpha.14")
        );
    }

    @Test
    void latestVersionTest() {
        Assertions.assertNotEquals("no_release", gitHubChecker.latestVersion());
    }

    @Test
    void allVersionsTest() {
        Assertions.assertNotNull(gitHubChecker.allVersions());
        Assertions.assertTrue(gitHubChecker.allVersions().size() > 0);
    }

    @Test
    void getAssetsDownloadUrlsTest() {
        List<String> assets = gitHubChecker.getAssetsDownloadUrls("3.0.0-alpha.14");
        Assertions.assertEquals(4, assets.size());
        Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-javadoc.jar"));
        Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-sources.jar"));
        Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.exe"));
        Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.jar"));
    }
}