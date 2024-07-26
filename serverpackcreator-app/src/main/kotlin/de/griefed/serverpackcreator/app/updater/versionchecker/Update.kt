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

import java.net.URL
import java.time.LocalDate
import java.util.*

/**
 * An instance of Update contains information about the release which is considered an update to the version which was used
 * to acquire this Update-instance.<br></br>
 * It gives you access to:<br></br>
 * - The version.<br></br>
 * - The description (release description as shown on release pages on GitLab or GitHub).<br></br>
 * - The [URL] to this release for visiting it in your browser.<br></br>
 * - The [LocalDate] at which this release was published/release.<br></br>
 * - A list of [ReleaseAsset], if any.<br></br>
 * - A list of [Source].
 *
 * @author Griefed
 * @param version The version of this update/release.
 * @param description The description (release description as shown on release pages on GitLab or GitHub), of this release/update.
 * @param link The URL to this release for visiting it in your browser.
 * @param releaseDate The date at which this release was published/release.
 * @param assets Available release-assets for this update/release, if any.
 * @param sources Available source-archives for this update/release.
 *
 * @author Griefed
 */
@Suppress("unused")
class Update(
    private val version: String,
    private val description: String?,
    private val link: URL,
    private val releaseDate: LocalDate,
    private val assets: List<ReleaseAsset>?,
    private val sources: List<Source>
) {
    /**
     * Get the version of this update/release.
     * @author Griefed
     * @return The version of this update/release.
     */
    fun version(): String {
        return version
    }

    /**
     * Get the description of this update/release, wrapped in an [Optional].
     * @author Griefed
     * @return The description of this update/release, wrapped in an [Optional].
     */
    fun description(): Optional<String> {
        return Optional.ofNullable(description)
    }

    /**
     * Get the [URL] to this release for use in your browser.
     * @author Griefed
     * @return [URL] to this release for use in your browser.
     */
    fun url(): URL {
        return link
    }

    /**
     * Get the [LocalDate] at which this release was published.
     * @author Griefed
     * @return [LocalDate] at which this release was published.
     */
    fun releaseDate(): LocalDate {
        return releaseDate
    }

    /**
     * Get the [ReleaseAsset]-list of available assets for this update/release, wrapped in an [Optional].
     * @author Griefed
     * @return [ReleaseAsset]-list of available assets for this update/release, wrapped in an [Optional].
     */
    fun assets(): Optional<List<ReleaseAsset>> {
        return Optional.ofNullable(assets)
    }

    /**
     * Get the [Source]-list of available source-archives for this update/release.
     * @author Griefed
     * @return [Source]-list of available source-archives for this update/release.
     */
    fun sources(): List<Source> {
        return sources
    }

    /**
     * Get the ZIP-archive-source of this update.
     * @author Griefed
     * @return [Source] of [ArchiveType.ZIP] of this update.
     */
    fun sourceZip(): Source? {
        for (source in sources) {
            if (source.type() == ArchiveType.ZIP) {
                return source
            }
        }
        return null
    }

    /**
     * Get the tar.gz-archive-source of this update.
     * @author Griefed
     * @return [Source] of [ArchiveType.TAR_GZ] of this update.
     */
    fun sourceTarGz(): Source? {
        for (source in sources) {
            if (source.type() == ArchiveType.TAR_GZ) {
                return source
            }
        }
        return null
    }

    /**
     * Get the tar-archive-source of this update. A tar-archive is usually only available for GitLab updates. GitHub typically
     * only provides zip- and tar.gz-archives.
     * @author Griefed
     * @return [Source] of [ArchiveType.TAR] of this update, wrapped in an [Optional].
     */
    fun sourceTar(): Optional<Source> {
        for (source in sources) {
            if (source.type() == ArchiveType.TAR) {
                return Optional.of(source)
            }
        }
        return Optional.empty()
    }

    /**
     * Get the tar.bz2-archive-source of this update. A tar.bz2-archive is usually only available for GitLab updates. GitHub typically
     * only provides zip- and tar.gz-archives.
     * @author Griefed
     * @return [Source] of [ArchiveType.TAR_BZ2] of this update, wrapped in an [Optional].
     */
    fun sourceTarBz2(): Optional<Source> {
        for (source in sources) {
            if (source.type() == ArchiveType.TAR_BZ2) {
                return Optional.of(source)
            }
        }
        return Optional.empty()
    }

    /**
     * Get a specific release-asset for a given name of said release.
     * @author Griefed
     * @param releaseName The name of the release asset.
     * @return [ReleaseAsset] for the given name, wrapped in an [Optional].
     */
    fun getReleaseAsset(releaseName: String): Optional<ReleaseAsset> {
        if (assets == null) {
            return Optional.empty()
        }
        for (releaseAsset in assets) {
            if (releaseAsset.name() == releaseName) {
                return Optional.of(releaseAsset)
            }
        }
        return Optional.empty()
    }
}
