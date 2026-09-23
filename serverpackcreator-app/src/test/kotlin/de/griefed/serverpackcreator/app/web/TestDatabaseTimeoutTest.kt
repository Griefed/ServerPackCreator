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
package de.griefed.serverpackcreator.app.web

import com.mongodb.ConnectionString
import de.griefed.serverpackcreator.api.settings.WebserviceConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Pins that the suite's MongoDB URI gives up quickly.
 *
 * The two `@SpringBootTest` classes here boot the real context and need no database — the driver
 * connects lazily — but their `ApplicationReadyEvent` listeners (`DeclaredIndexCreator` and the
 * migration runner) each perform one operation against it. With the driver's default 30 s
 * server-selection timeout, each context waited that out twice: measured at **60.37 s** for
 * `WebServiceContextTest` and **62.22 s** for `DatabaseUriPropertyTest`, i.e. 122.6 s of a 147.6 s
 * suite spent waiting for a server nobody expects to be there. With a bounded timeout the same eight
 * tests take 0.92 s and 5.71 s, and the whole module's suite went 147.6 s → 29.1 s.
 *
 * Read from the *effective* file under `build/resources/test`, not from `src`, because
 * `processTestResources` is what produces it.
 */
internal class TestDatabaseTimeoutTest {

    /** The longest a test may wait to discover that no MongoDB is running. */
    private val selectionBudget = TimeUnit.SECONDS.toMillis(5)

    @Test
    fun theSuitesMongoUriGivesUpQuicklyInsteadOfWaitingOutTheDefault() {
        val effective = File("build/resources/test/serverpackcreator.properties")
        Assertions.assertTrue(effective.isFile, "expected the processed test properties at $effective")

        val uri = effective.inputStream().use { Properties().apply { load(it) } }
            .getProperty(WebserviceConfig.DATABASE_URI_KEY)
        Assertions.assertNotNull(uri, "${WebserviceConfig.DATABASE_URI_KEY} is absent from $effective")

        val selectionTimeout = ConnectionString(uri!!).serverSelectionTimeout?.toLong()
        Assertions.assertNotNull(
            selectionTimeout,
            "the test URI sets no serverSelectionTimeoutMS, so every context boot waits out the driver's " +
                    "30 s default -- twice, once per ApplicationReadyEvent listener that touches Mongo"
        )
        Assertions.assertTrue(
            selectionTimeout!! <= selectionBudget,
            "serverSelectionTimeoutMS is ${selectionTimeout}ms, above the ${selectionBudget}ms this suite allows"
        )
    }
}
