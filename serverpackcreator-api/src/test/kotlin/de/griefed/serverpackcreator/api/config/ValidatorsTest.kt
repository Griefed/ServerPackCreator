package de.griefed.serverpackcreator.api.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Direct tests for the validators extracted from ConfigurationHandler in refactor Phase 1c:
 * [InclusionsValidator] and [ModpackDirectoryValidator]. The [ModloaderValidator] is pinned
 * through ConfigurationHandlerTest and the characterization tests, since its version-checks
 * require the full version-meta.
 */
internal class ValidatorsTest {

    /**
     * Pins that empty inclusions fail, an existing modpack-relative source passes, and a
     * missing source fails the inclusions-check.
     */
    @Test
    fun inclusionsRequireExistingSources(@TempDir tempDir: File) {
        val inclusionsValidator = InclusionsValidator()
        val emptyCheck = inclusionsValidator.checkInclusions(mutableListOf(), tempDir.absolutePath)
        Assertions.assertFalse(emptyCheck.inclusionsChecksPassed, "Empty inclusions must fail")

        File(tempDir, "config").mkdirs()
        val existingCheck = inclusionsValidator.checkInclusions(
            mutableListOf(InclusionSpecification("config")), tempDir.absolutePath
        )
        Assertions.assertTrue(existingCheck.inclusionsChecksPassed, existingCheck.inclusionErrors.joinToString("; "))

        val missingCheck = inclusionsValidator.checkInclusions(
            mutableListOf(InclusionSpecification("no_such_dir")), tempDir.absolutePath
        )
        Assertions.assertFalse(missingCheck.inclusionsChecksPassed, "Missing sources must fail")
    }

    /**
     * Pins that invalid in-/exclusion-filter regexes fail the inclusions-check.
     */
    @Test
    fun inclusionsRejectInvalidFilterRegexes(@TempDir tempDir: File) {
        val inclusionsValidator = InclusionsValidator()
        File(tempDir, "config").mkdirs()
        val inclusion = InclusionSpecification("config", null, "[invalid(regex")
        val configCheck = inclusionsValidator.checkInclusions(mutableListOf(inclusion), tempDir.absolutePath)
        Assertions.assertFalse(configCheck.inclusionsChecksPassed, "Invalid inclusion-regex must fail")
    }

    /**
     * Pins that lazy-mode as the sole inclusion passes the check without errors.
     */
    @Test
    fun lazyModeAloneIsAccepted(@TempDir tempDir: File) {
        val inclusionsValidator = InclusionsValidator()
        val configCheck = inclusionsValidator.checkInclusions(
            mutableListOf(InclusionSpecification("lazy_mode")), tempDir.absolutePath
        )
        Assertions.assertTrue(configCheck.inclusionsChecksPassed)
    }

    /**
     * Pins the modpack-directory check: empty paths and missing directories fail, an installed
     * modpack passes, and a directory containing "overrides" fails as an uninstalled export.
     */
    @Test
    fun modpackDirectoryMustExistAndContainNoOverrides(@TempDir tempDir: File) {
        val directoryValidator = ModpackDirectoryValidator()
        Assertions.assertFalse(directoryValidator.checkModpackDir("").modpackChecksPassed)
        Assertions.assertFalse(
            directoryValidator.checkModpackDir(File(tempDir, "missing").absolutePath).modpackChecksPassed
        )

        val installedModpack = File(tempDir, "installed")
        File(installedModpack, "mods").mkdirs()
        Assertions.assertTrue(directoryValidator.checkModpackDir(installedModpack.absolutePath).modpackChecksPassed)

        val exportedModpack = File(tempDir, "exported")
        File(exportedModpack, "overrides").mkdirs()
        Assertions.assertFalse(
            directoryValidator.checkModpackDir(exportedModpack.absolutePath).modpackChecksPassed,
            "Modpacks containing an overrides-directory must fail"
        )
    }
}
