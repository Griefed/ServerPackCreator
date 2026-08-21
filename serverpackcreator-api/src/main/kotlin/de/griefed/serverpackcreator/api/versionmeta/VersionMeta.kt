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

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.fabric.FabricIntermediaries
import de.griefed.serverpackcreator.api.versionmeta.fabric.FabricMeta
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.legacyfabric.LegacyFabricMeta
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import de.griefed.serverpackcreator.api.versionmeta.neoforge.NeoForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.quilt.QuiltMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.cachedLoggerOf
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
    /** Upstream list of Minecraft versions LegacyFabric supports. Separate from the loader list below: LegacyFabric
     * publishes the two independently, and a version needs an entry in *both* to be usable. */
    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabricUrlGame: URL =
        URI(VersionMetaConfig.LEGACYFABRIC_GAME_MANIFEST).toURL()

    /** Upstream LegacyFabric loader versions. */
    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabricUrlLoader: URL =
        URI(VersionMetaConfig.LEGACYFABRIC_LOADER_MANIFEST).toURL()

    /** Upstream LegacyFabric *installer* versions — a different series from the loader versions, and not interchangeable. */
    @Suppress("MemberVisibilityCanBePrivate")
    val legacyfabricUrlManifest: URL =
        URI(VersionMetaConfig.LEGACYFABRIC_INSTALLER_MANIFEST).toURL()

    /** Mojang's version manifest: the authority for which Minecraft versions exist, their type, and where each
     * version's own JSON lives (which is what declares the required Java). */
    @Suppress("MemberVisibilityCanBePrivate")
    val minecraftUrlManifest: URL =
        URI(VersionMetaConfig.MINECRAFT_MANIFEST).toURL()

    /** Upstream Forge versions, keyed by Minecraft version. */
    @Suppress("MemberVisibilityCanBePrivate")
    val forgeUrlManifest: URL =
        URI(VersionMetaConfig.FORGE_MANIFEST).toURL()

    /** NeoForge's *legacy* maven metadata, covering its first releases under the `net/neoforged/forge` artifact —
     * Minecraft 1.20 and 1.20.1 only. Kept because those versions exist nowhere else. */
    @Suppress("MemberVisibilityCanBePrivate")
    val oldNeoForgeUrlManifest: URL =
        URI(VersionMetaConfig.NEOFORGE_OLD_MANIFEST).toURL()

    /** NeoForge's current maven metadata, covering everything after the 1.20.1 era. */
    @Suppress("MemberVisibilityCanBePrivate")
    val newNeoForgeUrlManifest: URL =
        URI(VersionMetaConfig.NEOFORGE_NEW_MANIFEST).toURL()

    /** Upstream Fabric loader versions. Minecraft-independent: one loader line serves every supported version. */
    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlManifest: URL =
        URI(VersionMetaConfig.FABRIC_LOADER_MANIFEST).toURL()

    /** Fabric's intermediary mappings, which is what actually says whether Fabric supports a given Minecraft
     * version — the loader list alone cannot answer that. */
    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlIntermediariesManifest: URL =
        URI(VersionMetaConfig.FABRIC_INTERMEDIARIES_MANIFEST).toURL()

    /** Upstream Fabric *installer* versions, a separate series from the loader versions. */
    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlInstallerManifest: URL =
        URI(VersionMetaConfig.FABRIC_INSTALLER_MANIFEST).toURL()

    /** Upstream Quilt loader versions. Minecraft-independent, like Fabric's. */
    @Suppress("MemberVisibilityCanBePrivate")
    val quiltUrlManifest: URL =
        URI(VersionMetaConfig.QUILT_LOADER_MANIFEST).toURL()

    /** Upstream Quilt *installer* versions. The installer needs Java 17+ even when the server it installs runs on 8. */
    @Suppress("MemberVisibilityCanBePrivate")
    val quiltUrlInstallerManifest: URL =
        URI(VersionMetaConfig.QUILT_INSTALLER_MANIFEST).toURL()

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

    /**
     * Fabric's intermediary mappings. Consulted to answer whether Fabric (or Quilt, which runs Fabric mods)
     * supports a given Minecraft version — the loader versions alone cannot, since one loader line serves them all.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val fabricIntermediaries: FabricIntermediaries

    /** Keeps the locally stored manifests up to date against their upstream sources. */
    private val manifestUpdater = ManifestUpdater(utilities)

    /**
     * Where the startup manifest refresh runs. `Dispatchers.IO` owns no thread of its own, and the job below
     * is the only work ever submitted, so there is nothing to cancel and nothing to leak — deliberately not
     * `GlobalScope`, which this project has removed everywhere else.
     */
    private val refreshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * The startup manifest refresh, running in the background.
     *
     * **This is a change of contract, and the reason [awaitManifestRefresh] exists.** Construction used to
     * block until all twelve manifests had been checked, so the metas held refreshed data the moment the
     * constructor returned. They now hold the jar-seeded data immediately and the refreshed data shortly
     * after — which takes ~392 ms off a GUI launch, and far more off an offline one. A caller that genuinely
     * needs upstream-fresh data must say so by awaiting.
     */
    private val manifestRefresh: Job

    /**
     * Checks every manifest against its upstream, then re-parses whatever changed.
     *
     * The re-parse is the second half and cannot be skipped: the metas are built from the manifest *files*,
     * so a refreshed file is invisible until the meta reads it again. This is the same pair of steps
     * [update] performs, and concurrent re-parse while another thread reads a meta is not new here — the web
     * backend's `VersionRefreshSchedule` has always called [update] on a cron while requests read the metas.
     */
    private fun refreshManifests() {
        try {
            checkManifests()
            minecraft.update()
            fabricIntermediaries.update()
            fabric.update()
            legacyFabric.update()
            forge.update()
            neoForge.update()
            quilt.update()
            log.info("Manifests refreshed.")
        } catch (ex: Exception) {
            // Never fatal: the seeded manifests are perfectly usable, and this runs after startup where
            // throwing would take down whatever thread the dispatcher happened to use.
            log.warn("Could not refresh the version manifests; continuing with the manifests on disk.", ex)
        }
    }

    /**
     * Waits up to [timeoutMillis] for the background manifest refresh to finish, returning whether it did.
     *
     * For callers that must not show stale versions — the GUI's version dropdowns are built once and never
     * repopulated, so a freshly released Minecraft version would otherwise be missing until the next launch.
     * Cheap in practice: by the time a user can click anything the refresh has long finished, and this
     * returns immediately. Bounded so an unreachable host delays a dropdown instead of hanging the UI.
     */
    fun awaitManifestRefresh(timeoutMillis: Long = 10_000): Boolean = runBlocking {
        withTimeoutOrNull(timeoutMillis) { manifestRefresh.join() } != null
    }

    init {
        // Deliberately NOT checkManifests() -- see `manifestRefresh` below. Every manifest is already on
        // disk, seeded from the jar by `ApiWrapper.setup()`, so the metas below have working data without a
        // single request. Refreshing here cost ~392 ms of blocking startup behind the splash screen, and
        // rather more when offline, for data that is superseded seconds later anyway.
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
    
        // Kicked off last, so everything it re-parses already exists.
        manifestRefresh = refreshScope.launch { refreshManifests() }
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
     * Check a given manifest for updates, delegating to [ManifestUpdater] which owns the refresh.
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
    ) = manifestUpdater.checkManifest(manifestToCheck, urlToManifest, manifestType)

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