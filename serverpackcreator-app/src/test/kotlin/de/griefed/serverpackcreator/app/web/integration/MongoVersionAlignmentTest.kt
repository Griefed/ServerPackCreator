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
package de.griefed.serverpackcreator.app.web.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins which MongoDB release lines are in play: every image a user or developer is told to run is on
 * [DEPLOYED_LINE], and the database tests run [TESTED_LINE]. The two differ deliberately (see
 * [EmbeddedMongoAvailable.MONGOD_VERSION]); changing either is a decision this guard makes visible.
 *
 * The three files are declared inputs of this module's `test` task, so editing one re-runs this guard.
 */
class MongoVersionAlignmentTest {

    @ParameterizedTest
    @ValueSource(strings = ["../docker/docker-compose.yml", "../docker/docker-compose-dev.yml", "../README.md"])
    fun everyDeployedMongoIsOnTheDeployedLine(path: String) {
        val tags = Regex("""mongodb-community-server:(\d+\.\d+)\.\d+""")
            .findAll(File(path).readText())
            .map { it.groupValues[1] }
            .toList()

        assertTrue(tags.isNotEmpty(), "$path names no mongodb-community-server image")
        tags.forEach { assertEquals(DEPLOYED_LINE, it, "$path deploys MongoDB $it") }
    }

    @Test
    fun theTestsRunTheTestedLine() {
        assertEquals(TESTED_LINE, EmbeddedMongoAvailable.MONGOD_VERSION.substringBeforeLast('.'))
    }

    private companion object {
        const val DEPLOYED_LINE = "9.0"
        const val TESTED_LINE = "8.3"
    }
}
