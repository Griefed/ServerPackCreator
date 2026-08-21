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

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.settings.WebserviceConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Guards that the property-key ServerPackCreator writes its database-URI under is one Spring Boot still
 * reads.
 *
 * Spring Boot **4.0.0 removed** `spring.data.mongodb.uri`: its metadata carries
 * `deprecation.level = "error"` with `replacement = "spring.mongodb.uri"`, and the connection properties
 * moved from `DataMongoProperties` (`@ConfigurationProperties("spring.data.mongodb")`) to `MongoProperties`
 * (`@ConfigurationProperties("spring.mongodb")`). A removed key does not warn — it is simply not bound, so
 * Boot silently falls back to `spring.mongodb.uri`'s own default, `mongodb://localhost/test`.
 *
 * Measured with the real jar, same URI, only the key differing:
 * ```
 * spring.data.mongodb.uri   hosts=[localhost:27017]   credential=null
 * spring.mongodb.uri        hosts=[127.0.0.1:27017]   credential=MongoCredential{userName='spcuser'…}
 * ```
 * The host substitution is the tell: `127.0.0.1` was configured, `localhost` is Boot's literal default.
 * Consequence in production — data written to `test`, existing data invisible, authentication skipped, and
 * every `SPC_DATABASE_*` container variable ignored.
 */
@SpringBootTest(
    classes = [WebService::class],
    properties = [
        "de.griefed.serverpackcreator.spring.schedules.database.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.files.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.versions.refresh=-"
    ]
)
internal class DatabaseUriPropertyTest {

    companion object {
        /** Deliberately not `localhost`: Boot's fallback *is* `localhost`, so only a different host proves binding. */
        private const val CONFIGURED_URI = "mongodb://spcuser:spcpass@127.0.0.1:27017/spc-guard-db"

        /**
         * Registers the URI under whatever key [WebserviceConfig] actually writes. Done dynamically rather
         * than as an annotation constant so the guard follows the production constant instead of repeating
         * a string literal — repeating it is how a guard ends up passing by construction.
         */
        @JvmStatic
        @DynamicPropertySource
        fun registerConfiguredUri(registry: DynamicPropertyRegistry) {
            registry.add(WebserviceConfig.DATABASE_URI_KEY) { CONFIGURED_URI }
        }
    }

    @Autowired
    private lateinit var connectionDetails: MongoConnectionDetails

    /**
     * Pins that the configured URI reaches the driver: host, credentials and database all arrive. Asserted
     * against Boot's own resolved [MongoConnectionDetails] rather than the environment, because the
     * environment was never the problem — the property was present and simply not bound.
     */
    @Test
    fun theConfiguredDatabaseUriReachesTheDriver() {
        val resolved = connectionDetails.connectionString

        Assertions.assertEquals(
            listOf("127.0.0.1:27017"), resolved.hosts,
            "The configured host did not reach the driver. `localhost` here means Boot ignored " +
                    "${WebserviceConfig.DATABASE_URI_KEY} and used its own default"
        )
        Assertions.assertNotNull(
            resolved.credential,
            "The configured credentials did not reach the driver — SPC would connect unauthenticated"
        )
        Assertions.assertEquals(
            "spc-guard-db", resolved.database,
            "The configured database did not reach the driver; `test` means Boot's default won"
        )
    }

    /**
     * Pins that the key is one Boot still binds, by reading Boot's own configuration metadata off the
     * classpath. Generalises past this one rename: any future Boot release that retires the key SPC writes
     * fails here, at build time, instead of silently redirecting a production database.
     */
    @Test
    fun theKeyIsNotRetiredBySpringBoot() {
        val mapper = ObjectMapper()
        val entries = DatabaseUriPropertyTest::class.java.classLoader
            .getResources("META-INF/spring-configuration-metadata.json")
            .toList()
            .flatMap { url ->
                url.openStream().use { mapper.readTree(it) }
                    .path("properties").toList()
            }
        val documented = entries.filter { it.path("name").asText() == WebserviceConfig.DATABASE_URI_KEY }

        Assertions.assertTrue(
            documented.isNotEmpty(),
            "${WebserviceConfig.DATABASE_URI_KEY} is not a property any Spring Boot module on the " +
                    "classpath declares — nothing will bind it"
        )
        val retired = documented.filter { it.path("deprecation").path("level").asText() == "error" }
        Assertions.assertTrue(
            retired.isEmpty(),
            "${WebserviceConfig.DATABASE_URI_KEY} is retired by Spring Boot " +
                    "(deprecation level 'error'). Replacement(s): " +
                    retired.map { it.path("deprecation").path("replacement").asText() }
        )
    }
}
