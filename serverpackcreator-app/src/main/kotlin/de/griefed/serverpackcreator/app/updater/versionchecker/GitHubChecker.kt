/*
 * MIT License
 *
 * Copyright (c) 2022 Griefed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@file:Suppress("unused")

package de.griefed.serverpackcreator.app.updater.versionchecker

import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.utilities.common.Comparison
import de.griefed.serverpackcreator.api.utilities.common.SemanticVersionComparator
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.util.*

/**
 * Check a given GitHub repository for updates.<br></br>
 * Versions are checked for semantic-release-formatting. Meaning tags must look like the following examples:<br></br>
 * 2.0.0<br></br>
 * 2.1.1<br></br>
 * 3.0.0-alpha.1<br></br>
 * 3.0.0-alpha.13<br></br>
 * 1.2.3-beta.1<br></br>
 * and so on.
 * @author Griefed
 */
class GitHubChecker : VersionChecker {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val gitHubApi: URL
    private val gitHubApiLatest: URL
    private var repository: JsonNode? = null
    private var latest: JsonNode? = null

    /**
     * Constructs a GitHub checker with the given `user/repository` combination to allow for version checks as
     * well as version and URL acquisition.
     * @author Griefed
     * @param gitHubUserRepository GitHub `user/repository`-combination. For example `Griefed/ServerPackCreator`
     * @throws MalformedURLException Thrown if the resulting URL is malformed or otherwise invalid.
     */
    constructor(gitHubUserRepository: String) {
        gitHubApi = URI("https://api.github.com/repos/$gitHubUserRepository/releases").toURL()
        gitHubApiLatest = URI("https://api.github.com/repos/$gitHubUserRepository/releases/latest").toURL()
    }

    /**
     * Constructs a GitHub checker with the given `user/repository` combination to allow for version checks as
     * well as version and URL acquisition.
     * @author Griefed
     * @param user GitHub-user and owner of the repository.
     * @param repository GitHub repository owned by the aforementioned user.
     * @throws MalformedURLException Thrown if the resulting URL is malformed or otherwise invalid.
     */
    constructor(user: String, repository: String) {
        gitHubApi = URI("https://api.github.com/repos/$user/$repository/releases").toURL()
        gitHubApiLatest = URI("https://api.github.com/repos/$user/$repository/releases/latest").toURL()
    }

    /**
     * Refresh this GitHub-instance. Refreshes repository information, the latest version, as well as a list of all available
     * versions.
     * @author Griefed
     * @throws IOException Exception thrown if [.setRepository] or [.setLatest] encounter an error.
     * @return This GitHub-instance.
     */
    @Throws(IOException::class)
    override fun refresh(): GitHubChecker {
        setRepository()
        setLatest()
        setAllVersions()
        return this
    }

    /**
     * Check whether an update/newer version is available for the given version. If you want to check for PreReleases, too,
     * then make sure to pass `true` for `checkForPreReleases`.
     * @author Griefed
     * @param currentVersion The current version of the app.
     * @param checkForPreReleases `false` if you do not want to check for PreReleases. `true`
     * if you want to check for PreReleases as well.
     * @return [Update]-instance, wrapped in an [Optional], containing information about the available update.
     */
    override fun check(currentVersion: String, checkForPreReleases: Boolean): Optional<Update> {
        log.debug("Current version: $currentVersion")
        try {
            val newVersion = isUpdateAvailable(currentVersion, checkForPreReleases)
            if (newVersion != "up_to_date") {
                var description = "N/A"
                var releaseDate: LocalDate? = null
                val assets: MutableList<ReleaseAsset> = ArrayList()
                val sources: MutableList<Source> = ArrayList()
                for (release in repository!!) {
                    if (release["tag_name"].asText() == newVersion) {
                        description = release["body"].asText()
                        releaseDate = LocalDate.parse(
                            release["published_at"].asText()
                                .substring(0, release["published_at"].asText().lastIndexOf("T"))
                        )
                        for (asset in release["assets"]) {
                            assets.add(
                                ReleaseAsset(
                                    asset["name"].asText(),
                                    URI(asset["browser_download_url"].asText()).toURL()
                                )
                            )
                        }
                        sources.add(
                            Source(
                                ArchiveType.TAR_GZ,
                                URI(release["tarball_url"].asText()).toURL()
                            )
                        )
                        sources.add(
                            Source(
                                ArchiveType.ZIP,
                                URI(release["zipball_url"].asText()).toURL()
                            )
                        )
                        break
                    }
                }
                return Optional.of(
                    Update(
                        newVersion!!,
                        description,
                        URI(getDownloadUrl(newVersion)).toURL(),
                        releaseDate!!,
                        assets,
                        sources
                    )
                )
            }
        } catch (ex: NumberFormatException) {
            log.error("A version could not be parsed into integers.", ex)
        } catch (ex: MalformedURLException) {
            log.error("URL could not be created.", ex)
        }
        return Optional.empty()
    }

    /**
     * Gather a list of all available versions for the given repository.
     * @author Griefed
     * @return Returns a list of all available versions. If an error occurred, or no versions are available,
     * `null` is returned.
     */
    public override fun allVersions(): List<String>? {
        val versions: MutableList<String> = ArrayList(1000)
        if (repository != null) {
            // Store all available versions in a list
            for (version in repository!!) {
                if (!versions.contains(version["tag_name"].asText())) {
                    versions.add(version["tag_name"].asText())
                }
            }
        }
        log.debug("All versions: $versions")

        // In case the given repository does not have any releases
        return if (versions.isEmpty()) {
            null
        } else versions
    }

    /**
     * Get the latest regular release, or pre-release if `checkForPreRelease` is `true`.
     * @author Griefed
     * @param checkForPreRelease Whether to include alpha and beta releases for latest release versions.
     * @return Returns the latest regular release. If no regular release is available, `no_release` is returned.
     */
    public override fun latestVersion(checkForPreRelease: Boolean): String? {
        if (latest != null) {
            var version = latest!!["tag_name"].asText()
            if (checkForPreRelease) {
                val alpha = latestAlpha()
                val beta = latestBeta()
                if (beta != "no_betas" && SemanticVersionComparator.compareSemantics(version!!, beta, Comparison.NEW)) {
                    version = beta
                }
                if (alpha != "no_alphas" && SemanticVersionComparator.compareSemantics(version!!, alpha, Comparison.NEW)) {
                    version = alpha
                }
            }
            log.trace("Latest version: $latest")
            return version
        }
        return "no_release"
    }

    /**
     * Get the URL for the given release version.
     * @author Griefed
     * @param version The version for which to get the URL to.
     * @return Returns the URL to the given release version.
     */
    public override fun getDownloadUrl(version: String): String {
        if (repository != null) {
            for (tag in repository!!) {
                if (tag["tag_name"].asText() == version) {
                    return tag["html_url"].asText()
                }
            }
        }
        return "No URL found."
    }

    /**
     * Acquire this instances repository information and store it in a [JsonNode] for later use.
     * @author Griefed
     * @throws IOException Thrown if the repository can not be reached or any other unexpected error occurs.
     */
    @Throws(IOException::class)
    override fun setRepository() {
        repository = objectMapper.readTree(getResponse(gitHubApi))
    }

    /**
     * Acquires the latest version for this instance's repository.
     * @author Griefed
     * @throws IOException Thrown if the repository can not be reached or any other unexpected error occurs.
     */
    @Throws(IOException::class)
    private fun setLatest() {
        latest = objectMapper.readTree(getResponse(gitHubApiLatest))
    }
}