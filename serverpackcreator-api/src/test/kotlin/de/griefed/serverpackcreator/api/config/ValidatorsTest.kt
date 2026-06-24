package de.griefed.serverpackcreator.api.config

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Direct tests for the validators extracted from ConfigurationHandler in refactor Phase 1c:
 * [InclusionsValidator], [ModpackDirectoryValidator] and [ModloaderValidator]. The
 * modloader-version checks require the full version-meta, which is obtained from the (cached,
 * no-network) test [ApiWrapper].
 */
internal class ValidatorsTest {

    private val versionMeta =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).versionMeta

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

    /**
     * When lazy-mode is specified alongside other entries, lazy-mode is stripped and the remaining
     * entries are still validated — so a missing co-specified source still fails the check.
     */
    @Test
    fun lazyModeWithOtherEntriesIsStrippedAndRestValidated(@TempDir tempDir: File) {
        val inclusionsValidator = InclusionsValidator()
        File(tempDir, "config").mkdirs()
        val inclusions = mutableListOf(
            InclusionSpecification("lazy_mode"),
            InclusionSpecification("config"),
            InclusionSpecification("no_such_dir")
        )

        val configCheck = inclusionsValidator.checkInclusions(inclusions, tempDir.absolutePath)

        Assertions.assertFalse(configCheck.inclusionsChecksPassed, "A missing co-specified source must still fail")
        Assertions.assertFalse(
            inclusions.any { it.source == "lazy_mode" },
            "lazy_mode must be stripped when other entries are present"
        )
    }

    /**
     * A destination containing invalid path characters is rejected: it is nulled out and an
     * inclusion error is recorded. (Note: the current invalidity check only triggers when the
     * destination contains *every* forbidden character — see audit note on this latent bug.)
     */
    @Test
    fun invalidDestinationIsRejectedAndNulled(@TempDir tempDir: File) {
        val inclusionsValidator = InclusionsValidator()
        File(tempDir, "config").mkdirs()
        val inclusion = InclusionSpecification("config", "<>:\"|?*#%&{}\$!@`´=")

        val configCheck = inclusionsValidator.checkInclusions(mutableListOf(inclusion), tempDir.absolutePath)

        Assertions.assertNull(inclusion.destination, "An invalid destination must be nulled out")
        Assertions.assertFalse(configCheck.inclusionsChecksPassed, "An invalid destination must fail the check")
    }

    /**
     * A global filter entry (blank source plus a filter) is skipped from the source-existence
     * check and therefore passes even though its source does not exist on disk.
     */
    @Test
    fun globalFilterEntryIsSkippedFromSourceCheck(@TempDir tempDir: File) {
        val inclusionsValidator = InclusionsValidator()
        File(tempDir, "config").mkdirs()
        val inclusions = mutableListOf(
            InclusionSpecification("config"),
            InclusionSpecification("", null, null, "some.*exclusion")
        )

        val configCheck = inclusionsValidator.checkInclusions(inclusions, tempDir.absolutePath)

        Assertions.assertTrue(configCheck.inclusionsChecksPassed, configCheck.inclusionErrors.joinToString("; "))
    }

    /**
     * An invalid exclusion-filter regex fails the inclusions-check (complementing the existing
     * invalid-inclusion-filter test).
     */
    @Test
    fun inclusionsRejectInvalidExclusionRegex(@TempDir tempDir: File) {
        val inclusionsValidator = InclusionsValidator()
        File(tempDir, "config").mkdirs()
        val inclusion = InclusionSpecification("config", null, null, "[invalid(regex")

        val configCheck = inclusionsValidator.checkInclusions(mutableListOf(inclusion), tempDir.absolutePath)

        Assertions.assertFalse(configCheck.inclusionsChecksPassed, "Invalid exclusion-regex must fail")
    }

    /**
     * An unknown modloader fails the modloader check, while a known modloader passes
     * case-insensitively.
     */
    @Test
    fun modloaderCheckRejectsUnknownAndIsCaseInsensitive() {
        val modloaderValidator = ModloaderValidator(versionMeta)
        Assertions.assertFalse(
            modloaderValidator.checkModloader("NotARealLoader").modloaderChecksPassed,
            "Unknown modloader must fail"
        )
        Assertions.assertTrue(
            modloaderValidator.checkModloader("FORGE").modloaderChecksPassed,
            "Modloader check must be case-insensitive"
        )
    }

    /**
     * A modloader name that is not one of the five exact-cased loaders hits the version-check's
     * else branch and fails.
     */
    @Test
    fun modloaderVersionCheckFailsForUnknownLoader() {
        val modloaderValidator = ModloaderValidator(versionMeta)
        val configCheck = modloaderValidator.checkModloaderVersion("BadLoader", "1.0.0", "1.20.1")
        Assertions.assertFalse(configCheck.modloaderVersionChecksPassed)
    }
}
