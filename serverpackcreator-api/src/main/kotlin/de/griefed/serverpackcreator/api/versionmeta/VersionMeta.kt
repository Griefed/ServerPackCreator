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
 * @throws ParserConfigurationException indicates a serious configuration error.
 * @throws IOException                  if any IO errors occur.
 * @throws SAXException                 if any parse errors occur.
 *
 * @author Griefed
 */
class VersionMeta(
    /** Minecraft manifest file. */
    private val minecraftManifest: File,
    /** Forge manifest file. */
    private val forgeManifest: File,
    private val oldNeoForgeManifest: File,
    private val newNeoForgeManifest: File,
    /** Fabric manifest file. */
    private val fabricManifest: File,
    /** Fabric-installer manifest file. */
    private val fabricInstallerManifest: File,
    /** Fabric Intermediary manifest-file. */
    private val fabricIntermediariesManifest: File,
    /** Quilt manifest file. */
    private val quiltManifest: File,
    /** Quilt-installer manifest file. */
    private val quiltInstallerManifest: File,
    /** Fabric Legacy Game manifest file. */
    private val legacyFabricGameManifest: File,
    /** Fabric Legacy Loader manifest file. */
    private val legacyFabricLoaderManifest: File,
    /** Fabric Legacy Installer manifest file. */
    private val legacyFabricInstallerManifest: File,
    /** Object mapper-instance for JSON parsing. */
    objectMapper: ObjectMapper,
    /** Commonly used utilities across ServerPackCreator. */
    private val utilities: Utilities,
    /** ServerPackCreator settings. */
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

    /** Repairs a manifest that can no longer be parsed, so one bad write cannot end every start. */
    private val manifestIntegrity = ManifestIntegrity(utilities)

    /**
     * Where the startup manifest refresh runs. `Dispatchers.IO` owns no thread of its own, and the job below
     * is the only work ever submitted, so there is nothing to cancel and nothing to leak — deliberately not
     * `GlobalScope`, which this project has removed everywhere else.
     */
    private val refreshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * The startup manifest refresh, running in the background.
     *
     * **Construction does not wait for it, which is why [awaitManifestRefresh] exists.** The metas hold the
     * jar-seeded data the moment the constructor returns and the refreshed data shortly after; blocking on
     * all twelve manifest checks instead would add ~392 ms to a GUI launch, and far more offline. A caller
     * that genuinely
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
     *
     * **`@Synchronized` on the same monitor as [update], and that is the point.** This is the path the
     * background coroutine takes, and it calls each meta's `update()` *directly* rather than going through
     * [update] — so locking [update] alone left this one unguarded, which is the whole case the lock exists
     * for. Both are instance methods, so both take `this`.
     */
    @Synchronized
    private fun refreshManifests() {
        try {
            checkManifests()
            updateMetas()
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

    /**
     * Re-read every meta from its manifest files, repairing any manifest that can no longer be parsed.
     *
     * The metas are built from the files, so this is what makes a refreshed — or a repaired — manifest
     * visible. It is the same sequence for all three callers (construction, [update] and the background
     * [refreshManifests]), and the order matters: the Minecraft meta is what the loader metas resolve their
     * versions against, and Quilt reads Fabric's intermediaries.
     *
     * @throws IOException                  if a manifest can not be read and can not be repaired either.
     * @throws ParserConfigurationException indicates a serious configuration error.
     * @throws SAXException                 if an XML manifest can not be parsed and can not be repaired.
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun updateMetas() {
        updateOrRepair(minecraftManifest) { minecraft.update() }
        updateOrRepair(fabricIntermediariesManifest) { fabricIntermediaries.update() }
        updateOrRepair(fabricManifest, fabricInstallerManifest) { fabric.update() }
        updateOrRepair(legacyFabricGameManifest, legacyFabricLoaderManifest, legacyFabricInstallerManifest) {
            legacyFabric.update()
        }
        updateOrRepair(forgeManifest) { forge.update() }
        updateOrRepair(oldNeoForgeManifest, newNeoForgeManifest) { neoForge.update() }
        updateOrRepair(quiltManifest, quiltInstallerManifest) { quilt.update() }
    }

    /**
     * Run [update], and if it fails, repair whichever of [manifests] can no longer be parsed and run it once
     * more.
     *
     * A manifest is seeded from the jar only when it is **absent**, so without this an unreadable one that
     * exists is unrecoverable from inside the process: a mid-write truncation kills every start here, through
     * `ApiWrapper.setup()` and straight out of `main`, naming neither the file nor a remedy. A failure that
     * repaired nothing is rethrown unchanged — there is
     * no data to work with, and hiding that would leave every lookup answering about an empty meta, which is
     * how an engine defect gets published as a statement about a mod.
     *
     * [manifests] — The manifest files this meta is built from. [update] — Re-reads the meta from those files.
     *
     * @author Griefed
     */
    private fun updateOrRepair(vararg manifests: File, update: () -> Unit) {
        try {
            update()
        } catch (ex: Exception) {
            val repaired = manifests.filter { manifestIntegrity.restoreIfUnreadable(it) }
            if (repaired.isEmpty()) {
                throw ex
            }
            log.warn("Repaired ${repaired.joinToString { it.name }} and re-read the versions it declares.")
            update()
        }
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
        updateMetas()

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
     * @author Griefed
     */
    private fun checkManifest(
        manifestToCheck: File,
        /** The URL to the manifest. */
        urlToManifest: URL,
        /**
         * The type of the manifest, either [Type.MINECRAFT], [Type.FORGE], [Type.FABRIC] or
         * [Type.FABRIC_INSTALLER].
         */
        manifestType: Type
    ) = manifestUpdater.checkManifest(manifestToCheck, urlToManifest, manifestType)

    /**
     * Refresh every meta from the manifests on disk, returning this instance.
     *
     * **Serialised**, because the metas are refreshed from a background coroutine *and* by callers: each
     * meta publishes an internally consistent snapshot, but two overlapping runs could still leave one
     * meta on generation A beside another on generation B, so a lookup could miss a version its own release
     * list contained. The lock is uncontended in the normal case — one refresh at startup — and a refresh is
     * far too coarse to be on any hot path.
     *
     * @throws ParserConfigurationException indicates a serious configuration error.
     * @throws IOException                  if any IO errors occur.
     * @throws SAXException                 if any parse errors occur.
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    @Synchronized
    fun update(): VersionMeta {
        checkManifests()
        updateMetas()
        return this
    }
}