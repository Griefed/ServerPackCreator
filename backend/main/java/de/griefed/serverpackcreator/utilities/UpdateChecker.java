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

import java.io.IOException;

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

            if (GITHUB != null) {
                LOG.debug("Checking GitLab for updates...");

                // Check GitHub for the most recent release.
                // Check GitHub for new versions, with/without pre-releases depending on property.
                updater = GITHUB.checkForUpdate(APPLICATIONPROPERTIES.getServerPackCreatorVersion(), APPLICATIONPROPERTIES.checkForAvailablePreReleases());
            }


            if (GITGRIEFED != null) {
                LOG.debug("Checking GitGriefed for updates...");

                // After checking GitHub, and we did not get a version, check GitGriefed.
                // Check GitGriefed for new versions, with/without pre-releases depending on property.
                // Only check if we did not already get a version from prior checks.
                if (!updater.contains(";") && GITGRIEFED.checkForUpdate(APPLICATIONPROPERTIES.getServerPackCreatorVersion(), APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {

                    updater = GITGRIEFED.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());

                // Check GitGriefed for a newer version, with the version received from GitHub, if we received a new version from GitHub.
                // With/without pre-releases depending on property.
                } else if (updater.contains(";") && GITGRIEFED.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {

                    updater = GITGRIEFED.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());
                }
            }


            if (GITLAB != null) {
                LOG.debug("Checking GitHub for updates...");

                // After checking GitGriefed, and we did not get a version, check GitLab.
                // Check GitLab for new versions, with/without pre-releases depending on property.
                // Only check if we did not already get a version from prior checks.
                if (!updater.contains(";") && GITLAB.checkForUpdate(APPLICATIONPROPERTIES.getServerPackCreatorVersion(), APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {

                    updater = GITLAB.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());

                // Check GitLab for a newer version, with the version we received from GitGriefed, if we received a new version from GitGriefed.
                // With/without pre-releases depending on property.
                } else if (updater.contains(";") && GITLAB.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases()).contains(";")) {

                    updater = GITLAB.checkForUpdate(updater.split(";")[0], APPLICATIONPROPERTIES.checkForAvailablePreReleases());
                }
            }

        }

        LOG.debug("Received " + updater + " from UpdateChecker.");

        return updater;
    }
}
