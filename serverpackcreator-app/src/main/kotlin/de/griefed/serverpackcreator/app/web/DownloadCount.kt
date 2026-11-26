/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.app.web

import org.springframework.http.HttpMethod
import org.springframework.http.HttpRange

/**
 * Decides whether a request on an archive download route starts a new download, so that a resumed or
 * segmented download is counted once rather than once per request.
 */
object DownloadCount {

    /**
     * True for a `GET` without a `Range` header, or one whose first range starts at the archive's first byte —
     * the opening request of a download manager. A `HEAD`, a resume, every further segment, a malformed or
     * unsatisfiable range and a `bytes=0-0` size probe (unless that one byte is the whole archive) are not new
     * downloads.
     */
    fun isNewDownload(method: HttpMethod, range: String?, archiveLength: Long): Boolean {
        if (method != HttpMethod.GET) {
            return false
        }
        if (range.isNullOrBlank()) {
            return true
        }
        return try {
            val firstRange = HttpRange.parseRanges(range).firstOrNull() ?: return true
            val sizeProbe = firstRange.getRangeEnd(archiveLength) == 0L && archiveLength > 1
            firstRange.getRangeStart(archiveLength) == 0L && archiveLength > 0 && !sizeProbe
        } catch (invalid: IllegalArgumentException) {
            // A malformed range is answered 416 by Spring, so nothing is downloaded.
            false
        }
    }
}
