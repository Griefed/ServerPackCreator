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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Settings-group for the web-service side of ServerPackCreator: the MongoDB database-URI with
 * legacy-value migration and scheme-normalization, plus the cron-schedules of the webservice's
 * cleanup- and refresh-jobs. Extracted from ApiProperties (refactor Phase 1b); ApiProperties
 * remains the facade through which consumers access these values.
 */
class WebserviceConfig(private val store: PropertyStore) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Property keys for the web backend: database URI and the cleanup schedules. */

    companion object {
        /**
         * Database-URI used when none is configured or a legacy non-MongoDB URI is encountered.
         */
        const val FALLBACK_DATABASE_URI = "mongodb\\://user\\:password@localhost\\:27017/serverpackcreatordb"

        /**
         * Property-key holding the MongoDB database-URI.
         *
         * `spring.mongodb.uri` since Spring Boot 4.0.0 retired [LEGACY_DATABASE_URI_KEY]. Writing the old
         * key means Boot binds nothing and silently uses its own default, `mongodb://localhost/test` — so
         * this constant's *value* is load-bearing, not cosmetic. Pinned by `DatabaseUriPropertyTest`.
         */
        const val DATABASE_URI_KEY = "spring.mongodb.uri"

        /**
         * The pre-Spring-Boot-4 property-key, still read when [DATABASE_URI_KEY] is absent so an existing
         * installation keeps its configured database without anyone editing a file.
         *
         * Boot itself no longer binds it: its metadata carries `deprecation.level = "error"` since 4.0.0.
         * We read it, translate it, and write [DATABASE_URI_KEY] — Spring never sees this key again.
         */
        const val LEGACY_DATABASE_URI_KEY = "spring.data.mongodb.uri"

        /**
         * Property-key holding the cron-schedule for the webservice's cleanup-job.
         */
        const val CLEANUP_SCHEDULE_KEY = "de.griefed.serverpackcreator.spring.schedules.database.cleanup"

        /**
         * Property-key holding the cron-schedule for the webservice's version-refresh-job.
         */
        const val VERSION_SCHEDULE_KEY = "de.griefed.serverpackcreator.spring.schedules.versions.refresh"

        /**
         * Property-key holding the cron-schedule for the webservice's file-cleanup-job.
         */
        const val DATABASE_CLEANUP_SCHEDULE_KEY = "de.griefed.serverpackcreator.spring.schedules.files.cleanup"
    }

    /**
     * Fallback cron-schedule for the webservice's cleanup-job.
     */
    val fallbackCleanupSchedule = "0 0 0 * * *"

    /**
     * Fallback cron-schedule for the webservice's version-refresh-job.
     */
    val fallbackVersionSchedule = "0 0 0 * * *"

    /**
     * Fallback cron-schedule for the webservice's file-cleanup-job.
     */
    val fallbackDatabaseCleanupSchedule = "0 0 0 * * *"

    /**
     * URI of the MongoDB-database used by the webservice. Reading migrates legacy SQLite- or
     * PostgreSQL-URIs to [FALLBACK_DATABASE_URI]; writing prefixes the mongodb://-scheme when
     * missing.
     */
    var databaseUri: String = FALLBACK_DATABASE_URI
        get() {
            // The legacy key is the fallback, not an equal: an installation that predates Spring Boot 4
            // has only that one, and reading it here is what keeps its database configured. Anything found
            // under it is re-written under DATABASE_URI_KEY below, so Spring only ever sees the live key.
            var dbPath = store.properties.getProperty(DATABASE_URI_KEY)
                ?: store.properties.getProperty(LEGACY_DATABASE_URI_KEY, FALLBACK_DATABASE_URI)
            if (dbPath.isEmpty() ||
                dbPath.contains("sqlite") ||
                dbPath.contains("postgresql") ||
                // Not `startsWith("mongodb")`: that accepts the degenerate `mongodb:` a partially-configured
                // container used to produce, which then fails deep in the driver instead of here.
                !(dbPath.startsWith("mongodb://") || dbPath.startsWith("mongodb+srv://"))
            ) {
                log.warn("Your $DATABASE_URI_KEY-property didn't match a MongoDB-URL: $dbPath. It has been migrated to $FALLBACK_DATABASE_URI.")
                dbPath = FALLBACK_DATABASE_URI
            }
            store.define(DATABASE_URI_KEY, dbPath)
            field = dbPath
            return field
        }
        set(value) {
            if (!value.startsWith("mongodb://")) {
                store.define(DATABASE_URI_KEY, "mongodb://$value")
            } else {
                store.define(DATABASE_URI_KEY, value)
            }
            field = store.properties.getProperty(DATABASE_URI_KEY)
            log.info("Set database url to: $field.")
            log.warn("Restart ServerPackCreator for this change to take effect.")
        }

    /**
     * Cron-schedule of the webservice's cleanup-job, stored under [CLEANUP_SCHEDULE_KEY].
     */
    var cleanupSchedule: String
        get() = store.properties.getProperty(CLEANUP_SCHEDULE_KEY)
        set(value) {
            store.define(CLEANUP_SCHEDULE_KEY, value)
        }

    /**
     * Cron-schedule of the webservice's version-refresh-job, stored under [VERSION_SCHEDULE_KEY].
     */
    var versionSchedule: String
        get() = store.properties.getProperty(VERSION_SCHEDULE_KEY)
        set(value) {
            store.define(VERSION_SCHEDULE_KEY, value)
        }

    /**
     * Cron-schedule of the webservice's file-cleanup-job, stored under
     * [DATABASE_CLEANUP_SCHEDULE_KEY].
     */
    var databaseCleanupSchedule: String
        get() = store.properties.getProperty(DATABASE_CLEANUP_SCHEDULE_KEY)
        set(value) {
            store.define(DATABASE_CLEANUP_SCHEDULE_KEY, value)
        }

    /**
     * The default database-URI, for resetting the configuration to factory-state.
     */
    fun defaultDatabase(): String = FALLBACK_DATABASE_URI
}
