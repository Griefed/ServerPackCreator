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
        store.define("spring.data.mongodb.uri", "jdbc:sqlite:serverpackcreator.db")
        Assertions.assertEquals(WebserviceConfig.FALLBACK_DATABASE_URI, webserviceConfig.databaseUri)
        Assertions.assertEquals(
            WebserviceConfig.FALLBACK_DATABASE_URI,
            store.properties.getProperty("spring.data.mongodb.uri")
        )
    }

    /**
     * Pins that a valid MongoDB-URI is returned unchanged.
     */
    @Test
    fun databaseUriKeepsValidMongoValues() {
        val store = PropertyStore()
        val webserviceConfig = WebserviceConfig(store)
        store.define("spring.data.mongodb.uri", "mongodb://user:pass@somehost:27017/spcdb")
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
            store.properties.getProperty("spring.data.mongodb.uri")
        )
        webserviceConfig.databaseUri = "mongodb://user:pass@otherhost:27017/spcdb"
        Assertions.assertEquals(
            "mongodb://user:pass@otherhost:27017/spcdb",
            store.properties.getProperty("spring.data.mongodb.uri")
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
}
