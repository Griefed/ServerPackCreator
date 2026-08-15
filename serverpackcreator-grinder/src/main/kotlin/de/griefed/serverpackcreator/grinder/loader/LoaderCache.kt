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
package de.griefed.serverpackcreator.grinder.loader

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Installs a fully-resolved server base (loader jars + libraries) for one version-tuple into a target
 * directory. The production implementation boots a one-off setup container **with** network so the
 * ServerStarterJar self-installs; tests supply a fake. This is the *only* place network egress is
 * allowed in the grinder — every actual mod-boot then runs offline against the cached result.
 *
 * @author Griefed
 */
fun interface LoaderInstaller {
    /**
     * Populate [target] with the installed server for the given tuple, returning whether it succeeded.
     * Must be self-contained (no reliance on the candidate mod); the result is reused across all mods
     * sharing that loader/Minecraft combination.
     */
    fun install(target: File, loader: String, loaderVersion: String, minecraftVersion: String): Boolean
}

/**
 * Per-`(loader, loaderVersion, minecraftVersion)` cache of an installed server base, so the expensive,
 * network-dependent install runs **once** and every actual mod-boot mounts the cached tree and runs
 * offline (`--network none`). A tuple counts as installed only once its [MARKER] file is present, so a
 * crash mid-install is redone rather than served half-baked; installs of the same tuple are serialized
 * so concurrent workers can't install it twice.
 *
 * The cache is **bounded by time, not size**: [evictUnusedSince] drops tuples nothing has booted for a while
 * (see [ensureInstalled], which stamps a tuple as used on every hit), because each one costs ~150 MB and a
 * long sweep keeps minting new ones as loaders ship builds.
 *
 * @param cacheRoot       Root directory under which per-tuple base trees live.
 * @param installer       Performs the one-off, network-using install on a cache miss.
 * @param failureCooldown How long a *failed* tuple is left alone before another install is attempted. Without
 *                        this, an upstream artefact that 404s costs a full download-and-boot for every candidate
 *                        that wants it, for the rest of the sweep.
 * @param clock           Supplies "now" for the cooldown (injectable for tests).
 * @author Griefed
 */
