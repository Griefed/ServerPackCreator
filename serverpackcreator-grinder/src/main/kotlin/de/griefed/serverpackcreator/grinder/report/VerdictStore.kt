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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.grinder.GrindVerdict
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Where verdicts accumulate during a grind. A production implementation persists so a multi-day,
 * fire-and-forget run resumes after a restart instead of starting over; the in-memory implementation
 * here is the default and the test double. Implementations must be safe for concurrent [record] from
 * the worker pool.
 *
 * @author Griefed
 */
interface VerdictStore {
    /** Record (or replace, keyed by platform + project-slug + loader) one verdict. Thread-safe. */
    fun record(verdict: GrindVerdict)

    /** Every recorded verdict, in no guaranteed order (the table/CSV layer sorts). */
    fun all(): List<GrindVerdict>

    /**
     * Whether any verdict has been recorded for [slug] **on [platform]**. Slugs are not globally unique —
     * `jei` exists on Modrinth *and* CurseForge — so the platform is part of the identity; without it one
     * platform's verdict would suppress grinding the other's project entirely.
     */
    fun hasVerdictFor(platform: String, slug: String): Boolean =
        all().any { it.platform == platform && it.slug == slug }

    /**
     * The newest [GrindVerdict.verifiedAt] across the per-loader verdicts of [slug] on [platform], or
     * `null` when that project has never been ground. Lets a continuous grind re-verify only *stale*
     * projects (older than a TTL) while skipping fresh ones. Default-computed from [all]; both
     * implementations inherit it.
     */
    fun newestVerification(platform: String, slug: String): Instant? =
        all().filter { it.platform == platform && it.slug == slug }.maxOfOrNull { it.verifiedAt }
}

/**
 * The dedup identity of a verdict: platform + project-slug + loader. Shared by both stores so their key
 * schemes cannot drift apart (they once did — see the NUL-separator fix).
 */
internal fun verdictKey(platform: String, slug: String, loader: String) = "$platform/$slug/$loader"

/**
 * In-memory [VerdictStore], keyed by [verdictKey] (platform + slug + loader) so a re-verified
 * `(platform, project, loader)` replaces its previous verdict rather than duplicating — while the *same*
 * slug on a different platform stays a separate row. Backed by a [ConcurrentHashMap] so the worker pool
 * can record concurrently. Does not survive a restart — a persistent (file/Mongo) store is the production
 * upgrade.
 *
 * @author Griefed
 */
class InMemoryVerdictStore : VerdictStore {
    private val verdicts = ConcurrentHashMap<String, GrindVerdict>()

    override fun record(verdict: GrindVerdict) {
        verdicts[verdictKey(verdict.platform, verdict.slug, verdict.loader)] = verdict
    }

    override fun all(): List<GrindVerdict> = verdicts.values.toList()
}
