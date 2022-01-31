package de.griefed.serverpackcreator.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.versionchecker.github.GitHubChecker;
import de.griefed.serverpackcreator.utilities.versionchecker.gitlab.GitLabChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdateChecker {

    private static final Logger LOG = LogManager.getLogger(UpdateChecker.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private GitHubChecker GITHUB;
    private GitLabChecker GITGRIEFED;
    private GitLabChecker GITLAB;

    public UpdateChecker(LocalizationManager injectedLocalizationManager, ApplicationProperties injectedApplicationProperties) {

        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        this.GITHUB = new GitHubChecker(LOCALIZATIONMANAGER);
        this.GITLAB = new GitLabChecker(LOCALIZATIONMANAGER,"https://gitlab.com/api/v4/projects/32677538/releases");
        this.GITGRIEFED = new GitLabChecker(LOCALIZATIONMANAGER, "https://git.griefed.de/api/v4/projects/63/releases");
    }

    public UpdateChecker refresh() {
        try {
            this.GITHUB.refresh();
        } catch (Exception ex) {
            LOG.error("Error refreshing GitHub.", ex);
            this.GITHUB = null;
        }
        try {
            this.GITLAB.refresh();
        } catch (Exception ex) {
            LOG.error("Error refreshing GitLab.", ex);
            this.GITLAB = null;
        }
        try {
            this.GITGRIEFED.refresh();
        } catch (Exception ex) {
            LOG.error("Error refreshing GitGriefed.", ex);
            this.GITGRIEFED = null;
        }
        return this;
    }

    public String checkForUpdate() {
        String updater = LOCALIZATIONMANAGER.getLocalizedString("updates.log.info.none");

        if (!APPLICATIONPROPERTIES.getServerPackCreatorVersion().equals("dev")) {

            //Check GitLab mirror for most recent release
            LOG.debug("Checking GitLab for updates...");
            if (GITHUB != null) {
                updater = GITHUB.checkForUpdate(APPLICATIONPROPERTIES.getServerPackCreatorVersion(), APPLICATIONPROPERTIES.checkForAvailablePreReleases());
            }


            //After checking GitLab, and we did not get a version, check GitGriefed source
            if (GITGRIEFED != null) {
                LOG.debug("Checking GitGriefed for updates...");
                if (!updater.contains(";") && GITGRIEFED.checkForUpdate(APPLICATIONPROPERTIES.getServerPackCreatorVersion(), APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {
                    updater = GITGRIEFED.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());
                } else if (updater.contains(";") && GITGRIEFED.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {
                    updater = GITGRIEFED.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());
                }
            }

            //After checking GitGriefed, and we did not get a version, check GitHub mirror
            if (GITLAB != null) {
                LOG.debug("Checking GitHub for updates...");
                if (!updater.contains(";") && GITLAB.checkForUpdate(APPLICATIONPROPERTIES.getServerPackCreatorVersion(), APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {
                    updater = GITLAB.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());
                } else if (updater.contains(";") && GITLAB.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {
                    updater = GITLAB.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());
                }
            }

        }

        LOG.debug("Received " + updater + " from UpdateChecker.");

        return updater;
    }
}
