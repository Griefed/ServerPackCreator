package de.griefed.serverpackcreator.utilities.versionchecker;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.versionchecker.github.GitHubChecker;
import de.griefed.serverpackcreator.utilities.versionchecker.gitlab.GitLabChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

public class VersionCheckerTests {

    @Test
    void gitHubCheckTests() {
        try {
            GitHubChecker gitHubChecker = new GitHubChecker(new LocalizationManager());
            Assertions.assertNotEquals(
                    "2.0.0",
                    gitHubChecker.checkForUpdate("2.0.0", false)
            );
            Assertions.assertNotEquals(
                    "No updates available.",
                    gitHubChecker.checkForUpdate("2.0.0", false)
            );

            Assertions.assertNotEquals(
                    "2.0.0",
                    gitHubChecker.checkForUpdate("2.0.0", true)
            );
            Assertions.assertNotEquals(
                    "No updates available. No PreReleases available.",
                    gitHubChecker.checkForUpdate("2.0.0", true)
            );

            Assertions.assertEquals(
                    "https://github.com/Griefed/ServerPackCreator/releases/tag/3.0.0-alpha.14",
                    gitHubChecker.getDownloadUrl("3.0.0-alpha.14")
            );

            Assertions.assertNotEquals("no_release", gitHubChecker.latestVersion());

            Assertions.assertNotNull(gitHubChecker.allVersions());
            Assertions.assertTrue(gitHubChecker.allVersions().size() > 0);

            List<String> assets = gitHubChecker.getAssetsDownloadUrls("3.0.0-alpha.14");
            Assertions.assertEquals(4, assets.size());
            Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-javadoc.jar"));
            Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-sources.jar"));
            Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.exe"));
            Assertions.assertTrue(assets.contains("https://github.com/Griefed/ServerPackCreator/releases/download/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.jar"));
        } catch (JsonProcessingException | HttpClientErrorException ex) {

            System.out.println("GitHub API rate limit reached or GitHub otherwise unavailable.");

        }
    }

    @Test
    void gitLabCheckTests() {
        try {
            GitLabChecker gitLabChecker = new GitLabChecker(new LocalizationManager());

            Assertions.assertNotEquals(
                    "2.0.0",
                    gitLabChecker.checkForUpdate("2.0.0", false)
            );
            Assertions.assertNotEquals(
                    "No updates available.",
                    gitLabChecker.checkForUpdate("2.0.0", false)
            );

            Assertions.assertNotEquals(
                    "2.0.0",
                    gitLabChecker.checkForUpdate("2.0.0", true)
            );
            Assertions.assertNotEquals(
                    "No updates available. No PreReleases available.",
                    gitLabChecker.checkForUpdate("2.0.0", true)
            );

            Assertions.assertEquals(
                    "https://git.griefed.de/Griefed/ServerPackCreator/-/releases/3.0.0-alpha.14",
                    gitLabChecker.getDownloadUrl("3.0.0-alpha.14")
            );

            Assertions.assertNotEquals("no_release", gitLabChecker.latestVersion());

            Assertions.assertNotNull(gitLabChecker.allVersions());
            Assertions.assertTrue(gitLabChecker.allVersions().size() > 0);

            List<String> assets = gitLabChecker.getAssetsDownloadUrls("3.0.0-alpha.14");
            Assertions.assertEquals(4, assets.size());
            Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-sources.jar"));
            Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14-javadoc.jar"));
            Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.jar"));
            Assertions.assertTrue(assets.contains("https://git.griefed.de/api/v4/projects/63/packages/generic/ServerPackCreator/3.0.0-alpha.14/ServerPackCreator-3.0.0-alpha.14.exe"));
        } catch (JsonProcessingException | HttpClientErrorException ex) {
            System.out.println("GitLab API rate limit reached or git.griefed otherwise unavailable.");
        }

    }

}
