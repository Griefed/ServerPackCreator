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
 * @param newest The authoritative policy (SPC's `LoaderVersionResolver`) — always consulted for the newest.
 * @param cache  The install cache whose contents become the preference.
 * @author Griefed
 */
class CachedLoaderVersions(
    private val newest: LoaderVersionPolicy,
    private val cache: LoaderCache
) : LoaderVersionPolicy {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * The most recently used installed build for the pair, or the newest when none is cached. Most-recently-used
     * is deliberate: it keeps the sweep on one build per pair, which also keeps that build warm against
     * [LoaderCache.evictUnusedSince] instead of rotating through builds and re-installing each in turn.
     */
    override fun preferredVersion(loader: String, minecraftVersion: String): String? {
        val cached = cache.installedVersions(loader, minecraftVersion).firstOrNull()
            ?: return newest.preferredVersion(loader, minecraftVersion)
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
}
