package de.griefed.serverpackcreator.utilities.versionchecker.gitlab;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GitLabCheckerTests {

    private final GitLabChecker gitLabChecker = new GitLabChecker(new LocalizationManager());

    @Test
    void checkForNewUpdate() {
        Assertions.assertNotEquals(
                "2.0.0",
                gitLabChecker.checkForUpdate("2.0.0", false)
        );
        Assertions.assertNotEquals(
                "No updates available.",
                gitLabChecker.checkForUpdate("2.0.0", false)
        );
    }

    @Test
    void checkForNewAlpha() {
        Assertions.assertNotEquals(
                "2.0.0",
                gitLabChecker.checkForUpdate("2.0.0", true)
        );
        Assertions.assertNotEquals(
                "No updates available. No PreReleases available.",
                gitLabChecker.checkForUpdate("2.0.0", true)
        );
    }

    @Test
    void downloadUrlTest() {
        Assertions.assertEquals(
                "https://git.griefed.de/Griefed/ServerPackCreator/-/releases/3.0.0-alpha.14",
                gitLabChecker.getDownloadUrl("3.0.0-alpha.14")
        );
    }

    @Test
    void latestVersionTest() {
        Assertions.assertNotEquals("no_release", gitLabChecker.latestVersion());
    }

    @Test
    void allVersionsTest() {
        Assertions.assertNotNull(gitLabChecker.allVersions());
        Assertions.assertTrue(gitLabChecker.allVersions().size() > 0);
    }

    @Test
    void getAssetsDownloadUrlsTest() {
        List<String> assets = gitLabChecker.getAssetsDownloadUrls("3.0.0-alpha.14");
        Assertions.assertEquals(4, assets.size());
        Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-sources.jar"));
        Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-javadoc.jar"));
        Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.jar"));
        Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.exe"));
    }
}
