package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.versionchecker.github.GitHubChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UpdateCheckerTest {

    private final UpdateChecker UPDATECHECKER;

    UpdateCheckerTest() {

        ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
        this.UPDATECHECKER = new UpdateChecker(new LocalizationManager(APPLICATIONPROPERTIES), APPLICATIONPROPERTIES);

    }

    @Test
    void textInstances() {
        Assertions.assertNotNull(UPDATECHECKER.getGitHub());
        Assertions.assertNotNull(UPDATECHECKER.getGitLab());
        Assertions.assertNotNull(UPDATECHECKER.getGitGriefed());
    }

    @Test
    void checkForUpdateTest() {
        String latest = UPDATECHECKER.getGitHub().latestVersion(false);
        String latestPre = UPDATECHECKER.getGitHub().latestVersion(true);

        Assertions.assertNotNull(UPDATECHECKER.refresh());

        System.out.println("Old version should return the newest regular release, whilst not checking for pre-releases.");
        Assertions.assertEquals(latest,UPDATECHECKER.checkForUpdate(
                "2.0.0",
                false
        ).split(";")[0]);

        System.out.println("Old version should return the newest pre-release release, whilst checking for pre-releases, too.");
        Assertions.assertEquals(latestPre,UPDATECHECKER.checkForUpdate(
                "2.0.0",
                true
        ).split(";")[0]);



        System.out.println("Old alpha should return the newest regular release, whilst not checking for pre-releases.");
        Assertions.assertEquals(latest,UPDATECHECKER.checkForUpdate(
                "2.0.0-alpha.2",
                false
        ).split(";")[0]);

        System.out.println("Old alpha should return the newest alpha/beta, whilst checking for pre-releases, too.");
        Assertions.assertEquals(latestPre,UPDATECHECKER.checkForUpdate(
                "2.0.0-alpha.2",
                true
        ).split(";")[0]);



        System.out.println("Old beta should return the newest regular release, whilst not checking for pre-releases.");
        Assertions.assertEquals(latest,UPDATECHECKER.checkForUpdate(
                "2.0.0-beta.2",
                false
        ).split(";")[0]);

        System.out.println("Old beta should return the newest alpha/beta, whilst checking for pre-releases, too.");
        Assertions.assertEquals(latestPre,UPDATECHECKER.checkForUpdate(
                "2.0.0-beta.2",
                true
        ).split(";")[0]);



        System.out.println("Old beta, but newer than the newest regular release, should return no available updates, whilst not checking for pre-releases.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "3.0.0-beta.2",
                false
        ).split(";")[0]);

        System.out.println("Old beta, but newer than the newest regular release, should the latest available alpha/beta, whilst checking for pre-releases, too.");
        Assertions.assertEquals(latestPre,UPDATECHECKER.checkForUpdate(
                "3.0.0-beta.2",
                true
        ).split(";")[0]);



        System.out.println("Old beta, but newer than the newest regular release, should return no available updates, whilst not checking for pre-releases.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "3.0.0-alpha.2",
                false
        ).split(";")[0]);

        System.out.println("Old alpha, but newer than the newest regular release, should return the newest alpha/beta, whilst checking for pre-releases, too.");
        Assertions.assertEquals(latestPre,UPDATECHECKER.checkForUpdate(
                "3.0.0-alpha.2",
                true
        ).split(";")[0]);



        System.out.println("Future alpha should return no available updates, whilst not checking for pre-releases.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "123.456.789-alpha.2",
                false
        ).split(";")[0]);

        System.out.println("Future alpha should return no available updates, whilst checking for pre-releases, too.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "123.456.789-alpha.2",
                true
        ).split(";")[0]);



        System.out.println("Future beta should return no available updates, whilst not checking for pre-releases.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "123.456.789-beta.2",
                false
        ).split(";")[0]);

        System.out.println("Future beta should return no available updates, whilst checking for pre-releases, too.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "123.456.789-beta.2",
                true
        ).split(";")[0]);



        System.out.println("Newer version should return no available updates, whilst not checking for pre-releases.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "123.456.789",
                false
        ));

        System.out.println("Newer version should return no available updates, whilst checking for pre-releases.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                "123.456.789",
                true
        ));



        System.out.println("Newest version should return no available updates, whilst not checking for pre-releases.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                latest,
                false
        ));

        System.out.println("Newest alpha/beta should return no available updates, whilst checking for pre-releases, too.");
        Assertions.assertEquals("No updates available.",UPDATECHECKER.checkForUpdate(
                latestPre,
                true
        ));
    }

}
