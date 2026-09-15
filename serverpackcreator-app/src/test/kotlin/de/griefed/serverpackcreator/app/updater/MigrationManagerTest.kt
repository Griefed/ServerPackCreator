package de.griefed.serverpackcreator.app.updater

import com.electronwill.nightconfig.toml.TomlParser
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.settings.WebserviceConfig
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Characterization tests pinning the version-decision logic of [MigrationManager.migrate] before
 * any app-module restructuring (Phase 2). ApiProperties is mocked so the previous- and
 * current-version can be controlled directly; the chosen version-ranges never match a real
 * migration-method (the highest is 6.0.0), so migrate() never touches the filesystem. Behavior
 * is observed through the setOldVersion-callback.
 */
internal class MigrationManagerTest {
    private val tomlParser = TomlParser()

    /**
     * Builds a MigrationManager whose mocked ApiProperties reports the given previous- and
     * current-version. setOldVersion is stubbed and returned for verification.
     */
    private fun managerFor(previousVersion: String, currentVersion: String): Pair<MigrationManager, ApiProperties> {
        val apiProperties = mockk<ApiProperties>()
        every { apiProperties.oldVersion() } returns previousVersion
        every { apiProperties.apiVersion } returns currentVersion
        justRun { apiProperties.setOldVersion(any()) }
        return MigrationManager(apiProperties, tomlParser) to apiProperties
    }

    /**
     * Pins the first-run behavior: an empty previous-version stores the current version and runs
     * no migrations.
     */
    @Test
    fun firstRunStoresCurrentVersionWithoutMigrations() {
        val (manager, apiProperties) = managerFor(previousVersion = "", currentVersion = "6.0.0")
        manager.migrate()
        verify(exactly = 1) { apiProperties.setOldVersion("6.0.0") }
        Assertions.assertTrue(manager.migrationMessages.isEmpty())
    }

    /**
     * Pins that running a dev/alpha/beta current-version skips migrations entirely, leaving the
     * stored old-version untouched.
     */
    @Test
    fun devCurrentVersionSkipsMigrations() {
        val (manager, apiProperties) = managerFor(previousVersion = "6.0.0", currentVersion = "dev")
        manager.migrate()
        verify(exactly = 0) { apiProperties.setOldVersion(any()) }
        Assertions.assertTrue(manager.migrationMessages.isEmpty())
    }

    /**
     * Pins that upgrading from a pre-release previous-version skips migrations.
     */
    @Test
    fun upgradeFromPreReleaseSkipsMigrations() {
        val (manager, apiProperties) = managerFor(previousVersion = "6.0.0-beta.1", currentVersion = "6.0.1")
        manager.migrate()
        verify(exactly = 0) { apiProperties.setOldVersion(any()) }
    }

    /**
     * Pins that a non-release current-version (a development branch) skips migrations.
     */
    @Test
    fun nonReleaseCurrentVersionSkipsMigrations() {
        val (manager, apiProperties) = managerFor(previousVersion = "6.0.0", currentVersion = "main")
        manager.migrate()
        verify(exactly = 0) { apiProperties.setOldVersion(any()) }
    }

    /**
     * Pins that a normal release-upgrade with no matching migration-methods still records the
     * new version as the old version for the next run.
     */
    @Test
    fun releaseUpgradeWithoutMatchingMethodsStoresVersion() {
        val (manager, apiProperties) = managerFor(previousVersion = "99.0.0", currentVersion = "99.0.1")
        manager.migrate()
        verify(exactly = 1) { apiProperties.setOldVersion("99.0.1") }
        Assertions.assertTrue(manager.migrationMessages.isEmpty())
    }

    /**
     * Pins that the MigrationMessage value-object formats its from/to versions, change-count and
     * rendered text as the GUI and log expect.
     */
    @Test
    fun migrationMessageRendersChanges() {
        // No MigrationManager needed: MigrationMessage is a nested value-object, not an inner class.
        val message = MigrationManager.MigrationMessage("5.0.0", "6.0.0", mutableListOf("Changed A", "Changed B"))
        Assertions.assertEquals("5.0.0", message.fromVersion())
        Assertions.assertEquals("6.0.0", message.toVersion())
        Assertions.assertEquals(2, message.count())
        Assertions.assertEquals(listOf("Changed A", "Changed B"), message.changes())
        val rendered = message.get()
        Assertions.assertTrue(rendered.contains("From 5.0.0 to 6.0.0"))
        Assertions.assertTrue(rendered.contains("(1): Changed A"))
        Assertions.assertTrue(rendered.contains("(2): Changed B"))
    }

