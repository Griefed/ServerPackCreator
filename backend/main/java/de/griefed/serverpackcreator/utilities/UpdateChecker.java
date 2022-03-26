/* Copyright (C) 2022  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.versionchecker.github.GitHubChecker;
import de.griefed.versionchecker.gitlab.GitLabChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Initialize our GitHub and GitLab instances with the corresponding repository addresses, so we can then run our update
 * checks later on.
 * @author Griefed
 */
@Service
public class UpdateChecker {

    private static final Logger LOG = LogManager.getLogger(UpdateChecker.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private GitHubChecker GITHUB;
    private GitLabChecker GITGRIEFED;
    private GitLabChecker GITLAB;

    /**
     * Constructor for Dependency Injection.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager}.
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
     */
    @Autowired
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

        try {
            this.GITHUB = new GitHubChecker("Griefed/ServerPackCreator").refresh();
        } catch (IOException ex) {
            LOG.error("The GitHub user/repository you set resulted in a malformed URL.", ex);
            this.GITHUB = null;
        }

        try {
            this.GITLAB = new GitLabChecker("https://gitlab.com/api/v4/projects/32677538/releases").refresh();
        } catch (IOException ex) {
            LOG.error("The GitLab URL you set resulted in a malformed URL.", ex);
            this.GITLAB = null;
        }
        try {
            this.GITGRIEFED = new GitLabChecker("https://git.griefed.de/api/v4/projects/63/releases").refresh();
        } catch (IOException ex) {
            LOG.error("The GitGriefed URL you set resulted in a malformed URL.", ex);
            this.GITGRIEFED = null;
        }
    }

    /**
     * Refresh the GitHub, GitLab and GitGriefed instances, so we get the most current releases.
     * @author Griefed
     * @return {@link UpdateChecker} reference.
     */
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

    /**
     * Getter for the instance of our {@link GitHubChecker}.
     * @author Griefed
     * @return {@link GitHubChecker}.
     */
    public GitHubChecker getGitHub() {
        return GITHUB;
    }

    /**
     * Getter for the instance of our {@link GitLabChecker} for GitLab.
     * @author Griefed
     * @return {@link GitLabChecker}.
     */
    public GitLabChecker getGitLab() {
        return GITLAB;
    }

    /**
     * Getter for the instance of our {@link GitLabChecker} for GitGriefed.
     * @author Griefed
     * @return {@link GitLabChecker}.
     */
    public GitLabChecker getGitGriefed() {
        return GITGRIEFED;
    }

    /**
     * Check our GitLab, GitGriefed and GitHub instances for updates, sequentially, and then return the update.
     * @author Griefed
     * @param version {@link String} The version for which to check for updates.
     * @param preReleaseCheck {@link Boolean} Whether to check pre-releases as well. Use <code>true</code> to check pre-releases
     *                                       as well, <Code>false</Code> to only check with regular releases.
     * @return {@link String} The update, if available, as well as the download URL.
     */
    public String checkForUpdate(@NotNull String version, Boolean preReleaseCheck) {

        if (version.equalsIgnoreCase("dev")) {
            return LOCALIZATIONMANAGER.getLocalizedString("updates.log.info.none");
        }

        String updater = null;

        if (GITHUB != null) {
            LOG.debug("Checking GitHub for updates...");

            // Check GitHub for the most recent release.
            updater = GITHUB.checkForUpdate(version, preReleaseCheck);
        }

        if (GITGRIEFED != null && updater != null) {
            LOG.debug("Checking GitGriefed for updates...");

            // After checking GitHub, and we did not get a version, check GitGriefed.
            if (!updater.contains(";") && GITGRIEFED.checkForUpdate(version, preReleaseCheck).contains(";")) {

                updater = GITGRIEFED.checkForUpdate(version, preReleaseCheck);

            // Check GitGriefed for a newer version, with the version received from GitHub, if we received a new version from GitHub.
            } else if (updater.contains(";") && GITGRIEFED.checkForUpdate(updater.split(";")[0], preReleaseCheck).contains(";")) {

                updater = GITGRIEFED.checkForUpdate(updater.split(";")[0], preReleaseCheck);
            }
        }


        if (GITLAB != null && updater != null) {
            LOG.debug("Checking GitLab for updates...");

            // After checking GitGriefed, and we did not get a version, check GitLab.
            if (!updater.contains(";") && GITLAB.checkForUpdate(version, preReleaseCheck).contains(";")) {

                updater = GITLAB.checkForUpdate(version, preReleaseCheck);

            // Check GitLab for a newer version, with the version we received from GitGriefed, if we received a new version from GitGriefed.
            } else if (updater.contains(";") && GITLAB.checkForUpdate(updater.split(";")[0], preReleaseCheck).contains(";")) {

                updater = GITLAB.checkForUpdate(updater.split(";")[0], preReleaseCheck);
            }
        }


        LOG.debug("Received " + updater + " from UpdateChecker.");


        if (updater == null) {
            return LOCALIZATIONMANAGER.getLocalizedString("updates.log.info.none");
        } else {
            return updater;
        }
    }
}
