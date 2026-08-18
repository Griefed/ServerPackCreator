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
import org.bson.Document
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.MongoTemplate
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
 * @param mongoTemplate Used directly, because the mapped `RunConfiguration` type can no longer read the
 * old shape — that is precisely what is being fixed, so the repository is useless here.
 * @param migration The per-document transformation.
 */
@Component
class RunConfigurationListMigrationRunner(
    private val mongoTemplate: MongoTemplate,
    private val migration: RunConfigurationListMigration
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

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
            val collection = mongoTemplate.getCollection(COLLECTION)
            var rewritten = 0
            var inspected = 0
            for (storedConfig in collection.find()) {
                inspected++
                if (!migration.needsRewrite(storedConfig)) {
                    continue
                }
                val migrated = migration.rewrite(storedConfig)
                collection.replaceOne(Document("_id", storedConfig["_id"]), migrated)
                rewritten++
            }
            if (rewritten == 0) {
                log.debug("No run-configurations needed migrating ($inspected inspected).")
                return
            }
            log.info("Migrated $rewritten of $inspected run-configurations to embedded mod-lists.")
            dropOrphanedCollections()
        } catch (ex: Exception) {
            // Deliberately broad and non-fatal: an unreachable or partially-migrated database must not
            // stop the application from starting. The next start retries, because the check is per
            // document and idempotent.
            log.error("Could not migrate run-configuration mod-lists. Will retry on next start.", ex)
        }
    }

    /**
     * Drops the collections whose documents held nothing but their own id. Each failure is logged and
     * skipped: leaving an unused collection behind is harmless, where failing the migration over it is not.
     */
    private fun dropOrphanedCollections() {
        for (orphaned in ORPHANED_COLLECTIONS) {
            try {
                if (mongoTemplate.collectionExists(orphaned)) {
                    mongoTemplate.dropCollection(orphaned)
                    log.info("Dropped now-unused collection '$orphaned'.")
                }
            } catch (ex: Exception) {
                log.warn("Could not drop now-unused collection '$orphaned'. It is harmless to leave.", ex)
            }
        }
    }
}
