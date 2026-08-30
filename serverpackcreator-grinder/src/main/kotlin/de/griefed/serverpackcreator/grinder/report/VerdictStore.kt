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
     * Persist anything buffered, if this store buffers at all.
     *
     * Defaulted to a no-op so an implementation that writes through — or holds nothing on disk, like the
     * in-memory store — needs no ceremony, while a caller that must not lose work (the shutdown hook) can ask
     * without knowing which it holds.
     */
    fun flush() {}

    /**
     * Whether any verdict has been recorded for this project **on [platform]**. Answers "seen at all", which is
     * *not* what the daemon's skip check asks — `Grinder.grind` uses [newestVerification] against the re-verify TTL,
     * because a stale verdict must be re-ground. Kept as the readable predicate for that narrower question.
     *
     * Slugs are not globally unique —
     * `jei` exists on Modrinth *and* CurseForge — so the platform is part of the identity; without it one
     * platform's verdict would suppress grinding the other's project entirely.
     */
    fun hasVerdictFor(platform: String, slug: String, projectId: String? = null): Boolean =
        all().any { it.platform == platform && it.identifies(slug, projectId) }

    /**
     * The newest [GrindVerdict.verifiedAt] across the per-loader verdicts of [slug] on [platform], or
     * `null` when that project has never been ground. Lets a continuous grind re-verify only *stale*
     * projects (older than a TTL) while skipping fresh ones. Default-computed from [all]; both
     * implementations inherit it.
     */
    fun newestVerification(platform: String, slug: String, projectId: String? = null): Instant? =
        all().filter { it.platform == platform && it.identifies(slug, projectId) }.maxOfOrNull { it.verifiedAt }
}

/**
 * The dedup identity of a verdict: platform + project-slug + loader. Shared by both stores so their key
 * schemes cannot drift apart (they once did — see the NUL-separator fix).
 */
internal fun verdictKey(platform: String, slug: String, loader: String, projectId: String? = null) =
    if (projectId.isNullOrBlank()) "$platform/slug:$slug/$loader" else "$platform/id:$projectId/$loader"

/** The dedup identity of [this] verdict, so both stores derive their key the same way. */
internal fun GrindVerdict.identityKey() = verdictKey(platform, slug, loader, projectId)

/**
 * Whether this verdict is about the project denoted by [slug]/[projectId]. Ids win when both sides have one — that
 * is what lets a renamed project be recognised — and the slug is the fallback for verdicts recorded before ids were
 * tracked.
 */
internal fun GrindVerdict.identifies(slug: String, projectId: String?): Boolean =
    if (!projectId.isNullOrBlank() && !this.projectId.isNullOrBlank()) this.projectId == projectId
    else this.slug == slug

/**
 * The key a *legacy* (id-less) row for the same project would have had, or `null` when [verdict] is itself id-less.
 * Recording an identified verdict removes that row, so a store converges on project identity as projects are
 * re-ground instead of holding both a slug-keyed and an id-keyed copy. This is the whole migration.
 */
internal fun supersededLegacyKey(verdict: GrindVerdict): String? =
    verdict.projectId?.takeIf { it.isNotBlank() }
        ?.let { verdictKey(verdict.platform, verdict.slug, verdict.loader, null) }

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
        // Drop the id-less row for this project first, so an identified verdict replaces it rather than
        // sitting beside it. Without this a project ground before ids existed would hold two rows for good.
        supersededLegacyKey(verdict)?.let { verdicts.remove(it) }
        verdicts[verdict.identityKey()] = verdict
    }

    override fun all(): List<GrindVerdict> = verdicts.values.toList()
}
