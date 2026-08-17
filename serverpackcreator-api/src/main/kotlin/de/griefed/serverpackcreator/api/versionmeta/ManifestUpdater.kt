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
import de.griefed.serverpackcreator.api.utilities.common.create
import de.griefed.serverpackcreator.api.utilities.common.readText
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
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
 * @param utilities Commonly used utilities across ServerPackCreator.
 */
class ManifestUpdater(private val utilities: Utilities) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Check a given manifest for updates.
     *
     * If it does not exist, it is downloaded and stored.
     *
     * If it exists, it is compared to the online manifest. If the online version contains more
     * versions, the local manifest is replaced by the online one.
     *
     * @param manifestToCheck The manifest to check.
     * @param urlToManifest   The URL to the manifest.
     * @param manifestType    The type of the manifest, which decides how its versions are counted.
     * @author Griefed
     */
    fun checkManifest(
        manifestToCheck: File,
        urlToManifest: URL,
        manifestType: Type
    ) {
        if (manifestToCheck.isFile) {
            if (!utilities.webUtilities.isReachable(urlToManifest)) {
                log.warn(
                    "Can not connect to $urlToManifest to check for update(s) of $manifestToCheck."
                )
                return
            }
            try {
                manifestToCheck.inputStream().use { existing ->
                    utilities.webUtilities.openTimedStream(urlToManifest).use { newManifest ->
                        var countOldFile = 0
                        var countNewFile = 0
                        val oldContent: String = existing.readText()
                        val newContent: String = newManifest.readText()
                        when (manifestType) {
                            Type.MINECRAFT -> {
                                countOldFile = utilities.jsonUtilities.getJson(oldContent).get(VersionMetaConfig.TAG_VERSIONS).size()
                                countNewFile = utilities.jsonUtilities.getJson(newContent).get(VersionMetaConfig.TAG_VERSIONS).size()
                            }

                            Type.FORGE -> {
                                for (mcVer in utilities.jsonUtilities.getJson(oldContent)) {
                                    countOldFile += mcVer.size()
                                }
                                for (mcVer in utilities.jsonUtilities.getJson(newContent)) {
                                    countNewFile += mcVer.size()
                                }
                            }

                            Type.FABRIC_INTERMEDIARIES -> {
                                countOldFile = utilities.jsonUtilities.getJson(oldContent).size()
                                countNewFile = utilities.jsonUtilities.getJson(newContent).size()
                            }

                            Type.FABRIC, Type.FABRIC_INSTALLER, Type.QUILT, Type.QUILT_INSTALLER, Type.NEO_FORGE -> {
                                countOldFile = utilities.xmlUtilities.getXml(oldContent)
                                    .getElementsByTagName(VersionMetaConfig.TAG_VERSION).length
                                countNewFile = utilities.xmlUtilities.getXml(newContent)
                                    .getElementsByTagName(VersionMetaConfig.TAG_VERSION).length
                            }

                            Type.LEGACY_FABRIC -> if (manifestToCheck.name.endsWith(".json")) {
                                countOldFile = utilities.jsonUtilities.getJson(oldContent).size()
                                countNewFile = utilities.jsonUtilities.getJson(newContent).size()
                            } else {
                                val oldXML: Document = utilities.xmlUtilities.getXml(oldContent)
                                val newXML: Document = utilities.xmlUtilities.getXml(newContent)
                                countOldFile = oldXML.getElementsByTagName(VersionMetaConfig.TAG_VERSION).length
                                countNewFile = newXML.getElementsByTagName(VersionMetaConfig.TAG_VERSION).length
                                if (countOldFile == countNewFile) {
                                    if (oldXML.getElementsByTagName(VersionMetaConfig.TAG_VERSION).item(0).childNodes.item(0)
                                            .nodeValue != newXML.getElementsByTagName(VersionMetaConfig.TAG_VERSION).item(0).childNodes
                                            .item(0)
                                            .nodeValue
                                    ) {
                                        countNewFile += 1
                                    }
                                }
                            }

                            else -> throw InvalidTypeException(
                                "Manifest type must be either Type.MINECRAFT, Type.FORGE, Type.FABRIC or Type.FABRIC_INSTALLER. Specified: "
                                        + manifestType
                            )
                        }
                        log.debug("Nodes/Versions/Size in/of old $manifestToCheck: $countOldFile")
                        log.debug("Nodes/Versions/Size in/of new $manifestToCheck: $countNewFile")
                        if (countNewFile > countOldFile) {
                            log.info("Refreshing $manifestToCheck.")
                            updateManifest(manifestToCheck, newContent)
                        } else {
                            log.info("Manifest $manifestToCheck does not need to be refreshed.")
                        }
                    }
                }
            } catch (ex: SAXException) {
                JarUtilities.copyFileFromJar(
                    "de/griefed/resources/manifests/${manifestToCheck.name}",
                    manifestToCheck,
                    true,
                    ManifestUpdater::class.java
                )
                log.error(
                    "Unexpected end of file in XML-manifest. Restoring default "
                            + manifestToCheck.path
                )
            } catch (ex: ParserConfigurationException) {
                log.error("Couldn't refresh manifest $manifestToCheck", ex)
            } catch (ex: IOException) {
                log.error("Couldn't refresh manifest $manifestToCheck", ex)
            } catch (ex: InvalidTypeException) {
                log.error("Couldn't refresh manifest $manifestToCheck", ex)
            }
        } else {
            if (!utilities.webUtilities.isReachable(urlToManifest)) {
                log.error("CRITICAL! $manifestToCheck not present and $ urlToManifest unreachable. Exiting...")
                log.error(
                    "ServerPackCreator should have provided default manifests. Please report this on GitHub at https://github.com/Griefed/ServerPackCreator/issues/new?assignees=Griefed&labels=bug&template=bug-report.yml&title=%5BBug%5D%3A+"
                )
                log.error("Make sure you include this log when reporting an error! Please....")
            } else {
                updateManifest(manifestToCheck, urlToManifest)
            }
        }
    }

    /**
     * Ensures we always have the latest manifest for version validation available.
     *
     * @param manifestToRefresh The manifest file to update.
     * @param content           The content to write to the new manifest.
     * @author whitebear60
     * @author Griefed
     */
    @Throws(IOException::class)
    private fun updateManifest(
        manifestToRefresh: File,
        content: String
    ) {
        manifestToRefresh.create()
        manifestToRefresh.writeText(content)
    }

    /**
     * Ensures we always have the latest manifest for version validation available.
     *
     * @param manifestToRefresh The manifest file to update.
     * @param urlToManifest     The URL to the file which is to be downloaded.
     * @author whitebear60
     * @author Griefed
     */
    private fun updateManifest(
        manifestToRefresh: File,
        urlToManifest: URL
    ) {
        try {
            utilities.webUtilities.openTimedStream(urlToManifest).use {
                updateManifest(manifestToRefresh, it.readText())
            }
        } catch (ex: IOException) {
            log.error("An error occurred refreshing $manifestToRefresh.", ex)
        }
    }
}
