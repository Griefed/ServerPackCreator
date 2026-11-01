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
package de.griefed.serverpackcreator.api.versionmeta.fabric

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.create
import de.griefed.serverpackcreator.api.versionmeta.Meta
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import javax.xml.parsers.ParserConfigurationException

/**
 * Fabric meta containing information about available Quilt versions and installers.
 *
 * @author Griefed
 */
@Suppress("unused")
class FabricMeta(
    /** Fabric manifest file. */
    fabricManifest: File,
    /** Fabric-installer manifest file. */
    fabricInstallerManifest: File,
    /** Fabric Intermediary instance. */
    private val fabricIntermediaries: FabricIntermediaries,
    /** Object mapper for JSON parsing. */
    objectMapper: ObjectMapper,
    /** Commonly used utilities across ServerPackCreator. */
    private val utilities: Utilities,
    /** The cache-directory for all installers. */
    installerCacheDirectory: File
) : Meta {
    private val fabricLoader: FabricLoader = FabricLoader(fabricManifest, utilities)
    private val fabricLoaderDetails: FabricLoaderDetails = FabricLoaderDetails(objectMapper)
    private val fabricInstaller: FabricInstaller = FabricInstaller(fabricInstallerManifest, utilities)
    private val installerDirectory: File = File(installerCacheDirectory, "fabric")
    private val launchersDirectory: File = File(installerDirectory, "launchers")

    init {
        installerDirectory.create(createFileOrDir = true, asDirectory = true)
        launchersDirectory.create(createFileOrDir = true, asDirectory = true)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    /** Per-loader-version details, cached after first lookup so repeated queries cost nothing. */
    val loaderDetails = HashMap<String, FabricDetails>(100)

    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    override fun update() {
        fabricLoader.update()
        fabricInstaller.update()
    }

    override fun latestLoader() = fabricLoader.latestLoaderVersion()

    override fun releaseLoader() = fabricLoader.releaseLoaderVersion()

    override fun latestInstaller() = fabricInstaller.latestInstallerVersion()

    override fun releaseInstaller() = fabricInstaller.releaseInstallerVersion()

    override fun loaderVersions() = fabricLoader.loaders

    override fun installerVersions() = fabricInstaller.installers

    override fun latestInstallerUrl() = fabricInstaller.latestInstallerUrl()

    override fun releaseInstallerUrl() = fabricInstaller.releaseInstallerUrl()

    override fun installerFor(version: String) =
        if (isInstallerUrlAvailable(version)) {
            val destination = File(installerDirectory, "$version.jar")
            if (!destination.isFile) {
                val url = fabricInstaller.installerUrlMeta[version]!!
                val downloaded = utilities.webUtilities.downloadFile(destination, url)
                if (downloaded) {
                    Optional.of(destination)
                } else {
                    Optional.empty()
                }
            } else {
                Optional.of(destination)
            }
        } else {
            Optional.empty()
        }

    /**
     * Download the improved Fabric launcher and store it in the launcher-cache directory.
     * @author Griefed
     */
    fun launcherFor(minecraftVersion: String, fabricVersion: String): Optional<File> {
        val destination = File(launchersDirectory, "$minecraftVersion-$fabricVersion.jar")
        return if (!destination.isFile) {
            val url = fabricInstaller.improvedLauncherUrl(minecraftVersion, fabricVersion)
            val downloaded = utilities.webUtilities.downloadFile(destination, url)
            if (downloaded) {
                Optional.of(destination)
            } else {
                Optional.empty()
            }
        } else {
            Optional.of(destination)
        }
    }

    override fun isInstallerUrlAvailable(version: String) =
        Optional.ofNullable(fabricInstaller.installerUrlMeta[version]).isPresent

    override fun getInstallerUrl(version: String) =
        Optional.ofNullable(fabricInstaller.installerUrlMeta[version])

    override fun isVersionValid(version: String) = fabricLoader.loaders.contains(version)

    override fun isMinecraftSupported(minecraftVersion: String) =
        fabricIntermediaries.getIntermediary(minecraftVersion).isPresent

    /**
     * Get the [URL] to the Fabric launcher for the specified Minecraft and Fabric version.
     *
     * @author Griefed
     */
    fun improvedLauncherUrl(minecraftVersion: String, fabricVersion: String): URL =
        fabricInstaller.improvedLauncherUrl(minecraftVersion, fabricVersion)

    /**
     * Get details for a Fabric loader.
     *
     * @author Griefed
     */
    fun getLoaderDetails(
        /** Minecraft version. */
        minecraftVersion: String,
        /** Fabric version. */
        fabricVersion: String
    ): Optional<FabricDetails> {
        val key = "$minecraftVersion-$fabricVersion"
        return if (loaderDetails.containsKey(key)) {
            Optional.of(loaderDetails[key] as FabricDetails)
        } else if (fabricLoaderDetails.getDetails(minecraftVersion, fabricVersion).isPresent) {
            loaderDetails[key] = fabricLoaderDetails.getDetails(minecraftVersion, fabricVersion).get()
            Optional.of(loaderDetails[key] as FabricDetails)
        } else {
            Optional.empty()
        }
    }
}