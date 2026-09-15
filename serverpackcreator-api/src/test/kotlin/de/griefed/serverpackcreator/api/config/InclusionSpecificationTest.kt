package de.griefed.serverpackcreator.api.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for the value-equality of [InclusionSpecification], added in refactor Phase 2 so that
 * two inclusions with the same source, destination and filters compare equal. Previously the
 * class used reference-equality, which made list-comparisons and the editor's dirty-check
 * over-report and disabled the deduplication in ConfigurationHandler.isZip.
 */
internal class InclusionSpecificationTest {

    /**
     * Pins that two inclusions with identical fields are equal and share a hash-code.
     */
    @Test
    fun identicalSpecificationsAreEqual() {
        val first = InclusionSpecification("config", "configs", "include.*", "exclude.*")
        val second = InclusionSpecification("config", "configs", "include.*", "exclude.*")
        Assertions.assertEquals(first, second)
        Assertions.assertEquals(first.hashCode(), second.hashCode())
    }

    /**
     * Pins that source-only inclusions (the common case) compare equal by value.
     */
    @Test
    fun sourceOnlySpecificationsAreEqual() {
        Assertions.assertEquals(InclusionSpecification("mods"), InclusionSpecification("mods"))
    }

    /**
     * Pins that a difference in any single field makes two inclusions unequal.
     */
    @Test
    fun anyFieldDifferenceMakesUnequal() {
        val base = InclusionSpecification("config", "configs", "include.*", "exclude.*")
        Assertions.assertNotEquals(base, InclusionSpecification("mods", "configs", "include.*", "exclude.*"))
        Assertions.assertNotEquals(base, InclusionSpecification("config", "other", "include.*", "exclude.*"))
        Assertions.assertNotEquals(base, InclusionSpecification("config", "configs", "other.*", "exclude.*"))
        Assertions.assertNotEquals(base, InclusionSpecification("config", "configs", "include.*", "other.*"))
    }

    /**
     * Pins that value-equality makes list-operations (contains, deduplication) work by value —
     * the behavior ConfigurationHandler.isZip and the editor's dirty-check rely on.
     */
    @Test
    fun listOperationsUseValueEquality() {
        val list = arrayListOf(InclusionSpecification("config"), InclusionSpecification("mods"))
        Assertions.assertTrue(list.contains(InclusionSpecification("config")))
        Assertions.assertEquals(
            arrayListOf(InclusionSpecification("config"), InclusionSpecification("mods")),
            list
        )
    }
}