class LoaderCache(
    private val cacheRoot: File,
    private val installer: LoaderInstaller,
    private val failureCooldown: Duration = Duration.ofHours(1),
    private val clock: () -> Instant = Instant::now,
    private val templateProvenance: () -> String? = { null }
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Set once the operator has been told that pre-provenance installs are being trusted. */
    private val legacyProvenanceWarned = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Per-tuple locks so an install serializes by tuple without blocking unrelated tuples. */
    private val installLocks = ConcurrentHashMap<String, Any>()

    /**
     * When each tuple's install last failed, so a broken one is not re-attempted for every candidate that wants
     * it. Observed live: NeoForge 21.1.247's installer jar 404s upstream, and 1.21.1+NeoForge is one of the most
     * common combinations in the catalogue — without this, every such candidate paid a full download-and-boot
     * before failing, and turned a decisive boot into INCONCLUSIVE while doing so. In memory on purpose: a
     * restart is a reasonable moment to find out whether upstream has been fixed.
     */
    private val recentFailures = ConcurrentHashMap<String, Instant>()

    /** The cache directory for a version-tuple, whether or not it has been installed yet. */
    fun baseDirFor(loader: String, loaderVersion: String, minecraftVersion: String): File =
        File(cacheRoot, "${sanitize(minecraftVersion)}/${sanitize(loader)}/${sanitize(loaderVersion)}")

    /**
     * Whether the tuple's base is fully installed *and* was produced by the templates currently in force.
     *
     * The marker alone only says an install succeeded, which is why a template change that alters what an install
     * produces used to be served from cache regardless. A recorded provenance that differs from the current one is
     * therefore a miss. An **absent** provenance is not: it predates this field, and treating unknown as different
     * would re-install every cached tuple to answer a question that may not apply to it.
     */
    fun isInstalled(loader: String, loaderVersion: String, minecraftVersion: String): Boolean {
        val marker = File(baseDirFor(loader, loaderVersion, minecraftVersion), MARKER)
        if (!marker.isFile) {
            return false
        }
        val current = templateProvenance() ?: return true
        val recorded = recordedProvenance(marker) ?: run {
            warnAboutLegacyProvenanceOnce()
            return true
        }
        if (recorded == current) {
            return true
        }
        log.info(
            "Cached $loader $loaderVersion / Minecraft $minecraftVersion was installed with different start-script " +
                "templates (recorded ${recorded.take(12)}…, current ${current.take(12)}…); reinstalling so the " +
                "cached layer matches what a boot now expects."
        )
        return false
    }

    /** The template digest recorded in a completion [marker], or `null` for a marker written before it existed. */
    private fun recordedProvenance(marker: File): String? = runCatching {
        marker.readLines().firstOrNull { it.startsWith("$TEMPLATES_KEY=") }?.removePrefix("$TEMPLATES_KEY=")
    }.getOrNull()

    /**
     * Say once — not per tuple — that pre-provenance installs are being trusted, so an operator who changes a
     * template knows the older layers are not re-checked automatically and can invalidate by hand if it matters.
     */
    private fun warnAboutLegacyProvenanceOnce() {
        if (legacyProvenanceWarned.compareAndSet(false, true)) {
            log.warn(
                "Some cached loader installs predate template-provenance tracking and are being reused as-is. If a " +
                    "start-script template change altered what an install produces, delete the affected tuples " +
                    "under the cache root to force a reinstall; installs from now on record their provenance."
            )
        }
    }

    /**
     * Return the installed base directory for the tuple, running [installer] (once, with network) on a
     * miss. Returns `null` when the install fails — the partial tree is removed so the next attempt
     * retries cleanly. The install is serialized per tuple, so parallel workers share a single install.
     */
    fun ensureInstalled(loader: String, loaderVersion: String, minecraftVersion: String): File? {
        val baseDir = baseDirFor(loader, loaderVersion, minecraftVersion)
        if (markUsed(baseDir)) {
            return baseDir
        }
        synchronized(lockFor(baseDir)) {
            // Re-check under the lock: another worker may have installed it while we waited.
            if (markUsed(baseDir)) {
                return baseDir
            }
            val failedAt = recentFailures[baseDir.path]
            if (failedAt != null && Duration.between(failedAt, clock()) < failureCooldown) {
                log.debug(
                    "Not re-attempting $loader $loaderVersion / Minecraft $minecraftVersion — its install failed " +
                        "${Duration.between(failedAt, clock()).toMinutes()}m ago and is on cooldown."
                )
                return null
            }
            baseDir.deleteRecursively()
            baseDir.mkdirs()
            val installed = runCatching { installer.install(baseDir, loader, loaderVersion, minecraftVersion) }
                .onFailure { log.warn("Loader install threw for $loader $loaderVersion / Minecraft $minecraftVersion: ${it.message}") }
                .getOrDefault(false)
            if (!installed) {
                baseDir.deleteRecursively()
                // Remember the failure so the next candidate wanting this tuple fails fast instead of repeating a
                // full install; the reason was logged once, by whoever failed.
                if (recentFailures.put(baseDir.path, clock()) == null) {
                    log.warn(
                        "Install of $loader $loaderVersion / Minecraft $minecraftVersion failed — not retrying it " +
                            "for ${failureCooldown.toMinutes()}m. Candidates needing this combination will be " +
                            "reported INCONCLUSIVE until then."
                    )
                }
                return null
            }
            recentFailures.remove(baseDir.path)
            val provenance = templateProvenance()?.let { "$TEMPLATES_KEY=$it\n" } ?: ""
            File(baseDir, MARKER).writeText(
                "loader=$loader\nloaderVersion=$loaderVersion\nminecraftVersion=$minecraftVersion\n$provenance"
            )
            return baseDir
        }
    }

    /**
     * Every loader-version installed for the `(loader, minecraftVersion)` pair, **most recently used first**.
     *
     * Versions are read back from each tuple's [MARKER], not from the directory name: [baseDirFor] sanitizes
     * path segments, so a version string containing anything unusual would come back mangled and be handed to
     * pack generation as a version that does not exist. Incomplete installs (no marker) are skipped — they
     * cannot be booted. Used by [CachedLoaderVersions] to reuse an install instead of fetching a fresh one.
     */
    fun installedVersions(loader: String, minecraftVersion: String): List<String> {
        val loaderDir = File(cacheRoot, "${sanitize(minecraftVersion)}/${sanitize(loader)}")
        val versionDirs = loaderDir.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        return versionDirs
            .mapNotNull { dir -> File(dir, MARKER).takeIf { it.isFile }?.let { marker -> marker to recordedVersion(marker) } }
            .filter { (_, version) -> version != null }
            .sortedByDescending { (marker, _) -> marker.lastModified() }
            .mapNotNull { (_, version) -> version }
    }

    /**
     * Whether this tuple's install failed recently enough that [ensureInstalled] would refuse to retry it.
     *
     * Lets a version *policy* ask before choosing, rather than every candidate discovering it the expensive way: a
     * loader build whose installer artifact is missing upstream (NeoForge `21.1.247`, measured 2026-07-30) is on
     * cooldown after the first failure, and knowing that up front is what allows stepping down to an older build
     * instead of handing out an inconclusive verdict per candidate.
     */
    fun isInstallOnCooldown(loader: String, loaderVersion: String, minecraftVersion: String): Boolean {
        val failedAt = recentFailures[baseDirFor(loader, loaderVersion, minecraftVersion).path] ?: return false
        return Duration.between(failedAt, clock()) < failureCooldown
    }

    /** The raw `loaderVersion` a completion marker recorded, or `null` when it is unreadable. */
    private fun recordedVersion(marker: File): String? = runCatching {
        marker.readLines().firstOrNull { it.startsWith("loaderVersion=") }?.removePrefix("loaderVersion=")
    }.getOrNull()

    /**
     * Delete every cached tuple that has not been *used* within [retention], returning how many went. A
     * `zero`/negative retention disables eviction entirely (nothing is deleted), which is the opt-out.
     *
     * Without this the cache only grows: each tuple costs on the order of 150 MB, and loaders keep shipping
     * builds, so a months-long sweep mints new tuples indefinitely and eventually fills the disk. Eviction is
     * keyed on **last use** — [ensureInstalled] stamps the marker on every hit — so a tuple the sweep still
     * boots is never dropped however old its install is; only genuinely idle ones go, and a re-install costs
     * one networked setup boot if it comes back.
     *
     * Directories without a completion [MARKER] are swept regardless of age: an install that never finished
     * can never be served, so keeping it only leaks disk. Each candidate is examined under the same per-tuple
     * lock that installs take, so eviction can never delete a tree a worker is installing into.
     */
    fun evictUnusedSince(retention: Duration): Int {
        if (retention.isZero || retention.isNegative) {
            return 0
        }
        val cutoff = System.currentTimeMillis() - retention.toMillis()
        var evicted = 0
        for (baseDir in cachedTupleDirs()) {
            synchronized(lockFor(baseDir)) {
                val marker = File(baseDir, MARKER)
                // No marker at all counts as idle: nothing has claimed the tuple since it was written.
                val idle = !marker.isFile || marker.lastModified() < cutoff
                if (idle && baseDir.deleteRecursively()) {
                    evicted++
                    log.info("Evicted cached loader install ${baseDir.name} (${baseDir.parentFile?.name}) — unused for longer than ${retention.toDays()}d.")
                }
            }
        }
        return evicted
    }

    /**
     * Every `<minecraft>/<loader>/<loaderVersion>` directory currently under [cacheRoot]. Walked from disk
     * rather than from a registry so a cache left behind by an earlier run (or a crashed one) is covered too.
     */
    private fun cachedTupleDirs(): List<File> =
        (cacheRoot.listFiles()?.filter { it.isDirectory } ?: emptyList())
            .flatMap { minecraft -> minecraft.listFiles()?.filter { it.isDirectory } ?: emptyList() }
            .flatMap { loader -> loader.listFiles()?.filter { it.isDirectory } ?: emptyList() }

    /**
     * Whether [baseDir] holds a completed install, refreshing its last-use stamp when it does. The stamp is
     * what [evictUnusedSince] reads, so *asking* for a tuple is what keeps it alive.
     */
    private fun markUsed(baseDir: File): Boolean {
        val marker = File(baseDir, MARKER)
        if (!marker.isFile) {
            return false
        }
        marker.setLastModified(System.currentTimeMillis())
        return true
    }

    /**
     * Intern a lock per cache *directory* — deliberately keyed on the sanitized path rather than the raw
     * tuple, so installs and eviction agree on the same monitor (and two raw tuples that sanitize onto one
     * directory serialize, since they share its contents). Bounded by the finite tuple space.
     */
    private fun lockFor(baseDir: File): Any = installLocks.computeIfAbsent(baseDir.path) { Any() }

    /** Make a token safe to use as a path segment, collapsing anything unusual to an underscore. */
    private fun sanitize(token: String): String = token.replace(Regex("[^A-Za-z0-9._-]"), "_")

    /** The completion-marker name and the marker's template-provenance key. */

    companion object {
        /** Completion marker, written only after a successful install; its presence means cache-hit. */
        const val MARKER = ".spc-installed"

        /** Marker key holding the digest of the start-script templates an install was produced with. */
        internal const val TEMPLATES_KEY = "templates"
    }
}
