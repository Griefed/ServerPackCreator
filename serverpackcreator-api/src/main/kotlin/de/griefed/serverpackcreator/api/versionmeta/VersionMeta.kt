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
package de.griefed.serverpackcreator.api.versionmeta

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.create
import de.griefed.serverpackcreator.api.utilities.common.readText
import de.griefed.serverpackcreator.api.versionmeta.fabric.FabricIntermediaries
import de.griefed.serverpackcreator.api.versionmeta.fabric.FabricMeta
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.legacyfabric.LegacyFabricMeta
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import de.griefed.serverpackcreator.api.versionmeta.neoforge.NeoForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.quilt.QuiltMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import javax.xml.parsers.ParserConfigurationException

/**
 * VersionMeta containing available versions and important details for Minecraft, Fabric and Forge.
 *
 * @param minecraftManifest             Minecraft manifest file.
 * @param forgeManifest                 Forge manifest file.
 * @param fabricManifest                Fabric manifest file.
 * @param fabricIntermediariesManifest  Fabric Intermediary manifest-file.
 * @param fabricInstallerManifest       Fabric-installer manifest file.
 * @param quiltManifest                 Quilt manifest file.
 * @param quiltInstallerManifest        Quilt-installer manifest file.
 * @param objectMapper                  Object mapper-instance for JSON parsing.
 * @param legacyFabricGameManifest      Fabric Legacy Game manifest file.
 * @param legacyFabricLoaderManifest    Fabric Legacy Loader manifest file.
 * @param legacyFabricInstallerManifest Fabric Legacy Installer manifest file.
 * @param utilities                     Commonly used utilities across ServerPackCreator.
 * @param apiProperties                 ServerPackCreator settings.
 * @throws ParserConfigurationException indicates a serious configuration error.
 * @throws IOException                  if any IO errors occur.
 * @throws SAXException                 if any parse errors occur.
 *
 * @author Griefed
 */
