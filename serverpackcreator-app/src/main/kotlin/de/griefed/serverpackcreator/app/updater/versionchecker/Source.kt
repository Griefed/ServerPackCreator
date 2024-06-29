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

/**
 * A source-archive is the source code of the repository for the tag of the parent-release. It represents the state of the
 * code at the time of the release.
 * @author Griefed
 */
class Source
/**
 * Create a new source from an [ArchiveType] and the download-[URL] to this archive.
 * @author Griefed
 * @param archiveType Archive type of this source.
 * @param archiveUrl Download url for this source.
 */(private val archiveType: ArchiveType?, private val archiveUrl: URL) {
    /**
     * Get the [ArchiveType] of this source.
     * @author Griefed
     * @return [ArchiveType] of this source.
     */
    fun type(): ArchiveType? {
        return archiveType
    }

    /**
     * Get the download-[URL] for this source.
     * @author Griefed
     * @return Download-[URL] for this source.
     */
    fun url(): URL {
        return archiveUrl
    }
}
