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
package de.griefed.serverpackcreator.app.web.index

import de.griefed.serverpackcreator.app.web.WebService
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import java.util.Properties

/**
 * Guards that creating the module's declared indexes never becomes a condition of starting up.
 *
 * `spring.data.mongodb.auto-index-creation=true` does not merely permit index creation — it makes
 * `MongoTemplate`'s own bean creation perform it during context refresh, so the database has to be
 * reachable *then*. Measured against an absent MongoDB: with the property the context waits ~30 s in
 * `createIndexes`, throws `MongoTimeoutException` and cancels the refresh; without it the context starts
 * and the driver reconnects later. `docker/docker-compose.yml` starts the app alongside its `db` service,
 * so losing that race is the normal first boot.
 *
 * The same reasoning already governs the migration runner, which runs on `ApplicationReadyEvent` "so an
 * unreachable database delays the migration instead of blocking the boot". Indexes follow it.
 *
 * **This test exists in this shape because a guard that reads the shipped file is not a test that runs
 * with it.** `src/test/resources/application.properties` shadows the shipped one, so the setting that
 * broke startup never reached the context under test and the suite stayed green. Here the shipped file is
 * read *and* its value handed to a real context boot, so the combination that broke is the combination
 * asserted.
 */
@SpringBootTest(
    classes = [WebService::class],
    properties = [
        "de.griefed.serverpackcreator.spring.schedules.database.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.files.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.versions.refresh=-",
        // Deliberately the shipped value, asserted below to still be the shipped value. Hardcoded because
        // @SpringBootTest properties are annotation constants and cannot be computed.
        "spring.data.mongodb.auto-index-creation=false"
    ]
)
internal class DeclaredIndexStartupTest {

    @Autowired
    private lateinit var context: ApplicationContext

    /** The `application.properties` the running app reads, not the test copy that shadows it. */
    private fun shippedProperties(): Properties {
        val copies = DeclaredIndexStartupTest::class.java.classLoader
            .getResources("application.properties")
            .toList()
            .filterNot { it.path.contains("/resources/test/") || it.path.contains("/test-classes/") }
        Assertions.assertEquals(
            1, copies.size,
            "Expected exactly one non-test application.properties on the classpath, found: $copies"
        )
        val properties = Properties()
        copies.single().openStream().use { properties.load(it) }
        return properties
    }

    /**
     * Pins that the context this test booted — with no MongoDB anywhere — came up. The `properties` above
     * mirror the shipped setting, and the next test pins that they still mirror it.
     */
    @Test
    fun theContextStartsWithoutADatabase() {
        Assertions.assertNotNull(
            context.getBean(ModPackService::class.java),
            "The web context must start without a reachable database"
        )
    }

    /**
     * Pins that the shipped configuration does not switch on refresh-time index creation, which is the
     * setting the boot above was given. Without this the two halves could drift and the boot would be
     * asserting about a configuration nobody ships.
     */
    @Test
    fun theShippedConfigurationDoesNotCreateIndexesDuringRefresh() {
        val shipped = shippedProperties().getProperty("spring.data.mongodb.auto-index-creation")
        Assertions.assertTrue(
            shipped == null || shipped == "false",
            "spring.data.mongodb.auto-index-creation=$shipped makes a reachable MongoDB a startup " +
                    "requirement — declared indexes are created on ApplicationReadyEvent instead"
        )
    }
}
