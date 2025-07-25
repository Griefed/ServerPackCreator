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
package de.griefed.serverpackcreator.app.updater.versionchecker

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.utilities.common.Comparison
import de.griefed.serverpackcreator.api.utilities.common.SemanticVersionComparator
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Baseclass from which GitHub and GitLab checks extend. This class mainly provides the logic for comparing versions against
 * each other to find out which is newer. Extend from this class if you want to implement your own checkers, for platforms
 * like Gitea or anything else.
 * @author Griefed
 */
@Suppress("unused")
abstract class VersionChecker {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    protected var allVersions: List<String>? = null
        private set

    /**
     * Check whether an update/newer version is available for the given version. If you want to check for PreReleases, too,
     * then make sure to pass `true` for `checkForPreReleases`.
     * @author Griefed
     * @param currentVersion The current version of the app.
     * @param checkForPreReleases `false` if you do not want to check for PreReleases. `true`
     * if you want to check for PreReleases as well.
     * @return Returns a concatenated String whether an update is available or not. Examples:<br></br>
     * No update available: `No updates available.`<br></br>
     * New release available: `Current version: 2.0.0. A new release is available: 2.1.1. Download available at: https://github.com/Griefed/ServerPackCreator/releases/tag/2.1.1`
     * New prerelease available: `Current version: 2.0.0. A new PreRelease is available: 3.0.0-alpha.14. Download available at: https://github.com/Griefed/ServerPackCreator/releases/tag/3.0.0-alpha.14`
     */
    fun checkForUpdate(currentVersion: String, checkForPreReleases: Boolean): String {
        log.debug("Current version: $currentVersion")
        return try {
            val newVersion = isUpdateAvailable(currentVersion, checkForPreReleases)
            if (newVersion == "up_to_date") {
                "No updates available."
            } else {
                newVersion + ";" + getDownloadUrl(newVersion!!)
            }
        } catch (ex: NumberFormatException) {
            log.error("A version could not be parsed into integers.", ex)
            "No updates available."
        }
    }

    /**
     * Check for new versions in beta, alpha and regular release channels. If `checkForPreRelease` is false,
     * only regular releases are checked.
     * @author Griefed
     * @param currentVersion The current version of the app.
     * @param checkForPreReleases `false` if you do not want to check for PreReleases. `true`
     * if you want to check for PreReleases as well.
     * @return Returns the available update version. If no update is available, then `up_to_date` is returned.
     */
    protected fun isUpdateAvailable(currentVersion: String, checkForPreReleases: Boolean): String? {
        if (checkForPreReleases) {
            if (isNewBetaAvailable(currentVersion)) {
                return latestBeta()
            }
            if (isNewAlphaAvailable(currentVersion)) {
                return latestAlpha()
            }
        }
        if (SemanticVersionComparator.compareSemantics(currentVersion, latestVersion(checkForPreReleases)!!, Comparison.NEW)) {
            return latestVersion(checkForPreReleases)
        } else if (currentVersion.matches("\\d+\\.\\d+\\.\\d+-(alpha|beta)\\.\\d+".toRegex()) &&
            SemanticVersionComparator.compareSemantics(currentVersion, latestVersion(false)!!, Comparison.EQUAL)
        ) {
            return latestVersion(false)
        }
        return "up_to_date"
    }

    /**
     * Check for a new alpha version.
     * @author Griefed
     * @param currentVersion The current version to check against available alpha versions.
     * @return Returns true if a new alpha release is found.
     * @throws NumberFormatException Thrown if the passed `currentVersion` can not be
     * parsed into integers.
     */
    @Throws(NumberFormatException::class)
    private fun isNewAlphaAvailable(currentVersion: String): Boolean {

        // If no alpha releases are available, do not check for new alpha release.
        if (latestAlpha() == "no_alphas") {
            return false
        }
        val latestAlpha = latestAlpha()
        if (SemanticVersionComparator.compareSemantics(currentVersion, latestAlpha, Comparison.EQUAL) && currentVersion.contains("beta")) {
            return false
        }

        // Check if the given version is older than the latest alpha version by checking semantically. (1.2.3, 2.3.4, 6.6.6)
        return if (SemanticVersionComparator.compareSemantics(currentVersion, latestAlpha, Comparison.NEW)) {
            true
        } else if (SemanticVersionComparator.compareSemantics(
                currentVersion,
                latestAlpha,
                Comparison.EQUAL_OR_NEW
            ) && currentVersion.contains("-")
        ) {
            // If a new alpha, say alpha.5 for the given, say alpha.1, is available, return true.
            isPreReleaseNewer(currentVersion, latestAlpha)
        } else {
            false
        }
    }

    /**
     * Check for a new beta version.
     * @author Griefed
     * @param currentVersion The current version to check against available beta versions.
     * @return Returns true if a new beta release is found.
     * @throws NumberFormatException Thrown if the passed `currentVersion` can not be
     * parsed into integers.
     */
    @Throws(NumberFormatException::class)
    private fun isNewBetaAvailable(currentVersion: String): Boolean {

        // If no beta releases are available, do not check for new beta release.
        if (latestBeta() == "no_betas") {
            return false
        }
        val latestBeta = latestBeta()

        // Check if the given version is older than the latest beta version by checking semantically. (1.2.3, 2.3.4, 6.6.6)
        return if (SemanticVersionComparator.compareSemantics(currentVersion, latestBeta, Comparison.NEW)) {
            true
        } else if (SemanticVersionComparator.compareSemantics(
                currentVersion,
                latestBeta,
                Comparison.EQUAL_OR_NEW
            ) && currentVersion.contains("-")
        ) {

            // If a new beta, say beta.5 for the given, say beta.1, is available, return true.
            isPreReleaseNewer(currentVersion, latestBeta)
        } else {
            false
        }
    }

