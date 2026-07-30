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
package de.griefed.serverpackcreator.clientside

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers `(loader, Minecraft)` combinations that **proved unbootable in practice**, so they stop being
 * selected after the first mod discovers it.
 *
 * Version metadata is not a reliable oracle for this. Measured on 2026-07-30: Fabric's own meta lists Minecraft
 * `26.1.2` and answers the intermediary query with a placeholder `0.0.0`, so `Meta.isMinecraftSupported` says yes
 * — and then the generated `start.sh` aborts with *"Fabric is not available for Minecraft 26.1.2"*. Because the
 * grinder always picks the **newest** Minecraft a mod supports, and 26.x is newest, that mistake repeated for
 * every Fabric mod that had updated: 103 wasted boots in one morning. The same shape appeared for NeoForge
 * `21.1.247`, whose installer jar 404s upstream.
 *
 * Learning from the abort is deliberately preferred over trusting any metadata source: it is self-correcting,
 * needs no per-loader special cases, and it *expires*, so a loader that publishes support later is tried again.
 *
 * @param retention How long a combination stays marked unbootable. Long enough to stop a sweep wasting boots,
 *                  short enough that a real upstream release is picked up without a restart.
 * @param clock     Supplies "now" (injectable for tests).
 * @author Griefed
 */
class LoaderSupportMemory(
    private val retention: Duration = Duration.ofHours(24),
    private val clock: () -> Instant = Instant::now
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val unbootable = ConcurrentHashMap<String, Instant>()

    /** Whether this combination may still be selected for a boot. */
    fun isUsable(loader: String, minecraftVersion: String): Boolean {
        val markedAt = unbootable[key(loader, minecraftVersion)] ?: return true
        if (Duration.between(markedAt, clock()) >= retention) {
            unbootable.remove(key(loader, minecraftVersion))
            return true
        }
        return false
    }

    /**
     * Record that [loader] could not actually boot [minecraftVersion], with the console's own [reason]. Logged
     * once per combination — the point is to say it loudly the first time and then be quiet, instead of
     * repeating the same wasted boot for every mod that targets that version.
     */
    fun rememberUnbootable(loader: String, minecraftVersion: String, reason: String) {
        if (unbootable.put(key(loader, minecraftVersion), clock()) == null) {
            log.warn(
                "$loader has no usable build for Minecraft $minecraftVersion despite the version metadata " +
                    "claiming support ($reason). Not selecting that combination again for ${retention.toHours()}h."
            )
        }
    }

    /** Combinations currently considered unbootable — for reporting/diagnostics. */
    fun knownUnbootable(): Set<String> = unbootable.keys.toSet()

    private fun key(loader: String, minecraftVersion: String) = "$loader/$minecraftVersion"
}
