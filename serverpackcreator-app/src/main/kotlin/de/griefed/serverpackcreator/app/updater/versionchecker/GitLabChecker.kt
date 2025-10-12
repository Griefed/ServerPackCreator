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
 * Check a given GitLab repository for updates.<br></br>
 * Versions are checked for semantic-release-formatting. Meaning tags must look like the following examples:<br></br>
 * 2.0.0<br></br>
 * 2.1.1<br></br>
 * 3.0.0-alpha.1<br></br>
 * 3.0.0-alpha.13<br></br>
 * 1.2.3-beta.1<br></br>
 * and so on.
 * @author Griefed
 */
class GitLabChecker : VersionChecker {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val gitLabApi: URL
    private var repository: JsonNode? = null

    /**
     * Constructs a GitLab checker with the given GitLab-URL to allow for version checks as well as version and URL
     * acquisition.
     * @author Griefed
     * @param repositoryUrl The full /api/v4-GitLab-repository-URL you want to check. Examples:<br></br>
     * `https://gitlab.com/api/v4/projects/32677538/releases`<br></br>
     * `https://git.griefed.de/api/v4/projects/63/releases`
     * @throws MalformedURLException Thrown if the resulting URL is malformed or otherwise invalid.
     */
    constructor(repositoryUrl: String) {
        gitLabApi = URI(repositoryUrl).toURL()
    }

    /**
     * Constructs a GitLab checker with the given GitLab-URL to allow for version checks as well as version and URL
     * acquisition.
     * @author Griefed
     * @param repositoryUrl The full /api/v4-GitLab-repository-URL you want to check. Examples:<br></br>
     * `https://gitlab.com/api/v4/projects/32677538/releases`<br></br>
     * `https://git.griefed.de/api/v4/projects/63/releases`
     */
    constructor(repositoryUrl: URL) {
        gitLabApi = repositoryUrl
    }

    /**
     * Refresh this GitLab-instance. Refreshes repository information and the list of all available versions.
     * @author Griefed
     * @throws IOException Exception thrown if [.setRepository] encounters an error.
     * @return This GitLab-instance.
     */
    @Throws(IOException::class)
    override fun refresh(): GitLabChecker {
        setRepository()
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
     * @return [Update]-instance, wrapped in an [Optional], contianing information about the available update.
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
                        description = release["description"].asText()
                        releaseDate = LocalDate.parse(
                            release["released_at"].asText()
                                .substring(0, release["released_at"].asText().lastIndexOf("T"))
                        )
                        for (asset in release["assets"]["links"]) {
                            assets.add(
                                ReleaseAsset(
                                    asset["name"].asText(),
                                    URI(asset["direct_asset_url"].asText()).toURL()
                                )
                            )
                        }
                        for (source in release["assets"]["sources"]) {
                            var type: ArchiveType? = null
                            when (source["format"].asText()) {
                                "zip" -> type = ArchiveType.ZIP
                                "tar.gz" -> type = ArchiveType.TAR_GZ
                                "tar.bz2" -> type = ArchiveType.TAR_BZ2
                                "tar" -> type = ArchiveType.TAR
                            }
                            sources.add(
                                Source(
                                    type,
                                    URI(source["url"].asText()).toURL()
                                )
                            )
                        }
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
     * Get the latest regular release.
     * @author Griefed
     * @param checkForPreRelease Whether to include alpha and beta releases for latest release versions.
     * @return Returns the latest regular release. If no regular release is available, `no_release` is returned.
     */
    public override fun latestVersion(checkForPreRelease: Boolean): String? {
        if (allVersions != null) {
            var latest: String? = null
            if (checkForPreRelease) {
                latest = allVersions!![0]
            } else {
                for (version in allVersions!!) {
                    if (!version.contains("alpha") && !version.contains("beta")) {
                        latest = version
                        break
                    }
                }
            }
            if (latest == null) {
                return "no_release"
            }
            for (version in allVersions!!) {
                log.debug("version: $version")
                if (!version.contains("alpha") && !version.contains("beta") && SemanticVersionComparator.compareSemantics(
                        latest!!,
                        version,
                        Comparison.NEW
                    )
                ) {
                    latest = version
                }
            }
            if (checkForPreRelease) {
                val alpha = latestAlpha()
                val beta = latestBeta()
                if (beta != "no_betas" && SemanticVersionComparator.compareSemantics(latest!!, beta, Comparison.NEW)) {
                    latest = beta
                }
                if (alpha != "no_alphas" && SemanticVersionComparator.compareSemantics(latest!!, alpha, Comparison.NEW)) {
                    latest = alpha
                }
            }
            return latest
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
                    return tag["_links"]["self"].asText()
                }
            }
        }
        return "No URL found."
    }

    /**
     * Set the repository JsonNode, for the given `GITLAB_API`-URL this GitLabChecker-instance was initialized
     * with, so we can retrieve information from it later on.
     * @author Griefed
     * @throws IOException Thrown if the set repository can not be reached or the URL is malformed in any way.
     */
    @Throws(IOException::class)
    override fun setRepository() {
        repository = objectMapper.readTree(getResponse(gitLabApi))
    }
}