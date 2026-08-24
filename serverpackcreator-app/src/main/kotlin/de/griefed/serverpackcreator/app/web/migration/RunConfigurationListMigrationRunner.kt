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
package de.griefed.serverpackcreator.app.web.migration

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Applies [RunConfigurationListMigration] to the stored run-configurations once the application is up.
 *
 * The three list fields on `RunConfiguration` changed from `@DBRef` arrays to embedded strings, so an
 * instance upgrading across that change would otherwise fail to read its own configurations. This
 * rewrites them in place.
 *
 * Runs on [ApplicationReadyEvent] rather than during context startup so a database that is slow or
 * briefly unreachable delays the migration instead of preventing the application from starting — the
 * Mongo driver connects lazily, and the rest of the app tolerates that (see `WebServiceContextTest`).
 *
 * Safe to run repeatedly: documents already in the new shape are skipped without a write, so a restart
 * costs one collection read and nothing else. Failures are logged and swallowed for the same reason —
 * an instance that cannot migrate should still serve what it can rather than refuse to boot.
 *
 * @param store The database operations, behind [MigrationStore] so this class's safety decisions —
 * rewrite before dropping, never drop when nothing was rewritten, keep going when one drop fails — can be
 * asserted without a database. They are the decisions that can lose data if wrong.
 * @param migration The per-document transformation.
 */
@Component
class RunConfigurationListMigrationRunner(
    private val store: MigrationStore,
    private val migration: RunConfigurationListMigration
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** The collection name and logger this runner needs. The name is stated because the rewrite uses `MongoTemplate` rather than the repository — the mapped type can no longer read the old shape, which is the problem being fixed. */
    companion object {
        /** The collection Spring Data maps `RunConfiguration` to. */
        const val COLLECTION = "runConfiguration"

        /** Collections that existed only to hold the referenced single-field documents. */
        val ORPHANED_COLLECTIONS = listOf("startArgument", "clientMod", "whitelistedMod")
    }

    /**
     * Rewrites every stored run-configuration still holding `DBRef` lists, then drops the three
     * collections that only existed to be pointed at.
     *
     * The drop happens **after** a fully successful rewrite pass, and never when nothing was rewritten
     * on a database that still has documents needing it — losing the referenced ids before the
     * configurations are rewritten would make the data unrecoverable.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun migrate() {
        try {
            val storedConfigs = store.findAll(COLLECTION)
            var rewritten = 0
            for (storedConfig in storedConfigs) {
                if (!migration.needsRewrite(storedConfig)) {
                    continue
                }
                store.replace(COLLECTION, storedConfig["_id"], migration.rewrite(storedConfig))
                rewritten++
            }
            val inspected = storedConfigs.size
            if (rewritten == 0) {
                log.debug("No run-configurations needed migrating ($inspected inspected).")
                return
            }
            log.info("Migrated $rewritten of $inspected run-configurations to embedded mod-lists.")
            dropOrphanedCollections()
        } catch (ex: Exception) {
            // Deliberately broad and non-fatal: an unreachable or partially-migrated database must not
            // stop the application from starting. The *rewrite* retries on the next start, because the
            // check is per document and idempotent. The drop does not: it only runs when something was
            // rewritten, so a pass that rewrote everything and then failed to drop leaves those
            // collections behind for good. Harmless -- they are unreferenced -- but do not read this as a
            // promise that they will eventually go.
            log.error("Could not migrate run-configuration mod-lists. The rewrite retries on next start.", ex)
        }
    }

    /**
     * Drops the collections whose documents held nothing but their own id. Each failure is logged and
     * skipped: leaving an unused collection behind is harmless, where failing the migration over it is not.
     */
    private fun dropOrphanedCollections() {
        for (orphaned in ORPHANED_COLLECTIONS) {
            try {
                if (store.exists(orphaned)) {
                    store.drop(orphaned)
                    log.info("Dropped now-unused collection '$orphaned'.")
                }
            } catch (ex: Exception) {
                log.warn("Could not drop now-unused collection '$orphaned'. It is harmless to leave.", ex)
            }
        }
    }
}
