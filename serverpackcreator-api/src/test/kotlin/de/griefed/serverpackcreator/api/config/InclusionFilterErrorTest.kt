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
package de.griefed.serverpackcreator.api.config

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that a bad **exclusion** filter is reported as an exclusion problem.
 *
 * `InclusionsValidator` validates both filters and reported both with
 * `configuration.log.error.checkcopydirs.inclusion`, so a malformed exclusion-regex told the user
 * *"Invalid inclusion-regex specified"* — pointing at the field they did not touch. The dedicated
 * `…checkcopydirs.exclusion` key exists in every locale file and was referenced by nothing.
 */
internal class InclusionFilterErrorTest {

    private val configurationHandler =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).configurationHandler

    /** The errors produced for one inclusion whose filters are [inclusionFilter] / [exclusionFilter]. */
    private fun errorsFor(inclusionFilter: String?, exclusionFilter: String?): List<String> {
        val packConfig = PackConfig().apply {
            modpackDir = "src/test/resources/forge_tests"
            setInclusions(
                arrayListOf(InclusionSpecification("mods", null, inclusionFilter, exclusionFilter))
            )
        }
        return configurationHandler.checkInclusions(
            packConfig.inclusions, packConfig.modpackDir, ConfigCheck()
        ).inclusionErrors
    }

    @Test
    fun aBadExclusionFilterIsReportedAsAnExclusionProblem() {
        val errors = errorsFor(inclusionFilter = null, exclusionFilter = "[unclosed")

        Assertions.assertTrue(errors.isNotEmpty(), "a malformed exclusion-regex produced no error at all")
        Assertions.assertTrue(
            errors.any { it.contains("exclusion", ignoreCase = true) },
            "the exclusion-regex failure was reported as $errors, which names the wrong field"
        )
    }

    @Test
    fun aBadInclusionFilterIsStillReportedAsAnInclusionProblem() {
        val errors = errorsFor(inclusionFilter = "[unclosed", exclusionFilter = null)

        Assertions.assertTrue(
            errors.any { it.contains("inclusion", ignoreCase = true) },
            "the inclusion-regex failure was reported as $errors"
        )
    }
}
