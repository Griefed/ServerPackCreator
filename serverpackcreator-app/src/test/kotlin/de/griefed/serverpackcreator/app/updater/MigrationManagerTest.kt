package de.griefed.serverpackcreator.app.updater

import com.electronwill.nightconfig.toml.TomlParser
import de.griefed.serverpackcreator.api.ApiProperties
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
        val (manager, _) = managerFor(previousVersion = "5.0.0", currentVersion = "6.0.0")
        val message = manager.MigrationMessage("5.0.0", "6.0.0", mutableListOf("Changed A", "Changed B"))
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
}
