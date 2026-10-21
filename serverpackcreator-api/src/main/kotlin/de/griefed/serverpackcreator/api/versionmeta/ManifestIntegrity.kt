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
package de.griefed.serverpackcreator.api.versionmeta

import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Repairs a locally stored manifest that can no longer be parsed.
 *
 * ServerPackCreator seeds every manifest from the jar, but only ever when the file is **absent** — so a
 * manifest that exists and is unreadable is the one manifest nothing else in the process can replace, and a
 * truncated one ends every start inside its meta's `update` with no remedy short of deleting the file by
 * hand. This asks the cheap question — *does this file still parse?* — and answers a "no" with the copy the
 * jar carries.
 *
 * [utilities] — Commonly used utilities across ServerPackCreator.
 */
internal class ManifestIntegrity(private val utilities: Utilities) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Where the fallback manifests live inside the jar, and the directory [restoreIfUnreadable] restores from. */
    private val shippedManifests = "de/griefed/resources/manifests"

    /**
     * Repair [manifest] if it can no longer be parsed, reporting whether anything was done.
     *
     * A manifest the jar carries is overwritten with the shipped copy; one it does not — the per-version
     * `mcserver/<version>.json` files are the real case — is deleted, because an absent manifest is fetched
     * again on demand while an unreadable one fails for ever. The ETag sidecar goes with it either way: it
     * earns a `304`, which is a promise that the local copy is already the upstream one, and that promise is
     * false the moment the contents change. For the same reason the restored file's modification time is
     * cleared: it is the shipped snapshot, and the conditional request must ask for the whole current manifest
     * rather than be told that "now" is newer than anything upstream has.
     *
     * [manifest] — The manifest to check and, if it cannot be read, repair. Returns `true` if [manifest] was
     * restored or removed, `false` if it parses and was left alone.
     *
     * @author Griefed
     */
    fun restoreIfUnreadable(manifest: File): Boolean {
        if (isReadable(manifest)) {
            return false
        }
        val shipped = "$shippedManifests/${manifest.name}"
        if (javaClass.getResource("/$shipped") != null) {
            log.warn("Manifest $manifest can not be parsed. Restoring the copy shipped with ServerPackCreator.")
            JarUtilities.copyFileFromJar(shipped, manifest, true, javaClass)
        } else {
            log.warn("Manifest $manifest can not be parsed and is not shipped. Removing it so it can be fetched again.")
            manifest.delete()
        }
        File(manifest.parentFile, "${manifest.name}.etag").delete()
        // A restored manifest is the shipped snapshot, i.e. older than whatever the host had. The refresh asks
        // conditionally against this file's modification time, so leaving it at "now" would earn a 304 from the
        // upstreams that honour the header and strand the host on the snapshot. Zero is what HttpURLConnection
        // reads as "do not ask conditionally", so the next check fetches the whole current manifest.
        manifest.setLastModified(0)
        return true
    }

    /**
     * Whether [manifest] exists and parses as the format its extension declares.
     *
     * Anything thrown is an answer of "no", deliberately: the question is whether the metas will be able to
     * read this file, and they will not, whatever the parser called the problem.
     */
    private fun isReadable(manifest: File): Boolean {
        if (!manifest.isFile) {
            return false
        }
        return try {
            if (manifest.name.endsWith(".xml", ignoreCase = true)) {
                utilities.xmlUtilities.getXml(manifest)
            } else {
                utilities.jsonUtilities.getJson(manifest)
            }
            true
        } catch (ex: Exception) {
            log.debug("Parsing $manifest failed.", ex)
            false
        }
    }
}
