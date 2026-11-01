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

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.readText
import de.griefed.serverpackcreator.api.utilities.common.writeTextAtomically
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.ParserConfigurationException

/**
 * Keeps the locally stored version-manifests up to date against their upstream sources.
 *
 * Extracted from [VersionMeta], which remains the facade its collaborators use. The extraction exists
 * so this can be exercised against a local HTTP server: [VersionMeta] resolves its twelve URLs from
 * `VersionMetaConfig` constants and does the whole refresh inside its constructor, so nothing about
 * *how many requests a check costs* was reachable from a test while the logic lived there.
 *
 * [utilities] — Commonly used utilities across ServerPackCreator.
 */
internal class ManifestUpdater(private val utilities: Utilities) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private companion object {
        /** What [versionsIn] could not answer, because the content it was handed does not parse. */
        const val UNREADABLE = -1
    }

    /**
     * Check a given manifest for updates.
     *
     * If it does not exist, it is downloaded and stored.
     *
     * If it exists, it is compared to the online manifest. If the online version contains more
     * versions, the local manifest is replaced by the online one.
     *
     * @author Griefed
     */
    fun checkManifest(
        manifestToCheck: File,
        /** The URL to the manifest. */
        urlToManifest: URL,
        /** The type of the manifest, which decides how its versions are counted. */
        manifestType: Type
    ) {
        if (manifestToCheck.isFile) {
            // Reaching the host and reading its answer are reported differently: being offline is
            // ordinary and stays a WARN (as it did when a reachability pre-check produced it), while a
            // manifest that arrives and cannot be understood is a real defect and keeps its ERROR.
            // Twelve of these run per startup, so mixing the two would bury every genuine failure
            // under a dozen stack traces every time a user launches without a network.
            val connection: HttpURLConnection = try {
                val opened = utilities.webUtilities.openTimedConnection(urlToManifest) as HttpURLConnection
                // Ask to be told only about changes. The upstreams answer 304 with no body at all, so
                // an unchanged manifest -- the common case -- costs headers instead of its full size
                // plus two parses. A host that ignores the header answers 200 and everything below
                // runs exactly as it did before, which is why this needs no per-host special-casing.
                opened.ifModifiedSince = manifestToCheck.lastModified()
                // And the ETag, for hosts that ignore the timestamp. files.minecraftforge.net is the one
                // that matters: it ignores If-Modified-Since entirely but honours If-None-Match against
                // its weak nginx ETag, and it is the largest manifest otherwise transferred in full on
                // every startup. Both headers are sent; a host honouring either answers 304.
                rememberedEtag(manifestToCheck)?.let { opened.setRequestProperty("If-None-Match", it) }
                if (opened.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    log.info("Manifest $manifestToCheck is unchanged upstream.")
                    opened.disconnect()
                    return
                }
                opened
            } catch (ex: IOException) {
                log.warn("Can not connect to $urlToManifest to check for update(s) of $manifestToCheck.")
                log.debug("Connection to $urlToManifest failed.", ex)
                return
            }
            try {
                manifestToCheck.inputStream().use { existing ->
                    connection.inputStream.use { newManifest ->
                        val oldContent: String = existing.readText()
                        val newContent: String = newManifest.readText()
                        // The new copy is counted first and without a net: an answer that cannot be parsed is
                        // never adopted, however broken the local one is, or a truncated response would become
                        // the same permanent failure it is being adopted to repair.
                        val countNewFile = versionsIn(newContent, manifestType, manifestToCheck)
                        val countOldFile = try {
                            versionsIn(oldContent, manifestType, manifestToCheck)
                        } catch (ex: Exception) {
                            // The comparison parses the local copy too, so an unreadable manifest throws here
                            // and would discard the working download already in hand -- leaving the one
                            // manifest a host cannot read as the one it can never replace.
                            log.warn("Manifest $manifestToCheck can not be parsed. Adopting the upstream copy.")
                            log.debug("Parsing the local $manifestToCheck failed.", ex)
                            UNREADABLE
                        }
                        log.debug("Nodes/Versions/Size in/of old $manifestToCheck: $countOldFile")
                        log.debug("Nodes/Versions/Size in/of new $manifestToCheck: $countNewFile")
                        val refresh = countOldFile == UNREADABLE || countNewFile > countOldFile ||
                                (countNewFile == countOldFile &&
                                        legacyFabricHeadChanged(oldContent, newContent, manifestType, manifestToCheck))
                        if (refresh) {
                            log.info("Refreshing $manifestToCheck.")
                            updateManifest(manifestToCheck, newContent)
                            // Only now, and only paired with what was written: an ETag recorded for a
                            // manifest we declined to adopt would earn a 304 for content we do not hold.
                            rememberEtag(manifestToCheck, connection.getHeaderField("ETag"))
                        } else {
                            log.info("Manifest $manifestToCheck does not need to be refreshed.")
                        }
                    }
                }
            } catch (ex: SAXException) {
                // Only the upstream answer can reach this now -- an unreadable local copy is caught where it is
                // counted, above -- so the manifest on disk is the working one and must be left alone.
                log.error("Couldn't refresh manifest $manifestToCheck", ex)
            } catch (ex: ParserConfigurationException) {
                log.error("Couldn't refresh manifest $manifestToCheck", ex)
            } catch (ex: IOException) {
                log.error("Couldn't refresh manifest $manifestToCheck", ex)
            } catch (ex: InvalidTypeException) {
                log.error("Couldn't refresh manifest $manifestToCheck", ex)
            }
        } else if (!updateManifest(manifestToCheck, urlToManifest)) {
            // Attempt the download and report the failure, rather than probing reachability first and
            // then downloading: the probe cost a second full request for information the download
            // already produces, and this way the log names the actual failure instead of "unreachable".
            log.error("CRITICAL! $manifestToCheck not present and $urlToManifest could not be downloaded. Exiting...")
            log.error(
                "ServerPackCreator should have provided default manifests. Please report this on GitHub at https://github.com/Griefed/ServerPackCreator/issues/new?assignees=Griefed&labels=bug&template=bug-report.yml&title=%5BBug%5D%3A+"
            )
            log.error("Make sure you include this log when reporting an error! Please....")
        }
    }

    /**
     * How many versions [content] declares, counted the way [manifestType] stores them.
     *
     * This is the comparison the refresh is decided on — a manifest is adopted when the upstream copy carries
     * *more* versions than the local one — so each type is counted in the shape it is published in: Minecraft
     * nests its versions under one tag, Forge keys lists by Minecraft version, the loader metadata is maven
     * metadata, and LegacyFabric publishes both shapes under one type.
     *
     * [content] — The manifest to count, as it was read. [manifestType] — The type deciding how versions are
     * counted. [manifest] — The file the content belongs to — LegacyFabric's two shapes are told apart by name.
     * Returns the number of versions declared.
     *
     * @throws IOException                  if JSON content can not be parsed.
     * @throws SAXException                 if XML content can not be parsed.
     * @throws InvalidTypeException         if the type does not describe a countable manifest.
     * @author Griefed
     */
    @Throws(IOException::class, SAXException::class, InvalidTypeException::class)
    private fun versionsIn(content: String, manifestType: Type, manifest: File): Int = when (manifestType) {
        Type.MINECRAFT ->
            utilities.jsonUtilities.getJson(content).get(VersionMetaConfig.TAG_VERSIONS).size()

        Type.FORGE ->
            utilities.jsonUtilities.getJson(content).sumOf { mcVer -> mcVer.size() }

        Type.FABRIC_INTERMEDIARIES ->
            utilities.jsonUtilities.getJson(content).size()

        Type.FABRIC, Type.FABRIC_INSTALLER, Type.QUILT, Type.QUILT_INSTALLER, Type.NEO_FORGE ->
            utilities.xmlUtilities.getXml(content).getElementsByTagName(VersionMetaConfig.TAG_VERSION).length

        Type.LEGACY_FABRIC -> if (manifest.name.endsWith(".json")) {
            utilities.jsonUtilities.getJson(content).size()
        } else {
            utilities.xmlUtilities.getXml(content).getElementsByTagName(VersionMetaConfig.TAG_VERSION).length
        }

        else -> throw InvalidTypeException(
            "Manifest type must be either Type.MINECRAFT, Type.FORGE, Type.FABRIC or Type.FABRIC_INSTALLER. Specified: "
                    + manifestType
        )
    }

    /**
     * Whether LegacyFabric's XML metadata declares a different newest version while declaring the same number
     * of them, which is the one way it can change without the count moving.
     *
     * LegacyFabric prunes as it publishes, so a release and a removal in the same window leave the count
     * identical and the head different — and the count alone would then decline a genuine update for ever.
     *
     * Returns `true` if the newest declared version differs between the two.
     *
     * @author Griefed
     */
    @Throws(SAXException::class)
    private fun legacyFabricHeadChanged(
        /** The manifest currently on disk. */
        oldContent: String,
        /** The manifest the upstream answered with. */
        newContent: String,
        /** The type of the manifest; only LegacyFabric is pruned this way. */
        manifestType: Type,
        /** The file the content belongs to — only LegacyFabric's XML half is affected. */
        manifest: File
    ): Boolean {
        if (manifestType != Type.LEGACY_FABRIC || manifest.name.endsWith(".json")) {
            return false
        }
        val newestIn = { content: String ->
            utilities.xmlUtilities.getXml(content)
                .getElementsByTagName(VersionMetaConfig.TAG_VERSION).item(0).childNodes.item(0).nodeValue
        }
        return newestIn(oldContent) != newestIn(newContent)
    }

    /**
     * The sidecar recording the ETag of [manifest], beside the manifest itself.
     *
     * A file rather than a property because it must travel with the manifest: the manifests live in their
     * own directory that survives a wiped home, and a property store would drift from them.
     */
    private fun etagSidecar(manifest: File) = File(manifest.parentFile, "${manifest.name}.etag")

    /**
     * The ETag last recorded for [manifest], or `null` if there is none **or it no longer describes what is
     * on disk**.
     *
     * The pairing is verified rather than trusted, and that is the whole safety of this feature. An ETag
     * describes one exact body; if the manifest has since been replaced by something else — `ApiWrapper`
     * re-seeding it from the jar is the real case, a restore or a hand-edit are others — then offering the
     * old ETag would earn a `304` for content we do not hold and suppress a genuine update *permanently*.
     * The recorded byte length is what detects that, so a mismatch discards the ETag rather than risking it.
     */
    private fun rememberedEtag(manifest: File): String? {
        val sidecar = etagSidecar(manifest)
        if (!sidecar.isFile) {
            return null
        }
        return try {
            val lines = sidecar.readLines()
            val etag = lines.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
            val describedLength = lines.getOrNull(1)?.trim()?.toLongOrNull() ?: return null
            if (describedLength == manifest.length()) {
                etag
            } else {
                log.debug("Discarding the ETag for $manifest: it describes $describedLength bytes, not ${manifest.length()}.")
                null
            }
        } catch (ex: IOException) {
            log.debug("Could not read $sidecar; proceeding without an ETag.", ex)
            null
        }
    }

    /**
     * Records [etag] as describing the current contents of [manifest], together with the byte length that
     * lets [rememberedEtag] tell later whether the pairing still holds. A response without an `ETag` header
     * clears any previous record, so nothing stale survives.
     */
    private fun rememberEtag(manifest: File, etag: String?) {
        val sidecar = etagSidecar(manifest)
        try {
            if (etag.isNullOrBlank()) {
                sidecar.delete()
            } else {
                sidecar.writeText(etag + "\n" + manifest.length())
            }
        } catch (ex: IOException) {
            // A missing sidecar only costs one full download next time, so this must never fail a refresh.
            log.debug("Could not record the ETag for $manifest.", ex)
        }
    }

    /**
     * Ensures we always have the latest manifest for version validation available.
     *
     * @author whitebear60
     * @author Griefed
     */
    @Throws(IOException::class)
    private fun updateManifest(
        /** The manifest file to update. */
        manifestToRefresh: File,
        /** The content to write to the new manifest. */
        content: String
    ) = manifestToRefresh.writeTextAtomically(content)

    /**
     * Ensures we always have the latest manifest for version validation available.
     *
     * Returns `true` if the manifest was downloaded and written, `false` if it could not be fetched — which is
     * what lets the caller report an absent manifest without a separate reachability probe.
     *
     * @author whitebear60
     * @author Griefed
     */
    private fun updateManifest(
        /** The manifest file to update. */
        manifestToRefresh: File,
        /** The URL to the file which is to be downloaded. */
        urlToManifest: URL
    ): Boolean {
        return try {
            utilities.webUtilities.openTimedStream(urlToManifest).use {
                updateManifest(manifestToRefresh, it.readText())
            }
            true
        } catch (ex: IOException) {
            log.error("An error occurred refreshing $manifestToRefresh.", ex)
            false
        }
    }
}
