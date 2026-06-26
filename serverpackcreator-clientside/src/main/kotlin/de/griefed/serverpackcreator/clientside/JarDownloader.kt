/* Copyright (C) 2025 Griefed
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
 * Fetches a published mod-file to a local jar for scanning/booting. Implementations differ in *how*
 * they obtain the bytes: the API-driven [HttpJarDownloader] handles freely-distributable files, while
 * the boot-phase browser-downloader handles distribution-locked ones. A `null` return means the file
 * could not be obtained by this strategy (the caller may then fall back or defer).
 *
 * @author Griefed
 */
fun interface JarDownloader {
    /** Download [modFile] into [targetDirectory], returning the local jar or `null` on failure. */
    fun download(modFile: ModFile, targetDirectory: File): File?
}

/**
 * Route a file to the right download strategy: distribution-locked files (no `downloadUrl`) need the
 * [browserDownloader] (website flow), everything else uses the API-driven [httpDownloader].
 *
 * @author Griefed
 */
fun selectDownloader(modFile: ModFile, httpDownloader: JarDownloader, browserDownloader: JarDownloader): JarDownloader =
    if (modFile.locked) browserDownloader else httpDownloader

/**
 * [JarDownloader] for freely-distributable files: downloads straight from the platform-provided
 * `downloadUrl` via SPC's [WebUtilities]. Distribution-locked files (no `downloadUrl`) are not
 * handled here and yield `null` — they are the browser-downloader's job in the boot-phase.
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
