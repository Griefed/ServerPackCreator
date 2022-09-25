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
@Service
public final class UpdateChecker {

  private static final Logger LOG = LogManager.getLogger(UpdateChecker.class);

  private GitHubChecker gitHub;
  private GitLabChecker gitGriefed;
  private GitLabChecker gitLab;

  /**
   * Constructor for Dependency Injection.
   *
   * @author Griefed
   */
  @Autowired
  public UpdateChecker() {

    try {
      this.gitHub = new GitHubChecker("Griefed/ServerPackCreator").refresh();
    } catch (IOException ex) {
      LOG.error(
          "Either GitHub is currently unreachable, or the GitHub user/repository you set resulted in a malformed URL. "
              + ex.getMessage());
      this.gitHub = null;
    }

    try {
      this.gitLab =
          new GitLabChecker("https://gitlab.com/api/v4/projects/32677538/releases").refresh();
    } catch (IOException ex) {
      LOG.error(
          "Either GitLab is currently unreachable, or the GitLab URL you set resulted in a malformed URL."
              + ex.getMessage());
      this.gitLab = null;
    }
    try {
      this.gitGriefed =
          new GitLabChecker("https://git.griefed.de/api/v4/projects/63/releases").refresh();
    } catch (IOException ex) {
      LOG.error(
          "Either Git.Griefed is currently unavailable, or the Git.Griefed URL you set resulted in a malformed URL."
              + ex.getMessage());
      this.gitGriefed = null;
    }
  }

  /**
   * Refresh the GitHub, GitLab and GitGriefed instances, so we get the most current releases.
   *
   * @author Griefed
   */
  public void refresh() {
    if (gitHub != null) {
      try {
        gitHub.refresh();
      } catch (Exception ex) {
        LOG.error("Error refreshing GitHub.", ex);
        gitHub = null;
      }
    }

    if (gitLab != null) {
      try {
        gitLab.refresh();
      } catch (Exception ex) {
        LOG.error("Error refreshing GitLab.", ex);
        gitLab = null;
      }
    }

    if (gitGriefed != null) {
      try {
        gitGriefed.refresh();
      } catch (Exception ex) {
        LOG.error("Error refreshing GitGriefed.", ex);
        gitGriefed = null;
      }
    }
  }

  /**
   * Check our GitLab, GitGriefed and GitHub instances for updates, sequentially, and then return
   * the update.
   *
   * @param version         The version for which to check for updates.
   * @param preReleaseCheck Whether to check pre-releasesDescending as well. Use {@code true} to
   *                        check pre-releasesDescending as well, {@code false} to only check with
   *                        regular releases.
   * @return The update, if available, as well as the download URL.
   * @author Griefed
   */
  public Optional<Update> checkForUpdate(@NotNull String version, Boolean preReleaseCheck) {

    if (version.equalsIgnoreCase("dev")) {
      return Optional.empty();
    }

    Optional<Update> update = Optional.empty();

    if (gitHub != null) {
      LOG.debug("Checking GitHub for updates...");

      // Check GitHub for the most recent release.
      update = gitHub.check(version, preReleaseCheck);
    }

    if (gitGriefed != null) {
      LOG.debug("Checking GitGriefed for updates...");

      // After checking GitHub, and we did not get a version, check GitGriefed.
      if (update.isPresent()
          && gitGriefed.check(update.get().version(), preReleaseCheck).isPresent()) {

        update = gitGriefed.check(update.get().version(), preReleaseCheck);

        // Check GitGriefed for a newer version, with the version received from GitHub, if we
        // received a new version from GitHub.
      } else if (!update.isPresent()) {

        update = gitGriefed.check(version, preReleaseCheck);
      }
    }

    if (gitLab != null) {
      LOG.debug("Checking GitLab for updates...");

      // After checking GitGriefed, and we did not get a version, check GitLab.
      if (update.isPresent() && gitLab.check(update.get().version(), preReleaseCheck).isPresent()) {

        update = gitLab.check(update.get().version(), preReleaseCheck);

        // Check GitLab for a newer version, with the version we received from GitGriefed, if we
        // received a new version from GitGriefed.
      } else if (!update.isPresent()) {

        update = gitLab.check(version, preReleaseCheck);
      }
    }

    return update;
  }
}
