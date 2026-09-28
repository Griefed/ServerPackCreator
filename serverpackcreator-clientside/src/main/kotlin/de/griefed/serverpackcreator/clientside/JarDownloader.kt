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
package de.griefed.serverpackcreator.clientside

import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import java.io.File
import java.net.URI

/**
 * Fetches a published mod-file to a local jar for scanning/booting. There is one implementation,
 * [HttpJarDownloader], which downloads from the URL the platform published. A `null` return means the
 * file could not be obtained — for a distribution-locked file that is permanent, and the caller reports
 * it rather than falling back.
 *
 * @author Griefed
 */
fun interface JarDownloader {
    /** Download [modFile] into [targetDirectory], returning the local jar or `null` on failure. */
    fun download(modFile: ModFile, targetDirectory: File): File?
}

/**
 * [JarDownloader] for published files: downloads straight from the platform-provided `downloadUrl` via
 * SPC's [WebUtilities]. A distribution-locked file has no `downloadUrl` and yields `null`; that is the
 * end of the line, since the author opted out of third-party distribution and nothing here works around
 * it. Such a project is verified from Modrinth instead, where files carry a URL.
 *
 * @param webUtilities SPC's download helper (from [de.griefed.serverpackcreator.api.ApiWrapper]).
 * @author Griefed
 */
class HttpJarDownloader(private val webUtilities: WebUtilities) : JarDownloader {
    override fun download(modFile: ModFile, targetDirectory: File): File? {
        val url = modFile.downloadUrl ?: return null
        targetDirectory.mkdirs()
        val destination = File(targetDirectory, modFile.fileName)
        val success = webUtilities.downloadFile(destination, URI.create(url).toURL())
        return if (success) destination else null
    }
}