class VersionMeta(
    private val minecraftManifest: File,
    private val forgeManifest: File,
    private val oldNeoForgeManifest: File,
    private val newNeoForgeManifest: File,
    private val fabricManifest: File,
    private val fabricInstallerManifest: File,
    private val fabricIntermediariesManifest: File,
    private val quiltManifest: File,
    private val quiltInstallerManifest: File,
    private val legacyFabricGameManifest: File,
    private val legacyFabricLoaderManifest: File,
    private val legacyFabricInstallerManifest: File,
    objectMapper: ObjectMapper,
    private val utilities: Utilities,
    apiProperties: ApiProperties
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val legacyFabricUrlBase = "https://meta.legacyfabric.net"

    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabricUrlGame: URL =
        URI("$legacyFabricUrlBase/v2/versions/game").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabricUrlLoader: URL =
        URI("$legacyFabricUrlBase/v2/versions/loader").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val legacyfabricUrlManifest: URL =
        URI("https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/maven-metadata.xml").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val minecraftUrlManifest: URL =
        URI("https://launchermeta.mojang.com/mc/game/version_manifest.json").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val forgeUrlManifest: URL =
        URI("https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val oldNeoForgeUrlManifest: URL =
        URI("https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val newNeoForgeUrlManifest: URL =
        URI("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlManifest: URL =
        URI("https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlIntermediariesManifest: URL =
        URI("https://meta.fabricmc.net/v2/versions/intermediary").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlInstallerManifest: URL =
        URI("https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val quiltUrlManifest: URL =
        URI("https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml").toURL() // TODO Move URL to property

    @Suppress("MemberVisibilityCanBePrivate")
    val quiltUrlInstallerManifest: URL =
        URI("https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/maven-metadata.xml").toURL() // TODO Move URL to property

    /**
     * The MinecraftMeta instance for working with Minecraft versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val minecraft: MinecraftMeta

    /**
     * The QuiltMeta-instance for working with Fabric versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val fabric: FabricMeta

    /**
     * The ForgeMeta-instance for working with Forge versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val forge: ForgeMeta

    /**
     * The NeoForgeMeta-instance for working with NeoForge versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val neoForge: NeoForgeMeta

    /**
     * The QuiltMeta-instance for working with Quilt versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val quilt: QuiltMeta

    /**
     * The LegacyFabric-instance for working with Legacy Fabric versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabric: LegacyFabricMeta

    @Suppress("MemberVisibilityCanBePrivate")
    val fabricIntermediaries: FabricIntermediaries

    init {
        checkManifests()
        forge = ForgeMeta(
            forgeManifest,
            utilities,
            apiProperties.installerCacheDirectory
        )
        neoForge = NeoForgeMeta(
            oldNeoForgeManifest,
            newNeoForgeManifest,
            utilities,
            apiProperties.installerCacheDirectory
        )
        minecraft = MinecraftMeta(
            minecraftManifest,
            forge,
            utilities,
            apiProperties
        )
        fabricIntermediaries = FabricIntermediaries(
            fabricIntermediariesManifest,
            objectMapper
        )
        legacyFabric = LegacyFabricMeta(
            legacyFabricGameManifest,
            legacyFabricLoaderManifest,
            legacyFabricInstallerManifest,
            utilities,
            apiProperties.installerCacheDirectory
        )
        fabric = FabricMeta(
            fabricManifest,
            fabricInstallerManifest,
            fabricIntermediaries,
            objectMapper,
            utilities,
            apiProperties.installerCacheDirectory
        )
        forge.initialize(minecraft)
        neoForge.initialize(minecraft)
        quilt = QuiltMeta(
            quiltManifest,
            quiltInstallerManifest,
            fabricIntermediaries,
            utilities,
            apiProperties.installerCacheDirectory
        )
        minecraft.update()
        fabricIntermediaries.update()
        fabric.update()
        legacyFabric.update()
        forge.update()
        neoForge.update()
        quilt.update()
    }

    /**
     * Check all our manifests, those being Minecraft, Forge, Fabric and Fabric Installer, for whether
     * updated manifests are available, by comparing their locally stored ones against freshly
     * downloaded ones. If a manifest does not exist yet, it is downloaded to the specified file with
     * which this instance of the version meta was created.
     *
     * @author Griefed
     */
    private fun checkManifests() {
        runBlocking(Dispatchers.IO) {
            launch {
                checkManifest(minecraftManifest, minecraftUrlManifest, Type.MINECRAFT)
            }
            launch {
                checkManifest(forgeManifest, forgeUrlManifest, Type.FORGE)
            }
            launch {
                checkManifest(oldNeoForgeManifest, oldNeoForgeUrlManifest, Type.NEO_FORGE)
            }
            launch {
                checkManifest(newNeoForgeManifest, newNeoForgeUrlManifest, Type.NEO_FORGE)
            }
            launch {
                checkManifest(fabricIntermediariesManifest, fabricUrlIntermediariesManifest, Type.FABRIC_INTERMEDIARIES)
            }
            launch {
                checkManifest(legacyFabricGameManifest, legacyFabricUrlGame, Type.LEGACY_FABRIC)
            }
            launch {
                checkManifest(legacyFabricLoaderManifest, legacyFabricUrlLoader, Type.LEGACY_FABRIC)
            }
            launch {
                checkManifest(legacyFabricInstallerManifest, legacyfabricUrlManifest, Type.LEGACY_FABRIC)
            }
            launch {
                checkManifest(fabricManifest, fabricUrlManifest, Type.FABRIC)
            }
            launch {
                checkManifest(fabricInstallerManifest, fabricUrlInstallerManifest, Type.FABRIC_INSTALLER)
            }
            launch {
                checkManifest(quiltManifest, quiltUrlManifest, Type.QUILT)
            }
            launch {
                checkManifest(quiltInstallerManifest, quiltUrlInstallerManifest, Type.QUILT_INSTALLER)
            }
        }
    }

    /**
     * Check a given manifest for updates.
     *
     * If it does not exist, it is downloaded and stored.
     *
     *
     * If it exists, it is compared to the online manifest.
     *
     * If the online version contains more versions, the local manifests are replaced by the online ones.
     *
     * @param manifestToCheck The manifest to check.
     * @param urlToManifest   The URL to the manifest.
     * @param manifestType    The type of the manifest, either [Type.MINECRAFT], [Type.FORGE], [Type.FABRIC] or [Type.FABRIC_INSTALLER].
     * @author Griefed
     */
    private fun checkManifest(
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
                    urlToManifest.openStream().use { newManifest ->
                        var countOldFile = 0
                        var countNewFile = 0
                        val oldContent: String = existing.readText()
                        val newContent: String = newManifest.readText()
                        when (manifestType) {
                            Type.MINECRAFT -> {
                                countOldFile = utilities.jsonUtilities.getJson(oldContent).get("versions").size() // TODO Move tagName to property
                                countNewFile = utilities.jsonUtilities.getJson(newContent).get("versions").size() // TODO Move tagName to property
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
                                    .getElementsByTagName("version").length // TODO Move tagName to property
                                countNewFile = utilities.xmlUtilities.getXml(newContent)
                                    .getElementsByTagName("version").length // TODO Move tagName to property
                            }

                            Type.LEGACY_FABRIC -> if (manifestToCheck.name.endsWith(".json")) {
                                countOldFile = utilities.jsonUtilities.getJson(oldContent).size()
                                countNewFile = utilities.jsonUtilities.getJson(newContent).size()
                            } else {
                                val oldXML: Document = utilities.xmlUtilities.getXml(oldContent)
                                val newXML: Document = utilities.xmlUtilities.getXml(newContent)
                                countOldFile = oldXML.getElementsByTagName("version").length // TODO Move tagName to property
                                countNewFile = newXML.getElementsByTagName("version").length // TODO Move tagName to property
                                if (countOldFile == countNewFile) {
                                    if (oldXML.getElementsByTagName("version").item(0).childNodes.item(0) // TODO Move tagName to property
                                            .nodeValue != newXML.getElementsByTagName("version").item(0).childNodes // TODO Move tagName to property
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
                    VersionMeta::class.java
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
            urlToManifest.openStream().use {
                updateManifest(manifestToRefresh, it.readText())
            }
        } catch (ex: IOException) {
            log.error("An error occurred refreshing $manifestToRefresh.", ex)
        }
    }

    /**
     * Update the Minecraft, Forge and Fabric metas. Usually called when the manifest files have been
     * refreshed.
     *
     * @return The instance of this version meta, updated.
     * @throws ParserConfigurationException indicates a serious configuration error.
     * @throws IOException                  if any IO errors occur.
     * @throws SAXException                 if any parse errors occur.
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun update(): VersionMeta {
        checkManifests()
        minecraft.update()
        fabricIntermediaries.update()
        fabric.update()
        legacyFabric.update()
        forge.update()
        neoForge.update()
        quilt.update()
        return this
    }
}