    /**
     * Check whether the release number for the new version is bigger than the one of the current version, indicating a
     * newer pre-release is available.
     * @author Griefed
     * @param currentVersion The current version for which we want to check for newer versions availability.
     * @param newVersion The new version with which we want to check if it is indeed newer than the current version.
     * @return True if the new version is a newer pre-release.
     */
    private fun isPreReleaseNewer(currentVersion: String, newVersion: String): Boolean {
        val currentVersionReleaseNumber =
            currentVersion.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\\.".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                .toInt()
        val newVersionReleaseNumber =
            newVersion.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\\.".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                .toInt()
        return newVersionReleaseNumber > currentVersionReleaseNumber
    }

    /**
     * Gather all beta versions in a list.
     * @author Griefed
     * @return Returns a list of all available beta versions. If no beta releases are available for the given
     * repository, `null` is returned.
     */
    private fun allBetaVersions(): List<String>? {
        val betaVersions: MutableList<String> = ArrayList(1000)

        // Check all available versions whether they contain beta and add them to a list of beta versions.
        if (allVersions != null) {
            for (version in allVersions!!) {
                if (version.contains("beta") && !betaVersions.contains(version)) {
                    betaVersions.add(version)
                }
            }
        }
        return if (betaVersions.isEmpty()) {
            null
        } else {
            betaVersions
        }
    }

    /**
     * Gather all alpha versions in a list.
     * @author Griefed
     * @return Returns a list of all available alpha versions. If no alpha releases are available for the given
     * repository, `null` is returned.
     */
    private fun allAlphaVersions(): List<String>? {
        val alphaVersions: MutableList<String> = ArrayList(1000)

        // Check all available versions whether they contain alpha and add them to a list of alpha versions.
        if (allVersions != null) {
            for (version in allVersions!!) {
                if (version.contains("alpha") && !alphaVersions.contains(version)) {
                    alphaVersions.add(version)
                }
            }
        }
        return if (alphaVersions.isEmpty()) {
            null
        } else {
            alphaVersions
        }
    }

    /**
     * Get the latest beta release.
     * @author Griefed
     * @return Returns the latest beta release. If no beta release is available, `no_betas` is returned.
     * @throws NumberFormatException Thrown if a version can not be parsed into integers.
     */
    @Throws(NumberFormatException::class)
    protected fun latestBeta(): String {
        val betaVersions = allBetaVersions()
        var beta = "no_betas"
        if (betaVersions != null) {
            beta = betaVersions[0]
            for (betaVersion in betaVersions) {
                if (SemanticVersionComparator.compareSemantics(beta, betaVersion, Comparison.EQUAL_OR_NEW) && isPreReleaseNewer(
                        beta,
                        betaVersion
                    )
                ) {
                    beta = betaVersion
                }
            }
        }
        log.debug("Latest beta: $beta")
        return beta
    }

    /**
     * Get the latest alpha release.
     * @author Griefed
     * @return Returns the latest alpha release. If no alpha release is available, `no_alphas` is returned.
     * @throws NumberFormatException Thrown if a versions can not be parsed into integers.
     */
    @Throws(NumberFormatException::class)
    protected fun latestAlpha(): String {
        val alphaVersions = allAlphaVersions()
        var alpha = "no_alphas"
        if (alphaVersions != null) {
            alpha = alphaVersions[0]
            for (alphaVersion in alphaVersions) {
                if (SemanticVersionComparator.compareSemantics(alpha, alphaVersion, Comparison.EQUAL_OR_NEW) && isPreReleaseNewer(
                        alpha,
                        alphaVersion
                    )
                ) {
                    alpha = alphaVersion
                }
            }
        }
        log.debug("Latest alpha: $alpha")
        return alpha
    }

    /**
     * Acquire the response from a given URL.
     * @author Griefed
     * @param requestUrl The URL to get the response from.
     * @return The response from the given URL.
     * @throws IOException Thrown if the requested URL can not be reached or if any other error occurs during the request.
     */
    @Throws(IOException::class)
    protected fun getResponse(requestUrl: URL): String {
        val httpURLConnection = requestUrl.openConnection() as HttpURLConnection
        httpURLConnection.requestMethod = "GET"
        if (httpURLConnection.responseCode != 200) throw IOException("Request for " + requestUrl + " responded with " + httpURLConnection.responseCode)
        val bufferedReader = BufferedReader(
            InputStreamReader(httpURLConnection.inputStream)
        )
        var inputLine: String?
        val response = StringBuilder()
        while (bufferedReader.readLine().also { inputLine = it } != null) {
            response.append(inputLine)
        }
        bufferedReader.close()
        return response.toString()
    }

    protected val objectMapper: ObjectMapper
        get() {
            val objectMapper = ObjectMapper()
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            return objectMapper
        }

    protected abstract fun allVersions(): List<String>?
    @Throws(IOException::class)
    abstract fun refresh(): VersionChecker
    protected fun setAllVersions() {
        allVersions = allVersions()
    }

    protected abstract fun latestVersion(checkForPreRelease: Boolean): String?
    protected abstract fun getDownloadUrl(version: String): String
    @Throws(IOException::class)
    protected abstract fun setRepository()
    abstract fun check(currentVersion: String, checkForPreReleases: Boolean): Optional<Update>
}