    /**
     * Pins the regex that strips the Kotlin compiler's synthetic lambda suffix off a migration
     * method's name. Both migration discovery and version parsing run every declared method name
     * through it, so a name it fails to clean reaches `toInt()` as e.g. `0lambda` and takes the whole
     * migration run down — or, worse, cleans too much and silently resolves to the wrong version.
     *
     * It had no coverage while being written out twice, in two escaping-heavy copies. Now one
     * constant, pinned here.
     */
    @Test
    fun theLambdaSuffixIsStrippedFromMethodNames() {
        val strip = { name: String -> name.replace(MigrationManager.LAMBDA_SUFFIX, "") }

        // The shapes the compiler actually emits.
        Assertions.assertEquals("SixDotZeroDotZero", strip("SixDotZeroDotZero\$0lambda\$1"))
        Assertions.assertEquals("SixDotZeroDotZero", strip("SixDotZeroDotZero\$lambda\$"))
        Assertions.assertEquals("SixDotZeroDotZero", strip("SixDotZeroDotZero\$12lambda\$34"))

        // A plain migration method must survive untouched.
        Assertions.assertEquals("SixDotZeroDotZero", strip("SixDotZeroDotZero"))
        Assertions.assertEquals("5.0.0", strip("5.0.0"))

        // "lambda" without the dollar-delimiters is part of a name, not a suffix.
        Assertions.assertEquals("lambda", strip("lambda"))
    }

    /**
     * Builds a manager whose mocked ApiProperties exposes a **real** [WebserviceConfig] over a real
     * [PropertyStore], so the 9.0.0 migration's effect on the stored properties is observable.
     */
    private fun managerWithStore(
        previousVersion: String,
        currentVersion: String,
        store: PropertyStore
    ): MigrationManager {
        val apiProperties = mockk<ApiProperties>()
        every { apiProperties.oldVersion() } returns previousVersion
        every { apiProperties.apiVersion } returns currentVersion
        every { apiProperties.webserviceConfig } returns WebserviceConfig(store)
        justRun { apiProperties.setOldVersion(any()) }
        return MigrationManager(apiProperties, tomlParser)
    }

    /**
     * Pins that upgrading to 9.0.0 normalises a database-URI configured under the pre-Spring-Boot-4 key
     * **and tells the operator it happened**.
     *
     * The normalisation alone is already done by `WebserviceConfig.databaseUri` on every read, on every
     * build type. What only a migration can do is *report* it: the rename is invisible otherwise, and an
     * operator whose own tooling, container environment or `overrides.properties` still writes
     * `spring.data.mongodb.uri` needs to know that file is no longer read by Spring.
     */
    @Test
    fun upgradingToNineZeroZeroReportsTheRenamedDatabaseProperty() {
        val store = PropertyStore()
        store.define(WebserviceConfig.LEGACY_DATABASE_URI_KEY, "mongodb://user:pass@dbhost:27017/spcdb")
        val manager = managerWithStore("8.1.1", "9.0.0", store)

        manager.migrate()

        Assertions.assertEquals(
            "mongodb://user:pass@dbhost:27017/spcdb",
            store.properties.getProperty(WebserviceConfig.DATABASE_URI_KEY),
            "The configured URI must be carried over to the key Spring Boot 4 actually reads"
        )
        val reported = manager.migrationMessages.flatMap { it.changes() }
        Assertions.assertTrue(
            reported.any { it.contains(WebserviceConfig.DATABASE_URI_KEY) },
            "The rename must be reported to the operator, naming the new key. Reported: $reported"
        )
    }

    /**
     * Pins that an installation which never configured the old key is left alone — no message, nothing
     * written. A migration that reports itself to everyone is noise, and noise gets ignored.
     */
    @Test
    fun upgradingToNineZeroZeroSaysNothingWhenTheOldKeyWasNeverUsed() {
        val store = PropertyStore()
        store.define(WebserviceConfig.DATABASE_URI_KEY, "mongodb://user:pass@dbhost:27017/spcdb")
        val manager = managerWithStore("8.1.1", "9.0.0", store)

        manager.migrate()

        Assertions.assertTrue(
            manager.migrationMessages.flatMap { it.changes() }.none { it.contains("mongodb") },
            "Nothing to migrate, so nothing should be reported"
        )
    }
}
