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
 * @param fabricManifest          Fabric manifest file.
 * @param fabricInstallerManifest Fabric-installer manifest file.
 * @param fabricIntermediaries    Fabric Intermediary instance.
 * @param objectMapper            Object mapper for JSON parsing.
 * @param utilities               Commonly used utilities across ServerPackCreator.
 * @param installerCacheDirectory The cache-directory for all installers.
 *
 * @author Griefed
 */
@Suppress("unused")
class FabricMeta(
    fabricManifest: File,
    fabricInstallerManifest: File,
    private val fabricIntermediaries: FabricIntermediaries,
    objectMapper: ObjectMapper,
    private val utilities: Utilities,
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
    val loaderDetails = HashMap<String, FabricDetails>(100)

    /**
     * @author Griefed
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    override fun update() {
        fabricLoader.update()
        fabricInstaller.update()
    }

    /**
     * @author Griefed
     */
    override fun latestLoader() = fabricLoader.latestLoaderVersion()

    /**
     * @author Griefed
     */
    override fun releaseLoader() = fabricLoader.releaseLoaderVersion()

    /**
     * @author Griefed
     */
    override fun latestInstaller() = fabricInstaller.latestInstallerVersion()

    /**
     * @author Griefed
     */
    override fun releaseInstaller() = fabricInstaller.releaseInstallerVersion()

    /**
     * @author Griefed
     */
    override fun loaderVersions() = fabricLoader.loaders

    /**
     * @author Griefed
     */
    override fun installerVersions() = fabricInstaller.installers

    /**
     * @author Griefed
     */
    override fun latestInstallerUrl() = fabricInstaller.latestInstallerUrl()

    /**
     * @author Griefed
     */
    override fun releaseInstallerUrl() = fabricInstaller.releaseInstallerUrl()

    /**
     * @author Griefed
     */
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

    /**
     * @author Griefed
     */
    override fun isInstallerUrlAvailable(version: String) =
        Optional.ofNullable(fabricInstaller.installerUrlMeta[version]).isPresent

    /**
     * @author Griefed
     */
    override fun getInstallerUrl(version: String) =
        Optional.ofNullable(fabricInstaller.installerUrlMeta[version])

    /**
     * @author Griefed
     */
    override fun isVersionValid(version: String) = fabricLoader.loaders.contains(version)

    /**
     * @author Griefed
     */
    override fun isMinecraftSupported(minecraftVersion: String) =
        fabricIntermediaries.getIntermediary(minecraftVersion).isPresent

    /**
     * Get the [URL] to the Fabric launcher for the specified Minecraft and Fabric version.
     *
     * @param minecraftVersion Minecraft version.
     * @param fabricVersion    Fabric version.
     * @return URL to the Fabric launcher for the specified Minecraft and Fabric version.
     * @author Griefed
     */
    fun improvedLauncherUrl(minecraftVersion: String, fabricVersion: String): URL =
        fabricInstaller.improvedLauncherUrl(minecraftVersion, fabricVersion)

    /**
     * Get details for a Fabric loader.
     *
     * @param minecraftVersion Minecraft version.
     * @param fabricVersion    Fabric version.
     * @return Details of a Fabric loader for the given Minecraft and Fabric version, wrapped in an
     * [Optional].
     * @author Griefed
     */
    fun getLoaderDetails(
        minecraftVersion: String,
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