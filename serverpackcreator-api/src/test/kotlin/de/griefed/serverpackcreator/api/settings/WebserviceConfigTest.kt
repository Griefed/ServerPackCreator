package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for [WebserviceConfig], the web-service settings-group extracted from ApiProperties in
 * refactor Phase 1b. Pins the MongoDB-URI migration- and normalization-behavior and the
 * schedule-properties' pass-through to the property-store.
 */
internal class WebserviceConfigTest {

    /**
     * Pins that a non-MongoDB database-URI (legacy SQLite/PostgreSQL or empty) is migrated to
     * the fallback-URI when read.
     */
    @Test
    fun databaseUriMigratesLegacyValuesOnRead() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        store.define(WebserviceConfig.LEGACY_DATABASE_URI_KEY, "jdbc:sqlite:serverpackcreator.db")
        Assertions.assertEquals(WebserviceConfig.FALLBACK_DATABASE_URI, webserviceConfig.databaseUri)
        // Written under the live key, which is no longer the key it was read from: Spring Boot 4 retired
        // the legacy one, so leaving the result there would leave it unbound.
        Assertions.assertEquals(
            WebserviceConfig.FALLBACK_DATABASE_URI,
            store.properties.getProperty(WebserviceConfig.DATABASE_URI_KEY)
        )
    }

    /**
     * Pins that a valid MongoDB-URI is returned unchanged.
     */
    @Test
    fun databaseUriKeepsValidMongoValues() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        store.define(WebserviceConfig.DATABASE_URI_KEY, "mongodb://user:pass@somehost:27017/spcdb")
        Assertions.assertEquals("mongodb://user:pass@somehost:27017/spcdb", webserviceConfig.databaseUri)
    }

    /**
     * Pins that setting a database-URI without the mongodb://-scheme gets the scheme prefixed.
     */
    @Test
    fun databaseUriSetterPrefixesMissingScheme() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        webserviceConfig.databaseUri = "user:pass@somehost:27017/spcdb"
        Assertions.assertEquals(
            "mongodb://user:pass@somehost:27017/spcdb",
            store.properties.getProperty(WebserviceConfig.DATABASE_URI_KEY)
        )
        webserviceConfig.databaseUri = "mongodb://user:pass@otherhost:27017/spcdb"
        Assertions.assertEquals(
            "mongodb://user:pass@otherhost:27017/spcdb",
            store.properties.getProperty(WebserviceConfig.DATABASE_URI_KEY)
        )
    }

    /**
     * Pins that the three webservice-schedules read and write through the property-store.
     */
    @Test
    fun schedulesPassThroughToStore() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        webserviceConfig.cleanupSchedule = "0 0 1 * * *"
        webserviceConfig.versionSchedule = "0 0 2 * * *"
        webserviceConfig.databaseCleanupSchedule = "0 0 3 * * *"
        Assertions.assertEquals("0 0 1 * * *", webserviceConfig.cleanupSchedule)
        Assertions.assertEquals("0 0 2 * * *", webserviceConfig.versionSchedule)
        Assertions.assertEquals("0 0 3 * * *", webserviceConfig.databaseCleanupSchedule)
        Assertions.assertEquals(
            "0 0 1 * * *",
            store.properties.getProperty("de.griefed.serverpackcreator.spring.schedules.database.cleanup")
        )
        Assertions.assertEquals(
            "0 0 2 * * *",
            store.properties.getProperty("de.griefed.serverpackcreator.spring.schedules.versions.refresh")
        )
        Assertions.assertEquals(
            "0 0 3 * * *",
            store.properties.getProperty("de.griefed.serverpackcreator.spring.schedules.files.cleanup")
        )
    }

    /**
     * Pins the upgrade path: an installation that only has the pre-Spring-Boot-4 key keeps its configured
     * database, and the value is re-written under the live key so Spring actually binds it.
     *
     * This is the mechanism that protects existing users, and it has to be the mechanism rather than the
     * `MigrationManager` step beside it: migrations are skipped entirely on dev, alpha and beta builds.
     */
    @Test
    fun aLegacyKeyIsAdoptedAndRewrittenUnderTheLiveKey() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        store.define(WebserviceConfig.LEGACY_DATABASE_URI_KEY, "mongodb://user:pass@legacyhost:27017/spcdb")

        Assertions.assertEquals(
            "mongodb://user:pass@legacyhost:27017/spcdb", webserviceConfig.databaseUri,
            "A configuration written before Spring Boot 4 must still be honoured"
        )
        Assertions.assertEquals(
            "mongodb://user:pass@legacyhost:27017/spcdb",
            store.properties.getProperty(WebserviceConfig.DATABASE_URI_KEY),
            "The adopted value must be re-written under the key Spring Boot actually binds"
        )
    }

    /**
     * Pins that the degenerate `mongodb:` value is rejected rather than passed to the driver.
     *
     * The old check was `!startsWith("mongodb")`, which `mongodb:` satisfies — so a partially-configured
     * container (one missing `SPC_DATABASE_*` variable used to produce exactly that string) got a URI the
     * driver rejects much later, with a far less obvious message.
     */
    @Test
    fun theDegenerateMongoSchemeIsRejected() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        store.define(WebserviceConfig.DATABASE_URI_KEY, "mongodb:")

        Assertions.assertEquals(WebserviceConfig.FALLBACK_DATABASE_URI, webserviceConfig.databaseUri)
    }

    /**
     * Pins that `mongodb+srv://` survives, since the tightened check enumerates schemes rather than
     * prefix-matching. A DNS-seedlist URI is what a hosted Atlas cluster hands out.
     */
    @Test
    fun theSrvSchemeIsAccepted() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        store.define(WebserviceConfig.DATABASE_URI_KEY, "mongodb+srv://user:pass@cluster.example.net/spcdb")

        Assertions.assertEquals(
            "mongodb+srv://user:pass@cluster.example.net/spcdb", webserviceConfig.databaseUri
        )
    }
}
