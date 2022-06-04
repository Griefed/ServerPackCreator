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
import de.griefed.serverpackcreator.utilities.misc.Generated;
import de.griefed.versionchecker.GitHubChecker;
import de.griefed.versionchecker.GitLabChecker;
import de.griefed.versionchecker.Update;
import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Initialize our GitHub and GitLab instances with the corresponding repository addresses, so we can
 * then run our update checks later on.
 *
 * @author Griefed
 */
@Generated
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
   *
   * @param injectedLocalizationManager Instance of {@link LocalizationManager}.
   * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
   * @author Griefed
   */
  @Autowired
  public UpdateChecker(
      LocalizationManager injectedLocalizationManager,
      ApplicationProperties injectedApplicationProperties) {

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
      this.GITLAB =
          new GitLabChecker("https://gitlab.com/api/v4/projects/32677538/releases").refresh();
    } catch (IOException ex) {
      LOG.error("The GitLab URL you set resulted in a malformed URL.", ex);
      this.GITLAB = null;
    }
    try {
      this.GITGRIEFED =
          new GitLabChecker("https://git.griefed.de/api/v4/projects/63/releases").refresh();
    } catch (IOException ex) {
      LOG.error("The GitGriefed URL you set resulted in a malformed URL.", ex);
      this.GITGRIEFED = null;
    }
  }

  /**
   * Refresh the GitHub, GitLab and GitGriefed instances, so we get the most current releases.
   *
   * @return {@link UpdateChecker} reference.
   * @author Griefed
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
   *
   * @return {@link GitHubChecker}.
   * @author Griefed
   */
  public GitHubChecker getGitHub() {
    return GITHUB;
  }

  /**
   * Getter for the instance of our {@link GitLabChecker} for GitLab.
   *
   * @return {@link GitLabChecker}.
   * @author Griefed
   */
  public GitLabChecker getGitLab() {
    return GITLAB;
  }

  /**
   * Getter for the instance of our {@link GitLabChecker} for GitGriefed.
   *
   * @return {@link GitLabChecker}.
   * @author Griefed
   */
  public GitLabChecker getGitGriefed() {
    return GITGRIEFED;
  }

  /**
   * Check our GitLab, GitGriefed and GitHub instances for updates, sequentially, and then return
   * the update.
   *
   * @param version {@link String} The version for which to check for updates.
   * @param preReleaseCheck {@link Boolean} Whether to check pre-releasesDescending as well. Use
   *     <code>true</code> to check pre-releasesDescending as well, <Code>false</Code> to only check
   *     with regular releases.
   * @return {@link String} The update, if available, as well as the download URL.
   * @author Griefed
   */
  public Optional<Update> checkForUpdate(@NotNull String version, Boolean preReleaseCheck) {

    if (version.equalsIgnoreCase("dev")) {
      return Optional.empty();
    }

    Optional<Update> update = Optional.empty();

    if (GITHUB != null) {
      LOG.debug("Checking GitHub for updates...");

      // Check GitHub for the most recent release.
      update = GITHUB.check(version, preReleaseCheck);
    }

    if (GITGRIEFED != null) {
      LOG.debug("Checking GitGriefed for updates...");

      // After checking GitHub, and we did not get a version, check GitGriefed.
      if (update.isPresent()
          && GITGRIEFED.check(update.get().version(), preReleaseCheck).isPresent()) {

        update = GITGRIEFED.check(update.get().version(), preReleaseCheck);

        // Check GitGriefed for a newer version, with the version received from GitHub, if we
        // received a new version from GitHub.
      } else if (!update.isPresent()) {

        update = GITGRIEFED.check(version, preReleaseCheck);
      }
    }

    if (GITLAB != null) {
      LOG.debug("Checking GitLab for updates...");

      // After checking GitGriefed, and we did not get a version, check GitLab.
      if (update.isPresent() && GITLAB.check(update.get().version(), preReleaseCheck).isPresent()) {

        update = GITLAB.check(update.get().version(), preReleaseCheck);

        // Check GitLab for a newer version, with the version we received from GitGriefed, if we
        // received a new version from GitGriefed.
      } else if (!update.isPresent()) {

        update = GITLAB.check(version, preReleaseCheck);
      }
    }

    return update;
  }
}
