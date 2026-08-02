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

import de.griefed.serverpackcreator.clientside.LoaderVersionPolicy
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * A [LoaderVersionPolicy] that boots a loader build the [cache] already holds, falling back to [newest] only
 * when nothing is installed for that `(loader, Minecraft)` pair.
 *
 * **Why:** loaders ship builds constantly, and every new build is a fresh ~150 MB networked install for a
 * server that behaves, for the purposes of "does this mod boot", identically to the build already cached. On a
 * catalog-wide sweep that difference is the bulk of the work — the install dominates, the boot itself takes
 * seconds. Reusing the cached build turns the cache from something that grows with every loader release into
 * something bounded by the `(Minecraft, loader)` pairs actually crawled.
 *
 * **Why this is safe:** the newest build is still reported truthfully by [latestVersion], and `BootVerifier`
 * uses that for the two things a preference must never weaken — the support gate that decides whether the
 * combination is bootable at all, and the **crash re-check**. A mod that needs a newer loader than the cached
 * build fails to load, which looks exactly like a crash; without the re-check it would be published as a
 * HIGH-confidence clientside mod. `BootVerifier.recheckCrashOnNewestVersion` re-boots such a crash on the
 * newest build before letting it stand, so the reuse can only ever cost one extra boot, never a wrong verdict.
 *
 * @param newest            The authoritative policy (SPC's `LoaderVersionResolver`) — always consulted for the newest.
 * @param cache             The install cache whose contents become the preference.
 * @param availableVersions All known builds for a `(loader, Minecraft)` pair, newest first, used only to step down
 *                          from a build whose install the cache is refusing. Defaults to none, which keeps the
 *                          previous behaviour; the grinder supplies SPC's version metadata.
 * @author Griefed
 */
class CachedLoaderVersions(
    private val newest: LoaderVersionPolicy,
    private val cache: LoaderCache,
    private val availableVersions: (loader: String, minecraftVersion: String) -> List<String> = { _, _ -> emptyList() }
) : LoaderVersionPolicy {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * The most recently used installed build for the pair, or the newest when none is cached. Most-recently-used
     * is deliberate: it keeps the sweep on one build per pair, which also keeps that build warm against
     * [LoaderCache.evictUnusedSince] instead of rotating through builds and re-installing each in turn.
     */
    override fun preferredVersion(loader: String, minecraftVersion: String): String? {
        val cached = cache.installedVersions(loader, minecraftVersion).firstOrNull()
            ?: return firstInstallableVersion(loader, minecraftVersion)
        val newestVersion = newest.latestVersion(loader, minecraftVersion)
        if (newestVersion != null && newestVersion != cached) {
            log.info(
                "Reusing cached $loader $cached for Minecraft $minecraftVersion instead of installing $newestVersion " +
                    "(a crash on it is re-checked against $newestVersion before it counts)."
            )
        }
        return cached
    }

    /** Always the delegate's answer: the support gate and the crash re-check must not see a cached preference. */
    override fun latestVersion(loader: String, minecraftVersion: String): String? =
        newest.latestVersion(loader, minecraftVersion)

    /**
     * With nothing cached, the newest build the cache is not already refusing to install — otherwise the newest,
     * whatever its state.
     *
     * A loader version can be listed by its maven metadata while its installer artifact is simply absent: measured
     * 2026-07-30, NeoForge `21.1.247` is in the version index but `neoforge-21.1.247-installer.jar` **404s**, and
     * `1.21.1 + NeoForge` is one of the most common combinations in the catalogue. Every candidate wanting it paid a
     * full download-and-boot before failing, then took an INCONCLUSIVE verdict — for a build that cannot be installed
     * at all. Stepping down to the previous build turns that dead combination back into a real verdict.
     *
     * [latestVersion] is deliberately **not** consulted here and stays truthful, so the support gate and the crash
     * re-check still measure against the real newest: a crash on a stepped-down build is re-checked against the
     * newest exactly as a cached build's crash is, and if that newest is the uninstallable one the re-check comes
     * back INCONCLUSIVE, which by design leaves the crash standing rather than clearing it.
     */
    private fun firstInstallableVersion(loader: String, minecraftVersion: String): String? {
        val newestVersion = newest.preferredVersion(loader, minecraftVersion) ?: return null
        if (!cache.isInstallOnCooldown(loader, newestVersion, minecraftVersion)) {
            return newestVersion
        }
        val fallback = availableVersions(loader, minecraftVersion)
            .firstOrNull { it != newestVersion && !cache.isInstallOnCooldown(loader, it, minecraftVersion) }
        if (fallback == null) {
            log.warn(
                "$loader $newestVersion for Minecraft $minecraftVersion is on install cooldown and no older build is " +
                    "available to fall back to; candidates needing this combination stay inconclusive."
            )
            return newestVersion
        }
        log.info(
            "$loader $newestVersion for Minecraft $minecraftVersion could not be installed, so falling back to " +
                "$loader $fallback. The newest is still reported truthfully, so the support gate and the crash " +
                "re-check are unaffected."
        )
        return fallback
    }
}
