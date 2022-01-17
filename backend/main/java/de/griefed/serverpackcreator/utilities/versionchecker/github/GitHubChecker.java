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
package de.griefed.serverpackcreator.utilities.versionchecker.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.versionchecker.VersionChecker;
import de.griefed.serverpackcreator.utilities.versionchecker.gitlab.GitLabChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Check a given repository, by a given user, for updates.<br>
 * Versions are checked for semantic-release-formatting. Meaning tags must look like the following examples:<br>
 * 2.0.0<br>
 * 2.1.1<br>
 * 3.0.0-alpha.1<br>
 * 3.0.0-alpha.13<br>
 * 1.2.3-beta.1<br>
 * and so on.
 * @author Griefed
 */
public class GitHubChecker extends VersionChecker {

    private static final Logger LOG = LogManager.getLogger(GitHubChecker.class);

    private final String GITHUB_API = "https://api.github.com/repos/Griefed/ServerPackCreator/releases";
    private final String GITHUB_API_LATEST = "https://api.github.com/repos/Griefed/ServerPackCreator/releases/latest";
    private final RestTemplate REST_TEMPLATE = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();

    private JsonNode repository;
    private JsonNode latest;

    /**
     * Constructor for the GitHub checker. Requires the username <code>user</code> for which the given repository
     * <code>repository</code> will make up the URL called for checks.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} for i18n.
     * @throws JsonProcessingException Thrown if the requested repository couldn't be reached.
     * @throws HttpClientErrorException Thrown if the GitHub API rate limit was exceeded.
     */
    public GitHubChecker(LocalizationManager injectedLocalizationManager) throws JsonProcessingException, HttpClientErrorException {

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        setRepository();
        setLatest();
        setAllVersions();
    }

    /**
     * Gather a list of all available versions for the given repository.
     * @author Griefed
     * @return List String. Returns a list of all available versions. If an error occurred, or no versions are available,
     * <code>null</code> is returned.
     */
    @Override
    public List<String> allVersions() {
        List<String> versions = new ArrayList<>(1000);

        if (repository != null) {
            // Store all available versions in a list
            for (JsonNode version : repository) {
                if (!versions.contains(version.get("tag_name").asText())) {
                    versions.add(version.get("tag_name").asText());
                }
            }
        }

        // In case the given repository does not have any releases
        if (versions == null || versions.size() == 0) {
            return null;
        }

        return versions;
    }

    /**
     * Get the latest regular release.
     * @author Griefed
     * @return String. Returns the latest regular release. If no regular release is available, <code>no_release</code> is returned.
     */
    @Override
    public String latestVersion() {
        if (latest != null) {
            return latest.get("tag_name").asText();
        }

        if (!latestBeta().equals("no_betas")) return latestBeta();

        if (!latestAlpha().equals("no_alphas")) return latestAlpha();

        return null;

    }

    /**
     * Get the URL for the given release version.
     * @author Griefed
     * @param version String. The version for which to get the URL to.
     * @return String. Returns the URL to the given release version.
     */
    @Override
    public String getDownloadUrl(String version) {

        if (repository != null) {
            for (JsonNode tag : repository) {
                if (tag.get("tag_name").asText().equals(version)) {
                    return tag.get("html_url").asText();
                }
            }
        }

        return "No URL found.";
    }

    @Override
    protected void setRepository() throws JsonProcessingException, HttpClientErrorException {
        this.repository = getObjectMapper().readTree(REST_TEMPLATE.getForEntity(GITHUB_API, String.class).getBody());

    }

    private void setLatest() throws JsonProcessingException, HttpClientErrorException {
        this.latest = getObjectMapper().readTree(REST_TEMPLATE.getForEntity(GITHUB_API_LATEST, String.class).getBody());
    }

    /**
     * Get the asset download URLs for a particular tag/version.
     * @author Griefed
     * @param requestedVersion String. The version you want to retrieve the asset download URLs for.
     * @return List String. A list of download URLs for the assets of the given tag/version.
     */
    @Override
    public List<String> getAssetsDownloadUrls(String requestedVersion) {

        List<String> assetUrls = new ArrayList<>(20);

        if (repository != null) {
            for (JsonNode version : repository) {

                if (version.get("tag_name").asText().equals(requestedVersion)) {

                    for (JsonNode asset : version.get("assets")) {

                        if (!assetUrls.contains(asset.get("browser_download_url").asText())) {
                            assetUrls.add(asset.get("browser_download_url").asText());
                        }

                    }

                }

            }
        }

        return assetUrls;
    }
}
